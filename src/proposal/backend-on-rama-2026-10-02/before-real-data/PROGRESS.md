# Before real data is kept: where the build stands

For a session that takes this build over, and for the receipt to Sid. Kept by the build session
(Opus 5.5), started on 12 October 2026 on Sid's message to it. "The build session" below is the one
that holds the build at the time.

## Sid's words for this run

All of 12 October 2026, in the build session's chat. The first four paragraphs he pasted as one
block; the last two lines he typed under it.

- "Build what comes before real data is kept. Start from
  src/proposal/backend-on-rama-2026-10-02/BUILD-before-real-data.md and read what it names, in its
  order."
- "This phase is all about fixing things that were wrong assumptions. We are removing all the locks.
  We should be able to replay the log and get to latest, so what a gate decided also goes into a log,
  and the tables are made again from the logs. A refused act stays in the log: we are not choosing
  the destructive choice. And the limit of four on a permission's depth goes."
- "Run it by the Rama skill's stages, as the store's module, the door and the reads ran, and keep the
  explanation rules in src-store/CLAUDE.md."
- "Decide none of my open questions: build what the page marks as kept for now, list what you choose
  as provisional, and give me one receipt at the end."
- "use opus-5.5-xhigh for code implementation if you use subagents for it"
- "do the commits don't wait for my permissions"

How the build session reads them together. His open questions stay open: the queue, and the
contract's list "Not decided — kept only so that the build can run". Each is built to the kept answer
so that the other answer is a small change, and is decided by nobody. What the contract leaves to the
builder is chosen with its reason and listed as provisional. Where the build meets a new thing that
only he can decide, the build session picks a working answer so that the build does not stop, marks
it **provisional, for Sid**, and lists it under "For the receipt". A working answer is never written
as a decision of his, in this page or in the tree.

## The shorter path, from 13 October

After midnight Sid asked why the build took so long, and what the process was. His lines, as he
typed them: "but this is not a fresh work right??? it is about removing code that is now not
needed???"; "why do we need to go through 7 steps to remove the code?? and same for the tiny
one??"; "so why the fuck did we did that and doing that??? this is such a bad design are you pretty
sure this is a good way to wroK why or why not??"

What the build session answered. The full process of the skill was applied to every part alike, and
for the taking out of the locks and for the depth of a permission that was too much: the skill's own
page says that the full process is for work that designs depots, tables and topologies together,
and that a simpler task takes judgment. The build session had followed "Run it by the Rama skill's
stages" to the letter and had not told him at the start that a third of the build is a removal.
Two hours had passed with no line of the store changed. The one thing of those hours that changed
what is built is the run on Rama, which the contract asks for.

The path that the build session told him it takes from there, "unless you say otherwise". He did not
strike it. It is the build session's, and no decision of his; ruling 31 holds it.

## What to read, in this order

1. `../BUILD-before-real-data.md` — the contract. It is the full spec, and it names what is not
   decided.
2. `src-store/README.md` — Sid's decisions, the section of 12 October above all. It wins over the
   contract.
3. This folder's papers, by stage: `IMPLICIT_SPEC.md`, `DECOMPOSITION.json`, then for each part
   `PLAN*.md` and `PLAN_VALIDATION*.md`, and after the build `IMPLEMENTATION_VALIDATION*.md`,
   `TEST_VALIDATION*.md` and `FULL_SPEC_REVIEW.md`.
4. `stream-probe/RESULTS.md` (this build's) and `../reads/batch-probe/RESULTS.md` (the read build's):
   what was run on Rama about an append to a depot from inside a topology. Each is a probe: no form,
   name or table shape comes from one.
5. "Rulings", below: what the build session ruled on each paper, with the reason.

## The starting point, confirmed

The contract asks for three things before the first stage.

