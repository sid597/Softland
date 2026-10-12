# Plan: answers in a log — each gate keeps what it decided in a depot of its own

**What this paper is.** The plan of the part `answers-in-a-log` of the build "before real data is kept":
the contract's B1, B2, B6 and the first half of B5 (`../BUILD-before-real-data.md`), what the logs must
hold of a stray and of a batch of the base, and the measures of D with the code that measures. It designs
how each gate keeps what it decided in a log as well as in its tables, and says exactly what those logs
hold, because the part after this one, `the-rebuild`, reads nothing else. It writes no code of the store.
**The stage.** Phase 1 of the Rama skill (`phase-1-plan.md`), for the second part of three
(`DECOMPOSITION.json`, the entry `answers-in-a-log`).
**Who wrote it.** One fresh session (Opus 5.5), in the night of 12 to 13 October 2026, for the build
session to judge and a fresh session to check. It wrote this file and the folder
`plan-probes-answers-in-a-log/`, and changed no other file. The tree was at head `6d9a6a8d`; the line
numbers below are of `src-store/` and `test-store/` as they are there, before the part `the-locks-go`.

**Blocked.** Nothing. One tool call was refused, a tidy of the probe folder that held an `rm`; the folder
keeps its `.cpcache/`, as the stream probe's does.

**What it takes from `the-locks-go`, and where it would differ.** One thing, as ruling 19 gives it: a
thing on a log of acts is the maker's map with the door's two additions, `:act-id` and `:depot`, every
value plain; the module has no table of locks, no request for locks and no seal. The plan is written on
the code as it is today with that one change in mind. If the form came out otherwise, three things here
change and no other: the counts of reads and row writes in "F. What it costs" (they leave out the read
and the write of a lock's row); which lines of the explanations still stand when this part replaces them
("I"); and which kinds of row `decision/writes` gives. What an entry holds does not change: it is the
act-id, the word, the stamp and the gate's own digest of the thing as it came off the depot.

**How the skill's template is adapted.** The template plans a module. This part changes the two gates of
a module that stands, and adds two depots and two fields of one table. Every heading of the template is
kept and answered for what the part adds or changes. "Reads" are the reads that the part touches, and the
one reader that it serves, a rebuild, which is no read of the store. "Writes" are the new appends. Eleven
sections, A to K, are added because the task asks for them; where the template and a lettered section
would say one thing twice, the template's section is short and points at the letter.

**Marks.** "Read at" is read in the source at that line. "Run" names a run and its log: **S**, **M** and
**O** are this plan's three (`plan-probes-answers-in-a-log/RESULTS.md`); "the stream probe" is
`stream-probe/RESULTS.md`; "the batch probe" is `../reads/batch-probe/RESULTS.md`. "Read, not run" is a
line of Rama's pages under `reference/rama/docs/`, whose words are those of Rama's docs while 1.6.0 was
the newest release (`reference/rama/docs/README.md`); the release note of 1.6.0 says of a microbatch
"eliminating overhead in priming and commit phases and initiating replication of PState changes during
processing phase" (`reference/rama/release-notes.md`), so a line of those pages on when a batch's rows
replicate or commit is marked "read, in docs that describe the release; the note for 1.6.0 says the
commit and replication of a microbatch changed". "Reasoned" is derived here from lines that are named.
A file name without a folder is under `src-store/softland/store/`. "Entry" is the contract's word for
what a log of answers holds of one answer; in code this plan calls it an answer record, because
`gate/batch.clj` already uses "entry" for what step 3 needs of a thing.

## What was run for this plan

Three runs, in `plan-probes-answers-in-a-log/`, on Rama 1.6.0 in-process, launched as the store's proofs
launch their cluster: 4 tasks, 2 threads, 2 workers. Each is a probe: its module stands in for a gate and
requires nothing of the store's tree. Their page gives every line cited here.

- **S**, the streaming shape that this plan takes: a first read of whether the key has a row (the gate's
  check (a)); when it has none, the writes; then a commit boundary and an append with `:append-ack`; when
  it has one, no write, the boundary, and an append of a copy of the row. A failure was forced at each
  point of the event. The stream probe ran the append in twelve ways on an event that always writes; it
  did not run this shape.
- **M**, the microbatch shape that this plan takes: what a batch decided is put into a table of the
  topology in the attempt that writes it, on one task for the batch; the next run appends it and takes it
  out. A failure was forced in the attempt that writes and in the attempt that appends. An earlier form,
  in which each task kept and appended its own, ran too, with the same outcomes.
- **O**: Rama's setting `topology.stream.max.executing.per.task` set to 1 from outside a module's own
  code, and what it does to the events of one task.

## The design in short

1. **Two logs of answers, one for each gate**: the depots `*streaming-answer-depot` and
   `*microbatch-answer-depot`, each declared by its gate's own code, each closed to a client's append, each
   with no trimming option. No topology consumes either. Nothing reads them but a rebuild.
2. **An entry is the answer's row with the act-id**: `{:act-id :stamp :digest}` and `:refused` with the
   word when the thing was refused. No value of a fact is in it. A stray has an entry of its own,
   `{:stray :digest :task}` (ruling 8). A record of either depot is a map of entries:
   `{:answers [...]}`, `{:strays [...]}`, and in the microbatch gate's depot also the batch's id, and with
   the batch's first record its clock and its last stamp.
3. **Both gates keep an answer only after its row is committed.** The streaming gate: its reads and its
   writes as today, then a commit boundary, then the append (ruling 27). The microbatch gate: in the
   attempt that writes an answer's row it also puts the entry into a table of its own, and the next run
   of the gate, which begins only when that batch is committed on every task, appends it. That next run
   is the run that closes the batch for the door's reads.
4. **So no entry of an attempt that did not count exists.** What a failure can leave is a second, equal
   copy of an entry that counted. The rule for a rebuild: every entry under one act-id holds the same
   answer; take one.
5. **No answer stays in the tables for good with no entry.** The streaming gate appends a copy whenever
   an event finds its act-id answered, since that event may be Rama's second run of a record whose append
   was lost. The microbatch gate's table of what is to keep is set back and committed with the answers'
   rows, so the two are there together or not at all.

What it costs one accepted act of the streaming gate: no read and no row write more; one commit boundary
and one append that is waited for, about 0.8 ms more on the maker's call on one machine (the stream
probe), and one record of about 135 bytes for ever. What it costs a batch of the base: one small write in
its own attempt for each task that wrote an answer, and in the next run one read and one append for each
256 answers, about 3 ms each on one machine (M).

## Reads

This part adds no read of the store and changes none of the door's or of the read path's.

| Read | Who | What changes |
|---|---|---|
| `answer-by-act-id`, `depot-of-layer`, the read path's selects and pushes | the door, the read path | Nothing. They read the tables (B2, R3). No client of the store opens a depot of answers. |
| The gate's own read of its record before it decides, check (a) | each gate | The read is the same. In the streaming gate what follows a hit changes: the event still writes nothing, and now appends a copy of the answer that it found (A, B). |
| The head of step 2 of the microbatch gate, on the gate's task | the microbatch gate | It reads `:to-close` where it reads `:closed-through` today. Two reads, as today. |
| The closing's read of what the batch before left to keep | the microbatch gate, in the run that closes a batch, on one task | New: one range read of `:to-keep`, the keys of one batch. One seek and one step for each key. |
| A depot of answers, from its start | a rebuild, which is a client | New, and not a read of the store: Rama's client reads a partition of a depot by its offsets (E). |

Latency and throughput. The streaming gate serves what a person does directly, so the keeping must not
hold its task: nothing that lets go of the thread stands between an act's reads and its writes (ruling
27), and the append is one for an act. The base is the microbatch layer, "The base's second is fine"
(decision 8 of 11 and 12 October), so its keeping may wait for the next run.

## Writes

| Write | Depot | By whom | Record |
|---|---|---|---|
| An answer of the streaming gate | `*streaming-answer-depot` | the streaming gate's event, behind a commit boundary, `:append-ack` | `{:answers [entry]}` |
| A stray of the streaming gate | the same | the same | `{:strays [stray entry]}` |
| What a batch of the base decided | `*microbatch-answer-depot` | the microbatch gate, in the run that closes the batch, on the batch's keeping task, `:append-ack` | `{:batch id :answers [≤ 256 entries] :strays [...]}`; the first record of a batch that made a stamp also holds `:clock` and `:last-stamp` |
| A batch's clock | `*batch-clock-depot` | the microbatch gate, as today | unchanged |

No client appends to a depot of answers: each is declared `:disallow`, as the depot of batch clocks is
(ruling 15). The two depots of acts take the same appends as today.

## PState design

No new table. Two fields more in `$$microbatch-task` (`record/tables.clj:179-189`), and one field read no
more at the head of step 2.

`:to-keep`, on the keeping task of a batch: what the tasks sent to keep of that batch, until the next run
has appended it.

```
:to-keep  (map-schema String
            (fixed-keys-schema
              {:batch   Long
               :answers (vector-schema (fixed-keys-schema {:layer String :uuid UUID :stamp Long
                                                           :digest bytes :refused Keyword}))
               :strays  (vector-schema (fixed-keys-schema {:stray Keyword :digest String :task Long}))})
            {:subindex? true, with size tracking off})
:to-close (fixed-keys-schema {:batch Long :clock Long})        on the gate's task only
```

- **The key of `:to-keep`** is a text: the batch's id in 19 digits, the number of the task that sent the
  value in 5, and a part number in 5, with a space between. So the keys of one batch are neighbours, one
  range read gives them, and two tasks never write one key. A text and not a packed number: the id of a
  batch goes up with every run for as long as the module lives, and a packed number would bound it.
- **A value holds at most 256 entries**, answers and strays together. That is a number of the build, held
  by the pure function that makes the values, so the two vectors are small by the code and are not
  subindexed. The map is subindexed: a batch can hold many values, and each is read once and taken out.
  Size tracking is off: nothing asks its size, and tracking costs a read at each write (the skill,
  `pstate-schema.md`, "Size Tracking").
- **An entry is held by its layer's text and its uuid**, as each row of the record holds an id as its one
  text, so every position has a class and none is `Object`. `rows/id-of-text` gives the id back
  (`record/rows.clj:507-520`), and the answer's key is made from the same two (`answer-key`, `:170-178`).
- **`:to-close`** is the id of the newest batch that left something to keep, with the batch's clock when
  it made a stamp. It is the mark by which the next run knows that it closes a batch, and where the
  closing takes the batch's clock from.

Why this and not another form:

- **Option A, taken: values of up to 256 entries, one for each sending task and part.** A batch of n
  answers on T' tasks costs T' or a few more puts in its attempt, and the same number of removals in the
  next run. Read: 1 seek, T' steps.
- **Option B: one key for each answer**, written beside the answer's row. The code is one line in the
  write of an answer. It costs n puts and n removals for a batch where A costs about T'. Read: 1 seek, n
  steps. At 1,000 acts a second that is 2,000 small writes a second more than A, each replicated. It
  loses on write volume alone; its reads are the same.
