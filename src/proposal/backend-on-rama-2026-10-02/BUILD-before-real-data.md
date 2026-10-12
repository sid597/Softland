# Build: before real data is kept — the locks go, the gates' answers in a log, and the tables made again

Cut by: Sid's session of 12 October 2026, the one that went through the sheet "Before real data is kept"
with him in chat (Opus 5.5, effort max). It is the second cut of that day. The first kept the locks as a
choice of a layer; that evening Sid decided that the store keeps no locks, and that the door checks
nothing before it appends.
For: one build, run by the Rama skill's stages (see "How the build runs").
Status: a starter. Sid's decisions of 12 October 2026 are the contract, with those of 6 to 12 October
before them. This page names what is **not decided** and is kept only so that the build can run, and what
the builder **chooses** and lists as provisional. The build decides none of Sid's open questions. The
stage that writes the implied spec checks each "as built" line of this page against the code: the cutter
had the code read by a gatherer, and checked a few of its lines against the source (see "What this page
rests on").
Before the first stage: run the six proofs (`test-store/README.md`) and write their count lines on the
build's page as the "before"; read `git log --oneline -5` and `git status --short`. Files of other
sessions are in the working tree and not committed: `src/app/server/README.md`, two paths under `docs/`,
and scratch of the read build under `reads/` (its receipt, section 14). Leave them, and stage your own
paths by name.
The decisions, in Sid's words, are in `src-store/README.md`. Everything else a builder chooses is
engineering, written with its reason in a docstring, never as a decision.

## What this build is

Three changes to what the store keeps, made while each is only a change of code. No real data is kept
yet. After it is, each of these is a change to data that someone does not want to lose.

```
MAKER ── an act, its values plain ──► DOOR ──► THE LOGS OF ACTS ──► GATE ──► TABLES
                                       │                              │
                                       │                              └──► THE LOG OF ANSWERS    new · part B
                                       │
                                       └─ appends each act as the maker gave it                    part A
                                          today: the door seals every value with a lock, and a gate
                                          makes each lock and refuses a value that is not sealed

A REBUILD   reads the logs, and makes the tables again.                                           part B
            It decides nothing. A proof compares what it made with what the gates wrote.
```

- **A. The locks go.** The store keeps no lock. The door seals nothing, and a gate makes and opens none.
- **B. The gates' answers in a log.** What a gate decided for an act also goes into a log. A rebuild makes
  the tables again from the logs, and decides nothing again.
- **C. The limit of four on the depth of a permission is lifted.**

When this build passes: a maker's value is in the log as she gave it; no code of the store makes, asks
for, keeps or uses a lock; each answer that a gate gave is in a log; a rebuild makes the tables again from
the logs alone, and a proof shows them equal to the tables that the gates wrote, with each exception
named and its reason given; a permission can be deeper than four. The proofs of today stay and pass, each
changed where this build makes one of its lines untrue. The checks of the locks and of the seal go with
what they checked.

What this build is not: a sealed end of any kind; a forget; a record of who read what; a depot of its own
for anyone; a cut out of a log; a trim; a bound on what one sender may send; a backup, a second machine or
a real cluster; sign-in; a screen, a view or a tool.

## Read first, in this order

1. `src-store/CLAUDE.md` — the explanation rules. They bind every file of this build.
2. `src-store/README.md` — the tree, and the decisions sections. The last one, of 12 October, is this
   build's own. Read it with the sheet that it answers, `decisions/groups/g-check.txt`: its first page is
   the picture of what is kept of one act. Where the sheet and the section differ, the section is later
   and is Sid's. The sheet's box A of Z1, its sealed ends and its "time" in Z2, and its suggestion for Z4
   are not what he decided. Entry 7 answers a question that the sheet did not ask.
3. `reads/RECEIPT.md` — what is true of the store today, what the read build chose as provisional, and
   what it left not decided. Its section 10, rows 10 and 11, are met by this build's part B.
4. `reads/batch-probe/RESULTS.md` — what an append to a depot from inside an attempt of a batch does, as
   run on 12 October on Rama 1.6.0. Part B leans on it. It is a probe: no form, name or table comes from it.
5. `src-store/softland/store/README.md` — the store's page: "How the parts relate", "What the gate does
   not check, said plainly" (the order after a failure is there), "Decisions this module stands on",
   "Engineering choices" (rules 5, 9, 15 to 19), "Words a session made, Sid's to change".
6. The code that this build changes or reads, each by its namespace docstring first:
   `door/additions.clj` (which fact is a value fact, the seal, the request for locks), `door.clj` (the
   stock of locks, the append, the read of an answer), `door/stock.clj`, `door/seal.clj`,
   `door/declaration.clj` (the rules that a gate runs, the limits); `gate/decision.clj` (the opening of a
   sealed value, the rows of a decision); `gate/batch.clj`; `module.clj` (the depots);
   `module/streaming_gate.clj`, `module/microbatch_gate.clj`, `module/batch_clock.clj`,
   `module/door_reads.clj`; `record/rows.clj` and `record/tables.clj`; `act.clj` (the depth of a
   permission).
7. `BUILD-store-module.md`, `BUILD-door.md` and `BUILD-reads.md` — the handoffs of the same family. This
   build changes lines of the first two (see R9).
8. `decisions/OPEN-QUESTIONS.md` — the queue. Answers never live there.
9. The Rama skill (`/rama`), before any Rama code is read or written. `reference/rama/docs` is an extract
   of March 2026 whose pages name Rama 1.5.0; the store runs 1.6.0. A line read there is "read, not run".
   For part B: `11-stream-topologies.md`, "Fault-tolerance and retry modes"; `12-microbatch-topologies.md`,
   line 130; `23-acid-semantics.md`; `14-depots.md`, "Appending to depots from topologies".
10. Last, and only as findings: the rig's notes under `src/proposal/rig-2026-09-25/` on what Rama forced.
    No form, no name, no call shape and no default comes from the rig's code, the server's or Inland's.

## The decisions this build stands on

All are in `src-store/README.md`. The section of 12 October, by entry:

| Entry | What Sid decided | Sheet | In this build |
|---|---|---|---|
| 1 | Every table can be made again from the logs. A gate decides once, and what it decided also goes into a log. A lost machine is met by copies and backups. | Z1 | part B; the copies and backups are not |
| 2 | Every value is plain in the log, the store keeps no locks, and the shared logs are never cut. | Z2 | part A |
| 3 | The log is kept whole: "forever never trim". | Z3 | R2 |
| 4 | A model's reads are kept, always; a person's, until she turns it off. | Z4 | not built here |
| 5 | Three machines, three copies and Rama's own backups, when real data is kept. | Z6 | not built here |
| 6 | The eight of Z5: yes to each. | Z5 | the fourth is part C |
| 7 | An act from a maker who may not act is written to the log, and then refused. The door checks nothing first. | — | A4 |

Earlier decisions that this build leans on: the act holds plain values (6 October, 3; its other half, that
the door seals, is set aside by entry 2); a permission's form (6 October, 8); two depots of acts, one for
each gate (7 October, 1); the record is in the gates' PStates, and its reason as corrected on 12 October
(7 October, 2); the door reads an act's answer from the record by act-id, and an ack is a signal
(7 October, 6 and 7); a gate is the authority, because anything can append to a depot without the door
(7 October, 8); a stamp holds the clock time and a counter (8 October, 4); the tables keep a value plain
(11 and 12 October, 5); a read is done in the runtime with Rama's push, and a pushed value is not a cache
(11 and 12 October, 3).

Four lines of Sid's hold the whole build:

- 11 October: "can't we just replay the log?? ... even if the main gets blown up we should be able to
  replay log and get to latest".
- 12 October: "we are removing all the locks right??", and, when the two builds were laid out to him,
  "yeah lets remove it".
- 12 October, of an act that is refused: "why don't we store both? Like the data is useful", and "we are
  not choosing the destructive choice here."
- 12 October: "by default if its not set then we take the side of spectrum that captures more because we
  are not choosing the destructive side by defualt that can happen even after but the other side cannot."

## What the code does today

From a gatherer's report of 12 October, by file and line. The lines marked ✓ were checked against the
source by the cutter; the others were not. The implied spec checks each. Part A takes out what the first
four say.

- **The door seals each value fact.** A value fact is "a fact that carries a value and whose key is not
  one of the store keys" (`door/additions.clj`, namespace docstring ✓). `entry-of` decides it for each
  fact; `facts-to-append` seals, and `:v` becomes `:sealed` and `:lock-id`.
- **The door asks for locks ahead.** The request is an act with one fact under
  `:softland.store/lock-request`; the door reads the request's answer and then its locks with the query
  `locks-of-request`, and keeps a stock of unused locks in the memory of its process (`door.clj`,
  `door/stock.clj`).
- **A gate refuses a plain value.** The declaration's rule `:plain-value-is-sealed` gives the word
  `:not-sealed` (`door/declaration.clj:319` ✓); both gates run the declaration's check. A gate opens each
  sealed value with its lock (`gate/decision.clj`, `opened`) and keeps the plain bytes in the fact's row,
  as a String.
- **A gate makes each lock**, 32 random bytes, and writes it to its locks table and to no depot
  (`door/seal.clj`, `new-lock`; `new-lock-row` in each gate).
- **What a gate writes for one decided act.** The answer's row: `:refused` (absent when the act was
  accepted), `:stamp`, `:digest`, under the key "layer, act uuid". The row of each fact: the value, by
  which act and index, the stamp, who wrote it, the permission. `gate/decision.clj`, `writes`, makes the
  rows as data, and each gate writes them.
