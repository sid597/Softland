# The rig — a throwaway Rama build of the store's rules

Branch `rig-2026-09-25`, worktree `/mnt/data/projects/Softland-rig-2026-09-25`,
folder `src/proposal/rig-2026-09-25/`. Rama 1.6.0, rama-helpers 0.10.0,
Clojure 1.12.4, as pinned in the repo's root deps.edn. In-process cluster
only. Its records are not kept.

This file is rewritten at the end of every phase. A fresh session continues
from "What is next".

## Status after phase 0 (25 September 2026, 00:55 IST)

**Phase 0 ran. One reading of claim 2 failed, and I went on anyway.** Sid's
instruction was to stop and report if a claim fails. Every clause of claim 2
as worded held: a microbatch is decided before it is visible, made visible
at commit, and atomic across partitions in the sense that no attempt leaves
partial state. What failed is the stronger reading under the model's "commits
it in one step": that a reader sees the whole batch at one instant. It does
not. Each task makes its part visible when its own commit finishes, and a
reader can see a batch on one partition and not yet on another (ran, 5,822 of
410,155 read pairs). No gate decision rests on that reading: the micro gate
decides inside its own batch, and the next batch starts only after every
task has committed. Readers do rest on it. So the rig does not build on it:
the micro store is read through a settled frontier (rig choice R5), and a
test shows the frontier read never sees half a batch. If Sid meant stop,
the commits after phase 0 are the ones to discard; each phase is its own
commit.

## Phase 0: the two Rama claims, and the cross-module read

Module and tests: `src/rig/claims.clj`, `test/rig/claims_test.clj`.
Command: `clojure -M:test rig.claims-test` (about 50 s). Observations from
the last run: `runs/phase0-claims.txt`. Four tasks, four threads, one worker.
Failures are injected by throwing from topology code; holds by blocking a
task thread on a promise.

| claim | how it was checked | answer |
|---|---|---|
| A stream event is atomic on one partition | A failure between two writes in one event on one task. The attempt's writes were discarded; the replay applied each write once (both counters 1, though the first write's code ran twice). | ran-and-held |
| A stream record is processed at least once across a failover | A crash on a record's second task, after its first task committed at the partitioner. The record replayed from its start: the first task's write was applied on every run (3 runs, counter 3); the second task's once. | ran-and-held for a worker crash and restart. A failover to another replica cannot run on an in-process cluster (replication factor 1); that the replay rule is the same there is taken-from-the-docs |
| A microbatch is decided before it is visible | The batch held on task 3 after its write ran on task 0; tasks 0 to 2 read nil during the hold, 1 after the commit. | ran-and-held |
| A microbatch is made visible at commit | Reads after the commit show every task's write. | ran-and-held, per task |
| A microbatch is atomic across partitions | A crash on task 1 after task 0 wrote. No partial state survived; the retried batch applied once on every task (counts 2, though the task-0 write ran twice). | ran-and-held |
| A microbatch is visible on every partition at one instant (the model's one-step commit) | Six readers read two tasks one after the other for 20 s while 345 batches committed; each record raised a per-key maximum on all four tasks. | **ran-and-failed**: in 5,822 of 410,155 pairs the later read showed an older state than the earlier one, e.g. task 2 through record 14, then task 0 through record 7. Settled values agree on every task. Matches the docs: "each task commits independently and its writes become visible as soon as its own commit finishes" |
| Can a microbatch topology read a value another module wrote? | A microbatch topology reads a stamp through a mirror PState, and one from a stream topology of its own module. | ran-and-held: it read `[42 7]`, and a later batch saw the later stamp 43. The read is a read at a moment, not atomic with the other module's writes, and can differ across retries of one batch (taken-from-the-docs). So the offer need not carry the stamp; see R6 |

Four more findings, each ran:

1. **An exception thrown in topology code is fatal to the worker.** Rama
   1.6.0 logs "Unexpected throwable! Will be treated as a fatal!", shuts the
   worker down and restarts it (about 0.6 s here). Every task on that worker
   restarts. So a gate must never throw on an offer: every refusal is data.
2. **A crash replays records that had already completed.** The first
   record, finished before a later crash, ran again after it (its first
   write ran three times in all). Rama replays from the last checkpoint.
   The model replays only offers "begun and not finished"; the rig's
   answer record has to cover finished offers too, and does, by name.
3. **The offerer can see an error for an offer that went in.** The acked
   append threw on the client when the worker died, and the record was
   processed after the restart. An offerer must resend under the same name
   and take the answer from the record.
4. **A module update drains, it does not fail over.** `update-module!`
   waited for a held record to finish (8.7 s, released after 3). It is not
   a failover analog.

## Rig choices so far (not rulings)

- **R1. A sibling worktree for the branch.** A parallel session was active
  in the main working tree (an untracked folder created there five minutes
  before this session started, with a fresh classpath cache), and switching
  that tree's branch would have moved its HEAD. The repo already keeps
  branches in sibling worktrees.
- **R2. Phase 0 is a check, not a module.** The rama skill allows judgment
  for tasks that are not module builds; phase 0 skipped the planning
  artifacts. The store phases follow the skill's full process.
- **R3. Failure injection through global atoms.** Works only because the
  in-process cluster runs every task in one JVM.
- **R4. Tests assert "at least once", never "exactly twice".** A crash can
  replay a record more than once.
- **R5. The micro store is read through a settled frontier.** Because of
  the failed reading above. Decided in detail in phase 3.
- **R6. Cross-store stamps.** Open until phase 3: the micro gate can read
  what an offer stood on through a mirror; whether it does, or the offer
  carries the stamp, is decided there.

## Numbers so far

None yet. Machine for every number: AMD Ryzen 9 9900X (12 cores, 24
threads), 62 GB RAM, Linux 7.0.0-31-generic, OpenJDK 21.0.12.1.

## What is next

Phase 1, the stream store, through the rama skill's process: the implicit
spec, the decomposition into the rig's phases as subsystems, then per phase
a plan, a plan validation, a build with tests, and a commit. The full-spec
review runs after the last store phase.
