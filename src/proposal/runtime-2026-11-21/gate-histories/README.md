# Generated histories against the rules of the permission fix

[Parent: the runtime's page](../PROPOSAL.md). The build that this judges:
[BUILD.md](../../permissions-2026-11-27/BUILD.md), batches 1 to 4. The judge
of the store's piece 2, which this folder held before:
[README-piece-2.md](README-piece-2.md).

A check of the permission fix by a session that does not build it. Part 1,
the gate's rules and the read asked once, was written on 27 November 2026,
before the store's kernel. Part 2, on 28 November 2026 from the prompt that
Sid pasted, brought the rules to what was decided and found after part 1,
and added the read that stays open. Read for part 2:
[PERMISSIONS.md](../../permissions-2026-11-27/PERMISSIONS.md) section 6,
items 18 to 26, and section 12; BUILD.md with the clarifications that Sid
confirmed at its end; [CONTRACT.md](../../permissions-2026-11-27/CONTRACT.md)
sections 2.3 and 2.4; `state/loop-0d.md` and `state/loop-2b.md` beside them;
the first 100 lines of `ALLOWLIST.md`; and this folder. `PLAN.md`,
`HANDOFF-implementer.md` and the proofs under `test-store/` were not read.
Of `src-store/`, only the names of `read_path/lanes.clj` and its bound, to
make `fix.clj` load again. Nothing outside this folder and
`state/loop-2a.md` was changed.

## What it does

It makes random histories of one lane in the form of the fix, and holds
each thing of a history to the rules. And it plays a read that stays open
against every order in which its pushes and its answers can come.

- **The rules** ([reference.clj](src/gate_histories/reference.clj)): the
  plainest code that says batches 1 to 4. Each rule of the gate is one entry
  of a table: a word, the sentence in plain words, where the sentence comes
  from, and the test. `clojure -M:run sentences` prints the 42 sentences.
  Where the batches say nothing the rule is today's, by batch 1's "everything
  else is as today".
- **The statement to prove** ([statement.clj](src/gate_histories/statement.clj)):
  P section 12's statement, written a second time as a fold of its own over
  the acts that were taken. It calls no function of the rules. Its
  exceptions are named in its docstring.
- **The histories** ([histories.clj](src/gate_histories/histories.clj)):
  gives that stand on their own and gives that are lent, to people, agents, a
  run and both everyones; the right to give in finer values than the
  presets, and the right to give to everyone; changes up and down; ends by
  each of the four who may end; a giver who is lowered in place and then
  takes back what she gave; an end past an ended give; a raise that no give
  could make; several changes in one act; a lane handed over in one act, and
  the ways that fail; chains of lent gives to the bound of 32 and past it;
  gives for one sitting and for one exact act; what a stranger does under
  everyone's give; the same thing again, and another thing, under an act-id
  that has an answer.
- **A gate under test** is a map of four functions
  ([run.clj](src/gate_histories/run.clj), `rules-gate`). Nothing else knows
  which gate it plays. There are five: the rules themselves, their broken
  copies, the store before the fix, that store with one rule broken, and
  the store of the fix ([fix.clj](src/gate_histories/fix.clj), the
  builder's).
- **The read that stays open**
  ([live_read.clj](src/gate_histories/live_read.clj)): the lane over time as
  the store's tables hold it, the one query of CONTRACT 2.4 with its read
  set, a read that holds an answer against its pushes, and a world that
  brings pushes, callbacks and answers in any order. Its own section is
  below.

## What every history is held to

| Word | What must hold |
|---|---|
| agree | The gate takes a thing exactly when the rules take it. |
| told | Its sender is told what the rules say she is told: "accepted", "refused", the first answer for the same thing sent again, "not mine" for another thing under an act-id that has an answer. |
| once | Under an act-id that has an answer nothing is decided and nothing is written. |
| apart | A give that was never given is answered apart from one that ended (bug 3): the gate gives no word to both, for an act under such a give and for an end of one. The gate's words are its own; only that they differ is held. |
| proved | Every act that the gate took keeps the statement to prove, and nobody is shown the lane whom the statement does not let see. |
| see | After each decision that wrote, a read asked once lets an asker see the lane exactly when the rules do: 37 askers, each person, agent and run, one who holds nothing, in no sitting and in each, and one who is not signed in. |

A thing on which the gate and the rules differ is taken out of its history
for both, so the rest is still judged. A thing whose answer BUILD.md does not
settle is not sent, and is counted by its word.

## The runs

```
clojure -M:run                       # all that load, 2000 histories each, a seed of its own
clojure -M:run 2000 1                # so many histories, this seed
clojure -M:run rules 20000 1         # 1
clojure -M:run today 20000 1         # 2
clojure -M:run mutants 2000 1        # 3 and 4; `show` after the seed prints each smallest history
clojure -M:run fix 2000 1            # 5, the builder's
clojure -M:run fix-mutants 2000 1    # 6, the builder's
clojure -M:run live 2000 1           # 7
clojure -M:run live-mutants 2000 1   # 8; `show` prints each order
clojure -M:run sentences             # the rules in plain words
clojure -M:piece-2 2000 1            # the judge of piece 2, as it was
```

It needs Clojure and test.check. It exits 1 when anything is red.

1. **The rules as their own gate.** Nothing can differ; what is held is
   `proved`: every act that the rules take keeps the statement to prove. It
   also prints how often the histories reached each part of the rules, and
   names any rule that refused nothing.
2. **Against the store before the fix, on today's default values**
   ([today.clj](src/gate_histories/today.clj)). Each thing is said again in
   today's form and sent through today's own door and gate (`walk.clj`); the
   lane is read through today's read path. Each difference must fall under a
   reason of the allowlist, held by a test of the thing and of what stood,
   not by the two answers alone. What today's form cannot say is not sent,
   and is counted.
3. **The broken copies of the rules**: 67. The rules with one entry of the
   table taken out, 28 of them, or one function replaced or one rule not
   asked of a raise, 39. Each must be caught by a history.
4. **The store before the fix with one rule broken**: 12, the list of the
   judge of piece 2.
5. **Against the store of the fix**, and 6, **that store with one rule
   broken**: the builder's, in the last section of this page.
7. **The live read**: the reference read in every order of seven small
   histories, and in sampled orders of generated histories.
8. **The broken copies of the live read**: 19, of the read and of its
   query. Each must break something that a read is held to.

Runs 2 and 4 call the store before the fix. Against a store in the new form
they do not load, and the run says `NOT RUN` for them. To run them, put an
export of the store before the fix on the path in its place:

```
git archive 25cafbfe src-store | tar -x -C /somewhere/outside
clojure -Sdeps '{:paths ["src" "/somewhere/outside/src-store"]}' -M:run today 20000 1
```

## What part 2 changed in the rules, 28 November 2026

Each is in `reference.clj` as said here, with broken copies that a history
catches.

1. **The right to give to everyone (P section 6, item 26, "Option B").**
   `:everyone` is a fifth thing that a right to give can name. A give to
   `:anyone` or `:anyone-signed-in`, and a raise of one, needs it in the
   right to give of the give that the act names, besides the right to pass
   on what is passed (`may-give-to?`, the rule
   `opens-to-everyone-without-the-right`). It is passed on like the other
   four: only by one whose right to give names the right to give and names
   it. Inviting a named person, and lending to an agent or a run, need
   none of it. The owner's preset names it; a member's does not; a
   publisher's does.
2. **Whoever gave a give ends or lowers it only while she could still give
   it (item 26, the clarification).** The give that she gave it under must
   stand and must still let her pass on to its holder what it holds at that
   moment (`ends-as-giver?`). A giver who was lowered in place takes back
   nothing that she could no longer give. So finding 4 of `state/loop-2b.md`
   is gone: an owner lowered to see no longer ends the owners' gives that
   she made.
3. **Reading 4, turned.** The row of an ended give holds no value
   (CONTRACT 2.3), so it does not say what that give was lent from. The
   holder of a give further up a chain ends a lower one only when no give
   between the two was ended (`ends-as-lender?`). Its holder and an owner
   still end a give that fell.
4. **Reading 5, turned.** A raise is held as a give of what it adds: to the
   holder that the give has, lent as it is lent, covering what it covers.
   So besides the right to pass on what is added it needs what a give needs:
   a lent give is raised only under the give that it is lent from; a give
   that stands on its own only under one that stands on its own; none under
   a give that covers less, a give good in every sitting under one good for
   one. The rules of a give that speak of those are now asked of a raise
   too (`:of #{:give :raise}`).
5. **Reading 6, turned.** Anyone at all is held to seeing at a change too.
6. **The statement to prove** holds a change to the same as a give: its
   author held what is added over all that the give covers. Before, the
   statement let a raise out of a sitting through, as the rules did.
7. **A reason more for the run against the store before the fix**, "B1
   form": that store writes the gives above a give out in the permission, so
   it still lets one further up end a give past an ended one. It came up in
   3 of 20,000 histories, and is held by a test of the thing
   (`today.clj`, `past-an-end?`).

## What the runs found, 28 November 2026

On Sid's machine, in the working tree at `7f75eee5` (the store of Loop 3)
with this folder's changes. `clojure -M:run 2000 1`: six runs green, exit 0,
five and a half minutes.

**The rules as their own gate**, seed 1: 125,415 things sent, 55,363 taken.
Every rule of the table refused something. Taken: 16,923 gives to a person
that stand on their own and 1,016 lent; 17,243 lent to an agent and 685 to a
run; 2,373 to everyone; chains of each length from 1 to 32; 7,566 changes;
ends by a holder (4,236), a giver (2,326), one further up a lent chain (637)
and an owner (654); 1,404 lanes handed over in one act. Seeds 2 to 5 hold as
well, about 125,000 things each.

**The broken copies of the rules**: 67 of 67 caught under seeds 1 to 3, the
latest at history 120 of 2,000 under seed 1 ("reading 6, as it was"), 380
under seed 2 and 812 under seed 3. Among
them the store as it was before each change of part 2: Option A; a giver who
was lowered still taking back; reading 4, 5 and 6 as they were.

**Against the store before the fix**, an export of `25cafbfe`, seed 1: 70,880
things compared; 2,962 differ, each inside a reason, none outside.

| Reason | Things | What differs |
|---|---|---|
| B1 many | 1,613 | Today a give to a holder whose permission stands, or for a sitting that has one, is refused. |
| B3 change | 881 | Today has no change in place. |
| B1 own end | 311 | Today a holder ends her own only under a permission that lets her act. |
| B2 give | 129 | Today a holder of see, or of a permission for one sitting or one act, gives nothing; by the rules she lends to an agent or a run. |
| B3 one act | 17 | Today an act that ends a permission and gives one beneath it is taken. |
| B1 again | 11 | Today the same permission is given again at its place after an end. |
| B4 public | 0 | Today only the first holder opens a lane. By the rules of 27 November any holder who might give see opened it, 111 things; with item 26 only one whose right to give names everyone does, and on today's values that is the first holder. After 2,893 decisions a read still differed: today an agent or a run sees an open lane as everyone. |
| B1 form | 0 | 3 things in 20,000 histories under seed 21: one further up ends a give past an ended one. |

20,000 histories under seed 21, 722,609 things compared: no difference
outside the reasons. The store before the fix with one rule broken: 12 of
12 caught.

**Against the store of the fix** as Loop 3 left it (`7f75eee5`), seed 1:
124,871 things, each decided by the store's own walk and judged by the
rules. agree, told, once, apart, proved and see hold. Not one thing differs
under reading 4, 5 or 6, on which the store and the rules of 27 November
differed in 67 to 88 things of a run: the rules as turned and the store are
one. After 293 decisions a read asked once answered the word of its bound,
`:too-many-permissions`, for an asker whose runs hold more than the store's
32 gives; each is held by that count (`fix.clj`, `see-question`). That store
with one rule broken: 32 of 32 caught. One broken copy of the builder's
list is not played, because the function it replaced is gone from the
store (`lanes/holders-for`): the builder's to write again.

**The live read**, seed 1: below.

**The judge of piece 2**, `clojure -M:piece-2 2000 1` against the export:
holds as before.

## The read that stays open

BUILD batch 4, line 4: "A transfer in one act never shows a live viewer a
state in between." CONTRACT 2.4: a read's standing is that of the query's
snapshot only while its open pushes match the snapshot's read set entry for
entry; otherwise `:not-known-yet`. The examination of caching
(`state/loop-0d.md`) gave the invariants of the pure model. Its write-up is
on this machine only as a paste of Sid's in the record (‹b2ac587b›,
27 November), which has 9 to 12 whole and stops after the first three words
of 13, "The sentence …". The judge took 13 to be BUILD's sentence above,
held in every order; that reading is a session's.

`live_read.clj` has four parts, each the judge's own writing.

- **The lane over time.** The acts that the rules took, written one after
  the other. After each, a version: the row of each give, with the stamp of
  the decision that wrote it last and what it says, or no value once it
  ended; for each holder the run of her gives over the lane's default group,
  key to stamp, the stamp moved by a grant and by a change in place, the key
  gone at an end; and the lane's plain facts.
- **The query.** `lane-rights` reads one version in one turn and gives the
  standing and every row and run that it read, each with what it saw. For
  `:see`: everyone's runs first, then her own, and it stops at the first
  give that lets her see. For `:acts`: `:holder` when a give of her own lets
  her see, `:visitor` when only everyone's does, `:outside`. At the bound of
  a scan, a word. A run holds this query to the rules at every version: the
  same word, or the bound's.
- **The read.** It holds one answer, that of its last ask, and pushes on
  that answer's read set. It tells the answer's level only while every
  source of the read set is given and says what the answer saw. It works no
  level out of pushed rows. While it does not know, it tells nothing and
  owes the whole answer. It asks again when its answer was held against all
  its pushes, does not agree, and a rights push said something new since
  the last ask; else it waits. One ask in flight. A failed ask is asked
  again, three in a row at most; then the read ends with a word.
- **The world.** It writes versions, gives and advances each push on its
  own, hands each callback over later, runs a query at any version between
  the ask and the answer, lets it fail, and lets the answer of a failed ask
  come after all.

### What every step of a read is held to

| Word | What must hold |
|---|---|
| answered | It tells a level only when an answer that was handed to that very read, for its own asker, sitting and question, says that level and agrees with what its pushes say now, entry for entry (invariants 9 and 12). |
| was | The level that it tells was the lane's level, by the rules, at a version no older than what all its pushes have shown: no state in between two versions, and none that its pushes left behind. |
| not more | It never tells more than she had at any version that its pushes cover. |
| silent | While it does not know, it tells nothing; what it tells next is the whole answer, never a change (invariant 10). |
| asks | One ask in flight at most; and an ask is sent only when the read was opened, when its last ask failed, or when a rights push of it was given or changed since its last ask (invariant 11). |
| settles | When the world stops writing and delivers everything, the read comes to the lane's level at the last version, unless it ended for a cause that the world gave it. |

### Every order, and sampled orders

Seven small histories: the lane open to everyone, then one act that closes
it and gives Ben see; one act that moves what Asha sees under from her
first give to a new one, read by Asha, who is in before it; a lane handed
over in one act; a lender lowered and
raised in place under an agent's borrow; an end, and a fact written after
it; a run that says again what it said; more gives than a scan reads. For
each read of each, **every** order is played: every interleaving of writes,
of each push given and brought to each version at which it says something
else, of each query answered at each version that gives another answer, and
of one ask failing. 131,875 worlds under the runs as CONTRACT 2.4 has them,
152,560 with runs that keep a mark (the run prints each line's). The last
history is held for its query alone. In those orders a callback is handed
over at once; the delay of a callback is in the sampled orders.

Sampled, seed 1: 2,000 histories of up to 40 steps, one to four reads each.
29,818 versions written, 5,032 reads opened, 256,210 steps of a read. 10,930
answers, 4,108 of them of an older version than the store was at when they
came; 2,380 asks failed, and 2,234 of their answers came after all; 2,210
answers came after a later ask was sent. 8,258 callbacks, 1,448 of them
with more than one change at once. 4,361 changes in place, 77 of them of a
give that an open read rested on above its own. 2,081 acts that change more
than one give; 333 times such an act moved what an open read rested on and
left its level as it was. At 1,292 steps a read's pushes said different
versions and it knew; at 3,215 they did and it did not.

**What holds.** In every order of the small histories and in every sampled
order, under seeds 1 to 3, the reference read keeps answered, not more,
silent, asks and settles. It never told more than she had. For the transfer
in one act, read by one who is in before it: in none of its 1,376 worlds is
she told that she may not see, or anything but what she holds. The query and the rules give the same word at every
version.

**The broken copies**: 19 of 19 caught under seeds 1 to 3, 17 of them in
every order of a small history. Of the read: a remembered answer; a source
not given yet counts as agreeing (the gap between the query and the push);
an answer held to its rows and not its runs; a level worked out of pushed
rows; what a push said at its callback kept in place of what it says now;
telling while not knowing; a change told where the whole is owed; asking
with nothing new; never asking again; two asks in flight; an answer taken
by every read of the lane; a late answer taken in place of the held one; a
failed ask read as "may not see"; a failed ask not asked again; a read that
ended still telling a level. Of the query: a read set without the rows
above a lent give; without a holder's run; a query that reads runs in one
turn and rows in a later one; a level at the bound of a scan.

### Two questions that the model raises

Each is a kind of break that no read can mend from what the pages give it.
The run counts them and does not go red on them. Neither lets a reader keep
a standing that she lost: `not more` holds in every order.

**1. A run can say again what it said, and then an old answer agrees with a
state in between.** CONTRACT 2.4 takes a give's key out of its holder's run
at an end. So a run that was empty, got a give and lost it is empty again,
with nothing in it to tell the two apart. An answer from before then agrees
with it. In the small history of the transfer, read by Ben who opened his
read before the lane was opened:

```
a read is opened for Ben                              not known
ask 1 is answered: may-not-see (three empty runs)     not known
the three runs are given as of version 1              may not see
the store writes version 2: the lane is open
anyone's run comes to version 2                       not known; ask 2 is sent
the store writes version 3: one act closes the lane and gives Ben see
anyone's run comes to version 3: empty again          may not see
```

Ben's own run is still as of version 1. The pushes say (anyone's run at 3,
Ben's at 1), which is the state in between of the one act, and it is what
version 1 said. He is told "may not see" though he could see at versions 2
and 3. It lasts until his own run's push calls back. It needs an answer
older than the run's earlier state to be still held, so a read that was
opened while the lane was open never shows it. In sampled orders: 0 of 2,000
histories under seed 1, 3 under seed 2, 0 under seed 3.

With a run that also keeps the stamp of the last decision that wrote under
it, an end too, the same orders show none: 0 in every order of the seven
histories, 0 in 2,000 sampled under each seed (`*a-run-keeps-a-mark?*`). The
judge's position: the seventh table needs that one number more for each
holder's run, or a row that stays at an end with no value, as a fact's row
does. Without it, "equal to the read set" does not mean "nothing happened in
between" for a run, as the stamps make it mean for a row. It changes a
frozen shape, so it is Sid's.

**2. A fact can be told that was written after her give ended.** The push
of the lane's facts and the rights pushes are apart. In the small history
where Ben holds see, Asha writes, ends his give and writes again: the facts
push comes to the last version while Ben's rights pushes still say the
version before the end, his answer still agrees, and he is told the fact
that was written after. P section 12's statement says that nobody is shown
an act unless she holds a give that stands. 4, 4 and 2 of 2,000 sampled
histories under seeds 1 to 3, with a mark or without. Invariants 9 to 13 speak of the standing and not of
which facts go with it. Not checked: whether Rama can hand over the callback
of a later decision's fact before that of an earlier decision's rights row
on one task (the probe of Loop 0d measured two pushes across one act), and
whether today's read path has the same window. One way to close it in the
same manner: the snapshot says up to which stamp it read, and a fact with a
later stamp is told only after an answer of a later turn agrees. It costs a
query for each burst of writes for each open read. A question for Loop 3
and for Sid.

### A read under test

`client-step` takes a client, a read's id, an event and what that read's
pushes say now, and gives the client after and its effects: `[:join
source]`, `[:leave source]`, `[:ask n]`, `[:tell what]`. The store's own
pure read path is the next read under test: it needs that one function
written over it, as `fix.clj` did for the gate, and `answer-of` over its
`lane-rights`. Until then no code of the store's live read is judged here.

### What the model does not hold

Rama's pushes. A callback that is lost for good: the model delays a
callback, and a read that reads its pushes at each step is safe then, but
nothing in invariants 9 to 13 asks again for a change that came and went
unseen. A lane that is not made yet. Two lanes, and a routing id other than
the lane: an answer is kept apart by asker, sitting and question, which one
lane can show. The microbatch gate's copies. A read asked once.

## What the code showed that the pages have wrong, or do not say

From part 1, each still so:

1. **A change keeps who gave the give.** A change's value carries the
   give's own `:given-by` and `:under`, not the changer's.
2. **One owner ends, or lowers, another owner's give while an owner
   remains.** BUILD names it; the rules follow items 18 and 21.
3. **Today's form cannot tell a give from the one given at its place after
   an end** when both say the same.

Answered since: "any holder whose right to give names see can open the
lane" is item 26.

From part 2:

4. **The two questions of the live read**, above.
5. **"What the target permission holds"** in the clarification on the giver
   can be read as what it says or as what it allows at that moment. They
   differ only for a lent give whose lender was lowered. Reading 10 below.
6. **CONTRACT 2.4 names `:holder`, `:visitor` and `:outside` and no page
   says what each is of.** Reading 11 below. The store of Loop 3 reads them
   the same way, by the names of its `holder-plan`.
7. **Invariant 13 of the examination of caching is not on this machine
   whole**, nor are 14 to 19: `state/loop-0d.md` has three items for the
   nineteen.

## The judge's readings, each Sid's or the builder's to turn

Where BUILD.md can be read two ways and a history walks it, the judge had to
pick. A gate built on the other reading shows red here; that is a question,
not a break.

1. **Authority comes from the one give that an act names.** The giver ends a
   give in an act under the give she gave it under; a lender, under the give
   that it is lent from; an owner, under her owner's give.
2. **A give is lent from the give its act names, or stands on its own.**
3. **Its holder ends her own give in any act that is heard.**
4. **A give that fell with the one above it can still be ended** by its
   holder and by an owner; by one further up only with no ended give between
   (turned on 28 November, above).
5. **Raising needs what giving needs**: the right to pass on what is added,
   and all else that a give of it would need (turned on 28 November, above).
   Where giving the whole give after the raise would need more than giving
   what is added, the thing is not sent.
6. **Anyone at all holds see and nothing more**, at a give and at a change
   (turned on 28 November). `:anyone-signed-in` holds what it is given.
7. **An agent or a run is not anyone at all for reading either.**
8. **A person acts under everyone's give as one who holds it**: she writes
   under a give of write to anyone signed in, and lends everyone's give to an
   agent or a run. She is not its holder for ending what others lent from it.
9. **Nothing special under a give for one exact act.**
10. **"What the target permission holds" is what it allows at that
    moment.** For a give that stands on its own that is what it says. For a
    lent give under a lowered lender it is less; then a person who lent
    everyone's give to her agent can still end the borrow as its giver,
    which the other reading would refuse her. Where the two readings differ
    and she may end it by no other road, the thing is not sent. No history
    of the runs above walked into it.
11. **`:holder` is one whom a give of her own lets see; `:visitor` one whom
    only everyone's give lets see; `:outside` neither.**
12. **To give to everyone needs the right for the holder, whatever is
    passed**: a give to `:anyone-signed-in` of see alone needs it too.

## What is not sent, and what is not walked

Not sent, because BUILD.md does not settle the answer
(`reference/unsettled`): a change of what a give covers; a change that
changes nothing; a change of a give that fell; a raise by one who may give
what is added and not the whole; an act that ends a give and changes one
lent from it; a give under or lent from a give that the same act makes; an
end or a lowering by its giver of a give that says more than it holds
(reading 10).

Not walked: a making act that also opens its lane, or holds more than the
depot and the first give; who may make a lane for another holder; who owns
the base; `:system`; two lanes; the microbatch gate; a fact that names one to
replace; who may read a lane's acts whole; that the old form is refused for a
new act, which is the declaration's; a give lent from a give for one exact
act (`state/loop-2b.md`, finding 10).

## What waits

1. **The store's live read as a read under test**: `client-step` over the
   read path of Loop 3.
2. **The two questions of the live read**, for Sid.
3. **The builder's broken copy over `holder-plan`** in `fix.clj`.
4. **A cluster.** Nothing here was run on Rama.
5. **Every history.** A run of histories samples; only the seven small
   histories of the live read are played in every order.

## The files

| File | What it is |
|---|---|
| `src/gate_histories/reference.clj` | The rules, the read asked once, what is unsettled, the sentences. |
| `src/gate_histories/statement.clj` | The statement to prove, as a fold of its own. |
| `src/gate_histories/histories.clj` | The world, the steps, the things that a step sends. |
| `src/gate_histories/run.clj` | Playing a history against a gate, the statements held, the broken copies, the runs. |
| `src/gate_histories/live_read.clj` | The read that stays open: the lane over time, the query, the read, the world, every order of a small history. |
| `src/gate_histories/today.clj` | The store before the fix as a gate under test, and its excuses. |
| `src/gate_histories/fix.clj` | The builder's: the store of the fix as a gate under test, the readings on which it differed, and its broken copies. The judge's session made one entry of its list conditional and added the test of the bound's word, on 28 November. |
| `src/gate_histories/walk.clj`, `live.clj`, `record.clj` | As they were: the copy of today's walk, the live read through today's read path, Sid's record through the gate. `live.clj` is not the read of part 2. |
| `src/gate_histories/piece_2/` | The judge of piece 2, moved, its namespaces renamed and nothing else changed. |

The section below is the builder's, as written on 27 November against the
rules of that day. Since 28 November the rules are turned on readings 4, 5
and 6, so a `fix` run counts none under them; its numbers for the store of
Loop 3 are under "What the runs found" above.

## The store of the fix as a gate under test (the builder's, 27 November 2026)

Written by the session that built the store's pure kernel (Loop 2B), not by
the judge's session. `reference.clj`, `statement.clj`, `histories.clj` and
`today.clj` are as the judge left them. `run.clj` got two runs more and
nothing else of it changed.

```
clojure -M:run fix 2000 1           # the store of the fix, held to every statement
clojure -M:run fix-mutants 2000 1   # that store with one rule broken: each must be caught
```

[fix.clj](src/gate_histories/fix.clj) sends each thing through the store's
own door additions and its own walk of one thing
(`softland.store.gate.walk`), against a record held as a map
(`softland.store.record.in-memory`), and reads the lane through the store's
own rights (`softland.store.read-path.lanes`). The walk is not a copy: it is
the function that a gate's module is to drive against its tables. So what is
judged is the store's pure code. Nothing ran on Rama, and no live read.

With the store in the new form, `today` and the second half of `mutants` say
`NOT RUN`, as this page says above; they ran here against an export of the
store at `25cafbfe`, by the command above.

### What is not sent to the store, and why

Counted by its word in each run. Each is a thing whose form the store's
declaration refuses before any row is read, and the rules do not hold the
form of a thing.

- **An act that names one place in two facts**, under a plain key or a
  setting's. The rules take it.
- **An act that names one permission's id in two facts.** The rules refuse it
  too, as a give changed twice, by their last rule; the store's word for it
  comes first and is the same under a give that was never given and under one
  that ended, which `apart` would count against the store. So the rule "no
  give is changed twice in one act" refuses nothing in a `fix` run; the
  store's own proof holds it.
- **An act whose author is everyone.** A step that has the holder of a give
  write sends one when that holder is `:anyone-signed-in`
  (`histories.clj`, `lower-a-lender`). CONTRACT 2.1 makes everyone a holder
  that is never a `:who`; the rules take such an act as a party's.

### The readings on which the store differs: questions, not breaks

A thing on which the store and the rules differ under one of these is taken
out of its history for both, counted, and printed by the run. Each is held
by a test of the thing and of what stood (`fix.clj`, `question`).

- **Reading 4.** A give that fell with the one above it is ended by its
  holder and by an owner, as the rules say. One further up the chain than the
  end cannot end it: the row of the ended give holds no value (CONTRACT 2.3),
  so it no longer says what that give was lent from, and the store cannot
  tell that the author's give is above it. To keep the reading whole, an end
  has to keep what it ended.
- **Reading 5.** The rules hold a raise to what its author may pass on, of
  what is added, and to nothing else. The store holds it to what a give is
  held to as well: a right is added to a lent give only in an act under the
  give it is lent from; to a give that stands on its own only under a give
  that stands on its own; and to no give that covers more than the act's
  own. Without that, a holder of a give for one sitting, or of a lent give,
  raises another give of hers that is good in every sitting, or that stands
  on its own, and keeps what she added when the sitting is over or the
  lender's give ends. A reviewing session found it by running it against
  the store; by its reading the rules take it too.
- **Reading 6.** The rules hold anyone at all to seeing where the give is
  made, and take a change that raises anyone's give past seeing. The store
  holds it at the change too.

On the other six readings the store is built as the judge reads them.

### What the runs found

On a cloud machine. `clojure -M:run fix 2000 <seed>`, seeds 1 to 3: green,
exit 0, 79 to 86 seconds each.

| Seed | Things compared | Not sent for their form | Reading 4 | Reading 5 | Reading 6 |
|---|---|---|---|---|---|
| 1 | 94,214 | 653 (440 a permission named twice, 177 a place, 36 by everyone) | 1 | 32 | 55 |
| 2 | 93,405 | 656 (436, 182, 38) | 1 | 21 | 52 |
| 3 | 95,074 | 664 (500, 150, 14) | 1 | 21 | 45 |

Everywhere else agree, told, once, apart, proved and see hold: after each
decision that wrote, a read asked once for each of the 37 askers.

`clojure -M:run fix-mutants 2000 1`: 33 of 33 caught, the latest at history
22; 27 by `agree`, two by `apart` and four by `see`. One broken copy was
taken out of the list after a run of 2,000 histories did not catch it, and
`fix.clj` says why: the store without its word `:no-permission` takes
nothing that the store with it refuses, since each fact of an act that names
no permission is then refused by its own check.

The judge's own runs, after `run.clj` got its two runs more: `rules 2000 1`
green, 94,884 things, as above; `mutants 2000 1`, 54 of 54; and against the
export of the store at `25cafbfe`, `today 2000 1` green with the numbers of
the table above (70,875 compared, 3,076 inside the seven reasons, none
outside) and `mutants 2000 1`, 54 of 54 and 12 of 12.

What a `fix` run does not hold. A thing that is not sent, and a thing taken
out under a reading, is judged by nobody here: the store's own proof
(`test-store/softland/store/permission_proof.clj`) holds each of those
refusals, and nothing else does.