- **Option C: no table; append inside the attempt that decides.** No write at all. It loses on what it
  leaves in the log, not on cost: "A. The microbatch gate, three designs".

Finding 16 binds any table that this part changes: no table that a read is kept live on may hold a byte
array. `:to-keep` holds the digest as bytes; nothing is kept live on `$$microbatch-task`, which the read
path does not open (`read_path.clj` opens the four tables of facts), and the door's reads select
`:closed-through` alone.

## Depots

- `*streaming-answer-depot`: `:disallow`. Declared in `module/streaming_gate.clj`, by the gate that
  appends to it. A record sits on the partition of the task on which the gate decided, because an append
  from a topology goes to the partition of the task that the code is on (read, not run,
  `14-depots.md:254`; run, S and the stream probe's question 5) and the streaming gate's event never
  leaves its task.
- `*microbatch-answer-depot`: `:disallow`. Declared in `module/microbatch_gate.clj`. A record sits on the
  partition of its batch's keeping task. That partition says nothing of the acts in it.
- `*streaming-depot`, `*microbatch-depot`, `*batch-clock-depot`: unchanged.

No trimming option is set on any of the five (B6, R2, entry 3), and no migration of a depot is written.
Each gate's `add-to` takes the module's `setup` beside its `topologies`, so that the gate declares its
own depot of answers (ruling 18, its last sentence); `module.clj` passes both and names neither depot in
code.

**The skill's check of an internal depot** asks why the consuming topology cannot consume the
client's depot. No topology consumes these two. They are justified by the check's second ground: they
carry what is in no client's depot, the answers. Sid's entry 1 asks for exactly that.

## Topologies and PStates

No topology is added. The module keeps its three: `streaming-gate` (stream), `microbatch-gate`
(microbatch), `batch-clock` (stream).

**`streaming-gate`: stream, as built.** The reason is the one it has: the maker's append waits for the
decision. Its writes, each with its answer to a second run:

| Write | A second run of the event |
|---|---|
| Each row of a decision: a whole row at a fixed key, or a fixed key taken out (`module/streaming_gate.clj:324-347`) | Behind check (a): a second run after a commit writes none. As today. |
| The stray's digest into the set of its word (`:201`) | The same write however often it runs. As today. |
| **New: the append of the entry** | Not the same: each run that reaches it appends one record. It does no harm: an append is made only behind the commit of the answer that it copies, so each record under one act-id holds the same answer, and a reader takes one (B, C). This is B5 and ruling 12, which the skill's rule on a duplicate side effect asks the plan to show. |

The skill's check of a stream topology: a commit boundary, `(|direct (ops/current-task-id))`, stands
right before the one `depot-partition-append!` of this topology. It stands behind the gate's writes
(ruling 27).

**`microbatch-gate`: microbatch, as built.** It gains two fields of its own PState and one more
`depot-partition-append!`, in step 2, on the keeping task of the batch before. A second attempt of a
batch appends again: the same records, read from rows that the batch before committed (B).

**`batch-clock`: unchanged.** Its source keeps Rama's default retry mode. Ruling 9's third point asks
whether that mode must change so that the order of the depot of batch clocks is the order of the
keeping; this plan does not need it (D, K1).

## Query topologies

None is added and none is changed. `answer-by-act-id` reads the answers' table and, for a microbatch
act-id, `:closed-through` (`module/door_reads.clj:99-114`); it reads no depot.

## Partitioning efficiency

The part adds one read of a table, the closing's. Its placement first: what a batch left to keep should be
on one task, so that one read finds it, and on another task for another batch, so that neither the reads
nor the depot's records gather on one task. That is `f(batch id) → one task`, and the partitioner is
Rama's hash of the batch's id, the route of a batch's keepings. The table is for one run of the gate.

| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A run that closes no batch (a quiet run, or the batch before took nothing) | 1 − p | 0 | 0 |
| A run that closes a batch whose answers are on T' tasks | p: near 0 in a quiet time, near 1 under a steady load, where each run closes the one before | 1 | T', or a few more when one task wrote more than 256 answers |

N = 1 task: weighted seeks p, iterator reads p. N = 16: weighted seeks p, iterator reads at most 16 p.
N = 128: weighted seeks p, iterator reads at most 128 p. The seeks are flat. The steps grow with the
tasks that wrote an answer in the batch, at about 5 µs each, and never past the number of tasks.

The earlier form of M, in which every task keeps its own, costs N seeks for each closing, and at least
T' appends where this form makes one for each 256 answers: 128 seeks and up to 128 appends for a batch on
128 tasks. That is why it was put aside (A). **One thing is not run**: that the hash of a batch's id
spreads batches evenly over tasks. In M six batches fell on two tasks of four, which says nothing either
way. The build counts it over a few hundred ids before it writes the closing ("J"); where the spread is
poor, the route becomes the id modulo the number of tasks, by Rama's direct partitioner.

The streaming gate adds no read, so it has no table here.

## Design decisions

- **Subindexing.** `:to-keep` is subindexed; its values are bounded at 256 entries by the code. Nothing
  else is added.
- **Colocation.** An entry of the streaming gate is appended on the task that decided: no hop. What a
  batch keeps goes to one task by one more send from each task that wrote an answer, in the step that
  already sends each row to its home (step 4).
- **The gate's task.** `:to-close` is on the gate's task, with the gate's last stamp, which the closing
  needs with it.

## State primitive selection

- `*streaming-answer-depot`, `*microbatch-answer-depot` (depots): the logs. Durable, replicated, never
  trimmed. One record for each answered thing or for each 256 of a batch: bounded by what arrives.
- `:to-keep` and `:to-close` in `$$microbatch-task` (PState): durable, and set back by Rama with the
  answers' rows when an attempt fails, which is the property that the design rests on. A value for each
  256 answers of a batch, removed one run later: bounded by the batch.
- No TaskGlobal, nothing in the memory of a process, no store outside Rama (R5). A second attempt of a
  batch, and a second run of an event, can come after a worker was started again, and memory does not
  outlast that (the batch probe, B3.iii; the stream probe, 1.1).

## Resource usage analysis

Counted in "F. What it costs". In short. Disk, for ever: one record of 131 to 172 bytes for each answered
thing of the streaming gate, one more for each later run of its event, and one of about 110 bytes for
each arrival of a stray; in the base about 60 to 130 bytes for each answered act and 55 for each batch
that made a stamp (O, "SIZE": what Nippy makes of such a record; Rama's own writing was not measured).
Disk, for one run: `:to-keep`, about 110 bytes for each answer of the newest batch, gone when the next
run commits. Memory: none. Minimal: an entry holds the four things of B1 and no more; the task of a stray
is the one part beside them, and E says why it is needed.

## A. The logs of answers

### One log, or one for each gate

Taken: one for each gate, each declared by its gate's code (ruling 18).

- **One for each gate.** A gate appends where it decides or keeps, so nothing hops for the log. A later
  gate with a depot of acts of its own brings its own depot of answers and touches no code of the others
  (R6). The rebuild is handed a list, each part a gate with its log of acts and its log of answers
  (ruling 18).
- **One for both.** One name less. Each record would have to say which gate wrote it, the rebuild would
  split one log by that word, and the shared code would name a pair. A streaming entry sits on the task
  that decided, which the rebuild uses (E); in a shared depot that partition would also hold the base's
  records, which mean nothing there.

It costs nothing in I/O either way: the count of appends is the same. Which choice cannot be undone
later is in "I".

### Who may append, and the ack level

Only the gate that owns the depot: each is `:disallow`, and a client's append is refused ("Appending to
this depot is disallowed": the stream probe, question 5, seen in a trial whose log was not kept; N7 of
the proofs holds it on the store). The ack level is `:append-ack` in both gates:
the append returns when the record is on the depot's partition and replicated (read, not run,
`14-depots.md:206`). Not none: with no ack level an append was lost in 4 of 4 failed events (the stream
probe, 1.3; ruling 28). Not `:ack`: nothing consumes these depots, so `:ack` would wait for the same
thing today (`14-depots.md:206`), and on the day that a stream topology read one of them `:ack` from the
gate that appends would wait for it, or never come back (the stream probe, D). `:append-ack` says what
the gate waits for and keeps the log free of any reader's pace.

### What an entry holds, and for which things

```
an answer   {:act-id [layer depot uuid]   the act-id under which the thing arrived
             :stamp  n                    the stamp of the decision
             :digest bytes                the SHA-256 of the thing as it came off the depot, 32 bytes
             :refused word}               only for a refusal: the word
a stray     {:stray word :digest hex :task n}
```

An answer's entry is the answer's row (`rows/answer-row`, `record/rows.clj:201-208`) with the act-id
beside it: "the act it is for, refused or not with the word, the stamp, and the digest of the act", and
"No value of a fact is in it" (B1). The act-id is held as data and not as the text of the answer's key,
so that a rebuild makes the key with `rows/answer-key` of the day; "I" says what that keeps open.

The five kinds of thing that arrive (implied spec 5.2, "Which decisions get an entry"):

| The thing | The streaming gate | The microbatch gate |
|---|---|---|
| A thing that the declaration refuses | An answer's entry, with the declaration's word | The same |
| An act that a check refuses | An answer's entry, with the word | The same |
| An act that is accepted | An answer's entry, with no `:refused` | The same |
| A repeat, or another thing under a used act-id | **A copy of the entry of the answer that the act-id has.** The gate decides nothing and writes no row. It cannot tell a second record from Rama's second run of the first, and after a lost append the second run is what brings the entry (B, row S3) | Nothing. The entry is put with the answer's row, in one attempt, so there is no lost append to make good |
| A stray | A stray's entry at each arrival (ruling 8) | The same, at each arrival |

The copy of a repeat departs from the implied spec's sentence that "a stray and a repeat have nothing to
keep". Ruling 8 changed that for a stray. For a repeat it is this plan's choice, and K2 puts it to the
build session with the other roads.

### The streaming gate, three designs

Each is whole. "The event" is the walk of `module/streaming_gate.clj:194-347`.

**S-A. The append in the one group of the writes: write, then append, no boundary.**
- Under failure. The append is acked before the event's batch commits. So no answer can commit without
  its entry: the first property of ruling 12 holds by construction. But a batch that fails after the
  append leaves an entry of an answer that the tables never hold (run: the stream probe, F1 and F2 with
  no boundary, `1 · 1 · nil`), and the retry decides anew with another stamp, and can give another
  verdict (implied spec, edge 3). A reader of the depot is given the record before the row, 165 to 185
  times of 200 (the stream probe, 2.1).
- What one act pays. One append that is waited for: about 1.0 ms in the event, 0.8 ms on the client's
  call. A repeat pays nothing. A replay of n answered records adds nothing.
