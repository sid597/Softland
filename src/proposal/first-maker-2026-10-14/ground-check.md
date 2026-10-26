# Ground check

By the ground-runs agent (Opus 5.5, xhigh), the night of 14 to 15 October, at HEAD c90c7bcc. The task's phrases are a session's text that Sid pasted (`memtool who`: «3c021c3a»). `:n` means line n of `runs/ground-1-compare-output.txt`.

## 1. The old fixture through today's store

**Commands** (`runs/ground-1-run.sh`):
- The checkout `/mnt/data/projects/Softland-first-maker-check` is HEAD plus the three fixture files of `0d24b03e` (`runs/ground-1-run-stdout.txt:2-4`).
- Under the lock, it runs the proof, then `runs/ground-1-compare.clj`, which is check 2 on 4 tasks through the proof's own functions. That script prints every differing row of the old fixture's 475 (`:1-3`).

**Expected** by derivation (`test-store/softland/store/run_again_fixture/README.md:63-76`): 463 of 475 rows differ. Twelve keep their value: ten strays, and the last stamps of streaming `:base` and microbatch `:alice`. The act that carried a stamp an hour ahead is accepted, writing a fact in each gate and a facts-by-value row in streaming.

**Run:**
- Answers: all 307 differ (`:7`, `:249`): 297 in stamp and time, 6 in time only (refusals, stamp 0 or 1), 2 in stamp only (`:no-door-time`), 2 in `:refused` too (`0d24b03e:…/tables.edn:26-27, 119-120, 250-251, 278, 319`).
- Facts: 119 of 119 differ in stamp and time; two also in `:level-since`, which holds stamps (`:150`, `:419`).
- Facts by value: all 28 keys are on one side only. Each pairs with a store key that differs only in its stamp (`:213`, `:485`).
- Last stamps: 9 of 11 differ; the derivation's two are equal (`:240-247`, `:495`). Strays: 10 of 10 are equal (`:237`, `:492`). Totals: 463 differ and 12 are equal (`:498`).
- Accepted now: **two** acts, one per gate, each refused `:carried-stamp-too-far-ahead` in the fixture. They wrote exactly the predicted rows (`:496-497`, `:210`, `:236`, `:482`).

**The proof:** checks 2 and 3 failed, as expected. Each counted 494 rows apart, on 4 and on 8 tasks: 463 + 28 re-keyed + 3 new (my reasoning). Checks 1 and 4 and the naming rule passed: the six failures are all in checks 2 and 3. There was no OUT OF ITS LINE. Count `{:test 5, :pass 40, :fail 6, :error 0}`, exit status 1 (`runs/ground-1-summary.txt:8-25, 38, 47, 59, 63, 65, 214-215`). The proof prints no PASSED or FAILED lines (`summary:5`).

**Verdict:** agrees on every row class; the derivation's one act is two in the run, one per gate, which I read as the hand's one case played in each gate.

**Checkout:** removed. `git worktree list` shows the main tree and 52 other worktrees, none of them first-maker (`runs/ground-1-worktree-list-after.txt:1-4`). The 52 were already listed by my first `git worktree list` tonight, which I did not save.

## 2. The read-path check, twenty runs

`runs/ground-2-twenty.sh` ran the check 20 times, each run under the lock. **17 passed and 3 failed**: runs 07, 09 and 11 (`runs/ground-2-read-path-20-runs.txt:41, 61, 81`). Each failing run gave `{:test 28, :pass 552, :fail 1, :error 0}` and exit 1. No other check failed.

> FAILED through three layers, a permission is given in the nearest while the farther orders hold places: the layer that joins gives a whole answer anew with its own place in it, and the place that it moved is hidden; the screen is what a read asked once gives

The shown line, first from a failing run (line 42), then from a passing one (line 6):

> SHOWN … a layer that joins over farther orders gave 2 whole answers anew, the last with its own place and what a read asked once gives …
> SHOWN … a layer that joins over farther orders gave 1 whole answers anew, the last with its own place and what a read asked once gives …

**What failed.** Of the check's four parts (`test-store/softland/store/read_path_proof.clj:1286-1287`), only `(every? :anew erin-later)` was false (`runs/ground-2-fail-parts.txt:1-3`).
- The shown line counts pushes (`read_path_proof.clj:1303`). So at least one of the two pushes was not a whole answer (my reasoning).
- Which push came first: not checked.
- The receipt blames the order of Rama's pushes (`…/order-and-time/RECEIPT.md:60-65`). These runs did not test that.

## 3. Trying to break two claims — read by the ground-break agent

A cut of `runs/ground-3-break-findings.md` (paths under `src-store/softland/store/`). **Neither claim broke.**

1. "Order never uses the time" holds. The time enters at `module/walk.clj:295` and is only a row value after that (`record/rows.clj:192, 248, 446`; probe lines 30-32).
2. "A refusal moves the count and no fact" holds (`gate/decision.clj:553-555`; probe lines 28-29).
3. Wording: the count is per layer *per gate* (`gate/decision.clj:217-218`, `module/walk.clj:361, 397`).
4. Overstated: `gate/stamp.clj:26-28`. A negative time is refused (`door/declaration.clj:330`; probe line 12).
5. Latent: an accept with no time would throw (`record/rows.clj:248`; probe line 33).
6. Low: a wrapped stamp breaks `gate/decision.clj:457` (probe lines 5, 36-37).
7. Wording: "never throws" (`gate/stamp.clj:50`) holds only for Long or nil (probe lines 2, 6, 7).

The traces and the probe are in that file, `runs/ground-3-probe.clj` and its output. I checked five citations, in findings 1, 2, 4 and 5, at HEAD: all held.

## What these runs changed

- Tracked files: none changed.
- Outside `runs/`: the lock was held, not written. The checkout was made, then removed.
- A marker, `/tmp/claude-1000/gr1-said`, was made, then deleted.
- `/tmp/clojure-13426758051935281907.edn`, a failed compile check's report, remains. It is kept for Sid's word.

## Questions for Sid

Erin watches "what points at canvas-1" through three layers, and is then let into the nearest one. In 17 runs her screen got one complete new answer. In 3 it got two messages, at least one of them partial, and her screen still ended right.

Is the extra message fine? Then the check changes. Must a permission arrive as exactly one complete answer? Then the read path changes.

Default: nothing changes until you answer.
