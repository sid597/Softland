# The run-again fixture — kept acts, and the tables that they gave

[Parent: store proofs](../README.md).

This folder holds one fixture: the records of both depots of acts of a store, and
the tables that the store's gates made of them. A later build must make the same
tables from the same records, also on a store with another number of tasks.

The folder holds data and no code. One proof reads it,
[`softland.store.run-again-proof`](../run_again_proof.clj), as resources of the
classpath. Its namespace docstring holds the form of each file, how a run of
bytes is told from a string, and what each check does; each file's head says the
same in short.

| File | What it holds | Who writes it |
|---|---|---|
| [acts.edn](acts.edn) | The records of the two depots of acts as they stood on a store of four tasks: for each depot, for each partition in order, its records in the partition's order, each as it was appended, with its act-id and the time on it. | `make-fixture`, below. Nothing else. |
| [tables.edn](tables.edn) | The tables that those records gave, in a form that names no task: for each gate and each of its seven tables, the rows as a sorted vector of `[key row]`. | The same run of `make-fixture`: the two files are one making, and neither is changed alone. |

## What the proof does with it, and what a pass shows

Two checks of the proof read the fixture. Each launches the store's module on a
fresh in-process cluster, with 4 tasks and then with 8, appends the records of
`acts.edn` to the two depots by hand, each as it is and with its own time, each
partition's records in their order, and holds the store's tables against
`tables.edn`, row by row. A pass shows that this build's gates give, for the
kept acts, the rows that the store gave which the fixture was made of: on the
number of tasks of the making, and on another. It does not show that those rows
are right. They are what the store of the making gave.

When a file is absent, both checks fail with "the fixture is not made: run
make-fixture".

## How it was made, and how it is made

This fixture was made on 28 November 2026. It is the eleventh making. It
replaced the tenth for the form of an act, changed on purpose on Sid's
decisions of 28 November 2026 (`src-store/README.md`, "Decisions of 28 November
2026 — the form of an act"). The tenth was made on 28 November 2026 (27
November 2026 UTC), and replaced the ninth for the permission rules changed on purpose,
on Sid's decisions of 26 and 27 November 2026
(`src/proposal/permissions-2026-11-27/PERMISSIONS.md`, section 6;
`src/proposal/permissions-2026-11-27/BUILD.md`). The ninth was made on 25
November 2026, and replaced
the eighth for the name of the store's unit, which became `lane` on purpose, at
Sid's word of 25 November 2026: an act's digest and its weight hold the key's
name, so every answer changed. Fed the eighth making's records, the renamed store
gave the eighth's tables but for the digests, the weights, and what follows from a
yes that names an old digest. The first
three were made on 14 October 2026, the fourth and the fifth on 21 November,
and the sixth, the seventh and the eighth on 22 November. The
first two were made by the build "run again", in which both depots came to
place an act by its lane and the base's gate came to decide each act on the
lane's task in the depot's order: the first after the build's part B, the
second at the build's end, for two rules changed after the build's review
(`src/proposal/backend-on-rama-2026-10-02/run-again/RECEIPT.md`). The third
replaced the second for the rules that Sid's decisions of 14 October 2026
changed on purpose: order and time are kept apart, and order never uses the
time.

- the stamp of a decision, an accept or a refusal, is its lane's count of
  decisions: the lane's last stamp plus one, or 0. It holds no time, and an
  act's stamp is not raised above the stamps that the act carries;
- the time on a thing is a label beside the stamp: the answer holds it when
  the thing held a usable one, a refused thing too, and each row of a fact and
  of the facts by value holds the time of the act that wrote it;
- no rule reads the time or a stamp that an act carries: the rule that refused
  an act carrying a stamp more than 10 seconds ahead of its time is gone, with
  its word `:carried-stamp-too-far-ahead`;
- the time on an act and a carried stamp have no upper bound: each is any
  whole number not below zero.

