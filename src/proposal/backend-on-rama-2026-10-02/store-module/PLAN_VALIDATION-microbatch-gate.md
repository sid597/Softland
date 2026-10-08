# Plan Validation

Part: `microbatch-gate`, the second of the two parts of the store's module. Stage: Phase 2 of the Rama
skill, "Plan Validation". Written by: a fresh session (Opus 5.5), 8 October 2026, for the build session
"softland-11". The paper under check: `PLAN-microbatch-gate.md`, in this folder. This stage ran no
cluster and no code. It read the plan whole, the contract, the README, the implied spec (O2 to O10, C1
to C10, X31 to X42, X107, E4), the split, the first part's code and its three proofs where the plan names
them, and the skill's references and the local Rama docs for each claim about Rama.

**Verdict: minor-fail.** Eight faults, each closed by an edit of named lines of the plan, which this
stage made. No fault needs another design. One ruling of the build session, "one copy of a lock's
bytes", was applied to the plan; it holds, with one rule added for a case that it creates. Three things
go up to the build session or to Sid, because no edit of a plan settles them; they are in the last
section.

**Citations.** `P:n` is line n of the plan as it now stands, after this stage's edits. `C` is the
contract, `R` the README, `IS` the implied spec, as the plan cites them. `docs 12:124` is
`reference/rama/docs/12-microbatch-topologies.md`, line 124. `skill x.md` is a reference of the Rama
skill. Each statement here is *checked* (read in a source, and the source named), *derived* (it follows;
the steps are given), or *not run* (a behaviour of Rama read in a document; nothing was executed).

## The faults, and the lines changed

| # | Where in the plan | The fault | The edit |
|---|---|---|---|
| 1 | "The walk of one batch", step 3; "The order of the reasons" | An act that step 1 found without standing skipped rule 17: step 3 wrote its answer with the standing's word. The first part calls rule 17 before the standing (`streaming_gate.clj` 158–183), so an act with both faults got another word than in the streaming gate. Against C item 4, "the same checks", and IS C4, "the order of the checks is the order of the reasons". | `P:537–539`: step 3 calls `decision/refused-early` with the batch's clock first for such an act. `P:940–942` says why. A proof line was added (`P:1469–1470`). |
| 2 | "The order of the reasons" | "An act with no standing is refused before it asks, so it cannot make the gate read 256 places" was false for one kind: an act in a made layer whose `:who` holds no permission. It may come to stand by a give of the same batch, so step 1 lets it ask for every row and step 3 sends it to the meeting task. | `P:943–950` says which acts the sentence holds for, gives the bound (256 reads of places and 256 ranges of locks, rule 20), and why the gate cannot do less. Listed again at `P:1544`. |
| 3 | "Builder's rule 7 … what changes in the first part" | The plan changed `record/answer-read` and `record/lock-range-read` to take the routing id. The first part's pure proof gives `answer-read` a microbatch act-id with its layer and holds `[:microbatch key]` (`pure_proof.clj` 877–884). With the change that check gives nil. So "Every check of the first part still passes, unchanged" did not hold. | `P:749–758`, `P:1207`: the two functions stay as built; two are added beside them and the queries call those. `routing-id` moves to `record` (`batch` requires `record`). |
| 4 | "The design in short"; "The checks of one act"; "Partitioning efficiency" | The counts left out work that the design does: one write of `:turns` for each thing, and for each batch 2 reads on the gate's task and 1 of `:turns` on each task with things. Four kinds of input had no row in the tables. | `P:84–86`, `P:1091–1093`, `P:870–880`: said, with the bound, and with the cost of each kind that has no row. |
| 5 | Step 3, the parcel to the meeting task | The parcel did not carry the batch's clock and base. The meeting task decides with rule 17 and gives `base + turn`; it may hold no reply of its own. | `P:540–542`. |
| 6 | Step 2, on the gate's task | "Write the new `:last-stamp`" had no condition. Written in an iteration that gave no stamp, it moves with the clock, and every later empty iteration then writes `:closed-through` on every task, against the plan's own "never in a quiet time". | `P:507–509`. |
| 7 | The table of the first part's lines | Three explanations in files that the plan listed as not changed become false with this part: `record_pstates.clj` 32–35 ("the rows of one layer are on one task … each read routes by the layer"), the docstring of `declaration/stray-reason` 417–419, and the namespace docstring of `record.clj`. `src-store/CLAUDE.md`: an explanation changes with the code that it explains. | `P:1207–1210`, and the paragraph `P:1217–1222` on the two checks of the first part that rest on the order of the proof. |
| 8 | "Colocation"; "For the build to check first", 4 | "If they differed here, nothing would be decided wrongly" is not so for one read: X41's read of `$$streaming-facts` on the meeting task rests on `hash-by` and the hash partitioner giving one task for a layer. The first part's proof shows that they do. | `P:905–911`, `P:1574–1577`. |

Smaller edits, no fault of a decision: step 2's walk gives the task back between two keys (`P:524–526`,
IS X40); two findings added under rule 19 (`P:1294–1302`); the check-first list gained what this stage
found on point 1 and three points (`P:1559–1568`, `P:1579–1589`); a proof line for the closing of a batch
with nothing after it (`P:1470–1472`).

## The ruling "one copy of a lock's bytes": it holds, and is applied

The plan kept the whole row of a used lock twice. The ruling: when a value is accepted the bytes sit
beside the value, and the row at the request's home keeps which fact the lock seals and no bytes.

**The three things that could break it.**

- *The repeat of a request.* Checked in the plan: a repeat is told `:used` in step 2 and reads no lock
  row (`P:515–518`). `locks-of-request` after it gives each unused lock with its bytes. Holds.
- *The check that a lock seals one value.* Checked in the code: `decision/seal-fault` tests
  `record/seals-a-fact?` before `seal/opens?` (`decision.clj` 301–305), and `seals-a-fact?` reads
  `:seals-act` only (`record.clj` 193–196). The home's row keeps `:seals-act` and `:seals-index`. So a
  second value under a used lock is refused `:lock-already-seals-a-value` by the unchanged function, and
  the bytes are never asked of a row that has none. Holds.
- *Rule 16's opening before the accept.* A lock that is not used has its bytes at its home. The reply of
  step 2 brings them to the deciding task, which opens the value and, on accept, has the bytes in hand
  for the row beside the value. `decision/accept-writes` already gives that row whole
  (`decision.clj` 360–362). The home's row is a new whole row with no `:bytes`; one function is added to
  `record.clj` for it. Holds.

