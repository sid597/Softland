# The build "run again": where it stands

Read this page first when you pick the build up. It is written by the build session
(Opus 5.5, the night of 13 to 14 October 2026) as it goes, so it is true at the last
line of "The parts". It is not a ruling. Only Sid decides.

## What the build is for

Sid, 13 October 2026, the prompt of this build, word for word in its first lines:

> The store has to come out the same when it is run again from its depots. Today it does
> not, and it has five depots where I decided two.

His eight decisions, as he sent them:

1. The clock comes before the depot. The door writes the time on the act. No gate reads
   a clock.
2. There is local ordering: the order Rama gives inside one partition.
3. Every act is placed by its layer key, the base too. A layer is one line.
4. The base's rows: a copy on every task. One task decides, in the line's order.
5. The two depots that hold the answers go, and the rebuild with them. The depot of
   batch clocks goes. Two depots stay.
6. A change in what a gate accepts is a new version of the act. The old version's rules
   stay in the code. This starts with the first real record. Until then the fixture is
   made again only when a rule is changed on purpose, and the receipt says so. In this
   build it means the rule on the page and the fixture; the code still knows one
   version.
7. I am against the Rama setting that orders a layer's acts after a failure. Nobody has
   shown whether a failure in a fast layer can make acts that were in flight come out
   in another order when the store is run again. Run it and find out. If it can, look
   for a cheap way to close it. If there is none, say it on the store's page, and the
   proof names such acts.
8. A number counted per layer. The clock the door carries is the clock of the server
   that runs the door. What the door does with a thing it cannot label stays as built.

What he means by base, the same day: "the code that we are writing, and the basic things
the system provides to anyone who gets onboarded, like ingestors. Anyone can build on top
of it. Only I, or a few people or agents, change it. A change there affects everyone in
Softland. It is not partitioned by an entity. I said the base is microbatch because it
does not need to be instant. I never decided that its acts are spread over partitions."

Not decided by him, and decided by nobody in this build:

- what a second send of the same act carries as its time
- what happens to a record that arrives with no time
- what the time on the act is called
- where a time that is far ahead is stopped
- whether the layer that many people write into is a separate layer from the base

## How the work is sized

Not the Rama skill's seven stages on every piece. He asked for the process to be sized
to the work.

- **Removal, no plan.** The two depots of answers with `:to-keep` and `:to-close`; the
  rebuild, its reader, `Placing` and its two proofs; the depot of batch clocks, its
  topology, its table and the one throw.
- **Small change, no plan.** The door writes the time and the streaming gate takes it
  from the record; the number is counted per layer; the microbatch depot places an act
  by its layer key.
- **New design, one plan, its Rama claims run first.** The base's gate: one task
  decides in the line's order, and each row is copied to every task. The door's reads
  and the read path's reads of the base follow from it.
- **A run to find out.** Decision 7.

