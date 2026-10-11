# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

**What this paper is.** The implied spec of the reads: what the contract of the read build implies and
does not write out. It checks each line of the contract that says how the code stands today. It lists
each thing that a gate must now keep or refuse, each word of a query, each state that a read or a gate
can meet, and each place where the contract's words cannot all hold or are silent.
**For which contract.** `src/proposal/backend-on-rama-2026-10-02/BUILD-reads.md` at head `c3e65c06`.
"The contract" below is that file. "A1" to "A7", "B1" to "B5", "C" and "R1" to "R8" are its own
numbers. "Entry 1" to "entry 9" are the entries of "Decisions of 11 and 12 October 2026" in
`src-store/README.md`. "Rule 1" to "rule 21" are the store's rules in `BUILD-store-module.md`, as the
store's page says how each is met. Sid's decisions in `src-store/README.md` win over the contract, and
the contract wins over everything else.
**The stage.** The first of the Rama skill's stages, Phase 0 (`phase-0-implicit-spec.md`). It decides
nothing and designs nothing: no PState, no table shape, no form of a query, no name, no topology.
**Who wrote it.** One fresh session (model Opus 5.5, effort xhigh), 12 October 2026, for the build
session of the reads to judge. It wrote this one file and changed no other.

**What was read.** The skill (`SKILL.md`, `phases.md`, `phase-0-implicit-spec.md`,
`artifact-implicit-spec.md`, and of `foreign-client.md` and `microbatch.md` the parts on a push and on
a second attempt). The contract, whole. `src-store/CLAUDE.md`. `src-store/README.md`, whole. The sheet
`decisions/groups/g-reads.txt`, whole. `reads/push-probe/RESULTS.md` and `run-2.log`; of the two probe
files, the paths that they open. `src-store/softland/store/README.md`, whole. Whole, function by
function: `record/tables.clj`, `record/rows.clj`, `gate/decision.clj`, `gate/first_act.clj`,
`gate/stamp.clj`, `gate/batch.clj`, `door/declaration.clj`, `door/seal.clj`, `module.clj`,
`module/streaming_gate.clj`, `module/microbatch_gate.clj`, `module/door_reads.clj`. Of `door.clj`: the
namespace docstring, the calls of Rama's client, the getting of locks, the head of `send-act`. Of
`door/additions.clj`: the walk of the facts, the thing to append, the request. Of `act.clj`: the
shapes, the rules, `value?`. Of `door/text.clj`: the namespace docstring and the texts of scalars. Of
`BUILD-door.md`: rule D8 and part 1. Of `BUILD-store-module.md`: rules 1 to 21. The queue
`decisions/OPEN-QUESTIONS.md`, whole. Of `store-module/PROGRESS.md`: items 18 and 24. Of
`door/PROGRESS.md` and `door/README.md`: their headings and the lines on the door as the one client.
Of the proofs: the checks that a line below names, each read and not run. Of Rama's documentation
under `reference/rama/docs/`: the lines named below. Those pages are an extract of March 2026 whose
pages name Rama 1.5.0, and the store runs 1.6.0; each line taken from them is marked "read, not run".
Of the rig's notes: a search of its build notes and bench notes for a push and for a second attempt
of a batch. It found no finding on either; the one line that says "The second attempt" is of a bench
run and not of a batch (`src/proposal/rig-2026-09-25/BUILD_NOTES-numbers.md:253`).

**What was run.** Nothing, by this session: no proof, no probe, no cluster. The four proofs were run by
the build session on 12 October 2026 at head `c3e65c06`, each with exit 0: act-proof
`{:test 8, :pass 150, :fail 0, :error 0}`, pure-proof `{:test 16, :pass 540, :fail 0, :error 0}`,
module-proof `{:test 22, :pass 227, :fail 0, :error 0}`, door-proof
`{:test 26, :pass 193, :fail 0, :error 0}`. Where a line below says "a check of the proof", the check
was read by this session, and its pass is the build session's run. The push probe was run on
11 October by the session that cut the contract; this paper cites its page and its log.

**Blocked.** Nothing blocked this stage. Two things came into the tree while it was written:
`reads/PROGRESS.md` and `reads/batch-probe/`. Neither was read, so that this paper stands on the
contract and the code alone. What this paper would want the batch probe to show is under P17.

**Marks.** "Read at" means read in the source at that line. "Derived" means reasoned by this session
from lines that it names. "Not checked" means neither. A file name without a folder is under
`src-store/softland/store/`: `rows.clj` is `record/rows.clj`, `tables.clj` is `record/tables.clj`,
`decision.clj`, `batch.clj`, `first_act.clj` and `stamp.clj` are under `gate/`, `declaration.clj`,
`seal.clj`, `additions.clj` and `text.clj` are under `door/`, `streaming_gate.clj`,
`microbatch_gate.clj` and `door_reads.clj` are under `module/`. "The store's page" is
`src-store/softland/store/README.md`. "Sid's page" is `src-store/README.md`. "The sheet" is
`decisions/groups/g-reads.txt`.

## How the skill's template is adapted

The template is written for one Rama module. This build is two things.

- **Half A changes the module.** Its operations are the writes that a gate makes for one act. They are
  the template's home ground: what each gate must keep, what it must refuse, and what each write does
  to each read. They are given for the streaming gate and for the microbatch gate each, because the
  two differ. A streaming layer is whole on one task. A microbatch layer's facts are on the tasks of
  their entities, and its depot fact and its permissions are on every task.
- **Half B is a client of the module**, in the runtime's process, as the door is. It has no depot, no
  PState and no topology. So for half B "an operation" is a thing that a caller asks of the read path:
  a word of a query, asked once or kept live, through a list of layers. "Latency" counts the round
  trips to the cluster and the seeks on a task. "Data growth" has two homes: the memory of the
  runtime's process, and the rows that half A adds to the cluster.
- **The matrix** covers each thing whose state a read or a gate depends on: a layer, a permission, a
  place, a marked key, a live read, and a batch of the microbatch gate. Its "writes" are acts that a
  gate decides, and three events that are no act: an asker leaves, the runtime's process ends, and
  Rama runs a batch again.
- **Requirements, not designs.** A line says what must be true, with the clause of the contract or the
  line of the source that makes it so. Where the only way to say a requirement is to name a thing of
  today's code, the name is in backticks with its file, and it is there as a fact of today and not as
  a choice.
- **Open points are not requirements.** Where the contract is silent, cannot be met as written, or
  disagrees with Sid's page or with the code, the point is under "Open points for the build session
  to rule on", as `P1` to `P31`. Each has one of three labels. `[Sid's]`: only Sid can decide it; the build builds a kept answer so that the other
  answer is a small change, and proposes a row for his queue. `[builder's]`: an engineering choice of
  the plan. `[fact]`: only a run, or Rama's own pages, can settle it. This paper chooses none.

Words. "Act", "maker", "door", "gate", "depot", "layer", "place", "holder" as the tree uses them. "Key"
is the key of a fact. "Lock" is an encryption key. "Asker" is the one in whose name a read is made.
"Caller" is the code in the runtime that calls the read path. "A push" is Rama's proxy. "A live read"
is a read that stays open. "The other order" is the one more order of A6. "The three new store keys"
are the setting of A3, what a layer builds on of A5, and the mark of A6; this paper gives them no
names.

## The check of each "as built" line

The contract says: "The stage that writes the implied spec checks each 'as built' line of this page
against the code, since the cutter read docstrings and maps and not every function." Each line below is
a claim that the contract makes about the code as it stands today, or a claim about Rama on which it
rests. The functions were read, not only their docstrings. Verdicts: **holds**, **holds in part**,
**does not hold**.

| # | Where | The claim | Verdict | What the code says |
|---|---|---|---|---|
| L1 | "What this build is" | "The door is the one way by which an act goes into the store." | holds in part | It is so by rule and by no guard. "anything can still append to a depot without the door" (`door.clj:13-15`). The module's proof appends by hand (`test-store/softland/store/README.md:529-537`). |
| L2 | The picture | The gates' tables can be "read straight by the read path. No query of the store stands between." | holds | No PState of `src-store` is declared private: a search for `private?` finds nothing. The module's proof reads rows by a select with a partitioning key (`test-store/softland/store/module_proof.clj:248`, `:420`). Today the three reads are queries (`door_reads.clj:89`, `:115`, `:151`), and nothing in `src-store` opens a PState client or a push: a search for `foreign-pstate`, `foreign-select` and `foreign-proxy` finds nothing there. |
| L3 | A1 | "A gate opens each sealed value when it checks it." | holds | `decision.clj:292-309` and `:330`; `seal.clj:155-174`. Two limits. The seal is the last check, so a value is opened only when each earlier check passed (`streaming_gate.clj:221-238`; `batch.clj:615-620`). And `seals` stops at the first fact that fails, so a refused act's later values are not opened. Each value of an accepted act is opened once. |
| L4 | A1; entry 5 | The gate "keeps the plain bytes that it already has". | holds in part | It has them for an instant, and nothing gets them. `seal/opens?` is `(some? (open ...))` and gives true or false (`seal.clj:169-174`). `seal-fault` gives a word or nil (`decision.clj:308`). `writes` and `fact-row` take the arrived fact and write its sealed bytes with the lock (`decision.clj:362`; `rows.clj:159-163`). So the plain bytes must be carried from the opening to the row; no call does that today. |
| L5 | A1 | "The act in the log is not changed: the door seals as built". | holds | The door seals the `:v` of each fact under a plain key and leaves a value under a store key plain (`additions.clj:203-232`, `:345-382`; `door.clj:82-87`). |
| L6 | A1 | "a gate still refuses an unsealed value that arrives". | holds | The rule `:plain-value-is-sealed`, word `:not-sealed` (`declaration.clj:230`, `:414-418`). Each gate calls the declaration (`streaming_gate.clj:174`; `microbatch_gate.clj:260`). The refusal is a recorded answer. |
| L7 | A1 | "The locks table and the door's requests stay as built". | holds | Nothing to check but what "as built" is. The locks table: `tables.clj:104-110`. A lock's row names the fact that it seals (`rows.clj:209-215`). The request: one fact under `:softland.store/lock-request`, under the maker's who and permission (`additions.clj:413-439`). In a microbatch layer a lock that seals a value has its whole row beside the value and a row with no bytes at its home (`batch.clj:594-599`). |
| L8 | A2; the sheet's W12 | Today a fact's row holds "its value · its stamp · the id of the act it came in". Who wrote it is in neither row. | holds | `tables.clj:93-102`. The row holds the uuid of the act and the fact's index, not a whole act-id; the layer is in the row's key and the depot is told by the table (`rows.clj:20-22`, `:57-59`). |
| L9 | A3, A5, A6; R7; the sheet | "As built, a store key takes one write for a place". | holds in part | One write is coded for two keys: a depot fact after the making (`decision.clj:231-232`) and a permission fact on a place that holds a row (`:233`). A request's place is new by its form (`declaration.clj:407-412`). Nothing holds one write for a store key as such. So a key that is added to `store-keys` and to nothing else takes a second write with no refusal. Its retract is refused: `:store-key-retract-not-built` is given for any store key (`decision.clj:230`). |
| L10 | A4; the sheet's W1 | A permission is `[who layer granted-in]`, or the same with the permission above it. "It has no slot for what it allows". | holds | `act.clj:171-174`. It is 4 deep at most (`act.clj:102-104`, `:179-182`). The shape is in the maker's form, `act.clj`, and not in the door's declaration. |
| L11 | A4 | Today a gate checks that the act's permission stands, and no more. | holds | Check (d) is one comparison of bytes: the row at the place of `:who`'s permission holds the one text of the permission that the act names (`decision.clj:136-139`; `rows.clj:182-190`; `batch.clj:207`). |
| L12 | A6; the sheet's W11 | Today there is one table of facts, kept in the order layer · entity · key. | holds | `tables.clj:93-102`; `rows.clj:105-111`. |
| L13 | A7 | "The papers name a repair that gives both: keep each batch's clock ... (`store-module/PROGRESS.md`, item 24)." | holds in part | Item 24 names two ways: "the row before, kept beside the new one; or attempts of a batch made the same, by keeping each batch's clock (rule 9)" (`store-module/PROGRESS.md:222-229`). It is marked "Read in the code, not run". No paper shows that the repair "gives both". The store's page says of it: "To make both the same would take state outside the microbatch gate. Not built." (`:190-196`), and of rule 9: "a record of each batch's cut and clock, a second log beside the depot. Nothing is built for it; whether it is wanted is Sid's." (`:454-461`). See P16. |
| L14 | A7; the sheet's W13 | While a new fact of the base waits to be readable, "its place gives no row at all; a place that is replaced in every batch never gives one". | holds, derived | A place holds one row, and a write puts a whole row at its key (`tables.clj:93-95`; `microbatch_gate.clj:206-207`). A read that is held to closing gives a row only when its stamp is not later than `:closed-through` (`rows.clj:248-257`). So it would give nothing for a place whose one row is of a batch that is not closed. No read of today meets it: the one fact that a read gives is a layer's depot, which is written once (`door_reads.clj:151-172`; the store's page, `:395-400`). Not run. |
| L15 | "What the probe found" | "A table that a microbatch topology writes can be watched in the same way; its pushes came later." | holds, and it bears on A7 | The probe's case Z6 (`reads/push-probe/RESULTS.md:44`). The store's page says what such a read sees: "A select that goes straight to a PState of the microbatch gate is not held to that: it sees a batch that is not closed" (`:173-177`). So a push on the microbatch gate's facts gives rows of a batch that Rama can still run again. |
| L16 | "Not decided" | "One permission for each holder in a layer, as built." | holds | A permission fact sits on its holder (`declaration.clj:226`, `:392-396`), its place is found from the layer and the holder (`decision.clj:89-94`), and a second write there is refused (`decision.clj:233`). A check of the proof: `test-store/softland/store/module_proof.clj:771` and `:1241`. |
| L17 | "Not decided" | "The store's page says of it 'Sid should know this one'." | holds | The store's page, `:291-294`. |
| L18 | "Not decided" | Who may write the three new keys: "Kept: whoever may act in the layer, as for its depot (7 October, 3)." | holds in part | Decision 3 of 7 October says that the members set a layer's depot. The code lets one act write a depot fact, the making act (`first_act.clj:68-71`). Each later depot fact is refused, with the same depot or the other (`decision.clj:231-232`; a check of the proof at `module_proof.clj:774`). So no code shows "whoever may act in the layer" writing its depot. What the code does give: an act whose permission stands may write any fact of its layer (`decision.clj:116-153`), and check (f) holds a setting key to the layer's own entity (`:188-194`). |
| L19 | "Not decided" | "no layer names an owner as a fact". | holds | `store-keys` has three keys (`declaration.clj:125-139`). A fact under `:softland.store/owner` is refused `:store-key-not-known` (checks of the proofs at `test-store/softland/store/pure_proof.clj:587-589` and `module_proof.clj:749`, `:791`). |
| L20 | "Not decided" | "A permission's place takes one write, and nothing ends a permission." | holds | `decision.clj:229` and `:233`. Sid's page says of it "A session read this in `gate/decision.clj`; it was not run" (`src-store/README.md:392-394`). It is in the proofs: `module_proof.clj:771-772` and `:1241`. |
| L21 | "What the probe found" | "`place-key` joins layer, entity and key with a space". | holds | `rows.clj:105-111`. |
| L22 | "What the probe found" | "So the places of one entity are one run". | holds | Derived: no printed text of an id holds a space (`declaration.clj:265-271`), and the least character that a keyword's text can hold is `!`, the next after a space (`act.clj:226-238`). So the keys from `"L E "` up to `"L E!"` are the places of E in L and no other. That Rama keeps a table's top keys in the order of their bytes is read at `reference/rama/docs/17-serialization.md:165-169` and was run by the probe on a table of the same shape. What the run holds beside a tool's facts is F4 below. |
| L23 | "What the probe found" | "A streaming layer is whole on its task." | holds | The depot partitions by `:layer` (`module.clj:73`). The gate's event has no partitioner (`streaming_gate.clj:68-73`, `:247-260`). The store's page names the one way round it, a thing that a topology appends to another partition (`:143-154`); no topology of this build does. |
| L24 | "What the probe found"; "What this page rests on" | "A microbatch layer's facts are on the tasks of their entities (the namespace docstring of `module.clj`)". | holds in part | A fact under a plain key or under `:softland.store/lock-request` is on the task of its entity (`batch.clj:591-593`; `microbatch_gate.clj:390-397`). A fact under `:softland.store/depot` or `:softland.store/permission` is on every task (`batch.clj:152-158`, `:593`; `microbatch_gate.clj:391-392`). `module.clj:48-55` does not name the second kind; `rows.clj:37-43` does. |
| L25 | "What the probe found" | "so one entity of the base is one push, and a run across entities of the base is not". | holds, derived | Every row of one entity is on the task of that entity, and the rows that every task holds are there too. A run across entities meets one part on each task; and each task then gives its own copy of each permission and of the depot fact. Not run on the store's module. |
| L26 | "What the probe found" | "A push covers one task." | read, not run on the store | "They work exactly like `selectOne`", with "a variant accepting an explicit partitioning key" (`reference/rama/docs/15-pstates.md:436`, `:402`). The probe opened each push with a partitioning key. |
| L27 | "What this page rests on" | The facts table's schema; `place-key`; the check of a permission and `store-key-fault`; the two depots; "a permission fact sits on its holder". | holds | L8, L21, L11, L9, L23, L24; the store's page, `:291`. |
| L28 | "What this page rests on" | Rama: "a private table refuses every client". | read, not run | "Trying to fetch a client for a private PState will throw an exception." (`reference/rama/docs/28-clj-defining-modules.md:263`). |
| L29 | "What this page rests on"; "The builder chooses" | Rama: "a table can be added at a module update and filled from the start of a log". | holds in part; read, not run | A module update can add a PState and a topology (`reference/rama/docs/19-operating-rama.md:422`). But "`:start-from` options have no effect if the topology has run before, like on a module update" (`28-clj-defining-modules.md:321`), and in Rama only the topology that owns a PState writes it (`tables.clj:3-5`). So a new table of a new topology can be filled from the start of a depot. A new table of a gate that has already run cannot be filled that way. See P20. |
| L30 | "What this page rests on" | Rama: "a function in a client's path must be on the module's classpath, and a lambda cannot be". | read, not run | "the class of that object must be on the classpath of both the client and the module. Lambdas are not usable in PState client paths for this reason." (`reference/rama/docs/15-pstates.md:394`). |
| L31 | "Explanations" | The store's page says "that a gate keeps nothing of the plain bytes, that a store key takes one write, and that nothing reads a fact for a caller". | holds | The store's page, `:170-172` and `:480-482`; `:471-473` and `:663`; `:395-400`. More lines that this build makes untrue are F2 below. |
| L32 | R1 | Rule D8 of the door's handoff says that the door is the module's only client in the runtime. | holds | `BUILD-door.md:183-184`. The same sentence is in five explanations of the tree; F2 lists them. |
| L33 | R7 | "The store's rules 1 to 21 hold where they touch. This build changes two." Rules 13 and 16. | holds in part | The rules as `BUILD-store-module.md:176-248` writes them change in two, 13 and 16. What the store's page says of how other rules are met changes too. Rule 2: "A fact that names nothing to replace reads no row" (the store's page, `:433-434`); for a fact under a marked key a gate must know the row before, see A6. Rule 19: the whole row of a lock "is placed from the fact's row that names it" (`:498-501`); after A1 the fact's row names no lock. Rule 5's last sentence and rule 9 are touched by A7; see P16. Rule 21's counts of reads and writes change for a marked key. |
| L34 | C | "The base on a real cluster cannot be measured here, since no cluster runs". | not checked | This session ran nothing. The sheet says that the listening ports were looked at on 11 October. |

