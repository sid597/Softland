# Implementation validation: the read path

Part: `read-path` of the read build: the contract's B1 to B5, its rules R1 to R5, and the measures of
its section C (`../BUILD-reads.md`). Stage: the build, step "validate the implementation" (the Rama
skill's `phase-4-impl-validate.md`, by its template `artifact-impl-validation.md`). Written by the
build stage's own session (Opus 5.5), 12 October 2026, for the build session, which judges it. It
read no code of the rig, the server or Inland, and no `env.clj` or `.env` file. It committed nothing
and staged nothing.

Checked: the code under `src-store/softland/store/read_path.clj` and `read_path/` as this session
left it, against `PLAN-read-path.md` as its check left it, with rulings 78 and 80 in the place of the
plan's text, against the contract, and against rulings 1 to 80 of `PROGRESS.md`. "The plan" is that
plan; "function N" is item N of its "Every function, in the order to build". A file name without a
folder is under `src-store/softland/store/`. Line numbers are of the working tree at the end of this
stage.

Four kinds of statement are kept apart. *Run*: a file under `runs/` holds what was printed, and the
line names it. *Played*: the read path's pure proof moved the real functions through every order of
a small world, with no Rama. *Traced*: this session read the code line by line for the case.
*Not verified*: none of those.

**Nothing is kept between two changes beyond what ruling 26 with 76 allows.** The read path holds
who looks at what, what each asked for, the pushes, and one mark for each read. It holds no row, no
value, no answer and no place-answer. The build did not reach for a kept value, so it did not stop
for R2. Run: the fields of the two maps, read from the type while reads were open, are
`(:opened? :owed? :query :sources :task-routing-ids :tell :turn)` and `(:called :opening :readers)`
(`runs/after-read-path-read-path.txt`).

**Verdict of this step: pass, with two things that the build session must rule on before it calls
the part done.** Every check of the template that fits a client of the module holds. One fault of
the code was found by tracing in this step, fixed in place, and its check was seen to fail without
the fix (section 6, T1). Seven places where the plan could not be met as written are in section 4,
each with what was found on Rama and what was built. Two of them are not faults of the code against
the plan. They are facts about the design on Rama 1.6.0 as it runs, and each touches what the
contract asks:

1. **A live asker of a place, an entity or a run is told a change about a second late, in a
   streaming layer too, and Rama sends her whole run again each time.** Run, through the read path,
   in-process (`runs/measures-read-path.txt`, M4): from the streaming depot's ack to the call of the
   asker's function, middle 1,000 ms and slowest 1 in 100 2,001 ms for a place of the facts table,
   and 26 of 500 acts were told by a call of their own; for a place of the facts by value, under the
   same load, middle 0.15 ms, slowest 1 in 100 0.23 ms, and 500 of 500. The cause, by two runs: a
   push on rows that hold a run of bytes is sent its whole value anew at each change, and is called
   back about once a second; the same rows with a String, or with no value, get a diff that names
   the key, for each write (`runs/raw-rp-probe-bytes.txt`, `raw-rp-probe-pace.txt`,
   `raw-rp-probe-diffs.txt`). The bytes are in a fact's row since the part `what-the-gates-keep`;
   the row's form is that part's, and this part did not change it.
2. **The plan's worked case does not open on Rama as it ships.** Rama bounds the pushes on one table
   on one task: 1,000 opened, the next was refused (`runs/raw-rp-probe-limit.txt`). A canvas of
   5,000 kept live through three layers needs about 10,000, one for each check of ruling 12. Run:
   it gave the word `:reach-the-store` after 9.3 s each time, in a streaming layer and with the base
   as the farthest (M3). M2 at 1,000 live reads opened 998.

Neither is hidden by a passing check. Sections 4 and 11 give each with its numbers.

## 1. What was built, file by file

New files, all of this part:

| File | What it holds |
|---|---|
| `read_path.clj` | `softland.store.read-path`: the one file of the read path that requires Rama. The types `ReadPath` and `LiveRead`; the calls of Rama's client and the five forms of path; a read asked once; the pushes and their sharing; the steps of a live read; the five calls `open`, `read-once`, `read-live`, `leave`, `open-count` |
| `read_path/query.clj` | The form of a query as shapes, words and rules; the check; the numbers |
| `read_path/layers.clj` | The order of the layers, a layer's standing for an asker, what a read says of a layer that it left out, what a layer's own rows mean for a query |
| `read_path/sources.clj` | A source as data; what each word reads in one layer; `reading`; the calls of one round; the size rules of a run; which keys and places a change names; the three guards for letters that Rama does not order as the table does |
| `read_path/answer.clj` | A place-answer; the walk of a place; the whole answer; `told-by?`; `after-change` and `after-arrival` |
| `read_path/words.clj` | The seven words, why a live read ends, why a layer gave nothing |
| `read_path/README.md` | The read path's own page, with the four sections that the contract asks for |
| `test-store/softland/store/read_path_pure_proof.clj` | The pure proof (ruling 80), a file and a command of its own |
| `test-store/softland/store/read_path_proof.clj` | The proof against the module |
| `test-store/softland/store/read_measures.clj` | The measures |

Existing files, each edited after the paper of `base-for-a-reader` was in the folder, each read
again right before the edit. The working tree also holds that build's changes to some of these
files, not committed; `git diff` shows both, so no count of lines is given here.

| File | What this part did to it |
|---|---|
| `record/rows.clj` | Six functions more, in two new sections before the locks: `entity-run`, `own-rows-run`, `before?`, `run-of-ends`, `same-row?`, `holds-this-value?`. Three places of the namespace docstring: two lines of "Inputs and results", and "Callers" |
| `door.clj` | The namespace docstring, two places: the module has two clients in the runtime; the bullet on W3 and W4 as open is gone from the list of open rows, and a paragraph says where a read's steps run. No code |
| `door/README.md` | Item 8 of "How each of the door's builder's rules is met" |
| `module.clj` | The head comment gains this plan. "What a client of the module opens, by name" gains the four tables of facts. No code |
| `module/door_reads.clj` | The head comment gains this plan. The last paragraph of the namespace docstring. No code |
| `record/README.md` | The cell of `rows.clj` says what it now gives a reader |
| `softland/store/README.md` | The title and first paragraph; a paragraph on the read path; two rows in "The parts"; the line on which files are pure; a paragraph in "How the parts relate"; "Who reads a lock"; "A direct read of the tables"; the end of "Closing"; the "Where it shows" side of entries 1, 2, 6, 7 and 8 and rows for 3 and 4; a pointer to the read path's words; "Folders and namespaces" |
| `src-store/README.md` | The first paragraph; one row in the table of questions; the two aliases. The decisions sections are not touched |
| `src-store/softland/README.md`, `test-store/README.md`, `test-store/softland/README.md` | Each names the read path, its two proofs and its measures |
| `test-store/softland/store/README.md` | Three rows in the table of files; "The module's two clients, and three proofs" in the place of the door as the one client; a pointer from the three targets to the measures; the sections "The read path's pure proof", "The read path's proof" and "The measures" |

Ruling 35: entries 3 and 4 were new rows of the store's page. Their "Decision" cells hold the
contract's own table wording for those entries, and no new wording of Sid's decisions.

## 2. The template's checks

The template is written for a module. The read path adds no depot, no PState, no topology and no
query. Each check is given with whether it fits, and what was traced.

| Check | Does it fit | Result |
|---|---|---|
| Redundant conditionals, consecutive keypath, select-compute-transform, unnecessary `nil->val` | No dataflow is written in this part | Not applicable. Traced: `read_path.clj` holds no `<<sources`, no `<<if`, no `local-select>` |
| `:allow-yield?` | It is for reads inside a topology. A foreign read yields by itself (the skill's `foreign-client.md`, read, not run) | Not applicable. The longest walk that the read path asks of a task is bounded by the build's numbers: 257 rows, then 20,001, then a run of at most 20,000 (`path-of`, lines 352 to 393) |
| Non-subindexed collections without size limits | No collection is written | Not applicable |
| Stream topology idempotency, partial failure | No topology | Not applicable. The read path writes nothing: traced, and run as a search of the source (group 20 of the proof: `read_path.clj` holds no `foreign-append!` and no `foreign-depot`) |
| Single depot append per client operation | The read path appends to no depot | Holds, by the same search |
| Application-state caches survive restart | `held` is the memory of one process | Holds as the plan says: nothing rebuilds it, each caller asks again. It holds no value of the tables, so nothing of it can be stale. Not run: a restart of the runtime |
| No reimplementation of built-in operations | The read path uses Rama's own navigators and diff processor | Holds. Traced: `path-of` builds only `keypath`, `sorted-map-range`, `sorted-map-range-from`, `multi-path`, `view`, `LAST`, `FIRST`; `keys-of-diff` uses Rama's processor interfaces |
| Plan conformance | Yes | Section 3; the seven places where it could not be met are section 4 |

Three rules of the skill that are not in the template, held against the code:

- *Never trade I/O for simpler code.* A read asked once sends every missing source of one task in
  one call (`select-now`, line 471; run: a place through three layers is 3 calls in one round, group
  11). The sizes of several runs are asked at once, and then the runs (`run-now`, line 500). A check
  of ruling 12 is one row of a call of up to 2,000 paths.
- *Never trade fault tolerance for simpler code.* Every call of Rama's client goes through `attempt`
  (line 319), every wait has the one bound `:wait-ms`, an opening that throws is caught where it is
  made (`open-push`, line 536), and a push that is given after its bound is closed. Run: group 18.
- *Design for production.* What only a real cluster would show is in section 10.

## 3. Against the plan, function by function

| Plan | Built as | Notes |
|---|---|---|
| 1 to 5b, six functions in `record/rows.clj` | `rows.clj` lines 473 to 599 | As planned. `before?`'s docstring says what was seen of Rama's order beyond what the plan knew (section 4, point 5) |
| 6, 7 `words.clj` | `words`, `carried`, `because`, `left-out-words`, `word` | As planned |
| 8 numbers | `max-layers-of-one-read`, `default-numbers`, `numbers` | As planned. The rounds' number, 8, is `max-rounds` in `read_path.clj` |
| 9 `words`, `shapes`, `rules`, `reasons` | As planned, with one rule and one word more | Section 4, point 5: `:run-ends-are-ordered`, `:run-end-not-ordered`. So eighteen words and eight rules, where the plan has seventeen and seven |
| 10, 11 `rule-checks`, `check` | As planned | The rule of the count of layers is examined before the shape (`rules-before-the-shape`), since the engine walks a whole list: the plan's own sentence, "the count of `:layers` is taken with a bound before any layer of it is read". `:not-signed-in` that is not `true` gives `:no-one-asker`; `:stores-own-facts` that is no true or false gives `:not-a-query` with the part |
| 12 to 17 `layers.clj` | `own-row`, `made?`, `who-may-see`, `builds-on`, `marked-since`, `standing`, `in-order`, `left-out`, `meaning` | As planned. `in-order` gives nil also while the asker's standing in the start layer is not known, since the list is used only when she may see the layer |
| 18 the sources | `own-rows-source`, `permission-source`, `place-source`, `run-sources`, `by-value-source`, `check-source` | A source also holds `:layer`. It adds nothing to what makes two sources equal: every key of a source begins with the layer's text. It lets the calls of one round find the call of a layer |
| 19 `of-word` | As planned | |
| 20 `before-standing?` | As planned, and false for a run that has no first read | Section 4, point 5 |
| 21 `nearer-checks` | As planned, the tight rule of the check's fault F7 | It takes `by-layer` too, for each layer's order |
| 22 `reading` | As planned; it gives `:by-layer`, `:list-known?` and `:digest` beside the plan's five | It takes, for a live read, the set of the sources that the read holds. The plan words the rule as "a source that `known` already holds stays wanted"; the check's model, which the pure proof plays, keeps a source that the read holds, given or not. Built as the model |
| 23 `calls-for-once` | As planned | |
| 24 `run-is-whole?`, `over-the-limit?` | As planned | |
| 25 `named` | It always gives the set of the keys whose row differs, never `:all` | Section 4, point 1 |
| 26 to 30 `answer.clj` | As planned | `of-word` holds, for each word, the walk, the places and whether a layer holds a row |
| 30b `after-change`, `after-arrival` | As planned, without the case "a whole value came anew" | Section 4, point 1 |
| 31 to 35 | `ReadPath`, `what-threw`, `attempt`, `within`, `path-of`, `task-routing-ids`, `select-now`, `run-now`, `open`, `read-once` | As planned. `gather` and `select-async` are two small functions under them |
| 36 `keys-of-diff`, the kind of a diff | `keys-of-diff`, `diff-kind` | As planned |
| 37 `join!`, `leave-sources!`, `open-push` | As planned, with the check's fault F11 | `close-when-given!` is the leaver forcing the delay |
| 38 `known-of` | As planned | `pushed-values` is its counterpart for a read asked once |
| 39 `tell!`, `end-read!` | As planned, and `end-read!` takes the read's turn first | Section 6, T1 |
| 40 `on-callback`, `on-arrival` | `on-callback`, `on-change`, `on-arrival`, with `settle!`, `when-given!`, `after-step!` | As planned. `settle!` repeats "what is wanted, join, leave, `known` again" while it changes something, since a source that a step joins can be open already, for another read |
| 41 `read-live`, `leave`, `open-count` | As planned, with ruling 78 | Section 4, point 6 |

Functions that the plan does not list, each small and with one use: in `sources.clj`, `moves`,
`needs-task-routing-ids?`, `rights-kind`, `check?`, `places-of`, `outside-the-agreed-range?`,
`first-read-holds?`, `keys-of-run`, `row-in`; in `answer.clj`, `meanings`. The first five and the
last say, as pure functions, what the plan's step 5 says in prose, so that the pure proof plays them.

## 4. Where the plan could not be met, and what was done

1. **A value that Rama sends anew.**
   *The plan* ("Live", the table "Rama ends a push, sends a value anew…"): "Sends a value anew | a
   `ResyncDiff` after the first | if every row is the same, nothing. Else each read on it gets the
   whole answer anew."
   *Found, run on the store's module:* every change of a push on the facts table comes as a
   `ResyncDiff`, with the whole value and every row a new object. A push on the facts by value gets
   a `KeyDiff` (`runs/raw-rp-probe-diffs.txt`). With the plan's rule every change of a fact gave the
   asker a whole answer: in the first run of the proof, each word but "what points at a thing" gave
   `{:anew}` for an act inside it, an act in a farther layer under a nearer fact gave the asker a
   call, and a list written in a layer that she may not see gave her a call
   (`runs/raw-rp-proof-1.txt`).
   *Built:* a value sent anew is handled as a change of the keys whose rows differ between the value
   before and the value after (`on-callback`, lines 976 to 1018; `sources/named`). No row differs:
   nothing. `after-change` has no case for it.
   *Why:* it is the rule that the check's model and the pure proof play: a callback is the value
   before and the value after, and the keys that differ. The asker still ends at the newest state.
   The plan's rule was never played.
2. **The first read of a run, and letters that Rama does not order as the table does.**
   *The plan* ("R3, and a very large run"; function 3): the first read is form 3 for every run, and
   the table's order is by code point.
   *Found, on the store's table:* the table goes through its keys by code point. For a key that
   holds a letter from U+E000 on, or one outside the first plane, one thing was run by itself: a
   lookup in the map that Rama's client makes of a run missed a key that the map holds
   (`runs/raw-rp-probe-order.txt`, "a lookup of each of its own keys: [true true true false]"). Two
   were seen and are not explained. With the first read, a run between two such entities gave no
   place where the table holds two (`runs/raw-rp-proof-2.txt`); without the first read it gave both,
   and a third run between two such ends still gave two places of four
   (`runs/raw-rp-proof-3.txt`). A probe that asked Rama for the same ends by hand got every key from
   both forms of path, and for another pair of ends got a key that the order by code point puts
   after the range's end (`runs/raw-rp-probe-order.txt`). The proof's runs and the probe differ in
   more than one thing, so which step of Rama's is at fault is not shown.
   *Built:* a run whose end holds such a letter has no first read: its size, then the run whole
   (`sources/first-read-holds?`). A lookup that misses such a key goes through the run
   (`sources/row-in`). Each key of such a run is held to the run's two ends before it is given
   (`sources/keys-of-run`). And a run between two ends that a caller names is turned away, with
   `:run-end-not-ordered`, when an end holds such a letter; an entity whose id holds one is read as
   `[:entity e]`, whose run cannot lose a place of its own.
   *Why:* a read that gives fewer places than the table holds has said "no fact" where there is one,
   and the cause is not known, so the read path rests on none of the three. A word is the tree's
   way. This is one rule and one word more than the plan lists.
3. **How many rounds a live read takes.**
   *The plan* ("The pure core"): "what points at a thing … kept live, 5", and the loop "ends in at
   most 5 rounds".
   *Found, played:* from a start layer that is "members" it is 6. The list of a start layer is used
   only when she may see the layer; under "members" that needs her permission, one round after the
   layer's own rows. Through a given list it is 4.
   *Built:* the rules of the plan's table as they stand. The stop at 8 rounds holds, with two rounds
   to spare where the plan counted three.
4. **How many pushes Rama opens.**
   *The plan* ("The cost"): the worked case "holds 3 + 2 + 3 + 10,020 pushes"; `:max-checks-of-one-read`
   is 40,000.
   *Found, run:* 1,000 pushes opened on one table on one task, the next was refused, and a closed
   push made room for one more (`runs/raw-rp-probe-limit.txt`). Whether two tables on one task share
   the count was not run. Rama names a setting of its own for it, `pstate.proxy.limit`.
   *Built:* as planned. The read path sets none of Rama's settings and counts no push against the
   bound. An opening that Rama refuses ends in `:reach-the-store`; the read gives that word and
   leaves every push that it joined (run: M3, "pushes held: 0"). The docstrings and the read path's
   page say the bound.
   *Not built, and why:* a guard of the read path's own. It would be a number of this build standing
   in for a setting of Rama's that the read path cannot read, and whether the bound is raised or
   fewer pushes are held is the thing that is not decided (ruling 79).
5. **Ruling 78.** The plan says that the function can be called before its caller holds `:now`, and
   settles it by a line of the docstring. Built in its place: `read-live` calls the function once
   with `{:anew answer}` on the caller's thread, in the read's turn, before it returns
   (`read-live*`, lines 1195 to 1270; the call is at line 1263). Run: of 279 live reads that the
   proof opened with its noting function, each first call was `{:anew answer}`, equal to `:now`, on the caller's thread (group 24).
6. **Ruling 80.** The pure checks are a proof file of their own, `read_path_pure_proof.clj`, with a
   command of its own on the alias that holds Clojure alone. Nothing was put in `pure_proof.clj`.
7. **Two shapes of the measures.**
   *M2, the shape of runs:* the plan has each act go to another place. Built: the acts go round 256
   places. Each run holds every written place, so with places that only grow, 1,000 runs would come
   to hold 3,500 rows each in this one process. The line of the measure says so.
   *M4:* a third shape was added, a place of the facts by value in a streaming layer, so that a push
   on rows with bytes and a push on rows without are measured under one load. An act is known in a
   change by its act-id, not by its value.

## 5. What a read asked once does, traced

A place through `[:alice-notes :team :base]`, asked once, by `:alice`. `checked` (line 1052): the
thread is not marked; `query/check` gives the one form; no routing ids are needed. `read-once*`
(line 1077), round 0: `reading` on an empty `known` wants nine sources and all are missing.
`pushed-values` gives none. `calls-for-once` gives three calls: the two streaming layers by their
layer, and the base by the entity, in which the base's own rows and her permission ride. `select-now`
sends the three, each one `multi-path`, and waits for all with the one bound. Round 1: nothing is
missing; `answer/whole` walks the place from the nearest layer. Run: 3 calls in one round (group
11).

A run of 12 places with a first read of 4 and at most 10 places. Round 0: the first read gives 5
rows, so the source goes into `first-reads`. Round 1: her standing is `:yes`, so `run-now` asks the
size; the walk is full and its last key is before the run's end; the word
`:serve-a-read-that-large`. Two calls. For an asker who may not see the layer, `to-read-whole` is
empty, the size is not asked, and the layer is left out. Run: group 12, "2 calls", "1 call and no
word".

## 6. A live read, traced

**T1, the fault that this step found.** `on-callback` ends each read on a push when Rama ends the
push or a run grows too large. It called `end-read!` for every reader, also one that `read-live` was
still opening. `end-read!` took the read out and called its function with `{:ended}`. So a function
could be given `{:ended}` as its only call, with no answer before it, against ruling 78; and
`read-live` then spun through its rounds and gave a word.
*Fixed:* `end-read!` takes the read's turn first and calls the function only when the read was
opened (lines 793 to 815). Whether a read is opened changes only in its turn, so an opening and an
ending of one read no longer cross. `read-live*` gives its word at once when it finds the read gone,
and marks a read opened only if `:reads` still holds it.
*Run:* 40 times, Rama's ending diff was handed to the callback of a read's first push on another
thread while the read opened. 4 times the read was not opened yet: `read-live` gave a word and the
function was not called. 36 times it was opened: the function was given the answer and then
`{:ended}`. With `end-read!` put back as it was, by a scratch file that changed no source, the check
failed in the 8 turns that met a read that was not opened (`runs/raw-rp-mutation-ending.txt`).

**T2 to T10, traced, no fault found.** A function that throws at its first call: the read is ended
and told so, and `read-live` still gives the answer (run, group 18). A read that `leave` and
`end-read!` both reach: one swap takes it out, the other finds none, and `{:ended}` is not told to a
read that left. A reader that joins a push after `on-callback` read its readers: her first answer,
or her step, is made from the push's value, which holds the change. One mark and one callback for
one push. A step attaches one handler for each source that it joined; a chain of arrivals is bounded
by the five ranks. `settle!` at its bound leaves the mark owed, so no single place is told from a
state that is not settled. A run whose first read is not whole, in a layer that she may not see, is
never read further and never looked at. A row that every task holds rides a call of its own layer
and table. Every path that reaches Rama's client comes from `path-of`.

**The turn, and no deadlock.** A thread holds one read's turn at a time: `on-change`, `on-arrival`
and `end-read!` each take the turn of the one read that they work for, and `leave` takes none. An
arrival that is handled inside a step is the same read and the same thread. So no two threads wait
for each other's turns. Run: 200 acts from 4 threads, no two calls for one read overlapped; 50 times
two reads left each other inside their functions and nothing stood still.

## 7. B1 to B5 and R1 to R5 against the contract

| | The contract asks | Built | Seen |
|---|---|---|---|
| B1 | A query as data: a place, an entity, a run, what points at a thing; once or live | `query.clj`; `read-once`, `read-live` | Run, each word for each gate, group 3 and 14 |
| B2 | Who asks, and may she see | `layers/standing` | Run, groups 8 and 9 |
| B3 | The layer's list or the view's own; a layer that she may not see is left out and the read says so; the nearest layer that holds a fact wins, also with "nothing here" | `layers/in-order`, `answer/whole` | Run, groups 7 and 8 |
| B4 | A live read on each fact that shapes it; it ends what is no longer rightful | `on-change`, `on-arrival`, `layers/meaning` | Run, group 15. Played in every order |
| B5 | Those who look at the same thing share one push; it ends when the last one leaves | `join!`, `leave-sources!` | Run, group 16: 200 openings and 200 closes for 50 pairs |
| R1 | The read path is the one reader of facts | One file names the three reading calls | Run as a search, group 20 |
| R2 | Nothing shown before it is in the tables; no store of our own | No optimistic value; `held` holds no value | Run, group 17, as far as a run reaches; the fields of `held`, group 16 |
| R3 | Never a push on a whole table | `path-of` | Run, group 12. Traced, T10 |
| R4 | A part that was not asked for is not sent | `answer/place-answer` | Run, group 5. Rama sends the runtime the whole row (ruling 15) |
| R5 | A screen is shown the newest state | The steps of a live read | Run, groups 14 and 23. The newest state comes about a second late for a place, an entity or a run (section 11) |

## 8. What is not decided, and where each kept answer is code

Each is built to the kept or the working answer at one function, whose docstring says "Not
decided". None is decided here.

| The thing | Built as | The one function |
|---|---|---|
| A read that left a layer out says so (ruling 8) | The layer, and `:may-not-see` or `:not-made` | `layers/left-out` |
| How far a read walks from a layer (ruling 9) | The one list, no further | `layers/in-order` |
| `:from-layer` when she may not see the start layer (ruling 65) | That one layer is left out; its list is not used | `layers/in-order` |
| The store's own facts in a tool's read (ruling 11) | Left out unless the query asks | `answer/counts?` |
| "What points at a thing" through layers (rulings 12, 64) | The nearest layer that holds a row wins; given when the winner's value is the thing | `sources/nearer-checks` |
| One moment for all the layers (ruling 13) | No lining up | None |
| What a layer starts with (ruling 6) | `first-act/starts-with` | That function; `layers/who-may-see` is its one caller here |
| One permission for each holder; "see" stays "see"; who may write a setting, a list, a mark | No code here | — |
| Who asks is what the caller says | `:asker` is taken as given | `query/check` |

## 9. Every provisional choice of this part, as built

All are the builder's. The form of a query and its words. Where the code lives. Once or live by the
call (ruling 60). A source as what is shared, with `:layer` in it. A layer's own rows as one push. A read asked once
reads a layer's fact with its rights (ruling 61; the one predicate is `sources/before-standing?`,
whose docstring says that it is the builder's and what the other answer is).
Her permission followed only under "members". The three steps of a run, and no first read for a run
whose end Rama does not order. The five forms of path. The numbers: 256, 20,000, 40,000, 2,000,
10 s, 65 layers, 8 rounds. `Helpers/genHashingIndexKeys` to name a task (ruling 62). The whole
answer anew when what shapes a read changes (ruling 69). The asker's function on Rama's thread, in
the read's turn (ruling 67), and its first call by `read-live` (ruling 78). A read from inside a
function turned away (ruling 77). A place under a store key, asked without the store's own facts,
turned away (ruling 68). A value sent anew handled as the rows that differ. A read asked once given
the value of an open push. A run between two ends that Rama does not order turned away. The names
`standing` and "the read's turn".

## 10. What was run on Rama, what was played, what was derived, what no check reaches

*Run on Rama 1.6.0, in-process, on the store's own module:* the six proofs
(`runs/after-read-path-*.txt`); the measures (`runs/measures-read-path.txt`); and five probes of
this build, each a scratch file with its output beside it: the diff that each table gives a push
(`raw-rp-probe-diffs`); how often a push is called back under load (`raw-rp-probe-pace`); how many
pushes Rama opens (`raw-rp-probe-limit`); the order of a range's ends (`raw-rp-probe-order`). *Run
on a probe's own module:* that the bytes in a row are what makes Rama send a value anew
(`raw-rp-probe-bytes`).

*Played, with no Rama:* every order of the steps of a live read, on the functions that
`read_path.clj` calls: thirteen scenarios, 672 small lists and 620 lists, with the asker's screen
held against a read asked once at every end. With a step counting a check that it has just left,
the scenarios 1, 2, 8 and 11 and 118 of the 672 lists end wrong, which are the numbers of the
check's own model.

*Derived:* that the tests which let a step say nothing are each safe (the plan's "Live"); that no
two threads wait for each other's turns (section 6).

*What no check reaches:* a cluster of several machines; a network; a move of a task's leader; a
killed process; a restart of the runtime. Rama ending a push by itself, and Rama leaving a reader
out of a change: the proof hands the read path Rama's own diff. The order in which two pushes call
back. A number of tasks that changes. Many askers at once. Why Rama sends a value anew for rows that
hold bytes, and whether a setting changes it. Whether two tables on one task share the bound of
pushes. `Helpers/genHashingIndexKeys` outside the in-process cluster. What a range of Rama's does
between two ends that hold a letter from U+E000 on. A tool, a view, an inhabitant.

## 11. The measures, in short

One run, in-process, one machine, every row in memory, no network. Nothing is held to a number. The
run was printed by the code as it stood before three later small changes, none of which changes
what a measure measures; the head of `runs/measures-read-path.txt` names them.

| | What | Middle | Slowest 1 in 100 |
|---|---|---|---|
| M1 | The streaming gate, 100 acts a second, from the call of `send-act` to its return | 5.4 ms, and 4.8 ms with the key marked | 29.6 ms, and 28.3 ms |
| M1 | The base, the same acts, from the call to "accepted" | 1,203 ms | 1,415 ms |
| M2 | The streaming gate with 100 live reads on places that are not written | 4.4 ms | 34.4 ms |
| M2 | With 1,000 asked for: 998 opened, 2 met Rama's bound | 4.1 ms | 27.8 ms |
| M2 | With 998 live reads on runs that hold the written places | 4.7 ms; 10.5 ms from the due moment | 38.4 ms; 239.5 ms from the due moment |
| M3 | A canvas of 5,000 asked once, as a run, through one layer and three | 39.5 and 48.1 ms | |
| M3 | Asked once as what points at it, through one layer and three | 47.5 and 82.9 ms; 14 calls through three | |
| M3 | Kept live as what points at it, through one layer | 36.1 ms, 3 pushes | |
| M3 | Kept live as what points at it, through three layers | A word, `:reach-the-store`, after 9.3 s | |
| M4 | How late a push is, a place of the facts table, streaming, from the ack | 1,000 ms; 26 of 500 acts told by a call of their own | 2,001 ms |
| M4 | The same in the base, from the append-ack | 1,609 ms; 25 of 500 | 2,713 ms |
| M4 | A place of the facts by value, streaming, from the ack | 0.15 ms; 500 of 500 | 0.23 ms |

The marked and the not marked key of M1 are one run each, and their difference is inside what one
run can tell. M2's times from the due moment hold the maker's own lateness: the maker and the 998
askers' functions run in one process.

## 12. Lint, and what was not verified

`clj-kondo` 2026.05.25, run from the local Maven store with no cache, on `read_path.clj`,
`read_path/`, the two proofs, the measures and `record/rows.clj`: `errors: 0, warnings: 0`
(`runs/raw-rp-lint.txt`). It ran under the repository's own configuration, `.clj-kondo/config.edn`,
which has three linters off: a redundant expression, a var with the same name but for case, and an
unused public var. The run changed nothing in that folder. Its first run gave 12 warnings. Three were the read's turn taken through a
local; it is now named where it is taken. Two of the unused names in the proof were checks half
written, and each now holds the fact of its act. The read path's namespaces load with reflection
warnings on and give none.

Not verified: each "not run" of section 10. That the edits of the maps left every link whole: each
link was written by hand and not followed by a tool.

## 13. Points for the build session

1. A push on the facts table is a second late and resends its whole value, because a fact's row
   holds a run of bytes (section 11, M4; the verdict's point 1). The row's form is the part
   `what-the-gates-keep`'s. A row for the queue, and a question for Rama's team beside ruling 62's.
2. Rama as it ships opens 1,000 pushes on one table on one task. The worked case of rulings 12 and
   64 needs about 10,000 and does not open; `:max-checks-of-one-read` is 40,000. Ruling 79's cheaper
   way holds 2 pushes where this holds 10,020.
3. A read asked once is given the value of an open push, which can be behind the tables. Straight
   after the acks of 200 acts it gave 186 to 192 of the 200 places in the runs of this session; the
   figure differs from run to run. The other answer is one function, `pushed-values`, giving
   nothing.
4. One rule and one word more than the plan: a run between two ends that hold a letter from U+E000
   on, or outside the first plane, is turned away.
5. A live read from a "members" start layer opens in 6 rounds, where the plan derived 5.
6. `standing` is also the gates' word (the check's point 8); the read path's page says so. Sid's to
   change.
7. `runs/raw-rp-measures-full.txt` is 1.0 GB, nearly all of it Rama's log of 98,794 refused
   openings. This session did not remove it: Sid's rule is to ask before deleting, and nobody could
   be asked. Stage by name.
8. The store's page, the tree's map and the proofs' map were edited in a tree that holds the
   uncommitted changes of `base-for-a-reader`; the two builds' edits of those three files are in one
   diff.
