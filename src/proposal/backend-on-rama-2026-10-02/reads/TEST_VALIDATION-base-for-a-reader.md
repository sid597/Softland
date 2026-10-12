# Test Validation: the base, for a reader

**What this paper is.** The build stage's check of the proofs of the part `base-for-a-reader`: what
they hold, whether they can fail, and what they do not reach. It follows the skill's template
(`artifact-test-validation.md`). The proofs are the two that this part changed,
`test-store/softland/store/module_proof.clj` and `pure_proof.clj`; the act's proof and the door's
proof are not changed and were run.
**Who wrote it.** The session that wrote the code and the proofs, in the night of 11 to 12 October
2026 (Opus 5.5). So it is not a fresh reader's check: the build session runs the proofs itself.
**Marks**, as in `IMPLEMENTATION_VALIDATION-base-for-a-reader.md`: "Run" names a run of this session
and its file under `runs/`; "Read" is read in the source; "Not run" is said where it is so.

## What was run, and the count lines

Each from the repository root, on the tree as this session leaves it, after the read path's session
had added its functions to `record/rows.clj`. Whole outputs: `runs/raw-bfar-<proof>-final.txt`; cut
to the `SHOWN` lines, the count line and the exit status: `runs/after-base-for-a-reader-<proof>.txt`.

| Proof | Before this part | After | Exit |
|---|---|---|---|
| act | `{:test 8, :pass 160, :fail 0, :error 0}` | `{:test 8, :pass 160, :fail 0, :error 0}` | 0 |
| pure | `{:test 17, :pass 671, :fail 0, :error 0}` | `{:test 18, :pass 686, :fail 0, :error 0}` | 0 |
| module | `{:test 31, :pass 333, :fail 0, :error 0}` | `{:test 33, :pass 354, :fail 0, :error 0}` | 0 |
| door | `{:test 27, :pass 199, :fail 0, :error 0}` | `{:test 27, :pass 199, :fail 0, :error 0}` | 0 |

Where the counts come from. Pure, 15 more: 11 cases of the three functions of a batch's clock, held
in the group of the batch, and one group more with 4 checks, a batch run as two attempts. Module, 21
more: one group more on the first cluster with 4 checks (a place never gives nothing); 2 checks more
in the group after the module is started again (the kept clock under a later id; the gate's throw by
hand); 2 more in the last check of the base (the depot of batch clocks; the kept row); and one group
on the second cluster with 13.

The module's proof was run four times whole on the gate as built, and held each time. It takes about
two minutes; the second cluster is about 15 seconds of that.

**No check was removed, and none was made untrue by this part.** Three checks of the module's proof
were written wider, with their sense kept: the comparison before and after the module's start now
also holds the row of the kept clock; the last check of the base also reads the depot of batch
clocks; and the group of the path of a lock in the base no longer takes the snapshot for that
comparison, which the new group after it takes. That is one line moved.

## Minimize IPC launches

**PASS, with the reason for the second launch.** The pure proof launches nothing. The module's proof
launches two clusters where it launched one.

The state that would interfere, named: the three vars `base`, `decide-or-send` and `rows-to-send` of
`softland.store.gate.batch`, which the gate's dataflow calls. To learn what a first attempt made, and
to make it fail, the proof puts functions of its own into those vars. Rama takes the function that a
var holds at a launch, so they cannot be in place for one group and out for the others on one
cluster. Ruling 49 asks that every other check run on the functions as the store defines them. So the
one group has a cluster of its own, launched after the first is closed (`module_proof.clj`, `-main`),
and the store's functions are put back in a `finally`.

Every other check of this part shares the first cluster. The new group there (a place never gives
nothing) writes one entity of its own and asks for its own locks, which it takes out of the hand so
that no later check seals with them.

## Implicit spec coverage

**PASS for what the implied spec asks the proof to show; one row of its table is not exercised, and
is named.** The implied spec, "What the proof of this build must be able to show", VA7: "In the base:
straight after an act that replaces a fact, a read of the place gives a row, never nothing. And no
read gives a fact that a second attempt changes. The first can be shown in-process. The second can be
shown only if a second attempt can be made there."

| Asked | The test | What it holds |
|---|---|---|
| VA7, first: a read of a replaced place gives a row, never nothing | Module, first cluster, `a-place-of-the-base-never-gives-nothing` | 297 selects of the place, straight from the facts table on the task of its entity, from before the append of the replacing act until its row showed: each a row, the fact before or the fact after. The row at first sight is the row after the closing. At that first sight the door's read by act-id gave nothing yet, which the line says and no check holds |
| VA7, second: no read gives a fact that a second attempt changes | Module, second cluster, `a-batch-runs-a-second-time` | Each row that the second attempt made is the row that the first made, in every field; the tables hold those rows; `answer-by-act-id` gives their verdict, word and stamp |

The implied spec's table "What would have to be the same on a second attempt", row by row.

