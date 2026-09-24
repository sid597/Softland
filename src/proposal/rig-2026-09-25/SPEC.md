# The rig's spec

This is the user-facing spec for the rama skill's process (its `phases.md`).
Every phase of that process reads this file and the sources it names; they
are the only spec. Paths are relative to the worktree root,
`/mnt/data/projects/Softland-rig-2026-09-25`. The rig's own folder is
`src/proposal/rig-2026-09-25/` (below: "the rig folder").

## What the rig is

Softland's store is to be built on Rama. Its rules were ruled by Sid in chat
and a formal model of them holds. The rig is a throwaway Rama build of those
rules, whose records nobody keeps, built to learn three numbers and one count
before the real store is written. It runs on the in-process cluster only. It
is not the server: nothing under `src/app` is read or touched.

**Where the rulings are silent, pick the simplest thing and write the pick
down as a rig choice, not a ruling.** Rig choices live in the rig folder's
`RIG.md`, numbered R1, R2, and so on. A plan or build that makes a new pick
names it there.

Vocabulary, fixed by Sid on 24 September: "key" is a fact's key, the "in
this respect" part. "Lock" is an encryption key. Never one word for both.

## The sources that are part of this spec

Read these; they bind. Line ranges are for the files as they stand on the
branch.

1. **The rulings.** `src/proposal/frame-2026-09-15/PROGRESS.md`, the whole
   "Now" section, lines 9 to 195: the store-level rulings (42-65), the nine
   (67-107), the layer kinds (109-112), the two consequences (114-118), the
   sharpenings (120-165), the rig constraints (167-170), and what stays open
   (172-186). The rig does not change this file.
2. **The model's account.** `src/proposal/formal-model-2026-09-24/README.md`,
   the sections "What is in the toy", "Order between the two stores" and
   "The readings" (lines 42-152).
3. **The executable spec of the gates.**
   `src/proposal/formal-model-2026-09-24/src/formal/model.clj`. It says how
   an offer is placed, refused, stamped, admitted, locked, forgotten,
   promoted and read. **The rig's gates must decide the same way.** The
   configuration the rig implements is the model's `baseline` (every entry
   of `readings` turned), because every entry there is Sid's. The `ruled`
   configuration exists for comparison only.
4. **Fixed histories with known outcomes.**
   `src/proposal/formal-model-2026-09-24/src/formal/scenarios.clj`: the A,
   B and D cases. The model can be run (its folder has its own deps.edn) to
   get its answers for any history.

Where this file summarises a source, the source wins.

## Sid's instruction, verbatim (25 September 2026)

The phases below are Sid's words. Phase 0 is done (see "What Rama showed").

> 1. The stream store. A depot for offers, a stream gate topology, the
>    envelope as ruled (small positional core, the named parts, a version
>    marker, unknown parts refused), one-owner layers placed by layer, N tasks
>    fixed at launch. Per task: the log, answers by name, the partition's
>    stamp. Names random, made by the offerer, tagged with layer and class;
>    the class checked against the layer's class fact; a reuse with different
>    content refused by digest; a retry answered from the record; acts whole
>    or not at all; layer on the act. Tests: the model's stream cases.
>
> 2. Locks and forgetting. Real encryption, kept simple: a small symmetric lock
>    per value at write, wrapped under person locks as ruled, owner required
>    in one-owner layers, 7b among the other subjects, marks honoured. Lock
>    rows in a lock store for personal and hand layers, on the same task as
>    their values; in the record for agent layers. Value forget deletes the
>    row; person forget destroys the person lock; every forget is a fact; time
>    travel shows "erased on this date" and nothing else from after its
>    moment. Tests: the model's A cases, both directions.
>
> 3. The micro store. A microbatch gate for group and base layers placed by
>    entity, atomic across tasks, with a name row written in the same batch so
>    an answer is found by name plus layer. Wrapped locks in the record.
>    Permissions as facts in the layer they govern, checked only by the gate
>    that orders that layer; a revoke; an offer citing a permission from
>    another layer refused. Re-class of a hot layer moves it here. Tests: the
>    model's micro and permission cases, and the revocation race.
>
> 4. Promotion. A request act in the owner's layer; the read-out opens the
>    value through its lock on the owner's task and writes the crossing fact
>    there; a landing offer named from the request under a scheme reserved to
>    the store; pending with two states; a forget before the read-out refuses,
>    after it does not recall. Tests: both sides of the read-out line, a
>    retried request landing once, a failover between forward and landing.
>
> 5. Reads and read entries. Point reads and pattern reads; one read entry
>    line per pattern read carrying pattern, moment, role and a keyed
>    fingerprint of what matched, with a complete-or-partial mark; eager by
>    default in personal and shared layers. Every index over values rebuildable
>    from the log or purgeable by value id, so a forget reaches it.
>
> 6. Tools and grammars from inside. Keys are ids; a key's grammar is facts
>    about it, including which subjects its values name and whether they are
>    opaque. A tool is facts too: what it matches, its signature, its read
>    entry preference; a minimal runner finds tools by matching. Then the
>    count: add one new tool and one new grammar by writing facts only, and
>    count how many new compiled steps were needed. Zero is the target. Report
>    the count and every step that was needed, with why.
>
> 7. The three numbers, each with its method and the machine it ran on:
>    index writes per second with an agent session layer writing small acts
>    continuously; lock store growth under hand layers, bytes per value and
>    the curve over a hundred thousand values; one person's layer on one
>    thread, acts per second and latency.
>
> 8. Where practical, replay the model's fixed histories from scenarios.clj
>    through the rig and compare answers and reads with the model's. Report
>    every difference; a difference is a finding, not something to hide.

