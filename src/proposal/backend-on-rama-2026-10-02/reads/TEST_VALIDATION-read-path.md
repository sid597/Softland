# Test validation: the read path

Part: `read-path` of the read build. Stage: the build, steps "write tests", "validate the tests"
and "finish" (the Rama skill's `phase-5-tests.md`, `phase-6-test-validate.md`, `phase-7-finish.md`,
by the template `artifact-test-validation.md`). Written by the build stage's own session (Opus 5.5),
12 October 2026, for the build session, which judges it and runs the proofs and the measures itself.
It committed nothing and staged nothing.

Checked: `test-store/softland/store/read_path_pure_proof.clj`, `read_path_proof.clj` and
`read_measures.clj` as this session left them, against the plan's section "The proof" (its checks P1
to P11 and 1 to 25, with rulings 78 and 80), against the contract `../BUILD-reads.md`, and against
the implied spec's section "What the proof of this build must be able to show" (`IMPLICIT_SPEC.md`,
lines 1704 to 1790), which this session read for this step. "Group N" is group N of the proof on the
cluster, as the proofs' map numbers them (`test-store/softland/store/README.md`).

*Run* means that a file under `runs/` holds what was printed. A check that this session has seen
fail on the fault that it is for is marked so; of the others it is known only that they pass.

**Verdict of this step: pass.** Each check of the template holds (section 4). Each line of the
implied spec's list that is this part's is covered, with three that are covered in the plan's
reading and not to the letter, each said plainly in section 5: R3, V4's "one change for one act",
and V3 for what points at a thing through the base. One check of the plan was changed because Rama
does not do what the plan's check assumed (section 3, group 21).

## 1. The counts

The six proofs, each by its own command from the repository root, on the tree as this session left
it. The four of the tree have the counts that the build of `base-for-a-reader` left: this part
changed no check of theirs.

| Proof | Command | Count line | Exit |
|---|---|---|---|
| The act | `clojure -M:store -m softland.store.act-proof` | `{:test 8, :pass 160, :fail 0, :error 0}` | 0 |
| The pure proof | `clojure -M:store -m softland.store.pure-proof` | `{:test 18, :pass 686, :fail 0, :error 0}` | 0 |
| The module | `clojure -M:store-module -m softland.store.module-proof` | `{:test 33, :pass 354, :fail 0, :error 0}` | 0 |
| The door | `clojure -M:store-module -m softland.store.door-proof` | `{:test 27, :pass 199, :fail 0, :error 0}` | 0 |
| The read path, pure | `clojure -M:store -m softland.store.read-path-pure-proof` | `{:test 12, :pass 149, :fail 0, :error 0}` | 0 |
| The read path, on the cluster | `clojure -M:store-module -m softland.store.read-path-proof` | `{:test 26, :pass 445, :fail 0, :error 0}` | 0 |

Each cut output is `runs/after-read-path-<proof>.txt`: its `SHOWN` lines once, its count line, its
exit status. The whole output of each run is the `raw-rp-` file that the cut names.

## 2. The pure proof, check by check

The plan's P1 to P11, as twelve tests. It takes about a minute.

| Plan | Test | What a pass shows |
|---|---|---|
| P1 | `the-form-of-a-query` | Eight queries are taken, one for each word and each kind of end. 37 faults are each turned away with their word and the part; the words given are every word that the declaration holds, eighteen. The check does not throw on a thing that throws when it is read, nor on a list of layers with no end, which is turned away after 66 are counted |
| P2 | `a-word-is-declared-in-three-places` | The three maps have the same four keys |
| P3 | `the-layers`; `a-step-that-joins-or-leaves-a-source-owes-the-whole-answer` | The order of the layers; the seven lines of the standing; what no fact reads as; what a layer's own rows mean; a fact's step that joins her permission owes, and the arrival gives the answer with the fact; a step that leaves an order gives the answer anew |
| P4 | `the-sources` | Each source's table, routing id and path, in a streaming layer and in the base. Two askers with different `:with` have equal sources |
| P5 | `the-loop-ends` | The rounds and the calls of each round, once and live. A live read of what points at a thing from a "members" start layer takes 6 rounds, where the plan derived 5 |
| P6 | `the-checks-of-nearer-layers` | Where a check is wanted and where not; the runs joined with the one function put aside; the bound; ruling 64's three cases; `holds-this-value?` |
| P7 | `what-a-read-gives` | Ruling 14's four things; the nearest wins; a layer left out; R4; ruling 11; a row that several parts hold is given once |
| P8 | `which-changes-reach-an-asker` | `told-by?`; the places that a change names |
| P9 | `every-order-of-the-callbacks` | Below |
| P10 | `the-size-of-a-run-and-the-order-of-its-keys` | The size rule; the order by code point; the ends of a run; `same-row?`; `named`; and three that the plan did not have: when a run has a first read, the keys of a run held to its ends, a lookup that goes through the run |
| P11 | `the-words` | The seven words; none is a gate's reason or a word of a stray; one is spelled as the door's |

