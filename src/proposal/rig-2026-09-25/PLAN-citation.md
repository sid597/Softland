# Plan: the first tool, a citation that stays true (27 September 2026)

Written by the citation session (Opus 5.5, max effort) on branch
`citation-2026-09-27`. Sid's brief of 27 September, seven items; the
handover `src/proposal/citation-2026-09-27/HANDOFF.md` (on main). The store
side, the lookup from a fact to what stood on it, is `PLAN-dependents.md`.
Every choice marked **P-C** is a placeholder: the simplest thing that can
change later without touching a record, or, where it would touch one,
marked first-record. Nothing is a ruling. The handoff's order put a walk
with Sid before this build; Sid's brief of this session builds first, with
placeholders, and lists them for him when each piece is done.

## 0. The whole, in plain words

- **Material.** A file at a git revision is read into the store as facts:
  one fact for the file's reading (its units, their lines, which unit holds
  each line), one for its text by line, and, for a Clojure file, one fact
  per named top-level form on that form's own thing. The next revision's
  reading of the same file writes new facts on the same things, each
  replacing the last; that replace is the reading tool's claim "same name,
  same file, same function".
- **Citations.** A document is read at a revision; each `path:line` or
  `path:start-end` in it is a citation, a fact on its own thing. It is bound
  to the named form holding those lines at the revision its line was
  written (the commit `git blame` gives), as a fact that stands on that
  form's fact; or it gets a "not bound yet" fact with the reason.
- **The walk.** When a file's next reading lands, a tool woken by it asks the
  store what stood on the previous reading's facts (the lookup), and marks
  each citation it finds true (same text, same lines), moved (same text,
  other lines) or stale (text changed, or the function gone). Its marks
  stand on the new reading, so the reading after finds them in turn.
- **The report.** Every citation in the documents, with its state at HEAD.

Four tools, each a fact over a vocabulary of steps; operator code (the
driver) seeds the layer, writes the requests, runs the runner's passes and
writes the report.

## 1. Material (`rig.material`, beside `rig.revision`, which is unchanged)

### 1.1 Reading a file

`read-file [repo-path rev path]`: the whole text through the rig reader's
public API (`rig.revision/read-span` over lines 1 to the line count, which
a first call past the end reports), then cut by `rig.revision/cut-for`:
- Clojure (`.clj .cljc .cljs .edn`): `rig.revision/forms`; each unit gets a
  head, a kind and a name (1.2).
- Anything else: `rig.revision/blocks`, each unit kind `:passage`, no name;
  plus sections: a heading block (up to three spaces, one to six `#`, then a
  space, a tab or the end) opens a section of its level that runs to the
  line before the next heading of the same or a higher level, or to the
  last line; its title is the heading's text.

