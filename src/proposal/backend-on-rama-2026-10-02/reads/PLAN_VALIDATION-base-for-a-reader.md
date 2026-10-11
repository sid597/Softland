# Plan Validation: the base, for a reader

**What this paper is.** The check of `PLAN-base-for-a-reader.md`: the plan of the part
`base-for-a-reader` of the read build, the contract's A7 (`../BUILD-reads.md`) as rulings 16 and 17 of
`PROGRESS.md` read it. The check's job is to break the plan. It starts at FAIL and passes a point only
after a trace.
**The stage.** Phase 2 of the Rama skill (`phase-2-plan-validate.md`), for one part of three.
**Who wrote it.** One fresh session (Opus 5.5), in the night of 11 to 12 October 2026. It did not write
the plan. It wrote this paper and the folder `plan-check-probes-base-for-a-reader/`, and it changed
`PLAN-base-for-a-reader.md`. It changed no other file, nothing under `src-store/` or `test-store/`, and
it did not commit.

**Blocked.** Nothing.

**A ruling that cannot hold.** None. Two things about rulings are for the build session, and neither
breaks one. Ruling 39 counts three source files with a head comment; there are four (point 2). Ruling
49 gives the cost of the second cluster as "about 30 s"; the run gave about 6 s (point 1).

**The verdict: minor-fail.** The design holds: no trace found a way to a stamp from a clock that is not
kept, and none found a fact's row or an answer's verdict or stamp that two attempts make differently
with the clock kept. Nine faults were found, each of the plan's text or of its proof, none of the
design. Each is repaired in the plan. Ruling 49 is applied to the plan, as the task's first fix. Ruling
53 came while the check ran and changes the order of the builds; what it asks of this plan is applied
too, and said under its own heading.

**How the skill's template is adapted.** The template checks a module. This part adds one depot, one
stream topology and one PState to a module that stands, and changes the head of one step. Each heading
of the template is kept and answered for that. "Spec coverage" has one block for each thing that the
task names to break, with its trace. Four sections are added: what was run for the check, the first
fix, the faults with the plan's lines before and after, and the points for the build session.

**Marks.** "Read at" is read in the source at that line, in the tree as at `c3e65c06` for `src-store/`
and `test-store/`. "Run" names the run and its log. "Read, not run" is a line of Rama's pages under
`reference/rama/docs/`, the extract of March 2026 that names Rama 1.5.0, where the store runs 1.6.0.
"Derived" is reasoned here from lines that are named. "The plan's line N" is a line of the plan as the
build session committed it (`2ea0fff9`); the plan as it now stands has other numbers, so a changed
place is also named by its heading. A file name without a folder is under `src-store/softland/store/`.
"B" numbers are the batch probe's, "W" and "S" the plan's two runs, "T" this check's run.

**What was read.** The skill: `SKILL.md`, `phases.md`, `phase-2-plan-validate.md`,
`artifact-plan-validation.md`, `microbatch.md`, `stream.md`, `depot-reference.md`, `depot-design.md`,
`testing.md`, and of `dataflow.md` the sections on yielding and on what suspends. The contract, whole.
`src-store/CLAUDE.md`; of `src-store/README.md` the decisions of 7 October (1, 2, 4), 8 October (4) and
11 and 12 October (3, 8, 9). `PROGRESS.md`, whole, and again when rulings 52 to 55 came. This part's
entry of `DECOMPOSITION.json`, whole. Of `IMPLICIT_SPEC.md`: lines 1 to 217, "A7. The base, for a
reader", the matrix's "A batch of the microbatch gate", P16, P17, and "What the proof of this build
must be able to show". The plan, every line, with the sources and logs of its two runs.
`batch-probe/RESULTS.md`, whole, and of its sources `redef.clj`. Of `PLAN-what-the-gates-keep.md`: the
walks, the proofs and the list of explanations, read twice, the second time after its check was
committed (`794bc2fd`). Whole, function by function: `module.clj`, `module/microbatch_gate.clj`,
`gate/batch.clj`, `gate/stamp.clj`, `record/tables.clj`, `module/door_reads.clj`. Of
`gate/decision.clj`: `refused-early`, `standing`, `stamp-of`, `accept-writes`, `writes`. Of
`gate/first_act.clj`: `depot-at-making`, `making-act?`. Of `record/rows.clj`: the namespace docstring
and `closed?`. Of `module/streaming_gate.clj`: `other-gates-record-holds-layer?`. Of
`door/declaration.clj`: `stray-reason`. Of `test-store/softland/store/module_proof.clj`: the namespace
docstring, the hand, the hand for the base, the last check of the base, `test-ns-hook` and `-main`. Of
the store's page and the two proofs' maps: each line that the plan says it replaces. Of Rama's pages:
`14-depots.md:204-216`, `11-stream-topologies.md:138` and `:168`, `23-acid-semantics.md:112`,
`12-microbatch-topologies.md:116`, `:122`, `:130`.
**Not read.** `gate/batch.clj`'s callers in the door; the door; `keep.clj`, `commit.clj` and `fail.clj`
of the batch probe beyond what `RESULTS.md` quotes of their logs; `PLAN-read-path.md`, which came into
the tree while this paper was written.

## Points for the build session

Each is for a ruling or for the receipt. None is Sid's to decide here.

1. **Ruling 49 holds, and costs less than it says.** Three clusters one after the other in one JVM were
   run on the store's own module (T). A later cluster took 1.5 s to create and launch and under 1 s to
   close; a whole cluster with the making of the base and one batch took 4.3 s. So "about 30 s" is
   about 6 s, and the ruling's other road, a proof file with a command of its own, is not needed.
2. **Ruling 39 counts three files with a head comment; there are four.** `module/door_reads.clj:1-3`
   carries the same comment as `module.clj`, `module/streaming_gate.clj` and
   `module/microbatch_gate.clj`. This part changes a docstring in `streaming_gate.clj` and in
   `door_reads.clj`. The plan now has each of the two comments gain this plan. Strike that if a change
   of a docstring is not what the ruling means by "the part that changed the file".
3. **The order of the two clusters.** The plan now has the proof's cluster first and the cluster of the
   second attempt last. Then nothing is replaced in the JVM while every other check runs, which is the
   plainest reading of ruling 49. It costs one thing: the stop test's answer comes at the end of the
   proof's run and not at its start. The other order works too: with the first functions put back, a
   later launch ran those (T3).
4. **Ruling 53 was applied to the plan.** It came while this check ran. The plan's line numbers are of
   head `c3e65c06` and will not hold when this part is built after `what-the-gates-keep`; the plan now
   says at its top that each place is found by its quoted words. The proof's batch now holds the other
   part's rows, as N15 of that plan lists them. The code as that build leaves it did not exist when
   this check ran, so nothing here checks this plan's lines against it. See "A change that came while
   the check ran".
5. **A module update between two attempts of one batch.** The plan, and the page's line "For the
   receipt, from this plan", called the update that first brings the repair "the one moment" at which
   two attempts can still differ. Every update that changes the code from which a row is made is such
   a moment (fault F5). The plan's line for the receipt is written again; the page's line is the build
   session's to change.
6. **The one throw is wider than "the base stands still".** In the in-process cluster a throw starts
   the worker again: every task of that worker, both gates and the reads. While its cause stays it
   comes on every attempt (fault F6). Ruling 51's line for the receipt covers the phase timeout; this
   is a second line, and the plan now holds it.
7. **A form that is cheaper by one seek for a batch exists and is not taken.** Without the read before
   the append, every attempt appends and reads back: 2 seeks for a batch with things where the plan
   pays 3. It then leaves one record on the depot, which is never trimmed (ruling 50), for each
   attempt and not for each batch, and puts the wait for the ack into every later attempt. The plan's
   form is kept; "Throughput" has the numbers. Builder's.
8. **A forced failure gets slower each time on one cluster.** The second attempt came 1.0 to 1.5 s
   after the first for the first two failures of a worker, and 5 to 15 s after for the third to the
   sixth (T). The proof forces one. A part that wants more than one in that cluster pays for each.
9. **For the check of `PLAN-read-path.md`.** The plan's hand-on to that part gained one paragraph: the
   kept clock makes each shown row the row that stays, and it does not make every task show a batch
   together. After a commit that fails, the tasks that showed the batch and the others are apart until
   the second attempt commits: 3.4 s in B7's S3, and about 30 s by Rama's phase timeout with nothing
   set (fault F9).
10. **What no run here shows, and the design rests on.** That Rama drops what a failed attempt still
    has in flight when its ack comes back after the phase timeout (traced below: it would compute the
    same stamps). What `depot-partition-append!` gives the attempt when the keeper fails the record
    and no worker goes down. A move of a task's leader, and more than one copy of the row. Whether a
    module update can fall between one task's commit of a batch and another's. Each is named in the
    plan; ruling 17 (e) words the receipt for the first three.
11. **After the first of the two builds, the other plan's line numbers no longer hold either.** Both
    plans give lines of head `dc70e98b` or `c3e65c06` for `module/microbatch_gate.clj`, `gate/batch.clj`,
    the store's page and the proofs' map. Whichever is built second finds each place by its words. No
    two changes of the two plans meet in one line of code; in three docstring paragraphs and in tables
    of the maps they change different sentences of one paragraph ("The two plans in one walk").

## What was run for this check

One probe, T, in `plan-check-probes-base-for-a-reader/`: `src/check/two_clusters.clj`, with
`src/check/say.clj` for its log. It is a probe: no code of the store, and nothing in it is a form for
the store's proof. Rama 1.6.0, in-process, one machine, the store's own module `Store` from the tree as
it stands, with nothing under `src-store/` changed. Each cluster 4 tasks, 2 threads, 2 workers, as the
store's proofs launch theirs. The command is at the end of the folder's `deps.edn` and is run from the
repository root. The log is `logs/two-clusters.log`; Rama's own ERROR lines of the run, cut to 300
characters, are `logs/two-clusters-rama-lines.log`. One run, 177 s. No port was taken and no memory
ran short.

