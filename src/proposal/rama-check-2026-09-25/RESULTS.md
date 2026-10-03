# What ran, and what it showed (25 September 2026)

Rama 1.6.0, in-process cluster, 4 tasks, 4 threads, 1 worker, replication
factor 1 (the in-process cluster allows no other). Partition A is task 1,
partition B is task 2. Evidence: `run2.log` (the full run, `results.edn`),
`run3.log` and `run4.log` (exception classes), `run.log` (first run, which
died at its first injected failure; kept as evidence of that). Docs quoted
are the local extraction in `reference/rama/docs/` (generated 2026-03-22,
Rama version not recorded there).

Status words: **ran, held** / **ran, failed** / **ran, observed** (a race that
was caught) / **docs** (not run here).

## 1. Stream: one event writing to two partitions

- **Each partition's write is atomic on its own. Ran, held.** A throw between
  the append and the count on A discarded the append, the clock tick and the
  count together; after the retry A holds exactly one entry and count equals
  clock equals entries (s3). With retry `:none`, nothing of the event reached
  A at all (s1).
- **If the event fails between the two, A's write is visible and B's never
  happens. Ran, held.** s2 (retry `:none`, throw on B): A has the entry with
  stamp 19, count and clock advanced, B empty for that id. s5 (event held on
  B): a client read A's entry while B had nothing and the event had not
  completed.
- **The event is retried from the depot, at least once. Ran, held, with a
  mechanism the skill's summary does not describe.** Two paths:
  - Rama's own failure type (`rpl.rama.distributed.exceptions.IntentionalFailureException`,
    an internal class, not in the public API): clean in-process retry. A
    written twice (stamps 2 and 3), B once (stamp 3), no other record
    touched, nothing logged (run4).
  - Any ordinary exception (`ExceptionInfo`, `RuntimeException`; runs 2 and
    3): the batch fails and an in-process retry starts, and the same
    exception is classed "Unexpected throwable! Will be treated as a fatal!";
    about 80 ms later the worker is shut down and relaunched. The retry in
    flight is cut wherever it is. On relaunch the topology resumes from its
    last checkpoint and replays every record since, acked ones included.
    Net for one failed record: A written three times (stamps 4, 5, 8), B once
    (stamp 8); the already-acked baseline record replayed on both partitions
    with a new stamp. A client that appended with `:ack` got
    `CallbackException: Callback failure` for a write that landed (s4b).
- **A can see the write twice, with two different stamps. Ran, held.** The
  clock ticks once per attempt; B carries the last attempt's stamp.
- **A slow event is retried while still running. Ran, held.** The event-tree
  timeout here is 5 s (`stream-process-timeout {:timeout-seconds 5}` in
  run2.log). s5, held on B for longer, was retried from the depot five more
  times while the first attempt sat on B; A got six entries, B five; the
  first attempt's B write was discarded when its batch was force-failed
  after 30 s ("Detected stalled streaming batch").
- **A client read to a partition whose task thread is busy waits on that
  thread. Ran.** The read of B while B was held timed out its channel at
  10 s and reconnected.
- **One event's exception fails every event in that task's streaming batch.
  Docs** (11-stream-topologies.md, "Fault-tolerance and retry modes").

Consequences, one line each:
- The stream gate's answer-by-name must be per partition step: A's name row
  and B's name row, each written in the same event group as that
  partition's writes, and the retry must reuse A's recorded stamp rather
  than tick again, or B's copy carries a different stamp from A's record.
- A refusal is a value, never a thrown exception: an ordinary exception
  takes the worker down and replays every un-checkpointed record on it,
  acked ones included.
- The client's ack is not the answer; the model's "answers are durable,
  recorded where decided" is the shape that survives, because the client
  can get an error for a landed write.
- Gate work per event must stay well under the stream timeout, or the event
  runs twice at once.

## 2. Microbatch: one batch writing to two partitions

- **Nothing of the batch is visible before it commits. Ran, held.** With the
  batch held on B after A's writes, a client read of A showed only earlier
  batches (m2).
- **During commit, a reader can see one partition landed and the other not.
  Ran, observed.** 5,537 paired reads over 400 rounds; 85 times partition A
  already showed a later round than B read a moment later; the largest gap
  was 34 rounds, one batch's worth. Docs say the same
  (12-microbatch-topologies.md: "changes on different tasks may become
  visible at slightly different times").
- **Decided at prepare, made visible at commit, retried whole. Ran, held.**
  m1: throw on B after A's writes, worker died on the fatal path and
  relaunched, the same microbatch ID was re-attempted; A holds exactly one
  entry and the clock ticked once (2, not 3). Exactly-once held through a
  worker death.

Consequence: inside the microbatch store a cross-partition read has no
single moment; a name row on one partition can be visible before the fact
it answers for is visible on another, so "admitted" must not be read as
"readable now".

## 3. Stamps

- **A per-partition clock in a PState never goes backward. Ran, held.** In
  the stream topology it ticked 1 to 34 across every attempt and replay,
  never back, with no holes (a discarded attempt's tick is discarded with
  it). In the microbatch topology it ticked once per record, 1 to 5.
- **A microbatch event can read a stamp the stream topology wrote in another
  module. Ran, held.** A mirror PState read returned 100. A read of the
  stream topology's clock in the same module returned the committed value
  (29).
- **But it is a read at attempt time. Ran, held.** m3: attempt 1 read 100,
  the other module moved to 200 while attempt 1 was held, attempt 1 threw,
  attempt 2 read 200 and 200 landed. The gate learns what the other store
  holds now, not what the offer stood on when it was made.

Consequence: if an offer must record what it stood on, the offer has to
carry that stamp; the micro gate's read can only check it against what the
other store now holds, never recover it.

## 4. Failover

- **What the in-process cluster can do. Ran.** No replication (factor fixed
  at 1) and no way to kill a worker from the test API (its methods:
  launchModule, updateModule, destroyModule, pause and resume microbatch,
  waitForMicrobatchProcessedCount). `update-module!` with the unchanged
  module waited while an event was held mid-flight (did not return in 10 s)
  and returned about 4.5 s after release, so it cannot cut an event
  mid-flight. What the probe got instead, unplanned: seven worker crashes
  and relaunches from the fatal path in run 2. Through each, PStates
  persisted, the stream topology replayed from its checkpoint (at least
  once) and the microbatch topology re-attempted the same ID (exactly once).
- **A deposed leader. Docs, not run.** 21-replication.md: a leader
  replicates a replog entry to followers before making it visible, so what
  is visible stays visible after a leader switch; a new leader applies every
  entry in its replog before serving reads or running topologies; entries
  the old leader never applied are dropped, and the operation retries: a
  stream record from scratch, a microbatch whole with exactly-once.
  23-acid-semantics.md: reads on a PState never regress across a leadership
  change.

Consequence: the model's "answers are durable before they are given" rests on
"visible implies replicated", which is a docs promise here, not a run.

## Process note

The rama skill was loaded and its references read before any code. Its
multi-session phased build (spec, plan, validations, full-spec review) was
not run; this is a probe, and the skill's "use judgment for simpler tasks"
clause was applied, with the plan in README.md instead.