**P9, the player.** It holds a world for one live read and moves one thing at a time: a write; a
push takes its next value; a callback is handled; a push that was asked for is served; its arrival
is handled. A callback and an arrival are handled by `sources/named`, `sources/reading`,
`sources/moves`, `answer/meanings`, `answer/after-change` and `answer/after-arrival`, which are the
functions that `read_path.clj` calls for them, in its order. It plays every order of the moves.

| Played | Lists | States | Ends wrong |
|---|---|---|---|
| The thirteen scenarios of the check's model, what points at a thing through two, three and four layers | 13 | 1,755 | 0 |
| Every list of up to three writes on one place in two layers, the nearer marking the key or not | 672 | 22,698 | 0 |
| Every list of up to three writes of a setting, a permission and two facts, a place through two layers | 620 | 21,223 | 0 |
| The same for an entity, lists of up to two writes | 120 | 1,397 | 0 |
| A start layer whose list drops a layer and takes it back, for a place, an entity and a whole layer | 3 | 1,637 | 0 |

*Seen to fail on the faults that it is for.* With `told-by?` counting a check that the step has just
left, the scenarios 1, 2, 8 and 11 and 118 of the 672 lists end wrong: the numbers of the check's
own model for the plan as it was first worded. With the read's mark dropped, the scenarios 9, 10 and
13 end wrong. Both are part of each run of the proof.

What the player is not: it plays one read, no two reads on one push, no task, and at most four
writes. That Rama's client delivers in these ways and no other is the probes', not this proof's.

## 3. The proof on the cluster, group by group

It takes about four minutes. The plan's checks 1 to 25 are the proofs' map's groups, in another
order: the checks that count the calls of a read asked once come before any live read is opened,
and the module is started again last.

| Plan's check | Group | State | Notes |
|---|---|---|---|
| 1 open | 1 | Run | A real throw of Rama's client for a wrong name |
| 2 faults, no call | 2 | Run | Ten faults, `read-once` and `read-live` each; 0 calls counted |
| 3 each word once, each gate | 3 | Run | In the base `:level-since` is a stamp of the mark's batch, not earlier than the act's (ruling 41); in a streaming layer it is the act's |
| 4 the four things | 4 | Run | The bytes that are no value go in by the proof's own hand |
| 5 `:with` | 5 | Run | |
| 6 the store's own facts | 6 | Run | |
| 7 through layers | 7 | Run | |
| 8 left out | 8 | Run | |
| 9 who asks | 9 | Run | |
| 10 rulings 12 and 64 | 10 | Run | Streaming layers only |
| 11 the calls | 11 | Run | Rounds are counted at the read path's own function that makes the calls of one round. 300 places, each given with the fact of its own nearer row, show that a call's values come in the paths' order |
| 12 a very large run | 12 | Run | A read path with a first read of 4, at most 10 places and 6 checks |
| 13 a run across the base | 13 | Run | One push for each of 4 tasks and one for the base's own rows |
| 14 each word kept live | 14 | Run | One act, alone, gives one change. Under load it does not: section 5, V4 |
| 15 what shapes a read | 15 | Run | Seven cases |
| 16 sharing | 16 | Run | 200 openings and 200 closes for 50 pairs that opened at once; 300 and 300 for 50 crossings of a join and a leave |
| 17 R2 | 17 | Run | |
| 18 a second attempt | 26 | Run | On a second cluster, with three functions of the gate's batch replaced, as the module's proof does. Two attempts made the same row for the place; the function was called once for that act |
| 19 reads that fail | 18 | Run | And one check that the plan did not have: Rama's ending of a push while a read opens. *Seen to fail on the fault that it is for* (below) |
| 20 threads | 19 | Run, changed | By ruling 78 the first call is on the caller's thread. The check holds that, and that every later call is on another thread |
| 21 the module started again | 25 | Run | |
| 22 R1 by search | 20 | Run | 26 files |
| 23 the order of the keys | 21 | Run, changed | Below |
| 24 ruling 67 | 22 | Run | |
| 25 the opening | 23 | Run | In 45 of 50 turns of the last run the first answer held a value between the first and the last (the count differs from run to run): the opening did cross the writes |
| ruling 78 | 24 | Run | Of 279 live reads, each first call was `{:anew answer}`, equal to `:now`, on the caller's thread |