| # | Asked | Found |
|---|---|---|
| T1 | Do clusters one after the other in one JVM work at all: one is closed, the next is created, the same module object is launched again, and a batch is decided in each? | Yes, three times. Cluster 1 was created and launched in 3.6 s, made the base, decided a batch of 8 things in 0.7 s, took `update-module!` with the unchanged module, decided a batch after it, and closed. Clusters 2 and 3 were each created and launched in 1.5 s. Cluster 3 as a whole, with the base and one batch, took 4.3 s (log lines 2 to 9, 12, 55 to 59) |
| T2 | Three functions of `gate/batch.clj` are replaced in their vars after the module object was defined, and after the module was launched once in this JVM, and before the second launch. Does the second cluster run the replacements? | Yes. No attempt was noted while cluster 1 ran. After cluster 2 made its base, one attempt was noted; a batch of 8 things then gave one more with 15 rows (lines 8, 14, 15) |
| T3 | The first functions are put back before a third launch. Does the third cluster run those? | Yes. Cluster 3 made its base and decided a batch, and no attempt was noted (line 58) |
| T4 | The plan has `rows-to-send` throw "on the first task that comes there". Is the first attempt's note then whole? | Thrown on the first call, 6 batches: each time the throw came in a call that gave 0 rows, on the thread of worker `@2002`. Each note of a first attempt still held all 15 keys. Thrown in the first call that gives rows, 6 batches: each time the throw came in the call that gave 3 rows, on the thread of worker `@2003`. So the base's meeting task is on the other worker than the task that comes to `rows-to-send` first, and in the first way the note was whole because that other worker's thread went on (lines 16 to 51) |
| T5 | Can a check tell two attempts of one batch from two batches without the clock? | Yes, by the last stamp that `base` is given. In 12 of 12 batches that ran twice, both notes held the same last stamp. Each batch's last stamp was another than the batch's before (lines 16 to 49) |
| on the way | How soon the second attempt comes | The clocks of the two attempts were 1,146 and 1,481 ms apart for the first two failures on worker `@2002`, then 5,102, 15,331, 15,325 and 15,300 ms. On worker `@2003`: 1,001 and 1,548 ms, then 5,992, 10,298, 15,415 and 10,378 ms. Today's code reads the clock anew, so the difference of the clocks is the time between the attempts |

What T does not show: anything of the repair, which is not built; a real cluster; a killed process.

## The first fix: ruling 49 applied to the plan

Ruling 49: "The check of the second attempt has a cluster of its own ... Every other check of the
module's proof runs on the functions as the store defines them; the three functions that the proof
replaces are in place only for the cluster that makes a batch run twice, and are put back after it."
It goes against the plan's recommendation on its open point 4.

Each place of the plan where it bears, with the plan's lines before, and what stands there now.

| The plan's lines | Before | Now |
|---|---|---|
| 631-634, "Where it lives and how it is run" | "In the module's proof, `test-store/softland/store/module_proof.clj`, on the proof's one cluster, by the one command of today ... No new proof file." | The same command and no new file; "on the proof's one cluster" is gone. A new paragraph, "Two clusters, one after the other, in the one run": the proof's cluster first, on the functions as the store defines them; then `-main` puts the three noting functions in place, creates a second cluster, launches `Store` in it, runs one group there, closes it, and puts the first functions back in a `finally`; one count line, the two runs added; T's result |
| new, after it | — | "What the second cluster needs of its own": the hand's count of things sent, and a base of its own. `wait-until-processed!` waits for `(count @base-things-sent)` things (`module_proof.clj:316-320`), and Rama counts from the launch of a cluster (`testing.md`, "Testing Microbatch Topologies"). With the first cluster's things still counted, a wait on the second cluster would never end. So `-main` empties the hand's atoms, all but the shown lines, between the two |
| 636-645, "How a real batch is made to run a second time" | "Before `launch-module!`, `-main` puts another function in the var of each of three functions ... A replacement after the launch would do nothing until the module is updated (B5.b), so all three are in place for the whole run of the proof, and each gives exactly what the first function gives." | "in the second cluster. Before that cluster's `launch-module!` ... Rama takes the function that a var holds at each launch. Run (T2, T3) ... So the three are in place before the second cluster is launched and until it is closed" |
| 687-691, rulings 17 (a) and (b) as things observed | "in the same group. (a): the records on `*batch-clock-depot` are as many as the batches with things that `base` noted since the launch ..." | "in the same group, on the second cluster, where `base` notes each attempt"; the count is written again (fault F4); the last check of the base, which reads the new depot for lock bytes, is "on the proof's first cluster" |
| 693, the first sentence of VA7 | "One more check, with no failure in it." | "..., on the proof's first cluster." |
| 700, after the module is started again | "(the group that is there today)" | "(the group that is there today, on the proof's first cluster)" |
| 723-728, what the map says the proof does not reach | ends "... and that what the notes hold of the first attempt is the memory of the proof's process." | gains: a module update between two attempts; and "The map also says where each check runs: the batch that runs twice on a cluster of its own, with three functions of `gate/batch.clj` replaced by ones that give what the first functions give; every other check on the functions as the store defines them" |
| 790, function 10 | "`-main` puts them in place before the launch" | "`-main` puts them in place after the proof's first cluster is closed and before the second is launched, and puts the first functions back when the second is closed" |
| 791, function 11 | "new, the proof's hand" | "new, the proof's hand, for the second cluster" |
| 793, function 13 | "\"The proof\", above; and the last check of the base reads the new depot. Placed after the path of a lock in the base and before the module is started again" | which check runs on which cluster; the second cluster's group makes its own base, its locks and the fact that its batch replaces |
| new, function 13a | — | `-main` and `test-ns-hook`, changed: two clusters one after the other, one count line, the atoms emptied between, the first functions put back in a `finally`, the exit status over both runs |
| 895-902, the proofs' maps | "a group for the second attempt in the list of the module's proof" ... "The namespace docstring of `module_proof.clj`: its atoms (`:31-37`), the three replaced functions ..." | the group's line says that it runs on a cluster of its own, after the first is closed, with three functions replaced, and that every other group runs on the functions as the store defines them; the docstring's "State. The proof starts one in-process cluster" (`:29-37`) becomes two clusters, one after the other |
| 915-918, open point 4 | "**The module's proof runs with three functions replaced, for its whole run.** (a) As planned ... (b) A cluster of its own for the one check, at about 30 s more ... Recommended: (a)." | "**Settled by ruling 49: the check of the second attempt has a cluster of its own.** ... This plan had recommended the other way ... Two clusters one after the other in one JVM were run (T)" |

**The plan as changed, checked.** Three questions.

*Do two clusters one after the other in one JVM work?* Run: T1. Yes, on the store's own module, also
after a module update in the first.

*Does a function that is replaced before the second launch take effect, when the module object was
defined before the replacement?* The papers did not settle it. B5.a replaced a function before the
first launch of its module; B5.b showed that a replacement after a launch waits for a module update;
S launched once. None launched one module object twice in one JVM with a replacement between. Run: T2.
Yes. And the way back, T3: with the first functions put back, the third launch ran those.

*Does every other check then run on the functions as the store defines them?* By the order: the first
cluster is launched, run and closed before any var is changed. Derived from `-main` as the plan now
writes it; T1 shows the same order, with no attempt noted while cluster 1 ran.

## The faults, each with its fix

Each was a FAIL of the point where it stands. Each is repaired in the plan.

### F1. The throw "on the first task that comes there" does not make the first attempt's note whole

**The plan's lines 643-645 and 650-652.** "One of them, `rows-to-send`, throws once when the check asked
for it: in step 4, on the first task that comes there, when every row of step 3 is written and nothing
is committed." And: "So after the batch the proof holds, for each attempt, every fact row and every
answer row as that attempt made it (S3: 15 rows for 8 things, under the same keys in both attempts)."