- What the rebuild must do. Read each partition in the order of its offsets and take, under each act-id,
  the last entry. It rests on a failed attempt's append always landing before the retry's, which is
  reasoned and was not run for an append that is still on its way when Rama fails a stalled batch.
- Why it loses. It is the cheaper design by one boundary and by the copies. It loses on three things. The
  log would hold answers that no table ever held, so R5's "a reader never learns of a decision from the
  log first" would hold only because nobody reads. Differing entries under one act-id would be the normal
  state, so the rebuild could not tell a fault of the log from a failed attempt. And the skill's check of
  a plan fails it without a look: "ALWAYS add a commit boundary before `depot-partition-append!`"
  (`stream.md:49`; `artifact-plan-validation.md:63`).

**S-B. Write, commit boundary, append; and a copy on every hit of check (a). Taken.**
- Under failure. An append is made only in the part of the event behind the boundary, and that part runs
  only when the batch that holds the event's writes is committed (read, `stream.md:41`; run: S1, where
  the attempt that threw before the boundary left no record; the stream probe, F2 with a boundary, 6 of
  6). So every record under an act-id is a copy of a committed answer's row, and that row is written
  once. A failure between the commit and the append leaves an answer with no entry (run: S2); Rama runs
  the record again, the event finds the answer, and appends its copy.
- What one act pays. One boundary and one append: about 2.1 ms in the event, 0.8 ms on the client's call
  (the stream probe, question 4: 2.76 to 3.59 ms). A repeat pays the same, where today it pays one read.
  A replay of n answered records adds n records.
- What the rebuild must do. Group by act-id and take one. It needs no order of the log.

**S-C. S-B, with a mark that the answer is kept, in place of the copy on every hit.** The event writes a
mark beside the answer's row; behind the append it takes the mark out; a hit of check (a) reads the mark
and appends only when it is there.
- Under failure. As S-B. A failure between the append and the removal of the mark still leaves a copy at
  the next run, so the rebuild's rule is the same.
- What one act pays. Two row writes more for every accepted and every refused act, the second in a batch
  of its own that must commit before the client's append returns; and one read more at each hit.
- Why it loses. It makes every act pay two writes and a second commit so that a repeat and a replay pay
  one read where S-B makes them pay an append. Acts are the common thing; a replay is n records for one
  restart, and a repeat is a sender's second send. And it does not remove the copies, so it buys no
  simpler rule.

Not weighed as a design of its own, because R3 or the tree forbids it: a second topology that reads the
answers and appends for the gate (it would race the gate, or read the gate's log); a client's append
with no wait for the keeping (decisions 6 and 7 of 7 October are of the answer, and ruling 28 of the
ack).

### The streaming gate after the change, step by step

For one thing, on the task of the depot's partition.

| # | Step | Reads | Writes | Appends |
|---|---|---|---|---|
| 0 | The test for a stray (`:198`). A stray: its digest into the set of its word | — | 1, the set's add | — |
| 1 | Check (a): the answer under the act-id (`:210`). When one is there, the event decides nothing | 1 | — | — |
| 2, 3 | The digest, the declaration's check, the clock, the 10-second rule | — | — | — |
| 4 | Standing | 1 to 4 | — | — |
| 5 | Places | one for each place needed | — | — |
| 6b | The marked keys, and the rows before | 0 to 1, and one for each place under a marked key | — | — |
| 7 | The last stamp, the stamp, and each row of the decision (`:311-347`) | 1 | each row; the answer and the last stamp among them | — |
| **8** | **The commit boundary: `(|direct (ops/current-task-id))`.** The event gives its task back here, after its last write and never before | — | — | — |
| **9** | **The entry**: for a decision, from the act-id and the answer's row that step 7 wrote; for a hit of check (a), from the act-id and the row that step 1 read; for a stray, its word, its digest and the task. **One append to `*streaming-answer-depot`, `:append-ack`** | — | — | 1 |

Steps 0 to 7 are the event as it is, with one change of form: today the rows are written by a step that
runs once for each row (`ops/explode`, `:325`), and what stands behind it would run once for each row.
The writes become a loop that ends, as the microbatch gate writes its rows
(`module/microbatch_gate.clj:524-535`), and steps 8 and 9 stand behind it once, for all three roads.
Nothing between an act's first read and its last write lets go of the thread (ruling 27); the gate's rule
that no other event of the task runs between this event's reads and its writes holds as it does today.
Behind step 8 the event reads nothing and writes no row.

### The microbatch gate, three designs

**M-A. Append inside the attempt that decides**, on each task at the end of step 4.
- Under failure. The append is not set back when the attempt fails (read, `12-microbatch-topologies.md:130`;
  run, the batch probe, B3.ii), so each failed attempt leaves its entries. The kept clock makes them
  equal to the next attempt's, but for one word: an act into a layer of the other gate can get the other
  of two words (`module/microbatch_gate.clj:180-185`); and after a module update between two attempts
  they can differ in anything.
- What one batch pays. No row write. One append on each task that wrote an answer, inside the attempt.
- What the rebuild must do. Take the last entry under each act-id in the order of its partition, or tell
  the batch's attempts apart by a mark that the next run writes. Two rules for two gates.
- Why it loses. It is the cheaper design, by the writes of `:to-keep` and by one read for each closing.
  It loses because the log would hold a word, or after an update an answer, that the tables never held,
  before any row of the batch is visible; and because the next two designs remove the question that
  B5 asks, at a few small writes for a batch.

**M-B. Each task keeps its own, and appends it in the next run.** Each task that writes an answer puts
the entry into its own part of `:to-keep`; in the run that closes the batch every task reads its part,
appends, and takes it out.
- Under failure. As M-K below: run, `logs/batch-each-task.log`.
- What one batch pays. In its attempt: one put for each task that wrote an answer. In the next run: one
  seek on every task of the module, and one append on each task that kept something, side by side.
- Why it loses. The seeks grow with the tasks: N for each closing, where the batch may have touched
  three. And the appends are at least T' where M-K makes one for each 256 answers.

**M-K. One task keeps a batch, and appends it in the next run. Taken.**
- Under failure. An entry is put into `:to-keep` in the attempt that writes its answer's row. Rama keeps
  or drops an attempt's writes on all tasks together, so the entry and the row are committed together or
  not at all (run: M1; read, in docs that describe the release, `12-microbatch-topologies.md:116` and
  `:120`; the note for 1.6.0 says the commit and replication of a microbatch changed). The next run
  begins only when that batch is committed on every task (run: the batch probe, B7, S2; read, as above,
  `microbatch.md:128`), reads the entries from committed state and appends them. When that run fails
  after its appends, its second attempt appends the same again (run: M2, M3).
- What one batch pays. In its attempt: one send and one put for each task that wrote an answer. In the
  next run: one hop, one seek, one append for each 256 answers, one removal for each put.
- What the rebuild must do. The same as for the streaming gate: group by act-id and take one.

### The microbatch gate after the change, step by step

The four steps of `module/microbatch_gate.clj:378-567`, with what is new in bold.

| Step | Where | Reads | Writes | Appends |
|---|---|---|---|---|
| 1. arrive | the depot partition's task, for each thing | as today | as today. **For a stray, two sends more**: its entry to the batch's keeping task, and a word to the gate's task that the batch has something to keep | — |
| 2. the head | the gate's task | `:last-stamp`; **`:to-close`** where today `:closed-through` (`:427`); the batch's numbers; **whether a stray was sent** | the clock's keeping and `:last-stamp` as today; **`:to-close`: this batch's id, and its clock when it made a stamp; or the key taken out when this batch has nothing to keep and the key was there** | the batch's clock, as today |
| **2. the closing** | **the keeping task of the batch before, only when the head read a `:to-close`** | **one range of `:to-keep`, the keys of that batch** | **each of those keys taken out** | **one for each 256 entries, `:append-ack`, one after the other. The first record also holds that batch's `:clock`, from `:to-close`, and its `:last-stamp`, the gate's last stamp as the head read it; both only when `:to-close` held a clock** |
| 2. every task | every task | as today | `:closed-through`, when the head read a `:to-close` and there is a last stamp: today's write (`:454-455`), with `:to-close` as its test in place of `closes?` of two stamps | — |
| 3. decide | every task | as today | as today | — |
| 4. write and meet | every task | as today; **the strays that step 1 sent here** | as today. **Then what is to keep**: the answers among the rows that the task wrote from its parcels and among the rows of its pass, and the strays sent to it, 256 to a value at most, each sent to the batch's keeping task and put there under its key | — |

- The keeping task of a batch is the task that the hash of the batch's id gives. The closing goes there
  from the gate's task by one partitioner, before the step goes to every task.
- Step 4 ends today by sending each row of a pass to its home (`:558-566`). What is to keep rides that
  send as one more kind of row, whose home is the batch's id; `write-row` gains its case. So the step
  gains one pure call and no new block.
- The closing's appends give the keeping task back while they wait. What can run there meanwhile is what
  the gate's docstring lists for the append of a batch's clock (`:187-199`): a read of committed rows, an
  event of the streaming gate, an event of `batch-clock`. No event of this batch reads or writes
  `:to-keep` in step 2: step 1 writes nothing there, and step 4 has not begun.
- A run that closes nothing pays nothing new: the head reads two fields, as today.
- Why the test of the closing changes. Today a run closes when the gate's last stamp is not the stamp
  that is closed (`gate/batch.clj:511-518`). A batch that took only strays makes no stamp, and its
  strays would wait in `:to-keep` until a later batch made one; a store at rest could then hold a stray
  with no entry. `:to-close` is written by every batch that has something to keep, so the next run
  always closes it. Reading it in place of `:closed-through` keeps the head at two reads.

### Which declaration owns each new thing (ruling 18)

| The thing | Declared in |
|---|---|
| `*streaming-answer-depot` | `module/streaming_gate.clj`, `add-to` |
| `*microbatch-answer-depot` | `module/microbatch_gate.clj`, `add-to` |
| `:to-keep`, `:to-close` | `record/tables.clj`, `microbatch-task`, the schema of the microbatch gate's own table |
| The form of an entry and of a stray's entry | `record/rows.clj`, pure, called by both gates with the gate's depot word |
| What a task sends to keep, the records of a closing, the key and the range of `:to-keep` | `gate/batch.clj`, pure |

No new code chooses by a depot word, names the two depots of acts as a pair, or says "the other" (R6;
implied spec, section 2, is the list that this is held against, and this part adds no place to it).

## B. Each point of failure, for each gate

Ruling 12 asks two properties at every point: **P1**, no answer stays in the tables for good with no
entry in the log; **P2**, no entry of an attempt that did not count changes what a rebuild makes.

### The streaming gate

"The first part" is steps 0 to 7; "the second part" is steps 8 and 9. "Run again" is Rama's next run of
the same source record.

| # | Where the failure comes | Marked | The tables after it | The log after it | After Rama has run the record again | P1 | P2 |
|---|---|---|---|---|---|---|---|
| S1 | A throw in the first part, before or after the writes; or in another event of the same batch of events | run: S1 (after the writes); the stream probe, F2 with a boundary, 6 of 6; read, `11-stream-topologies.md:170` | nothing of the attempt: its writes are dropped | nothing: the append stands behind the boundary and was not made | the first run that counts decides: the rows, and one entry | no answer is in the tables | no entry was made |
| S2 | The commit of the first part's batch fails: the process dies, or the task's leader is lost, before the writes are replicated | read, `11-stream-topologies.md:170`, "They will never replicate or be made visible externally"; reasoned for the second part, which is sent on only when the writes before the partitioner are committed (`stream.md:41`). Not run: a probe cannot make a commit fail | nothing | nothing | as S1 | as S1 | as S1 |
| S3 | After the commit, before the append is made: a throw at the head of the second part, a throw of another event in that batch, a worker that goes down between the two | run: S2. The stream probe saw the same state by another road: O3 with a boundary, where the records behind a failed one had rows and no record | the answer and its rows, committed and readable | no entry | check (a) finds the answer; the event writes nothing and appends a copy: one entry | holds: the source record's event tree did not complete, so Rama runs it again (at least once: `11-stream-topologies.md:156`), and every run of an answered record appends | the copy is of the committed row |
| S4 | After the append: a throw behind it, a wait past the stream's timeout, a worker that goes down before the record is marked done | run: S3; the stream probe, F1 with a boundary, `1 · 1 · 1` | the answer | one entry | one copy more: two entries, the same | the entry is there | both entries hold the committed row |
| S5 | No failure of this event at all: Rama runs a record again whose event completed and whose maker was answered. After any throw on the worker, every record since the topology last wrote its progress, on every task of that worker; under "all after", the records behind a failed one; a leader that restarts before its progress is written | run: S, "WHOLE" (an id answered before four restarts has five records); the stream probe, O1, O2 and 1.8; read, `11-stream-topologies.md:166` | unchanged | one copy more for each run | — | the entry was there | each copy holds the committed row |
| S6 | An append that does not come back within `topology.stream.timeout.seconds`: Rama fails the batch with no restart and runs the records again | run for an event that waits on something else (the stream probe, 1.7, O4); reasoned for an append | the answer | the entry, or none yet | a copy | as S3 | as S4 |
| S7 | Two things under one act-id in one batch of events | run: S4 | the first's answer | two entries: the first's, and the second's copy, both appended behind the commit | — | the entry is there | both hold the one row |
| S8 | A later act of the layer is decided before a failed one is run again (the store's page, "The order after a failure") | reasoned. Not reached: the stream probe, 3.3; in S5 the two things reached the depot in the other order | each act's answer, with stamps in the order in which the gate decided | one entry for each | — | — | each entry is of a committed answer; the order is in the stamps, not in the log |
| S9 | A stray, at each of the points above | reasoned from S1 to S5: the same shape. The stray's write is not behind check (a) | the member of the set, or nothing before the commit | a stray's entry for each run that reached the append | one more | holds as in S3: a member with no entry is given one by the next run | each entry adds a member that the set holds |
| S10 | The gate's code is changed between two runs of one record | reasoned | when the first run committed: its answer. When it did not: nothing | as the rows above | a committed answer is copied and not decided again. An answer that was dropped is decided by the new code, and that is the first decision that counts | holds | holds: the entry is of the answer that the tables hold (R1) |

**P1, said once.** An answer of the streaming gate is in the tables with no entry only between the commit
of its batch and the append behind it: about a millisecond when nothing fails, and until the next run of
its record when something does. It rests on one thing of Rama's: a record whose event tree did not
complete is run again. "For good" would need the record never to run again, which the source's retry
mode rules out. One property follows that a maker can lean on: **when the maker's `:ack` append returned
without an error, the event tree is complete, so the entry is durable.** A reader by act-id can be given
an answer a moment before its entry is in the log; never the other way round.

**P2, said once.** There is no entry of an attempt that did not count. An append is made only behind the
boundary, the part behind a boundary runs only when the writes before it are committed, and the answer
under an act-id is written once. S ran it: of 57 records after four forced throws, none held another
row than the table's, and no row had no record.

**Ruling 29: what the log holds after a replay of n records that were already answered.** n more
records, one for each run, each a copy of the entry that is there. S: 60 events began for 28 source
records; the 57 that reached the append each left one record. The rebuild's answer does not rest on the
count, because it takes one entry under each act-id and holds the others to it; a hundred copies give
what one gives. The count is paid in bytes: about 135 for each run of an answered record. A restart runs
again each record since the stream topology last wrote its progress, which by Rama's defaults is every
30 seconds or after a number of records (read from the 1.6.0 jar by the stream probe; not run to a
number): at 100 acts a second, up to about 3,000 copies for one restart of a worker, 0.4 MB.

### The microbatch gate

"The batch" is batch b; "the next run" is the run with id b+1, which closes it.

| # | Where the failure comes | Marked | The tables after it | The log after it | After Rama has run the attempt again | P1 | P2 |
|---|---|---|---|---|---|---|---|
| M1 | An attempt of the batch fails while it is processed: a throw of the module's code, the phase timeout, a move of a leader. Before or after its rows and its puts of what to keep | run: M1; the batch probe, B2 and B3.i | nothing of the attempt was ever visible; `:to-keep` and `:to-close` are set back with the rows | nothing of the batch: its own attempts append none of its answers | the same things, the same id, the kept clock: the same rows and the same entries in `:to-keep` | no answer is committed | no entry was made |
| M2 | The commit of the batch fails after some tasks have shown their part | run on a stand-in, for rows: the batch probe, B7, S3 and S6. Reasoned for `:to-keep`: it is a table of the same topology, replaced with the rest ("the new version replaces the old version", `12-microbatch-topologies.md:128`: read, in docs that describe the release; the note for 1.6.0 says the commit and replication of a microbatch changed) | the rows of the first attempt on those tasks, readable by a direct select and not closed; then the second attempt's, the same but for one word | nothing of the batch: the next run has not begun (the batch probe, B7, S2) | the batch commits everywhere with the second attempt's rows and entries; the next run appends those | the door gives no answer of the batch yet; the entries come with the next run | the entry of an act into a layer of the other gate holds the word of the attempt that counted, because it is read after the commit |
| M3 | The next run fails after its appends: a throw later in its walk, its own commit, the timeout | run: M2 and M3 | the batch's rows as committed; the next run's own writes set back, so `:to-keep` holds the batch's entries again and `:to-close` names the batch again | the batch's records | the same records again: copies | the entries are there | each copy is read from the batch's committed state |
| M4 | The next run fails before its appends | reasoned: M1 for that run; in M3 of the probe the keeping task's first attempt is this for the things of its own batch | the batch's rows, committed and not closed | nothing of the batch yet | the appends | the door gives no answer of the batch until the run that appends commits | — |
| M5 | An append of the closing does not come back | reasoned; not run | as M4 | nothing, or some of the records | the attempt waits until Rama's phase timeout fails it, about 30 seconds where nothing is set (the store's page, "The clock of a batch"), and runs again | as M4. While it lasts the base answers no maker | copies |
| M6 | No next run comes: the module is stopped after the batch commits | reasoned. The proof's group that starts the module again can show the first run after a start | the batch's rows, not closed | nothing of the batch | the first run after the start reads `:to-close` and appends: both fields are durable | holds when the module runs again; a store that never runs again holds answers that no door gave and no log holds | — |
| M7 | A batch of strays alone | reasoned. M ran a stray beside other things (M0, M1, M3), each kept once; a batch of strays alone was not run | the members of the sets | nothing until the next run | the next run appends them, because each stray told the gate's task that the batch has something to keep | holds | — |
| M8 | The topology that keeps a batch's clock fails or does not answer | as built (the proofs' map, "What the module's proof does not reach"); unchanged by this part | — | — | — | — | — |