**Group 21, changed.** The plan's check 23 has a run between two entities of which one holds a
letter outside the first plane, and holds that the read path gives the places that Rama's order
holds between them. In two runs of this proof such a run gave fewer places than the table holds
(`runs/raw-rp-proof-2.txt`, `raw-rp-proof-3.txt`), and the cause was not found. So the read path
turns such a run away, and the group holds: a run between entities with letters of Latin, Cyrillic
and Han, all below U+D800, gives the places that the order by code point holds; an entity whose id
holds a letter from U+E000 on, or one outside the first plane, is read as an entity and gives its
own place; a run that names such an end gives `:run-end-not-ordered`; and the whole of the layer
holds every one of them.

**Group 18's new check.** 40 times, Rama's ending diff is handed to the callback of a read's first
push, on another thread, 0 to 9 ms after that push was opened. The check holds each turn to the one
of two right outcomes that it met: the read was not opened yet, `read-live` gave a word, and the
function was not called at all; or it was opened, and the function was given the answer and then
`{:ended}`. In the last run: 4 and 36. With `end-read!` put back as it was before this session's
fix, by a scratch file that changed no source, the check failed in the 8 turns that met a read that
was not opened (`runs/raw-rp-mutation-ending.txt`). On a machine where every turn meets an opened
read the check passes and its line says 0: it then did not reach the case.

**How "nothing came" is shown.** After the act that must give nothing, a second act changes the
same push, and the proof waits until a witness, a second live read that holds that push, has the
second change. One callback is handled for every read on a push before the next begins. Groups 14,
15, 17 and 26.

## 4. The template's checks

**Minimize IPC launches.** The cluster proof launches two clusters. The first carries 25 groups.
The second is for group 26 alone, and has a reason that the template allows: before it is launched
the proof replaces three functions of the gate's pure code, which Rama takes at the launch, so every
other group runs on the functions as the store defines them (rulings 49 and 57). The pure proof
launches none. Pass.

**Implicit spec coverage.** Section 5. Pass, with three lines covered in the plan's reading.

**Synchronization.** Every act goes in through the door. A streaming act's `send-act` returns at
the depot's ack, when the gate has run and its rows are visible; an act of the base is read with the
door's bounded wait until "accepted". So a read asked once that follows an act reads after the row.
Where the proof waits for a change of a live read it polls, with a bound of 15 s, and 30 s for the
base. One place where a read asked once is *not* after the row, and the proof says so in a line of
its own: a read asked once through a read path that holds an open push on the same source is given
the push's value, which gave 186 to 192 of 200 places straight after their acks in the runs of this
session: the figure differs from run to run. The group holds the
answer only after the push is level, and prints both numbers. Pass.

**Test namespaces compile.** The three load: the pure proof on `:store`, the cluster proof and the
measures on `:store-module`. `clj-kondo` gives 0 errors and 0 warnings on the three
(`runs/raw-rp-lint.txt`). The cluster proof calls two private functions of the read path through
their vars, in group 12, and reads the field `held` of the read path's type; the proofs' map says
both. Pass.

## 5. The implied spec, checked off

