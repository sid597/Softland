# Build: before real data is kept — the plain end, the gates' answers in a log, and the tables made again

Cut by: Sid's session of 12 October 2026, the one that went through the sheet "Before real data is kept"
with him in chat (Opus 5.5, effort max).
For: one build, run by the Rama skill's stages (see "How the build runs").
Status: a starter. Sid's decisions of 12 October 2026 are the contract, with those of 6 to 12 October
before them. This page names what is **not decided** and is kept only so that the build can run, and what
the builder **chooses** and lists as provisional. The build decides none of Sid's open questions. The
stage that writes the implied spec checks each "as built" line of this page against the code: the cutter
had the code read by a gatherer, and checked six of its lines against the source (see "What this page
rests on").
Before the first stage: run the six proofs (`test-store/README.md`) and write their count lines on the
build's page as the "before"; read `git log --oneline -5` and `git status --short`. Files of other
sessions are in the working tree and not committed: `src/app/server/README.md`, two paths under `docs/`,
and the read build's scratch under `reads/` (its receipt, section 14). Leave them, and stage your own
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
                                       └─ seals a value only under a key that the layer has sealed   part A
                                          today: the door seals every value, and a gate refuses a plain one

A REBUILD   reads the logs of acts and the log of answers, and makes the tables again.              part B
            It decides nothing. A proof compares what it made with what the gates wrote.
```

- **A. The plain end, and sealing as a choice.** A value goes into the log plain, with no lock, unless a
  fact on its layer says that its key is sealed.
- **B. The gates' answers in a log.** What a gate decided for an act also goes into a log. A rebuild makes
  the tables again from the logs, and decides nothing again.
- **C. The limit of four on the depth of a permission is lifted.**

When this build passes: a maker's value under a key that no fact of its layer seals is in the log plain,
and no lock and no request for locks was made for it; a value under a sealed key is sealed as today; each
answer that a gate gave is in a log; a rebuild makes the tables again from the logs, and a proof shows
them equal to the tables that the gates wrote, with each exception named and its reason given; a
permission can be deeper than four. The six proofs of today stay and pass, each changed where this build
makes one of its lines untrue.

What this build is not: a record of who read what; a depot of its own for anyone; a cut out of a log; a
trim; a backup, a second machine or a real cluster; sign-in; a screen, a view or a tool.

## Read first, in this order

1. `src-store/CLAUDE.md` — the explanation rules. They bind every file of this build.
2. `src-store/README.md` — the tree, and the decisions sections. The last one, of 12 October, is this
   build's own. Read it with the sheet that it answers, `decisions/groups/g-check.txt`: its first page is
   the picture of what is kept of one act. Where the sheet and the section differ, the section is later
   and is Sid's. The sheet's box A of Z1, its "time" in Z2 and its suggestion for Z4 are not what he
   decided.
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
| 2 | A value is plain in the log when nobody has chosen a lock for it. The shared logs are never cut. | Z2 | part A |
| 3 | The log is kept whole: "forever never trim". | Z3 | R2 |
| 4 | A model's reads are kept, always; a person's, until she turns it off. | Z4 | not built here |
| 5 | Three machines, three copies and Rama's own backups, when real data is kept. | Z6 | not built here |
| 6 | The eight of Z5: yes to each. | Z5 | the fourth is part C |

Earlier decisions that this build leans on: the act holds plain values, and the door seals (6 October, 3);
a permission's form (6 October, 8); two depots of acts, one for each gate (7 October, 1); the record is in
the gates' PStates, and its reason as corrected on 12 October (7 October, 2); the door reads an act's
answer from the record by act-id, and an ack is a signal (7 October, 6 and 7); a stamp holds the clock
time and a counter (8 October, 4); one lock seals one value (9 October, 2); the tables keep a value plain
(11 and 12 October, 5); a read is done in the runtime with Rama's push, and a pushed value is not a cache
(11 and 12 October, 3).

Three lines of Sid's hold the whole build:

- 11 October: "can't we just replay the log?? ... even if the main gets blown up we should be able to
  replay log and get to latest".
- 11 October: "plain we will not store any locks and such by default it is up to the layer, tool, group
  etc if they want to for their they can and should but not by default and we are providing the
  spectrum."
- 12 October: "by default if its not set then we take the side of spectrum that captures more because we
  are not choosing the destructive side by defualt that can happen even after but the other side cannot."

## What the code does today

From a gatherer's report of 12 October, by file and line. The lines marked ✓ were checked against the
source by the cutter; the others were not. The implied spec checks each.

- **The door seals each value fact.** A value fact is "a fact that carries a value and whose key is not
  one of the store keys" (`door/additions.clj`, namespace docstring ✓). `entry-of` decides it for each
  fact; `facts-to-append` seals, and `:v` becomes `:sealed` and `:lock-id`. Nothing in the door or in the
  declaration makes sealing a choice.
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

## What to build

The split into parts is the skill's stage to make. This is one reading of the whole.

### A. The plain end, and sealing as a choice

- **A1. A fact on the layer says which keys are sealed** (entry 2). One more store key. A layer with no
  such fact seals nothing: plain is where every layer starts. It is one of the store's own facts, as a
  layer's setting, its list and its marks are, and the read path treats it as it treats those.
- **A2. The door seals a value only under a sealed key.** Under any other key it appends the value as the
  maker gave it: no seal, no lock, and no request for locks. Under a sealed key it does what it does
  today.
- **A3. The door knows, before it appends, whether a key is sealed.** A gate's refusal comes after the
  log has the value, and nothing is ever cut out of a log (entry 2). So a value under a key that the door
  knows to be sealed never goes to the log plain. The door already learns a layer's depot from the module,
  by the query `depot-of-layer`; how it learns the sealed keys is the builder's.
- **A4. A gate refuses a plain value only under a sealed key.** Under a key that is not sealed it accepts
  the plain value and keeps it in the fact's row, as the read build left that row. Under a sealed key it
  does what it does today, the opening of the value with its lock too.
- **A5. The request for locks, the stock and the locks tables stay**, for sealed keys. A door that
  writes only under keys that are not sealed asks for no lock.
- **A6. What was written before is not touched.** A value that is sealed in the log stays sealed there,
  and its lock stays. A value that is plain in the log stays plain there. No log is written again.

### B. The gates' answers in a log, and the tables made again

- **B1. A gate appends what it decided for an act to a log** (entry 1): the act it is for, refused or
  not with the word, the stamp, and the digest of the act. That is what the answer's row holds. No value
  of a fact is in it.
- **B2. The tables stay the record that is read.** The door reads an act's answer from the tables by
  act-id, a gate reads its own record before it decides, and the read path reads the tables (7 October,
  2, 6 and 7). Nothing reads the log of answers but a rebuild.
- **B3. A rebuild makes the tables again and decides nothing.** It reads the logs of acts, the log of
  answers and the log of the clocks of batches, and no table but the locks tables. From them it makes
  each table that the gates write. The plan lists every table and every part of a row, and says for each
  what it is made again from. What cannot be made again is named with its reason, and goes to Sid in the
  receipt. Two that the cutter knows: the bytes of a lock, which are in no log by design; and what a gate
  keeps of a stray, where the store's page says that the word rests on an order that no depot holds
  (rule 19).
- **B4. A proof compares.** It sends acts through the door and through no door, on both depots: accepted
  and refused acts, an act sent twice, a second and different act under a used act-id, a stray, a
  retract, a fact under a marked key, a value under a sealed key and one under a key that is not. Then it
  runs the rebuild and compares what it made with what the gates wrote, row by row.
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
  to propose. They are kept with the builder's other numbers, and decided by nobody.
- Not here: a gate still reads none of the permissions above an act's own. That read comes when a
  permission can end, which is not built.

### D. The measures

Sid, 7 October: "idk lets go by whats measured and we will know better when we build more and optimise".
The build measures, and sets no target:

- an act through the door under a key that is not sealed, beside one under a sealed key;
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
- **R4.** A value under a key that the door knows to be sealed never goes to the log plain (A3). What the
  store cannot hold to at the moment when a key is being sealed is said plainly on the store's page, under
  "What the gate does not check, said plainly", and in the receipt.
- **R5.** Nothing is shown before it is in the tables, and the build keeps no store of its own beside
  Rama's push (`CLAUDE.md`, "Systemic repair and caching"; Sid, 11 October, of a pushed value: "i also
  don't consider it a cache"). If the build finds itself reaching for a cache, for the sealed keys of a
  layer or for anything else, it stops and says so on its page.
- **R6.** No new code takes the depots of acts to be two. About twenty places do today, and this build
  adds none. A depot of its own for an organisation is Sid's thought of 12 October and is not built.
- **R7.** A second attempt of an event or of a batch leaves the store as one attempt would. The Rama
  skill: "Duplicate side effects from retried processing are bugs."
- **R8.** Every word that the build makes is plain, says what the thing is, and is listed for Sid under
  "Words a session made, Sid's to change". "Lock" is an encryption key and "key" is the key of a fact,
  never one word for both (`src-store/CLAUDE.md`). No word comes from the rig's code.
- **R9.** The earlier handoffs hold where they touch. This build changes these lines. In
  `BUILD-store-module.md`: item 7 of "What to build", where a gate refuses a plain value under a key that
  is not a store key; rule 9, since a rebuild picks no winner and reads the answer; rule 19, since a
  rebuild is built. In `BUILD-door.md`: part 4 of "What to build", since the door seals a value only under
  a sealed key.
- **R10.** What is marked not decided below is built so that the other answer is a small change, and its
  docstring says that it is not decided.
- **R11.** Where the builder must choose a default between keeping a thing and dropping it, the default
  keeps it, and the choice is listed as provisional. This is Sid's line of 12 October above, which he gave
  for whose reads are kept; that it reaches a builder's own defaults is the cutter's reading.

## Not decided — kept only so that the build can run

The build builds each as written here, and decides none.

- **The grain at which a lock is chosen.** Kept: by key, as a fact on the layer. It is a session's
  reading of Sid's line of 9 October, dictated, where "log" is the lock: "any tool user layer group can
  decide by themselves what uh, a log delete means for different kind of values for their tool or layer".
  He was shown the reading twice and did not answer it. His line of 11 October names "the layer, tool,
  group etc".
- **Who may write the fact that seals a key.** Kept: whoever may act in the layer, as for a layer's
  setting, its list and its marks (the read build's receipt, section 8). With it, any holder of "see and
  act" in a layer can take the seal off a key, and each value that is written after that is in the log
  for ever.
- **Whether the seal can be taken off a key.** Kept: the fact takes a second write, as the three keys of
  the read build do.
- **What sealing a key does to the values that were written before.** Kept: nothing (A6).
- **The order of acts after a failure.** Kept: Rama's default for the setting, as today (D).

## The builder chooses, and lists as provisional

None of these is a decision of Sid's. Each is written with its reason, and named in the receipt.

- The form of the fact that seals keys, and how the door learns it before an append (A3).
- What a gate does with a sealed value under a key that is not sealed.
- The log of answers: one depot or one for each gate, how it is partitioned, what an entry holds beside
  the answer's row, and whether a topology reads it.
- How the rebuild tells an entry that counted from one of an attempt that did not (B5).
- The form of the rebuild, and where its code lives. It is code of the store with a proof of its own.
  The use of it on a running store is not in this build: the exchange of the tables, a change of the
  number of tasks, the downtime.
- What the bound on the size of an act counts, and its number (C2).

## Not in this build, by decision

- **Whose reads are kept** (entry 4). The decision is recorded. Nothing in the store says who is a model
  and who is a person until sign-in, and where such a line lands is not decided. The read path keeps
  nothing of a read.
- **The other ends of entry 2**: a depot of its own for an organisation; locks that their owners hold;
  locks that we host for a charge and also hand to their owners. A layer whose members hold their own
  locks.
- **A forget.** Nothing destroys a lock in this build, and nothing is cut out of a log.
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
store's page that this build makes untrue is replaced, not amended: among them that the door seals each
value, that a gate refuses a value that is not sealed, that the acts depots are the replay log, that
nothing is built for a rebuild, and that the module appends to a depot in one place. The page for the
rebuild holds "Decisions this build stands on" (the entries of 12 October by number, the rest by
pointer), "Engineering choices", "Not decided, kept for now", and "Words a session made, Sid's to change".
The proofs' README gains a row for the rebuild's proof. Sid's answers are in `src-store/README.md` as his,
never in these files as new text.

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
and after; the measures of D, and the result on Rama's setting as a question for him; the list of every
table with what it is made again from, and each thing that is not made again with its reason; what the
store cannot hold to while a key is being sealed (R4); every provisional choice; every not-decided thing
as it was built; every place where this page could not be met as written, and why; what the build found
that is not decided, proposed as rows for the queue and decided by nobody; `git status --short`.

Never read `src/app/server/env.clj` or any `.env` file.

## What this page rests on

- **Sid's:** the decisions section of 12 October, and the lines quoted above.
- **Run, by others:** what an append from inside an attempt of a batch does, on Rama 1.6.0 in-process
  (`reads/batch-probe/RESULTS.md`). The cutter ran nothing.
- **Read in the code by a gatherer** (a read-only agent on Sonnet, 12 October), by docstring first: each
  line of "What the code does today". The cutter checked six against the source, marked ✓ there. The list
  of places that take the depots of acts to be two was not checked.
- **Read in Rama's docs, not run**, in the extract that names 1.5.0: that a stream topology's writes to
  its tables are dropped when its event fails, and that a record is then processed again
  (`11-stream-topologies.md`, lines 156 to 170); that an append to a depot from a microbatch topology is
  not exactly-once across a failed attempt (`12-microbatch-topologies.md`, line 130; Rama's public page
  said the same on 12 October); that a table can be made again from a log when the processing gives the
  same result on a second run (`15-pstates.md`, line 45; `19-operating-rama.md`, "Task scaling").
- **A session's:** the reading of the whole into A to D, each rule's wording, each "kept" line under "Not
  decided", and that a rebuild which applies the answers needs no gate to give the same result twice. The
  last is reasoned, not designed and not run: B4 is its test.