**The case that the ruling creates.** *Derived.* The row beside the value and the home's row have one
key, `"<layer> <request uuid> <index>"`, in one PState. When the value's entity and the request's uuid
hash to one task, they are one row. With two whole rows the order of the two writes did not matter. With
one of them without bytes it does: if the home's row is written last, the bytes are gone and the value
can never be opened. On one task that is every value; on four tasks, the proof's cluster, one in four.
The rule that this stage wrote into the plan (`P:332–336`, `P:547–552`, `P:559–563`, `P:570–574`):

1. no lock row of an accept is written in step 3; both go to step 4 as parcels, also a deciding task's
   own (a lock row is read in step 2 only, so no act of the pass needs it);
2. in step 4 a task writes every home's row of a lock before any row with bytes;
3. the meeting task sends the home's row of a lock before the row with its bytes; Rama keeps the order
   of one task's events to another (checked: skill formal-model.md, "Partition ordering", line 592).

The two rows of one lock always come from one task, because one act of a batch uses a lock (two that
name it are both on the meeting task, and one of them is refused). The proof can force the case: a value
on the entity that is its lock's request's uuid has the home as its task (`P:1451–1453`).

**What the ruling changes outside the gate, said plainly.**

- IS O9 says, as *derived*: "it gives the bytes in each state of the lock: not yet used; sealing a
  current value; sealing a value that is no longer current. The door needs the first. The read side, not
  built, needs the other two to open a value." For a microbatch layer `locks-of-request` now gives the
  first only; a used lock comes with its way and no bytes. The read side finds the bytes beside the
  value, two reads on one task. The ruling overrides a derived line of the implied spec, not a line of
  the contract. In the plan at `P:1539–1543`.
- Whether a lock is given with bytes can change inside a batch that is not closed (`P:337–341`). The
  bytes themselves never change.
- F9's third limit. The ruling's reason was that the bytes count once. They do; the gain is about 35 of
  280 bytes for a used lock, because the home's row stays and most of a lock's row is its key and the
  fact that it names. "2 times" now holds from values of about 90 to 123 bytes, against 102 to 140
  before and 55 in a streaming layer (`P:1011–1017`). *Estimated from the sizes of the fields, as the
  plan's numbers are; not measured.*
- A rebuild or a move of rows: the row with the bytes does not hold the value's entity (`P:1294–1299`).

Lines of the plan changed for the ruling: 54, 80–86, 165–171, 296–347, 383, 547–552, 559–563, 570–574,
806, 976–978, 1008, 1011–1017, 1043–1045, 1049–1050, 1085–1086, 1101–1102, 1123, 1149–1157, 1207,
1294–1299, 1322–1325, 1414–1417, 1446–1453, 1466–1468, 1489–1492, 1535–1543, 1584–1589, 1619–1621.

## What this stage was asked to attack

### 1. The turn: the same on every attempt

*Checked:* "Each attempt for the same microbatch ID processes the exact same set of data from the
depots" and "each microbatch attempt starts with the PStates at exactly their state after the previous
microbatch ID" (docs 12:116). The fragment emits "per task, in depot append order" (skill microbatch.md,
line 13). The number of tasks cannot change (docs 19:483).

*The trace.* Four tasks. Fact `f0` is current at the place P = (`:base`, `:doc-1`, `:title`), on task 1.
Act A: one fact on `:doc-1`, `:title`, replacing `f0`; its first entity is `:doc-1`, so the depot puts
it on partition 1. Act B: first fact on `:doc-7` (task 3), second fact on `:doc-1`, `:title`, replacing
`f0`; partition 3. One batch takes A as the third thing of partition 1 and B as the first of partition 3.

- Attempt 1. `:turns` on task 1 goes 0, 1, 2; A's position is 2, its turn 2 × 4 + 1 = 9. B's position is
  0, its turn 0 × 4 + 3 = 3. Step 2 on task 1: P is named by A, as its own, and by B, not as its own, so
  it is `:shared` (`P:519–520`); the row `f0` is read once. Step 3: both meet (rule 3, `P:582–583`); A
  carries P as read at its turn, B carries P from the reply. Step 4 on the meeting task: B first (turn
  3): `f0` is current, B is accepted, P holds B's fact with the stamp `base + 3`. Then A (turn 9): the
  pass lays B's row over A's carried row; A is refused `:replaced-fact-not-current`.
- A second attempt, after a worker of task 2 stops in the commit. Rama sets `:turns` back to its value
  after the batch before, and gives the same things in the same order. The positions are 2 and 0 again,
  the turns 9 and 3, the reply `:shared`, the order on the meeting task B then A. B wins again. The base
  differs, because the clock is read again; no read gave the first stamp (point 4).

*What does not come out the same.* The winner is a function of the order, the record before the batch,
the batch's clock (rule 17) and, for X41 only, the other gate's record. The first two are the same on
every attempt, shown above. If B carried a stamp 10 seconds ahead of the clock, to the millisecond, B
could be refused on one attempt and pass on the next, and then A wins. The plan says so (`P:1527–1534`).
This is not a fault that a plan can close: rule 17 holds an act against "the gate's wall clock", rule 6
wants "the same winner on every attempt", and Rama sets back every PState in which this gate could keep
the first attempt's clock. It goes up; see the last section. **PASS** for the order, which is what the
plan builds.

### 2. Whole or nothing across tasks

*The trace, one fact stale.* Act C: `:who` `:system`, three sealed values, on `:e1` (task 0, its first
entity), `:e2` (task 1) and `:e3` (task 2); the third replaces `g`, and the current fact of that place is
`h`. Its answer's uuid hashes to task 3, its locks' request to task 1.

- Step 1 on task 0: the permission's row is there; questions to task 3 (answer), task 1 (locks, and the
  place on `:e2`, not wanted), task 2 (the place on `:e3`, wanted). No row is written but `:turns`.
- Step 2: task 3, no answer, `:yours`. Task 1, three lock rows, none shared. Task 2 reads the place:
  `h`. Replies to task 0.
- Step 3 on task 0: C meets no act. `decision/places` finds `h` not written by `g`:
  `:replaced-fact-not-current`. `decision/writes` for a refusal gives the answer and nothing else
  (`decision.clj` 394–396; the gate leaves out the last stamp). One parcel, the answer's row, to task 3.
- Step 4: task 3 writes the answer. Tasks 0, 1 and 2 are sent nothing of C. The three locks keep their
  rows, with bytes, free (rule 15).

