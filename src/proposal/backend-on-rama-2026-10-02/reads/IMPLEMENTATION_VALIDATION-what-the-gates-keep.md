# Implementation validation: what the gates keep

Part: `what-the-gates-keep` of the read build: the contract's A1 to A6 for both gates of the store's
Rama module, and its rule R7. Stage: the build, step "validate the implementation" (the Rama skill's
`phase-4-impl-validate.md`, by its template `artifact-impl-validation.md`). Written by the build
stage's own session (Opus 5.5), 12 October 2026, for the build session, which judges it. It read no
code of the rig, the server or Inland, and no `env.clj` or `.env` file. It committed nothing and
staged nothing.

Checked: the code under `src-store/softland/store/` as this session left it, against
`PLAN-what-the-gates-keep.md` as its check left it, against the contract `../BUILD-reads.md`, and
against the rulings 1 to 55 of `PROGRESS.md`. "The plan" is that plan; "item N" is item N of its
"Every function, in the order to build". Line numbers are of the working tree at the end of this
stage.

Three kinds of statement are kept apart: *traced* (the code was read line by line for the case),
*run* (a proof printed it; the four cut outputs are in `runs/after-what-the-gates-keep-*.txt`), and
*not verified*.

**Verdict of this step: pass.** One place where the plan's lines pull two ways was built to the
plan's own cost table (section 4, point 1). It is no failure of a check: the plan's performance is
the reason, which the template allows.

## 1. What was built, file by file

| File | What changed |
|---|---|
| `act.clj` | The shape `:permission-id` has a third alternative, `[who layer granted-in parent allows]` with `allows` one of `:see`, `:see-and-act`. `allows` (new) reads the slot. The namespace docstring says the three forms |
| `door/declaration.clj` | `max-builds-on` 64, `max-marked-keys` 32. `store-keys`: three entries more, each a setting on the layer, and the column `:written-again`. `second-write-keys` (new). Three shapes more. Six rules more, after `:lock-request-sits-on-its-act` and before `:plain-value-is-sealed`, each with its code. `permission-ids` takes the permission above for a count of 4 or 5. `ids-of` holds the layers of a builds-on fact and the keys of a marked-keys fact to the limit. `count-within?` (new, private) |
| `door/seal.clj`, `door/text.clj`, `door/additions.clj` | Docstrings only. `seal/opens?` stays, and says that no gate calls it |
| `record/rows.clj` | `fact-row` takes the act and the plain bytes, and gives the row with `:who` and `:permission` and no lock. `with-level-since`, `read-value`, `who-of`, `permission-of`, `marked-keys-of`, `value-digest`, `value-digest-of`, `by-value-key`, `by-value-run`, `by-value-route`, `by-value-row`, `by-value-entity`, `id-of-text`, `place-of-key` (new). `holds-plain-value?` is `holds-value?`; `plain-value-of` is `value-of`. Two private helpers, `first-form` and `parts-of-key` |
| `record/tables.clj` | `facts`: `:lock-act` and `:lock-index` gone; `:who`, `:permission`, `:level-since`, each a String. `facts-by-value` (new) |
| `gate/first_act.clj` | `starts-with` (new). `making-act?`: docstring |
| `gate/decision.clj` | `standing` gives `:permission-does-not-allow-acting`. `who-may-write-fault`, `marked-keys-place`, `reads-marked-keys?`, `since-by-place`, `rows-before-needed` (new). `opened` replaces `seals`. `level-since-after`, `by-value-writes`, `opened-fact` (new, private). `writes` takes one map of what the gate has in hand |
| `gate/batch.clj` | `on-every-task?`: five keys. `standing-on-arrival`: a permission that says `:see` is final. `questions`: `:marks-place`, `:k`. `marks-places-asked`, `batch-end` (new). `reads-of`, `replies`, `answer-replies`, `place-replies`, `with-replies`, `own-places-wanted`, `routed`, `decided`, `decide-or-send`, `meeting-reads` changed as the plan's items 26 to 31 say |
| `module/streaming_gate.clj` | One PState more. Step 6 calls `opened`. Step 6b (new). Step 7 hands `writes` the map, and writes or takes out a row of the facts by value |
| `module/microbatch_gate.clj` | One PState more. Step 2, after the read of `$$questions`: the marked-keys places, `reads-of` with their rows, the batch's end; `replies` with one argument more. `write-row` takes four PStates |
| `module/door_reads.clj`, `module.clj` | The renamed call; the head comment; `module.clj`'s docstring on where a row lives |

