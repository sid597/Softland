# Plan Validation

<!-- Phase 2 of the Rama skill, "Plan Validation". Copied from references/artifact-plan-validation.md and
filled in. The template's sections are all here, under their own headings. Five sections that the build
session asked for come before them. -->

Part: `streaming-gate-and-record`, the first of the two parts of the store's module
(`DECOMPOSITION.json`, first entry).
Plan under check: `PLAN-streaming-gate-and-record.md`, in this folder.
Written by: a fresh session (Opus 5.5, effort max), 8 October 2026, for the build session "softland-11".
This session read no code of the rig, the server or Inland. It wrote no code and made no commit. It ran
one thing: the act's proof as it stands, `clojure -M:store -m softland.store.act-proof`, which printed
`{:test 8, :pass 150, :fail 0, :error 0}`.

## How to read this paper

**Sources**, cited as the plan cites them. `C` is the contract (`BUILD-store-module.md`): `C item 3(d)`,
`C rule 10`. `R` is the README (`src-store/README.md`): `R 7-Oct 8`. `IS` is the implied spec:
`IS C3`, `IS X29`. `P` is the plan, by the name of its section, and by line where a line helps:
`P:591` is line 591 of the plan as it stands after this check. Rama is cited from the skill's
references (`skill stream.md`) and from the local copy of its docs: `docs 14:254` is
`reference/rama/docs/14-depots.md`, line 254.

**Three kinds of statement**, on each claim that a verdict rests on.

- *checked*: I read the passage and quote it, or I ran the thing.
- *derived*: it follows from passages that I read; the reasoning is given.
- *not verified*: neither the docs nor the skill say it, and nothing was run.

**Order.** First the verdict, what failed, what I changed in the plan, and what is left for the build
session and for Sid. Then the five sections that the build session asked for: three acts traced row by
row; the alignment of each read and write; the recount of reads and writes; each claim about Rama that
the plan marked; the record as the interface for the second part. Then the template's sections. The
builder's rules 1 to 21, each quoted, are inside "Spec coverage".

## The verdict

**minor-fail.** Nine things failed. Each is repaired in the plan by an edit of a few lines, and none
asks for another architecture: the depot, the one stream topology, the four PStates, the three queries
and the pure namespaces stand as the plan has them. One ruling of the build session is applied as well.

### What failed

Each line: what, where in the plan, what it broke, how it is repaired.

1. **A thing can sit on another partition than the one its layer hashes to, and the gate rested on it
   not doing so.** `P` "Depots" said "not guarded", on a claim marked *not verified*: that no call lets
   an appender pick the partition. The claim is false for a topology (*checked*, docs 14:252–254). The
   gate would then decide the thing against the record of a task where no row of its layer lives: a
   second making of a layer, and two answers under one act-id on two tasks. That breaks `IS C2`,
   `IS C10` and the last sentence of `C rule 10`. Repaired: the gate calls the hash partitioner on the
   thing's `:layer` after the test for a stray and before its first read.
2. **A right making act could not pass check (g) as the walk was written.** `P` "The walk of one
   event", step 5, ran (g) for every act. The permission that a making act writes is its own, with
   nothing above it, so (g) would refuse it. The plan's own count for a making act (4 reads, 4 writes)
   and `IS O3` show that the exception was meant; it was not said. Repaired: the walk, the table of
   checks and "The first act of a layer" now say what a right making act is not held to.
3. **"The plan cannot close the window" of `IS X29` was not supported.** `P` "What the plan found out".
   Rama has a setting that bounds the records pending for a stream topology on a task (*checked*, docs
   11:317). At 1 it would close the window (*derived*), and each act would then be a streaming batch
   of its own, which is paid for against F9's first target. Repaired: the finding names the setting,
   its price, and that the choice is Sid's.
4. **The engine of `act.clj` was given a second argument that the contract does not give.** `P` "Where
   the code lives" and "The door's declaration" passed the nesting limits beside the map of shapes.
   `C item 2` gives "the map of shapes as an argument", "for that, and for nothing else". Nothing needs
   the second one: no shape of the door's is inside itself. Repaired: the engine takes the map of
   shapes; the nesting limits stay `act.clj`'s own.
5. **The order of the reasons could break in the declaration's check, and the test for a stray was not
   said to be total.** `P` "The door's declaration", "The check", step 1: setting `:sealed` aside from
   "each fact" throws when `:facts` is no sequence or a fact is no map, and a throw is `:malformed`,
   where `act.clj`'s own order gives another word. That breaks `IS X6` and `IS C4`. Repaired: the step
   sets aside only from a plain map inside a sequence; the test for a stray gives `:no-act-id` for a
   thing that throws while it is tested (`IS C1`).
6. **One read told the gate nothing.** `P` "The walk of one event", step 4: when the place of `:who`'s
   permission holds another permission, the plan read the layer's class too. The row already shows that
   the layer is made. Repaired: no read there (SKILL.md, goal 7).
7. **The one text left the order of a map's entries open where two keys have one text.** `P` "The one
   text of a thing". Its first property, "the same thing has the same text everywhere", was then not
   shown (`IS C6`). Repaired: entries are sorted by the text of the key, then by the text of the value.
8. **Two findings claimed too much or too little.** `P` "State primitive selection" said that every
   write of an event is bounded by the limits of rule 20; the bytes of a key are not, because an id has
   no greatest length. `P` "C rule 19" said that the 10-second rule refuses no old stamp at a rebuild,
   and did not draw what follows: an act that rule 17 refused can come out of a rebuild accepted.
   Repaired: both are said as they are, with two more findings for the receipt.
9. **What a query gives for a first argument that is no id rested on what Rama's client does with
   it.** `P` "Query Topologies": the query's own test runs on the task, after the client has hashed
   the first argument (*checked*, skill query-topologies.md, "Leading partitioner"). What the client
   does with nil there is *not verified*. `IS X110` and `IS X119` ask for "nothing". Repaired: the hand
   tests the shape of what it passes before it calls, and gives nil itself.

### The ruling that is applied

In a lock's row the way of sealing is a small integer, not a keyword; the word for the way lives in
code beside the number. Applied in every place where the plan had a word in the row. The estimates
move with it: a lock row is about 93 bytes once it seals a fact (104 with the word), about 140 with its
key as written (151), about 102 where RocksDB shares the fronts of neighbouring keys (113; that sharing
is *not verified*).

One number in this change is mine, and the build session should confirm it or set it back. The plan
aimed at 100 bytes of lock for each value before a way was in the row, when its estimate was 95; and at
120 with the word, when its estimate was 113. With the integer its estimate is 102. I wrote the aim as
**110**, keeping the plan's own margin, in the three places that give it; and "55 bytes" where the plan
had "60" for the size of value above which "2 times" holds.

### What this check changed in the plan

The plan was 1,986 lines and is 2,087. The plan as it was is the one in commit `90ef0ab0`, which the
build session made while this check ran; I compared it with the copy that I read at the start, and
they are the same. My changes are not committed. `git diff` of the plan's file shows each of them: 226
lines in, 125 lines out. Each line below gives the lines as they were, then the lines as they are,
then the reason by the number above ("R" is the ruling).

| Lines before | Lines now | What changed | Why |
|---|---|---|---|
| 10–12 | 10–19 | The header: the rulings said as they stand, and a note that this check changed the plan | 4, R, all |
| 52 | 59 | The design table's row for the topology | 1 |
| 81 | 88 | The table of namespaces: the engine takes the map of shapes | 4 |
| 99–105 | 106–114 | "What changes in `act.clj`, exactly": one argument, not two | 4 |
| 111–112 | 120–121 | The leaves and the nesting limits stay `act.clj`'s own | 4 |
| 420 | 429 | The lock row's schema: `:way Long` | R |
| 426–432 | 435–444 | The paragraph on the way of sealing | R |
| 525 | 537 | "Depots", colocation | 1 |
| 540–542 | 552–560 | "Depots": the claim on who can pick a partition, as the docs have it | 1 |
| 573–577 | 591–612 | "The event has no partitioner" is now two paragraphs: the one partitioner, and what holds after it | 1 |
| 581–583 | 616–619 | "The event does not yield": the partitioner comes before the first read | 1 |
| 589 | 625–627 | The walk, step 0 | 1 |
| 598–600 | 636–639 | The walk, step 4 | 6 |
| 603 | 642–645 | The walk, step 5 | 2 |
| 614–616 | 656–658 | The cost of an act whose permission does not stand | 6 |
| 667 | 709 | The list of PStates: `:way Long` | R |
| 691–692 | 733–737 | "Query Topologies": what a caller tests before it calls | 9 |
| 736, 738–739 | 781, 783–785 | "Partitioning efficiency": where the partitioner is | 1 |
| 806 | 852–853 | "Colocation" | 1 |
| 814 | 861 | "The door's declaration": the ruling | 4 |
| 834–835 | 881–883 | The nesting limits | 4 |
| 845–846 | 893–894 | The part that the engine does not read | 4 |
| 904 | 952 | "What the engine is asked for" | 4 |
| 916 | 964–966 | The test for a stray: what a thing that passes has; that the test never throws | 1, 5 |
| 922 | 972–974 | "The check", step 1 | 5 |
| 959 | 1011 | The one text of a plain map | 7 |
| 1043–1049, 1051 | 1095–1102, 1104 | "Locks and the seal": the way is a number | R |
| 1091 | 1144–1146 | "The first act of a layer": not held to (g) | 2 |
| 1126–1127 | 1181–1182 | The table of the decision, rows (g) and "after (g)" | 2 |
| 1136–1137 | 1191–1193 | How check (c) is made | 6 |
| 1164 | 1220–1221 | How (g) and the store-key step are made | 2 |
| 1194–1195 | 1251–1256 | What rule 20 bounds and what it does not | 8 |
| 1213 | 1274 | Disk usage: the row of the locks | R |
| 1220–1228 | 1281–1290 | F9's third limit: the estimates and the aim | R |
| 1260–1264 | 1322–1326 | "Minimization", locks | R |
| 1283, 1288 | 1345, 1350 | The table of what each check reads: rows (c) and (g) | 6, 2 |
| 1303–1304 | 1365–1366 | "One event, one task, no hop" | 1 |
| 1310 | 1372–1373 | Reads of an act whose permission does not stand | 6 |
| 1320, 1326 | 1383, 1389–1390 | F9's second limit: the path of one act | 1 |
| 1340 | 1404 | "The path of a lock", step 2 | R |
| 1388–1389 | 1452–1454 | "Rule 16 on the layer's task" | 1 |
| 1422 | 1487–1488 | The builder's rules, line 10 | 1 |
| 1451 | 1517 | The builder's rules, line 21: the aim | R |
| 1489–1490 | 1555–1558 | `IS X29`, first point | 1 |
| 1504–1506 | 1572–1582 | `IS X29`: the setting that would close the window | 3 |
| 1508–1509 | 1584–1585 | `IS X29`: how wide the window is | 1 |
| after 1540, 1542, after 1543 | 1617–1621, 1623–1625, 1627–1636 | "C rule 19": an act that rule 17 refused; the order of a thing that a topology appended; the number of tasks | 8 |
| 1566 | 1659 | "C rule 21", third aim | R |
| 1645–1646 | 1738–1739 | "Words a session made": `:way`, `:aes-256-gcm` | R |
| 1767–1768 | 1860–1861 | "For the second part": the one read | 1 |
| 1870–1871 | 1963–1965 | "The plan's choices", 9 | R |
| after 1892 | 1987–1991 | "The plan's choices", 22: the gate's partitioner, with the other answer | 1 |
| 1898 | 1997 | "What could not be met", 1 | 4 |
| 1904 | 2003–2004 | "What could not be met", 2 | 3 |
| 1907 | 2007 | "What could not be met", 4: 55 bytes | R |
| 1918 | 2018 | "For the build to check first": the engine's one argument | 4 |
| 1920–1923 | 2020–2024 | "For the build to check first": what is not verified | 1, 8 |

In the blocks below, "change n" is the repair of failure n.

Not changed, on purpose: the "Design difficulty log". It is the planner's first-person record. Its
point 8 still says that no way was found to hold the depot's order; "What the plan found out" now has
the setting. Its point 6 and "What could not be met", 6, doubt that "the number of different strays" is
what rule 10 means by a count. Rule 10 as it stands says so in its own words: "The count of strays is
the number of different ones." The plan meets it; nothing there is unmet.

### Not failures: left for the build session, and for Sid

These are not counted in the verdict. Each is a choice that is not this session's.

For the build session:

- **The gate's partitioner (failure 1) is written into the plan as its choice 22, with the other
  answer beside it.** The other answer is to rest on the depot's partitioner alone and to say the
  limit in the module's README, as rule 14 does for "any sender can make any layer". It gives back one
  event for each act. I took the partitioner because `R 7-Oct 8` gives the gate its authority for
  exactly this reason, "anything can append to a depot without the door", and because the cost is an
  event on the same task, with no disk and no network. The cost is *not verified*.
- **A leaf for `:sealed`.** The plan asks whether `act.clj`'s new function may also take leaves that a
  caller adds. As ruled it may not, and `:sealed` is set aside and held by rules in code. That stands
  inside the ruling; the ask is still the plan's.
- **`act.clj`'s namespace docstring.** The plan notes that "Callers. Only the proof of this namespace
  requires it" and "Neither the door nor a gate is built" stop being true. The tree's rule is that an
  explanation changes with the code (`src-store/CLAUDE.md`). The contract keeps `parse`, `has-shape?`
  and the proof unchanged; it does not keep a docstring from being true.