- **The module's depots are three** (`module.clj:115` to `117` ✓): `*streaming-depot`,
  `*microbatch-depot`, and `*batch-clock-depot`, which takes no client's append.
- **The module appends to a depot in one place** (`module/microbatch_gate.clj:441` ✓): the microbatch
  gate appends each batch's id and clock reading to `*batch-clock-depot` with `:ack`, and the stream
  topology `batch-clock` keeps it in the table `$$batch-clock`.
- **The tables**: for each gate `answers`, `facts`, `facts-by-value`, `locks` and `task`; and
  `$$batch-clock`.
- **The streaming gate's source** has `{:start-from :beginning :retry-mode :all-after}`
  (`module/streaming_gate.clj:195` ✓). The microbatch gate's has `{:start-from :beginning}`.
- **The depth of a permission**: `max-permission-depth` is 4 (`act.clj:117` ✓), "How many permission ids
  one permission id can hold, itself counted." The act's form holds it, and the door's declaration through
  the same engine; the words are `:malformed` for an act's own permission and
  `:permission-fact-not-a-permission` for the value of a permission fact. No gate tests a depth itself.
- **About twenty places take the depots of acts to be two**: in the door, the act's form, the
  declaration, both gates, the three reads of the door, and the read path.
- **How much of the tree names a lock or a seal**, by a count of whole words on 12 October: about 880 of
  the 11,400 lines of the source, in 19 of its 26 files; about 1,450 of the 13,300 lines of the proofs. The
  two files that hold nothing else, `door/seal.clj` and `door/stock.clj`, are 561 lines.

