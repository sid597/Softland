fail

# Review: the citation change on `citation-2026-09-27` (27 September 2026)

This is the rama skill's full-spec review (`references/phase-full-spec-review.md`),
run as a falsification pass by a reviewer session (Opus 5.5, max effort). It
covers the uncommitted working tree in `/Users/sid/projects/Softland-citation`
as it stood between about 18:15 and 18:52 IST. No file I cite changed in that
window: the last change was `src/rig/cite.clj` at 18:25:53. Paths are relative
to `src/proposal/rig-2026-09-25/` unless they begin with `src/app` or `/`.

**How the method was adapted.** In the phase doc, one session finds, fixes and
re-runs until the suite is green. The brief allows one output file and no
cluster, so this pass only hunts. Nothing was fixed, and no in-process cluster
ran, because another session holds the machine's one cluster.

I ran pure checks from the rig folder (`clojure -M -e` or `-i`), none of which
loads a cluster:
- the finder over the 29 in-scope documents (28 on `origin/main` under
  `src/proposal` and `docs`, and `RIG.md` at `HEAD`);
- `find-step`, `reading-step` and `bind-step` over this repository with git;
- direct calls into `reads/page-init`, `recipe/run`, `material/reading-facts`
  and `cite/check-step`.

"Checked" means I read it in the working tree at the lines named, or ran it as
one of those checks. "Reasoned" means I derived it and did not run it.

**What was read:**
- the plans: `PLAN-dependents.md`, `PLAN-citation.md`,
  `PLAN_VALIDATION-citation.md`, and `BUILD_NOTES-citation.md` (which appeared
  at 18:26);
- PROGRESS.md "Now", which holds the rules;
- the whole diff;
- the new code in full: `rig.store.dependents`, `rig.store.dependents-query`,
  `rig.material`, `rig.cite`, `rig.cite.run` and `tools/citation.edn`;
- the parts of `reads`, `read_exit`, `gate`, `gate_event`, `module`,
  `shared_reads`, `promote`, `promote_shape`, `envelope`, `runner`, `recipe`,
  `client` and `revision` that the change touches or relies on;
- the new tests, and the existing tests the runner change could break
  (`tools_test` R1 to R6 and V-F5, and `spec_fixes_test`'s runner on a
  re-classed layer).

**Verdict: fail.** There are two high findings:
- the report crashes on the first miss it exists to show (1);
- a marker run that keeps failing lets the next walk be recorded without the
  citations it should have carried, and those citations are never walked
  again (2).

Five medium findings and eleven low ones follow. The store's write side held
under every trace I made: the gate's entries, the replays, the record path, the
refusals and the rebuild. The problems are in three places:
- the new query's bounds and totality;
- the tool's walk;
- the driver.

The existing suite has not been run over the change (finding 7).

## Findings

### 1. HIGH: the report crashes on the first miss of the walk (checked)

*Evidence.* `src/rig/cite/run.clj:186-187` call `short` on a commit string.
`short` here is `clojure.core/short`, a cast to a 16-bit number. The function
meant is `short-sha` (`:166`). The pure call
`(some-> {:value {:against {:commit "3c9a1b2d4e5f"}}} :value :against :commit short)`
throws `ClassCastException: class java.lang.String cannot be cast to class
java.lang.Number`.

*Scenario.*
- A citation is bound, but its latest mark is not against its file's head
  reading. Or it is bound to an earlier reading and was never marked. Findings
  2 and 3 produce such citations, and so does any marker run still failing
  when the driver stops.
- `state-at-head` (`:168`) takes the `:else` branch for that citation.
- `(some-> head-reading :value :commit short)` throws, because the head reading
  has a commit.
- `rows-by-cite` runs inside `run!` (`:310`), so the exception leaves `run!`.
  `-main` stops the cluster in its `finally` (`:338`) and writes neither the
  report nor the run's EDN.

PLAN-citation section 6 says of such rows: "anything else is a miss of the
walk, shown as such." With this bug, a run with a miss produces no report at
all.

