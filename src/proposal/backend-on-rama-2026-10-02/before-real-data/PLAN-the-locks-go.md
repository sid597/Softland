# Plan: the locks go

**The part.** `the-locks-go`, the first of the three parts of the build "before real data is kept":
the contract's A1 to A8 and C1 to C3. The contract is
`src/proposal/backend-on-rama-2026-10-02/BUILD-before-real-data.md`. "Entry N" is entry N of "Decisions
of 12 October 2026 — before real data is kept" in `src-store/README.md`. "Ruling N" is point N of
"Rulings" in this folder's `PROGRESS.md`. "The implied spec" is this folder's `IMPLICIT_SPEC.md`: J1 to
J22, P1 to P22 and its sections by number are its own. "The store's page" is
`src-store/softland/store/README.md`; "the proofs' map" is `test-store/softland/store/README.md`. A file
name without a folder is under `src-store/softland/store/`; a proof's file is under
`test-store/softland/store/`.
**The stage.** Phase 1 of the Rama skill, the plan. No file under `src-store/` or `test-store/` is
changed here.
**Who wrote it.** One fresh session (Opus 5.5), 12 and 13 October 2026, for the session that holds the
build to rule on. It wrote this paper and the folder `plan-probes-the-locks-go/`. It staged and
committed nothing.

**Marks.** **read** is read by this session in the source, at the file and line given, at head
`6d9a6a8d`. **run** is run by this session; the log is named. **doc** is read in a paper or a docstring
and not checked against the code. **gathered** is read by a gatherer that this session started (a
read-only agent, Opus 5.5 at effort xhigh, one for each proof) and not read by this session itself;
this session checked none of its line numbers. **reasoned** is derived here from the lines named and
not run.

