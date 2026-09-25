# The rig: what it is, why, and where it stands

Read this first. `RIG.md` is the detailed running record; this page is the
overview. Written 25 September 2026.

Location: branch `rig-2026-09-25`, worktree
`/mnt/data/projects/Softland-rig-2026-09-25`, folder
`src/proposal/rig-2026-09-25/`. Nothing here touches `src/app`.

## What the rig is

The rig is a throwaway build of Softland's store rules on Rama. The rules
are the rulings in `src/proposal/frame-2026-09-15/PROGRESS.md`, and a
formal model of them holds (`src/proposal/formal-model-2026-09-24/`).
The rig runs only on Rama's in-process cluster, and nobody keeps its
records. It exists to answer a few questions before the real store is
written.

## Why it exists

Some choices in the store are frozen by the first record we keep:

- which task a layer lives on
- how finely values are locked, and where each value's lock is kept
- whose locks a value is wrapped under, which decides who it dies with
- the shape of the envelope and of names

A record keeps the position it was written with. Changing one of these
choices later means migrating every record already written under it, and
where the change alters what a record means, it is not allowed at all. So
before the first kept record we want to know three things:

1. Does Rama behave the way the formal model assumes?
2. Are the ruled defaults affordable at the rates we expect?
3. Can the store grow from inside, by facts alone?

The rig answers these with a throwaway build, so the real store is written
knowing the answers.

## The questions, and what each one decides

| question | what it decides | status |
|---|---|---|
| Rama's claims: is a stream event atomic on one task and replayed at least once after a crash? Is a microbatch decided before it is visible and atomic across tasks? | whether the two gates can be built as ruled | done: all held except one reading, below |
| Index writes per second, with an agent session writing small acts continuously | whether an agent session can be an ordinary one-owner layer that writes every index as each act is admitted, or needs coarser acts or fewer indexes | measured; see "The numbers so far" |
| One person's layer on one thread: acts per second and latency | whether placing a person's whole layer on one task is enough, or persons need a different placement | measured; see "The numbers so far" |
| Lock store growth under hand layers: bytes per value over 100,000 values | whether a separate lock row per value is an affordable default for hand sessions, or they should default to one lock per act or locks kept in the record | measuring now |
| The count: add one new tool and one new key grammar by writing facts only, and count the code changes still needed | whether the line between what is compiled and what is a fact is drawn right, so the store can grow from inside | not started; needs most of the store; proposed as the real store's first milestone |
| Replaying the model's fixed histories through the rig | whether Rama carries the rulings exactly as the model decides them | not started; proposed as the real store's acceptance tests |

A number decides nothing without a threshold. These are the main session's
assumptions, not rulings; Sid sets the real ones:

| number | a result that confirms the ruling |
|---|---|
| agent rate | at least 1,000 admitted acts per second per task, with every index written |
| one person's layer | at least 100 acts per second, at 20 ms or less for the slowest 1 in 100 |
| lock store | at most twice the bytes of the values it protects; above four times, change the default |

The in-process cluster gives orders of magnitude, not production figures.
A result far from its threshold decides the question; a result near it
needs a real cluster.

## The numbers so far

Measured on 25 September by a fresh Opus session, on the stream store as
built, with 4 tasks and a thread each, on an AMD Ryzen 9 9900X with 62 GB
RAM. Medians of three runs checked for interference. Details and method:
`runs/phase7-agent-rate.txt`, `runs/phase7-one-thread.txt`,
`BENCH_NOTES-stream.md`.

