# Receipt: run again

proven by a hand-played maker, used by no inhabitant yet

Written on 14 October 2026 for Sid, by the session that finished the build (Opus 5.5: one
session, no builders; one reviewer at the end, at your word). The build session of the night before did parts 0 to D.
Short, as the prompt asked. The working is in `PROGRESS.md` beside this page and in the papers that it
names. Each line says whether a thing was run, derived, or only read. A "test" here is what
those pages call a proof.

## 1. What is true now

Your first line: "The store has to come out the same when it is run again from its depots.
Today it does not, and it has five depots where I decided two."

- **It comes out the same.** Run: a second module read the store's two depots from their
  beginning and decided every act again. Every row of every table of both gates, on each of 4
  tasks, was held against the first module's: 4,668 rows, 0 differ. On the store as it was
  before the build, 3,064 rows differed (`runs/first-proof-run-again.txt`).
- **It has two depots.** Run: the module's test asks the cluster for each depot by name. The two
  depots of acts are there. The two depots of answers and the depot of batch clocks are not.
- **The kept acts give the kept tables, also on another number of tasks.** Run: 333 kept
  records fed to a fresh store of 4 tasks and of 8. 475 rows, 0 differ.
- **After a failure too, as far as one could be made.** Run on the store itself: a throw in the
  base in the middle of a batch, a stall of 8 seconds with the thread held, and two throws in a
  streaming layer. After the four: 54 rows, 0 differ from a run again. One kind of failure
  could not be made in process: a batch that Rama fails without starting the worker again. It
  is on the store's page, "The order of a layer's acts after a failure", and it is a row of
  your queue.

Where each of your eight decisions is built: `PROGRESS.md`, "The parts".

## 2. What this last session did

The review of the whole stopped when the machine went down. Two reviewers of six had reported.
This session applied what they found, in the order that `PROGRESS.md` gave.

1. **A refused thing moves no layer's time.** Before: the stamp of a refusal held the time
   that was written on the refused thing. So a stranger with no permission could send one
   thing with a time one hour ahead. It was refused, and the layer's stamps were an hour ahead
   from then on. The owner's next act was accepted, with a stamp an hour ahead. Her act after
   that, which named that stamp, was refused "carried stamp too far ahead". Both reviewers
   found it, each alone, and ran it on the module in both gates. Now the stamp of a refusal is the layer's last stamp plus one, or 0. Only an
   act that a gate accepts moves a layer's time. This reverses the build session's own choice,
   "a refused act keeps its time". It was never yours.
   - Run: in both gates, a refused thing with a time one hour ahead gets the last stamp plus
     one. In the streaming gate a stranger's act too.
   - What is left of it, derived and not run: a refusal still costs its layer one stamp, and
     65,536 stamps are one millisecond. So refusals alone move a layer's time only when more
     than 65,536 of them are decided in one layer in every millisecond.
2. **The time on an act is below 2^45 milliseconds, the year 3084. It was 2^46.** Before: one
   accepted act with the greatest time, which carried the greatest stamp that it may, got the
   stamp 2^62. No act can carry a stamp that large, so no later act could name its facts. Now a
   layer's stamps reach 2^62 only by counting up, one for each decision: about 2^61 decisions
   in one layer.
   - Run, in the pure test: an act with the greatest time, which carries the greatest stamp
     that the rule of 10 seconds lets it, now gets 2^61 + 655,360,000.
   - One number of `PROGRESS.md` was not exact. It said that a carried stamp within 10 seconds
     of a time below 2^45 is below 2^61. It can be 655,359,999 above. What follows from it
     holds. The code and the pages have the exact number.
3. **The docstrings that said more than the code does**: each of the ten that `PROGRESS.md`
   lists, and the same lines on the store's page. Three of them change what a page tells you.
   - A gate does not know who wrote the time on an act. It knows that the part holds a number
     in range. Anything that appends to a depot writes its own.
   - One accepted act is 5 reads on disk, where the pages said 4. The layer's last stamp sits
     two levels down in its table, and Rama's skill text counts each level as a read. So the
     store's own arithmetic of a cold read gives 384 to 616 acts a second on one task, where
     the page said 480 to 770. Your aim is 1,000. Counted from the code. No run measured a read
     on disk.
   - The rule of 10 seconds does hold one door's clock against another's (section 4, the first
     point).