**What is wrong.** Step 4 calls `rows-to-send` once on every task (read at `microbatch_gate.clj:353`
and `:389`: the block goes to every task, and each task's pass ends in that call). Only a meeting task
gets rows from it (`batch.clj:837-848`: it gives what the pass holds, and a task with no act that meets
has an empty pass). The rows of the acts that meet, a give's fact and answer among them, are noted
only in the meeting task's call. A throw in another task's call, before the meeting task's, takes the
worker down. If the meeting task is on that worker, its call never comes in the first attempt, and the
first note lacks those rows. `rows-side-by-side` then gives a key "that the other attempt did not
make", which is the stop test's second outcome. So the plan's "every row" rests on where the base's
meeting task sits, and the stop test could be met by the proof's own hand.

**The evidence.** Run, T4. Thrown on the first call: 6 of 6 times the throw came in a call that gave 0
rows, on worker `@2002`. Thrown in the call that gives rows: 6 of 6 times on worker `@2003`. So with 4
tasks, 2 threads and 2 workers the base's meeting task is on the other worker than the first comer, and
S's note was whole for that reason. With the meeting task as the second task of the first comer's
thread, the note would lack 3 rows.

**The fix.** The throw comes in the call that gives rows, after it noted them. Every act of the check's
batch is into the base, so one call gives rows. Step 3 is done on every task before step 4 begins
(`microbatch.md`, "`<<batch` blocks"), so every row of `decide-or-send` is noted by then. The plan's
"How a real batch is made to run a second time" now says so, with a paragraph "Why not on the first
task that comes there, as S threw"; function 10 says "throws once when asked, in the call that gives
rows, after it noted them"; the hand-on says that every act of that batch stays in the base.

### F2. The check cannot tell two attempts of one batch from two batches

**The plan's lines 674 and 735-738.** "1. The notes hold two attempts of the batch." And the stop test:
"its notes hold two attempts of the batch, and one of these is seen: 1. the second attempt's `base` was
given another clock than the first's".

**What is wrong.** A note is begun by `base` with numbers, once in an attempt. Two notes after one send
can be two attempts of one batch, or two batches, if the pause did not hold the things together. The
plan gives the check no way to tell. With the repair built, two batches have two clocks and other
rows: the check would read that as the stop test's first or second outcome. The build would be sent to
"the row before" by a fault of the hand.

**The evidence.** Read at `microbatch_gate.clj:282-286`: `base` is given the clock, the last stamp and
the numbers, and nothing that names the batch. The last stamp is in `$$microbatch-task`, which Rama
sets back for a second attempt (B3.i), and a batch with things writes a later one (`:287-290`). Run,
T5: in 12 of 12 batches that ran twice both notes held the same last stamp, and no two batches shared
one.

**The fix.** A note also holds the last stamp that `base` was given, and the wall time. The check's
first line is now: "two notes since the send, one throw taken between them, and both given the same
last stamp and the same numbers". The stop test begins "its first line holds (the notes hold two
attempts of one batch)", and "Not the stop" gains: "or two notes that were given two last stamps, which
are two batches". One thing is added to the check's third line, which only printed the two wall times:
it now holds that the second attempt began later than the batch's clock, so that one clock is not two
readings in one millisecond.

### F3. The edge act would not have been at the edge, by the plan's own run

**The plan's lines 666-668 and 823-824.** "an act that carries a stamp whose time is the hand's clock at
the send plus 10,400 ms ... It is at the edge when the first attempt takes its clock within 400 ms of
the hand's reading, and the second attempt begins later than that."

**What is wrong.** The act shows the one other thing that the clock moves, the verdict of the
10-second rule (the implied spec's table of A7, its first row). It does so only when its time lies
between the two attempts' clocks plus 10 s. 400 ms after "the hand's clock at the send" is too early.

**The evidence.** Read in `plan-probes-base-for-a-reader/logs/store-second-attempt.log`: the making act
was answered at 6,422.8; the topology went on at about 6,723.5 (the things were answered at 8,831.5,
"2108 ms after the topology went on"); the first attempt took its clock at 6,845.7. So the pause and
the eight appends took about 300 ms and the clock came 122 ms after that: 423 ms after a reading at
the send. The act would have been accepted on both attempts and would have shown nothing. The second
attempt came 1,088 ms after the first (S), and 1,001 to 1,548 ms for a worker's first failure (T).

**The fix.** 10,700 ms, with the reading taken right before the edge act is appended, last, and the
topology let go on at once. The window is then: the first clock less than 700 ms after the reading, the
second more. By S: 122 ms and 1,210 ms. The plan's item 6 and its list of numbers say so. It is the
proof's number, and the check still prints when the act was not at the edge.

### F4. Ruling 17 (a) is counted against attempts, not batches

**The plan's lines 687-689.** "(a): the records on `*batch-clock-depot` are as many as the batches with
things that `base` noted since the launch".

**What is wrong.** `base` notes an attempt. The batch that runs twice gives two notes and one record:
its second attempt finds the clock kept at line (ii) and appends nothing. As written the check fails
on the built repair, or is written to the wrong count.

**The fix.** "as many as the batches with things since that cluster's launch. That is the notes less
one: the second attempt of the batch that ran twice found its clock kept and appended nothing."

### F5. "The same" across a module update, and "the one moment"

**The plan's lines 567, 573-576, 597 and 946-947.** Scenario 10: "A module update | The ids go on, and
the row and the depot stay (W6). A batch that was in flight is run again under its id and finds its
clock | The same". And: "One moment where two attempts can still differ, said plainly: the module
update that first brings this change into a cluster that runs."

**What is wrong.** A second attempt after a module update runs the code of the new module. When the
update changed the code or the forms from which a row is made, the second attempt writes other rows,
with the same clock. That holds for every such update and not for the first alone. The next is in this
very build: the part `what-the-gates-keep` changes what a fact's row holds. So "what a row of a
microbatch layer may rest on" lacked a condition, and the line for the receipt was too narrow.

**The evidence.** Run, B5.b (`batch-probe/logs/redef.log`, 11410 to 11673): after `update-module!` the
dataflow ran the function that the var then held. Read, not run: "If a microbatch fails ... it is
re-attempted with the same microbatch ID" (`12-microbatch-topologies.md:116`). Not run, and no line
read here says: whether an update can fall between one task's commit of a batch and another's. W6 shows
only that the ids go on.

**The fix.** Scenario 10's last cell: "The same, when the update did not change the code from which a
row is made ...". The paragraph after the scenarios: "One kind of moment ...: a module update that
changes the code or the forms from which a batch's rows are made, when it falls between two attempts
of one batch", with the two such updates of this build. The hand-on's conditions gain "and one version
of the module's code runs every attempt of the batch". The row for the store's page, "A direct read of
the tables", gains "That holds while one version of the module's code runs both attempts". The line for
the receipt is written again. The map's "does not reach" gains the case.

### F6. What the throw costs is said too small

**The plan's lines 325-329 and 849.** "A throw fails the attempt, and Rama runs the batch again." And,
of an id that comes again (line 183): "the gate would stop at once and loudly".

**What is wrong.** Two things are left out. A throw on a task's thread takes the worker down, so it
stops every task of that worker for that time, the streaming gate's and the reads' too. And while its
cause stays, the throw comes on every attempt, for ever.

**The evidence.** Run, B2.4: "Unexpected throwable! Will be treated as a fatal!", then the worker is
started again; T's Rama lines show the same for each of 12 throws. The batch probe's "Seen on the way":
reads from outside threw for about 0.5 s after a restart. The skill: "A record that throws
deterministically retries forever" (`microbatch.md`, "Guarantees"). T, on the way: 1 to 15 s between
attempts.

**The fix.** The plan's bullet "The throw of line (v)" gains "What a throw costs, said whole", with the
two causes that are known and that no run saw. The row for the store's page, the new paragraph "The
clock of a batch", ends "fails by a throw, with what a throw does to the worker". One line more for
the receipt. The ruling's own sentence on the throw, after ruling 51, holds as it stands.

### F7. Explanations that the part makes untrue and the plan did not name

**What is wrong.** The contract: "Each line of the store's page that this build makes untrue is
replaced, not amended." `src-store/CLAUDE.md`: "An explanation changes in the same commit as the code
that it explains." A search of the two trees for each sentence about the batch's clock, about what a
gate's dataflow does and about a throw found these beside the plan's list.

| Where | Says today | Why it becomes untrue |
|---|---|---|
| The store's page, `:110-112` | "Each gate's dataflow only reads rows, calls the pure functions and writes rows." | The microbatch gate's dataflow also reads the clock, appends one record to a depot, and throws in one case |
| `module/microbatch_gate.clj:33-37` | "Approach. The dataflow only reads rows, calls pure functions and writes rows ... None of them throws, so no thing can stop the gate" | The same. The second sentence stays true of the pure code and of what arrived, and must say what the one throw guards |
| `module/README.md:18` | "Each gate's dataflow only reads rows, calls pure functions and writes rows." | The same. The plan had "gains one sentence"; the line is replaced |
| `gate/stamp.clj:3-4` | "the gate reads the clock once for a decision and gives the number" | An attempt of the microbatch gate that finds its clock kept reads none |
| `module/streaming_gate.clj:1-3`, `module/door_reads.clj:1-3` | the head comment that sends a changer to the store-module's plans | Ruling 39; the part changes a docstring in each (point 2) |
| The store's page, `:486-491`, rule 18 | an act's one stamp | The task names rule 18. Read: it names no clock, and no line of it becomes untrue. The plan now says that it was read |

**The fix.** Each is in the plan's list now: a row for `:110-112` and for rule 18 in the table of the
store's page; `:33-37` in the paragraph of `microbatch_gate.clj`; `stamp.clj:3-4` under "Other source
files"; a paragraph "Two head comments more"; line 18 of `module/README.md` under "The maps".

### F8. Two rows of the maps were to "gain the new function or functions"

**The plan's lines 892-893.** "`record/README.md:14` and `gate/README.md:22`: each row gains the new
function or functions of its file."

**What is wrong.** `src-store/CLAUDE.md`, "Each level has one home": "A README holds no list of
functions". Read at `record/README.md:14` and `gate/README.md:22`: each row holds the responsibility
of a file and names no function.

**The fix.** "the responsibility of each row gains what its file now also holds, the schema of the kept
clock and the pure code of a batch's clock. No function is named there".

### F9. The hand-on to the read path leaves out how far apart two tasks can show one batch

**The plan's lines 624-628.** The hand-on to `read-path` names what still differs between two attempts,
and that a push calls back once more with the same value.

**What is wrong.** The part's promise is "a reader may rest on a row of the base as soon as a task shows
it". That is a promise about a row. A reader could take it for a promise about a batch. The store's
page says of two tasks "for a few milliseconds" (`:184-189`). When a commit fails after some tasks have
shown their part, it is not milliseconds.

**The evidence.** Run, B7.1: 0.3 to 3.0 ms with nothing held. Run, B7.2, S3: tasks 0 to 2 showed the
batch 3.44 s before the second attempt showed it on task 3, and "the first attempt's rows stay readable
for as long as the late task is away". Run, W5, the other log: Rama's phase timeout with nothing set is
30 s.

**The fix.** One paragraph more in the hand-on to `read-path`: "Each row that a task shows is the row
that stays; that every task shows a batch together is not given ...". Point 9.

## A change that came while the check ran: ruling 53

Rulings 52 to 55 were written on the page after this check began (`794bc2fd`). Ruling 53: "The order of
the builds changes; ruling 32 is replaced. `what-the-gates-keep` is built first ... that this part's
new rows are the same on a second attempt is shown by the build of `base-for-a-reader`, which now comes
after and sees every row."

It changes what a fresh session needs from this plan, so the plan was changed for it, in six places.

| Where in the plan | What was added |
|---|---|
| The head, a new paragraph "Ruling 53 ..." | (1) The line numbers are of head `c3e65c06` and will not hold; each place is found by its quoted words; the change of the walk is the same one, and the gate's file holds one reading of the clock. (2) The proof's batch holds the rows of both parts. (3) Each explanation is replaced as it stands after that build |
| "What this part hands on", to `what-the-gates-keep` | That part is built first, from ruling 17 and the split, and its plan holds the rule already; this part's build holds each of that part's rows to it |
| The same, the bullet on the proof's hand (the plan's lines 618-621) | Before: "That part adds its own rows to the batch of that check; the check must still give no key. If its build changes what `decide-or-send` or `rows-to-send` gives, the noting functions change with it." Now: this part's build puts those rows in the batch, and what they need before it on that cluster; the noting functions take what the two functions take and give after that part's build |
| "The batch of the check", a new item 7 | The rows of N15 of the other plan; the rows of the kind `:by-value`, a nil row among them; `rows-side-by-side` tells a key noted with no row from a key not noted |
| The stop test, a new paragraph | A key whose rows differ only in a field that the other part added, with one clock given to both attempts, is a fault of that row's making; it is the stop only when it stays with the code as the two plans write it |
| The paragraph on a module update, and its line for the receipt | No order between the two updates is claimed |

What this check could not do for ruling 53. The code as the build of `what-the-gates-keep` leaves it
did not exist while the check read the code and ran T: `git status` then showed no change under
`src-store/` or `test-store/`. When this paper was finished, that build had begun: six files under
`src-store/` were changed in the working tree (`act.clj`, `door/declaration.clj`, `gate/decision.clj`,
`gate/first_act.clj`, `record/rows.clj`, `record/tables.clj`), none by this session. So each line of
today's code that this paper cites is of `c3e65c06`, and nothing here was checked against the other
part's code. What the other plan says it changes was read, and is under "The
two plans in one walk".

## Query topology

None is added and none is changed (the plan, "Query topologies"). Read at `module/door_reads.clj:84-172`:
the three stay as built, each held to closing. No read of this part needs one: each is one select of
one row on one task.

## PState schemas

- **Groups by key type and partitioner.** The plan adds one PState, `$$batch-clock`,
  `{Keyword -> fixed-keys{:batch Long, :clock Long}}`, on the topology `batch-clock`. It is alone on
  its topology, so there is no group of two. Its merge into another topology's PState is the plan's
  schema C and open point 1, which ruling 46 settled: a PState has one owner, and the owner is the
  topology that answers the ack. Pass.
- **Any Object type?** No. Pass.
- **Fixed-keys for a record-like value?** Yes. One key at the top, so the row is one entry on disk and
  the batch and its clock are read and written together (`tables.clj:14-19` says the same of the
  record's rows). A fixed-keys value at the top, as `$$microbatch-task` is, would put the two fields in
  two entries. Pass.
- **Different fields at one position?** No.
- **Inner collections over 100 elements?** None: one row of two longs.

## Partitioning

- **The one write.** The row is written by the stream event with no partitioner, on the task of the
  depot's partition, which is the task where step 2 appended: the gate's task, the task of the hash of
  `:microbatch` (`batch.clj:73-76`; `microbatch_gate.clj:280-281`).
- **`|hash`: a large keyspace, no hot key?** The keyspace is one key, by what the thing is: a batch has
  one clock, as the gate has one last stamp, on the same task today (`tables.clj:38-39`). The work
  there is for a batch and never for an act: 3 seeks and 2 writes for a batch with things. A batch
  took about 117 ms in B6, so at the most about 8 batches a second. It cannot be spread: every task's
  numbers gather on one task to make one base. A global PState would put it on task 0 and cost two hops
  when the gate's task is another (the plan, "Depots"). Pass, with that reasoning.
- **`|all`:** none added.
- **The table for N = 1, 16, 128.** Filled in (the plan's lines 374-396). Three categories: a run with
  nothing 0.500, the attempt that keeps the clock 0.499, a later attempt 0.001; they add to 1.
  Recomputed here as totals over all tasks: 0.500 x 0 + 0.499 x 3 + 0.001 x 1 = 1.498 at each N. Every
  seek is on the gate's task and no read goes to every task, so the number does not grow with N. Pass.
- **A placement that stores state to assist partitioning:** not needed; one row on one task.

## Topologies

- **Microbatch unless justified?** The added topology is a stream topology. Its reason is the skill's
  second: "append coordination is needed (stream participates in depot ack, enabling write-then-read-back
  patterns)" (`microbatch.md`). With `:append-ack` and with no ack the same attempt read nothing back,
  0 of 82 (B3.ii). Pass.
- **A write that must be visible in single-digit milliseconds:** the kept row, before the attempt goes
  on. Stream. Run: 2.8 to 4.5 ms (B3.ii cost), 4.1 to 6.3 ms and once 29.2 (W). Pass.
- **Each concern of the stream topology:** one, to keep the clock of the newest batch. It needs the ack.
  Pass.
- **A choice made on grounds of test synchronisation?** No. The proof's needs shaped the proof, not the
  module.
- **A second stream topology** against the template's "at most one": ruling 46.

## Production readiness

- **Several clients at once?** No client appends to the new depot (`:disallow`), and the one appender
  is step 2 of the one microbatch topology, which runs one batch at a time. Pass.
- **A client that starts again?** Nothing of this part is in a client. Pass.
- **A worker that starts again at any point of a topology's run?** Traced under "The keeper" and "Kept
  before any stamp", for a restart before the append, between the append and the read back, after the
  keep, and while the keeper holds the record. Run: W3, W5, B4. Pass.
- **At large scale?** One row. The depot grows by one record for a batch with things and is never
  trimmed (ruling 50): about 4 MB a day at one batch a second, by the plan's own count of about 50
  bytes, which was not measured. Pass.
- **Each write of a stream topology that is not the same when run again:** the one write is `termval`
  of a whole row at a fixed key, behind the rule that a record of the row's batch or of an older one
  writes nothing. Made the same by that rule. Run: B4 and W3 line 23, where Rama handed the record a
  second time and nothing was written. Pass.
- **A stream topology that writes to several partitions:** it writes one row on its own task. Pass.

## Internal depot usage

`*batch-clock-depot`, `:disallow`. Why the consuming topology cannot read a client's depot: the depot
carries a value that is on no client's depot, a reading of the clock (the template's (b)). And the
consuming topology must have written before the appending attempt goes on (the template's (a)). Pass.

## Cross-topology correctness

One flow: step 2 of `microbatch-gate` appends, `batch-clock` consumes. The sender can append twice for
one batch: "A retry re-appends" (`microbatch.md`, "Guarantees"), here only when an attempt found no
clock kept. The receiver takes a second record of a batch as nothing to write. Traced under "The
keeper". Pass.

## Stream topology correctness

"For each `depot-partition-append!` in a stream topology: is there a `(|direct (ops/current-task-id))`
immediately before it?" The stream topology `batch-clock` appends to no depot. The one append is in
the microbatch topology, where a commit boundary is the attempt. Not applicable.

## In-memory state efficiency

No task global and no memory of a process (the plan, "State primitive selection"). A task global came
back empty after a worker's restart (B3.iii a), which is one cause of a second attempt. Pass.

## Minimality: adversarial simplification

**The simplest design that I could build.** No depot and no topology: each run of the gate writes its
own reading of the clock into the gate's own PState on the gate's task, and the next batch with things
stamps from it. Rama sets that row back with the rest, so every attempt of a batch reads the clock that
the run before left. It is the plan's "road with no depot" (its difficulty log).

Built out, side by side with the plan:

| | The plan | The road with no depot |
|---|---|---|
| A batch with things | 3 seeks, 1 depot record, 1 row write, 2 rounds of replication, about 3 to 6 ms of wait | 1 seek and 1 write more in `$$microbatch-task`, no wait |
| A run with nothing | nothing, and one reading of the clock less than today | 1 write, in every run: about 3 a second in the in-process cluster, one a minute on a real cluster by Rama's default sleep (B6.1) |
| The clock that a batch is stamped from | read in that batch's first attempt | the run before: up to one minute old on a real cluster that was idle (B6.1: 60,268 ms with the sleep at one minute) |
| The 10-second rule | held against the batch's own clock | An act that carries a fresh stamp of the streaming gate is more than 10 s ahead of a clock that is a minute old, and is refused `:carried-stamp-too-far-ahead` with nothing wrong (`stamp.clj:82-87`) |
| To make that right | — | a run at least every few seconds, by a tick depot or a lower sleep: then every such run pays the walk's fixed part for ever, also when nothing arrives, and a message to every task when a batch is to be closed |

So the simpler road refuses honest acts after a quiet time, or pays for runs that carry nothing. Ruling
17 names the depot's road. The plan's design stands against it.

Each mechanism of the plan:

### The depot `*batch-clock-depot`
- **Delete it:** nothing carries a value from the microbatch gate to state that Rama does not set
  back: "only the topology that owns a PState can write it" (`tables.clj:3-5`). A7's repair, "keep each
  batch's clock", cannot be built. Kept.
- **Merge or bypass it:** a second use of an act's depot would put a thing that is no act on a depot of
  acts, against rule 5's sense and ruling 34. No.

### The stream topology `batch-clock`
- **Delete it:** nothing answers the `:ack`; the attempt cannot know that the clock is kept before it
  makes a stamp (ruling 17 (a)). Kept.
- **Merge it** into `streaming-gate`: the plan built that form side by side (its table under
  "Topologies") and ruling 46 chose. The numbers that decide it are the plan's: the wait would stand
  in the streaming gate's queue on one task, 13 to 21 ms with 10 acts queued.

### The PState `$$batch-clock`
- **Delete it:** a later attempt has nothing to read. Topology code cannot read a depot's log. Kept.
- **One row or a row for each batch:** ruling 47.

### The read before the append
- **Delete it:** see "Throughput". It costs one seek for a batch and keeps the depot to one record for
  a batch. Kept, with the numbers there.

### The read after the append
- **Delete it:** the attempt would stamp from what it offered. Traced under "The keeper", case c: when
  two records of one batch are on the depot, the row holds the first that was processed, which can be
  the other attempt's. Two attempts would stamp from two clocks. Kept.

### The rule "a record of the row's batch or of an older one writes nothing"
- **Delete it:** a record that Rama hands the keeper twice or late would change a batch's clock after
  stamps were made from it, or put the row back to a closed batch. Run: Rama did hand a record twice
  (B4; W3 line 23). Kept.

### The throw
- **Delete it:** an attempt whose clock is not kept would go on with no clock. `base` would then throw
  on a nil clock (`batch.clj:334`: `(long now-millis)`), so there is a throw either way; the plan's is
  the one that says what happened. Kept. What it costs: fault F6.

### The three pure functions in `gate/batch.clj`
- **Merge them into the dataflow:** they would then not be in the pure proof. The store's way is that
  the dataflow "only reads rows, calls pure functions and writes rows". Kept.

## Throughput: adversarial

**The frequent operation** of this part is the keep, once for a batch with things. I looked for a form
that does less work in total.

**Built: no read before the append.** Every attempt reads the clock, appends, and reads the row back.
A second attempt's record is a record of the row's batch, so the keeper writes nothing, and the
attempt reads the first attempt's clock back. It is right in every interleaving of "The keeper".

| | The plan | No read before the append |
|---|---|---|
| The attempt that keeps the clock | 3 seeks, 1 record, 1 row write | 2 seeks, 1 record, 1 row write |
| A later attempt | 1 seek, no wait | 2 seeks, 1 record more, the wait for the ack, about 3 ms |
| Weighted seeks, by the plan's workload | 1.498 | 0.499 x 2 + 0.001 x 2 = 1.000 |
| Records on the depot, which is never trimmed | one for a batch, and one for each attempt that failed before its clock was kept | one for each attempt, with no bound while a batch fails again and again |
| The seek that is saved, as measured | 0.12 ms (B4), once in a batch of 117 ms or more | |

It saves one seek for a batch, and never one for an act. It gives up the bound on what is kept for
good: `phases.md`, "PState write volume per source event must be bounded by inputs the application
controls", read here for a log that nothing trims. A batch that throws for ever (fault F6) would then
add a record at each attempt for ever. The plan's form is kept. Point 7.

**Looked for and not found:** a way for the attempt to be given the kept clock with the ack, which
would save the read back. A stream topology can give a value back to a client of a depot append
(`stream.md`, "Returning values to the client with ack-return>"). No line of the skill or of Rama's
pages gives a topology the ack's value of its own `depot-partition-append!`. Read, not run.

**Seeks into iterator reads:** none to be had; each read is one row at one key.

## Spec coverage: trace every operation and constraint

The part has one operation, A7. Each block below is a thing that the task names to break.

### A7, its first sentence, as ruling 16 (1) reads it

- **Source.** The contract, A7: "A read shows no fact of a batch that could still be tried again".
  Ruling 16 (1): "a read shows no fact that a second try could change."
- **Trace.** Batch 41, 8 things on partitions 1 and 3, 4 tasks, the gate's task is task 0. Attempt 1
  keeps c1 = 1,791,750,432,650 and makes its rows. Tasks 0, 1 and 2 commit and show them. Task 3's
  commit fails. A reader selects the place P on task 1 and is given the row R with the stamp
  base(c1) + turn. Attempt 2: line (ii) finds `{41, c1}`, reads no clock, makes the base from c1 and
  the same last stamp (set back), and each row again. At P it writes R. The reader is given no change
  by a select; an open push gets one more callback with the same value (B7.3, S5).
- **Fault tolerance.** A worker that starts again: the row and the record are durable; attempt 2 finds
  c1 (W3, B4). A topology that is tried again: the keeper writes nothing for a record of the row's
  batch. A write on several partitions that fails partway: Rama keeps or drops an attempt's writes on
  all tasks together; the rows that some tasks showed are the rows that the next attempt writes.
- **Races.** Two clients on one place: decided by the turns, the same on each attempt
  (`batch.clj:118-127`). Messages out of order: `gather` and each reader of a gathered value do not
  rest on the order (`batch.clj:138-148`; "Why the attempts come out the same", below).
- **Flaws found.** One, fault F5: the sentence fails across a module update that changes the code of a
  row. Repaired in the plan's words; no code can close it.
- **Verdict.** PASS, after F5's fix.

### A7, its second sentence

- **Source.** "a place that holds a fact never gives nothing."
- **Trace.** The place P holds the row R0 of a closed batch. Batch 41 replaces it. A select straight to
  `$$microbatch-facts` on P's task gives R0 until that task commits batch 41, and R from then on: a
  write puts a whole row at the key (`microbatch_gate.clj:206-207`, `:345`), and a task's rows of a
  batch become visible together (`12-microbatch-topologies.md:124`, read, not run; run on a probe's
  module: B7). An attempt that fails while it is processed shows nothing, so the select still gives R0
  (B2.1). A second attempt after a commit that failed replaces R by R.
- **Fault tolerance.** After a worker's restart a read from outside threw for about half a second (the
  batch probe, "Seen on the way"). A read that throws is no read that gives nothing; what the read path
  does with it is that part's (ruling 14).
- **Flaws found.** None, with this reasoning: no state of the place has no row once it has had one,
  since no write of this gate removes a fact's row (`write-row`, `:199-221`: every case is a `termval`).
- **Verdict.** PASS. The proof's check of it is on the first cluster.

### Ruling 17 (a): kept before any stamp is made from it, and before any task can show a row

- **Source.** Ruling 17 (a): "The clock is kept before any stamp is made from it, and only in a run that
  has things to decide." The task: "trace the order of every event of one attempt on every task, with
  the suspension at the acked append."
- **Trace.** One attempt, the first, of batch 41. Lines are of `module/microbatch_gate.clj`.

  | # | Where | What happens | What exists of batch 41 |
  |---|---|---|---|
  | 1 | tasks 1 and 3, step 1 (`:239-276`) | each thing: its turn, its standing, its messages; gathered into `$$questions` on the tasks that they name | internal writes of `:turns`; nothing visible |
  | 2 | all | step 1 is done on every task before step 2 begins (`microbatch.md`, "`<<batch` blocks") | |
  | 3 | task 0 (`:280-284`) | step 2 begins as one event; it goes to the gate's task and reads the last stamp L, `:closed-through` and the numbers | one event, on the gate's task |
  | 4 | task 0, new lines (i) to (iii) | the id 41; the row of `$$batch-clock`, `{39, c39}`; `kept-clock` gives nil | |
  | 5 | task 0, new line (v) | reads the clock c1; appends `{41, c1}` with `:ack`; the event gives the task back | one event, waiting. No other task has an event of this batch: step 2 has not come to `\|all` (`:292`) |
  | 6 | task 0, the topology `batch-clock` | the record is on the depot's partition and on its followers; the event reads `{39, c39}`, writes `{41, c1}`; its streaming batch commits and is replicated; the ack goes back | the kept row, visible and durable |
  | 7 | task 0, new line (v) | the event goes on: reads the row, `{41, c1}`; `kept-clock` gives c1; `clock-must-be-kept` gives c1 | |
  | 8 | task 0 (`:286-290`) | `batch/base` of c1, L and the numbers: **the first stamp**. The new last stamp is written | internal writes only |
  | 9 | every task (`:292-324`) | `:closed-through`; the reads; `batch/replies` with c1 and the base | |
  | 10 | every task, steps 3 and 4 | the decisions, the rows | internal writes only |
  | 11 | each task, the commit | the task's rows become visible | **the first row that anyone can see** |

  Row 6 is before row 8, and row 8 is before row 11.

- **Can anything of this batch run on any task while the gate's task waits for the ack?** No. Between
  rows 5 and 7 the batch has one event, and it waits. Step 1 is done everywhere (row 2). Step 2 is one
  event until `|all` at `:292`, which is after the wait. Read at `:279-292`: no branch of the block
  leaves the gate's task before that line.
- **What can run on the gate's task meanwhile, and does it write what the step read?** An event of
  `batch-clock`, which must. An event of the streaming gate for a layer of that task: it writes the
  streaming gate's PStates and reads `$$microbatch-facts` and `:closed-through` as committed
  (`streaming_gate.clj:112-124`). A query. None writes `$$microbatch-task` or `$$questions`. The three
  values that the step read before the wait are in variables, and `:last-stamp` is written only at
  `:290`, after the wait.
- **Can a later step read a clock from anywhere but the kept one?** A search of the store's source for
  each reading of a clock and each random source finds, in the module: `microbatch_gate.clj:285`, which
  the plan replaces; `streaming_gate.clj:175`, the streaming gate's own; and `seal/new-lock`. After the
  change the microbatch gate reads the clock in line (v) alone. The clock then goes on in one variable:
  to `base` (`:286`) and to `replies` (`:320`), which puts it in the reply about each act's answer
  (`batch.clj:399`); `with-replies` gives it to the item (`:483`); `decided` and `decide-or-send` hold
  the 10-second rule against it (`:615`, `:634`, `:685`); an act that meets takes it in its parcel
  (`:693-694`), and `decide-next` gives that parcel to `decided` (`:835`).
- **Is the 10-second rule held against the kept clock on every task where it is held?** It is held on a
  deciding task in step 3 (`:634`, `:685`, and `:615` through `:704`) and on a meeting task in step 4
  (`:615` through `:835`). Each takes `:clock` of the item or of the parcel, which is the reply's.
  Every thing that is no stray asks for its answer (`batch.clj:245`, `:301`), so every item has that
  reply.
- **Fault tolerance.** A failure before row 6: no stamp was made; scenario 1 or 1a. Between rows 6 and
  7, the worker of the gate's task starts again: the event is gone; the row holds `{41, c1}`; attempt 2
  finds it at line (ii). After row 8: scenario 2 or 3.
- **An attempt that Rama gave up, whose ack comes later.** Not run. If the phase times out while the
  event waits at row 5 and no worker goes down, Rama begins attempt 2. If the first attempt's event
  were still run when its ack came, it would read the row of batch 41 and make the same base as attempt
  2, by (K). So even then no second clock comes into being. That Rama drops such an event is Rama's,
  and every microbatch topology with a partitioner rests on it. Point 10.
- **Flaws found.** None, with the reasoning above.
- **Verdict.** PASS.

### The keeper, on every interleaving

- **Source.** The task: one record twice; an older batch's record after a newer one; two records of one
  batch with two clocks; the event run again after its write committed; the keeper failing the record;
  a worker restarted between the append and the read back; a change of leader.
- **The rule** (the plan, function 4): the row is written when the table holds none or a row of an
  older batch. Else nothing is written.

  | Case | The row before | The record | Written | The row after |
  |---|---|---|---|---|
  | a. Rama hands one record twice | `{41, c1}` | `{41, c1}` | nothing | `{41, c1}`. Run: B4, W3 line 23 |
  | b. an older batch's record after a newer one | `{43, c3}` | `{41, c1}` | nothing | `{43, c3}`. Derived from the rule; the pure proof holds the case |
  | c. two records of one batch with two clocks, R1 = `{41, c1}` first | none of 41 | R1, then R2 = `{41, c2}` | R1 | `{41, c1}` |
  | c. the same, R2 first | none of 41 | R2, then R1 | R2 | `{41, c2}` |
  | d. the event run again after its write committed | `{41, c1}` | `{41, c1}` | nothing | the same |

- **Case c, which wins, and can two attempts stamp from two clocks?** R2 exists only when attempt 2
  found no row of 41 at line (ii). Had attempt 1 been given its ack, R1 was processed and the row was
  `{41, c1}`, visible, and attempt 2 would have found it. So attempt 1 was never given its ack, and by
  the table above it made no stamp. From attempt 2 on, each attempt that makes a stamp read the row
  after its own record was processed, or found it at line (ii). The row of 41 does not change once it
  is written: only a record of a later batch replaces it, and only step 2 of a later batch appends
  one, after batch 41 is committed on every task (`microbatch.md`, "Guarantees"; run: B7, S2). So every
  stamp of batch 41 is from the clock of the record that the keeper processed first. This is the reason
  for the read after the append: attempt 2 offered c2 and can be given c1.
- **The keeper fails the record (W5).** What the attempt gets from the append: Rama's page says an
  exception, since by default the success of a colocated stream topology is "determined solely on the
  first attempt" (`14-depots.md:214`; read, not run). W5 did not see that as such: its keeper failed by
  a throw, which took the worker of the gate's task down. With the gate's task on the worker where
  batches begin, the next attempt came 1.4 s later (`walk.log` lines 27 to 37: no `:appended` note of
  attempt 1). With it on the other worker, the attempt waited until the phase timeout, 30 s
  (`walk-other-task-rama-lines.log:33`, `:timeout-secs 30`), and attempt 2 came 31.4 s after. What
  Rama does: it hands the keeper the record again, 235 to 267 ms later, and the row is written. What
  the attempt does in each of the three ends: the append throws, and the attempt fails; the append
  does not return, and the phase timeout fails the attempt; the append returns although the keeper
  failed, and the read back finds no row of the batch, so `clock-must-be-kept` throws. In none is a
  stamp made. How long the base stands still: 1.4 s or about 31 s here, and ruling 51 leaves the
  timeout unset.
- **A worker restarted between the append and the read back.** The event is gone. The record is on the
  depot or it is not. If it was processed, attempt 2 finds c1. If it is on the depot and not
  processed, scenario 1a: Rama hands it to the keeper at the restart; case c. If it never reached the
  depot, scenario 1.
- **A change of leader.** Read, not run. What the pages promise: a write to a PState or a depot is not
  visible "until they're durable on disk on the leader and durable on disk on all ISR ... followers"
  (`23-acid-semantics.md:112`), and a batch whose attempt meets "a leadership change during
  processing" is tried again under its id (`12-microbatch-topologies.md:116`). So a row that an attempt
  read is on each in-sync follower, and a new leader has it. What nothing here shows: any of it on
  more than one copy; what the waiting append is given when the leader moves.
- **Flaws found.** None in the design. The plan's line 183 on an id that comes again is right and said
  too small: fault F6.
- **Verdict.** PASS.

### The throw

- **Source.** The task: "is it reachable in any traced case that is not a fault? Could it loop for
  ever? What then?"
- **Trace.** `clock-must-be-kept` throws when the row, read after an ack that came back, is not of the
  batch. After an ack the keeper has processed the attempt's record. By the rule, the row is then of
  this batch, written by this record or by an earlier one of the batch, unless the row is of a later
  batch or the record was no map of two integers. The record is made by `clock-record`, so it is one.
  A row of a later batch needs a record of a later batch, which no attempt appends while this batch is
  not committed. So no case that is no fault reaches it.
- **The faults that reach it.** (1) An id of a batch that comes again: the microbatch topology deleted
  and declared again under its name, while `batch-clock` and its row stay. The row is then "of a later
  batch" for every new batch. (2) An ack that returns although the row was not written. Neither was
  seen in a run.
- **Does it loop for ever?** In case (1), yes: each attempt reads its clock, appends, is acked, reads a
  row of another batch and throws, and the id goes up only when a batch succeeds
  (`25-integrating.md:336`, as B1 quotes it). "A record that throws deterministically retries forever"
  (`microbatch.md`). What then: the base decides nothing; each throw starts the worker again, in the
  in-process cluster (B2.4), so every task of that worker stops for that time, both gates and the
  reads; the depot gains one record at each attempt. An operator sees Rama's ERROR lines and the row.
  Nothing of this build makes case (1) happen.
- **Flaws found.** Fault F6: the plan said the cost too small. Repaired.
- **Verdict.** PASS, after F6's fix.

### The partition of the clock's depot, and the gate's task

- **Is the append always to the partition of the task that reads the row back?**
  `depot-partition-append!` appends "to the partition of the depot represented by current task" (its
  docstring in the 1.6.0 jar, as B1 quotes it). Run: W1, 25 of 25 records on partition 0 with the route
  `:microbatch`, and 25 of 25 on partition 2 with a route of task 2. The keeper's event has no
  partitioner, so it writes on the task of that partition. The read back is the next line of the same
  event, with no partitioner between (the plan's line (v); W's source, `walk.clj:225-226`). No hop.
- **What is the gate's task, and is it the same on every attempt?** The task of the hash of the keyword
  `:microbatch` (`batch.clj:73-76`; `microbatch_gate.clj:280-281`). It rests on the keyword and on the
  module's number of tasks, and on nothing of an attempt. `:last-stamp` has lived there since the
  store's module was built.
- **After a module update, or a change of the number of tasks?** The module's number of tasks never
  changes (`microbatch_gate.clj:136-141`). The plan needs no more than that, and less: if the gate's
  task were another one day, the read of the row there would give nil, the attempt would keep a clock
  on the new task, and every attempt of that batch would read it there. The last stamp would be what
  breaks then, which is the store's and not this part's.
- **Verdict.** PASS.

### Why the attempts come out the same: each input of an attempt

- **Source.** The task: "go through each thing that the microbatch gate computes in an attempt and name
  its inputs."
- **Trace.** By step, from the code.

  | Step | Computed | Its inputs | The same on a second attempt, because |
  |---|---|---|---|
  | 1 (`:240-276`) | the turn | the things in the partition's order; `:turns`; the task's number; the number of tasks | the same things in the same order (B2.3); `:turns` is set back (B3.i) |
  | 1 | a stray's word; the declaration's word | the thing | pure |
  | 1 | the standing on arrival | this task's rows of the permission and the depot | set back; no step writes them before step 3 |
  | 1 | the messages | the thing, the turn, the two rows | pure (`batch.clj:273-312`) |
  | 2 (`:282-290`) | the base, the new last stamp | the last stamp; the numbers; the clock | set back; gathered by `greater`, which no order of arrival changes (`batch.clj:131-148`); (K) |
  | 2 (`:295-324`) | the replies | what was asked; the rows that were asked for; the clock; the base | gathered, and each reader of it sorts or looks for a turn: `answer-replies` takes the least turn (`:392`), `place-replies` and `lock-replies` take sets of turns (`:409`, `:427-430`); the rows are set back |
  | 3 (`:327-349`) | which acts meet; each decision; each row | the entries and replies of the task, sorted by turn (`batch.clj:553`, `:543`); the task's own places at each turn | from steps 1 and 2; the rows that earlier turns of this attempt wrote on this task, in the order of the turns |
  | 4 (`:352-366`) | the rows written | the parcels | from step 3. Two parcels never hold one key: two acts that name one place or one lock are both sent to meet (`place-replies`, `lock-replies`), and an act with a row that every task holds always meets (`who-meets`, rule 1) |
  | 4 (`:367-388`) | the decisions of the acts that meet | the acts, sorted by turn (`batch.clj:726`); the meeting task's own copy; **the other gate's row**; **drawn bytes** | the first two as above. The last two are not the same: below |
  | 4 (`:389-397`, `:212-214`) | the rows sent; **a new lock's bytes** | the pass; a random source | the bytes are not the same: below |

- **Does any input other than the batch's things, the gate's own rows as the batch began, and the kept
  clock reach a fact's row, or an answer's verdict or stamp?** Three do reach the attempt: the other
  gate's row, the drawn bytes, and a new lock's bytes.
  - *A new lock's bytes* are written to `$$microbatch-locks` at a key of the request (`:212-214`). The
    routed row of a new lock holds no bytes (`batch.clj:600`). No fact's row and no answer holds them.
    A verdict could rest on them only through the check of a seal, and that check is given the lock's
    row as step 2 read it (`batch.clj:832`), or drawn bytes for a lock of this pass (`:830`), never the
    bytes that step 4 will write.
  - *The other gate's row* is read only for an act of which neither this record nor the pass holds
    anything, into a layer that starts streaming (`batch.clj:747-756`). The second trace below.
  - *The drawn bytes* are given to the check of a seal for a lock that a request of the same pass
    makes. The first trace below.
- **An act that is sealed with a lock whose request is in the same batch.** A request Q, turn 4, asks
  for 2 locks. An act U, turn 9, names lock 0 of Q for a sealed value. Step 1: Q sends `:new-locks` and
  U sends `:locks`, both under the answer key of Q, to the task of Q's uuid (`batch.clj:254-263`). Step
  2: `lock-replies` tells U `:shared? true`, since a request of another turn will make the lock
  (`:429-430`, `:442`), and tells Q `:shared? true`, since another turn names a lock that it makes
  (`:436-439`). Step 3: both meet (`who-meets`, rule 3). Step 4, in the order of the turns: Q is
  accepted, and the pass holds the word `:new` at the lock's key (`:793`). U: the pass gives `:new`, so
  the check is given the drawn row (`:830`); `decision/seals` tries to open U's sealed bytes with 32
  random bytes, and fails. U is refused, on every attempt, with the word of a value that its lock does
  not open, the same stamp (the base plus 9) and the same digest. With the turns the other way, U at 4
  and Q at 9: U finds no row for the lock (`:832`, the rows that step 2 read hold none) and is refused
  on every attempt; then Q is accepted. In neither order can the verdict be "accepted" on one attempt,
  but for random bytes that open a seal with a tag of 16 bytes, which the plan says. No field of a
  fact's row comes of it: a refusal writes its answer and nothing else (`decision.clj:394-399`).
- **An act sent to the wrong depot.** Two cases. (1) Its act-id names the other depot: a stray,
  `:act-id-of-the-other-gate` (`declaration.clj:468`). It gets no answer and sends no numbers
  (`microbatch_gate.clj:252-257`); its digest is added to a set, the same write on each attempt. (2)
  Its act-id names this depot and its layer starts streaming, say `[:alice :microbatch u]`. Step 1: no
  permission row and no depot row on this task, so its standing is `:open` and it asks for its answer
  only (`batch.clj:305`). It meets (rule 2). Step 4, on the task of the hash of `:alice`:
  `reads-other-record?` is true, and the row at the depot place of `:alice` is read in
  `$$streaming-facts`. Attempt 1 finds none: `standing` comes to `making-act?`, whose first test is
  that the gate's depot is the depot that the layer starts with (`first_act.clj:90-92`), false here; so
  `:layer-not-made`. Between the attempts the streaming gate makes the layer. Attempt 2 finds the row:
  `:layer-belongs-to-the-other-gate`. Refused both times; the stamp and the digest the same; the word
  another. The 10-second rule is held before the standing (`batch.clj:615`), against the kept clock,
  so it gives the same on both. No fact's row comes of it. Only `answer-by-act-id` gives the word, and
  it is held to closing (`door_reads.clj:99-109`).
- **Flaws found.** None in the code as it stands. What the plan names as still differing is exact:
  three things, none in a fact's row, one in an answer's word.
- **Verdict.** PASS. Run on the other side: S4, 15 of 15 rows differ today, in `:stamp` and in no
  other field.

### What stays held to closing

- **Source.** Ruling 17 (b): "Only the clock goes on that depot. A lock's bytes never do (rule 5). The
  door's three reads stay held to closing". Ruling 16 (2) and (5).
- **Trace.** The record is `{:batch :clock}`, made by `clock-record` from the id and the reading. The
  plan reads "only the clock" as "nothing of an act and no lock's bytes", and the build session took
  that reading (the page, after ruling 51). The three reads and the streaming gate's question are not
  changed (the plan's table "What stays held to closing"; read at `door_reads.clj:99-109`, `:128-145`,
  `:158-167`, and `streaming_gate.clj:112-124`). `:closed-through` is written as today (`:291-294`,
  not changed).
- **Verdict.** PASS.

### A batch with nothing to decide, a batch of only strays, the first batch ever, a batch after a module update

- **Trace.** *Nothing arrived:* `$$questions` holds nothing, so the numbers are nil
  (`microbatch_gate.clj:284`). The new lines stand behind the numbers: no id is taken, no row read, no
  clock read, nothing appended; the clock is nil. `base` gives nil for no numbers before it looks at
  the clock (`batch.clj:332`). `closes?` takes the last stamp and `:closed-through` and no clock
  (`:353-360`), so `:closed-through` is written on every task when the batch before gave a stamp, as
  today. The loop of replies runs no time. Run: W4, 6 runs, no clock read, 2 records before and 2
  after. *Only strays:* a stray sends no message (`:252-257`), so the same. *The first batch ever:* the
  table holds no row; `kept-clock` of nil gives nil; the attempt reads c1 and appends; the keeper finds
  none and writes; the read back gives c1. Run: B4, "it held before: nil → written". *After a module
  update:* the ids go on and the row stays (W6: 45 before, 56 after). A batch with nothing reads
  nothing. A batch with things finds a row of an older batch and keeps its own clock.
- **Verdict.** PASS.

### The cost, counted here

- **Source.** The task: "count the seeks, writes and messages that a batch with things pays more,
  yourself."
- **The count.** The attempt that keeps the clock, all on the gate's task: line (ii), 1 seek; line (v),
  1 record appended to the depot; the keeper's event, 1 seek and 1 row write; the read back, 1 seek.
  **3 seeks, 2 writes (one record, one row), 0 messages between tasks.** The plan's count is the same
  (its lines 443-451). On a real cluster, two rounds of replication to the followers of that one task.
  Rama's own state for the keeper's progress is Rama's and is not counted, by the plan or here. A
  later attempt: 1 seek. A run with nothing: none, and one reading of the clock less than today.
- **What `:ack` waits for on a real cluster, by the pages.** Read, not run (`14-depots.md:206`): that
  the record is appended on the depot's partition "and replicated successfully", "plus waiting for all
  colocated stream topologies to process the data, including replication of any updated PStates". One
  stream topology consumes this depot, so it waits for that one event's streaming batch.
- **Verdict.** PASS.

### Ruling 17 (d), as ruling 49 changes it: the proof and the stop test

- **Source.** Ruling 17 (d): "The proof makes a real batch of the store's microbatch gate run a second
  time, with no hook in the module's source (the probe's B5.a), and shows that each fact's row of the
  second attempt is the same in every field, the stamp too, as the first attempt made it. If the build
  cannot make that hold on the store's own gate, it stops the repair".
