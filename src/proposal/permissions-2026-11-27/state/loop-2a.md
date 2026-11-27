status:     DONE (Part 1: the gate's rules and the read asked once). Part 2, the live read, waits for Loop 0d
commit:     138fd5a1 and the one after it, which corrects two counts
verified:   clojure -M:run 2000 <seed> in gate-histories/, seeds 1 to 5, against untouched src-store: exit 0 each. The rules keep the statement to prove; on today's default values the rules and today's store differ only inside 7 reasons of ALLOWLIST.md (70,875 things compared under seed 1, 3,076 differ); 54 of 54 broken copies of the rules caught; 12 of 12 broken stores caught. 20,000 histories under seed 21: the same
next:       Loop 2B builder: write `:send` and `:sees` of a gate under test over the new kernel (run.clj, `rules-gate` shows the shape) and run `clojure -M:run rules` and `mutants` beside it. Sid: the two cases under "Not shown to Sid", and the nine readings in gate-histories/README.md

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