- `git log --oneline -5` at the start: `46c3e5dc` (memory: nothing more goes into a page that Sid
  approved until he has seen it), `68832bb2` (the contract, cut again: the locks go), `5cd74167`
  (Sid's decisions of the evening of 12 October), `0bfadf25`, `9a062b5b`.
- `git status --short` at the start: ` M src/app/server/README.md`; untracked `docs/.obsidian/`,
  `docs/Untitled.md`, `reads/QUESTIONS-rama.md`, four logs under `reads/plan-probes-read-path/logs/`,
  and the `raw-` files of the read build under `reads/runs/`. None is this build's. They are left as
  they are, and every commit of this build stages its own paths by name.
- The six proofs, run by the build session on that tree on 12 October, one after the other, each
  with exit 0. This is the "before":
  - `clojure -M:store -m softland.store.act-proof` — `{:test 8, :pass 160, :fail 0, :error 0}`
  - `clojure -M:store -m softland.store.pure-proof` — `{:test 18, :pass 690, :fail 0, :error 0}`
  - `clojure -M:store-module -m softland.store.module-proof` — `{:test 33, :pass 354, :fail 0, :error 0}`
  - `clojure -M:store-module -m softland.store.door-proof` — `{:test 27, :pass 199, :fail 0, :error 0}`
  - `clojure -M:store -m softland.store.read-path-pure-proof` — `{:test 12, :pass 152, :fail 0, :error 0}`
  - `clojure -M:store-module -m softland.store.read-path-proof` — `{:test 27, :pass 482, :fail 0, :error 0}`

  They are the six count lines with which the read build ended (its receipt, section 3). Each cut
  output is `runs/before-<proof>.txt`: the command, the time, the `SHOWN` lines once, the count line
  and the exit status. `runs/run-proofs.sh` made them, and makes every later run of the proofs in
  this build. The whole output of each run is `runs/raw-<label>-<proof>.txt`, which is not committed
  and which no session opens.

## The stages

The Rama skill's stages, as the store's module, the door and the reads ran: each design stage in a
fresh session (the Agent tool, model Opus 5.5, effort xhigh), ruled on here. This build changes a
module that exists, its door and its read path, and adds the rebuild; each stage's paper says how it
adapts the skill's template.

| stage | state | commit |
|---|---|---|
| the starting point confirmed | done | — |
| a run on Rama beside the first stage: what an append to a depot from inside an event of a stream topology does (`stream-probe/`), the contract's "Runs before the plan is fixed"; rulings 27 to 30 | done | in the commit after `ba29f4cb` |
| the implied spec, with the check of each "as built" line of the contract against the code; rulings 1 to 20 on its 22 points | done; judged sound | `ba29f4cb` |
| the split into parts: three, `the-locks-go`, `answers-in-a-log`, `the-rebuild`; rulings 21 to 26 on its notes | done; judged sound | in the commit after `ba29f4cb` |
| the plan of `the-locks-go`, with its run of ruling 19's condition (`plan-probes-the-locks-go/`); ruling 34 on its nine points | done | — |
| the plan of `answers-in-a-log`, with three runs of its own (`plan-probes-answers-in-a-log/`); ruling 32 on its six points | done | — |
| the build of `the-locks-go`, straight from its plan, by six sessions at once, each with files of its own (ruling 33) | running | — |
| the build of `answers-in-a-log`, straight from its plan; it takes D's first numbers before it changes the module (ruling 23) | — | — |
| the build of `the-rebuild`, with no plan of its own: its proof compares what it made with what the gates wrote, row by row | — | — |
| the review of the whole against the contract | — | — |
| the measures of the contract's section D, on the final tree | — | — |
| the receipt to Sid | — | — |

How a stage is run: a fresh agent reads the skill's doc for its stage, the contract and its read
list, and the papers before it; it writes one paper (or the code) and does not commit. The build
session reads its report and the parts of its paper that need a ruling, checks in the source the
facts that a ruling rests on, rules, has the rulings applied, and commits with a message that says
what and why. From the build of a part on, the build session takes the verdict from the build's own
papers and final runs, and does not judge the build a second time; one fresh review of the whole
follows (Sid, 12 October, of the read build: "why you have to do the judging when its already done
and being done???"). Each commit names its own paths in the commit itself (`git commit -m ... --
<paths>`). No `Co-Authored-By`, no session link, no attribution line. Never push. An agent that
writes code runs on Opus 5.5 at effort xhigh, at Sid's word. Each agent is told that a tool call
which is refused or would wait for a permission is not waited for: it takes another way and says so
in its report.

Two things about runs, from the read build. One in-process cluster at a time on this machine: two
collide on a port. So every run of this build that starts a cluster takes one lock first:
`flock <this folder>/runs/cluster.lock <the command>`. `runs/run-proofs.sh` does it for each proof
that needs Rama, and each agent is told to do it for a probe's run. And a proof's JVM loads the
source when it starts, so no file under `src-store/` or `test-store/` is edited while a sequence of
proofs still has one to start.

Where the build session placed the contract's runs. The contract says "The plan stage makes these
... before it chooses a form for the log of answers". They are run by a fresh session of their own,
beside the first stage, so that no plan waits for them and no plan is written before them. The plan
stage reads the results and makes more runs where its own questions need them.

## Other sessions in this tree

Three sessions write on `main` in this working tree on 12 October. The build session learned of the
other two from `git log` and `git status`, and of what Sid asked each from a note that the first
wrote and Sid pasted into the build session's chat, and from the record of his lines.

- **The paper-work session.** Sid gave it the six jobs of entry 6 of 12 October that are no build's:
  the pass over the decisions page and over the rulings file of September, "freezes", three lines
  for the method, the check's six reports with a row in the queue for each thing nobody asked,
  Rama's docs for 1.6.0, and one page of what must be true before real data is kept. Its limits, in
  its own note: it changes no code, and does not touch the contract, this folder or `test-store/`.
  Its commits: `dc4bd12b`, `f82165d2`, `ccf75654`, `3617906f`, `ea96d342`, `57f6d95d`. It has not
  read this build's contract, implied spec or rulings.
- **The session that keeps the open questions about Rama** (unnamed, `f55ca548` in the record). On
  Sid's line of 12 October, "we will not be sending anything right now keep them as open question
  only ... colocate it with code" (‹bf407e42›), it wrote `src-store/OPEN-QUESTIONS-rama.md` and one
  row of the table at the top of `src-store/README.md` that links to it. Neither is committed.

Where their work meets this build's, and what the build does:

- `src-store/softland/store/README.md`, the store's page. The paper-work session changed four
  places in `f82165d2`: the row F2 A of "Decisions this module stands on", where "the acts depots
  are the replay log" is now marked as a session's sentence; the row of E3·H7 B; the sentence on the
  reason for F5 A; and "see" and "see and act" under the words. Each part of this build edits that
  page. Its plans were started on the head after those commits, so they read the new text.
- `src-store/README.md`, the tree's entry. This build replaces lines of its opening paragraphs and
  of its table, and no line of a decisions section. The page holds the third session's row, not
  committed. A commit of this build takes only its own lines of that page: the index is pointed at
  "HEAD plus this build's edits", and `git diff --cached` and `git diff` are read before the commit.
- `decisions/OPEN-QUESTIONS.md`, the queue. The paper-work session added rows V1 to V34. This build
  writes nothing into the queue; the receipt names the rows that the build makes stale (V1, V17,
  V30, V31 and `act-kept`, by that session's own list) and proposes its new rows from V35.
- `decisions/METHOD.md`. Its new section names three things that Rama cannot change once data is
  kept: the number of tasks, the name of a depot or of a table, and the keys of a table. The plan of
  `answers-in-a-log` names a depot; it was told to mark each such name and each form of a key as
  cheap to change now and not later. The same section says that a builder's "is Sid's" line goes to
  the queue the same day, where Sid's word to this build is "one receipt at the end". Put to Sid.
- `reference/rama/release-notes.md`. The note for 1.6.0 says that the commit and the replication of
  a microbatch changed. The plan of `answers-in-a-log` was told to read it against each line of the
  docs that its table of a batch's points of failure leans on.
- `../PROGRESS.md`, the page of the folder above. The paper-work session edits its section "Now (12
  October 2026)". This build edits that section when it ends, and reads the page again first.
- `../WHAT-MUST-BE-TRUE.md`. Three of its lines are what this build delivers: every table made again
  from the logs, no locks, each log kept whole. Its last section is a session's and binds nobody.

## Rulings

Each ruling is the build session's, with its reason. None is a decision of Sid's. Three marks:
**kept** is the contract's own kept answer, built so that the other answer is a small change;
**provisional, for Sid** is a working answer to a thing that only he can decide and that the contract
does not name, picked so that the build does not stop; **builder's** is engineering, and is listed in
the receipt as a provisional choice.

### On the implied spec (`IMPLICIT_SPEC.md`), its 22 points

The paper is judged sound, and it is the ground for the plans. It checked each of the contract's 24
"as built" lines against the source: 17 hold, 6 hold in part, 1 does not (the places that take the
depots of acts to be two are 36 in 17 files, and not about twenty; its section 2 is the list that R6
is held against). It lists 22 jobs of the locks and the seal where the contract names three (3.3),
every part of every row with what it is made from (4), and 28 edges that a rebuild meets (5.2). The
build session read sections 1, 3.3, 4, 5.1, 5.2 and 8 whole, and checked in the source the three
facts that rulings below rest on: that a named shape with no number in `nesting` can hold itself no
times (`act.clj`, `conform`); the door's tests and their order (`door/additions.clj`, `reading`); and
what the test for a stray holds (`door/declaration.clj`, `stray-reason`).

Where a ruling and the paper differ, the ruling holds. The paper is not edited.

1. **P1, what the door still drops — builder's, and Sid hears of it.** The three tests stay: the
   thing is a plain map; it does not already hold `:act-id` or `:depot`; its `:layer` is an id that
   an act-id can hold, and an act-id given back is of that layer. Reason: the contract's own words,
   "as its first rule says already" (A4), and that rule, D1 of the door's contract: "The door's own
   words name only what the door could not do, never a judgment of the act." Each of the three is a
   thing to which the door cannot give an act-id under which a gate could answer: appended, it would
   be a stray, with no answer, ever. The door reads no part of what the act says for them: no who, no
   permission, no fact. So R4 holds. What Sid hears: three kinds of thing that a maker hands the door
   are in no log, and his line "we are not choosing the destructive choice" could be read to reach
   them. The other answer is to append each as it is, so that a gate keeps it as a stray, and still
   tell the maker the door's word. It was not taken because it makes the door send things under an
   act-id that a maker wrote, which his decision 5 of 6 October gives to the door alone, and nobody
   has designed that.
2. **P2, "holds nothing in the memory of its process" — builder's.** It means no state that changes
   between two calls. The door keeps its handles and the two numbers of a maker's wait for an answer,
   fixed at the opening. The two numbers get a home that is not the stock's file, and an opener can
   still give them.
3. **P3, the bound on the bytes of one value — builder's, a kept number.** The number is 65,536,
   counted on the bytes that the fact's row holds for the value: the UTF-8 of the one text of its
   normal form. Reason for the number: the contract says that the bound "stays, counted on the plain
   value", and 65,536 is the number that stays; it takes each value of today and values up to 28
   bytes longer, and of the two candidates it is the one that refuses less. It is held by a gate, and
   an act over it is refused before the gate reads any row for it; the plan says how the text of a
   value is not made more often than it must be. It stays a rule of the values that it bounds today,
   those under plain keys: a value under a store key keeps its own limits. The word of the seal goes
   and a plain word takes its place (R8). For the receipt: through the door a value of any size that
   Rama's client can write is now in the log.
4. **P4, a thing in today's sealed form — builder's.** `:sealed` and `:lock-id` leave the shape of an
   arrived fact, so the engine refuses such a fact with the word that it has for a part that the
   shape does not name; a request for locks is refused as a key of the store's namespace that the
   store does not know. No word of its own is made: the store has no sealed form, and a word for one
   would keep it in the code (Sid, of keeping the locks: "whenever we will build some code, it will
   always come up"). For the receipt: the code after this build refuses every value act of a log that
   the code at `13a1d65b` wrote. A7 says that nothing real is in one.
5. **P5, bytes that are no value — builder's, and Sid hears one part.** What only the seal's road can
   reach goes: the making of a row's String from bytes that are no text. What a reader answers for a
   String that is no value's text stays, because a read never throws on what a row holds; its
   docstring says that no write of a gate gives such a row. For Sid: once the seal is gone, no
   arrived value can be a run of bytes. The sheet lists "A value cannot be bytes, such as an image"
   as kept and not decided; the one road that existed, which nobody asked for, closes.
6. **P6, a maker's own second send — builder's, for the receipt.** As it falls out: one act under
   one act-id is one thing with one digest, so both sends read the one answer as their own. That is
   what the store's rule 1 asks. A1 forbids the other answer, a third addition of the door's.
7. **P7, which lines of the earlier handoffs change — builder's.** R9 names the heads. The build
   departs from every line of the two handoffs that parts A to C make untrue, and the plan of each
   part lists the lines that it departs from. Neither handoff file is edited: each is the contract of
   a build that is done. The store's page is where the tree says how each rule is met, and there
   each line that becomes untrue is replaced. D6 keeps its clause that the door never reads a fact of
   the record for a maker.
8. **P8, a stray — builder's.** That a thing is a stray is something a gate decided, and it goes into
   the log of answers with its word and its digest. A rebuild makes the sets of strays from those
   entries and never runs the test for a stray. Reason: R1. The paper shows that the sets can be made
   from the logs of acts alone today, by running the test again. But the test holds numbers that are
   the builder's and kept, the length of an id among them, and a rebuild that ran it again would give
   another set on the day that one of them changes. That is the case that entry 1 was decided
   against. The contract's sentence allows it: "the plan confirms that, or finds a log for it."
   The plan may show, with what it costs, why this cannot hold; it then names the test for a stray as
   the one place where a rebuild decides again, for the receipt.
9. **P9, what "equal" means, and the parts that are made from the runs — builder's; Sid hears each
   exception.** Equal is: each table, on each task, row by row, on a store at rest (ruling 13), with
   the number of tasks that decided the acts. The rebuild takes the number of tasks as something it
   is given, since a second run from the logs is the road to another number (entry 5 of 12 October,
   a session's note there); the comparison is made at the number that decided the acts. Part by part:
   - the rows of the facts by value of an earlier marking are made again, equal: applying the acts in
     the order of their stamps gives them, and leaving them out would be the dropping side (R11);
   - `:closed-through` is made again as what a store at rest holds, the gate's last stamp;
   - the row of `$$batch-clock` is made again from the log of the clocks of batches. The paper found
     that of two records of one batch the kept one is the first that the topology processed, and
     that this topology's retry mode lets a later record pass a failed one. The plan either makes the
     log's order the order of the keeping, or names the case as an exception with its reason;
   - `:turns` is the working state of one run of the gate. The plan says what a log would have to
     hold to give it again and what that costs, and then either makes it again or names it as an
     exception with its reason. The build session's view, for the plan to test: nothing reads it once
     its batch is committed, so it is an exception.
10. **P10, "and no table" — builder's.** No table that a gate wrote. The rebuild knows what it has
    itself made so far, the row before of a place among it.
11. **P11, telling an entry that counted from one that did not — builder's, the plan's to choose
    after the runs.** What holds whichever way: R3, in its own words, so no reader, no door, no
    gate's decision and no tool takes an answer from the log. If a topology of the module reads the
    log, the contract's line "Nothing reads the log of answers but a rebuild" gives way to R3's
    words; the contract gives that choice to the builder. The plan shows B5 with a run for each gate.
12. **P12, the keeping of an answer and the commit of the tables — builder's, the plan's to choose
    after the runs.** One reading is ruled here. R7 is of the tables, and of what a rebuild makes
    from the logs: a second attempt leaves the tables as one attempt would, and leaves the logs such
    that a rebuild comes to those tables. An entry more in a log is no fault (B5 is the contract's
    own answer for the log); a table made again that differs is. Two properties the plan must show at
    every point of failure that a run found real: no answer stays in the tables for good with no
    entry in the log; and no entry of an attempt that did not count changes what a rebuild makes.
13. **P13, the moment of a rebuild — builder's.** Equal is asked of a store at rest: every appended
    thing decided, every batch closed, the run after the last batch done. The proof waits for that.
    The rebuild's page says what it gives of a store that is not at rest, as what falls out and not
    as something proven equal.
14. **P14, R1 and the one form of an accepted act — builder's.** A rebuild never changes an answer
    that the log holds. It makes the rows of an accepted thing with the gates' own row functions.
    Giving a thing its form is not deciding; refusing it is. Where the code of the day cannot give
    the form of a thing that the log says was accepted, the rebuild writes the answer's row, makes no
    fact row that it cannot make, and names the act, so that the case is seen and not dropped. And R1
    is proven, not only reasoned: the rebuild's proof hands it an answer that today's gate would not
    give for its act, and shows that the answer is applied.
15. **P15, which log holds a batch's last stamp — builder's, the plan's.** Whichever log it is takes
    no client's append and has no trimming option. If it is none of the three kinds that B3 names,
    the list of what a rebuild reads grows, and the plan says so.
16. **P16, where the depth of a permission is held — builder's, around a number that is Sid's and
    stays not decided.** The shape keeps a number, and it is the value's: one number bounds an act's
    own permission and a permission that is given, and `max-permission-depth` goes as a number of its
    own. Reason: the contract's kept answer is "as deep as a value may be", and the paper found that
    this bounds only a permission that is given; an act's own is no value. With no number at all the
    engine lets a permission hold no parent, which the source confirms. An act's own permission that
    is deeper than any that can be given could never stand, so the shape refuses it before a row is
    read. The plan makes the two roads agree on one greatest depth and says the number; the receipt
    gives it (C3).
17. **P17, the bound on the size of an act — builder's, the plan's to propose.** What holds: a gate
    holds it and not the door, so an act over it is in the log whole and is refused with a plain
    word; it is counted without one more walk of the act where the gate already has what it counts;
    and the depth of a permission is kept below the text's 64 by ruling 16, not by this bound.
18. **P18, R6 and the new code — builder's.** R6 forbids a new choice by a depot word, a new pair of
    names in shared code, and a new "the other". And the rebuild takes its logs of acts as a list,
    each with the gate that decided it: it is the road by which a later depot of acts would be read,
    and the list costs it nothing. Whatever log of answers the plan chooses is declared by its gate's
    own code.
19. **P19, "as the maker gave it" — builder's.** The door adds its two parts and changes nothing
    else: a list of facts goes as a list, and the door makes no normal form. On one condition, which
    the plan runs: the digest that the door gives a maker is the digest that a gate takes of the
    thing off the depot, for `:facts` that is a list or a lazy sequence, and for a value of each kind
    that the form takes. Where a run shows that the two cannot agree, the door changes what it must
    for them to agree, and its docstring says what and why.
20. **P20 to P22 — Sid's, and kept.** The order of acts after a failure: Rama's default, as today;
    D measures the setting at 1 and the result goes to him. How deep a permission can be: as deep as
    a value may be. What entry 7 left open stays open; the receipt says beside it, as facts, which
    stops of the door went with the locks and that nothing now paces a sender.

Three things of the paper that are no open point and that later stages must hold:

- **Every value now crosses Rama's writing and reading of a record as Clojure data** (its J13).
  Today only bytes and the values of store keys do. A maker reads her answer by her digest, so a
  value that came off the depot with another text would read "not mine" for an act that was
  accepted. Ruling 19's run is the guard, and the proof shows a value of each kind through each
  depot.
- **The order of a marked key holds every fact under that key whose stamp is later than the key's
  stamp, and no other** (its edge 18). A rebuild that applies acts one by one in the order of their
  stamps must give that, also for a mark and a fact in one batch of the base.
- **The contract cites `15-pstates.md`, line 45, for a condition that is in `19-operating-rama.md`,
  "Task scaling"** (its 1.3, point 10). For the receipt.

### On the split into parts (`DECOMPOSITION.json`), its notes

The split is judged sound. It is not the contract's A, B, C and D, and the contract leaves the split
to this stage. Three parts, each with one hard part, each leaving the tree whole: `the-locks-go` (A1
to A8 and C1 to C3), `answers-in-a-log` (B1, B2, B6, the first half of B5, the stray's entry, a
batch's last stamp, the runs before the plan, and D with the code that measures), `the-rebuild` (B3,
B4, the second half of B5, R1, and the comparison's proof). Its first entry holds the coverage of
the contract and of section 9 of the implied spec; it could give nothing to no part.

21. **"The plan" is two plans — builder's.** Each choice of what a log holds, in rulings 8, 9, 11, 12
    and 15, is made by the plan of `answers-in-a-log`, and `the-rebuild` takes it as made. The split
    proposed a guard: that the plan of `the-rebuild` be written and checked before the build of
    `answers-in-a-log` is called passed. The build session rules a stronger one: the plan of
    `the-rebuild` is written and checked before the build of `answers-in-a-log` starts. Reason: the
    two are a writer and its one reader. A part of a row that the logs cannot give is then found on
    paper, and the plan of `answers-in-a-log` is corrected before any code of it is written.
22. **Rulings 14 and 15 do not meet — builder's.** The proof of ruling 14 hands the rebuild an
    answer that no gate of today would give. It may hand the rebuild its logs as data; it need not
    append to a depot. So a log that takes no client's append stays within the proof's reach.
23. **D's numbers are of two trees — builder's.** The build of `answers-in-a-log` writes the code
    that measures first, and takes D's first numbers on the tree as `the-locks-go` left it, before it
    changes the module. The build session runs the measures once more on the final tree, after the
    review, for the receipt.
24. **C inside `the-locks-go` — builder's.** Taken as the split made it. C plans no state and changes
    the forms and the checks that A changes anyway.
25. **The plans of `the-locks-go` and of `answers-in-a-log` are written side by side — builder's, and
    a departure from the skill.** The skill has each part planned on the part before it. The one
    thing that the second plan needs of the first is the form in which a thing is on a log of acts,
    and ruling 19 gives it. The plan of `answers-in-a-log` takes that form from ruling 19 and says
    so; if the first plan's run of ruling 19's condition changes the form, the check of the second
    plan is told. The builds are one after the other, in the split's order. For the receipt.
26. **One paragraph of the store's page is written three times — builder's.** Number 19 of "How each
    of the builder's rules is met". Each part replaces what it makes untrue. That is what leaving the
    tree whole after each part costs.

### On the stream probe (`stream-probe/RESULTS.md`)

The contract's three "Runs before the plan is fixed", and two more that the plans need, run by a
fresh session on a module of its own, Rama 1.6.0 in-process, one machine, one worker, four tasks.
Most scenarios ran once, and the paper says where two runs of one thing differed. The build session
read "The short of it", "Found", "Seen on the way" and "Not settled by these runs" whole. What the
plans stand on, each run and not read:

- A throw in an event of a stream topology starts the whole worker again, every task of it (36 of
  36). After the restart Rama runs again every source record since the topology last wrote its
  progress, the records that had succeeded and been answered too. One thing ran 37 times.
- With `:append-ack` or `:ack`, a record that a failed attempt appended stays, and the retry adds
  another. With no ack level an append was sometimes lost and sometimes not.
- A row and a record are never seen together: with no commit boundary before the append the record
  is readable first, with one the row is.
- A step that lets go of the thread between an event's read and its write, an append that waits or
  a commit boundary, lets other events of the batch read the row first: of 2,000 events that each
  add one to a row, 3 to 21 distinct numbers came out. Write, then append lost none.
- An event that waits holds its task's stream topology. An append that waits costs about 1 ms
  inside the event, and a boundary behind a write about 1 ms more; the client's `:ack` call went
  from 2.8 to 3.3 to 3.6 ms at 100 a second.
- A client reads a partition of a depot by its offsets and a range, and is given the values alone,
  with no offset and no time: 10,000 small records in 19 to 38 ms.
- `:ack` to a depot that the appending topology itself consumes is a deadlock, as the skill warns.

27. **In the streaming gate, nothing that lets go of the thread stands between an act's reads and
    its writes — builder's.** So the keeping of an answer, and any commit boundary, comes after the
    gate's writes. Reason: the probe's fourth point above, and the gate's own rule that "a yield
    would let another act of the layer in between this act's reads and its writes" (the store's
    page). This closes one of the two orders that ruling 12 left open, for the streaming gate.
28. **No part of the store rests on an append with no ack level — builder's.** The probe saw one
    lost.
29. **A source record that runs again is the common case and not the rare one — builder's.** After
    any throw on a worker, each record since the last written progress runs again, on every task.
    The plan of `answers-in-a-log` says what the log of answers holds after such a replay, with the
    count for a replay of n records, and shows that the rebuild's answer does not rest on that
    count.
30. **What the probe did not reach is said in the receipt — builder's.** A real cluster, a killed
    process, a change of leader, a clean restart by a module update, when Rama writes a topology's
    progress, and retry modes other than `:all-after`.

### The shorter path, and the plan of `answers-in-a-log`

31. **The shorter path — builder's, told to Sid on 13 October and not struck.** It replaces ruling 21
    and the order of ruling 25.
    - `the-locks-go` is built straight from its plan. Its plan gets no check of its own: the part
      designs no state, its proofs say whether it holds, and the review of the whole reads it.
    - `answers-in-a-log` keeps one check of its plan, and is then built. It is the part where a
      fault stays hidden until the day a rebuild is needed.
    - `the-rebuild` gets no plan and no check of its own. It is built with its proof, which compares
      what it made with what the gates wrote, row by row, so a wrong rebuild fails its own proof.
      What its builder stands on: section E of the plan of `answers-in-a-log`, which says for each
      part of each row which log gives it; the scope of `the-rebuild` in the split; rulings 9, 10,
      13, 14, 18 and 22.
    - One review of the whole follows, then the measures and the receipt.
    What is given up, and what guards it: a part of a row that the logs cannot give would be found
    when the rebuild is built, and the gates' code would then change after it was built. So the one
    check is told to hold section E against every part of every row of section 4 of the implied
    spec, as its first job.
32. **The six points of the plan of `answers-in-a-log` — builder's; each is its recommendation.**
    (1) The kept clock of a batch is made again from the gate's own log of answers, whose first
    record of a batch holds the clock that the batch read back; the depot of batch clocks is the
    check. This meets ruling 9's third point by a third road, and every case is then made again.
    (2) An event of the streaming gate that finds its act-id answered appends a copy of the entry.
    The gate cannot tell a maker's second send from Rama's second run of a record whose append was
    lost, and that second run is what leaves no answer without an entry. (3) "The most acts a second
    that one maker holds" is measured in both readings. (4) One depot of answers for each gate,
    declared by its gate. Their names, and the two new keys of the microbatch gate's table, cannot be
    changed by Rama once real data is kept; the receipt puts them before Sid as words to change now.
    (5) The test of a closing takes what the run before left to close, and its checks are written
    again. (6) Its facts for the rebuild go to the rebuild's builder.
    The plan's own runs, on a module of its own: a throw before the boundary leaves nothing; a throw
    between the commit and the append leaves a row with no entry until the record runs again; a throw
    after the append leaves two equal entries; a failed attempt of a batch leaves nothing in the log.
    The design in one line: both gates keep an answer only after its row is committed, so no entry of
    an attempt that did not count exists, and every entry under one act-id is the same.

33. **No check of a plan at all, and a part is built by several sessions at once — builder's, on
    Sid's word of 13 October.** His lines: "so start doing that and fuck the testing for every
    fucking word that is written focus on throughtput think about the process of working itself not
    blindly following instructiosn". The build session had started the one check that ruling 31
    kept, beside the build. He quoted its first line back, "adversarial, default-fail mindset ...
    PLAN_VALIDATION", with "These are the fucking things I am saying". It was stopped after a few
    minutes and wrote nothing. So ruling 31 stands without its second point: `answers-in-a-log` is
    built straight from its plan. What then guards the logs: the plan's own three runs on Rama, the
    proofs of the part, and the rebuild's proof, which fails when a part of a row cannot be made
    from the logs.
    How a part is built from here. One plan, several sessions at once, each with files of its own
    and no file shared; each takes the new shape of another session's forms from the plan and not
    from the file on disk. None runs a proof while the others write. When all are done, the build
    session runs the proofs once and sends each failure to the session that owns the file. No paper
    of validation is written. For `the-locks-go` the six are: the source of the door's side and of
    the act; the source of the gates, the record, the module and the read path; the proofs of the
    pure code; the module's proof; the door's proof with the read path's proof and the measures;
    and the README pages.
34. **The nine points of the plan of `the-locks-go` — builder's; each is its recommendation.** A
    string with a lone surrogate arrives with `?` in its place, by Rama's writing of a record, and
    the two digests still agree; it is left as it falls and said in the receipt. A thing that throws
    when it is read is told by the door, with its word for a thing that it could not read. The text
    of a value is made once and carried. The bound on the size of an act counts the text that an
    accept writes into its facts' rows. A thing that a worker cannot read is said in the receipt,
    and was not run. An opener gives the two numbers of a wait under one name. `src-store/CLAUDE.md`
    names the seal in two lines and is edited by no session of this build; the receipt says so. A
    send with no end is stopped nowhere by the door, and the door's proof drops that case.
    The plan's run of ruling 19's condition, on Rama 1.6.0 in-process: for 138 things, with a value
    of each kind and with facts as a vector, a list and a lazy sequence, on depots of both
    partitioners' shapes, the door's digest was the digest that a topology took of the thing off the
    depot. So the door changes nothing of what a maker gives, and a list goes as a list.
    Its three numbers, each the builder's, kept, decided by nobody: one value is 65,536 bytes at
    most, with the word `:value-too-large`; a permission is 32 deep at most on both roads; an act
    writes 18,874,368 bytes of text at most into its facts' rows, with the word `:act-too-large`.

## For the receipt

What the contract's last section asks the receipt to hold, gathered as the build goes.

- The proofs before: the six count lines above.
- The contract's "about twenty places" that take the depots of acts to be two are 36 in 17 files
  (`IMPLICIT_SPEC.md`, section 2).
- The jobs of the locks and the seal are 22 where the contract names three (`IMPLICIT_SPEC.md`,
  3.3). The plan of the part that takes the locks out says where each goes.
- For Sid, from rulings 1, 3, 4, 5, 6, 8, 9 and 20: what the door still drops; that a value of any
  size reaches the log through the door; that the new code reads no log of the sealed form; that no
  value can be bytes; a maker's own second send; a stray's entry in the log of answers; each
  exception to "every table"; and which stops of the door went with the locks.
- A place where two papers differ: the method page, since 12 October, says that a builder's "is
  Sid's" line goes to the queue the same day; the contract says that what the build finds is
  "proposed as rows for the queue and decided by nobody" in the receipt. The build follows the
  contract and writes nothing into the queue.
- Departures from the skill, each with its ruling: the first two plans side by side (25); the
  shorter path of 13 October (31, 33): no check of any plan, no plan of the third part, no paper of
  validation, and each part built by several sessions at once.
- The numbers that this build sets, each the builder's and decided by nobody (ruling 34): 65,536
  bytes for one value; 32 for the depth of a permission; 18,874,368 bytes of text that one act
  writes into its facts' rows.
- `src-store/CLAUDE.md` still names the seal, in its lines on words and on the rig's findings. No
  session of this build edits that file.
- A string with a lone surrogate reaches the log with `?` in its place (ruling 34).
- Names that Rama cannot change once real data is kept, for Sid to change now or keep (ruling 32):
  the depots `*streaming-answer-depot` and `*microbatch-answer-depot`, and the keys `:to-keep` and
  `:to-close` of the microbatch gate's table.
- No log gives two things (ruling 32; the plan of `answers-in-a-log`, D and E): `:turns`, the
  working state of one run of the gate, which nothing reads once its batch is committed; and the
  task on which a row of the base is, which rests on the number of tasks and on Rama's hash.
- A store that is stopped between a row's commit and its entry, and never runs again, holds an
  answer with no entry: about a millisecond in a streaming layer, one run of the gate in the base.
- From the stream probe, for Sid as facts of Rama on this machine: a throw in a gate's event starts
  the whole worker again, and every record since the last written progress runs again (ruling 29).
- Other sessions wrote in this tree while the build ran. See "Other sessions in this tree", above
  the rulings.
