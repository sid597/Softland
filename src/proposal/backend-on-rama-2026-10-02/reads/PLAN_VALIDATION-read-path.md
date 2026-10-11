# Plan Validation: the read path

**What this paper is.** The check of `PLAN-read-path.md`, the plan of the part `read-path` of the read
build: the contract's B1 to B5, its rules R1 to R5, and the measures of its section C. The contract is
`../BUILD-reads.md`. "Entry N" is entry N of "Decisions of 11 and 12 October 2026" in
`src-store/README.md`. "Ruling N" is point N of "Rulings" in `PROGRESS.md`. "The implied spec" is
`IMPLICIT_SPEC.md`. "The hand-off" is the section of that name in `PLAN-what-the-gates-keep.md`, as its
check left it. A file name without a folder is under `src-store/softland/store/`. "T" and "U" are the
plan's own two probes. "C1" to "C9" and "the models" are this check's runs (section 4).
**The stage.** Phase 2 of the Rama skill (`phase-2-plan-validate.md`), by its template
(`artifact-plan-validation.md`). The template is written for a module. The read path is a client of
the module and adds no depot, table, topology or query; where a heading of the template does not fit,
the heading says so (section 17).
**Who wrote it.** One fresh session (Opus 5.5), in the night of 11 to 12 October 2026. It did not write
the plan. It wrote this paper and the folder `plan-check-probes-read-path/`, and it changed the plan
where sections 2 and 3 say so. It changed nothing under `src-store/` or `test-store/`, and it
committed nothing.

**Verdict: minor-fail.** Fifteen faults, each fixed in the plan (section 3), after ruling 64 was
applied to it (section 2). One of them is in the plan's own least certain part: the argument that no
"before" value need be kept did not hold as the plan worded the steps of a callback, for the word
"what points at a thing". It holds once three rules are made exact, and a read then holds one mark
more, true or false, and no value. The design stands: the same sources, the same pushes, the same five
forms of path, the same calls. No fault needs the plan made again.

**Blocked.** Nothing.

**A ruling that cannot hold.** None was found. Two rulings hold and cost something that the plan did
not say; both are points for the build session (section 1, points 1 and 2). Ruling 67: an asker's
function that stays holds back the reads of other askers, and this was run. Ruling 26: the read now
holds one mark between two callbacks; it is no put-together value, and the build session may read the
ruling more strictly.

**Does the argument that no "before" value need be kept hold?** For a place, an entity and a run: yes,
as the plan worded it. For what points at a thing: not as worded, and yes as fixed. Section 5 has the
traces. The sequence that broke it, in the plan's own crossing case: the farther order loses a place,
and a nearer layer gets a row at that place. Both pushes take their new values. The order's callback
is handled first: a nearer layer holds a row "now", so nothing is told; and the same step leaves the
check, since no order holds the place any more. The check's own callback then reaches no read. The
asker keeps a place that the tables no longer give. A fix exists that keeps nothing between two
changes: the step counts only sources that it still holds, so it finds no nearer row and tells the
place. The build does not have to stop on its page for R2.

**What was read.** The skill: `SKILL.md`, `phases.md`, `phase-2-plan-validate.md`,
`artifact-plan-validation.md`, `foreign-client.md`, each whole. The contract, whole.
`src-store/CLAUDE.md`, whole, and of `src-store/README.md` the section of 11 and 12 October. Of the
sheet `decisions/groups/g-reads.txt`: its first page, W1, W2, W3, W11 and W12. `PROGRESS.md`, whole.
This part's entry of `DECOMPOSITION.json`. Of the implied spec: lines 1 to 217; "What holds for every
operation of half B" and B0 to B5; the matrix 1 to 5; P8, P9, P11 to P15, P23 to P31; "What the proof
of this build must be able to show"; "Latency and growth". The plan, every line, and after the fixes
its sections "Live", "The calls" and the proof again. The plan's two probes: `tasks.clj` to its case
T5; the logs `tasks-run-2.log` and `runs-run-2.log` whole, and of `tasks-run-1.log` its last lines;
`runs.clj` was not read. The hand-off, and "PState Design" of that plan. "What this part
hands on" of `PLAN-base-for-a-reader.md`, and of its check point 9 and fault F9. Both probes' results:
`push-probe/RESULTS.md` whole; of `batch-probe/RESULTS.md` its B7 and "Seen on the way". Of
`PLAN_VALIDATION-what-the-gates-keep.md`: points 5 and 6 and section 3. Of the door: `door.clj`, the
namespace docstring, `what-threw`, `attempt`, `append!`, `read-answer` and `open`; `door/words.clj`
to its first words. Of `act.clj`: `readable-keyword?` and `value?`. Of Rama's page on a push,
`reference/rama/docs/15-pstates.md`: lines 550 to 556, 704 to 734 and 1228 to 1232, read and not run,
of an extract that names Rama 1.5.0. Of the skill's `paths.md`: the lines on `sorted-map-range` and
`sorted-map-range-from`. Not read: `door/README.md`, `door_proof.clj`, the skill's `testing.md`, and
Rama's page `28-clj-defining-modules.md`; where a line below rests on one of them it says "by the
plan's citation". The code under
`src-store/softland/store/record/` and `gate/` was not read: it is being changed tonight, and the plan
stands on the hand-off (ruling 38).
**What was run.** One probe on Rama 1.6.0, in-process, three times; two models with no Rama, each
once. Section 4. No proof of the store was run. Nothing was run on the store's own module.
**What came while this check ran.** The build of `what-the-gates-keep` was committed (`371c6d2f`,
rulings 71 to 73). The check did not move to the code: the plan stands on the hand-off. One search
was made of that commit, of definitions only: each name that the plan takes from the hand-off is
defined there (`rows/place-key`, `place-of-key`, `read-value`, `who-of`, `permission-of`,
`marked-keys-of`, `value-digest`, `value-digest-of`, `by-value-run`, `by-value-entity`;
`first-act/starts-with` and `depot-at-making`; `declaration/store-key?`, `usable-id?`,
`store-namespace`, `max-builds-on`; `act/allows`, `act/as-shape`), and the four tables that `open`
fetches are declared under the names that the plan gives. No ruling of 71 to 73 touches this plan.

## 1. Points for the build session

Each is a thing that this check cannot settle, or settled and wants seen.

1. **Ruling 67 holds, and it costs more than the plan said (run: C4).** While one callback stayed for
   2.5 s, three of the seven other pushes waited, those on the same thread of Rama's client. Selects
   and openings of pushes from other threads waited as well: six of six selects that block, in each of
   two runs; four of six and six of six selects that do not block; four of six and three of six
   openings. So one asker's function that does not return soon holds back `read-once` and `read-live`
   of every other asker, up to their own bound. A second change for the same read, on the other
   thread, waits for the read's lock and holds that thread too. The plan's docstring line said only
   "every other change of that push's thread waits". It now says what was run (fault F5). The other
   answer is the plan's open point 8 (b), an executor of the read path's own with a queue, which is a
   thing kept between two changes. The check kept the ruling. A line for the receipt.
2. **A read holds one mark more: "the whole answer is owed" (fault F2).** Ruling 26: "It keeps no
   put-together value between two changes." The mark is no value of the tables. It is true or false,
   set by the step that finds that what shapes the read changed, and taken off by the step that gives
   `{:anew}`. Without it, a layer that joins over a farther order never gives its own places (model,
   scenarios 9 and 13). The other answer keeps nothing at all: `{:anew}` in every step in which the
   last missing source is given. Under ruling 12 that is a whole answer for each new block of a canvas
   that is read through layers, where the plan counts "one change to the asker".
3. **A seventh word, `:read-inside-an-askers-function`, and the guard that gives it (fault F5).** The
   check added both. With the guard struck and the docstring alone, a `read-once` inside a function
   waits up to `:wait-ms` with a thread of Rama's client and the read's lock held, whenever Rama's
   client gives the answer on that thread (run: C5, three of six).
4. **`:now` and the first change (fault F4).** The function can be called, on Rama's thread, before
   `read-live` has handed `:now` to its caller. The check fixed the docstring: the caller puts `:now`
   in place first, and its function may wait for that. The other answer: `read-live` hands `:now` to
   the function itself, as its first call, on the caller's thread and under the read's lock. The order
   is then whole, and ruling 67's "on a thread of Rama's client" gets one exception.
5. **A cheaper way for ruling 12's checks exists, and rulings 12 and 64 fix the count** ("one for each
   such place and nearer layer"). The template forbids to put a cheaper design aside without building
   it, so it is built here, on paper. For each nearer layer: size the layer with the plan's own three
   steps on `[:run :first :last]`. If it holds no more than `:max-places-of-one-run`, read it whole
   once, or keep one push on it, and take every check of that layer from it. Else the checks as
   planned. The worked case, a canvas of 5,000 through three layers, with 300 places in `:alice-notes`
   and 2,000 in `:team`: 2 seeks and 2,300 steps, about 1 ms of seeks and 2 to 23 ms of steps by the
   skill's figures, against 10,020 seeks, 3 to 5 s. Kept live: 2 pushes against 10,020. What it costs:
   a push on a whole layer is sent every write of that layer, under any key, where a check is sent
   only a write at its place; the runtime holds the layer's rows with their values; and a second way
   of answering one question, with a switch by size. The answer is the same in both, ruling 64
   included. A row proposed for the queue beside P12, to weigh with M3's numbers.
6. **The template's test for a cost that grows with the cluster trips on one row, and the contract
   asks for that read.** A run across entities of the base is one seek on each task: 9 seeks at one
   task, 24 at 16, 136 at 128. The contract says of it "a run across entities of the base is not" one
   push, and ruling 29 has the read path put the parts together. With the plan's guessed share of
   1 in 100 the weighted seeks go from 9.0 to 10.27. The module's own answer is the facts by value,
   which is one task at any size. For the receipt, as the plan says.
7. **Ruling 66 counts five functions more in `record/rows.clj`; ruling 64 needs a sixth,**
   `holds-this-value?`: the row holds bytes whose digest is the digest of the thing. It is the gate's
   own test for a value's order, so a place that is given from a nearer layer's row is given exactly
   when a mark would have put it in that layer's order.
8. **Two words (fault F12).** The plan and ruling 67 say "the read's lock". In this tree a lock is an
   encryption key, and the read path's own docstrings say that it reads none. The plan now has the
   code and its explanations say "the read's turn". And `standing`: the plan uses it for whether an
   asker may see a layer. It is the store's word for a permission that stands
   (`gate/decision.clj:137`, `standing`), and a search of the rig's source finds one `defn standing`.
   The plan's read list holds no file of the rig. The check did not change the word: as with ruling
   54, whether it stays is the build session's, and it is Sid's to change.
9. **What ruling 64 leaves, for the row of the queue (P12).** A place is found only through an order.
   So a place at which a nearer layer says the thing, and which no order of a layer that she may see
   holds, is not in the answer; and such a place leaves the answer when the farther fact moves away,
   though the nearer fact still says the thing. A read asked once and a live read agree on it, in
   every order of the callbacks (section 5).
10. **Rama's client had two threads for callbacks and for answers, in-process.** How many the client
    of a real cluster has, and whether it is a setting, was not found. Point 1 scales with it. A
    question for Rama's team, beside ruling 62's.
11. **What the opening and the argument rest on, and where it was run.** A push holds its new value
    when its callback runs (C1: 12 of 12 callbacks, and 6 of 6 when forty writes came as three
    changes). Rama's page does not say it in those words. It says "The CRC is checked after applying
    the diff" and "each subsequent callback receives the new value from the previous callback as its
    old value" (`15-pstates.md`, "Fault handling" and line 552; read, not run), which fits. If the
    build's check 25 shows a change lost between the opening of the pushes and `:now`, this is the
    first thing to look at.
12. **P9 is now a player of every order (fault F13).** It needs the step of a callback as pure
    functions, `answer/after-change` and `answer/after-arrival`, which the check added to the plan's
    list. The two models of this check are a pattern for the player, and no form: no name comes from
    them.
13. **A live read with a run across entities of the base holds the routing ids of the tasks as they
    were at its opening** (fault F15). If the number of tasks changed under it, nothing would notice.
    Not built, and the other sheet's (Z6). For the receipt.
14. **A call of Rama's client that blocks, made inside a callback, did not come back in one run of
    three** (run 1; C9 in runs 2 and 3 came back in a millisecond). The read path makes no such call:
    every call of it is one that does not block, with a bound. Noted for whoever writes a caller.

## 2. The first fix: ruling 64, applied before anything was checked

Ruling 64: "At a place that a farther layer's order gives, the nearest layer that holds a counted row
wins, and the place is given when the winner's value is the thing that was asked for. So a nearer layer
whose own fact at the place points at the same thing no longer hides the place: the read gives it from
the nearer layer's row, which the check of ruling 12 already has in hand."