## What the read build's session found for this build

Sid asked the session that finished the read build to pass on what it had found that this build could
meet. His line to it, 12 October: "tell it if you found more that might be addressed there now because
that phase is all about fixing things that were wrong assumptions". It sent sixteen things that day, when
this page still kept the locks as a choice. None is a decision. Each is that session's, read or run by it.
The cutter checked 1, 6, 8 and two of the three numbers of 13 against the source; the implied spec checks
each. They keep that session's numbers. Four fell away when Sid decided that the store keeps no locks, and
are at the end.

For part A:

1. **Today a maker who may not act is stopped before her value reaches a log.** The stop is the door's
   request for locks. When her stock is empty, a gate refuses the request and "nothing is ever recorded
   under the act's own act-id" (`door.clj`, "One faulty act has two outcomes"). With no locks the stop is
   gone. Sid decided that it goes, and that the door checks nothing first (entry 7): see A4.
6. The door reads a layer's depot on every send, in one round trip, and keeps no copy (`depot-of`, three
   call sites in `door.clj`). In the base that query is held to closing.
7. `rows/fact-row` has the branch for a plain `:v` already, used today under store keys only: the row's
   String is the value's one text. Its digest is the one that the door seals today, so a fact's place in
   the order by value does not change.

For part B:

8. **The last stamp of a batch is in no act and in no answer.** `batch/last-stamp-of-batch` can be later
   than the stamp of every act of its batch. It is the microbatch gate's last stamp. It is also the
   `:level-since` that a key gets when it is marked in the base, and each row of the facts by value under
   that mark holds it. See B3: a log comes to hold it.