**What was read whole.** The skill: `SKILL.md`, `phases.md`, `phase-1-plan.md`, `artifact-plan.md`,
`stream.md`, `microbatch.md`; the headings and the checks of `artifact-plan-validation.md`. The
contract. `src-store/CLAUDE.md`. Of `src-store/README.md`: its opening and table, and the decisions
sections of 6, 7, 8, 9 and 12 October. This folder's `PROGRESS.md`. Of `DECOMPOSITION.json`: this
part's entry whole, and the needs and risks of the other two. Of the implied spec: sections 1, 2, 3,
4.1 to 4.4, 5.1, 5.3, 6.1 to 6.3, 6.7, 6.9, 7.1 to 7.4, P1 to P7, P16 to P22, and 9. The store's page,
whole. The maps of `door/`, `gate/`, `module/`, `record/` and of `softland/`; `test-store/README.md`.
Of the source, each form: `act.clj`, `door/text.clj`, `door/digest.clj`, `door/additions.clj`,
`door/declaration.clj`, `door/words.clj`, `gate/decision.clj`, `module.clj`,
`module/streaming_gate.clj`, `module/microbatch_gate.clj`.
**What was read in part.** `door.clj`: lines 1 to 444 and 600 to 954 (not `settle`, `ask`,
`in-the-seat`, which go whole). `gate/batch.clj`: lines 1 to 155, 208 to 398 and 520 to 1116 (not the
turn, the gathering, the clock and the stamp of a batch, which this part does not change).
`record/rows.clj`: lines 1 to 514 (not the reads' tests and the functions that only a read calls).
`module/door_reads.clj`: its docstring and the head of `add-to`. `door/seal.clj`: the functions from
`known-way?` on. `door/stock.clj`: the five numbers. `record/tables.clj`, `gate/first_act.clj`,
`gate/stamp.clj`, `module/batch_clock.clj` and the read path's six files: every line that a search for
the whole words lock, seal, stock and request finds, and `read_path/answer.clj` at `counts?`.
`BUILD-store-module.md` and `BUILD-door.md`: every line that the same search finds.
`reads/PLAN-what-the-gates-keep.md`: its first 60 lines and its headings, for the form.
`reference/rama/docs/README.md`: its first section, which says that the copy is of Rama's docs while
1.6.0 was the newest release. No page of that copy was opened: no line of Rama's docs is leaned on here. Of the proofs: this session read none of the seven files itself. Each was read
whole by a gatherer (the module's, the door's and the pure proof), or in the parts named in its report
(the read path's two, the act's, the measures); the proofs' map was read by this session to line 288
and by the gatherers from there.
**Not read.** `src/app/server/env.clj` and every `.env` file. Every file whose name starts with `raw-`
under a `runs/` folder. The rig's code, the server's and Inland's; the rig's notes were not opened
either, since no question of this plan needed a finding of theirs beyond what the store's page cites.

**What was run.** Three runs of a probe of this session's own, in `plan-probes-the-locks-go/`, on Rama
1.6.0 in-process, one worker, four tasks; `RESULTS.md` there is their short. The two that start a
cluster went through `flock runs/cluster.lock`. No proof of the store was run.

**Blocked, or refused.** No tool call waited for a permission. One thing went wrong and was done
another way: the four gatherers were first started as agents of the type `code-hunter`, whose own
setting lets them read the client's tree only; each read nothing and said so. They were started again
as agents of the general type with the same brief, and each then read its proof.

**What changed in the tree while this was written.** The head moved from `6d9a6a8d` to `4bfc26a2`, by
commits of other sessions; no file under `src-store/` or `test-store/` differs between the two (`git
diff --stat`), so each line number here holds. The build's page gained rulings 31 and 32. Ruling 31 is
this plan's: **the part is built straight from this plan, with no check of the plan.** So the last
section says what the build should look at first, and section H is what the build session rules on
before the build starts. Ruling 32 is of the other plan.

**Rulings that cannot hold.** None. Two rulings are met by a reading that the build session should see
before the build starts; each is an open point, in section H:

- Ruling 19's condition holds for every case that was run, so the door changes nothing. One kind of
  value passes the condition and is still not kept as its maker gave it: a string with a lone
  surrogate (H1).
- Ruling 2 says that an opener "can still give them", of the two numbers of a maker's wait. Today an
  opener gives one of the two; the other is a constant. This plan keeps that (H7).

## How the skill's template is adapted

The template is for a new module. This part designs no new state. It takes two tables, one query, one
store key, two files and about a thousand lines out of a module that stands, changes the forms that
held them, and changes three numbers. So:

- **Reads** and **Writes** list each operation of the store that this part changes, as it is after the
  change.
- **PState Design** has no new PState. It says which two go, and what each field that stays holds.
- **Depots** says what a record of a depot of acts is after the change. That is section C, with its
  run.
- **Topologies and PStates** gives each gate's walk against today's dataflow, line by line where a
  line goes or changes. No topology is added or split.
- **Query Topologies** lists the two that stay and the one that goes.
- **Partitioning efficiency** is filled for the dominant read of the part's own changes, the door's
  read of an answer by act-id. The part changes no placement.
- **What one act costs each gate, before and after**, and **A second attempt**, are sections of their
  own: the brief of this stage asks for both.
- **Sections A to H** are the ones that the brief of this stage names. Sections B, F and G are what the
  build session of this part works from. C and D stand before B in this paper, because B names forms
  that C and D define.

A claim about Rama is marked run or doc. No target of latency or throughput is set: the contract sets
none ("The build measures, and sets no target").

## The short of it

1. **Twenty-two jobs of the locks and the seal, and eight more that this reading found, are each
   placed** (section A). None is left without a place, and several have two: eleven are needed no
   more, ten are done by another part of the store, and nineteen are things that Sid hears in the
   receipt, each with its sentence.
2. **A thing on a log of acts is the maker's act with two parts added, `:act-id` and `:depot`, and
   nothing else changed** (section C). The run of ruling 19's condition found the door's digest equal
   to a topology's for every thing appended, 138 things through each of two depots. So the door makes
   no normal form, and a list of facts goes as a list.
3. **A gate keeps, for a fact under a plain key, the one text of its value in its normal form, made
   once, in the last step of the declaration's check, before any row of the act's standing is read**
   (sections C and D). The text rides in the act's one form under `:text`, where today the sealed
   bytes ride under `:sealed`, and the row takes it as it is.
4. **Three numbers** (section D). The bound on one value: 65,536 bytes of that text, word
   `:value-too-large`. The one greatest depth of a permission, on both roads: 32, the value's own
   number. The bound on the size of an act: 18,874,368 bytes of text that an accept writes into the
   rows of its facts, word `:act-too-large`. Each is the builder's, kept, decided by nobody.
5. **The door's fourteen words become seven, its six calls five, its five handles four, and its five
   numbers one** (sections B and E).
6. **The proofs** (section B). The act's: 3 of 8 checks written again, for C. The pure: 2 of 18 go, 15
   are written again. The module's: 2 of 33 go, 23 are written again, 4 change in the hand alone; of
   its 354 assertions 88 touch a lock, 54 go and 34 are written again. The door's: 7 of 27 go, 5 go as
   checks of a lock and the thing that each also held is written again under a name of its own, 13 are
   written again in place. The read path's pure proof: 3 checks written again, which the implied spec
   did not count. The read path's proof: 3 written again. Eight checks are new.

## A. The list of A5, before anything is taken out

Every job that the locks and the seal do. J1 to J22 are the implied spec's (3.3); K1 to K8 are this
session's, found by its own reading and its run. "Where it goes" is one of three: **needed no more**,
with why; **another part does it**, with which and where; **Sid hears**, with the exact sentence for
the receipt. A job that could not be placed would be an open point: there is none.

| # | The job | Where it goes |
|---|---|---|
| J1 | A value in the log can be made unreadable for good, by destroying its lock | **Sid hears.** "The store can make no value unreadable. No lock is kept, so nothing can be destroyed to forget a value: a delete is a retract, and the value stays in the log and in the past." Entry 2 says it already; the receipt says it as what was taken out |
| J2 | One lock for one value: the grain of a forget | **Needed no more.** A grain of a forget needs a forget. Entry 2 sets entry 2 of 9 October aside and does not reverse it |
| J3 | Nothing secret is on a depot | **Sid hears.** "Every value is plain on a depot. A client of the cluster that reads a depot, or a copy of a log taken alone, now holds every value. Before, a log alone held sealed bytes, and the locks were in the tables." See K8 |
| J4 | A stop before the log: with an empty stock a value act is not appended when a gate refuses the door's request | **Sid hears**, and **another part does it**: a gate refuses the act itself, with the same word, under the act's own act-id (`gate/decision.clj`, `standing`; read at `:172-191`). "The door stops no act. Before, with an empty stock, the door did not append a value act when a gate refused its request for locks: for an unknown version, no session, a permission of another layer, a layer that is not made, a layer of the other gate, a wrong depot word, a who that is not the permission's holder, a permission that does not stand, and a permission that says see. Each such act is now in the log, with a recorded refusal under its own act-id, and no fact of it is in the tables." |
| J5 | A tie of a sealed value to its place: sealed bytes copied to another entity, key or layer do not open | **Sid hears.** "A value is no longer tied to its place. A value copied out of the log into another act, on another entity, key or layer, is a value like any other: whether it is accepted rests on the sender's permission alone." |
| J6 | A bound on the bytes of one value, held by the door before the log and by a gate | **Another part does it**, for the gate's half: the last step of the declaration's check (section D). **Sid hears**, for the door's half: "The bound on one value, 65,536 bytes, is held by a gate alone. Through the door a value of any size that Rama's client writes is now in the log: a run put a value of 20,000,000 characters there. A gate refuses it." |
| J7 | A tie of a sealed value to a layer and a depot, through its lock's id; and, by the door alone, to a who | **Needed no more**, for the first: a lock's layer was held so that a lock of one layer could not seal in another, and there is no lock. **Sid hears**, for the second: "No value is tied to who asked for its lock. The door kept that tie so that a forget of a person could find her values by her requests; nothing ever read it, and it is gone." |
| J8 | One accepted fact for one run of sealed bytes: a sealed fact copied out of the log into another act of the same place is refused | **Sid hears.** "A fact copied out of the log and sent again under another act-id is accepted when its sender may act there. Before, the lock that it named was used, and a gate refused it." The same act under the same act-id is still answered once (check (a)) |
| J9 | A check that the bytes of a value arrived as they were sealed: the cipher's tag fails for changed bytes | **Another part does it**: the digest. A gate takes the digest of the whole thing as it arrived, and a maker reads her answer by the digest that the door gave her; a thing that changed between the door and a gate reads "not mine" (`door/words.clj`, `answer`; read at `:162-188`). The digest covers every part of the thing, where the tag covered each value alone |
| J10 | A stop before the log, of the door's own: a value that is no value of the form, and a value in a fact that is a record, are not appended | **Another part does it**: the engine's leaf `:value` refuses such a value `:malformed` at a gate (`act.clj:326`, `:353`). **Sid hears**: "A value that is no value of the form is appended and then refused by a gate. Before, the door told its maker and appended nothing." |
| J11 | A stop before the log: an act with more than 256 values is not appended; and the walk of facts with no end stops at the 257th value | **Another part does it**, for the first: `:too-many-facts` at a gate (`door/declaration.clj:476`). **Sid hears**, for the second: "A thing whose facts have no end is no longer stopped at its 257th value: the door's call does not come back for it, as it does not today for a thing that has no end in another part." |
| J12 | Which bytes stand for a value: the door makes them, and a gate keeps what a lock opened, whatever the bytes are | **Another part does it**: a gate makes them, as the one text of the value's normal form (section C). **Sid hears**, as ruling 5 has it: "No arrived value can be a run of bytes. The one road by which bytes that are no value reached a fact's row, a seal made without the door, is closed." |
| J13 | Every value under a plain key crosses the depot as a run of bytes | **Sid hears**, with the run: "Every value now crosses Rama's writing and reading of a record as Clojure data. A run on Rama 1.6.0 in-process found the door's digest equal to a topology's for a value of each kind that the form takes, 81 values through each of two depots." See K2 |
| J14 | Each send of an act with a value is another thing, with another digest | **Sid hears**, as ruling 6 has it: "A maker's own second send of one act under one act-id is the same thing, with one digest. Both sends read the one answer as their own; 'not mine' is said only of a thing that differs." |
| J15 | A pace on one who: the seat, the size rule, the wait of a request in the base | **Sid hears**, as ruling 20 has it: "Nothing paces a sender. The locks were the one thing that paced a who's values through the door." |
| J16 | An order that the depot must hold: a request before the first use of its lock | **Needed no more.** There is no request. The other two orders that `module.clj:77-79` names stay |
| J17 | Acts that no maker made: each request is a record on a depot of acts, with an answer and a row of the facts table | **Needed no more**, and **Sid hears**: "The door sends no act of its own. Each record that the door appends is a maker's act, and a read that asks for the store's own facts is shown no request." |
| J18 | A record of which request, and so which who, permission and session, asked for the lock that seals a fact | **Sid hears.** "No row says who asked for the lock of a value. The room that was kept for forgetting a person by her requests is gone with the locks." The fact's own row still holds who wrote it and under which permission |
| J19 | One of the two reasons for closing, and one of the two things that a second attempt of a batch makes anew | **Needed no more**, for the lock's half. **Another part does it**, for closing: it stays, for its other reason, the word for a layer of the other gate (`module/door_reads.clj:43-59`, read). After this part a second attempt of a batch makes one thing anew, that word, and no bytes |
| J20 | The door's memory, its sixth call `stock-count`, and its numbers | **Needed no more**, and **Sid hears**: "The door holds nothing between two calls. Its call `stock-count` is gone, and an opener gives one number where she gave five." Ruling 2 |
| J21 | A stop before the log, of the stock's making: a value act whose `:who` cannot be hashed or compared is not appended | **Another part does it**: a gate refuses a who that is no id `:malformed`. **Sid hears**, with J10: such an act is appended when Rama's client can write it |
| J22 | A home for two numbers that are not a lock's: the longest wait that the door counts, and the sleep between two reads of a maker's wait | **Another part does it**: a pure namespace of their own, `door/wait.clj` (section B) |
| K1 | **A thing that throws while it is read is told so before any round trip.** The door's walk gives `:read-the-act` at the part whose read threw, and nothing is appended (`door/additions.clj:114-126`, `:256-274`, read). With no walk the door reads `:layer` alone. The digest hides a throw (`door/digest.clj:53-54`), and Rama's client then meets the same throw, so the word would be `:append-cleanly`, of which "nothing was appended" cannot be said | **Another part does it**, as this plan proposes: the door takes its digest by a walk that tells a throw, and gives `:read-the-act` with nothing appended. Open point H2 |
| K2 | **Which code replaces a character that cannot be written.** The door's `text/utf8` writes a lone surrogate of a string as `?` before the seal, so what crosses Rama is a run of bytes that Rama does not change (run, `logs/sizes.log`, S5) | **Sid hears.** "A string that is not well-formed UTF-16, one with half of a surrogate pair, is kept with `?` in the place of the half. Before, the door made that replacement; now Rama's writing of a record makes it. The maker reads her answer as her own all the same." Open point H1 |
| K3 | **The door told a maker the gate's reason inside its own call.** The word `:have-its-request-accepted` carries the request's answer with the gate's word (`door/words.clj:88-91`, `:133`) | **Needed no more.** Her act's own answer says the word, one call later (`answer-of`) |
| K4 | **A place for a request on the microbatch depot**: "a request for locks arrives where its locks and its answer will be" (`module.clj:87-89`) | **Needed no more.** The sentence leaves the docstring |
| K5 | **Check (a) kept a second run of a streaming event from making a lock again** (`module/streaming_gate.clj:108-113`; the store's rule 5) | **Needed no more.** Check (a) stays for its other reasons: one answer for one act-id, no stamp given twice, no entry of the facts by value taken out again |
| K6 | **The most locks of one request, 256, bounded the rows that one event writes for a request** (`door/declaration.clj:81-84`) | **Needed no more.** The limit goes with the request. The rows of one event stay bounded by the 256 facts of one act |
| K7 | **A second walk of four parts of the maker's thing before a seat is taken**, so that a part with no end holds its own maker's thread and no other (`door.clj:676`) | **Needed no more.** There is no seat. A thing with no end holds its own maker's thread, in the digest (J11) |
| K8 | **A copy of a depot alone gives no value.** Today a backup of a log, or a mirror of it, holds sealed bytes, and the locks are in another place, the tables. Nobody meant it (J3 is the meant half) | **Sid hears**, in J3's sentence. It bears on entry 5, the backups: a backup of the logs is now a copy of every value |

**What the list does not hold, on purpose.** Costs that go with the locks are no jobs: one read of a
lock's row and one opening for each value, a row written again, an event and about 550 bytes for each
request. They are in "What one act costs each gate". And two things that look like jobs and are not the
locks': the read of a layer's depot on each send (finding 6) stays as it is; and closing stays.

## Reads

Each read of the store as it is after this part. None is added.

| Read | Access | After this part |
|---|---|---|
| The answer under an act-id (`answer-of` of the door) | The query `answer-by-act-id [routing-id act-id]`, straight to one task: the layer's for a streaming act-id, the uuid's for a microbatch one. One read of the answers; for a microbatch layer one more, of the task's `:closed-through`, when a row is there | Unchanged. The digest that it gives is of the thing as it arrived, with each value plain |
| The depot of a layer (the door, for each act-id that it makes) | The query `depot-of-layer [layer]`, to the layer's task | Unchanged (finding 6) |
| The locks of a request | The query `locks-of-request` | **Gone**, with the two tables that it read (A3) |
| A fact, a run of facts, what points at a thing (the read path) | Selects and pushes of the four tables of facts, with a partitioning key | Unchanged in each call. What a row's `:value` holds is, for each value that came through the door, the String that it holds today (finding 7; run, `logs/sizes.log`, S1: the row's String of today is `=` to the one text of the normal form, for six values) |
| The strays of a gate, the kept clock (an operator) | Selects of `$$streaming-task`, `$$microbatch-task`, `$$batch-clock` | Unchanged |

## Writes

| Write | Depot, and who appends | After this part |
|---|---|---|
| A maker's act | `*streaming-depot` with `:ack`, or `*microbatch-depot` with `:append-ack`: the door, one append for one send | The thing is the maker's act with `:act-id` and `:depot` added (section C). No append comes before it |
| The door's request for locks | The same two depots: the door, when a stock is short | **Gone.** The door appends no act of its own |
| A batch's clock | `*batch-clock-depot`: the microbatch gate, once for a batch with things | Unchanged |

Nothing else appends (read: a search of the source for an append finds `door.clj:215-216` and
`module/microbatch_gate.clj:441`, as the implied spec's L7 has it).

## PState Design

No PState is designed. Two go: `$$streaming-locks` and `$$microbatch-locks`, with their schema `locks`
(`record/tables.clj:164-170`). Why this is the only reasonable form: A3 says it in so many words, and
no row of another table names a lock (read: `record/rows.clj:54-57`, "No row names a lock").

What stays, and why nothing of it changes.

- `answers`, `facts`, `facts-by-value`, `task`, `microbatch-task`, `batch-clock`: each schema as it is.
- The field `:value` of a fact's row stays a `String`. The reason is the read build's, run by it and
  kept: a push on rows that hold a byte array is sent the whole value anew (the store's page, "The
  value in a fact's row is plain, and a String"). What changes is that the String has **one form**:
  the one text of the value. The second form, U+0000 and one character for each byte, was for bytes
  that are no text, and only a seal could bring those (ruling 5).
- No key changes, so no row moves to another task.

**A module update on a cluster that holds the two tables.** Not run and not read in Rama's docs by
this session: what Rama asks of an update that declares two PStates fewer. It does not arise in this
build: A7 says that nothing real is in a log, the store runs only on clusters that its proofs start,
and the two proofs that update the module update it to itself. The receipt should say that the first
real cluster starts with the module as this build leaves it.

## Depots

Three, as today, declared as today (`module.clj:115-117`): `*streaming-depot (hash-by :layer)`,
`*microbatch-depot (hash-by batch/first-entity)`, `*batch-clock-depot :disallow`. No trimming option
(R2). What a record of a depot of acts is after this part is section C. Both partitioners run on the
appending client and read the thing as the maker gave it: `:layer`, and the entity of the first fact.
The second calls `first` on `:facts`, which walks the first chunk of a lazy sequence and changes
nothing of it (run: `:facts` as a lazy sequence went through that depot with its digest unchanged).

## Topologies and PStates

Three topologies, as today. None is added, split or changed in its type.

- **`streaming-gate`: stream.** Why this type: both of the template's reasons. The door's append
  waits for the gate (`:ack`), and a streaming layer is for what a person does directly (7 October,
  entry 4). Every concern in it needs that: it is one event that decides one act. PStates after this
  part: `$$streaming-answers`, `$$streaming-facts`, `$$streaming-facts-by-value`, `$$streaming-task`.
  Each write, and whether a second run writes the same:
  - a stray's digest, added to a set (`NONE-ELEM`): the same however often it runs;
  - each row of a decision, a whole row at a fixed key (`termval`) or a fixed key taken out
    (`NONE>`): the same write however often it runs, and behind check (a);
  - today's one write that is not the same on a second run, a new lock's random bytes, is gone.
- **`batch-clock`: stream.** Unchanged, and not this part's.
- **`microbatch-gate`: microbatch.** Unchanged in type. PStates after this part:
  `$$microbatch-answers`, `$$microbatch-facts`, `$$microbatch-facts-by-value`, `$$microbatch-task`,
  and the three temporary ones.

No schema holds `Object`; none is added.

### The streaming gate's event, against today's dataflow

`module/streaming_gate.clj`, `add-to` (read, `:188-347`).

| Step | Today | After |
|---|---|---|
| The declarations (`:189-193`) | Five PStates | Four: the line of `$$streaming-locks` goes |
| 0. A stray (`:198-202`) | The test, the digest, the set | Unchanged |
| 1. Check (a) (`:206-211`) | One read of the answers | Unchanged |
| 2 and 3. The digest, the declaration's check, the clock, the 10-second rule (`:214-217`) | `digest/digest`, `declaration/check`, `refused-early` | The same four lines. `declaration/check` now ends with the step that makes and counts the texts (section D), so a value over the bound, and an act over its bound, come out here as `*declarations-reason`, before any row of the standing is read |
| 4. Standing (`:220-243`) | One to four reads | Unchanged |
| 5. Places (`:247-261`) | One read for each fact that names something to replace or is a permission fact | Unchanged. Its `*refused-places` is now the act's last refusal |
| 6. The seal (`:264-282`) | One range of locks for each request named; `decision/opened` | **Gone, all nineteen lines.** `*refused` is what step 5 gave |
| 6b. The marked keys (`:287-307`) | One read of the marks, one of each row before | Unchanged |
| 7. The stamp and the writes (`:311-347`) | `in-hand` holds `:lock-rows`, `:plain`, `:since-by-place`, `:place-rows`, `:level-from`; seven kinds of write | `in-hand` holds the last three. The cases `:lock` and `:new-lock` go (`:336-341`). Five kinds stay: `:fact`, `:by-value` in and out, `:answer`, `:last-stamp` |

`new-lock-row` goes (`:164-170`), and the require of `door.seal`. `rows-as-map` goes with the one read
of a range that used it (`:172-177`).

The event still has no partitioner and gives its task back nowhere (ruling 27 holds of the tree as
this part leaves it).

### The microbatch gate's four steps, against today's dataflow

`module/microbatch_gate.clj`, `add-to` (read, `:359-567`).

| Step | Today | After |
|---|---|---|
| The declarations (`:373-377`) | Five PStates | Four: the line of `$$microbatch-locks` goes |
| 1. Arrive and question (`:382-419`) | The turn; a stray; `declaration/check`; one or two reads of standing; `batch/arrive` | The same lines. `batch/arrive` gives no question about locks. A value over the bound, and an act over its bound, are refused here as the declaration's word, and so ask for their answer only |
| 2. Reply (`:423-492`) | The clock and the base; the closing; the marks; one read for each key asked, of three kinds | The case `:locks` goes (`:482-484`). Two kinds of read stay: an answer, a place |
| 3. Decide (`:495-517`) | `own-places-wanted`, `decide-or-send`, the task's own fact rows written at once | The same lines |
| 4. Write and meet (`:520-567`) | `rows-in-write-order`; `write-row` with four PStates; a lock row drawn for each act of a pass; `decide-next` with five arguments | The rows are written as they were sent: no order is needed between them (see `batch.clj` in section B). `write-row` takes three PStates and three kinds. The line that draws a lock row goes (`:555`). `decide-next` takes four arguments |

`new-lock-row` (`:233-241`) and `lock-rows-in` (`:265-287`) go, and the require of `door.seal`.
`rows-as-map` (`:243-247`) goes with `lock-rows-in`, its one caller.

## Query Topologies

Two stay, unchanged: `answer-by-act-id` and `depot-of-layer`. One goes: `locks-of-request`
(`module/door_reads.clj`, read at the docstring; `:125-159` by the implied spec).

- `answer-by-act-id`. A streaming act-id with an answer: 1 read, 1 meaningful. A microbatch act-id
  with an answer: 2 reads (the row, then `:closed-through`), 2 meaningful. An act-id with no answer: 1
  read, which finds nothing; the answer "nothing yet" is what that read is for. The second read is
  made only when the first found a row, so the count follows the input by a branch that is there
  today.
- `depot-of-layer`. 1 read, or 2 for a row of a microbatch layer; unchanged.

## What one act costs each gate, before and after

The act: one value under a plain key, on one entity, nothing named to replace. Reads and writes are
counted from the dataflow, as the store's page counts them, and not measured. "Before" is the page's
own count (read, the store's page, lines 390 to 409 and 702 to 727).

**The streaming gate, one event on one task.**

| | Reads before | Reads after | Row writes before | Row writes after |
|---|---|---|---|---|
| Accepted, the key not marked | 5: the answer, the permission, the lock, the marked keys, the last stamp | **4**: without the lock | 4: the fact, the lock, the answer, the last stamp | **3**: without the lock |
| Accepted, the key marked | 6: and the row before | **5** | 5, or 6 when an entry goes out | **4, or 5** |
| Refused for its standing | 4 at most | 4 at most | 2: the answer, the last stamp | 2 |
| Refused by the declaration, a value over the bound among them | 2: the answer, the last stamp | 2 | 2 | 2 |

**The microbatch gate, the act alone in its batch.** The messages are those between two tasks.

| | Reads before | Reads after | Row writes before | Row writes after | Messages before | Messages after |
|---|---|---|---|---|---|---|
| Accepted, the key not marked | 4: the permission, the answer, the lock at its home, the marked keys | **3** | 4: the fact, the lock's whole row, the home's row, the answer | **2**: the fact, the answer | 6 | **3**: the question about the answer, its reply, the answer's row |
| Accepted, the key marked | 5 | **4** | 5 or 6 | **3 or 4** | 7 or 8 | **4 or 5** |

The count of 3 messages is reasoned from `gate/batch.clj`: an act on one entity asks one question that
leaves its task, about its answer (`:329`); gets one reply (`:584-602`); and sends one row away, the
answer (`:828`). The three that go were the question about the locks, its reply, and the home's row of
the lock. What each thing that arrives pays beside these, the read and write of its task's count of
turns, is unchanged.

**What the door pays for one send.** Before: one append, and one read of the layer's depot when no
act-id was given back; with a stock that is short, three round trips more in a streaming layer, and in
the base the time until the request's batch is closed (`door.clj:797-802`, read). After: one append,
and at most the one read. No send waits for another send.

**What stops being kept.** For each value: one row of a lock in a streaming layer, two in the base.
For each request: one record on a depot of acts, one answer, one row of the facts table, and a row for
each lock asked for, used or not. The store's page gives the home's row of a used lock as about 105
bytes (doc); `door/stock.clj` gives a request as about 550 bytes "for good" (doc).

**The work of a gate's thread on one value** (run, `logs/sizes.log`, S1; the middle of some runs in
one JVM, a floor and not a measure of a gate). For a value whose text is 65,536 bytes, a string:

| | Before, microseconds | After, microseconds |
|---|---|---|
| The digest of the thing | 44, over the sealed bytes | 397, over the text of the value as given |
| The seal opened | 472 | none |
| The row's String | 56, from the opened bytes | 359, the one text made, once |
| The bytes of that String counted, for the bound | none: an array's length | about 20, one pass over the String (reasoned: a pass that makes no array) |
| Sum | about 570 | about 780 |

For the string `"hello"` the same rows give 8.3 before and 4.7 after. So a small value costs a gate
less than today, and a value near the bound about a third more, all of it the two walks that make a
text of the value: the digest's, of the value as given, and the row's, of its normal form. A map of
520 small maps, 55,941 bytes of text, costs 1.3 ms for each of the two. And the door's side: before,
the text made, the seal and the digest, about 0.9 ms for the string of 65,536 bytes; after, the digest
alone, 0.4 ms.

## A second attempt (R7)

For each write that stays, what a second attempt of an event or of a batch leaves.

**The streaming gate.** Rama can run a record again after its writes were committed; the stream probe
saw every record since the topology's last written progress run again after a throw (ruling 29, run by
that probe).

- *After a commit.* The second run reads the answer at check (a) and ends. It writes nothing. This is
  as today (`module/streaming_gate.clj:108-113`, read), and no part of it rested on a lock.
- *After a discard.* The second run is the first that counts. Its rows are those of the first but for
  the stamp, which holds the clock; the discarded rows were never visible. Today a second run also
  makes other lock bytes; after this part **no write of a decision holds anything but what the thing,
  the rows read and the clock give**.
- *A stray.* Its write is not behind check (a): an add to a set, the same however often it runs.
  Unchanged.
- *The order after a failure.* Not this part's, and not changed: Rama's default (ruling 20).

**The microbatch gate.** Rama sets a gate's PStates back when an attempt fails, and each attempt takes
the batch's kept clock (the store's page, "The clock of a batch"; run by the read build's proofs).
After this part every row of a second attempt is the row of the first, in every field. One thing can
still differ, as today: the word with which an act into a layer of the other gate is refused. So
closing stays (J19). What this part hands to `answers-in-a-log`: a second attempt of a batch makes
anew that one word and nothing else.

**The door.** One append for one send. An append that threw may have gone through: the word is
`:append-cleanly`, the door sends nothing again, and the maker reads the record. When she sends the
act again under its act-id, a gate answers the first and decides nothing twice (check (a)); both sends
now read that answer as their own (J14). What a retry can no longer leave: a lock that was taken and
seals nothing.

**The append of a batch's clock.** Unchanged, and not this part's.

## Partitioning efficiency

This part changes no placement and no partitioner. The dominant read of what it touches is the door's
read of an answer by act-id.

**The placement that the read wants.** One task for one act-id, found from the act-id alone. That is
what the module has: a streaming answer on its layer's task, a microbatch answer on the task of its
uuid; the query's first argument is the routing id, which Rama's client hashes. Nothing is derived
anew here.

The proportions are assumed, not measured: most layers are streaming layers (7 October, entry 4), and
a maker that reads its answer after an ack of the streaming depot finds it.

### N = 1 task
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A streaming act-id with an answer | 0.85 | 1 | 0 |
| A microbatch act-id with an answer of a closed batch | 0.10 | 2 | 0 |
| An act-id with no answer yet | 0.05 | 1 | 0 |

Weighted seeks = 1.10 | Weighted iterator reads = 0

### N = 16 tasks
The same three rows with the same numbers: the query goes to one task and never fans out.
Weighted seeks = 1.10 | Weighted iterator reads = 0

### N = 128 tasks
The same. Weighted seeks = 1.10 | Weighted iterator reads = 0

Flat. The write path is flat too: a streaming act reads and writes on its layer's one task, and a
microbatch act on one entity reads on two tasks, its own and its answer's, whatever the number of
tasks is. After this part it is one task fewer in the base: the home of a lock.

## Design Decisions

- **Subindexing.** None changes. Each PState that stays is a flat map from a string key to a row.
- **Colocation.** None changes. A fact's row is where it was; the one row that sat apart from its
  value, a lock's at its home, is gone.
- **Where the text of a value is made.** In the declaration's check, as its last step, once. Section
  D gives the two forms that were weighed.
- **The door reads one part of a maker's thing, `:layer`.** It tests that the thing is a plain map,
  that it does not hold `:act-id` or `:depot`, and that its layer is an id that an act-id can hold
  (ruling 1). It reads no who, no permission, no fact (R4).

## State primitive selection

- `$$streaming-answers`, `$$streaming-facts`, `$$streaming-facts-by-value`, `$$streaming-task`, and
  the same four of the microbatch gate (PStates): each written by its gate, a bounded count of rows
  for one act, 256 facts at most. Durable. Unchanged.
- `$$streaming-locks`, `$$microbatch-locks` (PStates): gone.
- The door's stocks (the memory of one process): gone. The door keeps its four handles and one
  number, fixed at the opening, and nothing that changes between two calls (ruling 2).
- No TaskGlobal, no cache, no external store. None is added (R5).

## Resource usage analysis

**Disk.** Less, in three places. The two tables of locks: one row for each lock that was ever asked
for, used or not, for ever. The log: for each value the 28 bytes that a seal adds to its plain bytes
(`door/seal.clj:109-118`), the lock id that each sealed fact carries, and the whole record of each
request. The facts: a request's own row. What a value takes in the log changes from the bytes of its
text to Rama's own writing of the value as data; that size was not measured. A fact's row is as it
was. **Memory.** The door's grows with nothing; today it grows with the whos that wrote a value through
it. **Minimization.** Nothing is kept twice by this part. One thing is carried twice inside one
attempt of a batch, as today: the entry of an act in `$$questions` holds the thing and the act
(`gate/batch.clj:392`, read), so the bytes of a value are in it twice, today as the same sealed bytes
and after as the value given and its text. That is open point H5, and it is not built here.

## C. The form of a thing on a log of acts and under the door's digest

Rulings 19 and 6; the contract's "The builder chooses", its first line. Provisional, the builder's.

### The form

**On the log: the maker's act, as she gave it, with two parts added.** `:act-id`, `[layer depot
uuid]`, given back by the maker or made by the door; and `:depot`, the second slot of that act-id.
Every other part is the maker's own object: the door fills no missing part, reorders nothing, makes no
part a vector, and gives no value its normal form. A fact on the log is the maker's fact: `:e`, `:k`,
and when she gave them `:v` and `:replaces`. No fact holds `:sealed` or `:lock-id`.

**Under the door's digest: that same thing.** The door takes `digest/hex-of` of the thing that it
appends, and gives its maker the 64 hex characters. A gate takes `digest/digest` of the thing as it
comes off the depot. Both are the one function of today (`door/digest.clj:41-61`, read), which this
part does not change in its code.

What the door does today that is neither sealing nor one of its two additions, and stops doing: it
makes `:facts` a vector when it is a sequence (`door/additions.clj:410-411`, read), and the bytes that
it seals are of the value's normal form (`:227`).

### The run of ruling 19's condition

Run by this session before the form was fixed: `plan-probes-the-locks-go/`, `logs/digests.log`; the
short is `RESULTS.md` there. A module of the probe's own on Rama 1.6.0 in-process, with one depot
placed by `:layer` and read by a stream topology, and one placed by the store's own
`batch/first-entity` and read by a microbatch topology. For each case: the store's digest of the thing
before the append, of the thing inside the topology, and of the record that a client read back from
the depot's log.

| What was sent | Cases | The door's digest is the topology's, on each depot |
|---|---|---|
| A value of each kind that the form takes, as a fact's `:v`: each scalar kind; the two ends of the long range, an Integer, a Short, a Byte; big integers on both sides of the long range, of both classes; doubles with -0.0, the least and the greatest; decimals with trailing zeros, a zero with a scale, exponents of 400; strings with U+0000, with characters outside the first plane, with both, of 200,000 characters; each collection empty, with nil inside, sorted, a subvector, of 100 entries; nested, inside 32 collections | 81 | 81 of 81 |
| `:facts` as a vector, a list, a lazy sequence not yet walked, one walked, one made by `concat`, one made by `lazy-seq` and `cons`, a chunked sequence, a `Cons`, a subvector, each empty form | 18 | 18 of 18 |
| Parts of the act that are no value: the whole act a sorted map; `:subjects` a sorted set and `:based-on` a sorted map; an Integer as `:version`; an own permission of depth 4, 5, 32, 33, 63, 64, 65 and 200; a given one of depth 32 and 33 | 14 | 14 of 14 |
| Kinds that the form does not take, which a gate refuses and whose maker still reads her answer by her digest: a list and a lazy sequence as a value, a ratio, a float, a character, a symbol, NaN, an infinity, a keyword that does not read back, a value inside 33 and inside 70 collections, a run of bytes, a date, a Java list, a Java map, an array, a record, `:facts` that is no sequence; and two values that hold such a keyword | 25 | 25 of 25 |
| A thing that holds a plain Java object | 1 | Not appended: Rama's client throws |

**No kind differed.** So the form stands as ruling 19 gives it, and the door changes nothing for the
digests to agree. The digest of the record that a client reads back from the log is the same too: a
later reader of the log, which is what a rebuild is, takes the digest that a gate took.

What the run found beside the condition.

- **Classes change on the way, and the one text is blind to each change that was seen.** A lazy
  sequence and a `Cons` come off the depot as a chunked sequence; a subvector as a vector; an array
  map of nine entries as a hash map. A list stays a list. The text writes every sequence that is no
  vector as `( … )` (`door/text.clj:207`, read), so a list, a lazy sequence and a chunked sequence
  have one text, and a vector another. It follows, and the proofs must hold it: a maker who gives her
  facts as a list gets another digest than one who gives the same facts as a vector, and each reads
  her own answer.
- **For every value of a kind that the form takes, and every form of `:facts`, the record read back is
  `=` to the thing appended.** So "a maker's value is in the log as she gave it" holds by Clojure's
  equality for those cases.
- **One kind of value passes the condition and is not kept as given: a string with a lone surrogate.**
  It is a value of the form. Rama writes the lone half as `?`. The digests agree, because the digest
  is taken of UTF-8, which makes the same replacement; the text and the value do not. Today the door
  makes the same replacement before it seals. A gate cannot see it. Open point H1.
- **A thing nested 20,000 deep is written and read back; one nested 100,000 deep is not appended**
  (`logs/deep.log`). The one text stops at 65 collections, so a gate's walks of such a thing are
  short; Rama's own writing and reading of it are not. That run had one stack size for the client and
  for the tasks. Open point H6.

### What a gate makes of the thing: the one form on arrival

The declaration's check gives a gate the act in its one form on arrival, as today. After this part:

- **The shape.** `:arrived-act` is `:act-id`, `:depot`, then the ten parts of the maker's `:act` as
  `act.clj` declares them, `:facts` among them as the maker's own `[:seq-of :fact]`. The shape
  `:arrived-fact` goes: what arrives as a fact is the maker's `:fact`. A fact that carries `:sealed`
  or `:lock-id` is refused `:unknown-part`, by the engine's rule for a part that the shape does not
  name (`act.clj:354`; ruling 4). A fact under `:softland.store/lock-request` is refused
  `:store-key-not-known`, by a rule that stands (`door/declaration.clj:484-487`).
- **Each fact of the one form holds `:e`, `:k`, `:v`, `:replaces` and `:text`.** For a fact that has a
  value, `:text` is the one text of the value's normal form, a String: what the fact's row will hold.
  For a fact under a store key `:v` stays beside it, the value in its normal form, which the rules
  and the checks read; such a value is small by the store keys' own limits. For a fact under a plain
  key `:v` is nil: a gate reads nothing of a tool's value, and keeps its text. For a fact with no
  value both are nil. The reason is in section D: the text is made once.
- **This is the shape that the one form has today**, with another thing in the second place: today a
  fact under a plain key holds `:v` nil and `:sealed`, the bytes that become its row's value
  (`door/declaration.clj:639-647`, `with-sealed`, read). So each function of `gate/decision.clj` and
  `gate/batch.clj` that asks whether a fact has a value asks it of a store-key fact alone, today and
  after (read: `store-key-fault` at `decision.clj:289-297`; `accept-writes` at `:553`, `:565`;
  `first_act.clj`'s test of a making act).

### What the parts after this one stand on

- **The digest of B1** is `digest/digest` of the thing as it came off its depot. This part does not
  change the function.
- **The rows of B3** are made from the thing by two calls that a gate makes: `declaration/check`,
  which gives the one form on arrival, and `decision/writes`, which gives each row as data. A rebuild
  that reads a thing from a log and makes those two calls makes a fact's row by the functions that
  the gates call (finding 11). The row of a fact with a plain value is `fact-row` of a fact that holds
  `:text` (finding 7).
- **Ruling 14** is within reach: giving a thing its form is `declaration/check`, which decides nothing
  of the record; what it refuses is recorded as an answer by the gate that decided it.

## D. The bounds

Rulings 3, 16 and 17; C1 to C3; the implied spec's 5.3. Each number is **the builder's, kept, decided
by nobody**.

### D1. The bound on the bytes of one value

**The number.** `max-value-bytes`, 65,536, in `door/declaration.clj` where `max-sealed-bytes` is
today. **What is counted.** The UTF-8 bytes of the one text of the value in its normal form: the bytes
that the fact's row holds for the value (ruling 3). **For which values.** A value under a plain key. A
value under a store key keeps its own limits, as today. **The word.** `:value-too-large`, in the place
of `:sealed-value-too-large`.

**Where it is held: by a gate, in the last step of the declaration's check.** The check has two steps
today, the shape and then the rules. It gets a third, after the rules:

> **the values.** For each fact that has a value, in the act's order: the one text of the value is
> made, `(text/text v)` of the normal form that the engine gave, and its UTF-8 bytes are counted. For
> a fact under a plain key, a count over `max-value-bytes` gives `:value-too-large`, and the step ends
> there. The text goes into the fact's one form as `:text`; under a plain key `:v` becomes nil. Then
> the bound on the act (D3).

**How a gate counts it before it reads a row.** Both gates run the declaration's check before any row
of the act's standing or of its places is read, today and after:

- the streaming gate at its step 2 (`module/streaming_gate.clj:215`, read). One read comes before it,
  check (a), the answer under the act-id (`:210`); that read is what lets a repeat cost nothing more,
  and it is not a read "for" the act's decision. A thing refused by the check then reads the task's
  last stamp and writes its answer (`:311`, `:343-347`).
- the microbatch gate at its step 1 (`module/microbatch_gate.clj:403`, read). Before it the task
  reads and writes its count of turns (`:389-392`). A thing that the check refuses reads no
  permission (`:404-406`) and asks for its answer alone (`gate/batch.clj:384-388`).

So this plan reads ruling 3's "before the gate reads any row for it" as: before any row of its
standing, of its places or of the marked keys. That is where today's bound on the sealed bytes stands.

**How often the text of a value is made for one act.**

| The act | Times the one text of a plain value is made by a gate |
|---|---|
| Refused by the shape or by a rule of the declaration | 0. The step of the values is the last |
| Refused `:value-too-large` | Once for each value up to the first that is over the bound, in the act's order; none after it |
| Refused `:act-too-large`, by the 10-second rule, for its standing or for a place | Once for each value. The texts are dropped with the decision |
| Accepted | **Once for each value.** The String that was counted is the String that the fact's row holds |

Beside these, the digest's walk feeds the text of the value as given to the hash, piece by piece, once
for each thing that a gate decides; that is today's walk, and it now meets a value where it met sealed
bytes. And under a marked key the row's String is turned to bytes once for the value digest, as today
(`gate/decision.clj:560`, `record/rows.clj:273-279`).

**Why the step is the last, and why the text rides in the one form.** Two forms were weighed.

- *A. A rule of the declaration counts the bytes, and `fact-row` makes the text again.* The simplest
  code: the one form on arrival is `act.clj`'s own with two parts more, a fact's row is `(text/text
  (:v fact))` for every fact, and the bound is one more pair in `rules`. It makes the text of each
  accepted value twice, once to be counted and once for the row. A count without a text does not
  help: a walk through the store's own `text/feed` that only counts costs what making the text costs
  (run, `logs/sizes.log`: 358 against 359 microseconds for a string of 65,536 bytes, 1,271 against
  1,259 for a map of 520 small maps). The second making is 0.1 microseconds for a small value and 0.36
  to 1.3 ms for one near the bound.
- *B. The text is made once, where it is counted, and carried to the row.* Chosen. It is what ruling 3
  asks: "the plan says how the text of a value is not made more often than it must be." It costs no
  byte more in the microbatch gate, where the act travels from the task where it arrived to the task
  that decides it: the one form carries the text **in the place of** the value, as it carries the
  sealed bytes in the place of the value today. And it is the smaller change to `gate/decision.clj`
  and `gate/batch.clj`: where they take a value's bytes from what the check of the seals gave, they
  take the fact's `:text`.

The step is last because it is the costly one, as the seal's check is last today ("The seal comes
last: it is the most costly", the store's page). A thing with 100,000 facts is refused
`:too-many-facts` before one text is made. What moves with it: of a thing with two faults, a value
over the bound and one that a rule names, the rule's word is the recorded one; today
`:sealed-value-too-large` stands before the three last rules.

If the build session rules for A, five forms differ and no other: `check*`, the shapes, `fact-row`,
`accept-writes` and the proofs' helper that makes a fact with a value. Open point H3.

### D2. The one greatest depth of a permission

**32, on both roads.** An act's own permission can hold 32 permission ids, itself counted, and so can
a permission that is given. One deeper is refused `:malformed` on either road.

**Where it is held.** `nesting` in `act.clj` (`:199-202`, read): `{:permission-id max-depth}` where it
has `{:permission-id max-permission-depth}`. `max-permission-depth` goes as a number of its own (C1;
ruling 16). The engine gives a named shape that has no number in `nesting` the number 1
(`act.clj:397`), so the entry stays and its number changes.

**The three numbers that bound a depth, each with exactly what it counts.** "Depth" is how many
permission ids one permission id holds, itself counted: a first permission has depth 1. A permission
of depth d is d vectors, one inside the other.

| The number | What it counts, and from what | Off by one? | The depth that it allows |
|---|---|---|---|
| The shape's number for `:permission-id`, in `nesting`: 32 after this part | How many times the named shape is around a place, the place's own counted. The engine adds one each time it enters the shape and refuses when the count is over the number (`act.clj:396-399`) | No. The count starts at 1 for the outermost permission, so the number is the depth | 32. Held on both roads: on an act's own `:permission`, a part of the act; and on the value of a fact under `:softland.store/permission`, which the rule `:permission-fact-holds-a-permission` hands to the same engine with a count that starts anew (`door/declaration.clj:398-409`) |
| `act/max-depth`, 32 | How many collections are around a part of a value. `value?` refuses a **collection** that has 32 or more around it, and takes a scalar at any depth that it reaches (`act.clj:274-290`). The count starts at 0 for the value itself | Yes, by one, between a collection and a scalar: a scalar can have 32 collections around it, a collection 31. The innermost vector of a permission of depth d has d - 1 around it, so d - 1 must be 31 or less | 32. Held on a fact's `:v` alone, so on a given permission, and never on an act's own (the implied spec, 5.3) |
| `text/max-depth`, 64 | How many collections are around a part in the one text, counted from the thing whose text is taken. A part with **more than** 64 around it is written `#too-deep` (`door/text.clj:198`) | Yes: 64 around is written out, 65 is not | It depends on what the text is of. The ids of the innermost vector of a permission of depth d have d around them in the text of the permission alone, d + 1 in the text of a thing that holds it as the act's own (the act is one more), and d + 3 as a fact's value (the act, its facts, the fact). So the permission is written whole to depth 64 alone, 63 as an act's own, and **61 as a given one** |

Each line of the last column was run (`logs/sizes.log`, S2): `#too-deep` first shows at depth 65, 64
and 62; `value?` is true at 32 and false at 33; and with the shape's number set to 32 for the run, an
own permission of depth 32 passes the act's check and one of 33 is `:malformed`, a give of depth 32
passes the declaration's check and one of 33 is `:malformed`, and a permission of depth 32 with the
fifth slot is a value and has the shape.

**The two roads agree** because the first two numbers are one number: `nesting` reads `max-depth`.
**Every bound stays below the text's** (C3): the least depth at which a text loses a part is 62, in the
digest of a thing with a given permission, and the bound is 32.

**The words at the bound.** An act whose own permission has depth 33 is `:malformed`, by the shape. A
give of depth 33 is `:malformed` too, by the leaf `:value`, which reads `:v` in the shape's pass before
any rule. So `:permission-fact-not-a-permission` is never given for depth after this part; it stays
for a value that is no permission.

**What a deep permission costs before any check refuses it.** The engine stops at depth 33 and the
digest at 65, whatever the thing holds: an act whose own permission has depth 100,000 is refused in 32
microseconds (run, S2). Rama's writing and reading of the thing come before both (C, last bullet).

**What a permission of depth 32 costs each row written under it** (run, S2 and S3). Its one text is a
field of each fact's row, and again of each row of the facts by value (`record/rows.clj:299-300`,
`:504-505`, read).