| What a reader of a fact gets | Held by |
|---|---|
| Whether the act was accepted, at the edge of the 10 seconds | Module: the edge act's word is held to what the rule gives against the first attempt's clock; in the run it was at the edge and was refused on both attempts. Pure: with a clock one second later that act is accepted and every other answer keeps its word |
| Which of two acts on one place wins | Module: two acts on one place under a plain key, and two under a marked key, the same rows in both attempts; the place holds the later turn's value |
| The plain value | Module: `:value` of 14 fact keys, 7 of them values that a lock opened |
| The fact that wrote the row, the who, the permission | Module: those fields of each row; a person's row holds her and her permission |
| The stamp | Module: `:stamp` of each row that holds one: the 18 answers, the facts under 14 keys, and the 2 rows of the facts by value that came in. A lock's row holds none |
| The lock's bytes, which differ and which a reader of facts does not get | Module: the 2 keys of new locks are noted with no row in both attempts, and the table holds a row of 32 bytes for each. Pure: in two runs of one batch with one clock the bytes of the 2 new locks are the only thing that differs |
| The row of the other order | Module: 4 keys of the facts by value, two rows in and two keys taken out, the same in both attempts; no task holds a row under a key that went out |
| The reason word of a refusal: the word for a layer of the other gate | **Not exercised on a second attempt.** The batch holds no act into a layer of the other gate, by the plan's own line: every act of it is into the base, so that one call gives rows and every field must be the same. Read in the code: such an act is refused on every attempt and writes its answer alone. The first cluster's check of each gate telling the other gate's layers shows both words once each, with no fact written |

The plan's "What the check holds", line by line, is the group's checks 1 to 6 in that order, and
rulings 17 (a) and (b) are its last two. The plan's scenarios by hand:

| Scenario of the plan | Held by a test? |
|---|---|
| 1, 1a, 6, 11: a failure before the clock is kept; the topology fails the record; it does not answer | The rule, in the pure proof: of two records of one batch the first that is processed stays, in either order. On a cluster: not run here; the plan's probe ran them on a stand-in walk |
| 2, 3: a failure after the keep, before a row is written, or after some tasks have shown their rows | The proof's failure is between the two: every row is made, nothing is committed. A failure while a batch commits: not run on the store's module |
| 4: two failures in a row | Not run: the group forces one, because each one takes Rama longer than the last |
| 5: a worker starts again in the middle | Yes: the forced failure starts the worker again |
| 7: a batch with nothing to decide | Yes: a quiet 1.5 s adds no record to the depot. That it reads no clock is read in the code |
| 8: the first batch ever | Yes: the first batch of the second cluster kept its clock from an empty table. Pure: the row to keep when the table holds none |
| 9: Rama runs the topology's event again | Pure: a record handed over twice changes nothing; a late record of a closed batch writes nothing. On the cluster: the kept row is the same after Rama's module update; whether Rama handed a record again then is not known |
| 10: a module update | The kept row is the same and the next batch keeps under a later id. An update between two attempts: not run |
| 12: a move of a leader | Not run |
| 13: a batch in which every thing is refused by the declaration | The first cluster's refusal of an unsealed value alone in its batch is answered with a stamp, as before this part; every batch with numbers passes the head of step 2 |
| 14: the act at the edge | Yes, above |

**The pure proof's cases, against the plan's function 14.** Each of the three functions has its cases
and is also given 14 things that are no row and no record, a sequence that throws when it is read
among them: none throws. `clock-record` is given ids and readings that are no integer. The run of one
batch twice is its own group.

## Synchronization

**PASS.** Each write that a read follows is waited for, with a time limit.

- On both clusters the hand for the base waits until the gate has processed each thing that it sent,
  and then reads each answer in a loop of 30 s at most. An answer is readable only when its batch is
  closed, so each row that a check then selects is committed on every task.
- The hand for the batch that runs twice does not use Rama's count of processed things, which a
  failed attempt could move: it reads the answers of all 18 things in a loop of 60 s at most, and
  takes a read that throws as nothing yet, since a read can throw while a worker is started again.
  Only then are the notes, the kept row, the depot's log and the tables read.
- The kept row is read straight after that batch, before another batch with things is sent. The run
  that closes a batch has nothing to decide and keeps no clock, so the row is then that batch's. The
  check of its clock against the notes would fail if it were not.
- The count of the depot's records after the quiet time is read after a sleep of 1.5 s.
- One read is not waited for, by its purpose: the selects of the place that is being replaced, which
  must look while the batch is in flight. It ends when the new row shows or after 30 s.

Each forced wait has a limit: 60 s for the answers of the batch that runs twice; Rama's own 60 s for
the count of processed things; 30 s for an answer.

## Test namespaces compile

**PASS.** Both proofs load and run. Run: the pure proof on the alias `:store`, which holds Clojure
alone, so `softland.store.gate.batch` with its three new functions loads with no Rama on the
classpath. The module's proof requires two namespaces more, `softland.store.gate.batch` and
`softland.store.module.microbatch-gate`, both public.