The 18:22 run had no miss, so it did not trip. The end-to-end test has no miss
row either (finding 16).

*Fix.* Use `short-sha` on both lines. Add a test that renders a miss row, for
example a bound citation with no mark whose file has a later reading.

### 2. HIGH: one persistently failing marker run lets the next walk drop its citations for good (reasoned from the code)

*Evidence.*
- `runner.clj:178-190` runs each unanswered match in the read's order and
  moves on to the next match after a failed run.
- The marker reads only `[:dependents A]`, where A is the previous reading's
  act (`tools/citation.edn:54`).
- `check-step` (`cite.clj:408-448`) marks what it finds. It cannot tell that
  the walk into A never landed.
- A failed run is not offered, so a pass made only of failures counts as quiet
  (`run.clj:107,113`), and the driver moves on to the next round.

*Scenario (one runner, P-C18 kept).*
1. File F has citations c1 to cn bound to reading R1. Their binder acts stand
   on `[R1 0]` and on a form of R1.
2. Round 1: R2 lands, and the marker run M2 fails. It fails deterministically
   in either of two ways:
   - n ≥ 512. The walk's act would have n marks plus its walk fact, and
     `:emit-all` refuses more than 512 facts (`recipe.clj:250-254,287`,
     `:too-many-facts`).
   - `[:dependents R1]` passes the read's limit (`:partial-read`,
     `recipe.clj:276`). Each binder act writes two entries under R1, so this
     happens at about n = 500 under the runner's default limit of 1,000
     (`runner.clj:60`).
   Any other failure that persists does the same.
3. Round 2: R3 lands. In the pass, M2 is retried and fails again. Then M3 runs.
   `[:dependents R2]` holds only acts that stood on R2, which are the binders of
   citations written at R2's commit, and none of c1 to cn. M3 is admitted. Its
   `:cite/walk` says `:found k :marked k` and names no gap.
4. M4 reads `[:dependents R3]` and finds M3 without them. From then on, c1 to cn
   are never marked again, and no record says the walk skipped them.

At today's scope the busiest file has 13 citations (checked: 164 Clojure
citations, 50 files), so the 18:22 run did not reach this. The validation
counted about 6,100 citations of one file in main's Markdown as a whole.

This breaks two of the frame's promises, silently and permanently, after a
single persistent failure:
- "a guaranteed walk" (PROGRESS.md:29-31);
- "you find out. Always."

*Fix.*
- The marker refuses to walk until the walk into A has landed. It fails with
  `:previous-walk-missing` unless `[:dependents A]` holds a `:cite/walk` fact
  whose `:reading` is `[A 0]`. Every walked reading has one: the first
  reading's walk too, since M1 writes a walk with `:found 0` and stands on its
  match `[R1 0]`. A persistent failure then stops that file's walk, reported
  every pass, instead of thinning it silently.
- Before the scope grows, lift the 512-fact cap for a walk's marks, for example
  by splitting one walk over several acts.

### 3. MEDIUM: a citation whose snapshot is forgotten is marked once, then dropped (checked)

*Evidence.* `cite.clj:422-441`.
- A row whose value is erased goes into `erased`, and the walk writes a `lost`
  mark for it. That mark has no `:bound`.
- At the next walk, the `lost` mark is a `:cite/mark` row whose value is a map.
  So it is not in `erased`, which keeps only non-map values (`:429`).
- `snapshot-of` gives `[c nil]` for it, and `:425` drops that from `cites`.

A pure run confirms it:
- walk 1 over `{:k :cite/bound :erased-at 180}` gives
  `[:cite/mark :cite/hlost :snapshot-erased]` and a walk with `:marked 1`;
- walk 2, over walk 1's facts as rows, gives only the walk fact, with
  `:marked 0`.

Two places say this does not happen:
- the docstring (`:414-415`): "never dropped";
- PLAN-citation section 10, F18's answer: "marked `:stale :snapshot-erased`".

The unit test (`cite_test.clj:222-228`) stops at walk 1.