Each admitted small act makes 4 index writes (its answer record, its log
row, its chain head, the task's clock) and 4 reads.

| number | result | against the threshold |
|---|---|---|
| agent rate, one offerer waiting on each ack | about 300 acts per second; slowest 1 in 100 at 4.7 ms | one agent needs about 100 a second, so one is served three times over |
| agent rate, 16 offerers on one layer | about 2,250 acts per second, 9,000 index writes per second; slowest 1 in 100 at 9.9 ms | about 2 times the 1,000 threshold |
| agent rate, 128 offerers | about 5,200 acts per second, still rising | 5 times the threshold; points to yes, too close for this cluster to decide |
| one person's layer, one act at a time | about 300 acts per second; 3.3 ms typical, 4.7 ms for the slowest 1 in 100 | rate 3 times the threshold, latency 4 times under it |
| one person's layer, 8 at once | about 1,300 acts per second; slowest 1 in 100 at 8.7 ms | rate 13 times the threshold: decided yes |
| lock store growth | still being measured; results land in `runs/phase7-lock-growth.txt` | |

Most of the 3.3 ms is this machine's disk: about six small flushes per act.
The gate's own work is under a millisecond, and its thread never went above
60% of a core. Every offer into a layer was decided on that layer's one
task.

## What was found so far

From phase 0, run on the in-process cluster with Rama 1.6.0:

- A stream event is atomic on one task. A record replays from its start
  after a worker crash, at least once, and records that had already
  finished replay too.
- An exception thrown inside topology code kills the whole worker. So a
  gate must never throw; every refusal has to be data.
- A client can get an error for an offer that actually went in. So an
  offerer resends under the same name and reads the answer from the record.
- A microbatch is decided before it is visible and leaves no partial state
  across tasks.
- **A microbatch is not visible on every task at once.** Each task shows
  its part when its own commit finishes, and a reader saw half a batch in
  about 1 in 70 read pairs. The formal model assumes the opposite. The fix
  planned for phase 3 is a settled frontier: readers see a batch only once
  every task has committed it.
- A microbatch can read another module's data, as a read at one moment.

From the stream store's build and its reviews:

- Every write the gate makes has to be safe to repeat, because Rama replays.
- The offer depot keeps each value's plaintext after the value is
  forgotten. The locks plan names this as an open gap: forgetting cannot
  reach the depot until the depot is trimmed. It has to be settled before
  the first kept record.
- Rama 1.6.0 has no built-in time-ordered UUID generator, though the rama
  skill's reference names one; the rig makes its own.

From the measurements:

- **Stamps run ahead of real time on a busy task.** A stamp counts
  milliseconds, and each decision stamps at least one past the task's last
  stamp. Above 1,000 acts a second on one task, stamps pull ahead of the
  wall clock: 21 seconds ahead after 18 seconds at 2,250 a second. The
  clock promises still hold. A read by wall time, such as "as of 3pm", and
  a comparison of stamps across the two stores, do not. The stamp's unit
  is frozen by the first kept record, so before then stamps need a finer
  unit, or a wall time plus a counter that keeps the time part at real
  time.

## What exists

- **Phase 0, done.** A small check module and its tests, `src/rig/claims.clj`
  and `test/rig/claims_test.clj`.
- **Phase 1, the stream store, done.** One depot of offers, placed so each
  offer lands on its layer's home task. One gate there decides each offer in
  a single step: it checks the envelope, reads what it needs, decides in the
  formal model's order of refusals, writes, and answers the offerer. Its
  suite passes: 13 tests and 728 checks, on 2, 4 and 8 tasks.
- **Phase 2, locks and forgetting: planned, not validated, not built.**
- **Phase 3, the micro store: planned, not validated, not built.**
- Phases 4 to 8 (promotion, reads, tools and grammars, the numbers in full,
  the replays) are not started. The three numbers are measured on slices
  instead: two are in, lock growth is finishing.

## Open for Sid

1. The three thresholds above.
2. The direction. The current one is: measure the three numbers now, then
   start the real store with the count as its first milestone. The
   alternative is to continue the rig through phases 2 to 8 as first briefed.
3. Where the real store lives: a new module beside the server, or the
   server's existing Rama modules evolved toward the rulings. This needs the
   map of the rulings onto `src/app/server` that another session made.
4. Two places where the model and a ruling differ, from `IMPLICIT_SPEC.md`:
   D2, the model gives no lock to the facts the store acts on, though ruling
   7 says every value gets one; and D3, the model's person forget is not a
   fact, though every forget must be one.
5. Phase 0 went past the failed microbatch reading instead of stopping, on
   the reasoning that no gate decision depends on it. If that was wrong,
   everything after the phase 0 commit is what to discard.
6. `test/rig/store/gate_test.clj` is a partial file from an interrupted
   write, uncommitted and untouched. Keep or delete.
7. The stamp's unit: milliseconds let a busy task's stamps run ahead of
   real time. A finer unit, or a wall time plus a counter, has to be chosen
   before the first kept record.

## The files

| file | what it is |
|---|---|
| `README.md` | this overview |
| `STARTER-next.md` | where the next session starts |
| `RIG.md` | the running record: status, phase 0's table, rig choices R1 to R18, what is next |
| `SPEC.md` | the brief the build follows: Sid's phases verbatim, pointers to the rulings and the model |
| `IMPLICIT_SPEC.md` | every requirement the rulings imply, where the sources differ, and the open picks |
| `DECOMPOSITION.json` | the six build stages |
| `PLAN-stream-store.md`, `PLAN_VALIDATION-stream-store.md` | phase 1's design and its review |
| `IMPLEMENTATION_VALIDATION-stream-store.md`, `TEST_VALIDATION-stream-store.md`, `BUILD_NOTES-stream-store.md` | phase 1's build: code review, test review, build log |
| `PLAN-locks-and-forgetting.md`, `PLAN-micro-store.md` | phases 2 and 3's designs, not yet validated |
| `src/rig/store/` | the stream store: envelope, gate decision, module, client |
| `test/rig/` | the tests; `test/rig/store/stream_gate_test.clj` is phase 1's main suite |
| `runs/` | saved results of the runs, including the numbers (`phase7-*.txt`) |
| `BENCH_NOTES-stream.md`, `BENCH_NOTES-locks.md` | the measurement sessions' running logs |

## How to run it

From the rig folder:

    clojure -M:test rig.claims-test
    clojure -M:test rig.smoke-test rig.store.envelope-test rig.store.stream-gate-test

The first checks the Rama claims, in about a minute. The second is phase
1's suite, in under half a minute.