The hand changed with the rules. Its things whose time is chosen are now an
act with no time, an act with a time an hour behind, an act that carries the
greatest long as a stamp, an act with a time in the year 3084, and two honest
acts behind it, the second based on the fact of the first.

No run held the store with these rules against the second making. What
follows is derived from the rules and the second making's `tables.edn`, not
run. Fed the second making's records, the store would give another value in
463 of its 475 rows: every answer (its stamp, or a `:time` that it did not
hold), every row of a fact and of the facts by value (`:time`, and the stamps
in it), and the last stamp of each lane but two. A key of the facts by value
holds the stamp of its key's marking, so none of those keys would be found.
Twelve rows would keep their value: the ten rows of strays, whose digests do
not hold a stamp, and the last stamps of the two lanes whose only decisions
are two refusals (`:base` in the streaming gate, `:alice` in the microbatch
gate), which a refusal already counted 0 and 1. The second making's act that
carried a stamp an hour ahead of its time was refused; it would be accepted,
and write a fact's row in each gate and a row of the facts by value in the
streaming gate.

The third making ran on 14 October 2026 at 14:48 IST. It wrote `acts.edn`,
104,788 bytes, 337 records (the streaming depot's four partitions 132, 0, 17
and 5; the microbatch depot's 7, 0, 176 and 0), and `tables.edn`, 90,418
bytes, 488 rows: in the streaming gate 142 answers, 61 facts, 25 facts by
value and 14 rows of the task's table; in the microbatch gate 169, 64, 6 and
7. The run of all seven proofs after it passed, the two checks of this
fixture among them. The receipt of the change says that the fixture was made
again, and why (`src/proposal/backend-on-rama-2026-10-02/order-and-time/RECEIPT.md`).

The fourth making ran on 21 November 2026, and replaced the third for three
rules of a permission that were changed on purpose, on Sid's decisions of 20
and 21 November 2026 (`src/proposal/runtime-2026-11-21/PROPOSAL.md`, entry 6):

- a permission ends: a permission fact with no value, by a holder above the
  permission or by its own, is taken, where it was refused
  `:ending-a-permission-not-decided`. What was given beneath it ends with it:
  an act is refused when a permission above its own no longer stands;
- a holder is given a permission again after an end, where a permission's
  place took one write;
- only the lane's first holder writes its setting, its list and its marks,
  where any holder who may act did.

The hand changed with the rules: in each gate it now sends a line of thirteen
things in which a permission ends and is given again, and the making holds the
word of each. The third making's records held no end, no second give and no
setting by a holder of a given permission, so the store with the new rules
gave the third making's tables from the third making's records: the proof
passed on the third making before this one replaced it. It was made again so
that the kept acts hold an end.

It wrote `acts.edn`, 113,640 bytes, 363 records (the streaming depot's four
partitions 145, 0, 17 and 5; the microbatch depot's 7, 0, 189 and 0), and
`tables.edn`, 98,347 bytes, 527 rows: in the streaming gate 155 answers, 66
facts, 28 facts by value and 14 rows of the task's table; in the microbatch
gate 182, 69, 6 and 7. The run of all seven proofs after it passed. The
receipt of the change says that the fixture was made again, and why
(`src/proposal/runtime-2026-11-21/BUILD-store-piece-1.md`).

The fifth making ran on 21 November 2026 at 23:13 IST, and replaced the
fourth for the rules of what a permission covers, which were added on purpose,
on Sid's decisions of 21 November 2026 that an act is the smallest thing a
permission covers and that a holder can hold more than one permission in a
lane (`src/proposal/runtime-2026-11-21/PROPOSAL.md`, entry 6;
`src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md`):

- a permission says what it covers in a sixth slot of its id, one run or one
  act, and sits on what it covers under a key of its own,
  `:softland.store/run-permission` or `:softland.store/act-permission`, where
  the store refused both keys as unknown store keys;
- an act under a permission for a run is taken only in that run, and one
  under a permission for an act only when it is that act, by the uuid of its
  act-id and its digest;
- a permission for a run or for an act gives nothing, and a run ends no
  permission of its holder but its own.

The hand changed with the rules: in each gate it now sends a line of ten
things under permissions for one run and for one act, and the making holds the
word of each. The fourth making's records held no permission for a run or for
an act, so the store with the new rules gave the fourth making's tables from
the fourth making's records: the proof passed on the fourth making before this
one replaced it. It was made again so that the kept acts hold both.

It wrote `acts.edn`, 121,700 bytes, 383 records (the streaming depot's four
partitions 155, 0, 17 and 5; the microbatch depot's 7, 0, 199 and 0), and
`tables.edn`, 106,099 bytes, 559 rows: in the streaming gate 165 answers, 71
facts, 30 facts by value and 14 rows of the task's table; in the microbatch
gate 192, 74, 6 and 7. The run of all seven proofs after it passed. The
receipt of the change says that the fixture was made again, and why
(`src/proposal/runtime-2026-11-21/BUILD-store-piece-2.md`).

The sixth making ran on 22 November 2026 at 14:22 IST, and replaced the fifth
for the rules of a permission for one act, which were changed on purpose, for
the form of a yes for one act in the prompt that Sid pasted on 22 November
2026 as his go for the build, a session's form
(`src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md`):

- a yes for one act sits on an id of its own, a uuid that its giver makes,
  and not on the uuid of its act's act-id; the act goes under any act-id of
  its maker's;
- the digest that a yes names is of the act as its maker made it, in its one
  form, without its act-id, its depot, its time and its permission, where it
  was of the thing as it arrived, with its act-id and its depot;
- the gate uses a yes up when it takes its act, writing its use at the yes's
  place, and an act under a used yes is refused; a yes's place takes one
  give.

The hand changed with the rules: in each gate it now sends a line of twelve
things under a permission for one run and under yeses for one act, the act
taken under a yes and sent again, and the making holds the word of each. The
fifth making's records hold the act of a permission for one act in the first
form, whose digest is of the thing with its act-id: under the new rules that
act is refused `:act-not-the-one-permitted`, in each gate. So the proof failed
on the fifth making before this one replaced it, at exactly those rows: of 559,
the answers of the two acts, now refusals, and their facts, and the one fact
by value, now unwritten, on each task of the base. It was made again for the
rules, and not to make the proof pass.

It wrote `acts.edn`, 123,504 bytes, 387 records (the streaming depot's four
partitions 157, 0, 17 and 5; the microbatch depot's 7, 0, 201 and 0), and
`tables.edn`, 106,629 bytes, 563 rows: in the streaming gate 167 answers, 71
facts, 30 facts by value and 14 rows of the task's table; in the microbatch
gate 194, 74, 6 and 7. The receipt of the change says that the fixture was
made again, and why
(`src/proposal/runtime-2026-11-21/BUILD-store-piece-2-repair.md`).

The seventh making ran on 22 November 2026 at 15:07 IST, and replaced the
sixth for two changes made on purpose, in piece 3 of the store's line
(`src/proposal/runtime-2026-11-21/store-piece-3/SPEC.md`):

- each gate keeps each lane's line: for each decision a line row under its
  stamp, the thing again under the same key when it weighs no more than its
  bound, and a row by the who that the thing names, in what the gate keeps
  for a task. Three tables more, of which the sixth making holds none;
- who may read a lane's acts whole is the eighth store key,
  `:softland.store/who-may-read-acts`, a setting that the lane's first
  holder writes, where the store refused the key as an unknown store key.

The hand changed with the second: in each gate it now sends a line of seven
things that write the setting and are refused for it, and the making holds the
word of each, and that each answer of each gate has its line row with the
same uuid, word, time and digest. On the sixth making the proof failed only
at the rows of the three new tables, which the sixth making does not hold:
every row of the four tables before them was the same, and the first module
and the run again agreed on every row, the line's too. It was made again for
the line and the key, and not to make the proof pass.

It wrote `acts.edn`, 127,741 bytes, 401 records (the streaming depot's four
partitions 164, 0, 17 and 5; the microbatch depot's 7, 0, 208 and 0), and
`tables.edn`, 343,535 bytes, 1,706 rows: in the streaming gate 174 answers, 73
facts, 30 facts by value, 174 line rows, 174 things and 188 rows of the task's
table; in the microbatch gate 201, 76, 6, 201, 201 and 208. The file is three
times the sixth's: it holds each thing again, as its line does. The receipt of
the change says that the fixture was made again, and why
(`src/proposal/runtime-2026-11-21/BUILD-store-piece-3.md`).

The eighth making ran on 22 November 2026 at 22:19 IST, and replaced the
seventh for one change made on purpose, in the second repair of piece 2
(`src/proposal/runtime-2026-11-21/BUILD-store-piece-2-second-repair.md`):

- the use of a yes for one act, the end at the yes's place that the accept of
  its act writes, names the act that used it and no fact of it: its row holds
  no `:by-index`, where it held the count of the act's facts, an index past
  its last fact, which named no fact that the act holds.

On the seventh making the proof failed only at those rows: of 1,706, the four
rows of a yes's use, two in the streaming gate and two in the base's facts on
each task, each differing in `:by-index` alone; the run again and the run
after failures agreed on every row. It was made again for the row's form, and
not to make the proof pass.

It wrote `acts.edn`, 127,741 bytes, 401 records (the streaming depot's four
partitions 164, 0, 17 and 5; the microbatch depot's 7, 0, 208 and 0), and
`tables.edn`, 343,483 bytes, 1,706 rows, the seventh's counts in every table:
in the streaming gate 174 answers, 73 facts, 30 facts by value, 174 line rows,
174 things and 188 rows of the task's table; in the microbatch gate 201, 76,
6, 201, 201 and 208. The run of the proof on it passed, 48 checks.

The tenth making ran on 28 November 2026 at 02:40 IST (27 November 2026 UTC),
and replaced the ninth for the rules of permissions and party ids changed on
purpose, on Sid's decisions of 26 and 27 November 2026
(`src/proposal/permissions-2026-11-27/PERMISSIONS.md`, section 6;
`src/proposal/permissions-2026-11-27/BUILD.md` and `CONTRACT.md`):

- a party's id in the store says what kind of party it is (`:person.<id>`,
  `:agent.<id>`, `:run.<id>`, or `:system`), while `:anyone` and
  `:anyone-signed-in` hold permissions and never act;
- a permission sits at a uuid of its own under the single store key
  `:softland.store/permission` (replacing `:softland.store/run-permission`,
  `:softland.store/act-permission` and `:softland.store/who-may-see`), its
  value is a map holding `:holder`, `:allows`, `:covers`, `:given-by` and
  optional `:session`, `:under` and `:lent-from`, and an act names the uuid of
  the permission it acts under (or omits `:permission` on a making act or an
  act that only ends its author's own permissions);
- a permission's id takes one permission for good, so re-giving after an end
  uses a new uuid, an owner is a permission with `:owner true` tracked in the
  task table's `:owners` set, and each gate keeps a seventh table,
  `permissions-by-holder` (copied across tasks in the microbatch gate).

Checks 1 (`StoreAgain`) and 4 (after planned failures) passed with 0 differing
rows across all seven tables of both gates before `make-fixture` ran. It wrote
`acts.edn`, 138,237 bytes, 401 records (the streaming depot's four partitions
164, 0, 17 and 5; the microbatch depot's 7, 0, 208 and 0), and `tables.edn`,
365,263 bytes, 1,739 rows: in the streaming gate 174 answers, 75 facts, 30
facts by value, 13 permissions by holder, 174 line rows, 174 things and 196
rows of the task's table; in the microbatch gate 201, 78, 6, 7, 201, 201 and
209. The run of the proof on it passed, 48 checks.

The eleventh making ran on 28 November 2026 at 16:52 IST, and replaced the
tenth for the form of an act, changed on purpose on Sid's decisions of 28
November 2026 (`src-store/README.md`, "Decisions of 28 November 2026 — the
form of an act", entries 1 to 3), by the change that brought the store to
that form:

- an act-id is `[lane uuid]` and names no depot, and a thing as it arrives
  is the maker's act with `:act-id` and `:door-time` added, with no
  `:depot`. A lane's depot is the lane's own fact. The stray word
  `:act-id-of-the-other-gate` is gone: an act-id with a depot word in it is
  `:act-id-malformed`;
- `:subjects` and `:claimed-when` are no parts of an act;
- a fact's key may be a UUID as well as a keyword. The hand sends no such
  key, so the kept acts hold none.

The hand changed with the rules: in each gate the two strays under an act-id
of the other gate are now two strays under an act-id with a depot word in it,
the malformed act-id of two slots is now one with a depot word in the place of
its uuid, and the act with each optional part carries neither part that left.
Not run, and derived from the form: the tenth making's records each hold an
act-id of three slots, so the store of this change keeps each as a stray, and
none gives the tenth's rows.

It was made twice that day. The first making (commit `4ad925e5`) sent the
base's stray of the old form under the act-id `[:base uuid]`, which is well
formed, so the base took it as an act sent twice, with an answer of its own
and the stamp of every later act of the base one higher. The proof was mended
to send it under `[:base :microbatch uuid]`, and the fixture made again with
the command below. Before that mend, a second making on the first's tree was
held against the first with the uuids, the times and the digests taken out:
the records were the same, record for record, and so were the rows but for
their places in tables sorted by uuid text. So two makings differ in what the
paragraph below says they differ in, and in nothing else.

It wrote `acts.edn`, 124,708 bytes, 399 records (the streaming depot's four
partitions 163, 0, 17 and 5; the microbatch depot's 5, 0, 209 and 0), and
`tables.edn`, 352,648 bytes, 1,737 rows: in the streaming gate 174 answers,
75 facts, 30 facts by value, 13 permissions by holder, 174 line rows, 174
things and 195 rows of the task's table; in the microbatch gate 201, 78, 6, 7,
201, 201 and 208. Against the tenth: two records fewer, in each gate the one
case of the stray word that went, and in each gate one row fewer of the task's
table, that word's under the strays.

From the repository root, behind the lock of this machine's cluster runs:

```sh
clojure -M:store-module -m softland.store.run-again-proof make-fixture
```

It launches the store with four tasks, plays the proof's hand with the fixture's
numbers, waits for rest, reads each depot partition's records and every table,
and writes the two files here. It writes nothing when the store does not hold
what the fixture is for, or when a file's text does not read back as its value.
A folder named behind `make-fixture` gets the files in place of this one; that
is how a second making is held against the first. A making gives other uuids and
other times, and so other digests and other labels of time, than the one before:
the two files of two makings are not the same text.

A run of the proof never writes here.

## When it may be made again

The rule is a session's text, which Sid pasted into his prompt of 13 October
2026 as his decision 6. The record shows that he did not type it:

> A change in what a gate accepts is a new version of the act. The old version's
> rules stay in the code. This starts with the first real record. Until then the
> fixture is made again only when a rule is changed on purpose, and the receipt
> says so. In this build it means the rule on the page and the fixture; the code
> still knows one version.

So the fixture is not made again to make a failing proof pass.

## When the proof fails on the fixture

The store changed what it gives for kept acts. The proof's lines that begin
`DIFFERENT` name the first rows, each with its gate, its table and its key. It
is one of two things:

- a fault: the build changed an answer, a row or a stamp and did not mean to.
  The build is repaired, and the fixture stays as it is;
- a rule that was changed on purpose. Then the receipt of the build that
  changed it says which rule, and that the fixture was made again for it.