| | Depth 4 | Depth 32 |
|---|---|---|
| The text of the permission, with ids like UUIDs | 547 bytes | 4,383 bytes: 137 for each depth |
| The same, with ids of 256 characters | 3,091 bytes | 24,735 bytes: 773 for each depth |
| Who and permission together, in each row, UUID ids | 591 | 4,427 |
| The same, ids of 256 characters | 3,347 | 24,991 |
| The time of making that text once | 1 and 11 microseconds | 8 and 83 microseconds |

And a gate reads none of the permissions above an act's own, at any depth (the contract's "Not here";
the store's page, lines 505 to 508, whose "up to 3 reads" becomes "up to 31").

**Not decided, and where the other answer is made** (R10; ruling 20). How deep a permission can be is
Sid's, and he has not answered it. Kept: as deep as a value may be. The one place is `nesting` in
`act.clj`, and its docstring says "Not decided". A smaller answer is a change of that one number: both
roads then end at it, an act's own with `:malformed` and a given one with
`:permission-fact-not-a-permission`. A greater one needs `max-depth` too, which is a kept number of 6
October, and must stay under 62.

### D3. The bound on the size of an act

**What it counts.** The bytes of text that an accept of the act writes into the rows of its facts: for
each fact, the UTF-8 bytes of its value's text, none for a retract, and the UTF-8 bytes of the one
text of the act's `:who` and of its permission, which each fact's row holds. The text of a value under
a store key is counted like any other: the step makes it too, and the fact's row takes it.