Result: `{:commit :cut :lines [text of each line] :units [...] :sections
[...] :unit-by-line [...] :section-by-line [...]}` or `{:error r :commit?}`
(missing path, binary, not UTF-8, too large, unreadable forms). A unit:
`{:i :kind :head :name :lines [first last] :chars [start end] :digest}`,
lines 1-based and inclusive, chars as `rig.revision` counts them, digest
HMAC-SHA256 over the content under the rig's secret, first 32 hex digits
(P-C1; keyed, as the rig's constraint on fingerprints over values asks).
`:unit-by-line` and `:section-by-line` are vectors indexed by line − 1,
each the index of the unit (any form or passage, named or not) or of the
innermost section holding the line, or nil. The pointer session indexes
these with `nth`; the binder uses them for containment.

### 1.2 Names and kinds, as the server's adapter gives them

`form-head [content]`, a small reader of the form's first two data (no
evaluation, no library), as `src/app/server/ingest/clojure_adapter.clj`
does it (checked: `:66-89` its table, `:94-141` resolution, `:143-172`
duplicates): a top-level reader conditional is `:clj/reader-cond`, unnamed;
a form that is not a list is `:clj/other`; otherwise the head symbol,
resolved through metadata, maps by its unqualified name through the
server's table (`ns def defonce defn defn- defrecord deftype defprotocol
defmacro defmulti defmethod deftest defmodule comment`, and `e/defn` as
`:clj/electric-fn`). Name-carrying kinds, and `ns` as the name `"ns"`, take
the next datum, through metadata, when it is a symbol; `defmethod` appends
`":"` and its dispatch value's text. Duplicate names in one file get `~2`,
`~3` in file order (the server's R7).

Two additions (P-C2): `deframaop` and `deframafn` map to `:clj/rama-op`
and `:clj/rama-fn`, named, since this repo's store is written in them; and
any other head whose unqualified name starts with `def` and whose next
datum is a symbol is `:clj/def-other`, named. A kept fact carrying a kind
makes the table first-record.

### 1.3 Things and facts (first-record where a kept fact would carry them)

- **The file** is a thing, `:file/h<32 hex>` from `[repo line path]`, an
  HMAC under the rig's secret, so an id does not confirm a guessed path
  (P-C3: derived; the frame's direction is a random id plus a registry cell
  plus same-as, open items 17 and 60; "line" is the line of work, `:main`
  or `:rig`, because the same path holds different files on the two
  branches).
- **A named form** is a thing, `:form/h<32 hex>` from `[repo line path
  name]` (P-C3). Unnamed forms, passages and sections are rows of a reading,
  found by (reading fid, `:i`); no thing.
- **A file at a revision** is a fact, not a thing ("a version is on the
  fact; a new version is a new fact on the same thing; no thing per
  version", LEDGER row of 17 September).

One reading is one act by the reading tool, facts in this order:
0. `:material/file` on the file thing: `{:repo :line :path :rev :commit
   :cut :units [...] :unit-by-line [...] :sections [...] :section-by-line
   [...] :text-fid fid :gone {name fid}}` or, for a file missing or
   unreadable at the revision, `{... :missing true}` or `{... :error r}`
   with no units. Each named unit carries `:thing` and `:fid`. Replaces the
   file's previous reading.
1. `:material/text` on the file thing: `{:commit :lines [...]}`; replaces
   the previous text. Absent for a missing or unreadable file.
2. … one `:material/form` per named form, on its thing: `{:repo :line :path
   :commit :name :kind :head :lines :chars :digest}`; replaces the form
   thing's previous fact (a form or a gone fact) when the previous reading
   names one. **This replace is the claim of item 3**: the same function at
   the next revision is the same name in the same file, written by the
   reading tool, whose recipe names the rule (`:same :name-in-file`, the
   only rule the step knows, P-C4).
3. … one gone fact per name the previous reading had present and this one
   does not: `:material/form` `{:gone true :repo :line :path :commit
   :name}`, replacing its head.

Every fact id in the reading's value is absolute, `[run-name idx]`: the
runner gives the recipe the name its act will have (section 3). A reading
where nothing changed still writes every form again (P-C5): the next
revision was read, so every citation that stood on the old forms is walked
and marked, true included. An unreadable reading writes no form and no gone
fact and carries the previous heads.

## 2. The citation (`rig.cite`)

### 2.1 Finding (the finder's step)

A document at a revision (`rig.revision/read-span`, whole). Per line:
- `path:N` and `path:N-M`, `-` or `–`, where `path` is a run of letters,
  digits and `_./~+-` (no space) ending in an extension from a closed list
  of source and text extensions (so `example.com:8080` is not one), and the
  path in backticks with the colon outside, `` `path`:N `` (P-C6);
- a continuation after a citation in the same line takes that citation's
  path: `:N` or `:N-M` after a comma, `and`, `;` or `&`, and a bare `N` or
  `N-M` after a comma or `and` when a delimiter (a backtick, `,;.)]`,
  `and` or the line's end) follows it, so `12, 3 tests` is prose (P-C6).
Each is `{:at {:line :col} :text :path-text :lines [a b] :inherited?}`.

**When it was written** (P-C7): the commit that last changed its line,
`git blame --porcelain <rev> -- <doc>`.

**Which file it names** (P-C8): the tree of the document's own line of work
at that commit first, then each other line of work at its last commit
before that commit's time (`git rev-list -1 --before`). A candidate is a
path equal to `path-text` or ending in `/path-text`. One candidate:
resolved. Several: the ones sharing the longest directory prefix with the
document; one left, resolved, else ambiguous with the candidates. None in
any line: not found. The target kind is by extension: Clojure, Markdown or
other.

Facts, one act a document: `:cite/found` per citation on its thing
(`:cite/h<32 hex>` from `[repo line doc-path doc-commit line col]`,
P-C3): `{:doc {:line :path :commit} :at :text :path-text :lines :written
:target :resolved {:line :path :commit :file} | :unresolved {:reason
:candidates} :inherited?}`; and `:cite/scanned` on the document's thing
(`:doc/h…`): `{:line :path :commit :found n}`, so a document with no
citation still records that it was read.

### 2.2 Binding (the binder's step)

Over a found citation and the rows of `[:ek file :material/file]`, the
file's readings:
- not Clojure, or unresolved: `:cite/unbound` `{:reason :markdown-target |
  :other-target | :path-not-found | :ambiguous-path ...}`;
- no reading at the resolved commit yet: the step fails, nothing is
  offered, and the next pass retries;
- the reading missing or unreadable: unbound, `:file-missing` or
  `:unreadable`;
- lines past the end: unbound, `:lines-past-end`;
- both ends in one named form (`:unit-by-line`), once the lines at either
  end that no unit holds (blank lines, comments between forms) are set
  aside, so `code_import.clj:251-322` still names `commit-lineage`, which
  ends at 321 before a blank line (P-C17; found in the run over this
  project): **`:cite/bound`** `{:cite
  :found :cited [a b] :form {:thing :fid :name :kind :lines :digest
  :commit} :reading fid :path :line}`, the act standing on the form's fact
  and the reading's fact with their stamp;
- both ends in one unnamed form: unbound, `:inside-unnamed-form` with its
  kind; else unbound, `:not-inside-one-form`.

A citation bound to a reading that is no longer the file's latest (bound
late, P-C9) is also marked against the latest reading in the same act,
which stands on that reading too, so the next walk finds it.

### 2.3 Marking (the marker's step)

Woken by a file's reading. For a first reading there is nothing to walk.
Otherwise it has read `[:dependents A]`, A the previous reading's act: the
facts of every act that stood on any fact of A. Among them the
`:cite/bound` and `:cite/mark` facts name citations; one mark per citation,
against the new reading (P-C10, "what a stale citation shows", open item
44):
- the form present, same digest, same lines: `:true`;
- same digest, other lines: `:moved`, with the lines now;
- other digest: `:stale`, `:changed`, with the lines now;
- no form of that name: `:stale`, `:gone`; the file missing: `:stale`,
  `:file-gone`; unreadable: `:stale`, `:unreadable`.
A mark compares with the snapshot taken at binding, never with the previous
mark, so the state is always "since it was written". `:cite/mark` `{:cite
:state :why :bound {...} :against {:reading :commit :form :lines
:digest}}` on the citation's thing; the act stands on the new form facts it
compared with and on the facts it read. Every run also writes one
`:cite/walk` on the file's thing, `{:reading :commit :was :found :marked}`,
so a walk that found nothing is still recorded and not run again.

### 2.4 Older citations (open item 85, P-C11)

The runner runs a new tool over every existing match, so a marker added
after readings exist walks every reading once, in stamp order within a
pass. The binder's late mark covers a citation bound after its file moved
on. Nothing else is owed.

## 3. The vocabulary and the runner (`rig.store.recipe`, `rig.store.runner`)

What the tools need that the vocabulary lacks, each counted (section 7):
- **`:read` step** `{:pattern formula}`: a pattern read through the one
  exit as the tool: its id the reader, kind `:tool`, `:for` the layer's
  owner, working layer and read layer the pass's layer, its permission, its
  `:rows?`, the pass's limit, role `:stood-on` (P-C12, first-record: the
  role is written into every read line). It returns `{:rows :mark
  :entry}`. A refused read fails the step; a partial read fails it
  (`:partial-read`); a pattern holding nil reads nothing and returns no
  rows (P-C13), so a first reading has no previous act to look up.
- **`:emit-all` step** `{:facts formula}`: every fact of a list, each
  `{:e :k :v}` with an optional `:replaces` fact id; the key must be in
  `:out`, the value not nil.
- **`:stand-on` step** `{:pairs formula}`: `[[fid stamp] ...]` the run's act
  stands on besides its match and its tool (T-FR5 widened, P-C14).
- **`[:vec f ...]`**, a formula: a vector of formula values (a pattern is a
  vector).
- **`:replaces` and `:run`**, parts of a match: the matched fact's
  `:replaces`, and the name the run's act will have.
- **Four domain steps**: `:material/reading`, `:cite/find`, `:cite/bind`,
  `:cite/check` (sections 1 and 2); the first two read git through the
  operator's repository ids, as `:revision/*` does.
- **The runner** (validation F7, F16): names every match's run first and
  looks up which are answered, then, for each match with no answer, runs
  its recipe and offers at once, in the match read's order (for `[:k k]`,
  by entity, then stamp), so each run sees what the ones before it wrote,
  a recipe's reads are recorded only for a run that runs, and a file's two
  requests in one pass chain. It gives each run its read function and its
  act's name, puts the `:stand-on` pairs into the act's stood-on, and
  offers nothing for a run with no facts (`:no-output`), since an act of
  no facts is refused on its face and recorded nowhere.
- **One runner per layer at a time** (P-C18, validation F14): two runners
  can lose a late-bound citation (one binds late while the other's marker
  has already walked); the lasting fix is an expected version on the late
  mark, which the envelope does not have yet.

## 4. The tool, as facts (`tools/citation.edn`)

Four tool facts, EDN data the driver writes through the door as the
operator (only the operator or the owner writes tools, For Sid 63), each
granted `[tool L L]`:
- `:material-reader`: matches `[:k :material/read]`; reads `[:latest file
  :material/file]`; `:material/reading`; emits the reading's facts.
- `:cite-finder`: matches `[:k :cite/doc]`; `:cite/find`; emits.
- `:cite-binder`: matches `[:k :cite/found]`; reads `[:ek file
  :material/file]`; `:cite/bind` with `:target :named-form` (P-C17); stands
  on; emits.
- `:cite-marker`: matches `[:k :material/file]`; reads `[:dependents
  previous-act]`; `:cite/check` with `:show :state-and-lines` (P-C10);
  stands on; emits.
Each rule a step applies is a literal argument from a closed menu, so the
pick is in the tool fact, where a person changes it by writing a new tool
fact, and the menu is the step's code (validation F11; PROGRESS.md "the
menu is baked in code, the default lives in the first facts, the pick is a
fact"): `:same :name-in-file`, `:target :named-form`, `:show
:state-and-lines`, each a menu of one tonight.
Their key graph has no cycle, so the loop check runs all four. Requests
are facts too: `:cite/doc` `{:repo :line :rev :path :lines {line
head-commit}}` on the document's thing, and `:material/read` `{:repo :line
:rev :path}` on the file's thing, written by the person (P-C15).

## 5. The driver (`rig.cite.run`, operator code)

1. One in-process cluster (4 tasks); the seed: `:people`, the person
   `:sid`, the layer `:softland` (personal, owner `:sid`), Sid's
   permission; the four tools and their grants.
2. A `:cite/doc` request for each Markdown document under `src/proposal/`
   and `docs/` at main's head, and for `RIG.md` at the rig branch's head.
3. Passes until quiet (a pass that offers nothing new); a tool whose match
   read is partial stops the driver, because the runner reads one page of
   matches with no cursor and matches past the limit would never run
   (validation F8; the lasting fix is b7, a cursor on pattern reads).
4. The reading plan (P-C16): the found citations read back through the
   exit; for each resolved Clojure file, its distinct binding commits in
   commit-time order, then its line of work's head. Round k writes each
   file's k-th request, then passes until quiet, so a citation is bound
   before its file's next reading lands.
5. The report, read through the exit as Sid.

## 6. The report (`runs/citation-report.md`)

Every citation, by document and line: the text, what it resolved to, what
it bound to (form and commit), and its state at HEAD:
- the latest mark, when it is against the file's head reading;
- "true, bound at HEAD" when bound to the head reading and never marked;
- "not bound yet" with the reason;
- anything else is a miss of the walk, shown as such.
Counts first.

## 7. The count, as the rig's machinery count takes it

A unit is one named code unit added to the fixed side or the vocabulary,
or one existing unit whose behaviour changes (the rig's assumed unit,
IMPLICIT_SPEC O21). Classes: (a) fixed-side steps the frame promised, (b)
fixed-side steps nobody anticipated, (c) capabilities. Taken in
`BUILD_NOTES-citation.md` as built, with the zero: nothing under `src/`
names the four tools.

## 8. Tests

Pure: `rig.material-test` (heads, names, kinds, duplicates, blocks,
sections, per-line tables, the reading facts across revisions),
`rig.cite-test` (finding, blame, resolution on a scratch git repository,
binding, marking), `rig.store.recipe-citation-test` (the new steps and
formulas). On a cluster: `rig.store.dependents-test` (PLAN-dependents.md
section 9) and `rig.cite-tools-test`: the four tools as facts over a
scratch repository whose cited function moves, changes and goes, the
report's states, and the zero.

## 9. Placeholders

- P-C1 (first-record) the digest: HMAC-SHA256 of the content under the
  rig's secret, 128 bits, inside a sealed value, and copied into bound and
  mark values.
- P-C2 (first-record, once a kept fact carries a kind) two Rama heads and
  any `def…` head named.
- P-C3 (first-record) thing ids derived from their source by an HMAC under
  the rig's secret; lines of work in the file's. A citation's thing is
  derived from its document's commit, so reading a document at a later
  commit makes new citation things with no replace, and the earlier ones
  stay bound and walked (review finding 13); the driver reads each document
  once.
- P-C4 the same-function rule: same name, same file; the replace is the
  claim.
- P-C5 every reading restates every form.
- P-C6 the citation forms, the extension list, continuations in one line.
- P-C7 written-at is the line's blame commit.
- P-C8 path resolution: own line first, then others by time; directory
  preference.
- P-C9 a late binding is marked at once.
- P-C10 (first-record) what a stale citation shows: the state, why, the
  lines now; the mark's value shape.
- P-C11 what a new tool owes history: every existing match, once.
- P-C12 (first-record) a recipe's store read has the role `:stood-on`.
- P-C13 a nil in a pattern reads nothing.
- P-C14 a run's act stands on what its recipe names.
- P-C15 (first-record) requests are the person's facts, in the shapes of
  section 4.
- P-C16 the reading plan is operator code: binding commits, then the head.
- P-C17 what a citation binds to: the named form holding both ends, once
  the lines at the ends that no unit holds are set aside.
- P-C18 one runner per layer at a time.
- The vocabulary (first-record once a kept tool fact names it): the steps
  `:read`, `:emit-all`, `:stand-on` and the four domain steps, the formula
  `[:vec]`, the parts `:run` and `:replaces`, and the rule literals.
- P-C19 (first-record) which part is the store's walk and which the tool's:
  the walk is the tool's read of the store's lookup, not a tool woken by
  "something you stood on changed" (HANDOFF.md, "Choices the walk must put
  to Sid"; STARTER-round-3 item 2).

## 10. Revised after validation (`PLAN_VALIDATION-citation.md`)

The validation of this plan was a minor-fail. Each finding and what the
build does:
- F7: the runner, section 3 (built before the validation reported).
- F8: the driver stops on a partial match read, section 5.
- F9: the finder's forms, section 2.1, with tests over this repository's
  shapes (`envelope.clj:18-56, 81-86, 113-119, 367`, `` `path`:N ``).
  Other reference forms, such as "PLAN-reads-rest.md line 1424", are not
  `path:line` and are not sought; the validator's `:cite/unparsed` fact for
  them is not built.
- F10: ids and digests keyed, sections 1.1, 1.3.
- F11: the rules as literal arguments, section 4.
- F12: P-C10, P-C12 and P-C19 marked first-record; P-D6 in
  PLAN-dependents.md.
- F13, declined, for Sid: a run's act does not name its reads. The fix
  proposed (one stood-on pair per read entry) makes a run's act depend on
  which pass and which runner read, so a rerun is no longer a retry and two
  runners at once would offer one name with two contents (T-FR5, V-F5: the
  run's name and content are a function of the layer, the tool fact and the
  match). "Reads on the act once" and "a rerun is a retry" pull against
  each other for a tool's runs; the read entries are recorded, and name
  the tool, the layer and the moment, but no act names them.
- F14: one runner per layer, P-C18.
- F15: the reading carries `:heads` (every name ever seen, present or
  gone, with its head's fact id), `:gone` (accumulating until a name comes
  back) and `:text-fid` (the text fact is written for every reading,
  missing and unreadable ones included), so a form that comes back replaces
  its gone fact and the text chain never breaks (built; `rig.material-test`
  "readings-across-revisions").
- F16: a run with no facts is not offered; `:stand-on` validates its pairs
  as data; quiet is "no run offered and answered".
- F18: the request's `:lines` is `{line head-commit}`, the heads of the
  lines of work the finder may resolve a path in, recorded with the request
  so the resolution is reproducible; the `:read` step's layer, reader and
  limit, section 3; the report lists runs refused in the last pass; a
  citation whose binding snapshot no longer opens is marked `:stale
  :snapshot-erased`; "bound before the next reading" holds with one runner,
  complete match reads and admitted readings, which the driver's rounds and
  its partial check give; the blame rule's latent failure (a later edit of
  a line moves written-at forward) stays, P-C7.

## 11. Reviewed (`REVIEW-citation.md`)

A fresh Opus session reviewed the built code as a falsification pass:
fail, 2 high, 5 medium, 11 low. What the build does now:
- 1 (high): the report called `clojure.core/short` where `short-sha` was
  meant, so a miss of the walk stopped the report; fixed, and a test renders
  a miss.
- 2 (high): a marker run that fails every pass (at 512 or more citations of
  one file, `:emit-all`'s cap, or a partial lookup) let the next reading's
  walk go on without the citations it should have carried, silently and for
  good. The marker now refuses to walk a reading unless the previous
  reading's walk stood on it (`:previous-walk-missing`), so a gap stops the
  file's walk loudly, retried and reported every pass. Lifting the cap for a
  file of more than about 500 citations (a walk split over several acts) is
  not built.
- 3 (medium): a citation whose snapshot was forgotten was marked once and
  then dropped; it is now marked on every walk, the mark standing on the row
  it read. When only a forgotten mark carries a citation into a walk and its
  bound fact still opens, the citation is still marked `:snapshot-erased`:
  the marker does not re-read bound facts.
- 4, 5 (medium): the lookup's query, PLAN-dependents.md section 5.
- 6 (medium): a read error that is not the file's own (`rig.material/
  file-errors`), a failed blame, `ls-tree` or `rev-list` fail the step, so
  the run is retried, instead of becoming a fact.
- 7 (medium): the full suite runs once on this tree (BUILD_NOTES).
- 8: the nil rule looks at the pattern's top level only.
- 9: the report is rerun after the fixes.
- 10: the rebuild's comments say put pages only.
- 11: a run's claimed millisecond is that of the latest stamp it stood on.
- 12: first-record marks, section 9.
- 13: P-C3.
- 14: every mark carries the binding snapshot with `:within`.
- 15, for Sid: once a file's latest reading is forgotten, the reader cannot
  read its heads, so that file is never read again, and a late mark goes
  against the latest reading that still opens. What a reading after a
  forgotten one should do is open.
- 16: tests for 1 to 3, a third reading in the end-to-end test, the
  lookup's row limit and its direct call.
- 17, for Sid, with P-C12: a read whose run then failed is recorded with the
  role `:stood-on`, though nothing stood on it; it needs a role of its own.
- 18: a run's act may stand on at most 1,024 pairs besides its match and
  tool (`:too-many-pairs`).
