# The store rig: where it fits, what it checks, where it stands

Read this first. It began as the text of the overview page
(https://claude.ai/artifact/Nd6s5VymTNiFRLdH1QnAd4), kept here so any session
can load it. `RIG.md` is the detailed running record; its "Overnight state"
wins on build state. Written 25 September 2026; brought up to date on 26
September, after the overnight build, where a section says so. That update
did not change the overview page.

Location: branch `rig-2026-09-25`, worktree
`/mnt/data/projects/Softland-rig-2026-09-25`, folder
`src/proposal/rig-2026-09-25/`. Nothing here reads or touches `src/app`.

**Status, 26 September, after the overnight build.** Sid's decision of 25
September, evening, stands: the rig is a candidate for the store core,
built in the form it should be because it may be kept; its adoption is
his, after a read of its code against the rulings; its records are not
kept until edition one. On his overnight brief every remaining phase of
SPEC.md was built on this branch, from 01:37 to about 10:30 IST ("What
happened" has the times). The one full suite passed on step 6b's tree: 26
namespaces, 163 tests, 8,085 assertions, 0 failures; the fixes for H-1 and
M-1 after it ran the 15 namespaces they touch once, one failure fixed and
its namespace rerun green. All 14 of the model's fixed histories replay as
the model says. The three numbers are measured on the finished store,
before those fixes, against thresholds that are still guesses. The
machinery count is 33 as built, 35 by one unit rule. Two reviews by
reading: the full-spec review found eight items at the seams no stage's
tests cross (H-1, high, and M-1, medium, are fixed; M-2, medium, and five
lows are questions for Sid), and step 6b's review five lows. What is left
is Sid's reading and his rulings: "What is left" below, and `RIG.md`'s
"For Sid" and "What is next". Text below that says "throwaway" is
history.

*Status, 25 September, evening (history).* This is the page as written at
11:05 IST. Since then, Sid's decision: the rig is a candidate for the store
core, not a throwaway; built in the form it should be, because it may be
kept; records not kept until edition one. The order of work is
`src/proposal/frame-2026-09-15/PROGRESS.md`, "Next"; "What is left: two
ways forward" below is superseded by it. Phases 2, 3 and 4 wait; the next
build is `STARTER-next.md`.

Softland is meant to be a place people and agents change from the inside,
including the tools they change it with. Its frame keeps everything in one
store as facts and leaves only the runtime fixed. The rig is a build of
that store's rules on Rama, a candidate for the store core, whose records
nobody keeps until edition one. It checks the line before the store's
first record: whether the platform gives what
the rules assume, whether it stays affordable at the scale Sid holds, and
whether new tools can arrive as facts alone.

## Where this fits: a chain of questions, each narrower than the last

The rig is the latest step in a chain that starts with what Softland is
for. Each step asked a narrower question than the one above it, and was
answered before the next began.

1. **Softland** (the vision). A place where people and agents understand,
   create and change things together, and can change the place itself from
   inside, tools included. The vision's own test is building Softland in
   Softland. *Source: carry-on.md, sections 5 and 6.*

   ↓ which raises

2. **The line** (frame proposal, 15 to 18 September). In such a place every
   piece is fixed, changeable only by stopping the place and rebuilding it,
   or live, changeable from inside while it runs. The problem is where that
   line goes, what the fixed side must guarantee so the live side stays
   coherent, and "how you would know the line is drawn right before you have
   built everything on it." *Sid on this statement: "layer -1 reads right."*

   ↓ the frame's answer

3. **One substance** (the walk, 16 September). Everything is the same kind
   of record: things, tools, the shapes values must have, the policies on
   who may write where, and the gate's own decisions. Only the runtime that
   reads the records is made of something else. So one store holds the
   whole world, and it is the fixed floor that everything live stands on.
   *Sid ruled on 21 September: one store for the whole world.*

   ↓ which means

4. **The first record** (21 September). Sid: "the store only appends and
   never rewrites, so whatever a fact does not carry when it is made is gone
   for every earlier fact. what i want first: the conventions that would
   paint me in a corner." Eight research voices were applied to find those
   corners. *Source: CORNERS.md.*

   ↓ answered by

5. **The rulings** (22 to 25 September). Sid's answers, ruled in chat: two
   gates and where each kind of layer lives, a lock per value and forgetting
   by destroying locks, names made by the offerer, the envelope every fact
   carries, permissions, promotion, and reads. *Source: PROGRESS.md, "Now".*

   ↓ checked for contradictions by

6. **The formal model** (24 and 25 September). A small executable model of
   the rulings, run over 30,000 generated histories. The first round found
   six places where the rulings were silent or pulled against a property,
   and a second round added five changes. Each is a reading Sid made his
   own. *Source: src/proposal/formal-model-2026-09-24/.*

   ↓ checked on the real platform by

7. **The rig** (25 September). **You are here.** The second half of the
   frame's problem, run on Rama itself. Does the platform give what the
   rules assume? Does it stay affordable at the scale Sid holds? Can a new
   tool and a new grammar arrive as facts alone? PROGRESS.md names it "the
   rig that tests the line".

   ↓ feeds

8. **The decision record** (next). Both sessions' lists merged, each ruling
   marked, landing as DECISIONS.md. *Source: PROGRESS.md, "Next".*

   ↓ then

9. **The real store, and its first kept record.** From here the conventions
   freeze. Whatever the first record carries, every later record is
   measured against.

## What it checks against: the frame's six promises, and the rulings under them

The frame's problem statement says what coherent must mean for everything
live. Those six promises are what the fixed side owes, so they are the
yardstick for the rig. Each part of the rig checks one of them, through the
rulings written to keep it.

| The frame's promise | What the rig checked | Where it stands |
|---|---|---|
| "When something you built on changes, you find out. Always." | Read entries that fingerprint what a read matched, so a later change can be noticed. Phase 5. | built and tested (26 September): every read through one exit, standing reads delivering changes |
| "Every change says who made it, when, and what they were looking at." | The envelope: every act carries who, when, the permission it acts under, and the stamps of what it stood on. Phase 1. | built and tested; reads recorded through the one exit (26 September) |
| "Many people and agents work at once, each in their own working copy, without blocking each other." | Each person's layer and each agent session on its own home task, measured as one person's layer on one thread and the agent rate. Phase 7. | measured on the finished store (26 September) |
| "Things from outside enter ... and none of the above breaks." | A gate that never throws, refusing malformed records as data, needed because an exception kills the worker. Phases 0 and 1. | tested |
| "The place cannot lie. It never shows a guess as a fact or old as current." | Answers only once the decision is visible; no batch read half-committed, planned as the settled frontier; stamps that run ahead of real time, found in phase 7. | built (26 September): the settled frontier; stamps on the hybrid clock, not yet ruled |
| "It stays affordable as the number of people and agents grows." | The three numbers. Phase 7. | measured on the finished store against guessed thresholds |

### Each rig question checks a ruling

| Rig question | The ruling it checks | What a bad answer would change |
|---|---|---|
| Rama's claims (phase 0) | Ruling 1: a stream gate for one-owner layers, a microbatch gate for shared ones. PROGRESS.md: "the rama skill verifies before code." | The gate kinds. One reading failed, readers can see half a batch, so the micro store needs a settled frontier. |
| Index writes at the agent rate | Ruling 2: one-owner layers placed by layer; N, the task count, fixed at launch for two years. "A stream is many small acts." | How many agent sessions one task can carry, and so how large N must be for the scale Sid holds. |
| One person's layer on one thread | Ruling 2's placement by layer, with re-class as the way out for a hot layer. | Whether one thread can carry a person, or a person's layer needs splitting. |
| Lock store growth | Ruling 7: every value gets its own lock, a row of its own in personal and hand layers; forgetting destroys the lock. | Whether a lock row per value is an affordable default, or hand layers lock per act or keep locks in the record. |
| The count (phase 6) | The line itself: keys are ids and their grammar is facts; tools add facts, never envelope parts; one substance. | Each compiled step still needed marks a piece on the fixed side that the frame says should be live. |
| Replays (phase 8) | The formal model's finding that the rulings hold together. | Any difference between the rig and the model is a finding about Rama, or about the rulings. |

### The yardstick for the numbers

The thresholds used below are the main session's guesses. The yardstick the
frame work actually holds is the scale Sid set out on 21 September:

> hundreds to thousands of agents per person, writing at machine speed; tens
> of thousands of people and their agents on one problem; a field of about ten
> million papers with layers of summaries; the planet; years of running.
> *(CORNERS.md, section 0, "the scale he holds")*

Against that scale the agent rate matters as a divisor. It says how many
agent sessions one task can carry, which feeds the choice of N, fixed at
launch for two years. As arithmetic under stated assumptions, not a
measurement: if one task admits about 5,000 small acts a second, as it did
here in-process, and an agent at full speed writes about 100 a second, one
task carries about 50 agents at full speed. A person running 1,000 agents at
full speed would span about 20 tasks. How fast an agent really writes, and
how many run at once, are the inputs that turn this into a threshold.
*(26 September: on the finished store one task admitted about 1,200 small
agent acts a second, so the same arithmetic gives about 12 agents at full
speed a task, derived, not measured; the step that measures it, variant B,
did not run.)*

## At a glance

*Brought up to date on 26 September.* Every phase is built and tested on
this branch. The one full suite, on step 6b's tree: 26 namespaces, 163
tests, 8,085 assertions, 0 failures, 0 errors. The full-spec review's H-1
and M-1 were fixed after it: the 15 namespaces the fixes touch ran once,
and their one failure was fixed and its namespace rerun green. The three
numbers are measured on the finished store, before those fixes; each
verdict is judged against thresholds the main session assumed, and Sid
sets the real ones. On 25 September they had been measured on thin slices
of what existed then; those are kept as history below.

| Phase | What it is | State |
|---|---|---|
| 0 | Rama claims | built and tested (25 September) |
| 1 | Stream store | built and tested; stamps on the hybrid clock |
| 2 | Locks and forgetting | built and tested (wave 1) |
| 3 | Micro store | built and tested (wave 1) |
| 4 | Promotion | built and tested (wave 2) |
| 5 | Reads and read entries | built and tested (the read exit, then the rest in wave 2) |
| 6 | Tools, grammars and the count | built and tested (the stream side in wave 2, the micro side in step 6b); the machinery count 33, or 35 by one unit rule |
| 7 | The three numbers | measured on the finished store (the minimum set) |
| 8 | Replays of the model's histories | all 14 as the model says |

| Number (ruling) | Result on the finished store | Verdict (assumed thresholds) |
|---|---|---|
| Agent layer rate (ruling 2) | about 1,200 small acts a second on one task, 12,400 index writes (9 per act, 10.1 with its share of lease acts); the task's thread never past 61% of a core | near 1,000, undecided on this cluster; the door, not the gate, limited it, so the task's own ceiling is unmeasured |
| One person's layer on one thread (ruling 2) | 226 acts a second at one writer, 1,062 to 1,198 at 32 to 128 writers; at 100 acts a second arriving on a schedule, 46.1 ms for the slowest 1 in 100 | rate passes; latency near, on the failing side (2.3 times 20 ms); its way out, re-class, was qualified by the full-spec review's H-1, fixed after the run |
| Lock store growth (ruling 7) | a fixed 181 bytes a value logical, 81 on disk, whatever the value's size; linear to 100,000 values; about 34 more for each extra person | depends on value size: over four times 40-byte values logically, just over two on disk; fine at 200 bytes |

## Why now: some choices freeze at the first record

A stored record keeps the position it was written with. Changing certain
conventions later means migrating every record already written under them,
and where the change alters what a record means, it is not allowed at all.
Four choices are of that kind:

- **Placement.** Which task a layer's data lives on.
- **Lock grain and location.** How finely a value is locked, and where its
  lock is kept. This decides what a forget can reach.
- **Wrap subjects.** Whose locks a value is wrapped under, which decides who
  it dies with.
- **The envelope and names.** The shape every record carries.

That is why the rig runs before the real store, and why its records are
thrown away. A throwaway build can still change any of these choices; a
kept record cannot. So everything the rig finds either confirms a ruling,
or changes one while changing it is still cheap.

The rulings live in `src/proposal/frame-2026-09-15/PROGRESS.md`, and a
formal model of them holds: all 14 of its fixed histories come out as
stated.

## The plan: nine phases, built the rama skill's way

Sid's brief set out nine phases, 0 to 8. Phase 0 checks the Rama claims.
Phases 1 to 6 build the store one layer of behaviour at a time, and phase 6
ends with the count: add one new tool and one new key grammar by writing
facts only, then count the code changes still needed, with zero the target.
Phase 7 takes three numbers, and phase 8 replays the model's fixed
histories through the rig to compare answers.

Each build phase runs through the rama skill's own steps; to keep the two
numbering systems apart these are called *steps*: plan, validate the plan,
then a build session that implements, validates the code, writes tests,
validates the tests, and runs the suite to green. Each design step runs in
a fresh session so it does not anchor on an earlier choice. That process
caught real problems, among them 14 fixes to the stream store's plan, a
layer's owner given as text instead of an id, which would have crashed the
worker and replayed forever, and a float in a set that broke the digest.

## What happened: a night and a morning (25 September), then the next night (26 September)

Times marked ~ are reconstructed from the session; the rest are commit
times.

**Night**

- **~00:40, the brief.** Nine phases, with the instruction to stop and
  report if a Rama claim fails.
- **00:53, phase 0 committed.** The two Rama claims checked on the
  in-process cluster. One reading failed; the run continued.
- **01:42, spec and implicit spec.** The brief and every requirement the
  rulings imply, written down.
- **~01:40 to 02:20, three plan sessions cut off.** The API's safeguard
  filter stopped three Opus sessions writing the stream store plan. A
  fourth, on Fable, finished it in 26 minutes.
- **03:02, phase 1 partial.** Stream store built and smoke-tested. Writing
  its test suite, a safety classifier stopped the response. The night run
  ended.
- **03:18, plan validated.** Minor-fail, with 14 fixes applied to the plan
  in place.

**Morning**

- **08:34, restart.** At Sid's request, fresh sessions on both models:
  Fable plans and validates, Opus builds.
- **~08:45, direction question.** Whether to build the whole rig, or measure
  the numbers first and start the real store.
- **08:52, unexplained rewrite.** Something rewrote `gate.clj`, undoing one
  fix. The build restored it and checked its files against a backup after
  every step. Neither plan session admits to it. *(Traced on 26 September
  to Claude Code's own checkpoint restore in the main session; `RIG.md`,
  "Found tonight".)*
- **09:30, phase 1 suite passes.** 13 tests, 728 checks, on 2, 4 and 8
  tasks. Phase 2's plan committed alongside.
- **09:52, phase 3 plan.** The micro store designed, not yet validated.
- **11:05, overview.** A README written so any session can pick up the rig.
- **11:47, two numbers.** Agent rate and one person's layer, measured on the
  stream store, with a starter for the next session.
- **12:29, third number.** Lock store growth, on a slice built from the
  phase 2 plan. All three numbers in.

**The next night (26 September, IST; commit times)**, on Sid's overnight
brief: build every remaining phase. Three builders ran in turn (A from
01:37, B from 04:26, C from 07:03), each handing off when its transcript
passed 3.5 MB; plans, builds and reviews ran in fresh sessions, each in
its own worktree. `RIG.md`, "Stages tonight", has each landing in full.

- **01:49 to 04:05, the plans.** The overnight defaults and questions
  (01:49); stamps on the hybrid clock (02:07); phase 2's plan revised for
  sealing values at the door (02:36); the read exit, the revision reader
  and phase 3 planned and validated (02:39 to 02:48); phase 4 (03:32); the
  revision reader and the read exit built (03:36, 03:40); the caching rule
  run on two copies (03:39); phase 8, the rest of phase 5 and phase 6
  planned (03:54 to 04:05).
- **05:32, wave 1.** Phases 2 and 3 built, merged and wired to the read
  exit; phase 7's plan (05:35); the review of wave 1 (06:28); phase 8's
  replays, pass 2 (06:47).
- **07:51, wave 2.** Phase 4, the rest of phase 5 and phase 6's stream
  side, merged; one full suite, 148 tests.
- **08:07 to 09:07.** Phase 8's pass 3: all 14 histories as the model says
  (08:07); step R, the review's fixes (08:23); the full-spec review (09:02);
  phase 7's harnesses (09:07).
- **09:21, step 6b**, the micro gate's grammar, built in Sid's Codex session
  after the session's permission check denied builder C's builder for it;
  the night's one full suite, 163 tests, 0 failures. Its review (09:59).
- **10:14, the three numbers** measured on the finished store.
- **10:30, the full-spec review's H-1 and M-1 fixed**, the 15 namespaces
  the fixes touch run, one failure fixed, the replays 14 of 14.

## Phase 0: the Rama claims the model assumes

Failures were injected by throwing inside topology code, and holds by
blocking a task thread, all on the in-process cluster with Rama 1.6.0.
Every clause of the microbatch claim as worded held. What failed is a
stronger reading behind the model's one-step commit.

| Claim | How it was checked | Result |
|---|---|---|
| A stream event is atomic on one partition | A crash between two writes in one event. The attempt's write was discarded; the replay applied each write once. | held |
| A record replays at least once after a crash | A crash on a record's second task after the first committed. The record replayed from its start. | held; for a replica failover, taken from the docs |
| A microbatch is decided before it is visible | A batch held on one task; other tasks read nil until it committed. | held |
| Made visible at commit; no partial state across tasks | A crash on one task after another wrote left nothing behind; the retry applied once. | held |
| Visible on every partition at one instant | Six readers read two tasks in turn for 20 s while batches committed. | **failed** |
| A microbatch can read another module's value | A microbatch topology read a stamp through a mirror. | held, as a read at a moment |

The failed line matters because the model assumes a batch appears
everywhere at once. In the test a reader saw half a batch in 5,822 of
410,155 read pairs. No gate decision depends on it, because the micro gate
decides inside its own batch and the next batch starts only after every
task has committed. Readers do depend on it, so the phase 3 plan reads the
micro store through a settled frontier, where a reader sees a batch only
once every task has committed it.

Phase 0 went past this failed reading instead of stopping, on the judgement
that no gate decision rests on it. If that was the wrong call, everything
after the phase 0 commit is what to discard.

## Findings that shape the store

- **An exception is fatal to the worker** (hard rule). A throw inside
  topology code shuts the worker down and restarts it, in about 0.6 s. Every
  task on it restarts. So a gate must never throw; every refusal is data
  with a reason.
- **Completed records replay after a crash.** Rama replays from the last
  checkpoint, so a record that had already finished can run again. The gate
  answers a replay from what it already wrote, and writes nothing twice.
- **A client can get an error for an offer that went in.** An offerer that
  sees an ack failure resends under the same name and reads the answer from
  the record.
- **Stamps run ahead of real time** (freezes at the first record). A stamp
  counts milliseconds, and each decision stamps at least one past the
  task's last. Above 1,000 acts a second on a task, stamps pull ahead of the
  wall clock: 21 seconds ahead after 18 seconds at 2,250 a second. A read by
  wall time, such as "as of 3pm", and a comparison of stamps across the two
  stores would then mislead; that consequence is reasoned, not measured. The
  stamp's unit must be settled before the first record. *(26 September:
  stamps are now a hybrid clock, wall milliseconds plus a counter, `RIG.md`
  default 2, not yet ruled; on the finished store they stayed within 4 ms
  of the wall.)*
- **The depot keeps plaintext after a forget** (open gap). A forget destroys
  a value's lock, but the original offer sits readable in the depot until it
  is trimmed. The locks plan names this and leaves it open. It has to be
  settled before the first kept record. *(26 September: closed for values
  by `RIG.md` default 1, not yet ruled: the door seals each value under a
  lock leased from the gate, so the depot holds only sealed values. A
  control fact's text still reaches the depot as sent, `RIG.md` For Sid 69
  and 70.)*
- **A module update drains, it does not fail over.** An update waits for
  in-flight records to finish. It is not a stand-in for a failover.
- **Small quirks of Rama 1.6.0.** No `ops/random-uuid7`, though the rama
  skill's reference names one, so the rig makes its own. Only one in-process
  cluster runs at a time on port 2002, so measurement sessions take turns.

What the overnight build found, the Rama facts and the seams among them, is
in `RIG.md`, "Found tonight".

## The numbers on the finished store (26 September)

Phase 7's minimum set ran on the finished store, step 6b's tree, from 09:27
to 10:11 IST: every timing the median of three runs, on the same machine as
the slices below, against the same thresholds, which are still the main
session's guesses. Details, every window and the verdicts: `RIG.md`,
"Numbers so far", and `BUILD_NOTES-numbers.md`, "The run".

| Number | What the finished store gave | Against the guessed threshold |
|---|---|---|
| Index writes at the agent rate | 227, 413, 907 and about 1,200 small acts a second at 1, 4, 16 and 64 writers on one task; 9 index writes an act (10.1 with its share of lease acts), 12,400 a second at the top; the task's thread at most 61% of a core | within ten times of 1,000 at every count, so undecided here; past 64 writers the door's pool sort, not the gate, set the rate |
| One person's layer on one thread | 226 acts a second at one writer, 1,198 at 128; at an arrival of 100 acts a second, 4.2 ms typical and 46.1 ms for the slowest 1 in 100 | rate passes; latency 2.3 times over 20 ms, near, on the failing side |
| Lock store under hand layers | 181 bytes a value logical, 81 on disk, whatever the value's size, linear to 100,000 values; about 34 more for each extra person | 4.52 times a 40-byte value logically (2.02 on disk); 0.90 times a 200-byte value (0.41) |

The finished store does more per act than the slices did (locks, read
indexes, grammar rows), so its rates are 0.4 to 0.76 of theirs. Number 3's
verdict had a qualification: its way out for a layer one thread cannot
carry is re-class to the micro gate, which the full-spec review's H-1
found ended the owner's reads through the one exit when the layer is also
a working layer. H-1's fix landed after the run: a re-classed working
layer's reads are recorded and shown, a read after the re-class waiting
about two microbatch cycles (`RIG.md` For Sid 71 and 90). The numbers were
measured before the H-1 and M-1 fixes; H-1's adds a read of the layer's
class to every entry act on the exit's path, and whether the fixes move
numbers 1 and 3 is unmeasured.

## The numbers on slices of the stream store (25 September, history)

Two numbers ran on the stream store as built; the third ran on a small slice
that writes real lock rows the way the phase 2 plan says. Four tasks, a
thread each, on an AMD Ryzen 9 9900X with 62 GB RAM, Linux
7.0.0-31-generic, OpenJDK 21.0.12.1. The in-process cluster gives orders of
magnitude, not production figures: a result far from its threshold decides
the question, and one near it needs a real cluster. Details and method:
`runs/phase7-agent-rate.txt`, `runs/phase7-one-thread.txt`,
`runs/phase7-lock-growth.txt`.

| Number | Threshold that would confirm the ruling (a guess; Sid sets the real one) |
|---|---|
| Index writes at the agent rate | at least 1,000 admitted acts per second per task, every index written |
| One person's layer on one thread | at least 100 acts per second, at 20 ms or less for the slowest 1 in 100 |
| Lock store under hand layers | at most twice the value bytes; above four times, change the default |

Each admitted small act makes **4 index writes** (its answer record, its log
row, its chain head, the task's clock) and 4 reads.

### Throughput against concurrent writers into one layer

| Writers | One person's layer (acts/s) | Agent layer (acts/s) |
|---|---|---|
| 1 | 300 | 299 |
| 2 | 374 | not measured |
| 4 | 711 | 646 |
| 8 | 1,307 | not measured |
| 16 | 2,228 | 2,249 |
| 32 | 3,362 | not measured |
| 64 | 4,424 | not measured |
| 128 | 5,192 | not measured |

One writer that waits for each ack gets about 300 a second, held there by
ack latency. Throughput kept rising with every writer added while the task
thread stayed under 60% of a core, so the limit reached was the writers in
flight, not the gate.

### Latency for one person's layer (slowest 1 in 100)

| Writers | 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 |
|---|---|---|---|---|---|---|---|---|
| p99 (ms) | 4.7 | 7.3 | 8.3 | 8.7 | 9.7 | 16.5 | 22.0 | 43.9 |

It stays under 10 ms through 16 writers and crosses 20 ms between 32 and
64. One offerer at a time: 3.3 ms typical, 4.7 ms for the slowest 1 in 100,
20.9 ms worst. Most of the floor is this machine's disk syncs, about six
small flushes per act; the gate's own work is under a millisecond.

### Lock store bytes per value, whatever the value's size

| Stored as | Logical bytes per value | On disk, compacted |
|---|---|---|
| base64 text (the plan's choice) | 189 | 108 |
| raw bytes | 169 | 87 |
| each extra person a value must die with | about 44 more | about 40 more |

| Value size | Lock store ÷ value, base64 | Lock store ÷ value, raw |
|---|---|---|
| 40 bytes | 4.7× logical, 2.7× on disk | 4.2× logical, 2.2× on disk |
| 200 bytes | 0.95× logical, 0.54× on disk | 0.85× logical, 0.43× on disk |

Growth stayed exactly linear to 100,000 values.

### What the numbers say

**Agent rate.** One writer waiting on acks gets about 300 acts a second,
three times the 100 a second assumed for one agent streaming tokens.
Sixteen writers reach about 2,250 a second and 9,000 index writes a second,
about twice the threshold. The agent layer was not measured past 16
writers; the same code path on a personal layer passed 5,000 a second at 128
and was still climbing, five times the threshold. Both are within ten times
of it, so the result points to yes without the in-process cluster being
able to confirm it.

**One person's layer.** Every offer into a layer was decided on that
layer's one task, and the task thread never went above 60% of a core. The
rate is far above need, 13 times the threshold at 8 writers. The latency is
probably fine but not decided: the best slowest-1-in-100 is 4.7 ms, only
about four times under the 20 ms line, and most of it is disk rather than
the gate.

**Lock store.** A lock row is a fixed 169 bytes per value stored as raw
bytes, 189 as base64 text, 87 or 108 on disk after compaction, plus about 40
for each extra person a value must die with. So the verdict turns on how big
hand-session values are, which nobody knows yet: the lock store is smaller
than 200-byte values, and four to five times 40-byte ones. Staying under
twice the value needs values of about 85 to 95 bytes or more in logical
bytes; on disk after compaction, about 45 to 55. By calculation rather than
a run, keeping the lock in the record or using one lock per act does not
bring 40-byte values under twice; the sealed lock alone is 60 bytes. The
threshold's denominator matters too. Measured against the rows the store
already writes for each value, the lock store is under one times
everywhere, because a 40-byte value already costs the store about seven
times its size before any lock.

## What exists: the store, built and tested (26 September)

*Brought up to date on 26 September.* One module holds both stores. The
code is under `src/rig/store/`; `RIG.md` has each stage's landing ("Stages
tonight") and every rig choice (R1 to R117).

- **The stream store** (phase 1), for one-owner layers: described below.
  Its stamps are a hybrid clock, wall milliseconds times 65,536 plus a
  counter, so they stay at the wall at any rate (`RIG.md` default 2).
- **Locks and forgetting** (phase 2). The door leases locks from the gate
  and seals every value under one before it is sent, so the offer depot
  holds no plaintext value. At decision the gate wraps the value's lock
  under the person locks of whoever the value is about; the lock sits as a
  row beside its value in personal and hand layers and in the record for
  agent layers. A value forget and a person forget are facts that destroy
  locks, and a read shows a forgotten value only as the date it was erased.
- **The micro store** (phase 3), for shared layers: a microbatch gate whose
  batch is folded on one task and decided in name order, read through a
  settled frontier so no reader sees half a batch. A permission check walks
  its chain; a revoke is one write.
- **Promotion** (phase 4): a request in the owner's layer, the read-out on
  the owner's task, the crossing, a stored forward that is re-sent after a
  crash, and a landing on either gate. The copy travels sealed to a landing
  lease's X25519 public key, so its only opener is the private key in that
  lease row, on the task where the landing is decided, and the decision
  deletes it whatever the answer.
- **Reads** (phase 5): one exit for every read. It queries, records a read
  entry (the read's moment and a keyed fingerprint of what matched), and
  shows nothing until the entry is acknowledged. Shared layers are read at
  a settled frontier; standing reads deliver changes; an agent session's
  close keeps or drops its entries; indexes are purged by a forget and
  rebuilt in pages. Every act into a working layer goes through the gate
  that orders it, so reads stay recorded after the layer is re-classed
  (the H-1 fix).
- **Tools and grammars** (phase 6). A key's grammar is a fact in the layer
  it governs, checked at both gates from rows read in the decision's own
  event; a tool is a fact run by a minimal runner over a recipe of named
  steps; the revision reader cuts a file at a git revision into passages and
  functions. The proof's test grammar and tool were added as facts alone.
  The machinery count, the compiled steps that took: 33 as built, 35 by one
  unit rule (`RIG.md`, "The machinery count so far").
- **The numbers** (phase 7): harnesses under `test/rig/bench/`, run once as
  the minimum set (above).
- **Replays** (phase 8): the model's 14 fixed histories played through the
  store in lockstep with the model; all 14 answer as the model says.

The one full suite, on step 6b's tree: 26 namespaces, 163 tests, 8,085
assertions, 0 failures, 0 errors, in about 17 minutes
(`runs/grammar-micro-suite-cmd.sh` runs it under the cluster lock). Three
reviews: of wave 1, which fixed one fault and left two for step R; and, by
reading, of the whole module against the whole spec (eight items; H-1 and
M-1 fixed after the full suite, their 15 namespaces run) and of step 6b
(five lows). What they raised for
Sid is in `RIG.md`, "For Sid".

**The stream gate, as phase 1 built it** (25 September). Every offer is one
act under one name. It lands on its layer's home task,
and a single stream gate decides it in one atomic event: it checks the
envelope, reads what it needs, decides in the formal model's order of
refusals, writes, and answers the offerer through the ack. There is no
partitioner inside the event, so the answer and the admitted facts become
visible together, and a replay is answered from the record.

```
offerer --append :ack--> offer depot (hash-by :layer) --routes--> the layer's home task
                                                                     |
                                                        gate event, one atomic write, no hop:
                                                        check envelope -> read state -> decide
                                                        (refusal order) -> write (every index)
                                                                     |
                                   layer state, all on this task: settings, answers by name,
                                   log, chain heads, permissions, clock
offerer <--ack: yes/no + stamp-- gate        (a replay is answered from the record;
                                               nothing is written twice)
```

On 25 September a small act made 4 index writes there; the finished store's
makes 9, with its locks and read indexes. Phase 1's own suite passed then:
13 tests and 728 checks, on 2, 4 and 8 tasks, in about 17 seconds. It
covers the model's stream cases: admission whole on the layer's home,
resends answered from the record, reused names refused by a keyed digest,
refusals in the model's order, permissions, control facts, re-class, chains
of replaces, the clock promises, a 600-fact act, and crashes on both sides
of the writes.

*History (25 September): "Designed, not built."* Phase 2 was then a plan
for a per-value AES-256-GCM lock wrapped under the person locks of whoever
the value is about, and phase 3 a plan for the microbatch gate; both are
built now, as above.

## How the work was run

Design steps ran in fresh sessions so none anchored on an earlier choice.
After the API's filter cut off three Opus plan sessions in a row, the plans
moved to Fable and the builds stayed on Opus, each the other's fallback.
Phases were planned and built in overlapping waves rather than strictly in
turn, since one design step alone took 25 to 75 minutes.

| What went wrong | What was done |
|---|---|
| Three Opus plan sessions cut off by the API's safeguard filter | Moved the design steps to Fable; it finished the plan in 26 minutes |
| A safety classifier stopped the response writing the stage 1 test suite | Stopped, wrote the state down and committed; the tests were written later in a fresh session |
| An unexplained rewrite of `gate.clj` at 08:52 | Restored the fix and checked files against a backup after each step; the cause was traced on 26 September to a checkpoint restore in the main session |
| Two measurement sessions colliding on the cluster port | Ran them one at a time, each waiting for a quiet window |
| *26 September:* a builder sat 30 minutes on a command waiting for an approval nobody saw | Stopped and resumed; builders were told to avoid commands that may need approval |
| *26 September:* a fresh module per case on one in-process cluster degraded the cluster | The replays run a cluster per case |
| *26 September:* the session's permission check denied the launch of step 6b's builder, and refused one builder's merge | Sid's Codex session built step 6b; the harnesses landed by Sid's word |

Overnight, on 26 September, Sid's test rule set the pace: test only when
needed, not after every step. The rama skill's validations and the reviews
were done by reading, the one full suite ran once, on step 6b's tree, the
fixes for H-1 and M-1 after it ran only the namespaces they touch, and
phase 7's minimum set was its harnesses' first run. Every in-process
cluster run queued on one lock, one cluster at a time.

## What is left

*Brought up to date on 26 September.* The build is done; what is left is
Sid's, and the work that follows his rulings (`RIG.md`, "What is next",
has each with its pointers):

1. **His rulings**: `RIG.md`'s "For Sid", 1 to 90, the questions that would
   touch a record, the forms that freeze at the first record first; the
   eight defaults taken overnight; and the machinery count's unit rule.
   Whether the rig becomes the store core is his, after a read of its code
   against the rulings.
2. **Edition one's review of the first-record placeholders**, since
   nothing is kept until edition one is written.
3. **The model's third round**: the seed designed against the reference
   tool, and how a passage stays the same across revisions.
4. **The thesis count**, which a fresh session takes later, on a tool
   nobody knows yet.
5. **The performance findings**: the gate's own ceiling, unmeasured behind
   the door; the one-writer latency's tail; number 3's latency near the
   line; the cost of a re-classed layer's key rows, whose cheap remedy is
   caching and so goes to an adversarial examination first.

*History (25 September): two ways forward.* The page offered two paths:
continue the rig as briefed, or start the real store now. Sid's decision
that evening made the rig a candidate for the store core, and his overnight
brief had every remaining phase built. The page's text, as written:

The rig has checked the Rama claims, taken the three numbers and produced a
tested stream gate. The count and the model-history replay are still
undone. Whether to keep building the rig, or start the real store, is
Sid's call.

- **Path A: continue the rig as briefed.** Validate and build phases 2 and
  3 side by side, then 4 and 5, then 6, 7 and 8. It gives the count and the
  model-history replay on throwaway code, and builds the store twice: once
  to throw away, once to keep.
- **Path B: start the real store now** (the main session leaned this way).
  The three numbers carry over unchanged, since they depend on Rama and the
  conventions, not this code. The count becomes the real store's first
  milestone, still before its first kept record, and the model's fixed
  histories its acceptance tests. The stream gate and the plans carry over
  as input.

### Readiness of the remaining phases (25 September, history)

Every phase in this table is built now (see "At a glance").

| Phase | What it needs first |
|---|---|
| 2 Locks and forgetting | a validation pass, then build |
| 3 Micro store | a validation pass, then build; its build first checks the Rama questions the plan left open |
| 4 Promotion | a design session; it can be planned from the phase 2 and 3 plans |
| 5 Reads and read entries | a design session; its open read-moment question is a rig choice, per PROGRESS.md |
| 6 Tools, grammars, the count | a thinking session with Sid first: what counts as a compiled step is still open |
| 7 The numbers in full | mostly mechanical: rerun the harnesses on the finished store |
| 8 Replays | mostly mechanical, after 2 to 4: an adapter over the model's histories |

### What only Sid can settle

*Brought up to date on 26 September:* items 3 and 4 have moved on, marked
below; the rest stand, beside `RIG.md`'s "For Sid" and its defaults, and
whether two nodes can run here, which `RIG.md` carries too.

1. **The three thresholds.** The rate, latency and lock size that turn each
   number into a yes or no. The ones used here are guesses; the scale set
   out on 21 September is the yardstick they should come from.
2. **How big hand-session values are.** This decides whether a lock row per
   value is an affordable default.
3. **The stamp's unit.** Milliseconds let a busy task's stamps run ahead of
   real time. A finer unit, or a wall time plus a counter, freezes at the
   first record. *(26 September: built as the hybrid clock, wall
   milliseconds plus a counter, `RIG.md` default 2; still Sid's to rule
   before the first kept record.)*
4. **The direction.** Path A or Path B above. *(Decided on 25 September,
   evening: the rig is a candidate for the store core; the overnight brief
   had every phase built. Its adoption is still his.)*
5. **Where the real store lives.** A new module beside the server, or the
   server's existing Rama modules evolved toward the rulings. This needs the
   map of the rulings onto `src/app/server` that another session made.
6. **Two model-versus-ruling gaps** (IMPLICIT_SPEC.md D2 and D3). The model
   gives no lock to the facts the store acts on, though a ruling says every
   value gets one. The model's person forget is not a fact, though every
   forget must be one.
7. **The phase 0 call.** Going past the failed microbatch reading instead of
   stopping. Right, or discard everything after phase 0.
8. **A partial test file.** `test/rig/store/gate_test.clj`, left by the
   interrupted write, uncommitted and untouched. Keep or delete.

## Where to find it

*Brought up to date on 26 September;* `RIG.md`, "The skill's artifacts so
far", lists every file.

| File | What it is |
|---|---|
| `README.md` | this overview |
| `RIG.md` | the running record: the overnight state, the stages as each landed, what was found, the questions for Sid, the defaults, phase 0's table, rig choices R1 to R117, the numbers, the machinery count, what is next |
| `SPEC.md` | the brief the build follows: Sid's phases verbatim, pointers to the rulings and the model |
| `IMPLICIT_SPEC.md` | every requirement the rulings imply, where the sources differ, and the open picks |
| `DECOMPOSITION.json` | the six build stages |
| `PLAN-<stage>.md`, `PLAN_VALIDATION-<stage>.md` | each stage's design and its review |
| `IMPLEMENTATION_VALIDATION-<stage>.md`, `TEST_VALIDATION-<stage>.md`, `BUILD_NOTES-<stage>.md` | each stage's build: code review, test review, build log |
| `BUILD_NOTES-wave1.md`, `BUILD_NOTES-wave2.md` | the two merges of stages built side by side |
| `EXAMINATION-copies.md`, `REVIEW-wave1.md`, `REVIEW-full-spec.md`, `REVIEW-6b.md` | the caching examination and the three reviews |
| `STARTER-next.md`, `STARTER-6b.md` | the starters for the read exit's session (25 September) and for step 6b |
| `src/rig/store/` | the store: both gates, locks, promotion, reads, grammars, tools, the doors |
| `src/rig/revision.clj`, `src/rig/claims.clj` | the revision reader, and phase 0's check |
| `src/rig/bench/`, `test/rig/bench/` | the lock slice, and the measurement harnesses |
| `test/rig/` | the tests; `test/rig/replay_test.clj` plays the model's histories |
| `runs/` | saved results: each run's summary, the numbers (`phase7-*`, `phase7-final-*`), the replays (`phase8-*`) |
| `BENCH_NOTES-stream.md`, `BENCH_NOTES-locks.md` | the slices' measurement logs |

From the rig folder:

    clojure -M:test rig.claims-test
    clojure -M:test rig.smoke-test rig.store.envelope-test rig.store.stream-gate-test
    sh runs/grammar-micro-suite-cmd.sh

The first checks the Rama claims, in about a minute. The second is phase
1's suite, in under half a minute. The third is the one full suite, 26
namespaces, about 17 minutes, under the cluster lock; it rewrites the
replay report `runs/grammar-micro-replays.txt`, and step 6b saved its
output as `runs/grammar-micro-suite.txt`.

## Appendix: rig choices

Where the rulings were silent, the rig picked the simplest thing and wrote
it down as a choice, never a ruling. The full text is in `RIG.md`; the
stream store's own picks, P1 to P16, are in `PLAN-stream-store.md`. R1 to
R18, below, are phases 0 and 1's. *Brought up to date on 26 September:*
the night's choices, by stage, each group with its full text in `RIG.md`,
"Rig choices so far":

- **R19** a permission check walks its chain; a revoke is one write
- **R20 to R31** wave 1, the merge of phases 2 and 3 (`BUILD_NOTES-wave1.md`)
- **R32 to R41** phase 8's replays, passes 1 and 2
- **R42 to R48** phase 2, locks and forgetting
- **R49 to R57** phase 3, the micro store
- **R58** the review of wave 1's fix: after a refused lease the micro door
  seals under throwaway locks
- **R59 to R72** phase 4, promotion
- **R73 to R87** the rest of phase 5, reads
- **R88 to R94** phase 6's stream side, tools and grammars
- **R95 to R97** wave 2's merge
- **R98 to R101** phase 8's pass 3
- **R102 to R105** step R, the review's fixes
- **R106** visibility as ruling 9's default in code, the read exit's RC6
  (a question for Sid, since ruling 9 says seed policy facts)
- **R107 to R111** step 6b, the micro gate's grammar
- **R112 to R117** the fixes for the full-spec review's H-1 and M-1

Phases 0 and 1's:

- **R1** the branch lives in a sibling worktree
- **R2** phase 0 was a check, not a module
- **R3** failure injection through global atoms, in-process only
- **R4** tests assert "at least once", never an exact replay count
- **R5** the micro store is read through a settled frontier
- **R6** cross-store stamps: left for phase 3 to decide; both plans have the
  offer carry them
- **R7** "the model's stream cases" means the gate's decision branches
- **R8** "the A cases both directions": erased is erased, open stays open
- **R9** the build stages are Sid's phases, in his order
- **R10** the rama skill's steps are pipelined across stages
- **R11** the design phases run on Fable
- **R12** the rig makes its own UUID7s
- **R13** only the operator writes control facts; the owner may switch grain
- **R14** in the second run, fresh sessions do all the writing, both models
- **R15** phases run in overlapping waves
- **R16** a carried stood-on stamp must be below 2^62
- **R17** the value domain is one form per equality class
- **R18** two revocations of one permission in one act are stale