**P1, said once.** An answer of the base is in the tables with no entry from the commit of its batch
until the next run's append, about 120 ms in M (the record of one id came 147 ms after its row). The
door gives an answer of the base only when its batch is closed, and `:closed-through` is written by the
attempt that made the appends and commits only when that attempt does. So **an answer of the base that
the door gave is in the log.** A direct read of the tables, which is how the read path reads a fact, can
be given a row before its act's entry is in the log.

**P2, said once.** There is no entry of an attempt that did not count: no attempt appends what it
decided itself. M: 21 kept answers of 16 ids after three forced throws; none differs from the table's
row; the attempt that failed after its writes left none.

**The replay of the microbatch gate.** A committed batch is never run again. What repeats is an attempt
of a run that closes: each attempt that reaches its appends leaves the records of the batch before once
more. The copies of one batch are the failed attempts of one run.

## C. How an entry that counted is told from one that did not

The rule, for both gates, from the logs alone:

> **Every entry under one act-id holds the same answer. A rebuild takes one of them.**
> Two entries under one act-id that differ in the word, the stamp or the digest are a fault of the store,
> not an attempt that did not count.

- An entry "counted" in the contract's sense when the tables hold its answer. By B, every entry does.
  So the rebuild tells nothing apart: it groups the entries of a log by `:act-id`.
- What it does when two differ is the rebuild's plan to say. This part hands it the fact that the gates
  cannot make that state, so that the rebuild names it and does not pick one silently.
- A stray's entries: the set of a word on a task holds each `:digest` of the entries with that `:stray`
  and that `:task`, once.
- What a batch keeps of itself: every record with one `:batch` that holds `:clock` and `:last-stamp`
  holds the same two.

**No topology of the module reads either log.** The contract's sentence "Nothing reads the log of
answers but a rebuild" (B2) stands as written, and ruling 11's narrowing of it is not used. How the code
keeps to R3: the source names each depot of answers in two places, its declaration and its append; no
`source>` names one; the door and the read path open neither. A check of the module's proof holds that,
by a search of the source as text (G, N8).

## D. What the logs hold of a batch of the base, and of a stray

**The last stamp of a batch** (ruling 15; findings 8 and 10; B3, "a log comes to hold it"). The first
record that a closing appends for a batch that made a stamp holds `:last-stamp`: the gate's last stamp as
the next run's head read it, which is the last stamp that the batch can hold
(`gate/batch.clj:504-509`). Every record of a batch holds `:batch`, so each act is tied to its batch by
the record that its entry is in, and through the batch's id to that stamp: the `:level-since` of a key
that an act of the batch marks, and the `<since>` in each key of the facts by value under that mark. The
log that holds it is the microbatch gate's depot of answers: one of B3's three kinds, closed to a
client's append, with no trimming option. B3's list of what a rebuild reads does not grow.