**The count.** 34 lines. 22 hold: L2, L3, L5 to L8, L10 to L12, L14 to L17, L19 to L23, L25, L27, L31,
L32. 8 hold in part: L1, L4, L9, L13, L18, L24, L29, L33. 3 are Rama's and are read, not run, with no
difference found: L26, L28, L30. 1 is not checked: L34. None was found false outright. The lines that
hold in part are the ones to read: each says what the code does in place of the claim. Three of them
change what the build must do. L4: the plain bytes are not in any hand that could write them. L9: a
new store key takes a second write as it falls out, and its retract is refused as it falls out. L18:
no code shows the members of a layer writing its depot, so "as for its depot" names a decision and no
code.

### What the check found that the contract does not say

Each is read in the source. Each bears on a requirement or on an open point below.

- **F1. The proofs of today check the row that A1 takes away.** The module's proof checks that a
  place "holds the sealed bytes as sent, the fact that wrote it, its lock"
  (`module_proof.clj:612-614`, `:1027-1029`), that a retract's row has "no value and no lock"
  (`:692-694`), and that a tool's fact under a bare word is "sealed" by the test
  `(some? (:lock-act ...))` (`:785-788`, `:1252-1254`). The pure proof checks the row's fields with
  `:lock-act` and `:lock-index` (`test-store/softland/store/pure_proof.clj:909-910`, `:1208`), the
  exact map of "the three store keys" and which is a setting (`:417-419`, `:544`, `:2334`). See P1.
- **F2. The explanations that this build makes untrue**, beside the three that the contract names.
  That the door is the module's one client: `door.clj:52`; the store's page, `:165-167`;
  `door_reads.clj:73-75`; `door/README.md:223`; `test-store/softland/store/README.md:529-531`. That
  a gate keeps nothing of the plain bytes: `decision.clj:59-60`, `:296-297`, `:326`; `seal.clj:10-12`,
  `:170-172`. That a row's value is "sealed when a lock is named": `rows.clj:15-19`, `:152-156`; the
  store's page, `:584-585`. That the store's keys are three: `declaration.clj:28`, `:102-103`;
  `additions.clj:8-9`, `:167-181`; the store's page, `:253`. That the rows which every task holds are
  two: `batch.clj:152-158`; `rows.clj:41-42`; the store's page, `:355-358`. That no read meets a
  place that is replaced: the store's page, `:395-400`. That the door "reads no fact of the record for
  a maker" stays true, and its line on W3 and W4 as open does not (`door.clj:111-113`).
- **F3. Nothing may route by a whole key.** "A row is placed by the task on which its gate writes it,
  not by a hash of its whole key. So nothing may route by a whole key." (`tables.clj:46-47`). Rama's
  client routes a select and a push by "the first key in the path" unless a partitioning key is given
  (`reference/rama/docs/28-clj-defining-modules.md:455-461`, `:521`; read, not run). The first key of
  a place's path is the whole key. So each read and each push of the read path must name the task by
  a partitioning key: the layer for a streaming layer; for a microbatch layer the entity of a fact,
  and any task for a row that every task holds.
- **F4. A run of an entity holds the store's own facts.** The facts table holds every accepted fact,
  under a store key or a plain key (`rows.clj:29-35`). A permission sits on its holder as an entity.
  A layer's depot sits on the layer as an entity. A request for locks sits on the uuid of its act as
  an entity. So the run of the entity `:alice` in a layer holds her permission in that layer; the
  run of the layer's own entity holds its depot, and after this build its setting, its list and its
  marks; and a run across entities holds each request for locks. See P11.
- **F5. A row's field has one class, and no row holds a collection.** "No row holds a collection, and
  no position is of the class Object. Each row is a fixed-keys schema, each field of one class"
  (`tables.clj:57-60`). An id is a keyword or a UUID, "two classes" (`rows.clj:53-54`). A permission
  is a vector inside a vector, up to 4 deep. A plain value is held today "as the bytes of its one
  text, so the field has one class" (`rows.clj:33-35`). A2 puts a who and a permission in the row.
  See P22.
- **F6. The door's request for locks is an act under the maker's own who and permission**
  (`additions.clj:423-426`). When a gate refuses the request, the door gives the maker the word
  `:have-its-request-accepted` with the request's answer, and appends nothing for the maker's act
  (`door.clj:559-561`). So a refusal that A4 adds meets a value act at the door's request first.
- **F7. The form of a making act is exact.** Its permission has 3 slots and is `[holder layer layer]`,
  and it has exactly two facts: the depot and that permission (`first_act.clj:79-94`). So a part
  more in a permission, and a third fact at the making, each change `making-act?`.
- **F8. Two texts for one id.** A row's key is made of the printed text of an id: a UUID as its 36
  characters (`declaration.clj:265-271`). The one text of a value that is a UUID is `#uuid "..."`
  (`text.clj:132`). A keyword has one text in both. So "the value is that thing's id" has two printed
  forms for a UUID. It bears on A6.
- **F9. What `plain-value-of` gives for bytes that are no value of the form.** It reads the first EDN
  form of the bytes and gives nil when that throws (`rows.clj:192-200`). A gate "does not check that
  what a lock opens is a value of the form" (`decision.clj:326-328`). The door seals only a value of
  the form, as its one text, which `clojure.edn` reads back (`additions.clj:220-224`; `text.clj:56-57`).
  A thing that is appended without the door can hold other bytes under a right seal. After A1 those
  bytes are in the row. See P14.
- **F10. Which record holds a layer is told by the layer's id.** `depot-of-layer` reads the record
  that `depot-at-making` names, "no act of this build changes a depot, so the other record cannot hold
  it" (`door_reads.clj:59-62`, `:158`). The door reads the layer's depot for each act-id all the same,
  to leave room for a layer whose depot changes (`door.clj:248-267`). See P30.
- **F11. A push that Rama cannot serve ends after a count of errors.** "if a proxy errors 10 times
  within 120 seconds, it is forcibly terminated" (`reference/rama/docs/15-pstates.md:732`, under
  "Fault handling"; read, not run). The probe's three refusals hung "for about 100 seconds"
  with an error "every 10 seconds" (`reads/push-probe/RESULTS.md:51-54`). This looks like that
  default, but the cause was not verified.

## Operations

### What holds for every operation of half A

These are requirements of the store today that each change of a gate must keep. Each is read in the
source.

- **G1. A gate never throws on what arrives.** A throw in a topology stops its partition. A refusal is
  data: an answer with a reason word (`streaming_gate.clj:24-29`; `microbatch_gate.clj:33-37`;
  `declaration.clj:46-48`). So each new check gives a word and each new row is of the classes that the
  schema declares (`rows.clj:61-64`).
- **G2. One act is whole or nothing.** In the streaming gate all the writes of one decision are one
  group in one event, with no partitioner and no yield (`streaming_gate.clj:68-73`). In the microbatch
  gate the rows of an accept go to several tasks inside one attempt, which Rama keeps or drops whole
  (`microbatch_gate.clj:79-82`). So each row that half A adds for an act is written in that same
  event or that same attempt.
- **G3. A second run writes the same.** Each write of a decision is a whole row at a fixed key, behind
  check (a) (`decision.clj:388-391`; `streaming_gate.clj:75-84`). The skill: "Every write must produce
  correct results even if processing retries." (`SKILL.md:32`).
- **G4. A gate reads on its own task** (decision 5 of 7 October, F5 A; the store's page, `:239`). The
  streaming gate's event makes no hop. In a microbatch layer the rows that every act needs are on
  every task.
- **G5. The order of the checks is the order of the reasons** (`decision.clj:16-20`;
  `declaration.clj:201-217`). A new check has a place in that order, and a thing with two faults gets
  the word of the first.
- **G6. Each word that the build makes is plain and listed for Sid** (R6). Each new reason word, each
  new key and each new field of a row is one.
- **G7. No kept data.** "Before real data is kept, it is code" (the sheet, `:273`). No cluster holds
  a record of this store: the proofs start an in-process cluster and close it
  (`test-store/README.md:23-27`). So a change of a row's fields is a change of code and needs no
  migration. Not checked by a run of this session.

### A1. A plain value in the facts table (entry 5; rule 16)

**What must hold.** For each accepted fact that arrives sealed, the row of its place holds the plain
bytes that the lock opened, and names no lock. The act in the depot is as the door sent it. A gate
still refuses a value that arrives unsealed under a plain key (L6). The locks table, the lock's row
that names the fact it seals, and the door's requests are as built (L7).

| | The streaming gate | The microbatch gate |
|---|---|---|
| Where the value is opened | In the one event, on the layer's task, at the check of the seal (`streaming_gate.clj:238`). | Where the act is decided: its deciding task in step 3, or the layer's meeting task in step 4 (`batch.clj:615-621`, `:798-835`). |
| Where the row is written | The same event, the same task. | The task of the fact's entity. For an act on one entity that is the deciding task. For any other fact the row travels to its home inside the attempt, as a sealed row does today (`batch.clj:644-647`). |
| A second run | A run after a commit ends at check (a). A run after a discard opens the same sealed bytes with the same lock and gets the same plain bytes. | Each attempt opens the same bytes. A lock that an act can use was made by a request of a closed batch, or the use is refused (`batch.clj:806-811`). So the plain bytes are the same on each attempt. Derived. |
| The lock's rows | One row; it keeps its bytes and names the fact (`rows.clj:45-47`). | Two rows as today: the whole row beside the value, and the home's row with no bytes (`rows.clj:47-51`). |

**States that it meets.** A fact with a sealed value: the row holds the plain bytes. A fact under a
store key: the row holds the one text of the value, as today (`rows.clj:165-166`). A retract: the row
holds no value (`rows.clj:154-156`). A refused act: no fact row is written, and no plain bytes are
kept anywhere (`decision.clj:388-389`).

**Invariants.**
- The plain bytes are in the fact's row and nowhere else that the build adds: not in an answer, not on
  a depot, not in a word. Rule 5 holds as it is: a lock's bytes are never on a depot.
- One value is opened once for one decision (rule 16's first sentence stays).
- The seal still binds a value to its place: a gate still refuses sealed bytes that do not open at
  the fact's place (`decision.clj:308`).
- After A1 a row that names no lock is no longer "a fact under a store key". Today `fact-row` says of
  it "A fact with a plain value, which is a fact under a store key" (`rows.clj:152-154`). Three
  callers read that test. `standing` and `standing-on-arrival` ask `holds-plain-value?` of the row at
  a permission's place (`decision.clj:138`; `batch.clj:207`). `depot-of-layer` asks `plain-value-of`
  of the row at a depot's place (`door_reads.clj:163`, `:171`). Each of those places can hold only a
  fact under its store key, which never arrives sealed (`declaration.clj:380-383`). So none of the
  three changes its answer. Derived. What changes is what the two functions give for a tool's row:
  `plain-value-of` then gives a tool's value where it gave nil, and the proofs' test for "sealed" by
  `:lock-act` is gone (F1).

**Edge cases.**
- Bytes that are no value of the form (F9). The gate keeps what the lock opened, whatever it is. P14.
- A value at the limit: sealed bytes are 65,536 at most (`declaration.clj:74-77`), so plain bytes are
  65,508 at most; a seal adds 28 (`seal.clj:32-33`).
- The lock of a current value can no longer be found from its fact's row. It can be found from the
  act in the depot, which holds each fact's `:lock-id`. Rule 19's line on a rebuild says "placed from
  the fact's row that names it" (the store's page, `:498-501`): that line is replaced. No code reads
  a lock from a fact's row today (F1's search of `:lock-act`).