No row of a fact or a lock of C reaches any task. *The same act with all three good:* step 3 accepts;
the fact on `:e1` is written at once on task 0; parcels: the fact on `:e2` with its lock's row to task 1,
the fact on `:e3` with its lock's row to task 2, the first lock's row to task 0 itself, three home rows
to task 1, the answer to task 3. A worker of task 2 stops before the commit: the attempt is dropped on
every task, also the fact that task 0 wrote "at once", because the priming of the next attempt sets every
PState back (*checked:* docs 12:120, 23:37; skill formal-model.md §6.2, "Txn-Preserve"). **PASS.**

### 3. A pair of acts of two kinds that depend on each other

The plan decides an act where its first entity lives, or on its layer's meeting task. This stage looked
for a pair, one of each kind, in one batch, where the one that stays depends on the one that goes.

- *A give and an act under it.* The give holds a `:permission` fact: meeting task (rule 1). The act
  under the new permission finds its `:who`'s place empty on its own task: meeting task (rule 2). Both
  are there; the pass decides them in turn order. Give first: the act stands. Act first: refused
  `:permission-does-not-stand`. No act that stays depends on the give: an act stays only when its
  permission's row was there before the batch, and "a store key takes one write for a place" (C rule
  13), so no give of the batch can change that row (`decision.clj` 230).
- *A making act and an act into that layer.* Before the making the class place is empty on every task,
  so every act into the layer goes to the meeting task (`P:484–485`). They ask for no row; none is
  needed, because nothing of the layer is in the record. Making first: the act is decided against the
  rows of the pass. Two makings: the second finds the first's rows in the pass and is refused at (g).
- *A request for locks and an act sealed under one of them.* The lock's home says `:shared` to both
  (`P:521–522`). Request first and accepted: the lock has no bytes until the write, so the pass gives
  `decision/seals` bytes drawn for the test, and the use is refused `:lock-does-not-open-the-value`.
  Drawing random bytes is right: fixed bytes could be guessed, and a forger's value would then be
  accepted under a lock whose real bytes do not open it, against rule 16.
- *An act that stays and an act that goes, on one place.* Own for both (one first entity): the earlier
  turn that stays writes at once and the later reads it at its turn; a later turn that would stay goes
  with the earlier one (rule 4). Own for one only, or for neither: `:shared`, both go. This stage walked
  the six orders of (stays, goes) × (earlier, later) × (own, not own) and found each covered.

Each is the same on every attempt, because each rests on the turn and on rows as they were before the
batch. **PASS.**

### 4. Closing

*The claim, checked.* "After all PStates on a task have been checkpointed and replicated, their external
versions are updated … (though changes on different tasks may become visible at slightly different
times)" (docs 12:124). "The latest versions for the last two microbatch IDs are always kept, ensuring the
microbatch can restart … if any worker involved in the commit phase were to fail" (12:124). "When a new
version of a PState is computed for the same microbatch ID, the new version replaces the old version in
the external view of the PState" (12:128). The last sentence speaks of an old version *in the external
view*. So a reader can see one task's part of an attempt that is then made again. The skill says the
same (skill microbatch.md, "Guarantees"). **The plan's claim is supported.** *Not run.*

It matters. Without closing, a door could read a new lock's bytes on the request's task, seal a value
with them, and find after a second attempt that the record holds other bytes for that lock. The value
could never be opened.

*Can a reader get an answer that later changes?* A read gives a row when its stamp is not above the
task's `:closed-through`. `:closed-through` is the gate's last stamp before the iteration that writes it,
so it is the last stamp of batch N, written by iteration N+1. Iteration N+1 starts only after "changes
to all PStates have replicated and been made visible" for N (*checked:* docs 23:98), and N's id is then
passed, so N is never made again. A row of N+1 has a stamp above its base, and the base is above the
last stamp of N on every attempt (`stamp.clj` 78–80), so no attempt of N+1 shows a readable row. The two
reads of a query are one event on one task, and a task's PStates change together (12:124). An answer's
row and a class row are never written twice (C2; rule 13). So: **no**, for `answer-by-act-id` and
`class-of-layer`. For `locks-of-request`: the bytes never change; whether a used lock is given with
bytes can, inside one batch that is not closed. That comes with the ruling and is said above.