Line numbers on the left are of the plan at head `efaf6275`; on the right, of the plan as this check
leaves it. Each replacement, old text and new, is in `plan-check-probes-read-path/edits/01_ruling_64.py`
as it was applied; `git diff` of the plan shows the same.

| Where | Before | After |
|---|---|---|
| "What a read gives", 469-484 → 510-536 | "A place is in the answer when the nearest layer that holds any row at that place has it in its order. … a nearer fact that moves the block, or takes it away, hides it." And: "One edge, built as the ruling says and named in open point 5: a nearer layer whose own fact at the place points at the same thing, and was written before that layer's mark, hides the farther place and is not in its own order." | A place is found through an order; at it the nearest layer that holds a counted row wins; "The place is given when the winner's value is the thing that was asked for (ruling 64)": from the order's row, or from the check's row if `rows/holds-this-value?` holds. And what an asker must know: a place can come from a layer that says `:not-marked`, or with a stamp that is not later than `:level-since`; a place is found only through an order |
| The pure core, (4), 573-580 → 632-642 | "…the first such layer that holds any row at the place, by its own order or by its check, and then whether that layer's order has it." | "…the place is then given when that layer's order has it, or when the check's row holds the thing (ruling 64)." |
| `sources/nearer-checks`, 552-556 → 610-615 | "…each layer at a position before j whose standing is `:yes` and whose own order does not hold that place: one source S6." | "…one source S6 in each layer of standing `:yes` that is nearer than the nearest layer whose order holds that place. A layer behind that one cannot win the place, so nothing is read there." (fault F7 too) |
| The source S6, 176 → 185 | "does a nearer layer hold a row at a place…" | "which row, if any, does a nearer layer hold at a place… (ruling 64 reads its value)" |
| Trace 4, 928 | "The answer: each place whose nearest row is in an order" | "…or is a check's row that holds `:canvas-1` (ruling 64)" |
| Trace 11, 982-992 → 1173-1199 | The move gives `{:holds :no-fact}` because "`:team` holding a row that its order does not have" | The move gives `{:holds :no-fact}` because the row's value is another thing. Moving back, in a streaming layer, is two pushes in no order, and both orders are traced. A second part, "Ruling 64, the case that it changes": a nearer layer that does not mark the key says the same thing; the place is given from it; then the farther fact moves away and the place is told as gone |
| The function list, 1253-1268, 1322 → 1473-1494, 1550 | five functions in `record/rows.clj`; `order-place-answer` of an order's row | a sixth, `holds-this-value?` [row value]; `order-place-answer` "takes a row of an order, or a check's row that holds the thing" |
| The pure proof, P6, 1153 | wants a check "exactly where a nearer layer could hide a place" | also a place that two orders hold with a layer between them; `of-places` on a check's row that holds another thing, "nothing here", the asked thing; `holds-this-value?` on a UUID, a string with spaces, a decimal with trailing zeros, bytes that are no value |
| The proof, check 10, 1185 | "Ruling 12 as built: hidden, hidden, shown from the nearer." | "Rulings 12 and 64 as built": also a nearer layer that does not mark the key and writes another thing, then the same thing; a nearer fact written before its layer's mark; then the farther layer moves the place away |
| "Not decided", 1460 | "B3 holds: a nearer row at the place hides it" | "B3 holds: at a place that an order gives, the nearest layer that holds a row wins, and the place is given when the winner's value is the thing" |
| Open point 5, 1487-1496 → 1729-1740 | "Ruling 12 as built: one edge, and its count of pushes. … Recommended: as ruled" | "Settled by ruling 64, against this point's first recommendation", with what stays for the queue |
| "For the receipt", 1530-1538 | "the edge of open point 5 (with P12)" | "with P12, what ruling 64 leaves: a place is found only through an order"; and under "What Sid should know" the two sentences of point 9 above |
| The measure M3, 1238 | "each of two nearer layers holds 50 facts at places of the canvas" | "…25 that say another thing and 25 that say the canvas", so that both sides of ruling 64 are in the number |

What the pure core's function tells at a change, with ruling 64 in it. A check's push calls back, nil
to a row or a row to another: `told-by?` finds no nearer row; `of-places` finds this layer the nearest
that holds a row; the place is told from that row if it holds the thing, else as `{:holds :no-fact}`.
An order loses a place and no order holds it any more: the checks at the place are left and the place
is told as `{:holds :no-fact}`, whatever a check held (fault F1; section 5 says why it must).

## 3. The faults, and the fix of each

Line numbers as in section 2. The exact old and new text of each fix is in
`plan-check-probes-read-path/edits/`, in the files `02_live.py` to `09_played.py`, each a list of
replacements as it was applied; they are a record and do not run a second time. A few small things
were changed by hand after them: four lines wrapped again, one sentence of step 5 on a source that a
step joined, one in the receipt's lines, and the list of the live half at the plan's top. `git diff`
of the plan has all of it: 432 lines in, 172 out.

### F1. A step that left a check could still be silenced by it: the asker kept a place that the tables no longer give

**The plan's lines 633-646 (step 5) and 651-669 (the argument).** "5. *For each read that holds the
push*, under that read's lock, with `known` made of the values that the read's pushes hold at this
moment: `sources/reading` again. A source that is now wanted and not held is joined; one that is held
and no longer wanted is left. … `answer/told-by?` says whether this change is to be told: … and no
nearer layer of standing `:yes` holds a counted row at the place now." And: "One test looks at other
pushes: 'a nearer layer that she may see holds a row at the place now'. It is sound for this reason.
Take any place, and the last callback that the read path handles among those that changed anything
that the final answer at that place rests on: the row of the layer that wins it at the end, that
layer's standing, or the list. … If it is a change of the winning layer's row, no nearer layer that
she may see holds a row there, so it is told".

**What is wrong.** `known` is made first, and the leaving comes after it. So `told-by?` can count a
check that the same step has just left. And the argument has no case for an end at which no layer wins
the place: the farther order lost it. The trace, with the plan's own crossing case (section 5, trace
P1): the order's callback finds the check holding a row, tells nothing, and leaves the check; the
check's callback reaches no read; the asker keeps the place.

**The evidence.** The model, with `told-by?` looking at the values gathered before the leaving
(`logs/crossing.log`, the first block): scenarios 1, 2, 8 and 11 have an order that ends wrong, and
118 of 672 lists of up to three writes on one place in two layers. The shortest: `W W U(C,1,p)
U(O,2) H(O,2)`, both writes, the check's push takes its row, the order's push takes the loss, the
order's callback is handled. "the screen at the end: {'p': (2, 1, 'order')}   the tables give: {}".
With the fix: none, in every order.

**The fix (now 695-725, 766-831).** Step 5 makes `known` again after the joining and the leaving: "A
source that this step left is not in it." `told-by?` counts a nearer layer's row "only in a source
that `reading` wants and `known` holds" (function 30). The argument is written again as it holds:
three tests that let a step say nothing, three things that are told with no test, and why the last
thing told at a place is the answer at the end.

### F2. A whole answer that was owed could be lost when the last source to arrive was a check