**The kept clock of a batch** (ruling 9, third point). The same first record holds `:clock`: the clock
that the batch's stamps were made from, as the gate read it back from `$$batch-clock` after the keeping
(`module/microbatch_gate.clj:434-444`) and wrote into `:to-close`. So the row of `$$batch-clock` is made
again as the `:batch` and the `:clock` of the batch with the greatest id that holds a clock. Ruling 9
gives two roads for a batch with two records on the depot of batch clocks: make that depot's order the
order of the keeping, or name the case. This plan takes neither, because the gate's own log now says
which clock was kept. The first road cannot be shown: the retry mode "all after" does not close the
window in which a later record is processed before a failed one (the store's page, "The order after a
failure"; the stream probe, 3.4), so the order of the keeping would still not be the depot's. The depot
of batch clocks stays a check: the clock of a batch is the `:clock` of one of that batch's records
there. K1.

**`:turns`** (ruling 9, fourth point). What a log would have to hold to give it again: for each task
that took a thing in a batch, the batch's id and how many things the task gave a turn. An entry does not
give it: a repeat takes a turn and has no entry. What it would cost: one read of `:turns` on every task
in every batch with things, after the last thing has its turn; one put; and one more record for a task
and a batch, for ever. What it would buy: the equality of a field that nothing reads once its batch is
committed. `batch/position` reads it only against the id of the running batch
(`gate/batch.clj:159-167`), and for any other id starts at 0. The build session's view holds: **`:turns`
is an exception**, named with this reason for the receipt. The comparison leaves it out.

**`:closed-through`** needs nothing more: ruling 9 makes it again as what a store at rest holds, the
gate's last stamp, which is the `:last-stamp` of the batch with the greatest id.

**A stray** (ruling 8). Each arrival of a stray has an entry, `{:stray :digest :task}`, in the depot of
answers of the gate on whose depot it arrived. The word is `declaration/stray-reason`'s, the digest is
`digest/hex-of` of the thing, as the set holds it (`module/streaming_gate.clj:198-202`;
`module/microbatch_gate.clj:394-399`), and the task is the task of the set. So a rebuild makes each set
from entries and never runs the test for a stray: ruling 8 holds as written, and this plan shows nothing
in its place. Its cost: in the streaming gate one boundary and one append for each arrival of a stray;
in the microbatch gate two sends, and a part of one put and of one record.

## E. What the logs hand the rebuild

**The logs, as a rebuild is given them.** A client reads a depot partition by partition: the number of
partitions (`foreign-object-info`), the start and the end offset of each (`foreign-depot-partition-info`),
and a range (`foreign-depot-read`), which gives a vector of the appended values in the order of their
offsets (run: the stream probe, question 5). So the form is: for each gate, its log of acts and its log
of answers, each a vector of partitions, each partition a vector of records. The number of partitions is
the module's number of tasks. What a read does not give: no offset inside a record (a record's place is
the start plus its position), no time of the append, and no call for either. A range past the end throws
for the whole read. One read is one event on the task's thread, so a reader takes ranges of a few tens
of kilobytes (`14-depots.md:242`); a record of the microbatch gate's depot can be near 30 KB, so few at
a time.

**How an entry is tied to its thing.** By two equalities: the entry's `:act-id` is the thing's
`:act-id`, and its `:digest` is `digest/digest` of the thing as the depot gives it, which is the function
that the gate called on it (`door/digest.clj:41-54`). Under one act-id a log of acts can hold several
things; the one with the entry's digest was decided, and each other is a repeat or another thing that
got no answer (edges 7 and 8). A thing with no entry of either kind was never decided.

**What gives the order in which the rebuild applies the acts** (finding 9). The stamps, never the
offsets of a log. In the streaming gate a stamp is strictly later on its task at each decision, and every
row of a layer is on one task: the acts of one partition of `*streaming-answer-depot`, in the order of
their stamps. The order of that partition's offsets is the order of the appends, with copies behind
later acts' entries. In the microbatch gate no two acts share a stamp: the acts of all partitions of
`*microbatch-answer-depot`, in the order of their stamps.

**Each table and each part of a row.** "The entry" is the one entry taken under an act-id (C). "The
thing" is the thing tied to it. "Its own state" is what the rebuild has itself made so far (ruling 10).

| Table, part | Which log gives it, from which part of which record, by which rule |
|---|---|
| Answers: the key | The entry's `:act-id`: its layer and its uuid, by `rows/answer-key` |
| Answers: `:refused`, `:stamp`, `:digest` | The entry's `:refused` (absent for an accept), `:stamp`, `:digest`, as they are (4.9, rows 2 and 3; edges 10 and 12 to 15) |
| Facts: the key, `:value`, `:by-act`, `:by-index`, `:who`, `:permission` | The thing, in its one form (ruling 14), only when the entry has no `:refused` |
| Facts: `:stamp` | The entry's `:stamp` |
| Facts: `:level-since`, for a key that stays marked | Its own state: the row before at the layer's marked-keys place |
| Facts: `:level-since`, for a key that is newly marked | Streaming: the `:stamp` of the marking act's entry. Microbatch: the `:last-stamp` of the batch whose `:batch` the record of the marking act's entry holds (4.9, row 4; edge 17) |
| Facts by value: the key | The thing; `<since>` as `:level-since`; the value's digest from the fact's row |
| Facts by value: that an entry is put or taken out | The stamps, and its own state: the fact's stamp is the entry's, the key's stamp as above, the row before at the place. The store's sentence, "every fact under that key whose stamp is later than the key's stamp, and no other", can be held from them, also for a mark and a fact of one batch (edge 18): both stamps are in the log |
| `$$streaming-task`, `:last-stamp`, on each task | The greatest `:stamp` of the answers' entries on that partition of `*streaming-answer-depot` (4.9, row 6) |
| `$$streaming-task`, `:strays`, on each task | The strays' entries of that depot with that `:task`: `:stray` to the set of `:digest` |
| `$$microbatch-task`, `:strays`, on each task | The same, from `*microbatch-answer-depot` |
| `$$microbatch-task`, `:last-stamp`, on the gate's task | The `:last-stamp` of the batch with the greatest `:batch` that holds one (4.9, row 5; edge 23) |
| `$$microbatch-task`, `:closed-through`, on every task | The same number (ruling 9) |
| `$$microbatch-task`, `:turns` | **No log gives it.** The count of things that a task took in its last batch is kept nowhere, and nothing reads it after its batch commits (D). An exception for the receipt |
| `$$microbatch-task`, `:to-keep` and `:to-close` | No log gives them, and none need: on a store at rest `:to-keep` holds no key and `:to-close` is absent, on every task. The comparison holds a map that was emptied equal to one that was never written |
| `$$batch-clock`, its one row | `:batch` and `:clock` of the batch with the greatest `:batch` that holds a clock. The depot of batch clocks is the check (D; 4.9, row 10; edge 25) |
| The task of a row of a streaming layer | The partition of the entry in `*streaming-answer-depot`: the task that decided, also for a thing that sat on a partition that its layer's hash does not give |
| The task of a row of the base, and how many rows a fact under one of the five keys is | **No log holds the placing.** The number of tasks is the number of partitions of any depot of the module; which task a route goes to is Rama's hash, which the rebuild's plan must match or let Rama do (4.9, row 9; edges 22 and 28). The partition of a record of `*microbatch-answer-depot` says nothing of a row's task |
| The tables of locks | Gone with `the-locks-go` |

**What the logs give the last sentence of ruling 14** (ruling 22). A record is plain data. Nothing in it
shows that a gate wrote it: no seal, no chain to the record before. So a proof reads a depot of answers
with a client, changes an entry in the vectors, and hands the rebuild those vectors; it need not append.
It must change each copy of the entry, or the rule of C names the act.

**What no log gives**, in one place: `:turns`; the task of a row of the base and the count of rows under
the five keys, which rest on Rama's hash; and nothing else of section 4 of the implied spec.

**The six things that the split has this part hand on** (`DECOMPOSITION.json`, "hands_to_the_parts_after"),
and where each is: (1) the logs, and what each part of a record is for: the table above and D; (2) what
tells an entry that counted: C; (3) which way ruling 8 and ruling 9's two choices went: D; (4) the
proof's hand that makes an attempt of each of the store's own gates fail: G; (5) what no run of this
part could reach: G, and the probe's page; (6) each word and each name: "I".

## F. What it costs

Counted from the dataflow. "Today" is the store's page ("What one accepted act costs the streaming
gate", "What one accepted act costs the microbatch gate"). "With the locks gone" is reasoned from the
same dataflow with the read of a lock's range (`module/streaming_gate.clj:277`) and the write of a lock's
row (`:336`) taken out; the plan of `the-locks-go` gives the counts that hold. The times are of one
in-process cluster on one machine, with one copy of each task: an order of size, and D measures the
store's own.

### The streaming gate, one thing

| The thing | Today: reads · row writes | With the locks gone | After this part |
|---|---|---|---|
| An accepted act: one value, one entity, nothing named to replace, the key not marked | 5 · 4 | 4 · 3 | 4 · 3, and one commit boundary, one append that is waited for, one record |
| The same, the key marked | 6 · 5, or 6 | 5 · 4, or 5 | the same, and the same three |
| A refused act whose permission does not stand | 4 at most · 2 (the answer, the last stamp) | the same | the same, and the same three |
| A repeat, or another thing under a used act-id | 1 · 0 | 1 · 0 | 1 · 0, and the same three |
| A stray | 0 · 1, and the read by which the set keeps its size | the same | the same, and the same three |

What the three cost, as the stream probe measured them for write, boundary, append with `:append-ack`
(its question 4, `cost.log`):

- inside the event: the code of an event went from 0.05 ms to 2.18 ms in the middle, about 1 ms for the
  boundary and 1 ms for the append;
- on the maker's call: the client's `:ack` append went from 2.76 ms to 3.59 ms in the middle at 100 a
  second; for one client that sends each thing when the one before is answered, from 479 to 314 a second;
  with 2,000 sent at once the task did 9,212 a second against 10,197, which cannot be told apart;
- the time a task is held: the event gives its task back at the boundary, after its last write. While
  the appends of one batch of events are on their way, the next batch of that task's events does not
  begin (the stream probe, 6; read, `23-acid-semantics.md:29`). So the wait of about 1 ms is paid once for
  a batch of events and not once for each event, which is what the 2,000 at once show.

Beside the numbers before the build (`../reads/runs/measures-final.txt`, M1): the streaming gate at 100
acts a second took 4.5 to 4.6 ms in the middle from the call of `send-act` to its return. The keeping
should add under a millisecond to that; D's second measure gives the number.

### The microbatch gate, one batch

A batch of n answered acts whose answers' rows are on T' tasks, with s strays.

| | Today | After this part |
|---|---|---|
| In the batch's own attempt | each act as the store's page counts it; for the batch, on the gate's task: 2 reads at the head, the clock's keeping (3 reads, one record, one row; a later attempt 1 read), the last stamp's write, and `:closed-through` on every task when a batch is closed | the same, and: one write of `:to-close` on the gate's task; one send and one put for each task that wrote an answer, or more for a task with over 256; two sends for each stray |
| In the next run | 2 reads at the head; `:closed-through` written on every task | the same, and: one write of `:to-close`; one hop to the keeping task; one range read there, one seek and about T' steps; one append for each 256 entries, one after the other, waited for; one removal for each key |
| A run with nothing to close | 2 reads at the head | 2 reads at the head |