Builders work at once where their files do not overlap. The build session keeps the
plan, the rulings between builders and every commit. A code page (a docstring, a
folder's map) is written from the finished code inside its part.

## The parts

A part is committed only when every proof passes. The first proof is the one exception:
it is committed while it fails.

| Part | What | The run-again proof | State |
|---|---|---|---|
| 0 | The proof: a second module over the store's two depots from their beginning, every table compared | fails, committed failing | **done.** On the store as it was before the build 3,064 rows differ (`runs/first-proof-run-again.txt`) |
| A | The clock before the depot, the number per layer, the streaming gate's depot of answers and the rebuild out | still fails, on the base alone: the streaming gate's four tables hold 0 rows that differ | **done.** What it changed is in `PART-A.md`; its runs are `runs/after-a-*.txt` |
| B | The base's gate, the batch clock and the microbatch gate's depot of answers out, the door's reads, the read path. **This part makes the first proof pass.** The fixture is made here | passes: 0 rows differ on a run again, and the kept acts give the kept tables with 4 tasks and with 8 | **done.** The source is switched and every proof is adapted (`PART-B.md`); committed as `9a46f81a`, with all seven proofs passing |
| C | Decision 7 on the store itself: closed, or said on the store's page and named by the proof | passes | **run.** On a stand-in gate of the store's form no failure that could be made in process changed the order, in 87 of 87 runs (`probes-order-after-a-failure/RESULTS.md`). On the store itself three kinds of failure were made inside the run-again proof, a throw in the base, a stall past Rama's timeout and a throw in a streaming layer: after each, a second run gave the same tables, 0 rows differing. One case was reached in neither: a batch that fails with no restart and loses its rows, as a failed copy to a second machine would make it. No cheap way to close that case was found. It is said on the store's page, and the proof names an act that is decided out of its line |
| D | The pages: the store's page, the source entry's decisions of 13 October, the proofs' map, the queue, the open rows of both receipts, the progress page of the folder above, the questions for Rama's team | — | **done.** Written from the finished code, committed as `6dae07ce` |
| E | One fresh review of the whole, its fixes, the final runs, one receipt | passes at the last commit | **done, with the review cut short.** The machine went down at about 02:25 on 14 October, 14 minutes into the review. Two reviewers of six had reported. A second session (Opus 5.5) applied their findings that morning, lean: one session, no builders, no reviewers, as the prompt that Sid pasted said. That prompt was the night session's; Sid had asked why six reviewers, not for none. One reviewer then ran at his word (`RECEIPT.md`, section 7). It did what the section "What was left" below lists, in its order: the two fixes, the docstrings, the fixture made again and the final runs (`runs/final-*.txt`), in one commit; then the receipt, `RECEIPT.md`, in the next. What the review did not reach is in the receipt |

## Working answers, each kept for now and decided by nobody

Each is the smallest answer with which the build could go on. Each is one place in the
code, and its docstring says "Not decided: kept for now".

1. **The time on the act is called `:door-time`.** One def.
2. **A second send of one act carries the door's clock of the second send.** The door
   does nothing special for it. The digest leaves the time out, so both sends read the
   one answer as their own; a gate decides the first of the two to arrive.
3. **A record that arrives with no time is refused, with the word `:no-door-time`.**
   It has an act-id, so it can be answered; its stamp is the layer's last plus one.
4. **Nothing stops a time that is far ahead.** The form holds the time to 0 or more and
   below 2^45 milliseconds; a time outside gets the word of point 3. The bound was 2^46
   until the end of the build. At 2^45 a layer's stamps reach 2^62, which no act can
   carry, only after about 2^61 decisions in that one layer; at 2^46 one accepted act
   could get the stamp 2^62 itself (`max-time` in `door/declaration.clj` holds the
   arithmetic). And a thing that a gate refuses moves no time, whatever time it holds
   (decision 3 below).

## Decisions that the build session made, for Sid to review

1. **The word of an act that is sent to the wrong depot comes from the act alone.**
   Each gate read the other gate's table to choose between `:layer-not-made` and
   `:layer-belongs-to-the-other-gate`. That is an order across two depots, and no second
   run gives it again. Now a gate whose own record holds nothing for a layer that starts
   with the other gate's depot answers `:layer-belongs-to-the-other-gate`, whether or
   not the other gate has made the layer. Forced by his first line and his decision 2;
   the choice of that word over the other is the session's.
2. **The proof runs in a detached checkout of the head when it has to prove a tree that
   is not the working tree.** `/mnt/data/projects/Softland-run-again-baseline`, at
   `9c5da833`. It holds a checkout and nothing else.

3. **A refused thing moves no layer's time.** The stamp of a refusal, whatever its
   word, is its layer's last stamp plus one, or 0 in a layer with no stamp yet. Neither
   the time on the thing nor a stamp that it carries is in it. Engineering, the
   session's. It reverses the session's first answer, "a refused act keeps its time",
   which both reviewers who reported found to be a fault, each on their own: one
   refused thing with a time far ahead, from a sender with no permission, set its
   layer's stamps ahead for good, and the rule of 10 seconds then refused each honest
   act that carried a true stamp of that layer.
4. **Where the time stands in the order of the reasons.** A part that the form does not
   know, and a `:depot` that is no depot word, are told before a missing time; each of
   the maker's own parts after it. It follows from where the part stands in the shape.
5. **Two strays that differ only in the time are one stray.** It follows from the
   digest, which leaves the time out for every thing alike.
6. **The key of a layer's last stamp is the text of the layer id.** A layer id is a
   keyword or a UUID, and a table's key has one class.
7. **The pages of the store are written once, after part B.** `src-store/README.md`,
   the store's page and the proofs' map still describe the store as it was before the
   build. The docstrings and the folder maps beside the code are true at each commit.

More are added below as they are made.

## What was left, and is done

Done on 14 October 2026 by a second session, in the order of point 4 below. The
prompt that Sid pasted said: commit when the seven tests pass, then write the receipt. So the receipt,
`RECEIPT.md` beside this page, is the commit after the one that holds the rest. The
rest of this section is as the first session wrote it, and each of its points was
carried out as it says, with one correction of its arithmetic: in point 2 a carried
stamp within 10 seconds of a time below 2^45 is not below 2^61 but up to 655,359,999
above it. What follows from it there holds.

Written at the end of the first session, on the morning of 14 October 2026, so that a
new session can finish from this page alone. The way to finish, as the first session
proposed it that morning and put in the prompt that Sid pasted: lean. One session, no
fan of builders, no more reviewers, a short receipt. Sid had asked why six reviewers;
he did not ask for none. A "proof" is a test
file; the seven are run with `runs/run-proofs.sh <label>`.

The last commit is `6dae07ce`. All seven tests passed on `9a46f81a`, and the commit
after it changed pages only. Nothing is uncommitted but this page and two cuts of
measures.

### 1. One fault to fix: a refused thing moves its layer's time

Found by both reviewers who reported, each on their own. A thing that a gate REFUSES
still gets a stamp made of the time written on it, and that stamp becomes the layer's
last stamp. So one refused thing with a time far ahead, from a sender with no
permission, also into a layer that is not made, sets the layer's stamps ahead for good.
The rule of 10 seconds then refuses every honest act that carries a true stamp of that
layer. It was run on the store's module in both gates: a stranger's thing one hour
ahead was refused; the owner's next act was accepted an hour ahead; her act after that,
carrying that stamp, was refused `:carried-stamp-too-far-ahead`.

- The fix: in `gate/decision.clj`, `stamp-of`: for a refusal the stamp is the layer's
  last stamp plus one, or 0 with none. No time, and nothing of what the thing carries.
  Only an act that a gate accepts can then move a layer's time.
- It reverses decision 3 of the session's list on this page, "a refused act keeps its
  time", which was the session's and not Sid's.
- Tests that hold the old behaviour and change with it: the groups
  `the-time-on-the-act` and `the-time-on-the-act-in-the-base` of the module's test
  (the check that a thing refused for another fault keeps its time), and the cases of
  `stamp-of` for a refused thing in the pure test.

### 2. One bound to move: the time below 2^45, not 2^46

Run by a reviewer: with a time of 2^46 less one and a carried stamp at its own bound,
one accepted act gets the stamp 2^62 exactly, which no later act can carry. With the
time below 2^45 a stamp of the time is below 2^61, a carried stamp within 10 seconds
of it is too, and "last plus one" needs about 2^61 decisions to reach 2^62.

- `door/declaration.clj`, `max-time`, with that arithmetic in its docstring.
- Tests that name 2^46 change with it: the rows of times in the pure test, and check d
  of the module's time group.

### 3. Docstrings that say more than the code does

- `gate/stamp.clj`, namespace: "below 2^62" and "later than each stamp that the act
  carries" are not true of every stamp; say the two exceptions.
- `door/declaration.clj`, `max-time` and the store's page and working answer 4 on this
  page: "so that no stamp passes 2^62" is the claim that point 2 makes true.
- `door.clj`, `send-act`: "nothing holds one clock against the other" is untrue: the
  rule of 10 seconds does, through a layer's stamps. And "`:digest` is the digest of
  the very thing that was appended": it is of that thing without its time.
- `gate/stamp.clj`, `gate/decision.clj` `time-on`, `declaration/time-part`: each says
  the time is what the door wrote. The code knows only that the part holds a number in
  range. Say "which the door writes, or whatever else appended the thing".
- `door.clj`: the second send is said to be answered in one place. It is three: there,
  the door's test of its own parts, and the digest. Mark the digest's
  `without-the-time` "Not decided: kept for now" too.
- `gate/decision.clj`, `standing`: what is forced is that the word cannot follow the
  other gate's record; which word is given is the session's choice.
- `module/streaming_gate.clj`: still says a run "looks into" the order after a
  failure. The run exists: say what it found, and that a throw starts the worker again.
- `module/walk.clj`, `module/microbatch_gate.clj`, the store's page: "4 reads and 3
  writes" for an accepted act. By the Rama skill's own count the layer's last stamp is
  two levels, so 5. A stray is "no read": the set tracks its size, which Rama's docs
  say costs a read. The cost of the copy leaves out the bytes sent and that the base
  is stored once on each task.
- `module/microbatch_gate.clj` and the store's page: a thing of the base on a wrong
  partition would be accepted there and its copies would write over the rows on the
  base's own task. No client can put one there; say it.
- `module.clj`: "on a thing that is no map a keyword gives nil and does not throw" is
  inexact for a sorted map whose keys do not compare with a keyword.

### 4. Then, in this order

1. Make the fixture again: `clojure -M:store-module -m softland.store.run-again-proof
   make-fixture`, behind the lock. Points 1 and 2 are rules changed on purpose, and the
   receipt says so; the fixture folder's map gets the new day and reason.
2. Run the seven tests: `bash runs/run-proofs.sh final`.
3. Bring the few page lines that points 1 and 2 touch into line: the store's page on
   the time, working answer 4 and decision 3 on this page, the queue's row on a time at
   the end of its range.
4. Write the receipt, `run-again/RECEIPT.md`, short. Commit.

### 5. What the receipt must tell Sid, beside the usual

- **His to decide, not built.** The rule of 10 seconds is held against the time on the
  act, so it holds one door's clock against another's, and whoever may act in a layer
  can push it 10 seconds ahead. A reviewer's other rule: hold a carried stamp of the
  act's own layer against that layer's last stamp, which needs no clock. It sits inside
  his open question on where a far time is stopped.
- **A consequence of the working answer on a second send.** The digest leaves the time
  out, so a thing with no time and the same thing with a time have one digest. A thing
  appended without the door and refused for its missing time would be read by its
  maker's later send as her own answer.
- **For whoever builds versions (his decision 6).** The time is examined before the
  version. A rule that differs by version needs the version read first.
- **Cost, found and not built.** One row for each layer that holds its last stamp and
  its marks would give an accepted act 3 reads where the count is 5.
- **Was so before this build.** One very large thing holds a task's thread while it is
  digested, before any bound is held: 5.7 seconds for 256 MB on the reviewer's run.
- **The review was cut short.** Not reviewed: the read path, whether the tests notice
  a broken store, and the pages against the code. The reviewer who breaks the store
  left partial runs in `/mnt/data/projects/Softland-run-again-review-proofs/review-out/`.
- **What the session would do otherwise**, as said to Sid that morning: delete first
  where it separates and let the old tests judge it; change behaviour after, touching
  only the tests it must break; one reviewer, as the build's prompt asked; fewer new checks. And the
  root of the night's size: the tests and their map describe the store's insides, so a
  simpler inside cost a rewrite of both.
- **Scratch that is not committed** and waits for his word: four detached checkouts
  beside the repository (`Softland-run-again-baseline`, `-review-gates`,
  `-review-proofs`, `-review-pure`); `draft-the-bases-gate/`;
  `runs/fixture-second-making/`; `runs/falsify-check-4/` and the `falsify-` files; the
  `raw-` and `try-` files under `runs/`.

## Found by the build, for Sid to hear of

Each follows from one of his decisions or was met on the way. None is decided.

1. **A live read of the base is shared only by askers whose reads start from one
   layer.** With a copy on every task, an asker reads the base on the task of her own
   layer, under that layer's routing id. Before, every asker shared one live read of
   the base's own rows. Derived, not run: at Rama's default bound of 1,000 live reads
   on one table on one task, about 500 layers for each task can read the base live.
   The read path's own count is by routing id and does not see this bound.
2. **A second module with another number of tasks decides a layer on the task of its
   depot partition**, not on the task of the layer's hash (run on a stand-in, 8 tasks
   over a depot of 4). So the way to more tasks needs depots of its own, or one hop at
   the head of each gate. Not built.
3. **A maker cannot tell three states apart behind a failed append**: an act that was
   accepted, an act that was decided after a worker started again, and an act that
   never reached the depot (run on a stand-in). The door's word for it says so, and
   she reads the record by act-id.
4. **One thing with a time at the end of the range** is accepted, when its sender may
   act in the layer. With the range at 2^46 it left its layer 65,535 stamps before
   2^62, and none when it carried the greatest stamp of its millisecond. The range now
   ends at 2^45, and such a thing leaves its layer about 2^61 stamps. That an accepted
   act can still put its layer's time centuries ahead goes with "where a time that is
   far ahead is stopped".
5. **A reader on another task than the base's can see a new fact a moment later.** Read
   in Rama's docs; not seen in a run of this build.

## Where the papers are

- `probes-the-bases-gate/` — the Rama claims of the base's gate, run on a stand-in.
- `probes-order-after-a-failure/` — decision 7, run on a stand-in gate.
- `smoke/` — small runs of a part on the store itself, before its proofs are adapted.
- `runs/` — the cut output of each proof run. A file named `raw-...` is a whole output:
  never open one, and none is committed.