- **An id has no greatest length.** The form gives none and rule 20 gives none. A key in this record
  holds two or three ids. Whether Rama takes a record of many megabytes, and what RocksDB does with a
  key of that length, is *not verified*; the plan now lists both for the build to check first. A limit
  on the text of an id would be a fifth number beside rule 20's four, and is the build session's to
  give.
- **The locks PState could be private.** The two pages of Rama's docs differ on whether a query
  topology of the module can read a private PState (*checked*: docs 15:222, docs 28:263). The build can
  try it in a minute. Rule 16 lets any caller read a lock in this build, so nothing rests on it.
- **The aim of 110 bytes** (above).

For Sid, from the plan as it now stands. The first and the last were in it before this check; the
setting in the second and the decision of rule 17 in the third were added by it:

- In this build a holder has one permission in a layer (`P` "PState Design", "Which entity a
  permission fact sits on").
- The order of the decisions is the depot's order except across a failed streaming batch; the one
  setting that would close that window is paid for against F9's first target (`IS X29`).
- What a rebuild from the depots does not give again: a lock's bytes, a stamp, a decision that rule 17
  made, and the order across such a window (`C rule 19`).
- A random UUID in an act-id is his word (`R 6-Oct 5`); the Rama skill asks for a version 7 UUID.

## Rulings after the check

The build session ruled on this check the same day, and amended the contract for it (`C item 3`,
`C rule 20`). I applied two edits to the plan for its two rulings and changed nothing else in the plan.
In this paper only this section is new; the rest is the check as it was made. Where the rest no longer
describes the plan, this section says so.

### Ruling A: no partitioner in the streaming gate

**The ruling.** Choice 22 goes the other way. The contract now says it: "The depot's own partitioner
puts a thing on its layer's task; the gate makes no hop (the build session's ruling on the check of the
plan: a thing that a topology of another module appends to another partition is decided against that
task's record and touches no row of the true layer)" (`C item 3`). The reasons given: a hop makes two
events for each act, and one event for each act on the layer's task is what a streaming layer is for;
every client's append goes through the depot's partitioner; this build has one module and one client.

**What I took out of the plan.** The hash partitioner on the layer, from each place where failure 1's
repair had put it: the design table; "Depots", colocation; the two paragraphs of "Topologies and
PStates", which are again the one paragraph "The event has no partitioner"; "The event does not
yield"; the walk, step 0; "Partitioning efficiency" and "Colocation"; "The checks of one act" ("one
event, one task, no hop"); the path of one act under F9's second limit; "Rule 16 on the layer's task";
the two sentences of `IS X29` that named it; "For the second part"; "For the build to check first".
One accepted act is again one event: 4 reads and 4 writes, recounted as before.

**What the plan says in its place.** The finding stays, as the reason that anything is said: an append
from a topology "always appends to the partition represented by the current task" (docs 14:254),
"whether in the same module or a different module" (docs 14:252). The consequence is said in four
places: "Depots", under "Not guarded, by the build session's ruling"; the list of what the module's
README says, in "Where the code lives"; choice 22; and line 10 of the builder's rules. In the plan's
words: a thing that a topology appends to another partition is decided against that task's record; it
can be refused as an act into a layer that is not made, or leave rows and an answer on that task that
no read finds; the rows of the true layer, on its own task, are not touched. One sentence of "C rule
19" changed with it: such a thing is decided on the task where it sits, at a rebuild as at the first
run.

**Whether a documented test without a hop exists.** The ruling asked for one, to be added as a stray's
word `:on-another-task` if it exists, and not to be invented if it does not. I found none, and added
no word. What I looked for and what is there:

- *The hash that `hash-by` uses.* The docs say "the hash of the value extracted by the provided
  function" (docs 14:38), "hash of input modded by number of tasks in module" (docs 16:153), and the
  same in docs 28:79 and 28:265. No page names the function, and no page or reference gives a call
  that returns the task of a key without going there. *Checked*: I searched docs 14, 15, 16, 28 and 29
  and every reference of the skill.
- *What a topology can know of its own place.* `ops/current-task-id` and `ops/module-instance-info`,
  the second for the number of tasks (skill syntax.md; skill pstate-schema.md, "Partitioning
  control"). Both are documented. Neither gives the other side of the comparison.
- *The one documented way that would work, and why it is not this plan's.* A depot can have a
  partitioner of the module's own writing, a function of the data and the number of partitions (docs
  14:35–48; skill depot-design.md). The gate could call the same function again and compare with its
  task. That is exact and documented. But it replaces `hash-by`, which the ruling names as the
  depot's partitioner, and then the leading partitioner of each query, which "must be a built-in
  partitioner (not custom)" (skill query-topologies.md), no longer names the layer's task: each read
  would need a task id worked out by its caller. That is another design, so I did not write it in.
  The plan says this in two sentences at the end of "Depots".

**What this changes in this paper.** Failure 1 stands as a finding. Its repair is taken back, and the
contract now carries the behaviour as its own. So, in the sections above:

- "What failed", 1: the last sentence, "Repaired: …", no longer holds. The plan states the consequence
  and the contract states it too.
- "The alignment of each read and each write": each row marked "holds after change 1" holds for every
  thing that a client appended. For a thing that a topology appended to another partition it does not
  hold, by the ruling.
- "A thing on the wrong partition, before and after this check": the plan is again as under "As the
  plan was". The sentences under "As the plan is" do not describe it.
- "Three acts, row by row": in the tables, "the partitioner" in step 0 is gone. The rows, the reads
  and the writes are the same.
- "Production readiness", the last point, and "Minimality", the block "The gate's one partitioner":
  the gate has one event for each act and no partitioner.
- "Spec coverage": the blocks `C item 1`, `C item 9`, `IS C2`, `IS C10` and rule 10 say "repaired by
  change 1". Read them as: found, and ruled. `IS C2` and the last sentence of rule 10, "one act-id
  never gets two answers", hold for every thing that a client appends. For a thing that a topology
  appends to another partition the contract now says what happens instead.
- "Not failures", the first point: ruled, the other way from what I had written.

### Ruling B: a limit on the text of an id

**The ruling.** The contract's rule 20 now ends: "and 256 characters in the printed text of an id or
of a fact's key (the check of the plan found that a readable keyword has no length limit, so a row's
key had none)". A number of the build, kept and not decided. The word is `:id-too-long`.

**What I added to the plan.**

- "The door's declaration", "The limits": five numbers, not four, and a paragraph on the fifth. With
  it an answer's key is 293 characters at most, a lock's 297, a fact's 770.
- The same section, the rules: rule 20, `:ids-within-the-limit`, with the word `:id-too-long`. It
  holds every id that the act holds, wherever it sits, and each fact's key.
- The same section, the test for a stray: one more test, after the shape of the act-id and before the
  class.
- "The decision", the table: the word in the row "before (a)", and a row for rule 20.
- "Query Topologies": each query holds the ids that it is given to the limit before it builds a key,
  and gives nothing for one over it; the hand tests the limit before it calls. The three examples
  that cost no read name the case.
- "PState Design" and "Subindexing": the reason words of a stray are five.
- "State primitive selection": the sentence that said a key is not bounded now gives the bound.
- The builder's rules, line 20: one more sentence. "Words a session made": `:id-too-long` in two
  tables, the rule's name in a third, and 41 words where the plan said 40.

**One reading that is mine, for the build session to confirm.** The ruling says that a thing over the
limit is refused with the word, and that the test comes before any row key is built from the id. For
most ids both hold together: `:who`, an entity and a fact's key are tested in check (b), before step 4
builds a place's key, and the refusal is recorded under the act-id. For the layer slot of the act-id
they cannot both hold: the key of the answer itself is built from that layer. So for that one id the
thing is a stray. The gate keeps its digest under `:id-too-long` and gives it no answer. The word is
then a stray's word and a reason word of the declaration, and the plan's two tables of words say so.

**What this changes in this paper.** "What failed", 8, first half: the bytes of a key are now bounded.
"Not failures", the fourth point, "an id has no greatest length": ruled. In the block of rule 8, the
count is 41. In the block of rule 20, there are five limits.

### Confirmed, and stated without a ruling

Confirmed by the build session: the aim of 110 bytes of lock for each value; the way of sealing as a
small integer; the engine taking the map of shapes only; the other seven repairs.

Stated, and not ruled, because they go to Sid: `topology.stream.max.executing.per.task` stays at
Rama's default, and the module's README names it as the setting that closes the window of `IS X29` at
a cost in throughput; a refusal by rule 17 can come out otherwise at a rebuild. The plan already keeps
the default and reports the choice ("What the plan found out", `IS X29`), and has the second finding
under "C rule 19". I added nothing to the plan for either, since the build session asked for two
edits and nothing else. One thing follows that is not yet in the plan: its list of what the module's
README says does not name the setting.

### The plan after the rulings, and the re-check

The plan is 2,121 lines. `git diff` of its file against commit `90ef0ab0` shows every change of this
check and of the two rulings together: 256 lines in, 121 lines out. The table "What this check changed
in the plan" gives the lines as they were before the rulings. Its column "Lines now" is out, from the
plan's header on, by the lines that the two edits added.

I walked the edited plan once more for what the two edits could have broken.

- *The accepted act.* A1 of "Three acts": no stray, the layer `:alice` is 6 characters; check (a);
  check (b), where rule 20 holds each id; the permission's place; the lock; the last stamp. One
  event, 4 reads, 4 writes.
- *The order of the reasons.* Rule 20 is the last rule of the declaration, so no word that the plan
  gave before is given later than it was. As a stray's word it comes after `:act-id-malformed`, where
  the layer slot is known to be an id, and before the two tests that compare it.
- *A thing with a layer of 300 characters.* A stray: its digest, 64 characters, under `:id-too-long`;
  no key holds the layer. So no layer with such an id can be made, and a query for one gives nothing
  without a read.
- *A thing with a good layer and a `:who` of 300 characters.* Check (a) reads the answer's key, 293
  characters at most. Check (b) refuses at rule 20. Step 7 writes the answer and the stamp. The key
  `"<layer> <who> :permission"` is never built.
- *The other seven repairs.* None of their lines was touched, except where a sentence held both: the
  test for a stray still says that it never throws, and "Query Topologies" still has the hand's test.

Nothing broke. The verdict stays: minor-fail, with each failure repaired or ruled.

## Three acts, row by row

The plan's dataflow, walked with values. A cluster of 16 tasks. Alice's personal layer has the id
`:alice`, as in the plan's own example of a permission; the hash of `:alice` gives task 5. A stamp is
written `t·c`: clock time `t` in milliseconds and counter `c`, the integer `t × 65,536 + c`. `u-…` is
a UUID. Each read is counted as the plan counts it.

**The layer is made first** (`C item 9`). The thing M1: act-id `[:alice :streaming u-m1]`,
`:class :streaming`, `:who :system`, `:permission [:alice :alice :alice]`, facts
`{:e :alice :k :class :v :streaming}` and `{:e :alice :k :permission :v [:alice :alice :alice]}`.

| Step (`P` "The walk of one event") | What is read | What is found | What follows |
|---|---|---|---|
| 0, stray | nothing | a plain map; the act-id has its shape; class slot `:streaming`; layer slot is `:layer` | no stray; the partitioner on `:alice`, task 5 |
| 1, check (a) | `$$streaming-answers` `":alice u-m1"` | nothing | go on |
| 2, digest, check (b) | nothing | shape `:arrived-act`; the 19 rules hold | go on |
| 3, the 10-second rule | the clock, once: 1,000 | no `:based-on` | go on |
| 4, standing | `$$streaming-facts` `":alice :system :permission"`, then `":alice :alice :class"` | both empty | the layer is not made; `making-act?` is true: the gate is the streaming one, the permission is `[holder layer layer]`, `:who` is `:system`, the facts are the two |
| 5, places | nothing | no fact names something to replace; (f) holds: the class fact is on the layer; (g) and the store-key step are not held for the two facts | go on |
| 6, the seal | nothing | no sealed fact | go on |
| 7, stamp and writes | `$$streaming-task` `:last-stamp` | nothing yet | stamp `1000·0` |

Written, in one event on task 5: `$$streaming-facts` `":alice :alice :class"` →
`{:value "(:streaming as text)", :by-act u-m1, :by-index 0, :stamp 1000·0}`;
`":alice :alice :permission"` → `{:value "([:alice :alice :alice] as text)", :by-act u-m1, :by-index 1,
:stamp 1000·0}`; `$$streaming-answers` `":alice u-m1"` → `{:stamp 1000·0, :digest d(M1)}`;
`$$streaming-task` `:last-stamp 1000·0`. 4 reads, 4 writes: the plan's count. The layer as entity and
the holder as entity are the same id here, and the two facts still have two places, because the key is
part of the place.

Before this check, step 5 as written ran (g) on the permission fact of M1 and refused it with
`:permission-fact-not-beneath-the-acts-own`. That is failure 2.

**A request for two locks**, R1: act-id `[:alice :streaming u-r1]`, `:who :alice`, the same
permission, one fact `{:e u-r1 :k :lock-request :v 2}`. Check (a): nothing. Check (b): rule 9 (a count
of 1 or more), rule 10 (256 at most), rule 11 (the entity is the uuid of the act-id) hold. Standing:
`":alice :alice :permission"` holds a row whose `:value` is the text of the act's permission, so (c)
and (d) pass on one read. Places: no read; the place of a request is new by its form. Stamp: last is
`1000·0`, clock 1,005, so `1005·0`. Written: `$$streaming-locks` `":alice u-r1 000"` →
`{:bytes B0, :way 1}` and `":alice u-r1 001"` → `{:bytes B1, :way 1}`; the request's fact at
`":alice u-r1 :lock-request"`; the answer; the last stamp. 3 reads, 2 + 3 writes.

