# Plan — the gates keep the line (stage 1 of piece 3)

<!-- Phase 1 of the Rama skill, for the stage `gates-keep-the-line` of DECOMPOSITION.json. -->

**22 November 2026.** Written by one Claude session, alone, in a fresh context,
on Fable. It was asked for as one of two plans for the same stage that do not
lean on each other; the other, on Opus, was stopped twice by a safeguard
before it wrote anything, so this is the stage's one plan
([HANDOFF.md](HANDOFF.md) says the rest). The session that asked for it
changed these two paragraphs and the file's name, and nothing else. It is not
yet checked: Phase 2 has not run. It read [SPEC.md](SPEC.md)
whole, [IMPLICIT_SPEC.md](IMPLICIT_SPEC.md) whole, the stage's scope in
[DECOMPOSITION.json](DECOMPOSITION.json), the store's code as piece 2 left it,
and the Rama skill's references. It ran one probe on Rama's in-process cluster,
twice ("What was run", at the end). It wrote no code of the store and changed
no file but this one. Where SPEC.md and IMPLICIT_SPEC.md differ, SPEC.md
governs.

## How to read a claim here

Each claim carries its source in brackets: a file and what it says, a document
of Rama, or the probe and what it printed.

- **read**: in a file of the repository, in the Rama skill, or in Rama's docs.
- **run**: printed by the probe of this plan, or by a proof named with it.
- **the plan's**: this plan's own conclusion, or its count from the dataflow.
  Nothing in this plan was measured on disk.
- **a session's**: a choice that Sid can turn. All are listed again near the end.

## How the skill's template is adapted

The template is for a new module. This stage changes a module that is built, as
the plan of the read build did before it
[read: `backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md`, "How the
skill's template is adapted"]. So:

- **Reads** lists what a later stage must be able to find in what the gates
  keep. The reads themselves are the later stages'. "The hand-off" is the one
  section that they take whole.
- **Writes** lists what a gate keeps for each kind of thing.
- **PState Design** holds every road that was found, with its cost.
- **Topologies** says what each gate's walk gains. No topology is added.
- Sections that the template does not have and the task asks for: the two
  facts of Rama (Q7, Q4), the setting's key, the second run, what the walk's
  contract costs, the proofs, the pages, where the spec is not met as written,
  and a session's choices.

## The design in short

For each decision, on the layer's task, in the same event or the same part of a
batch as the answer, a gate writes three things more, each a whole value at a
fixed key, with no read:

1. **a line row**, under `"<layer> <stamp, 19 digits>"`: the act's uuid, the
   answer's fields again, the who that the thing names, and the thing's weight;
2. **the thing again**, under the same key in a table of its own, when its
   weight is within a bound of the build;
3. **a row by who**, under `"<layer> <who> <stamp, 19 digits>"`, when the thing
   names a usable who.

The depot is not touched. The answers' table stays as it is, so check (a) and
the door's read by act-id cost what they cost today. No key holds a task or a
position in a depot.

## Reads

No read is built in this stage. This is what the later stages must be able to
find, and where the kept state gives it. Access method and path are theirs.

| What a later stage must find | Source of the requirement | Where the kept state holds it |
|---|---|---|
| A layer's decisions from a stamp on, in the order of the stamps, a bounded count | SPEC "The read" | The line rows of one layer are one run of neighbouring keys, in the numeric order of the stamps: one seek and one step for each row |
| Each decision's act-id and its answer, equal to the door's | SPEC "What must hold" 6; IMPLICIT R8 | The line row holds the uuid and the four fields of the answer's row; the layer is in the key and the depot is the gate's |
| The thing whole, "the arrival that was decided" | SPEC "The read" | The thing at the same key, which is the object that the walk decided |
| A bound in bytes on what is fetched, a thing over the bound told apart | SPEC "Bounded, by count and by bytes"; IMPLICIT R15, R16 | The line row holds the thing's weight, and a word when no thing is kept; the thing is in another table, so a row is read without it |
| Whose a decision is, with no thing fetched | SPEC "The who of a refused thing is read from the raw thing"; IMPLICIT R7, R16 | `:who` of the line row |
| Her own things in a layer that she may not see, at a cost bounded by what is hers | SPEC last section, "Her own things, without a walk through everyone's" | The rows by who: her decisions in one layer are one run of neighbouring keys |
| The layer's last stamp | SPEC "The read"; IMPLICIT L1 | Unchanged: `:last-stamp` of the gate's table for a task [read: `record/tables.clj`, `task`] |
| The setting | SPEC "The setting" | A fact's row at `"<layer> <layer> :softland.store/who-may-read-acts"` in the facts table |
| A stray, and a second thing under a used act-id, are in no read | SPEC "A stray is not in it"; "What must hold" 6 | Neither has a line row, a thing or a row by who |
| The same read kept live | SPEC "Whether the read can also be kept live" | The line is a top-level map whose rows hold no run of bytes; a new decision is a new key at the end of its layer's run |

## Writes

No depot is added and no depot is changed. A write stays what it is today: a
client's append to `*streaming-depot` or `*microbatch-depot`
[read: `module.clj`]. This table is what one decision writes more.

| What arrived [read: IMPLICIT O1, the six kinds] | Line row | The thing again | Row by who |
|---|---|---|---|
| A stray | no | no | no |
| A second thing under an act-id that has an answer | no | no | no |
| A thing that the declaration refused | yes | when it has a weight within the bound | when its `:who` is a usable id |
| A thing that a gate's check refused | yes | yes | yes |
| A taken act | yes | yes | yes |
| A thing for a layer of the other gate, with this depot in its act-id | yes, in this gate's record, under this gate's count for that layer id | as above | as above |

The last row is a session's choice. Such a thing is decided and stamped in the
gate where it arrived [read: `gate/decision.clj`, `standing`], and this plan
keeps one rule: every decision of a gate has a line row in that gate's record.
SPEC's default for Q2 is that no read of a line gives those rows. They cost
three writes on a thing that is rare, and turning Q2 later then costs nothing.

## PState Design

### The question, and the eleven roads found

SPEC "What Phase 1 has to weigh", first two bullets: where the thing whole comes
from at a read, and whether the answers' table can serve the order by stamp.

Costs are for one decision, the same in both gates unless said. Today's costs
are the walk's own count [read: `module/walk.clj`, "What one call costs"].
"1 to 6" are SPEC "What must hold" 1 to 6.

**Road A. The gate keeps the thing again, as it holds it, beside a small row. Taken.**
- Cost: no read. Three writes at most: the line row, the thing, the row by who.
  One walk of the thing more on the task's thread, for its weight.
- Kept twice: each thing whose weight is within the bound, once in its depot
  and once in the things table; the four fields of the answer, in the answer's
  row and in the line row; the who, in the line row and in the key of the row
  by who.
- 1: holds. Each row is made of the thing, the rows read and the stamp. 2:
  holds. The keys hold a layer, a stamp and a who. 3: holds. Writes and pure
  calls only. 4: holds. The weight cannot throw, and the bound holds the size
  of what a gate writes. 5: holds. All of it is on the layer's task; for the
  base that task also holds the last stamp, the answers and a whole copy of the
  base's facts [read: `module/microbatch_gate.clj`, "Where a row lives"]. 6:
  holds. Every write is behind check (a), and the thing written is the object
  that was decided.

**Road B. The gate keeps the position of the thing in its depot; the read takes the thing from the depot.**
- Cost: no read, one small write. Nothing is kept twice.
- It fails on four grounds, each enough.
  - Rama gives a topology no position of its record. A stream source binds the
    record's value and nothing else, and so does a microbatch source
    [read: skill `stream.md`, "Source binding"; `microbatch.md`, "Source
    binding and emitting records"]. The public names of `com.rpl.rama.ops` that
    hold "offset", "position", "record", "depot", "task-id" or "microbatch-id"
    are `current-microbatch-id` and `current-task-id`, and no other [run: the
    probe, part A]. `microbatch-depot-info` takes a cluster, a module's name
    and a topology's name, and gives "a microbatch topology's latest processed
    offsets": a client's call, and not the record's place [run: the probe,
    part A, Rama's own docstring].
  - A gate could count the records of its partition instead. The streaming gate
    cannot keep such a count right: a stream topology "can retry a record even
    after all PState writes have completed and committed" [read: skill
    `stream.md`, "Stream retries and exactly-once"], and a stray and a second
    thing under a used act-id stand behind no check (a) that would stop a
    second count [read: `module/walk.clj`, steps 0 and 1].
  - "A position differs between a store of 4 tasks and one of 8" [read: SPEC
    "What must hold" 2].
  - The read. A client reads a depot by one partition and a range of offsets
    [read: skill `depot-reference.md`, "Reading Ranges"]. A layer's decisions
    are not neighbours in their partition [read: `gate/stamp.clj`]. So a page
    of n decisions is n calls, where the number of round trips must not grow
    with n [read: IMPLICIT O3, "Latency"]. And the fetch "is done as a single
    synchronous event" with a rule of thumb of 50 kb [read: Rama's docs,
    `depots.html`, kept at `research/softland/rama-reference/docs/14-depots.md`,
    line 242]: a refused thing has no bound.

