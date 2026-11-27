status:     DONE for the gate's pure kernel and the read of an asker's rights asked once. NOT DONE: the read path's query, sources, answers and acts, which wait for Loop 0d and are Loop 3's with Ring 2
commit:     COMMIT-TO-FILL
verified:   permission-proof 61 of 61; in gate-histories/, clojure -M:run fix 2000 <seed> for seeds 1 to 3 green (about 94,000 things each, agree, told, once, apart, proved and see), fix-mutants 33 of 33, rules green, mutants 54 of 54; today and the broken stores green against an export of the store at 25cafbfe. Nothing ran on Rama
next:       Sid: the two questions under "Questions for Sid" (the judge's readings 4 and 6), and the choices under "The builder's choices" to turn. Then Loop 0d, and Loop 3 over softland.store.gate.walk

# Loop 2B: the pure kernel of the permission fix, held to the judge

Cloud Claude (Fable), 27 November 2026, from Sid's line. Started from
`claude/kind-planck-c6336y` at `6865376f`. Read: `PLAN.md`,
`HANDOFF-implementer.md`, `BUILD.md`, `CONTRACT.md`, `PERMISSIONS.md`
sections 5, 6 and 12, `ALLOWLIST.md` lines 1 to 100, the judge's folder
whole, and the store's pure code whole but for `read_path/answer.clj`,
`acts.clj`, `query.clj` and `door/weight.clj`, of which only the lines that
name a permission. Not read: the proofs under `test-store/` beyond their
harness, anything above the store, `read_path.clj`.

`reference.clj`, `statement.clj`, `histories.clj` and `today.clj` of the
judge are untouched. Clojure's command-line tools were installed on the
cloud machine (1.12.7.1681, the official installer); everything below was
run, not read.

## What was built

Under `src-store/softland/store/`, in place: one form, the old one refused.

- `act.clj`. `:who` is a party whose id says its kind: `:person.<id>`,
  `:agent.<id>`, `:run.<id>`, `:system`. `:permission` is an optional UUID.
  The written-out permission, its one rule, `allows` and `covers` are gone.
- `door/declaration.clj`. Five store keys. `:softland.store/permission`
  sits on a UUID and holds `:permission-value` as CONTRACT 2.3 has it; a
  part given as nil is left out of the one form, so two values that say the
  same have one text. `door/digest.clj`: a yes names its act as before; the
  check under "the least yes" needs no made-up permission any more.
- `record/rows.clj`. A row holds the id of the permission that its act
  named. An end is a row with no value that a fact wrote; a use is one that
  no fact wrote. The key of a permission in the order by holder.
- `gate/chain.clj`. The chain is found one row at a time by `:lent-from`,
  32 at most. A permission stands when its row and each row above hold a
  value: no stamp is compared. What it allows now is what it says, capped by
  each one above as that one says now. Who holds through everyone.
- `gate/decision.clj`. The rules of batches 1 to 4: an act is heard under a
  permission that stands and is its author's, or it makes its lane, or all
  it does is end its author's own; a give, a change, an end; the result as a
  whole (nothing left lent from what the act ends, the lane keeps an owner).
  Each fact is held against the rows as they were before the act. Each step
  says which rows it wants next, since a permission must be read to learn
  what it is lent from.
- `gate/walk.clj`, new. The walk of one thing as a pure function in steps:
  what it wants read, what it does with what the reads gave, and at the end
  the rows to write. The order of a decision is here and nowhere else.
  Whoever drives it only reads and writes. This is what Loop 3's module is
  to drive in place of today's `module/walk.clj`.
- `gate/first_act.clj`. The making act in the new form; the two ids of
  CONTRACT 2.1 as functions; no start value for who may see.
- `read_path/lanes.clj`. An asker's rights in a lane as one read in steps,
  of the same shape as the walk: the depot, her permissions by holder (her
  own, anyone's, anyone signed in's), and the rows above each lent one.
- `record/in_memory.clj`, new. The record as a map, for a proof and for the
  judge: how each read is answered and each row written.

Under `test-store/`: `permission_proof.clj`, new, the kernel's own proof.

In the judge's folder: `fix.clj`, new, the store as a gate under test, and
two runs more in `run.clj` (`fix`, `fix-mutants`). Its page has a last
section on both.

## What does not load now, and whose it is

Run with Clojure alone: `read_path/sources.clj` does not compile against
the new kernel, and `answer.clj`, `acts.clj` and `query.clj` need it. Of the
store's proofs that need no cluster, `act-proof` runs and 131 of its checks
fail and one throws; `pure-proof` and `read-path-pure-proof` do not compile.

Traced by a search for the names that are gone, not run, since Rama was not
fetched on this machine: `module/walk.clj` calls five of them and
`read_path.clj` twenty-seven, so the module and the read path's client do
not load; `door.clj`, `module/door_reads.clj` and the two gates' files name
none. `record/tables.clj` is unchanged: it has no table of the permissions
by holder and no owners of a lane yet. The four proofs on Rama were not
tried.

That is PLAN's cut by rings: Ring 2 and the live read are Loop 3's, the
proofs Loop 4's.

PLAN lists the read path's pure files under this loop "incorporating Loop
0d's live-read invariants". Loop 0d has not run, and how a read holds its
rights while it stays open is what it examines. So of the read path only
the rights asked once were built, and the sources, the answers and the
acts were left as they are. Nothing remembers an answer anywhere in what
was built.

## What was run

All on the cloud machine, on the code as committed.

| Run | What it gave |
|---|---|
| `clojure -M:store -m softland.store.permission-proof` | 61 of 61 checks pass, one or more for each "must be true" line of BUILD's batches 1 to 4 that the gate or a read asked once can hold |
| `clojure -M:run fix 2000 1`, `2`, `3` in the judge's folder | Green, exit 0, 78 to 81 seconds each. 94,218, 93,409 and 95,078 things, each decided by the store's own walk and judged by the rules; after each decision that wrote, a read asked once for each of 37 askers. agree, told, once, apart, proved and see hold. Taken out as questions: 1 thing under reading 4 in each run; 56, 52 and 45 under reading 6. Not sent for their form: 652, 656 and 664 |
| `clojure -M:run fix-mutants 2000 1` | 33 of 33 broken copies of the kernel caught, the latest at history 22: 27 by agree, 2 by apart, 4 by see |
| `clojure -M:run rules 2000 1` | Green: 94,884 things, as the judge's page has it |
| `clojure -M:run mutants 2000 1` | 54 of 54 broken copies of the rules caught. `NOT RUN` for the store before the fix, as the judge's page says of a store in the new form |
| `today 2000 1` and `mutants 2000 1` against an export of the store at `25cafbfe` | Green, with the judge's own numbers: 70,875 compared, 3,076 inside the seven reasons, none outside; 54 of 54 and 12 of 12 |

What the runs do not show. Nothing ran on Rama. No live read. The judge
sends one lane on the streaming gate: the base and the microbatch gate are
held by three checks of the proof alone. What the judge does not send is
held by the proof alone, or by nothing: the builder's choices below say
which.

One broken copy was taken out of the list of 34 after 2,000 histories did
not catch it: the store without its word `:no-permission` takes nothing
that the store with it refuses, because each fact of such an act is refused
again by its own check. The word decides only what is recorded and how
much of the thing the gate keeps.

PLAN's exit gate says "under 20 seconds". The judge's own `rules` run takes
15; the run against the store takes 80, most of it in reading what a
permission says from its row's text.

## Questions for Sid: where the store and the judge differ on its readings

The run takes such a thing out of its history, counts it and prints the
count. Neither is fixed on either side.

1. **Reading 4: who can end a give that fell.** The judge: a give that fell
   with the one above it can still be ended by all four who may end it. The
   store: by its holder and by an owner, yes; by one further up the chain
   than the end, no. The row of an ended give holds no value (CONTRACT 2.3,
   schemas unchanged), so it does not say what that give was lent from, and
   the store cannot tell that the author's give is above it. One thing in
   about 94,000 walks into it. The fallen give is dead either way; what is
   lost is that this one party can tidy it away. To keep the reading whole
   an end has to keep what it ended, which changes the frozen contract. This
   session's position: leave the store as built and turn the reading.
2. **Reading 6: anyone at all holds seeing and nothing more.** The judge's
   rules test it where the give is made, and take a change by an owner that
   raises anyone's give to write, to the right to give, or to owning. The
   store refuses that change with the same word as the give. About 50 things
   in 94,000. This session's position: the store is right by item 20, and
   the rule needs its test at a change too.

On the other seven readings the store is built as the judge reads them, and
no difference showed.

## The builder's choices, each Sid's or a later session's to turn

What the judge does not send, because BUILD.md does not settle it:

- **A change of what a give covers, or of its sitting: refused, not built**
  (`:changing-what-a-permission-covers-not-built`). BUILD batch 3 says a
  change replaces "what it allows or covers". To build it, what is lent
  from a give has to be capped by what that give covers at each moment, and
  its key by holder moved. This narrows a "must be true" line of BUILD, so
  it is named here first.
- **A change that changes nothing: refused.** By the general rules it would
  need nothing of its author, write a row and name a new writer of it.
- **A change of a give that fell: taken** when the general rules pass. To
  refuse it costs up to 31 reads for each change.
- **A raise by one who may give what is added and not the whole: taken**,
  as the judge's reading 5 has it.
- **An act that ends a give and changes one lent from it: taken.** "Left
  lent from one the act ends" is held of the gives that an act makes.
- **A give under, or lent from, a give of its own act: refused, with one
  exception.** A making act may hold a third fact: seeing to anyone at all,
  given by the first holder under the first permission of the same act. So
  the base, and a lane born open, start open in one act, and "what a lane
  starts with is in the log". The judge sends no such making act.

Others:

- **The making act**: its author is the first holder, or the system for a
  person's lane, as today; the first holder is a person or the system; the
  base is the system's. No gate holds a making act to the ids of CONTRACT
  2.1: an id is its giver's, and the judge's histories use others.
- **The system is no person**: it is not covered by a give to everyone. It
  sees a lane under a permission that names it.
- **The words.** An act under a give never given: `:permission-never-given`;
  ended: `:permission-ended`; used: `:permission-used-up`. An end of each:
  `:no-permission-to-end`, `:permission-already-ended`,
  `:permission-already-used`. An ended or used place no longer says who held
  the permission, so an act under one gets that word whoever sent it.
- **Where a rule sits.** What a permission says against its own act (who
  gave it, under what, to whom) is the gate's, after the standing, as
  today's check (g) was; the declaration holds its shape alone. "No give
  changed twice in one act" is the declaration's `:place-named-twice`.

## What the code showed that the pages have wrong, or do not say

1. **CONTRACT 2.4's list of watches is short by one holder.** A signed-in
   person also sees under a give to `:anyone-signed-in` that allows see, so
   her rights read that holder's run too; and a give to everyone that is
   good for one sitting sits in that sitting's run. Loop 3 and Loop 0d need
   both.
2. **The judge's histories send acts whose author is everyone**
   (`lower-a-lender`, when the borrower's give is `:anyone-signed-in`'s),
   and its rules take them. CONTRACT 2.1 forbids it; the store refuses the
   form. Not sent to the store, and counted.
3. **The judge's `apart` counts any word of the gate.** A thing with a fault
   of form gets the declaration's word before any row is read, the same
   under a give never given and under one that ended. Only two facts at one
   permission's id walk into it; not sent, and counted.
4. **A raise of a lent give by one who is not above it.** The rules take it
   (reading 5), and so does the store. So a person who holds see can add see
   to another person's agent's give: the agent then sees, when its own
   lender does. It cannot pass the lender's rights, but the lender had lent
   less. If that is not wanted, a raise of a lent give needs its act to be
   under a give that it is lent from. Not built; Sid was not shown it.
5. **A lane whose one owner's give is anyone signed in's.** The owner guard
   counts any owner's give that stands on its own, whoever holds it, as the
   judge's does. Not walked.
6. **A give lent from a give for one exact act falls when that give is
   used**: the use leaves no value in its row. The judge's rules would let
   it stand. No history makes one, and such a give could take no act.
7. **The holder scan has no bound** in `lanes.clj`: CONTRACT 2.4 says
   "bounded" and gives no number for how many of a holder's permissions a
   read goes through.
8. **Cost, measured in the judge's run and not on Rama.** Reading what a
   permission says from its row's text is the cost that showed (about 34
   microseconds a row before the row was read once for each decision; a
   decision under a chain of 32 reads 32 rows). The walk reads each row
   once and keeps what it says for that one decision. Nothing is kept
   between two decisions.

## For Loop 3

- Drive `softland.store.gate.walk` (`begin`, `reads`, `resume`, `outcome`):
  one loop of reads on the task, then the writes in the order given. The
  reads are `[:answer k]`, `[:fact k]`, `[:owners lane-text]` (the size of
  the lane's set of owners) and `[:last-stamp lane-text]`. New kinds of
  write: `[:holder-permission key stamp-or-nil]`, `[:owner id true-or-nil]`,
  `[:stray word hex]`. `softland.store.record.in-memory` shows each as data.
- An accept under a permission for one exact act can write two rows at one
  place; the order given must be kept.
- `lanes/rights-begin`, `rights-reads`, `rights-resume`, `rights` are the
  pure core of the one query of CONTRACT 2.4, for a read asked once.
- `kept-bound`: a holder who may only see has standing now, with no special
  word.
