# Plan Validation: what the gates keep

**What this paper is.** The check of `PLAN-what-the-gates-keep.md`, the plan of the part
`what-the-gates-keep` of the read build: the contract's A1 to A6 for both gates, and its rule R7. The
contract is `../BUILD-reads.md`. "Entry N" is entry N of "Decisions of 11 and 12 October 2026" in
`src-store/README.md`. "Ruling N" is point N of "Rulings" in `PROGRESS.md`. "The implied spec" is
`IMPLICIT_SPEC.md`. "The store's page" is `src-store/softland/store/README.md`. A file name without a
folder is under `src-store/softland/store/`. "The other plan" is `PLAN-base-for-a-reader.md`.
**The stage.** Phase 2 of the Rama skill (`phase-2-plan-validate.md`), by its template
(`artifact-plan-validation.md`). The template is written for a new module; where a heading does not fit
a change of a module that stands, the heading says so.
**Who wrote it.** One fresh session (Opus 5.5), in the night of 11 to 12 October 2026. It did not write
the plan. It wrote this paper and the folder `plan-check-probes-what-the-gates-keep/`, and it changed
the plan where a fault is listed below. It changed nothing under `src-store/` or `test-store/`, and it
committed nothing.

**Verdict: minor-fail.** Eleven faults. Each is a thing that the plan left out, said in two ways, or
said of Rama without a run. None is a fault of the design. Each is fixed in the plan, and section 2
gives the plan's lines before and after. The forms that rulings 40 to 45 took stand as they are.

**Blocked.** Nothing.

**A ruling that cannot hold.** None was found. Each ruling that touches this plan was held against it
(section 8).

**The other plan of the walk.** It was in the folder. It was read as commit `2ea0fff9` has it (85,168
bytes, written 02:15): its design in short, its step 2 line by line, what it hands on, its proof, its
functions. Its own check was running while this paper was written, and that check first applies
ruling 49 to it. If that check changes the table of step 2, section 9 is made again.

**What this verdict rests on.** Ruling 33: this check may pass before the run of ruling 17 (d) is in.
Every row of a microbatch layer here is the same on a second attempt only given the kept clock. If that
run goes the other way, items 1, 6, 7, 8 and 9 of this part's scope are planned again.

**What was read.** The skill: `SKILL.md`, `phases.md`, `phase-2-plan-validate.md`,
`artifact-plan-validation.md`; of `stream.md`, `microbatch.md`, `paths.md` and `pstate-schema.md` the
sections that the plan cites. The contract, `src-store/CLAUDE.md` and `src-store/README.md`, whole.
`PROGRESS.md`, whole, and its rulings 46 to 51 when they came. This part's entry of
`DECOMPOSITION.json`. Of the implied spec: lines 1 to 519, the matrix, the proof and the latency
sections. The plan, every line. Whole, function by function: `act.clj`, `door/declaration.clj`,
`door/seal.clj`, `door/text.clj`, `gate/decision.clj`, `gate/first_act.clj`, `gate/batch.clj`,
`gate/stamp.clj`, `record/rows.clj`, `record/tables.clj`, `module.clj`, `module/streaming_gate.clj`,
`module/microbatch_gate.clj`; of `module/door_reads.clj` everything from line 40 on, with its three
queries; of `door/additions.clj` the walk of one fact; of `door.clj` the lines that a search for a store
key, a row and a permission finds. Both probes' results; of the push probe's two files the module of
each, and the lines of its log on a removal. Of the four proofs:
the lines that the searches of section 2, fault 1, find; they were not read whole.
**What was run.** One probe, once: `plan-check-probes-what-the-gates-keep/`, Rama 1.6.0 in-process, 4
tasks. Section 3. No proof of the store was run.

## 1. Points for the build session

1. **The pass rests on ruling 17 (d)** (ruling 33), as said above.
2. **A cheaper form exists, outside what the contract lets this build change.** Every accepted act with
   a fact under a plain key in a streaming layer pays one seek more than today, 5 where it paid 4, also
   in a layer that marks no key. The plan sought a read to ride on and found none; within the store as
   built that holds (section 12). One form does save it: a row for each streaming layer that holds the
   layer's last stamp and its marks, read once and written once by each act in place of the task's
   last stamp. It gives 4 reads again: 480 to 770 acts a second on one task by the store's arithmetic,
   against 384 to 616. It changes what a stamp is, "strictly increasing on its task"
   (`gate/stamp.clj:6-8`), to "in its layer", writes a row for each layer id that a refused act names,
   and puts the marks in two homes, since a microbatch layer needs them on every task. The contract
   changes two of the store's rules and no stamp. So it is not adopted and not a fault. It is a row for
   the queue, or a thing to weigh when the measures of C are in.
3. **Three names of the plan are also names in the rig's code.** A search of the rig's source finds
   `:by-value` 82 times, a function `value-digest` in its namespace of locks, and `read-values`. The
   plan says "None is from the rig", and its read list holds no file of the rig. The contract's own
   sentence says "by value". I have not verified where the names came from. The implied spec's test for
   R6 is "No word is the rig's", which a reading of the list against the rig's code would not pass for
   these three. A rename touches the hand-off that the plan of `read-path` has already taken, so it is
   the build session's to rule and not fixed here. `:members` is in the rig's code too, and is Sid's
   word.
4. **The skill's test for `|hash` trips, and the contract forces it.** In the base, a row of the facts
   by value goes to the task of its value's digest, so one value that very many places hold is one hot
   run on one task. The template says to fail a `|hash` whose key can be hot. A6 asks for "a run of
   neighbouring rows", a push covers one task, and R3 forbids a watch that is no run. The plan built the
   other placement, a run spread over k tasks with k kept for each value, and costed it: 1 + k seeks
   and k pushes for every read where it is 2 and 1, and one read more for each write. The build
   session's page already holds this for the receipt. It is passed here on the contract, not on the
   skill.
5. **A removal of a key that holds no row reaches an open push as a removal** (run, section 3, X3). The
   microbatch gate makes one such removal by design, when two acts of one batch write one place under a
   marked key. The hand-off now says so. The plan of `read-path` took the hand-off before this line was
   in it.