**Road C. The gate keeps the thing again as bytes that it makes itself, in parts of a bounded size.**
- Cost: no read; the line row, the row by who, and one write for each part.
  One serialisation of the thing on the task's thread, inside the walk.
- Kept twice: the thing, as bytes.
- What it buys: an exact size, a position of the class of a byte array, no
  large value anywhere, and a thing of any size read back part by part.
- Why it is not taken. The only serialiser at hand is Nippy, which Rama uses
  "under the hood" [read: Rama's docs, `clj-serialization.html`]; the namespace
  `taoensso.nippy` loads on the store's classpath [run: the probe, part A].
  That bytes made by a direct call thaw to the thing as it arrived, for every
  kind of thing that can come off a depot, is said by no document. The read
  path would then hand a caller what this code thawed, and not what Rama gave.
  Road A lets Rama write what Rama read.
- What would turn the choice to C: a bound of a real cluster on one PState
  value or on one answer below the bound of Road A; or Sid's word that a thing
  heavier than that bound must be read back by stamp.

**Road D. The walk keeps the small rows; a second topology over the same depots writes the things.**
- Cost: the walk writes two rows more. The second topology reads the answer by
  act-id, one read, and writes the thing, for each thing of each depot.
- Kept twice: the thing.
- It fails: the thing would become visible later than the answer and the last
  stamp, and "no reader ever sees a last stamp s for which O3 has no decision
  at s" [read: IMPLICIT W3]. The second topology would have to find "the
  arrival that was decided" again, and the digest leaves the time out
  [read: `door/digest.clj`, `without-the-time`]. It buys the thing's write off
  the walk's thread.

**Road E. One row for each decision that holds the small fields and the thing.**
- Cost: one write, and the row by who.
- It fails: a value nested in a top-level map is "stored as single serialized
  values" [read: skill `pstate-schema.md`, "Storage Model"]. A read of a row
  then loads its thing. A visitor's page withholds almost everything, and its
  cost must not "grow with the bytes of what is withheld" [read: IMPLICIT R16].

**Road F. The answers' table serves the order by stamp.** SPEC's second bullet.
- It cannot as it is: its key is `"<layer> <uuid>"` [read: `record/rows.clj`,
  `answer-key`], and a uuid is random, so a layer's answers lie together and in
  no order of stamps.
- F1: the answer moves under the stamp, and the uuid's key holds the stamp
  alone. No more writes than today's two. But the door's read by act-id becomes
  two reads, and it must stay "one read of one key" [read: IMPLICIT "What
  exists and must come out unchanged"]. Every maker reads her answer by act-id
  for every act [read: `src-store/README.md`, 7 October, entry 6]. A seek is
  0.3 to 0.5 ms and a buffered write far less [read: skill `SKILL.md`,
  "Performance costs"]. Road A's one write more for each decision is cheaper
  than one seek more for each read of an answer [the plan's].
- F2: the line row holds the uuid alone, and the read takes each answer by its
  key. A page of n decisions then costs n seeks more [the plan's].
- Taken instead: the line row holds the answer's four fields again. Both rows
  are made of the same values in the same decision.

**Road G. The gate keeps the one text of the thing.** Not the thing as it
arrived: the text writes a record, a Java collection and a symbol as `#other`
and a deep part as `#too-deep` [read: `door/text.clj`]. SPEC asks for "the
thing whole, as it arrived on the depot".

**Road H. A gate keeps nothing more; a read walks the depot partition.** A walk
through everyone's things, with no bound [read: SPEC "Her own things, without a
walk through everyone's"].

**Road I. For the base, the line on every task, as the base's facts are.**
- Cost: for each decision of the base, a refusal too, one message to each of
  the N tasks and three writes on each of the N - 1 others; today a refusal
  goes nowhere [read: `module/microbatch_gate.clj`, "The copy"]. Each thing of
  the base is then stored N times.
- Not taken. The rows that say who may read are on the base's task already,
  with the last stamp, so one task holds all that a read of the base's acts
  needs. What it would buy is reads of the base's acts on every thread. What
  would turn it: reads of the base's line so many that the one task that
  decides the base cannot carry them. Then the line rows alone would be
  copied, and never the things.

**Road J. No rows by who; an asker outside a layer names the stamps of her own things.**
- Cost: nothing for a decision. A read of k named stamps costs k seeks, each
  held against `:who` of the line row.
- Why it is not taken. SPEC's read is "from a stamp on", and an asker who may
  not see the layer "is given her own things there". From a stamp on, with no
  order by who, the read walks the layer's line. And what she sent would be
  listed nowhere in the store unless she kept its stamps herself.
- What would turn it: Sid's word that an outside asker reads back only what she
  can name. The table and its one write then go, and `:who` of the line row
  serves alone.

**Road K. The line and the rows by who as two fields of the gate's table for a task.**
- Cost: the same writes. One read more for each run that is read: a field and
  then a key are two levels [read: `record/tables.clj`, on `:last-stamp`].
- Not taken: the run by stamp is the read that is asked most, and a run of a
  top-level map's keys is the form that Rama's push was seen to watch
  [read: `record/tables.clj`, on `facts-by-value`]. It would save two PState
  partitions for each gate on each task. Close for the rows by who; a
  session's choice.

### The three tables

One schema for both gates, as the four that exist [read: `record/tables.clj`].
Each gate declares three PStates more with them.

| Table | Key | Value |
|---|---|---|
| `line` | `"<layer> <stamp>"`, the stamp as 19 decimal digits with zeros in front | a fixed-keys row: `:act` UUID, `:digest` String, `:refused` Keyword, `:time` Long, `:who` String, `:weight` Long, `:not-kept` Keyword |
| `things` | the same key | the thing: `clojure.lang.IPersistentMap` |
| `line-by-who` | `"<layer> <who> <stamp>"`, the who as the printed text of its id | the stamp, a Long |

**The key.** A layer's text and a space, as every key of the record begins
[read: `record/rows.clj`, "Why the keys are strings"]. The stamp has a fixed
width, because Rama orders string keys by their bytes, and "10" is before "9".
Nineteen digits hold 2^63 - 1, the greatest stamp [read: `gate/stamp.clj`,
"Limit"]. So the order of the keys of one layer is the numeric order of its
stamps [the plan's; held by a pure proof].

**Why flat maps.** A map inside a map costs one seek more for each read
[read: `record/tables.clj`, "Why flat maps"].

**Why the rows by who are not more keys of the line.** Their key has three
parts where a line row's has two, and their value is another class. In one map
the two would also mix: the printed text of a who that is a UUID begins with
digits, so its keys would sort into a layer's run of stamps [the plan's]. The
facts by value are a table of their own for the same kind of reason
[read: `record/tables.clj`, on `facts-by-value`].

**The line row, field by field.**
- `:act`: the uuid of the act-id. The key gives the layer and the gate's own
  record gives the depot, as for an answer [read: `record/rows.clj`, `answer-key`].
- `:refused`, `:time`: as in the answer's row; absent for an accept, and for a
  thing with no usable time.
- `:digest`: the digest as 64 hex characters, the form in which a caller gets
  it [read: `record/rows.clj`, `answer-of`]. A String and not a run of bytes: a
  push on rows that hold a byte array sent the whole value anew about once a
  second [read: `record/tables.clj`, `facts`, run on Rama 1.6.0], and SPEC asks
  the last stage to weigh a read kept live. It costs 32 bytes more for each
  decision. A session's choice.
- `:who`: the one text of the id under the thing's `:who`, when that is a
  usable id [read: `door/declaration.clj`, `usable-id?`]; absent otherwise. It
  is read from the thing as it arrived, also for a thing that the declaration
  refused for another fault.
- `:weight`: below. Absent for a thing that cannot be weighed.
- `:not-kept`: absent when the thing is in `things` under the same key. Else a
  word: `:too-heavy`, or `:not-weighed`.

The stamp is in the key and not in the row.

**Two tables under one key.** The skill's rule is one PState for data that
shares a key and a partitioner. The line and the things share both, and are
split on a cost: a nested value is one serialised value (Road E), so one PState
would load a thing with each row. Split, a holder's page of n decisions is two
runs, 2 seeks and 2n steps. As one row it is 1 seek, but a visitor's page of n
withheld decisions reads n things from disk, each of up to the bound below
[the plan's]. The split costs one PState partition more for each gate on each
task.

**The class of a thing.** Each thing that is no stray is a plain map and no
record [read: `door/declaration.clj`, `stray-reason`, `plain-map?`]. A position
of the class `clojure.lang.IPersistentMap` took an array map, a hash map and a
sorted map from a stream event, and each came back of its own class and equal
[run: the probe, part B]. The inside of a thing has no schema, and can have
none: "A thing can be anything" [read: SPEC "What must hold" 4]. The skill's
rule against the class Object is met at the position and cannot be met inside
it. See "Where the spec or the skill is not met as written".

**The weight.** A pure function of a thing, the same in every process, which
cannot throw. It is new, and stands beside the one text and the digest.
- For a thing that the one text writes out whole, it is the number of UTF-8
  bytes of that text [read: `door/text.clj`, `text` and `utf8-count`].
- Where the text stops (a record, a Java map or collection, an array, a symbol,
  a ratio, a float, a character, a part more than 64 collections deep), the
  weight goes on: a record as the map of its fields and its class's name, a map
  or a collection as its parts, the others as their printed text.
- It gives no weight for a thing with a part of a class that it does not know,
  and for a thing that throws when it is read.
- It walks with a stack of its own and not by calling itself, so no depth
  overflows a thread's stack.
- Cost: one walk of the thing, of the order of the digest's walk, which each
  thing already pays before the declaration refuses it [read:
  `src-store/softland/store/README.md`, "How long one act can hold a task"].
  The build may feed the weight and the digest from one walk; this plan counts
  two.

Why a measure of the gate's own, and not the size that Rama writes: Rama gives
a topology no call that says the bytes of a value [read: the skill's
references name none]. What a weight of w is on the wire is not measured.

**The bound on what is kept: `max-kept-weight`, 33,554,432 (32 MiB).** A number
of the build, a session's. A gate keeps the thing again only when it has a
weight and the weight is not over this number.

- Why any bound. Without one, a single thing of a size that Rama's depot takes
  and a PState value does not would make the gate's write throw in every run of
  its event, and "a thing that throws each time stops the partition"
  [read: IMPLICIT W6; `module/microbatch_gate.clj`, "A throw is no quiet
  retry"]. Today no thing can do that: a gate writes a small answer for it. And
  a write for one event must be bounded by what the application controls
  [read: skill `phases.md`, "Cross-phase rules"].
- Why this number. Every thing that passes the declaration is written out whole
  by the one text [read: `door/text.clj`, `max-depth`], so its weight is its
  text's bytes. This plan's sum from the declaration's limits
  [read: `door/declaration.clj`, the limits; `act.clj`, `max-subjects`,
  `nesting`], with 768 bytes for an id of 256 characters:

  | Part | At most, in bytes |
  |---|---|
  | The texts of the values (`max-act-bytes`) | 18,874,368 |
  | 256 facts, each its entity, its key, what it replaces, its part names | 627,200 |
  | 1,024 entries of `:based-on` | 893,000 |
  | 256 subjects | 197,000 |
  | A permission 32 deep | 99,500 |
  | Who, layer, session, act-id, because-of, and the small parts | 4,300 |
  | Sum | about 20,695,400 |

  So every taken act, and every thing that a gate's check refused, is kept
  again, with room. A pure proof holds the sum against the number.
- A thing over it is one that the declaration refused. Its line row says
  `:too-heavy` and holds its weight. It is in its depot and in no read by
  stamp.

### What the store then keeps twice, and how much

[the plan's; sizes are estimates from the fields, not measured]

- **Each thing of weight up to 32 MiB**: once in its depot, once in `things`.
  The second copy is in RocksDB, which writes a value again when it compacts; a
  depot is a log. So the second copy costs more disk work than the first.
- **The answer's fields**, about 100 bytes for each decision.
- **The who**: in the line row and in the key of the row by who.
- A line row with its key is about 200 to 300 bytes; a row by who about 60 to
  120.
- In the base: once, on the base's task. Nothing of this is copied to another
  task.
- A flood of refused things from one sender: each costs three writes more and
  no read, and its bytes a second time when it weighs 32 MiB or less. The depot
  already takes the same bytes with no bound. The bound on what one sender can
  put into a depot is "not built, and due before a stranger can reach the door"
  [read: `src-store/README.md`, 12 October, entry 7]. That is where a flood is
  stopped, for the depot and for this copy alike.

## Depots

None is added, and none is changed [read: `module.clj`: two depots, each
`(hash-by :layer)`]. No topology appends to a depot, before or after
[read: IMPLICIT O1, "this piece adds no topology that appends to a depot of
acts"].

## Topologies and PStates

No topology is added. The module keeps its one stream topology and its one
microbatch topology.

- **`streaming-gate`: stream.** Why this type: a maker's append with `:ack`
  returns when her act is decided and its rows are visible
  [read: `module/streaming_gate.clj`, "What an ack means"]. The new writes
  belong in it: they must become visible with the answer (W3).
  - Each new write is a whole value at a fixed key, behind check (a). A record
    that Rama runs again after a commit finds its answer and writes nothing. A
    run after a discard writes the same values, since the weight, the who and
    the stamp are made of the thing and of the rows read
    [read: `module/walk.clj`, "When a thing is run again"]. No write here
    appends or counts.
  - It writes on one task and never hops, so no part of an event can be left
    out by a failure.
  - PStates more: `$$streaming-line`, `$$streaming-things`,
    `$$streaming-line-by-who`.
- **`microbatch-gate`: microbatch.** Why this type: unchanged, the base
  [read: `module/microbatch_gate.clj`, "What it is built for"].
  - The three writes are on the deciding task, in that task's part of the
    batch, with the answer. A second attempt of a batch writes the same values.
  - The copy to every task is not touched: the walk gives a gate only the rows
    of facts and of the facts by value to copy [read: `module/walk.clj`,
    `copies-of`].
  - PStates more: `$$microbatch-line`, `$$microbatch-things`,
    `$$microbatch-line-by-who`.

Schemas, in full:

    line          {String (fixed-keys-schema {:act UUID  :digest String  :refused Keyword
                                              :time Long  :who String  :weight Long
                                              :not-kept Keyword})}
    things        {String clojure.lang.IPersistentMap}
    line-by-who   {String Long}

**The walk, against today's.** `decide` takes three tables more. Behind check
(a) it makes two pure calls more on the thing as it arrived: its weight, and
its usable who. `decision/writes` takes the thing with them and gives three
entries more, in front of the answer and the last stamp. The loop of writes
gains three cases, each a `keypath` and a `termval`. The layer's last stamp
stays the last write. A stray and a second thing under a used act-id end before
any of this: nothing is added to them, not the weight either.

No added step can throw (W6). The weight and the usable who give nothing where
a thing throws. Each key is made of the layer of a thing that is no stray,
which is an id within the limit [read: `door/declaration.clj`,
`stray-reason`], of a who that passed `usable-id?`, and of the stamp. Each row
holds exactly the classes of its schema, and is made only by
`record/rows.clj`, as every row is [read: `record/rows.clj`, "The functions
that make a row"].

**Whatever is added goes through `add-gates-and-reads`** [read: IMPLICIT A5]:
each gate's `add-to` declares its three PStates, so `StoreAgain` runs them.

## Query Topologies

None in this stage. The door's two are not changed [read:
`module/door_reads.clj`].

## Partitioning efficiency

**The placement that the reads want.** Every read that SPEC names is of one
layer: a run of its line, one decision by its stamp, one who's decisions in it.
So all that a layer's line holds should be on one task, the task that holds the
layer's last stamp and the rows that say who may read. That is the layer's
task. Each depot puts a thing there [read: `module.clj`, "How a depot
partitions"], and the walk writes where it runs. No partitioner is added.

The four kinds of read, with shares that are this plan's estimate and rest on
no measure. "Seeks" is the number of tasks that a read goes to, as the template
counts it. "Steps" is for a page of 100.

| Kind of read | Share | Tasks read, N = 1 | N = 16 | N = 128 | Steps |
|---|---|---|---|---|---|
| A page of the line, by a holder, an importer or a poller | 0.60 | 1 | 1 | 1 | 100 rows and up to 100 things |
| One decision by its stamp | 0.25 | 1 | 1 | 1 | 1 row and 1 thing |
| A visitor's page: stamps, and her own things whole | 0.10 | 1 | 1 | 1 | 100 rows and her own things |
| An outside asker's own things | 0.05 | 1 | 1 | 1 | her rows by who, then each row and thing |

Weighted tasks read: 1.0 at N = 1, 1.0 at N = 16, 1.0 at N = 128. Flat.
The same for the base, whose line is on the base's task alone.

**A layer that is far larger than the others.** One layer's line is on one
task, as its acts are one line of one partition: Sid's decision 3 of 13 October
[read: `src-store/README.md`]. This stage adds the things to that task. Another
placement was weighed: the things of a large layer spread over tasks. For the
streaming gate it needs a partitioner behind the decision, and then the thing
commits apart from its answer [read: `module/walk.clj`, "A limit for a gate
that copies"]. For the base the batch would commit both together, but a page of
100 would then read up to 100 tasks at N = 128, where it reads 1. Not taken, on
that count [the plan's].

## Design Decisions

- **Subindexing.** None is added. The three tables are top-level maps, and Rama
  keeps each key of a top-level map as its own entry on disk [read: skill
  `pstate-schema.md`, "Storage Model"].
- **Colocation.** The depots partition by the layer, and each new row is
  written on the task of the decision. In the base, on the base's task alone.
- **A cache.** None. Nothing is kept in the memory of a process.

## The two facts of Rama

**Q7. Is a topology given the position of the record that it decides?** No, by
every document and by Rama's public names; see Road B. What is settled is that
no documented operation gives it. That is all that a design can build on.

**Q4. Rama's own bounds on a record, a PState value and an answer.** Not
settled from documents: the pages on depots, PStates and all configs, as kept
under `research/softland/rama-reference/docs/`, state no greatest size [read: a
search of 14, 15 and 26 for a size bound found the depot read's rule of thumb
and nothing else].

- What was run: one thing of 24 MB, of 67 MB and of 163 MB of text was
  appended, written again as one PState value by a stream event, and selected
  back equal; a small thing behind them was decided and read [run: the probe,
  part C: append with `:ack` 438, 988 and 2,644 ms; select 234, 740 and
  1,817 ms].
- What that does not show: a real cluster. In process nothing crosses a wire
  and nothing is replicated.
- How the plan handles it: no gate writes a value heavier than 32 MiB, five
  times under what was seen to work in process; and the later read holds its
  own bound in bytes, which this stage gives it the weight for.
- The probe that would settle the rest: on a cluster of more than one machine,
  append one thing of 32 MiB to each depot, and select its row of `things`. It
  belongs on the page of what must be true before real data is kept
  [read: `src-store/README.md`, 12 October, entry 6, its eighth].

## The setting's key, as a rule of a gate

`:softland.store/who-may-read-acts`, the eighth store key. The word is SPEC's
working word, a session's.

- **In `store-keys`**: its value has the shape of a new word shape, `:members`
  or `:everyone`; it sits on the layer; it is a setting; it is written again.
  Four rules of a gate then follow with no new check, each read from that
  table [read: `door/declaration.clj`, `setting-keys`, `second-write-keys`;
  `gate/decision.clj`]:
  - **who writes it**: an act under the layer's first permission, and no other
    (`who-may-write-fault`, `:only-the-first-holder-writes-a-setting`);
  - on an entity that is not the layer: `:setting-not-on-the-layer`;
  - with no value: `:store-key-retract-not-built`;
  - a making act cannot carry it: its form is exactly two facts
    [read: `gate/first_act.clj`, `making-act?`]. So it takes a second act.
- **One rule is new**, in the declaration: the value is one of the two words,
  else `:who-may-read-acts-fact-not-members-or-everyone`. It stands right
  behind the rule of who may see a layer, beside the rules of the other
  settings.
- **What it reads as with no fact**: `:members`, for every layer, the base too.
  `starts-with` gains the key [read: `gate/first_act.clj`]. No gate calls that
  function: a reader does.
- **The rule that changes on purpose**: a fact under this key is refused
  `:store-key-not-known` today, and is taken after [read: IMPLICIT S5]. For it
  the fixture is made again.
- **What it costs a gate**: what a setting costs today, 4 reads (the answer,
  the permission, two for the last stamp), and 6 writes where it was 3 [the
  plan's count from the walk's steps: a setting reads no place and no marked
  keys]. In the
  base its fact's row is copied to every task, as each setting's is.
- **Before the rule is built**, its statements go into the reference of the
  generated histories [read: IMPLICIT A10]. Another session is writing in that
  folder now; the build takes it up with that session's work in place.

## A second run of the gates makes the same tables (O6)

- **A1.** The three tables of each gate join the comparison of the run-again
  proof: row for row between `Store` and `StoreAgain`, and against the fixture
  on a fresh store of 4 tasks and of 8.
- **A2, A3.** Each of the three holds a row on the layer's task alone, so the
  form with no task holds the union of the tasks' rows, the second of its three
  forms [read: `run_again_proof.clj`, "The form with no task"]. No key and no
  value holds a task or a position. Nothing is left out of the form.
- **A4.** A row is made of one thing, of rows of its own layer and of its
  layer's stamp. The order of two partitions or of the two depots is in none.
- **A5.** See "Topologies".
- **A6.** The rule for bytes stays: the one position that is a run of bytes is
  `:digest` of an answer's row. `:digest` of a line row is a string in its
  table. A thing is written into `tables.edn` as itself; the hand sends only
  what EDN holds, as today, and the making fails on anything else. The things
  that EDN does not hold are held by the module's proof on the cluster.
- **A7.** The fixture is made again, the sixth making, for the rule of the
  key. The hand gains things that write the setting and things that are
  refused for it. The fixture's page and the receipt of the build say so. It is
  not made again to make a check pass.
- **A8.** Check 4 compares the three tables too, after each of its failures.
- **A9. The decisions from before this build.** A PState is not made again from
  a depot when a module is updated [read: skill `SKILL.md`]. A store that held
  decisions before this build would hold no line row for them, and its line
  would not start at 0. This plan's position: this build reaches a store that
  holds decisions only by a second run of the gates over its depots, the aim of
  13 October; never by an update in place. No real data is kept today, and
  the rule of versions "starts with the first real record" [read:
  `src-store/README.md`, 13 October, entry 6]. For the next stage: a stamp
  below the layer's last stamp with no line row is a fault to say, and never
  the end of a line.
- **A10.** See "The setting's key".

## What the walk's contract costs with this

The contract: no partitioner, no yield, no read that may yield and no append
between the walk's first read and its last write [read: `module/walk.clj`, "It
never gives its task back"]. None is added. What is added is on the task's
thread, inside that stretch.

| What arrived | Today: reads, writes | After: reads, writes |
|---|---|---|
| A stray | 0, one add to a set | the same |
| A thing under an act-id that has an answer | 1, 0 | the same |
| A thing that the declaration refuses | 3, 2 | 3, and 3 to 5 |
| An act whose permission does not stand | 5 at most, 2 | 5 at most, 5 |
| A making act | 5, 4 | 5, 7 |
| A give, or an end | 5, 3 | 5, 6 |
| One accepted act with one value under a plain key | 5, 3 | 5, 6 |
| The same, under a marked key | 6, 4 or 5 | 6, 7 or 8 |
| The greatest call | 8,228, 770 | 8,228, 773 |

In the microbatch gate the same, on the deciding task, and nothing more on any
other task. Counted from the dataflow as the walk counts [the plan's].

Beside the rows:
- **One walk of the thing more**, for its weight, for each thing that reaches
  step 2. For a thing of no bounded size that is work of the order of its
  digest, which it pays today.
- **The thing's bytes are written a second time**, when it weighs 32 MiB or
  less: Rama serialises it for the PState and replicates it. In a streaming
  layer the maker's `:ack` waits for that [read: skill `stream.md`, "Streaming
  batch-level visibility"]. Run in process, with no replication: an event that
  does this one write, for a thing of 24 MB, gave its `:ack` after 438 ms
  [run: the probe, part C]. For an act of ordinary size no run here measured
  the addition.
- **A read of the task waits** for the walk as it does today, and that longer.

## The hand-off: what a later stage may rest on

1. **Every decision has a line row.** For each layer id of which a gate has
   decided a thing, that gate's `line` holds one row for each stamp from 0 to
   the layer's last stamp, on the layer's task and on no other. It becomes
   visible with the answer and the last stamp.
2. **A line row never changes.** It is written once, as the answer's row is.
3. **The answer in a line row is the door's.** The uuid, the word of a refusal,
   the time and the digest are those of the answer's row under that act-id.
4. **The thing under a line row's key is the arrival that was decided**, when
   `:not-kept` is absent. Its digest is the row's digest, and its usable time
   is the row's time [read: IMPLICIT R9].
5. **The weight bounds the thing.** A thing that is kept weighs what its row
   says, and no more than `max-kept-weight`. A read that holds a bound in bytes
   reads the row first and takes the thing or leaves it.
6. **`:who` is a usable id, or absent.** It is what the thing names, which no
   gate holds against a sender [read: IMPLICIT R5].
7. **The rows by who of one who in one layer are one run**, in the order of the
   stamps. A who with no decision there has none, and that read is one seek
   that finds nothing.
8. **The base.** Its line, its things and its rows by who are on the base's
   task alone. That task also holds the base's last stamp, its answers and a
   whole copy of its facts, the permission rows and both settings among them.
9. **A layer id can have a line in each gate's record** (Q2). Which one a read
   of a layer takes is the next stage's, by SPEC's default.
10. **Not in any of the three tables:** a stray, and a second thing under a
    used act-id.

    **The door's lines of IMPLICIT_SPEC's matrix hold as today**, in every
    row: the answers' table, check (a) and the door's two reads are not
    changed, so the door's read by act-id stays one read of one key.
11. **What must hold before a key is made**, as for every key of the record
    [read: `record/rows.clj`, the last paragraph of its docstring]: the layer
    and the who are tested with `usable-id?`, and the stamp is a whole number
    from 0 to 2^63 - 1.
12. **A layer whose id holds a letter from U+E000 on**: Rama was seen to give
    fewer rows of a range for such keys [read: IMPLICIT R20]. These keys begin
    with the same layer text.

## State primitive selection

- `line`, `things`, `line-by-who` (PStates, for each gate): durable. One
  decision writes three values at most, and the bytes of one thing of 32 MiB at
  most. The store's own bounds control both.
- No TaskGlobal, and no outside system.

## Resource usage analysis

### Disk

[the plan's estimates; nothing measured]

| | One entry | Grows by |
|---|---|---|
| `line` | 200 to 300 bytes with its key | one for each decision |
| `things` | the thing as Rama serialises it, 32 MiB of weight at most | one for each decision whose thing is kept |
| `line-by-who` | 60 to 120 bytes | one for each decision whose thing names a usable who |

On a task that decides 1,000 acts a second, the store's target [read:
`src-store/README.md`, 7 October, entry 9], with things of 1 KB: about 1 MB a
second in `things`, 0.25 MB in `line` and 0.1 MB in `line-by-who`, beside about
1 MB a second that the depot takes for the same acts. No depot and none of
these tables is ever trimmed.

### Memory

No state in memory. One walk holds the thing once, as today.

### Minimization

- Is anything kept twice? Yes; "What the store then keeps twice" says what, and
  each road that keeps less fails a line of SPEC.
- Can a field of the line row go? Each is needed when the thing is not fetched:
  the uuid, the answer and the who for a decision that is withheld or too heavy.
- A row by who holds the stamp and nothing else.

## Proofs

In the form of the store's proofs: each check is a sentence, printed after
PASSED or FAILED [read: `src-store/CLAUDE.md`, "The checks explain
themselves"]. The sentences are drafts.

**Pure, with no cluster** (`pure_proof.clj`):
- "The weight of a thing that the one text writes out whole is the number of
  UTF-8 bytes of its one text."
- "A thing that holds a record, a Java list, a sorted map, a symbol, a ratio
  and a float has a weight, and each part is counted in it."
- "A thing with a part of a class that the weight does not know has no weight,
  and so has a thing that throws when it is read; neither throws."
- "A thing nested 100,000 deep is weighed and overflows no stack."
- "The sum of the declaration's limits is below the bound on what is kept, so
  each thing that passes the declaration is kept again."
- "The who of a thing that the declaration refuses for another fault is read
  from the thing; a who that is absent, no id, or over the limit reads as
  none, and so does a thing that throws."
- "The keys of the line put the stamps 9, 10 and 9,223,372,036,854,775,807 in
  their numeric order, and a layer's run holds no key of a layer whose text
  begins with its text."
- "A refusal writes its thing, its line row, its row by who, its answer and the
  layer's last stamp, the last stamp last; an accept writes them behind its
  facts."
- "A thing heavier than the bound writes no thing, and its line row says so; a
  thing that names no usable who writes no row by who."
- "The answer that a line row gives is the answer that its answer's row gives:
  the same verdict, reason, stamp, time and digest."
- "A fact under the key of who may read a layer's acts passes with `:members`
  and with `:everyone`, and is refused with any other value under a word of its
  own."
- "Each thing that an earlier rule refused keeps its word with the key in it."
- "Only an act under the layer's first permission writes the key; on another
  entity, with no value, or as a marked key, it is refused with the word of
  the other settings."
- "With no fact, who may read a layer's acts reads as members, for the base
  too."

**The module on the in-process cluster, both gates** (`module_proof.clj`):
- "A taken act, a thing that the declaration refused and a thing that a gate's
  check refused each have a line row at their stamp, with the act's uuid, the
  answer, the who and the weight, and the thing under that key is the thing
  that was sent."
- "A thing that holds a record, a run of bytes and a sorted map is read back
  under its stamp with the same one text and the same digest."
- "A thing heavier than the bound has its line row and its answer and no
  thing, and the thing behind it in its layer is decided."
- "A stray has no line row; a second thing under a used act-id changes none,
  and the thing under that stamp still carries the first time."
- "The line rows of a layer hold each stamp from 0 to its last stamp once."
- "Each decision whose thing names a usable who has a row by who, a stranger's
  refused thing in a layer that she may not see among them."
- "The base's line, its things and its rows by who are on the base's task and
  on no other."
- "After a throw in an act's event and in a batch of the base, each of the
  three tables holds what a store with no failure holds."
- "The first holder writes who may read the layer's acts and writes it again; a
  holder of a given permission is refused; in the base the row is on every
  task."

**The run-again proof** (`run_again_proof.clj`): its four checks take the three
tables of each gate; the placement holds that the task of a layer holds its
line, its things and its rows by who; the fixture is made again as A7 says.

**Not reached by these:** a real cluster; what a thing of 32 MiB costs with
replication; what Rama writes on disk for a thing of weight w. Each goes on the
proofs' page [read: `test-store/softland/store/README.md`, the sections "does
not reach"].

## Files that the build changes, in the order to build

1. `src/proposal/runtime-2026-11-21/gate-histories/`: the statements of the
   key's rules (A10).
2. `door/declaration.clj`: the key in `store-keys`; its word shape; its rule
   and word; `usable-who`; `max-kept-weight`.
3. A new pure namespace beside `door/text.clj` for the weight, with its entry
   in `door/README.md`.
4. `gate/first_act.clj`: `starts-with` gains the key.
5. `record/rows.clj`: the key of a line row and of a row by who, the two rows,
   the two ends of each run, the stamp of a key. `record/tables.clj`: the three
   schemas.
6. `gate/decision.clj`: `writes` takes the thing, its weight and its who, and
   gives the three entries. The module's proof puts a function of its own in
   place of `writes` for two groups [read:
   `test-store/softland/store/README.md`, "The module's proof"]; that stand-in
   changes with it.
7. `module/walk.clj`: the three tables, the two pure calls, the three cases of
   the loop; its count of each kind of thing.
8. `module/streaming_gate.clj`, `module/microbatch_gate.clj`: three PStates
   each, handed to the walk. `module.clj`: the six names, in its docstring.
9. The proofs, then the sixth making of the fixture.

Not changed: `module/door_reads.clj`, `gate/chain.clj`, `gate/stamp.clj`,
`door/text.clj`, `door/digest.clj`, `act.clj`, and the read path.

**Pages that change with the code** [read: SPEC "Pages that change with it"]:
`src-store/MODEL.md` ("What the store keeps" gains the line; "The store's own
keys" says eight; "What cannot be got back" keeps its three entries and changes
their reason: the gates keep the act by its stamp, and no read is built until
the next stage; the numbers gain the bound); the maps of `module/`, `record/`,
`gate/` and `door/`; `src-store/softland/store/README.md` (the cost of one act,
the words a session made); the proofs' page; the fixture's page. The "Decisions
of ..." sections are Sid's and are not changed.

## Where the spec or the skill is not met as written

1. **"H whole, whatever the thing is, within R15"** [IMPLICIT E1, "made x a
   thing that the declaration refuses arrives"]. Not met for two kinds of
   refused thing: one heavier than 32 MiB, and one with a part that the weight
   does not know. A holder gets the stamp, the act-id, the answer and a word,
   and never the thing, by any read by stamp. The plan's position: SPEC "What
   must hold" 4 asks that what a read gives back "must be bounded", and a gate
   that writes a value of no bound can be stopped by one thing. The thing is in
   its depot.
2. **"The place is chosen so that no thing that is refused today gets another
   word, except a thing whose only fault was this key"** [IMPLICIT S4]. No
   place meets it. A thing with a fact under this key and a fault that a later
   rule or a gate's check names is refused `:store-key-not-known` today; after
   the build it is refused with the word of its other fault. What can be met,
   and is: each thing that an earlier rule or the shape refuses keeps its word.
3. **"Never use `Object` as a schema type"** [skill `phase-1-plan.md`, schema
   rules]. The position of a thing has the class of a Clojure map. Its inside
   has no schema: "A thing can be anything" [SPEC "What must hold" 4].
4. **"Do NOT declare separate PStates for data that shares a key and a
   partitioner"** [skill `phase-1-plan.md`, step 2]. `line` and `things` share
   both. The ground is under "Two tables under one key".
5. **The skill's test for a hash placement** asks that no key take "a
   disproportionate share of events or storage" [skill
   `artifact-plan-validation.md`, "Partitioning"]. A layer can. It is one line
   on one task by Sid's decision; "Partitioning efficiency" says what was
   weighed.

## Facts that could not be settled

1. **Rama's bound on a record, a value and an answer on a real cluster.** See
   Q4.
2. **What a weight of w is in bytes on disk and on the wire.** Not measured.
   The next stage's bound is in weight.
3. **What many values of several megabytes cost RocksDB under a PState.** Not
   measured, and no document of Rama says. The measure: the gates' measures
   [read: `test-store/softland/store/gate_measures.clj`] with acts of 1 MB and
   of 16 MB, before and after.
4. **Whether Rama's push watches a run of the line's keys.** The finding is for
   the facts' tables. The last stage's to run.
5. **A batch of a streaming layer that Rama fails with no restart** is not
   reached today [read: `src-store/softland/store/README.md`, "What was not
   reached"]. Should it happen, the line follows the stamps as the answers do,
   and the run-again proof would name the acts.

## A session's choices, each Sid's to turn

1. A thing heavier than 32 MiB, or one that cannot be weighed, is not kept
   again. The number.
2. The rows by who are kept, for an asker outside a layer (Road J is the other
   way).
3. A taken act is kept again as a refused thing is. It follows SPEC's reading
   that a taken act whole is held to the rule of a refused one.
4. The base's line is on the base's task alone (Road I is the other way).
5. A thing for a layer of the other gate gets a line row in the gate where it
   arrived.
6. The digest in a line row is hex text.
7. The words: `line`, `things`, `line-by-who`, `weight`, `:not-kept`,
   `:too-heavy`, `:not-weighed`, `max-kept-weight`; the key
   `:softland.store/who-may-read-acts`, which is SPEC's working word; its
   reason word.
8. This build reaches a store that holds decisions by a second run, and never
   by an update in place (A9).
9. The line and the rows by who are tables of their own (Road K is the other
   way).

## Design difficulty log

- **Where the thing comes from.** I wanted Road B: nothing kept twice. It did
  not survive the reading. Rama gives a topology no position, a stream gate
  cannot count one, and a client's read of a depot is one call for each thing.
  Once B was gone the question was how to keep the thing, not whether.
- **Object or bytes (A against C).** Close. C is the better form for a thing of
  any size, and it types the position. It rests on a serialiser that no
  document gives a topology. A is what the probe showed to give a thing back as
  it arrived, with Rama's own means. What moved me to add the bound to A was
  the partition: one thing that a depot takes and a PState does not would stop
  a layer for good.
- **The measure.** The count of the one text is not a bound for a thing that
  holds a Java list: the text writes it as one word. So the weight goes on
  where the text stops, and a thing that it cannot weigh is not kept. I did not
  find a measure that is both exact and free.
- **The rows by who.** One write for each decision, for a read that may be
  rare. It was settled by SPEC's own form of the read, "from a stamp on", for
  an asker who may not see the layer.
- **Not contested:** that the answers' table stays; that the line is on the
  layer's task; that no topology is added.

## What was run

One probe, on Rama 1.6.0 in process, on a module of its own with one depot and
one stream topology. Not the store's module. Its source and what it printed are
outside the repository, in
`/mnt/data/projects/research/softland/store-line-2026-11-21/probes-second/`:
`keeps_a_thing.clj`, `keeps_a_thing.first-run.out`, `keeps_a_thing.out`. Run
from the repository root, behind the machine's cluster lock:

    flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
      clojure -M:store-module -i <the probe's file>

- **Part A, names.** `com.rpl.rama.ops` holds `current-microbatch-id` and
  `current-task-id` among the names searched, and no other. `com.rpl.rama`
  holds the client's depot calls and `microbatch-depot-info`, whose docstring
  and arguments are quoted under Road B. `taoensso.nippy` loads.
- **Part B, a thing kept again.** A PState position of the class
  `clojure.lang.IPersistentMap`, written by a stream event with the thing as it
  came off the depot. Eleven things: a plain one; one that is itself a sorted
  map; one that is itself a hash map; and things that hold a record, bytes, a
  sorted map with string keys, a Java list, a symbol with a ratio, a float and
  a character, a list with a lazy sequence, a part 70 deep, and an Integer, a
  Long and a Short. Each came back with the same one text and the same digest,
  and its part of the same class; the three numbers kept their classes. Each
  was `=` to what was sent but the one with bytes, since two arrays are equal
  only when they are one. The first run printed a fault of the probe's own for
  the three things with no part; the second run printed them.
- **Part C, a large thing.** The numbers are under Q4. The second run sent the
  thing of 24 MB alone.