- **Can it make a batch of the store's own gate run twice with no hook?** Run: S, on the store as it
  stands, 2 attempts, 1,088 ms apart. Run: T, 12 batches, 2 attempts each, in a cluster that was
  launched after another had been closed.
- **Does it learn what the first attempt made without resting on what it is meant to show?** Yes: the
  noting functions keep, in the memory of the proof's process, each row as the first function gave it.
  No table is read for the first attempt's rows. Before F1's fix the note of the first attempt was
  whole only by where the meeting task sits.
- **Is "the same in every field, the stamp too" checked for each fact's row and each answer?**
  `decide-or-send` gives the facts written at once and each parcel of rows; `rows-to-send` gives the
  rows of the acts that meet. Those are every fact row, every answer row and every lock row that an
  attempt makes (`batch.clj:649-708`, `:837-848`). S3: 15 rows for 8 things, 7 facts and 8 answers. The
  check's fourth line compares them key by key, bytes by content; its fifth compares each with the row
  that is selected after the batch; its sixth with `answer-by-act-id`.
- **Is the stop test exact, and could it pass or stop falsely?**
  - *The second attempt never ran:* one note. The first line fails. The plan: "Not the stop". The proof
    does not pass.
  - *The noting hand saw only one attempt:* the same.
  - *Two batches taken for two attempts:* before F2's fix this read as the stop. Now the first line
    fails, by the last stamps.
  - *One clock by chance:* the third line now holds that the second attempt began later than the clock.
  - *The first note not whole:* before F1's fix this read as the stop's second outcome. Now the throw
    comes after the one call that gives rows.
  - *The outcome that stops the repair:* the first line holds, and another clock was given, or a key
    stays from `rows-side-by-side`, or a selected row is not the row that both made, and it stays with
    the code as planned.