A related case: suppose an earlier mark's value is erased while the citation's
`:cite/bound` still opens. The citation is marked `:snapshot-erased` although
its snapshot is intact, because the marker never reads the bound fact.

*Fix.*
- Treat every citation row whose snapshot is not a map as erased, not only
  rows whose value is not a map.
- Have the lost mark's act stand on the row it read.
- Before declaring the snapshot gone, read `[:latest c :cite/bound]`.

### 4. MEDIUM: `read-dependents` returns rows unbounded by the limit, and opens each act without yielding (checked in code; the magnitudes are reasoned)

*Evidence.* In `dependents_query.clj:52-73`, the limit counts index entries,
through the exit's page loop. Every kept act is then read whole (`:61`,
`subselect ALL`), and every row of it is opened (`:62-70`).

The row loop (`:62-70`) has no `yield-if-overtime`, and neither does the key
loop (`:76-82`). The page loop yields (`:38`), and the act loop yields once per
act (`:53`). `read-pattern`'s own key loop yields (`reads.clj:1791-1799`).

*Scenario.*
- At limit L, the answer holds up to L acts with all of their facts.
- Nothing caps the facts in an act (validation F1: neither the envelope nor the
  door). A lookup of 1,000 entries over acts of 512 facts returns about 512,000
  rows. The exit's ceiling is 10,000 rows a read (`reads.clj:609`).
- For a person or a model, the read entry records every matched pair in one
  `:exact` value (`reads.clj:1121`).
- One act of n facts is opened in a single stretch without yielding. At 1 to 3
  local seeks per open, 512 facts hold the task for roughly 0.5 to 0.75 s. The
  rig halved its put pages to stay within 0.1 to 0.3 s for events that never
  yield (`reads.clj:1149-1156`).

The marker's own lookups are small today, about 2c + 2 rows.

*Fix.*
- Count shown rows toward the limit: stop taking acts once the rows would pass
  it, and mark the answer `:partial`.
- Hold the answer to `max-limit` rows.
- Add `(yield-if-overtime)` at the top of the row loop and of the key loop.

### 5. MEDIUM: `read-dependents` is a public query that trusts its inputs: not total, and no visibility check (checked in code; the throws run pure)

*Evidence.*
- `dependents_query.clj:87-95` declares a query over `[*layer *pp *m]`.
- `:36` hands them to `reads/page-init` with no check.

Pure runs of the same functions the query calls:
- `page-init` throws `NullPointerException` when `m` is nil;
- it throws `NullPointerException` when the parsed pattern has no `:limit`;
- `dependents/bounds` (`dependents.clj:132-134`, a `case` with no default)
  throws `IllegalArgumentException` for any other `:kind`.

An exception in topology code is fatal to the worker (PROGRESS.md:192-193). Any
client can reach the query by name, just as `read_exit.clj:40-56` connects to
the others. With a well-formed pattern it returns the dependents of any layer,
values opened, with no `:for`, no `visible?` check and no read entry. That is a
second read door beside the one exit.

The two sibling queries that are also invoked by name, `shared-read-pattern` and
`shared-read-point`, take raw arguments and check them again: `reader-args`,
the parse and visibility (`shared_reads.clj:700-705,1438-1470`). The namespace
docstring's "Total" (`:18-19`) holds only for calls that come through
`read-pattern`.

*Fix.* Give the query the same inputs as `read-pattern` (`*layer *for *pattern
*as-of *limit`). Have it parse, check the layer's kind and visibility, and take
the moment itself, as the shared queries do. At the least, put a guard at its
head that answers `{:refused :bad-read}` unless all three hold:
- `*layer` is a readable keyword;
- `*m` is an integer;
- `*pp` is a parsed dependents pattern.

### 6. MEDIUM: environment failures are recorded as facts about the file or the citation, permanently (checked)

*Evidence.*
- `material.clj:408-410` treats every read error except `:missing-path` as an
  unreadable reading, and `reading-step` (`:460-473`) offers it.
- `rig.revision` returns `:git-failed`, `:unknown-revision` and
  `:not-a-repository` as data (`revision.clj:593-594,613-619`).