6. **After a worker was started again, Rama handed a stream topology every record of its partition
   once more** (run, section 3, X7): eight things whose writes were committed seconds before, each run
   again in order. The probe's topology has no check (a), so it made each write again. In the streaming
   gate each such event ends at check (a), and every write of this part is behind it. It also bears on
   the other plan's topology `batch-clock`, whose rule already takes a record twice.
7. **The table of the proofs was made longer by a search, not by a reading of the proofs whole.** The
   plan named 13 checks that the build makes untrue. This check found 14 more (fault 1). The four files
   hold 6,966 lines. So the build should expect more, and ruling 1 has it name each in the receipt.
8. **`:level-since` is a field that the row of one place has.** The template asks for a record class
   where rows of one table differ in their fields, and passes a field that can be absent on rows of one
   shape. It is read here as the second: one shape, as `:value` is absent in a retract's row. Ruling 43
   took the field.

## 2. The faults, and the fix of each

Lines "before" are of the plan at commit `b378e8f0`. Each fix is in the plan now; `git diff` of the
plan shows all of them and nothing else.

| # | The plan's lines before | What is wrong | The evidence | The fix, and where it is now |
|---|---|---|---|---|
| 1 | 885-899, the table of checks made untrue; 901-902 | It names 13 checks. Ruling 1 has each named, and the split's list of risks says "the plan lists each before the build, so that the counts can be read". 14 more are made untrue: each whole row that a check writes out (entry 7 puts who and the permission in it), the counts of the declaration, and what `questions`, `reads-of`, `replies`, `routed` and `meeting-reads` give. Line 901 calls them fixtures; a fixture is a call, and these are expected values | `test-store/softland/store/pure_proof.clj:401-409` ("the five shapes", "the twenty-one rules", the reasons), `:913`, `:916`, `:1215`, `:1223-1224`, `:1232-1233` (rows with no who), `:1393-1395`, `:1450-1464`, `:1467-1485`, `:1563-1564`, `:1604-1609`, `:1619-1630`, `:1884-1921`, `:2739` | 14 rows added to the table, each with its decision and what it is written again as; a paragraph that says how they were found and that the build may find more; line 901 now sends a written-out value to the table |
| 2 | 752, "`on-every-task?`: five keys" | `standing-on-arrival` changes (lines 166 and 476-477 say so) and is not in "Every function, in the order to build". A build from that list leaves it | `gate/batch.clj:204-214`: its first test gives `:stands` for a row that holds the act's permission. Left so, a holder of "see" is still refused, in step 3 by `decision/standing` (`batch.clj:616`), after asking for each of its rows | The function is in item 26 of the list, with what it gives and where the new test stands |
| 3 | 556-596, the costs; 934-968, the explanations | The greatest case of one act's hold of a streaming task is not stated, and the line of the store's page that states today's is not among the explanations. The split's scope asks for both (items 8 and 10) | The store's page `:204-217`: the event "does not give the task back". Today a fact that names nothing to replace reads no place (`decision.clj:162-176`); under a marked key it reads one | A paragraph after line 596: 256 facts under a marked key, 257 seeks more, 512 digests over about 33.5 MB, 512 writes of the other order. A row for `:204-217` in the table of explanations |
| 4 | 934-968 | The table says "every explanation" and leaves out two folder maps, three docstrings of functions that change, and the namespace docstring of `gate/batch.clj` on the data between the functions and the gate | `gate/README.md` (its row for `first_act.clj`), `record/README.md:25-27`, `streaming_gate.clj:141-146` and `microbatch_gate.clj:223-228` ("its four PStates"), `microbatch_gate.clj:199-203` (`write-row`), `decision.clj:212-224` ("the three store keys"), `batch.clj:42-61` | Five rows added |
| 5 | 798-874, the hand-off | A session that reads only the hand-off cannot know three things. (a) A push on a run can be sent the removal of a place that the run never held. (b) How to tell a row of the store's own keys in a run, which ruling 11 has the read path leave out. (c) That the functions which make a key test nothing: a thing that is no id can print with a space, and its key can name another place | (a) the run, X3, with the plan's own line 361-362. (b) implied spec F4. (c) `declaration/id-text` is `(str id)` (`declaration.clj:265-271`); the door's reads test first (`rows.clj:304-311`) | One more line under "What a read must not rest on", and a new last part of the hand-off, "What must hold before a key is made, and how a row of the store's own keys is told" |
| 6 | 290, step 6b | For an act that writes the marks and names something to replace, step 5 has read the marks row; step 6b reads it a second time | `decision.clj:170-176`: `places-needed` gives the place of each fact that names something to replace | "unless step 5 read it already" |
| 7 | 325, `meeting-reads` | "the marked-keys place always" can be read as: every act that meets reads the marks. That would be a seek for nothing for each act that meets for another reason | Line 376-378 of the plan: such an act has its `:since` from step 2 | The row says for which fact each place is read, and that no other act reads the marks there |
| 8 | 658-659; 361-362; 503-504; N13 at 922 | The plan rests on `NONE>` at a top-level key and marks it "Read, not run by the store". And it does not say what follows from taking out a key that holds no row | The run, section 3 | Line 658 gives the run, X2 to X7. Line 362 gives X3. S1 gives what was seen of a stream topology after a restart. N13 selects the key of each entry that went out, and appends one thing a second time |
| 9 | 609-629, "Partitioning efficiency" | The table holds the read alone. The template asks for each category of input; an act is the other one, and the write to every task is the one line that grows with the number of tasks | `artifact-plan-validation.md`, "Partitioning": "a single worst-case category is not a workload" | A second table, for one act at N = 1, 16 and 128: weighted seeks 4.95 at each N; weighted row writes 4.25, 4.26, 4.38 |
| 10 | N15 at 924 | It rests on the other part's hand and does not say that the hand must change. That hand notes what `decide-or-send` and `rows-to-send` give, and this part makes both give rows of the kind `:by-value`, a nil row among them | The other plan, "The proof of ruling 17 (d)", and its line 618-621: "If its build changes what `decide-or-send` or `rows-to-send` gives, the noting functions change with it" | N15 says what the hand must take and what the batch of that check gains, on that check's own cluster (ruling 49) |
| 11 | 320 | The line names `*now`. The other part's build replaces the clock at line 285 and may give it another name | The other plan's table of step 2, its row for `:295-324`: "The same call, with the batch's clock" | The row says: the batch's clock, under whatever name that build gave it |