4. **The tests.** Changed: only what the two fixes broke. In the pure test, 10 rows. In the
   module's test, the two groups on the time, two checks of the 10-second rule, and the test's
   own arithmetic of a stamp. One thing was added, inside a check that was there: the
   stranger. No check is new, and each count of passes is as it was. Three rows of the pure
   test still name 2^46, as a time that is refused. They pass and are true, so they stay.
5. **The fixture is made again**, behind the lock. Points 1 and 2 are rules changed on purpose,
   and your rule for the fixture asks the receipt to say so. Before it was replaced, the store
   with the two rules was held against it: 14 of its 475 rows differed, with 4 tasks and with
   8. They are the stamps of 12 refused answers, and the last stamps of 2 layers whose last
   decision was a refusal. No row of a fact differed, so no accepted act moved
   (`runs/against-the-first-fixture-run-again.txt`, a run that fails on purpose).

## 3. The tests

Run with `bash runs/run-proofs.sh final`. The cuts are `runs/final-*.txt`.

| Test | Passes on `9a46f81a` | Passes at the end | Fails, errors |
|---|---|---|---|
| act | 163 | 163 | 0 |
| pure | 691 | 691 | 0 |
| module | 444 | 444 | 0 |
| door | 275 | 275 | 0 |
| read-path-pure | 209 | 209 | 0 |
| read-path | 553 | 553 | 0 |
| run-again | 46 | 46 | 0 |