The four files whose head comment names a plan each gained this part's plan (ruling 39).

## 2. The template's checks

- **Redundant conditionals.** None. In the streaming gate's `<<cond` (`streaming_gate.clj:320-341`)
  and in `write-row` (`microbatch_gate.clj:239-261`) each branch writes another PState, or takes a
  key out where its neighbour puts a row: `NONE>` and `termval` are two operations, not one
  operation with a variable. *Traced.*
- **Consecutive keypath.** None added.
- **Select, compute, transform.** Step 6b reads the row of the marked keys and rows before, and step
  7 writes whole rows that pure functions made from them. None is an accumulation that an aggregator
  would do: each row is replaced whole, or its key is taken out. The same holds of the marked-keys
  read in step 2 of the microbatch gate. *Traced.*
- **Unnecessary `nil->val`.** None.
- **`:allow-yield?`.** No range read was added. Step 6b's loop makes up to 256 point reads with no
  yield (`streaming_gate.clj:289-297`): the streaming gate's event gives the task back nowhere, by
  design, since a yield would let another act of the layer in between this act's reads and its
  writes. The plan states the greatest case, and the store's page now says it ("How long one act can
  hold a task of the streaming gate"). In the microbatch gate the marked-keys places are read by
  `rows-at` before the loop of step 2 that yields; they are one for each layer that a task's
  questions name, and only the base is a microbatch layer today. *Traced.*
- **Collections with no limit.** No row holds a collection. `:level-since` is a String, the text of
  a map with 32 entries at most (`max-marked-keys`, held by the rule
  `:marked-keys-within-the-limit` before a row is made). A question holds two fields more, a key
  and the key of the marked-keys place: each within the limit of an id. *Traced.*