## Can the proof of ruling 17 (d) fail

The task: "The check must be able to fail: it must tell two attempts of one batch from two batches,
and it must not pass when the second attempt never ran."

- *The second attempt never ran, or the throw was never made.* Then one note is there, or no throw.
  The group's first check holds `[2 1 ...]` for the count of notes and of throws and fails. Read in
  the check; no run of this session made that case.
- *Two batches taken for two attempts.* The first check holds that both notes were given the same
  last stamp and the same numbers. The gate's last stamp is set back for a second attempt and is
  later for a later batch. The same check holds that the batch before was given another last stamp,
  so the test is seen to tell: in each of four runs that part held.
- *The first attempt's note not whole.* The throw comes in the one call of `rows-to-send` that gives
  rows, after it noted them; every act of the batch is into the base, so one call gives rows. Run:
  the throw came in the call that gave 20 rows, in each run.
- *One clock by chance.* The third check holds that the second attempt began later than the clock:
  1,033 to 1,163 ms later in the runs.
- *The comparison blind to a difference.* Run, on the gate changed for one run so that a later
  attempt reads the clock anew (`runs/raw-bfar-mutation.txt`): `{:test 1, :pass 9, :fail 4,
  :error 0}`. Check 2 failed: the attempts were given two clocks, and the kept row held the first.
  Check 3 failed: two bases. Check 4 failed with 35 keys: all 18 answers, 15 facts and 2 rows of the
  facts by value. Of them 32 differed in `:stamp` alone; the row of the marked keys in `:stamp` and
  `:level-since`, so a stamp that the gate derives for a newly marked key moves with the clock too;
  the edge act's answer in `:stamp` and `:refused`; and the edge act's fact was noted in the second
  attempt only, where the act was accepted. The 14 keys of used locks, the 2 of new locks and the 2
  keys that went out of the facts by value hold no stamp and did not differ. And the check of the
  words failed: the edge act got `:accepted`. The gate was put back and compared byte for byte with
  the file as built. And in every run of the proof itself: the first attempt's note beside itself with one stamp
  moved gives that key with `:stamp`; with one fact left out, that key as noted in one attempt only;
  with a row that goes out left out, that key too, so a key that was noted with no row is told from a
  key that was not noted.

One fault of the proof was found by that changed run and repaired: three `SHOWN` lines said "one
clock" and "the same row in every field" in fixed words, whatever the checks beside them found. Each
now prints what was observed: the clocks that the two attempts were given, how many keys differ and
the first of them, and the word that the edge act got beside the word that the rule gives.

## What no check reaches

Said plainly, and in the proofs' own map (`test-store/softland/store/README.md`, "What the module's
proof does not reach").

1. **A reader across a second attempt.** The batch fails while it is processed, so no row of the
   first attempt was ever in a table. What is set side by side is what each attempt made, as the
   noting functions were handed it, and what the tables hold after the second. That a reader who saw
   a first attempt's row is then given no change was run by the batch probe, on a module of its own.
2. **A commit that fails after one task showed its part**, and the moment between two tasks' commits,
   on the store's module (rulings 17 (e) and 48).
3. **The word for a layer of the other gate, changing between two attempts.** Above.
4. **The gate's one throw inside the gate.** No run reaches it. Its function is called by hand.
5. **A failure before the clock is kept**, a record that the keeping topology fails, and that topology
   not answering, on the store's module.
6. **Two failures of one batch.**
7. **A module update between two attempts of one batch**, and the update that brings this part into a
   cluster that runs the store without it.
8. **A killed process, a move of a leader, a real cluster, more than one copy** of the kept row, and
   what the ack of the append waits for there.
9. **The first attempt's new lock bytes**: made inside the write, not learned.
10. **What the keep costs.** 4 to 14 ms between the clock and the call of `base` was seen in four
    runs on one machine with one copy. It is no measure, and the measures of the contract's section C
    are not this part's.
11. **The notes themselves.** They are the memory of the proof's process and outlast the worker only
    because the in-process cluster is one JVM.

## Points for the build session

1. The module's proof takes about two minutes and prints Rama's ERROR lines twice: at the module's
   start again, as before, and for the one throw on the second cluster.
2. Point 3 above could be shown only by a batch whose acts are into two layers, which undoes the
   reason why the first attempt's note is whole, and by making a streaming layer in the second
   between the two attempts, which rests on a time. I did not build it: the plan excludes it and its
   check passed that. If it is wanted, it is a change of the hand, not a line.
3. The read path's proof also makes a batch of the base run a second time, on a cluster of its own
   (read, not run by me). When it passes, it is a second run of what this group shows, by other code.

## Verdict

**pass.** Each check of the template passed after a walk through the tests, the suite passes with no
failure and no error on its last run, and the proof of ruling 17 (d) fails when the clock is read
anew.