- "One copy of a lock's bytes", in a microbatch layer. The write does not rest on the fact's row: the
  whole row of a used lock is sent to the task of the value's entity from the act's own fact
  (`batch.clj:582-599`). So the write stays as built. What rested on the row is the way back. The
  store's page says "the bytes of a lock that seals a value are read beside the value" (`:411-413`).
  To read them one needs the lock's key and the value's task. The fact's row gave the key until now.
  The home's row of the lock names the fact by its act's uuid and its index, and not by its entity
  (`rows.clj:217-224`), so it does not give the task. After A1 the one row with a used lock's bytes
  is found only through the act in the depot. No code of today reads it:
  `locks-of-request` leaves the bytes of a used lock out (`rows.clj:259-270`). It bears on the other
  sheet, where the log's copy still needs its lock; the receipt should name it.
- "A forget destroys the lock and also removes the row" (the sheet, `:260`) is not this build.

**Concurrency.** None new. **Latency.** No read more and no row more: the same row holds other bytes,
28 fewer for the value, and no `:lock-act` and `:lock-index`. **Growth.** One row for each place, as
today. **What the contract leaves unsaid.** How the plain bytes reach the row (L4), which is the
plan's. What a read gives for bytes that are no value (P14).

### A2. The writer with each fact (entry 7; R4)

**What must hold.** Each fact's row holds who wrote it and the permission that the act named. Both are
parts of the act in its one form (`act.clj:144-146`), in hand wherever a row is made. "A read that
does not ask for them is sent nothing of them."

Both gates: the same, since both make a row from the act with one function (`decision.clj:362`). In
a microbatch layer a row that every task holds carries them on every task.

**States that it meets.**
- The who is the holder of the permission: the common case.
- The who is not the holder. One case exists: the system makes a personal layer for its person, "that
  act's `:who` is `system` and its permission is the person's" (`first_act.clj:29-33`). So the two
  parts are not one fact said twice, and a read that asks who wrote a thing can get either.
- A fact under a store key: its row holds them too. The contract says "A fact's row", of every fact.
- A retract: its row holds them. Who took a thing away is then within reach.

**Invariants.**
- "Who wrote it" is the act's `:who` as the act claims it. A gate cannot check it: "`:who` is a
  claim" (the store's page, `:139-142`). Until sign-in is built, a read gives a claim.
- The permission is the one that stood for the act, since (d) passed. For a making act it is the one
  that the act itself wrote.
- R4 holds for what a caller is handed. Whether it also holds for what Rama sends the runtime is P15.

**Edge cases.** The two parts are of two classes and one is a collection (F5); P22. Their size: an
id prints to 256 characters at most (`declaration.clj:84-89`), and a permission holds 12 ids at most,
so the two add about 40 to 80 bytes to a common row and about 3.4 KB at the limit. Derived.

**Latency.** No read more. **Growth.** Bytes in every fact's row, for good. **Unsaid.** Nothing asks
for the session, the act that caused it, or what it stood on: the whole act from a fact is not in this
build (entry 7).

### A3. The layer's setting (entry 1)

**What must hold.** One more store key. Its fact sits on the layer's own entity. Its value is one of
two: Sid's words are "members" and "everyone". It takes a second write. A fact under it keeps a plain
value on arrival, and the door leaves it plain.

**Where it must pass, as the code stands.**
- The door tells a store key by `declaration/store-key?` and leaves its value plain
  (`additions.clj:208-209`). A key in the store's namespace that is not in `store-keys` is sealed by
  the door and refused by a gate `:store-key-not-known` (`additions.clj:179-181`;
  `declaration.clj:375-378`). So the key is in `store-keys`, the one table that the door and both
  gates read.
- `store-keys` names for each key the shape of its value, the entity that it sits on, and whether it
  is a setting (`declaration.clj:102-139`). A setting is "a key whose fact a gate takes only on the
  layer's own entity"; check (f) refuses it elsewhere `:setting-not-on-the-layer`
  (`decision.clj:188-194`). That is decision 8 of 7 October (F8 C), Sid's.
- The value's shape is held by a rule of the declaration with a reason word of its own, as
  `:depot-fact-holds-a-depot-word` is (`declaration.clj:224`, `:385-386`). A value that is neither of
  the two words is refused there. A sealed fact under the key is refused `:store-key-sealed`
  (`:380-383`).

| | The streaming gate | The microbatch gate |
|---|---|---|
| A first write | One row at the place layer · layer · the key, on the layer's task. | One row. On which task is P18: the task of the layer's entity as any fact, or every task as a depot fact is. |
| A second write | The row is written again. With no `:replaces` no row is read (rule 2). With `:replaces` it is held to check (e). | The same. If every task holds the row, each copy is written again inside one attempt, and the act goes to the meeting task as a give does (`batch.clj:513-543`). |
| Two writes at once | The depot's order; the later stays. | The order of the turns; the later turn's row is the one written (`batch.clj:837-848`). |
| A retract | Refused `:store-key-retract-not-built` as the code stands (L9). P21. | The same. |

**States that it meets.** No fact yet: P6 says what a read takes then. "members". "everyone". Written
again with the same word: accepted, a new row with a new stamp and writer.

**Invariants.**
- The gate does not read the setting to decide an act. Who may act is the permission alone.
- A read finds the setting from the layer's id alone, with one read on one task.
- Who may write it: the kept answer is "whoever may act in the layer" (P5). As the code stands that
  is what falls out: any act that passes its standing may write the fact.

**Edge cases.** A setting fact in the making act: the form of a making act has exactly two facts
(F7), so today it is refused `:layer-not-made`. A setting for a layer of the other gate: the act is
refused as any act into that layer is. The base's setting is a row of a microbatch layer, written a
second time: it is the first row of today's store that both is replaced and decides what a read may
show. See A7.

**Latency.** For a gate: nothing more for an act that writes no setting. **Growth.** One row for each
layer; one depot record for each write, for good.

### A4. What a permission allows (entry 1)

**What must hold.** A permission says what it allows: see, or see and act. A permission that was kept
without the part reads as "see and act". "A gate refuses an act under a permission that says 'see'".
A permission's place still takes one write (R7).

**What "an act needs 'act'" meets, case by case.**

| The act | Today | With A4 |
|---|---|---|
| An act of a holder whose permission allows see and act, or was kept without the part | Passes (d). | Passes. |
| An act of a holder whose permission says "see" | Passes (d). | Refused, with a recorded answer and a new reason word. The permission stands, so today's two words of (d) do not fit. Its place in the order of the checks is P3. |
| A give: an act with a fact under `:softland.store/permission` | The giver's own permission must stand, and the given one must be straight beneath it (`decision.clj:196-210`). | The giver needs "act". The given permission says what it allows; the giver chooses. A holder of "see" can give nothing. |
| A making act | Not held to (d): it names the permission that it writes (`decision.clj:149-150`). | It names a permission that does not stand yet, so "under a permission that says see" has no row to read. If its own permission says "see", the layer is made and nobody can ever act in it: its one holder may not act, a permission's place takes one write, and nothing raises a permission. P3. |
| The door's request for locks | An act under the maker's who and permission (F6). | For a holder of "see" it is refused. Her value act then ends at the door with `:have-its-request-accepted` and is never appended; her act with no value, a retract, reaches the gate and is refused there. The same is true today of a who with no permission (the store's page, `:197-203`). |
| An act of `:system` | "`:system` is a who like any other" (`door.clj:79`). | The same rule. Its permission in the base, written by the base's making act, is one of the two cases above. |

| | The streaming gate | The microbatch gate |
|---|---|---|
| Where it is known | The row at the place of `:who`'s permission, which (d) reads already (`streaming_gate.clj:183-184`). No read more, unless P2's third option. | The same row, from the task's own copy, in step 1 (`microbatch_gate.clj:265-266`). |
| How final a "see" is | — | A row at a permission's place never changes (`batch.clj:197-199`). So a refusal for "see" is final in step 1, as a refusal for standing is: the act asks for its answer only. |

**Invariants.**
- No act can name a permission that allows more than the row holds. Today that is held by the
  comparison of bytes of (d) (L11). Where the part lives decides whether that comparison still does
  it: P2.
- A given permission cannot allow more than its giver's, since only a holder of "act" can give.
- A holder has one permission in a layer (P4), so she cannot hold "see" from one giver and "act" from
  another; and "see" stays "see" (P7).
- The code carries both forms: with the part, and without it.

**Edge cases.** A permission whose part is a third word: refused by the declaration, with a word. A
permission in an act that says "see and act" where the row says "see": refused, as a permission that
does not stand. Permissions above the act's own are not read today (the store's page, `:296-299`): a
permission above that says "see" cannot have a permission beneath it, because its holder could not
give.

**Latency.** No read more with P2's first two options. **Growth.** A few bytes in each permission's
row, and in each act if the part is in the permission that an act names.

### A5. What a layer builds on (entry 2)

**What must hold.** One more store key. Its fact sits on the layer's own entity and holds an ordered
list of layers. "It takes a second write." "A layer builds on nothing until a fact says so."

Where it must pass is as A3: `store-keys`, a shape for its value, a rule with a word, a setting by
check (f), plain through the door. Both gates as in A3's table.

**What the value's shape must hold, each implied.**
- Each element is an id that a row's key can be made of. The read path makes a key from each. Rule 20
  holds each id to 256 characters "before a row key is built from an id"; the rule
  `:ids-within-the-limit` reads inside a permission fact's value today and inside no other value
  (`declaration.clj:344-350`).
- The list has an order, and the order is the writer's (entry 2: "the one who writes the fact states
  the order").
- How many layers a list can hold is a number of the build, as the five of `declaration.clj:62-89`
  are. Unsaid.
- An empty list says "nothing", so a layer can go back to building on nothing with a second write
  and no retract.

**States that it meets.** No fact. A list. A list written again: shorter, longer, in another order.
A list that names the layer itself. A list that names one layer twice. A list that names a layer that
is not made, or a layer that the writer may not see, or a layer of the other depot. The gate checks
none of these as the code stands, and the contract asks for no such check. Two lists that name each
other. See P9.

**Invariants.** The gate does not read the list to decide an act. A read finds it from the layer's id
alone, with one read on one task.

**Growth.** One row for each layer; a depot record for each write, for good.

### A6. A marked key, and one more order (entry 6)

**What must hold.** A fact on the layer marks a key. For a marked key the gates keep each fact in one
more order, by value, "so that 'what points at this thing' is a run of neighbouring rows". The mark
takes a second write, "since a mark can be taken off".

**What that implies of a gate's write, in words of what must be true.**
- For a layer, a key K that is marked and a thing T: the rows that answer "what points at T under K"
  are one run on one task, found from the layer, K and T alone. A push covers one task (L26), and R3
  forbids a watch of a whole table.
- The run holds a place exactly when the current fact of that place, under K, has T as its value. So
  when a fact under K replaces a fact, or is written over one with no `:replaces`, or is a retract,
  the entry of the value before goes out of the other order in the same event or the same attempt.
  To do that the gate must know the row before. Today a fact that names nothing to replace "reads no
  row" (rule 2; `decision.clj:162-176`). So a fact under a marked key costs one read of its place
  that it does not cost today.
- The run for T holds no place whose value only begins with T's text. `place-key` rests on "No
  printed text of an id holds a space" (`rows.clj:56-57`). The one text of a value can hold a space
  and any character: a string, a vector (`text.clj:20-36`). And a UUID has two texts (F8).
- "Points at" is the sheet's word: "a fact points at a thing when its value is that thing's id"
  (the sheet, `:281`). So a value that is a set or a vector of ids points at nothing by that word.
- The other order's rows are written in the same event or attempt as the fact's row (G2). A reader
  never sees a fact in one order and not in the other for longer than Rama takes to show one task.
- A gate must know, where it writes a fact, whether the fact's key is marked in the act's layer
  (G4).

| | The streaming gate | The microbatch gate |
|---|---|---|
| Knowing the marks | A read on the layer's task, in the event. At the least one read more for each act that holds a fact under a plain key, also when no key of the layer is marked. | On the task where the act is decided. That is the task of the act's first entity, any task. So the marks are within reach of every task without a hop for each act (G4), as a permission is. P18. |
| The row of the other order | On the layer's task, with all the layer's rows. | On one task for one T, found from the layer, K and T. That is not the task of the fact's entity, so the row travels inside the attempt, as a lock's home row does today. |
| A mark and a fact under its key, together | The depot's order says which came first. | Two acts of one batch on two tasks. Whether the fact is kept in the other order rests on the order of their turns, so the two "meet" in the store's sense (`batch.clj:27-30`). P18. |
| A second attempt | — | The other order's rows follow from the fact rows, so they are the same when those are. |

**States that it meets.** See the matrix of a marked key below. In short: a key that is not
marked; a key that is marked; a mark that is taken off; a fact under the key that was written before
the mark (P20); a fact whose value is no thing's id (P10).

**Edge cases.**
- A fact under a marked key with a value of 65,508 bytes. The other order is "by value". A row's key
  is made of printed texts held to 256 characters each (rule 20). What a gate does with a value over
  whatever limit the other order has is P10.
- The key that is marked is a store key, such as `:softland.store/permission`. Then "what points at
  this permission" would be a run. Unsaid; the kept reading is that a mark is for a tool's key.
- A thing T that many facts point at: one run that grows without a bound. A canvas is the sheet's
  case. The skill: a collection that grows past about 100 elements must not be loaded whole
  (`SKILL.md:59`).
- A fact that points at a thing of another layer: an id names no layer, and "a fact has no layer of
  its own" (decision 6 of 6 October). The run is in the layer of the fact, not of the thing.

**Latency, by the store's own arithmetic, derived and not measured.** The store's page: "4 reads give
480 to 770 acts a second on one task" by the cost of a cold read (`:504-507`). One accepted act with
one value under a marked key reads at least 2 rows more: the marks, and the row before. It writes 1
or 2 rows more: the new entry, and the old one out. By the same arithmetic 6 reads give 320 to 513
acts a second on one task, and 5 reads, for an act under a key that is not marked, 384 to 616. The
skill's rule is that work on the write path is paid once and a read's seeks are paid at each read
(`SKILL.md:41`). C measures it.

**Growth.** One row of the other order for each place under a marked key whose current fact has a
value. Places are never removed, so it grows with the places under marked keys, for good. What a
mark that is taken off leaves is P20.

### A7. The base, for a reader (entry 8)

**What the contract asks.** "A read shows no fact of a batch that could still be tried again, and a
place that holds a fact never gives nothing. The papers name a repair that gives both: keep each
batch's clock, so that a second try comes out the same (`store-module/PROGRESS.md`, item 24). It is
tried first, with a run. If the run does not hold, the row before is kept beside the new one."
Sid's page, entry 8: the two things "are a session's, and he struck neither".

**What today's code does.**
- Rama shows a batch task by task, and can run the batch again after one task has shown its part. A
  second attempt reads the clock again and makes other lock bytes (`microbatch_gate.clj:84-87`,
  `:285`, `:143-151`).
- So each task holds `:closed-through`. Each run of the gate writes it on every task with the gate's
  last stamp as it was before the run (`microbatch_gate.clj:291-294`; `batch.clj:353-360`). A batch
  is closed one short run after it.
- The three reads give a row of a microbatch layer only when its stamp is not later than
  `:closed-through` (`door_reads.clj:102-107`, `:134-145`, `:160-165`; `rows.clj:248-257`). The
  streaming gate holds the base's depot row to the same test (`streaming_gate.clj:112-124`).
- A select or a push that goes straight to the table is not held to it (L15).
- What is the same on every attempt: the things of the batch, the turns, which act has an act-id,
  which acts meet, the order (`microbatch_gate.clj:98-100`; `batch.clj:118-127`). The count of turns
  is in a PState because "Rama sets a PState back when an attempt of a batch fails"
  (`tables.clj:34-36`).