- **Does the plan say plainly what the proof does not reach?** Yes (its "What the map then says"): a
  commit that fails after one task showed its part; the moment between two tasks' commits; the first
  attempt's new lock bytes; a killed process; a move of a leader; more than one copy; and now a module
  update between two attempts. Ruling 48 leaves the window on the store's module to the batch probe.
- **Flaws found.** F1, F2, F3, F4, and the application of ruling 49 with the second cluster's own
  count. All repaired.
- **Verdict.** PASS, after the fixes.

### The hand-on: what a row of a microbatch layer may rest on

- **Source.** The split, scope item 4: "The rule of what a row of a microbatch layer may rest on, said
  so that the next part can hold each of its rows to it".
- **Is it exact?** The rule names three things: the things of the batch with their turns; the rows of
  this gate's own PStates as the batch began, with what earlier turns wrote; the kept clock, through
  the base and the 10-second rule. And what a row must not take from: a clock that the attempt reads,
  a random source, a PState of another topology, the memory of a process, the order of arrival, which
  attempt it is. Held against the table of inputs above: each input that is the same on a second
  attempt is one of the three, and each of the three that are not the same is in the second list. The
  number of tasks and the id of the batch are the same on each attempt and are in no list; a row that
  rests on them stays right.
- **Is the condition enough for the later parts' rows?** Held against the other plan's table "Every new
  row is the same on a second attempt" (its lines 380 to 394 before its check): the plain value, from
  the act's sealed bytes and a lock's row as step 2 read it; the who and the permission, from the act;
  `:level-since`, from the row before, the act and the batch's end, which is the base and the numbers;
  a key of the other order, from the marks row as the task held it when the batch began. Each is of
  the three. One condition was missing, for all rows: one version of the module's code (fault F5).