9. A rebuild applies acts in the order of their stamps, not of a depot. After a failure the streaming
   gate can decide a later record first, and the base's depot holds no order between two partitions.
10. A gate's last stamp, made again, must not be below any `:level-since`.
11. `decision/writes` takes the verdict, the stamp and the digest, and gives each row as data. A rebuild
    that calls it makes its rows by the functions that the gates call.
12. The tables hold rows that no read names: the rows by value of an earlier marking stay when a mark is
    taken off. The comparison of B4 will meet them.

For part C:

13. **Three numbers bound the depth of a permission, not one.** `act/max-permission-depth`, 4.
    `act/max-depth`, 32: a permission that is given is the value of a fact, and a value is inside 32
    collections at most. `text/max-depth`, 64: a part that is deeper is written as `#too-deep`. Past it,
    two permissions that differ only deep inside have one text and one digest, and a gate compares texts.
    That session read the lines named and not the whole walk.
14. A permission's whole chain is copied as text into the row of each fact and of each fact by value, and
    Rama sends whole rows to every push. Depth is paid in each row that is written under it.

For part D and the whole:

15. The numbers before this build are in `reads/runs/measures-final.txt`. Each open push that holds a
    written row costs the gate at each write there. So a measure is made with no live read open, or says
    that one is.
16. No table that anything is kept live on may hold a byte array: a push on such rows is sent the whole
    value anew about once a second (the read build's ruling 86). The answers' `:digest` is a byte array,
    and nothing watches it today. Where the read path takes the depots to be two, for R6: `read_path.clj`,
    in the four table fields of `ReadPath`, `open`, `pstate-of` and `task-routing-ids`;
    `read_path/sources.clj`, `gate-of`; `read_path/answer.clj`, `fact-id`. The read build's review scripts
    are in `reads/runs/review-scripts/`. When that build changed the class of a field, four checks had
    been passing for a wrong reason; part A changes what a fact arrives as, so the review looks for the
    same.

Fell away with the locks. Each was about a fact on a layer that would say which keys are sealed, and
there is none: 2, the read that a gate would pay for it at each act; 3, that fact on every task of the
base; 4, the read path and that key; 5, that a key of the store's own namespace could not be sealed.

## What to build

The split into parts is the skill's stage to make. This is one reading of the whole.

### A. The locks go

- **A1. The door appends each act as the maker gave it** (entry 2). It seals no value, asks for no lock
  and keeps no stock. The door then holds nothing in the memory of its process.
- **A2. A gate takes a plain value** and keeps it in the fact's row, as the read build left that row. It
  makes no lock and opens none. The rule that refuses a value that is not sealed goes, and the rules and
  the words of the seal with it.
- **A3. The module has no table of locks, no request for locks and no read of a request's locks.** The
  request leaves the store's own keys.
- **A4. The door checks nothing of an act before it appends it** (entry 7), as its first rule says
  already. An act from a maker who may not act is in the log, a gate refuses it, and its answer says so.
- **A5. Before anything is taken out, the plan lists every job that the locks and the seal did, meant or
  not, and says where each goes**: it is needed no more, another part does it, or Sid hears of it in the
  receipt. Three are known. The stop of a maker who may not act goes (entry 7, finding 1). The tie of a
  sealed value to its place goes with the seal: by the store's rule 16, sealed bytes that are copied to
  another entity, key or layer do not open. The bound on the bytes of a value moves (A6).
- **A6. The bound on the bytes of one value stays, counted on the plain value.** It is 65,536 today, on
  the sealed bytes. It is one of the builder's numbers, kept and decided by nobody.
- **A7. No log is written again.** Nothing real is in one, and the proofs make their own.
- **A8. The proofs.** Each check of a lock or of the seal goes with what it checked, and each check that
  held a sealed value is written again for a plain one. The review looks for a check that passes for a
  wrong reason (finding 16).

### B. The gates' answers in a log, and the tables made again

- **B1. A gate appends what it decided for an act to a log** (entry 1): the act it is for, refused or
  not with the word, the stamp, and the digest of the act. That is what the answer's row holds. No value
  of a fact is in it.
- **B2. The tables stay the record that is read.** The door reads an act's answer from the tables by
  act-id, a gate reads its own record before it decides, and the read path reads the tables (7 October,
  2, 6 and 7). Nothing reads the log of answers but a rebuild.
- **B3. A rebuild makes the tables again and decides nothing.** It reads the logs of acts, the log of
  answers and the log of the clocks of batches, and no table. From them it makes each table that the
  gates write. The plan lists every table and every part of a row, and says for each what it is made
  again from. What cannot be made again is named with its reason, and goes to Sid in the receipt. One is
  known: what a gate keeps of a stray rests, by the store's page, on an order that no depot holds (rule
  19); the plan confirms that, or finds a log for it. The last stamp of a batch is in no act and in no
  answer (finding 8): a log comes to hold it, since Sid's decision is every table. A rebuild applies acts
  in the order of their stamps (finding 9).
