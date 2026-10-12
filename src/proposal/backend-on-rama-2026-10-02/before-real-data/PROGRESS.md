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
| a run on Rama beside the first stage: what an append to a depot from inside an event of a stream topology does (`stream-probe/`), the contract's "Runs before the plan is fixed" | running | — |
| the implied spec, with the check of each "as built" line of the contract against the code | running | — |
| the split into parts | — | — |
| the plan of each part | — | — |
| the check of each plan | — | — |
| the build of each part | — | — |
| the review of the whole against the contract | — | — |
| the measures of the contract's section D | — | — |
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
collide on a port. And a proof's JVM loads the source when it starts, so no file under `src-store/`
or `test-store/` is edited while a sequence of proofs still has one to start.

Where the build session placed the contract's runs. The contract says "The plan stage makes these
... before it chooses a form for the log of answers". They are run by a fresh session of their own,
beside the first stage, so that no plan waits for them and no plan is written before them. The plan
stage reads the results and makes more runs where its own questions need them.

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
- Another session committed in this tree while the build ran, on the other items of entry 6: the
  pass over the decisions page, "freezes", three lines for the method, and Rama's docs for 1.6.0
  (`dc4bd12b` to `3617906f`). The section of 12 October changed in one quoted line and in no
  decision. `reference/rama/docs/README.md` now says what the copy is.