- One append with `:append-ack` from inside an attempt took 2.6 to 3.0 ms, and two took 4.8 to 5.9 ms
  (M, "COST"). So the run that closes a batch of up to 256 answers is about 3 ms longer, and the door's
  answer of the base comes about that much later, beside 1,010 to 1,212 ms in the middle today
  (`measures-final.txt`, M1 the base). The batch's own attempt waits for nothing new, so a fact's row of
  the base shows when it shows today.
- On a real cluster each append waits for one round of replication to the followers of one task.

### What is kept for ever (implied spec 6.9)

| Log | One record for | Bytes, as Nippy writes such a record (O, "SIZE") |
|---|---|---|
| `*streaming-answer-depot` | each thing that got an answer | 131 to 136 accepted; 172 refused with a word of 25 letters |
| | each later run of an event for an answered act-id: a repeat, another thing under the act-id, a replay | the same again |
| | each arrival of a stray | about 110 to 120 |
| `*microbatch-answer-depot` | each 256 entries of a batch, and at least one for a batch that left something | about 60 to 130 for each answered act: a digest and a uuid are 48 bytes that do not pack, and the rest is the stamp and the words; 55 more for a batch's clock and last stamp |
| | each failed attempt of a run that closes, when it had appended | the records of the batch before, again |

Rama's own writing of a record to a depot was not measured. At 100 answered acts a second a gate's depot
of answers grows by about 1.1 GB a day, at 1,000 by about 11 GB; "Nobody has priced the disk" (entry 3).
The store can forget none of it.

## G. The proofs

No new proof file. The new checks are in the proofs of today.

### New checks, and which proof holds each

In the module's proof (`test-store/softland/store/module_proof.clj`), on its first cluster, by the hand:

| # | The check | In which group |
|---|---|---|
| N1 | Each answer's row of the streaming gate has an entry (implied spec, section 9, item 3): for each thing that got an answer, at least one entry under its act-id, with the row's word or none, its stamp and its digest; the entry has exactly those parts and no value of a fact; each entry under one act-id is the same; an entry is on the partition of its layer's task | 14, "The stamps and the depot", over everything that the streaming groups sent |
| N2 | A repeat leaves a copy: after the same act is appended a second time, one entry more under its act-id, the same | 4 |
| N3 | A stray has an entry at each arrival, with its word, its digest and its task, and the set still holds it once | 7, and 23 for the microbatch gate |
| N4 | Each answer's row of the base has an entry once its batch is closed: read right after the door's read gave the answer, the entry is there. And the batch: one of its records holds `:clock` and `:last-stamp`; the clock is the `:clock` of a record of that batch on the depot of batch clocks; the greatest `:last-stamp` is the gate's last stamp | 31, its last part, over everything that the base's groups sent |
| N5 | The last stamp of a batch is in a log (finding 8): the `:level-since` that group 25 reads for a newly marked key is the `:last-stamp` of the batch whose records hold the marking act's entry | 25 |
| N6 | On a store at rest `:to-keep` holds no key on any task and `:to-close` is absent; a quiet time of 1.5 s adds no record to either depot of answers | 31 |
| N7 | A client's append to a depot of answers is refused | 14 |
| N8 | R3 and B2: in the store's source each depot of answers is named where it is declared and where it is appended to, and nowhere else; no `source>` names one; `door.clj` and `read_path.clj` name neither. A search of the source as text, as the read path's proof searches for readers of facts | 14 |
| N9 | After the module is started again the answers' entries are there, and each entry under one act-id is still the same; whether a start that is no failure runs records again is counted and shown, and not held to a number | 13 and 31 |

On the module's proof's second cluster, where functions of the store's pure code are replaced before the
launch by ones that give what the store's give:

| # | The check | How the failure is made |
|---|---|---|
| N10 | A batch whose first attempt fails after its rows are noted (group 32, as it is): once the batch is closed, each of its answers has one entry, the row's; the act into a layer of the other gate has the row's word | the throw of today, in `rows-to-send` |
| N11 | The run that closes a batch fails after its appends: each entry of that batch is there twice, the same | two functions more are replaced: the pure one that makes the records of a closing, which notes that it gave some; and `batch/marks-places-asked`, which every task calls behind the closing in the same step (`module/microbatch_gate.clj:460`), and which throws, once, when the note is there |
| N12 | An event of the streaming gate fails behind its commit and before its append: the answer is readable, no entry is there until Rama has run the record again, and then one is, the row's. And the replay: each thing that the group sent before the throw has one entry more, the same; every entry holds its row; every row has an entry | the pure function that makes an entry is called in the second part of the event, and its stand-in throws, once, for one act-id that the proof marks |

In the pure proof: the form of an entry and of a stray's entry, and that an act-id comes back from its
text for a keyword and for a UUID; what a task sends to keep, 256 to a value at most, with keys that sort
by the batch and that two tasks never share; the records of a closing; when a run closes; and the two
sends of a stray.

### How a proof makes each gate fail after the keeping, with no hook in the module's source

**The microbatch gate.** The module's proof already makes a batch run a second time: before the launch
of its second cluster it puts three functions into vars of `gate/batch.clj` that give what the store's
give, and one of them throws once (`module_proof.clj:2596-2648`). Rama takes the function that a var
holds at the launch (the batch probe, B5). That way reaches what is needed, with two functions more
(N11). The keeping is an append of Rama's and no function of the store, so the note is taken by the pure
function that makes the records right before it, and the throw comes from the next pure function that
the same task calls. The batch probe saw that a replacement which closes over an atom fails the launch;
the functions of today are held by vars and read atoms of the proof's namespace, and the new ones do the
same.

**The streaming gate.** Nothing of the store runs behind the append in an event, so a proof cannot throw
there. It reaches the two things that matter in another way (N12). A throw behind the commit and before
the append is row S3 of B. And after that throw Rama starts the worker again and runs every record since
the last written progress: each of those events is a second run behind a keeping, which is row S5 and
the common case of ruling 29.

**What a proof cannot reach, by name.** A throw behind the append inside the same event. A commit that
fails, of an event's batch or of a batch of the base. A process that is killed; a task's leader that
moves; a second copy of a record. An append whose ack does not come, and the phase timeout during a
closing. A later act of a layer decided before a failed one. When Rama writes a stream topology's
progress, and so how many records one restart runs again. A third forced failure in one cluster was not
tried in a proof: the proofs' map says that each one takes Rama longer than the last, and M made three
in ten seconds.

### Checks of today that this part makes untrue

A search of the six proofs for a count of the module's depots, and for "the module appends in one
place", found none in code: the proofs open the depots by name and count none. What changes:

- the pure proof's checks of `batch/closes?`, whose input changes (A);
- group 32, where the hand counts the records of the depot of batch clocks: unchanged, and N10 and N11
  stand beside it;
- the proofs' map and `test-store/README.md`, which say that no proof shows "a failure in the middle of
  an event" and "an event that Rama runs twice": N12 shows one of each (I).

No check rests on a time that the keeping moves past its bound: group 17 watches a row from before its
batch is closed until after, and the closing is about 3 ms later; group 32 reads for 60 s.

## H. The measures of D, and the code that measures

**Where the code lives.** A namespace of its own beside the measures of the read build:
`test-store/softland/store/gate_measures.clj`, `softland.store.gate-measures`, run with
`clojure -M:store-module -m softland.store.gate-measures`, behind the build's lock. A run that is no
proof: it prints each number on a line that starts with `MEASURED` and holds nothing to a number. It
opens a door and no read path, so **no live read is open in any of its measures**, and its first line
says so (finding 15). It names no depot of answers, so the same file runs on the tree before this part
and after it.

| Measure of D | What runs | Load and shape | Beside which line of `measures-final.txt` |
|---|---|---|---|
| 1. An act through the door and a gate | The read build's M1, as it is: one maker, acts of one fact, a string of 100 bytes under a plain key, each to another place of one layer. The streaming gate with the key not marked and with it marked, timed to the return of `send-act`; the base, timed to the door's "accepted" | 3,000 acts at 100 a second after 500 that are not counted | "M1 the streaming gate … the key not marked", 4.621 and 4.505 ms; "… the key marked", 4.513 and 4.427 ms; "M1 the base", 1,212.22 and 1,010.276 ms |
| 2. What the keeping adds to an act of the streaming gate | Measure 1's two streaming shapes, on the two trees: the difference of the middles. And one shape by the hand, since the door gives each send a new act-id: the same thing appended a second time with `:ack`, 1,000 times, timed to the return | as measure 1 | the same lines |
| 2. What the keeping adds to a batch of the microbatch gate | Measure 1's base shape, with the door opened on a wait that reads every 5 ms, and two times for each act: to the first sight of its answer's row by a select of the table, which is the commit of its batch on that task; and to the door's "accepted", which is the closing. The keeping is in the second and not in the first | as measure 1 | "M1 the base" |
| 3. Rama's setting at 1 | Measure 1's first shape. And one maker's most: acts of the same kind sent at 200, 400, 800 and 1,600 a second without a wait for the answer before the next, each for 10 seconds, with the middle and the slowest time of each and whether the last was answered within a second of its sending; and one maker that sends each act when the one before is answered | 3,000 at 100 a second; then the rates | "M1 the streaming gate … the key not marked", and the same shapes on the same tree with Rama's default |

**How the setting is set to 1 for one run and left as it is in the module.** The measuring code holds a
module of its own of three lines: it calls the definition of the store's module and then sets the option
for the topology `streaming-gate` at the launch, with
`set-launch-topology-dynamic-option!` and `"topology.stream.max.executing.per.task"`. O ran that form on
a stand-in: with it one record of a task was pending at a time, where 399 were without it. The second
module has a name of its own, and the door opens on that name. `module.clj` and the gates do not name the
option (R10: the other answer is one line of a launch, and no code of the store). The in-process cluster
has no call that changes an option of a launched module (read from the 1.6.0 jar), so measure 3 starts a
cluster of its own.

**Which numbers are taken on which tree** (ruling 23).

| Tree | Numbers | Kept as |
|---|---|---|
| As `the-locks-go` left it, before this part changes the module | measure 1, whole; and measure 2's shapes, as its "before" | `runs/measures-before-answers.txt` |
| After this part | measure 1 and 2 again: the difference is what the keeping adds; and measure 3 | `runs/measures-after-answers.txt` |
| The final tree, after the review | all three once more, by the build session | for the receipt |

So the build writes this file before it changes the module, and runs it once then.

**How long a run takes.** Measure 1: three shapes of 35 seconds, and the base's waits: about two and a
half minutes. Measure 2: two shapes more, about one minute. Measure 3: a cluster of its own, 35 seconds,
then four rates of 10 seconds with each setting: about three minutes. About seven minutes in all; with
the argument `small`, about one. Each time is a floor for a round trip on one machine with every row in
memory, and says nothing of a disk or of a second node.