- **B4. A proof compares.** It sends acts through the door and through no door, on both depots: accepted
  and refused acts, an act from a maker who may not act, an act sent twice, a second and different act
  under a used act-id, a stray, a retract, and a fact under a marked key. Then it runs the rebuild and
  compares what it made with what the gates wrote, row by row.
- **B5. An entry of an attempt that did not count does no harm.** Rama's page says that an append to a
  depot from a microbatch topology is not exactly-once across a failed attempt, and the runs of 2 and 12
  October saw such an append stay. The plan says, with a run, what the log of answers holds after a failed
  attempt of each gate, and how the rebuild comes to the answer that the tables hold.
- **B6. The logs are kept whole** (entry 3): no trimming option is set on the log of answers, as none is
  set on the logs of acts or on the log of the clocks of batches.

### C. The limit of four on the depth of a permission is lifted

- **C1.** `max-permission-depth` goes, in the act's form and in the declaration (entry 6, the fourth of
  Z5). Sid, 8 October: "group->group->agents->agents-groups-Agent group->...".
- **C2. A bound on the size of an act takes its place.** What it counts and its number are the builder's
  to propose. They are kept with the builder's other numbers, and decided by nobody. A permission's whole
  chain is written into each row under it (finding 14), which the bound can count.
- **C3. With the 4 gone, two more numbers bound a permission's depth** (finding 13). A permission that is
  given is a value, and a value is inside 32 collections at most; that number is one of the builder's,
  kept and not decided. The text of a part stops at 64 collections, and past it two different permissions
  have one text. So every bound on the depth of a permission stays below the text's, and the receipt says
  how deep a permission can be when the build is done.
- Not here: a gate still reads none of the permissions above an act's own. That read comes when a
  permission can end, which is not built.

### D. The measures

Sid, 7 October: "idk lets go by whats measured and we will know better when we build more and optimise".
The build measures, and sets no target. The numbers before it are in `reads/runs/measures-final.txt`, and
each measure is made with no live read open, or says that one is (finding 15):

- an act through the door and a gate, beside the numbers before the build: the door asks for no lock,
  and a gate reads and writes no row of a lock;
- what the append of an answer adds to an act of the streaming gate, and to a batch of the microbatch
  gate;
- Rama's setting `topology.stream.max.executing.per.task` at 1, which the store's page names as what
  closes the window in the order of acts after a failure: an act's time at 100 acts a second, and the most
  acts a second that one maker holds. The result goes to Sid. The choice is his, and the build leaves the
  setting as it is.

## Runs before the plan is fixed

The plan stage makes these, on Rama 1.6.0, before it chooses a form for the log of answers. Each is a
probe: nothing of its tables or names is a proposal for the store.

1. An append to a depot from inside an event of a stream topology whose source has the retry mode "all
   after": what the depot holds when the event fails after the append, and when it is retried.
2. Whether such an append and the event's writes to its tables become visible together.
3. What the append costs an event.

The same for the microbatch gate was run on 12 October (`reads/batch-probe/RESULTS.md`).

## The builder's rules

Each has its source. The plan decides how each is met.

- **R1.** A rebuild decides nothing again (entry 1). It applies the answer that the log holds. A gate
  whose rules were changed after an act was decided gives that act no new answer.
- **R2.** Nothing is cut out of a log, and no trimming option is set on any (entries 2 and 3). No
  migration of a depot is written.
- **R3.** The tables stay the record that is read (B2). The log of answers is no second road to an
  answer for the door, for a gate or for a tool.
- **R4.** The door checks nothing of an act (entry 7). No part of this build puts a look at a permission
  before the log.