**Its number.** `max-act-bytes`, 18,874,368, which is 18 times 2^20, and is `max-facts` times
(`max-value-bytes` + 8,192): 256 values at the bound of one value, and 8,192 bytes of who and
permission for each of 256 facts. **Its word.** `:act-too-large`.

**Why this count.** C2 names it: "A permission's whole chain is written into each row under it
(finding 14), which the bound can count." It is what the limit of four bounded: with ids of 256
characters, who and permission at depth 4 are 3,347 bytes in each row and 856,832 in the rows of 256
facts; at depth 32 they are 24,991 and 6,397,696. And with the texts of D1 in hand it costs no walk
(ruling 17): each value's count is taken as its text is made, and who and permission are two short
texts.

**Why this number.** Three things were asked of it.

- *It refuses no act that passes today.* The largest act that today's limits take writes 17,634,048
  bytes of such text (run, S4: 256 values of 65,536 bytes, and who and permission of depth 4 with ids
  of 256 characters). R11: where the builder must choose, the default keeps.
- *It refuses no act whose ids are no longer than a UUID's, at any depth to 32.* Who and permission at
  depth 32 with UUID ids are 4,427 bytes in each row; an act of 256 values at the bound under them
  writes 17,910,528. So "a permission can be deeper than four" holds for a full act, not only for a
  small one.
- *It still binds.* An act of 256 facts under a permission of depth 32 with ids of 256 characters
  writes 6,397,696 bytes of who and permission, so its values can average 48,737 bytes and no more;
  without the bound the largest act would write 23,174,912 (run, S4).

**What it costs a gate to hold.** One sum, in the step that makes the texts; and the texts of the
act's who and of its permission made once there. Today those two texts are made anew for each fact's
row and for each row of the facts by value (`record/rows.clj:299-300`, `:504-505`); after this part
`accept-writes` makes them once for an act and hands them to each row. So for an accepted act the
permission's text is made three times, at the bound, at check (d) and at the rows, whatever its facts,
where today it is made once and then once or twice for each fact: 3 ms less for an act of 256 facts at
depth 4, and 22 ms less at depth 32 (run, S4).

**What the largest act that passes then writes.** 18,874,368 bytes of text into the rows of its facts
at most. Into the rows of the facts by value, at most the who and the permission once more for each
fact under a marked key: the bound does not count those, because which keys are marked is read only
after every check. And its record on the log: a thing of 16.8 MB of values was appended to both shapes
of depot and read by both kinds of topology in under half a second (run, `logs/deep.log`).

**Where it is held.** In the same last step of the declaration's check, after the values: so an act
over it is in the log whole, is refused by a gate with a plain word, and costs that gate no row of its
standing. An act over it and a value over D1 gives `:value-too-large`, the first of the two.

**What the proofs can reach.** An act over this bound holds 12.5 MB of values or more. The pure proof
holds the bound at its edge with such an act. The module's proof sends one through each depot; the
run above says that the in-process cluster takes it.

Two other counts were weighed; each is an answer that the build session can rule for. Open point H4.

### The numbers of the build after this part

| Number | Where | Before | After |
|---|---|---|---|
| The most bytes of one value | `door/declaration.clj` | `max-sealed-bytes`, 65,536, of the sealed bytes: 65,508 plain | `max-value-bytes`, 65,536, of the text |
| The most locks of one request | `door/declaration.clj` | `max-locks-in-request`, 256 | Gone |
| The depth of a permission | `act.clj` | `max-permission-depth`, 4 | Gone as a number of its own: `max-depth`, 32 |
| The most bytes of one act | `door/declaration.clj` | None | `max-act-bytes`, 18,874,368 |
| The door's numbers | `door/stock.clj` | Five, and the longest wait | One, `:read-every-ms` 200, and the longest wait, in `door/wait.clj` |

Unchanged: 256 facts, 1,024 entries of `:based-on`, 256 characters of an id, 64 layers built on, 32
marked keys, 256 subjects, 32 collections around a value, 64 in the one text, a carried stamp below
2^62.

## B. What goes, what changes, what stays: file by file, form by form

"Gone" is the whole form. "Changed" says what the form takes and gives after. A form that is not named
is untouched. Line numbers are of head `6d9a6a8d`. A docstring that becomes untrue is in section F;
here a form is "changed" when its code changes.

### The source

**`door/seal.clj`: gone whole** (177 lines). **`door/stock.clj`: gone whole** (384 lines), but two
numbers, which get a home below.

**`door/wait.clj`: new, pure.** The numbers of a maker's wait for an answer (J22; ruling 2).

| Form | What it is |
|---|---|
| `longest-wait-ms` | As today in `door/stock.clj:84-88`: the longest wait that the door counts. A constant |
| `default-numbers` | `{:read-every-ms 200}`: the sleep between two reads of a wait, with today's reason |
| `numbers` | Takes what an opener gave, a map or nil. Gives the door's numbers: the default, with the opener's `:read-every-ms` when it is a fixed-width integer of 1 or more, cut to `longest-wait-ms`. Any other name in the opener's map is not read |

**`act.clj`.**

| Form | After |
|---|---|
| `max-permission-depth` (`:117-119`) | Gone |
| `nesting` (`:199-202`) | Changed: `{:permission-id max-depth}`. Its docstring says what the number is, that it is the value's own on purpose so that the two roads agree, and "Not decided" (D2) |

**`door/text.clj`.** No code changes. One function may come, private or public as the builder finds
right: the count of the UTF-8 bytes of a String with no array made, for D1's count. **`door/digest.clj`.**
`digest`, `hex-of`, `is-digest-of?` untouched. One function comes if H2 is ruled as proposed:
`readable-hex-of`, which takes anything and gives the digest as hex, or nil for a thing that throws
while it is read.

**`door/declaration.clj`.**

| Form | After |
|---|---|
| `max-sealed-bytes` (`:76-79`) | Changed to `max-value-bytes`, 65,536: the most UTF-8 bytes of the one text of a value under a plain key |
| `max-locks-in-request` (`:81-84`) | Gone |
| `max-act-bytes` | New: 18,874,368 (D3) |
| `store-keys` (`:119-197`) | Changed: the entry `:softland.store/lock-request` goes. Five keys stay |
| `shapes` (`:227-274`) | Changed: `:arrived-fact`, `:lock-id` and `:lock-count` go; `:arrived-act` holds `:act-id`, `:depot` and the maker's ten parts as `act.clj` declares them. Five shapes of the door's own stay: `:arrived-act`, `:depot-word`, `:who-may-see-word`, `:layer-list`, `:key-set` |
| `rules` (`:276-327`) | Changed: ten pairs go, seventeen stay, in today's order. Gone: `:store-key-fact-has-no-seal`, `:lock-request-holds-a-count`, `:lock-request-within-the-limit`, `:lock-request-sits-on-its-act`, `:plain-value-is-sealed`, `:sealed-fact-has-no-plain-value`, `:seal-is-whole`, `:sealed-is-bytes`, `:sealed-within-the-limit`, `:lock-named-once` |
| `reasons` (`:340-350`) | Changed: read from the two declarations as today, with the two words of the last step added by name, `:value-too-large` and `:act-too-large` |
| `ids-of` (`:433-459`) | Changed: the line that reads a lock id's layer goes (`:453`) |
| `rule-checks` (`:461-584`) | Changed: the code of the ten rules goes |
| `without-sealed`, `with-sealed` (`:626-647`) | Gone |
| `with-texts` | New, private: the last step. Takes the one form that the engine gave. Gives `{:ok act}`, each fact with its `:text` (section C), or `{:refuse :value-too-large}`, or `{:refuse :act-too-large}`. It makes the text of each value once, and of the act's who and permission once |
| `check*`, `check` (`:649-684`) | Changed: the shape, then the rules, then `with-texts`. `check` still never throws |

`stray-reason`, `store-key?`, `id-text`, `id-within-the-limit?`, `usable-id?`, `act-id-in-layer?`,
`setting-keys`, `second-write-keys`, `max-facts`, `max-based-on`, `max-id-text`, `max-builds-on`,
`max-marked-keys`: untouched.

**`door/additions.clj`.** The door's additions are two.

| Form | After |
|---|---|
| `most-values-in-one-act` | Gone |
| `parts-read`, `read-parts` (`:106-126`) | Changed: the door reads one part, `:layer`. A read that throws gives `:read-the-act` at `:layer` |
| `own-parts-word`, `act-id-word`, `plain-map?`, `series-word` | Untouched |
| `entry-of`, `walk`, `parts-carried`, `carried-of` (`:153-293`) | Gone |
| `reading*`, `reading` (`:295-344`) | Changed. Takes a maker's thing and the act-id that she gives back, or nil. Gives `{:thing :layer}`, or a word. Three tests, ruling 1's: a plain map whose `:layer` can be read (`:read-the-act`); no `:act-id` and no `:depot` already (`:add-its-parts`); a layer that an act-id can hold, and an act-id given that is of that layer (`:make-an-act-id`). It reads no fact, no who, no permission (R4) |
| `facts-to-append` (`:348-385`) | Gone |
| `thing-to-append` (`:387-412`) | Changed. Takes a reading and an act-id. Gives `{:thing}`: the maker's map with `:act-id` and `:depot` added. It takes no locks and can end in no word of a seal |
| `request` (`:416-442`) | Gone |

**`door/words.clj`.**

| Form | After |
|---|---|
| `words` (`:43-121`) | Changed: seven words stay, `:find-the-module`, `:make-an-act-id`, `:read-the-layers-depot`, `:read-the-act`, `:add-its-parts`, `:append-cleanly`, `:read-the-answer`. `:add-its-parts` says only `:act-id` or `:depot` on the act. `:read-the-act` also says a thing that threw while the door took its digest, if H2 is ruled as proposed |
| `carried` (`:123-138`) | Changed: `:most`, `:sealed-bytes`, `:request`, `:request-answer` and `:waited-ms` go. `:at` names a part of the act, or `:count` |
| `word`, `answers`, `answer`, `acks` | Untouched |

**`door.clj`.**

| Form | After |
|---|---|
| The type `Door` (`:147-171`) | Changed: six fields, the module's name, the two depots, the two queries, the numbers. No `locks-query`, no `stocks` |
| `read-locks` (`:237-252`) | Gone |
| `read-answer-until` (`:310-351`) | Changed: takes the door, an act-id, the moment at which the wait ends, and whether to sleep first. Gives `[:answer …]`, `[:threw …]` or `[:nothing]`. The branch of a request in doubt and its outcome `[:given-up]` go |
| `enter!`, `leave!`, `take!`, `waited-word`, `settle`, `ask`, `in-the-seat`, `locks-for` (`:355-685`) | Gone |
| `open` (`:689-718`) | Changed: four fetches, two depots and two queries; the numbers through `wait/numbers`; no stock |
| `send-act` (`:770-874`) | Changed. The steps: (1) `additions/reading`; (2) the act-id, given or made with one read of the layer's depot; (3) the thing built, and its digest; (4) one append. It gives `{:act-id :digest :ack}` or one of five words: `:read-the-act`, `:add-its-parts`, `:make-an-act-id`, `:read-the-layers-depot`, `:append-cleanly` |
| `answer-of` (`:876-940`) | Changed in one name: `wait/longest-wait-ms` |
| `stock-count` (`:942-954`) | Gone |
| `printed`, `what-threw`, `attempt`, `append!`, `read-answer`, `depot-of`, `after`, `left`, `passed?`, `sleep-for`, `act-id-for`, `act-ids-for` | Untouched |

The requires of `door.stock` and of `java.util.concurrent` go; `door.wait` comes.

**`gate/decision.clj`.**

| Form | After |
|---|---|
| `lock-in-this-record?`, `lock-ranges`, `opened-fact`, `opened` (`:386-467`) | Gone |
| `accept-writes` (`:534-569`) | Changed. `in-hand` holds `:since-by-place`, `:place-rows`, `:level-from`. For each fact: its row; for a fact under a marked key the rows of `by-value-writes`. No `:lock` row and no `:new-lock` key. It makes the text of the act's who and of its permission once and hands both to each row |
| `writes` (`:571-621`) | Changed in what it gives: `[:fact …]`, `[:by-value …]`, `[:answer …]`, `[:last-stamp …]`. Its six arguments stay |
| `refused-early`, `permission-place`, `depot-place`, `makes-the-layer?`, `standing`, `places-needed`, `places`, the five faults, `marked-keys-place`, `reads-marked-keys?`, `since-by-place`, `rows-before-needed`, `stamp-of`, `level-since-after`, `by-value-writes` | Untouched in code |

