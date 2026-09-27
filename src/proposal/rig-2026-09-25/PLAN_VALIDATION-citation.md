major-fail

# Plan validation: PLAN-dependents.md and PLAN-citation.md (27 September 2026)

Phase 2 of the rama skill (`references/phase-2-plan-validate.md`), run
adversarially by a validator session (Opus 5.5, max effort). Default FAIL;
every PASS below names the lines that carry it.

**What was read.** Both plans as they stood at 17:24 and 17:28 IST
(11,327 and 17,055 bytes; unchanged when this file was written). The rules:
`src/proposal/frame-2026-09-15/PROGRESS.md` "Now" (lines 9-230, the
Softland mirror). The brief as relayed (the citation tool; a lookup "ids
only, rebuildable from the log, respecting who may see what"; the simplest
thing that can change later without touching a record, marked a
placeholder). `src/proposal/citation-2026-09-27/HANDOFF.md`. The rig's code,
and RIG.md "Defaults taken overnight, not ruled" (1925-2068) and "The
machinery count so far" (2944-3059).

**Which code.** Every line number below is at `HEAD` = `1e63217e`
(`rig-2026-09-25`'s head, which `citation-2026-09-27` sits on), read with
`git show HEAD:<path>`. While this validation ran, the citation session was
building in this clone: at 17:53 IST `gate`, `module`, `reads`, `recipe`,
`runner` and `shared_reads` were modified and `dependents.clj`, `cite.clj`,
`material.clj` and five tests were new, all uncommitted. I judged the plans,
not that code; where the build already answers a finding, the finding says
so, because then the fix is to write down what the build does.

**Checked and reasoned.** "Checked" means read in code at `HEAD`, or
measured with `git` on this clone. "Reasoned" means derived and not run. No
cluster was run (another session holds the machine's one cluster).

**Edits.** None but this file. The phase doc says a validator fixes the plan
itself on minor-fail; the caller's instruction to write nowhere else
overrides that.

## Verdicts

- **PLAN-dependents.md: major-fail.** The index's grain (P-D2) writes n × m
  entries per act, uncapped, inside the gate's non-yielding event, and gives
  act-level answers one row per (stood-on fact, dependent fact). The plan's
  own marker is the workload that makes both quadratic (F1). The plan chose
  this grain for less code, which the skill forbids. Changing it touches
  sections 3, 4, 5, 6, 8 and 10. The plan's claims about the shared path
  (F2) and the standing path (F3) are false against the code, and its
  rebuild is neither wired nor safe to read during (F4, F5).
- **PLAN-citation.md: minor-fail.** Every finding is a localized edit. The
  most serious (F7) is already fixed in the uncommitted build, but the plan
  does not say it, and P-C11 depends on it.
- **Overall: major-fail**, the worse of the two. When unsure between minor
  and major, the method says major.

**What would move each to pass.**
- PLAN-dependents: act grain, or fact grain with its break point stated, a
  cap and deduplicated rows (F1); the `micro-pp` guard named (F2); P-D4's
  reason corrected and its site named (F3); a sweep judge for `:ix-dep`, or a
  drop-then-put wired into `rebuild!` with reads refused during it (F4); the
  put page's cap counting stood-on (F5); F6, F17 and F20; P-D6 marked
  first-record (F12).
- PLAN-citation: F7 written into section 3, then F8 to F16 and F18.

## Findings

Severity is for the plan as written. Each finding gives the evidence, the
scenario traced, and the fix.

### F1. HIGH (PLAN-dependents P-D2, section 8; PLAN-citation 2.3). Fact grain is quadratic, and the plan's own marker triggers it.

*Evidence (checked).*
- The gate writes every `:index-put` entry in the decision's one event
  (`gate_event.clj:65-67`, inside the yes branch at `:50`). That event never
  yields (module.clj's one-event design).
- Nothing caps facts per act or stood-on entries per act:
  `envelope.clj:373,387-390` checks each stood-on pair but not their number,
  `:396-400` refuses only an empty act, and the door (`client.clj`) caps
  neither.
- The rig's own event budget: a 4,096-row put page of sealed values held its
  task about 0.47 s (0.16 s when the rows open nothing), against a plan of
  0.1 to 0.3 s, so the cap was halved to 2,048 (`reads.clj:1135-1142`).
- A pattern read returns at most 10,000 rows (`reads.clj:602`).
- PLAN-citation 3 (line 215) fails a `:read` step on a partial answer
  (`:partial-read`).

*Scenario (reasoned from the plans' designs).* A file is cited by c
citations. Its marker act Mₖ has n = c + 1 facts (c marks and one walk). It
stands on its match and its tool, on the forms it compared (at most c), and
on "the facts it read" (the previous marks and bindings, about c to 2c)
(PLAN-citation 2.3, lines 197-198). So m ≈ 2c to 3c, and the act writes
n × m ≈ 2c² to 3c² `:ix-dep` entries in one event.

The next marker reads `[:dependents Aₖ]` at act level. Every entry under a
fact of Aₖ is its own row, so Mₖ alone contributes (c + 1) × (the number of
Aₖ's facts it stood on, up to c + 1) ≈ c² rows, one per (`:on`, fact) pair.
The same fact repeats once per `:on`.

| Citations of one file (c) | Entries per marker act | Rows in the next lookup | What happens |
|---|---|---|---|
| 12 (the most in scope today) | about 300 to 500 | about 170 | works |
| about 100 | 20,000 to 30,000 (about 0.3 s, reasoned from the page figures above) | about 10,000 | the lookup reaches the limit, the next marker's `:read` is partial, its step fails every pass, and **the walk for that file stops for good** |
| 1,000 | 2 to 3 million (tens of seconds holding the task, reasoned) | far past any limit | stops |

*Scale (checked with `git grep` on `origin/main`).* In the driver's scope
(`src/proposal/**` and `docs/**` Markdown, and RIG.md) there are 125 Clojure
citations, the busiest file 12 (`src-inland/softland/inland/module.clj`). So
the first run is below the break. All of main's Markdown holds 24,147
Clojure citations; 6,109 of them name the file `render.cljs`, mostly
under `history/`. HANDOFF step 3 is "First use over this project's citations".

*Why the grain is also wrong in kind.* Stood-on is a part of the act
(`env/act-parts`), not of each fact. One entry per (dependent fact, stood-on
fact) records the cross product and says nothing more than act grain does.
The plan picks fact grain "because the read then shows fact rows through the
read exit's existing 'no copy' path ... with no new topology code"
(PLAN-dependents 86-89). The skill: "Never trade I/O efficiency for code
simplicity."

*The alternative, built and costed.* One entry per (stood-on fact,
dependent act):
- Address: `fid-text(F) · hex16(stamp) · canonical(act)`.
- Entry: `{:on :on-stamp :act :stamp :n}`.
- Writes: m entries per act, not n × m. The marker at c = 100 writes about
  250 entries, not about 25,000.
- Read: one seek plus k iterations over the entries, then one seek per
  distinct act on `[:log act]` with `subselect ALL` and n iterations. Today's
  `:read-row` path costs one seek per row (`reads.clj:1746-1750`), so act
  grain is also cheaper to read.
- Act-level answers deduplicate by act, and each row carries `:on` as the
  set of A's facts that act stood on. At c = 100 that is about 200 rows
  instead of about 10,000.

*Fix.* Adopt act grain. That means a new show branch (an act entry
expanded to its rows), counting rows rather than entries for the limit and
the budget, and rewriting sections 3, 4, 5, 6 and 8. Or, if Sid keeps fact
grain as the placeholder, the plan must at least:
- state the break point (about 100 citations of one file);
- refuse, as data, an act whose n × m passes a stated cap;
- deduplicate act-level rows by fact;
- count `:ix-dep` writes in the put page's cap (F5).

### F2. HIGH as written, answered by the uncommitted build (PLAN-dependents 5, P-D3). The shared path does not parse its own patterns.

*Evidence (checked).* The plan says "`shared-read-pattern` parses its own
patterns and refuses this one `:bad-pattern` (P-D3). Checked when built"
(lines 147-149). In fact:
- `shared-pattern>` parses with `reads/parse-pattern`
  (`shared_reads.clj:1464`), and so do `opening-answer` (`:1616`) and
  `delta>` (`:1678`).
- `micro-pp` passes every kind but `:kv` (`:341-354`).
- So once `reads/parse-pattern` accepts `[:dependents x]`, a read of a group
  layer or a re-classed layer reaches `index-read> :micro` (`:786-796`) with
  `:ix :ix-dep` on `$$micro`, whose layer schema has no such field
  (`shared_reads.clj:80-110`, `micro.clj:135`).

*What would happen (reasoned).* This validation did not establish what Rama
does with a keypath into an undeclared fixed-keys field. There are two
possibilities:
- It throws. A throw in a query topology is fatal to the worker
  (`runs/phase5-read-build-probe.txt`: "Unexpected throwable! Will be
  treated as a fatal!"). Any reader could then take a worker down.
- It navigates to nil. The answer is `:complete` and empty. For a
  re-classed layer, step 6 merges in the stream era's `:ix-dep`
  (`shared_reads.clj:1496-1498`), so only stream-era dependents show, marked
  complete.

*Fix.* Name the guard: `micro-pp` refuses the dependents kinds
`:bad-pattern`. That covers `shared-pattern>` and `delta>`'s shared branch,
because both check `(refusal? *mpp)` before any index read. The uncommitted
build adds exactly this; the plan should say so.

### F3. MEDIUM (PLAN-dependents 5, P-D4). The standing-read reasoning is wrong in mechanism.

*Evidence (checked).* The plan says a standing read's deltas "scan `:ix-s`
and keep what `entry-matches?` names" (lines 150-153). That holds only for
`:all`, `:e` and `:k`. `delta-state` reads `:ix-s` with a filter only for
those three kinds (`shared_reads.clj:429-438`, `delta-prefix` `:421`); every
other kind reads its own index from the cursor `prefix · hex16(m+1)`
(`after-bound`, `:423-427`).

*Scenario (checked, and reasoned for the stamp's magnitude).*
- A fact-level `[:dependents F]` address is time-ordered within its prefix,
  like `[:ek]`, so its delta would deliver correctly.
- An act-level delta never delivers. Its cursor
  `canonical(nm) · hex16(m+1)` sorts above every entry of the act, whose
  next part is `%08x(idx)`. A stamp today is ms × 65,536 ≈ 1.17 × 10¹⁷, whose
  hex begins `01a…`; any index below about 2.7 × 10⁷ prints below that.

*Fix.*
- Correct the reason: act level must be refused; fact level would work, so
  refusing it is a choice.
- Name the site: the module, in `read-delta` before its opening read.
  `standing/subscribe!` is client code, and another client could call the
  query directly.
- The uncommitted build refuses in `read-delta` and `delta>`.
- Consider allowing fact-level standing reads: a live "what stood on F"
  feed is the frame's "you find out" for a view someone holds.

### F4. MEDIUM (PLAN-dependents 6, P-D5). The rebuild is not wired, and drop-then-put gives complete answers that are wrong.

*Evidence (checked).*
- a. `index-op` accepts `:rebuild-sweep` for any field in `all-fields`
  (`reads.clj:1176`). Adding `:ix-dep` to it makes that sweep legal, and
  `sweep-page-writes` would then delete every entry, since `fact-implied`
  knows only ek, ke, kv and s (`:435-444`, `:460-499`). The plan says the
  sweep "is refused `:bad-op`" but names no site; it has to be a change in
  `index-op`.
- b. `:drop` is documented as a test-only op (`reads.clj:1151-1162`).
  `rebuild!` sweeps a hard-coded list of fields (`read_exit.clj:187`), and
  `restore!` calls `rebuild!` (`:484`). Neither drops nor sweeps `:ix-dep`.
  So "a rebuild of this field is drop then put" exists only as a manual
  sequence.
- c. Drop-then-put empties the field and then refills it. A
  `[:dependents F]` read in between answers `:complete` with dependents
  missing. The existing rebuild (put, then sweep) never removes an entry the
  log implies.

*Fix (preferred).* A sweep judge for `:ix-dep`. An entry is implied if and
only if:
- its `:fid`'s record is a yes at `:stamp`;
- the row exists;
- `[:stood-on act on]` equals `:on-stamp`.

The sweep page already reads the record and the row (`reads.clj:1513-1515`);
the judge adds one seek. Add `:ix-dep` to `rebuild!`'s list.

*Fix (keeping P-D5).* Name the `index-op` change, wire drop-then-put into
`rebuild!` and `restore!`, and refuse `[:dependents x]` while a rebuild runs.
That needs a flag, which is itself a new mechanism.

### F5. MEDIUM (PLAN-dependents 6). The put page's bound does not cover what the plan adds.

*Evidence (checked).* The page stops at 2,048 rows (`reads.clj:1467`,
`:1135-1142`). The new `subselect ALL` read of `[:stood-on name]` has no
cap, since stood-on size is unbounded (`envelope.clj:387-390`), and the
`:ix-dep` writes (the sum of nᵢ × mᵢ) are outside the cap. Atomicity holds:
the reads and writes are on one task, and a page never yields
(`reads.clj:1440-1444`).

*Fix.* Count stood-on entries (act grain), or nᵢ × mᵢ (fact grain), toward
the page's cap. An act larger than the cap still goes whole.

### F6. LOW (PLAN-dependents 4 and 6). One throw erases a whole rebuild page.

*Evidence (checked).* `put-page-writes` wraps its body in one `try` that
returns no writes on any throwable (`reads.clj:336-347`). Appended inside
it, a throw from `dependents/index-writes` empties the whole page, the other
four indexes included, and the page still reports progress. In `decide*`
the same throw is `:gate-error` (`gate.clj:462-465`, `:511-514`).

*Fix.* Make `dependents/index-writes` total, with an error flag, and have
the put page answer `{:refused …}` on that flag.

### F7. HIGH as written, answered by the uncommitted build (PLAN-citation 3, 2.4 P-C11). The runner's order breaks the walk once recipes read the store.

*Evidence (checked).* `run-tool` computes every match's recipe
(`runner.clj:138`), then looks up names (`:143`), then offers (`:150-157`).
The plan's only runner change is "gives each run its read function and its
act's name, and puts the `:stand-on` pairs into the act's stood-on"
(line 230). With a `:read` step:

- a. Every pass recomputes every match, including runs already recorded.
  Each recomputation writes a read entry act (`read_exit.clj:147-148`) with
  role `:stood-on`, for a read nothing used: one entry per match per tool
  that reads, every pass, for as long as the layer lives.
- b. P-C11 fails. A marker added after readings R₁..Rₖ exist computes all
  its runs before it offers any. So Mⱼ's read of `[:dependents Aⱼ₋₁]` does
  not see Mⱼ₋₁'s marks. Every citation bound before Rⱼ₋₁ drops out of the
  walk. Mⱼ is then recorded and never reruns, so the loss is permanent.
  "Walks every reading once, in stamp order within a pass" (lines 204-206)
  is false with this runner.
- c. Two `:material/read` requests for one file in one pass both compute
  against the same latest reading. The second is refused `:stale-replaces`
  (`gate.clj:252-254`), a recorded no under its run name, so that request
  is never read again.

*Fix.* Write into section 3:
- Names first: `recipe/run-name` needs no facts.
- Then `answered-runs`.
- Then, for each unanswered match, compute and offer, in the match read's
  order (for `[:k k]`: entity, then stamp).

The uncommitted runner does exactly this, and its docstring cites
"PLAN-citation.md section 3" for it. The plan does not say it.

### F8. MEDIUM (PLAN-citation 3 and 5). The runner reads one bounded page of matches and stalls silently.

*Evidence (checked).*
- The match read is one read with a limit of 1,000 by default and 10,000 at
  most (`runner.clj:54,131-133`; `reads.clj:602`).
- It has no cursor: "b7, a 'start after' on the pattern read, is deferred"
  (RIG.md 2982-2983).
- A partial read "ran on its first `limit` matches only"
  (`runner.clj:173-174`).

*Scenario.* Every pass re-reads the same first N matches in address order,
already-run ones included, so matches past N never run. The pass offers
nothing new and counts as quiet.

*Scale (checked).* Today's counts are below 1,000: 125 citations and 29
documents. But `[:k :material/file]` and `[:k :material/read]` grow with
files × rounds, and main's Markdown as a whole (24,147) is past 10,000.

*Fix.* The driver treats `:mark :partial` on any match read as a stall and
fails loudly, not as quiet. State the ceiling. The lasting fix is b7, a
cursor on pattern reads.

### F9. MEDIUM (PLAN-citation 2.1, P-C6). The finder misses forms this repository uses.

*Rule broken.* HANDOFF.md 93-95: "Every other citation, most of them
Markdown, is reported as 'not bound yet', as a fact, never skipped."

*Evidence (checked with `git grep` on `origin/main`, `src/proposal` and
`docs` Markdown).*
- 4 Clojure citations (10 of any extension) put the path in backticks with
  the colon outside, for example `` `rama/envelope.clj`:18-19 `` and
  `` `page/matter_room.cljc`:66-70 and :226 `` (WHERE-THE-PLAN-FITS.md:1150).
- 10 Clojure line numbers (26 of any extension) are bare continuations after
  a comma: `envelope.clj:18-56, 81-86, 113-119, 367` (CONCLUSION.md:202),
  `object_container.clj:113, 628-657` (:206).
- Together that is about 14 of about 136 in-scope Clojure citations, one in
  ten, never found and so never reported.
- The class as written, `[A-Za-z0-9 _ . / ~ + -]` (line 131), reads as
  including a space. With a space, "src-inland module.clj:129-136"
  (WHERE-THE-PLAN-FITS.md:1152) becomes the path-text
  "src-inland module.clj", which is not found.

*Fix.*
- Accept `` `path`:N `` and bare `N` or `N-M` continuations after a comma
  or "and".
- State the class without a space.
- Write a `:cite/unparsed` fact for each line holding an extension-and-digits
  token that no form parsed, so a miss is a fact (integration.md, "a miss is
  a fact with a subject and a time").

### F10. MEDIUM, against the rules (PLAN-citation 1.3 P-C3, 1.1 P-C1). Unkeyed fingerprints over content.

*Rule.* "Fingerprints over values are keyed" (PROGRESS.md 167-170).

*Evidence.*
- P-C3's thing ids are unkeyed digests of content: `:file/h…` from
  `[repo line path]`, `:form/h…` from `[… name]`, `:cite/h…`. They sit where
  no forget reaches. An entity id is in the clear in every log row, every
  index address and every `:ix-dep` entry, and it confirms a guessed path or
  name.
- P-C1's digest is unkeyed. It sits inside a sealed value, but it is copied
  into `:cite/bound` and `:cite/mark` values under other locks. Forgetting
  the form's value leaves the copies.

*Fix.* Key both, with an HMAC under a secret the operator's configuration
names (as `:revision/*` takes repositories from configuration). Or mark each
first-record with the rule it bends. The frame's direction for ids is random
plus a registry cell (items 17 and 60).

### F11. MEDIUM, against the frame and integration.md (PLAN-citation 2 and 4). The rules Sid expects to be picks are compiled.

*Evidence.* The binding rule ("both ends in one named form"), the
same-function rule (P-C4) and what a stale citation shows (P-C10, item 44)
live inside `:cite/bind`, `:material/reading` and `:cite/check`. The tool
facts only wire them.

*Rule.*
- PROGRESS.md 37-40: "the menu is baked in code, the default lives in the
  first facts, the pick is a fact on a layer, key, tool or value."
- integration.md requirement 3 (lines 49-51): "inspectable, changeable and
  reusable after first use, by the people using it. Today that means editing
  an EDN record."
- HANDOFF.md 22-25: the pointer's targeting rule changes what a citation
  binds to, "while using it".

*Fix.* Give each step its rule as a literal argument from a closed menu,
with the default in the tool fact:
- `:cite/bind {:target #{:containing-form :lines :passage}}`;
- `:material/reading {:same #{:name-in-file}}` (the plan already names
  this one);
- `:cite/check {:show …}`.

It costs one argument per step now, and it is the pointer's lever later.

### F12. LOW-MEDIUM, against the rules. Placeholders that touch what a record carries are not marked first-record.

- **P-C10**, the mark's value shape. Item 44 is among the things that
  "change what a kept record carries" (PROGRESS.md 172-186).
- **P-C12**, the role `:stood-on`. It is written into every `:read/pattern`
  line (ruling 3's roles).
- **P-D6**, what the store writes when it finds a dependent. The handoff
  lists it among the questions that "freeze at the first kept record"
  (HANDOFF.md 38-41).
- **The largest open choice is taken with no P- mark:** the walk as a tool's
  read of the store, instead of a tool woken by "something you stood on
  changed" (HANDOFF.md 103-108; STARTER-round-3 item 2).

*Fix.* Mark all four.

### F13. LOW-MEDIUM, against the rules (PLAN-citation 3). A run's act does not name its reads.

*Rule.* "Who, when and reads on the act once" (PROGRESS.md 53-54).

*Evidence (checked).* A run's act references neither the read entries of
its `:read` steps nor its match read's entry. Entry acts get fresh names
(`read_exit.clj:140`) and stand on nothing (`:101-115`).

*Fix.* `read!` already returns `:entry` and `:entry-stamp`
(`read_exit.clj:153-155`). Have the `:read` step return them, and have the
runner add `[[entry 0] entry-stamp]` to the act's stood-on: one pair per
read.

### F14. MEDIUM (PLAN-citation 2.2 P-C9, 5). The walk needs one runner per layer, and the plan does not say so.

*Scenario, two runners (reasoned).*
1. Runner X's binder reads Rₖ as the latest reading and computes a late
   mark (P-C9).
2. Meanwhile runner Y's reader offers Rₖ₊₁, and Y's marker computes Mₖ₊₁
   from `[:dependents Aₖ]` before X's late mark lands.
3. The citation now stands on Rⱼ and Rₖ only. Mₖ₊₁ is recorded, and no later
   walk reaches the citation.

With a single runner, I traced reader, binder and marker in all six tool
orders: the late mark is found in every order.

The runner's "two runners at once offer one name" (`recipe.clj:256-263`,
the runner's docstring) is about names, not about the walk.

*Fix.* State "one runner per layer at a time" as a placeholder, with this
reason. The lasting fix is an expected version on the late mark; the rig's
envelope has none (HANDOFF findings).

### F15. LOW-MEDIUM (PLAN-citation 1.3). Heads are not carried forward through gone and missing readings.

*Scenario.* "One gone fact per name the previous reading had present and
this one does not" (lines 115-117). A name gone in Rₖ and still absent in
Rₖ₊₁ gets no new gone fact. If `:gone` in Rₖ₊₁'s value does not carry it,
the form's return in Rₖ₊₂ is written with no `:replaces`. The gone fact then
stays a head for ever, so the form's thing has two heads, and the replace
that "is the claim" of sameness (item 3) is lost.

The same happens to `:material/text` after a missing or unreadable reading:
it has no `:text-fid` to replace.

*Fix.* `:gone` accumulates until the name reappears. A missing or unreadable
reading carries `:text-fid` and the previous heads, and the plan says in
which field (it says so only for unreadable).

### F16. LOW (PLAN-citation 3, 5). A refusal on its face makes "passes until quiet" loop.

*Evidence (checked).* An act refused on its face is recorded nowhere
(`module.clj:126-128`), so its run has no record and is offered again every
pass. Data can reach this:
- `:emit-all` over an empty list, as a recipe's only output, gives an empty
  act (`:empty-act`, `envelope.clj:400`). The runner offers `{:facts []}`,
  because an empty vector is truthy (`runner.clj:139-141`).
- A `:stand-on` pair with a malformed fid, or a stamp at or above 2^62,
  makes the whole offer `:malformed` (`envelope.clj:387-390`).

*Fix.*
- Fail the step, or the run, when it has no facts.
- Have `:stand-on` validate its pairs as data.
- Define quiet as "no run offered", and report runs refused on their face
  as misses.

### F17. LOW (PLAN-dependents 2 and 7). State what P-D1 and P-D3 cost the promise.

*Evidence.* A dependent in another layer, such as Bob's citation standing on
`:softland`'s forms, is never walked. The marker reads its own layer, and a
tool is woken only by facts in its own layer (the runner reads matches in
L). After a re-class the lookup is refused for the layer's whole history,
its stream era included.

*Fix.* Say in section 7 that "When something you built on changes, you find
out. Always." holds within one layer under P-D1, and that a re-class stops
the walk. Then Sid rules them knowingly.

### F18. LOW (PLAN-citation 2 to 6). Smaller gaps.

- **The `:lines` part of a request.** `:cite/doc`'s `:lines {line
  head-commit}` (line 246) is unexplained. If it is precomputed blame, the
  person's request carries what the finder's step is meant to compute.
- **What a `:read` step reads.** Its layer, `:for`, `:working` and `:limit`
  are unstated. The build uses L, the owner, L and the pass's limit.
- **The report.** It should list refused runs and runs refused on their
  face.
- **The guarantee.** "Bound before the next reading" holds only with one
  runner, complete match reads and admitted readings. Say so, and have the
  driver check.
- **Blame.** A later edit of a line (a re-wrap, a typo fix) moves
  written-at forward and can bind to the wrong form without a signal. Today
  this is latent: for 122 of 122 in-scope citations, the blame commit is the
  first commit whose document held that citation text (checked).
- **Lost snapshots.** A citation whose `:bound` snapshot no longer opens
  drops out of the walk silently. Give it a mark (for example
  `:stale :snapshot-erased`) instead.

### F19. LOW, for Sid (PLAN-dependents 2, visibility, the other direction).

Section V traces the direction the plan argues: the owner of F cannot learn
of a private dependent. The other direction is new. The lookup shows an
act's stood-on (`:on`, `:on-stamp`) to anyone who can read the act's layer.

*Scenario.* A promotion's landing into a one-owner target stands on the
source fact and on the crossing (`promote.clj:177`); the store composes this
stood-on. The crossing's name is `[source-layer nil :crossing uuid]`, and
the landing's name carries the same uuid (`envelope.clj:274-287`). So a
reader of the target can confirm a guess of the source layer with
`[:dependents [L nil :crossing uuid]]`. Today no read through the exit shows
a landing's provenance: rows carry fid, stamp, e, k, replaces and mark
(`reads.clj:833-834`).

*Fix.* Put it to Sid: either a promotion's provenance is visible to the
target's readers, or `:on` is filtered to facts in layers the reader can
see.

### F20. LOW (PLAN-dependents 4, wording). "Writes the same entries" is imprecise.

"A replay of the event writes the same entries (I-G2)" (lines 108-110). A
replay whose first attempt did not commit decides again and may take
another stamp from the wall clock. Nothing of the first attempt landed, so
exactly one set of entries lands either way.

*Fix.* Say exactly that.

## I. The store side: each scenario traced

1. **`write-decided>` writes any field named in `:index-put`: PASS
   (checked).** `(keypath *layer *ix *ia) (termval *ie)` takes the field from
   the entry (`gate_event.clj:65-67`), for a yes only (`:50`). It needs
   `:ix-dep` in the fixed-keys merge (`module.clj:92-98`). The entry's
   classes fit:
   - `:on` and `:fid` are normalised vectors (`envelope.clj:412`, the
     `norm-fid` keys);
   - stamps are Longs;
   - `:mark` is a set (`:367`);
   - a nil `:replaces` is allowed (the skill's `pstate-schema.md`: "nil is
     always permitted").
2. **An entry with `:copy false` is shown by reading the log row: PASS
   (checked).** `show-how` gives `:read-row` unless the entry has
   `:erased-at` or `:opened` (`reads.clj:856-867`). The read path reads
   `[:log nm idx]` and opens it as of the moment (`:1746-1750`). `shown-row`
   builds e, k, replaces and mark from the entry (`:869-879`).
3. **`fixed-keys-schema` and absent keys: PASS (checked; the question is
   moot).** The `:ix-dep` entry has its own schema, and all nine fields are
   present. Existing no-copy entries and tombstones already omit fields
   (`reads.clj:244-245,261`); the skill says "All keys are optional on
   write".
4. **The shared path refuses the pattern as data: FAIL (F2).**
5. **A standing read would silently never deliver: FAIL in its reasoning
   (F3).** True at act level, false at fact level.
6. **The rebuild's put page can read `:stood-on`.** Atomicity: PASS, one
   task and no yield (`reads.clj:1440-1444`). Bounds: FAIL (F5).
7. **Adding `:ix-dep` to `all-fields`: no breakage, but it makes a sweep
   legal (F4a).** `all-fields` is used only by `index-op`'s sweep (`:1176`)
   and drop (`:1209`) (checked by grep over `src/` and `test/`). `purge>`,
   the person purge pages, `rebuild!` and `restore!` name their own fields.
   The test helper `stream-fields` (`reads_rest_test.clj:95`) is local.
8. **A replayed gate event: PASS (checked; F20 on wording).**
   `record-or-decide>` reads the name's record first
   (`gate_event.clj:118-127`). If the first attempt committed, the record
   path runs and writes no `:ix-dep`. If it did not, it left nothing (an
   event is atomic on one task, rama-check RESULTS), and one fresh set
   lands.
9. **A resend answered from the record: PASS (checked).** It goes by the
   same record path.
10. **A stale-replace refusal: PASS (checked).** The reason is
    `:stale-replaces` (`gate.clj:252-254`), so `yes?` is false and `ix` is
    `no-index-writes` (`:462-464`). The plan appends only for a yes, and
    `write-decided>` writes `:index-put` only in its yes branch.
11. **A value forget of a dependent: PASS (checked).** `purge>` tombstones
    ek, ke and s and deletes kv and of (`reads.clj:298-321,381-433`); it
    never touches `:ix-dep`. The entry still leads to the row, which opens
    `{:erased-at date}` and shows as erased (`:836-841`). The forget act
    itself stands on its target (`read_exit.clj:250`), so it appears among
    the target's dependents as a `:forget` fact. That is true, and ids only.
12. **A person forget: PASS (checked).** `person-page>`, `dying-rows>` and
    `purge>` name their fields, and `:ix-dep` holds no value. The
    forget-person act stands on the person's making fact (`client.clj:399`),
    which gives one entry in `:people`.
13. **A promotion's landing: PASS for a stream target (checked).** The
    crossing is decided by `gate/decide` (`promote_flow.clj:93-95`) and a
    stream landing by `record-or-decide>` (`:132`), both through `decide*`.
    The crossing stands on `{src carried, [req 0] stamp}` (`promote.clj:83`)
    and the landing on `{src carried, [crossing 0] stamp}` (`:177`). A micro
    target writes nothing (P-D3). F19 covers visibility.
14. **A re-classed layer (the H-1 fix): consistent, but the consequence goes
    unstated (F17).** Runs and read entries go through the micro gate, which
    writes no `:ix-dep`. Reads take the shared path (`reads.clj:1679-1680`)
    and are refused, with F2's guard. The marker's steps fail and are
    reported, not silently lost.
15. **Reads as of a moment: PASS (checked and reasoned).**
    - At fact level the range ends at `prefix · hex16(m+1)`, as `[:ek]` does
      (`reads.clj:702`).
    - At act level, entries are filtered by stamp ≤ m (`stamp-ok?`, `:732`).
    - Rows open as of m (`:1749`).
    - A dependent's stamp is after its `:on-stamp` (the clock rule, ruling
      4). So nothing admitted after m shows, and an erasure shows its date.
16. **Collisions of the act-level prefix: PASS (checked).** A name is
    `[readable-keyword (nil|class) scheme uuid]` (`envelope.clj:291-298`). A
    readable keyword cannot hold U+0000 (`:91-101`), and a UUID's text holds
    none. So `canonical(nm) · U+0000` prefixes exactly the entries of that
    act's facts.
17. **Encodings: PASS (checked).** `%08x` prints more than eight digits for
    an index at or above 2^32; stood-on indexes come from the offerer, and
    `fid?` allows any non-negative integer. The U+0000 after the index keeps
    prefixes exact; only sort order breaks, as with `reads/fid-text` today.
    The dependents namespace's own copy of `fid-text` only has to agree with
    itself, since it both writes and reads.
18. **Write volume per event against the skill's rule: FAIL (F1).**

## II. Visibility: can F's owner learn of a private dependent?

*Setup.* Alice owns F in `:alice` (personal). Bob's act D in `:bob` stands
on F. D's entries sit in `:bob`'s `:ix-dep`.

- **The lookup.** Alice's `[:dependents F]` in `:alice` or `:base` finds
  nothing of D. In `:bob` it is refused `:not-visible` (`reads.clj:578-589`,
  `:1682-1683`), the same answer a layer never made gets. No signal.
- **The read entry.** It lands in the reader's working layer
  (`read_exit.clj:101-115`), so Bob's reads stay in Bob's layer. A reader
  who records into a layer Alice can see publishes their own read; that is
  the exit's general rule, not this plan's.
- **The fingerprint.** It is a keyed HMAC over the [fid stamp] pairs the
  reader was shown (`reads.clj:925-945`), and Alice cannot make Bob's read.
- **An error.** A malformed x is refused before the settings are read
  (`:1662-1664`). No refusal depends on whether D exists.
- **Stamps and subjects.** D's stamp is later than F's, and nothing changes
  in `:alice`. D's subject slot comes from Bob's layer owner, the grammar and
  the tool (ruling 8), not from stood-on, so Alice's person forget does not
  reach D. The task clock that layers on one task share is an existing side
  channel that this plan does not change.

*Result: PASS for the direction asked.* The reverse direction is F19.

## III. The tool side

- **The runner computes every run of a tool before offering any: FAIL
  (F7).** The build fixes it; the plan does not say so.
- **Tools run in their facts' stamp order: PASS with one runner.** I traced
  all six orders of reader, binder and marker over one round. Bindings land
  before the next round, and the late mark is found. With two runners it
  fails (F14).
- **Runs are named from layer, tool fact and match.** With a `:read` step, a
  run is no longer a function of that triple alone. A rerun with the same
  parts is answered from the record: values are sealed, and the digest
  leaves them out (`envelope.clj:203-225`). A rerun with a different
  stood-on is refused `:name-taken`, and the first act stands. Looking names
  up first (F7) makes such reruns rare.
- **Every run offers at least one fact: PASS as designed.** That is the
  reader's fact 0, the finder's `:cite/scanned`, the binder's bound or
  unbound fact, and the marker's walk fact. The hazards are in F16.
- **Late binding: PASS with one runner. First readings: PASS.** A first
  reading has no `:replaces`, so the pattern holds nil (P-C13) and only the
  walk fact is written.
- **The marker finds the dependents of the previous reading's act: PASS at
  today's scale.** Beyond about 100 citations of one file it fails (F1).
- **Every mark compares with the binding snapshot: PASS as designed.** Each
  mark carries `:bound`. One silent exit remains (F18).
- **The loop check: PASS (checked).** The key graph is `:material/read` →
  {file, text, form}; `:cite/doc` → {found, scanned}; `:cite/found` →
  {bound, unbound, mark}; `:material/file` → {mark, walk}. It has no cycle.
  `loop-free` looks only at match patterns (`recipe.clj:298-322`), and a
  tool reading its own outputs through `:read` is not a wake-up.
- **A `:read` inside `recipe/run` during the compute phase: PASS.** `read!`
  is plain Clojure over the foreign API and blocks on the entry's answer;
  `recipe/run` catches every throwable (`recipe.clj:224,242`). The executor
  stops being pure: each read writes an entry act.
- **A run's act referencing its own name in fact values: PASS.** The name
  can be computed before the offer (`recipe.clj:256-274`). Values are
  sealed and outside the digest. The recipe must be given the class-tagged
  name the offer will use.
- **Gone and reappearing forms: FAIL (F15). A file missing at HEAD: PASS for
  its marks** (`:stale :file-gone`); carrying its text forward is F15.
- **Blame and path resolution on this repository's documents: PASS for
  blame and resolution (checked); FAIL for the finder (F9).**
  - All 122 in-scope Clojure citations on main resolve to exactly one
    candidate under the plan's rule (114 on main, 8 on the rig line).
  - HANDOFF.md's 16 all land on the intended file.
  - Bare names resolve as their text means: `module.clj` in FACTS.md goes to
    `src-inland`'s, `envelope.clj` in CONCLUSION.md to the server's.
- **"Bound before the next reading": PASS only under unstated conditions
  (F18).**

## IV. Against the rules and the brief

- **Rules.** F10 (fingerprints keyed), F11 (the pick is a fact), F12
  (first-record marks), F13 (reads on the act).
- **The frame's "a guaranteed walk"** (PROGRESS.md 28-31) is not guaranteed
  as written: F7, F8, F14.
- **The brief.**
  - "Ids only": PASS. Entries hold ids, keys, stamps and marks. With P-C3
    the entity ids are unkeyed digests of paths (F10).
  - "Rebuildable from the log": in principle only. The rebuild is not wired
    and gives wrong complete answers mid-way (F4).
  - "Respecting who may see what": PASS (section II).
  - "Simplest thing, marked a placeholder": mostly done; F12 lists the
    marks that are missing.

## V. The skill's checklist, as it applies to an extension

- **PState schemas: PASS.** One new field sits beside the five index fields
  of `$$layers`. It has the same key and partitioner as they do, so it is a
  field, not a new PState. The entry is a fixed-keys-schema; the map is
  subindexed with size tracking off; no Object anywhere.
- **Partitioning: PASS.** The field lives on the layer's home task, like
  every one-owner index, and a lookup touches one task. Seeks do not grow
  from N = 1 to 16 to 128. Nothing uses `|all`. All of a layer's walk runs
  on one task under ruling 2; re-classing is the release valve.
- **Topologies: PASS.** None is new. Writes happen in the stream gate's
  existing event, and the rebuild runs on `*index-ops`.
- **Production readiness.** Concurrent clients, client restarts and worker
  restarts: PASS (one atomic event). Large scale: FAIL (F1, F5, F8).
  Non-idempotent writes: none, since all are sets. Writes across partitions:
  none.
- **Internal depots, stream appends, in-memory state: none added.**
- **Minimality.** The simplest design I can sketch that still does the
  job: one field per one-owner layer, one entry per (stood-on fact,
  dependent act), written in the admitting event; one pattern through the
  exit at fact and act level; rebuilt by put pages and a sweep judge. Against
  it:
  - The plan's n × m grain is more than needed (F1).
  - The entry's `:e`, `:k`, `:replaces` and `:mark` duplicate what
    `:read-row` reads anyway. Keeping them spares `shown-row` a change; that
    is optional.
  - Drop-then-put stands where a sweep should (F4).
  - Each mechanism justifies itself: without `:ix-dep` the brief's lookup is
    lost; without `:on-stamp` no reader can tell which version of F was
    stood on; without the act-level form the marker needs one lookup per
    fact of A (60 seeks for a 60-form file); without P-D4 an act-level
    standing read silently never delivers.
- **Throughput: FAIL.** F1's act grain costs less on both the write and the
  read.

## Self-consistency

Every place this file says "not wired", "unstated", "overclaims" or "fails"
is a numbered finding and counts in the verdicts above. The PASS items
carry no hedge that would contradict them.

PHASE_VALIDATION:major-fail
