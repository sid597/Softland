# Plan: what the gates keep

**The part.** `what-the-gates-keep` of the read build: the contract's A1 to A6 for both gates, and its
rule R7. The contract is `src/proposal/backend-on-rama-2026-10-02/BUILD-reads.md`. "Entry N" is entry N
of "Decisions of 11 and 12 October 2026" in `src-store/README.md`. "Ruling N" is point N of "Rulings" in
`reads/PROGRESS.md`. "The implied spec" is `reads/IMPLICIT_SPEC.md` (L, F, G, P, VA are its numbers).
"The store's page" is `src-store/softland/store/README.md`. A file name without a folder is under
`src-store/softland/store/`, as the implied spec writes them.
**The stage.** Phase 1 of the Rama skill, the plan. No code of the store is written here.
**Who wrote it.** One fresh session (Opus 5.5), 12 October 2026, for the build session to judge. It wrote
this one file. It ran nothing: no proof, no probe, no cluster.

**Blocked.** Nothing.

**Changed by the check of this plan.** The check (`PLAN_VALIDATION-what-the-gates-keep.md`, 12 October
2026) found eleven faults, each small, and fixed each here. Its paper gives the lines before and after.
It also ran the one thing that this plan had only read: the removal of a row at a top-level key
(`plan-check-probes-what-the-gates-keep/`, "X1" to "X7" below are that run's cases).

**Rulings that cannot hold.** None was found. One is met by a reading that the build session should see
before the check: ruling 10 asks for "a text of the value that no other value has". This plan uses the
SHA-256 of the value's bytes, as hex. Two values with one digest would share a run. The store already
rests on the same hash for an act's answer (`door/digest.clj`, "Why the whole 32 bytes are kept"). It
is open point 3.

**What was read.** The skill: `SKILL.md`, `phases.md`, `phase-1-plan.md`, `artifact-plan.md`,
`stream.md`, `microbatch.md`, `pstate-schema.md`, `pstate-schema-clojure-api.md`; of `paths.md` and
`batch.md` the lines on `NONE>`, on a range of a map, and on a batch block with no aggregator. The
contract, `src-store/CLAUDE.md`, `src-store/README.md`, `reads/PROGRESS.md`, each whole. This part's
entry of `DECOMPOSITION.json`, and the other two entries' needs and hands. Of the implied spec: lines 1
to 519, the matrix 1 to 4, P1 to P7, P10, P14, P18 to P22, the proof and the latency sections. Both
probes' results. Whole, function by function: `act.clj`, `door/declaration.clj`, `door/seal.clj`,
`door/text.clj`, `door/digest.clj`, `gate/decision.clj`, `gate/first_act.clj`, `gate/batch.clj`,
`gate/stamp.clj`, `record/rows.clj`, `record/tables.clj`, `module.clj`, `module/streaming_gate.clj`,
`module/microbatch_gate.clj`, `module/door_reads.clj`. The store's page, whole; the maps of `gate/`,
`record/`, `module/`. Of `door/additions.clj`: the walk of one fact. Of `door.clj` and `door/README.md`:
the lines that a search for a store key and a permission finds. Of
`store-module/PLAN-microbatch-gate.md`: "The design in short", steps 3 and 4, "Who goes to the meeting
task", "Why the decisions are those of deciding one at a time", the cost tables. Of the four proofs: the
lines that a search finds (the search is given under "The proofs"); they were not read whole.
**Not read.** `PLAN-base-for-a-reader.md` and its probe folder, by the task. What this plan takes from
that part is ruling 17 and the decomposition's words for it.

## How the skill's template is adapted

The template is for a new module. This part changes a module that is built. So:

- **Reads** lists what a read must be able to find in the record; the read itself is the next part's.
  **The hand-off** is one section that the plan of the read path takes whole.
- **Writes** lists what a gate keeps or refuses for one act, for each gate.
- **PState Design** gives each change of the record with the other forms that were weighed.
- **Topologies** gives each gate's walk line by line against today's dataflow. No topology is added.
- Sections that the template does not have, and the task asks for: A1, A4 and R7 in full; each scenario
  traced by hand; every function in the order to build; the words and the numbers; the proofs; the
  explanations; what is not decided; open points.
- A claim about Rama is marked **run** (by a proof of the store or by a probe), **read** (in the skill
  or in Rama's docs), or **derived**.

## Reads

What the read path must find, and from what. The forms are under "PState Design" and in the hand-off.

| What a read asks | Found from | Seeks | Where |
|---|---|---|---|
| The fact of a place: its value, who wrote it, the permission that the act named, its stamp, the fact that wrote it | layer, entity, key | 1 | the facts table |
| Who may see a layer | the layer | 1 | the facts table, the layer's own entity |
| What a layer builds on | the layer | 1 | the same |
| Whether a key is marked, and since which stamp its order is level | the layer | 1 | the same |
| What a permission allows | the permission's row, read already for "may she see" | 0 more | the permission's own text |
| What points at a value under a marked key | layer, key, the stamp above, the value | 1, and one step for each place | the facts by value, one run on one task |

Each is one select with a path on one PState on one task, so none needs a query topology
(`phase-1-plan.md`, step 1). The read path composes them; that is its plan's.

## Writes

Each is a change of what a gate does for one act that arrives on a depot. No depot is added.

| | What changes |
|---|---|
| A1 | A sealed fact's row holds the plain bytes that the lock opened, and names no lock |
| A2 | Each fact's row holds the act's `:who` and the act's `:permission`, each as its one text |
| A3 | A fact under `:softland.store/who-may-see` on the layer's own entity; `:members` or `:everyone` |
| A4 | An act under a permission that stands and says `:see` is refused `:permission-does-not-allow-acting` |
| A5 | A fact under `:softland.store/builds-on` on the layer's own entity; a vector of layer ids |
| A6 | A fact under `:softland.store/marked-keys` on the layer's own entity; a set of keys. For each fact under a marked key, one row in the facts by value, and the row of the value before taken out |
| R7 | The three new keys take a second write. A permission's place and a depot take one. A retract under any store key is refused |

## PState Design

No real data is kept yet, and each proof starts a fresh in-process cluster (implied spec G7). So each
schema below is changed in code and needs no migration. Once data is kept, the change of the facts
table (two fields gone, three added, sealed bytes become plain) would need a migration of each row with
its lock in hand, and the facts by value would have to be filled from the facts table.

### 1. The fact's row (A1, A2)

`tables/facts`, one schema for both gates. Key: `rows/place-key`, `"<layer> <entity> <key>"`, unchanged.

| Field | Class | What it holds |
|---|---|---|
| `:value` | byte array | A fact under a plain key: the plain bytes that the lock opened. A fact under a store key: the UTF-8 of the one text of its value, as today. Absent for a retract |
| `:by-act` | UUID | The uuid of the act that wrote the row. Unchanged |
| `:by-index` | Long | The index of the fact in that act. Unchanged |
| `:stamp` | Long | The stamp of that act's decision. Unchanged |
| `:who` | String | The one text (`text/text`) of the act's `:who`. New |
| `:permission` | String | The one text of the act's `:permission`. New |
| `:level-since` | String | Only in the row of the fact under `:softland.store/marked-keys`: the one text of a map from each marked key to a stamp. New; see 4 |

Gone: `:lock-act` and `:lock-index`.

The one text and not the printed text of an id, for `:who`: a UUID has two texts (implied spec F8), and
`#uuid "..."` reads back as a UUID where its 36 characters read back as a symbol. Strings and not byte
arrays: each is short, nothing compares them by bytes, and a proof can show them. Ruling 22: one class
each, no collection.

Obvious, so no other form is costed: the two parts are known where the row is made, they are read with
the row, and a second table for them would cost a second seek for a read that asks for them.

### 2. The three new store keys (A3, A5, A6)

Each is a row of the facts table, at a place on the layer's own entity. Each is in `store-keys`, so
the door leaves its value plain and a gate keeps the one text of the value in `:value`. Each is a
setting in the code's sense (`setting-keys`), so check (f) refuses it on any other entity
`:setting-not-on-the-layer`. Each takes a second write, and a retract of it is refused (R7, below).

| Key | Place | Value | Limits | No fact reads as |
|---|---|---|---|---|
| `:softland.store/who-may-see` | `"<layer> <layer> :softland.store/who-may-see"` | `:members` or `:everyone` | — | `:everyone` for the base, `:members` for any other layer |
| `:softland.store/builds-on` | `"<layer> <layer> :softland.store/builds-on"` | a vector of layer ids, in the writer's order. `[]` says "nothing" | 64 layers; each id within 256 characters | `[]` |
| `:softland.store/marked-keys` | `"<layer> <layer> :softland.store/marked-keys"` | a set of keys. `#{}` says "none". To take one mark off, the set is written again without it | 32 keys; each key within 256 characters; no key of the store's namespace (open point 1) | `#{}` |

"No fact reads as" is one function, `first-act/starts-with`, of the layer's id and the key, beside
`depot-at-making` (ruling 6). No gate calls it. No fact is written at the making.

A gate checks nothing else of a list: a layer that is not made, the layer itself, a layer named twice,
two lists that name each other are all taken (implied spec A5). Ruling 9 gives the read's handling.

### 3. The permission (A4)

`act.clj`, `shapes`, `:permission-id`, with the one slot more of ruling 2:

```
[:or [:tuple :id :id :id]
     [:tuple :id :id :id :permission-id]
     [:tuple :id :id :id :permission-id [:enum :see :see-and-act]]]
```

- `[who layer granted-in]`: a first permission. It has no slot and never gets one.
- `[who layer granted-in parent]`: a given permission, kept without the slot. It reads as `:see-and-act`.
- `[who layer granted-in parent allows]`: a given permission that says what it allows.

One function reads the slot: `act/allows`, permission → `:see` or `:see-and-act`. It is the one place
where "without the slot it reads as see and act" is code, and the read path calls it for B2.

The engine needs no change: `:or` takes the first alternative that fits, a tuple fits only its own
count, and the enum is a leaf form. The nesting limit stays 4 (`max-permission-depth`): the slot is not
a permission. The permission's row holds the one text of the whole vector, slot and all; check (d) stays
a comparison of bytes.

Where a permission's slots are read today, and what each does with the third form:

| Where | Reads | With five slots |
|---|---|---|
| `act.clj`, the rule `:permission-is-in-the-acts-layer` (and its copy in `declaration.clj:361-362`) | slot 1 | the same |
| `declaration.clj:322-328`, `permission-ids` | slots 0 to 2, and slot 3 **only when the count is 4** | **changes**: slot 3 when the count is 4 or 5. Else the ids of the permission above are not held to 256 characters |
| `declaration.clj:392-396`, sits on its holder | slot 0 | the same |
| `decision.clj:136-144`, `standing` | slot 0; the whole text | **changes**: the new check after (d) |
| `decision.clj:204-210`, check (g) | slots 1, 2, and 3 with `(nth given 3 nil)` | the same: the permission above is slot 3 in both given forms |
| `first_act.clj:86`, `making-act?` | the count is 3 | the same. A permission with a slot is no making act's |
| `batch.clj:204-214`, `standing-on-arrival` | the whole text | **changes**: a permission that stands and says `:see` is final |

### 4. The mark, and since which stamp a key's order is level (A6; rulings 19 and 20)

The maker's fact: one fact, a set of keys (ruling 19). The gate must also keep, for each marked key,
the stamp from which its order is level, and it must read that with the marks in one read where an act
is decided. A maker cannot write a gate's stamp, so the gate derives it at the write: for each key of
the new set, the stamp that the row before gave it; for a key that the row before did not hold, the
stamp of this write (`level-from`, below).

Where the derived map is kept:

- **A. A field of the row at the marked-keys place** (`:level-since`). The gate reads one row for the
  marks and has both. No table more, no write more. Cost: the facts schema gets a field that one place
  uses, and that row holds a thing that no maker wrote.
- **B. A table of its own**, layer → the map. One write more for each write of the marks, one
  partition more on each task for each gate, and the same one read for each act.
- **C. No derived stamp: each write of the marks starts every key's order anew**, from that row's own
  stamp. Nothing derived. But marking a second key would hide the first key's order.

**Chosen: A.** The skill says not to declare a second PState for data that shares a key and a
partitioner (`phase-1-plan.md`, step 2). C loses a canvas's order when anybody marks another key. It is
open point 4, because the read path reads this field. The field is absent in every other row, as
`:value` is absent in a retract's: one shape of row with a field that can be absent, not two shapes.

`level-from`, the stamp that a newly marked key gets:

- **Streaming gate:** the stamp of the act that writes the marks. An act is decided with the marks as
  they were before it, so the act's own facts are not in the order, and every fact with a later stamp
  under that key is.
- **Microbatch gate:** the last stamp of the batch, `base + (greatest position + 1) × tasks − 1`
  (`batch/last-stamp-of-batch`), which is not below any stamp of the batch. A mark holds from the batch
  after (ruling 18), so every fact of the mark's own batch has a stamp that is not later, and every
  fact of a later batch has a later one. With the act's own stamp in its place, a fact of the same
  batch with a later turn would have a later stamp and not be in the order. Open point 2.

So one sentence holds for both gates, and the read path may say it to an asker: **the order of a marked
key holds every fact under that key whose stamp is later than the key's stamp in `:level-since`, and no
other.**

### 5. The facts by value: the one more order (A6; rulings 10 and 20)

One table more for each gate: `$$streaming-facts-by-value` and `$$microbatch-facts-by-value`, one
schema, `tables/facts-by-value`: a top-level map from a String to a row.

**The key**, by `rows/by-value-key`:
`"<layer> <key> <since> <value digest> <entity>"`, one space between two parts.

- `<layer>`, `<key>`, `<entity>`: `declaration/id-text`, as in a place's key. None holds a space.
- `<since>`: the digits of the key's stamp in `:level-since`, as the deciding gate held it.
- `<value digest>`: `rows/value-digest`, the SHA-256 of the row's `:value` bytes as 64 hex characters.
  For a value that came through the door those bytes are the UTF-8 of the one text of the value in its
  normal form (`additions.clj:220`).

**A row holds** `:by-act` UUID, `:by-index` Long, `:stamp` Long, `:who` String, `:permission` String:
the fact's row without its value. The value is the one that the asker named. So "what points at T, and
who put it there" is one seek and one step for each place, with no second seek for each place; and a
fact that is written again with the same value changes the row, so a push on the run sends it.

**The run of one value** under one key in one layer: every key that begins with
`"<layer> <key> <since> <value digest> "`. As a range: from that text, to the same text with `!` in
place of its last space, the end not in the range. `!` is the character after a space, and a printed
id begins with `:` or with a hex digit, so the range holds those keys and no other (the same argument
as implied spec L22). `rows/by-value-run` gives the two ends.

**No run holds a row of another value, whatever the value is** (ruling 10). Each part before the entity
holds no space, and the digest has a fixed length and only hex digits. So a key begins with a run's
text only when its layer, key, stamp and digest are that run's. Two values have one digest only by a
collision of SHA-256. A string with spaces, a map, a number, a value of 65,508 bytes, and bytes that
are no value of the form (implied spec F9) are all 64 hex characters in the key. **The two texts of a
UUID** (F8) do not meet: the digest is always taken of the value's one text, `#uuid "..."`, never of
the printed text of an id. A read asks with `rows/by-value-run`, which takes the value and makes that
text itself, so no caller chooses a text.

**A later mark never shows a row whose fact is no longer current** (ruling 20). The key holds the stamp
since which the key is marked. A mark that is taken off and put on again gets a new stamp (4, above),
so it names other keys. The rows of the earlier marking stay where they are, for ever, and no read
names them. Inside one marking the gate keeps the order level (the walks, below), so a run under the
current stamp holds a place exactly when the place's current fact has that value and was written since
that stamp. What it costs: after a second marking the order holds only the facts that are written
after it, and the read says since when; and each marking leaves at most one row for each place.

**Where a row lives.** Streaming layer: the layer's task, with every row of the layer. Microbatch
layer: the task of the hash of the value digest, the 64-character String (`rows/by-value-route` reads
it out of a key). So the run of one value is on one task, found from the value alone, and the runs of
one value under two keys are on one task. It is not the task of the fact's entity; how the row gets
there is in the walk.

Other forms that were weighed:

| Form | A read of "what points at T" | Why not |
|---|---|---|
| The value's whole text in the key | 1 seek, m steps | A key of up to 65,508 bytes; a text can hold spaces, so a run needs an end mark that no value's text holds; bytes that are no text cannot be a String |
| The printed id for a value that is an id, a digest for any other | 1 seek, m steps | A gate would judge what an id is, which ruling 10 and the queue's K5 keep from it |
| `{run text → a subindexed map of entity → row}` | 2 seeks, m steps; a push on the whole inner map is refused by Rama (push probe, N2) | One seek more for each read and each write; the probe ran a run of a flat map |
| In a microbatch layer, the row on the task of the fact's entity | N seeks, one on each task; N pushes | The contract asks for one run; a push covers one task |
| The stamp out of the key, and a read that leaves out rows older than the mark | 1 seek, m steps and the stale rows | The stale rows are sent by each push and counted by each count; a window of the first rows of a run is wrong |
| The order kept level for a key for ever once it was marked | 1 seek, m steps | Every write under a key whose mark was taken off pays for ever; a mark could not be taken off in any sense that saves a write |

## Depots

None is added and none changes. The act in the log is as the door sent it: sealed bytes and a lock id.

## Topologies and PStates

No topology is added. Each gate declares one PState more, with `tables/facts-by-value`. The streaming
gate then owns five PStates and the microbatch gate five.

- **`streaming-gate`: stream**, as built: the door's append waits for the answer to be readable. Each
  write of an accept, the new ones too, is `termval` or `NONE>` at a fixed key: the same write however
  often it runs (`stream.md`, "Stream retries and exactly-once").
- **`microbatch-gate`: microbatch**, as built.

### The streaming gate's event, line by line

Lines are of `module/streaming_gate.clj` at head `dc70e98b`. Steps 0 to 3 (lines 156 to 176) do not
change.

| Step | Today | After |
|---|---|---|
| declare | 149-152: four PStates | one more: `$$streaming-facts-by-value` |
| 4, standing | 183-202 | The reads do not change. `decision/standing` gives one word more, `:permission-does-not-allow-acting`, when the row holds the act's permission and `act/allows` of it is `:see`. No row more is read for it, and no place is read after it |
| 5, places | 208-218 | The reads do not change. `decision/places` also calls `who-may-write-fault` (R7) |
| 6, the seal | 225-238 | The lock reads do not change. `decision/opened` replaces `decision/seals`: it gives the word of the first sealed fact that fails, or the plain bytes of each sealed fact, by index. Each value is opened once |
| 6b, new | — | Only for an act that no check refused, and only when `decision/reads-marked-keys?` says so: read the row at `decision/marked-keys-place`, unless step 5 read it already (an act that writes the marks and names something to replace: its place is in `*place-rows`, and a second read of it would be a seek for nothing). Then read the place of each fact under a marked key whose row step 5 did not read (`decision/rows-before-needed`). The rows join `*place-rows`, and so does the marks row under its own place, which is the row before for an act that writes the marks |
| 7, the stamp and the writes | 241-260 | `decision/writes` takes what is in hand as one map: the lock rows, the plain bytes, the place rows, for each place under a marked key its key's stamp (`decision/since-by-place` of the act and the marks row), and `level-from`, which is the stamp. One case more in the `<<cond`: `:by-value`, a `termval` of the row, or `NONE>` at the key when the row is nil |

Which acts read the marks row: an act that holds a fact under a plain key, or a fact under
`:softland.store/marked-keys`. A request for locks, a give, a making act, a setting and a list do not.
A refused act does not: no check reads the marks, so the read comes after the last check.

The order of the checks, and so of the reasons: early; standing with the new word last in it; places
(e), (f), (g), then `who-may-write-fault`, then what a store key does not take; seals.

Everything is one event on one task with no partitioner, as today. So the fact's row, the row that
goes out of the other order and the row that comes in become visible together (`stream.md`, "When
PState writes commit": the writes between two partitioners are one group).

### The microbatch gate's four steps, line by line

Lines are of `module/microbatch_gate.clj` and `gate/batch.clj` at head `dc70e98b`. **This plan changes
no line of step 2's clock: not 282 to 290 (the last stamp, the clock, the base, the new last stamp),
and not 291 to 294 (the closing).** Those are the other part's (ruling 38).

**Which rows every task holds** (ruling 18): `batch/on-every-task?` is true for five keys: the depot, a
permission, and the three new ones. What follows from that one function, with no other change: such a
fact asks no question (`questions`); its act goes to the meeting task (`who-meets`, rule 1); its row
is sent to every task (`routed`, then `|all` at line 391).

| Step | Lines | Today | After |
|---|---|---|---|
| declare | 231-234 | four PStates | one more: `$$microbatch-facts-by-value` |
| 1 | 261-273 | reads the permission's row, and the depot's row when that is nil; `batch/arrive` | **no read more.** In `questions`, a question about a place also holds `:marks-place`, the key of the layer's marked-keys place, and `:k`, the fact's key |
| 2 | 295-296 | `$$questions`, `reads-of` | after 295, on each task: `batch/marks-places-asked` of `*asked` gives the marked-keys place of each layer that a question about a place names; `rows-at` reads each from this task's own copy, **once for a layer in a batch**; `reads-of` takes those rows. A place whose key is marked is read when an asker does not have it as its own. Then the number of tasks and `batch/batch-end` of `*base` and `*numbers`, both bound before `|all`; it calls `last-stamp-of-batch` again and reads no clock |
| 2 | 320 | `(batch/replies *read *found *now *base)`, with the batch's clock in the place of `*now` under whatever name the other part's build gave it | one argument more, the batch's end. The reply about an answer carries it as `:batch-end`, beside `:clock` and `:base`. The reply about a place whose key is marked carries `:since`, the key's stamp, to every asker, and the row to each asker that does not have the place as its own |
| 2 | `batch.clj:403-415` | a place is shared when two acts name it, unless each has it as its own | a place under a marked key is shared whenever two acts of the batch name it |
| 3 | 340-341 | `own-places-wanted`: own places that name something to replace | also own places whose reply carries `:since` |
| 3 | 342 | `decide-or-send` | `decided` takes each place's `:since` and the batch's end; the parcel of an act that meets carries both |
| 4 | 363, 395 | `write-row` with three PStates | with four; one case more, `:by-value`, as in the streaming gate |
| 4 | `batch.clj:764-782` | `meeting-reads`: permission places, and a depot place that names something to replace | also the place of a fact under `:softland.store/who-may-see` or `:softland.store/builds-on` that names something to replace; and the place of a fact under `:softland.store/marked-keys` whether it names something to replace or not, since its row before gives the stamps of `:level-since`. An act that holds no fact under `:softland.store/marked-keys` does not read the marks here: it has the `:since` of each of its places from step 2 |

**Where the marks are read.** Step 2, on the home task of a place, from that task's own copy, once
for each layer that the batch's questions name there. Every act with a fact under a plain key asks
about that fact's place already, its own places too (`batch.clj:248-253`), and gets a reply for each;
the reply now says whether the place's key is marked, and since which stamp. So the marks cost one
read for a task and a layer in a batch, however many acts the batch holds, where a read by each act in
step 1 would cost one for each act. For an act on one entity the home task is the act's own task, so
that is ruling 18's "one read on the act's own task" when the act is alone in its batch, and less under
load. An act whose facts are on k tasks has the row read on each of those tasks. It is no cache: the
row is read from the PState in the step, and the step reads it again on every attempt.

Nothing writes `$$microbatch-facts` in steps 1 and 2, and the batch before is committed on every task
before this one starts (`microbatch.md`, "Guarantees"). So each act has the marks as every task held
them when the batch began, the same on every attempt.

**How the row before is in hand.** By the questions and replies that exist.

- Its own place (on the act's first entity): read on the deciding task at the act's turn (step 3), after
  the earlier acts of that task wrote their own facts.
- A place on another task: read by its home task in step 2 and sent in the reply.
- An act on the meeting task: the row that an earlier act of the pass wrote, else the row that it
  carried (`decide-next`, unchanged).

**Two acts of one batch on one place under a marked key.** Both go to the meeting task, by the change
of `place-replies`. Reason: a row of the other order lives on a third task, and its writes must arrive
there in the order of the turns. Today two acts on one own place are decided on their own task, one
after the other; their rows of the other order would be gathered on the third task in no order
(`batch/gather`: "The order of a gathered vector means nothing"), and a row could be put after it was
taken out. On the meeting task the pass decides them in turn, each against what the earlier one wrote,
and `rows-to-send` sends one row for each key, the last. So for every key of the other order, in every
batch, either one act writes it, or all its writers are in one pass. A parcel and a row sent by a pass
never have one key; step 4 needs no order between them, as today.

What they leave: with the place at value V0 before the batch, act 1 writing T and act 2 writing U, the
pass gives for the entry of V0 "out", for the entry of T "in" and then "out", for the entry of U "in".
One row for each key is sent: V0 out, T out, U in. The "out" for T takes out a row that was never
written: one message and one delete of nothing. Run (X3, in both kinds of topology): such a removal
changes no row and throws nothing, and **a push that is open on the run of T is sent a removal of that
place all the same**, a place that the run never held. This is the one way by which this plan takes out
a key that holds no row; `by-value-writes` gives no "out" for a row before that is not in the order. The
hand-off says it to the read path.

**How a row of the other order reaches its home.** `decision/writes` gives `[:by-value key row]`, with a
nil row for "out". `batch/routed` gives it the home `rows/by-value-route` of the key, which is the
value digest. An act decided where it arrived sends it as a parcel of the kind `:rows` (step 3), and
the home writes it in step 4. A pass sends it by `|hash` of that home (line 394). Both are inside the
attempt, which Rama keeps or drops whole on all tasks.

**Two acts of one batch that write one marks row, or one setting.** Both meet (rule 1). The pass takes
them in turn. For the marks, `level-from` is the batch's end for both, and the second act's row before
is the first act's, so a key that both hold keeps one stamp. The later turn's row is the one sent to
every task. An act that names in `:replaces` a fact that an earlier act of the pass has replaced is
refused `:replaced-fact-not-current`.

**A mark holds from the batch after** (ruling 18). Every act of a batch is decided with the `:since`
that step 2's replies gave for its places, also on the meeting task after an earlier act of the pass
wrote the marks. The row that the pass wrote is used for one thing, the stamps of `:level-since`.

**Every new row is the same on a second attempt.** What each rests on:

| Row | Made from |
|---|---|
| A fact's `:value` | the act's sealed bytes and its lock's bytes. A lock that an act can use was made by a request of an earlier batch; a lock that a request of this pass makes is given drawn bytes and the use is refused (`batch.clj:806-811`) |
| `:who`, `:permission` | the act |
| `:stamp`, in both tables | the base and the turn. The base rests on the batch's clock, which the other part keeps across attempts |
| `:level-since` | the row before at the act's turn, the act's set, and the batch's end: the base, the batch's greatest position and the number of tasks |
| A key of the other order | the layer, the key, the entity; the marks row as the place's home task held it when the batch began; the digest of the plain bytes, or of the row before |
| Which acts meet | the things of the batch and those marks rows |

Nothing rests on a clock that is read again, a random source, the memory of a process or the order in
which messages arrive. This is the rule that the other part hands over, as this session holds it from
ruling 17 and the decomposition (implied spec G3). If the other part's run sends A7 the other way
(ruling 33), the rows of the base here are planned again.

## Query Topologies

None is added. The three of the door do not change but for one renamed call
(`record/plain-value-of` → `record/value-of`, in `depot-of-layer`). Their answers do not change
(implied spec A1, "Invariants").

## A1 in full

**Where the plain bytes come from.** Today `seal/opens?` is `(some? (open ...))` and the bytes are
dropped (implied spec L4). `decision/opened` calls `seal/open` and keeps what it gives. It takes the
gate's depot, the act and the lock rows, as `seals` does, and makes the same four tests in the same
order for each sealed fact. It gives `{:refused word}` for the first fact that fails, or
`{:plain [...]}`, one entry for each fact in the act's order: the plain bytes of a sealed fact, nil for
any other. `decision/writes` hands entry i to `rows/fact-row` for fact i. One value is opened once for
one decision. In the microbatch gate the bytes are in the fact's row from the moment the act is
decided, and travel to the row's home as the sealed bytes do today: in a parcel, or in what a pass
sends, inside the attempt.

**What a gate now writes.**

| The fact | `:value` |
|---|---|
| Sealed, under a plain key, accepted | the plain bytes that the lock opened. Whatever they are: no gate reads them (ruling 14) |
| Under a store key | the UTF-8 of the one text of its value, as today |
| A retract | absent; the row is there |
| Of a refused act | no row. The plain bytes of a value that was opened before a later fact failed are dropped with the event |

**What reads `:lock-act`, `:lock-index`, `holds-plain-value?` and `plain-value-of` today.**

| Where | Today | After |
|---|---|---|
| `rows.clj:159-166`, `fact-row` | writes the sealed bytes and the lock | writes the plain bytes, who, the permission |
| `rows.clj:182-190`, `holds-plain-value?` | true when no lock is named and the bytes are the value's text | renamed `holds-value?`: true when the bytes are the value's text. Its two callers ask it of the row at a permission's place, where only a fact under that store key can be, and such a fact never arrives sealed (`declaration.clj:380-383`). So no answer changes |
| `rows.clj:192-200`, `plain-value-of` | nil for a sealed row | renamed `value-of`: reads the first form of any row's bytes. Its caller, `depot-of-layer`, asks it of the row at a depot's place. No answer changes. For a tool's row it now gives the tool's value |
| `tables.clj:98-99` | two fields | gone |
| the proofs | "sealed" is told by `(some? (:lock-act row))` | written again (below) |

No other code reads the two fields (a search of `src-store`).

**What the lock's row still holds.** Everything that it holds today. Streaming: one row, with the
bytes, the way, and the fact that it seals. Microbatch: the whole row on the task of the value's
entity, written from the act's own fact (`batch.clj:594-599`), and the home's row with no bytes.

**What no longer holds.**

- "One copy of a lock's bytes", its second half (the store's page, `:410-413`): "the bytes of a lock
  that seals a value are read beside the value". The fact's row gave the lock's key; it no longer does.
  The lock of a current value is now found only from the act in the depot, which holds each fact's
  `:lock-id`. No code reads it either way.
- Rule 19 (`:498-501`): the whole row of a lock "is placed from the fact's row that names it". After A1
  it is placed from the act in the depot.
- The tables' copy of a value no longer needs its lock. Destroying a lock forgets the log's copy and
  not the tables'. That is entry 5 and the other sheet's; the receipt names it.

**What stays as built.** The act in the log; the door's sealing; the rule `:plain-value-is-sealed` and
its word `:not-sealed`; the locks table and both of a lock's rows; the door's requests; the three reads
held to closing.

## A4 in full

**The check.** In `decision/standing`, straight after check (d): when the row at the place of `:who`'s
permission holds the act's permission, and `act/allows` of that permission is `:see`, the word is
`:permission-does-not-allow-acting`. It comes after `:permission-does-not-stand` and
`:who-not-the-permissions-holder` and before every check of a place (ruling 3). It costs no read: the
slot is in the permission that the act names, which the row was just compared with.

| The act | What happens |
|---|---|
| A making act | Its permission is `[holder layer layer]`, three slots, so it allows see and act. A making act whose permission has four or five slots is no making act (`first_act.clj:86`) and is refused `:layer-not-made`; the layer can still be made by a right act |
| A give without the slot, `[bob L L alices]` | As today. Bob holds see and act |
| A give with the slot, `[bob L L alices :see]` or `[... :see-and-act]` | The declaration takes it. Check (g) reads slot 3, the same in both. The row holds the text with its slot |
| Bob, holding `:see`, acts and names his permission as the row has it | (d) passes; refused `:permission-does-not-allow-acting` |
| Bob names it without the slot, or with `:see-and-act` | The bytes are not the row's; refused `:permission-does-not-stand`. **Nobody raises her right by naming it** |
| Bob gives | His act is refused as any act of his. So no permission has a `:see` above it |
| A second give to Bob that says `:see-and-act` | Refused `:store-key-takes-one-write`, as today (ruling 7) |
| Bob sends a value through the door | The door's request for locks is an act under his who and permission; a gate refuses it with the new word; the door gives `:have-its-request-accepted` with that answer and appends nothing for his act (ruling 3). The door does not change |

**The kept forms that carry no slot**: three slots and four. Every permission that a record of today
holds is one of them, and both read as see and act.

In the microbatch gate the new word is final in step 1 (`standing-on-arrival`): a row at a permission's
place never changes, so the act asks for its answer only.

## R7

- **The second write.** As the code stands a second write under a store key is refused only for a
  depot and for a permission (`decision.clj:231-233`; implied spec L9). The three new keys are in
  neither line, so a second write is taken with no change of that function but its docstring.
  `:replaces` stays optional (ruling 21): with none, the row is written over; with one, check (e) holds
  it, and of two writers at once the later is refused.
- **The one write.** A permission's place and a depot after the making: as built, the same two words.
- **A retract** under any store key, the three new ones too: `:store-key-retract-not-built`, as it
  falls out of `decision.clj:230`; a permission's keeps `:ending-a-permission-not-decided`.
- **Who may write the three** (ruling 5): one function, `decision/who-may-write-fault`, called from
  `places` after check (g) for each fact under a key of `declaration/second-write-keys`. It gives nil:
  kept, whoever may act in the layer, and the act has passed its standing. Its docstring says that this
  is not decided, that Sid's word is "the owner", and what the kept answer lets happen. The other
  answer is its body.
- **Rule 16.** A1.

## Each scenario, traced by hand

Layer `L` is streaming. Key `:on` is marked. Stamps are small numbers for reading.

**S1. Rama runs a stream event again.** After a commit: check (a) finds the answer, the event ends, and
nothing is written; the new step 6b is never reached. After a discard: every read is made again from
committed rows and gives the same rows, so the same keys are written; the stamp may differ, and no
reader saw the first. The event has no partitioner, so no part of it is committed on another task. The
new writes are `termval` and `NONE>` at fixed keys. Seen in the check's run: after a worker was started
again, Rama handed a stream topology every record of its partition once more, eight things whose writes
were long committed, and a topology with no check (a) made each write again, a removal too
(`logs/remove.log`, 7710 to 7712). In the store each such event ends at check (a). So check (a) is what
keeps an entry that was put back by a later act from being taken out again; no write of this part may
ever stand before it.

**S2. A second attempt of a batch.** Rama sets the gate's PStates back (batch probe, B3.i) and gives the
same things in the same order (B2.3). By the table "Every new row is the same on a second attempt",
each row of this part is the same in every field, given the kept clock. A new lock's bytes still
differ; they are not this part's, and the door's reads stay held to closing.

**S3. Two acts of one batch.** One place under a marked key, one marks row, one setting: each is in the
walk above. One place under a key that is not marked: as today.

**S4. A mark and a fact under its key.**
- *In one batch:* the home task of the fact's place read the marks in step 2, where the key is not
  marked. No row of the other order, whatever the two turns. The marks row gets the batch's end for that key; the fact's
  stamp is not later; so "every fact with a later stamp" stays true.
- *In one act,* either gate: the act is decided with the marks as they were before it. The fact is not
  in the order. Its stamp is the act's (streaming) or not later than the batch's end (microbatch).
- *In a streaming layer, two acts:* the depot's order. A fact after the mark is in the order.

**S5. A key marked, its mark taken off, facts replaced meanwhile, the key marked again.**

| Stamp | Act | `:level-since` | The facts by value |
|---|---|---|---|
| 100 | marks `#{:on}` | `{:on 100}` | — |
| 110 | `:b1 :on` := `:c1` | | in: `L :on 100 d(:c1) :b1` |
| 120 | marks `#{}` | `{}` | nothing; the row stays |
| 130 | `:b1 :on` := `:c2` | | nothing: the key is not marked. The row of 110 is now stale |
| 140 | marks `#{:on}` | `{:on 140}` | — |
| | a read of what points at `:c1` | says since 140 | the run `L :on 140 d(:c1) ` is empty. The stale row is under 100 and no read names it |
| 150 | `:b1 :on` := `:c3` | | the row before has stamp 130, not later than 140: nothing to take out. In: `L :on 140 d(:c3) :b1` |
| 160 | `:b1 :on` := `:c1` | | the row before has stamp 150: out `L :on 140 d(:c3) :b1`; in `L :on 140 d(:c1) :b1` |

The fact of 130 is in no run after 140; the read says "since 140", which is true.

**S6. A fact under a marked key.**
- *Replaced*, with `:replaces` or without: the row before is in hand; when its stamp is later than the
  key's stamp and its value has another digest, its entry goes out; the new entry comes in.
- *Written again with the same value:* one digest, so no "out"; the entry is written over with the new
  stamp and writer.
- *Retracted:* the entry of the value before goes out; nothing comes in.
- *Each kind of value:* `:canvas-1`; a UUID; `"a string with spaces"`; `42`; `{:a 1}`; 65,508 bytes.
  Each is the bytes that the door sealed, and each key holds 64 hex characters for it. The greatest
  value costs one SHA-256 over 65,508 bytes on the gate's thread, and a second when it is replaced.
  `1` and `1.0` are two values with two texts and two runs.

**S7. A holder of "see".** A4's table. Streaming: 3 reads (the answer, the permission, the last stamp)
and 2 writes (the answer, the last stamp). Microbatch: final in step 1, its answer only.

**S8. The base, where a row under a store key is on every task.** The five keys' rows are written by
the meeting task to every task inside one attempt: N row writes for one fact. A reader finds each on
any task, so on the task where she reads an entity. A run across entities meets each of them once on
each task (implied spec L25); the read path gives it once (ruling 29).

## What one accepted act costs, before and after

By the skill's figures: a seek is 0.3 to 0.5 ms cold; a message between tasks is a hop. Derived, none
measured. The act: one sealed value, on one entity, nothing named in `:replaces`.

| Gate | | Reads | Row writes | Messages |
|---|---|---|---|---|
| Streaming | today | 4: the answer, the permission, the lock, the last stamp | 4: the fact, the lock, the answer, the last stamp | 0 |
| Streaming | after, the key not marked, or no key of the layer marked | 5: and the marks row | 4 | 0 |
| Streaming | after, the key marked | 6: and the row before | 5: and the entry in; 6 when an entry goes out | 0 |
| Microbatch | today | 3: the permission, the answer, the lock | 4: the fact, the lock's two rows, the answer | 6 |
| Microbatch | after, the key not marked | 3, and the marks row once for the act's task and layer in the batch: 4 for an act alone in its batch | 4 | 6 |
| Microbatch | after, the key marked | and the row before, at its turn on its own task: 5 for an act alone in its batch | 5, or 6 | 7, or 8: the entry to its home; the entry out to its home |

With `:replaces` named, the row before is read today, so the marked case reads one row more than
today and not two. An act with no fact under a plain key (a request for locks, a give, a setting, a
list) reads and writes what it does today. A write of the marks reads the marks row and writes one row
(N in the base). A refused act reads nothing more in the streaming gate. In the microbatch gate the
marks row is read in step 2 for the places that were asked about, before any act is decided, as a
place that names something to replace is read today.

**Each read more, and why none can be saved.**

- *The marks row.* A gate must know whether the key is marked where it writes the fact. The row is the
  layer's and no row that every act reads is: the answer is found by the act, the permission by its
  holder, the last stamp by the task. So it cannot ride on a read that is made already. Keeping it in
  memory from one event or one batch to the next would be a cache, which the project's rule sends to
  its own examination, and a second attempt of a batch could then decide with other marks. In the
  streaming gate it is one read for each act with a fact under a plain key: events share nothing. It
  is one row for a layer, read at every act, so it is the row most likely to be in RocksDB's memory;
  by the store's own arithmetic (rule 21) 5 cold reads give 384 to 616 acts a second on one task. In
  the microbatch gate it is one read for a task and a layer in a batch, so at 1,000 acts in a batch on
  one partition it is one read where a read for each act would be 1,000.
- *The row before, for each fact under a marked key that names nothing to replace.* Without it the
  entry of the value before cannot be found: its key holds that value's digest. Storing the digest in
  the fact's row would not save the read. 6 cold reads give 320 to 513 acts a second.

**What is not saved, said plainly.** In the microbatch gate a reply carries the whole row before, with
its value of up to 65,508 bytes, and the decision uses its stamp and the digest of its value. The same
is true today of a fact that names something to replace. Open point 5.

**How long one act can now hold a task of the streaming gate.** The event does not give the task back
(the store's page, `:204-217`), and this part adds to what one act can make it do. The greatest case:
an act of 256 facts, each under a marked key, each a sealed value of 65,508 plain bytes, none naming
something to replace, each over a place that holds such a value written since the mark. Today that act
reads no place. After: the marks row and 256 rows before, 257 seeks more (77 to 129 ms cold) that bring
about 16.8 MB of rows into the event; 512 digests, one of each new value and one of each value before,
over about 33.5 MB; and 512 writes of the other order beside the 256 fact rows. The bound is still the
declaration's: 256 places, each read once at most, in step 5 or in step 6b. Nothing is built against
it, as nothing is today; the measures of C say what it costs. The microbatch gate gives the task back
between two acts, as today.

**What grows for ever.**

| What | By how much |
|---|---|
| The facts table | One row for each place, as today. Each row: about 40 to 80 bytes more for who and the permission (3.4 KB at the limits), 24 fewer for the lock's name, 28 fewer for the seal |
| The facts by value | One row for each place under a marked key whose current fact has a value and was written since the mark: about 100 bytes of key and 100 of row. And for each marking that has ended, at most one row for each place, which stays |
| One run | Without a bound: every place that holds one value under one marked key is one run on one task. A key whose values are few, such as `true`, puts all its writes on a few tasks in a microbatch layer. Ruling 10 keeps "what a marked key may hold" for the queue; the receipt names this |
| The depots, the locks table | As today |

One act writes at most 256 rows into the other order and takes at most 256 out, by `max-facts`: bounded
by an input that the store controls (`phases.md`, cross-phase rules).

## Partitioning efficiency

The placement that the dominant read of this part wants, "what points at one value under one key":
all its rows on one task that is found from the value. `f(layer, key, value)` = the layer's task in a
streaming layer, which is forced, since a streaming layer is whole on its task; and
`hash(value digest) mod N` in a microbatch layer. The marks row that the read needs first is on that
same task in both.

Proportions are assumed, not measured: most layers are streaming. Seeks are totals over all tasks.

| Data category | Proportion | Seeks/op at N = 1 | at N = 16 | at N = 128 | Steps/op |
|---|---|---|---|---|---|
| A run of m places, streaming layer | 0.8 | 2: the marks row, the run | 2 | 2 | m |
| A run of m places, microbatch layer | 0.1 | 2 | 2 | 2 | m |
| A run that is empty | 0.1 | 2 | 2 | 2 | 0 |

Weighted seeks = 2 at each N. Weighted steps = 0.9 × m at each N. Flat.

For the write: one fact under a marked key in a microbatch layer touches at most two tasks more, the
home of the new entry and the home of the old, at any N.

The same for what a gate does for one act, since an act is the other kind of input of this part.
Proportions are assumed, not measured. Seeks and row writes are totals over all tasks; each act is
taken alone in its batch.

| Data category | Proportion | Seeks/op at N = 1 | at N = 16 | at N = 128 | Row writes/op at N = 1, 16, 128 |
|---|---|---|---|---|---|
| One value under a plain key, streaming layer, key not marked | 0.70 | 5 | 5 | 5 | 4, 4, 4 |
| The same, key marked, a value before to take out | 0.10 | 6 | 6 | 6 | 6, 6, 6 |
| One value under a plain key, microbatch layer, key not marked | 0.10 | 4 | 4 | 4 | 4, 4, 4 |
| The same, key marked, a value before to take out | 0.05 | 5 | 5 | 5 | 6, 6, 6 |
| A give in a streaming layer | 0.049 | 4 | 4 | 4 | 3, 3, 3 |
| A fact under one of the five keys whose row every task holds, microbatch layer | 0.001 | 3 | 3 | 3 | 2, 17, 129 |

Weighted seeks = 4.95 at each N. Flat. Weighted row writes = 4.25, 4.26 and 4.38: the one line that
grows is the write to every task, which is small and rare (the skill's test for `|all`), and its
reads do not grow.

**Each write's partitioner.**

- A row of the facts by value in a microbatch layer: `|hash` of the value digest. The digests of many
  values spread evenly. **One value that very many places hold is a hot key**, and the skill's test
  for `|hash` fails on it. It is forced by the contract, not chosen: A6 asks that "what points at this
  thing" be "a run of neighbouring rows", a push covers one task (push probe; implied spec L26), and R3
  forbids a watch that is not a run. The other placement was costed: a value's run spread over k
  tasks, with k kept as state for the value. A read is then 1 + k seeks and k pushes for every value,
  where it is 2 and 1; each write under a marked key reads that state, one read more; and it helps
  only a value with very many places. It is not built. The receipt names the hot run with ruling 10's
  row for the queue.
- A row under one of the three new keys in a microbatch layer: `|all`, as the depot fact and a
  permission. Each is small (at most about 17 KB, one row for a layer) and written rarely, and a read
  or a decision on any task then needs no hop.
- Every row of a streaming layer: no partitioner; the layer's task.

**Why the facts by value are a table of their own**, with the same class of key as the facts table.
The key has another structure: five parts where a place has three. In one map a key of the other
order whose second part is a key that is also some entity's id would sort into that entity's run. And
in a microbatch layer the two are placed by different parts.

## Design Decisions

- **Subindexing.** None. Both tables are top-level maps, where Rama keeps each key as its own entry and
  in order (`pstate-schema.md`, "Storage Model"). A range of such a map in a topology is run by the
  store today (`streaming_gate.clj:233`); a push on such a range was run by the push probe (F2).
- **Colocation.** The marks row is where an act is decided (every task, or the layer's task). The entry
  of the other order is where its run is read.
- **`NONE>` at a key of a top-level map** is a delete with no read (`paths.md`, "No-Read
  Optimizations"). Run by the check of this plan, on Rama 1.6.0 in-process, in a stream topology and in
  a microbatch topology, on a table of the shape of the facts by value
  (`plan-check-probes-what-the-gates-keep/logs/remove.log`). X2: the row is gone for the topology's own
  next read, for a select of the key and of the run, and a push on the run is sent the removal. X3: a
  key that holds no row is taken out with no throw. X4: one event, and one batch, takes one row out and
  puts two, and all three show together. X5: a key that was taken out is put again by a later write. X6
  (stream): two writes at one key in one event leave what the later one wrote. X7 (microbatch): an
  attempt that took a row out and then failed left the row in place for every read from outside, and the
  second attempt read the row before it took it out again. The microbatch writes went to their task by
  `|hash` of a String, and a client found them with that String as the partitioning key. The store's own
  module has not run it; N13 and N14 do.

## State primitive selection

- The fact's row, the three keys' rows, the facts by value: PStates. Each is the truth that a read
  gives. Writes for one act: at most 256 fact rows and 512 writes of the other order.
- No TaskGlobal and no store outside Rama. Nothing is kept in the memory of a process.

## Resource usage analysis

Disk: "What grows for ever". Memory: none of this part's. Minimization: the row of the other order
holds who and the permission a second time, about 40 to 80 bytes for each place under a marked key. It
is kept so that a read of a run that asks for the writer costs no seek for each place; without it a
canvas of 5,000 places costs 5,000 seeks, 1.5 to 2.5 s cold. `:level-since` holds each marked key a
second time, in one row for a layer.

## Every function, in the order to build

Pure first. "→" is what it gives.

**`act.clj`**
1. `shapes`, `:permission-id`: the third alternative. Docstrings of the namespace and of the shape.
2. `allows` (new): a permission in its one form → `:see` or `:see-and-act`. The fifth slot when there
   is one, else `:see-and-act`. Must hold: the thing has the shape.

**`door/declaration.clj`**
3. `max-builds-on` 64, `max-marked-keys` 32 (new).
4. `store-keys`: three entries more, each with `:sits-on :the-layer` and `:setting true`, so that
   `setting-keys` holds them and check (f) needs no change; and one column more, `:written-again`,
   true for the three. Three shapes more in `shapes`: `:who-may-see-word` `[:enum :members :everyone]`, `:layer-list`
   `[:seq-of :id]`, `:key-set` `[:set-of :key]`.
5. `second-write-keys` (new): the set of keys whose `:written-again` is true, read from the table.
6. `rules`, six more, after `:lock-request-sits-on-its-act` and before `:plain-value-is-sealed`, in
   this order, each with its code in `rule-checks`:
   `[:who-may-see-fact-holds-its-word :who-may-see-fact-not-members-or-everyone]`,
   `[:builds-on-fact-holds-layers :builds-on-fact-not-a-list-of-layers]`,
   `[:builds-on-within-the-limit :builds-on-too-many-layers]`,
   `[:marked-keys-fact-holds-keys :marked-keys-fact-not-a-set-of-keys]`,
   `[:marked-keys-within-the-limit :too-many-marked-keys]`,
   `[:marked-keys-are-plain-keys :marked-key-in-the-stores-namespace]`.
   Each keeps a fact with no value, as the rules beside them do; a gate refuses the retract.
7. `permission-ids`: the permission above when the count is 4 or 5. `ids-of`: also the ids of a
   builds-on fact's value and the keys of a marked-keys fact's value, so that `:id-too-long` holds them
   before a row key is built from one.

**`door/seal.clj`, `door/text.clj`, `door/additions.clj`**: docstrings only. `seal/opens?` stays for
the proofs that call it; no gate calls it, and its docstring says so.

**`record/rows.clj`**
8. `fact-row` [act fact plain act-uuid index stamp] → the row of 1.
9. `with-level-since` [row since-map] → the row with `:level-since`, the one text of the map.
10. `holds-value?`, `value-of`: the two renames of A1.
11. `read-value` (new) row or nil → `[:no-fact]`, `[:nothing-here]`, `[:value v]` or `[:not-a-value]`.
    `:value` only when the bytes read as one EDN form `v`, `act/value?` holds for it, and the UTF-8 of
    the one text of its normal form is the row's bytes. It never throws. For the read path (ruling 14).
12. `who-of`, `permission-of` (new): row → the id, the permission, read back from the text; nil when
    there is none or it does not read. Never throws.
13. `marked-keys-of` (new): row or nil → a map from key to stamp; `{}` for no row and for a text that
    does not read as such a map. Never throws.
14. `value-digest` (new): a byte array → 64 hex characters, the SHA-256 of the bytes.
    `value-digest-of` (new): a value → `value-digest` of the UTF-8 of the one text of its normal form.
15. `by-value-key` [layer k since digest entity] → the key. `by-value-run` [layer k since value] →
    `[from to]`, the end not in the range. `by-value-route` key → its digest part.
    `by-value-row` [act act-uuid index stamp] → the row. `by-value-entity` key → the entity, as an id.
16. `id-of-text` (new): a printed id → the id: a keyword for a text that begins with `:`, else a UUID;
    nil for neither. `place-of-key` (new): a place's key → `[layer entity key]`.

**`record/tables.clj`**
17. `facts`: the schema of 1. `facts-by-value` (new): the schema of 5.

**`gate/first_act.clj`**
18. `starts-with` (new) [layer k] → what no fact under k reads as. Docstring: not decided.
    `making-act?`: docstring only.

**`gate/decision.clj`**
19. `standing`: the new word.
20. `who-may-write-fault` (new) [act fact] → nil. `places` calls it.
21. `marked-keys-place` [act]; `reads-marked-keys?` [act] → true for an act with a fact under a key
    that is no store key, or under `:softland.store/marked-keys`. The streaming gate asks it.
22. `since-by-place` [act marks-row] → a map from the key of each place of the act whose key is
    marked to that key's stamp. `rows-before-needed` [since-by-place place-rows] → the keys of those
    places that `place-rows` does not hold. The streaming gate calls both.
23. `opened` [gate-depot act lock-rows] replaces `seals`.
24. `level-since-after` (private) [row-before new-keys level-from] → the map.
    `by-value-writes` (private): for one fact, its plain bytes, the row before and the key's stamp →
    zero, one or two `[:by-value key row]`. "Out" only when the row before has a value, its stamp is
    later than the key's stamp, and its digest is not the new one.
25. `writes` [act-id act refused stamp digest in-hand], `in-hand` a map of `:lock-rows`, `:plain`,
    `:since-by-place`, `:place-rows`, `:level-from`; empty for a refusal. Must hold for an accept:
    `opened` gave `:plain` with these lock rows; `:place-rows` holds the row of each place of
    `:since-by-place`, and of the marked-keys place when the act writes it.

**`gate/batch.clj`**
26. `on-every-task?`: five keys. `standing-on-arrival`: a row that holds the act's permission, where
    `act/allows` of it is `:see`, gives `{:standing :final :word :permission-does-not-allow-acting}`
    and no longer `:stands`; the test for the word stands before the test that gives `:stands`. (If it
    were left as it is, the act would still be refused, in step 3, by `decision/standing`; it would
    then have asked for each of its rows first.)
27. `questions`: a question about a place holds `:marks-place` and `:k`.
28. `marks-places-asked` (new) [asked] → the marked-keys places that the questions about places name,
    each once. `reads-of` [asked marks-rows]: a read of a place holds `:since` when its key is marked,
    and `:read?` as the walk says.
29. `batch-end` (new) [base numbers tasks] → nil with no base, else `last-stamp-of-batch`.
    `replies`, `answer-replies`: `:batch-end`. `place-replies`: `:since`, the row, and the rule for a
    marked place. `with-replies`: `:batch-end`, and `:since-by-place` from the replies about places.
30. `own-places-wanted`: own places that name something to replace or are in `:since-by-place`.
31. `routed`: `:by-value`. `decided`: `opened`, `:since-by-place`, the batch's end.
    `decide-or-send`, `decide-next`: carry and pass them. `meeting-reads`: the keys of the table above.

**`module/streaming_gate.clj`, `module/microbatch_gate.clj`**: the walks above.
**`module/door_reads.clj`**: the renamed call.

## Words a session made, Sid's to change

None is from the rig. None is `owner` or `class`.

| Word | What it is |
|---|---|
| `:softland.store/who-may-see` | The store key of a layer's setting: who may see the layer |
| `:members`, `:everyone` | Its two values. Sid's words |
| `:softland.store/builds-on` | The store key of the layers that a layer builds on, in order |
| `:softland.store/marked-keys` | The store key of the keys that a layer marks |
| `:see`, `:see-and-act` | The fifth slot of a permission: what it allows. Sid's words |
| `:permission-does-not-allow-acting` | The gate's word for an act under a permission that stands and says `:see` |
| `:who-may-see-fact-not-members-or-everyone`, `:builds-on-fact-not-a-list-of-layers`, `:builds-on-too-many-layers`, `:marked-keys-fact-not-a-set-of-keys`, `:too-many-marked-keys`, `:marked-key-in-the-stores-namespace` | The declaration's six words, each named for what is wrong |
| the six rule names of step 6; `:who-may-see-word`, `:layer-list`, `:key-set`; `:written-again` | Names in the declaration |
| `:who`, `:permission`, `:level-since` | Fields of a fact's row |
| "the facts by value"; `$$streaming-facts-by-value`, `$$microbatch-facts-by-value`; `:by-value` | The one more order: its tables, and the word for one of its rows between a decision and a gate |
| value digest | The SHA-256 of a value's bytes, as hex: the value's text in that order |
| `:marks-place`, `:k`; `:since`; `:batch-end`; `:since-by-place` | In a question about a place; in a reply about a place; in a reply about an answer; in an item, and in what `writes` is handed |
| `allows`, `starts-with`, `opened`, `holds-value?`, `value-of`, `read-value`, and the other function names above | Functions |
| `:no-fact`, `:nothing-here`, `:value`, `:not-a-value` | What `read-value` says of a row |

## Every number of the build, kept and not decided

| Number | Value | Why this one |
|---|---|---|
| `max-builds-on` | 64 | Sid, 9 October: "if someone is building like 50 layers". The row is at most about 16.5 KB |
| `max-marked-keys` | 32 | Every act with a fact under a plain key reads this row: at most about 8.3 KB of keys and 9 KB of stamps |
| The width of the value digest | 64 hex characters | SHA-256 whole, as the store's digest of an act |

The plain bytes of a value are at most 65,508: the limit of 65,536 sealed bytes less the seal's 28.

## The hand-off: every form of the record that a read reads

This section stands alone. The plan of the read path takes it whole. Names are of this plan; each is on
the list for Sid. A streaming layer's record is in the PStates named `$$streaming-...`, a microbatch
layer's in `$$microbatch-...`. Which a layer is in: `first-act/depot-at-making` of its id (the base is
microbatch, every other layer streaming).

**Which key a client gives Rama to reach a row.** Nothing may route by a row's whole key: a row is
placed by the task on which its gate writes it. So every select and every push names a partitioning
key.

| The row | Streaming layer | Microbatch layer |
|---|---|---|
| A fact under a plain key, or under `:softland.store/lock-request` | the layer id | the fact's entity, as an id |
| A fact under `:softland.store/depot`, `/permission`, `/who-may-see`, `/builds-on`, `/marked-keys` | the layer id | any: **every task holds the row**. Give the key of the read that it goes with |
| A row of the facts by value | the layer id | the value digest: `rows/value-digest-of` of the value, a String of 64 characters |

In a microbatch layer a run across entities meets the five rows of the second line once on each task.

### The facts: `$$streaming-facts`, `$$microbatch-facts`

- **Key:** `(rows/place-key layer entity key)` → `"<layer> <entity> <key>"`. Back to its parts:
  `(rows/place-of-key text)` → `[layer entity key]`.
- **One entity is one run:** from `"<layer> <entity> "` to `"<layer> <entity>!"`, the end not in it.
- **No row** at a place: no fact was ever accepted there in this layer.

| Field | Class | Read back with |
|---|---|---|
| `:value` | byte array; absent for a retract | `(rows/read-value row)` → `[:no-fact]` for no row; `[:nothing-here]` for a row with no value; `[:value v]` with the value as its maker gave it; `[:not-a-value]` for bytes that are no value of the form. It never throws |
| `:who` | String | `(rows/who-of row)` → the id that the act claimed as its `:who`. A claim until sign-in is built |
| `:permission` | String | `(rows/permission-of row)` → the permission that the act named, which stood. What it allows: `(act/allows permission)` |
| `:stamp` | Long | the stamp of the act's decision |
| `:by-act`, `:by-index` | UUID, Long | the fact that wrote the row; with the layer and the layer's depot it is the fact id `[[layer depot by-act] by-index]` |
| `:level-since` | String | only in the marked-keys row: `(rows/marked-keys-of row)` |

A retract's row holds who retracted and under which permission. A fact under a store key holds its
value as the one text, and `read-value` reads it the same way.

### The five rows under the store's own keys that a read reads

| Place | Value | No row means |
|---|---|---|
| `(rows/place-key layer layer :softland.store/depot)` | `:streaming` or `:microbatch` | the layer is not made |
| `(rows/place-key layer holder :softland.store/permission)` (on the holder, not the layer) | the holder's permission: 3, 4 or 5 slots. `(act/allows p)` → `:see` or `:see-and-act`; any permission that is there allows seeing | the holder has no permission in the layer |
| `(rows/place-key layer layer :softland.store/who-may-see)` | `:members` or `:everyone` | `(first-act/starts-with layer :softland.store/who-may-see)`: `:everyone` for the base, `:members` for any other layer |
| `(rows/place-key layer layer :softland.store/builds-on)` | a vector of at most 64 layer ids, in the writer's order; it may be empty, name the layer itself, name a layer twice or name a layer that is not made | `[]` |
| `(rows/place-key layer layer :softland.store/marked-keys)` | a set of at most 32 keys; `(rows/marked-keys-of row)` → a map from each marked key to a stamp | no key is marked |

Each of the last three can be written again at any time, and never retracted. A holder has one
permission in a layer, and its row is written once.

### The facts by value: `$$streaming-facts-by-value`, `$$microbatch-facts-by-value`

- **What it holds.** For a key K that the layer marks, with the stamp S that `marked-keys-of` gives for
  K: one row for each place `[layer entity K]` whose current fact has a value and a stamp later than S.
  A retract has no row. A fact whose stamp is not later than S has no row: it was written before the
  mark, or in the same act as the mark, or in a microbatch layer in the same batch.
- **How to name the run of one value V under K:** read the marked-keys row; if K is not in
  `marked-keys-of`, the layer does not serve the question for K. Else
  `(rows/by-value-run layer K S V)` → `[from to]`; the run is every key from `from` up to `to`, `to`
  not in it. V is the value as a maker would give it: for a thing whose id is a UUID, the UUID itself.
- **Where the since stamp is:** S, from the marked-keys row. It is in the run's text. When the marks
  row changes and K has another stamp, the run is another run, and the rows of the earlier one are not
  to be read.
- **A row:** `:by-act` UUID, `:by-index` Long, `:stamp` Long, `:who` String, `:permission` String, read
  back as in the facts table. It holds no value: the value is V.
- **The entity of a row:** `(rows/by-value-entity key)`. The place is `[layer entity K]`.
- **No row in the run:** no place holds V under K, among the facts later than S.
- In a microbatch layer the marked-keys row is on every task, so it can be read with the partitioning
  key of the run.

### What a read must not rest on

- In a streaming layer a fact's row and its rows of the other order change in one event. In a
  microbatch layer they are on two tasks, and Rama shows a batch task by task.
- In a microbatch layer the tasks do not show a write of a row that every task holds at one instant.
- A lock: no row of the facts table names one. The locks table is not a read's.
- That a removal which a push on a run is sent names a place that the run held. In a microbatch layer,
  when two acts of one batch write one place under a marked key, the gate takes out the entry of the
  first act's value though it was never written, and Rama sends an open push on that value's run the
  removal of that place all the same (run: X3 of `plan-check-probes-what-the-gates-keep/`). A read
  takes the removal of a place that it does not hold as no change.

### What must hold before a key is made, and how a row of the store's own keys is told

- The functions that make a key or the two ends of a run test nothing of what they are given. Before a
  read calls one with what a caller gave: each id passes `declaration/usable-id?`; a fact's key is a
  readable keyword within the limit (`act/readable-keyword?`, `declaration/id-within-the-limit?`); the
  value V of `by-value-run` and of `value-digest-of` passes `act/value?`. The functions do not throw
  on anything else, and that is the danger: a thing that is no id can print with a space in it, and a
  key made of it can name a row of another place. So the test comes first, as the door's reads make it
  (`rows/depot-read`).
- A row of a run is under one of the store's own keys when `declaration/store-key?` of the key part
  that `rows/place-of-key` gives is true. A gate refuses every other key of the namespace
  `softland.store`, so no accepted fact is under one.

## The proofs

Found by a search of the four files for `:lock-act`, `:lock-index`, `holds-plain-value?`,
`plain-value-of`, `seal/open`, `store-keys`, `setting-keys`, `:softland.store/owner`,
`:softland.store/class`, `fact-row`, `:permission-id`, and for `:value` of a row. The files were not
read whole; the build reads each group that a hit is in. Lines are of head `dc70e98b`.

**Checks that a decision of this build makes untrue, and what each is written again as** (ruling 1).

| Proof | Line | Today | Made untrue by | Written again as |
|---|---|---|---|---|
| act | 161 | the declared shape of a permission, two forms | ruling 2 | three forms |
| pure | 417-420 | "the three store keys, the shape of the value of each, and which is a setting" | entries 1, 2, 6 | the six store keys, the shape of each, which is a setting, which is written again |
| pure | 544 | "each of the three store keys is known" | the same | each of the six, with a right plain value |
| pure | 876-878, 909-910 | the row of a sealed fact: sealed bytes, `:lock-act`, `:lock-index`; the classes | entries 5, 7 | the row holds the plain bytes, who, the permission; no lock; the classes of each field |
| pure | 926-934 | `holds-plain-value?` and `plain-value-of` are false and nil for a sealed row | entry 5 | `holds-value?` and `value-of` of a row with a tool's value |
| pure | 1208 | `writes` gives the fact's row with sealed bytes and its lock | entry 5 | the row with the plain bytes that `opened` gave |
| pure | 2334 | "a fact under each of the three store keys keeps its plain value, and takes no lock" | entries 1, 2, 6 | under each of the six |
| pure | 2720 | shows the store keys and the settings | the same | shows six |
| module | 612-614, 1027-1029 | "the place holds the sealed bytes as sent, the fact that wrote it, its lock" | entry 5 | the place holds the plain bytes of the value, the fact that wrote it, who and the permission, and no lock |
| module | 618-620, 1038-1049, 1352, 1462, 1544-1546 | the lock opens the row's `:value` | entry 5 | the lock's row still names the fact; the lock opens the sealed bytes that the hand sent, and what it opens is the row's `:value` |
| module | 692-694 | a retract's row has "no value and no lock" | entry 5 | no value; who retracted and the permission |
| module | 785-788, 1252-1254 | a tool's fact is "sealed", by `(some? (:lock-act row))` | entry 5 | the row's value reads back as the tool's value, and the store's rows are as they were |
| module | 911-912 | a sealed value of 65,536 bytes is kept as 65,536 | entry 5 | kept as 65,508 plain bytes |
| pure | 332, 401-403 | "the five shapes that the door adds", and their second writing | entries 1, 2, 6 | the eight: with `:who-may-see-word`, `:layer-list`, `:key-set` |
| pure | 384, 404-406 | "the twenty-one rules, the maker's first, each with its reason", and their second writing | entries 1, 2, 6; ruling 40 | the twenty-seven, in the order of step 6 |
| pure | 407-409 | the words with which the check refuses | the same | the same set and the six new words |
| pure | 2739 | "each word of the declaration comes from a thing that earns it" | the same | the same check; a thing that earns each of the six new words is added to its rows, or it fails |
| pure | 913 | the row of a fact under a store key: its value's text, `:by-act`, `:by-index`, `:stamp`, and no other field | entry 7 | the same and who and the permission |
| pure | 916 | the row of a retract: `:by-act`, `:by-index`, `:stamp`, and no other field | entry 7 | the same and who and the permission |
| pure | 1215, 1223-1224, 1232-1233 | the rows that `writes` gives for a request, for a making act, for a retract and a give: no who, no permission | entry 7 | each row with who and the permission |
| pure | 1393-1395 | a question about a place holds `:turn`, `:to`, `:question`, `:wanted?`, `:own?` and no more | ruling 45 | and `:marks-place` and `:k` |
| pure | 1450-1464 | `reads-of` takes the questions alone; a place is read only when an asker that does not have it as its own wants it | rulings 18 and 45 | `reads-of` takes the marks rows too; a read of a place under a marked key holds `:since`, and the place is read when an asker does not have it as its own |
| pure | 1467-1473, 1563-1564 | a reply about an answer holds `:says`, `:clock`, `:base` and no more | ruling 41 | and `:batch-end` |
| pure | 1476-1485 | a reply about a place holds `:key`, `:shared?`, and `:row` when wanted; a place that two acts have as their own is not shared | entry 6; ruling 45 | under a marked key it holds `:since` too, and the row for each asker that does not have the place as its own; such a place is shared whenever two acts name it. Under a key that is not marked: as today |
| pure | 1604-1609 | "the home of each row: a fact under :softland.store/depot or :softland.store/permission on every task" | ruling 18 | under each of the five keys; and the home of a row of the facts by value is its value digest |
| pure | 1619-1630 | the rows that the meeting task reads from its own copy: a permission fact's place, and a depot fact's that names something to replace | ruling 18 | and the place of a setting or a list that names something to replace, and of a marks fact always |
| pure | 1884-1921 | the model that the run of one batch is held against puts a row "on every task" for two keys | ruling 18 | for five; it holds the facts by value too |

These fourteen rows were added by the check of this plan. It found them by a search of the two files
for a whole row written out, for the counts of the declaration, and for each function of
`gate/batch.clj` that this plan changes. It did not read the four proofs whole either. So the build
reads each group that it changes and names in the receipt each further check that it had to write
again, with its decision (ruling 1).

Each call of a function whose arguments change (`fact-row`, `writes`, `seals`, `arrive`, `replies`,
`reads-of`, `decided`) is a fixture and changes with it; where a check writes out what such a function
gives, the check is in the table above. The two keys `:softland.store/owner` and
`:softland.store/class` stay keys that the store does not know, so the checks at pure 587-590 and 648,
and module 749, 791, 1214 and 1251, stay as they are. No check of the door's proof was found untrue.

**New checks.** "Each gate" means once in a streaming layer and once in the base.

| # | What it does | What a pass shows |
|---|---|---|
| N1 | act: parses an act under each of the three forms; a fifth slot that is a third word; a fourth slot that is a word | The shape takes the slot and nothing else there |
| N2 | pure: `act/allows` of each form | Without the slot it reads as see and act |
| N3 | pure: the declaration on each new key: a right value; each wrong value with its word; sealed; a builds-on of 65 layers; 33 marked keys; a marked key of the store's namespace; an id of 257 characters inside each value | The six rules and their order |
| N4 | pure: `standing` for a holder of `:see` who names her permission as the row has it, without the slot, and with `:see-and-act` | The new word, and that naming more does not stand |
| N5 | pure: `opened` gives the plain bytes for a right seal and the word for each wrong one; `writes` puts those bytes in the row | L4 is closed in the pure code |
| N6 | pure: `by-value-key`, `by-value-run`, `value-digest-of` for a keyword, a UUID, a string with spaces, a number, a map and 65,508 bytes; a key of another value, another layer, another key and another stamp is outside the run | Ruling 10's sentence, as far as texts go |
| N7 | pure: `by-value-writes` over each row of S5 and S6 | Ruling 20, in the pure code |
| N8 | pure: the steps of a batch with two acts on one place under a marked key; with a mark and a fact in one batch; with two writes of the marks | They meet; the marks hold from the batch after; one stamp for a key that both hold |
| N9 | module, each gate (VA1): after an accepted value, the row, the depot's record, the lock's row; an unsealed value refused `:not-sealed`; a refused act leaves no row | A1 |
| N10 | module, each gate (VA2): who and the permission in the row; for the system's making of a personal layer the two differ | A2 |
| N11 | module, each gate (VA3, VA5, R7): each new key written, written again, written again naming `:replaces`, a stale `:replaces`, a retract, a wrong value, another entity, sealed; a permission a second time; a depot after the making | A3, A5, R7, each refusal recorded with its word |
| N12 | module, each gate (VA4): a holder of `:see` acts, gives; a give to her of `:see-and-act`; a holder of each kept form acts; a making act whose permission has a slot | A4 |
| N13 | module, each gate (VA6): mark a key; a fact with a keyword id, then with a UUID; replace it; write over it with no `:replaces`; the same value again; retract; take the mark off, replace, mark again. After each act a select of the run of each value, and of the key of each entry that went out. Once, the same thing appended a second time | The run of each value after each act; S5. That a row which went out is gone in the store's own module, which had not run a removal. That a second run of an act behind check (a) leaves the run as it was |
| N14 | module, the base: in one batch, two acts on one place under a marked key; a mark and a fact under it | What the walk says they leave |
| N15 | module, the base: each row that this part adds, on a second attempt of a real batch, by the other part's hand. That hand notes the rows that `decide-or-send` and `rows-to-send` give (`PLAN-base-for-a-reader.md`, "The proof of ruling 17 (d)"); this part makes both give rows of the kind `:by-value`, a row that is nil among them, so its noting functions and its check of two attempts side by side take that kind too, and tell "out" from "no row noted". That check has a cluster of its own (ruling 49), so the earlier batches that this one stands on, the mark and the first value, are sent on that cluster too. The batch of that check gains: a fact under a key that an earlier batch marked, over a value to take out; two acts on one place under that key; a write of the marks, of the setting and of the list; a give with the slot | The same in every field, and the same keys taken out (decomposition, scope item 9). It rests on that hand |
| N16 | door: a holder of `:see` sends a value | `:have-its-request-accepted`, with the gate's new word in the request's answer; nothing appended for her act |

The four proofs pass at the end of this part. Their count lines change; the build gives them before
and after, and names each check of the first table with its decision.

**What no check reaches.** That a streaming layer's fact and its rows of the other order have no moment
between: it rests on one event with no partitioner, read in the code. A failure in the middle of an
event. A real cluster. What a run of very many places costs a gate. A collision of the digest.

## Every explanation that the part makes untrue

Lines are of head `c3e65c06`, as the decomposition gives them; the other part may have moved some.
Each is replaced, not amended.

| File and lines | Says today | Will say |
|---|---|---|
| `decision.clj:59-60`, `:296-297`, `:326-328` | `seals` keeps nothing of the plain bytes | `opened` gives the plain bytes of each sealed value to `writes`, which puts them in the fact's row; they are nowhere else |
| `seal.clj:10-12`, `:170-172` | a gate "keeps nothing of the plain value" | a gate keeps the plain bytes in the fact's row; `opens?` is a test that no gate calls |
| `rows.clj:14-22`, `:29-35`, `:152-157` | a value is "sealed when a lock is named"; `:lock-act`, `:lock-index` | the row of 1; a fourth kind of row, in the facts by value |
| `rows.clj:37-43`; `tables.clj:46-54`; `module.clj:48-55`; `microbatch_gate.clj:26-31` | two keys' rows are on every task | five; and where a row of the facts by value lives |
| `tables.clj:3`; `streaming_gate.clj:6`, `:12-15`; `microbatch_gate.clj:6`, `:15-18`; `module/README.md`, its table | "four PStates" | five, by name |
| `streaming_gate.clj:31-66` | the walk; "4 reads and 4 row writes" | the walk with step 6b; the counts of this plan |
| `microbatch_gate.clj:39-44`, "The one order" | the acts are decided one at a time in the order of their turns | and one thing is not: a mark holds from the batch after (ruling 18) |
| `microbatch_gate.clj:46-72` | the four steps | with the marks row, the row before, the facts by value |
| `declaration.clj:28`, `:102-124`; `additions.clj:8-9`, `:167-181`; `door/README.md:75`, `:83`, `:294` | "the three store keys" | the store keys, with no count; the table says which take a second write |
| `batch.clj:152-158`, `:236-239`, `:517-518`, `:566-567`, `:766-768` | two keys | the five; the marked place |
| `act.clj:171-174`, and the namespace docstring | two forms of a permission | three, and what the slot says |
| `first_act.clj:56-78` | the form of a making act | the same, and that a permission with a slot is none |
| `door_reads.clj:163`, `:171` | — | the renamed call only |
| the store's page `:55-60`, `:79-81` | "How the parts relate" | the seal's bytes are kept; the rows on every task |
| `:170-172` | "keeps nothing of the plain bytes" | keeps them in the fact's row, and still does not check that they are a value of the form |
| `:239` (F5 A), `:251`, `:253` (K1 A) | "Where it shows" | by ruling 35: only that side changes: the five rows on every task; checks (d), (g) and the new word; the six keys. A table is added for the entries of 11 and 12 October: 1, 2, 5, 6, 7, each with where it shows |
| `:287-289`, `:415-421`, `:504-507` (rule 21) | the counts of one act | the table of this plan |
| `:291-294` | one permission for a holder; "Sid should know" | the same, and that it is not decided (ruling 4) |
| `:355-358`, `:360-368` | two keys on every task; when an act goes to the meeting task | five; and two acts on one place under a marked key |
| `:370-376` | the order of one batch | and that a mark and a fact of one batch are not decided in the order of their turns |
| `:402-413` | "One copy of a lock's bytes", "Sid should know two things" | the write as built; the fact's row no longer gives the lock's key; the lock is found from the act in the depot |
| `:433-434` (rule 2) | "reads no row" | reads no row for a check; a fact under a marked key reads its place for the other order, and still cannot be stale |
| `:471-473` (rule 13), `:663-664` | one write for a store key | a second write for the three; one for a permission and a depot; each retract refused |
| `:480-482` (rule 16) | "drops the plain bytes" | keeps them in the fact's row |
| `:498-501` (rule 19) | "placed from the fact's row that names it" | placed from the act in the depot |
| `:517-538`, `:540-561`, `:577-596`, `:608-671`, `:673-699` | the words | each word of this plan is added; `:lock-act` and `:lock-index` go |
| "Engineering choices" | — | one paragraph for each of: the facts by value and its key; `:level-since`; who may write the three; what a layer starts with. Each kept answer says that it is not decided (R8) |
| `test-store/softland/store/README.md` | the groups of the act's, the pure and the module's proof | each group whose check is written again or added |
| the store's page `:204-217` | "How long one act can hold a task of the streaming gate": its reads and the opening of its values | and the marks row, the row before of each fact under a marked key, a digest of each such value and of each value before, and the writes of the other order; the greatest case of this plan |
| `gate/README.md`, its table | `first_act.clj`: "The depot that a layer starts with, and the form of a making act"; `batch.clj`: what an act asks and what the tasks reply | `first_act.clj`: and what a layer starts with when no fact says who may see it or what it builds on; `batch.clj`: and since which stamp a place's key is marked, and the home of a row of the facts by value |
| `record/README.md:5-6`, `:25-27` | "its rows, and the schemas"; "a plain value is kept as the bytes of its one text" | every value is kept plain: the bytes that its lock opened, or the one text of a value under a store key; the facts by value and the text of their key; the digest that the key holds |
| `streaming_gate.clj:141-146`, `microbatch_gate.clj:199-203`, `:223-228` | `add-to`: "its four PStates"; `write-row`: three PStates, "a whole row at a fixed key" | five; `write-row`: four PStates, and a row that is nil takes its key out |
| `decision.clj:16-38`, `:212-224` | the list of the checks names `seals`; `store-key-fault`: "a store key takes one write for a place", "the three store keys" | `opened`, the new word of the standing, `who-may-write-fault`; a permission's place and a depot take one write, the three new keys a second |
| `batch.clj:42-61`, and the docstring of each function that "Every function, in the order to build" changes | the data between the functions and the gate, by step; what each function takes and gives | with `:marks-place` and `:k` in a question, `:since` and `:batch-end` in a reply, `:since-by-place` in an item, the kind `:by-value` among the routed rows; each docstring as its function then is (`src-store/CLAUDE.md`: a docstring changes with its function) |

**Head comments** (ruling 39): `module.clj`, `module/streaming_gate.clj`, `module/microbatch_gate.clj`
and `module/door_reads.clj` each gain the line
`;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md`.

Not this part's: the sentences on the door as the module's one client (R1), which the read path's plan
replaces.

## Not decided: how each is built, and where the other answer would be made

| Thing | Built as | The one place for the other answer |
|---|---|---|
| One permission for a holder in a layer (ruling 4) | As built: a permission sits on its holder | The rule `:permission-fact-sits-on-its-holder` and `decision/permission-place`, as the store's page says |
| Who may write the setting, the list, the marks (ruling 5) | Whoever may act in the layer | The body of `decision/who-may-write-fault` |
| What a layer starts with (ruling 6) | Read from no fact | `first-act/starts-with` |
| "See" stays "see" (ruling 7) | A permission's place takes one write | The line of `store-key-fault` that gives `:store-key-takes-one-write` for a permission |
| What a marked key may hold (ruling 10) | Every value, by its digest; nothing refused | `decision/by-value-writes` for "kept in no run"; a rule of the declaration cannot refuse it, since a mark is a fact of the record: it would be a check in `places` |

## Open points for the build session

Only what no ruling settles.

1. **A mark of a key in the store's namespace** (implied spec A6, "Edge cases": "Unsaid").
   (a) Refused by the declaration, `:marked-key-in-the-stores-namespace`. (b) Taken, and the store's
   own facts are kept in the other order. (c) Taken, and never kept. **Recommended: (a)**, and it is
   what the plan builds. With (c) nothing tells the writer. (b) puts each permission in a run.
2. **The stamp of a newly marked key in a microbatch layer.** (a) The batch's last stamp, carried to
   the deciding task in the reply about an answer. It adds two lines to step 2 after line 295 and one
   argument at line 320, beside the other part's lines and not in them. (b) The act's own stamp, with
   none of those; then a fact of the mark's own batch with a later turn has a later stamp and is not in
   the order, and the read's "since" is wrong for it. With (b) the test "its stamp is later than the
   key's stamp" in `by-value-writes` also takes out a row that was never written, a delete of nothing.
   **Recommended: (a)**; the plan builds it. The checks of the two plans should read lines 279 to 324
   together: this plan adds the read of the marks and the batch's end after line 295 and changes line
   320, and the other plan changes the clock above them.
3. **Ruling 10's "text that no other value has" as a digest.** (a) SHA-256 of the value's bytes.
   (b) The value's whole text in the key. **Recommended: (a)**; see "PState Design", 5.
4. **`:level-since` as a field of the marked-keys row.** (a) The field. (b) A table of its own.
   **Recommended: (a)**; see "PState Design", 4. The read path reads it either way through
   `rows/marked-keys-of`, so (b) changes that function's argument and one line of the hand-off.
5. **A reply that carries the row before with its whole value.** (a) As today's reply for a replaced
   fact. (b) The home task sends the row without `:value` and with the value's digest. **Recommended:
   (a) for this build**, with the measure of C: a value under a marked key is most often an id. (b) is
   a change of `place-replies` and of one line of `by-value-writes`.

## Design difficulty log

- **Since which stamp, and the mark that comes back.** Contested. The first thought was a set of keys
  and nothing derived. It fails on the mark that is taken off and put on again: the stale rows cannot
  be told from the true ones without a read of each place. A stamp in the key settles it, and then the
  gate must hold a stamp for each key, which a maker cannot write. Deriving it at the write and keeping
  it in the same row was the cheapest form that keeps one read for an act.
- **The order of writes on a third task.** The hardest part. A row of the other order is on a task that
  is neither the act's nor the fact's, and the microbatch gate has no order between what two steps or
  two tasks send there. Three ways were weighed: a fifth step that writes what the passes send; a rule
  that sends the earlier act to the meeting task as well; and "two acts on one place under a marked key
  meet". The last is one condition in `place-replies` and leaves every key with one writer or one
  pass. It sends to one task only acts that write one place twice in one batch.
- **Riding the marks on a read that is made already.** Sought and not found. The depot's row is not
  read for an act whose permission stands, so a range over the layer's own rows would add a seek and
  not save one.
- **Where the microbatch gate reads the marks.** The first design read them in step 1, once for each
  act, on the act's own task, as ruling 18 counts it. Costed against a batch of 1,000 acts it lost: the
  home task of a place is asked about every place anyway and can read the marks once for a layer. That
  is what the plan builds. It reads the row on more tasks for an act that is spread over several and is
  alone in its batch.
- **The row of the other order with who and the permission in it.** Close. It doubles those bytes for a
  place under a marked key. It was kept because entry 7 puts the writer within reach of every read, and
  a run that had to fetch each place's row for it would cost a seek for each place.
- **The digest.** Not close once the greatest value was costed: a key of 65 KB, and a text with spaces.
