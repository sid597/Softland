status:     DONE (part 1, the gate's rules and the read asked once; part 2, the rules brought to item 26 and to the review of the kernel, and the read that stays open). Two questions for Sid from the live read, below. Not pushed: see "The push"
commit:     the commit that holds this text; the one after it writes its hash here
verified:   in gate-histories/, clojure -M:run 2000 1 in the working tree at 7f75eee5 (the store of Loop 3): six runs green, exit 0. The rules keep the statement to prove (125,415 things); 67 of 67 broken copies of the rules caught; against the store of Loop 3, 124,871 things, agree, told, once, apart, proved and see hold, none under reading 4, 5 or 6, and 32 of 32 of that store's broken copies caught; the live read keeps answered, not more, silent, asks and settles in every order of seven small histories (131,875 worlds) and in 2,000 sampled histories (256,210 steps of a read); 19 of 19 broken copies of the live read caught. Against an export of the store at 25cafbfe: today 2000 1 and 20000 21 green, 12 of 12. Seeds 2 and 3 of mutants, live and live-mutants: the same. Nothing ran on Rama
next:       Sid: the two questions under "Two questions for Sid", and the merge of main with origin/main. Loop 3 or its next: `client-step` of live_read.clj written over the store's pure read path, so that the store's live read is a read under test; and the one broken copy of fix.clj over `holder-plan`

# Loop 2A, part 2: the rules brought up to date, and the read that stays open

Claude (Fable) on Sid's machine, 28 November 2026, from the prompt that Sid
pasted. The record holds no line of his with that prompt's words, so what it
asks is taken as the prompt's, not as his word: the one place where that
matters is the push, below.

Read: `PERMISSIONS.md` section 6, items 18 to 26, and section 12; `BUILD.md`
whole; `CONTRACT.md` sections 2.3 and 2.4; `state/loop-0d.md`,
`state/loop-2b.md`; the first 100 lines of `ALLOWLIST.md`; `gate-histories/`
whole but `walk.clj`, `record.clj` and `piece_2/`. For invariants 9 to 13,
the write-up of Loop 0d as the record has it in a paste of Sid's
(‹b2ac587b›): 9 to 12 whole; 13 stops after "The sentence …", and the
judge took it to be BUILD batch 4, line 4. Of `src-store/`: the names of
the functions of `read_path/lanes.clj` and its bound of 32, after the
store of Loop 3 was committed while this ran. Not read: `PLAN.md`,
`HANDOFF-implementer.md`, anything under `test-store/`.

Touched: `gate-histories/` and this file. Nothing else.

## What was built

In `src/proposal/runtime-2026-11-21/gate-histories/`, its page the README
there, which has each of these in full:

- `reference.clj`. Item 26: a give to everyone, or a raise of one, needs
  `:everyone` in the right to give (`may-give-to?`); whoever gave a give
  ends or lowers it only while the give she gave it under still lets her
  pass on what it holds (`ends-as-giver?`). The three readings on which the
  kernel and the judge differed, turned to what the review found: nobody
  ends a give as one further up past an ended give; a raise is held to all
  that a give of what it adds is held to; anyone at all is held to seeing
  at a change too. 42 sentences.
- `statement.clj`. A change is held to what a give is: its author held what
  is added over all that the give covers.
- `histories.clj`. The owner's preset names everyone; a publisher's preset;
  four steps more: who opens a lane and who passes that right on, a giver
  lowered in place who takes back what she gave, an end past an ended give,
  a raise that no give could make.
- `run.clj`. 13 broken copies of the rules more, 67 in all; the runs `live`
  and `live-mutants`.
- `live_read.clj`, new. The lane over time as the tables of CONTRACT 2.4
  hold it; the one query with its read set; a read that holds an answer
  against its pushes; a world that brings pushes, callbacks and answers in
  any order; every order of a small history; sampled orders of generated
  histories.
- `today.clj`. One reason more, "B1 form": the store before the fix still
  lets one further up end a give past an ended one, since its form writes
  the gives above out.
- `fix.clj`, the builder's. Two things, so that it loads and holds against
  the store of Loop 3: one broken copy is played only while the function it
  replaced is there (`lanes/holders-for` is gone), and the word of the
  read's bound is held by a count.

## What was run

All on Sid's machine, in the working tree at `7f75eee5` with this loop's
changes.

| Run | What it gave |
|---|---|
| `clojure -M:run rules 2000 <seed>`, seeds 1 to 5 | Green. Seed 1: 125,415 things, 55,363 taken; every rule refused something; every act taken keeps the statement to prove |
| `clojure -M:run mutants 2000 <seed>`, seeds 1 to 3 | 67 of 67 broken copies of the rules caught; the latest at history 812 (seed 3) |
| `clojure -M:run fix 2000 1` | Green against the store of Loop 3: 124,871 things, each decided by the store's own walk; agree, told, once, apart, proved and see hold. Under reading 4, 5 and 6: none. After 293 decisions a read answered the word of its bound, each with more than 32 gives in the asker's runs |
| `clojure -M:run fix-mutants 2000 1` | 32 of 32 caught; one of the builder's 33 not played |
| `clojure -M:run live 2000 <seed>`, seeds 1 to 3 | Green. Every order of seven small histories, 131,875 worlds; 2,000 sampled histories, 256,210 steps of a read under seed 1. The two questions below counted, not red |
| `clojure -M:run live-mutants 2000 <seed>`, seeds 1 to 3 | 19 of 19 broken copies of the read and of its query caught, 17 in every order of a small history |
| `today 2000 1` and `today 20000 21` against an export of `25cafbfe` | Green. Seed 1: 70,880 compared, 2,962 inside seven reasons, none outside. Seed 21: 722,609 compared, none outside; 3 things under the new reason B1 form |
| `mutants 2000 1` against that export | 67 of 67 and 12 of 12 |
| `clojure -M:piece-2 2000 1` against that export | Holds, as before |

What the runs do not show. Nothing ran on Rama. No code of the store's
live read was judged: the read that is played is the judge's model of the
invariants, and its broken copies. A run of histories samples; only the
seven small histories of the live read are played in every order, and in those a
callback is handed over at once.

## Two questions for Sid

Both are in the README with the order of events that shows each. Neither
lets a reader keep a standing that she lost: in no order did the read tell
more than she had.

1. **A holder's run can say again what it said.** CONTRACT 2.4 takes a
   give's key out of its holder's run at an end. A run that was empty, got a
   give and lost it is empty again, and an answer from before agrees with
   it. So with pushes that say different versions, an old answer can agree
   with the state in between of a transfer in one act, and the read tells
   "may not see" to one who could see at every version since. It lasts until
   the push that is behind calls back, and it needs an answer older than the
   run's earlier state to be still held. With one number more in the seventh
   table, the stamp of the last decision that wrote under a holder's run, an
   end too, every order of the small histories and every sampled order show
   none. A reader who is in before a transfer in one act is never told that
   she may not see, in any order, with the table as it is. This session's position: add it. It changes a frozen shape.
2. **A fact can be told that was written after her give ended**, when the
   push of the lane's facts runs ahead of her rights pushes. P section 12's
   statement forbids it. Invariants 9 to 13 speak of the standing and not of
   which facts go with it. Not checked: whether Rama can bring the two
   callbacks in that order on one task, and whether today's read path has
   the same window.

## The judge's readings that are new

In the README, readings 10 to 12: "what the target permission holds" is
what it allows at that moment; `:holder` is one whom a give of her own lets
see, `:visitor` one whom only everyone's does; to give to everyone needs
the right whatever is passed. The store of Loop 3 agrees with the first and
the third on every thing of the run, and with the second by the names in
its code.

## The push

The prompt says to commit and to push. Committed, on `main`, these paths
alone. Not pushed. While this ran, `origin/main` took the 13 commits of pull
request 43 and local `main` took Loop 3's commit, so the two have parted:
a push needs a merge first, and `CLAUDE.md` puts pushing and merging with
Sid. Nothing of the 13 commits is in `gate-histories/` or in this folder.

# Loop 2A, part 1: the judge of the permission rules

Cloud Claude (Fable), 27 November 2026, from the prompt that Sid pasted.
Read: `PERMISSIONS.md` sections 6 and 12, `BUILD.md` whole, `CONTRACT.md`
sections 1 to 2.3, `ALLOWLIST.md` lines 1 to 95, and `gate-histories/`
whole. Of `src-store/`, read and not touched: the gate's end and give checks
(`gate/decision.clj`, lines 262 to 365) and the yes's digest
(`door/digest.clj`, lines 196 to 262), for the saying again in today's form.
Not read: `PLAN.md`, `HANDOFF-implementer.md`, anything under `test-store/`.
No file under `src-store/` or `test-store/` was touched.

## What was built

In `src/proposal/runtime-2026-11-21/gate-histories/`, its page the README
there:

- `reference.clj`: batches 1 to 4 as 37 sentences, each a function or an
  entry of a table with its test. `clojure -M:run sentences` prints them.
- `statement.clj`: the statement to prove, written a second time as a fold
  of its own over the acts that were taken.
- `histories.clj`: histories in the form of CONTRACT.md 2.1 to 2.3.
- `today.clj`: the store before the fix as a gate under test, each thing
  said again in today's form, each difference held to a reason of the
  allowlist by a test of the thing.
- `run.clj`: the four runs.
- The judge of piece 2 was moved to `piece_2/`, its namespaces renamed and
  nothing else changed; it still holds (`clojure -M:piece-2`). Its page is
  `README-piece-2.md`.

Clojure was not on the cloud machine. Its command-line tools were put in the
session's scratchpad, and everything above was run, not read.

## What was run

`clojure -M:run 2000 <seed>`, seeds 1 to 5: four runs, all green, exit 0 each;
60 seconds for seed 1, run alone.

| Run | Seed 1 |
|---|---|
| The rules as their own gate | 94,884 things, 31,551 taken; every rule refused something; every act taken keeps the statement to prove |
| Against the store before the fix, today's default values | 70,875 things compared; 3,076 differ, each inside a reason; none outside. B1 many 1,613, B3 change 884, B1 own end 311, B2 give 129, B4 public 111, B3 one act 17, B1 again 11 |
| The broken copies of the rules | 54 of 54 caught; the latest at history 567 |
| The store before the fix with one rule broken | 12 of 12 caught |

20,000 histories under seed 21: the rules, 955,245 things, hold; against the
store before the fix, 722,605 things compared, no difference outside the
reasons. `clojure -M:piece-2 2000 1`, the judge of piece 2 after its move:
holds.

What this does not show: no code of the fix was judged, since none is
written. Nothing ran on Rama. The live read was not touched.

## Not shown to Sid

Two cases follow from what he decided, and he was not shown either.

1. **Any holder whose right to give names see can open the lane to
   everyone.** Item 20 makes everyone a holder like any other; batch 2 lets
   whoever may give see give it to another. Today only the first holder
   opens a lane. If opening is to be an owner's act, that is a rule more.
   (Answered since, by item 26 of P section 6: giving to everyone needs
   `:everyone` in the right to give. Part 2 holds it.)
2. **One owner ends, or lowers, another owner's give while an owner
   remains.** BUILD batch 3 names it; items 18 and 21 read together.

## What the pages have wrong, or do not say

1. A change keeps who gave the give and what it was given under (P section
   12, point 2; BUILD, "so both stay readable after a change"). The
   implementer's record in P section 12 can be read the other way ("writes
   the row again with the changer's two"): that is the row's own fields, not
   the value's. A gate that puts the changer in the value takes the giver's
   right to end away from her.
2. BUILD batch 3, line 1, "changes may name what the same act makes": no
   line says what a give may name of a give that its own act makes. The
   judge sends no such thing.
3. BUILD batch 4, line 1, "written in the lane's order when the lane is
   made": no line says under what the give to everyone of a making act is.
   The judge's making act holds the depot and the first give alone.
4. Today's store: a holder who is given the same permission again after an
   end is above what hung beneath her ended one, and can end it. It is B1
   again in the allowlist's words, seen in the gate.