### An act that is accepted

A1: act-id `[:alice :streaming u-a1]`, `:who :alice`, `:permission [:alice :alice :alice]`, one fact
`{:e :note-1 :k :text :sealed S1 :lock-id [[:alice :streaming u-r1] 0]}`. The door sealed the plain
bytes under B0 with a fresh random part, to the place `[:alice :note-1 :text]`.

| Step | What is read | What is found | What follows |
|---|---|---|---|
| 0 | nothing | no stray | the partitioner; already on task 5 |
| 1, (a) | read 1: `":alice u-a1"` | nothing | go on |
| 2, (b) | nothing | shape holds; rule 12 (a value under a plain key is sealed), 14 (the seal is whole), 15, 16 (bytes, 65,536 at most), 17 to 19 hold | go on |
| 3 | the clock: 1,020 | no `:based-on` | go on |
| 4, (c), (d) | read 2: `":alice :alice :permission"` | the row's bytes are the text of the act's permission | both pass; nothing more read |
| 5, (e), (f), (g) | nothing | the fact names nothing to replace: never stale (`C rule 2`); no setting key; no permission fact | go on |
| 6, the seal | read 3: the range `":alice u-r1 000"` to `"… 000"` | `{:bytes B0, :way 1}`, no fact named | the lock is this layer's; it seals nothing; way 1 is known; B0 opens S1 at `[:alice :note-1 :text]`; the plain bytes are dropped |
| 7 | read 4: `:last-stamp` | `1005·0` | stamp `1020·0` |

Written, in the one event: `$$streaming-facts` `":alice :note-1 :text"` →
`{:value S1, :lock-act u-r1, :lock-index 0, :by-act u-a1, :by-index 0, :stamp 1020·0}`;
`$$streaming-locks` `":alice u-r1 000"` → `{:bytes B0, :way 1, :seals-act u-a1, :seals-index 0}`;
`$$streaming-answers` `":alice u-a1"` → `{:stamp 1020·0, :digest d(A1)}`; `:last-stamp 1020·0`.
4 reads, 4 writes. Each write is a key and a whole row, so none reads (skill paths.md, "No-Read
Optimizations": "`keypath` + `termval` immediately = write only, no read").

### An act that is refused

A2: act-id `[:alice :streaming u-a2]`, the same `:who` and permission, one fact
`{:e :note-1 :k :text :sealed S2 :lock-id [[:alice :streaming u-r1] 1] :replaces [[:alice :streaming u-zz] 0]}`.

| Step | What is read | What is found | What follows |
|---|---|---|---|
| 1, (a) | read 1: `":alice u-a2"` | nothing | go on |
| 2, (b) | nothing | holds; rule 18: the fact does not name a fact of its own act | go on |
| 4, (c), (d) | read 2: the permission's place | the act's permission | pass |
| 5, (e) | read 3: `":alice :note-1 :text"` | `:by-act u-a1`, `:by-index 0`; the fact named is `u-zz`, 0 | `:replaced-fact-not-current`; the seal step is not reached |
| 7 | read 4: `:last-stamp` | `1020·0` | stamp `1031·0` at clock 1,031 |

Written: `$$streaming-answers` `":alice u-a2"` →
`{:refused :replaced-fact-not-current, :stamp 1031·0, :digest d(A2)}`; `:last-stamp 1031·0`. Nothing
else: the lock `[… u-r1] 1` still names no fact and can seal a later value (`C rule 15`). The place
still holds A1's fact. 4 reads, 2 writes.

### An event that Rama runs twice

Rama can process a record again "even after all PState writes have completed and committed" (skill
stream.md, "Stream retries and exactly-once"; docs 11:166). Four cases, with the rows above.

**Case 1: after the first run was committed.** The leader of task 5 fails after A1's batch is
replicated and before the topology's progress is written. The new leader pushes M1, R1, A1 and A2
again, in the depot's order. For each: no stray; the partitioner; check (a) reads the answer's key and
finds the row; the event ends. Counted against the brief's four questions:

- *A second answer?* No. Nothing is written at check (a). Each answer row is as it was, with its first
  stamp and its first digest.
- *A lock made again for a sealed value?* No. R1's retry ends at check (a), before the step that makes
  bytes. `":alice u-r1 000"` still holds B0, which seals `:note-1`'s value.
- *A second stamp?* No. `:last-stamp` is written only in step 7, which is not reached. It stays
  `1031·0`.
- *A stray counted twice?* See case 4.

**Case 2: after the first run was discarded.** A1 is in a streaming batch with another event that
throws. "If any one of those events throws an exception all the events will fail" (docs 11:168) and
"all PState writes during that batch of execution will be discarded" (docs 11:170). A1's four writes
are gone together, the answer among them. The door's append gets an error although the act will land
(docs 14:214; this is Q22 A's reason). The retry finds nothing at check (a) and decides A1 as a first
decision, perhaps with another stamp. One answer; the lock is marked once.

**Case 3: a request whose first run was discarded.** R1's lock rows were discarded with its answer.
The retry makes other bytes B0′, B1′ under the same two keys. No reader saw B0: a query sees only
committed state (skill pstate-schema.md, "Read visibility"), and the ack had not come back. So no
value can be sealed under bytes that are gone (`C rule 5`; `IS X94`).

**Case 4: a stray.** The thing `{:act-id [:alice :microbatch u-s] :layer :alice …}` arrives on the
streaming depot. Step 0 gives `:act-id-of-the-other-gate`; the digest's hex text is added to the set
under that word in `$$streaming-task`, on the task where it arrived; no answer. Run again after the
commit: the same element is added to a set that holds it. The set does not change and its size does
not change. The count of rule 10 is the size.

**What the retry mode adds.** With "all after", a failure of one record makes Rama process every
record after it on the partition again (docs 11:162). Each of those that was committed costs one read
and ends at check (a).

### A thing on the wrong partition, before and after this check

This is failure 1, traced. Another module mirrors `*streaming-depot` and appends from one of its
topologies while it is on the task that holds partition 3. The thing is a making act for the layer
`:carol`, which hashes to task 9: act-id `[:carol :streaming u-x]`, `:who :mallory`, permission
`[:mallory :carol :carol]`, the two facts of a making act.

*As the plan was.* The event runs on task 3. Step 0: no stray; the act-id is well formed, of this
gate, and its layer is the thing's `:layer`. Check (a) reads `":carol u-x"` on task 3: nothing. Check
(b) holds. Standing reads `":carol :mallory :permission"` and `":carol :carol :class"` on task 3: both
empty, whatever task 9 holds, because "if `current-task ≠ home($$p, k)`, the read returns `nil` (empty
partition), not an error" (skill formal-model.md, "Read-Int"). `making-act?` is true. The gate
accepts, and writes the class, the permission and an accepted answer on task 3. If Carol's layer was
made on task 9, the record now holds the layer twice, with two first permissions. Acts that the same
appender sends to partition 3 under Mallory's permission are accepted there. `answer-by-act-id` goes
to task 9 and finds none of it. If the same act-id also arrives by a client, task 9 gives it a second
answer. `IS C2`, "one act-id, one answer, for ever", is broken in the record; so is `IS O3`, "a layer
is made once".

*As the plan is.* Step 0 on task 3: no stray. The partitioner on `:carol` moves the event to task 9.
Every read and every write is there. If the layer is made, the standing step finds
`":carol :carol :class"` and refuses at (d) with `:permission-does-not-stand`; the refusal is written
on task 9 under `":carol u-x"`, where a reader looks. One record of the layer; one answer.

## The alignment of each read and each write

The rule: "`local-select>` / `local-transform>` on key `k` require `current-task = home($$p, k)` for
correctness; misaligned writes are silent data loss" (skill formal-model.md, 9.2, invariant 10). In
this plan the home of a row is not the hash of its whole key. It is the hash of the layer that the key
begins with. Rama allows that: "a write lands on whatever task the topology routes to before the
`local-transform>` — there is no default placement" (skill pstate-schema.md, "Partitioning control"),
and its docs name the case, "a PState is partitioned by something other than top-level keys in its
structure" (docs 15:418). The price is that nothing may route by the whole key.

| Read or write | Key | Task it must be on | How the plan gets there | Verdict |
|---|---|---|---|---|
| The stray's digest (write) | none: the task's own value | the task where the thing arrived | no partitioner before it | holds |
| Check (a) (read) | `"<layer> <uuid>"` from the act-id | hash of the layer | the gate's partitioner on `:layer`; the stray test has shown that the act-id's layer slot is `:layer` | holds after change 1 |
| The permission's place, the class's place (read) | `"<layer> <who> :permission"`, `"<layer> <layer> :class"` | hash of the layer, not of `:who` | the same partitioner; the layer of the key is the act's `:layer` | holds after change 1 |
| The places of (e) and (g) (read) | `"<layer> <entity> <key>"` | hash of the layer, not of the entity | the same; the layer of each key is the act's own, because a fact has no layer of its own (`R 6-Oct 6`) | holds after change 1 |
| The locks (range read) | `"<layer> <request uuid> <index>"`, from the lock id | hash of the layer in the lock id | the gate tests first that the lock id's layer is the act's layer and its class the gate's (`P` "The path of a lock", 5); only then it reads | holds after change 1 |
| `:last-stamp` (read, write) | none | the layer's task | the task's own value, on the task where the event decides | holds |
| Fact rows, lock rows, the answer row (write) | as above | hash of the layer | written in the same event as the reads, with no partitioner between | holds after change 1 |
| `answer-by-act-id` | `"<layer> <uuid>"` | hash of the layer | the leading hash partitioner on the first argument; the query tests that this argument is the act-id's layer slot, else nil | holds |
| `locks-of-request` | the range of one request | hash of the layer | the same | holds |
| `class-of-layer` | `"<layer> <layer> :class"` | hash of the layer | the leading hash partitioner on the layer | holds |
| The operator's select of strays | none | the task asked for | an explicit partitioning key | holds; it sees the strays of one task |

Three things were checked beside the table.

- *The depot's hash and the gate's and the queries' are the same.* `(hash-by :layer)` and `|hash` on
  the layer: "most common way to shard is with `hash(k) mod num-tasks` either through `hash-by` depot
  partitioner or with `(|hash *k)` dataflow call" (skill core-concepts.md, "Task Model"). *Checked* as
  far as the skill says it; the proof shows it by reading an answer after an ack.
- *The leading partitioner of a query is run by the client.* "If the first line of a query topology is
  a partitioner, it is evaluated client-side to route the query directly to the right task" when it is
  built-in, targets one task, and "all inputs must be topology input variables" (skill
  query-topologies.md, "Leading partitioner"). The plan's three queries meet the three conditions.
  *Checked.*
- *No call routes by a whole key.* The plan uses `local-select>` and `local-transform>` only, and the
  one foreign select gives its partitioning key. A `select>` or a foreign select with no key would go
  to `home(pstate, first-key(path))` (skill formal-model.md, section 6), which is another task. The
  plan says so (`P` "Partitioning efficiency"). *Checked.*

What the table rested on before this check, in every row marked "after change 1", was that the thing
sits on its layer's partition. That is true of a client's append and not of a topology's (docs
14:254). The trace above shows what followed.

## The recount of reads and writes for one accepted act

The plan: "4 reads and 4 row writes, in one event, on one task" for one accepted act with one sealed
value that replaces nothing. Recounted from the skill's cost model, "each path navigation through a
top-level key or subindexed structure is a RocksDB read" (SKILL.md, goal 7).

Reads:

1. `$$streaming-answers`, one top-level key. 1.
2. `$$streaming-facts`, one top-level key, the permission's place. 1.
3. `$$streaming-locks`, one range that begins and ends at the lock's key: 1 seek and 1 iteration. A
   top-level map is sorted and takes range navigators: "top-level and subindexed maps in PStates are
   sorted, and these navigators are how you efficiently iterate over portions of those indexes" (docs
   08:393). 1.
4. `$$streaming-task`, the field `:last-stamp`. The top level is a fixed-keys schema, and "first-class
   schemas (top-level map and fixed-keys-schema) are backed by RocksDB — each key/field is individually
   addressable on disk" (skill pstate-schema.md, "Storage Model"). The field is read without the
   strays beside it. 1.

Writes:

1. The fact's row: a top-level key and a whole row. No read.
2. The lock's row, written again whole with the bytes as read. No read. If it were written field by
   field it would read: "`keypath` + further navigation does read" (skill paths.md).
3. The answer's row. No read.
4. `:last-stamp`: one field of a top-level fixed-keys schema, replaced. No read, by the same passage
   as read 4.

**4 reads and 4 writes: the plan's count holds.** Three things could have hidden a read and do not.
Size tracking "adds an extra disk read on every write", but only for a subindexed structure (skill
pstate-schema.md, "Size Tracking"); the three keyed PStates are top-level maps, and Rama "only knows
the approximate number of keys in a top-level map" (docs 15:1036). The check of a subindexed
structure's location reads the disk only where a subindexed structure is written, which is the stray's
path. The topology's own progress is Rama's write, not the gate's.

With change 1 the count does not move: the partitioner is an event on the same task, with no read.
With change 6 one count is lower: an act whose `:who` holds another permission costs 3 reads, not 4.