The require of `door.seal` goes.

**`gate/batch.clj`.**

| Form | After |
|---|---|
| `lock-in-this-record?`, `lock-indexes-by-request` (`:244-260`) | Gone |
| `questions` (`:297-349`) | Changed: two kinds, `:answer` and `:place`. The kinds `:locks` and `:new-locks` go (`:340-349`) |
| `marks-places-asked` (`:522-538`) | Changed: the line that leaves a store key's place out goes (`:534`). After this part no fact under a store key asks about a place: each of the five keys is on every task and asks nothing |
| `reads-of` (`:540-572`) | Changed: no read of the kind `:locks` (`:570-572`) |
| `lock-replies` (`:624-655`) | Gone |
| `replies` (`:657-681`) | Changed: two cases, `:answer` and `:place` |
| `with-replies` (`:685-710`) | Changed: no `:lock-rows` |
| `routed` (`:781-830`) | Changed: three kinds of row, `:fact`, `:by-value`, `:answer`. `:lock`, `:lock-home` and `:new-lock` go |
| `decided` (`:832-865`) | Changed: takes what is known of the act, the two rows of its standing, whether the other gate's record holds the layer, and the rows of its places. No lock rows, and no call of `opened` |
| `decide-or-send` (`:897-960`) | Changed: a parcel of the kind `:meet` carries no lock rows; the call of `decided` |
| `rows-in-write-order` (`:964-972`) | Gone. It put each home's row of a lock before any other row. No order is needed between the rows that a task is sent: each key of a batch has one sender, or all its senders are in one pass, which sends one row for a key, the last (`rows-to-send`). The gate takes `:rows` of what it gathered |
| `new-pass` (`:980-984`) | Changed: `{:facts {} :out []}` |
| `lay-over` (`:1044-1056`) | Changed: it lays fact rows over the pass |
| `decide-next` (`:1058-1100`) | Changed: takes the pass, the act as it was sent, the rows that the meeting task read, and the other gate's depot row. No drawn lock row |
| `rows-to-send` (`:1102-1116`) | Changed: one row for each kind and key, the last. No home's row first |
| `who-meets` | Untouched in code; its third rule names "a place" where it named "a place or a lock" |

**`record/rows.clj`.**

| Form | After |
|---|---|
| `lock-key` (`:188-197`) | Gone |
| `held-value` (`:224-259`) | Gone: the making of a row's String from bytes (ruling 5) |
| `plain-bytes` (`:261-271`) | Changed: the UTF-8 of the String. The branch for a String that begins with U+0000 goes with the form that it read back; such a String is then bytes that read as no value, which is what a reader is told of it today |
| `fact-row` (`:281-305`) | Changed. Takes a fact in its one form on arrival, the uuid of its act, its index, the stamp, and the texts of the act's who and of its permission. Gives the row, with `:value` the fact's `:text` when it has one. One branch where it had two |
| `by-value-row` (`:496-505`) | Changed: takes the two texts in the place of the act |
| `holds-value?` (`:331-341`) | Changed: compares the row's String with the one text of the value given |
| `read-value` (`:362-393`) | Untouched in code. Its four answers stay (ruling 5); its docstring says that no write of a gate gives a row that earns the fourth |
| `lock-row`, `lock-row-sealing`, `lock-row-without-bytes`, `seals-a-fact?`, `lock-of`, `locks-by-index`, `locks-by-index-used-without-bytes`, `lock-range-read`, `lock-range-read-by-routing-id` | Gone |
| Every other form | Untouched |

**`record/tables.clj`.** `locks` gone (`:164-170`). **`module.clj`.** No code changes.
**`module/streaming_gate.clj`, `module/microbatch_gate.clj`.** As "Topologies and PStates" has them:
`new-lock-row` and `rows-as-map` gone from both, `lock-rows-in` gone, `write-row` changed to three
PStates and three kinds. **`module/door_reads.clj`.** The query `locks-of-request` gone. **The read
path.** No code changes.

**R6.** No new code takes the depots of acts to be two. Three of the 36 places of the implied spec's
section 2 go with this part (17, 23, 24), and none is added: `door/wait.clj` names no depot, and
`with-texts` is one function for any gate.

### The proofs

The counts of forms and lines are **gathered**. What each check holds after, and what in it could
pass for a wrong reason, are this session's, reasoned from what was gathered.

**Four patterns to look for** (finding 16; the implied spec, 5.1, its last paragraph), each with where
it was found:

- (a) *Two digests held to be unequal, where the seal's random part was the reason.* The door's proof,
  `the-answer-back`, line 871; the pure proof, `digest-cases` line 341 and group 15, line 4027.
- (b) *Nil or an empty map read from a thing that is gone, and taken for "nothing was made".* The
  module's proof at nine places that read locks (750, 1039, 1044, 1132, 1144, 1362, 1634, 2130,
  2337); the door's proof wherever it holds `stock-count` or the count of requests to be unchanged.
- (c) *A row's value held against a String or bytes that the proof makes by the store's own function
  from the same value.* The module's proof at 802, 1187, 1569, 1984 and 2475, through its helper
  `plain-bytes`; the pure proof at 1045, 1513 and 1592.
- (d) *A refusal expected with no word, where the thing is now refused earlier.* The pure proof, lines
  3902 and 3788; the module's proof, line 2334. Every other refusal that was gathered names its word.

**The act's proof** (`act_proof.clj`, 8 checks). For C only: 3 checks are written again, 5 are
untouched, none goes. Its two forms that name a seal (`parts-cases` 181-185, `refusal-rows` 348-349)
say that the maker's form refuses a fact with a seal or a lock id as `:unknown-part`; they stay true
and stay.

| Check, by the map's group | Lines | After |
|---|---|---|
| 1, the parts: the version and the limits | 190-192, and the SHOWN line 534 | Written again: four numbers where it had five, `[1 256 32 2^62]`. And that the shape's number for a permission is `max-depth` |
| 6, the limits at their edges | 435-440 | Written again: a permission of depth 32 is `:ok` and of 33 `:malformed`, each with the fifth slot and without it. Depth 5, which was `:malformed`, is `:ok` |
| 7, ids and values: the shapes | 505-509 | Written again: depth 5 and depth 32 have the shape `:permission-id`; depth 33 has not |

Wrong reason to look for: a case that still builds depth 5 and expects `:malformed` fails loudly, so
none passes in silence. The helper `deep-permission` (134-138) builds any depth.

**The pure proof** (`pure_proof.clj`, 18 checks). 2 go, 15 are written again, 1 is untouched
(`the-stamp`).

| Check (group) | After | What it holds after, and what could pass for a wrong reason |
|---|---|---|
| `the-one-text-and-the-digest` (1) | Written again | The case "the same act sealed again has another digest" (341) goes. In its place, the opposite, which is now true: the same act built twice has one digest. The two cases that carry sealed bytes as "a thing with bytes" (330, 336) keep a run of bytes as a part that is no value. Its cases at 302-305, the text at 64 and 65 collections, stay as they are |
| `the-declaration` (2) | Written again | Five shapes and seventeen rules written out a second time; seven limits, with `max-value-bytes` and `max-act-bytes` among them and no limit of locks; five store keys. The one form on arrival, with `:text`. "A tool's own fact under a bare word passes as a fact under a plain key" (606-615) is held with a plain value; `:lock-request` as a bare word stays a tool's own key. The case at 615, which holds `:not-sealed` three times for a plain value under a key that looks like a store key, inverts: such a value passes. Left as it is, it fails loudly |
| `refusals-of-the-declaration` (3) | Written again | One refused thing for each word that stays. The rule that each word must come from a thing that earns it (3767-3772) is what keeps this honest: with ten words gone and two come, a table that still held a case for a gone word, or none for a new one, fails. New rows: a fact that carries `:sealed`, and one that carries `:lock-id`, each `:unknown-part` (ruling 4); a fact under `:softland.store/lock-request`, `:store-key-not-known`; a value of 65,537 bytes of text `:value-too-large` and one of 65,536 `:ok`, counted in bytes with a string of three-byte characters; an act at `max-act-bytes` `:ok` and one byte over `:act-too-large`. The rows "each of the three arriving sealed" (674-676) go. A permission fact five deep (651) is `:ok`; one 33 deep is `:malformed` |
| `the-maker's-check-inside-the-door's` (4) | Written again | Its helper `makers-part` (3780-3785) takes `:sealed` and `:lock-id` off and puts `:v 1`: it then has nothing to take off. Wrong reason (d): the expected word is computed by `act/parse`, not written; keep it so, and keep the count of rows at 3790 |
| `the-order-of-the-reasons` (5) | Written again | Eight of its eighteen rows have a rule of the seal or of a request as one of two faults (719-738) and go or change. New rows hold the new order: a value over the bound beside a place named twice gives `:place-named-twice`; a value over the bound beside an act over its bound gives `:value-too-large` |
| `nothing-throws` (6) | Written again | In its hand only: two of its things carry a sealed fact (783, 784) |
| `the-seal` (8) | **Goes** | It held `door/seal.clj`, which is gone |
| `the-first-act` (9) | Written again | In its fixture only: `making` spells `:lock-id nil :sealed nil` on each fact (907-908) and then spells `:text` |
| `the-records-rows` (10) | Written again | Three kinds of row. The cases of a lock's key and rows (1026, 1206, 1213, 1224) go. The cases of the String with two forms, of bytes that are no text, and of each of the 256 bytes (1054, 1063, 1075) go with `held-value`. What stays of them: a fact's row holds the text itself; `plain-bytes-of` gives its UTF-8; a row whose String is no value's text is `[:not-a-value]` to a reader, held on a row that the proof writes by hand. Wrong reason (c): 1045 holds the row's bytes against `(text/utf8 "a text")`, the function that made them. Hold the row's `:value` against the literal `"\"a text\""`, as 1047 does |
| `the-decision` (11) | Written again | `seals-cases` goes whole (15 cases). `writes-cases`: an accept of one value writes three rows, the fact, the answer and the last stamp (1586 held four). Wrong reason (b): seven of its cases pass `{:lock-rows {} :plain [nil …]}` to `decision/writes` (1608 to 1647); those keys are read by nothing after, so a case that still passes them proves nothing of them. `by-value-cases`: the proof's own step of the streaming gate (1658-1690) hands a value's bytes over where the seals' check gave them (1672); it then takes `:text` from the declaration's check. Wrong reason (c): each expected key of a run is made by `record/value-digest-of`; keep at least the cases that hold a digest made by the proof's own SHA-256 of a literal text (1145-1151) |
| `the-batch` (12) | Written again | The questions, the replies and the homes of rows about locks go (in `arrival-cases` 1 and 3 mixed, `reply-cases` 3 and 2 mixed, `routing-cases` 4, `deciding-cases` 1 and 5 mixed). A reply about a place under a marked key, who goes to the meeting task, and the home of each row stay |
| `a-batch-is-decided-in-one-order` (13) | Written again | The fixture's things that are about locks go: two acts under one lock (acts 10, 11), a request and a use of its lock (12, 13), a value whose lock's home is its own task (24), and the eleven lock rows. Act 17, a plain value that was refused `:not-sealed` (3907), is now accepted: its expected answer inverts. Wrong reason (d): 3902 expects one of two lock words and goes. `differences` compared locks apart (2802-2808) and loses that branch |
| `a-batch-under-marked-keys` (14) | Written again | In its fixture only: eight acts carry a sealed fact, and six fact rows are built from one |
| `a-batch-run-twice-with-one-clock` (15) | Written again | With one clock **no row differs** between the two attempts. Today the check holds that exactly the two new locks differ, in their bytes alone (4027). Wrong reason (b): "no difference" passes for an empty batch too. Hold the count of rows compared beside it, as 4031 does for the stamps |
| `the-doors-words` (16) | Written again | Seven words. Three of its cases use `:not-sealed` as the example of a gate's reason (3105, 3114, 3120) and take another word |
| `the-doors-additions` (17) | Written again | See below |
| `the-doors-stock` (18) | **Goes** | Its cases of the two numbers that are no lock's are written again in a new check, `the-doors-wait` |

`the-doors-additions`, group 17, after. Things that end in one of the door's words: no plain map
(`:read-the-act`); a sorted map whose keys cannot be compared with a keyword (`:read-the-act` at
`:layer`); no layer, a layer that is no id, a layer of 257 characters, an act-id of another layer
(`:make-an-act-id`); `:act-id` or `:depot` already there (`:add-its-parts`). Everything else that was
a door's word goes in, and the declaration's check has the gate's word for it: a value that is no
value, and a fact that is a record with a value, `:malformed`; a fact that holds `:sealed` or
`:lock-id`, `:unknown-part`; 257 facts with values, `:too-many-facts`. New cases:

- the thing that is built holds `:facts` as the very object that the maker gave, for a vector, a list
  and a lazy sequence, and the lazy sequence is not walked by the building;
- each fact is the maker's own object, a fact with a value too;
- the reading comes back for a thing whose facts have no end, for a who that throws when it is hashed
  and for a permission that throws when it is read: the door reads none of them;
- the digest of the thing with its facts as a list is the digest with the same facts as a lazy
  sequence, and is not the digest with them as a vector. Today's case at 3452 holds the three equal,
  because the door makes a vector of each: it inverts, and left as it is it fails loudly.

**The module's proof** (`module_proof.clj`, 33 checks, 354 assertions of the form `check`). 2 checks
go, 23 are written again, 4 change in the hand alone, 4 are untouched. Of the 88 assertions that touch
a lock, 54 go with what they checked and 34 are written again.

*The hand.* One helper turns a plain value into a sealed fact, `seal-with` (308-312), and `sealed` is
that with a lock taken from the hand (314-318); there are 77 calls of the two. After, the helper
gives the maker's fact `{:e :k :v}`, and the hand asks for no lock. `open-hand` loses the query
`locks-of-request` and the two tables of locks; `locks-of`, `ask-for-locks!`, `take-lock!`,
`lock-row`, `ask-for-base-locks!`, `base-lock-key`, `seen-before-closing` and the atoms
`locks-in-hand` and `lock-bytes-read` go. The hand's own `plain-bytes` (302-306) goes too: see (c).
The three functions that the second cluster replaces are `batch/base`, `batch/decide-or-send` and
`batch/rows-to-send`; none is about a lock, and the notes that they keep lose the kinds `:lock`,
`:lock-home` and `:new-lock`.