**The plan's lines 640-641 and 647-649.** "…when nothing is missing, the function gets `{:anew
(answer/whole …)}`; when something is missing, the arrival of the last missing source gives it." And
step 6: "A check of ruling 12 that arrived gives `{:changed …}` for its place. Any other source that
arrived gives `{:anew …}` when nothing is missing any more."

**What is wrong.** A permission is given in a nearer layer that marks the key. The layer's order is
joined; when it is given, checks become wanted, so something is still missing and no `{:anew}` is
given. The last source to arrive is then a check, which gives `{:changed}` for its own place. The
whole answer is never given: the places of the layer's own order never reach the asker, nor do
`:left-out` and `:orders`.

**The evidence.** The model with the arrival rule "by kind" (`logs/crossing.log`, its last lines):
scenarios 9 and 13 fail. Scenario 9, `W P S(O,1) A(O,1) S(C,1,p) A(C,1,p)`: "the screen at the end:
{'p': (2, 2, 'order')}   the tables give: {'q': (1, 1, 'order'), 'p': (2, 2, 'order')}".

**The fix (now 704-725, 900-904).** The read holds one mark, "the whole answer is owed". Step 5 sets
it; "While it is owed, no single place is told. When nothing is missing, the function gets `{:anew …}`
and the mark is taken off." Step 6 gives `{:anew}` when it is owed and nothing is missing any more,
whatever kind of source arrived. The mark is also set by any step that joined or left a source that
is no check, so that no step which says nothing rests on a later step of another push (section 5, "A
setting written again"). "What is held" lists the mark, and says why it is no store of our
own. Point 2 above.

### F3. "A place that still waits" was not said exactly, and one reading of it loses a change

**The plan's lines 574-575.** "`of-places` gives the place-answers of a set of places, or nil for a
place that still waits for a source."

**What is wrong.** Waits for which source: every wanted source at the place, or those that the walk
from the nearest layer reaches before it stops? With the first reading a place waits for a check behind
the layer that already wins it. If that check is then left, by an order that comes to hold the place,
nothing tells the place.

**The evidence.** The model with a place waiting for every wanted source: scenario 12, four layers,
fails (`logs/crossing.log`, "wait=strict").

**The fix (now 632-642).** "The walk stops at the layer that wins. A place waits when the walk meets,
before it stops, a layer whose standing is `:not-known-yet`, or a wanted source that `known` does not
hold; a source behind the layer that wins is not waited for."

### F4. Nothing was said of a callback that comes while a live read opens

**The plan's lines 341-342, 362-363, 385-386, 734-735.** "After it returns, the function is called
with each change". "It gives the answer now, and a handle." The read's entry holds "whether it is
opened", and no line says what that is for.

**What is wrong.** Three things. (a) A change that falls between the reading of a push's value for
`:now` and the mark "opened" is lost if its callback only looks at the mark: the trace is in section
6, L4. (b) If `read-live` held the read's lock while it waits for Rama, a callback that waits for the
lock would hold a thread of Rama's client, and the opening that `read-live` waits for can need that
thread (run: C4, C5): it would stand until `:wait-ms`. (c) "After it returns, the function is called"
cannot be held by a lock: the lock is let go before the call returns to its caller.

**The fix (now 744-764, 356).** A section "How a live read opens, and what its asker gets first":
`read-live` holds no lock while it waits; it makes `:now` under the read's lock; a callback takes the
lock before it looks at "opened". The calls' table says "The function is called with each change that
comes after `:now` was made". The docstring says what a caller that keeps a screen does. Point 4
above.

### F5. What a slow function stops was understated, and a read inside a function was left to its wait

**The plan's lines 387-389.** "The function must return soon: while it runs, every other change of
that push's thread waits. It must not call `read-once` or `read-live`, which wait for Rama on a thread
that Rama may need. It may call `leave`."

**What is wrong.** Run, C4 and C5 (section 4): a function that stays holds back selects and openings
of other callers, not only changes of its own push's thread. And a `read-once` inside a function is
not only forbidden: as planned it waits out `:wait-ms`, 10 s, with a thread of Rama's client and the
read's lock held.

**The fix (now 399-429, 454, 1589-1591).** The docstring's paragraph says what was run, with the
counts. `leave` inside a function is said with its run (C5, C6). A read inside a function is turned
away at once: `tell!` marks its thread for the length of the call, `read-once` and `read-live` look at
the mark first and give `:read-inside-an-askers-function` with no call to Rama. The words are seven.
Points 1 and 3 above.

### F6. A list or a mark written in a layer that she may not see gave her a whole answer anew

**The plan's lines 637-640.** "If what it means for this query is the same before and after
(`layers/meaning`: made or not, the setting, the list when it is the start layer, the stamp of the
asked key's mark; or whether her permission is there), nothing. Else … the function gets `{:anew …}`".

**What is wrong.** The stamp of the asked key's mark, and the list of a start layer, are in the meaning
whatever her standing. So when a member marks the asked key in a layer that this asker may not see,
she is given `{:anew}` with the answer that she had. The answer tells nothing; the call tells that
something of that layer was written. The implied spec, B2: "A read is never given a row of a layer
that the asker may not see, in any part".

**The fix (now 727-742, 1521-1524).** `layers/meaning` holds: made or not, the setting, whether her
permission row is there; "and, only when her standing with these rows is `:yes`, the list when it is
the start layer, and the stamp of the asked key's mark." The pure check P3 and the proof's check 15
hold it. A first form of this fix made the meaning the standing alone; the second model showed that
wrong (92 of 620 lists, `logs/standing.log`), and the plan has the form that holds, with the reason
beside it (now 736-742).

### F7. Three reads that could not change an answer

**The plan's lines 538, 541-542, 552-554.** "S5 … the layer's own rows are known and mark the key;
and, live, its standing is as for S4". "A layer whose standing is `:not-made` or `:may-not-see` wants
nothing but its S1 and, when its setting is `:members`, her S2". The checks: "each layer at a position
before j whose standing is `:yes` and whose own order does not hold that place".

**What is wrong.** (a) A read asked once reads the first 256 rows of the order of a layer that she may
not see, one round after her standing is known: the rights came in the round before. The template:
"If any N > M: FAIL — query issues wasted reads." (b) A layer that is not made reads as "members" by
its start value, so a live read kept a push on her permission in a layer that cannot hold one. (c) A
check in a layer that lies between two orders which both hold the place cannot change the answer: the
nearer of the two orders wins before that layer is reached.

**The fix (now 591, 594-596, 610-615).** S5 is wanted when the layer marks the key "and its standing
is `:yes`". A layer that is `:not-made` wants its S1 alone. A check is wanted only in layers nearer
than the nearest order that holds the place. The model plays both rules for the checks and both end
right; the tight one reads less.

### F8. By the text, a place from a layer was four calls, where the cost and check 11 count three

**The plan's lines 504, 534-537, 901-902.** "`:from-layer s` … While `s`'s own rows are not known, it
gives nil." S2 and S3 are wanted "as soon as the layer is in the list". The trace: "Round 1: the call
for `:alice-notes`. Its own rows give the list. Round 2: the calls for `:team` and `:base`."

**What is wrong.** With the list nil in round 1, nothing but the start layer's own rows is wanted then.
Its permission and its place are wanted in round 2, as a call of its own beside the two others: four
calls. Check 11 holds "3 in two".

**The fix (now 556-559, 598-600).** "The start layer of `:from-layer` is in the list from the first
round, before its own rows are known: its S2 and its fact source are wanted with its S1".

### F9. Four things about the numbers

**The plan's lines 879-880, 869, 413, 631-632.** "`{:what :checks-of-one-read :most 10000}`", where
the number is 40,000 (ruling 63). "After the push is open, each change is counted". "the read path's
wait passed before Rama gave every value". No line said what a live read does when its checks grow
past their number.

**The fix (now 1069-1071, 1056-1058, 453).** 40000. "Under an open read, a change that makes the
checks more than the number ends the read: `:a-run-grew-too-large`, with the same `:what` and
`:most`." "The value that the push gives at its opening is counted too". `:wait-ms` is "one bound for
the whole call".

### F10. "The design in short" said the comparison that "Live" rejects

**The plan's lines 155-157.** "…and calls the asker's function when that differs from what the diff's
old value gave." The plan's own "Live" (666-668): "A comparison of 'before' and 'after' with the other
pushes as they are now would be wrong".

**The fix (now 164-166).** "…calls the asker's function unless one of three tests shows that nothing
changed for her ("Live")."

### F11. Five edges of sharing and of failing that had no line

**The plan's lines 709-711, 756-762, 1350-1353.** Section 6 has the trace of each.

- A leaver whose swap takes an entry out, while the joiner that made the entry has not yet forced its
  delay: by "`close!` when the future is given", the leaver has no future to close, and the joiner
  then opens a push that no entry holds. Fix (now 934-940): the leaver "forces the entry's delay to do
  so".
- A call of the function that is begun after `leave`. Fix (now 941-942, 1589): `tell!` looks, under
  the read's lock and just before it calls, whether `:reads` still holds the read.
- A step that reads the value of a push which another thread's `leave` closed in that moment: Rama's
  client throws (T6). Fix (now 942-944, 1587-1588): `known-of` catches it and the step stops for that
  read.
- "A push that step 5 joined is not given within the wait": no thread waits for it. Fix (now 871-877):
  the wait is a bound on the push's future, and the read is ended on the thread of the JVM's timer.
- An opening that throws inside a delay: Clojure keeps the thrown thing in the delay and throws it at
  each later reader. Fix (same lines): it is caught where it is made, and the delay holds a future of
  a word.
- A function that throws, and is then given `{:ended}`: fix (now 394-397), a throw there is dropped.

### F12. Two words of the code that this tree does not allow

**The plan's lines 385, 734, 1338, 1352.** "the read's own lock"; "`task-keys` [read-path] → the keys
of the microbatch tables' tasks". `src-store/CLAUDE.md`: "'Key' is the key of a fact. 'Lock' is an
encryption key. Never one word for both."

**The fix (now 427-429, 1574-1575, 1623).** "In the code and its explanations this lock is called the
read's **turn**, and never a lock". `task-routing-ids`, after the store's own `routing-id`. Point 8
above, with `standing`.

### F13. Checks that could pass falsely, and things with no check

**The plan's lines 1156, 1186, 1189-1194.**

- P9 "Handles the two callbacks in both orders, each with the values as they are after both writes".
  That is one of the ways in which the values can stand, and it does not play the leaving: in the
  failing order of F1, P9's second callback would still be handled, where the real step has left the
  check. P9 would pass on the plan's own crossing case while the code fails it.
- Checks 14, 15 and 17 hold that an asker is given nothing. A check that looks at once passes while
  the change is still on its way.
- Check 11 counts selects; with a live read open from a check before it, a read asked once takes a
  value from the push and the count is lower.
- Check 16 could not see a push that no entry holds.
- Nothing ran a function that leaves or asks inside its own call (ruling 67), nor a write in the
  opening of a live read, nor a list or a mark in a layer that she may not see.
- Check 19 hands the read path an `UngracefulTerminationDiff`; whether the proof can make one of
  Rama's class is not known.

**The fix (now 1363, 1373-1380, 1402-1416).** P9 is a player of every order on the pure functions of
the step. A paragraph "How the proof shows that nothing came": a second act on the same push, and a
witness that holds that push. Check 11 runs with no live read open. Check 16 holds that the map of
pushes has no entry without a reader and that `foreign-proxy-async` was called as often as `close!`.
Checks 24 and 25 are new: the function on Rama's thread, and the opening. Check 15 gained the layer
that she may not see and the layer that joins over farther orders. Check 19 says what it does if the
class cannot be made. Check 23 gained an entity and a layer whose text is the start of another's.

### F14. Four shapes of the measures were not exact

**The plan's lines 1236-1239.** Ruling 28: "The plan gives each shape."

- M4, in the base: "to the moment at which the asker's function is called with that act's value". Two
  acts of one batch at one place give one change, with the later value (R5). At 10 acts a second some
  acts have no call of their own, and the measure would wait for one.
- M2: "1, 100 and 1,000 pushes" named no asker and no setting, so the count of pushes was not fixed.
- M3: its live shape through three layers opens about 10,000 pushes in one call, under a wait of 10 s.
- M1, the base: "how many acts a batch held", with no way named to tell it.

**The fix (now 1456-1459).** M4: "to the first call of the asker's function with that act's value or
with a later act's", and it prints how many acts were told only by a later one. M2: one asker who
holds a permission in a "members" layer, n + 2 pushes, `open-count` printed. M3: its read path is
opened with `:wait-ms` 120,000, and a word is printed as what was measured if one comes. M1: from the
answers' stamps, by the function of `gate/stamp.clj` that gives a stamp's batch if the tree has one,
else the line says that it is not told.

### F15. Two limits that the plan stated wrongly or not at all

**The plan's lines 806-807, 314-315.** "The number is read at each read that needs it and is not
remembered between two reads". "It never throws, whatever `x` is."

**What is wrong.** A live read with a run across entities of a microbatch layer holds one source for
each task for as long as it is open: the number is remembered, in the sources. And the check of a
query walks `:layers` by its shape before the rule counts it, so a list with no end does not come
back.

**The fix (now 986-994, 900-904, 323-329).** The plan says what a live read holds, that a change of
the number of tasks under it is not noticed, and that the docstring and the receipt say so. The
number itself costs no call (run: C8). The check of a query: "a part that has no end does not come
back. The count of `:layers` is taken with a bound before any layer of it is read"; and a thing of a
`:points-at` that is larger than any value that a gate keeps "passes, the read makes its digest and
one call, and finds no row".

## 4. What was run for this check

`plan-check-probes-read-path/`: `deps.edn`; `src/probe/callbacks.clj`; `model/crossing.py` and
`model/standing.py`; `logs/`, five files, each whole and short; `edits/`, the replacements as they
were applied to the plan. Each is a probe or a model: no form, name or table shape of the store comes
from one.

**The probe, `probe.callbacks`.** Rama 1.6.0, in-process, 4 tasks, 2 threads, one table that a stream
topology writes, a top-level map from a String to a row. Three runs. Run 1 is of the probe's first
form and stood still (see C9); runs 2 and 3 are of the form in the folder. Each case once in a run.

| # | Asked | Found |
|---|---|---|
| C1 | When a push's callback runs, does that push hold the new value? | Yes: 12 of 12 callbacks, one event each; and 6 of 6 when forty writes with no pause came as three callbacks for each push. A deref inside the callback gave the callback's new value each time |
| C2 | One event changes a row that two pushes watch, a place and a run. Inside each callback, what does the other push hold? | The two callbacks ran on two threads. The place's callback found the run's push with the old value, 6 of 6; the run's callback found the place's push with the new one. So two steps that cross see each other's change or do not |
| C3 | The first callback of a push opened with the call that does not block | A `ResyncDiff` with no old value, on the push's thread, 0.1 ms before a function chained to the future ran, on that same thread; the caller got the push after both. A push on a place with no row: the same. A function chained to a select's future ran on a thread of Rama's client too |
| C4 | A callback that stays 2.5 s | Eight pushes on one task: four on each of two threads, `…executor-pool-0-0` and `…pool-1-0`. The three others on the sleeper's thread got their callbacks 2,516 and 2,522 ms after the write; the four on the other thread after 174 and 182 ms. From six other threads at once, run 3: selects that block 2,340 ms, six of six; selects that do not block, waited for: 4, 5 and four times 2,340 ms; openings of a push: 4, 4 and four times 2,341 ms. Run 2: six of six, six of six, three of six |
| C5 | Inside a callback: `close!` of its own push, of a push on the same thread, of one on the other thread | Each returned in 0.01 ms. The status inside was still `:active`, and a deref inside still gave the value. After the callback returned each got its `DestroyedDiff` and was `:terminated`; a write to all six then called back the three that were open and no other |
| C5 | Inside a callback that stays 2.5 s: six selects and six openings that do not block | Three of each were given inside it. The other three were given within 0.3 s after it returned |
| C6 | `close!` from another thread while the push's callback runs | Returned in 0.01 ms, status still `:active`. A write made after it gave no callback. After the callback: `DestroyedDiff`, `:terminated` |
| C7 | The order of String keys | `"L x"`, `"L x a"`, `"L x!"`, `"L xz"`, `"L xä"`, then U+E000, U+FFFF, U+1F600: by code point, which is the order of the bytes of UTF-8. Java's `compareTo` puts U+1F600 before U+E000 |
| C8 | What `foreign-object-info` costs | 200 calls in 0.09 and 0.14 ms: it answers from what the client holds |
| C9 | A select that blocks, called inside a callback | Runs 2 and 3: back in 0.95 and 1.29 ms. Run 1: not back; a close of that push stayed `:active`, a later write gave no callback, and the probe stood still at its next opening of a push until it was stopped by hand |

**The plan's own probes, held against their logs.** Each line of the plan's tables T and U was held
against `tasks-run-2.log` and `runs-run-2.log`. T1 to T9 and U1 to U6 are as the logs have them: the
four keys `["d" "a" "b" "g"]`; one task for each key, for a select and for a push; the `MultiDiff`
whose processor was handed `[:put "L m a"] [:put "L m d"] [:out "L m b"]`; the `NewValueDiff[null]`
of a removed row; the first callback, a `ResyncDiff`; the two threads; `close!`; the times of T8; the
throw of the large window in U1; `[20001 "L big k020000"]`, `[1 "L n x"]` and `[0]` in U6. Two small
things: the push's future of T7 is 1.4 ms in that log where the plan says 1.3; and the log has one
line more than the plan's table, which the size rule needs: a window of 20,001 from the start of a
run of 3 gave `[20001 "L big k019996"]`, since the walk has no end and runs on into the next run. The
worker that a `submap` stopped is in the last lines of `tasks-run-1.log`, with Rama's own two lines.

What these settle: what the opening and the argument rest on (C1, C2, C3); what ruling 67 costs and
what a function may do (C4, C5, C6, C9); `rows/before?` (C7); the number of tasks at no call (C8).
What they do not: the store's own tables; a real cluster and its client; many pushes; a microbatch
table for C1 to C6. In run 2 the line "C7 by code point" is wrong by a fault of the probe's own sort;
the log's first line says so, and run 3 has it right.

**The models.** No Rama. Each plays, for one live read, every order of these moves: a write; a push
takes its next value; a callback is handled under the read's lock; a push that was asked for is served
with the table as it is then; its arrival is handled. A source that the read left gives it nothing
more. At the end, the asker's screen (`:now`, then each change) is held against what a read asked once
gives from the tables.

- `model/crossing.py`: what points at a thing, with ruling 64. Thirteen scenarios through two, three
  and four layers, and every list of up to three writes on one place in two layers.
  `logs/crossing.log`:

  ```
  == THE PLAN AS WORDED  {'told': 'before-leave', 'arrival': 'by-kind', 'wait': 'lazy', 'checks': 'plan'}
    FAIL     13 states     2 ends  1. the plan's own crossing: the farther order loses p while the nearer layer gets another row at p
    FAIL     14 states     3 ends  2. ruling 64: a nearer layer that does not mark the key writes the same thing; then the farther order loses p
    FAIL    716 states     5 ends  8. three layers, four writes on one place
    FAIL     13 states     2 ends  9. a permission is given: a nearer layer joins, with a place q of its own, over a farther place p
    FAIL     38 states     2 ends  11. a new place in the farther order, with a nearer row there already
    FAIL    312 states     2 ends  13. a nearer layer joins while a check of a new place is on its way
    every list of up to 3 writes on one place in 2 layers: 672 lists, 23461 states, 118 lists with a failing order
  == THE PLAN AS FIXED BY THE CHECK  {'told': 'after-leave', 'arrival': 'owed', 'wait': 'lazy', 'checks': 'tight'}
    (thirteen lines, each ok)
    every list of up to 3 writes on one place in 2 layers: 672 lists, 22906 states, 0 lists with a failing order
  == the fixed plan with told=before-leave: scenarios that fail: ['1', '2', '8', '11']; small lists that fail: 118 of 672
  == the fixed plan with arrival=by-kind: scenarios that fail: ['9', '13']; small lists that fail: 0 of 672
  == the fixed plan with wait=strict: scenarios that fail: ['12']; small lists that fail: 0 of 672
  == the fixed plan with checks=plan: scenarios that fail: none; small lists that fail: 0 of 672
  ```

  "The plan as worded" reads step 5 by its letter: `known` is made, then the leaving. The plan's words
  also allow the other reading; the fault is that the letter fails and that nothing held the builder
  to the other.
- `model/standing.py`: one place through two layers, the nearer with a setting and, for the asker, a
  permission or none. Every list of up to three writes of the setting, the permission and the two
  facts, from four starts. `logs/standing.log`:

  ```
  owes=by-kind   620 lists of up to 3 writes, 22142 states, 0 lists with a failing order
  owes=standing  620 lists of up to 3 writes, 22727 states, 92 lists with a failing order
  owes=setting   620 lists of up to 3 writes, 22320 states, 0 lists with a failing order
  owes=any-step  620 lists of up to 3 writes, 22167 states, 0 lists with a failing order
  ```

  `by-kind` is the plan as worded: for a place it ends right in every order. `standing` is the first
  form of this check's own fix F6. `any-step` is the plan as it now stands.

What the models are not. They are the check's reading of the plan's steps, in another language, by the
session that also wrote the fixes. They play one read, one place or two, and at most four writes. They
show that the fixed steps hold on those, and that each fix is needed. The build's P9 plays the real
functions.

## 5. The argument that no "before" value need be kept, traced

What Rama's client can do, by what was run. The changes of one push come in order, on one thread (T5).
The changes of two pushes come in no order, on two threads (T5, C2). A push holds its new value when
its callback runs (C1). A second push of the same row can still hold the old value then (C2). A step
is handled under the read's lock, so two steps of one read never overlap; between "the push takes its
value" and "its step has the lock", any other step can run. So in a trace `U(x)` is "the push x takes
its next value" and `H(x)` is "its callback is handled"; `H(x)` comes after `U(x)`; for one push they
keep their order; everything else can fall anywhere. `S(x)` and `A(x)` are "a push that the read asked
for is served" and "its arrival is handled".

The asker's screen is `:now`, then each `{:changed}` put in, each `{:anew}` in the place of all. The
claim to break: after the last step, the screen is what a read asked once gives.

### A place, through one layer and through three

One layer, `[:team]`, the place `p`. Its one fact source is the push on `p`. Its changes come in
order, each is told (no nearer layer), and what is told is worked out from the push's value. The last
one tells the last value. A second attempt of a batch in the base writes the same row: the callback
comes with a row that `rows/same-row?` finds the same (batch probe S5; T5), step 3 drops it, nothing
is told. Nothing can go wrong with one push.

Three layers, `[:alice-notes :team :base]`. At the start only the base holds `p`, with `b1`; the screen
shows `b1` from the base. Two writes: `:team` writes `t1` at `p`; the base writes `b2`.

| The order | Step by step | The screen at the end |
|---|---|---|
| `U(team) H(team) U(base) H(base)` | `H(team)`: no nearer row; told `t1` from `:team`. `H(base)`: `:team` holds a row at `p`; nothing | `t1` from `:team` |
| `U(base) H(base) U(team) H(team)` | `H(base)`: no nearer row yet; told `b2`. `H(team)`: told `t1` | `t1` from `:team` |
| `U(team) U(base) H(base) H(team)` | `H(base)`: `:team`'s push already holds `t1`; nothing. `H(team)`: told `t1` | `t1` from `:team` |
| `U(base) U(team) H(team) H(base)` | `H(team)`: told `t1`. `H(base)`: nothing | `t1` from `:team` |

The third line is the one in which a step says nothing because of a value whose own step has not run.
It is safe because the read still holds `:team`'s push: nothing but a change of what shapes the read
leaves a fact source of a place or a run, and that gives the whole answer anew. So `H(team)` comes,
finds no nearer row, and tells. A row of the facts table never goes away (a retract is a row), so a
nearer row that silenced a step stays the winner. The plan's argument (its old lines 651-664) is this,
and it holds for a place.

A retract in the nearest layer: the same, with `{:holds :nothing-here}` told from `:alice-notes`. An
entity or a run: the same argument for each place of the run; a change names its places (T5), and each
place is worked out by itself. A run across entities of the base is T pushes; a place is in one part,
but for a row that every task holds, which `answer/one-of-each` takes from the part with the latest
stamp. One task shows a batch seconds before another (the other check's fault F9): the asker is told
the places of the first task when they come and the others seconds later; each row that she is told is
a row that stays. She can see half of an act for those seconds. That is ruling 13, no lining up.

### What points at a thing: P1, the sequence that broke the plan as worded

Layers `[:team :base]`, both mark `:parent`. The base: `:block-7 :parent` is `:canvas-1`; `:team` holds
nothing there. The read holds the two orders and one check, `(keypath ":team :block-7 :parent")`,
which holds nil. The screen: `:block-7` from the base. Two writes: W1, `:team` writes `:canvas-2`
there; W2, the base moves `:block-7` to `:canvas-3`. The tables at the end: no order for `:canvas-1`
holds the place; a read asked once does not give it.

The order `W1 W2 U(check) U(base's order) H(base's order)`:

1. `U(check)`: the check's push holds `:team`'s row. Its callback is not handled yet.
2. `U(order)`: the base's order no longer holds the place.
3. `H(order)`, by the plan's old lines 633-646. `known` is made: the check holds a row.
   `sources/reading`: no order gives the place, so the check is no longer wanted and is left.
   `told-by?`: "no nearer layer of standing `:yes` holds a counted row at the place now" is false,
   by `known`. Nothing is told.
4. The check's callback: the read no longer holds that push. Nothing.

The screen keeps `:block-7` from the base. The tables give nothing. The plan's argument does not reach
this: it takes "the last callback … that changed anything that the final answer at that place rests
on: the row of the layer that wins it at the end", and at this end no layer wins.

By the fixed step 5: after the leaving, `known` is made again, and the check is not in it. `told-by?`
finds no nearer row. `of-places`: no order holds the place: `{:changed {[:block-7 :parent] {:holds
:no-fact}}}`. Right. The other orders of the same two writes: `H(check)` first tells `{:holds
:no-fact}` (a nearer row with another thing), and `H(order)` tells it once more; or `H(order)` first,
before `U(check)`, tells it. The model plays all of them (scenario 1).

### P2, the same with ruling 64 in it

`[:alice-notes :base]`; `:alice-notes` does not mark `:parent`. W1: Alice writes `:block-7 :parent` as
`:canvas-1` in her own layer, the same thing. W2: the base moves the block away. By ruling 64, after W1
the place is given from `:alice-notes`; after W2 no order holds it, and it is not found.

- `H(check)` then `H(order)`: the check tells `{:holds :value :value :canvas-1 :layer :alice-notes …}`.
  The order's step leaves the check and tells `{:holds :no-fact}`.
- `U(check) U(order) H(order)`: as P1. By the plan as worded the asker keeps the place from the base.
  By the fixed step she is told `{:holds :no-fact}`.

Here the answer at a place rests on a farther source while a nearer layer wins: the nearer row counts
only while some order holds the place. That is the one such case, and it ends with the step that
leaves the check, which always tells. The model's scenarios 2, 3, 4, 7, 8 and 11 turn it every way:
the nearer says the thing and the farther moves away and back; the nearer hides and then says the
thing; three layers.

### A nearer layer that gets or loses a row in the same moment, through three layers

Scenario 6 of the model: three layers, all mark. The middle order loses `p`, the nearest gets a row
with another thing, the farthest is written again with the same thing. 86 states, one end, right.
Scenario 12, four layers: the second loses `p`, so checks are asked in the second and the third; then
the third's order gets `p`, and the third's check is left before it was given. This is where "a place
waits" must be exact (fault F3): when the second's check arrives with a row, the walk stops there, and
the place is told though the third's check is still on its way.

A new place while its checks are on their way, and then gone again (scenario 11): the order gives
`p`; a check is asked; `p` waits. The nearer layer writes; the farther moves `p` away. The check
arrives and tells what it finds; the order's step then leaves it and tells `{:holds :no-fact}`. In no
order does a check that is left take a change with it that the asker needed: the step that leaves it
tells.

### A setting written again, a permission given

The second model plays these for a place. The sequence that showed this check's own first fix wrong:
the setting of `N` goes to "members"; the asker holds a permission; a fact of `N` changes.

`U(S1) U(FN) H(FN) S(S2) A(S2) H(S1)`:

1. `H(FN)`: `reading` finds the new setting in the push of the own rows, though that push's own step
   has not run. Her permission is wanted now, and is joined. Her standing is not known yet. The place
   is not told.
2. `A(S2)`: her permission is given, with a row.
3. `H(S1)`: the setting differs, "everyone" before and "members" after. Her standing is `:yes` before
   and after.

If the whole answer were owed only when her standing differs, step 3 would owe nothing, and the fact
of step 1 would never be told. So the setting itself is in the meaning, as the plan had it. And step 1
owes by itself: it joined a source that is no check. `A(S2)` then gives `{:anew}` with the fact in it,
and no step that says nothing rests on a later step of another push. The plan as worded ends right
here too, by another road: its step 6 gives `{:anew}` when her permission arrives.

The setting to "members" for an asker with no permission: the plan's trace 6. Under the new rule each
step in that window tells no single place, and `{:anew}` comes when her permission's push is given
with nil: the layer is left out. A fact of that layer whose step runs after that reaches no read: the
fact source was left. One whose step ran before the setting's change was in any push is told: "as late
as a push is, and no sooner" (implied spec, B4).

A setting written again with the same word: the row differs, the meaning does not; nothing. A
permission given while the setting goes to "everyone": either step joins the layer's fact source and
owes; `{:anew}` when it is given; the other step gives it once more or finds nothing new.

### The list written again; a key marked, unmarked, marked again

The list: the start layer's own rows call back; the meaning differs; layers join and leave; `{:anew}`
when nothing is missing. A layer that left takes its sources with it, and their callbacks reach no
read; the whole answer is worked out without that layer.

A key marked with the stamp 900, the mark taken off, marked again with 950, each before the push of
the step before was given. Step 1: the order of 900 is joined; owed. Step 2: the meaning differs
again; the order of 900 is no longer wanted and is left, though its push is not given yet; it is
closed when it is given. Nothing is missing: `{:anew}` with `:not-marked`. Step 3: the order of 950 is
joined; owed; `{:anew}` with `:level-since 950` when it is given. The late arrival of the order of 900
finds that the read no longer holds it: nothing. The two runs are two sources: their texts hold their
stamps (the hand-off), so no row of the first marking is read under the second.

### A second attempt of a batch, and one task seconds before another

A second attempt that writes the same rows: each open push gets one more callback with a row that is
the same (batch probe S5), or a `ResyncDiff` with rows that are the same after a worker's restart
(S7). Step 3 drops each. Nothing is told, and nothing is joined or left.

A batch of the base that writes a fact and, on another task, its row of the other order, seconds
apart. `[:base :team]`, a place that `:team`'s order holds; the base writes the same thing there. The
facts row shows first: the check in the base holds a row with the thing; by ruling 64 the place is
told from the base's row. Seconds later the base's order gets the place: its step leaves the check and
tells the place from the order's row, the same fact. The other way round, the order first: told from
the order; the check is never asked. A base fact that moves away, with the removal from the order
seconds late: the asker keeps the place from the base's order for those seconds, then the order's step
asks a check, which arrives with the new row, and the place is told as gone. It is late, as the hand-off
says a read of two tasks is, and it ends right.

### What this section does not show

That Rama's client delivers in these ways and no other. C1 to C3 and T5 are one machine, in-process,
one table of a stream topology. The models play one read. The proof of the build cannot force the order
of two pushes on Rama; its P9 plays every order on the real functions, and its checks 14, 16 and 25
make a crossing likely.

## 6. The lock and the threads (ruling 67)

**L1. A function that calls `leave` on its own read, inside its call.** The thread is a thread of
Rama's client, inside `tell!`, with the read's lock held. `leave` takes no lock: one swap takes the
read out of `:reads` and out of each entry; the entries that are left with no reader are taken out;
their pushes are closed. `close!` inside a callback, of that callback's own push, returned in 0.01 ms
and ended the push after the callback returned (C5). The function returns; `tell!` returns; the step
ends. A later callback of a shared push finds the read in no entry. Holds.

**L2. A function that leaves another read.** The same swap, for the other read. If another thread is
at that moment in a step of the other read, it holds that read's lock and may be about to call its
function: "a call of the function that another thread had begun to make can still arrive in the moment
in which `leave` returns" (the plan, on `leave`). For "none is begun after that", `tell!` must look
whether the read is still there, under the lock, just before it calls. The plan did not say so: fault
F11. That other step can also be reading the value of a push that this `leave` closes: fault F11, the
third edge.

**L3. Two reads share a push, each leaving the other inside its function, on two threads.** Thread A
holds the lock of R1 and calls `leave` for R2; thread B holds the lock of R2 and calls `leave` for R1.
Neither `leave` takes a lock, so nothing waits for anything. Each swap takes one read out. The shared
entry loses its last reader in exactly one of the two swaps, and that leaver closes its push: once.
`close!` from another thread while that push's callback runs returned at once (C6). Both functions
return. Holds. The proof's check 24 runs it 50 times.

**L4. A callback that arrives while `read-live` is still opening the read's pushes.** The plan had no
line (fault F4). The trace that loses a change, if the callback only looks at the mark "opened":
`read-live` reads the value of push P for `:now`, `v1`. Then P takes `v2` and its callback runs; the
read is not opened; nothing. Then `read-live` marks the read opened and returns `v1`. No later step
tells `v2`.

By the fixed plan: the callback takes the read's lock before it looks. Either it has the lock first:
it does nothing, and `read-live` then reads P under the lock, and P holds `v2` already (C1): `:now` has
`v2`. Or `read-live` has it first: `:now` has `v1` or `v2`, the read is opened, the lock is let go; the
callback takes it, finds the read opened, and tells `v2`. None lost. A change can come twice: in
`:now`, and again as `{:changed}` with the same place-answer. In order: the read's lock orders the
calls of the function among themselves; between `:now` and the first call the read path gives no
order, and the docstring says what the caller does (point 4).

And the lock is not held while `read-live` waits for Rama. If it were: a callback for this read waits
for the lock on a thread of Rama's client; the push that `read-live` waits for is given on that thread
(C3: a function chained to a push's future ran on the push's thread; C5: three of six openings made
inside a callback were given only after it returned). `read-live` would wait out `:wait-ms` and give
`:get-an-answer-within-its-wait` whenever a write fell in its opening.

**L5. A callback after `leave` returned.** `close!` returns at once; the push ends after the callback
that runs (C6); a write made after `close!` gave no callback (C6). The read path's own step looks up
"each read that holds the push" in `held` when it runs: the read that left is not there. One call that
another thread had begun can still arrive (L2).

**L6. The last asker leaves while another joins the same push.** The plan's "The two races of B5":
two swaps of one atom, so one is first. Leave first: the entry is gone, the joiner makes a new one and
opens a new push. Join first: two readers, nothing is closed. One edge had no line: the joiner's swap
is done and its delay not yet forced, and a `leave` of that same read, on another thread, takes the
entry out. The joiner then forces a delay that is in no map, and opens a push that nobody closes. This
can happen for a join that a callback makes for an opened read, whose caller holds the handle. Fault
F11, the first edge: the leaver forces the delay and closes what it gives.

**L7. Two open the same at once.** As the plan says: both swaps end with one entry that holds both;
a delay runs once. Holds. If the opening throws, a delay of Clojure keeps the thrown thing and throws
it at each later reader: fault F11, the fifth edge.

**L8. A push whose opening throws, or does not come back.** For `read-live`: its wait has one bound
for the call; it gives `:reach-the-store` or `:get-an-answer-within-its-wait`, leaves what it joined,
and "the push is closed if it is given later". For a join inside a callback no thread waits: fault
F11, the fourth edge. With the bound on the future, the read ends with `:a-read-it-came-to-need-failed`
on the thread of the JVM's timer.

**L9. Can a slow asker stop Rama's client from delivering to others?** Yes, and it was run: C4, and
point 1. The plan's docstring line said less than that: fault F5.

**L10. A function that calls `read-once` or `read-live` inside its own call.** As the plan had it: the
read waits for Rama with its bound, on a thread that Rama's client uses to give answers. Three of six
selects made inside a callback were not given until it returned (C5). So the call ends after 10 s in
`:get-an-answer-within-its-wait`, or it is given in time, by which thread Rama's client picks. For
those 10 s that thread and the read's lock are held, and with them the reads of others (L9). It does
not stand for ever: the read path makes no call that blocks. Fixed further: the call is turned away at
once, with a word and no call to Rama (fault F5, point 3).

## 7. The loop "read what is wanted, work out what is wanted next"

**It ends.** The plan's argument: five ranks; a source of rank r is wanted as a function of the query
and of sources of ranks below r. Held against the table of ranks after the fixes. Rank 0, the start
layer's own rows: the query alone. Rank 1, each layer's own rows: the list, which is the query or
rank 0. Rank 2, her permission: once, the list; live, the own rows of that layer (rank 1). Rank 3,
a place or a run: once, the list; live, the standing, which is ranks 1 and 2. The order S5: the marks
(rank 1) and the standing (ranks 1 and 2). Rank 4, a check: the rows of the orders (rank 3) and the
standings. No source is wanted by a source of its own rank or above. So with tables that stand still,
round i leaves nothing of a rank below i missing, and five rounds end it.

**Its bound.** Tables do not stand still under a live read: a list or a setting that is written while
the read opens makes a round be made again. The plan stops at 8 rounds and gives
`:get-an-answer-within-its-wait`; the fixed plan also holds one bound of time for the whole call. Both
bounds give a word and never a wrong answer. The sizing of a run is two selects inside the fetching of
one source, not a round.

**The round trips of a read asked once through three layers.** `{:asks [:place :note-41 :text] :asker
:alice :layers [:alice-notes :team :base]}`. Round 1: `reading` with nothing known wants, for each of
the three layers, its own rows, her permission and the place. `calls-for-once` groups them by table
and routing id: `$$streaming-facts` with `:alice-notes`, three paths; the same with `:team`;
`$$microbatch-facts` with `:note-41`, where the base's own rows and her permission ride, since every
task holds them. Three calls, sent at once, nine seeks. Round 2: nothing is missing. One round, three
calls: as the plan counts. One seek of the nine can be for nothing: her permission in the base, whose
setting is "everyone". To leave it out, the setting would have to be known before the call, which
costs a round trip more for every layer; the skill counts a round trip above a seek. Taken as the plan
has it.

From a layer: by the old text four calls, not three (fault F8); by the fixed text one, then two. What
points at a thing through three layers, once: rights; then the first read of each order; then the
checks, one call for each nearer layer and each 2,000 places: the plan's five rounds for the worked
case hold, with the two sizing selects of the base's order in between.

## 8. Each form of path, and R3

Can a query that passes the form make the read path send Rama a path on a whole table, a path that
Rama does not serve, or a run with no bound? A query holds no path, no table and no partitioning key.
`path-of` makes one of five forms from a source, and a source is made by six functions from texts that
`record/rows.clj` makes of ids that passed the check.

| Form | What the widest query makes of it | Whole table? | Served, by a run | Bound |
|---|---|---|---|---|
| 1 `(keypath key)` | one row | no | T5, the push probe's F1 | one row |
| 2 `(sorted-map-range from to)` | `[:run :first :last]`: `"<layer> "` to `"<layer>!"` | no: a run of one layer's keys. The push probe's Z5 opened a range over all of one task's keys | T3, T4, U6 | read whole, or opened as a push, only after forms 3 and 4 showed no more than 20,000 places |
| 3 the first 257 from the run's start, cut at the run's end | the same ends | no | U1, U2 | 257 rows walked |
| 4 the count of the first 20,001 from the run's start, and its last key | the run's start, no end | no | U6 | 20,001 rows walked, on the task, whatever layer they are of; two values cross |
| 5 `(multi-path …)` of forms 1, 2 and 3 | 2,000 paths at most | no | T8, T9, U2 | the bounds of its parts |

A source of another kind makes `path-of` give nil, and no call is made (check 12). `submap`, the form
that stopped a worker (T9), is in no form. One thing of form 4 to know: its walk has no end, so on a
task that holds other layers it counts rows of other layers and brings back the key of one. That key
is compared with the run's end and handed to nobody.

A run that grows after it was sized: under a push, step 4 ends the read. Between the sizing and the
opening the plan counted nothing: fault F9.

**The two ends of each run, against Rama's order of String keys.** The order is by code point (run:
C7). The least character that a printed id can hold is `!`, the next after a space: read at
`act.clj:246-249`, the first character of a keyword's name is of ``[\p{L}*+!_?$%&=<>.\-]`` and the
rest of that with digits, `'` and `#`; a UUID prints as hex digits and `-`. No printed id holds a
space or a character below it.