The other counts of `P` "The checks of one act", recounted the same way, hold: a repeat 1 and 0; a
request for n locks 3 and n + 3; a making act 4 and 4; a give 4 and 3; a refusal by the declaration
2 and 2; a value that replaces a fact 5 and 4; a stray 2 reads (the field `:strays`, and the set's
size) and one element.

**The arithmetic against F9's first target**, recounted: 4 reads at 0.3 to 0.5 ms and 0.1 ms of
computing are 1.3 to 2.1 ms an act, so 476 to 769 acts a second on a task with every read cold. The
plan's "480 to 770" holds, and so does its plain sentence that the target is not met by that
arithmetic.

## Each claim about Rama that the plan marked, and what I found

| The plan's claim, and where | Marked | What I found |
|---|---|---|
| "This stage found no call that lets an appender pick the partition of such a depot" (`P` "Depots", as it was) | not verified | **False for a topology.** "`depotPartitionAppend` always appends to the partition represented by the current task" (docs 14:254), "whether in the same module or a different module" (docs 14:252). A mirror of the depot appends that way (skill mirrors.md, "Mirror depot appends"), and only a `:disallow` depot refuses a mirror (skill mirrors.md:214). True for a depot client (docs 14:254). Failure 1. |
| "The plan cannot close the window" of `IS X29` (`P` "What the plan found out", as it was) | derived, not verified | **Not supported.** The references show no way for a stream topology to learn a record's offset; that half holds. But `topology.stream.max.executing.per.task` is "the max number of depot records that can be pending for a stream topology for each task" (docs 11:317). At 1 no later record is pending while one waits for its retry (*derived*). Failure 3. |
| The rest of the `IS X29` reasoning: the retry modes; a failed batch discards its writes; a later batch does not become visible before an earlier one | from the docs | **Holds.** docs 11:158, 11:162, 11:166, 11:168, 11:170 and 21:45 say what the plan quotes. One more passage carries the last point: "the topology runner on that task does not move on to the next batch of events until the previous batch has finished replicating its changes" (docs 23:29). |
| A byte array as the class of a schema position (`P` "Topologies and PStates") | not verified | **Not found either way.** The skill asks for "a resolvable JVM class symbol" (pstate-schema.md, "Schema Well-formedness") and lists no array class. Rama serialises with Nippy (docs 30). The plan's fallback said "the nearest form"; it now says a declared class, never `Object`. |
| RocksDB stores a key as its difference from the key before it (`P` "Why the keys are strings"; F9's third limit) | not verified | **Not found.** The docs say only that a PState with a top-level map uses RocksDB, "by default … two-level indexing and … a 256MB block cache" (docs 15:1217). The estimate "on disk" leans on it; the estimate "as written" does not. |
| Whether Rama's RocksDB compresses its blocks (`P` "Minimization") | not found | **Not found.** The same passage is all there is. |
| Whether Rama takes a depot record of 16.8 MB (`P` "The length of one event") | not found | **Not found.** No limit on a record or a message is in docs 14, 19 or 26. |
| The two pages on a private PState differ (`P` "Query Topologies") | from the docs | **Holds.** "inaccessible for reads for anything other than the owning topology" (docs 15:222); "only be readable from topology code within the module definition" (docs 28:263). |
| Which values Rama carries unchanged (`P` "IS X16") | from the docs, not run | **Holds as far as it goes.** docs 30 has the two sentences that the plan quotes, and nothing on byte arrays, on -0.0, on an `Integer`, or on a list. The plan's answer, a check in the proof against the depot's log, is the right one. |
| A record read back from the depot's log has been through serialisation (`P` "IS X16") | not verified | **Not found.** In the in-process cluster "serialization only crosses workers" (skill testing.md, rule 7), which is why the plan reads the log and launches two workers. Whether the log's read gives what was written to disk is the build's first check. |
| The order of string keys (`P` "Why the keys are strings") | from the docs | **Holds.** "Rama implements this property as well for built-in serialization for relevant types like numbers and strings" (docs 17:169); "top-level keys sort the same way, not just subindexed" (skill pstate-schema.md, "Subindexing"). |
| The leading partitioner; the ack levels; an ack that fails for an accepted act; depots keep every record; a depot's partitioner runs on the client | from the skill and the docs | **Each holds**: skill query-topologies.md "Leading partitioner"; docs 14:206; docs 14:214; skill depot-reference.md "Depot Trimming"; skill depot-design.md, "Depot partitioners run on the appending client". |
| Reads on the two sides of a yield "are NOT atomic" (`P` "Topologies and PStates") | from the skill | **Holds** (skill dataflow.md:199). The same page gives the reason that carries change 1: "events that yield at the same points do keep their relative order". |

Every line number that the plan gives for Rama's docs was opened. Each is the line that holds the
words quoted.

Two facts of Rama that the plan did not have, and now has under "C rule 19". Rama cannot change a
module's number of tasks (docs 19:483). Of the two ways that its docs give to get more, the one that
replays the depots "only works if your processing is deterministic" (docs 19:489), which this gate's
is not; the other moves the rows of the PStates as they are, and fits a record in which every key
names its layer.

## The record as the interface for the second part

The question: can a microbatch gate, whose layer is spread over all tasks, use these row shapes and
these pure functions and still meet `C rules 6, 7 and 18`? This section designs nothing of the second
part. It asks only what in this plan would shut a design out, or would have to be made again.

**What holds, and why.**

- *The rows.* No row names a task. Each key names its layer and its own id. So a row of a microbatch
  layer can live on the task of its entity, of its request, or on every task; the shapes do not say.
  "Same record shape" (`C item 4`) is the same four schemas under the other gate's names, which Rama
  requires in any case: "only that topology may write it" (skill core-concepts.md).
- *The checks.* They are functions of an act and of rows, and the rows of an accept come back as
  data, each with its key. A gate that decides a batch "one at a time in one order" (`IS O2`) can fold
  over the acts with the rows that earlier acts wrote laid over the rows that it read; the keys of
  this plan serve as the keys of that overlay. That is what makes "the same checks" (`C item 4`)
  literal.
- *`C rule 7`.* A streaming layer's class is one row at a place found from the layer id. A microbatch
  layer's class fact sits on the layer's own entity (`C item 2`; F8), so its natural home is the task
  that the layer id gives, which is where the streaming gate needs to read it.
- *`C rule 18`.* The next stamp takes the last stamp as a number. The greatest of several tasks' last
  stamps is the same call.

**What the second part will find hard to build on.** None of these needs this part's record to be made
again. The first and the fifth are one-line changes in this part's code; they are named so that the
second part's plan does not find them late.

1. *The three queries route by the layer as their first argument.* For a streaming layer that is the
   row's task. A microbatch answer is found "from its act-id alone" (`C rule 7`), and if it does not
   live on the task that the layer id gives, the query must either go through that task first, so that
   every read of a base answer passes one task, or change its first argument, and then each caller
   changes. The plan says so in one clause (`P` "For the second part", 6). In this build the only
   caller is the proof's hand.
2. *Check (d) is one local read because a permission fact sits on its holder and the whole layer is
   on one task.* In a layer whose facts are spread by entity, the holder's place is on the holder's
   task, and every act on every task needs it. The second part must copy the permissions of its layer
   to where its acts are decided, or hop for each act; `R 7-Oct 5` gives Sid's cost reason against the
   hop. Check (c) is read off the same row, so it follows the same answer.
3. *A lock's home follows from its request; its value's home follows from the value's entity.* The id
   of a lock is `[request's act-id, index]`, and the request's one fact sits on the uuid of its act.
   "Sits beside its value" (`C item 7`) and "a second value under a lock that already seals one" then
   need one place that decides for a lock, also when two acts of one batch name it from two tasks
   (`IS X42`).
4. *The check functions are given by stage, for a whole act with all its rows in one hand.* A gate
   that checks each task's part of an act where its rows live needs the checks of the places and of
   the seal by fact, and a way to pick the recorded reason in the contract's order from what several
   tasks found (`IS C4`).
5. *`making-act?` takes the two facts "in either order".* A depot that partitions by "the entity of
   the first fact" (`C item 1`) then puts the base's making act on the task of `:base` or on the task
   of `:system`, by the order of its facts. The form is the builder's (`C item 9`), and the second
   part can fix the order.
6. *The last stamp is one value for each task of each gate.* `C rule 18` needs the greatest over the
   tasks on which an act writes before any of them is written.
7. *Check (a) needs the answer's home to follow from the act-id, and the depot places the act by an
   entity.* The same act-id twice in one batch, on two tasks (`IS X35`), meets only where the answer
   lives.

**One thing that this part asks of the second**, and the plan says it (`P` "For the second part"):
whether the other gate's record holds a layer must be one read on the task that the layer id gives.
It follows from the streaming gate's event, which must not leave its task between its first read and
its last write. With facts placed by entity, point 3 of "What holds" gives it.

---

The template's sections follow, each under its own heading.

## Query topology: answer-by-act-id

- Input examples present: yes, four.
- Example 1, an act that was decided: N = 1 read, M = 1. N == M? yes.
- Example 2, an act-id with no answer: N = 1, M = 1. N == M? yes. The read finds no row, and that is
  the output: "nothing yet" (`C item 8`). `keypath` on an absent key "navigates to nil (multiplicity
  1)" (skill pstate-schema.md, "PState Nil Semantics"), so the query still gives one value. No design
  can say "nothing yet" without this read.
- Examples 3 and 4, a thing that is no act-id of this gate: N = 0, M = 0.
- Any N > M: no.
- M values across examples: 1, 1, 0, 0. All same? no.
- Marked variable with a dynamic approach? yes: "the inputs that cost 0 are stopped by a test before
  the read; no read is issued and left empty" (`P` "Query Topologies"). The approach is a test before
  the one read.
- Verdict: PASS. One point was repaired around it: failure 9, on what the caller tests.

## Query topology: locks-of-request

- Input examples present: yes, four.
- Example 1, a request that made 64 locks, range 0 to 63: N = 1 seek and 64 iterations, M the same.
  N == M? yes.
- Example 2, range 10 to 10: N = 1 seek and 1 iteration, M the same. yes.
- Example 3, an act-id that made no lock: N = 1 seek and 0 iterations. Whether that seek is "wasted"
  is the template's question, so the other design was built. To avoid the seek the query would first
  read the request's fact at `"<layer> <uuid> :lock-request"`, which holds the count: 2 seeks for every
  request that made locks, 1 for one that made none. The plan's design costs 1 and 1. The seek cannot
  be saved without a seek that costs more often. And its empty result is the output: "nothing" before
  the request's answer is visible (`IS X97`). M = 1. yes.
- Example 4, arguments of the wrong shape: N = 0, M = 0.
- M values: 1 seek in every case that reads; the iterations vary with the range. All same? no.
- Marked variable with a dynamic approach? yes: "variable in the number of rows, fixed in the number of
  seeks. The variable part is one range navigator over neighbouring keys." The range is cut to 256
  rows.
- Verdict: PASS.

## Query topology: class-of-layer

- Input examples present: yes, three.
- Example 1, a layer that the streaming gate made: N = 1, M = 1. yes.
- Example 2, a layer that is not made: N = 1, M = 1. nil is the output (`IS X116`); no design says "not
  made" without the read.
- Example 3, a thing that is no id: N = 0, M = 0.
- M values: 1, 1, 0. Marked? yes: "fixed: 1 read", with the test before it.
- Verdict: PASS, with failure 9 repaired.

## PState schemas