| Check | Assertions about a lock, of all | After |
|---|---|---|
| `the-path-of-a-lock` | 14 of 14 | **Goes** |
| `the-path-of-a-lock-in-the-base` | 8 of 8 | **Goes** |
| `layers-are-made` | 1 of 14 | Written again: a value act into a layer that is not made is refused `:layer-not-made` and writes no row. Today it is a request, and "makes no lock" is an empty map from the read of locks (750): pattern (b) |
| `an-act-lands` | 6 of 9 | Written again: 5 go. The one that stays (802) holds the row's value as the one text of the value's normal form, and now also that the log holds the value plain, as given |
| `refusals-and-repeats` | 2 of 21 | Written again. "An unsealed value is refused" (862) goes: its opposite is the rule now. Two more assertions rest on that refused act as their example of a recorded refusal and of a repeat (865, 868); they take another refusal, a stale `:replaces`. "Two facts under one lock" (896) goes. Its two assertions of pattern (a), 870 and 874, hold a different thing under a used act-id to be "not mine": they stay, since the things differ |
| `settings-and-permissions` | 2 of 23 | Written again, for A and for C. "A permission five deep is no permission" (960-961) inverts: a give at depth 5 is accepted, and an act under it. New: gives down to depth 32, an act under depth 32 whose row holds the whole permission, a give of depth 33 refused `:malformed`, an act that names an own permission of depth 33 refused `:malformed` |
| `strays-and-limits` | 2 of 16 | Written again: a value whose text is 65,537 bytes is refused `:value-too-large`, one of 65,536 is accepted and its row holds those bytes (1105). The row of zeros that the hand sealed, which a reader is told is no value (1109), goes: no gate's write gives such a row (ruling 5) |
| `the-reads-take-anything` | 2 of 6 | Written again for two reads |
| `what-a-fact-keeps`, and `-in-the-base` | 7 of 11, 5 of 7 | Written again. Each row holds the one text of its value, for a value of each kind; the act in the log is as the hand sent it, each fact with its value plain (1193, 1987). "The lock of each value opens the log's bytes" (1196, 1990) and "a gate still refuses an unsealed value" (1207) go. "An act whose first value opens and whose second does not is refused whole" (1203, 1997) holds whole-or-nothing with a fault that still exists: a second fact that names a stale fact to replace. Wrong reason (c): 1187 and 1984 hold each row's bytes against `plain-bytes`, which is `text/utf8` of `act/normal-value`, the store's own two functions. Hold the row's `:value` against a literal for each kind, as 803 does for one |
| `the-keys-that-take-a-second-write`, and `-in-the-base` | 4 of 15, 1 of 10 | Written again: the word `:store-key-sealed` leaves each list of expected words (1279, 2073) |
| `what-a-permission-allows`, and `-in-the-base` | 1 of 10, 1 of 7 | Written again: a holder of see sends a value act, and it is refused `:permission-does-not-allow-acting` under its own act-id with no row. Today it is her request, and "makes no lock" is an empty map (1362, 2130): pattern (b) |
| `an-act-lands-in-the-base` | 8 of 11 | Written again: 7 go, 1 stays as in `an-act-lands` |
| `an-answer-is-readable-when-its-batch-is-closed` | 3 of 4 | Written again with a maker's act in the place of a request: the answer's row is in the table, and the read by act-id gives nothing until the batch is closed |
| `refusals-and-repeats-in-the-base`, `one-batch-one-order`, `a-person-acts-in-the-base` | 1 of 11, 1 of 9, 1 of 15 | Written again: the unsealed value (1669) and the two acts under one lock (1738) go |
| `an-act-on-several-tasks` | 6 of 15 | Written again: its six assertions of lock rows go. That the act is refused whole and accepted whole, and has one stamp, stays |
| `after-the-module-is-started-again`, `the-base-after-…` | 2 of 7, 3 of 8 | Written again: the record is as it was, with no locks among it. "A lock that was asked for before seals a value after" (2412, 2472) goes |
| `the-stamps-and-the-depot`, and `-of-the-base` | 1 of 5, 2 of 10 | Written again: "no lock's bytes are on the depot" (2437, 2514) goes. That the depot holds every thing sent, as it was sent, stays, and now says that each value is plain there |
| `a-batch-runs-a-second-time` | 4 of 13 | Written again. The batch holds a plain value that was there to be refused `:not-sealed` (2825, word 11 of 2908): it is accepted now, so the list of expected words changes. The rows of the two attempts side by side are equal with no lock rows among them (2906) |
| `what-points-at-a-thing`, `-in-the-base`, `one-batch-under-a-marked-key`, `a-place-of-the-base-never-gives-nothing` | None | In the hand alone: they send values |
| `what-rama-carries`, `the-base-is-made`, `a-long-batch`, `each-gate-tells-the-other-gates-layers` | None | Untouched |

**The door's proof** (`door_proof.clj`, 27 checks, 175 assertions). 7 go. 5 go as checks of a lock,
and the thing that each also held is written again under a name of its own. 13 are written again in
place. 2 are untouched.

*Its hands.* `open-round` loses the query of locks and the two tables; `request?`, `requests-of`,
`asked`, `locks-round`, `lock-rows`, `key-of-lock`, `lock-ids-of`, `locks-asked-in`, `stocks-of`,
`stock-told`, `opened`, `without-the-random-parts`, `plain-bytes` and the atom `outside-the-stocks`
go. `counting`, which counts each append and each query, stays, and is what holds A1: after this part
every send is one append and at most one query.

| Check | After |
|---|---|
| `a-request-in-doubt`, `the-seat-and-the-wait`, `a-requests-answer-with-another-digest`, `an-act-over-three-runs`, `a-lock-never-crosses-a-who`, `a-wait-for-locks-that-passes`, `a-request-in-doubt-in-the-base` | **Go.** Each held a request, a stock or a seat and nothing else |
| `locks-and-the-seal` | Goes as a check of a lock. Written again as **`a-value-is-in-the-log-as-given`** (new; the implied spec's section 9, first item): the record on the depot holds the value as the maker gave it, `{:b 1.50M :a [1 2N]}` and not its normal form; the row holds the text of the normal form, `{:a [1 2] :b 1.5M}`; the digest of the record is the door's; an act with a store-key fact, a retract and a value is accepted |
| `a-refused-request` | Goes as a check of a request. Written again as **`a-maker-who-may-not-act`** (new; entry 7): a value act under a permission that was never given, and one into a layer that is not made, are each appended, refused under their own act-id with the gate's word, and leave no row; the log holds each value plain. Today the same sends end in the door's word, with nothing under the act's act-id (916) |
| `a-who-that-cannot-be-a-key` | Goes. Its three sends become cases of `things-that-are-no-act` |
| `the-doors-memory-starts-again` | Goes. Written again as **`the-door-holds-nothing`** (new; ruling 2): a second door, opened on the module, reads the answer of an act that the first door sent, by the act-id and the digest alone |
| `no-locks-bytes-anywhere` | Goes. Two of its nine assertions stay, in a check of their own: each of the door's seven words was given in the run (1790), and the door left no stray (1789) |
| `the-door-is-opened` | Written again: four handles; the assertion of `stock-count` (545) goes; the second door is opened with `{:read-every-ms …}` |
| `round-trips-are-counted` | Written again: a first value send with no act-id is one append and one query, where it was two and three (603). The other counts stand. Wrong reason: none, the numbers are written out |
| `the-door-and-the-hand-send-the-same-thing` | Written again: the door's record is equal to the hand's but for the uuid of the act-id. Today three things are held to differ (719-723), the act-ids, the request ids and the sealed bytes; one is left. Pattern (a) in small: keep the assertion that the two act-ids differ, or the equality could hold of one record compared with itself |
| `the-digest-is-the-gates` | Written again: facts given as a vector, a list and a lazy sequence are each accepted under the door's digest, and the log holds a vector, a list and a sequence. Today it holds `(vector? (:facts r))` for all three (741): that inverts |
| `things-that-are-no-act` | Written again. Of its fourteen kinds that end in a word, eight stay (a thing that is no map; no layer; a sorted map that cannot be read; a record; a layer that is no id; a layer that is too long; `:act-id` or `:depot` already there; an act-id of another layer). Four go in and are refused by a gate: a fact that is a record with a value, a value fact that holds `:sealed`, a value that is a ratio, a list or NaN, and 300 value facts. One changes its edge: a value of 65,507 characters, which the door stopped (777), is accepted, and the edge is a gate's, at 65,536 bytes of text. **One must go: facts with no end** (765); after this part that send does not come back, and a check of it would hang the proof. Its assertion of the count of things in the log (812-814) subtracts the requests and takes another number |
| `the-answer-back` | Written again at 871: a maker's own second send of one act has the same digest, and both sends read the answer as their own (ruling 6). Pattern (a): today it holds the two digests to be unequal |
| `a-holder-of-see`, `an-act-id-with-the-other-depot-word` | Written again: the value act goes in and is refused under its own act-id, as the act with no value beside it is today (960, 1652). Today it stops at the request (951, 1655) |
| `reads-that-throw` | Written again for the two reads that stay (1097-1102); its nine assertions of a request's reads go |
| `the-openers-numbers` | Written again: the opener's one number; a wait of 250 ms with a read each 100 ms makes 4 queries (1317) |
| `many-makers-at-once` | Written again: 400 acts on 16 threads are each accepted. Its five assertions of lock ids and of the size rule go |
| `the-base-through-the-door` | Written again: a value act into the base is one record more in its log, where it was two (1528), and its calls are one query and one append (1534). The wait of a request's first read (1537) goes |
| `the-module-is-started-again` | Written again: an answer from before reads the same after, and a value act after is accepted |
| `an-act-with-no-value`, `an-append-that-threw` | Untouched |

*The checks that rest on a time.* Six are named in the map's section of that name (the proofs' map,
lines 902 to 924; gathered). Four rest on a request for locks or on a stock's seat and go with their
checks (groups 10, 12, 20, 22). Two rest on the wait for an answer and stay: a wait of 600 ms with a
read each 200 ms makes 4 queries (group 3), and a wait of 250 ms with a read each 100 ms makes 4
(group 15). So the risk that the split names, "the checks of the door's proof that rest on a time,
once no send meets a request", comes down to those two, which no send's time enters.

**The read path's pure proof** (`read_path_pure_proof.clj`). The implied spec has it as "none": it
names no lock. But it calls two forms that this part changes, and 3 of its checks are written again
in a line or two each.

- Its helper `fact-row*` (114-126) builds every row through `rows/fact-row` with six arguments, and
  hands a value's bytes in where "the plain bytes that the fact's lock opened" went. It takes the new
  arguments.
- `the-checks-of-nearer-layers` (997) and `what-a-read-gives` (1007, 1018) make a row whose value is
  no value of the form with `rows/held-value`. They write that String by hand. This is where the
  reader's fourth answer, `:not-a-value`, stays proven (ruling 5).
- `the-size-of-a-run-and-the-order-of-its-keys` (1172-1173, 1207) makes two equal Strings with
  `held-value` and holds them to be not identical. Written as two literals they would be one object,
  and the assertion would fail: the proof must build two Strings.

**The read path's proof** (`read_path_proof.clj`, 27 checks). None goes. 3 are written again. Its
hand `open-round!` loses its handle of `locks-of-request` (159), and the require of the seal.

| Check | After |
|---|---|
| `the-four-things-that-a-place-can-hold` (532-580) | Written again as three things through a gate: no fact, nothing here, a value. The fourth, bytes that are no value, was made twice by a seal of the proof's own hand and three appends round the door (539-563); no gate's write makes it now. It stays proven in the pure proof, above |
| `the-stores-own-facts` (617-619) | Written again: the set of the store's keys in a run that asks for them has no `:softland.store/lock-request` |
| `a-batch-that-runs-a-second-time` (1716) | Written again: its first send is there "so that the system's stock holds locks" and goes. Its numbers of throws and of attempts (1747) were shaped by which sends met a request; the build derives them anew |

Wrong reason to look for, in a check that this plan does not change: `a-very-large-run` holds a count
of selects for a run across a whole layer (862), and that layer held the rows of the door's requests;
the count may move with them.

**The measures** (`read_measures.clj`). No code changes: it names no lock. What changes is what its
times hold: about one send in 64 met an empty stock inside the timed call (gathered, from the size
rule), and none does after. That is for D, which is not this part's.

### The new checks

Each is asked of this part by the implied spec's section 9, by a ruling, or by an edge of 6.1, 6.2 or
6.7. No new proof file.

| # | Check | Where | What it shows |
|---|---|---|---|
| 1 | `a-value-is-in-the-log-as-given` | The door's proof | Section 9, item 1 (above) |
| 2 | `no-code-of-the-store-names-a-lock` | The pure proof | Section 9, item 2: it reads each source file of the tree as data and finds no symbol and no keyword whose name holds "lock", "seal" or "stock", but Clojure's `locking` |
| 3 | `the-module-keeps-no-lock` | The module's proof | Section 9, item 2: the cluster gives no table of locks and no query of a request's locks; a fact under `:softland.store/lock-request` is refused `:store-key-not-known`, and a fact that carries `:sealed` or `:lock-id` is refused `:unknown-part`, through each depot |
| 4 | `a-value-of-each-kind-through-each-depot` | The door's proof | Section 9, its last line; the first of the build page's three things. A value of each kind that the form takes, through the door and each depot: accepted under the door's digest, and read back from its row as the value in its normal form. The cases are those of this plan's run |
| 5 | `a-permission-deeper-than-four` | The module's proof, in `settings-and-permissions` | Section 9, item 5; 6.7's edges: a give at each depth from 4 to 33, on each depot; an act under depth 5 and under depth 32; an own permission deeper than any that was given; the fifth slot at depth 32 |
| 6 | The bounds | The pure proof and the module's | D1 and D3 at their edges, and through each depot: a value over its bound, and an act over its bound, are in the log whole and refused with their word |
| 7 | `a-maker-who-may-not-act`, `the-door-holds-nothing` | The door's proof | Entry 7 and ruling 2 (above) |
| 8 | The edges of a value | The module's proof, in `what-a-fact-keeps` | 6.2: `false`, an empty string and an empty map are values and no retract, each with its text in its row; a value inside 32 collections is kept and one inside 33 is `:malformed` |

## E. The words

R8. Each word that leaves, from which list; each word that this part makes; the lists as they stand
after.

### Words of the locks and of the seal that leave

**From the store's page, "Words a session made, Sid's to change"** (read, lines 843 to 1105).

| Where in that list | Words that leave |
|---|---|
| Carried from the contract and the decisions (846-848) | `:sealed`, `:lock-id`, `:not-sealed`; and of "the three store keys of 8 October", `:softland.store/lock-request`: two stay |
| The paragraph of the store keys (856-868) | "`:lock-request`" as a bare word that is the tools', and "its value arrives sealed" |
| The words of the read build, for what the gates keep (891-895) | `opened`; `held-value`. `plain-bytes`, `plain-bytes-of`, `holds-value?`, `value-of`, `read-value` and its four answers stay |
| Names in Rama (926, 933, 940) | `$$streaming-locks`, `$$microbatch-locks`, `locks-of-request` |
| Folders and namespaces (943-958) | `door.seal`, `door.stock` |
| Fields of a row (971-974) | `:bytes`, `:way`, `:aes-256-gcm`, `:seals-act`, `:seals-index` |
| The reason words of the declaration (996-1021) | `:lock-id-malformed`, `:store-key-sealed`, `:lock-request-not-a-count`, `:too-many-locks-requested`, `:lock-request-not-on-its-act`, `:sealed-with-a-plain-value`, `:seal-incomplete`, `:sealed-not-bytes`, `:sealed-value-too-large`, `:lock-named-twice` |
| The names in the declaration (1023-1040) | The shapes `:arrived-fact`, `:lock-id`, `:lock-count`; the ten rules that section B names; ":the-uuid-of-its-own-act", one of the three words that say where a fact sits |
| The reason words of the gate (1061-1064) | `:lock-not-made-for-this-layer`, `:lock-already-seals-a-value`, `:lock-way-not-known`, `:lock-does-not-open-the-value` |
| Words between the decision and the gate (1066-1070) | `:lock`, `:new-lock`, `:lock-rows`, `:plain` |
| The words of a batch (1086-1102) | The two kinds of question `:locks` and `:new-locks`; `:lock-home`; "the home's row of a lock"; in "route", "the request's uuid of a lock"; in "reply", "or a lock" |

**From the door's page, `door/README.md`, "Words a session made, Sid's to change"** (read, lines 228
to 295).

| Where in that list | Words that leave |
|---|---|
| Carried (232-234) | `:sealed`, `:lock-id` |
| Namespaces (236-239) | `softland.store.door.stock`. "Additions" stays, for two things where it was three |
| Calls (243-249) | `stock-count`, with `:unused-locks`, `:stocks`, `:open-requests`; `sealed-length` |
| The door's five numbers (251-259) | `:request-wait-ms`, `:fewest-at-once`, `:most-at-once-streaming`, `:most-at-once-microbatch` |
| What a call gives (261-272) | `:most`, `:sealed-bytes`, `:waited-ms`, `:request`, `:request-answer` |
| The door's fourteen words (274-280) | `:seal-a-value`, `:seal-that-many-values`, `:have-its-request-accepted`, `:append-its-request-cleanly`, `:get-locks-within-its-wait`, `:read-its-requests-answer`, `:read-its-requests-locks` |
| Words of the stock (282-295) | Every row: a stock, a run and `:runs`, an open request and `:open`, in doubt and `:since`, the seat, given out and `:given-out`, `:calls` and `:cell`, `:lock`, value fact; and of a reading, `:carried`, `:entries`, `:values` |

**From the read path's page.** None. Its line "never called a lock: a lock is an encryption key in
this tree" stays: it is the tree's rule for the word (`src-store/CLAUDE.md`, "Words").

