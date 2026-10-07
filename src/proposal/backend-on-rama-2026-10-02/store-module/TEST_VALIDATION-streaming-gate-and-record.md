# Test validation

Part: `streaming-gate-and-record`. Stage: the build, step "validate the tests" (the Rama skill's
`phase-6-test-validate.md`). Written by the build's own session (Opus 5.5), 8 October 2026, for the
build session "softland-11".

Checked: the two proofs under `test-store/softland/store/` against the contract's "The proof", the
plan's "The proof's hand, and what the proof shows for this part", and the implied spec's edge cases
and matrix, as far as they hold for the class streaming.

- `pure_proof.clj`, `clojure -M:store -m softland.store.pure-proof`: no cluster, Clojure alone.
- `module_proof.clj`, `clojure -M:store-module -m softland.store.module-proof`: Rama's in-process
  cluster, four tasks, two workers.
- `act_proof.clj` is not changed: 150 passes, as before.

## The template's checks

- **Cluster launches.** One. The ten groups of the module's proof run on one cluster, in one order.
  They share layers on purpose: a give must come before the act under it.
- **Synchronisation.** Each append uses the ack level `:ack`, which waits for the stream topology;
  each read comes after it. No wait is skipped.
- **The namespaces load.** Both proofs ran.
- **Coverage of the implied spec.** Walked case by case. The result is below.

## What the walk found, and what was added

The first version of the module's proof passed, and the walk found six things that it did not show.
Each is added, as cases in the existing groups:

1. A place named twice, a fact that replaces a fact of its own act, two facts under one lock
   (C rule 12; IS X66, X67, X70). The plan lists the first among what the proof shows.
2. A value fact on the layer's own entity under a plain key (IS X73).
3. A repeat of a refused act after the record has moved so that the act would now pass (IS X103,
   IS C5).
4. A value that names nothing, onto a place that holds a retract (IS X65).
5. A request for locks that names something to replace (IS X100), in the pure proof.
6. The record after the module is started again (IS C8; IS X19, X99, X112). The plan had it as not
   reached. Rama's module update with the unchanged module drops the memory of the old instance
   and keeps the depot and the PStates; the proof then reads the same answer and the same locks,
   and gets a later stamp. It is not a worker that stops in the middle of an event.

## Where each thing is shown

- **The contract's fourteen lines for this part**: each on a `SHOWN` line of the module's proof, in
  the contract's words. The two others, the base and one act through the microbatch gate, need the
  next part.
- **Things that arrive** (IS X1 to X16): the words of a stray and of the declaration, one thing for
  each, in the pure proof; on the cluster, five strays, a thing that is no act, an unsealed value,
  facts as a list, each limit, and the digest of a thing read back from the depot's log.
- **The stamp** (IS X17 to X24, without X22): the pure proof, on numbers; on the cluster, the stamps
  of all answers increasing on each task, a carried stamp too far ahead and one 5 seconds ahead.
- **Layers** (IS X43 to X58, E1): the form of a making act, one act for each way of not having it,
  in the pure proof; on the cluster, the makings, a making for a made layer, the system's second
  act, a class fact after the making.
- **Places** (IS X59 to X74, E2): each row of the matrix in the pure proof, on rows built as the
  gate writes them; on the cluster, a value, a replace, a stale replace, a retract, a replace of a
  retract.
- **Permissions** (IS X75 to X89, E3): on the cluster, each case.
- **Locks** (IS X90 to X101, E4; IS X114): on the cluster, each state of a lock and each write that
  meets it, except a lock whose way is not known, which no act can make; the pure proof has it.
- **Answers and repeats** (IS X102 to X106, X108, X110 to X113, E5), **the reads** (IS X116, X118,
  X119): on the cluster.

## What no proof reaches

Said in `test-store/softland/store/README.md` too.

- IS X25, X27 to X29, X94: an ack that fails, a worker that stops in an event, a move of a leader, a
  record that Rama runs twice, a batch that fails. The in-process cluster gives no way to make one
  happen. What the code does then is traced in the implementation's paper, not run.
- IS X31, and everything of the class microbatch: the next part.
- A thing that a topology appends to another partition: no topology of the build does it.
- The three targets of F9: nothing is measured. The module's proof runs in about 15 seconds.

One choice to know: the cluster has four tasks, fixed, where the skill suggests a random count. The
proof shows that its layers are on more than one task, and prints which.

Verdict of this step: pass.