- **R5.** Nothing is shown before it is in the tables, and the build keeps no store of its own beside
  Rama's push (`CLAUDE.md`, "Systemic repair and caching"; Sid, 11 October, of a pushed value: "i also
  don't consider it a cache"). If the build finds itself reaching for a cache, it stops and says so on
  its page.
- **R6.** No new code takes the depots of acts to be two. About twenty places do today, and this build
  adds none. A depot of its own for an organisation is Sid's thought of 12 October and is not built.
- **R7.** A second attempt of an event or of a batch leaves the store as one attempt would. The Rama
  skill: "Duplicate side effects from retried processing are bugs."
- **R8.** Every word that the build makes is plain, says what the thing is, and is listed for Sid under
  "Words a session made, Sid's to change". No word comes from the rig's code.
- **R9.** The earlier handoffs hold where they touch. This build changes these lines. In
  `BUILD-store-module.md`: item 7 of "What to build", the locks and the seal, goes whole; rules 5, 15 and
  16 go with it; rule 9, since a rebuild picks no winner and reads the answer; rule 19, since a rebuild
  is built; and of rule 20, the limit on the locks of one request. In `BUILD-door.md`: part 3 of "What to
  build", the locks, goes, and part 4 loses its seal; rules D3 and D6 go with them.
- **R10.** What is marked not decided below is built so that the other answer is a small change, and its
  docstring says that it is not decided.
- **R11.** Where the builder must choose a default between keeping a thing and dropping it, the default
  keeps it, and the choice is listed as provisional. This is Sid's line of 12 October above, which he gave
  for whose reads are kept; that it reaches a builder's own defaults is the cutter's reading.

## Not decided — kept only so that the build can run

The build builds each as written here, and decides none.

- **The order of acts after a failure.** Kept: Rama's default for the setting, as today (D).
- **How deep a permission can be once the 4 is gone.** Kept: as deep as a value may be, 32 today (C3).
  Sid was told that "lifted" gives about 32 and not a depth without end; he did not answer it.

## The builder chooses, and lists as provisional

None of these is a decision of Sid's. Each is written with its reason, and named in the receipt.

- The form in which a fact is appended, now that nothing is sealed.
- The log of answers: one depot or one for each gate, how it is partitioned, what an entry holds beside
  the answer's row, and whether a topology reads it.
- How the rebuild tells an entry that counted from one of an attempt that did not (B5).
- The form of the rebuild, and where its code lives. It is code of the store with a proof of its own.
  The use of it on a running store is not in this build: the exchange of the tables, a change of the
  number of tasks, the downtime.
- What the bound on the size of an act counts, and its number (C2).

## Not in this build, by decision

- **A sealed end of any kind** (entry 2). A value that its owners seal before it reaches the door needs
  nothing of the store. Locks that we host for a charge and hand to their owners, and a depot of its own
  for an organisation, are Sid's thoughts of 12 October and are not designed.
- **A forget.** The store can forget nothing when this build is done. A delete is a retract.
- **What entry 7 names as not built.** The three uses of a refused attempt: to raise it to the place that
  it was for, to see what a sender in bad faith does, to give an author back what was refused. And, before
  a stranger can reach the door: a bound on how much one sender can put into the log, and a way to block a
  sender.
- **Whose reads are kept** (entry 4). The decision is recorded. Nothing in the store says who is a model
  and who is a person until sign-in, and where such a line lands is not decided. The read path keeps
  nothing of a read.
- **Looking back**, and every other word of a query that the read build left out.
- **Copies, machines, the licence, backups and one restore, the number of tasks, who may reach Rama's
  ports, the proofs on two real nodes** (entries 5 and 6). They belong to the day a real cluster starts.
- **The other seven of Z5** (entry 6). Each is a session's work of its own.
- Sign-in, and who may act as the system. A screen, a view, a tool, an agent. What a key is (the queue's
  K5). How a permission ends.

Do not build any of these. Where the code must leave room for one, say so in the docstring.

## Explanations, as the tree's rules say