**One thing to hold in view when measure 1 is read beside M1** (implied spec 6.8): M1's sends took
their locks from a stock, so a request's round is not in its middle.

## I. The explanations

`src-store/CLAUDE.md` binds each. A line that this part makes untrue is replaced, not amended, in the
commit that changes its code. Where `the-locks-go` has written a sentence again by then, this part's
change is made to the sentence as it then is, found by the words under "Says today".

### By file

| File | Says today | Will say |
|---|---|---|
| `softland/store/README.md`, the first paragraph, and "The parts", the rows of `module/` and `module.clj` | "the two depots of acts, the gate of each, the record that each gate keeps"; "It owns the three depots" | The module has five depots: the two of acts, a depot of answers for each gate, and the depot of batch clocks. `module.clj` declares three; each gate declares its depot of answers |
| the same, "How the parts relate", the streaming gate's walk, steps 2 and 6 | "If one is there, the event ends."; the walk ends at the writes | Step 2: the event then decides nothing, writes no row, and keeps a copy of that answer. A step 7: behind a commit, the gate appends the entry to its depot of answers |
| the same, the microbatch gate's four steps | step 2 names the clock and the replies; step 4 the writes | Step 2 also closes the batch before: on that batch's keeping task, what it left to keep is appended. Step 4 also sends what is to keep of this batch to its keeping task |
| the same, its last paragraph | "The streaming gate's dataflow only reads rows, calls the pure functions and writes rows. The microbatch gate's dataflow does one thing more" | Each gate's dataflow reads rows, calls the pure functions, writes rows, and appends to its depot of answers; the microbatch gate also appends a batch's clock |
| the same, "What the gate does not check", "Where a thing sits" | "The one append that a topology makes is the microbatch gate's, of a batch's clock" | A topology appends in three places, and to no depot of acts: each gate to its depot of answers, and the microbatch gate to the depot of batch clocks |
| the same, "Decisions this module stands on", the row F2 A | "That the acts depots are the replay log is a session's sentence … What a rebuild from the microbatch depot reaches is under rules 9 and 19" | The gates' PStates are the record that is read. What a gate decided is also in its depot of answers (entry 1 of 12 October), from which, with the depot of acts, the tables can be made again |
| the same, "What one accepted act costs the streaming gate" and "… the microbatch gate" | the counts of reads and row writes | The same counts, and what the keeping adds: for an act of the streaming gate one commit boundary and one append; for a batch what F counts |
| the same, "The clock of a batch" | the depot holds no cut; "Nothing reads its old records" | Unchanged but for one sentence: the clock that a batch kept is also in the first record that the gate's depot of answers holds of the batch |
| the same, "Closing" | "Each run of the gate writes `:closed-through` with the gate's last stamp as it was before the run" | The run that closes a batch also appends what the batch left to keep, before it writes `:closed-through`; so an answer of the base that the door gives is in the log. The run knows that it closes a batch by `:to-close` |
| the same, "How each of the builder's rules is met", number 5 | "the one append that the module's code makes is of a batch's id and clock" | Its sentence on a lock goes with `the-locks-go`; what stays says that no value of a fact is appended by the module: an entry holds an act-id, a word, a stamp and a digest |
| the same, number 9 | "What would make it the same is a record of each batch's cut and clock … Whether the rest is wanted is Sid's" | A rebuild does not cut batches again: each act's stamp is in its entry, and each batch's clock and last stamp are in the gate's depot of answers |
| the same, number 19 | "What a rebuild would not give again: … a stamp … a refusal by rule 17; the order across a failed batch; … which of the two words" | Each of those is now in an entry: the stamp, the word, and so the order. What no log gives: `:turns`. Its first sentence, "Nothing is built for a rebuild", stays true until `the-rebuild` (ruling 26) |
| the same, "Words a session made, Sid's to change" | — | The words below |
| `softland/store/module.clj`, the namespace docstring | the list of what a client opens; "State. The module owns the three depots"; "it is the replay log of those layers"; "One place of the module's code appends" | Two depots more in the list, each "no client can append to it; a rebuild reads its log". The module declares three depots and each gate one. A depot of acts holds what was sent; what a gate decided of it is in the gate's depot of answers. Three places append |
| `softland/store/module/README.md`, the first paragraph, both gates' rows, "How the files relate" | "its two depots of acts and its depot of batch clocks"; "It appends to one depot"; "The streaming gate's dataflow only reads rows, calls pure functions and writes rows" | The five depots and who declares each; each gate's row names its depot of answers; the keeping in each gate, in two sentences |
| `softland/store/module/streaming_gate.clj`, the namespace docstring | "What it changes. It owns five PStates"; the walk, steps 0, 1 and 7; "The event has no partitioner and does not yield … all its writes are one group"; "When Rama runs an event again … writes nothing" | It also declares and appends to `*streaming-answer-depot`. The walk gains the keeping, and step 1 its copy. The event's reads and writes are one group with nothing between them that gives the task back; behind them stand one commit boundary and one append. A second run writes no row and appends one copy |
| the same, `add-to` | "Takes the module's `topologies`" | Takes the module's `setup` and `topologies`; declares the depot |
| `softland/store/module/microbatch_gate.clj`, the namespace docstring | "What it changes"; the walk, steps 1, 2 and 4; "Closing"; "When Rama runs a batch again"; "Yielding" | The two fields and the depot; the two sends of a stray; the closing; what step 4 sends to keep; that no attempt appends what it decided itself; that the closing's appends give the keeping task back, and what can run there meanwhile |
| the same, `add-to`, `write-row` | — | `add-to` takes `setup`; `write-row` has the case of a row to keep |
| `softland/store/gate/batch.clj`, the namespace docstring, "The words of a batch" and the data by step; `closes?` | "True once after each batch that gave a stamp" | The keeping task of a batch; what steps 1, 2 and 4 hand on for the keeping; `closes?` is true once after each batch that left something to keep |
| `softland/store/record/rows.clj`, the namespace docstring | — | The form of an entry and of a stray's entry, with their two functions |
| `softland/store/record/tables.clj`, the namespace docstring, "microbatch-task", and the function `microbatch-task` | "the same for the microbatch gate, with two fields more and one changed" | Four fields more: `:to-keep` and `:to-close` with why each has its shape (the PState design above) |
| `softland/store/gate/README.md`, `softland/store/record/README.md` | the maps | One clause each: the pure parts of the keeping; the form of an entry |
| `test-store/README.md` | "None shows a cluster of several machines, a failure in the middle of an event" | The third shows one failure of an event of the streaming gate behind its commit, and one failure of a run that closes a batch, both made by the proof |
| `test-store/softland/store/README.md`, the module's proof, groups 4, 7, 13, 14, 23, 25, 31, 32; "What the module's proof does not reach"; "The measures" | "an event that Rama runs twice, a batch of streaming events that fails" is not reached | Each group gains its check of N1 to N12. The list of what is not reached is G's. A paragraph for the measures of this build: how to run them, and that they prove nothing |

### Words that this part makes (R8)

Each says what the thing is. None comes from the rig's code. Each is a session's and Sid's to change.

| Word | What it is |
|---|---|
| depot of answers | The log that holds what one gate decided. In code `*streaming-answer-depot` and `*microbatch-answer-depot` |
| entry (in code: answer record) | What a depot of answers holds of one answer: the act-id, the stamp, the digest, and the word of a refusal. The contract's word; the code says "answer record" because `gate/batch.clj` uses "entry" for another thing |
| stray's entry (in code: stray record) | What a depot of answers holds of one arrival of a stray: its word, its digest, its task |
| `:answers`, `:strays`, `:batch`, `:clock`, `:last-stamp` | The parts of a record of a depot of answers. `:stray` and `:task` are the two parts of a stray's entry that no row names |
| copy | A second entry of one answer. Every entry under one act-id is a copy of the others |
| to keep, `:to-keep` | What a batch decided and the log does not hold yet |
| `:to-close` | The batch that the next run of the gate closes: its id, and its clock |
| keeping task | The task that keeps what one batch decided until the next run appends it |
| the run that closes a batch | The next run of the gate after a batch. The store's page says "closing"; this part adds that the run also appends |
| `softland.store.gate-measures` | The code that measures D |

### What Rama cannot change once real data is kept

Read in Rama's docs and not run (`../decisions/METHOD.md`, "What the check of 10 October taught"): Rama
cannot change the name of a depot or of a table (`19-operating-rama.md:424`), the keys of a table, since
a migration is of values (`15-pstates.md:772-776`, `:897`), or the number of tasks of a module
(`19-operating-rama.md:483`). Each of these is one word to change today, in a tree that holds no real
data. So that Sid can change a word before then:

| What this part makes | Why it cannot be changed later | What breaks if it is changed then |
|---|---|---|
| The names `*streaming-answer-depot` and `*microbatch-answer-depot` | The name of a depot | A depot under a new name starts empty. The answers that were kept stay under the old name, and a rebuild must read two depots for one gate |
| The two keys `:to-keep` and `:to-close` of the table `$$microbatch-task` | The keys of a table | A gate that reads a new key finds nothing under it, and what the old key named is never closed or appended |
| The form of an entry, of a stray's entry and of a record | Rama can write the records of a depot again at a module update (`14-depots.md:288`), and R2 says that this build writes no such migration and cuts nothing | A later form means a rebuild that reads both forms for ever |
| That a record of `*streaming-answer-depot` sits on the task that decided | A record stays on the partition where it was appended, and the number of tasks cannot change | Nothing can move it. E reads the task of a streaming layer's rows from it |

What stays free: the form of a key inside `:to-keep`, the bound of 256, and the route of a batch's
keepings. On a store at rest `:to-keep` holds no key, and the partition of a record of
`*microbatch-answer-depot` means nothing; each can change with any module update.

Where two forms would both work, the one that cannot be undone:

- **One depot of answers, or one for each gate.** Neither can be undone: to join two depots or to part
  one, records are written again. One for each gate is the choice under which a later gate adds a depot
  and no existing depot changes.
- **An entry holds the act-id as data, or the text of the answer's key.** Both tie an entry to its
  thing. The text would put today's form of a table's key into every record ever kept, and the key of a
  table is what Rama cannot change in place: the way past it is a new module that is filled from the
  logs, and those logs would then hand it keys of the old form. The plan takes the act-id as data.
- **The task of a stray in its entry, or in the partition of its record.** The plan holds it in the
  entry, so that the microbatch gate is free to keep a batch on any task.

### How what is added keeps each rule

- **R2.** No trimming option is set on either depot of answers, and none of this part's code writes or
  declares a migration of a depot. N6 and N9 read the depots whole.
- **R3.** The tables stay the record. No topology has a source on a depot of answers; the door and the
  read path open neither; no gate reads one before it decides, and a topology cannot read a depot but
  as a source. N8 holds it.