- What differs on a second attempt, as the store's page lists it: the clock, and with it each stamp;
  a new lock's bytes (rule 5, `:438-443`); and two words: "the 10-second rule reads the clock, for an
  act that carries a stamp at the edge of the 10 seconds; and the word for a layer of the other gate
  follows what that gate had made by then" (`:190-196`).

**What would have to be the same on a second attempt, for a reader of facts, after A1.** Derived, from
the lines above. After A1 a fact's row holds the plain value, the fact that wrote it, the stamp, the
who and the permission, and no lock. Field by field:

| What a reader of a fact gets | Is it the same on a second attempt today? | What it rests on |
|---|---|---|
| Whether the act was accepted, so whether the row is there | Not always. The 10-second rule reads the clock. An act that carries a stamp at the edge can be refused on one attempt and accepted on the next. | The clock of the batch. |
| Which of two acts on one place wins | Yes. | The turn (`batch.clj:118-127`). |
| The plain value | Yes. | The sealed bytes of the act, and a lock of a closed batch (A1's table). |
| The fact that wrote the row, the who, the permission | Yes. | The act. |
| The stamp | No. | The clock of the batch, and the gate's last stamp. The last stamp is in a PState, so it is set back; the clock is read again (`microbatch_gate.clj:282-290`). |
| The lock's bytes | No, and after A1 a reader of facts does not get them. | A random source. Only the door reads them, by a read that is held to closing. |
| The row of the other order (A6) | As the fact's row. | The fact's row. |
| The reason word of a refusal | Not always: the word for a layer of the other gate. | The other gate's record at that moment. A reader of facts does not get it; a reader of an answer does. |

So for a reader of facts, one thing differs between attempts: the batch's clock. It moves the stamp
of every row, and at the edge of the 10 seconds it moves whether an act is accepted. If each attempt
of a batch used one clock reading, each fact's row would be the same on each attempt. Derived, not
run. Three things would still differ, and none is in a fact's row: a new lock's bytes, the word for a
layer of the other gate, and the bytes drawn for the check of a lock that is not made yet
(`microbatch_gate.clj:386`).

**The two things that the contract asks, against the two ways.**

| | "shows no fact of a batch that could still be tried again" | "a place that holds a fact never gives nothing" |
|---|---|---|
| The repair: each attempt of a batch uses one clock | Does not hold as written. A read then shows a fact of a batch that is not closed. What holds is the weaker thing: it shows no fact that a second try could change. P16. | Holds: a read gives the place's one row, the newest. |
| The row before, kept beside the new one | Holds: a read gives the newest row whose stamp is not later than `:closed-through`. | Holds, if the row before is the place's row as of the last closed batch. When a batch's rows show on a task, that task's `:closed-through` is the last stamp of the batch before (`microbatch_gate.clj:291-294`), so the row of the batch before is closed. Two acts of one batch on one place must not leave the earlier turn's row as "the row before". Derived. |

**What each way asks of a live read.** With the repair, a push on the facts gives each new row as its
task shows it, and the read path shows it. With the row before, what a reader may see changes when
`:closed-through` moves, and the row does not change then. So the read path must follow
`:closed-through` of each task that it reads, live. Every batch with things writes it on every task
(the store's page, `:393-395`), so each live read of the base gets that push for each batch.

**What makes it matter most.** Three kinds of fact of the base decide what a read may show, and this
build is the first to read them as facts: the asker's permission, the layer's setting, and the list.
The setting takes a second write. If a read of the base's setting were held to closing with one row
for a place, then for one run after the setting is written again it would find no row, and would
take the start value, which is "everyone" for the base (P6). So a base that is "members" would read
as "everyone" for that run. Derived. The contract's second sentence closes that, by either way.

**Unsaid.** Whether the door's three reads stay held to closing (P16). What a reader sees on a task
between a first attempt that showed its part and the second attempt (P17). Whether the first is tried
"with a run" that can make a batch fail after one task has shown its part: the store's own page says
"No proof reaches: a second attempt of a real batch, a commit that fails after one task has shown its
part" (`store-module/PROGRESS.md:175-180`).

### What holds for every operation of half B

- **H1. One reader.** "No tool reads Rama by itself. The read path is the one reader of facts in the
  runtime" (R1). Nothing in Rama holds that: the tables are open to any client (L2). It is a rule of
  the code, as D8 was. P26.
- **H2. Nothing of our own beside the push.** "Nothing is shown before it is in the tables. No
  optimistic update, and no store of our own that we keep fresh beside Rama's push" (R2). Sid's
  ruling is on what the pushed value is: "yeah i also don't consider it a cache" (entry 3). So the
  value that Rama's push keeps level in the runtime's memory is allowed. Anything else that the read
  path would hold and keep fresh is what R2 tells the build to stop at.
- **H3. Each read names its task** (F3). No read and no push routes by a place's whole key.
- **H4. One value for one push, and never a whole table** (R3). A push watches one row, or one run of
  neighbouring rows of one table. A caller's query is data, so the read path must turn away, before
  any call to Rama, a query that it could only serve by watching a whole table or a whole collection
  that keeps its elements apart: "Rama does not refuse it cleanly, so the read path must." A run
  stays inside one layer: the right to see is of one layer (B2), and one task of a streaming gate
  holds the rows of many layers.
- **H5. A function in a path is in the module's code** (L30). The read path and the module share one
  classpath in this tree (`src-store/README.md:31-42`).
- **H6. The read path owns no durable state.** Its memory is the memory of one process and is empty
  at each start. Its handles are borrowed. The tree's rule: keep apart "durable state, the memory of
  one process, and a handle that is borrowed" (`src-store/CLAUDE.md:61`).
- **H7. It never reads a lock.** After A1 a value needs none. The store's page says of the door that
  it "reads only the locks of its own requests" (`:165-167`); that stays the only read of locks in
  the runtime.
- **H8. What a push calls runs on a thread of Rama's client.** "Every callback for a `proxy` call
  runs in the background on a dedicated thread from a thread pool. This guarantees diffs are
  processed in exactly the order in which they were sent back" (`reference/rama/docs/15-pstates.md:554`;
  read, not run). So the changes of one push come in order, and the changes of two pushes come in no
  order that anything states. A read through a list of layers is several pushes.
- **H9. The family's manners.** The door never throws for what a maker gave, tests a thing that is no
  id before it calls, and makes no round trip for a thing that cannot have an answer
  (`door.clj:32-38`, `:220-224`). The contract names the door as "the precedent of the same family".
  No clause of the contract asks the same of the read path in words; it is implied by the family.

### B0. Opening the read path

The contract names no part for it. It is implied: a client of the module holds handles, and the door
is opened on a cluster handle and the module's name and "borrows five handles of Rama's client"
(`door.clj:40-42`).

**What must hold.** The read path can reach the facts of both gates, the rows of the other order, and
whatever A7's way needs. It is opened once for a process, or it says what two of it cost, as the door
does ("Two opened doors are two sets of stocks", `door.clj:50-52`): two read paths in one process
would not share a live read, so B5 would not hold between them. **States.** Not opened. Opened. The
cluster gone under it: each call ends in a word or a thrown thing that the caller can tell from "no
fact" (H9). **Unsaid.** Whether the read path and the door are opened together. Whether anything
closes it; nothing closes the door (`door.clj:42`).

### B1. A query written as data (entry 6)

"Its first words: a place; an entity, or any run of neighbouring places; what points at a thing.
Each is asked once, or kept live." Sid's page: "A tool hands the read path a query written as data.
Nobody decides what a tool asks. What is decided is the few words that a query is made of. A word
that does not exist yet ... is added to the store once, in code."

**What must hold for every word.**
- A query is data: a caller can build it, hold it and compare it without the read path's code. Two
  askers "look at the same thing" (B5) when their queries are the same thing in one layer; so "the
  same" must be decidable from the data.
- A query says: which word; what it is about; through which layers, or from which layer (B3); who
  asks (B2); once or live; and which parts of a row it asks for (R4). The form is the builder's.
- A word that the read path does not know is turned away with a word of its own, with no call to
  Rama. So is a query whose parts are no ids within the limit (H9).
- The answer to a word tells three states of a place apart: no layer of the list holds a fact there;
  the nearest layer that holds one holds a value; the nearest layer that holds one holds a retract.
  B3 needs the third: "also when that fact says 'nothing here'". P14.

**The word "a place".** In each layer of the list, the row at the place of the layer, the entity and
the key. One row in each layer. Once: one read for each layer. Live: one push for each layer.

**The word "an entity, or any run of neighbouring places".** In each layer, the rows from one key of
the table to another. One entity is the run of L22. The table's order is the order of the printed
texts: layer, then entity, then key (L12). So a run can be: some keys of one entity; all keys of one
entity; the entities between two entity ids; a whole layer. A tool chose none of these orders: the
entities of a tool are neighbours only if their ids print as neighbours. Sid's page says "one entity,
or any run of neighbouring keys"; the contract says "any run of neighbouring places". P29.

**The word "what points at a thing".** In each layer, for a key that the layer marks and a thing: the
places whose current fact under that key has the thing as its value (A6). For a key that the layer
does not mark, the word cannot be served from that layer: the read path says so for that layer and
never reads the whole layer for it. The same key can be marked in one layer of the list and not in
another.

**States that each word meets, in one layer.**

| The state | A place | An entity or a run | What points at a thing |
|---|---|---|---|
| The layer is not made | No row. | No row. | No row. Whether the read says "not made" is P8. |
| The asker may not see the layer | Left out; the read says so (B3). | The same. | The same. |
| Nothing there | No row: go on to the next layer. | An empty run. Live: the first place comes when it is written (the probe's F3). | An empty run. |
| A fact with a value | The row. | Each row of the run. | The place is in the run. |
| A retract | The row, with no value: this layer wins, and says "nothing here". | The same, place by place. | The place is not in this layer's run. Whether it then hides the same place in a farther layer is P12. |
| The key is not marked in this layer | — | — | Not served from this layer. |
| A row of a batch that is not closed (a microbatch layer) | A7. | A7, place by place. | A7. |
| The store's own facts at the place | F4, P11. | F4, P11. | — |
| A very large run | — | The builder's: "what a read does when a run is very large". It must not hold the gate's task for the whole run (`SKILL.md:26`). P27. | The same. |

**Once.** The value now, from each layer, put together (B3). The layers are read on up to as many
tasks as there are layers, and no query of the store stands between (the contract's picture). So the
parts of one answer are read at moments that differ. P13.

**Live.** "gets the value now and each change after." The value now is what "once" gives. A change
after is a change of what the read gives, put together: B4.

**Latency.** Under "Latency and growth". **Throughput.** Driven by how many askers look at how many things. A read once
costs its seeks each time it is asked; a live read costs its opening once and then a diff for each
change in its run. **Concurrency.** Many callers call at once, on their own threads, as makers call
the door.

### B2. Who asks, and may she see (entry 1)

"The read path reads the layer's setting and the asker's permission. Until sign-in is built, who asks
is what the caller says."

**What must hold.** An asker may see a layer when the layer's setting is "everyone", or when she holds
a permission in the layer. Sid's page: "'members' is whoever holds a permission in the layer, since
the store holds nothing else that the word could mean; 'everyone' takes in a person who is not signed
in" (a session's reading, which he did not strike). Any permission lets her see: one that says "see",
one that says "see and act", one that was kept without the part.

**What it reads, as the code stands.** The setting: the row at the place of the layer's own entity
under the setting's key. The permission: the row at the place layer · asker ·
`:softland.store/permission`, the place that a gate reads for an act of hers (`decision.clj:89-94`).
In a streaming layer both are on the layer's task. In a microbatch layer every task holds the
permission; where the setting is, is P18.

**States that it meets.**

| The setting | The asker's permission in the layer | May she see? |
|---|---|---|
| "everyone" | Any, or none | Yes. Her permission need not be read. |
| "members" | "see"; "see and act"; kept without the part | Yes. |
| "members" | None | No. |
| No fact | Any | The start value decides: P6. Kept: a new layer is "members", the base is "everyone". |
| The layer is not made | None can be | Nothing to see. P8. |
| A row of a batch that is not closed | The same question as A7: a give that a second attempt could refuse must not open a read. | |

**Invariants.**
- The check is made when a read is asked, once or live. For a live read it holds for as long as the
  read is open (B4).
- A read is never given a row of a layer that the asker may not see, in any part: not its value, not
  that a place holds a fact, not how many places a run holds. What it is told of such a layer is
  that it was left out (B3, P8).
- Who asks is a claim until sign-in is built, as `:who` is in an act (the store's page, `:139-142`).
  The read path cannot check it and does not.

**Edge cases.** An asker that is no id: P31. An asker who is not signed in sees a layer whose setting
is "everyone", and so the base (K14 of the queue, half of it). `:system`: "a who like any other"
(`door.clj:79`); it sees what its permissions and the settings let it see. The asker of a read is not
the who of any act: a read writes nothing and keeps nothing ("this build keeps no read"). A holder's
permission is a fact on her id as an entity; an asker who may see a layer can read who its members
are by reading those entities (F4, P11).

**Latency.** At the least one seek for the setting, and one more for the permission when the setting
is not "everyone". On the layer's task they can share a round trip with the read itself only if one
call can carry them; the contract's picture has no query of the store. See "Latency and growth".

### B3. Which layers, and putting them together (entry 2)

"The list that the layer gives, or the view's own. A layer of the list that the asker may not see is
left out, and the read says that it left one out. For each place the nearest layer that holds a fact
wins, also when that fact says 'nothing here'."

**What must hold.**
- A read is given a list of layers, or a layer to start from. Sid's page: "a read is given a list, or
  a layer to walk the list from, and where a read starts and where a person's acts go are two
  choices of the view" (a session's, not put to him one by one).
- From a layer: the layer itself is nearest, then the layers of its list in the order of the fact
  (A5). A layer with no such fact gives the layer alone.
- From a view's list: the layers in the view's order.
- Each layer of the list is held to B2 for this asker. One that she may not see is left out, and the
  answer says that one was left out.
- For each place: the row of the nearest layer that holds a row. A row with no value is a row.
- The answer says, for each place, which layer won, or the caller cannot tell whose fact she sees.
  Unsaid in the contract; implied by "nearest wins", since a writer must know which layer to replace
  a fact in, and `:replaces` names a fact by an act-id that holds its layer (decision 5 of
  6 October).

**States that it meets.** A list of one layer: no putting together. A list in which two layers are
of two depots: one read of each gate's table. A list that names a layer twice: the nearer one counts.
A list that names a layer which is not made. A layer whose own list changes while a read through it
is open (P23). A layer of the list that she may not see now and may see later (P23).

**Edge cases.**
- How far a read walks: the list of the start layer only, or on through the lists of those layers
  (P9). The picture on the sheet is `:alice-notes → :team → the base`.
- "What points at a thing" through a list (P12).
- A run through a list: the places of the run in all layers, put together place by place. The run's
  ends are the same entity and key in each layer; only the layer's part of each row's key differs.
- A place whose nearest row is in the base and is of a batch that is not closed (A7).

**Latency.** One read of the list's fact when a layer gives the list; then each layer's reads, which
can go to their tasks at once. **Concurrency.** The layers answer in no order (H8).

### B4. Live (entry 3)

"A live read is Rama's push, one for each layer of the list. What makes a read rightful is itself a
fact: the asker's permission, and now the layer's setting, which can be written again. The read path
keeps a live read on each, and ends the reads that are no longer rightful."

**What must hold.**
- A live read gives the value now, and then each change of it. "The value" is what B3 puts together,
  so a change is a change of that: a new row in a farther layer at a place where a nearer layer
  holds a row changes nothing that the asker sees.
- A screen is shown the newest state (R5). The read path promises no caller every state between: "a
  reader can be left out of one change and is then sent the value anew" (the sheet, `:37`; Rama's
  page says it of streaming tables, `reference/rama/docs/15-pstates.md:1230`; read, not run). When
  Rama sends a value anew, the read path treats it as a whole new value and not as one change.
- For each layer of a live read, and for this asker, the read path follows the two facts of B2 live.
  When they no longer let her see the layer, her read of that layer ends, and nothing of that layer
  is given to her after the read path has learned it.
- How soon: as late as a push is, and no sooner. Rama's two pushes, of the setting and of the facts,
  come in no stated order (H8). So a fact that was written after the setting went to "members" can
  reach the read path before the setting's change does. C measures "how late a push is".
- A live read that ends tells its asker that it ended. It never stays looking open while nothing can
  come.

**What can end a live read, each implied.**

| The cause | Source |
|---|---|
| The asker leaves | B5. |
| The setting is written again, to "members", and she holds no permission | B4. |
| Her permission ends | Not built: "nothing ends a permission". The sheet: "The read path follows a permission live, so it follows an end whenever one exists" (`:435`). The room is left; no case can reach it in this build. |
| Rama ends the push after too many errors | F11. Read, not run. |
| The runtime's process ends | H6. Every live read is gone. Rama clears a push that sends no heartbeat (`reference/rama/docs/15-pstates.md:728`; read, not run). |
| The cluster is gone, or the task's leader moves | Rama's page says that a push "will resync from scratch" when its heartbeat finds no subscription (`15-pstates.md:720`; read, not run). Not run by the probe. |

**States of a live read.** In the matrix below: opening; open; no longer rightful; ended; its
last asker left.

**Edge cases.** A layer that was left out at the opening and that she may see later; a layer's list
that is written again under an open read; a layer that is made after the read opened: P23. Whether
the end of one layer's read ends the whole read or drops that layer: P24. A change of the setting
from "members" to "everyone" ends nothing; it can let a layer in (P23). A live read of the base under
either way of A7.

**Latency.** The opening costs what "once" costs. After it, each change costs Rama the work of the
push, and for a streaming table that work "is done with the write, on the gate's own thread, up to a
time limit" (the sheet, `:37`). So open live reads on a layer are a cost of each write of its gate.
C measures it. P27.

### B5. One live read, shared (entry 3)

"Those who look at the same thing share one push. It ends when the last one leaves. Sid, 11 October:
'i also don't consider it a cache'."

**What must hold.**
- For one thing in one layer there is one push, however many askers look at it. The unit that is
  shared is the push of one word in one layer: two askers whose lists differ share the push of each
  layer that both lists hold.
- The unit that is rightful is one asker in one layer (B2). So one asker's read of a layer can end
  while the push goes on for the others.
- A second asker of an open push gets the value now from what the push holds in memory, with no read
  of the facts from Rama. Her right to see is still read for her.
- R4 holds for each asker. A push carries what it carries for all of them; an asker who did not ask
  for the writer is handed nothing of it.
- The push ends when its last asker leaves, and frees what it held in the runtime and in Rama.
  Rama's page: "It's best to call `close` as soon as a `ProxyState` is no longer needed"
  (`reference/rama/docs/15-pstates.md:726`; read, not run).
- The pushes that follow the setting of a layer are shared in the same way. The push that follows
  one asker's permission in one layer is shared by her own reads of that layer.

**States.** No push for the thing. One asker. Many. The last one leaving while a new one comes: the
new one must not be handed a push that is closing, and two who come at once must not open two.

**Edge cases.** An asker that never says that she left: P25. Two askers of one thing, one of whom may
no longer see the layer. A thing that is "the same" in two layers is two pushes. A run and a place
inside it are two things by any plain reading of "the same thing"; whether a place can be served
from an open run is the builder's.

**Growth.** The runtime holds, for each open push, every row of its run (the probe:
"a push over 20,000 places keeps 20,000 rows in the watcher's memory", `RESULTS.md:64-65`). Sharing
bounds that by the count of things looked at, not of askers. See "Latency and growth".

## Entity State × Write Matrix

Six things. For each: its states, then for each state and write, each related read. The reads, named
once and used in every block:

- **place**: the word "a place", once, by an asker who may see the layer.
- **run**: the word "an entity or a run", once.
- **points**: the word "what points at a thing", once.
- **live**: a live read that is open on the thing already.
- **may-see**: the read path's check of B2 for an asker.
- **gate**: what the gate's next act of this layer meets.
- **door**: the door's three reads: an act's answer, a request's locks, a layer's depot.

Where a line says "in the base", it holds for any microbatch layer. Where it says "A7", what a read
gives rests on the way that the build session rules for A7 (P16).

### 1. A layer

States: not made; made as a streaming layer; made as a microbatch layer (the base). Beside that: its
setting is absent, "members" or "everyone"; its list is absent or a list.

```
Not made x a right making act (accepted)
  - place, run, points: nothing of a tool's. The layer holds two rows: its depot on the layer's own
    entity, and the first permission on its holder. A run of either entity gives that row (F4, P11).
  - may-see: the first holder, yes. Any other asker: by the start value (P6). Kept: no for a new
    layer, yes for the base.
  - live: a read that was opened on the layer while it was not made: P23.
  - gate: the next act needs a permission that stands; a second making act is refused.
  - door: the layer's depot is readable; in the base one run later, when the batch is closed.
Not made x any other act (a fact, a setting, a list, a mark, a making act with a third fact)
  - Refused :layer-not-made, or :layer-belongs-to-the-other-gate. No row. Every read as before.
  - door: the act's answer says refused.
Made, setting absent x a setting "members"
  - may-see: only holders. For a new layer nothing changes (kept start: "members"). For the base the
    start was "everyone", so askers with no permission in the base may no longer see it.
  - live: the base's open reads of askers with no permission end (B4).
  - place, run: the row of the setting is on the layer's own entity (F4).
  - gate: nothing changes. A gate does not read the setting.
Made, setting absent or "members" x a setting "everyone"
  - may-see: every asker, signed in or not.
  - live: open reads go on. An asker whose read left this layer out: P23.
  - place, run, points: unchanged.
Made, setting "everyone" x a setting "members"
  - may-see: only holders.
  - live: reads of askers with no permission end, as late as a push is (B4).
  - place, run, points, asked again by such an asker: left out, and the read says so.
  - In the base: A7. A read that found no row for the setting for one run would take "everyone".
Made x a setting with the same word again
  - Accepted. A new row: other stamp, other writer. may-see: unchanged. live: the push of the
    setting sends a change of the row; no read ends.
Made x a setting fact with another word, or sealed, or on another entity
  - Refused by the declaration or by check (f). Every read as before.
Made, list absent x a list
  - place, run, points asked "from this layer" after it: through the layer, then the list.
  - asked with a view's own list: unchanged.
  - live "from this layer", opened before: P23.
  - gate: nothing changes.
Made, a list x a list written again (longer, shorter, another order, empty)
  - As above. An empty list: the layer alone.
Made x a retract of the setting, the list or a mark
  - Refused as the code stands (L9). P21.
```

### 2. A permission: one holder in one layer

States: none; kept without the part; "see"; "see and act".

```
None x a give that says "see" (by a holder of "act", straight beneath her own)
  - may-see for the holder: yes, whatever the setting.
  - live: her reads that left this layer out: P23. The push that follows her permission's place sends
    the new row.
  - gate, her next act: refused, she may not act (A4). Her request for locks: refused; her value act
    ends at the door (F6).
  - run of her id as an entity in this layer: her permission's row (F4).
  - In the base: the row is on every task; a read that goes to any task finds it. A7 for the run
    before the batch is closed.
None x a give that says "see and act", or a permission in the form without the part
  - may-see: yes. gate: her acts pass (d) and A4. She can give beneath it.
"see" x a give to the same holder that says "see and act"
  - Refused :store-key-takes-one-write (L20). She stays "see". P7.
  - Every read as before.
Any x a give to the same holder by another giver
  - Refused, the same word. One permission for a holder in a layer (P4).
Any x a retract of the permission
  - Refused :ending-a-permission-not-decided. Every read as before. No live read ends by it.
None x a give by a holder of "see"
  - Refused (A4). No row. may-see for the one named: as before.
None x an act of the holder
  - Refused :permission-does-not-stand, as today. A read by her: by the setting alone.
```

### 3. A place in one layer

States: empty; a fact with a value; a retract; in a microbatch layer also "replaced in this batch",
which is the row of a batch that is not closed.

```
Empty x a fact (no :replaces)
  - place: the row. run: the row is in each run that covers the place.
  - live on the place: one change, the new row. live on a run: one change, the new place only (the
    probe's F2). A live run that was empty: its first place (F3).
  - through a list: this layer wins the place from every farther layer.
  - points: if the key is marked and the value is a thing's id, the place joins that thing's run.
  - gate: a later fact can name it in :replaces.
Empty x a fact that names something to replace
  - Refused :replaced-fact-not-current. No row. Every read as before.
Empty x a retract
  - Accepted (a check of the proof at module_proof.clj:696). The place holds a row with no value.
  - place: "nothing here", from this layer. That differs from "no row": through a list, a farther
    layer's fact at the place is now hidden. P14.
  - points: the place is in no run.
A fact x a fact with no :replaces
  - Accepted; it is never stale (rule 2). place: the new row. live: one change.
  - points, key marked: the place leaves the old value's run and joins the new value's run, in the
    same event or attempt (A6).
A fact x a fact that names the current fact
  - As above.
A fact x a fact that names a fact that is not current
  - Refused. Every read as before.
A fact x a retract
  - place: "nothing here". run: the place stays in the run, with no value. live: one change.
  - through a list: a farther layer's fact stays hidden.
  - points: the place leaves the value's run.
A retract x a fact
  - place: the row. As "empty x a fact", and the fact can name the retract in :replaces (rule 3).
Any x a fact with the same value again
  - A new row: other stamp, other fact that wrote it, perhaps another writer. live: one change of the
    row. points: the same run; the entry stays.
In the base: a fact x any accepted write
  - Until the batch is closed the place's one row is of a batch that is not closed. A7: either the
    read gives that row (the repair), or it gives the row before (the row before, kept).
  - It never gives nothing (the contract).
In the base: a place that is replaced in every batch
  - The same: a read always gives a row. With one row for a place and a read held to closing it
    would never give one (L14).
```

### 4. A marked key: one key in one layer

States: not marked; marked; the mark taken off. Beside that: facts under the key that were written
while it was not marked.

```
Not marked x a fact under the key
  - place, run: the row. points: not served for this key in this layer (B1).
  - gate: one row written, as today. Whether the gate still pays a read to learn that the key is
    not marked is in A6's latency.
Not marked x the mark
  - points for a thing, asked after: the facts under the key that are written from now on. The
    facts that were written before: P20. Until that is ruled, a read must not give a part of the
    truth as if it were the whole.
  - place, run: unchanged, but for the mark's own row (F4).
  - gate: each later fact under the key costs the reads and writes of A6.
Marked x a fact under the key, value a thing's id, place empty
  - points for that thing: the place is in the run. live points: one change, the new place.
Marked x a fact under the key that replaces a value T by a value U
  - points for T: the place is gone. points for U: the place is there. Both in the same event or
    attempt. live points for T: one change, the place removed.
Marked x a retract under the key
  - points for the value before: the place is gone.
Marked x a fact under the key whose value is no thing's id (a string, a number, a set, 60,000 bytes)
  - P10: refused; or kept in the facts table and in no run; or kept in a run by some text of it.
Marked x the mark taken off
  - points, asked after: not served for this key in this layer.
  - live points that are open: they can get no change after it; whether they end and what the asker
    is told is P24.
  - the rows of the other order: P20.
Mark taken off x the mark again
  - As "not marked x the mark". The facts that were written in between are facts written before the
    mark (P20).
Marked in one layer of a list, not in another
  - points through the list: served from the first, not from the second; the read says so (B1).
In the base: a mark and a fact under its key in one batch
  - Which came first is the order of their turns. P18.
```

### 5. A live read

States: opening; open; no longer rightful; ended; its last asker left.

```
Opening x Rama gives the first value
  - the asker: the value now, put together over the layers (B3). The read is open.
Opening x the asker may not see a layer of the list
  - the asker: the value of the other layers, and the word that one was left out (P8).
Opening x Rama cannot serve the path
  - The read path never asks for such a path (H4). If Rama still fails, the asker gets a word, in
    the time of a call and not after 100 seconds (F11).
Open x a write inside its run
  - the asker: the change, if it changes what she sees (B4). A second asker of the same thing: the
    same change.
Open x a write outside its run
  - Nothing (the probe's F2, Z3).
Open x Rama sends the value anew
  - the asker: the newest value as a whole. No promise of the states between (R5).
Open x the setting goes to "members", the asker holds no permission
  - No longer rightful. Her read of that layer ends as soon as the read path learns it. Other
    askers of the same push go on (B5).
Open x a give to an asker whose read left the layer out
  - P23.
Open x a second asker of the same thing
  - She gets the value now from the push's memory, after her own check of B2. One push (B5).
Open x the asker leaves, others stay
  - The push goes on. She gets nothing more.
Open x the last asker leaves
  - The push is closed in the runtime and in Rama. A new asker a moment later opens a new push and
    gets a fresh value now.
Open x the runtime's process ends
  - Every live read is gone with the process (H6). Rama clears the pushes that send no heartbeat.
    After a start, each caller asks again.
Open x Rama ends the push
  - Ended. Each asker is told (B4).
Ended x anything
  - The asker gets nothing more, and can ask again.
```

### 6. A batch of the microbatch gate

States, for the rows that a batch wrote on one task: visible on the task and not closed; closed;
and "tried again", when Rama runs the batch a second time after a task has shown its part.

```
Visible, not closed x (no further write)
  - door: an act's answer of this batch: "nothing yet". A request's locks: none. The base's depot:
    nothing. Each is held to closing, as built (door_reads.clj:102-107, :134-145, :160-165).
  - gate (streaming, asking whether the base is made): no, not yet (streaming_gate.clj:112-124).
  - place, run, points, live, in the base: A7. The repair: the new row. The row before: the row
    before.
  - may-see, where the asker's permission or the base's setting is of this batch: A7, the same way.
    With the row before, a give of this batch does not let her see yet.
  - one read on two tasks: one task can show the batch a few milliseconds before another ("One
    instant for two tasks", the store's page, :184-189). Closing does not change that.
Visible, not closed x the next run of the gate begins
  - The batch is closed: every task had made it visible, and Rama cannot run it again.
  - door: the answer, the locks and the depot are readable.
  - place, run, points, live: the new row, by either way.
Visible, not closed x Rama runs the batch again
  - Today: the stamps are others, a new lock's bytes are others, and at the edge of the 10 seconds
    an act can get the other verdict (A7). No read of today gave a row of the first attempt.
  - With the repair: each fact's row comes out the same. What a select and an open push see on the
    task that had shown the first attempt, from then until the second attempt shows: P17.
  - With the row before: a read gave nothing of the first attempt, so nothing that it gave changes.
  - door: unchanged, held to closing.
Closed x anything
  - Nothing that a read gave of it ever changes.
```

## Open points for the build session to rule on

Each point gives the contract's words, what the code or Rama says, the readings or options that this
session sees, and what each costs. This paper chooses none. The labels: `[Sid's]`, `[builder's]`,
`[fact]`, as "How the skill's template is adapted" says.

### P1 `[builder's]` "The four proofs of today stay and pass"

**The words.** "The read path has a proof of its own against the module. The four proofs of today stay
and pass." The receipt asks for "the proofs' outputs with their count lines before and after".
**The code.** F1: the module's proof and the pure proof check the very row that A1 changes, the exact
set of store keys that A3, A5 and A6 add to, and "sealed" by a field that is gone.
**Readings.** (a) The four files stay. Each check that a decision of this build makes untrue is
written again for the new row, and the counts change; the receipt's "before and after" shows it. (b)
No check of the four changes. That cannot hold with A1: the check at `module_proof.clj:612-614` would
fail.
**Cost.** With (a) the proofs' map changes with them. The door's build used "the three other proofs
keep their counts" as a check on itself (`door/PROGRESS.md:346`); this build cannot. One more thing:
the proofs use `:softland.store/owner` and `:softland.store/class` as their two examples of a key
that the store does not know (`pure_proof.clj:587-590`; `module_proof.clj:749`, `:1214`). A new store
key under either word breaks those checks.

### P2 `[builder's]`, and its first option changes a form that Sid decided: where a permission's one part more lives

**The words.** "A permission gets one part more: see, or see and act. A permission that was kept
without the part reads as 'see and act', so the code carries both forms." The sheet: "A permission
gets one slot more: what it allows." Decision 8 of 6 October: "It is `[who layer granted-in]`, or
`[who layer granted-in parent]` where the parent is a permission."
**The code.** The shape of a permission is in the maker's form, and each act names a whole permission
in `:permission` (`act.clj:146`, `:171-174`). Check (d) is a comparison of bytes between the row and
the permission that the act names (L11). A permission fact's value must have that shape
(`declaration.clj:130-134`, `:388-389`), sit on the holder that its first slot names (`:392-396`),
and be straight beneath the act's own, read by its slots (`decision.clj:204-210`). A making act's
permission has exactly 3 slots (F7). The ids inside a permission are found by its first three slots
and its fourth (`declaration.clj:322-328`).
**Options.**
1. The part is a slot of the permission itself. The act's declared form changes, in `act.clj`. Every
   act of a holder then names what her permission allows. A permission above carries its own part
   inside each permission beneath it. The fourth slot is the permission above today, so the part
   must be told from it. Check (d)'s comparison keeps holding that no act names more than the row
   holds. Cost: a second change of `act.clj` (the store's page speaks of "The one change of
   `act.clj`", `:272`); the act proof's checks of a permission; and decision 8's two forms are no
   longer the whole form, which is Sid's to allow.
2. The part is in the value of the permission fact, beside the permission. The act's form does not
   change. But the row's bytes are then no longer the one text of the permission that the act names.
   So check (d) changes (`decision.clj:138`; `batch.clj:207`), and so do the four places that read
   the fact's value as a permission (`declaration.clj:388-396`, `:348-349`; `decision.clj:204-210`;
   `first_act.clj:92-93`). "Both forms" are then a bare permission, and a permission with its part.
3. The part is a fact of its own. Then "a permission gets one part more" is not what is built. A
   holder's right is in two places that are written at two moments. A gate reads one row more for
   each act, 5 where the store's page counts 4 (`:287-289`), and in a microbatch layer every task
   holds it. "Kept without the part" is then "no such fact".

Each option must keep one thing: nobody can raise her own right by naming it.

### P3 `[builder's]` "An act needs 'act'": where the check stands, its word, and the making act

**The words.** "A gate refuses an act under a permission that says 'see'; the sheet's words are 'an
act needs act'."
**The code.** The order of the checks is the order of the reasons (G5). For a layer that is made, the
standing gives two words (`decision.clj:141-144`), and neither fits a permission that stands and
says "see". A making act is not held to (d) or to (g) (`decision.clj:149-150`, `:250-253`).
**Unsaid.**
1. The reason word. It is new, plain, and listed for Sid (R6).
2. Its place in the order. With the standing, after `:permission-does-not-stand`: an act with a
   "see" permission and a stale fact then gets the new word.
3. A making act whose own permission says "see". (a) It is no right making act. It is then refused
   `:layer-not-made`, as it falls out of a form that asks for more; the layer can still be made by
   a right act. Cost: one more line in the form of a making act, which is the builder's own form
   (`first_act.clj:9-10`). (b) It is accepted. The layer is made, nobody can ever act in it, its
   setting can never be written, and its id is used for good. Until sign-in is built "any sender can
   make any layer that is not made, for any holder" (the store's page, `:139-140`), so an id can be
   used up by a right making act today too.
4. The door. A holder of "see" who sends a value gets the door's word `:have-its-request-accepted`
   and not a gate's answer for her own act (F6). "The door checks nothing of the act's form"
   (`door.clj:13-14`). The contract asks no change of the door.

### P4 `[Sid's]` One permission for each holder in a layer

**The words.** "One permission for each holder in a layer, as built. One holder cannot hold 'see' from
one giver and 'act' from another."
**The code.** L16. The store's page: "the other answer changes one rule of the declaration and the
key that the gate reads" (`:293-294`).
**Kept.** One. **The other answer** would also change B2: the read path reads one place for an asker
today, and would read a run of them. **For the queue:** may a holder hold more than one permission in
a layer.

### P5 `[Sid's]` Who may write a layer's setting, its list and the mark of a key

**The words.** "Sid's word for the setting is 'the owner', and no layer names an owner as a fact. Kept:
whoever may act in the layer, as for its depot (7 October, 3)." Sid, entry 1: "The owner of the layer
decides they can say on the group members or everyone".
**The code.** L18: the kept answer is what falls out, and "as for its depot" names a decision and no
code. Sid's page puts "who may write" the mark under "the builder's to propose" (entry 6); the
contract puts it here. The kept answer is the same.
**What the kept answer lets happen.** Any holder of "act" in a layer can set it to "everyone", and so
show every member's facts to a person who is not signed in. Any holder of "act" in the base can set
the base to "members". Any holder of "act" can write the list, and so change what every reader "from
this layer" sees.
**The other answer** needs a fact that says who the owner is, or a rule that reads it from the
record. A session's line of 9 October: "a layer is the system's when the system holds its first
permission; the layer's first act writes that" (`src-store/README.md:293-295`). The build keeps the
check of who may write these three at one place, so that the other answer is one check.
**For the queue:** who is the owner of a layer, and who may write its setting, its list and its marks.

### P6 `[Sid's]`, with a `[builder's]` part: what a layer starts with

**The words.** "Kept: a new layer is 'members', the base is 'everyone', and a layer builds on nothing
until a fact says so. With 'everyone' a person who is not signed in sees the base, which is half of
the queue's K14."
**The code.** One start value is in code today: `depot-at-making`, a function of the layer's id
(`first_act.clj:49-54`). A making act has exactly two facts (F7).
**The builder's part: is a start value read from no fact, or written as a fact at the making?**
1. From no fact, by a function of the layer's id. The making act does not change. The value is in
   no depot record: a later change of the kept answer changes how every layer with no fact reads,
   old ones too. And "no row" must never be taken for "the start value" where a row exists and is
   held back (A7).
2. A fact that the making act writes. The form of a making act changes, and each proof that makes a
   layer. The value is in the log. A change of the kept answer touches only layers that are made
   after it.

**For the queue:** K14 stays open; this build's kept answer is its first half.

### P7 `[Sid's]` A permission that says "see" stays "see"

**The words.** "A permission's place takes one write, and nothing ends a permission. To raise one, or
to end one, is not built and not decided."
**The code.** L20.
**What follows.** A holder who was given "see" by mistake cannot write in that layer under that id,
ever. The read path follows a permission live, "so it follows an end whenever one exists" (the sheet,
`:435`); no case reaches that in this build. **For the queue:** how a permission is raised, and how it
ends. The contract lists "How a permission ends" under "Not in this build, by decision".

### P8 `[Sid's]` A read that left a layer out says so: what it says

**The words.** "It tells the asker that a layer exists which she may not see. Sid let the follow-on
stand; nobody asked him about that edge."
**Unsaid.** Whether the read says that one was left out, how many, or which. And a layer of the list
that is not made: with the same word, the asker cannot tell a wrong id from a closed layer; with a
word of its own, she learns which ids are made. Where the list came from a layer's own fact, naming
the layer that was left out also tells her what that layer builds on.
**Kept by the contract.** It says that it left one out. **For the queue:** what a read may tell of a
layer that the asker may not see.

### P9 `[Sid's]` How far a read walks from a layer

**The words.** B3: "The list that the layer gives, or the view's own." Sid's page, entry 2: "A layer
says which layers it builds on, in order, as a fact on the layer. A read gets that list when the view
names none." A session's, on the same page: "a read is given a list, or a layer to walk the list
from". The sheet's picture: `:alice-notes → :team → the base`.
**Readings.**
1. The one list. A read from a layer looks in that layer and in the layers of its fact, and no
   further. For the picture, `:alice-notes` names both `:team` and the base. Cost: one read for the
   list. When `:team` changes what it builds on, each layer that builds on `:team` is written again
   by whoever may.
2. The walk. A read also follows the lists of those layers, and on. Then `:alice-notes` names
   `:team`, and `:team` names the base, and a change of `:team`'s list changes what Alice sees. Cost:
   one read for each layer of the walk, in turn, before any fact is read; an order where two lists
   meet; an end for two lists that name each other; and a live read "from a layer" then rests on
   every list of the walk.

A gate checks nothing of a list in either reading (A5). The choice says what a person sees, so it is
Sid's. In either reading these are the builder's: a list that names its own layer, a layer twice, or
a layer that is not made.
**For the queue:** does "builds on" reach through the layers that a layer builds on.

### P10 `[Sid's]` A fact under a marked key whose value is no thing's id

**The words.** A6: "For a marked key the gates keep each fact in one more order, by value". The sheet:
"a fact points at a thing when its value is that thing's id" (`:281`), and "How a gate knows that a
value is a thing's id is not designed; it comes back to you if it shapes the store." (`:316`).
**The code.** A value is any value of the form: a string, a number, a map, a set, up to 65,508 plain
bytes (`act.clj:245-270`). An id is a readable keyword or a UUID (decision 9 of 6 October). A row's
key is made of texts of 256 characters at most that hold no space (rule 20; `rows.clj:53-57`). F8.
**Options.**
1. A gate refuses a fact under a marked key whose value is no id. A tool then meets a new refusal
   when a member marks a key. That shapes what a tool can write.
2. A gate accepts it and keeps it in no run. The fact is in the facts table, and "what points at"
   never shows it. Nothing tells anybody.
3. Every value is kept in the other order, by some text of it. "What points at a thing" is then
   "which places hold this value", for any value. It needs a limit on that text, and a way to keep
   one value's run apart from the run of a longer value.

Nothing can tell a keyword that is an id from a keyword that is a word, such as `:done`; what a key
is, and what an id is, is the queue's K5.
**For the queue:** what a marked key may hold.

### P11 `[Sid's]` The store's own facts in a tool's read

**The words.** None. The contract is silent.
**The code.** F4.
**Readings.**
1. A read gives every row of its run. An asker who may see a layer can then read who its members
   are, what each may do, its setting, its list, its marks, and each request for locks with its
   count.
2. The read path leaves out a row under a key of the store's namespace unless the query asks for it.
   Cost: a test of each row's key; and a view does need some of them, such as what a layer builds on.
3. They are read only by words of their own, beside the three first words.

It says what one member may learn of the others, so it is Sid's. **For the queue:** may an asker who
sees a layer read its permissions and its settings.

### P12 `[Sid's]` "What points at a thing" through a list of layers

**The words.** B1's third word, with B3: "For each place the nearest layer that holds a fact wins,
also when that fact says 'nothing here'."
**The derivation.** A block E, under the key `:parent`. In the base E's fact says canvas-1. In
`:team` E's fact says canvas-2, or is a retract. A read asks what points at canvas-1, through `:team`
and then the base. By B3 the place of E under `:parent` is won by `:team`, so E does not point at
canvas-1. But `:team`'s run for canvas-1 holds only facts whose value is canvas-1, and E is not among
them. The base's run for canvas-1 gives E. Nothing in the two runs says that `:team` holds another
fact at that place.
**Readings.**
1. B3 holds for this word. For each place that a farther layer's run gives, the read learns whether
   a nearer layer holds any row at that place. Cost: one seek for each such place and each nearer
   layer, and for a live read one push for each. By the skill's figure of 0.3 to 0.5 ms for a seek
   (`SKILL.md:37`), 5,000 places through 3 layers is up to 10,000 seeks: 3 to 5 seconds of seeks,
   or 10,000 pushes. Any way to get the same answer for less asks the gates to keep more than A6
   names.
2. The runs of the layers, joined. A place is given when the current fact of any layer that she may
   see points at the thing. No cost more. But a person who moves a block, or takes it away, in her
   own layer still sees it on the base's canvas.

The second is another answer to what a person sees. **For the queue:** what "what points at a thing"
means when a nearer layer holds another fact at the place.

### P13 `[Sid's]` One moment for all the layers of one read

**The words.** The contract's picture: "RAMA — the gates' tables, read straight by the read path. No
query of the store stands between." The sheet lists "One moment for all the layers of one read; one
query for a whole list of layers" as "The builder's, in the build's contract" (`:439`). The contract
names neither. The queue holds it open as ?6: "Does one read line up the moments of the layers that
it read".
**What follows from the picture.** A read through k layers is k reads, at k moments, on up to k
tasks. A writer who writes two layers in two acts can be seen half done. Inside one microbatch layer
two tasks show one batch at two instants (the store's page, `:184-189`). The skill would have one
query of the store do the k reads in one round trip (`SKILL.md:39`); the picture rules that out.
**Kept, as it falls out.** No lining up. **For the queue:** ?6 stays open, with this finding.

### P14 `[builder's]` What a read gives for a value

**The words.** A1: the row holds "those plain bytes". B3: the nearest fact wins "also when that fact
says 'nothing here'".
**The code.** F9. `plain-value-of` gives nil for no row, for a retract, and for bytes that do not
read; and it gives a thing for bytes that read as what `value?` refuses, such as a list or a symbol;
and it reads only the first form of the bytes (`rows.clj:192-200`).
**Unsaid.**
1. The three states of a place, told apart: no row, a retract, a value. That function alone does not
   tell them apart.
2. Whether a caller is handed the bytes of the one text, as the row holds them, or the value read
   back from them.
3. Bytes that are no value of the form. The gate could check what a lock opens: rule 16 says "the
   grammar check has its place". The contract does not ask it. With no check, the read path must
   stand any bytes and say what it gives for them. With a check, the gate reads each value's bytes
   on its own task, 65,508 at most.

By entry 4 each program that reaches Rama is trusted, so such bytes would come from a fault and not
from a stranger.

### P15 `[fact]` R4 against a push that sends whole rows

**The words.** R4: "A read that did not ask for a part of a row is sent nothing of it (entry 7)." A2:
"A fact's row also holds who wrote it, and the permission that the act named."
**Rama, as run by the probe.** A push on a run sends the changed row whole:
`KeyDiff[L1 big k002500 | NewValueDiff[{:stamp 2, :value "changed"}]]` (`run-2.log:9`). A push on one
place can end in one field of the row (the probe's F1). A push that gathers the values of a run into
one list sends "the whole list again on each change" (F4). A function in a path must be in the
module's code (L30).
**Readings of "is sent".**
1. The caller is handed nothing of it. Rama may send the whole row to the runtime, and the read path
   drops the part. Entry 7's own words fit this: "a read that does not ask for the writer is sent
   nothing of it; the row keeps it within reach." Cost: the bytes of the who and the permission of
   each row cross from Rama to the runtime for each read, asked or not, and sit in the push's memory.
2. Rama sends nothing of it. Then a path over a run must leave a part out of each row and still send
   one small change for one changed place; or the writer is not in the row that is watched, which is
   against A2's words.

**What a run must show.** Whether a path that leaves a part out of each row of a run opens, and what
it sends for one changed place. Not run. It also bears on B5: under the second reading two askers who
asked for different parts do not "look at the same thing".

### P16 `[builder's]` A7: which reading holds, and what stays held to closing

**The words.** "A read shows no fact of a batch that could still be tried again, and a place that
holds a fact never gives nothing. The papers name a repair that gives both: keep each batch's clock,
so that a second try comes out the same ... It is tried first, with a run. If the run does not hold,
the row before is kept beside the new one."
**The code.** A7 above: what today's code does, the table of what differs on a second attempt, and
the table of the two things against the two ways.
**What the build session must rule.**
1. The first sentence, under the repair. With the repair a read shows a fact of a batch that is not
   closed, so the sentence does not hold as written. (a) It is read as "shows no fact that a second
   try could change"; the repair then meets it, if P17 holds. (b) It is read as written; then only
   the row before meets it, and the repair is not tried.
2. The door's three reads. Under the repair a refusal's word can still differ between attempts, and
   a new lock's bytes still differ (A7). The contract says "the door's requests stay as built". (a)
   They stay held to closing: a fact is then shown to a reader up to one run before its maker can
   read "accepted" for it by the door. (b) They are let go: an answer's word or a lock's bytes that
   a read gave could then change.
3. The facts that B2 and B3 read in the base: the asker's permission, the setting, the list. They are
   read by the same way as a tool's facts, or a read can take "no row" for the start value (A7,
   "What makes it matter most").
4. With the row before: each place of a microbatch layer costs a read more than one row; the read
   path follows `:closed-through` of each task that it reads, live; and the store's "one row for a
   place" (`tables.clj:17-19`) no longer holds for that gate. Each count of seeks under "Latency and growth" grows
   for the base.
5. The streaming gate's own question about the base (`streaming_gate.clj:112-124`) is held to closing
   today. Under the repair it can stay so.
6. Rule 9. The store's page says of a record of each batch's cut and clock: "Nothing is built for
   it; whether it is wanted is Sid's." (`:460-461`). It says that of a rebuild from the depot. The
   repair keeps less: one clock for the attempts of one batch. The receipt should say that the
   repair, if built, is a first part of what rule 9 left to Sid.

**Cost.** The repair: one more thing kept for each batch, outside what Rama sets back; a read of the
base is then as a read of a streaming layer. The row before: more rows and more seeks for every read
of the base, one push more for each task of each live read, and no state outside the gate.

### P17 `[fact]` A7: what a second attempt gives, and what a reader sees meanwhile

**Rama's pages, read and not run.** "Each attempt for the same microbatch ID processes the exact same
set of data from the depots"; "each microbatch attempt starts with the PStates at exactly their state
after the previous microbatch ID" (`reference/rama/docs/12-microbatch-topologies.md:116`). The reset
at the start of an attempt "only affects the internal view" (`:120`). "all changes to all PStates on
a task in a microbatch become visible at the exact same time (though changes on different tasks may
become visible at slightly different times)" (`:124`). "When a new version of a PState is computed
for the same microbatch ID, the new version replaces the old version in the external view of the
PState." (`:128`). The skill: a write to a store that is no PState is "NOT rolled back on retry", and
the id of a batch "is the same across retries of the same microbatch" (`microbatch.md`, "Idempotent
writes to non-PState stores"). An append to a depot from a microbatch topology has no such promise:
"A retry re-appends" (`microbatch.md:127`; the page, `12-microbatch-topologies.md:130`).

**What this paper would want the batch probe to show.** Each is a thing that only a run settles.
1. That a failure can be made in-process after one task has shown its part of a batch. The store's
   own page says that no proof reaches it (`store-module/PROGRESS.md:175-180`). If it cannot be
   made, the repair cannot be "tried first, with a run" in the sense of A7, and the receipt says so.
2. What a select, and an open push, see on the task that showed the first attempt, from that moment
   until the second attempt shows there: the first attempt's rows, the rows before, or nothing.
3. What an open push is sent when the second attempt's rows are the same, byte for byte, as the
   first attempt's: nothing, the same changes again, or the value anew.
4. What survives a failed attempt, by kind. A write to a PState of the same topology: the pages say
   it is set back. A value that is kept outside the topology's PStates: where, and whether a second
   attempt reads it as the first wrote it. A value in the memory of one process: whether it survives
   a restart of a worker, since a restart is one cause of a second attempt.
5. That the kept clock can be durable, and within reach of the task that needs it, before any task
   of that attempt can show its part. Else a task can show rows that were stamped from a clock that
   is not kept.
6. That each attempt takes the same things in the same order on each partition, so that the turns
   are the same. The store's page says so of the order (`:370-376`), from Rama's pages.
7. The time: how long after the append a row of the base is visible on its task, against how long
   until its batch is closed. The store measured 873, 885 and 893 ms to a closed answer
   (`store-module/PROGRESS.md:175-180`); the push probe saw a push of a microbatch table 114 and
   285 ms after the append call (`RESULTS.md:44`). The difference is what the repair would give a
   reader.
8. What cannot be run here at all, said as such: a move of a task's leader, and two machines.

### P18 `[builder's]` The three new keys in a microbatch layer

**The words.** A3, A5 and A6 say "a fact on the layer". The contract says nothing of tasks.
**The code.** `on-every-task?` names two keys (`batch.clj:152-158`). A fact under any other key is on
the task of its entity. For a fact on a layer's own entity that is the task of the layer's id, which
is also the layer's meeting task (`batch.clj:27-30`).
**The mark.** A gate must know it where an act is decided (A6). As the code stands there are two
precedents. Every task holds the row, as a permission: a write of it goes to every task, and its act
goes to the meeting task (`batch.clj:513-520`). Or the act asks the layer's task, as it asks a home
task for a place or a lock: then each act that holds a fact sends one question to one task for the
layer, which rule 6 warns of: "Do not send all acts through one task unless the plan shows that it
must".
**The setting and the list.** No gate reads them. On the layer's task: a read finds them from the
layer's id. On every task: a read of an entity of the base finds them on the task where it reads the
entity, with no second task.
**From which act a mark holds, inside one batch.** (a) By the order of the turns. Then each act of
the batch that holds a fact under that key meets the mark's act, on the meeting task. (b) From the
batch after. Then a mark and a fact of one batch are not decided "one at a time in the order of
their turns" (`microbatch_gate.clj:39-40`), and the store's page must say so.

### P19 `[builder's]` The form of the fact that marks a key

**The words.** "A fact on the layer marks a key." The builder chooses "The form of the fact that marks
a key, and of the table that A6 adds".
**What each form meets in today's code.**
1. One fact on the layer's own entity, whose value holds every marked key of the layer. Check (f)
   holds it to the layer as a setting. It is one place. Two members who mark two keys at once write
   one place: the later one stays and the earlier mark is lost, unless each names `:replaces` and
   the one that is refused tries again. To take one mark off, the whole value is written again.
2. One fact for each marked key. The facts need places of their own, and one store key on one
   entity is one place. The entity would then be the key that is marked; a key is a readable
   keyword, and a readable keyword can be an id (decision 9 of 6 October). Such a fact is not on the
   layer's own entity, so check (f) as written refuses it if the key is a setting.

The first meets "A fact on the layer" plainly. The second meets it only if "on the layer" is read as
"in the layer". How many keys a layer may mark is a number of the build, unsaid.

### P20 `[builder's]` Facts that were written before the mark, and a mark that is taken off

**The words.** The builder chooses "What happens to the facts of a key that were written before it was
marked. The sheet says that a table can be filled later from the log, the gates' answers and the
locks, while all three are whole."
**The code and Rama.** A mark is an act, at any time; it is no update of the module. The facts table
is in the order layer · entity · key, so the facts of one key are not neighbours: to find them, a
gate reads every row of the layer. The skill: "PState write volume per source event must be bounded
by inputs the application controls" (`phases.md:31`), and long work on a task blocks every read of it
(`SKILL.md:26`). The streaming gate's event "does not give the task back" (the store's page,
`:204-207`). L29: a table of a gate that has run is not filled from the start of a depot.
**Options for the facts before.**
1. The other order holds the facts that are written after the mark. "What points at" is then not
   whole, and a read cannot tell: it must be said to the asker, by some sign that the plan gives.
2. The gates keep the other order for every plain key, always, and a mark only says what a read may
   ask. Each write under any plain key then pays A6's reads and writes. A6's words, "For a marked key
   the gates keep each fact", are then not what is built.
3. A fill after the mark, by something that is not the gate's one event. While it runs, the answer
   is not whole.
4. A mark is taken only for a key that holds no fact yet. That cannot be known without the same
   reading of every row.

**Options for a mark that is taken off.** The rows of the other order stay and grow stale; a later
mark must not show them as true. Or they are removed: as many writes as the key has facts. Or they
are kept level although no read may ask. The skill: "Never delete data unless the spec explicitly
requires it" (`SKILL.md:34`).

### P21 `[builder's]` A retract, and `:replaces`, under the three new keys

**The words.** R7: the three "each take a second write". A6: "a mark can be taken off". "A layer
builds on nothing until a fact says so".
**The code.** L9: a retract under any store key is refused (`decision.clj:230`), and rule 13 says
"each retract under a store key is refused". R7 changes rule 13 for a second write only.
**Readings.** (a) Retracts stay refused. A mark is taken off, and a list is emptied, by a second
write of a value that says so; the setting has two words and no "none". (b) A retract is allowed for
some of the three. Then "no row" and "a row with no value" both mean the start value, and P6 and A7
must read both right.
**`:replaces`.** A second write that names nothing is written over the row with no read (rule 2). Two
members who write a setting at once: the later one stays, and neither is told. With `:replaces` the
later one is refused. Whether the store asks for `:replaces` under these keys is unsaid.

### P22 `[builder's]` The who and the permission in a row whose fields have one class

**The words.** A2. **The code.** F5.
**Options, as the code stands.** They are held as the bytes of their one text, each a field of one
class, as a plain value is held today. Or the record's rule "No row holds a collection" changes. With
the first, a read that asks for them reads the text back to an id and a permission, and P14's
question of what is handed back is asked of them too.

### P23 `[builder's]` A live read through a list: what changes it beside a fact

**The words.** B4: "The read path keeps a live read on each, and ends the reads that are no longer
rightful." The words name an ending only.
**Unsaid.**
1. A layer that was left out, and that she may now see: a give to her, or the setting to "everyone".
   It joins the open read, or it stays out until she asks again. To join, the read path must follow
   the two facts of B2 for each layer that she may not see: one push more for each such layer and
   asker.
2. The list of the start layer is written again. The open read follows the new list, and then the
   list's fact is followed live and layers come and go under an open read; or the read keeps the
   list of its opening.
3. A layer of the list that was not made is made.
4. A key is marked while a read of "what points at" is open in that layer.

**Cost.** Each "it joins" is one push more and one more cause of a change. Each "it stays" means
that the asker sees what was right at the opening and not what is right now, until she asks again.
R5 says "A screen is shown the newest state"; it does not say of which list or of whose rights.

### P24 `[builder's]` The ending of a live read

**The words.** B4, and the picture's "a read in each layer".
**Unsaid.**
1. One layer of a list is no longer rightful. That layer drops out and the read says from then on
   that one was left out; or the whole read ends.
2. If the layer drops out: at each place that it had won, the asker now sees the next layer's fact,
   or none. That is a change to give her, for each such place.
3. What the asker is told at an ending: that it ended, and why, in a word of the read path's own.
   Nothing of the layer's facts.
4. Rama ends a push (F11), or a mark is taken off under an open read: the read path opens the push
   again, or ends the read.
5. Rama sends the value anew: it is given as a whole new value (B4). What a caller that keeps a
   screen does with a whole new value is the caller's.

### P25 `[builder's]` Leaving

**The words.** "It ends when the last one leaves."
**Unsaid.** How a caller says that an asker left. What happens to a caller that never says it, such as
a view that ends in an error. The door says the like of its stocks plainly: "Nothing bounds either
but the end of the process" (`door.clj:63-64`). The two races of B5. And after the runtime's process
starts again, each caller asks again; nothing tells a caller, and how a screen reaches the runtime is
not this build.
**Cost.** With no bound, the runtime's memory grows with the callers that never leave, each by the
rows of its runs. A bound is a rule, a time or a count, and nothing asks for one.

### P26 `[builder's]` What holds R1 and R2

**R1.** "No tool reads Rama by itself." Nothing in Rama holds it. A table that is declared private
"refuses every client" (L28), the read path too, so that cannot be used. R1 is then held by the tree:
one place opens the tables, and the proofs' own handles are the stated exception, as they were for D8
(`test-store/softland/store/README.md:529-537`). What a proof can show of it is a search.
**R2.** What the read path holds in memory beside the values that Rama's pushes keep level. At the
least it holds who looks at what (B5), and what each asker asked for. The question is the value that
B3 puts together, for a live read through k layers. (a) It is worked out from the k pushed values at
each change: the cost is the work at each change, the whole run at the worst, one place at the
best. (b) It is kept and changed in step with the pushes. By a plain reading that is "a store of our
own that we keep fresh beside Rama's push", and the contract says: "If the build finds itself
reaching for one, it stops and says so on its page." The repository's rule sends it to an examination
in a new session (`CLAUDE.md`, "Systemic repair and caching"). The same question is asked by A7's
second way, if a row were held back in the read path's memory until its batch is closed.

### P27 `[fact]` What a push costs a gate, and three things not run on the store's module

1. A select or a push of a long run runs on the task that holds the rows. A streaming gate decides
   its layer's acts on that same task, one event at a time (`SKILL.md:24-26`). Whether a client's
   read of a long run gives the task back while it walks is not known here. The module's own
   queries ask for it (`door_reads.clj:137-141`), and Rama's page names a page size "for selects
   using the `.allowYield()` option" (`reference/rama/docs/15-pstates.md:1231`). Not run for a push.
2. What each write of a gate costs with n pushes open on its partition, and the limit past which
   askers are "dropped from processing that diff and will resync" (`15-pstates.md:1230`). The page
   says it of streaming tables. For a table that a microbatch topology writes it says nothing.
3. A push with a partitioning key on the store's own tables. A select with one is in the module's
   proof (`module_proof.clj:248`, `:420`), which the build session ran. The probe opened pushes with
   one on tables of its own. A push on the store's tables is not run.
4. Many watchers at once; large values; a real cluster; a network between the runtime and Rama
   (`RESULTS.md:59-67`).

C measures the first two, in-process.

### P28 `[builder's]` The shapes of C's measures

**The words.** "a gate's slowest 1 in 100 at 100 acts a second; the same with live reads open on the
layer; the opening of a canvas of 5,000 facts; how late a push is."
**Unsaid.** Which gate, or both. Acts of what size, under a marked key or not. How many live reads,
on what runs. What "a canvas of 5,000 facts" is as a query: one run of 5,000 places, or "what points
at the canvas" for some count of blocks and then each block's own facts, which is one read for each
block unless the blocks' ids are neighbours (B1). "How late": from the append's call, from its ack,
or from the moment the row is visible; and for which depot. The probe's times are from the append
call (`RESULTS.md:11-12`, `:44-47`). The measures set no target; Sid sets targets "with those numbers
in front of you" (the sheet, `:427`).

### P29 `[builder's]` "Any run"

**The words.** The contract: "an entity, or any run of neighbouring places". Sid's page, entry 6:
"one entity, or any run of neighbouring keys". Sid's page wins. "Keys" can be read as the keys of one
entity, or as the keys of the table, which are places.
**Unsaid.** What a caller may name as a run: two ends; a beginning and a count, which the probe ran as
a window (Z4); all of one entity; all of one layer. Each end is made of ids within the limit (H9),
and both ends are in one layer (H4).
**In the base.** A run inside one entity is on one task. A run across entities has one part on each
task (L25). The read path puts the parts together and gives once each row that every task holds. The
proofs' cluster has 4 tasks, so that is 4 reads or 4 pushes there; the number of tasks of a real
cluster is on the other sheet.
**A whole layer.** It is a run and not a table, so R3 does not forbid it (the probe's Z5). Its size
has no bound.

### P30 `[builder's]` Which record a read looks in

**The code.** F10. A read must know the gate of each layer of its list, to choose a table and a task.
**Options.** (a) From the layer's id, by `depot-at-making`: no read, and right for as long as "no act
of this build changes a depot". (b) From the layer's depot fact: one read more for each layer, or a
push on it. That leaves room for a layer whose depot changes (the queue's ?9), as the door does. The
door paid that read and says why: "The door remembers no layer's depot between two calls: that would
be a kept copy of what the cluster holds" (`door.clj:260-262`). Whether the read path pays it is
unsaid.

### P31 `[builder's]` Who asks

**The words.** "Until sign-in is built, who asks is what the caller says."
**Unsaid.** What a caller says for a person who is not signed in. What the read path does with an
asker that is no id within the limit: a key cannot be made of it (H9), so she sees only layers that
are "everyone", or the read is turned away. Whether one read can be made for several askers at once;
by B2 a read is of one asker. `:system` as an asker is a who like any other; who may act as the
system is the queue's K9.

### The rows that this paper proposes for Sid's queue, decided by nobody

From P4 to P13, one line each. Each is built to the kept answer named at its point.

| From | The question |
|---|---|
| P4 | May a holder hold more than one permission in a layer? |
| P5 | Who is the owner of a layer, and who may write its setting, its list and its marks? |
| P6 | What does a layer start with: who may see it, and what it builds on? (K14's first half is kept as "everyone" for the base.) |
| P7 | How is a permission raised from "see" to "see and act", and how does a permission end? |
| P8 | What may a read tell of a layer that the asker may not see, or that is not made? |
| P9 | Does "builds on" reach through the layers that a layer builds on? |
| P10 | What may a marked key hold, and what does a gate do with a value that is no thing's id? |
| P11 | May an asker who sees a layer read its permissions, its setting, its list and its marks? |
| P12 | What does "what points at a thing" mean when a nearer layer holds another fact at the place? |
| P13 | Does one read line up the moments of the layers that it read? (The queue's ?6.) |

## What the proof of this build must be able to show

Requirements on what can be observed, not a design of the proof. A line that waits on an open point
names it. "For each gate" means once in a streaming layer and once in the base.

### From "When this build passes"

"a caller in the runtime's process asks for a place, an entity, a run of places, or what points at a
thing, through a list of layers, and gets the value now and each change after. The read path has a
proof of its own against the module. The four proofs of today stay and pass."

- **V1.** The proof is the only asker ("No view, no screen, no tool, no agent. The proof is the only
  asker."). It calls the read path as a caller in the runtime would, on the module `Store` in Rama's
  in-process cluster, and reads no table round the read path for the thing that it shows. Where it
  does read round it, to compare, its map says so, as the door's proof does.
- **V2.** For each of the three words, and for each gate: asked once, the answer holds the value that
  an accepted act wrote through the door.
- **V3.** For each of the three words: through a list of two layers or more, the nearest layer that
  holds a fact wins a place; a retract in a nearer layer hides a farther fact; a place that only a
  farther layer holds is given from there; the answer says which layer won. For "what points at a
  thing": by the reading that P12 rules.
- **V4.** For each of the three words, kept live: the value now; then one change for one act that
  changes it; then no change for an act outside it. A change that an act in a farther layer makes at
  a place that a nearer layer holds gives the asker nothing.
- **V5.** Left out: an asker who may not see a layer of the list gets the other layers and the word
  that one was left out, and nothing of that layer.
- **V6.** The read path's proof is a proof of its own, with a row in the proofs' map
  (`test-store/softland/store/README.md`), and that map says what a pass shows and what it does not
  reach.
- **V7.** The four proofs pass, by the reading that P1 rules, with their count lines before and after.

### Half A, each for each gate

- **VA1.** After an accepted act with a value: the row of the place holds the plain bytes and names no
  lock; the record of the act in the depot holds the sealed bytes and the lock's id, as the door
  sent them; the lock's row names the fact that it seals. A value that arrives unsealed under a
  plain key is still refused `:not-sealed`. A refused act leaves no plain bytes anywhere.
- **VA2.** The row holds who wrote the fact and the permission that the act named. For the system's
  making of a personal layer the two differ, and both are right.
- **VA3.** The setting: written; written again with the other word; a third word refused; on another
  entity refused; sealed refused. Each refusal is a recorded answer with its word.
- **VA4.** A holder of "see": her act is refused with the new word; her give is refused; a give to
  her of "see and act" is refused, as a second write of a permission's place is today. A holder of
  "see and act", and a holder whose permission was kept without the part: both act. A making act
  whose own permission says "see": by the ruling of P3.
- **VA5.** The list: written; written again; a read "from the layer" uses the newest.
- **VA6.** A key is marked. A fact under it with a thing's id as its value is then in that thing's
  run. A fact that replaces it with another id: the place has left the first run and is in the
  second, with no moment that a read can see between, in a streaming layer. A retract: the place is
  in no run. A fact with no `:replaces` over a fact: the same as a fact that replaces. The mark
  taken off: by the ruling of P20.
- **VA7.** In the base: straight after an act that replaces a fact, a read of the place gives a row,
  never nothing. And no read gives a fact that a second attempt changes. The first can be shown
  in-process. The second can be shown only if a second attempt can be made there (P17, 1). If it
  cannot, the proof shows what it reaches and its map says what it does not, as the module's proof
  does today of "a second attempt of a real batch".

### R1 to R8, each as a thing that can be observed, or not

| Rule | What can be observed | How far it reaches |
|---|---|---|
| R1 | By a search of the tree: one place opens a PState client or a push. The door opens none ("The door opens no PState", `door.clj:74-75`). The proofs' map names the proofs' own handles as the exception. Rule D8's sentence is replaced in each place that F2 lists. | A search shows the tree, not a process. Nothing in Rama holds it (P26). |
| R2 | An act that a gate refuses gives a live asker nothing. A value that a live asker gets is in the tables: the fact that wrote it has an accepted answer. After the last asker of a thing leaves, the read path holds nothing of it. | That the read path holds no store of its own is shown by reading its code, and by the build's page if it stopped (P26). |
| R3 | A query that could only be served by a watch of a whole table is turned away at once, with the read path's own word and no call to Rama. | "At once" is against the 100 seconds of the probe (`RESULTS.md:51-54`). |
| R4 | A read that did not ask for the writer is handed no writer and no permission. A read that asked is handed both. | What crosses from Rama to the runtime is P15. |
| R5 | After several acts on one place in quick turn, a live asker ends at the newest value. The proof asserts no count of changes between. | It is a limit that the read path states, not a thing that a pass proves. |
| R6 | Each new word is on the list "Words a session made, Sid's to change". No word is the rig's. | A reading of the list against the code. |
| R7 | Rule 13: a second write is taken under each of the three new keys; a second write of a permission's place is still refused `:store-key-takes-one-write`; a depot fact after the making and a retract of a permission are still refused with their words. Rule 16: VA1. The other rules: the four proofs (V7). | L33 names the lines of the store's page that change beside those two. |
| R8 | For each of the five things that are not decided, and each of P4 to P13 that the build session keeps: the docstring at the one place where the kept answer is code says that it is not decided. | That the other answer is "a small change" is judged by the review, not by a run. |

### The measures of C

Each is a number that the proof prints with what it is a number of. None is a pass or a fail: "The
build measures, and sets no target". Their shapes are P28.

- A gate's slowest 1 in 100 at 100 acts a second.
- The same with live reads open on the layer.
- The opening of a canvas of 5,000 facts.
- How late a push is.
- The base on a real cluster: not measured, and the receipt says so.

### What a pass does not show

As the proofs' map says of the four today (`test-store/README.md:35-43`): a cluster of several
machines; a failure in the middle of an event or of a batch; a network between the runtime and Rama.
And for this build: many askers at once; a restart of the runtime; a move of a task's leader; an
inhabitant. The receipt's line is the contract's: "proven by a hand-played asker, used by no
inhabitant yet".

## Latency and growth

Requirements and arithmetic, no design. The skill's figures (`SKILL.md:37-41`): a seek is about 0.3
to 0.5 ms; a step to the next row after a seek is about 1 to 10 µs; each call of a client is a full
round trip of the network. Latency is the longest path of one request, with work on several tasks at
once counted once. Throughput is every seek and step of the request, summed over all tasks. Every
figure below is derived by that arithmetic. None is measured.

### What each read costs at the least

| The read, in one layer | Round trips | Seeks | Steps |
|---|---|---|---|
| May she see: the setting | 1, or none more if it rides with the read | 1 | 0 |
| May she see: her permission, when the setting is not "everyone" | the same | 1 | 0 |
| The list of a layer, when the layer gives it | 1, before the layers can be read | 1 | 0 |
| A place | 1 | 1 | 0 |
| An entity, or a run, of n places, in a streaming layer | 1 | 1 | n |
| The same in the base, inside one entity | 1 | 1 | n |
| A run across entities in the base, with T tasks | T, at once | T | n in all |
| What points at a thing, m places | 1 | 1 | m |
| Which record the layer is in, if it is read (P30) | 1 | 1 | 0 |

Through k layers: the sum of the lines for each layer. The layers can be asked at once, so the
latency is the slowest layer's, after the list. With no query of the store between (the contract's
picture), the round trips are of the client: at the least one wave to the k tasks, and one before it
when a layer gives the list.

What the open points add: A7's second way adds, for each task of the base that a read touches, one
seek for `:closed-through`, and more than one row for a place (P16). P12's first reading adds up to
m × (k − 1) seeks to "what points at a thing". P9's walk adds one round trip for each layer of the
walk, in turn.

### Worked cases

- **A place through 3 streaming layers, each "members", from a layer that gives the list.** 1 seek
  for the list. Then in each layer: the setting, the permission, the place: 3 seeks. In all 10
  seeks, 3 to 5 ms of seeks. The longest path is 4 seeks, 1.2 to 2 ms, and two round trips.
- **An entity of 100 places in one layer.** 1 seek and 100 steps: about 0.5 ms and 0.1 to 1 ms. The
  skill gives this very case as about 0.6 ms, against about 50 ms for 100 separate seeks
  (`SKILL.md:38`).
- **A canvas of 5,000 facts as one run.** 1 seek and 5,000 steps: about 0.5 ms and 5 to 50 ms, on the
  task of the layer. For that time the task decides no act, unless the read gives the task back
  (P27). As 5,000 separate places it would be 5,000 seeks: 1.5 to 2.5 s. The probe opened a run of
  5,000 in 59 ms and of 20,000 in 32 ms, in-process, with a warming JVM (`RESULTS.md:39-40`,
  `:11-12`); that is a run, of a table of another module.
- **"What points at a canvas" with 5,000 blocks, through 3 layers, by P12's first reading.** 3 seeks
  and up to 5,000 steps in each layer, and then up to 10,000 seeks: 3 to 5 s.

### What a live read costs

- **The opening** costs what the read costs once. Rama's page says that the first value is "recomputed
  from scratch by executing the path like a `selectOne` call"
  (`reference/rama/docs/15-pstates.md:550`; read, not run).
- **Each change** costs Rama the work of the push. For a table that a stream topology writes, that
  work is done with the write, on the gate's thread, up to a time limit (`15-pstates.md:1230`; read,
  not run). So it is a cost of each act of the layer's gate. C measures it.
- **Each open push** sends a heartbeat on a fixed interval, and holds memory in the task's process
  (`15-pstates.md:720-724`; read, not run).
- **How many pushes one live read opens, at the least**, by the contract's own words: one for each
  layer of the list (B4); for each asker and each layer one on her permission; for each layer one on
  its setting, shared by every asker (B5). A read of a place through 3 layers by one asker: 3, and
  up to 3, and 3. For a run across entities in the base: one for each task. The count beyond that is
  the builder's ("How many pushes one read opens").

### What a gate's write costs, before and after

The store's page counts, for one accepted act with one sealed value: 4 reads and 4 row writes in the
streaming gate (`:287-289`); 3 reads, 4 row writes and 6 messages between tasks in the microbatch
gate (`:415-421`).

| Change | Reads more | Row writes more |
|---|---|---|
| A1 | 0 | 0: the same row, other bytes |
| A2 | 0 | 0: bytes in the same row |
| A3, A5 | 0 for an act that writes neither | 1 for an act that writes one |
| A4 | 0, or 1 by P2's third option | 0 |
| A6, an act with a fact under a plain key that is not marked | at least 1, to know that it is not marked, unless the marks ride on a row that is read already | 0 |
| A6, a fact under a marked key | at least 2: the marks, and the row before, unless the fact names `:replaces` and the row is read already (`decision.clj:170-176`) | 1 or 2: the new entry, and the old one out |
| A7, the repair | what keeping and reading one clock for a batch costs: for a batch, not for an act | — |
| A7, the row before | 0 or 1 for each fact | 1 for each fact of the base that replaces a row |

One act holds 256 facts at most (`declaration.clj:64-67`). So one act writes at most 256 entries
into the other order and takes at most 256 out. That keeps the skill's rule: "PState write volume per
source event must be bounded by inputs the application controls" (`phases.md:31`). A fill of old
facts at a mark would not keep it (P20).

### What grows for ever

| What | By how much | Bound |
|---|---|---|
| The facts table | One row for each place, as today. Each row gains the who and the permission: about 40 to 80 bytes commonly, about 3.4 KB at the limit (A2). Each row of a value loses the lock's name and the seal's 28 bytes. | The count of places. A place is never removed: a retract is a row. |
| The other order | One row for each place under a marked key whose current fact has a value. | The count of such places. What a mark that is taken off leaves is P20. |
| The row before, by A7's second way | One row more for each place of the base that has been replaced. | The count of such places. |
| The depots | One record for each act, the acts that write the three new keys among them. | None: the depots are never trimmed (`module.clj:33-35`). |
| The locks table | As today. After A1 the tables' copy of a value needs no lock; the lock rows stay, since "the log's copy needs them until the other sheet is settled". | As today. |
| A kept clock for each batch, by A7's first way | One value for each batch, or for the last batch only: the plan's. | Unsaid. |

### What the runtime's process holds, and loses at a restart

- For each open push, every row of its run. The probe: "a push over 20,000 places keeps 20,000 rows
  in the watcher's memory" (`RESULTS.md:64-65`). After A1 each row holds its plain value, 65,508
  bytes at most, and after A2 its who and its permission. A run of 20,000 rows of about 200 bytes is
  about 4 MB. At the limit of a value it is about 1.3 GB.
- It grows with the count of things that are looked at and with the size of each run. Sharing (B5)
  keeps it from growing with the count of askers. What a read does when a run is very large is the
  builder's.
- For each asker and each layer, the one row of her permission; for each layer, the one row of its
  setting. Small rows; their count is askers times layers.
- Who looks at what. That is the read path's own, of one process.
- None of it is durable. At a restart all of it is gone, and each caller asks again (P25).

### What drives the volume

- Reads once: the count of askers times the things that each asks, each time it asks.
- Live reads: the openings, by the count of things looked at; then the changes, by the acts that fall
  in open runs; then the heartbeats, by the count of open pushes.
- Acts: as today, by the makers. What an act costs its gate now also rises with the pushes that are
  open on its task (P27) and with the marks of its layer (A6).