- **Verdict.** PASS, after F5 and F9.

### The explanations and the words

- **Each line that the part makes untrue has its replacement named?** Checked against the split's
  field `explanations_to_replace` and against a search of both trees. The store's page: "Closing"
  (`:385-400`), "A direct read of the tables" (`:173-177`), "The same word on every attempt of a batch"
  (`:190-196`), rules 5, 9, 17 and 19, the first paragraph, the rows of "The parts", "The stamp of a
  batch": each is in the plan's table, and each "Says today" was read at its line and is as quoted.
  `module.clj`, `microbatch_gate.clj`, `module/README.md`, the two proofs' maps: named. What was
  missing is fault F7; one wording against the tree's rules is fault F8.
- **Ruling 35.** The row F6 A of the table of 7 October: the plan changes its "Where it shows" side and
  says "His wording of the decision is not written again". The row F2 A: the same side. The decisions
  sections of `src-store/README.md` are not touched; of that file the plan changes lines 6 to 8, the
  tree's first paragraph. Kept.
- **The words.** `*batch-clock-depot`, `batch-clock`, `$$batch-clock`, `:batch`, `:clock`,
  `clock-record`, `kept-clock`, `clock-row-to-keep`, `clock-must-be-kept`, "the batch's clock", "to
  keep a clock". Each says what the thing is. A search of the rig, the server and Inland for the five
  compound names finds none. The batch probe's own names (`*clocks`, `keeper`, `$$kept`) are not taken.
  Each is in the plan's list for "Words a session made, Sid's to change".