- **Groups by key type and partitioner.**
  - `$$streaming-answers`, `$$streaming-facts`, `$$streaming-locks`: each has a `String` key, and each
    row is placed by the hash of the layer that its key begins with. They are a group of three by the
    template's test. Why they are not one PState with a field for each piece of data: there is no key
    at which an answer, a fact and a lock would be three fields. An answer is keyed by the layer and
    an act's uuid; a fact by the layer, an entity and a key; a lock by the layer, a request's uuid and
    an index. That is a difference in key structure, which is one of the two reasons that the template
    takes. The two ways to merge them all the same were built by the plan and costed: one PState keyed
    by the layer with three subindexed maps costs 6 to 8 reads for an act against 4 (`P` "The first
    question"); one flat PState with the kind in the key keeps the 4 reads and puts three kinds of row
    at one position, which the skill answers with an interface and a record for each kind
    (pstate-schema.md, "Storing Polymorphic Data"). I recounted the first: a read through a top-level
    key and then a subindexed map is 2 by the skill's model, and a write into a subindexed map under a
    top-level key reads the row above it. The plan's figures hold.
  - `$$streaming-task`: alone. It holds two pieces of data of one task in one fixed-keys value, which
    is what the template asks for.
- **Any `Object` type?** no. One class is *not verified* as a schema class, the byte array; the plan
  now says that the fallback is a declared class and never `Object`.
- **Uniform record-like values use a fixed-keys schema?** yes, all four.
- **Do instances at one position have different fields?** Each PState was examined.
  - Answers: `:refused` is absent for an accepted act. One shape with one field that can be nil.
  - Locks: `:seals-act` and `:seals-index` are absent until the lock seals a value. One lock, the same
    row, at two times of its life.
  - Facts: this is the one that the planner doubted (`P` "Design difficulty log", 2), and I examined
    it as a possible failure. A row has six fields. `:value` is nil for a retract. `:lock-act` and
    `:lock-index` are nil for a plain value under a store key. My verdict is that this is one shape
    with fields that can be nil, which the template lets pass in its own words: "a nullable field on
    instances that otherwise share the same shape is not this case". Three reasons. No row has a field
    that belongs to one kind of row only; every row is "the current fact of a place" with the same six
    names. The two things that can be nil are nil in the form itself: `act.clj` says "each fact holds
    its four parts; a fact that gave no `:v`, or a nil one, holds `:v nil` and is a retract", and the
    lock id of an arrived fact is optional in the declaration. And the contract gives the row as one
    shape with one slot for both: "`{value or sealed, the fact-id that wrote it, the stamp}`"
    (`C item 5`). PASS.
- **Inner collections that can pass 100 elements subindexed?** yes. No row of the three keyed PStates
  holds a collection. In `$$streaming-task`, the map from a reason word to a set has four keys at
  most, and code enforces it: the keys are the four words that `declaration/stray-reason` can give.
  Each set under it is subindexed.
- Verdict: PASS.

## Partitioning

**Each write, its partitioner, and why.**

- The rows of a layer (answers, facts, locks): the hash of the layer. The depot's `hash-by :layer`
  places a client's append; the gate's hash partitioner on the layer holds it for every thing.
- The stray's digest and the task's last stamp: no partitioner. They are the value of the task where
  the event is.
- `|all`: not used.

The template's test for `|hash`: is the keyspace large, and free of any key that takes a
disproportionate share? The keyspace is the layers: one for each person and one for each group, so
many on a task. A key can be hot: all the acts and all the rows of one layer are on one task, and a
busy group's layer is such a key. By the template that fails, unless the spec asks for it. The spec
does: "the streaming depot partitions by the act's `:layer`" (`C item 1`); "one event per act, on the
layer's task" (`C item 3`); and the implied spec says of it "a very large layer is one task's load.
That follows from the decision; it is a known limit, not a fault to design round" (`IS O1`). So the
placement is given, and the plan's reason for it is drawn from the spec alone. PASS.

**The table of `P` "Partitioning efficiency".**

- Filled in for N = 1, 16 and 128? yes.
- Every category of input as its own row, and the proportions sum to 1? yes: an act-id whose answer is
  recorded, 0.97; an act-id with no answer yet, 0.02; a thing that is no act-id of this gate, 0.01.
  The sum is 1.00. The proportions are the planner's assumption, and the plan says so.
- Weighted sums computed for each N? yes: 0.99 seeks and 0 iterator reads, three times.
- Is "seeks/op" the total across all tasks? yes. The read goes to one task, so the total is the one
  task's seek. No row is an `|all` read.
- Recomputed: 0.97 × 1 + 0.02 × 1 + 0.01 × 0 = 0.99 at N = 1, at N = 16 and at N = 128. It does not
  grow. The same holds for the gate's own reads, which are 4 on one task at every N.
- Are the reasons drawn from the spec only, and free of any assumption about the other part's
  mechanisms? yes. The plan's "For the second part" lists what that part gets and what is left to it,
  and says of itself "this plan designs none of its mechanisms". The one thing it asks of that part,
  one read on the layer's task, is a requirement on it and not a mechanism of it.
- A placement that stores state to help: not needed. Every read of this part names one layer, and
  one task for a layer is 1 seek on 1 task; there is no cheaper placement to look for.
- Verdict: PASS.

## Topologies

- **Microbatch unless justified?** The one topology is a stream topology, and it is justified three
  times: the decision must be visible within single-digit milliseconds (`IS O1`, from `R 7-Oct 4`);
  the door reads the answer right after the ack (`R 7-Oct 6`); and the contract names the type
  (`C item 3`).
- **Each write that must be visible in single-digit milliseconds, and its topology.** The rows of a
  decision: the stream topology. None is in a microbatch.
- **Each concern of the stream topology, and whether it needs stream semantics.**
  - The decision of an act and its rows: yes, for the two reasons above.
  - Making locks on a request: yes. The request is an act of the layer; its locks must be visible with
    its answer (`IS C3`), which the door reads right after the ack.
  - The sign of a stray: the test must be in the event, because it comes before check (a). The write
    of the sign does not need the latency or the ack. By the template's words that concern belongs in
    a microbatch topology. I pass it, for three reasons that are on the page. The plan built that
    topology and counted it: a second reader of the whole streaming depot, 8.64 million more record
    reads and about 5 GB read again each day on the example task, against 864 small writes
    (`P` "`$$streaming-task`", way 2). What the template guards against is a write that a retry
    repeats; this one is an add to a set, and a retry adds nothing. And the build session ruled it;
    I found no line of the spec and no fact of Rama that it breaks. Rule 10 asks for a set's
    behaviour in its own words: "once however often it comes".
- **Any choice made on grounds of test synchronisation or of what can be observed?** no.
- Verdict: PASS.

## Production readiness

- **Many clients at once?** yes. Scenario: two doors send two acts that both replace fact F of one
  layer. Both are on the layer's partition; the depot gives them an order; they cross the gate's
  partitioner in that order. The first reads F's row at check (e), is accepted, and writes the row.
  The second reads the new row, whether the first is committed or still pending in the same batch,
  because "reads inside the owning topology see its uncommitted PState writes" (skill stream.md). It
  is refused `:replaced-fact-not-current`. Two layers on one task share one thing, the task's last
  stamp, and an event reads and writes it with nothing between.
- **After a client restarts?** yes. The module keeps nothing that depends on a sender. A sender that
  lost its ack reads by act-id; with no answer it sends the same thing again, and the second copy is
  a repeat (`IS X26`).
- **If a worker restarts at any point of an event?** yes. The four cases of "An event that Rama runs
  twice" above cover it: the writes of a batch are discarded together or committed together.
- **At large scale?** yes. The three keyed PStates are top-level maps, which are on disk key by key.
  The strays are subindexed sets. Nothing is held in memory. Three things grow for ever by decision:
  the depot (`C rule 4`), the answers (`IS C2`) and the locks (`IS O6`).
- **Each write of the stream topology that is not idempotent, and its resolution.** No write appends
  to a list or adds to a counter. Two writes give other content on a second run: the bytes of a new
  lock, and the stamp. Both are *deduplicated by check (a)*: the answer row commits in the same batch
  as every other row of the decision, so a run after a commit ends before it writes, and a run after
  a discard is the first one that counts. The stray's digest is *made idempotent by being an add to a
  set*.
- **A stream topology that writes to several partitions: can a failure part-way leave a write
  undone?** no. After change 1 an act's tree has two events. The first writes nothing, unless the
  thing is a stray, and then it writes one element and ends. The second writes every row, on one
  task. No tree writes on two tasks.
- Verdict: PASS.

## Internal depot usage

None. The plan declares one depot, which clients append to.

## Cross-topology correctness

No data flows between topologies. The part has one.

## Stream topology correctness

No `depot-partition-append!` in the topology. Nothing to guard.

## In-memory state efficiency

No TaskGlobal and no cache. The one thing that a process holds is a `SecureRandom`, which keeps
nothing of the record. The last stamp is read from the record at each decision, by the build session's
ruling. PASS.

## Minimality — adversarial simplification

**The simplest design that I can build for this part.** One depot by layer. One stream topology that
looks up the act-id of each thing, checks the act against a few rows of its layer, and writes rows.
Maps keyed so that each lookup is one read. Three reads for the door. Set against it, the plan has
these mechanisms, and each must justify itself.

### The streaming depot
- **Delete it**: no act can enter. "Two depots, one per gate" (`C` F6 A).
- **Merge or bypass it**: the only thing to merge with is the other gate's depot, and F6 A keeps
  them apart. PASS.

### The stream topology `streaming-gate`
- **Delete it**: `C item 3` is the whole part. PASS.
- **Merge or bypass it**: nothing to merge with.

### `$$streaming-answers`
- **Delete it**: check (a) and the read by act-id go. "A gate reads its own record by act-id before
  it decides" (`R 7-Oct 7`).
- **Merge or bypass it**: into the facts map, under a key of its own. That is one position with two
  kinds of row and saves no read. PASS.

### `$$streaming-facts`
- **Delete it**: "the layer's current facts" (`C item 5`), and checks (d) and (e).
- **Merge or bypass it**: the two merged designs are costed under "PState schemas". PASS.

### `$$streaming-locks`
- **Delete it**: "the locks" (`C item 5`); `C rule 5`.
- **Merge or bypass it**: the lock's bytes into the row of its value. The plan built it: a lock is
  read by its id in each state (`IS O9`), and a fact that is replaced leaves the facts map while its
  lock must stay (`IS O4`). A table by lock id is needed in any case. PASS.

### `$$streaming-task`, the last stamp
- **Delete it**: "strictly increasing on its task" (`C item 6`) does not hold through a restart
  (`IS X19`).
- **Merge or bypass it**: a copy in memory would take one read of four off each act. It is the
  project's caching rule and the build session's ruling that it is not taken. PASS.

### `$$streaming-task`, the strays
- **Delete it**: "the gate keeps its digest under a reason word, on its task" (`C rule 10`).
- **Merge or bypass it**: it is already in one PState with the last stamp. PASS.

### The three query topologies
- **Delete them**: the door cannot read (`C items 7 and 8`; `C` "The proof", "reads the class").
- **Merge or bypass them**: two of the three could be one foreign select each in this part. What is
  lost: the scope asks that they be built "in a form that the next stage can complete for the other
  class" (`DECOMPOSITION.json`, point 10), and a client's select of one gate's PState is not such a
  form. The answer by act-id is named a query topology by the contract. PASS.

### The operator's select of strays
- **Delete it**: the sign of a stray is then kept and never read.
- **Merge or bypass it**: it is one foreign select, the least there is. PASS.

### The test for a stray, before check (a)
- **Delete it**: `C rule 10`, "one act-id never gets two answers". PASS.

### The one text and the digest
- **Delete it**: `C rules 1 and 11`.
- **Merge or bypass it**: a digest of the serialised bytes. The door does not hold the bytes that the
  cluster makes, and a serialiser may change what no check can see. PASS.

### The stamp, its packing, the 10-second rule
- **Delete it**: `C item 6`; `C rule 17`. PASS.

### The request for locks as an act with a count; a lock's id from its request
- **Delete it**: `C item 7`. An id made inside a stream topology differs on a retry (skill
  unique-ids.md), so the ids come with the record. PASS.

### The way of sealing in the lock's row, and the word `:lock-way-not-known`
- **Delete it**: `C rule 16`, "a lock's row says which way of sealing the lock is for". The word is
  what the code answers for a number that it does not know; `C rule 8` asks that it say what it is.
  PASS.

### The gate's one partitioner (added by this check)
- **Delete it**: the trace "A thing on the wrong partition" above; `IS C2`; `IS C10`.
- **Merge or bypass it**: a test of the task without going there. The references give the task's own
  id (`ops/current-task-id`) and no operation that gives the task of a key, so there is nothing to
  compare with. PASS.

### The nesting limits as a second argument of `act.clj`'s engine
- **Delete it**: nothing breaks. `act.clj`'s own limits apply by the name of a shape, and no shape of
  the door's is inside itself. **FAIL.** Removed from the plan (failure 4).

### `:sealed` set aside from the engine's pass
- **Delete it**: the engine refuses every sealed fact as `:unknown-part`.
- **Merge or bypass it**: a leaf for bytes, which the ruling as it stands does not give. Inside the
  ruling there is no other way; the plan asks the build session. PASS.

### Retry mode "all after", and the start from the beginning
- **Delete the first**: the order among the records that are retried is then not kept: "can't
  guarantee this" (docs 11:158).
- **Delete the second**: a thing that is in the depot before the topology first runs on a partition
  is skipped: "by default it starts processing from the end" (docs 11, "Start from options"). PASS.

### The pure namespaces
They add no depot, no topology and no PState. They are how the code is cut, and the next part calls
the same functions. Not a mechanism of Rama; nothing to delete.

## Throughput — adversarial

**The accepted act: 4 reads.** For each read I built the design without it.

- Without the read of check (a): F7 A asks for it, and a retry would then write twice.
- Without the read of the permission: check (d) is then not made.
- Without the read of the lock: the gate cannot open the value (`C rule 16`), and cannot tell a
  second use.
- Without the read of the last stamp: only from memory. Not taken, by the ruling.
- With the three keyed PStates as one PState keyed by the layer: 6 to 8 reads.

No design that I can build does less. One note on the first read: for a new act-id the key was never
written, and the plan's argument that such a read is answered from memory is an argument and not a
measure, as the plan says.

**The read by act-id: 1 read, one roundtrip, routed by the client.** It cannot be less.

**The act under a permission that does not stand: 4 reads, and 3 are enough when the place of `:who`
holds another permission.** FAIL as found. Repaired (failure 6).

**The seal's range read.** One seek and one iteration for each index between the lowest and the
highest that the act uses. An act that uses index 0 and index 255 of one request iterates 256 rows,
about 1.3 ms, where two point reads cost two seeks, 0.6 to 1.0 ms. The two are of one size, and the
range is never worse than 256 rows. PASS.

**Verdict:** PASS after the repair of failure 6. F9's first target is not met by the arithmetic of
cold reads, and the plan says so; `C rule 21` asks the plan to say what it aims at, and the targets
are Sid's "to optimise toward" (`R 7-Oct 9`).

## Spec coverage — trace every operation and constraint

Three groups of blocks: the contract's items for this part; the rules of the implied spec that hold
for every operation, with three of its edge cases; and the builder's rules 1 to 21, each quoted. The
operations of the implied spec are traced inside them: O1 in items 1 and 3; O3 in item 9; O4 in checks
(e) and (f) and item 7; O5 in checks (d) and (g); O6 and O9 in item 7; O7 in check (a); O8 in item 8;
O10 in the block of that name. O2 is the next part's.

**What is the same in every block.** It is given once here, and each block says only what is its own.

- *Memory cleared by a restart.* The plan holds no state in the memory of a process. Every row that a
  check reads is in a PState, and after a restart the gate reads the same rows.
- *A retry.* A run after a commit reads the answer at check (a) and ends: 1 read, no write. A run
  after a discard finds nothing at check (a) and is the first decision that counts. So a retry
  doubles nothing. The four cases are traced above.
- *A write on several partitions that fails part-way.* The event that decides writes on one task, and
  a streaming batch commits whole or not at all (docs 11:140, 11:170).
- *Two clients on one entity.* The depot's partition orders them, and the second sees the rows that
  the first wrote.
- *Out of order by a hop.* Each thing that is no stray crosses the gate's one partitioner, which
  keeps the order of a partition's things (docs 16:129). Across a failed batch the order of the
  decisions can differ from the depot's order; that is the block `IS X29`.

### The contract's items

#### C item 1: the streaming depot
- **Source**: "The streaming depot partitions by the act's `:layer`."
- **Trace**: `*streaming-depot`, `(hash-by :layer)`. A client appends M1 with `:layer :alice`; the
  client computes the partition from the thing; it is task 5 of 16. A thing that is no map, such as
  the number 7: the keyword gives nil, and the partition is the one that the hash of nil gives; the
  gate there finds a stray. A thing on which the keyword throws: the throw is the appender's (skill
  depot-design.md: "depot partitioners run on the appending client").
