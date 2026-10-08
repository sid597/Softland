# Implementation validation

Part: `microbatch-gate`. Stage: the build, step "validate the implementation" (the Rama skill's
`phase-4-impl-validate.md`). Written by the build's own session (Opus 5.5), 8 October 2026, for the
build session "softland-11". It read no code of the rig, the server or Inland.

Checked: `batch.clj`, `microbatch_gate.clj` and the changed lines of `streaming_gate.clj`,
`door_reads.clj`, `record.clj`, `record_pstates.clj` and `module.clj`, against
`PLAN-microbatch-gate.md`, the check of that plan, the contract's rules 1 to 21 and the implied spec.
Three kinds of statement: *traced* (I read the code line by line for the case), *run* (a proof or a
probe printed it), *not verified*.

## The Rama behaviours that the plan said to check first

Each was *run*, in a probe module on a cluster of four tasks and two workers, before any code of the
gate was written. The probe is not in the repository. None was false.

1. A run of the gate that takes no thing commits what it wrote, and the run after a batch with things
   starts without Rama's sleep: `:closed-through` was on every task about 1.0 s after one append with
   nothing after it. The plan's mark on an internal depot is not needed, and is not built.
2. The things of one partition run in the depot's order, each as far as the first partitioner: 40
   things on three partitions got the positions 0, 1, 2 and so on in the order appended.
3. A PState read and write before an aggregation; a combiner of our own into a temporary PState,
   two levels deep; a read of it by a key, and whole.
4. `hash-by` on the depot and the hash partitioner give one task, for keywords and for a UUID.
5. The microbatch id is a Long, and its text is the same on a second attempt. The probe threw once in
   step 3: the batch ran again with the same id, and each thing got the same position, because Rama
   had set the count back.
6. A stream topology reads a PState of the microbatch topology on its own task, and the other way
   round. The gate that is added first names a PState that is declared later, and Rama takes it.
7. 1,000 things for a partition in a batch: read in the skill's reference, not run.
8. A lock's row written without `:bytes` reads back with the field absent.
9. Rows that one task sends to another through a partitioner are written there in the order sent:
   120 rows, 40 groups of three, each in order.

## The template's checks

- **Redundant conditionals.** None. The branches of `write-row` write to three PStates, and one makes
  a lock; the branches of step 2's read go to three PStates. *Traced.*
- **Consecutive keypath.** None.
- **Select, compute, transform.** Two: the count of turns and the gate's last stamp are read, a
  number is computed, and it is written. Each number is needed in the same event, for the turn and
  for the base, so the read cannot fold into the write. Every other write is a whole row at a key.
- **Unnecessary `nil->val`.** None.
- **`:allow-yield?`.** Found and fixed: the range read of a request's locks holds up to 256 rows. In
  the gate it now gives the task back (`lock-rows-in` became an operation of Rama for that), and so
  does the microbatch branch of `locks-of-request`. The streaming lines are as the first part built
  them. The loops of steps 2, 3 and 4 call `yield-if-overtime`. *Run* by the proof's batch of 400 acts.
- **Collections with no limit.** No row holds a collection. The sets of strays are subindexed.
- **Idempotency.** No stream topology is added. Every write of the microbatch gate is a PState write
  of one attempt, which Rama keeps or drops on all tasks together. No depot append, no TaskGlobal.
  The clock and a new lock's bytes differ between attempts; no read gives a row before its batch is
  closed. *Traced.*
- **One append for each client operation.** Yes.
- **Caches.** None. The count of turns and the last stamp are in a PState.
- **Built-ins written again.** None.
- **No throw on what arrived.** Each function that touches a thing is the first part's or
  `batch/first-entity`, which the pure proof gives ten things that are no acts. *Traced and run.*

## Against the plan

The depot, its partitioner, the topology, the four PStates and their homes, the four steps, the three
temporary PStates, the turn, the base and the last stamp, the four rules of who meets, closing, the
routing id, the three reads, the read of the other gate's record in both gates, the order of the
checks and each word are the plan's. No reason word and no word of a stray is new. The counts hold,
*traced*: 3 reads and 4 row writes for one accepted act with one sealed value on one entity.

**Where the code leaves the plan, and why.**

1. `locks-of-request` gives a used lock of a microbatch layer without its bytes also when the lock's
   home is its value's task, where the one row holds them
   (`record/locks-by-index-used-without-bytes`, one function more than the plan lists). The plan says
   that the read gives a used lock with no bytes, and its own path gave the bytes in that case: on a
   cluster of one task, for every lock. A read whose answer follows the hash of two ids is a fault.
2. Nothing else. A first draft ended step 2 before `|all` in a run with nothing to stamp or close; the
   plan sends it, so the draft's filter is removed.

**Smaller choices that the plan leaves open.** A place's question is sent for an own place too, and
each question gets one reply, as the plan words it. An entry carries the thing and the act; the
digest of an act is taken in step 3, of a thing that the declaration refused in step 1. The row with
bytes that the check of a seal is given for a lock of the same pass is drawn once for each act of a
pass. `batch/gather` is one combiner for the three temporary PStates.

## Against the spec

O2, X32 to X37, X41, X107, X31, C1 to C10 and rules 5, 6, 7, 10, 15 to 18 were each *traced* through
the four steps, and each is *run* by a proof but for what is listed below. The first part's functions
are unchanged: `decision.clj`, `declaration.clj` (one docstring), `first_act.clj`, `stamp.clj`,
`seal.clj`, `digest.clj`, `text.clj`, `act.clj`.

**Not met as written; each is in the module's README.** A select straight on a microbatch PState sees
a batch that is not closed. Two tasks do not open at one instant. On a second attempt of a batch the
10-second rule and the word for a layer of the other gate can differ. A rebuild from the microbatch
depot does not pick the same winner across two partitions (rule 9). F9's third limit is missed for
small values. An act whose `:who` holds no permission in a made layer costs up to 512 reads.

**Not verified.** A second attempt of a real batch of the gate, a commit that fails after one task
showed its part, and the three targets of speed and size. clj-kondo is not installed here, so no
linter ran; each namespace loads with no warning, and the proofs run the dataflow.

Verdict of this step: pass.