Pure runs:
- `reading-facts` over each of `:git-failed`, `:unknown-revision`,
  `:not-a-repository` and `:internal` gives a `:material/file` fact and a
  `:material/text` fact, each carrying that `:error`;
- `state-against` then gives `:stale :unreadable` for a citation of the file;
- `reading-step` on a revision git does not know returns facts, not an error.

*Scenario.*
1. A transient git failure happens during a file's head reading. A request
   naming a bad revision does the same.
2. The reading is admitted. The request's run is recorded and is never read
   again.
3. The marker then marks every citation of the file `:stale :unreadable`.
4. A binder that meets such a reading at its binding commit writes
   `:cite/unbound :unreadable` for good (`cite.clj:364`).

The finder has the same shape:
- `blame` returns nil on any git failure (`cite.clj:52`). That becomes
  `:unresolved {:reason :no-written-commit}` (`:251`) and then an unbound fact.
- A failed `ls-tree` is cached as an empty tree (`:189`), which gives
  `:path-not-found`.

PLAN-citation section 1.1 lists the errors a reading records: "missing path,
binary, not UTF-8, too large, unreadable forms". Those are properties of the
file. The code records any error.

*Fix.*
- In `reading-step`, return `{:error ...}` so the run fails and retries on the
  next pass, unless the error is one of `:missing-path`, `:binary`,
  `:not-utf-8`, `:too-large`, `:unreadable` or `:not-a-file`.
- In `find-step`, fail the step when `blame` or `tree-paths` fail, instead of
  recording a reason.

### 7. MEDIUM (process): the existing suite has not been run over the change (checked in the notes)

BUILD_NOTES-citation.md "Runs" lists three kinds of run:
- the new pure tests;
- `dependents-test` with `cite-tools-test` (62 of 62);
- the preview over the repository.

It does not list the rig's full suite: 172 tests on `1e63217e`. The change
reaches code that suite covers:
- `gate/decide*`, for every admitted act;
- `read-pattern`'s parse and dispatch;
- `shared-pattern>`, `delta>` and `read-delta`;
- the put page;
- `runner/run-tool`.

I traced R1 to R6, V-F5 and the runner on a re-classed layer against the new
runner, and read them as still holding. That is reasoned, not run. The phase
doc makes a green suite the precondition for a pass.

*Fix.* Run the full suite on this tree before the merge.

### 8. LOW: P-C13's nil rule skips reads whose pattern names a store-placed act (checked)

*Evidence.* `has-nil?` (`recipe.clj:256-261,270`) checks every nested element of
the pattern. A store-placed act's name carries a nil class
(`envelope.clj:37-41,267-272`). That covers forgets, crossings, person acts and
class and grain settings.

A pure run of `recipe/run` with a `:read` of `[:dependents [:alice nil :offer
u]]` gives `{:rows [] :mark :complete :skipped true}`. The read function is
never called, so no read entry is written either.

A tool that asks what stood on a forget, or on a crossing, gets a complete empty
answer. The citation's tools never name such acts, so nothing is wrong today.
For any tool that does, the answer is wrong and marked complete.

*Fix.* Skip only when a top-level element of the pattern is nil,
`(some nil? pattern)`. That is P-C13's stated case: "a first reading has no
previous act to look up".

### 9. LOW: the report on disk predates the binder it describes (checked)

`runs/citation-report.md` was written at 18:22:34, and `cite.clj`, with
`within-units`, changed at 18:25:53. Its row "`HANDOFF.md` 139,
`reads.clj:578-590` → not bound yet: not-inside-one-form" binds to `visible?`
(lines 578-589) under the current code. A pure `find-step` → `reading-step` →
`bind-step` gives `:within [578 589]` and a bound fact.

BUILD_NOTES-citation.md:162-176 calls that run a preview. Its counts, such as
"19 not inside one form", will change.

*Fix.* Rerun the driver before the report or its counts are quoted.

### 10. LOW: comments and docstrings describe the rebuild as it was before the validation (checked)