Not a fault, and changed for the builder's sake: a note at the head of the plan (after line 14) that
the check changed it.

## 3. The run: the removal of a row at a top-level key

The plan's design rests on it: each fact that leaves a value's run is a row taken out of a top-level
map. The push probe had run it once in a stream topology (`push-probe/run-2.log:15-16`, a
`KeyRemoveDiff`). It was not run in a microbatch topology, not for a key that holds no row, and not in
an attempt that fails. So this check ran it in both.

`plan-check-probes-what-the-gates-keep/`: `deps.edn`, `src/probe/remove.clj`, `logs/remove.log` (97
lines), `logs/remove-rama-lines.log` (Rama's ERROR lines for the forced failure). One table for each
kind of topology, a top-level map from a String to a row of fixed keys, with keys of the plan's shape,
`"L :on 100 <value> <entity>"`. A row that is nil is taken out with
`(local-transform> [(keypath key) NONE>] ...)`, any other is put with `termval`. In the microbatch
topology each write goes by `|hash` of a String to a task that is not the one where its thing arrived,
as a row of the other order does in the plan. The times are milliseconds since the JVM loaded the
probe.

"X1" to "X7" are the cases of this run, in the order of its log.

| # | What was done | Stream topology | Microbatch topology |
|---|---|---|---|
| X1 | three rows put, two in the run of c1 | read back; the run gives two (3081 to 3124) | the same, found by a client with the String as partitioning key (4814 to 4916) |
| X2 | one row of the run taken out | the topology's own next read: nil. A select of the key: nil. The run: one row. A push on the run: `KeyRemoveDiff[L :on 100 c1 :b1]` (3453 to 3768) | the same (5269 to 5672) |
| X3 | a key that never held a row taken out | no throw; no row changed; **the push got `KeyRemoveDiff[L :on 100 c1 :never]`** (3772 to 4078) | the same (5719 to 6116) |
| X4 | one thing takes one row out and puts two | one event; all three show together (4083 to 4399) | one batch; the same (6165 to 6558) |
| X5 | a key taken out, then put by the next thing | the row is back; the push got a removal and then the new row (4403 to 4715) | the same (6611 to 7206) |
| X6 | two writes at one key in one event | out then in: the row is there. In then out: no row (4719 to 4736) | not run: the plan sends one row for a key |
| X7 | a batch takes a row out and puts one; its last step throws once | — | a select of the task's keys gave the row until the worker was down, and after it the second attempt's state: never the first attempt's. Inside, the second attempt read `{:stamp 140 ...}` at the key before it took it out again (7258 to 7892) |

```
 7258.4  INSIDE  microbatch batch 18 task 2 thing "7 ..." TAKES OUT "L :on 100 c1 :b1" · read before: {:stamp 140, :who ":alice"} · read after: nil
 7284.3  INSIDE  microbatch batch 18 task 0 : the last step THROWS on purpose, after the writes of this attempt
 7463.8  OUTSIDE MICROBATCH   a select of the task's keys now gives [:read-threw "CallbackException: Callback failure"]
 7735.4  INSIDE  microbatch batch 18 task 2 thing "7 ..." TAKES OUT "L :on 100 c1 :b1" · read before: {:stamp 140, :who ":alice"} · read after: nil
 7892.0  OUTSIDE MICROBATCH   a select of the task's keys now gives ["L :on 100 c1 :b2" "L :on 100 c3 :b9" "L :on 100 c4 :b1"]
```

What it settles for the plan. The removal works as the plan needs it in both gates. A removal is set
back with the rest when an attempt fails, so a second attempt takes out the same row. The range that
the plan gives for a run, from the run's text to the same text with `!` in place of its last space,
gave the rows of that value and no other, on a table that also held c2, c3 and c4. A row that a
microbatch topology wrote on the task of `|hash` of a String was found by a client that gave that
String as the partitioning key.

What it adds, that the plan did not have. X3: a push is told of the removal of a key that the run never
held (fault 5, point 5). And between 7710 and 7712 the stream topology, on the task whose worker was
started again, was handed all eight of its things again and ran each (point 6):

```
 7710.8  INSIDE  stream  task 0 thing "1 put" PUTS "L :on 100 c1 :b1" · read before: {:stamp 140, :who ":alice"} · read after: {:stamp 110, :who ":alice"}
 7711.3  INSIDE  stream  task 0 thing "2 out" TAKES OUT "L :on 100 c1 :b1" · read before: {:stamp 110, :who ":alice"} · read after: nil
```

What it does not settle. A real cluster; a killed process; a removal under load; the store's own
module, which the plan's checks N13 and N14 run. Each case ran once.

## 4. The counts, counted from the walk

The act: accepted, one sealed value, one entity, nothing named in `:replaces`. Counted here from the
dataflow; the plan's table (its "What one accepted act costs") matches in every cell.

**The streaming gate**, `module/streaming_gate.clj`.

| | Reads, by line | Row writes |
|---|---|---|
| Today | the answer (`:169`), the permission (`:184`), one range of locks (`:233`), the last stamp (`:241`): 4 | the fact, the lock again, the answer, the last stamp (`:245-260`): 4 |
| After, the key not marked | and the marks row, in step 6b: 5 | 4 |
| After, the key marked | and the place of the fact, in step 6b: 6 | and the entry in: 5. And the entry of the value before out: 6 |

With `:replaces` named: today 5 reads (the place, `:216`); after, marked, 6: the place is read once, in
step 5. A request for locks, a give, a setting, a list: `reads-marked-keys?` is false, so no read
more. A refused act: step 6b stands after the last check, so no read more. An act that writes the
marks: the marks row once (fault 6 made that true for the act that names something to replace).

**The seek that an act with no fact under a marked key pays.** One: the marks row, for each accepted
act with a fact under a plain key, in every streaming layer. The plan says so in its table and under
"Each read more". Section 12 gives why it stands, and point 2 the one form that would save it.

**The microbatch gate**, `module/microbatch_gate.clj` with `gate/batch.clj`. The store's page counts
the messages between two different tasks and leaves the batch's numbers out; counted the same way.

| Step | Today | After, key not marked | After, key marked |
|---|---|---|---|
| 1, on the act's task A | read the permission (`:266`). Send: the question for the answer to the uuid's task U, for the locks to the request's task R (2 messages; the entry and the own place stay on A) | the same; the question about the place holds `:marks-place` and `:k` | the same |
| 2 | U reads the answer; R reads the range of locks; the own place is not read. Replies: 2 messages | A reads the marks row, once for the layer in the batch: 1 read more | the same read; the reply about the place holds `:since` |
| 3, on A | no read. The fact is written at once. Parcels: the answer to U, the lock's home row to R (2 messages); the lock's whole row stays on A | the same | A reads its own place, the row before: 1 read more. Parcels more: the entry to the task of its value's digest, and the entry of the value before to its task: 1 or 2 messages |
| 4 | U writes the answer, R the home's row, A the lock's whole row | the same | and 1 or 2 writes of the other order |
| Reads / row writes / messages | 3 / 4 / 6 | 4 / 4 / 6 | 5 / 5 or 6 / 7 or 8 |

For an act alone in its batch the marks row is one read on the act's own task, which is ruling 18's
count as ruling 45 keeps it. For 1,000 acts of one layer on one task in a batch it is one read. For an
act whose facts are on k tasks it is one read on each of them.

## 5. The streaming gate's event

**Each row that is read more, and when.** Step 6b, after the seal, only for an act that no check
refused. First the row at the layer's marked-keys place, when the act holds a fact under a plain key or
under `:softland.store/marked-keys`. Then the place of each fact under a key that the row marks, when
step 5 did not read it. Nothing is read between the checks, so no refused act pays.

**The order of the reasons.** Early; then the standing, where the new word comes after the two words
of check (d): the row holds the act's permission, and only then is `act/allows` asked; then (e), (f),
(g), `who-may-write-fault`, what a store key does not take; then the seal. Traced with an act of two
faults: a holder of "see" whose act also names a stale fact gets `:permission-does-not-allow-acting`,
since the standing is before the places (`streaming_gate.clj:205-206`). A holder who names her
permission with `:see-and-act` where the row says `:see` gets `:permission-does-not-stand`: the bytes
differ, so the first test of `standing` fails before `allows` is asked.

**No hop, no yield.** Step 6b is `local-select>` on `$$streaming-facts`, the gate's own table on the
layer's task. The new case of the `<<cond` writes `$$streaming-facts-by-value` on the same task. No
partitioner is added, so the fact's row, the entry out and the entry in are one group
(`stream.md`, "When PState writes commit"). Run: X4. What the plan had not said: with no yield, the
greatest act now holds the task for 257 seeks and 512 digests more (fault 3).

**When Rama runs the event again.** After a commit: the answer is there, the event ends at
`streaming_gate.clj:170`, and step 6b and step 7 are not reached. So no entry is taken out or put
twice. This is not a small thing: the run showed a stream topology with no such check take out, on a
second run, a row that a later thing had put back (section 3, 7710 to 7711). After a discard: the reads
of step 6b are made again from committed rows, the same keys are written, and no reader saw the first.

## 6. The microbatch gate, traced with real batches

The base, 4 tasks. `:on` is marked since 1000: the marks row on every task holds `#{:on}` with
`:level-since` `{:on 1000}`. `d(x)` is the digest of the one text of x. `P` is the place
`":base :b1 :on"`, on the task of `:b1`, here A. The layer's meeting task is M.

**T1. One act alone.** Section 4.

**T2. Two acts of one batch on one place under a marked key.** P holds `:c0`, stamp 2000, so the entry
`":base :on 1000 d(:c0) :b1"` is on the task of d(:c0). Act 1, turn 1, writes `:c1` at P; act 2, turn
5, writes `:c2`. Both have P as their own.

1. Step 1, on A, for each: the permission stands; a question about P with `:own? true`,
   `:wanted? false`, `:marks-place ":base :base :softland.store/marked-keys"`, `:k :on`.
2. Step 2, on A: the marks row is read once. P's key is marked, so the read holds `:since 1000`. Both
   askers have P as their own, so P is not read. Two turns name P and its key is marked: `:shared?` is
   true, by the plan's change of `place-replies`. Today it would be false (`batch.clj:409-410`).
3. Step 3, on A, in turn: each act is shared, so each meets. For each, `own-places-wanted` gives P, and
   A reads P: the row of `:c0` both times, since neither act writes here. Each goes to M as one parcel
   with its row before, its locks, `{P 1000}` and the batch's end.
4. Step 4, on M, the pass in turn. Act 1: the row before is `:c0`, stamp 2000, later than 1000, another
   digest: out `... d(:c0) :b1`, in `... d(:c1) :b1`. Act 2: the row before is what act 1 wrote in the
   pass (`batch.clj:817-821`), stamp base + 1, later than 1000: out `... d(:c1) :b1`, in
   `... d(:c2) :b1`.
5. `rows-to-send` keeps the last row for each kind and key (`batch.clj:843-846`): the fact at P is act
   2's; `d(:c0)` out; `d(:c1)` out, since its last row is the out; `d(:c2)` in. Each goes by `|hash` to
   the task of its digest and is written there.

What is left: P holds `:c2`. The other order holds `... d(:c2) :b1` and nothing else of P. The out for
`d(:c1)` takes out a key that was never written: no change (X3), and a push on the run of `:c1` is told
of a removal (fault 5). No two writes of one key reach a task in this batch, so no order between
messages is needed. The plan's claim "for every key of the other order, in every batch, either one act
writes it, or all its writers are in one pass" holds: one key of the other order is one place and one
value, two writers of one key name one place, and two acts that name one place under a marked key meet.

**T3. A mark and a fact under its key, in one batch.** `:on` is not marked. Act X, turn 0, writes the
marks `#{:on}`; act Y, turn 1, writes `:c1` at P. X holds a fact whose row every task holds, so X meets
(rule 1) and asks nothing about a place. Y's home task reads the marks in step 2: no row, or a row
without `:on`. So Y gets no `:since`, is decided on A, and writes no entry. X in the pass:
`meeting-reads` gives the marks place; the row before holds no `:on`; `:on` is new, so it gets the
batch's end, base + (greatest position + 1) × 4 − 1. Y's stamp is base + 1, not later. So "every fact
with a later stamp is in the order, and no other" holds, whatever the two turns are. With the act's own
stamp in place of the batch's end, and Y's turn after X's, Y's fact would be later and not in the
order: that is what ruling 41 closes.

**T4. A mark and a fact in one act.** The act holds the marks fact, so it meets. The home task of its
place read the marks as the batch began: not marked. No entry. The key gets the batch's end; the fact's
stamp is not later. In a streaming layer: step 6b reads the marks row as it was before the act; the key
gets the act's stamp; the fact has that same stamp; not later.

**T5. A mark taken off and put on again, with facts replaced meanwhile** (rulings 20 and 43). Five
batches of the base, each with one act, the bases 10000, 20000 and so on, 4 tasks.

| Batch | Act | `:level-since` after | The facts by value |
|---|---|---|---|
| 1 | marks `#{:on}` | `{:on 10003}` | — |
| 2 | P := `:c1`, stamp 20000 | | in `:base :on 10003 d(:c1) :b1` |
| 3 | marks `#{}` | `{}` | nothing; the row stays |
| 4 | P := `:c2`, stamp 40000 | | nothing: no `:since`. The row of batch 2 is now of no current fact |
| 5 | marks `#{:on}` | `{:on 50003}` | — |
| | a read of what points at `:c1` | since 50003 | the run `:base :on 50003 d(:c1) ` is empty |
| 6 | P := `:c3`, stamp 60000 | | the row before has stamp 40000, not later than 50003: no out. In `... 50003 d(:c3) :b1` |
| 7 | P := `:c1`, stamp 70000 | | the row before has stamp 60000: out `... 50003 d(:c3) :b1`; in `... 50003 d(:c1) :b1` |

The two questions of the task. Does a run that a read may name hold a row whose fact is not current?
No: a read names runs under the stamp that the marks row holds now, 50003; the one row of a fact that
is not current is under 10003. Does a current fact written since the stamp lack its row? No: a fact
with a stamp later than 50003 is of a batch after batch 5, every task held `{:on 50003}` when that
batch began, so it got `:since 50003` and its entry. The base of a batch is later than the gate's last
stamp, which is the end of the batch before (`batch.clj:320-351`), so "later than the stamp" and "of a
later batch" are one thing.

The same with the mark taken off and put on inside one batch, acts X1 and X2, and a fact Y beside them:
both X meet; X1 leaves `{}`; X2's row before is X1's row in the pass, so `:on` is new and gets the
batch's end S'. Y was decided with the marks as the batch began, under the old stamp S, and its entry is
under S. After the batch a read names runs under S', which are empty, and Y's stamp is not later than
S'. It holds.

**T6. A retract and a replace under a marked key.** A retract: its question about the place holds
`:k`; the reply holds `:since`; the row before is read; it has a value and a later stamp: out. Nothing
comes in. A replace with `:replaces`: the row before is read as today, for check (e), and serves the
other order too. The same value again: one digest, no out; the entry is written over with the new
stamp and writer.

**T7. A second write of a new store key from two tasks in one batch.** Two acts write
`:softland.store/who-may-see` on the layer's entity; one arrives on the task of the layer, the other on
the task of another first fact. Each holds a fact whose row every task holds, so each meets. The pass
takes them in turn. Without `:replaces`: each is accepted, and `rows-to-send` sends the later turn's
row to every task. With `:replaces` of the row before the batch: the first is accepted, and the second
is refused `:replaced-fact-not-current`, since the pass holds the first one's row. The same for the
list and for the marks; for the marks the second act's row before is the first act's, so a key that
both hold keeps one stamp.

**T8. A second attempt of each of these batches.** Each thing that a row is made of was held against
the other plan's rule ("What this part hands on": the things of the batch with their turns; the rows of
this gate's PStates as the batch began and what earlier turns wrote; the kept clock through the base).

| Row | Made from | Rests on anything else? |
|---|---|---|
| `:value` | the act's sealed bytes and its lock's row as step 2 read it. A lock that a request of the same pass makes is given drawn bytes, and the use is refused (`batch.clj:806-811`) | no |
| `:who`, `:permission` | the act | no |
| `:stamp` | the base and the turn | the kept clock |
| `:level-since` | the row before at the act's turn, the act's set, the batch's end | the kept clock, through the base |
| a key of the other order | the layer, the key, the entity; the marks row as the batch began; the digest of the plain bytes, or of the row before | no |
| which acts meet | the questions of the batch, gathered by a function that does not rest on the order of arrival (`batch.clj:138-148`), and the marks rows | no |
| which row is sent for a key | the pass, in the order of the turns | no |

And a removal is set back with the attempt (X7). So each row and each key taken out is the same on
each attempt, given the kept clock.

## 7. The key of the other order

`"<layer> <key> <since> <value digest> <entity>"`.

**The run of one value never holds a row of another.** A key begins with a run's text
`"<layer> <key> <since> <digest> "` only when its first four parts are that run's: no part holds a
space. Checked for each part. A layer, a key and an entity print by `id-text`, which is `str`
(`declaration.clj:265-271`), of a readable keyword, whose pattern holds no space (`act.clj:226-229`), or
of a UUID. The stamp is digits. The digest is 64 hex characters. For each kind of value: a keyword and
a UUID through the door are the UTF-8 of their one text (`additions.clj:220`), a UUID as
`#uuid "..."` and never as its 36 characters, so the two texts of implied spec F8 do not meet; a string
with spaces, a number, a map and a value of 65,508 bytes are each 64 hex characters in the key; bytes
that are no value of the form are too, and no read can name their run, since `by-value-run` makes the
text itself and `read-value` gives `[:not-a-value]` for that row in the facts table. The two agree.
Two values share a run only by a collision of SHA-256 (ruling 42).

**The two ends, at the edges of Rama's order.** The range is from `P + " "` to `P + "!"`, the end not
in it. A string is in it exactly when it begins with `P + " "`: `!` is the next character after a
space, and both are one byte in UTF-8, so the argument is the same whether Rama orders keys by their
characters or by their bytes, also for a keyword with letters outside ASCII. Run on a table with four
values: section 3.

**The row's task in a microbatch layer, from the value alone.** The gate sends the row by `|hash` of
the digest, a String; a reader gives `rows/value-digest-of` of the value as the partitioning key. Run:
the microbatch cases of section 3 wrote by `|hash` of a String and were read with that String. The
marks row that a read needs first is on every task, so on that one.

## 8. The rulings, A1, A4, R7

**Each ruling that touches the plan.**

| Ruling | Kept? | Where |
|---|---|---|
| 1, the proofs | after fault 1 | "The proofs" |
| 2, 3, 7: the slot, "an act needs act", "see" stays "see" | yes | "PState Design" 3; "A4 in full" |
| 4, 5, 6: one permission; who may write; what a layer starts with | yes | "Not decided"; `who-may-write-fault`; `first-act/starts-with` |
| 10: every value kept, by a text that no other value has; a retract has no row | yes | "PState Design" 5; S6 |
| 14, its part here: the gate keeps what the lock opened | yes | "A1 in full"; `read-value` |
| 18, as 45 has it: five keys on every task; a mark holds from the batch after | yes | the walk; T3 |
| 19, 20: one fact; no fill; the rows stay; no stale row shown | yes | "PState Design" 4 and 5; T5 |
| 21: a retract refused; `:replaces` optional | yes | "R7"; T7 |
| 22: one class for each field, no collection | yes | "PState Design" 1 |
| 35, 39: Sid's wording not written again; the head comments | yes | "Every explanation" |
| 38: the clock's lines are the other part's | yes | section 9 |
| 40 to 45 | yes | the plan builds each as ruled |

**A1.** The plain bytes come from `seal/open`, which gives them today and whose caller drops them
(`seal.clj:169-174`, `decision.clj:308`). `opened` keeps them. Each reader of the old fields, found by a
search of `src-store/` for `:lock-act`, `:lock-index`, `holds-plain-value?` and `plain-value-of`:
`tables.clj:98-99`; `rows.clj:159-166`, `:182-200`; `decision.clj:138`; `batch.clj:207`;
`door_reads.clj:163`, `:171`. The plan's table names each and no other exists. The two callers of
`holds-plain-value?` ask it of a permission's place and `depot-of-layer` asks `plain-value-of` of a
depot's place; a fact under a store key never arrives sealed (`declaration.clj:380-383`), so no answer
changes. The right row for each fact: a sealed fact, the bytes that the lock opened; a fact under a
store key, the text of its value, as today; a retract, no value. What still finds a lock: the act in
the depot, which holds each fact's `:lock-id`, and the lock's own row, which names the fact that it
seals. No row of the facts table leads to a lock. The contract asks for exactly that ("the row names no
lock"), and the build session's page holds it for the receipt.

**A4.** Nobody raises her right by naming it: the row holds the text of the whole permission, slot and
all, and check (d) compares bytes. Each of the three forms, at each place that reads a permission's
slots, found by a search for each read of a slot: `act.clj:462` and `declaration.clj:362` (slot 1);
`declaration.clj:322-328` (slot 3 only for a count of 4: the plan changes it, else the ids above a
five-slot permission are not held to 256 characters); `declaration.clj:392-396` (slot 0);
`decision.clj:136-144`; `decision.clj:204-210` (slot 3 by `(nth given 3 nil)`, the same in both given
forms); `first_act.clj:86` (a count of 3); `batch.clj:204-214`. The plan's table names each. A making
act with a slot is no making act and is refused `:layer-not-made`. A give without the slot reads as
see and act; a give with it is taken by the third alternative. The depth: the slot is no permission,
so the engine's count of `:permission-id` inside itself is as today (`act.clj:179-182`, `:373-379`). A
holder of "see" cannot give, so no permission stands beneath a "see".

**R7 and the kept answers.** One write for a permission's place and for a depot: the two lines of
`store-key-fault` stay (`decision.clj:231-233`). A second write for the three new keys: they are in
neither line. Each retract under a store key: `decision.clj:230` gives the word for any store key.
Who may write the three: one function that gives nil, whose body is the other answer.

## 9. The two plans in the walk

| File | The other plan changes | This plan changes | Do they meet? |
|---|---|---|---|
| `module/microbatch_gate.clj` | step 2, lines 284-286: the clock at 285 gives way to the kept clock; 286 is the same call with it | the PStates (231-234); step 2 after 295, line 296 and line 320; `write-row` (199-221) and its two calls (363, 395) | at line 320 only, by a name (fault 11). The marks are read after `|all`, the clock is kept before it |
| `gate/batch.clj` | three new pure functions; the docstring of `base` | `on-every-task?`, `standing-on-arrival`, `questions`, `reads-of`, the replies, `with-replies`, `own-places-wanted`, `routed`, `decided`, `decide-or-send`, `decide-next`, `meeting-reads`; two new functions | no function in common. `batch-end` calls `last-stamp-of-batch` with the base that the other part makes from the kept clock |
| `module.clj` | one depot more; one call more; the docstring's lines on the depots | the docstring's lines 48-55; one line of the head comment | no |
| the proof's hand for a second attempt | it replaces `base`, `decide-or-send` and `rows-to-send` by functions that note what they give | it changes what the last two give | yes, and the plan did not say so (fault 10) |

Every new row of this plan against the other plan's rule: T8.

## 10. The implied spec, checked off

**"What holds for every operation of half A".** G1: each new check gives a word; each new row is made
by a function of `record/rows.clj`; `opened`, the digest and `marked-keys-of` do not throw; a nil row
for the `NONE>` case does not throw (run). G2: one event; one attempt. G3: section 5, T8. G4: the marks
are on the layer's task, or on every task. G5: section 5. G6: "Words a session made"; point 3. G7: no
kept data.

| Section | Each line that the implied spec lists | In the plan |
|---|---|---|
| A1 | a sealed fact; a fact under a store key; a retract; a refused act. The four invariants. Bytes that are no value; a value at the limit; the lock no longer found from the row; one copy of a lock's bytes | "A1 in full": each. A sealed value of 28 bytes opens to no bytes at all; the row then holds a value of length 0, which `read-value` gives as `[:not-a-value]`. It falls out and is not said; the build may give it a check |
| A2 | who is the holder; who is not the holder; a fact under a store key; a retract. One class each | "PState Design" 1; N10 |
| A3 | first write, second write, two at once, a retract, for each gate. No fact; the two words; the same word again. A setting in the making act; a setting for a layer of the other gate | "PState Design" 2; "R7"; T7; N11. The making act: line 129 |
| A4 | the six rows of its table; the two rows by gate; the four invariants; a third word; "see and act" named where the row says "see" | "A4 in full"; section 8 |
| A5 | the four things that the value's shape must hold; the seven states | "PState Design" 2, with 64 layers and each id within the limit; line 131-132 for what a gate does not check |
| A6 | the six things that it implies of a gate's write; the four rows by gate; the five states; the four edge cases | "PState Design" 4 and 5; the walks; S4 to S6; open point 1 for a marked store key; "What grows for ever" for a thing that many facts point at |

**The matrix.** 1, a layer: every act of its ten blocks is in "R7", "PState Design" 2 and the standing.
2, a permission: the seven blocks are the rows of "A4 in full". 3, a place: the plan's S6 covers each
write under a marked key; under a key that is not marked the gate does what it does today. 4, a marked
key: S4, S5, S6 and T3 to T6 here; what a read does in each state is the part `read-path`'s.

**"What the proof of this build must be able to show", half A.** VA1: N9. VA2: N10. VA3: N11. VA4: N12
and N16. VA5: N11, as far as the row; "a read uses the newest" is the read path's. VA6: N13 and N14.
The row R7: N11. What no check reaches is said in the plan: that a fact's row and its entries have no
moment between in a streaming layer, a failure in the middle of an event, a real cluster, a very long
run, a collision of the digest.

**F1.** Each check that F1 names has its row in the plan's table. Fault 1 is what F1 did not name.
**F2.** Each line that belongs to this part has its replacement named; fault 4 is what the plan's own
table left out beside them.

## 11. The hand-off

Read alone, as the plan of `read-path` reads it. With the three additions of fault 5, a session can
write a reader from it: it gives the tables, the key of each row and the way back to its parts, the
task of each row for each kind of layer, each field with the function that reads it, the five rows of
the store's own keys with what no row means, the run of a value with its two ends, and what a read
must not rest on. Each function that it names is in "Every function, in the order to build": items 2,
11 to 16 and 18. Nothing in it is said and not built. One thing it leaves to that plan on purpose: how
a read reaches every task of the base for a run across entities.

## 12. The template's lists

**Query topology.** None is added. Entry 3 has a read made "straight from the tables"; the contract's
picture says "No query of the store stands between".

**PState schemas.** The four tables with a String key on one task are four because their keys share no
structure: an act's uuid, a place of three parts, a lock's id, and now five parts in another order;
and in a microbatch layer the facts by value are placed by another part of the key than the facts. One
map for both would put a key of the other order into the range of whatever entity its second part
spells; a mark in front of the key would avoid that, and the placement by another part would remain.
No field of the class Object. Each row is a fixed-keys schema. `:level-since`: point 8. No row holds a
collection, so nothing is left to subindex; the tables are top-level maps, where each key is its own
entry on disk.

**Partitioning.** `|hash` of the value digest: point 4. `|all` for the three new keys' rows: each is
one row for a layer, at most about 17 KB, written when a member changes a setting. The table of
efficiency is there for N = 1, 16 and 128; with fault 9 it holds the acts too. Recomputed: the read is
2 seeks at each N, on one task; the weighted seeks of an act are 4.95 at each N. Nothing grows with the
number of tasks but the row writes of the rare write to every task.

**Topologies.** The microbatch gate is as built. The streaming gate is a stream topology as built: the
door's append waits for the answer. The one concern that this part adds to it is the other order. It
needs the stream: a tool whose append has come back must find its own fact in "what points at the
canvas", and VA6 asks that a place leave one run and join the other "with no moment that a read can
see between". No choice was made for the sake of a test.

**Production readiness.** Many clients at once: one event of a task at a time; one order for a batch.
A client that restarts: no state of a client is in this part. A worker that restarts: section 5, T8,
X7. Scale: the tables are top-level maps; one act writes at most 256 entries and takes 256 out. Writes
of the stream topology that are not the same when made twice: none; each is `termval` or `NONE>` at a
fixed key, and each is behind check (a). The stream topology writes on one task.

**Internal depots, flows between topologies, appends from a stream topology, memory of a task.** This
plan adds none.

**Minimality.** The simplest design that I can make for the contract: who and the permission as two
fields of the fact's row; the plain bytes in place of the sealed ones; three more entries in the one
table of store keys; one more alternative in the shape of a permission; and one more top-level map for
each gate, keyed by layer, key, value and entity, which the gate keeps level by reading the marks and
the row before. The plan is that design with four things more.

- *The stamp in the key, and `:level-since`.* Without them a mark that comes back shows rows of facts
  that are no longer current, or the gate must walk a layer; ruling 20 forbids both. Folded into the
  marks row, so no table more.
- *The digest.* Without it a key has no bound and a run needs an end mark that no value holds (ruling
  42).
- *"Two acts on one place under a marked key meet."* Without it two writes of one key reach a third
  task in no order (`batch.clj:138-144`: "The order of a gathered vector means nothing"), and a row
  could be put after it was taken out. One condition in `place-replies`.
- *`who-may-write-fault` and `starts-with`.* Each gives a fixed answer. R8 asks that the other answer
  be a small change, and rulings 5 and 6 ask for one function each.

Who and the permission a second time, in the entry: without them a read of a canvas of 5,000 places
that asks for the writer costs 5,000 seeks, 1.5 to 2.5 s cold, against one seek and 5,000 steps.

**Throughput.** For the act, which is the frequent thing. The marks row cannot ride on a read that is
made: the answer is found by the act's uuid, the permission by its holder, the lock by its request, the
last stamp by the task; none is the layer's. Two rows of one table are one seek only as neighbours, and
the marks row is no neighbour of a permission's unless it were copied to each holder, a write without
a bound. Kept in the memory of a process it would be a cache, and a second attempt could decide with
other marks. The one form that saves the seek moves the last stamp: point 2, built there, costed, and
outside the contract. The row before cannot be saved: the entry to take out is keyed by the digest of
the value before. In the microbatch gate the marks cost one read for a task and a layer in a batch.
For the read: one seek and a step for each place; nothing lower exists for a run.

## 13. Each operation and rule, by the template's block

### A1. A plain value in the facts table
- **Source**: "A gate opens each sealed value when it checks it. It now keeps those plain bytes in the
  fact's row, and the row names no lock. The act in the log is not changed: the door seals as built,
  and a gate still refuses an unsealed value that arrives."
- **Trace**: alice sends `{:e :note-1 :k :text :v "hello"}` through the door. The door seals the 7
  bytes of `"hello"` in its quotes to 35 bytes. The gate: `opened` gives `{:plain [7 bytes]}`;
  `writes` hands them to `fact-row`; the row holds the 7 bytes, `:by-act`, `:by-index`, `:stamp`,
  `:who ":alice"`, `:permission "[:alice :alice :alice]"`, and no lock. The lock's row is written again
  with the fact that it seals. The depot's record holds the 35 bytes and the lock id.
- **Fault tolerance**: a restart loses nothing, since nothing is in memory. A second run of the event
  ends at check (a). A second attempt of a batch opens the same bytes with the same lock. The row of a
  fact on another task travels inside the attempt.
- **Races**: two acts on one place in a streaming layer are decided in the depot's order; in the base
  in the order of their turns.
- **Flaws found**: none in the design. Fault 1: the proofs that check the old row.
- **Verdict**: PASS, after fault 1.

### A2. The writer with each fact
- **Source**: "A fact's row also holds who wrote it, and the permission that the act named."
- **Trace**: the system makes alice's layer: `:who :system`, the permission `[:alice :alice :alice]`.
  Both rows of the making act hold `:who ":system"` and `:permission "[:alice :alice :alice]"`. A
  retract by bob holds bob and his permission. `who-of` reads the text back to the id; a UUID reads
  back as a UUID, since the text is `#uuid "..."`.
- **Fault tolerance**: both are parts of the act; the same on every run.
- **Races**: none; the row is one write.
- **Flaws found**: none, with reasoning: both are in hand wherever `fact-row` is called
  (`decision.clj:362`), and a row on every task carries them on every task.
- **Verdict**: PASS.

### A3 and A5. The layer's setting, and what a layer builds on
- **Source**: "One more store key: a fact on the layer whose value is 'members' or 'everyone'. It takes
  a second write." "One more store key: a fact on the layer that holds an ordered list of layers. It
  takes a second write."
- **Trace**: alice writes `{:e :alice :k :softland.store/who-may-see :v :everyone}`. The door leaves
  the value plain, since the key is in `store-keys`. The declaration: the rule
  `:who-may-see-fact-holds-its-word`. The standing; check (f), the entity is the layer;
  `who-may-write-fault`, nil; `store-key-fault`, nil for this key. One row. She writes `:members`: the
  row is written over. `:secret`: refused `:who-may-see-fact-not-members-or-everyone`. On `:note-1`:
  refused `:setting-not-on-the-layer`. Sealed: refused `:store-key-sealed`. With no value: refused
  `:store-key-retract-not-built`. A list of 65 layers: `:builds-on-too-many-layers`; an id of 257
  characters in it: `:id-too-long`, by `ids-of`, before any key is made of it.
- **Fault tolerance**: the row is whole at a fixed key. In the base it is written to every task inside
  one attempt.
- **Races**: T7.
- **Flaws found**: none, with reasoning: each refusal falls out of code that is there; the two new
  things are the rule of the declaration and the entry of the table.
- **Verdict**: PASS.

### A4. What a permission allows
- **Source**: "A permission that was kept without the part reads as 'see and act', so the code carries
  both forms. A gate refuses an act under a permission that says 'see'".
- **Trace**: alice, under `[:alice :team :team]`, gives `[:bob :team :team [:alice :team :team] :see]`.
  Check (g): slot 3 is alice's own permission. The row at bob's place holds the text with `:see`. Bob
  acts and names it as the row has it: (d) passes, `allows` gives `:see`, refused
  `:permission-does-not-allow-acting`. He names it without the slot, or with `:see-and-act`: the bytes
  differ, `:permission-does-not-stand`. He gives carol a permission beneath his: his act is refused. A
  second give to bob that says `:see-and-act`: `:store-key-takes-one-write`. In the base the refusal is
  final in step 1 (fault 2).
- **Fault tolerance**: a row at a permission's place never changes, so the word is the same on every
  run.
- **Races**: a give of "see" and bob's first act in one batch: his standing is open, both meet, and
  the pass decides the give first if its turn is first; his act then finds the row and is refused with
  the new word.
- **Flaws found**: fault 2.
- **Verdict**: PASS, after fault 2.

### A6. A marked key, and one more order
- **Source**: "For a marked key the gates keep each fact in one more order, by value, so that 'what
  points at this thing' is a run of neighbouring rows. The mark takes a second write, since a mark can
  be taken off."
- **Trace**: T2 to T6, and the plan's S4 to S6 for a streaming layer.
- **Fault tolerance**: sections 5 and 6, T8; X7 for a removal in a failed attempt.
- **Races**: T2 for two acts on one place; T3 for a mark and a fact; T7 for two writes of the marks.
- **Flaws found**: faults 5, 6, 7, 8.
- **Verdict**: PASS, after those.

### R7
- **Source**: "Rule 13: the setting, what a layer builds on, and the mark of a key each take a second
  write; a permission's place still takes one. Rule 16: a gate keeps the plain bytes of a value in the
  facts table."
- **Trace**: section 8.
- **Fault tolerance, races**: as A3.
- **Flaws found**: none, with reasoning: `store-key-fault` names a permission and a depot by their own
  keys, so a new key earns no word there, and a retract of any store key earns one.
- **Verdict**: PASS.

## 14. Self-consistency

Each place above where this paper names something that does not hold is one of the eleven faults, and
each is fixed in the plan, or it is a point of section 1 that the contract or a ruling settles: point 2
(outside the contract), point 4 (forced by A6), point 8 (ruling 43). Point 3 is not settled; it is the
build session's.

PHASE_VALIDATION:minor-fail