| The run | Its two ends | The edge | Holds? |
|---|---|---|---|
| An entity `:a` | `"L :a "` to `"L :a!"`, the end not in it | `:ab`: `"L :ab …"` has `b` where the ends have a space and `!`; it is after both. `:a!x`: `"L :a!x …"` is longer than `"L :a!"` and begins with it, so it is after the end | yes |
| A whole layer, `:first` to `:last` | `"L "` to `"L!"` | a layer `:team2`, or `:team!`: its keys begin with `":team2 "` or `":team! "`, after `":team!"` | yes |
| `[e k]` as the last end | `place-key` and one space | a key between `"L e k"` and `"L e k "` would have to go on with a character below a space | yes (U4) |
| The first end after the last | `[:run [:ab] [:a]]`: `"L :ab "` is after `"L :a!"` | turned away by the rule `:run-ends-in-order`; Rama would give an empty map (U5) | yes |
| The layer's own rows | `"L L :softland.store/"` to the same with `0` for `/` | a key `:softland.store.x/k` has `.` where the start has `/`, and is before it; no accepted fact is under another key of that namespace (the hand-off). The run holds the store keys that sit on the layer's own entity, and her permission when the layer's id is a holder's id: at most as many rows as there are store keys | yes |
| The facts by value | `"L K S <digest> "` to the same with `!` for the last space | a stamp `9000` beside `900`: `0` where the run has a space. The digest has 64 hex characters, so no digest is the start of another | yes, by the hand-off's own argument, which that plan's check ran (X1) |