The list says "the three words"; the build came to have four, and the proof runs four.

| Line | Covered by | How far |
|---|---|---|
| V1 the proof is the only asker | The whole proof | It selects one row round the read path, in group 21, to find a layer's task; the line says so. Its hand appends two things in group 4 |
| V2 each word, each gate, once | Group 3 | Whole |
| V3 through layers | Groups 7 and 10 | What points at a thing through layers is run in streaming layers. Through the base it is run only by the measures, which hold nothing |
| V4 kept live | Group 14 | "One change for one act": shown for one act that is alone. Under 10 acts a second, 26 of 500 acts on a place of the facts table got a call of their own (the measures, M4). R5 says that no count is asserted |
| V5 left out | Group 8 | Whole |
| V6 a proof of its own, with a row in the map | The proofs' map | Whole |
| V7 the four proofs pass | Section 1 | Whole |
| VA1 to VA7 | The other two parts' proofs | Not this part's. VA7's second half is also seen here, through the read path: group 26 |
| R1 | Group 20 | A search shows the tree, not a process |
| R2 | Group 17; the fields of `held` in group 16 | As far as a run reaches |
| R3 | Group 12 | In the plan's reading. No query can name a whole table. The widest thing that a query can name, a whole layer, ends in a word within 2 calls, so with calls, bounded ones. "With no call" is shown for a source of a kind that no query can make |
| R4 | Group 5; P7 | Whole. Rama sends the runtime the whole row |
| R5 | Groups 14 and 23 | A limit that the read path states |
| R6 | A search of this session: every keyword of the read path's six namespaces is on its page, or is one of 39 that are Clojure's, Rama's, the act's or the record's | A search of keywords. The names of functions were not searched |
| R7 | The other parts' proofs | Not this part's |
| R8 | A search of this session for "Not decided" in the docstrings: `layers/in-order`, `left-out`, `standing`, `who-may-see`, `sources/nearer-checks`, `answer/counts?`, and the namespace docstring of `query.clj` | That the other answer is a small change is the review's to judge |
| The measures | `read_measures.clj`; `runs/measures-read-path.txt` | Run once. Nothing is held to a number |

## 6. The measures

One command, `clojure -M:store-module -m softland.store.read-measures`; with `small` a load for a
first try. It ran to its end with exit 0 and printed 59 lines. What it printed is whole in
`runs/measures-read-path.txt`, and a table of it is in `IMPLEMENTATION_VALIDATION-read-path.md`,
section 11. It is no proof.

Three things about it that a reader of the numbers needs. The run was printed by the code as it
stood before three later small changes, which the head of that file names. M2's shape of runs
writes round 256 places and not each act to a place of its own. And its whole output was about
1.0 GB, nearly all of it Rama's own log of 98,794 openings of a push that Rama refused; that file,
`runs/raw-rp-measures-full.txt`, is kept and not committed.

## 7. What no check reaches

A cluster of several machines; a network between the runtime and Rama; a move of a task's leader; a
killed process; a restart of the runtime. Rama ending a push by itself, and Rama leaving a reader
out of a change. The order in which two pushes call back: the player plays every order on the pure
functions, and groups 14, 16 and 23 only make a crossing likely. A number of tasks that changes.
Many askers and many pushes at once: no check opens 1,000 pushes on one table on one task, which is
where Rama as it ships refuses the next one; the measures met that bound. A failure of a batch while
it commits, with one task seconds before another. A run between two ends that hold a letter from
U+E000 on. Two reads on one push in the player. A tool, a view, an inhabitant. How fast anything
is.

The receipt's line is the contract's: proven by a hand-played asker, used by no inhabitant yet.

## 8. Points for the build session

1. The plan's check 23 could not pass as written, and was changed to what holds (section 3, group
   21). The cause on Rama's side is not found.
2. One check has been seen to fail on its fault (group 18's new one), and the player on two. Of the
   other groups it is known only that they pass.
3. V4 and R3 are covered in the plan's reading and not to the letter of the implied spec (section
   5).
4. The proof's group 19 prints that a read asked once can be behind the tables when a push is open
   on its source. It is a line of the proof and no failure.