- **Verdict.** PASS, after F7 and F8.

### The two plans in one walk

- **Source.** Ruling 38: "The one walk of a batch is changed by two plans: `base-for-a-reader` owns the
  batch's clock in step 2 and the depot and topology that keep it; `what-the-gates-keep` owns every
  other change of the walk." Rulings 41 and 45 add lines to step 2 beside the clock's.
- **What I saw, and when.** The other plan was read once while its check still ran, and again after
  that check was committed (`794bc2fd`). Its check changed one row of step 2 that bears on this part:
  the call at `:320` is now given as "`(batch/replies *read *found *now *base)`, with the batch's clock
  in the place of `*now` under whatever name the other part's build gave it".
- **Line by line**, `module/microbatch_gate.clj`:

  | Lines | This plan | The other plan |
  |---|---|---|
  | 231-234, the PStates | — | one more |
  | 261-273, step 1 | — | no read more; a question holds two fields more |
  | 282-283 | the same | "changes no line ... 282 to 290" |
  | 284 | the same | — |
  | 285, the clock | replaced by lines (i) to (v) | — |
  | 286, `base` | the same call, given the batch's clock | — |
  | 287-294 | the same | "not 291 to 294" |
  | 295-296 | — | the marks rows; the batch's end, from the base and the numbers, both bound before `\|all` |
  | 320, `replies` | the same call, given the batch's clock | one argument more, the batch's end |
  | 340-342, 363, 395 | — | step 3 and step 4 |

  No line is changed by both. The batch's end rests on the base, so on the kept clock, and is the same
  on each attempt.
- **`gate/batch.clj`.** This plan adds three functions and changes the docstring of `base`. The other
  plan changes `questions`, `reads-of`, `replies`, `place-replies`, `decided`, `decide-or-send`,
  `meeting-reads` and others, and adds `batch-end` and `marks-places-asked`. None is the same function
  but in a docstring's line.
- **`module.clj`.** This plan: the depot, the call of `batch-clock/add-to`, and lines 6 to 37 of the
  docstring. The other plan: lines 48 to 55 of the docstring.
