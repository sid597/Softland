# Generated histories against the rules of the permission fix

[Parent: the runtime's page](../PROPOSAL.md). The build that this judges:
[BUILD.md](../../permissions-2026-11-27/BUILD.md), batches 1 to 4. The judge
of the store's piece 2, which this folder held before:
[README-piece-2.md](README-piece-2.md).

A check of the permission fix by a session that does not build it. Written
on 27 November 2026, before the store's kernel is written, from
[PERMISSIONS.md](../../permissions-2026-11-27/PERMISSIONS.md) sections 6 and
12, BUILD.md with the clarifications that Sid confirmed at its end,
[CONTRACT.md](../../permissions-2026-11-27/CONTRACT.md) sections 1 to 2.3
for the shapes alone, and the first 95 lines of
[ALLOWLIST.md](../../permissions-2026-11-27/ALLOWLIST.md). `PLAN.md`,
`HANDOFF-implementer.md` and the proofs under `test-store/` were not read.
Nothing in `src-store/` or `test-store/` was changed.

This is part 1: the gate's rules and the read asked once. What a read tells
while it stays open, and that a transfer in one act shows no state in
between (BUILD batch 4, line 4), wait for the examination of caching;
`live.clj` is as it was.

## What it does

It makes random histories of one lane in the form of the fix, and holds
each thing of a history to the rules.

- **The rules** ([reference.clj](src/gate_histories/reference.clj)): the
  plainest code that says batches 1 to 4. Each rule of the gate is one entry
  of a table: a word, the sentence in plain words, where the sentence comes
  from, and the test. `clojure -M:run sentences` prints the 37 sentences.
  Where the batches say nothing the rule is today's, by batch 1's "everything
  else is as today".
- **The statement to prove** ([statement.clj](src/gate_histories/statement.clj)):
  P section 12's statement, written a second time as a fold of its own over
  the acts that were taken. It calls no function of the rules. Its
  exceptions are named in its docstring.
- **The histories** ([histories.clj](src/gate_histories/histories.clj)):
  gives that stand on their own and gives that are lent, to people, agents, a
  run and both everyones; the right to give in finer values than the two
  presets; changes up and down; ends by each of the four who may end; several
  changes in one act; a lane handed over in one act, and the ways that fail;
  chains of lent gives to the bound of 32 and past it; gives for one sitting
  and for one exact act; what a stranger does under everyone's give; the same
  thing again, and another thing, under an act-id that has an answer.
- **A gate under test** is a map of four functions
  ([run.clj](src/gate_histories/run.clj), `rules-gate`). Nothing else knows
  which gate it plays. There are four today, below. The store of the fix is
  the fifth: it needs `:send` and `:sees` written over the new door and
  kernel, and `walk.clj` brought to the new module's walk.

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

## The four runs

```
clojure -M:run                  # all four, 2000 histories each, a seed of its own
clojure -M:run 2000 1           # so many histories, this seed
clojure -M:run rules 20000 1    # 1
clojure -M:run today 20000 1    # 2
clojure -M:run mutants 2000 1   # 3 and 4; `show` after the seed prints each smallest history
clojure -M:run sentences        # the rules in plain words
clojure -M:piece-2 2000 1       # the judge of piece 2, as it was
```

It needs Clojure and test.check. It exits 1 when anything is red.

1. **The rules as their own gate.** Nothing can differ; what is held is
   `proved`: every act that the rules take keeps the statement to prove. It
   also prints how often the histories reached each part of the rules, and
   names any rule that refused nothing.
2. **Against the store before the fix, on today's default values**
   ([today.clj](src/gate_histories/today.clj)). Each thing is said again in
   today's form and sent through today's own door and gate (`walk.clj`); the
   lane is read through today's read path. The histories keep to today's
   values: every give lent from the give above it, and what a give allows one
   of today's two words. Each difference must fall under a reason of the
   allowlist, held by a test of the thing and of what stood, not by the two
   answers alone. What today's form cannot say is not sent, and is counted.
3. **The broken copies of the rules**: 54. The rules with one entry of the
   table taken out, 27 of them, or one function replaced, 27. Each must be
   caught by a history.
4. **The store before the fix with one rule broken**: 12, the list of the
   judge of piece 2. Each must show a difference outside the allowlist's
   reasons, through the saying again in today's form.

Runs 2 and 4 call the store before the fix. Once `src-store` takes the new
form they do not load, and the run says `NOT RUN` for them and stays green on
1 and 3. To run them then, put an export of the store before the fix on the
path in its place:

```
git archive <a commit before the fix> src-store | tar -x -C /somewhere/outside
clojure -Sdeps '{:paths ["src" "/somewhere/outside/src-store"]}' -M:run today 20000 1
```

## What the runs found, 27 November 2026

On a cloud machine, against `src-store` as committed at `25cafbfe` (the
working tree, untouched). `clojure -M:run 2000 <seed>` under seeds 1 to 5:
all four green each time, exit 0; 60 seconds for seed 1, run alone.

**The rules as their own gate**, seed 1: 94,884 things sent, 31,551 taken.
Every rule of the table refused something. Taken: 7,391 gives to a person
that stand on their own and 1,118 lent; 12,594 lent to an agent and 644 to a
run; 1,313 to everyone; chains of each length from 1 to 32; 1,087 changes;
ends by a holder (3,030), a giver (953), one further up a lent chain (40) and
an owner (99); 2,063 lanes handed over in one act. 20,000 histories under
seed 21, 955,245 things, hold as well.

**Against the store before the fix**, seed 1: 70,875 things compared. The
rules and today's store differ on 3,076, each inside a reason of the
allowlist, and nowhere else:

| Reason | Things | What differs |
|---|---|---|
| B1 many | 1,613 | Today a give to a holder whose permission stands, or for a sitting that has one, is refused. |
| B3 change | 884 | Today has no change in place. |
| B1 own end | 311 | Today a holder ends her own only under a permission that lets her act. |
| B2 give | 129 | Today a holder of see, or of a permission for one sitting or one act, gives nothing; by the rules she lends to an agent or a run. |
| B4 public | 111 | Today only the first holder opens a lane. After 2,893 decisions a read differed too: today an agent or a run sees an open lane as everyone. |
| B3 one act | 17 | Today an act that ends a permission and gives one beneath it is taken. |
| B1 again | 11 | Today the same permission is given again at its place after an end, and its holder is then above what hung beneath the ended one. |

20,000 histories under seed 21, 722,605 things compared: the same, no
difference outside the reasons. 7,983 things of seed 1 were not sent because today's form cannot
say them, by twelve words that the run prints; the first three are an act
that names no give (3,220), a give that today's two words cannot say
(2,015), and an act under everyone's give (1,053).

**The broken copies**: 54 of 54 caught under seeds 1 to 5; the latest at
history 567 of 2,000 ("a lent owner's give counts as the lane's owner").
Three things were found only by a statement other than `agree`: "never given
is answered as ended" by `apart`; the read's four by `see`; "another thing
under an answered act-id is decided" by `once`.

**The store before the fix with one rule broken**: 12 of 12 caught under
seeds 1 to 5.

**The judge of piece 2**, `clojure -M:piece-2 2000 1`: holds as before the
move.

## What the code showed that the pages have wrong, or do not say

Found while the rules were written. Each is in the rules as said here.

1. **A change keeps who gave the give.** P section 12, point 2: "never the
   holder, the lender or who gave it". CONTRACT 2.3 calls `:given-by` and
   `:under` "the granting act's". So a change's value carries the give's own
   `:given-by` and `:under`, not the changer's, and the one who gave a give
   can still end it after another changed it.
2. **Any holder whose right to give names see can open the lane.** Item 20
   makes everyone a holder like any other, and batch 2 lets whoever may give
   see give it to another. Today only the first holder opens a lane. Sid was
   not shown this case.
3. **One owner ends, or lowers, another owner's give while an owner
   remains.** BUILD names it; the rules follow items 18 and 21.
4. **Today's form cannot tell a give from the one given at its place after
   an end** when both say the same. So today a holder who is given a
   permission again can end what hung beneath her ended one. By the rules
   she cannot: it is another give. Nothing she gains by it: what hung
   beneath fell for good.

## The judge's readings, each Sid's or the builder's to turn

Where BUILD.md can be read two ways and a history walks it, the judge had to
pick. A gate built on the other reading shows red here; that is a question,
not a break.

1. **Authority comes from the one give that an act names.** The giver ends a
   give in an act under the give she gave it under; a lender, under the give
   that it is lent from; an owner, under her owner's give. Today's gate is so.
   The other reading: any give of hers will do, while the right one stands.
2. **A give is lent from the give its act names, or stands on its own.** No
   give is lent from a third give.
3. **Its holder ends her own give in any act that is heard**: under no give,
   or under any give of hers that stands, whatever that give covers.
4. **A give that fell with the one above it can still be ended** by those
   who may end it, as today.
5. **Raising needs what giving needs, of what is added.** Where giving the
   whole give after the raise would need more, the thing is not sent.
6. **Anyone at all holds see and nothing more.** A give of write to
   `:anyone` is refused; `:anyone-signed-in` holds what it is given.
7. **An agent or a run is not anyone at all for reading either.** One who is
   not signed in sees an open lane; an agent sees it only under a give lent
   to it.
8. **A person acts under everyone's give as one who holds it**: she writes
   under a give of write to anyone signed in, and lends everyone's give to an
   agent or a run. She is not its holder for ending what others lent from it.
9. **Nothing special under a give for one exact act.** The general rules
   decide what its one act may hold.

## What is not sent, and what is not walked

Not sent, because BUILD.md does not settle the answer
(`reference/unsettled`): a change of what a give covers; a change that
changes nothing; a change of a give that fell; a raise by one who may give
what is added and not the whole; an act that ends a give and changes one
lent from it; a give under or lent from a give that the same act makes.

Not walked: a making act that also opens its lane, or holds more than the
depot and the first give; who may make a lane for another holder; who owns
the base; `:system`; two lanes; the microbatch gate; a fact that names one to
replace; who may read a lane's acts whole; that the old form is refused for a
new act, which is the declaration's.

## What waits

1. **The store of the fix as a gate under test.** `:send` and `:sees` over
   the new door and kernel, and the copy of the walk brought to the new
   module. Until then no new code is judged: the rules are held to the
   statement to prove, to today's store on today's values, and to their own
   broken copies. (The builder's session wrote that gate over the pure
   kernel on 27 November: the last section of this page. The module on a
   cluster still waits.)
2. **Part 2: the live read.** What a read tells while it stays open, the
   non-flicker of a transfer, and the watches of CONTRACT 2.4, after the
   examination of caching.
3. **A cluster.** Nothing here was run on Rama.
4. **Every history.** A run samples.

## The files

| File | What it is |
|---|---|
| `src/gate_histories/reference.clj` | The rules, the read asked once, what is unsettled, the sentences. |
| `src/gate_histories/statement.clj` | The statement to prove, as a fold of its own. |
| `src/gate_histories/histories.clj` | The world, the steps, the things that a step sends. |
| `src/gate_histories/run.clj` | Playing a history against a gate, the statements held, the broken copies, the runs. |
| `src/gate_histories/today.clj` | The store before the fix as a gate under test, and its excuses. |
| `src/gate_histories/fix.clj` | The builder's: the store of the fix as a gate under test, the readings on which it differs, and its broken copies. |
| `src/gate_histories/walk.clj`, `live.clj`, `record.clj` | As they were: the copy of today's walk, the live read through today's read path, Sid's record through the gate. |
| `src/gate_histories/piece_2/` | The judge of piece 2, moved, its namespaces renamed and nothing else changed. |

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
- **Reading 6.** The rules hold anyone at all to seeing where the give is
  made, and take a change that raises anyone's give past seeing. The store
  holds it at the change too.

On the other seven readings the store is built as the judge reads them.

### What the runs found

On a cloud machine. `clojure -M:run fix 2000 <seed>`, seeds 1 to 3: green,
exit 0, 78 to 81 seconds each.

| Seed | Things compared | Not sent for their form | Reading 4 | Reading 6 |
|---|---|---|---|---|
| 1 | 94,218 | 652 (439 a permission named twice, 177 a place, 36 by everyone) | 1 | 56 |
| 2 | 93,409 | 656 (436, 182, 38) | 1 | 52 |
| 3 | 95,078 | 664 (500, 150, 14) | 1 | 45 |

Everywhere else agree, told, once, apart, proved and see hold: after each
decision that wrote, a read asked once for each of the 37 askers.

`clojure -M:run fix-mutants 2000 1`: 33 of 33 caught, the latest at history
22; 27 by `agree`, two by `apart` and four by `see`. One broken copy was taken out of the list after a run of 2,000 histories did
not catch it, and `fix.clj` says why: the store without its word
`:no-permission` takes nothing that the store with it refuses, since each
fact of an act that names no permission is then refused by its own check.

The judge's own runs, after `run.clj` got its two runs more: `rules 2000 1`
green, 94,884 things, as above; `mutants 2000 1`, 54 of 54; and against the
export of the store at `25cafbfe`, `today 2000 1` green with the numbers of
the table above (70,875 compared, 3,076 inside the seven reasons, none
outside) and `mutants 2000 1`, 54 of 54 and 12 of 12.