- `reads.clj:1222` says "a rebuild of it is drop then put". The revised P-D5 is
  put pages only.
- `read_exit.clj:175-181` says "five index fields" and "When it returns, the
  fields hold exactly what the log implies". `:ix-dep` is put but never swept,
  so after a rebuild it holds at least what the log implies.

*Fix.* Say "put pages only" and "six fields", and say that entries of
`:ix-dep` the log does not imply are skipped at read.

### 11. LOW: a run's claimed time can precede facts it stood on (reasoned)

*Evidence.* `runner.clj:105,108`. Stood-on now includes the recipe's pairs, but
`:claimed-when` is still the millisecond of the later of the match and the
tool.

*Scenario.*
1. In one pass the reader lands R_k.
2. A binder then runs late: either it was added after the readings (P-C11) or
   an earlier failure delayed it. It binds to R_{k-1}, so its act is stamped
   after R_k.
3. The marker for R_k stands on that binder's `:cite/bound` fact, but claims a
   time before it.

The gate's stamp still follows every stood-on stamp (`clock.clj:43-51`), so
ruling 4 holds for stamps. Only the claim is earlier than its own basis.

*Fix.* Take the maximum over every stood-on stamp. That is still a function of
the act's own parts.

### 12. LOW: placeholders written into kept facts are not marked first-record (checked against the plan)

PLAN-citation's rule (lines 7-11) is that a placeholder which would touch a
record is marked first-record. Section 9 marks P-C10, P-C12 and P-C19.

These are also written into kept facts:
- **P-C3.** The derived thing ids are the entity of every material and citation
  fact. Changing the derivation splits each thing into before and after.
- **P-C1.** The digest is copied into bound and mark values.
- **P-C15.** The request values have a shape.
- **P-C2.** Form facts carry kinds. Section 1.2 says "A kept fact carrying a
  kind makes the table first-record", but section 9 does not mark it.
- **The new vocabulary.** Tool facts are records, and they name the steps
  `:read`, `:emit-all`, `:stand-on` and the four domain steps, the formula
  `[:vec]`, the parts `:run` and `:replaces`, and the rule literals.

*Fix.* Mark each first-record in section 9 and in BUILD_NOTES' list, or say
why it does not touch a record.

### 13. LOW: every revision of a document makes new citation things, and the old ones are walked for ever (reasoned)

*Evidence.* The cite thing is derived from the document's commit
(`cite.clj:215-219`). `find-step` writes `:cite/found` with no `:replaces`
(`:254-264`).

*Scenario.* A document read at its next commit gives a new thing for every
citation in it, including unchanged ones. The previous revision's citations stay
bound, and every later walk of their files marks them again. The driver never
reaches this, since it reads one revision of each document per run. P-C3 does
not say it.

*Fix.* State it under P-C3. Or derive the thing from the document's line of
work and path, and the citation's text and position, and replace the previous
revision's found fact.

### 14. LOW: the `:bound` snapshot that marks carry has two shapes (checked)

- A late mark carries the binder's snapshot, `:within` included
  (`cite.clj:377-379`).
- A mark built from a `:cite/bound` row takes `select-keys v [:form :cited]`
  (`:396`), which drops `:within`.
- Later marks copy whichever shape they found (`:397`).

P-C10, the mark's value shape, is first-record.

*Fix.* Take the same keys on both paths.

### 15. LOW: a forgotten latest reading stops its file's reader and misplaces a late mark (reasoned)

- `reading-step` fails `:previous-unreadable` on every pass once the latest
  `:material/file` value is erased (`material.clj:470`). The file is never read
  again, and the run only reports that each pass.
- `bind-step` drops erased readings (`cite.clj:340,356`). Its `head` is then an
  older reading, and a late mark goes against the wrong one.

*Fix.* Decide what a reading after an erased one does, since it cannot read the
old value's `:heads`. Choose `head` by stamp over every row.

### 16. LOW: the tests do not reach the paths behind findings 1 to 3 (checked)