- **Fault tolerance**: the depot is Rama's log; nothing of the plan's is in memory.
- **Races**: none of its own.
- **Flaws found**: the plan read the depot's partitioner as a guarantee about where every thing sits.
  It is one for a client and not for a topology (docs 14:254). Failure 1.
- **Verdict**: FAIL as found.
- **If FAIL**: the gate goes to the layer's task itself. Done: change 1.

#### C item 2: the door's declaration
- **Source**: "a small shape, declared as data in the same style as `act.clj`'s `shapes` (reuse its
  engine by requiring `softland.store.act`; for that, and for nothing else, `act.clj`'s engine may
  take the map of shapes as an argument, with `parse`, `has-shape?` and the act's proof unchanged):
  what arrives on a depot is the maker's act plus `:act-id`, `:class`, and each value fact's `:v`
  replaced by `:sealed` bytes and a `:lock-id`. Facts under a **store key** keep a plain value."
- **Trace**: the door's map of shapes is `act/shapes` with five entries added; `:arrived-act` is the
  two parts of the door and then the maker's ten, read from `act/shapes`. A thing with `:version 2`
  gives `:unknown-version`, `act.clj`'s own word, because the part is `act.clj`'s own entry. A thing
  with a part `:owner` gives `:unknown-part`. A fact under a plain key with a plain `:v` gives
  `:not-sealed` (rule 12 of the declaration). A fact under `:class` with a seal gives
  `:store-key-sealed` (rule 5). The three store keys are declared in one table with the reason for
  each.
- **Fault tolerance**: the check is a pure function; the same thing gives the same word on every run.
- **Races**: none.
- **Flaws found**: two. The engine was given the nesting limits as a second argument, which the
  sentence above does not give and which nothing needs (failure 4). And step 1 of the check set
  `:sealed` aside from "each fact": on a thing with `:version 2` and `:facts [7]` that step throws
  before the engine runs, the throw becomes `:malformed`, and `act.clj` gives `:unknown-version` for
  the same thing (failure 5).
- **Verdict**: FAIL as found.
- **If FAIL**: the engine takes the map of shapes only; the step sets aside only from a plain map
  inside a sequence. Done: changes 4 and 5. What stays the plan's ask to the build session: a leaf
  for `:sealed`.

#### C item 3(a): the record already holds an answer
- **Source**: "(a) the record already holds an answer under this act-id → return it, decide nothing
  (F7; builder's rule 1)".
- **Trace**: A1 is sent a second time. Step 1 reads `":alice u-a1"` and finds
  `{:stamp 1020·0, :digest d(A1)}`. The event ends. No digest is taken, no row is written. The door
  reads the same stamp as before. A different thing under `u-a1` ends at the same step; its sender
  reads `d(A1)`, which is not the digest of what it sent.
- **Fault tolerance**: this check is what makes a retry safe; see the cases above.
- **Races**: the same thing twice in one streaming batch. The second event reads the first's row
  before it is committed, because the owning topology reads its own pending writes (skill stream.md).
  One decision.
- **Flaws found**: none found, with reasoning: the answer row is written in the same event as every
  other row of the decision, so there is no state in which an act's rows are in the record and its
  answer is not.
- **Verdict**: PASS.

#### C item 3(b): the door's declaration, on arrival
- **Source**: "(b) the door's declaration (which contains `act.clj`'s check)".
- **Trace**: a thing with a good act-id and nothing else of an act, `{:act-id [:alice :streaming u-q]
  :layer :alice}`. No stray. Check (a): nothing. The digest is taken. The engine reads `:arrived-act`:
  `:class` is absent, so `:class-not-the-act-ids-class`. Step 7: stamp; the answer
  `{:refused :class-not-the-act-ids-class, …}` and the last stamp are written. 2 reads, 2 writes.
- **Fault tolerance**: as the common part. A thing that is refused here is refused the same way on
  every run: no clock and no row is read before step 3.
- **Races**: none.
- **Flaws found**: the two of C item 2.
- **Verdict**: FAIL as found, with C item 2; repaired by changes 4 and 5.

#### C item 3(c): the layer's class is this gate's
- **Source**: "(c) the layer's class is this gate's".
- **Trace**: A1 in the made layer `:alice`: the permission's row is found, and with it the layer is
  this gate's, because a row of `$$streaming-facts` is written only by an accept, an accept is in a
  layer that this gate made or makes, and a making act is right only for the gate of the class that
  the layer starts with. A making act for `:base` with the act-id `[:base :streaming u]`: both places
  empty; `making-act?` is false, because the streaming gate's class is not the class that `:base`
  starts with; `:layer-not-made`. The word `:layer-belongs-to-the-other-gate` needs a made layer of
  the other class, which only the next part can make (`IS X31`).
- **Fault tolerance**: the class is a row; nothing in memory.
- **Races**: two making acts for one layer in one batch: the second finds the first's rows pending and
  is refused at (g) or at (d).
- **Flaws found**: none found for this part's share, with reasoning: the invariant "this gate's
  record holds fact rows only for layers of its class" holds by how a fact row can come to be, and no
  act of this build changes a class. The read that told nothing is counted under (d).
- **Verdict**: PASS. `IS X31` is left open by the scope (`DECOMPOSITION.json`, "OPERATIONS OWNED").

#### C item 3(d): the act's permission stands
- **Source**: "(d) the act's permission stands: the record holds it, each permission above it in its
  chain stands, and its holder is the act's `:who` (E4 A) — or the act is the layer's first act
  (item 9)".
- **Trace**: Alice gives `[:agent-1 :alice :alice [:alice :alice :alice]]`; the row is at
  `":alice :agent-1 :permission"`. An act by `:agent-1` under it: one read of that place; the row's
  bytes are the text of the act's permission; pass. An act by `:mallory` under Alice's permission: the
  place `":alice :mallory :permission"` is empty; the class's place holds a row; the holder slot of
  the act's permission is `:alice`, not `:mallory`: `:who-not-the-permissions-holder`. An act by
  `:agent-1` under a permission that was never given: its place holds another permission; refused
  `:permission-does-not-stand`. "Each permission above it stands" is not read. It holds by how a
  permission row can come to be: a right making act writes one with nothing above; a give writes one
  straight beneath the giver's own, which stood at that decision; no act ends one. That is the build
  session's ruling, and I checked the induction: its three steps are the three ways a row under
  `:permission` is written or kept, and rule 13 closes the fourth, a second write on the place.
- **Fault tolerance**: the rows are durable; a give and the first act under it are ordered by the
  depot.
- **Races**: a give and an act under it in one batch: the act is accepted only if it is after the
  give in the depot.
- **Flaws found**: one. In the third case above the plan read the class's place although the row at
  the permission's place had already shown that the layer is made (failure 6).
- **Verdict**: FAIL as found.
- **If FAIL**: no read of the class when the permission's place holds a row. Done: change 6.

#### C item 3(e): each `:replaces` is the current fact
- **Source**: "(e) each `:replaces` is the current fact for its entity and key (builder's rules 2 and
  3)".
- **Trace**: A2 above: the place holds `u-a1`, 0; the fact names `u-zz`, 0; refused. A fact that
  names `[[:alice :streaming u-a1] 0]` on that place: the row's `:by-act` and `:by-index` are the
  same; pass; the row is written over. A fact that names a fact of another layer or of the class
  `:microbatch`: stale without a comparison of rows. A fact that names nothing: no read; the row is
  written.
- **Fault tolerance**: as the common part.
- **Races**: two acts replace one fact: the first in the depot's order wins; the second reads the
  first's row.
- **Flaws found**: none found, with reasoning: the fact id that a row keeps, the uuid and the index,
  is whole, because the layer is in the key and the class is the PState's.
- **Verdict**: PASS.

#### C item 3(f): no setting key on another entity
- **Source**: "(f) no setting key on an entity other than the layer (F8)".
- **Trace**: an act by Alice with `{:e :bob :k :class :v :streaming}`: (c), (d) and (e) pass; (f):
  `:class` is a setting and `:bob` is not the layer; `:setting-not-on-the-layer`. No read.
- **Fault tolerance**, **races**: as the common part.
- **Flaws found**: none found, with reasoning: the one setting of this build is declared as such in
  the table of store keys, and the check uses only the act.
- **Verdict**: PASS.

#### C item 3(g): a permission fact writes a permission beneath the act's own
- **Source**: "(g) a fact under `:permission` writes a permission beneath the one that the act itself
  acts under, in the act's layer, and nothing else (E2 A)".
- **Trace**: Alice's give above: the value's fourth slot is exactly the act's permission; its layer
  and its `granted-in` are `:alice`; the place `":alice :agent-1 :permission"` is read and is empty;
  accepted. A give whose fourth slot is a permission that the giver does not act under:
  `:permission-fact-not-beneath-the-acts-own`. A permission with nothing above it, in a made layer:
  the same word. A `granted-in` of another layer: `:permission-fact-granted-in-another-layer`.
- **Fault tolerance**, **races**: as the common part.
- **Flaws found**: one. A right making act writes a permission with nothing above it, which is its
  own. Step 5 as written ran (g) for every act, so the making act M1 above would have been refused
  (failure 2).
- **Verdict**: FAIL as found.
- **If FAIL**: say that a right making act is not held to (g) or to the store-key step. Done:
  change 2.

#### C item 3: the accept, and the two refusals that are "not decided"
- **Source**: "then accept: write the facts, their new versions, and the answer under the act-id, with
  a stamp. Not decided, so refused in this build with a word that says so: a retract of a permission
  (who may end one) and a change of a layer's class (re-class, ?9)."
- **Trace**: the accept of A1 above: four rows in one event. A fact `{:e :agent-1 :k :permission}`
  with no value: `:ending-a-permission-not-decided`. A fact `{:e :alice :k :class :v :microbatch}` in
  the made layer: `:changing-a-class-not-decided`.
- **Fault tolerance**: the rows of an accept commit together.
- **Races**: none of its own.
- **Flaws found**: none found, with reasoning: both words say "not decided", and both are tested
  after (g), where the contract's own sentence has them.
- **Verdict**: PASS.

#### C item 5: the record
- **Source**: "PStates, per task: the layer's current facts (`entity → key → {value or sealed, the
  fact-id that wrote it, the stamp}`); answers by act-id (`act-id → {:accepted | :refused reason,
  stamp}`, with the digest of builder's rule 1); the layer's settings as facts on its own entity (the
  class); permissions as facts; the locks."
- **Trace**: each part is a row above: the fact at `":alice :note-1 :text"`; the answer at
  `":alice u-a1"`; the class at `":alice :alice :class"`; a permission at `":alice :alice
  :permission"`; a lock at `":alice u-r1 000"`. Entity and key are one sorted key, so a place is one
  read and the keys of an entity are neighbours.
- **Fault tolerance**: PStates; nothing in memory (`IS C8`).
- **Races**: none of its own.
- **Flaws found**: none found, with reasoning: under "PState schemas".
- **Verdict**: PASS.

#### C item 6: the stamp
- **Source**: "an integer the gate assigns, strictly increasing on its task, later than each stamp
  that the act carries, below 2^62. It holds the clock time and a counter (H2 A)."
- **Trace**: the stamps above: `1000·0`, `1005·0`, `1020·0`, `1031·0`. Two decisions at clock 1,031:
  the second is `1031·1`, the last plus one. The clock steps back to 900: the next is `1031·2`. An act
  that carries `1040·3`, 9 ms ahead of a clock at 1,031: the stamp is `1040·4`. An act that carries a
  stamp 11 seconds ahead: refused `:carried-stamp-too-far-ahead`, with an ordinary stamp. Recounted:
  2^62 ÷ 65,536 = 2^46 ms, which is about 2,230 years, so the packing ends in the year 4199; 65,536
  stamps fit one millisecond.
- **Fault tolerance**: the last stamp is read from `$$streaming-task` at each decision, so a restart
  and a move of the leader keep it. A run that was discarded took its stamp with it.
- **Races**: two layers on one task: one value, read and written by one event with nothing between;
  the second event of a batch sees the first's pending write.
