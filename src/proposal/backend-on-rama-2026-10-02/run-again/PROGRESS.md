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
| B | The base's gate, the batch clock and the microbatch gate's depot of answers out, the door's reads, the read path. **This part makes the first proof pass.** The fixture is made here | passes: 0 rows differ on a run again, and the kept acts give the kept tables with 4 tasks and with 8 | the source is switched and every proof is adapted (`PART-B.md`); the whole set of proofs is run once more before its commit |
| C | Decision 7 on the store itself: closed, or said on the store's page and named by the proof | passes | **run.** On a stand-in gate of the store's form no failure that could be made in process changed the order, in 87 of 87 runs (`probes-order-after-a-failure/RESULTS.md`). On the store itself three kinds of failure were made inside the run-again proof, a throw in the base, a stall past Rama's timeout and a throw in a streaming layer: after each, a second run gave the same tables, 0 rows differing. One case was reached in neither: a batch that fails with no restart and loses its rows, as a failed copy to a second machine would make it. No cheap way to close that case was found. It is said on the store's page, and the proof names an act that is decided out of its line |
| D | The pages: the store's page, the source entry's decisions of 13 October, the proofs' map, the queue, the open rows of both receipts, the progress page of the folder above, the questions for Rama's team | — | written from the finished code; the proofs' map is being finished |
| E | One fresh review of the whole, its fixes, the final runs, one receipt | passes | not begun |

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
   below 2^46 milliseconds, so that no stamp passes 2^62; a time outside gets the word
   of point 3.

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

3. **A refused act keeps its time.** A thing that the form refuses for a fault other
   than its time still carries a usable time, and the stamp of its refusal holds it. A
   thing with no usable time gets its layer's last stamp plus one, or 0 in a layer with
   no stamp yet. Engineering, the session's.
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
4. **One thing with a time at the end of the range** (2^46 less one) is accepted and
   leaves its layer 65,535 stamps before 2^62. It goes with "where a time that is far
   ahead is stopped".
5. **A reader on another task than the base's can see a new fact a moment later.** Read
   in Rama's docs; not seen in a run of this build.

## Where the papers are

- `probes-the-bases-gate/` — the Rama claims of the base's gate, run on a stand-in.
- `probes-order-after-a-failure/` — decision 7, run on a stand-in gate.
- `smoke/` — small runs of a part on the store itself, before its proofs are adapted.
- `runs/` — the cut output of each proof run. A file named `raw-...` is a whole output:
  never open one, and none is committed.