- The end-to-end test reads one file twice, at c1 and c3
  (`cite_tools_test.clj:87`). So no walk passes through a marker act, which is
  the chain's second link.
- No row there is a miss (finding 1).
- The erased-snapshot test stops at the first walk (finding 3).
- `dependents_test.clj:129` checks the shared-layer refusal through `micro-pp`
  directly, not through a read of a group layer or a re-classed layer.

*Fix.* Add:
- a third reading in the scratch repository;
- a miss row, rendered;
- two walks after a forget;
- one `read!` of `[:dependents x]` in a re-classed layer.

### 17. LOW: read entries record the role `:stood-on` for reads that nothing stood on (checked)

`recipe-read` (`runner.clj:137-148`) gives every `:read` step's entry the role
`:stood-on` (P-C12, first-record). A run that then fails offers no act, so
nothing stood on its read. Examples are a binder waiting `:no-reading-yet`
through every pass until its reading lands, and a `:partial-read`. These entries
build up at one per failing run per pass.

*Fix.* Put this to Sid with P-C12: a role for a read whose run offered nothing.

### 18. LOW: `:stand-on` has no count cap (checked)

`pairs-refusal` (`recipe.clj:300-309`) checks each pair, but not how many there
are. The gate writes one `:ix-dep` entry and one `:stood-on` entry per pair in
the admitting event, which never yields. The envelope caps neither (validation
F1). So one tool fact can make every run's act stand on any number of pairs.

*Fix.* Cap the pairs in a run, as `:emit-all` caps facts (for example at 1,024),
and refuse the step with `:too-many-pairs`.

## What held, trace by trace

**1. The query `read-dependents` and `dependents-read>`** (checked in code; the
Rama semantics follow the exit's own loops, which already run in tests).
- The nested `loop<-` is sound:
  - the outer loop emits `*rows` once;
  - the inner loop emits `*acc2 *pc2` once;
  - every branch ends in `:>` or `continue>`, so nothing after an `<<if` needs
    a variable bound on only one branch;
  - the destructuring `(first *gt :> [*act *on])` has the same form as the put
    page's.
- The moment works at both grains:
  - at fact grain, the range ends at `prefix · hex16(m + 1)`;
  - at act grain, it ends at `prefix-end`, and `page-step` filters by the
    entry's `:stamp`;
  - rows open as of m.
- An act not admitted by the moment is skipped: `yes-by?` after the page loop
  checks it. Skipped entries still count toward the limit, so an answer can be
  marked partial with fewer rows than the limit.
- The open cache threads correctly. The outer loop hands `*pc` to the inner
  loop, gets `*rpc` back as `*pc2`, and leaves it unchanged on the skip branch.
- The answer is `pattern-answer`'s. `read!` records `[:dependents x]` as the
  pattern, the count of rows, the fingerprint over the rows' `[fid stamp]`
  pairs, and for exact readers the list.
- It is total when called from `read-pattern`, because `dependents/kind?` only
  lets through a parse from `parse-pattern`, and m comes from `moment`. It is
  not total when called directly (finding 5). Bounds and yielding are finding 4.

**2. The gate** (checked).
- In `gate.clj:467-471,504`, `dep` is computed for a yes only and appended to
  `:index-put`. `write-decided>` writes it unchanged, in the decision's one
  atomic group, with no read.
- `index-writes` is total over what the envelope's parse gives, since each
  stood-on pair is a fid with a carried stamp. A nil return throws inside
  `decide*`, which `decide` turns into its unrecorded `:gate-error`.
- The record path writes nothing: `record-or-decide>` reads the name's record
  first. A replay whose first attempt did not commit left nothing, and decides
  again with one fresh set of entries.
- A `:stale-replaces` refusal writes no entries: `yes?` is false, so `dep` is
  `[]` (dependents test 5).
- A promotion landing into a stream target is decided by the same `decide`. Its
  stood-on carries the source's stamp, because the request must stand on its
  source (`promote_shape.clj:91`). So a landing cannot trip the new throw.
- The crossing, decided in the source layer, writes its entries there.
- A re-classed layer: acts after the re-class go to the micro gate, which
  writes no `:ix-dep`. Reads of the layer take the shared path, where
  `micro-pp` refuses the pattern after the visibility check. The stream era's
  entries stay unreachable, as P-D3 and F17 state.

**3. The rebuild** (checked).
- The put page reads each yes act's `[:stood-on name]` with `subselect ALL`.
  An act that stood on nothing reads as empty, which the rebuild test passes
  through.
- It counts those pairs toward the 2,048 bound.
- It appends `index-writes` per act. An act it cannot index adds nothing
  (`or ... []`), so no throw can empty the page.
- Every write is a set at a computed address, so the page is idempotent.
- `rebuild!` runs the put pages first, so it puts `:ix-dep`. It sweeps only the
  other five fields.
- `restore!` calls `rebuild!` for every stream layer. Forget replays do not
  touch `:ix-dep`, which holds ids only.
- `:rebuild-sweep` over `:ix-dep` is refused `:bad-op` (`all-fields` excludes
  it). `:drop` accepts it. The comments are finding 10.

**4. The runner's order** (checked in code; the tests' outcomes are reasoned,
not run).
- Names are derived before any recipe runs. `answered-runs` runs once, and the
  unanswered matches run and are offered one by one, in the match read's order.