### Words that this part makes

Each is plain and says what the thing is. None was taken from the rig's code, which this session did
not open; whether the rig's code holds one of them by chance is not known.

| Word | What it names |
|---|---|
| `max-value-bytes` | In `door/declaration.clj`: the most UTF-8 bytes of the one text of a value under a plain key. 65,536 |
| `:value-too-large` | A reason word of the declaration: a value under a plain key whose text is over that |
| `max-act-bytes` | In `door/declaration.clj`: the most bytes of text that an accept of one act writes into the rows of its facts. 18,874,368 |
| `:act-too-large` | A reason word of the declaration: an act over that |
| `:text` | In a fact's one form on arrival: the one text of the fact's value in its normal form |
| `with-texts` | In `door/declaration.clj`, private: the last step of the check |
| `softland.store.door.wait`, `door/wait.clj` | The namespace of the numbers of a maker's wait for an answer |
| `readable-hex-of` | In `door/digest.clj`, only if H2 is ruled as proposed: the digest of a thing as hex, or nil for a thing that throws while it is read |
| The names of the new checks | Section B, "The new checks" |

### The lists after

**The store's page.** Carried: `:act-id`, `:depot`, the two store keys of 8 October, `:system`,
`:base`, `:streaming`, `:microbatch`, `:accepted`, `:refused`; "stray", "place", "holder", "making
act". The store keys are five: `:softland.store/depot`, `:softland.store/permission`, and the three of
the read build. Names in Rama: the module, its alias, three depots, three topologies, four PStates of
each gate, the three temporary ones, `$$batch-clock`, and two queries. Folders and namespaces: `door`
holds `text`, `digest`, `declaration`, `additions`, `wait`, `words`. Fields of a row: `:refused`,
`:stamp`, `:digest`, `:value` ("the fact's value, plain, as a String: the one text of the value in its
normal form"), `:by-act`, `:by-index`, `:who`, `:permission`, `:level-since`, and the task's fields.
The reason words of the declaration, beside `act.clj`'s seven: eighteen, the sixteen that stay and
the two that come. The names in the declaration: five shapes and seventeen rules. The reason words of
the gate: fifteen. Words between the decision and the gate: `:fact`, `:by-value`, `:answer`,
`:last-stamp`; and for what a gate hands `writes`, `:since-by-place`, `:place-rows`, `:level-from`.
The words of a batch: two kinds of question, `:answer` and `:place`. Marks in the one text: as they
are; `#bytes` stays, since the text of anything can still hold a run of bytes.

**The door's page.** Carried: `:act-id`, `:depot`, `:digest`, `:stamp`, `:reason`, `:accepted`,
`:refused`; `act-id-for` and `answer-of`; "nothing yet" and "not mine". Calls: `open`, `act-ids-for`,
`send-act`. The door's number: `:read-every-ms`, and `longest-wait-ms` in `wait.clj`. What a call
gives: `:ack` with its two words, `:answer` with its four, `:could-not`, `:appended` with its two,
`:at`, `:module`, `:layer`, `:threw`, `:said`. The door's seven words. A reading, with `:thing` and
`:layer`.

## F. The explanations

The contract's "Explanations, as the tree's rules say"; `src-store/CLAUDE.md`. Each line that this
part makes untrue is replaced, not amended, in the commit of the code that it explains. One line for
what replaces each. A docstring of a form that section B has as changed or gone is not listed again.

### The tree's pages

| File | What becomes untrue | What replaces it |
|---|---|---|
| `src-store/README.md`, its opening (lines 13-15) and its table. The decisions sections are Sid's, and no build edits them | "The door's state, its stocks of unused locks, is the memory of one process. So is the read path's" | The door owns no state: it borrows four handles and holds one number. The read path's state is the memory of one process. The table's rows stay. This file holds another session's line that is not committed: the build stages its own lines of it and no other |
| `src-store/softland/README.md` (lines 7-9) | "the door's stocks, and what the read path holds of its live reads, are the memory of the process that opens them" | What the read path holds of its live reads is the memory of the process that opens it; the door holds nothing |
| `src-store/CLAUDE.md`, line 85 | "the seal before the depot", as a choice that a finding of the rig forced "here" | Not a build's to change without a ruling: it is the tree's rules file. Open point H8 |

### The store's page, by head

| Head, and lines | What becomes untrue | What replaces it |
|---|---|---|
| The opening (5-13, 21-23) | "the three reads by which the door reads the module" | Two reads |
| "The parts" (35, 36, 41, 42) | The door's row: it "asks the store for locks by an act of its own, seals each value", "Its stocks of unused locks", "five handles". The row of `door/`: "the seal", "its stock as a value", "One random source". The row of `module/`: "the three reads" | The door opens on the module, gives act-ids, appends each act as its maker gave it and reads the answer back; no state; four handles. `door/`: the declaration, the one text, the digest; what the door makes of one act, the numbers of a wait, its words; no state. `module/`: two reads |
| "How the parts relate" (56-62, 75-79, 85-88, 93-97, 132-135) | `seal.clj` among the shared code; step 4's "its seals"; step 6's "its plain value"; "a lock on the task of its request's uuid"; "The door reads … Each read" | The list without the seal; step 4 ends at the places; a fact's row holds the text of its value; three kinds of row have a task of their own, a fact, an answer, a row of the facts by value |
| "What the gate does not check" (206-212) | "Who reads a lock", whole | Gone. In its place, said plainly: any client of the cluster can read every value, in a table and now in a log too (J3) |
| The same (213-217) | "What a lock opens", whole | Gone. In its place: the gate keeps the one text of a value that passed `value?`, and no arrived value is a run of bytes |
| The same (241-256) | "Two things it can make otherwise … a new lock's bytes, in the locks table; and, in an answer, which of two words" | One thing: the word |
| The same (257-262) | "That a lock seals a value, one run early", whole | Gone |
| The same (276-282) | "256 ranges of locks"; "As it falls out, a value act of such a who stops at its request for locks when its stock is empty" | Up to 256 reads of places. The door stops no act: each act of such a who reaches the gate, and is recorded (entry 7) |
| The same (283-305) | "How long one act can hold a task": the opening of each sealed value, 256 sealed values of 65,536 bytes, 32,768 lock rows, 65,508 plain bytes, "requests that were accepted before", the door's own acts | The same paragraph for plain values: the making of each value's text, the digests of the order by value, and the bound on the act, 18,874,368 bytes of text. The door bounds nothing |
| "Decisions this module stands on" (312-316) | "Decision 3 is why a gate, not the form, refuses an unsealed value" | Decision 3 stands in its first half, the act holds plain values; its other half is set aside by entry 2 of 12 October |
| The same (340, 347, 361) | K1 A, "the door's request for locks"; H1 A, "One lock seals one value", and where it shows; entry 5, `opened` and "The act in the depot's log is as the door sent it, sealed" | K1 A with two keys, and that the third left on 12 October. H1 A: set aside by entry 2, and shown nowhere. Entry 5: `fact-row` keeps the text of the value; the act in the log is as the maker gave it |
| The same, a table that is new | Nothing of 12 October is listed | "Before real data is kept (12 October)", for the entries that this part makes show: 2 (no lock, every value plain: `door.clj`, `declaration.clj`), 6, its fourth (`nesting` in `act.clj`, `max-act-bytes`), 7 (the door's three tests and no other: `additions.clj`). The other parts add their entries |
| "Engineering choices" (374-379) | "No leaf fits a run of bytes, so a fact's `:sealed` is set aside from the engine's pass and held by two rules in code" | Gone. The nesting limit is `act.clj`'s own and is the value's number |
| The same (381-388) | "a lock by its id"; "the range read of a request's locks" | An answer is found by an act's uuid and a fact by its place; the order of string keys carries the run of one value |
| "What one accepted act costs the streaming gate" (390-409) | 5 reads and 4 writes, 6 and 5; "one sealed value"; "a request for locks" among the acts that read no marks | The table of this plan, 4 and 3, 5 and 4 |
| "A permission fact sits on its holder", "What a permission allows" | Nothing | Untouched |
| "The value in a fact's row is plain, and a String" (433-449) | "what the lock opened"; the String's two forms; "The answers and the locks keep their byte arrays"; "The row names no lock: the lock of a value is found from the act in the depot" | The row holds the one text of the value in its normal form, made once by the declaration's check. Why a String and no byte array stays, as the read build ran it. The answers keep their byte array |
| "The gate does not read the permissions above the act's own" (505-508) | "up to 3 reads on every act" | Up to 31 |
| "How locks are asked for and read" (514-519), "The cipher" (521-525), "One copy of a lock's bytes" (687-700) | Each, whole | Gone |
| "The first argument of a query routes it" (548-552) | "For `answer-by-act-id` and `locks-of-request`" | For `answer-by-act-id` |
| "Where each row of a microbatch layer lives" (554-578) | The bullet of a lock | Gone |
| "Closing" (666-685) | "A second attempt makes other lock bytes, and can give …"; "a lock's bytes or a word that later change"; "the door's three reads" | A second attempt can give an act into a layer of the other gate the other of two words; so a reader could see a word that later changes; the door's two reads |
| "What one accepted act costs the microbatch gate" (702-727) | 4 reads, 4 writes, 6 messages; "its value of up to 65,508 bytes" | 3, 2 and 3; 65,536 |
| "The placing of the checks" (729-732) | "The seal comes last: it is the most costly" | The making and the counting of the values' texts is the last step of the declaration's check: it is the most costly |
| "How each of the builder's rules is met", 5 (746-753) | Whole | The module makes no lock. The one append that its code makes is of a batch's id and clock |
| 15 (792-794) | "A refusal writes the answer and no lock row … The sealing function takes a fresh random part" | A refusal writes the answer and nothing else; in the streaming gate also the task's last stamp |
| 16 (795-799) | Whole | The gate keeps the one text of each value in the fact's row. The text of a value of a refused act is dropped with the decision |
| 19 (809-821), for what it says of a lock (ruling 26) | "a lock's bytes, which are in no depot"; "and the whole row of a lock that seals a value, which is placed from the act in the depot …" | Both go. The rest of the paragraph is the other two parts' to replace |
| 20 (822-824) | "Seven numbers" | Seven still, and other ones: the limit of locks goes, the bound on the act comes, the bound on a value is of its text |
| 21 (825-841) | The third aim, "110 bytes of lock or fewer for each value", and each sentence on it; "the 5 reads of one accepted act give 384 to 616 acts a second", "the 6 … 320 to 513" | Two aims. By the same reckoning, 4 reads give 480 to 770 and 5 give 384 to 616 (reasoned: the page's own numbers for 5 reads, and the same cost of a read for 4) |
| "Words a session made" (843-1105) | Section E | Section E |

### The folder maps

| File | What becomes untrue | What replaces it |
|---|---|---|
| `door/README.md` | Nearly all of it: the table of the four files that a gate requires, with `seal.clj`; the table of the door's own three, with `stock.clj`; "How the files relate"; "Boundaries" ("Nothing here writes a lock"); the row H1 A and the row K4 B; "Two calls that the contract does not name"; "The stock" and "What the stock costs the base", whole; "The order of the steps of a send"; "The words", fourteen; the rules 1, 3, 4, 6; the lists of words | Three files that a gate requires and three of the door's own, `additions.clj`, `wait.clj`, `words.clj`. One call that the contract does not name, `act-ids-for`. The steps of a send are four. Rule 1 without its corner: the door tests what its two additions need. Rule 3: gone with the request. Rule 6: the door reads no fact of the record for a maker (ruling 7 keeps that clause). The rows of `seal.clj` and `stock.clj` go with their files |
| `gate/README.md` (19, 34-36) | The row of `decision.clj`: "the opening of each sealed value, whose plain bytes go into the fact's row"; "the opening of a sealed value, are `door/`'s" | The checks of one act against rows of the record, and the rows that a decision writes. The form of what arrived is `door/`'s |
| `module/README.md` (5-7, 13-16) | "the three reads"; "the five PStates … the locks", twice; "The three query topologies … the locks of a request" | Two reads; four PStates for each gate; two query topologies |
| `record/README.md` (the row of `rows.clj`; "What it borrows") | "the String that a fact's row holds for its value's plain bytes"; "for a fact under a plain key the bytes that its lock opened, which the door sealed" | The text that a fact's row holds for its value; every value is kept as its one text, made by the declaration's check |
| `read_path/README.md`, line 71 | "The read path reads a value from its row and opens no lock: it holds no handle of the locks, and no query of the door's" | The read path reads a value from its row, and calls no query of the door's |

### The docstrings of namespaces, and of forms that do not change in code

| File | Lines | What replaces it |
|---|---|---|
| `act.clj`, namespace | 49-50, 57-58, 70-72, 76-77 | Nothing here reads a clock, takes a random id, appends or reads a record. The door's additions ask nothing of it but the shape of an act-id. "Not here" names no seal and no locks |
| `act.clj`, `parse` | 536-539 | It is the check of the maker's act, before the door adds an act-id and a depot. A fact that carries a part that the form does not name is `:unknown-part` |
| `door/text.clj`, namespace and `feed-map` | 7-15, 172-174 | The record keeps a value by this text: a fact's row holds the text itself. A large value is fed piece by piece |
| `door/digest.clj`, namespace | 19-25 | The digest is taken of the thing as it arrived, each value plain |
| `door/declaration.clj`, namespace and `store-key?` | 5-10, 42-46, 52-61, 212-213 | What arrives is the maker's act with an act-id and the layer's depot. The engine reads every part. The check has three steps |
| `door/words.clj`, namespace | 2-3, 20-23 | Seven words. A word carries no word of a gate |
| `door.clj`, namespace | 1-123 | Written anew: what the door does, its five calls, its seven words, that it owns no state and keeps nothing between two calls, what goes plain into a depot (everything), what the door can cost when it checks nothing (entry 7's list, as facts), and its limits: a thing with no end |
| `door.clj`, `printed`, `act-id-for`, `answer-of` | 140-143, 733-735, 894-895, 905-910 | No stock is named. A maker's own second send reads its answer as her own (ruling 6) |
| `gate/decision.clj`, namespace, `reads-marked-keys?`, `store-key-fault` | 16-41, 74-77, 346-348, 280-281 | Three groups of checks. No function opens anything |
| `gate/batch.clj`, namespace and `who-meets` | 45-59, 741-743 | The routes and the kinds without a lock |
| `record/rows.clj`, namespace, `value-digest` | 7-57, 85-91, 116-135, 440 | Three kinds of row; a value is held as its one text; 65,536 |
| `record/tables.clj`, namespace | 6, 36-39, 73, 82, 86, 99, 137 | Without the locks |
| `module.clj`, namespace | 20, 56-58, 77-79, 87-89 | Two queries. "So nothing secret goes on a depot …" goes: every value is plain on a depot. Two orders that the acts of a layer depend on. A microbatch act arrives where its places are |
| `module/streaming_gate.clj`, namespace and `add-to` | 14-21, 62-64, 72-79, 84-92, 108-115, 181 | Four PStates; no step 6; 4 reads and 3 row writes; a second run after a discard differs in its stamp alone |
| `module/microbatch_gate.clj`, namespace, `write-row`, `add-to` | 17-20, 34-44, 75-111, 155-185, 289-296, 361 | Four PStates; three homes of a row; the steps without locks; closing for one reason; one thing that a second attempt makes anew |
| `module/door_reads.clj`, namespace | 9-85 | Two queries; closing for one reason; "Any client of the cluster can read a lock's bytes" goes |
| `read_path/answer.clj`, `counts?` | 100 | "and a run across entities gives no fact of the store's own" |

### The proofs' two maps

- `test-store/README.md`: **no line becomes untrue.** It was read whole and names no lock, no seal, no
  stock and no request. The split guessed that it does.
- `test-store/softland/store/README.md`: the table at its head (the rows of the pure proof, "the seal
  … and its stock of locks"; of the module's proof, the atoms of locks; of the door's proof, "the
  door's six calls" and "the locks that the proof itself left outside every stock"); the act's groups
  1, 6 and 7; the pure proof's groups 1 to 6 and 8 to 18, and of "What the pure proof does not reach"
  the lines on a seal and on the stock (274-275, 285-287); the module's section and the door's, each
  check that section B names, and of the door's the whole of "The checks that rest on a time" but its
  two lines on a wait for an answer; the read path's group of the four things and "What it changes
  outside the read path", which says that the proof appends by its own hand. The map of each new
  check says how to run it, what a pass shows, what it changes outside the code that it checks, and
  what it does not reach.

### The lines of the earlier handoffs that this part departs from

Ruling 7: listed, not edited. Each file is the contract of a build that is done.

**`BUILD-store-module.md`.** Line 10 and 79 (H1, a lock for each value); 25 (the picture: lock, seal);
46 (the act-id, the class and the seal as the door's declaration); 49 ("a permission is 4 deep at
most"); 77 (three store keys); 99-102 (a value fact's `:sealed` and `:lock-id`; `:lock-request`);
119-122 (item 5: "value or sealed"; "the locks"); 128-138 (item 7, whole); 155 and 162 (the proof's
hand asks for locks and seals; "an unsealed value refused"); 185-187 (rule 5); 217 (rule 13: "a
request for locks holds a count"); 223-224 (rule 15); 225-231 (rule 16); 237 (rule 19: "locks are kept
through a rebuild"); 241 (rule 20: "65,536 bytes in one sealed value, 256 locks in one request");
247-248 (rule 21, its third limit); 253 (the lock table and a destroy step); 265 (the choices: how
locks are asked for and read, the cipher).

**`BUILD-door.md`.** Lines 27 and 33 (what the door does: locks, seal); 49, 51 and 53-54 (the seal
among what a gate requires; three queries; the hand's helpers of locks); 67-68 (the three additions;
"the door seals, the maker never does"); 85-86 (H1 A); 90 (three store keys); 103 and 111-112 ("three
queries"; "Its one memory is a stock of locks"); 119-129 (part 3, whole); 131-143 (part 4: "the door
reads `:layer` and `:facts`", the seal, "the three additions", "a stock empty and the request
refused"); 158-159 (the proof's cases of a lock); 174-175 (D3); 179-180 (D6, in its two clauses of a
lock; its clause that the door never reads a fact of the record for a maker stays); 195, 211, 213 and
231 (what is not built, and what the page lists, where each names a lock or the stock).

## G. The order of the build and its proof

Sid's way: a part is written whole and then proven. No test loop after each small edit.

**The order in which the build session changes the files.** Each step leaves the next one something
to stand on; no step is proven before the end, but for the two runs named below.

1. `act.clj`: `nesting`, and `max-permission-depth` out.
2. `door/text.clj` and `door/digest.clj`: the docstrings, and the one function of each that section B
   names.
3. `door/declaration.clj`: the limits, the store keys, the shapes, the rules, `with-texts`, the check.
4. `record/rows.clj` and `record/tables.clj`.
5. `gate/decision.clj`, then `gate/batch.clj`.
6. `module/streaming_gate.clj`, `module/microbatch_gate.clj`, `module/door_reads.clj`, `module.clj`.
7. `door/wait.clj`, `door/additions.clj`, `door/words.clj`, `door.clj`. Then `door/seal.clj` and
   `door/stock.clj` are taken out of the tree. They stay in the repository's history at `13a1d65b`
   (entry 2).
8. `read_path/answer.clj`, its one docstring.
9. The proofs, in this order: the act's, the pure, the read path's pure, the module's, the door's, the
   read path's.
10. The explanations of section F: each folder map, the store's page, the two entry pages, the proofs'
    map.

**What it runs at the end.** From the repository root:

```
bash src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/run-proofs.sh after-the-locks-go
```

It runs the six proofs one after the other, takes the build's lock for each that starts a cluster,
and writes `runs/after-the-locks-go-<proof>.txt` with each count line. Every count line changes. No
file under `src-store/` or `test-store/` is edited while it still has a proof to start. Then, once,
the run that is no proof, with its small load, to see that it still runs:
`flock <this folder>/runs/cluster.lock clojure -M:store-module -m softland.store.read-measures small`.

**The two places where a run earlier saves a rewrite.**

- **After step 9's first three proofs are written, and before the three that need a cluster:**
  `bash …/run-proofs.sh after-the-locks-go act pure read-path-pure`. They need no cluster and take
  seconds. The pure proof runs whole batches in memory in 200 orders against deciding one at a time
  (its groups 13 to 15): a fault in `gate/batch.clj` or `gate/decision.clj` shows there, where it
  costs nothing, and not in the module's proof, where each run starts two clusters. The hands of the
  three cluster proofs are written on the forms that this run has proven.
- **After step 6, one load of the module**, `clojure -M:store-module -e "(require
  'softland.store.module)"`: Rama compiles each dataflow when the namespace loads, so a variable left
  unbound by a step that went, `*plain` or `*lock-rows`, shows in a second and before any proof is
  written against it.

**Commits.** Not this stage's. For the build session: the code and each explanation that it makes
untrue in one commit (`src-store/CLAUDE.md`); `src-store/README.md` holds another session's line that
is not committed.

## H. Open points for the session that holds the build

Each has the words in question, the options, what each means, and a recommendation with its reason.

**H1. A string that is not well-formed UTF-16.** The words: ruling 19, "the digest that the door
gives a maker is the digest that a gate takes of the thing off the depot … for a value of each kind
that the form takes"; the build page, "a value that came off the depot with another text would read
'not mine' for an act that was accepted". The run: a string with a lone surrogate is a value of the
form; it comes off the depot with `?` in the place of the half; the two digests are equal all the
same, because the digest is taken of UTF-8; today the door makes the same replacement before the seal.
- *As it falls out.* The condition holds. The row and the log hold another string than the maker
  gave, and she is not told. The door's docstring and the receipt say it (K2's sentence).
- *`value?` takes no such string.* A gate cannot hold that: the half is gone when the thing arrives.
  It would hold only at the door, which checks nothing (entry 7).
- *The digest tells the two apart*, by feeding a lone half as its own mark. Then the door's digest and
  a gate's differ for such a thing, and its maker reads "not mine" for an act that was accepted: the
  fault that ruling 19 guards against.
Recommended: as it falls out, said in the receipt. It changes nothing that holds today, it is a
string that a correct tool does not make, and each other option either cannot be built at a gate or
breaks the condition.

**H2. A thing that throws while it is read** (K1). The words: ruling 1, "The door's own words name
only what the door could not do"; `door/words.clj`, `:read-the-act`, "the thing is no plain map, or
it threw while the door read it".
- *As it falls out.* The door's digest of such a thing is the digest of `#unreadable`; Rama's client
  meets the throw; the word is `:append-cleanly`, "the door does not know whether the depot holds the
  thing", although nothing was appended.
- *The door takes its digest by a walk that tells a throw*, and gives `:read-the-act` with nothing
  appended. One function in `door/digest.clj`.
Recommended: the second. The door walks the thing once for its digest in any case; this adds no walk
and no test of the act's form, and it keeps "nothing was appended" true of every word but the one
that cannot say it.

**H3. Where the text of a value is made** (D1). The words: ruling 3, "the plan says how the text of a
value is not made more often than it must be".
- *B, as planned.* Once, in the last step of the declaration's check; the one form on arrival carries
  it as `:text`.
- *A.* A rule of the declaration counts, and `fact-row` makes the text again. The one form on arrival
  is `act.clj`'s own with two parts more, and `:arrived-fact` needs no word. An accepted value's text
  is made twice: 0.1 microseconds more for a small value, 0.36 to 1.3 ms more for one near the bound.
Recommended: B, for the ruling's own sentence. A is the simpler code and the build session may weigh
it so; five forms differ.

**H4. What the bound on the size of an act counts, and its number** (D3). The words: C2, "What it
counts and its number are the builder's to propose"; ruling 17.
- *As planned.* The text that an accept writes into the rows of the act's facts; 18,874,368. It
  refuses no act that passes today, and none at any depth whose ids are like UUIDs.
- *Who and permission alone, times the facts; 2,097,152.* The same line for depth, and nothing said of
  the values. Its proof is light: an act over it can be 200 KB. Its word must then say what is too
  large, since an act of 16.8 MB passes it.
- *The bytes of the one text of the thing as it arrived*, counted in the digest's own walk. It bounds
  the record on the log, and does not count that the permission is written again for each fact, which
  is what the limit of four bounded. It also moves the digest of the microbatch gate to its first
  step.
- *A number that is meant to bind in ordinary work*, 1 MiB or 4 MiB of an act's text. It refuses acts
  that pass today. What one act may cost a streaming layer's task is a choice of Sid's kind, and the
  store's page names it as not decided ("How long one act can hold a task").
Recommended: as planned. For the receipt, as a fact and no decision: the first three options leave
the largest act within a tenth of what it is today, and the bound that would shorten the longest
event is the fourth.

**H5. The thing travels with the act through a batch.** The words: "Never trade I/O efficiency for
code simplicity" (the skill). Read: the entry of an act in `$$questions` holds `:thing` and `:act`
(`gate/batch.clj:392`), and the thing is used once more, for its digest, in step 3 (`:941`, `:944`).
So the bytes of each value are written to a temporary PState and sent to the deciding task twice,
today and after this part.
- *Left as it is.* This part changes no I/O of a batch but what the locks took.
- *The digest is taken in step 1 and the entry carries it in the place of the thing.* Half the bytes
  of each act in a batch. A repeat then costs one walk of the thing, where today it costs none.
Recommended: left as it is in this part, and handed to the plan of `answers-in-a-log`, which changes
the same lines and needs the digest where an answer is kept. It is not a job of the locks.

**H6. A thing that a worker cannot read.** The words: the store's page, "None of them throws, so no
thing can stop a partition". The run: a thing nested 20,000 deep was written and read; one nested
100,000 deep was not appended, because Rama's client threw. Client and tasks had one stack size.
- *Said in the receipt as not run*: whether a client with a deeper stack than a worker's can append a
  thing that the worker cannot read, and what a gate's partition then does.
- *A run on two JVMs* with two stack sizes. It needs a real cluster, which is not this build's.
Recommended: the first. With the door testing nothing of a value, the depth of what reaches a depot is
bounded by Rama's client alone; before, the door held a value to 32 collections, and still held no
other part.

**H7. What an opener may give.** The words: ruling 2, "the two numbers of a maker's wait for an
answer … an opener can still give them".
- *One name, `:read-every-ms`*, as today; `longest-wait-ms` stays a constant that cuts a maker's
  bound. This plan.
- *Two names*: the longest wait is the opener's too.
Recommended: one. Nothing asks for the second, and a longest wait that an opener lowers is a new
behaviour of `answer-of`.

**H8. The rules file names the seal.** The words: `src-store/CLAUDE.md`, line 85, "Where a finding of
the rig on Rama forced a choice here, such as refusals as data, the seal before the depot, or the
stamp, cite the finding". The seal is no choice of this tree after this part.
- *The build takes the three words out* with the code.
- *The line is left* for whoever keeps the rules file.
Recommended: the build session rules on it; it is the tree's rules and no explanation of code. The
root `CLAUDE.md` holds the same three words in "Reading the project", and is Sid's.

**H9. A send with no end.** The words: ruling 1's three tests; `door.clj`, "Limits". Before, facts
with no end in which values keep coming were stopped at the 257th value. After, the door reads no
fact, so no send of a thing with no end comes back: it holds its maker's thread in the digest's walk.
The door's proof must drop its case of such a thing (section B), or the proof hangs.
- *As it falls out*, said in the door's docstring and in the receipt (J11's sentence).
- *The door bounds its own walk.* That is a test of the act before the log, which entry 7 takes away.
Recommended: as it falls out.

## Rulings that cannot hold

None. Each ruling that names this part was traced in the code and, where it names a run, run: 1 (the
three tests, section B, `additions.clj`), 2 (H7), 3 (D1), 4 (section C, the shape), 5 (`held-value`
gone, `read-value` kept), 6 (J14), 7 (section F, the last table), 16 (D2, run), 17 (D3), 19 (section
C, run), 20 (D2's last paragraph; J15), 24 and 25 (C and D are in this one plan; the form that the
plan of `answers-in-a-log` took from ruling 19 is the form that the run leaves standing).

## Design difficulty log

**The text of a value, once or twice.** This was the hard one. I first took the simplest form: the
bound as one more rule, and the row making its text as the store-key branch does today. It reads
best: what arrives is the maker's fact, and a row is one line. Then I measured, and a counting walk
costs what making the text costs, so "count, then make" is two makings; for a value near the bound
that is a third of a millisecond to more than a millisecond, on a thread that a layer's other acts
wait for. Ruling 3 asks in its own words that the text not be made more often than it must. What
settled it was seeing that today's one form already has the shape that carrying needs: a fact under a
plain key holds no `:v`, and its bytes ride beside it. So carrying the text is the smaller change to
the gates, and costs a batch no byte. The call was close, and H3 gives the other form whole.

**What the bound on an act counts.** I went through four counts. The first that came to mind, the
bytes of the thing, is free in the digest's walk, and I held it until I saw that it does not count
what the contract points at: the permission written again in each row. A count of who and permission
alone is the exact heir of the limit of four and is cheap to prove, but its word would lie, since an
act of 16.8 MB passes it. Once the texts are made in one step the whole count is free, and the number
came from three things asked of it at once. It is the builder's, and it binds only in a corner; H4
says so plainly, and says which other number would do more.

**The one depth.** Forced once the code was read. The engine gives a shape with no number the number
1, and a given permission is a value. One number for both roads is the only form in which the two
agree without a second rule.

**Whether the door should still tell a maker that her thing cannot be read** (H2). Small, and it took
longer than it should: I had to see that the digest hides the throw before I saw that the word would
change to the one word that cannot say "nothing was appended".

**What was not contested.** That the two tables, the query, the store key and the two files go; that
the door's additions are two; that closing stays. Each follows from A1 to A3 and the rulings with no
choice in it.

## The skill's checks, gone through

Step 6 of the stage: each check of `artifact-plan-validation.md` held against this plan. No validation
paper is written here.

- *Query topologies.* Two stay as they are. `answer-by-act-id` makes its second read only when the
  first found a row. Its one read that finds nothing is the question "is there an answer", and is
  today's.
- *PState schemas.* None is added, none holds `Object`, and no two that stay share a key and a
  partitioner in a way that this part changes.
- *Partitioning.* No partitioner changes. The table is filled at 1, 16 and 128 tasks; its proportions
  sum to 1; its weighted seeks are 1.10 at each.
- *Topologies.* No topology is added. No choice here was made for the sake of a test: the bound on an
  act was weighed on what it refuses, and its cost to a proof is said beside it and did not choose it.
- *Production readiness.* Many makers at once: the door holds nothing that two sends share, so nothing
  in it orders them. A client that starts again: an act-id and a digest that a maker kept read the
  answer through any door (the new check `the-door-holds-nothing`). A worker that starts again at any
  point: "A second attempt", for each write that stays. Scale: no collection is added.
- *Writes of a stream topology that are not the same on a second run.* None after this part; the one
  of today goes.
- *Internal depots, appends from a stream topology, task globals.* None is added or changed.
- *Minimality.* The part removes. It adds three things: the last step of the check (ruling 3), a home
  for two numbers (ruling 2), and, if H2 is ruled so, one function of the digest. H3 gives the form
  without the first.

## What rests on reasoning, for the build to look at first

Ruling 31: this plan gets no check of its own, and the part's proofs say whether it holds. Nothing
here was proven on the store's own module: no file of it was changed. Four things rest on reasoning or
on what was gathered, and each shows early in the build, at the run of the three proofs that need no
cluster (section G).

- That `with-texts` as the last step keeps every thing that passes today's declaration passing, and
  refuses nothing but a value or an act over its bound: reasoned from the rules that go and the two
  bounds. The pure proof's groups 2 and 3 hold it.
- That no order is needed between the rows that a task of the microbatch gate is sent, once no lock's
  row is among them: reasoned from `gate/batch.clj:109-116` and `rows-to-send`. The pure proof's
  groups 13 and 14 hold it, in 200 orders.
- The count of 3 messages for one act of the microbatch gate: reasoned from `gate/batch.clj`. No proof
  holds a count of messages; the store's page gives it as counted from the dataflow.
- Each line number of a proof, and that no check of the read path's proof holds a number that rested
  on the rows of the door's requests beside the three that section B names: gathered. The gatherer of
  that proof did not read `read_path.clj`.