- **Flaws found**: none found, with reasoning: the next stamp is the greatest of three numbers, and
  two of them are "last plus one" and "carried plus one", so both "strictly increasing" and "later
  than each carried stamp" hold by the form of the function.
- **Verdict**: PASS.

#### C item 7: locks and the seal
- **Source**: "The module makes locks on request. The request is an act with a fact under
  `:lock-request` … The door reads a lock's bytes by a query … The gate refuses a plain value under a
  non-store key (`:not-sealed`), a sealed value whose lock the store did not make for this layer, and
  a second value under a lock that already seals one."
- **Trace**: R1 and A1 above. A second act with a value under `[… u-r1] 0`: the row names `u-a1`, 0;
  `:lock-already-seals-a-value`. An act in the layer `:bob` that names `[[:alice :streaming u-r1] 1]`:
  the lock id's layer is not the act's; `:lock-not-made-for-this-layer`, with no read. Sealed bytes of
  `:note-1` copied to `:note-2` under a free lock: the place is the associated data, the tag does not
  hold, `:lock-does-not-open-the-value`. `locks-of-request` for `u-r1`, range 0 to 1: one seek, two
  rows, the bytes and the way of each.
- **Fault tolerance**: the four cases above; rule 5 is its own block below.
- **Races**: two acts under one lock in one batch: the second reads the first's pending write of the
  lock's row.
- **Flaws found**: none found, with reasoning: a lock's row is written at two moments only, when its
  request is accepted and when an accepted act uses it; both are behind check (a); the second writes
  the bytes as they were read.
- **Verdict**: PASS. The ruling on the way is applied.

#### C item 8: the answer by act-id
- **Source**: "a query topology: act-id → the recorded answer, or nothing yet."
- **Trace**: after the ack of A1, the hand calls `answer-by-act-id` with `:alice` and the act-id. The
  client hashes `:alice` and sends the query to task 5. The query tests that `:alice` is the layer
  slot; reads `":alice u-a1"`; gives `{:verdict :accepted, :reason nil, :stamp …, :digest "…"}`. For
  `u-none`: nil. For an act-id of the class `:microbatch`: nil, with no read, in this part.
- **Fault tolerance**: a query reads committed state; an answer that was read stays (`IS C2`).
- **Races**: a read during the decision gives nothing or the whole answer; the batch becomes visible
  at once.
- **Flaws found**: one. For a first argument that is no id the plan promised nil, and the first
  argument is hashed by the client before any test of the plan's runs (failure 9). And one thing that
  is not a flaw but is the plan's own form: the query takes the layer beside the act-id. The act-id
  alone still finds the answer, since the layer is its first slot.
- **Verdict**: FAIL as found.
- **If FAIL**: the caller tests the shape before the call. Done: change 9.

#### C item 9: the first act of a layer
- **Source**: "A gate accepts an act into a layer for which the record holds nothing when it is a
  making act: it writes the layer's class and one permission, the layer's first, with nothing above
  it, and it names that permission as the one it acts under. … Any other layer: class streaming
  (F4 C); the act's `:who` is the holder of the first permission … or `:system` … Any other act into a
  layer that is not made is refused."
- **Trace**: M1 above, a personal layer made by `:system`. A group's layer `:crew` made by `:bob`:
  `:who :bob`, permission `[:bob :crew :crew]`; the place `":crew :bob :permission"` and the class's
  place are empty; accepted. A value act into `:nowhere`: both places empty; no making act;
  `:layer-not-made`. A making act with `:who :mallory` and the permission `[:bob :crew :crew]`: `:who`
  is neither the holder nor `:system`; `:layer-not-made`. M1 sent again under a new act-id: `:who
  :system` holds no permission in `:alice`; the class is there; `:who-not-the-permissions-holder`.
- **Fault tolerance**: the class, the permission and the answer commit together, so no reader sees a
  class without an accepted answer (`IS O3`).
- **Races**: two making acts for one layer: one after the other on the layer's task.
- **Flaws found**: the one of (g), failure 2. And before change 1, a making act on another partition
  made the layer a second time (failure 1).
- **Verdict**: FAIL as found; repaired by changes 1 and 2.

### The rules of the implied spec that hold for every operation

#### IS C1: no exception on what arrives
- **Source**: "each check is 'a recorded refusal word, never an exception' … this reaches every step
  that touches what arrived, not only the checks".
- **Trace**: the string `"x"` on the depot: the stray test gives `:no-act-id`; the one text is
  `"x"` in quotes; one set element. A map with a good act-id and `:facts 7`: the engine gives
  `:malformed`. Sealed bytes of 5 bytes, too short to hold a random part and a tag: the opening gives
  nothing; `:lock-does-not-open-the-value`. A row whose field has another class than the schema's
  would throw in the topology (skill troubleshooting.md); rows are made by one namespace that gives
  the declared classes.
- **Fault tolerance**: a thing that threw would stop its partition on every retry; that is why this
  rule is first.
- **Races**: none.
- **Flaws found**: the test for a stray was not said to be total (failure 5). A thing on which a
  lookup of `:act-id` throws reaches the gate only by a topology's append, since a client's
  partitioner would throw first.
- **Verdict**: FAIL as found; repaired by change 5.

#### IS C2: one act-id, one answer, for ever
- **Source**: "once the record holds an answer under an act-id, nothing changes it: not a repeat, not
  a retry by Rama, not a different act under the same act-id. An answer is never deleted".
- **Trace**: the answer of A1 through a repeat, a retry and a different thing: check (a) each time.
  No path of the plan writes an answer row a second time or deletes one.
- **Fault tolerance**: the cases above.
- **Races**: the same act-id twice in one batch: check (a) on the pending row.
- **Flaws found**: one. A thing on another partition could get an answer there, and the same act-id
  another on the layer's task (failure 1).
- **Verdict**: FAIL as found; repaired by change 1.

#### IS C3: whole or nothing, and visible together
- **Source**: "a reader sees all of these or none of them. No reader sees an accepted answer before a
  fact of that act, or a fact before the answer."
- **Trace**: A1's four rows are written in one event on task 5; "all writes to PStates from all events
  during one of these batches are made visible at the same time" (docs 11:140). A2's refusal writes
  the answer and the task's stamp.
- **Fault tolerance**: a failed batch discards all four.
- **Races**: a query between two of the four writes sees none of them.
- **Flaws found**: none found, with reasoning: after change 1 the first event of an act writes
  nothing, so all of an act's rows are still in one event.
- **Verdict**: PASS.

#### IS C4: the order of the checks is the order of the reasons
- **Source**: "when a thing has two faults, the check that comes first gives the recorded reason".
- **Trace**: an act with an unsealed value and a permission that does not stand: `:not-sealed`, from
  (b), before (d). An act with a stale `:replaces` and a lock that is used: `:replaced-fact-not-
  current`, from (e), before the seal. The plan places what the contract does not: the 10-second
  rule after (b), the store-key step after (g), the seal last.
- **Fault tolerance**, **races**: none of its own.
- **Flaws found**: the one of C item 2 (failure 5).
- **Verdict**: FAIL as found; repaired by change 5.

#### IS C5: a repeat does not depend on the record of the moment
- **Source**: "the answer to a repeat is the recorded answer, even when the record has moved since".
- **Trace**: A2 is refused as stale; later the place moves so that A2 would pass; A2 is sent again:
  check (a) gives the refusal with the stamp `1031·0`. A1 again: accepted, although its lock now
  seals a value; the seal step is not reached.
- **Flaws found**: none found, with reasoning: check (a) is before every check that reads the record.
- **Verdict**: PASS.

#### IS C6: the digest
- **Source**: "the same thing gives the same digest in every process, on every node, after every
  restart. Two different things do not give the same digest in practice."
- **Trace**: A1 as the door holds it and as the gate gets it: the same one text, part by part; the
  same SHA-256. An `Integer` 7 and a `Long` 7 have one text, and no check tells them apart. A map
  with two keys that are symbols: both keys have the text `#other clojure.lang.Symbol`.
- **Fault tolerance**: the function reads nothing but the thing.
- **Flaws found**: one. For that last map the plan sorted the entries "by the text of the key", and
  two keys with one text leave the order to the map (failure 7). Such a thing is always refused, so
  the harm is a refused sender that is told its act-id is used by another act.
- **Verdict**: FAIL as found; repaired by change 7.

#### IS C7: the stamp
- **Source**: as C item 6, and "one decision, one stamp … The plan says how many stamps one clock tick
  holds, and in which year the packing reaches 2^62."
- **Trace**: under C item 6.
- **Verdict**: PASS.

#### IS C8: the record is durable
- **Source**: "nothing that a later decision or a later read needs lives only in the memory of one
  process, unless it is built again from durable state before it is used."
- **Trace**: the five things that a decision needs are five rows. The plan declares no TaskGlobal.
- **Verdict**: PASS.

#### IS C9: the acts depots are never trimmed
- **Source**: "each thing that is appended stays for ever … nothing secret goes on a depot".
- **Trace**: the module sets no trimming option; "by default depots store all data permanently"
  (skill depot-reference.md). A request carries a count; the event appends to no depot.
- **Verdict**: PASS.

#### IS C10: the gate is the authority
- **Source**: "no check of a gate assumes an honest door. The gate checks the act-id, the class and
  the seal again." And `R 7-Oct 8`: "anything can append to a depot without the door".
- **Trace**: a thing with the class slot of the other gate: a stray. A thing whose `:class` is not
  its act-id's: refused. A plain value: refused. A thing that a topology put on another partition:
  the trace above.
- **Flaws found**: the gate assumed where the thing sits (failure 1).
- **Verdict**: FAIL as found; repaired by change 1.

#### IS X16: which values Rama carries unchanged
- **Source**: "for the plan to check: which values Rama carries unchanged … The digest (C6), the index
  of a fact (X13) and the plain value of a store-key fact rest on it."
- **Trace**: the plan found two sentences in the docs and nothing on byte arrays or lists; I found
  the same (docs 30). It makes the proof's first check the answer: a thing with each kind of value is
  appended, read back from the depot's log, and its digest compared.
- **Flaws found**: none found, with reasoning: the design is not bent to an unknown; the build stops
  if a kind does not come back as sent.
- **Verdict**: PASS.

#### IS X29: an act is not decided after a later act of its layer
- **Source**: "an act of a layer that is not yet decided is not decided after a later act of that
  layer. The plan shows this from Rama's guarantees; it does not assume it." And the scope: "what the
  plan must find out or say for this gate: … implied spec X16 and X29" (`DECOMPOSITION.json`,
  point 12). And `C rule 19`: "a rebuild of the record from the depots is not built and not tested
  here. For the day that one is asked for, the plan says what it found".
- **Trace**: three acts X, Y, Z of one layer are in flight. X is in a batch that throws. Its writes
  are discarded (docs 11:170). Y and Z are already queued as events on the task; they run in the next
  batch and commit. Then X is retried, and with "all after" Y and Z after it (docs 11:162). X is
  decided against a record that Y and Z have written; Y and Z end at check (a). If X and Y replace
  one fact, Y won, and a replay of the depot in its order would have X win.
- **Fault tolerance**: this is the fault. One answer for each act-id still holds, and so does whole
  or nothing.
- **Flaws found**: the sentence cannot be shown from Rama's guarantees under Rama's default, and the
  plan said so. What it said beside that was wrong in one point: that the window cannot be closed
  (failure 3).
- **Verdict**: FAIL as found, on the finding. As the plan stands it says what Rama gives, names the
  setting that would close the window and its price, and sends the choice to Sid. That is what the
  scope and rule 19 ask of this part. Whether the order is to be bought at that price is not this
  check's to rule and not the plan's.
- **If FAIL**: done: change 3.

#### IS X31: an act names a layer of the other class
- **Source**: "refused at check (c): the layer belongs to the other gate."
- **Trace**: no layer of the class microbatch can be made before the next part. The step that asks
  the other gate's record is in the walk and gives "no".
- **Verdict**: left open by the scope: "O1 of the implied spec, whole, with one case left open: X31"
  (`DECOMPOSITION.json`). Not a failure of this part.

#### IS O10: read a layer's class
- **Source**: "it gives nothing for a layer that is not made, and the class for a layer that is made
  … visible together with the accepted answer of the making act."
- **Trace**: `class-of-layer` for `:alice` after M1: one read of `":alice :alice :class"`; the value's
  text is read back as `:streaming`. Before M1's batch is visible: nil. For `:nowhere`: nil.
- **Flaws found**: the one of C item 8, on a first argument that is no id (failure 9).
- **Verdict**: FAIL as found; repaired by change 9.

### The builder's rules, 1 to 21

Each rule is quoted from the contract as it stands. Then where the plan meets it, or where it did not.
The fault-tolerance and race points are those of the common part unless a block says more.

#### Rule 1
- **Source**: "A second, different act under a used act-id must not read back the first act's answer
  as its own. Each answer keeps a digest of its act; the gate tells a repeat from a different act by
  it."
- **Trace**: A1 is accepted with the digest `d(A1)`. A forger sends another thing under
  `[:alice :streaming u-a1]`. Check (a) finds the row; the event ends; nothing is written. The forger
  reads `d(A1)`, takes the digest of its own thing, and sees that they differ.
- **Flaws found**: none found, with reasoning, and with one thing said plainly. In the plan the gate
  does not take the digest of the second thing, so it is the sender and not the gate that tells the
  two apart. Rule 11, the later and narrower rule, says exactly that: "The sender compares it with the
  digest of what it sent. The gate writes nothing for a second, different act." The gate does the same
  for a repeat and for a different act, which is nothing, so there is nothing that it would do with
  the difference.