- The report lines keep their shapes:
  - `{:match :name :recorded}`;
  - `{:answer :reason :stamp :match :name}`;
  - `{:refused :step-failed ... :match}`;
  - `{:refused :step-failed :error :no-output :match}`, which is new.
- R2's "both ran before: answered from the record" holds, because the lookup
  still comes first.
- R4 holds: an empty `also` leaves stood-on as `{match, tool}`.
- R6 holds: a new tool fid gives a new name, so the run runs.
- V-F5's two runners each look the names up, then run and offer. The second is
  answered from the record, or finds the name recorded, and both answers are
  `:yes`.
- `spec_fixes_test`'s re-classed runner holds: names are tagged by the class
  from settings, and `run-record` looks under both tags.
- The driver's `passes!` reads `:answer`, `:reason` and `[:error :error]` in
  those shapes.

**5. The recipe executor** (checked; the four tools parse and pass the loop
check in `recipe_citation_test`).
- `run` catches every throwable, and each new step returns data.
- The bounds: 16 steps; a formula at most 8 deep and 256 nodes, with `[:vec]`
  counted; 512 emitted facts in a run, counted together with `:emit` facts.
  `:stand-on` has no cap (finding 18).
- A malformed tool fact is refused `:malformed-tool` with a reason:
  - a rule literal outside its menu of one gives `:bad-arguments`;
  - so does a missing or extra argument.
- The loop check runs all four tools. Their key graph (`:material/read` →
  file, text, form; `:cite/doc` → found, scanned; `:cite/found` → bound,
  unbound, mark; `:material/file` → mark, walk) has no cycle. A tool reading
  its own outputs through `:read` is not a wake-up.

**6. `rig.material/reading-facts` across a chain of readings** (checked by hand,
and against `material_test`).
- I traced these cases:
  - a first reading;
  - a next reading;
  - a name gone;
  - a name still gone;
  - a name back;
  - a name gone again;
  - a missing file;
  - an unreadable file;
  - a reading after a missing one;
  - a reading after an unreadable one.
- In every case:
  - fact 0 replaces the previous reading;
  - fact 1 replaces `:text-fid`;
  - each form replaces `(:heads nm)`, a form fact or a gone fact;
  - `:gone` accumulates until a name comes back;
  - `:heads` values are distinct, because form and gone indexes never overlap;
    so no act replaces one fact twice.
- A `:stale-replaces` refusal can come only from a concurrent runner (P-C18).
  The runner's new order chains a file's two requests in one pass.
- The weak spots are the ones above: environment errors recorded as readings
  (6), and an erased previous reading (15).