The cuts name the head `008e2ee4`: the seven ran on the working tree, and it was committed
when they had passed, as `0d24b03e`. No file under `src-store/` or `test-store/` changed
between the run and the commit (checked by the files' times).

Not run again by this session: the two cuts of measures, `runs/measures-*-final.txt`. They are
the build session's. The two fixes change no read and no write of a gate.

## 4. What needs you

Nothing below is built, and none of your open questions is decided.

- **Yours to decide: what the rule of 10 seconds is held against.** It refuses an act that
  names a stamp more than 10 seconds ahead of the time on that act. No gate has a clock, so
  "the time on that act" is the clock of whichever server ran the door. An example. Two servers
  each run a door, and the second one's clock is 15 seconds ahead. Bob writes a note through
  the second. Alice at once changes his note through the first, so her act names the stamp of
  his note. The gate sees a stamp 15 seconds ahead of her time and refuses her. Neither of
  them did anything wrong. And one more thing follows: whoever may act in a layer can send an
  act with a made-up stamp 10 seconds ahead. It is accepted, and the layer's stamps are then 10
  seconds ahead of the clock.
  A reviewer named another rule: hold a stamp that an act names from its own layer against
  that layer's last stamp. A true stamp of a layer is never later than the last one, and this
  needs no clock. The session's reading of it, derived and not run: it covers a stamp of the
  act's own layer. An act can also name a fact of another layer, the base for one, and a gate
  cannot hold that stamp so without reading the other gate's record, which a run again does
  not give twice. This sits inside your open question, where a time that is far ahead is
  stopped.
- **What follows from the working answer on a second send.** The digest of a thing leaves the
  time out, so that your own second send reads the first one's answer as its own. So a thing
  with no time and the same thing with a time have one digest. If a thing is appended without
  the door, with no time, it is refused "no door time". If its maker later sends the same act
  through the door under the same act-id, she reads that refusal as her own answer. Derived
  from the code; no run made it. The working answer is now marked "not decided" in all three
  places that make it: the door's call, the door's test of its own parts, and the digest.
- **For whoever builds versions** (your decision 6). The gate examines the time before it
  reads the version. A rule of the time that differs by version needs the version read first.
- **A cost that was found and is not built.** One row for each layer that holds its last stamp
  and its marked keys would make one accepted act 3 reads, where the count is 5.
- **It was so before this build.** One very large thing holds a task's thread while its digest
  is taken, before any bound is held: 5.7 seconds for 256 MB, in a reviewer's run. Not run by
  this session.
- **The review was cut short.** Not reviewed: the read path, whether the tests notice a store
  that is broken, and the pages against the code. The reviewer who breaks the store left
  partial runs in `/mnt/data/projects/Softland-run-again-review-proofs/review-out/`. This
  session first ran no reviewer, because the prompt you pasted said "no reviewers". That was
  the night session's proposal, not your word: you had asked why six, not for none. One
  reviewer then ran, at your word (section 7).
- **What the build session would do otherwise**, as it said to you that morning: delete first
  where the work separates, and let the old tests judge it; change behaviour after, and touch
  only the tests that it must break; one reviewer, as the build's prompt asked; fewer new checks. And the root
  of the night's size: the tests and their map describe the store's insides, so a simpler
  inside cost a rewrite of both. This last session saw the same at a small size. The two fixes
  are five lines of code. With the ten corrections of wording, the store's page changed by 118
  lines and the tests' map by 39.

## 5. The commits

None is pushed.

| Commit | What |
|---|---|
| `c7aa8fc9` | The test that runs the gates again over the depots, failing on the store as it was |
| `0d76f1c9` | Part A: the door writes the time, and the streaming gate decides the same when run again |
| `d30351ff` | The plan of the base's gate, with the runs of its Rama claims |
| `9a46f81a` | Part B: the base is one line with a copy on every task, two depots, and the store comes out the same |
| `6dae07ce` | The pages: the tests' map, the queue, the open rows of the two receipts, the folder's page |
| `008e2ee4` | Where the build stopped, and what was left |
| `0d24b03e` | The two fixes, their tests, the docstrings, the fixture made again, the final runs |
| the one after | This receipt |

## 6. Scratch that is not committed, and waits for your word

Nothing was deleted. Each is named here so that you can say which goes.

- Four detached checkouts beside the repository: `/mnt/data/projects/Softland-run-again-baseline`,
  `-review-gates`, `-review-proofs`, `-review-pure`.
- In this folder: `draft-the-bases-gate/`; `runs/fixture-second-making/`;
  `runs/falsify-check-4/` and `runs/falsify-run-again.txt`; 55 `raw-` files and 18 `try-` files
  under `runs/`. Of those, this session made 14 `raw-` files and 6 `try-d-` files.
- 43 logs under `probes-order-after-a-failure/logs/` and `probes-the-bases-gate/logs/`, named
  `…-stdout.txt`.
- Beside this folder: `../run-again-probe-2026-10-13/`. It was there, untracked, when this
  session began. This session did not make it and did not open it.
- The reviewer's checkout, `/mnt/data/projects/Softland-review-break-the-store`, with its bugs'
  diffs and runs in `review-out/` (section 7).

## 7. The one review, after the receipt

You asked for it after reading this receipt. One reviewer, given the code and not this session's
conclusions, worked in a checkout of its own at `b9330485`. It put small bugs into the store, one
at a time, and ran the tests that could see each. Its diffs and runs are in that checkout's
`review-out/`. This session checked the four bugs that got past every test, and the runs of the
two fixes put back, against those files.

- **19 bugs: 15 caught, mostly by several checks. 4 got past every test that could see them.**
  None of the four changes what the tables hold:
  - the task that decides an act of the base also writes a copy of it to itself;
  - a refusal, a repeat or a stray is sent to every task with nothing to write;
  - the streaming gate's retry mode is "individual" instead of "all after";
  - two checks of a stray are made in the other order.

  Two more were caught by a single check each. The six are now on the page about the tests, as
  ideas for tests not yet written.
- **The two fixes hold, and the tests notice if either is undone.** Putting the refusal's stamp
  back made 4 checks of the pure test and 15 of the module's fail. Putting the bound back to 2^46
  made 6 of the pure test fail. One edge was found: after a last stamp whose counter is full, a
  refusal's "last plus one" carries into the time by one millisecond. That takes 65,536 decisions
  in one millisecond. The docstrings now say so.
- **Yours to decide, and it sharpens section 4's first point.** One *accepted* act with a time far
  ahead still does the harm that the fix removed for refusals. An example: someone who may act in
  a layer appends an act without the door, with a time one hour ahead, or a door's server clock
  runs an hour fast. The act is accepted, and every later stamp of that layer is an hour ahead.
  For that hour, an honest act that names any fact written since is refused "carried stamp too
  far ahead". With a time at the end of the range, that lasts until the year 3084. The bound at
  2^45 keeps the stamps below 2^62; it does not stop this. It sits inside your open question of
  where a time that is far ahead is stopped. Run by the reviewer on the pure functions
  (`review-out/part2-probe.clj`), and it follows from the rule in the code.