- **Explanations.** Three paragraphs of the microbatch gate's namespace docstring are changed by both,
  in different sentences: `:15-24` (this plan: what it appends to and borrows; the other: "four
  PStates" becomes five), the step 2 of `:46-72`, and the docstring of `add-to` at `:223-228`. The
  table of `module/README.md` and several rows of the store's page are changed by both in the same
  way. Whichever build is second edits the text as the first left it.
- **The proof.** N15 of the other plan rests on this plan's hand, and its check has already written
  "That check has a cluster of its own (ruling 49)". It asks that the noting functions keep rows of the
  kind `:by-value` and tell a key that is taken out from a key that is not noted; this plan now says
  both (function 12, and item 7 of the batch).
- **Verdict.** PASS: no clash. Point 11 for the line numbers.

## The implied spec, checked off

Each state and edge case that the implied spec lists for this part, with where the plan holds it.

| The implied spec | The plan | |
|---|---|---|
| A7, "What today's code does": a second attempt reads the clock again and makes other lock bytes | the changed step 2; "What still differs", item 1 | covered |
| the same: each task holds `:closed-through` | "What stays held to closing"; the table of step 2, `:291-294` "The same" | covered |
| the same: the three reads and the streaming gate's question are held to closing | the same table | covered |
| the same: a select or a push straight to the table is not held | "What this part hands on" | covered |
| the same: what is the same on every attempt | "The walk, step by step, on a second attempt" | covered; traced above |
| the same: what differs, the clock, the lock bytes, two words | (K); items 1 and 2; scenario 14 for the 10-second word | covered |
| The table "What would have to be the same": whether accepted | scenario 14; the proof's item 6 | covered, after F3 |
| the same: which of two acts on one place wins | the proof's item 2 | covered |
| the same: the plain value; the who; the permission; the other order | the hand-on, "What each new row must keep" | covered, for the other part |
| the same: the stamp | (K) | covered |
| the same: the lock's bytes; the reason word | items 1 and 2 | covered |
| "Three things would still differ" | items 1, 2 and 3 | covered, exactly those three |
| The two things against the two ways | the proof; the stop test; "What the row before would then ask of each part" | covered |
| "What each way asks of a live read" | the hand-on to `read-path` | covered, after F9 |
| "What makes it matter most": the base's own facts that decide a read | ruling 16 (3), the read path's | not this part's |
| "Unsaid": the door's three reads | ruling 16 (2) | covered |
| "Unsaid": what a reader sees between the attempts | scenario 3, from B7.3 | covered, on the probe's module (ruling 48) |
| "Unsaid": a failure after one task has shown its part | "The window on the store's own module: left to the probe" | said plainly |
| Matrix 6, visible and not closed, no further write | "What stays held to closing"; the hand-on | covered |
| the same, "one read on two tasks" | the hand-on to `read-path` | covered, after F9 |
| Matrix 6, the next run begins | not changed | covered |
| Matrix 6, Rama runs the batch again, with the repair | scenarios 2 to 5 | covered |
| Matrix 6, closed | not changed | covered |
| P16, 1 to 6 | rulings 16 (1) to (6); the row for rule 9; the lines for the receipt | covered |
| P17, 1: a failure in-process after one task showed its part | B7, on the probe's module; ruling 48 | said plainly |
| P17, 2 and 3: what a select and a push see | scenario 3 | covered |
| P17, 4: what survives a failed attempt | "PState design", "Why a PState, and of another topology" | covered |
| P17, 5: durable and within reach before any task can show its part | "Kept before any stamp", "Kept before any task can show a row" | covered; traced above |
| P17, 6: the same things in the same order | (K), what it rests on | covered |
| P17, 7: the time | "Resource usage analysis" | covered |
| P17, 8: what cannot be run here | the map's "does not reach"; the lines for the receipt | covered |
| VA7, first sentence | the proof, "The first sentence of VA7" | covered |
| VA7, second sentence | the proof, the batch that runs twice | covered, after F1 to F4 |
| "What a pass does not show" | the map's lines | covered |

## The rulings, checked off

| Ruling | What it asks of this plan | The plan |
|---|---|---|
| 16 (1) | the first sentence read as "no fact that a second try could change" | held; F5 names the one exception |
| 16 (2), (5) | the door's three reads and the streaming gate's question stay held to closing | held |
| 16 (3), 17 (c) | the read path's | handed on |
| 16 (6) | the receipt says that the repair keeps a first part of what rule 9 left to Sid | held: the row for rule 9, and a line for the receipt |
| 17 (a) | kept before any stamp; only in a run with things | held; traced |
| 17 (b) | only the clock on the depot; no lock's bytes | held, in the reading that the build session took |
| 17 (d) | the proof; the stop | held, after F1 to F4 and ruling 49 |
| 17 (e) | the receipt says what was run where | held |
| 32, now 53 | the order of the builds | ruling 53 applied to the plan |
| 33 | the stop test, and what "the row before" would ask of each part, stated and not designed | held |
| 34 | the depot takes no act; a line for the receipt | held |
| 35 | a row that words Sid's decision is not written again | held |
| 38 | the plan names the lines of the walk that it changes | held: `:284-286` |
| 39 | the head comments gain this plan | two of four were named; F7; point 2 |
| 46 | a stream topology of its own | held |
| 47 | one row, the newest batch | held |
| 48 | the window on the store's module left to the batch probe | held, said in the map |
| 49 | a cluster of its own for the second attempt | applied by this check; run: T |
| 50 | the depot is not trimmed; a row proposed for the queue | held |
| 51 | the phase timeout stays unset; a line for the receipt | held |

## The plan's two runs: does each log show what the plan says

| The plan says | The log | |
|---|---|---|
| W1: 25 batches with things, 25 records, all on the gate's task's partition, on task 0 and on task 2 | `walk.log` line 51, `{0 25}`; `walk-other-task.log` line 50, `{2 25}` | shows it |
| W2: the id is a `java.lang.Long` | `walk.log` line 4 | shows it |
| W3: batch 43, attempt 2 found the kept clock and read none; the keeper was handed the record twice | lines 17 to 26; line 23, "it held `{:batch 43 ...}`" | shows it |
| W4: 6 runs in 3 s, no clock read, 2 records before and after | line 12 | shows it |
| W5: the keeper threw, was handed the record again 235 to 267 ms later; the next attempt 1.4 s after, or 31.4 s with the phase timeout | `walk.log` lines 27 to 37 (18,377.4 to 18,644.3; 18,374.5 to 19,731.0); `walk-other-task.log` lines 26 to 33 (18,849.9 to 19,085.3; 18,847.0 to 50,261.7); `walk-other-task-rama-lines.log:33`, "Microbatch processing phase timed out", `:timeout-secs 30` | shows it. Attempt 1 has no `:appended` note in either log: the attempt never came back from the append |
| W6: the row the same across the update; the id 56 after 45 | lines 41 to 44 | shows it |
| the cost: least 4.1, middle 5.2, most 29.2 ms | line 13 | shows it |
| S2: two attempts, 1,088 ms apart; 8 things answered in 2.1 s; 7 accepted, 1 refused | `store-second-attempt.log`: "the clocks differ by 1088 ms"; "2108 ms"; `{:accepted 7, :permission-does-not-stand 1}` | shows it |
| S3: 15 rows in each attempt under the same 15 keys | "15 keys in attempt 1, 15 in attempt 2" | shows it. Why the first note was whole: F1 |
| S4: 15 of 15 differ, in `:stamp` only; the 6 selected fact rows are the second attempt's | `{:stamp 15}`; "the same as attempt 2 made: 6 · the same as attempt 1 made: 0" | shows it |

## The plan's claims about today's code

Each was read at its file and line. Those that a trace above already cites are not listed again.

| The claim | Read at | |
|---|---|---|
| "The part changes the head of step 2, `microbatch_gate.clj:284-286`, and no other line of the walk" | `:284` the numbers, `:285` the clock, `:286` `base` | as said |
| "`base` gives nil for no numbers before it looks at the clock (`batch.clj:332`)" | `:332`, `(when numbers ...)` | as said |
| "each thing that is no stray sends one (`batch.clj:297`, `:302`, `:312`)" | the three lines | as said |
| "a stray sends nothing (`microbatch_gate.clj:252-257`)" | `(identity [] :> *messages)` | as said |
| "The reply about an act's answer carries both to the task that decides it (`batch.clj:391-400`)" | `:399-400` | as said |
| "the module sets no trimming option ... (`module.clj:33-35`)" | the docstring's lines, and the two `declare-depot` at `:73-74` with no option | as said |
| "the dataflow of `add-to` names ... `*microbatch-depot` (`:236`) and `$$streaming-facts` (`:383`)" | both lines | as said. That a depot which another file declares can be named as the target of `depot-partition-append!` is not run; the plan says so |
| "the first test of that form is that the layer starts with this gate's depot (`first_act.clj:61`, `:85`)" (the plan's line 533) | `:65`, the docstring's item 1; `:92`, the test | The claim is as said. The two line numbers were off; the plan now gives `:65` and `:92`. A correction of a citation, counted with no fault |
| "A fact's row names a lock by the act that asked for it and an index (`rows.clj:15-22`)" | the namespace docstring | as said |
| "One reading of the clock for a batch" (`:74-77`), "Closing" (`:84-96`), "When Rama runs a batch again" (`:98-103`), "Yielding" (`:105-109`) | each paragraph | as quoted |
| the store's page at `:5-10`, `:31-32`, `:86`, `:145-146`, `:173-177`, `:190-196`, `:235-236`, `:332-334`, `:378-383`, `:385-400`, `:415-421`, `:438-443`, `:454-461`, `:483-485`, `:492-501` | each | as quoted |
| the proofs' map at `:325-329` and `:577-600`; `test-store/README.md:41-42` | each | as quoted |

## Self-consistency check

Read again for a point that is passed while its own text names a hole.

- Each "not run" above is a limit that ruling 17 (e) and the contract's "A real cluster and a second
  node" leave outside this build, and the plan names each. None is passed as shown.
- "The keeper", case b, is derived and not run. The rule is one comparison of two longs, and the plan
  puts the case in the pure proof. Passed as derived.
- "Throughput" finds a form that pays one seek less, and keeps the plan's. The reason is a bound that
  the skill asks for, with the numbers side by side. It goes to the build session as point 7, and it is
  no hole in what the contract asks.
- "Kept before any stamp" passes with one line that rests on Rama: what becomes of an attempt that was
  given up. The trace shows that the kept clock holds either way.
- Faults F1 to F9 were each a FAIL of the point where it stands. Each is repaired in the plan, and the
  point is passed for the plan as it now stands.

PHASE_VALIDATION:minor-fail