**7. `rig.cite` on this repository** (checked by pure runs over
`origin/main` = `89c8e1a9` and `HEAD` = `1e63217e`).
- **Finding.** The finder finds 342 citations in the 29 documents. 164 of
  them name Clojure files, and 45 of the 342 are continuations. I sampled the
  continuations against the lines, for example `server.clj:77,88`,
  `envelope.clj:18-56, 81-86, 113-119, 367`, and
  `` `page/matter_room.cljc`:66-70 and :226 ``. All were real. Eight lines
  hold a Clojure path near a number with no citation found. Five of them cite
  no line: "a trace of what rewrote gate.clj on the 25th", "the 08:52 rewrite
  of `gate.clj`", "`micro.clj` block 2b" and two like them. Three are line
  references in the prose form F9 raised and PLAN-citation section 10
  declined, all in RIG.md:
  - `` (`envelope.clj` 43 to 52; `gate.clj` 219 to 235 ``;
  - `model.clj 467`;
  - `` `locks.clj` 591 to 606 ``.
  HANDOFF.md:88-89 defines a citation as a `path:line`, so these are outside
  its "never skipped" rule.
- **Resolution.** All 164 Clojure citations resolve: 153 on `main` and 11 on
  `rig`. None is unresolved. Crossing lines of work works: a main document
  citing the rig's `gate.clj` binds at the rig commit before the line's writing
  time (report row 1097, `stamp-for` at `fdc79b6c`). Bare names go where the
  validation said: `module.clj` to `src-inland`'s file, and `envelope.clj` in
  main's documents to the server's.
- **Binding.** `within-units` binds a range that runs onto blank lines (the
  `visible?` case in finding 9).
- **The late mark.** It stands on the head reading. With one runner I found it
  in the next walk in both tool orders I traced.
- **Stand-on pairs.** The stamps are right, since every fact of an act shares
  the act's stamp.
- **States.** The states are as specified. The snapshot's shape is finding 14,
  its loss finding 3, and the chain break finding 2.

**8. The rules** (checked).
- Values are sealed at the door. Thing ids and digests are keyed
  (`env/hmac-hex`), and `:ix-dep` holds ids only, so no forget has anything to
  reach there.
- Visibility through the one exit is the dependent's layer's (dependents
  test 4). F19, the other direction (`:on` can confirm a guess of a
  promotion's source layer), stays with Sid as the plan says.
- The one new hole is the ungated query (finding 5).
- Two things remain Sid's: the first-record marks (finding 12), and whether a
  run's act names its reads (F13, declined).

## The plans' answers, checked against the code

**True in the code:**
- **F1:** the index is at act grain, with one entry per stood-on fact and act.
- **F2:** `micro-pp` refuses the pattern.
- **F3:** `read-delta` refuses it before the opening, and `delta>` refuses it
  after the parse.
- **F4:** puts only, stale entries skipped at read, and the sweep refused. The
  comments lag (finding 10).
- **F5:** stood-on counts toward the put page's bound.
- **F6:** `index-writes` is total. The put page adds nothing for an act it
  cannot index, and the gate gives `:gate-error`.
- **F7:** the runner looks names up first, then runs and offers match by match.
- **F8:** the driver throws on a partial match read.
- **F9:** backticks and bare continuations work on this repository's shapes.
  `:cite/unparsed` was declined.
- **F10:** ids and digests are keyed.
- **F11:** the rules are literal arguments.
- **F12:** the four marks the validation asked for are there, but more
  placeholders need them (finding 12).
- **F15:** traced in item 6 above.
- **F16:** no-output runs are not offered, pairs are checked, and quiet means
  no run offered and answered.
- **F17 and F20:** the text is right.

**Stated, not enforced in code:** F14. One runner per layer is a placeholder,
and nothing in the runner prevents two.

**Left to Sid:** F13 and F19.

**Partly true, F18:**
- the `:snapshot-erased` mark holds for one walk only (finding 3);
- "bound before the next reading" rests on each round reaching quiet, but
  `passes!` stops at 12 passes without saying so (`run.clj:102,113`). A face
  refusal is offered and answered on every pass, so one of them keeps a round
  busy until that cap.
