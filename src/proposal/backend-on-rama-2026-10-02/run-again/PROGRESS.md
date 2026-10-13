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
| A | The clock before the depot, the number per layer, the streaming gate's depot of answers and the rebuild out | still fails, on the base | its source is being edited |
| B | The base's gate, the batch clock and the microbatch gate's depot of answers out, the door's reads, the read path. **This part makes the first proof pass.** The fixture is made here | passes | its Rama claims are being run |
| C | Decision 7 on the store itself: closed, or said on the store's page and named by the proof | passes | a run on a stand-in gate is under way |
| D | This folder's pages, the queue, the open rows of both receipts, the progress page of the folder above | — | not begun |
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

More are added below as they are made.

## Where the papers are

- `probes-the-bases-gate/` — the Rama claims of the base's gate, run on a stand-in.
- `probes-order-after-a-failure/` — decision 7, run on a stand-in gate.
- `smoke/` — small runs of a part on the store itself, before its proofs are adapted.
- `runs/` — the cut output of each proof run. A file named `raw-...` is a whole output:
  never open one, and none is committed.