`rows/before?` must be by code point and not by `compareTo`: for a letter outside the first plane the
two differ (C7). The plan says "by code point"; the fixed plan says why not the other, and check 23
gained the entity and the layer whose text is the start of another's.

## 9. The base

**Which task each word reads in a microbatch layer, with which partitioning key.** Held against the
hand-off's table "Which key a client gives Rama to reach a row", line by line.

| The plan's source | The hand-off | Agrees? |
|---|---|---|
| S3, a place: the entity | "A fact under a plain key …: the fact's entity, as an id" | yes |
| S4, an entity, or a run whose two ends name one entity: the entity | the same | yes |
| S4, a run across entities: one source for each task | "a run across entities meets the five rows of the second line once on each task" | yes |
| S1 and S2: any task; live the layer id, once the key of the read that they go with | "any: every task holds the row. Give the key of the read that it goes with" | yes |
| S5: `(rows/value-digest-of V)` | "the value digest: `rows/value-digest-of` of the value, a String of 64 characters" | yes |
| S6, a check: the entity | a fact under a plain key | yes |

**A row that every task holds is given once.** Such a row is under a store key, so it is in an answer
only with `:stores-own-facts true`. Then `answer/one-of-each` takes the row with the latest stamp among
the parts. Two parts can differ for seconds after a commit that failed (the other check's F9): the
part that shows the batch has the later stamp, and that row is the one that stays. Holds.

**`Helpers/genHashingIndexKeys` (ruling 62).** What it is: a class of Rama's jar that gives, for n,
one String for each task, which Rama's own hash partitioner sends to that task. What the plan's run
showed: T2, the same four Strings as the test namespace's function; T3 and T4, a select and a push
with each as partitioning key read and watched one task each. What would break: if the number of tasks
changed, a read asked once would read the new number at its next call (it costs nothing, C8); a live
read would go on with the routing ids of its opening and miss a task, with nothing to tell it (fault
F15, point 13). If Rama changed its hashing, the class is Rama's own and would change with it; what is
not known is whether Rama means the class for a client outside a test (the plan's open point 3, ruling
62).

**Rights from one task, facts from another, seconds apart.** Live, the base's own rows and her
permission are watched on the task of the layer's id, and a fact on the task of its entity. After a
commit that fails, one of the two can show a batch seconds before the other. A permission that is
given in that batch: if the rights' task is late, she joins seconds late. A setting that goes to
"members" in that batch, with the rights' task late: she is told facts of that batch for those
seconds. A fact of a later batch cannot come before: Rama began no run before every task had committed
(batch probe S2). So what she can be told too much is of the one batch that held the setting's change.
That is "as late as a push is" (implied spec, B4), stretched by the failed commit; the hand-on of the
other plan says that a read must not rest on a batch being whole across tasks, and the plan does not:
it says so under "What a read must not rest on". A read asked once reads rights and fact on one task
in one call.

## 10. Who may see: every path by which a row could reach an asker who may not see its layer

The rule, implied spec B2: "A read is never given a row of a layer that the asker may not see, in any
part: not its value, not that a place holds a fact, not how many places a run holds. What it is told
of such a layer is that it was left out".

| The path | Trace | Holds? |
|---|---|---|
| A read asked once reads rights and facts in one call (ruling 61) | `:bob`, `[:alice-notes :team]`, a run. Round 1 brings from `:alice-notes` its own rows, no permission row, and up to 257 rows of the run. `standing`: `:may-not-see`. `answer/whole` walks layers of standing `:yes` only. The 257 rows are dropped when `read-once` returns. The size of the run is not asked ("steps 2 and 3 are made only when the layer's standing is `:yes`") | yes. The rows cross from Rama to the runtime; nothing of them is handed over. Ruling 61 takes that |
| The same for what points at a thing | By the old text, round 2 also read the first 256 rows of that layer's order, with her standing already known | no row reached her; the read was for nothing. Fault F7 |
| A callback handled just before the setting's change | The setting goes to "members"; a fact of the layer changes after it. If the fact's step runs while no push holds the new setting yet, she is told the fact | as the spec has it: "a fact that was written after the setting went to 'members' can reach the read path before the setting's change does". C measures how late (M4) |
| The same, with the setting already in its push | The fact's step runs `reading` on the pushes' values: the setting is "members", her permission is joined, her standing is not known yet. Nothing is told | yes; and by the fixed rule that step owes the whole answer, so nothing is lost for an asker who does hold a permission (section 5) |
| After the setting's step | Her read left the layer's fact sources. A later callback of those pushes finds her read in no entry; the other askers of the push get their change | yes (check 15) |
| A list or a mark written in a layer that she may not see | By the old text she got `{:anew}` with the answer that she had: a signal that something of that layer was written | no. Fault F6, fixed |
| The store's own facts (ruling 11) | The run of the entity `:alice` holds her permission; a run across entities holds each request for locks. `answer/counts?` gives false for a store key's row unless the query holds `:stores-own-facts true`; `told-by?` holds "the row counts" | yes. With the part asked, any asker who may see the layer gets them: ruling 11's answer, in the receipt |
| A place under a store key (ruling 68) | `[:place :team :softland.store/who-may-see]` without `:stores-own-facts`: the rule `:store-key-is-asked-for` turns it away, no call | yes |
| What "left out" tells (rulings 8, 65) | `:left-out [{:layer l :why :may-not-see}]`, or `:not-made`. From a start layer that she may not see: that one layer, and its list is not used. No count, no place, no word of the layer's facts. `:serve-a-read-that-large` is given only for a layer of standing `:yes` | yes: it tells what ruling 8 lets it tell, and that a layer is made or not |
| An asker that is no id | `:asker "bob"`, or an id of 300 characters: `:asker-is-no-id`, `:id-over-the-limit`, no call. Neither `:asker` nor `:not-signed-in`, or both: `:no-one-asker` | yes |
| A person who is not signed in | `standing`: "members" gives `:may-not-see` without a read of any permission; "everyone" gives `:yes` | yes |
| One thing shared by two askers of whom one may not see it | The push carries whole rows for all. A step runs for each read that holds the push; a read holds a fact source only while its asker's standing is `:yes` or, for a moment, not known yet, and in that moment nothing of the layer is told | yes |
| A read asked once that takes a value from an open push | The value is the layer's rows as the push holds them; her standing is still read for her, and `answer` gives nothing of a layer whose standing is not `:yes` | yes |
| The writer and the permission of a row (R4) | `answer/place-answer` puts `:writer` and `:permission` in only when the query's `:with` asks | yes |
| What ruling 64 adds | A place can be given from a nearer layer's row that the check read. The check is asked only in a layer of standing `:yes` | yes |

## 11. What is held in memory (R2; ruling 26)

Every thing that the read path holds between two calls or two callbacks, after the fixes.

| What | For how long | A copy of what the tables hold, kept fresh by the read path? |
|---|---|---|
| The four handles of Rama's client, borrowed | the process | no |
| The numbers | the process | no |
| `:pushes`: for each source its opening (a delay around Rama's future), its readers, the mark that its first callback came | while a read holds the source | no. The source is the name of a thing that is watched, no value of it |
| `:reads`: for each read its query, its function, its lock, the set of its sources, whether it is opened, whether the whole answer is owed | until `leave`, or the read's end | see below |
| For a live read with a run across entities of a microbatch layer: the routing ids of the tasks, inside its sources | as long as the read is open | a fact of the cluster and none of the tables. It is not kept fresh: fault F15, point 13 |
| A thread's mark, for the length of one call of an asker's function | that call | no |
| The values: each `ProxyState` | while the push is open | Rama's, kept level by Rama. Entry 3: "yeah i also don't consider it a cache" |
| A read asked once: what it fetched | until it returns | no |

Two of these are nearest to the line. The mark "the whole answer is owed" says something about what
the asker was told, and it is the one such thing. It is true or false; no step works out an answer
from it; it is set by one step and taken off by another, and never "kept fresh" against a table
(point 2). The set of a read's sources follows the tables: for what points at a thing it holds one
check for each place of a farther order and each nearer layer, 10,020 in the worked case. It is made
again by `reading` from the pushes' values at each step, and no answer is worked out from it. Ruling
26 names "the pushes" among what is kept; the set is which pushes.

Nothing else. The read path holds no row, no value, no answer and no place-answer in a field or an
atom: by the plan's text, and the proof holds it as far as a run can (check 17: after every asker
left, the two maps are empty; and the entries have no field for a value).

**Lost at a restart of the runtime:** all of it. Nothing tells a caller; each asks again. Rama clears a
push that sends no heartbeat (read, not run, by the plan's citation).
**What grows with callers that never leave:** their reads and their pushes, and in Rama's client every
row of every run that they hold, values too. `open-count` shows the count. Nothing bounds it but the
end of the process (ruling 25), and the docstring says so.

## 12. The numbers (ruling 63), and a very large run

| The number | Ruling 63 | The plan | Agrees? |
|---|---|---|---|
| places read first | 256 | `:run-start-places` 256; the window is 257 | yes |
| places of one run | 20,000 | `:max-places-of-one-run` 20,000; the sizing walk is 20,001 | yes |
| checks of one read | 40,000 | `:max-checks-of-one-read` 40,000 in the table; "`:most 10000`" in the text | no: fault F9, fixed |
| paths in one call | 2,000 | `:max-paths-in-one-call` 2,000 | yes |
| the wait | 10 s | `:wait-ms` 10,000; whether for one wait or for the call was not said | fault F9, fixed: the call |

**The three steps of a run.** A run of 30,000 places in `:team`, asked once. Step 1, form 3, in the
layer's first call: 257 rows come back, so the run is not known to be whole. Step 2, form 4: `[20001
k]`, and `k` is before the run's end `":team!"`; so the run holds more than 20,000: the word
`:serve-a-read-that-large` with `{:what :places-of-one-run :most 20000}`, and no place. Two calls, at
most 20,258 rows walked on the task. A run of 12,000: step 2 gives `[12000 k]`, or `[20001 k]` with
`k` after the run's end when other layers' rows follow on the task; the run is read whole by form 2.
The rule, "more than 20,000 exactly when the count is 20,001 and the last key is before the run's
end", was held against the four answers of U6: `[20001 "L big k020000"]` inside a run of 200,000,
over; `[20001 "L big k019996"]` for a small run whose window ran on into the next run, not over; `[1
"L n x"]`; `[0]`. It holds on each.

**A run that grows past the bound under an open read.** Step 4 of "Live" counts the new value of each
change and ends each read on the push with `:a-run-grew-too-large`. The asker's function gets
`{:ended …}` and asks again; she then gets the word for a run that is too large. Between the sizing
and the opening nothing was counted: fixed (F9).

**In the base** a run across entities is sized for each task by itself. So the bound of 20,000 is for
one layer on one task, as the table of numbers says, and a run of the base can bring T times 20,000
rows to the runtime. The plan says "on one task" in the table and nowhere warns of the product: at 4
tasks, 80,000 rows. It is what ruling 63 took ("None bounds the bytes of a run"), so it is no fault;
it belongs in the receipt beside that line.

**The checks.** More than 40,000 wanted: `reading` wants none, and the read gives the word. Under an
open read the plan had no line: fixed (F9).

## 13. The proof and the measures

**Each requirement of the implied spec's "What the proof of this build must be able to show", for half
B and for R1 to R5, and its check.**

| The requirement | The check | Could it pass falsely? |
|---|---|---|
| V1, the proof is the only asker, and reads no table round the read path | the proof's first paragraphs; each `SHOWN` line that compares says so | — |
| V2, each word, each gate, once: the value that an accepted act wrote | 3 | no: `:fact` and `:stamp` are held against the door's act-id and answer |
| V3, the nearest layer wins; a retract hides; a farther place comes from there; the answer says which layer won; "what points at" by ruling 12 | 7; 10, with ruling 64 after section 2 | no |
| V4, kept live: the value now; one change for one act; none for an act outside; none for a farther act under a nearer row | 14 | yes, for "none", as planned: fault F13. Fixed by the witness |
| V5, left out: the other layers, the word, nothing of that layer | 8; 15 for a live read | 8 holds "every other part of the answer equals the answer of the same query without that layer", which is strong. 15 as V4 |
| V6, a proof of its own, with a row in the map | the file; the explanations' table | — |
| R1, one place opens a table | 22, a search of every `.clj` under `src-store/` | it shows the tree and no process, as the spec says |
| R2, a refused act gives nothing; each value given is of an accepted act; nothing is held after the last asker left | 17; P9 for the reason | "gives nothing": by the witness. That no value is held: by reading the code, and the plan says so |
| R3, the widest query ends in a word with no watch of a whole table | 12, 2 | no: calls are counted |
| R4, no writer and no permission unless asked | 5, P7 | no |
| R5, after several acts in quick turn the asker ends at the newest value | 14; 25 for the opening | no count of changes is asserted, as the spec asks |
| Ruling 17 (c), a second attempt gives the asker nothing | 18 | it holds "called once"; the second attempt must have ended first: the witness |
| Ruling 67, the function on Rama's thread | 20; and nothing for a function that leaves or asks | fault F13: checks 24 and 25 are new |
| Rulings 23, 24, 69 | 15 | — |
| B5 and its two races | 16 | it could not see a push that no entry holds: fault F13, fixed |

**Does P9 play the sequence that it claims?** No, as planned: section 3, F13. It handled the two
callbacks "each with the values as they are after both writes". In the order that fails, the real
step leaves the check, and the second callback reaches no read; P9's second callback would be handled
all the same and would tell the right thing. P9 would pass while the code keeps the place. As fixed,
P9 is a player: it does the joining and the leaving that `reading` asks, drops what a left source
would have brought, and plays every order.

**What no check can reach, and does the plan say so?** A cluster of several machines, a network, a
move of a leader, a killed process, Rama ending a push by itself, a reader left out of a change, a
number of tasks that changes, many askers, a restart of the runtime, an inhabitant: the plan's "What
no check reaches" names each. Added by this check: the order in which two pushes call back cannot be
forced on Rama.

**The measures (rulings 28 and 36).** Ruling 28, clause by clause.

| Ruling 28 | The measure | Exact? |
|---|---|---|
| "the streaming gate, and the base beside it" | M1: 100 acts a second for 30 s after 500, one fact of 100 bytes, each at another place; timed from the due moment and from the call | yes. "how many acts a batch held" had no way: fault F14 |
| "live reads open on the layer in three counts" | M2: 1, 100, 1,000, in two shapes | the asker and the setting were not said: fault F14 |
| "a canvas of 5,000 facts as one run, and as 'what points at the canvas', once and live, through one layer and through three" | M3: 8 shapes, 7 times each, the first 2 not counted; a streaming layer, and the base as the farthest | yes, but its wait: fault F14 |
| "how late a push is, from the append's call and from its ack, for each depot" | M4: from the call of `send-act` and from its return | not for the base: fault F14. And the door makes its append inside `send-act`, after the seal: the fixed line says so |
| "No target." | "holds nothing to a number, and exits 0 unless it cannot run" | yes |

Ruling 36: the code is in `test-store/`, beside the proofs, a run that is no proof, on the alias of the
module's proof. Holds. The contract's C, "The base on a real cluster cannot be measured here … the
receipt says so": M5 prints the line.

## 14. The form of a query; the explanations; the words

**Every fault of a query gets a word and no throw.** The plan's seventeen words of the check, each
held against a query that has the fault: a thing that is no map, `:not-a-query`; a part that the form
does not hold, `:part-not-known`; no `:asks`, or a first slot that is no word, `:asks-no-word-known`;
a known word with a wrong part, that word's own (`:place-not-named`, `:entity-not-named`,
`:run-not-named`, `:points-at-not-named`); an asker that is no id; neither or both of `:asker` and
`:not-signed-in`; `:layers` that is no list of ids; `:from-layer` that is no id; neither or both of
the two; more than 65 layers; a part of a row that is not known; an id over the limit; run ends out
of order; a thing that is nil; a store key that was not asked for. Two limits of "never throws" had
no line: fault F15.

**Nothing within the contract's first words is turned away by the form.** "a place; an entity, or any
run of neighbouring places; what points at a thing. Each is asked once, or kept live." A place:
`[:place e k]`. An entity: `[:entity e]`. Any run of neighbouring places: two ends, each `:first`,
`:last`, `[e]` or `[e k]`; that covers some keys of one entity, one entity, the entities between two
ids, from an entity on, a whole layer. What points at a thing: `[:points-at k thing]`, the thing any
value of the form. Once or live: the call (ruling 60). A run by a beginning and a count is not in the
form; ruling 29 asks for "the words of a run" with both ends in one layer, and the plan gives two
ends. Not a fault.

**Nothing outside them slips through.** Looking back, finding by words, what stands on a fact and
everything one person wrote have no word, and an unknown word is turned away. A `:points-at` under a
store key is turned away either way (ruling 40). One thing passes that can have no answer: a thing
larger than any value that a gate keeps. It costs its digest and one call. Said now (F15).

**The explanations.** Each line that the part makes untrue has its replacement named, in the plan's
table "Every explanation that the part adds or makes untrue". Held against the split's list
`explanations_to_replace`: `door.clj:52` and `:111-113` (both read here as they stand: "In the runtime
the door is the module's only client", and the line on W3 and W4); the store's page, "Who reads a
lock", "A direct read of the tables", "Closing"; `module/door_reads.clj`; `door/README.md:223`;
`module.clj`; `src-store/README.md`, the table and the first paragraph, never its decisions sections
(ruling 35); `test-store/README.md` and `test-store/softland/store/README.md`. Each is in the plan's
table. The read path's own page holds the contract's four sections. No README holds a list of
functions by the plan's text. After the fixes the count of checks in that table is 25, and "the six
functions".

**The words.** The read path's words are seven after fault F5. Each reads after "the read path could
not". None is a gate's reason or a word of the door (P11 holds it). A search of the rig's source for
23 of the plan's names (`read-once`, `read-live`, `points-at`, `left-out`, `told-by`, `from-layer`,
`stores-own-facts`, `not-signed-in`, `open-count`, `leave-with`, `level-since`, `nothing-here`,
`may-not-see`, `could-not`, `routing-id`, `nearer-checks`, `in-order`, `:anew`, `:asker`, `:asks`,
`standing`, `run-of-ends`, `own-rows`) finds two: `in-order`, in prose and as an argument, never as a
definition; and `standing`, defined once there, and the store's own word since its module (point 8).
"Key" and "lock": fault F12.

## 15. The rulings, each held against the plan

| Ruling | What it asks of this part | The plan | Holds? |
|---|---|---|---|
| 8 | left out: which layer, and one of two words | `:left-out`, `layers/left-out`, the one function | yes |
| 9 | the one list, no further; each layer once, the start layer first | `layers/in-order` | yes |
| 11 | the store's own facts left out unless asked | `answer/counts?`, `:stores-own-facts` | yes |
| 12 | B3 holds for "what points at"; one function | `sources/nearer-checks` | yes, as ruling 64 closes it |
| 13 | no lining up | said | yes |
| 14 | four things told apart; never throws on a row | the four `:holds` | yes |
| 15 | R4 as "handed nothing"; askers of different parts share | `answer/place-answer`; `:with` is no part of a source | yes |
| 16 (3) | the base's own facts read straight | `layers/standing`, said | yes |
| 17 (c) | nothing for a callback with the same value | step 3, `rows/same-row?` | yes |
| 23 | a live read follows what shapes it | S1 as one run of the layer's own rows; S2 only under "members": the plan's two readings, which the build session took | yes |
| 24, with 69 | a layer that is no longer rightful drops out; a word of the read path's own; nothing opened again; a whole new value | "The ending"; `{:anew}`; `{:ended}` | yes |
| 25 | the handle; nothing bounds a caller that never leaves; the two races closed | `leave`, `open-count`, "The two races of B5" | yes, with F11's edges |
| 26 | "It keeps no put-together value between two changes" | "Sharing, and what the read path holds" | yes; the mark is point 2 |
| 27, 28, 36 | the measures | section 13 | yes after F14 |
| 29 | the words of a run; in the base one part on each task, a row that every task holds once | the table of ends; `one-of-each` | yes |
| 30 | the record that `depot-at-making` names | said; the depot fact is read always, the plan's third reading | yes |
| 31 | one asker; a word for a person who is not signed in; no id, turned away | `:asker`, `:not-signed-in` | yes |
| 37 | no cut now | said at the plan's top, with the seam | yes |
| 38 | the record's forms from the hand-off, not from the code | every key text is made by `record/rows.clj`; section 9 | yes |
| 40 to 43 | a store key is never marked; the stamp of a mark; the digest; since which stamp | the rule `:store-key-is-asked-for`; `:level-since`; S5's routing id | yes |
| 60 | once or live is the call | two calls, one query | yes |
| 61 | a read asked once reads the fact with the rights | `sources/before-standing?`; S5 read beyond that: F7 | yes after F7 |
| 62 | `Helpers/genHashingIndexKeys` | section 9 | yes |
| 63 | the numbers | section 12 | yes after F9 |
| 64 | applied | section 2 | yes |
| 65 | `:from-layer` when she may not see the start layer | `in-order` gives `[s]` | yes |
| 66 | five functions more in `record/rows.clj`, as the build's last step | six now | point 7 |
| 67 | the function on Rama's thread under the read's lock; "The docstring says what the function must not do" | section 6 | yes; its cost is point 1 |
| 68 | a place under a store key, not asked, is turned away | the rule | yes |
| 69 | the whole answer anew when what shapes a read changes | step 5 | yes after F2 |
| 70 | five forms of path and no other | section 8 | yes |

## 16. The implied spec, checked off

"H" is "What holds for every operation of half B".

| The implied spec | The plan | |
|---|---|---|
| H1 one reader | `read_path.clj` is the one namespace that opens a table; check 22 | covered |
| H2 nothing of our own beside the push | section 11 | covered |
| H3 each read names its task | every source has a routing id; section 9 | covered |
| H4 one value for one push, never a whole table; a run stays in one layer | section 8; `rows/run-of-ends` gives two texts that begin with the layer's text | covered |
| H5 a function in a path is in the module's code | the five forms use Rama's own navigators, and `count` | covered (U1, U6 ran it) |
| H6 no durable state | "State primitive selection" | covered |
| H7 it never reads a lock | it opens the four tables of facts and no other | covered |
| H8 a callback on Rama's thread; two pushes in no order | "What a callback is"; section 5 | covered after F1 |
| H9 the family's manners | a word in place of a throw; the check before any call | covered; F15 for its reach |
| B0 opened once; two of it share nothing; the cluster gone under it | `open`; `:reach-the-store`; "nothing closes the read path" | covered |
| B1 a query is data; "the same" is decidable; an unknown word; three states of a place | the form; a source as the key of sharing; ruling 14's four | covered |
| B1, each line of "States that each word meets, in one layer": not made; may not see; nothing there; a value; a retract; the key not marked; a batch not closed; the store's own facts; a very large run | `:left-out` twice; `{:holds :no-fact}`; the row; `{:holds :nothing-here}`; `:not-marked`; read straight (ruling 17); `counts?`; section 12 | covered |
| B2 the table of "May she see?", six lines | `layers/standing`'s seven lines | covered |
| B2 "The check is made when a read is asked … For a live read it holds for as long as the read is open" | S1 and S2 are followed; F6 for what they tell | covered after F6 |
| B3 a list or a start layer; each layer held to B2; the nearest row; which layer won | `in-order`; `:layer` in each place-answer | covered |
| B3's states: one layer; two depots; a layer twice; a layer not made; a list that changes; a layer seen later | traces 1, 3, 5, 8, 6, 7 | covered |
| B4 the value now and each change; the newest state; rightfulness followed; "as late as a push is"; an end is told | "Live"; F1 to F4 | covered after the fixes |
| B4, "What can end a live read", six causes | `leave`; the setting; a permission's end, for which room is left; `UngracefulTerminationDiff`; the process; a resync | covered; the last three are read, not run, and the plan says so |
| B5 one push for one thing in one layer; a second asker from the push's memory; closed when the last leaves | "Sharing"; section 6 | covered after F11 |
| B5 "A run and a place inside it are two things" | two sources, said | covered |
| The matrix "5. A live read", 14 lines | opening: "How a live read opens"; may not see at opening: trace 5; Rama cannot serve: `:wait-ms`; a write inside: step 5; outside: no callback (the push probe); anew: `{:anew}`; setting to "members": trace 6; a give: trace 7; a second asker: trace 12; one leaves: trace 12; the last leaves: trace 12; the process ends: said; Rama ends the push: `{:ended}`; ended: nothing more | covered |
| The matrix 1 to 4, the lines that name a read | traces 5 to 11; ruling 64 for "points" under a nearer retract or another value | covered |
| P8, P9, P11 to P15, P23 to P31 | the rulings of the same numbers, section 15 | covered |
| "What a live read costs", "Worked cases" | "The cost"; the count of pushes, 8 for a place through three layers | covered |

## 17. The template's lists

**Query topology.** None: the contract's picture, "No query of the store stands between". In its
place, the reads of one call. Example 1, a place through three layers: N = 9 seeks; M = 9 when all
three are "members", 8 when the base is "everyone" (her permission there is not needed). Example 2,
the same for a person who is not signed in: N = M = 6, no permission is read. M differs with the
setting, which is not known before the call; to know it first costs a round trip for each layer
(section 7). Marked variable, and taken. One wasted read that had no such reason: fault F7.

**PState schemas.** None declared or changed. No Object type, no collection: the tables are the part
before's, and its check held them.

**Partitioning.** The read path writes nothing and places nothing. The plan's table is filled for 1,
16 and 128 tasks; its five rows are every kind of read; the shares sum to 1 (0.50, 0.35, 0.10, 0.04,
0.01); seeks are totals across tasks. Recomputed: 9.0, 9.15, 10.27. It grows, by one row: point 6.
The shares are a guess, and the plan says so: "No inhabitant asks yet".

**Topologies.** None. **Internal depot, cross-topology, stream topology correctness.** Nothing to
check: the read path appends nothing.

**Production readiness.**
- Many callers at once: one atom, swaps, a lock for each read; section 6. Yes after F11.
- After the runtime's process starts again: nothing is kept; each caller asks again; the spec
  tolerates it (H6). Yes.
- A worker of Rama started again at any point: a read in that second gives `:reach-the-store`; an
  open push is sent its value anew, the same rows give nothing; a push that Rama ends ends the read.
  Trace 14, check 21. Yes, as far as an in-process cluster shows.
- Large scale: one run is bounded by 20,000 places for each task, not by bytes (ruling 63); a live
  read under ruling 12 holds up to 40,000 pushes; nothing bounds the count of reads. Each is said.

**In-memory state.** No TaskGlobal. In the runtime: one atom of two maps. A read's set of sources at
300 bytes a source is 3 MB for the worked case of 10,028 pushes. Maps of maps, not flat arrays: the
template's rule is for a task's thread in a module, where a pause holds every caller; this is the
runtime's own heap. Named, not failed.

**Minimality.** The simplest design that could meet the spec: one select for each layer and source,
asked once; for a live read one push for each source and, at each callback, the whole answer worked
out again and handed over. The plan differs from it in these, each held to "what breaks if it goes":

| The mechanism | Delete it | Merge or bypass it |
|---|---|---|
| The layer's own rows as one run, one push | four seeks and four pushes for a layer in its place | it is the merge |
| Her permission's push only under "members" | one push for each asker of the base, for a fact that shapes nothing there | — |
| `{:changed}` for the places of a change, where the sketch gives the whole answer | a whole answer of a canvas for each block that moves; B4: "a change is a change of that" | — |
| The three tests that let a step say nothing | V4: "A change that an act in a farther layer makes at a place that a nearer layer holds gives the asker nothing" | — |
| The mark "the whole answer is owed" | scenarios 9 and 13 of the model | point 2: `{:anew}` at every last arrival, at the cost named there |
| The mark that a push's first callback came | the first callback would be taken for a value sent anew, and a new place under ruling 12 would give a whole answer | by "the future is not given yet" (C3 saw the first callback before the future): one run, no page of Rama's; the mark is the safe one |
| A delay around each opening | two who come at once open two pushes (B5) | — |
| The read's lock | two calls of one function at once, in no order (H8) | — |
| The three steps of a run | a run of any size crosses to the runtime (U6), or an exact count walks a task with no bound | the plan's difficulty log has the four ways with their numbers |
| The checks of ruling 12 | ruling 12's other answer; Sid's, in the queue | point 5 |
| The guard on a read inside a function, and its word | a call that waits 10 s on Rama's thread | point 3: the build session may strike it |
| `open-count` | the proof's checks 12, 16, 17 and the measures read it; ruling 25 has a caller that never leaves | — |
| Five pure namespaces | — | a split of files, no mechanism |

**Throughput.** For each frequent read, the least work that this check could build.

| The read | The plan | Less? |
|---|---|---|
| A place through k layers, once | k calls at once, 3 seeks for each layer | one query of the store would be one call; the contract's picture rules it out. 2 seeks for a layer that is "everyone": only by a round trip more |
| The same, live | 2k + g pushes; a change is one walk over k maps in memory | one push for each layer would need the rights in the row that is watched; that is the part before's table |
| An entity | one seek and its rows | — |
| What points at a thing, through layers | one seek for each order, and one for each place and nearer layer | yes: point 5, built and costed there; rulings 12 and 64 fix the count |
| A run across entities of the base | one seek on each task | the facts by value, for a tool that marks a key; point 6 |

By the template's letter the fourth row fails. It is a ruling's count and Sid's open question, so it is
a point and no fault of the plan.

## 18. Each operation and rule, by the template's block

"Worker restart" is a restart of Rama's worker; "the runtime" is the process that holds the read path.
A read writes nothing, so "a multi-partition write fails partway" and "two clients writing the same
entity" are asked of what a read sees of such writes.

### B1. A query written as data
- **Source:** "Its first words: a place; an entity, or any run of neighbouring places; what points at
  a thing. Each is asked once, or kept live."
- **Trace:** `{:asks [:run [:block-0001] [:block-0400]] :asker :alice :layers [:alice-notes :team
  :base]}`. `query/check`: the shape, then the seven rules; the ends are in order by `rows/before?`.
  Round 1: one call for each streaming layer with form 3 on `"<layer> :block-0001 "` to `"<layer>
  :block-0400!"`; four calls to the base, one for each task. `:team` gives 9 rows, fewer than 257:
  whole. The answer: `:places`, a map from `[entity key]` to a place-answer.
- **Fault tolerance:** the runtime restarts: nothing was kept. A worker restarts during the call:
  `:reach-the-store`, and the caller asks again. A batch of the base that fails partway: each row that
  a task shows is the row that stays; one task can show the batch seconds before another, and the
  answer then holds part of an act (ruling 13).
- **Races:** two acts on one place at once: the read gives whichever row the table holds. Callbacks of
  two pushes in no order: section 5.
- **Flaws found:** F8 (the count of calls from a layer), F15 (the reach of "never throws").
- **Verdict:** FAIL as planned; fixed.

### B2. Who asks, and may she see
- **Source:** "The read path reads the layer's setting and the asker's permission. Until sign-in is
  built, who asks is what the caller says."
- **Trace:** `:bob`, `[:alice-notes]`, a place. One call: the own rows hold a depot row and no setting
  row, so the setting is `(first-act/starts-with :alice-notes …)`, "members"; no permission row;
  `:may-not-see`. The answer: `{:layers [:alice-notes] :left-out [{:layer :alice-notes :why
  :may-not-see}] :place {:holds :no-fact}}`. The place's row came in the same call and no function
  looks at it.
- **Fault tolerance:** a second attempt of the batch that gave a permission in the base writes the same
  row (ruling 17); a read never rests on a give that an attempt could take back.
- **Races:** the setting written while a read is open: section 5, section 10.
- **Flaws found:** F6 (a mark or a list in a layer that she may not see), F7 (a push on a permission
  in a layer that is not made).
- **Verdict:** FAIL as planned; fixed.

### B3. Which layers
- **Source:** "The list that the layer gives, or the view's own. A layer of the list that the asker
  may not see is left out, and the read says that it left one out. For each place the nearest layer
  that holds a fact wins, also when that fact says 'nothing here'."
- **Trace:** `:bob`, `:layers [:alice-notes :team :nowhere :team :base]`, a place: `in-order` gives
  four layers; `:alice-notes` is `:may-not-see`, `:nowhere` `:not-made`; the place comes from `:team`
  or the base; a retract in `:team` gives `{:holds :nothing-here :layer :team …}` and hides the base's
  value. For "what points at a thing": section 2.
- **Fault tolerance, races:** as B1.
- **Flaws found:** none in this rule after ruling 64 was applied; with reasoning: one walk from the
  nearest layer makes every place-answer, for each word, and a layer that is left out is not walked.
- **Verdict:** PASS, as changed by section 2.

### B4. Live
- **Source:** "A live read is Rama's push, one for each layer of the list. What makes a read rightful
  is itself a fact: the asker's permission, and now the layer's setting, which can be written again.
  The read path keeps a live read on each, and ends the reads that are no longer rightful."
- **Trace:** the plan's worked case, eight pushes for a place through three layers; then sections 5
  and 6.
- **Fault tolerance:** the runtime restarts: every live read is gone, and each caller asks again. A
  worker restarts: a `ResyncDiff`; rows that are the same give nothing; rows that differ give
  `{:anew}`. A batch that is tried again: the same rows, nothing (ruling 17 (c)). A push that Rama
  ends: `{:ended}`, read and not run.
- **Races:** two pushes in no order: section 5. A callback in the opening: section 6, L4.
- **Flaws found:** F1, F2, F3, F4, F6, F9, F10, F11.
- **Verdict:** FAIL as planned; fixed.

### B5. One live read, shared
- **Source:** "Those who look at the same thing share one push. It ends when the last one leaves."
- **Trace:** the plan's trace 12: Alice opens three pushes; Bob joins two and opens his permission's;
  2 live reads and 4 pushes; one write, one callback, each function once; Alice leaves, 1 and 3; Bob
  leaves, 0 and 0.
- **Fault tolerance:** the runtime restarts: no read, no push; Rama clears what sends no heartbeat.
- **Races:** section 6, L3, L6, L7.
- **Flaws found:** F11.
- **Verdict:** FAIL as planned; fixed.

### R1. One reader
- **Source:** "No tool reads Rama by itself. The read path is the one reader of facts in the runtime".
- **Trace:** `read_path.clj` requires Rama; the five pure namespaces load on the alias `:store`, with
  no Rama; check 22 reads every `.clj` under `src-store/` for three names. The sentence of rule D8 is
  replaced in each of its five places.
- **Flaws found:** none, with reasoning: nothing in Rama can hold it (the implied spec, P26), and the
  plan claims a search and no more.
- **Verdict:** PASS.

### R2. Nothing of our own beside the push
- **Source:** "Nothing is shown before it is in the tables. No optimistic update, and no store of our
  own that we keep fresh beside Rama's push … If the build finds itself reaching for one, it stops and
  says so on its page."
- **Trace:** section 11, and section 5 for the reason that none is needed. A refused act writes no
  row, so no push calls back, and the asker gets nothing.
- **Flaws found:** F1: as worded, the plan's way of keeping nothing gave a wrong screen. The fix keeps
  nothing of the tables. F2 adds one mark, point 2.
- **Verdict:** FAIL as planned; fixed, with a point for the build session.

### R3. Never a watch of a whole table
- **Source:** "The read path never opens a push on a whole table or on a whole collection that keeps
  its elements apart (the probe). Rama does not refuse it cleanly, so the read path must."
- **Trace:** section 8.
- **Flaws found:** none, with reasoning: no caller gives a path; the widest thing that a word names is
  one layer; a push is opened on forms 1 and 2 only, and form 2 after a size.
- **Verdict:** PASS.

### R4. A part that was not asked for
- **Source:** "A read that did not ask for a part of a row is sent nothing of it (entry 7)."
- **Trace:** two askers of one place, one with `:with #{:writer}`: one push; `answer/place-answer`
  makes each one's place-answer; hers holds `:writer`, his does not (trace 12, check 5).
- **Flaws found:** none: ruling 15 reads "is sent" as "is handed", and the plan says what crosses from
  Rama.
- **Verdict:** PASS.

### R5. The newest state
- **Source:** "A screen is shown the newest state. A caller that must see every act cannot rest on a
  push".
- **Trace:** forty writes with no pause came as three callbacks (C1); the function is called with what
  the read gives now for the places named; the last call holds the last value.
- **Flaws found:** F4: a change in the opening could be lost by one reading of the plan's silence;
  and `:now` against the first change.
- **Verdict:** FAIL as planned; fixed.

### C. The measures
- **Source:** "The build measures, and sets no target: a gate's slowest 1 in 100 at 100 acts a second;
  the same with live reads open on the layer; the opening of a canvas of 5,000 facts; how late a push
  is. The base on a real cluster cannot be measured here".
- **Trace:** section 13.
- **Flaws found:** F14.
- **Verdict:** FAIL as planned; fixed.

## 19. Self-consistency

Read again for a line that says "a gap", "not ideal" or "a tradeoff" beside a pass.

- Section 12 says that a run of the base can bring T times 20,000 rows. It is ruling 63's own line,
  "None bounds the bytes of a run", so it is no fault; it is named for the receipt.
- Section 17 says that the template's throughput test fails on ruling 12's checks, and its test of
  growth on one row. Each is a ruling's or the contract's, and is a point: 5 and 6. Neither is passed
  as a pass.
- Section 17 names the maps of maps in the runtime's heap. The template's rule is for a task's thread.
  Named, not passed as held.
- Section 5 says what it does not show: Rama's client was run on one machine, and the models are the
  check's own. The fixed steps are held by P9 of the build's proof, which plays the real functions.
  This is the least certain part of this paper, as it was of the plan. One of this check's own fixes,
  F6 in its first form, was wrong, and the second model showed it. So a fix here is held as right only
  where a model played it: F1, F2, F3, F6 and the rule by which a step owes. F4, the opening, is
  derived from C1 and not played; check 25 runs it.
- R3 and R1 are passed with reasoning and no flaw; B3 is passed as changed by ruling 64.
- Every FAIL of section 18 is fixed in the plan, and each fix is in section 3.

The verdict is minor-fail: each fault was a line to change, and the lines are changed. The points of
section 1 are the build session's.

PHASE_VALIDATION:minor-fail