- **R5.** No entry is in a log before its answer's row is committed (B). The build keeps no store of its
  own: `:to-keep` is a PState of Rama's, written and set back with the rows. Nothing here is a cache,
  and the design did not reach for one.
- **R6.** Each depot of answers is declared by its gate. The form of an entry is one pure function that
  takes the gate's depot word, as each check does. No new code names the two depots of acts as a pair.
- **R7.** A second run of an event, and a second attempt of a batch, leave the tables as one would: the
  streaming gate's writes stay behind check (a), and `:to-keep` is a table of the microbatch gate. They
  leave the logs such that a rebuild comes to those tables: an entry more is a copy (ruling 12).
- **R10.** The order of acts after a failure is kept as today: the module sets no option, and the other
  answer is one line of a launch, which H's third measure uses and no code of the store holds.
- **R11.** Three defaults, each the one that keeps more, each provisional: a stray has an entry at each
  arrival; a repeat leaves a copy and the gate drops none; the first record of a batch holds its clock
  beside its last stamp, though the depot of batch clocks holds a clock too.

## J. The order of the build, and its proof

The part is written whole and then proven. No test loop after a small edit.

1. **The measuring code first**, `gate_measures.clj`, and one run of it on the tree as `the-locks-go`
   left it: `runs/measures-before-answers.txt` (ruling 23).
2. The pure code: the form of an entry and of a stray's entry in `record/rows.clj`; in `gate/batch.clj`
   the two sends of a stray, what a task sends to keep, the key and the range of `:to-keep`, the records
   of a closing, and `closes?`; the two fields in `record/tables.clj`.
3. The streaming gate: `add-to` takes `setup` and declares the depot; the writes become a loop that
   ends; the boundary and the append behind the three roads.
4. The microbatch gate: `add-to` and the depot; step 1's sends; the head's read and write of
   `:to-close`; the closing; step 4's rows to keep and `write-row`'s case.
5. `module.clj`: the two calls.
6. The proofs: the pure proof's checks; N1 to N9 in their groups; N10 to N12 on the second cluster; the
   proofs' map.
7. The explanations of "I".
8. At the end: `bash <this folder>/runs/run-proofs.sh after-answers-in-a-log`, the six proofs; then the
   measures again, `runs/measures-after-answers.txt`.

Where a run earlier saves a rewrite. Each is a load of one namespace or one short run behind the lock,
not a proof.

- **After step 3**: that the streaming gate's new tail compiles and that one act through it leaves one
  entry. Rama checks a dataflow when the module is launched, and the tail changes the form of the
  writes.
- **Before step 4's closing**: how the hash of a batch's id spreads a few hundred ids over four tasks.
  When it is poor, the route is the id modulo the number of tasks, by the direct partitioner; it is one
  line then and a change of two steps later.
- **After step 4**: one batch and its closing on a cluster, before the checks are written against the
  depot's records.
- **Before N9**: whether a module update with the unchanged module runs stream records again. The
  stream probe did not run a start that is no failure. N9 is written to what the run shows.

## K. Open points for the session that holds the build

1. **Where the kept clock of a batch is made again from.** The words, ruling 9: "The plan either makes
   the log's order the order of the keeping, or names the case as an exception with its reason."
   - (a) The retry mode of `batch-clock`'s source becomes "all after". It means one option changed in a
     topology that stands, and it still does not make the depot's order the order of the keeping: the
     window that the store's page names for that mode stays.
   - (b) The case of two records of one batch is named as an exception.
   - (c) The gate's own depot of answers holds the clock that the batch read back, and the row is made
     again from it; the depot of batch clocks is the check. It means that the rebuild takes one number
     of the row from another log than ruling 9 names.
   - **Recommended: (c).** The row is then made again in every case, and the number is the one that the
     stamps were made from. It costs 8 bytes for a batch.
2. **A repeat leaves a copy in the streaming gate's log.** The words, the implied spec, 5.2: "a stray
   and a repeat have nothing to keep".
   - (a) Each event that finds its act-id answered appends a copy (this plan). A repeat then costs a
     boundary and an append, and the log grows by one record for each.
   - (b) A mark beside the answer says that it is kept (design S-C): two row writes and a second commit
     for every act, one read more for a repeat, and copies still.
   - (c) No copy. Then a failure between the commit and the append leaves an answer with no entry for
     good, which ruling 12 forbids.
   - **Recommended: (a).** The gate cannot tell a second record from Rama's second run of the first,
     and the second run is what closes row S3.
3. **"The most acts a second that one maker holds"** (D, its third measure). Two readings: the greatest
   steady rate of sends that are not waited for at which the answers keep up; or the rate of one maker
   that sends each act when the one before is answered. With the setting at 1 the second changes
   little, since such a maker has one act on its way at a time; the first is where the setting costs.
   **Recommended: both**, as H has them. They are two shapes of one run.
4. **One depot of answers for each gate, and their names.** Ruling 18's last sentence is read as two
   depots. The names cannot be changed by Rama once real data is kept ("I"). **Recommended: as
   planned**, and that the receipt puts the two names before Sid as words to change now or keep.
5. **The test of a closing changes its input.** `batch/closes?` reads two stamps today and is held by
   the pure proof. This plan gives it `:to-close`, so that a batch of strays alone is closed and the head
   stays at two reads. The other road keeps `closes?` as it is and reads `:to-close` as a third field,
   one read more in every run of the gate. **Recommended: the change**, with its checks written again.
6. **For the plan of `the-rebuild`**, as facts of this one and no question: the rebuild is handed each
   log as the vectors that a client's read gives (E); it takes one entry under an act-id and names two
   that differ (C); a proof that hands it an answer that no gate gave changes every copy (E).

### Rulings that cannot hold

None. Ruling 8 holds as written: a stray has an entry, and the plan shows nothing in its place. Ruling 9's
fourth point holds as the build session saw it: `:turns` is an exception. Ruling 9's third point is met
by a third road, which is K1. Ruling 27 holds: the boundary and the append stand behind the gate's
writes. Ruling 28 holds: both appends are `:append-ack`.

## For the receipt

- Provisional choices of this part, each the builder's: one depot of answers for each gate and their
  names; the form of an entry, with the act-id as data; `:append-ack`; a copy for each run of an
  answered record in the streaming gate; the keeping of a batch on one task and its append by the next
  run; the bound of 256; the clock of a batch in the gate's log (K1); `:to-close` as the test of a
  closing (K5).
- For Sid, as facts. An answer of a streaming layer whose maker's append returned without an error is
  in the log. An answer of the base that the door gave is in the log. Between a row's commit and its
  entry lies about a millisecond in a streaming layer and one run of the gate in the base; a store that
  is stopped in that time and never runs again holds an answer with no entry.
- Each thing that no log gives: `:turns`, with its reason (D); and the task of a row of the base, which
  rests on the number of tasks and on Rama's hash (E).
- A depot of answers grows by about 135 bytes for each answered act, and by as much again for each
  repeat and each record that Rama runs again. Nobody has priced the disk.
- Names that Rama cannot change once real data is kept: "I".
- What no run reached: G, "What a proof cannot reach", and the probe's own list.

## The skill's checks, held against this plan

Phase 1's last step. Each is a check of `artifact-plan-validation.md`.

- **Stream unless justified; a stream concern that does not need stream.** No topology is added. The
  keeping of the streaming gate is in the stream topology because it must follow the decision of the
  same event; moved to a microbatch topology it would need a source on the gate's answers, which is a
  reader of the log or a second road to an answer (R3).
- **Concurrent clients; a client that restarts; a worker that restarts at any point.** B, every row. A
  client holds no state of the keeping.
- **Scale.** `:to-keep` is subindexed and its values are bounded at 256 by the code. A depot of answers
  grows without end, by decision (entry 3).
- **Each non-idempotent write of a stream topology.** One: the append. Its resolution: it does no harm,
  because each record under one act-id is a copy of one committed row and a reader takes one (B, C).
- **A stream topology that writes to several partitions.** It does not: the boundary is to its own task.
- **Internal depots; data between topologies.** No topology consumes either depot. They are justified by
  what they carry, which no client's depot holds.
- **The commit boundary before an append from a stream topology.** There.
- **Minimality.** The simplest design that could meet the spec is M-A with S-A: append in the attempt
  that decides, and let the rebuild take the last entry. Each thing beyond it, and what is lost without
  it, in the spec's words:
  - the commit boundary and the copy on a hit: without them the log holds answers that the tables never
    held, against "Nothing is shown before it is in the tables" (R5), and the skill's own check fails;
  - `:to-keep` and the append by the next run: without them a failed attempt's entry stays, and "An
    entry of an attempt that did not count does no harm" (B5) rests on the order of a partition and on
    one version of the code;
  - `:to-close`: without it a batch of strays alone is never closed, against "each answer that a gate
    gave is in a log" as ruling 8 reads it for a stray;
  - one keeping task for a batch: without it the closing reads on every task;
  - the two sends of a stray, and `:task` in its entry: ruling 8;
  - two depots and not one: ruling 18.
- **Throughput.** The cheaper designs are constructed and costed in A: S-A saves one boundary for an act
  and the copies; M-A saves the puts and one read for a batch. Each loses on what the log then holds,
  not on cost.

## Design difficulty log

- **Whether the streaming gate's append stands in the group of its writes or behind a commit.** This was
  the closest call. The first form is cheaper and can never leave an answer without an entry; I took it
  for the better design until I set it against two things. The stream probe shows the record readable
  before the row, and sometimes with no row ever. And the second form has a property that the first
  cannot have: the log holds only answers that the tables hold, so the question of B5 has nothing to
  answer. What settled it was S, on the shape with check (a) in front: the one window of the second
  form, a row with no record, is closed by the same read that the gate already makes.
- **What to do with a repeat.** I did not want a repeat to cost an append. Two other forms were built on
  paper: a mark that the answer is kept, and a copy only when the digest is the same. The first charges
  every act to spare a rare one. The second saves nothing that matters, since Rama's second run has the
  same digest. So the copy stayed, and K2 says so.
- **The microbatch gate.** My first design had each task keep and append its own, and I ran it. Filling
  in the table of seeks showed it wrong by the skill's own test: the closing read on every task, and the
  appends grew with the tasks. One keeping task for a batch came from that table, and was run again. I
  weighed keeping every batch on the gate's task and put it aside: every record of the base would then
  sit on one partition.
- **What an entry names the act by.** I had the text of the answer's key, because the table of what is
  to keep holds texts and a rebuild writes that key. The build session's note on what Rama cannot
  change turned it: a key's form is the one thing of a table that cannot be changed in place, and the
  log must not carry it.
- **`:turns`.** I looked for a cheap way to make it again and found one read on every task in every
  batch. Nothing reads the field after its batch, so the read would be paid for nothing.
- **Not contested**: the ack level, once the stream probe had lost an append with none; that no topology
  reads the log; and that the last stamp and the clock of a batch travel with the batch's first record.