Each phase ends with its tests run, `RIG.md` rewritten, and a commit.

## What Rama showed in phase 0 (binding on the design)

From `RIG.md`, phase 0; each ran on the in-process cluster with Rama 1.6.0.

1. A stream event is atomic on one task: its writes commit together at the
   next partitioner or at the end of the event, or are discarded together.
2. After a crash a stream record replays from its start, at least once, and
   possibly more than once. Records that had already completed since the last
   checkpoint replay too. Writes committed on other tasks before the crash
   are applied again on every replay. Every gate write must therefore be
   idempotent under replay, and a replayed offer must get the same answer.
3. An exception thrown in topology code is fatal to the worker: the worker
   shuts down and restarts. **A gate never throws on an offer; every refusal
   is data.**
4. The client can get an error for an offer that went in. An offerer resends
   under the same name and takes the answer from the record.
5. A microbatch is decided before it is visible and leaves no partial state
   across tasks. It is not visible on every task at one instant: each task
   makes its part visible when its own commit finishes, and a reader can see
   a batch on one task and not yet on another. **Nothing may rely on seeing
   a microbatch on every task at once.** Rig choice R5: the micro store is
   read through a settled frontier, so a reader never sees half a batch; the
   design of the frontier is phase 3's.
6. A microbatch topology can read another module's PState through a mirror,
   and a PState of another topology in its own module. Such a read is a read
   at a moment, not atomic with the other side's writes.
7. The next microbatch starts only after every task has committed the
   current one.

## Rig choices already made (from RIG.md)

- R1. The branch lives in a sibling worktree.
- R2. Phase 0 was a check, not a module; the store follows the skill's full
  process.
- R3. Failure injection in tests uses global atoms (in-process cluster only).
- R4. Tests assert "at least once", never an exact replay count.
- R5. The micro store is read through a settled frontier (phase 3 designs it).
- R6. Whether the micro gate reads what an offer stood on in the other store,
  or the offer carries the stamp, is phase 3's pick.
- R7. "The model's stream cases" (phase 1's tests) means the stream gate's
  decision branches in `model.clj`: admit an act whole on its layer's home;
  refuse on its face a name made for another layer or class; refuse a reused
  name with other content by digest; answer a retry from the record; refuse
  a fact outside the act's layer, a class mismatch, a permission that does
  not cover the write, lives in another layer, does not exist, or is revoked,
  and a stale or doubled replace; stamp by the clock promises; and the same
  answer after a crash mid-offer. `scenarios.clj` has no stream-only set.
- R8. "The model's A cases, both directions" (phase 2's tests) means every
  A case checked both ways: what must be erased is erased, and what must
  stay open stays open, for the one-owner and the shared layer alike.
- R9. The decomposition into build stages is Sid's phases 1 to 6, in his
  order; phases 7 and 8 measure and replay what the stages built.

## What the rig is not

- Not the server, not durable, not deployed. The in-process cluster only.
- Not a place for new rulings. A rig choice never changes `PROGRESS.md`.
- Not bound to the model's toy world. The model has two people, four
  entities, two fact keys and five layers; the rig takes layers, people,
  entities, keys and tools as data. Its tests use the model's world so the
  answers can be compared.

## Tests and evidence

- Tests are `clojure.test` namespaces under the rig folder's `test/`, run
  with `clojure -M:test <ns> ...` from the rig folder.
- Every claim that a capability works says what was run and what it showed.
  A claim that was only reasoned says so.
- Measurements (phase 7) state the method, the machine and the run. The
  in-process cluster is not a proxy for production latency or throughput;
  only orders of magnitude mean anything, and the numbers say so.