- **Idempotency of the stream topology.** Each new write is `termval` at a fixed key or `NONE>` at
  a fixed key (`streaming_gate.clj:324-328`), behind check (a) (`:204-205`), in the same group as
  the answer's row. A second run after a commit ends at check (a) and writes nothing: *run*, the
  module's proof appends one accepted act a second time after a later act had put back the entry
  that it took out, and the run stays as the later act left it ("the same act appended a second
  time"). A second run after a discard reads the same committed rows, so it makes the same keys; the
  key of a row of the facts by value holds no stamp of the act, only the key's stamp from the
  committed row of the marks. No id is made in the topology and nothing is appended to a depot.
  *Traced.* A discard itself was not made to happen: *not verified*.
- **Partial failure in the stream topology.** One event, one task, no partitioner: step 6b and the
  new case of step 7 are `local-select>` and `local-transform>` on the gate's own PStates. *Traced.*
- **One append for each client operation.** No client operation was added. The door did not change:
  *run*, the door's proof passes with no check of it written again.
- **Caches.** None. The row of a layer's marked keys is read from the PState in each event that
  needs it and in each batch that needs it, on every attempt. I did not reach for a copy in the
  memory of a process at any point of this build.
- **Built-ins written again.** None.

## 3. Against the plan, item by item

Each item of "Every function, in the order to build", with where it is and how it was checked.

| Item | Built at | Check |
|---|---|---|
| 1 `:permission-id`, third alternative | `act.clj`, `shapes` | *run*: act proof, "the declared shape", and the cases of the slot |
| 2 `allows` | `act.clj` | *run*: pure, "what a permission allows" |
| 3 the two numbers | `declaration.clj` | *run*: pure, "the seven limits" |
| 4 `store-keys`, three shapes | `declaration.clj` | *run*: pure, "the six store keys", "the eight shapes" |
| 5 `second-write-keys` | `declaration.clj` | *run*: the same case |
| 6 six rules, in the plan's order, before `:plain-value-is-sealed` | `declaration.clj`, `rules`, `rule-checks` | *run*: pure, "the twenty-seven rules"; six cases of two faults for the order |
| 7 `permission-ids`, `ids-of` | `declaration.clj` | *run*: pure, three rows of the long id |
| 8 `fact-row` | `rows.clj:202` | *run*: pure, three rows written out; module, each gate |
| 9 `with-level-since` | `rows.clj:226` | *run*: pure, "the row at a layer's marked-keys place" |
| 10 `holds-value?`, `value-of` | `rows.clj:250`, `:268` | *run*: pure |
| 11 `read-value` | `rows.clj:278` | *run*: pure, nine things that are no value; module, a value of 65,508 zero bytes |
| 12 `who-of`, `permission-of` | `rows.clj:310`, `:321` | *run*: pure; module |
| 13 `marked-keys-of` | `rows.clj:333` | *run*: pure, six texts that are no such map |
| 14 `value-digest`, `value-digest-of` | `rows.clj:350`, `:358` | *run*: pure, against a SHA-256 that the proof takes itself |
| 15 the five `by-value-` functions | `rows.clj:368` to `:449` | *run*: pure; module, the selects of each run |
| 16 `id-of-text`, `place-of-key` | `rows.clj:421`, `:436` | *run*: pure |
| 17 the two schemas | `tables.clj` | *run*: the module's proof writes each row on the cluster; a row of another class would throw there |
| 18 `starts-with` | `first_act.clj:103` | *run*: pure; module |
| 19 `standing` | `decision.clj:137` | *run*: pure, eight rows; module, each gate; door |
| 20 `who-may-write-fault`, called from `places` after check (g) | `decision.clj:248`, `:297` | *run*: pure; module, a second holder writes the setting |
| 21 `marked-keys-place`, `reads-marked-keys?` | `decision.clj:332`, `:340` | *run*: pure |
| 22 `since-by-place`, `rows-before-needed` | `decision.clj:353`, `:371` | *run*: pure |
| 23 `opened` | `decision.clj:433` | *run*: pure, eleven words and three right seals |
| 24 `level-since-after`, `by-value-writes` | `decision.clj:483`, `:496` | *run*: pure, through `writes`; module |
| 25 `writes` with the map in hand | `decision.clj:568` | *run*: pure, thirteen cases |
| 26 `on-every-task?`, `standing-on-arrival` | `batch.clj:205`, `:249` | *run*: pure |
| 27 `questions` | `batch.clj:284` | *run*: pure |
| 28 `marks-places-asked`, `reads-of` | `batch.clj:437`, `:455` | *run*: pure |
| 29 `batch-end`, the replies, `with-replies` | `batch.clj:489` to `:625` | *run*: pure |
| 30 `own-places-wanted` | `batch.clj:784` | *run*: pure |
| 31 `routed`, `decided`, `decide-or-send`, `decide-next`, `meeting-reads` | `batch.clj:696` to `:1015` | *run*: pure, case by case and in a batch run in memory in 200 orders |
| the streaming gate's walk | `streaming_gate.clj:239-341` | *run*: module |
| the microbatch gate's walk | `microbatch_gate.clj:337-344`, `:368`, `:411`, `:444` | *run*: module |
| the renamed call | `door_reads.clj` | *run*: door, `depot-of-layer` gives each layer's depot as before |

Names, words and numbers are the plan's, each one. No name was taken from the rig's code: this
session read no file of it.

## 4. Where the plan could not be met as written, and what was done

1. **A request for locks and the read of the marked keys, in the microbatch gate.**
   *The plan's lines.* Its table of step 2: "`batch/marks-places-asked` of `*asked` gives the
   marked-keys place of each layer that a question about a place names". And its cost section: "An
   act with no fact under a plain key (a request for locks, a give, a setting, a list) reads and
   writes what it does today."
   *What I found.* A request for locks asks about a place: its own fact's, under
   `:softland.store/lock-request` (`batch.clj`, `questions`: "for each fact under a plain key or
   under :softland.store/lock-request"). Built to the first line, a batch that holds one request
   reads the layer's marked keys on the task of that request's uuid: one read more than today,
   against the second line. No store key can be marked (the declaration refuses it, ruling 40), so
   that read can never change a decision.
   *What I built.* A question about a place holds `:marks-place` and `:k` whatever the key, as item
   27 says, so the form of a question is one. `marks-places-asked` leaves out the questions whose
   key is a store key (`batch.clj:437-453`). So a request reads what it read before.
   *Why.* The plan's own cost table, which its check counted and confirmed cell by cell, and the
   skill's rule that a read is never traded for simpler code. *Run*: pure, "the marked-keys places
   that a task reads before it replies ... none for the place of a request for locks".

Nothing else. In particular the three lines of step 2 that are the other part's
(`microbatch_gate.clj:325-327` today: `*numbers`, `*now`, `*base`) are as they were, and so are the
names that they bind. This part's lines of step 2 stand after `(|all)` and after the read of
`$$questions` (`:337-344`), and the call of `batch/replies` (`:368`) names the batch's clock by the
var that those lines bind today, `*now`.

## 5. The streaming gate's event, traced

`streaming_gate.clj`. Steps 0 to 3 are untouched.

- **Step 4, standing** (`:213-237`). The reads are those of before. `decision/standing`
  (`decision.clj:137-190`): its first test is `holds-value?` of the row against the act's
  permission, a comparison of bytes of the whole text, slot and all. Only when that holds is
  `act/allows` asked; `:see` gives `:permission-does-not-allow-acting`. *Traced* with three acts of
  one holder whose row says `:see`: named as the row has it, the new word; named without the slot,
  the bytes differ, so the second branch gives `:permission-does-not-stand`; named with
  `:see-and-act`, the same. *Run*: pure, module (each gate), door.
- **Step 5, places** (`:239-255`). The then-branch now binds `*place-rows` to `{}`, so that the var
  is bound after the `<<if` on both branches; step 6b reads it. `decision/places`
  (`decision.clj:297-327`): (e), (f), (g), then `who-may-write-fault` for a fact under a key of
  `second-write-keys`, then `store-key-fault`. A making act is held to none of the last three, as
  before; it holds no such fact, since its form is exactly two facts.
- **Step 6, the seal** (`:257-276`). `decision/opened` gives `{:refused word}` or `{:plain [...]}`;
  the output is destructured into `*refused` and `*plain`. The then-branch binds `*plain` to nil.
  One call of `seal/open` for each sealed fact, and none after the first fact that fails.
- **Step 6b** (`:278-301`). The test is `(and> (nil? *refused) (decision/reads-marked-keys? *act))`;
  `and>` stops at the first false, so a thing that the declaration refused, whose `*act` is nil,
  never reaches the second call (which takes nil all the same). The marks row is taken from
  `*place-rows` when step 5 read it, else read once. `since-by-place` gives the places of the act
  under a marked key; `rows-before-needed` leaves out those that step 5 read; the loop reads each
  of the rest once. `*rows-in-hand` is the place rows, the rows before, and the marks row under its
  own place.
- **Step 7** (`:303-341`). For a refusal the map in hand is `{}`. For an accept it holds the lock
  rows, the plain bytes, the stamps of the marked places, the rows in hand, and the act's own stamp
  as the stamp of a newly marked key. One case more: a row of the facts by value that is nil takes
  its key out with `[(keypath *key) NONE>]`; any other is put with `termval`.

No partitioner and no yield were added, so the fact's row, the entry that goes out and the entry
that comes in are one group. *Traced*; that they have no moment between is *not verified* by a run.

**The counts, counted from the dataflow.** One accepted act, one sealed value, one entity, nothing
named to replace.

| | Reads | Row writes | The plan's table |
|---|---|---|---|
| The key not marked, or no key of the layer marked | 5: the answer (`:204`), the permission (`:219`), one range of locks (`:271`), the marks row (`:286`), the last stamp (`:305`) | 4: the fact, the lock, the answer, the last stamp | 5 and 4: the same |
| The key marked | 6: and the row before (`:296`) | 5: and the entry in; 6 when an entry goes out | 6, and 5 or 6: the same |

With `:replaces` named the place is read once, in step 5. A request for locks, a give, a setting, a
list: `reads-marked-keys?` is false, so they read what they read before. A refused act: step 6b
stands after the last check. A holder of `:see`: 3 reads (the answer, the permission, the last
stamp) and 2 writes. An act that writes the marks: the marks row once.

## 6. The microbatch gate's batch, traced

`microbatch_gate.clj` with `gate/batch.clj`.

- **Step 1.** No read more. `questions` adds `:marks-place` and `:k` to a question about a place.
  `standing-on-arrival` (`batch.clj:249-282`): a row that holds the act's permission, where that
  permission says `:see`, is `{:standing :final :word :permission-does-not-allow-acting}`; the test
  stands before the one that gives `:stands`. `arrive` then gives the act no question but the one
  about its answer.
- **Step 2** (`:337-344`, on every task, after `(|all)`). `marks-places-asked` names the places;
  `rows-at` reads each from this task's copy of `$$microbatch-facts`; `reads-of` takes the rows and
  parses each once. A read of a place under a marked key holds `:since`, and `:read?` is true when
  an asker does not have the place as its own. `batch-end` is `last-stamp-of-batch` of `*base` and
  `*numbers`, both bound before `(|all)`; it reads no clock. `place-replies`: under a marked key
  the place is shared whenever two turns name it; each asker is told `:since`; the row goes to each
  asker that does not have the place as its own.
- **Step 3.** `own-places-wanted` adds the act's own places that are in `:since-by-place`. An act
  decided where it arrived writes the fact of its own place at once; each row of the facts by value
  is a parcel of the kind `:rows` to the task of its value digest. An act that meets carries
  `:since-by-place` and `:batch-end` in its parcel.
- **Step 4.** `write-row` writes or takes out. In the pass, `decide-next` gives each act its place
  rows as they are at its turn (what an earlier act of the pass wrote, else what the meeting task
  read, else what the act carried), and its `:since-by-place` as it carried it. `meeting-reads` adds
  the place of a setting or a list that names something to replace, and the place of a marks fact
  always. `rows-to-send` keeps one row for a kind and key, the last, which for a key of the facts by
  value can be nil.

**Traced with batches**, each also *run* in the pure proof's batch in memory in 200 orders of
arrival, and on the cluster in the module's proof:

- Two acts on one place under a marked key, each with the place as its own. Both are told
  `:shared? true`, so both meet. In the pass the first takes out the entry of the value before and
  puts its own; the second takes out the first one's entry, which was never written, and puts its
  own. Three rows are sent: two removals and one put. *Run*: module, "the first act's value has no
  row on any task".
- A mark and a fact under its key in one batch. The fact's home task read the marks as the batch
  began: not marked, so no `:since` and no entry. The marking act meets (rule 1), reads the marks
  row on the meeting task, and gives the new key the batch's end. *Run*: module, the fact's stamp is
  one below the key's stamp, and the fact is in no run.
- Two writes of the marks in one batch. The second act's row before is the first act's row in the
  pass, so a key that both hold keeps one stamp. *Run*: pure, the batch in memory.
- An act that arrives on another task than its marked place's. The reply carries the row before.
  *Run*: module, "replaced by an act that arrived on another task".

**The counts.** The same act, alone in its batch.

| | Reads | Row writes | Messages | The plan's table |
|---|---|---|---|---|
| The key not marked | 4: the permission, the answer, the lock, the marks row on the act's own task | 4 | 6 | 3 and the marks row once: 4; 4; 6: the same |
| The key marked | 5: and the row before, at the act's turn | 5, or 6 | 7, or 8 | 5; 5 or 6; 7 or 8: the same |

## 7. A1, A4 and R7 against the contract

- **A1.** The plain bytes come from one call of `seal/open` in `opened-fact`
  (`decision.clj:410-431`); `writes` hands entry i to `fact-row` for fact i. The row names no lock.
  The act in the log is not touched: no code of this part appends, and the door did not change.
  *Run*, each gate: the row's bytes are the plain bytes that the hand sealed; the log's record
  holds the sealed bytes and the lock ids and no plain value; the lock's row names the fact and
  opens the log's bytes to the row's bytes; an unsealed value is refused `:not-sealed`; an act
  whose first value opens and whose second does not leaves no row and no used lock.
- **A2.** `fact-row` and `by-value-row` take the act and write the one text of its `:who` and of its
  `:permission`. *Run*: for the system's making of a personal layer the who is `:system` and the
  permission the person's.
- **A3, A5, A6.** Each key is in `store-keys` as a setting, so the door leaves its value plain and
  check (f) holds it to the layer's entity.
- **A4.** Section 5. A making act names a first permission, three slots; `making-act?` is unchanged,
  so an act whose permission has four or five slots is refused `:layer-not-made`. *Run*.
- **R7.** `store-key-fault` (`decision.clj:267-295`) is unchanged in code: its two lines of one
  write name a depot and a permission, so the three new keys fall through to no word. A retract
  under any store key keeps its word. *Run*, each gate and each key: a first write, a second, one
  that names the fact there, a stale one, a retract.

## 8. What is not decided, and where each kept answer is code

| Thing | Built as | Its docstring says "not decided" at |
|---|---|---|
| One permission for a holder in a layer | As built | `declaration/store-keys`; `decision/permission-place` |
| Who may write the setting, the list, the marks | Whoever may act in the layer | `decision/who-may-write-fault`, whose body is the other answer |
| What a layer starts with | Read from no fact | `first-act/starts-with` |
| "See" stays "see" | A permission's place takes one write | `decision/standing`; `decision/store-key-fault` |
| What a marked key may hold | Every value, by its digest | `decision/by-value-writes` |

## 9. The explanations

Each line of the plan's table "Every explanation that the part makes untrue" was replaced, not
amended. The store's page gained a table for the entries 1, 2, 5, 6 and 7 of 11 and 12 October, a
paragraph in "Engineering choices" for each of: what a permission allows, the plain value, who
wrote a fact, the three keys, the mark, `:level-since`, the facts by value; and six lines in "What
the gate does not check". Its three rows that word an earlier decision of Sid's (F5 A, E3·H7 B,
K1 A) keep his wording; only "Where it shows" changed (ruling 35). The decisions sections of
`src-store/README.md` were not edited. The maps of `gate/`, `record/`, `module/` and `door/`, the
proofs' own map and the verification map were brought level.

## 10. Lint, and what was not verified

`clj-kondo` (the root `deps.edn` holds it) over the pure code, `door.clj` and the four proofs: 0
errors, 0 warnings. Over the three files of dataflow it reports what it cannot read of Rama's
dataflow, an output binding counted as an argument and a `*var` as unresolved; the same kinds of
line as on those files at the last commit. *Run.*

*Not verified*, by this step or any run of this build: an event that Rama discards and runs again;
a second attempt of a batch, which the build of `base-for-a-reader` is to show for this part's rows
too (ruling 53); a real cluster; what the one read more costs; a push on a run. The removal of a key
that holds no row reaching an open push was run by the check of the plan on a stand-in table, not on
this module.

## 11. Points for the build session

1. Section 4, point 1: `marks-places-asked` leaves out the place of a request for locks.
2. `read-value` gives `[:value nil]` for a row whose bytes are the text `nil`, as the plan's
   sentence reads. The door never seals them; a thing appended without the door can. Its docstring
   says so. If a reader should be told `[:not-a-value]` there, it is one test in that function.
3. `door/README.md:75` said "the three store keys" inside a sentence about the decisions of 8
   October. The plan's table has it changed to no count, and it now reads "the store's keys". If
   ruling 35 should hold that sentence as his wording, it is one phrase to put back.
4. The names `:by-value` and `value-digest` (ruling 54) are on the store's page under "The words of
   the read build", with one sentence that says where each comes from.
5. For the build of `base-for-a-reader`: `batch/replies` takes one argument more, the batch's end;
   `decide-or-send` and `rows-to-send` give rows of the kind `:by-value`, a nil row among them; the
   pure proof's model `one-at-a-time` decides each act with the marks as the record held them before
   the batch.
6. The groups of the module's proof are numbered anew in the proofs' own map: 30 where there were
   20.