- **Verdict**: PASS.

#### Rule 2
- **Source**: "A fact that names nothing to replace is never refused as stale; it becomes the current
  fact of its place."
- **Trace**: a second value on `:note-1`'s `:text` that names nothing: no read of the place; the row
  is written over A1's. A1's lock row still names A1's fact (`IS E2`, `IS E4`).
- **Verdict**: PASS.

#### Rule 3
- **Source**: "A retract is recorded as the current fact of its place, so that a later fact can replace
  it."
- **Trace**: `{:e :note-1 :k :text}` with no value and no seal, in an act `u-a3`: the row becomes
  `{:by-act u-a3, :by-index 0, :stamp …}` with no value. A later fact that names `[… u-a3] 0` passes
  (e).
- **Verdict**: PASS.

#### Rule 4
- **Source**: "The acts depots are never trimmed. That is Rama's default; F2 A rests on it."
- **Trace**: no trimming option is set; "by default depots store all data permanently" (skill
  depot-reference.md, "Depot Trimming"). *Checked.*
- **Verdict**: PASS.

#### Rule 5
- **Source**: "A lock's bytes are made inside the module and are never appended to a depot (the
  finding: the log keeps every appended value). Neither a retry nor a replay makes a lock again for a
  value that is already sealed."
- **Trace**: B0 is made in R1's event and written to `$$streaming-locks` only. R1 holds the count 2.
  The event appends to no depot. The retry of R1 after its commit ends at check (a): B0 stays. The
  retry of R1 after a discard makes B0′: no value was sealed under B0, because no reader saw it. A
  replay is not built (`C rule 19`); the lock's row keeps the fact that it seals, which is what a
  replay would need to tell "the same act" from "a second use".
- **Flaws found**: none found, with reasoning: the only step that makes bytes is behind check (a), and
  the bytes and the answer commit in one batch.
- **Verdict**: PASS.

#### Rule 6
- **Source**: "In one batch of the microbatch gate, two acts that replace the same fact get the same
  winner on every attempt, and an act that touches several tasks is accepted whole or refused whole.
  Do not send all acts through one task unless the plan shows that it must (the Rama skill's first
  goal; open item ?15)."
- **Trace**: the next part's. What this part owes it is under "The record as the interface for the
  second part": the rows name no task, and the checks take rows.
- **Verdict**: PASS for this part's share: nothing here shuts it out.

#### Rule 7
- **Source**: "Either gate can tell which gate a layer belongs to. A microbatch act's answer is found
  from its act-id alone (Q22 A, F7 A)."
- **Trace**: the streaming gate tells its own layers by their rows, and has the step that asks the
  other gate's record. A streaming act's answer is found from the act-id: its layer slot gives the
  task, and the layer and the uuid give the key.
- **Verdict**: PASS for this part's share. `IS X31` and the microbatch half are the next part's.

#### Rule 8
- **Source**: "Each word that the build makes — a reason that a gate records, a key, an id — says in
  plain words what the thing is. No word comes from the rig's code. The module's README lists them
  under 'Words a session made, Sid's to change'."
- **Trace**: I counted the plan's list: 4 words of a stray, 18 of the declaration, 18 of the gate; 40,
  as the plan says. Each has a line that says what it is. None is "offer", "by-layer", "lease", or
  "name" for an act's id. I did not read the rig, so "no word comes from the rig's code" is held here
  by the four words that the contract names. Change 1 adds no word. The ruling moves `:aes-256-gcm`
  out of the rows and into code; the list says so.
- **Verdict**: PASS.

#### Rule 9
- **Source**: "To find out, and to say in the plan: whether a rebuild from the acts depots picks the
  same winner when two microbatch acts on different entities collide."
- **Trace**: the next part's. The like question for this gate is `IS X29`.
- **Verdict**: PASS for this part's share.

#### Rule 10
- **Source**: "A gate answers under an act-id only when the act-id's class is the gate's and its layer
  is the act's layer. Anything else that arrives — a thing with no act-id, an act-id of the wrong
  shape, an act-id of the other gate or of another layer — is a stray: the gate keeps its digest
  under a reason word, on its task, once however often it comes, and gives it no answer. The count of
  strays is the number of different ones. So one act-id never gets two answers, a retry adds nothing,
  and a stray writes a bounded amount."
- **Trace**: the four strays, one for each word: the number 7, `:no-act-id`; a map whose act-id has
  two slots, `:act-id-malformed`; the class slot `:microbatch`, `:act-id-of-the-other-gate`; the layer
  slot `:bob` beside `:layer :alice`, `:act-id-of-another-layer`. Each: one digest, one element, about
  70 bytes, no answer. The same stray a thousand times: one element. The count is the size of the
  set, which Rama keeps when size tracking is on (skill pstate-schema.md).
- **Flaws found**: one, against the sentence that begins "So". A thing that passes the four tests
  could still sit on another partition, and then one act-id could get two answers (failure 1). The
  thing is no stray by the rule's own list, so the repair decides it, on its layer's task.
- **Verdict**: FAIL as found; repaired by change 1.

#### Rule 11
- **Source**: "The answer under an act-id keeps the digest of its act, and the read by act-id gives the
  digest with the answer. The sender compares it with the digest of what it sent. The gate writes
  nothing for a second, different act. The digest is one function that the door and the gates share."
- **Trace**: the answer row's `:digest`, 32 bytes; `answer-by-act-id` gives it as hex text;
  `softland.store.digest` loads under the alias `:store`, with Clojure alone, so the door can call it.
- **Verdict**: PASS.

#### Rule 12
- **Source**: "An act names a place — an entity and a key — once. The declaration refuses an act with
  two facts on one place, and a fact that names a fact of its own act in `:replaces`."
- **Trace**: two facts on `:note-1`, `:text` in one act: `:place-named-twice` (rule 17 of the
  declaration). A fact whose `:replaces` holds the act's own act-id: `:replaces-own-fact` (rule 18).
- **Verdict**: PASS.

#### Rule 13
- **Source**: "In this build a store key takes one write for a place. A permission that already stands
  is not given again; a fact under `:permission` on a place that holds one is refused; each retract
  under a store key is refused; a `:class` fact after the making act is refused; a request for locks
  holds a count. Each is refused with the word that says it is not built. Which entity a permission
  fact sits on is the plan's."
- **Trace**: Alice gives agent-1's permission a second time: the place holds a row;
  `:store-key-takes-one-write`. A `:permission` fact with no value: `:ending-a-permission-not-decided`.
  A `:class` or `:lock-request` fact with no value: `:store-key-retract-not-built`. A `:class` fact in
  a made layer with the layer's own class: `:store-key-takes-one-write`; with the other class:
  `:changing-a-class-not-decided`. A request whose value is `"two"`: `:lock-request-not-a-count`. The
  last sentence is answered: a permission fact sits on its holder, which is the build session's
  ruling; the plan says to Sid what follows, that a holder has one permission in a layer.
- **Flaws found**: none found for the rule, with reasoning: each of its five cases has a word and a
  place in the order. The making act's own class fact is not "after the making act"; the plan now
  says that the store-key step does not hold for it (failure 2).
- **Verdict**: PASS for the rule; the making act's case is under C item 3(g).

#### Rule 14
- **Source**: "`granted-in` is the act's layer, for a first permission and for one given beneath; the
  gate refuses another value, so that the slot stays true. The gate checks nothing more about who
  holds the top of a layer … Until sign-in is built, any sender can make any layer that is not made,
  for any holder. The module's README says so."
- **Trace**: a first permission `[:bob :crew :base]`: not `[holder layer layer]`, so the act is no
  making act; `:layer-not-made`. A give with `granted-in :base`:
  `:permission-fact-granted-in-another-layer`. The plan has the README say the last sentence.
- **Verdict**: PASS.

#### Rule 15
- **Source**: "A lock that a refused act named is free: a refusal writes the answer and nothing else.
  'A lock seals one value' counts accepted values. The door takes a fresh random part for each seal."
- **Trace**: A2 above named `[… u-r1] 1` and was refused; the lock's row is untouched; a later act
  seals a value under it and is accepted. The sealing function takes 12 fresh random bytes at each
  call.
- **Flaws found**: none found, with reasoning, and with one thing said plainly. A refusal in the plan
  writes two things: the answer row and the task's last stamp. The second is forced by C item 6. The
  answer of a refusal holds a stamp (`C item 5`), and stamps are "strictly increasing on its task"; if
  the stamp of a refusal were not kept as the task's last, the next decision in the same millisecond
  would get the same stamp. So "nothing else" can only be about what the act named and what it would
  have written, which is what the rule's first clause is about.
- **Verdict**: PASS.

#### Rule 16
- **Source**: "A gate opens each sealed value once, with its lock, to check that the lock opens it, and
  keeps nothing of the plain value. A fact in the record can then always be opened, and the grammar
  check has its place. The seal binds a value to its place: sealed bytes copied to another entity, key
  or layer do not open. A lock's row says which way of sealing the lock is for, so that the way can
  change later without a mark in each value. Any caller of the module can read a lock's bytes in this
  build; the door proper is the guard, later. The seal is for a forget of the depot's and the disk's
  copy; it does not hide a value from a client of the cluster."
- **Trace**: A1's seal step above. The place `[:alice :note-1 :text]` is the cipher's associated
  data. The row's `:way` is 1. `locks-of-request` gives the bytes and the way to any caller. A retry
  after a commit does not open the value again: it ends at check (a).
- **Flaws found**: none found, with reasoning: the gate holds the plain bytes only between the opening
  and the next line, in one event, on the task where the lock's row is.
- **Verdict**: PASS, with the ruling applied: the way is a small integer.

#### Rule 17
- **Source**: "A gate refuses an act that carries a stamp more than 10 seconds ahead of the gate's wall
  clock. So a forged stamp cannot push a task's stamps to the end of their range."
- **Trace**: clock 1,031; an act whose `:based-on` holds a stamp with the time 12,000:
  `:carried-stamp-too-far-ahead`; its answer holds `1031·n`. A stamp with the time 11,000: within 10
  seconds; the act goes on, and if it is decided its stamp is one more than the carried one. So the
  stamps of a task stand at most 10 seconds ahead of its clock.
- **Flaws found**: none found for the rule. One thing follows from it for a rebuild, and the plan did
  not draw it: failure 8.
- **Verdict**: PASS.

#### Rule 18
- **Source**: "A microbatch act has one stamp, later than the last stamp of each task on which it
  writes."
- **Trace**: the next part's. The next stamp is a function of the clock, one last stamp and the
  carried stamps; the greatest of several last stamps is the same call.
- **Verdict**: PASS for this part's share.

#### Rule 19
- **Source**: "A rebuild of the record from the depots is not built and not tested here. For the day
  that one is asked for, the plan says what it found: locks are kept through a rebuild and never made
  again; stamps would be new, because a stamp holds the gate's clock time at the decision (H2 A); and
  rule 9's answer. What F2 A reaches — the decisions and the current facts — goes to Sid in the
  receipt."
- **Trace**: the plan says the three things. What it missed bears on "the decisions": an act that
  rule 17 refused at clock 1,031 is not ahead of a clock years later, so a rebuild decides it by the
  checks after rule 17, and it can come out accepted.
- **Flaws found**: that one (failure 8).
- **Verdict**: FAIL as found, on the finding.
- **If FAIL**: say it. Done: change 8, with two more findings that this check came to: the order of a
  thing that a topology appended, and what Rama's docs give for a change of the number of tasks.

#### Rule 20
- **Source**: "The door's declaration gives limits, and a gate refuses a thing over one: 256 facts in
  one act, 1,024 entries of `:based-on`, 65,536 bytes in one sealed value, 256 locks in one request."
- **Trace**: 257 facts: `:too-many-facts`. 1,025 entries: `:too-many-based-on`. Sealed bytes of
  65,537: `:sealed-value-too-large`. A request for 257: `:too-many-locks-requested`. Each thing is
  digested and read by the engine before its rule refuses it, since the refusal needs the digest.
- **Flaws found**: the four limits are held. Beside them the plan said that every write of an event
  is bounded by them; the bytes of a key are not (failure 8).
- **Verdict**: PASS for the rule; the sentence beside it is repaired by change 8.

#### Rule 21
- **Source**: "The three limits of F9 … are targets, and the rig's measures are the starting point: at
  least 1,000 acts each second on one task, at the rate of an agent (measured: all accepted, the
  slowest 1 in 100 at 254 ms); at least 100 acts each second for one person, with the slowest 1 in
  100 at 20 ms (measured: 46 ms); the locks take no more than 2 times the bytes of the values
  (measured: 169 bytes of lock for each value). The plan says what it aims at for each."
- **Trace**: the plan gives an aim for each: 1,000 acts a second on one task; 20 ms from the append to
  the answer in the door's hand; 110 bytes of lock for each value, after the ruling. It gives the
  arithmetic for the first, 476 to 769 acts a second with every read cold, which I recounted, and
  says that the aim is then not met.
- **Flaws found**: none found, with reasoning: what the rule asks of the plan is that it say its aim
  for each, and it does, with the working.
- **Verdict**: PASS.

### The template's two anti-patterns

- *Skipping the fault-tolerance check.* Not done: restart and retry are traced with rows in "An event
  that Rama runs twice", and each block names what is its own.
- *Skipping the race check.* Not done: the two clients on one fact, the repeat inside one batch, the
  two making acts and the two acts under one lock are each traced.

PHASE_VALIDATION:minor-fail