*Can an answer stay unreadable for ever?* Step 2 does not read the depot's things: it starts on task 0,
goes to the gate's task, and from there to every task. *Checked:* a batch block that reads nothing of
the fragment "runs every microbatch cycle", and without a tick depot "only runs when new events arrive
on other subscribed depots or the sleep interval for microbatching has passed (one minute by default)"
(skill microbatch.md, lines 88–114). So the worst case is one sleep, one minute by default, and not for
ever. Whether the iteration after a batch with things starts at once is supported by docs 12:159 ("If a
microbatch does not process any records, the runner will sleep") and 12:114; one sentence of the skill
can be read the other way. The plan has a way out that needs no new design (a mark on an internal
depot), and the proof now has the line that settles it (`P:1470–1472`). **PASS**, with the point first on
the build's list.

### 5. The class and the permissions on every task

A permission's rows are written in step 4, from the meeting task, to every task, in the same attempt.
*Checked:* one attempt's writes on all tasks are one transaction (skill formal-model.md §6.2). So at the
start of each batch every task holds the same permissions. Inside the batch that gives one, no task
holds it in steps 1 and 3; an act that needs it finds the place empty and goes to the meeting task,
where the pass holds it. An act is decided against a record that lacks a permission only on the meeting
task, in turn order. "Still has it" cannot be: nothing ends a permission (C item 3). **PASS.**

### 6. Builder's rule 7 in both directions, and the first part's lines

Each line number that the plan names was held against the files: `streaming_gate.clj` 93–101, 103–111,
176, 177, 4–92; `door_reads.clj` 59–71, 73–88, 90–98, 4–49; `record.clj` 217–245; `module.clj` 46–53,
4–45. All are as the plan says.

- *X31.* `[:base :streaming u]` on the streaming depot is no stray. The event runs on the task of
  `:base`; both places in `$$streaming-facts` are empty; `:base` starts with the other class, so the
  gate reads the class row of `:base` in `$$microbatch-facts`, which every task holds, and that task's
  `:closed-through`. A stream topology may read a PState of another topology of its module and sees its
  external view (*checked:* docs 23:19, 23:102; skill formal-model.md, "Read-Int"). The guard by
  `:closed-through` is needed: without it the gate could record `:layer-belongs-to-the-other-gate` for
  ever from a making that a second attempt then refuses.
- *X41.* `[:alice :microbatch u]` on the microbatch depot goes to the task of `:alice` and reads the
  class place in `$$streaming-facts` there. That rests on `hash-by :layer` and the hash partitioner
  giving one task (fault 8).
- *The 117 checks.* The module's proof has 117. One sends a making of the base to the streaming depot
  and holds `:layer-not-made` (`module_proof.clj` 331–346). After this part that is X31's word before the
  base is made; the proof must make the base after it. One asks `answer-by-act-id` for
  `[:alice :microbatch u]` with `:alice` and holds nil (711); still nil. Neither fails. The check that
  would have failed is a pure one (fault 3).

**PASS** after the edits for faults 3 and 7.

### 7. Rule 18 and the gate-wide last stamp

"A microbatch act has one stamp, later than the last stamp of each task on which it writes." *Derived
from the code:* `next-stamp` gives the greatest of the clock, the last stamp plus one, the carried stamp
plus one (`stamp.clj` 77–80). The base is therefore above the gate's last stamp, which is not below any
stamp that the gate gave on any task. `too-far-ahead?` is `time-of(stamp) > now + 10,000`
(`stamp.clj` 86–87), so an act that passes rule 17 carries no stamp above `pack(now + 10,000, 65,535)`,
which is the bound that the plan holds the gathered greatest to (`P:623–627`). So the base is above each
stamp of each act that passes. An act's stamp is the base plus its turn. The new last stamp is the base
plus the greatest turn that the batch can hold. On a second attempt the last stamp is read again from the
state after the batch before. **PASS**, on every attempt.

Noted, no fault: the stamps of one batch reach `(positions × tasks) / 65,536` milliseconds above the
base; 2 ms at Rama's defaults and 128 tasks. Whoever sets `depot.microbatch.max.records` far up should
know that a stamp more than 10 seconds ahead is refused when another act carries it.

### 8. The three door reads, for both classes

Each begins with the hash partitioner on its first argument (skill query-topologies.md, "Leading
partitioner"). For a microbatch act-id that argument is the uuid, which the caller takes from the act-id
and from nothing else; the gate writes the answer after the same partitioner on the same uuid. A
request's locks are on its uuid's task, with its answer. The class of the base is on every task, so on
the task of `:base`. For a streaming act-id nothing changes for a caller. **PASS.**

### 9. The counts

Recounted from the steps. One sealed value on one entity: reads, the permission (step 1), the answer and
the lock's range (step 2): 3, and 1 iteration. Row writes: the fact, the lock's row with bytes, the home's
row, the answer: 4. Messages to another task: 2 questions, 2 replies, 2 rows: 6. Longest path: 3. Three
values on three tasks: 3 reads and 3 iterations; 10 row writes; 4 + 4 + 4 = 12 messages; 3 hops. The
weighted sums: 0.60 × 3 + 0.20 × 4 + 0.05 × 3 + 0.05 × 2 + 0.04 × 3 + 0.01 × 3 + 0.02 × 3 + 0.02 × 1 +
0.01 × 2 = 3.10; iterations 0.60 + 0.20 + 0.15 + 0.02 = 0.97; the proportions sum to 1.00. **The plan's
numbers are right for what they count.** What they left out is fault 4.

### 10. "For the build to check first": what the references say now

| The plan's point | Found |
|---|---|
| 1. An empty iteration commits; the next iteration starts without the sleep | The first half is supported (skill microbatch.md, 88–114). The second is supported by docs 12:159 and 12:114, and one sentence of the skill can be read against it. Worst case one sleep. Point 4 above |
| 2. A task's things run one after the other, to the first partitioner, in the depot's order | Supported: skill microbatch.md, line 13; a partitioner is where an event suspends (skill formal-model.md §6.1) |
| 3. A PState read and write before an aggregation; a combiner into a temporary PState; a read of it by a path | Supported: skill batch.md, "Constraints"; docs 10:476–556, which shows `allPartition` at the head of a block and `localSelect` of the temporary PState |
| 4. `hash-by` and the hash partitioner give one task | Not said in the docs. Shown by the first part's proof for a keyword and a UUID, on four tasks |
| 5. The microbatch id is the same on each attempt | Supported: "the same across retries of the same microbatch" (skill microbatch.md, 140). It is called opaque; that its text is the same is one step more |
| 6. One gate reads the other's PState on its own task | Supported: docs 23:102; skill formal-model.md, "Read-Int". Added: the order in which the module declares them |
| 7. 1,000 things for a partition in a batch | Supported: skill testing.md, 152 |

### 11. Rules 9 and 19: is the finding right?

*Rule 9. The plan's answer is no, and it is right.* A batch is "the next set of unprocessed data that has
accumulated on depots" (docs 12:114), up to `depot.microbatch.max.records` for each partition (12:122). A
record has "a partition index and an offset" (14:234); nothing orders two partitions. In the first run
the act at offset 5,000 of a busy partition can be in batch 10 and the act at offset 100 of a quiet one
in batch 20. A rebuild from the start takes 1,000 of each partition for each batch: offset 100 is in
batch 1 and offset 5,000 in batch 6. If the two replace one fact, the other one wins. Inside one
partition the order holds.

*Rule 19.* Locks kept, stamps new, rule 17 at a later clock: right, as in the first part. Two findings
were missing and are added (`P:1294–1302`): where the row with a lock's bytes goes in a rebuild, and that
the two words of rule 7 rest on an order between the two depots that no depot holds.

One sentence is not supported by the docs either way: that Rama "gives no call to read back" the range
of a batch. The docs name the internal PState (12:116) and say nothing of reading it. It bears on what a
rebuild could use, and on nothing that is built.

## Claims about Rama that were checked

| The plan says | Source read | Found |
|---|---|---|
| The same things on every attempt; PStates set back | docs 12:116, 12:120 | as said |
| No order between two tasks' events; one sender's order kept | skill formal-model.md, 592 | the second is said; the first follows from its silence |
| One attempt's writes on all tasks are one transaction | skill formal-model.md §6.2; docs 23:37 | as said |
| Visible task by task; a batch can run again after a task showed it | docs 12:124, 12:128 | as said |
| The next batch starts when every task has committed | docs 23:98 | as said |
| Rama sleeps only after an iteration that processed nothing | docs 12:159 | the plain reading; see point 4 |
| Batch blocks run one after the other, each done on all tasks | docs 09:432, 09:456 | as said |
| A batch block begins on task 0 | skill microbatch.md, 65 | as said |
| Two-phase aggregation with a combiner, one value for each outgoing task | docs 10:219–221 | as said |
| A partitioner inside a branch | skill dataflow.md, 222–228 | as said |
| A depot's partitioner runs on the appending client | skill depot-design.md, 132 | as said |
| A topology appends to the partition of its task | docs 14:254 | as said |
| The number of tasks never changes; rows can be moved as they are | docs 19:483, 19:489 | as said |
| Rama gives no call to read back a batch's range | docs 12:116, 12:122 | not said either way |

No claim was found false.

## Query topology: answer-by-act-id
- Input examples present: yes, five.
- Example 2 (microbatch, decided, closed): N=2, M=2. N == M? yes.
- Example 3 (microbatch, not decided): N=1, M=1: the empty row is the output. yes.
- Example 4 (decided, not closed): N=2, M=2: the second read is what says "not yet". yes.
- Example 1 (streaming): N=1, M=1. Example 5 (bad arguments): N=0.
- M values: 1, 2, 1, 2, 0. All same? no. Marked variable, the second read inside an `<<if` on the row:
  yes. **PASS.**

## Query topology: locks-of-request
- Input examples present: yes, five.
- Example 2 (16 locks, accepted, closed): N=3 and 16 iterations, M=3. yes.
- Example 3 (not decided, or refused): N=1, M=1. Example 4 (not closed): N=2, M=2. Example 5: N=0.
- Variable, each read behind a test of the one before: yes. **PASS.** After the ruling it gives a used
  lock without its bytes; the reads are the same.

## Query topology: class-of-layer
- Input examples present: yes, three.
- Example 2 (the base, made and closed): N=2, M=2. Not made: N=1, M=1. Example 1: N=1, M=1.
- Variable for the base, the record chosen before any read: yes. **PASS.**

## PState schemas
- By (key type, partitioner): `$$microbatch-answers`, String, the hash of the act-id's uuid.
  `$$microbatch-facts`, String, the hash of the entity, and every task for two keys.
  `$$microbatch-locks`, String, the hash of the request's uuid and of the value's entity.
  `$$microbatch-task`, no key, one value for a task. The answers and the home rows of the locks share a
  key type and a partitioner. Why they are not one PState: the key structure differs, one row for an act
  against up to 256 for a request; a used lock's row with bytes lives by another partitioner; and "Same
  record shape" (C item 4) gives the first part's four schemas, which the build session ruled. **PASS.**
- Any Object type? no.
- Uniform values use fixed-keys-schema? yes, each row.
- Different fields at one position? Each row has one shape with fields that can be absent: `:refused`,
  `:value` and the lock of a retract, the fact of an unused lock, and now `:bytes` in the home's row of
  a used lock. The template allows a nullable field on one shape. **PASS.**
- Inner collections over 100 subindexed? The only ones are the sets of strays, subindexed. **PASS.**

## Partitioning
- The hash of an entity (facts, entries): a large keyspace; one hot entity is one task's load, by
  decision 10 of 6 October (`R 6-Oct 10`), which is given. **PASS.**
- The hash of a random uuid (answers, locks): large and even. **PASS.**
- The hash of one value, twice: the layer id for the meeting task, `:microbatch` for the gate's task.
  The template fails a hot key. What these two take: the gate's task, one read and one write for a
  batch and one gathered pair from each task (docs 10:221), nothing for an act. The meeting task, only
  the acts that must be put in one order with an act of another task, and the acts of fault 2. Rule 6
  allows one task where "the plan shows that it must", and the plan shows it (`P:590–610`, and its
  difficulty log, 1). **PASS**; what an unauthorised sender can put on the meeting task is fault 2.
- `|all`, three uses. A row under `:class` or `:permission`: about 150 bytes, written at a give or a
  making, read by every act on its own task; Sid's reason for F5 A (`R 7-Oct 5`). `:closed-through`: 8
  bytes on each task, once for a batch with things, so its rate is the batch's and not the acts'.
  **PASS.**

Partitioning efficiency:
- All three cluster sizes: yes.
- Every category as a row, proportions summing to 1: nine rows, sum 1.00. Four kinds had no row; the
  plan now gives each one's cost (fault 4), and each is on single tasks. **PASS after the edit.**
- Weighted sums computed for each N: yes, 3.10 and 0.97; recomputed above.
- Totals across tasks: yes. No read of the gate or of the door goes to every task; what goes to every
  task is writes and events, and the plan says which (`P:881–888`).
- Growth from N = 1 to N = 128: the reads of an act are flat. The reads of a batch are at most 2, and 1
  of `:turns` on each task with things, so at most one more read for a thing at any number of tasks,
  falling with the size of the batch (`P:870–877`). Not a cost that grows with the cluster. **PASS.**
- Placement schemes that keep state: the plan costed the places of an answer (four), of the permissions
  (three) and of a lock (three), each with numbers. **PASS.**

## Topologies
- Microbatch unless justified? yes; C item 4 names it.
- Writes that must be visible in single-digit milliseconds: none in this part. F4 C puts what a person
  does directly in streaming layers.
- Stream topologies: none added. The streaming gate gains two reads on its own task for X31, on the
  path of an act for the base on the wrong depot only.
- A choice made for test synchronisation or to be observable? no. The proof pauses the topology to put
  two things in one batch, and the design does not rest on it (`P:1427–1428`). **PASS.**

## Production readiness
- Many clients at once: yes. Two acts are in one batch, where the turn orders them, or in two.
- A client that restarts: yes. It sends the same thing again; the same first entity gives the same
  partition; it is told `:used`.
- A worker that restarts at any point: yes. The attempt is dropped on every task and made again. Nothing
  that a decision needs is in the memory of a process; the count of turns is in a PState for that reason.
- Large scale: yes. Durable rows are top-level keys or subindexed sets. The temporary PStates hold one
  batch.
- Non-idempotent writes of a stream topology: none added.
- A stream topology that writes to several partitions: none.

## Internal depot usage
- None. The plan's way out for check-first point 1 would add one, a mark for each batch with things.
  Its reason would be (a), an ordering dependency. Not built.

## Cross-topology correctness
- No data flows between the two gates through a depot. Each reads one row of the other's record, for
  rule 7. What that read can see on a second attempt is hidden by closing in one direction (X31) and
  changes only a refusal's word in the other (X41).

## Stream topology correctness
- No `depot-partition-append!` in the streaming gate, before or after this part.

## In-memory state efficiency
- No TaskGlobal and no cache. `$$questions`, `$$replies` and `$$parcels` are Rama's temporary PStates:
  cleared in the priming of each attempt (docs 12:120) and built again from the batch and the PStates.
  **PASS.**

## Minimality — adversarial simplification

**The simplest design that could meet the spec.** One microbatch topology on the microbatch depot. Each
act is sent to the task of its layer and decided there one at a time, in a fixed order, with the
streaming gate's event; rows are written from there to their homes. One PState more for nothing. That
is the streaming gate inside a batch. It meets every rule but the last sentence of rule 6: "Do not send
all acts through one task unless the plan shows that it must." The plan's own first form was this
(`P:1594–1597`). Each mechanism below is what the plan has beyond it.

### `*microbatch-depot`
- **Delete it**: C item 1, "Two depots"; F6 A. Given.
- **Merge or bypass it**: no; the contract fixes what each partitions by.

### The topology `microbatch-gate`
- **Delete it**: C item 4. Given.
- **Merge or bypass it**: a topology has one type, and the contract names a stream topology and a
  microbatch topology.

### `$$microbatch-answers`, on the uuid's task
- **Delete it**: C item 5, "answers by act-id"; C2.
- **Merge or bypass it**: on the layer's task it is the sketch, and every answer of the base is written
  and read on one task. On the deciding task with a pointer: 2 rows and 2 seeks for a read, against 1
  and 1 (`P:226–240`). Kept.

### `$$microbatch-facts`, with the class and the permissions on every task
- **Delete the copies**: check (d) then costs a hop for every act. `R 7-Oct 5`: "a copy is bytes and a
  rare write; a fetch is a cross-task hop on every act". The plan's table: 0 messages and N row writes
  for a give, against 2 messages for every act and 1 row write. Ruled by the build session. Kept.

### `$$microbatch-locks`, at the request's home and beside the value
- **Delete the row beside the value**: C item 7, "lock rows co-located with their values in the record,
  so a later forget destroys a lock and that value is gone".
- **Delete the home's row of a used lock**: a second value under the lock would be refused
  `:lock-not-made-for-this-layer` and not `:lock-already-seals-a-value`, and a row would be deleted that
  the spec does not say to delete (SKILL.md). Kept, without bytes.

### `$$microbatch-task`: `:turns`
- **Delete it**: the turn needs the position of a thing in its partition's part of the batch, the same
  on every attempt (rule 6). A count in memory goes on from where a failed attempt stopped. An order by
  uuid needs no count and breaks the depot's order on one partition, which is the one order that a
  sender can rely on. An order "by partition, then by arrival" needs no count either, and lets every
  act of partition 0 win over every act of partition 1 in every batch. Kept; one write for a thing.
- **Merge or bypass it**: it is one field of the task's value, not a PState.

### `$$microbatch-task`: `:closed-through`
- **Delete it**: a reader can then see a stamp, a reason or a lock's bytes that a second attempt
  replaces: IS C2, "once the record holds an answer under an act-id, nothing changes it"; IS O6, "once
  a reader could have read a lock's bytes, those bytes never change". Point 4 above shows that Rama
  allows it. Ruled by the build session.
- **Merge or bypass it**: the other built form keeps the clock of a batch and the lock bytes outside
  this gate: one internal depot, a second source and one PState in the streaming gate, and one word
  that still differs (`P:672–675`). More mechanism for less. Kept.

### `$$microbatch-task`: the gate's `:last-stamp`, on the gate's task
- **Delete it**: rule 18 and C7, a stamp that increases through a restart.
- **Merge or bypass it**: one for each task needs the greatest over the tasks that an act writes on,
  before any of them is written: a gathering for each act. A global PState costs one more batch block
  for every batch (`P:369–375`). Ruled. Kept.

### The four batch blocks and the three temporary PStates
- **Delete one**: the homes must hold every question before they say `:shared` or `:yours`; the
  deciding tasks must hold every reply before they know who meets; the meeting task must hold every act
  that meets before it starts. Each is a wait for all tasks. Tried: writing the parcels on arrival in
  block 3, without `$$parcels`. That saves no block, because the meeting task still needs one, and the
  ruling on a lock's bytes now uses the gathering for the order of two writes. Kept.

### The meeting task and the four rules of who goes there
- **Delete it**: either every act goes to one task (the sketch), or two acts that need one row are
  decided on two tasks with no order between them, against rule 6 and X32 to X37.
- **Merge or bypass it**: the gate's task could serve. It would put the acts that meet of every layer on
  one task. The layer's task spreads them by layer, and is where X41's read is. Kept.

### The routing id of the first two reads
- **Delete it**: the read then goes through the layer's task first: a hop for every read, and every
  read of a base answer through one task. C rule 7, "found from its act-id alone", is met: the routing
  id is a slot of the act-id. Kept.

### The pure namespace `softland.store.batch`
- **Delete it**: the logic moves into dataflow, where no proof without a cluster reaches it. The first
  part keeps the same line. Kept.

Nothing in the plan is beyond what the last sentence of rule 6 and rule 7 ask. **PASS.**

## Throughput — adversarial

The frequent operation is one act with one sealed value on one entity.

- *Its reads.* The permission, on its own task. The answer under its act-id, which F7 A says to read
  before deciding, on the uuid's task because of rule 7. The lock, at its request's home because a lock
  is asked for before its value is known. 3 reads. A design with fewer would have to skip one of the
  three. None can be skipped. The streaming gate pays 4.
- *Its writes.* The fact, the answer, the lock beside the value, the home's row of the lock, and the
  count of turns. Built against it: way 1 of "Where a lock lives", 1 lock row for a value: 1 write and
  about 105 bytes less for each value, and the lock is not beside its value, against C item 7. Not
  taken, on the contract's word. The count of turns: one write over one key for each thing. Built
  against it: a position taken from the order in which a task gathers its own entries. It gives no turn
  in step 1, where the homes need it to say which asker is first. Not taken.
- *Its messages.* 6. Each pair is forced by where a row is found: the answer from the act-id, the lock
  from its id. Built against it: the answer beside the fact with a pointer on the uuid's task: 2 rows for
  an act and 2 seeks and a hop for every read by the door. Costlier in sum.
- *For a batch.* N writes of `:closed-through`. Built against it: a mark on each task that holds rows of
  the batch saves the write on the other tasks and costs one write on each task with rows; under load
  every task has rows, and the two are the same. Not taken, with the numbers at `P:881–888`.
- *A repeat* pays the reads of its first run. Built against it: a step that reads the answers alone:
  two more waits for all tasks in every batch, for every act. Not taken.

No cheaper design was found that still meets the contract. **PASS.** Fault 2 is the one cost that the
plan had wrong, and it is of an act that is refused.

## Spec coverage — trace every operation and constraint

The traces of points 1 to 8 above are the traces of most blocks; a block that rests on one says which.

### O2. An act on the microbatch depot (C item 4)
- **Source**: "A microbatch topology with the same checks, over a batch, across tasks. Decisions of one
  batch are visible together (the finding). Same record shape. Builder's rule 6 holds it."
- **Trace**: points 1 and 2. The checks are the first part's functions, called on one act with all its
  rows on one task (`P:929–936`); the schemas are the first part's four.
- **Fault-tolerance**: worker restart, the attempt is made again from the state after the batch before.
  Retry, the same turns; another clock. A write that fails on one task: nothing of the attempt is kept.
- **Race**: two clients on one place, the turn. Events out of order between tasks: each step waits for
  all tasks, and inside a step nothing depends on the order of arrival but the two writes of one lock,
  which have their rule.
- **Flaws found**: the order of two reasons, fault 1. Fixed.
- **Verdict**: PASS after the edit.

### "Decisions of one batch are visible together" (C item 4; IS C3)
- **Source**: IS O2: "no reader sees the accepted answer of a microbatch act while a fact of that act on
  another task is not yet visible."
- **Trace**: an answer is readable on its task when that task has committed the iteration after its
  batch. That iteration started after every task had made the batch visible (docs 23:98). So when the
  answer is readable, each fact of its act is visible on its task.
- **Fault-tolerance**: an attempt that one task showed and that is made again is not readable (point 4).
- **Race**: two tasks open one batch to the three reads at slightly different times. The one pair of
  the door's reads on two tasks is the answer of the base's making and the class of the base: for a few
  milliseconds one can be given and the other not yet. Rama opens the tasks one by one (docs 12:124);
  no plan changes that. Both reads then give the same for ever.
- **Flaws found**: none that a plan can close, with reasoning: the quoted line holds as the plan builds
  it; what does not hold is one instant for two tasks, which is a fact of Rama and is said at
  `P:1514–1521`.
- **Verdict**: PASS.

### O3. Make the base
- **Source**: C item 9: "The base, `:base`: class microbatch (F4 C); the first permission is the
  system's, `[:system :base :base]`; the act's `:who` is `:system`."
- **Trace**: the making has a `:class` and a `:permission` fact: meeting task. Nothing of the layer is
  in the record or the pass: `standing` gives nil for a right making act (`decision.clj` 149–150).
  Writes: the class row and the permission row on each of 4 tasks, the answer on the uuid's task.
- **Fault-tolerance**: restart or retry, the same decision. The rows on every task are one attempt.
- **Race**: two makings in one batch, on the tasks of `:base` and of `:system` by the order of their
  facts: both on the meeting task; the earlier turn is accepted, the later refused at (g).
- **Flaws found**: none, with reasoning: point 3, second case.
- **Verdict**: PASS.

### O4. Write value facts and retracts
- **Source**: C rule 2: "A fact that names nothing to replace is never refused as stale; it becomes the
  current fact of its place." C rule 3: "A retract is recorded as the current fact of its place."
- **Trace**: a fact that names nothing asks for no row and is written. Two such on one place from two
  partitions: `:shared`, both accepted on the meeting task, one row sent for the key, the later turn's,
  with the later stamp (X33). A retract is a row with no value, by `record/fact-row`.
- **Fault-tolerance / race**: as O2.
- **Flaws found**: none, with reasoning: the rows before the batch plus the rows of earlier turns are
  what each act is held to (`P:590–610`), and this stage found no pair that escapes it (point 3).
- **Verdict**: PASS.

### O5. Give a permission
- **Source**: C item 3(g): "a fact under `:permission` writes a permission beneath the one that the act
  itself acts under, in the act's layer, and nothing else."
- **Trace**: `:system` gives `[:alice :base :base [:system :base :base]]`. Meeting task. The place of
  `:alice`'s permission is read from that task's copy: empty. Accepted; 4 rows and the answer. In the
  next batch `:alice`'s act finds the row on its own task: 1 read, no hop.
- **Fault-tolerance / race**: point 5. A second give to `:alice` in the same batch finds the first in
  the pass: `:store-key-takes-one-write`.
- **Flaws found**: none, with reasoning: point 5.
- **Verdict**: PASS.

### O6. Ask for locks
- **Source**: C rule 5: "A lock's bytes are made inside the module and are never appended to a depot …
  Neither a retry nor a replay makes a lock again for a value that is already sealed."
- **Trace**: a request for 16, its fact on its own uuid: the depot puts it where its answer and its
  locks will be. 2 reads, 18 row writes, no message. Bytes are made at the write. This part appends to no
  depot.
- **Fault-tolerance**: a second attempt makes other bytes; `locks-of-request` gave none of the first,
  because the request's answer was not closed. A repeat after the commit is told `:used`.
- **Race**: a request and a use in one batch: point 3, third case.
- **Flaws found**: none, with reasoning: the bytes of a lock are readable only when their batch cannot
  be made again.
- **Verdict**: PASS.

### O7. Send under an act-id that was sent before
- **Source**: C rule 1: "A second, different act under a used act-id must not read back the first act's
  answer as its own." C rule 11: "The gate writes nothing for a second, different act."
- **Trace**: the answer's home says `:used` when a row is there, and in one batch `:yours` to the least
  turn and `:taken-in-this-batch` to each other. Nothing is written for the others. The read gives the
  digest; the sender compares.
- **Fault-tolerance**: the least turn is the same on every attempt.
- **Race**: X35, the same act-id on two tasks: they meet at the answer's home, whatever their facts.
- **Flaws found**: a repeat costs its reads again; the plan says so. No rule is broken.
- **Verdict**: PASS.

### O8. Read the answer by act-id
- **Source**: C rule 7: "A microbatch act's answer is found from its act-id alone (Q22 A, F7 A)."
- **Trace**: point 8; 1 read while there is nothing, 2 when the answer is there.
- **Fault-tolerance**: a row of an attempt that is made again is not given (point 4).
- **Race**: a read at the same time as the commit gives nothing or the whole answer.
- **Flaws found**: the head functions, fault 3. Fixed.
- **Verdict**: PASS after the edit.

### O9. Read a lock's bytes
- **Source**: C item 7: "The door reads a lock's bytes by a query."
- **Trace**: `locks-of-request` on the request uuid's task: the request's answer, `:closed-through`, one
  range. An unused lock: its bytes. A used lock: its way, no bytes.
- **Fault-tolerance / race**: under the ruling, above.
- **Flaws found**: IS O9's derived "in each state of the lock" is narrowed for this class, by a ruling
  of the build session that this stage was told to apply. It is not a fault of the plan, and no edit of
  the plan would close it; it is in the list for Sid.
- **Verdict**: PASS as ruled.

### O10. Read a layer's class
- **Source**: C rule 7: "Either gate can tell which gate a layer belongs to."
- **Trace**: `class-of-layer :base`: the class row on the task of `:base`, then `:closed-through`.
  Nothing before the making's batch is closed (X117).
- **Flaws found**: none, with reasoning: the row is on every task, written in one attempt, never
  written again.
- **Verdict**: PASS.

### C1. No exception on what arrives
- **Source**: C item 3: "each a recorded refusal word, never an exception".
- **Trace**: a thing that is the number 5. The depot's `first-entity` gives nil on the client. In step 1
  it is a stray: its digest is added to a set, and it ends. The greatest carried stamp of any thing is
  read "with a guard" (`P:496`). Each function that touches a thing is the first part's.
- **Fault-tolerance**: "a record that throws deterministically retries forever" (skill microbatch.md,
  129) is why this matters here more than in the streaming gate: it stops every partition, not one.
- **Flaws found**: none in the plan. The new pure functions of `batch` must hold the same rule; the plan
  says they do (`P:926–927`), and the pure proof is where that is shown.
- **Verdict**: PASS.

### C2, C5. One act-id, one answer; a repeat does not depend on the record of the moment
- **Source**: IS C2: "once the record holds an answer under an act-id, nothing changes it".
- **Trace**: check (a) is the reply `:used`; no act told `:used` is decided or written. For a reader:
  point 4.
- **Flaws found**: none, with reasoning: an answer's key is written once, by the act that was told
  `:yours`.
- **Verdict**: PASS.

### C4. The order of the reasons
- **Source**: IS C4: "when a thing has two faults, the check that comes first gives the recorded reason".
- **Trace**: an act of `:bob`, who holds no permission and whose permission names `:alice` as holder,
  carrying a stamp 60 seconds ahead. The streaming gate records `:carried-stamp-too-far-ahead`. The plan
  as written recorded `:who-not-the-permissions-holder`.
- **Flaws found**: fault 1.
- **Verdict**: FAIL as written; PASS after the edit at `P:537–539`.

### C6, C7, C8, C9, C10
- **C6**: the digest is the first part's, taken of the thing as it arrived. PASS.
- **C7**: point 7. "A stamp that a reader could have seen never changes": point 4. PASS.
- **C8**: every row is in a PState; the temporary PStates are built again in each attempt. PASS.
- **C9**: no trimming option on the depot (`P:414`); a request holds a count. PASS.
- **C10**: the stray test, the declaration and the class are checked again on arrival; a thing that a
  topology put on another partition is still decided against the true rows, because this gate reaches
  every row by a partitioner (`P:416–420`). PASS.

### X31 with X41, and X107 (C rule 7; C rule 10)
- **Source**: C rule 10: "one act-id never gets two answers".
- **Trace**: point 6. X107: each gate answers only under an act-id of its own class
  (`declaration.clj` 429), so the same three slots cannot be answered twice.
- **Flaws found**: fault 8, a sentence. Fixed.
- **Verdict**: PASS after the edit.

### X32 to X37
- **X32**: point 1. **X33**: O4. **X34**: one act writes a place and names nothing, another replaces the
  fact that was current before: one place, two acts, `:shared` or one deciding task; the turn decides.
  **X35**: O7. **X36**: point 2. **X37**: point 3.
- **Verdict**: PASS, each.

### X38, X39, X40
- **X38** — source: "Nothing of the attempt is visible on any task. The retry decides the same batch and
  gives each act the same answer." The first sentence: met for the three reads by closing; not for a
  select straight on a PState, which the plan says and the module's README will say. The second: met but
  for the clock and X41's word. Sent up; see point 1.
- **X39**: an answer is readable later than Rama's own commit by one iteration, never earlier. PASS.
- **X40**: a batch of nothing: step 2 runs, finds nothing to close or closes the batch before. A long
  batch: steps 2, 3 and 4 give the task back; step 1 does one thing at a time to a partitioner. PASS.

### X42, with C rules 9 and 19
- Point 11. The finding is right. **PASS.**

### C rule 6
- **Source**: "In one batch of the microbatch gate, two acts that replace the same fact get the same
  winner on every attempt, and an act that touches several tasks is accepted whole or refused whole. Do
  not send all acts through one task unless the plan shows that it must."
- **Trace**: points 1 and 2. The third sentence: an act that meets no other is decided where it arrived;
  the plan shows why the others must meet.
- **Flaws found**: the clock of rule 17, which is the conflict of two rules named in point 1 and not a
  fault that an edit of the plan closes.
- **Verdict**: PASS for what the plan builds. The conflict goes up.

### C rule 18
- Point 7. **PASS.**

### C rule 21
- **Source**: "The plan says what it aims at for each."
- **Trace**: it says: 1,000 acts a second on one partition, with an iteration inside a second; an answer
  readable within two iterations; the third limit not met for small values (`P:1312–1325`).
- **Verdict**: PASS. None is measured, and the plan says so.

### The template's two anti-patterns
- Fault tolerance was traced where the spec is silent: points 1, 2 and 4, and each block.
- Races were traced for two clients on one place and for events out of order: points 1 and 3.

## For the build session and for Sid

1. **Rule 6 against rule 17.** "The same winner on every attempt" and "more than 10 seconds ahead of the
   gate's wall clock" cannot both hold to the letter on Rama: a second attempt reads a later clock, and
   Rama sets back every PState of this gate. Closing hides the difference from the three reads. To make
   it not exist takes state outside the gate (the plan's second way, `P:672–675`). The plan leaves it to
   the build session; so does this stage.
2. **A used lock is read beside its value, not by `locks-of-request`,** in a microbatch layer. That
   follows from "one copy of a lock's bytes". And the ruling saves about 35 bytes of 280, not half: F9's
   third limit is still missed for small values in the base. Only Sid can say whether "co-located" is
   worth the home's row, and H1 is his.
3. **A select straight on a microbatch PState sees a batch that is not closed.** The three reads do
   not. Whether those PStates are made private is open item ?11.
4. **An unauthorised sender costs the microbatch gate more than the streaming gate:** up to 512 reads
   for one act, against 4, and each such act passes the base's meeting task (fault 2). It follows from
   deciding a batch as one order. The door proper is the guard, later.
5. **The proof's first microbatch act settles the plan's weakest point:** if the base's making is not
   readable within about a second, the iteration after a batch does not start at once, and the plan's
   mark on an internal depot is the change.

PHASE_VALIDATION:minor-fail