`src-store/CLAUDE.md` binds every file. Each namespace and each function has a docstring; each folder a
README with a parent link; no function lists in READMEs; no history of corrections. Each line of the
tree's pages that this build makes untrue is replaced, not amended: among them that the door seals each
value and keeps a stock of locks as the memory of its process, that a gate makes each lock and refuses a
value that is not sealed, that the acts depots are the replay log, that nothing is built for a rebuild,
and that the module appends to a depot in one place. The words of the locks and of the seal leave the
lists of "Words a session made, Sid's to change". The page for the rebuild holds "Decisions this build
stands on" (the entries of 12 October by number, the rest by pointer), "Engineering choices", "Not
decided, kept for now", and "Words a session made, Sid's to change". The proofs' README gains a row for
the rebuild's proof. Sid's answers are in `src-store/README.md` as his, never in these files as new text.

## How the build runs

By the Rama skill's stages, as the store's module, the door and the reads ran: the implied spec, the split
into parts, the plan and the check of the plan each in a fresh session, then one build session, then one
fresh review of the whole against this page. The papers go in
`src/proposal/backend-on-rama-2026-10-02/before-real-data/`, never under `src-store/` or `test-store/`. A
build's page, `before-real-data/PROGRESS.md`, records where the build stands, for a hand-over.

Three things that the read build learned, carried here by the cutter. Sid said the first two of that
build, on 12 October.

- A build is not judged a second time by the build session: "why you have to do the judging when its
  already done and being done???" The build stage makes its own validation and its own final runs, and one
  fresh review of the whole follows.
- An agent that waits for a permission tells its parent nothing: "the 2 parts were waiting on the
  permissions which i explicitly said not to have them". Whoever starts an agent for this build first
  makes sure that it cannot be asked for one.
- The shell is zsh. A sequence of proofs loads the source as each JVM starts, so no source is edited while
  one still has a proof to start. Never open a file whose name starts with `raw-` under a build's `runs/`.

Sid's way for a build: write a part whole, then prove it. No test or review loop after each small edit.
Keep the checks that the skill sets at its stages, and add none.

## Commits and the receipt

Commit on `main`, grouped by concern, each message saying what and why. No push. No `Co-Authored-By`, no
`Claude-Session`, no attribution line of any kind. Read `git log` and `git status` before each commit, and
stage named paths only: other sessions commit in this tree.

The receipt to Sid: the line "proven by a hand-played maker, used by no inhabitant yet", so that presence,
tested behaviour and use stay apart; the commit hashes; the proofs' outputs with their count lines before
and after; the measures of D, and the result on Rama's setting as a question for him; the list of the jobs
that the locks and the seal did, and where each went (A5); the list of every table with what it is made
again from, and each thing that is not made again with its reason; how deep a permission can be (C3);
every provisional choice; every not-decided thing as it was built; every place where this page could not
be met as written, and why; what the build found that is not decided, proposed as rows for the queue and
decided by nobody; `git status --short`.

Never read `src/app/server/env.clj` or any `.env` file.

## What this page rests on

- **Sid's:** the decisions section of 12 October, and the lines quoted above.
- **Run, by others:** what an append from inside an attempt of a batch does, on Rama 1.6.0 in-process
  (`reads/batch-probe/RESULTS.md`). The cutter ran nothing.
- **Read in the code by a gatherer** (a read-only agent on Sonnet, 12 October), by docstring first: each
  line of "What the code does today". The cutter checked six against the source, marked ✓ there. The list
  of places that take the depots of acts to be two was not checked. The count of lines that name a lock or
  a seal is the cutter's, by a search for whole words; a line that names a lock is not always a line that
  goes.
- **The read build's session's:** the sixteen findings, sent on 12 October at Sid's word to it. That
  session read or ran each; the cutter checked 1, 6, 8 and two numbers of 13 against the source. A4, A5,
  C3 and the last stamp of a batch in B3 rest on them.
- **Read in Rama's docs, not run**, in the extract that names 1.5.0: that a stream topology's writes to
  its tables are dropped when its event fails, and that a record is then processed again
  (`11-stream-topologies.md`, lines 156 to 170); that an append to a depot from a microbatch topology is
  not exactly-once across a failed attempt (`12-microbatch-topologies.md`, line 130; Rama's public page
  said the same on 12 October); that a table can be made again from a log when the processing gives the
  same result on a second run (`15-pstates.md`, line 45; `19-operating-rama.md`, "Task scaling").
- **A session's:** the reading of the whole into A to D, each rule's wording, each "kept" line under "Not
  decided", and that a rebuild which applies the answers needs no gate to give the same result twice. The
  last is reasoned, not designed and not run: B4 is its test.
