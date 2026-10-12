# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

**What this paper is.** The implied spec of the build "before real data is kept": what its contract
implies and does not write out. It checks each line of the contract that says how the code stands
today. It lists each place that takes the depots of acts to be two, each use of a lock or of the seal
with what that use does for the store, each table that a gate writes with what each part of a row is
made from, each state and edge that the three changes imply, and each place where the contract's words
cannot all hold, are silent, or differ from the code or from Sid's decisions.
**For which contract.** `src/proposal/backend-on-rama-2026-10-02/BUILD-before-real-data.md`, last
changed by commit `68832bb2`. "The contract" below is that file. "A1" to "A8", "B1" to "B6", "C1" to
"C3", "D" and "R1" to "R11" are its own numbers. "Entry 1" to "entry 7" are the entries of "Decisions
of 12 October 2026 — before real data is kept" in `src-store/README.md`. "Rule 1" to "rule 21" are the
store's rules in `BUILD-store-module.md`, as the store's page says how each is met. "Finding 1",
"finding 6" to "finding 16" are the contract's own numbers under "What the read build's session found
for this build". Sid's decisions in `src-store/README.md` win over the contract, and the contract wins
over everything else.
**The stage.** The first of the Rama skill's stages, Phase 0 (`phase-0-implicit-spec.md`). It decides
nothing and designs nothing: no depot, no table, no form of a row, no topology, no name, no form of
the rebuild. Where a requirement could not be said without a mechanism, the property is said.
**Who wrote it.** One fresh session (model Opus 5.5), 12 October 2026, for the session that holds the
build to rule on. It wrote this one file and changed no other. It staged and committed nothing.

**What was read whole.** The skill: `SKILL.md`, `phases.md`, `phase-0-implicit-spec.md`,
`artifact-implicit-spec.md`. The contract. `src-store/CLAUDE.md`. `src-store/README.md`, with every
decisions section. The sheet `decisions/groups/g-check.txt`. `reads/RECEIPT.md`. Of the source, line by
line: `door/additions.clj`, `door.clj`, `door/seal.clj`, `door/declaration.clj`, `door/words.clj`,
`door/text.clj`, `door/digest.clj`, `gate/decision.clj`, `gate/stamp.clj`, `gate/first_act.clj`,
`gate/batch.clj`, `module.clj`, `module/streaming_gate.clj`, `module/microbatch_gate.clj`,
`module/batch_clock.clj`, `module/door_reads.clj`, `record/tables.clj`.
**What was read in part.** `reads/batch-probe/RESULTS.md`: lines 1 to 330 and 440 to 520 (the short
of it, the table of what was found, B1 to B4, what could not be run, what is not settled); not B5 and
B6, and of B7 the last third. `src-store/softland/store/README.md`: lines 1 to 870 and 959 to 1070 (the
parts, how the parts relate, the act, what the gate does not check, the decisions, every engineering
choice with the rules 1 to 21, and of the words the fields of a row, the words of a stray and the
reason words). `act.clj`: lines 60 to 548 (the limits, the shapes, the engine, the check).
`door/stock.clj`: lines 1 to 215 and 296 to 384. `record/rows.clj`: lines 1 to 507 and 660 to 812 (the keys,
each row, the reads' tests; not the functions that only a read calls, lines 507 to 660).
`read_path.clj`: `pstate-of`, `task-routing-ids`, `open`, and the type's fields.
`read_path/sources.clj`: lines 86 to 171. `read_path/answer.clj`: `counts?` and `fact-id`.
`BUILD-store-module.md`: item 7 of "What to build" and rules 1 to 21. `BUILD-door.md`: "What to build"
and the rules D1 to D8. `BUILD-reads.md`: its headings and its lines on a lock, a seal and a rebuild.
`decisions/OPEN-QUESTIONS.md`: every row, each cut to its first 330 characters. `test-store/README.md`.
Of the proofs: the names of every form whose code names a lock, the seal or a depot of acts, found by
a search that leaves out strings and comments; and the checks that a line below names, each read and
not run. Of Rama's documentation under `reference/rama/docs/`: `11-stream-topologies.md` lines 150 to
172, `12-microbatch-topologies.md` line 130, `14-depots.md` from line 250, `15-pstates.md` line 45,
`19-operating-rama.md` "Task scaling". Of the skill's references: the lines of `stream.md`,
`microbatch.md` and `depot-design.md` on an append to a depot from a topology. Of the rig's notes
(`src/proposal/rig-2026-09-25/RIG.md`, lines 2150 to 2175): its table of what was run on Rama, as
findings only. The earlier paper `reads/IMPLICIT_SPEC.md`: its first 60 lines and its headings, for
the form, and no requirement.
**What was not read.** `src/app/server/env.clj` and every `.env` file. Every file whose name starts
with `raw-` under a `runs/` folder. The rig's code, the server's and Inland's. The read path beyond
the lines named. The folder maps `door/README.md`, `gate/README.md`, `record/README.md`,
`module/README.md`, `read_path/README.md` and the proofs' own map beyond its headings.

**What was run.** No proof, no probe, no cluster. Three kinds of command: reads of files by range;
searches (`grep`, `wc`); and one script of this session's own, kept in its scratch directory outside
the repository, which blanks the strings and the comments of a Clojure file and prints the code lines
that match a word, each with the name of its top-level form. The counts below that say "code lines"
are that script's.

**Blocked, or refused.** Nothing. No tool call was refused and none waited for a permission.

**What changed in the tree while this was written.** The head moved from `46c3e5dc` to `dc4bd12b`,
a commit of another session: "Rama's docs for 1.6.0: the version is written on the copy, and the
release notes come in". It adds `reference/rama/docs/README.md`, which says that the words of the copy
are the words of Rama's docs while 1.6.0 was the newest release, and that one thing names 1.5.0, the
Maven example. This paper still marks each line from that copy "read, not run": nothing here ran
Rama. Another session added one row to the table at the top of `src-store/README.md` and an untracked
page, `src-store/OPEN-QUESTIONS-rama.md`. No decisions section changed. This paper read questions 8
to 10 of that page, which bear on part B, and takes no requirement from it. The source under
`src-store/softland/` and the proofs under `test-store/` did not change between the two heads.

**Marks.** "Read at" means read in the source at that line, at the head named above. "Doc" means read
in a paper or a docstring that is named, and not checked against the code that it speaks of.
"Reasoned" means derived by this session from lines that it names; nothing reasoned here was run.
"Read, not run" is for a line of Rama's docs or of the skill. "Not checked" means none of these. A
file name without a folder is under `src-store/softland/store/`. "The store's page" is
`src-store/softland/store/README.md`. "Sid's page" is `src-store/README.md`. "The sheet" is
`decisions/groups/g-check.txt`. "The receipt" is `reads/RECEIPT.md`. "The probe" is
`reads/batch-probe/RESULTS.md`.

## How the skill's template is adapted

The template is for a new module. It asks for each operation that the module exposes, and for a
matrix of each entity's states against each write. This build makes no module. It changes one that
is built, with its two clients, and it takes one thing out, changes what a second thing keeps, and
lifts one number. So the paper keeps the template's two sections and adds five before them, each of
which the brief of this stage or the contract asks for by name.

| Section | Where it comes from |
|---|---|
| 1. The check of each "as built" line | The contract's head: "The stage that writes the implied spec checks each 'as built' line of this page against the code" |
| 2. The places that take the depots of acts to be two | R6, and the contract's "About twenty places", which nobody checked |
| 3. Every use of a lock or of the seal, and the jobs that they did | A5: the plan must say where each job goes. This section lists what the code shows and says of no job where it goes |
| 4. Every table that a gate writes, part by part | B3: the plan must say what each is made again from. This section says what each is made from today |
| 5. The states and the edges that the three changes imply | The template's "edge cases" and "what must always be true", for A, B and C |
| 6. Operations | The template. An operation here is a call or a step that this build changes or adds: a send through the door, a gate's decision of a plain value, the keeping of an answer in a log, the rebuild, the comparison, an act under a deep permission, the measures |
| 7. Entity State × Write Matrix | The template. The entities are the ones whose states the three changes touch |
| 8. Open points | P1 to P22, each marked as Sid's or as the builder's, with the words in question, the options and what each would mean. This paper chooses none |
| 9. What the proof of this build must be able to show | From "When this build passes" and B4, as things that can be observed |

What the template asks and this paper does not give as a number: a latency target and a throughput
target. The contract sets none: "The build measures, and sets no target" (D). Section 6 says for each
operation what the code shows of its cost today and what the change adds or takes away.

No depot, table, form of a row, topology or name is chosen below. Where a line names a thing of
today's code, it is today's.

## 1. The check of each "as built" line

Each line is held against the source at the head named above. "Holds" means that the source says what
the line says. "Holds in part" means that the line is true of what it names and leaves out a thing
that the source shows and that bears on this build; the thing is given. "Does not hold" means that
the source says otherwise.

### 1.1 "What the code does today": the twelve lines

| # | The line, in short | Verdict | What the source says |
|---|---|---|---|
| L1 | The door seals each value fact; `entry-of` decides it for each fact; `facts-to-append` seals, and `:v` becomes `:sealed` and `:lock-id` | Holds | Read at `door/additions.clj:8-9` (the sentence that the contract quotes), `:153-235` (`entry-of`), `:348-385` (`facts-to-append`; line 385 takes `:v` out and puts `:sealed` and `:lock-id`). What the line leaves unsaid, and section 3 needs: `entry-of` also gives four of the door's words, and nothing is appended for the act then (`:205-235`) |
| L2 | The door asks for locks ahead: a request is an act with one fact under `:softland.store/lock-request`; the door reads the request's answer and then its locks with `locks-of-request`; it keeps a stock in the memory of its process | Holds in part | Read at `door/additions.clj:416-442` (`request`), `door.clj:446-524` (`settle`: the answer, then `read-locks`), `:526-569` (`ask`), `:147-154` (the type's `stocks`). "Ahead" is true of the count and not of the moment: the door asks only when a take from the stock comes back short, inside the call of a maker whose act needs the locks (`door.clj:627-629`), and it then asks for more than that act needs (`door/stock.clj:151-187`) |
| L3 | A gate refuses a plain value: the rule `:plain-value-is-sealed`, the word `:not-sealed`; both gates run the declaration's check; a gate opens each sealed value with its lock and keeps the plain bytes in the row as a String | Holds | Read at `door/declaration.clj:319` and `:546-550`; `module/streaming_gate.clj:215`; `module/microbatch_gate.clj:403`; `gate/decision.clj:412-467`; `record/rows.clj:224-259`, `:281-305`. The rule is of a fact under a plain key: a fact under a store key keeps a plain value (`door/declaration.clj:547`) |
| L4 | A gate makes each lock, 32 random bytes, and writes it to its locks table and to no depot | Holds | Read at `door/seal.clj:79-81`, `:120-125`; `module/streaming_gate.clj:164-170`, `:339-341`; `module/microbatch_gate.clj:233-241`, `:311-313`. The microbatch gate also draws one such row for each act of a pass and writes none of those (`:555`; `gate/batch.clj:1073-1076`) |
| L5 | What a gate writes for one decided act: the answer's row, and the row of each fact; `writes` makes the rows as data and each gate writes them | Holds in part | The two rows are as the line says (read at `record/rows.clj:201-208`, `:281-305`; `gate/decision.clj:571-621`). `writes` gives four more kinds that the line does not name: a row of the facts by value, in or taken out; a lock's row written again; a key at which a new lock is made; the task's last stamp (`gate/decision.clj:594-606`). And a gate writes three things that `writes` does not give: the set of a stray's word, the count of turns of a batch, and the stamp through which a task's rows are closed. Section 4 has each |
| L6 | The module's depots are three (`module.clj:115` to `117`) | Holds | Read at `module.clj:115-117`. The third is declared `:disallow` |
| L7 | The module appends to a depot in one place (`module/microbatch_gate.clj:441`), with `:ack`, and the topology `batch-clock` keeps it in `$$batch-clock` | Holds | Read at `module/microbatch_gate.clj:441` and `module/batch_clock.clj:88-96`. A search of the source for an append finds this line and the door's two client appends (`door.clj:215-216`) and no other. The line says "each batch's id and clock reading". The source: only a batch with things that are no strays, and only an attempt that finds no clock kept for the batch (`module/microbatch_gate.clj:432-444`). So the depot can hold two records of one batch (`module.clj:66-68`) |
| L8 | The tables: for each gate `answers`, `facts`, `facts-by-value`, `locks` and `task`; and `$$batch-clock` | Holds | Read at `module/streaming_gate.clj:189-193`, `module/microbatch_gate.clj:373-377`, `module/batch_clock.clj:89`. The microbatch gate's fifth has a schema of its own, with three fields that the streaming gate's has not (`record/tables.clj:172-189`). The microbatch gate also has three PStates that live for one attempt of one batch and are no part of the record (`module/microbatch_gate.clj:28-30`) |
| L9 | The streaming gate's source has `{:start-from :beginning :retry-mode :all-after}`; the microbatch gate's has `{:start-from :beginning}` | Holds | Read at `module/streaming_gate.clj:195`, `module/microbatch_gate.clj:379`. The third topology's source has `{:start-from :beginning}` and Rama's default retry mode (`module/batch_clock.clj:91`) |
| L10 | The depth of a permission: `max-permission-depth` is 4 (`act.clj:117`); the act's form holds it, and the door's declaration through the same engine; the words are `:malformed` and `:permission-fact-not-a-permission`; no gate tests a depth itself | Holds | Read at `act.clj:117-119`, `:199-202` (`nesting`), `:393-399` (the engine counts a named shape inside itself), `:161` (the part `:permission` names no reason, so the engine gives `:malformed`); `door/declaration.clj:308`, `:497-498`, `:398-409` (the rule hands the value to the same engine). A search of the code for "depth" and "nesting" finds lines in `act.clj` and `door/text.clj` and in no file under `gate/` or `module/`. What the line leaves unsaid is in 5.3: the engine gives a name that is not in `nesting` no nesting at all |
| L11 | About twenty places take the depots of acts to be two | Does not hold as a count | Section 2 lists 36 places in 17 files. Three of them go with the locks. The kinds of place that the line names are right: the door, the act's form, the declaration, both gates, the three reads of the door, the read path. Two kinds are missing from it: the record's rows and schemas, and the module's own declaration |
| L12 | How much of the tree names a lock or a seal: about 880 of the 11,400 lines of the source, in 19 of its 26 files; about 1,450 of the 13,300 lines of the proofs; the two files that hold nothing else are 561 lines | Holds in part | Counted again: 875 of 11,415 lines in 19 of 26 files, and 1,424 of 13,312 lines of the proofs, for the whole words "lock", "locks", "seal", "seals", "sealed". With "stock", "stocks" and "sealing" it is 1,024 and 1,571. The two files are 177 and 384 lines, 561. "Hold nothing else" does not hold of `door/stock.clj`: it holds `longest-wait-ms` (`:84-88`) and the number `:read-every-ms` (`:103-110`), and `answer-of` uses both for a maker's wait for an answer (`door.clj:335`, `:933`). Neither is a lock's |

### 1.2 The twelve findings that remain

| # | The finding, in short | Verdict | What the source says |
|---|---|---|---|
| F1 | Today a maker who may not act is stopped before her value reaches a log; the stop is the door's request for locks; when her stock is empty a gate refuses the request and "nothing is ever recorded under the act's own act-id" | Holds, with three bounds | The quoted words are read at `door.clj:817-819`. The bounds, each from the source. (a) Only an act with a value under a plain key is stopped: an act with no such value takes no lock and goes in (`door.clj:860-861`; the store's page, line 281: "an act with no value goes through"). (b) Only when her stock is empty: with locks in the stock "the act goes in, and its refusal is recorded under its act-id" (`door.clj:815-816`). (c) Her value does not reach a log, and the door's request does: it is an act on the depot under her who, permission, session and version, and its refusal is in the answers (`door/additions.clj:426-429`; `door/words.clj:88-91`). The stop is wider than a maker who may not act: 3.3, job J4 |
| F6 | The door reads a layer's depot on every send, in one round trip, and keeps no copy (`depot-of`, three call sites); in the base that query is held to closing | Holds in part | Three call sites: read at `door.clj:739`, `:765`, `:853`. No copy: `:266-268`. "On every send" does not hold: a send whose maker gives an act-id back makes no read of the depot (`door.clj:850-851`; `:734-735`). Held to closing in the base: read at `module/door_reads.clj:168-177` |
| F7 | `rows/fact-row` has the branch for a plain `:v` already, used today under store keys only; its digest is the one that the door seals today, so a fact's place in the order by value does not change | Holds | The branch: read at `record/rows.clj:304-305`. Used under store keys only: a plain `:v` under a plain key is refused before any row is made (L3). The same bytes: the door seals `(text/utf8 (act/normal-value v))` (`door/additions.clj:227`); a gate's fact holds `:v` in its normal form, since the engine's leaf `:value` gives `normal-value` (`act.clj:326`), and the branch takes `(text/utf8 (:v fact))`. Reasoned from those three lines: one value gives one run of bytes on both roads, so one value digest (`record/rows.clj:435-452`). What it rests on and no run of this store has shown for a fact's value: that a value comes off the depot with the text that it went in with. The module's proof shows it for one thing of each kind, inside a thing that is no act (`test-store/softland/store/module_proof.clj:698-730`, read, not run by this session) |
| F8 | The last stamp of a batch is in no act and in no answer; it can be later than the stamp of every act of its batch; it is the microbatch gate's last stamp; it is the `:level-since` that a key gets when it is marked in the base, and each row of the facts by value under that mark holds it | Holds; one word to make exact | Read at `gate/batch.clj:504-509`: the base plus (the greatest position + 1) times the number of tasks, minus 1, which is the greatest turn that the batch can hold and not the greatest that it holds. The gate's last stamp: `module/microbatch_gate.clj:450-451`. The stamp of a newly marked key: `gate/batch.clj:574-582`, `:864` (`:level-from batch-end`), `gate/decision.clj:485-496`. In no act and no answer: reasoned; it is made from the batch's clock, the gate's last stamp before the batch, the greatest stamp that a thing of the batch carries, the greatest position in the batch and the number of tasks (`gate/batch.clj:475-493`), and the last three are of the batch's cut, which no depot holds. "Each row of the facts by value holds it": the key of each such row holds it, as its third part, and no field of the row does (`record/rows.clj:454-465`, `:496-505`) |
| F9 | A rebuild applies acts in the order of their stamps, not of a depot; after a failure the streaming gate can decide a later record first; the base's depot holds no order between two partitions | Holds as two facts and one consequence | The two facts are docstrings of the source and lines of Rama's docs, not runs: `module/streaming_gate.clj:120-124`; `11-stream-topologies.md:168-170` (read, not run); `module.clj:93-95`; `gate/batch.clj:176-185`. That stamps give the order of the decisions wherever two acts can write one row: reasoned. In a streaming layer every row is on one task and a stamp is strictly later than the task's last (`gate/stamp.clj:79-82`). In the microbatch gate there is one last stamp for the gate and no two things of a batch share a turn (`gate/batch.clj:495-502`). No row is shared between the two gates: a key begins with its layer, and a layer is of one gate |
| F10 | A gate's last stamp, made again, must not be below any `:level-since` | Holds | Reasoned from `gate/batch.clj:475-493` and `gate/decision.clj:521-532`. The next batch's base is the next stamp after the gate's last stamp. If that last stamp were below the stamp of a marked key, a later fact under that key could get a stamp that is not later than the key's. `by-value-writes` puts an entry for every fact that it is called for, and takes an entry out only when the row before has a stamp later than the key's; so that fact's entry would stay when the fact is replaced. The same holds of the streaming gate's last stamp of a task and the stamp of a key that an act of that task marked |
| F11 | `decision/writes` takes the verdict, the stamp and the digest, and gives each row as data; a rebuild that calls it makes its rows by the functions that the gates call | Holds in part | Read at `gate/decision.clj:617-621`. It takes six things: the act-id; the act in its one form on arrival, or nil; the word of the refusal, or nil; the stamp; the digest; and, for an accept, what the gate has in hand. For an accept, what is in hand is not in an answer's row: the rows before of the act's places under marked keys and of the marked-keys place, the stamp of each marked key, and the stamp that a newly marked key gets (`:579-592`). The act in its one form is what the declaration's check gives for the thing (`door/declaration.clj:661-684`). In the microbatch gate the rows then pass `batch/routed`, which gives each row its home, sends a row under one of five keys to every task, and leaves the last stamp out (`gate/batch.clj:781-830`) |
| F12 | The tables hold rows that no read names: the rows by value of an earlier marking stay when a mark is taken off | Holds | Read at `gate/decision.clj:485-532` and `record/rows.clj:68-72`. Nothing in the source takes out an entry whose key holds an earlier stamp of the mark: `by-value-writes` builds each key that it takes out with the key's present stamp |
| F13 | Three numbers bound the depth of a permission: `act/max-permission-depth`, 4; `act/max-depth`, 32; `text/max-depth`, 64 | Holds; what each counts is in 5.3 | Read at `act.clj:117-119`, `:106-109`, `:265-290`; `door/text.clj:74-80`, `:198`. One thing the finding does not say: 32 bounds a permission only where it is a value, which is a permission that is given. An act's own `:permission` is a part of the act and no value, and the leaf `:value` never reads it (`act.clj:161`, `:191-194`) |
| F14 | A permission's whole chain is copied as text into the row of each fact and of each fact by value, and Rama sends whole rows to every push | Holds for the rows; the push is a doc | Read at `record/rows.clj:299-300` and `:504-505`: `(text/text (:permission act))` in both. That Rama sends whole rows: the receipt, section 9, on R4 (doc) |
| F15 | The numbers before this build are in `reads/runs/measures-final.txt`; each open push that holds a written row costs the gate at each write | Holds as a pointer | The file is there. The cost: the receipt, section 4, point 4 (doc; a run of that session) |
| F16 | No table that anything is kept live on may hold a byte array; the answers' `:digest` is a byte array, and nothing watches it today. Where the read path takes the depots to be two: `read_path.clj`, in the four table fields of `ReadPath`, `open`, `pstate-of` and `task-routing-ids`; `read_path/sources.clj`, `gate-of`; `read_path/answer.clj`, `fact-id`. The review scripts are in `reads/runs/review-scripts/` | Holds in part | The digest's class: read at `record/tables.clj:131-133`. Nothing watches it: the read path opens the four tables of facts and no other (`read_path.clj:1122-1125`), and the door reads an answer by a query. The six places are each as named (`read_path.clj:280-283`, `:1122-1125`, `:376-380`, `:463`; `read_path/sources.clj:91-96`; `read_path/answer.clj:121-125`). The list is not whole: five more functions of `read_path/sources.clj` test a depot word themselves (`own-rows-source` `:108`, `permission-source` `:118`, `place-source` `:127`, `run-sources` `:152`, `by-value-source` `:166`). The folder of scripts is there |

### 1.3 What the check found that the contract does not say

Each is taken up in the section named.

1. `door/stock.clj` holds two things that are not a lock's and that a maker's wait for an answer uses
   (L12; 3.3, J22).
2. The door stops more than a maker who may not act. With an empty stock it stops a value act for
   every reason for which a gate refuses the request; and by itself it stops a value that is no value
   of the form, an act with more than 256 values, and a value over the limit of bytes (3.3, J4, J10,
   J11, J6).
3. The seal is the one road by which bytes that are no value of the form reach a fact's row today
   (3.3, J12).
4. A second send of one act with a value is another thing today, with another digest. Without the
   seal it is the same thing (3.3, J14; P6).
5. The locks are one of the two reasons that the store's page gives for closing, and one of the two
   things that a second attempt of a batch makes anew (3.3, J19).
6. Three parts of the microbatch gate's rows come from something that is in no depot and that the
   contract does not name: the count of turns, the stamp through which a task is closed, and the task
   on which a row is (4.6, 4.8, 4.9).
7. What a gate keeps of a stray is made from the thing on its depot alone. What the store's rule 19
   says rests on an order between the two depots is another thing: the word of an act that was sent
   to the wrong depot (4.5; P8).
8. An act's own permission is no value, so 32 does not bound it; and the engine gives a shape that
   has no number no nesting at all (5.3; P16).
9. The depot of batch clocks can hold two records of one batch, and which one the gate's stamps were
   made from is the one that the keeping topology processed first (4.7; 5.2, edge 25).
10. The contract cites `15-pstates.md`, line 45, for "a table can be made again from a log when the
    processing gives the same result on a second run". Line 45 says that a depot is "a source to
    recompute PStates"; the condition is in `19-operating-rama.md`, "Task scaling": "This approach
    only works if your processing is deterministic" (both read, not run).

## 2. The places that take the depots of acts to be two

R6: "No new code takes the depots of acts to be two. About twenty places do today, and this build
adds none." The list below is the count that nobody had made. A place is in it when its code would
have to change for a third depot of acts: it names one of the two depot words to choose between two
roads, it declares the set of depot words, it holds a thing of each gate under its own name, or it
asks about "the other gate" as one gate. A place that takes the gate's depot as an argument and does
the same for any depot is not in the list; those are named after it. Every line was read in the
source.

**36 places in 17 files.** Three go with the locks (marked "goes with A"), which leaves 33.

| # | File and form | What in it takes the depots to be two |
|---|---|---|
| 1 | `act.clj`, `shapes`, the entry `:act-id` (`:180`) | The second slot of an act-id is `[:enum :streaming :microbatch]`. An act refers to act-ids in `:because-of`, `:replaces` and `:based-on`, so the maker's own form holds the two words |
| 2 | `door/declaration.clj`, `shapes`, the entry `:depot-word` (`:269`) | The same enum, for the part `:depot` of what arrives and for the value of a fact under `:softland.store/depot` |
| 3 | `door/declaration.clj`, `stray-reasons` and `stray-reason` (`:337`, `:619`) | The test is "not this gate's depot" and holds for any number. The word that it gives says two: `:act-id-of-the-other-gate` |
| 4 | `gate/first_act.clj`, `depot-at-making` (`:53-58`) | `(if (= base layer) :microbatch :streaming)`: a layer that is not made has one of two depots. Seven of the places below call it |
| 5 | `gate/decision.clj`, `makes-the-layer?` (`:133-137`) | Takes one true-or-false, "the other gate's record holds the layer" |
| 6 | `gate/decision.clj`, `standing` (`:172-191`) | The same one answer, and the word `:layer-belongs-to-the-other-gate` |
| 7 | `gate/batch.clj`, `gates-depot` (`:131-134`) | The pure code of a batch is of one depot word, `:microbatch`. The word is also the route of the gate's task and the one key of `$$batch-clock` |
| 8 | `gate/batch.clj`, `reads-other-record?` (`:999-1008`) | "The layer starts with the other gate's depot": `(not= gates-depot (first-act/depot-at-making …))` |
| 9 | `gate/batch.clj`, `decided` and `decide-next` (`:849-865`, `:1077-1100`) | Carry the one answer about "the other gate" into `standing` |
| 10 | `module.clj`, `Store` (`:114-121`) | Declares the two depots of acts by name, each with its own partitioner, and adds one gate for each |
| 11 | `module/streaming_gate.clj`, `add-to` (`:188-347`) | Five PStates named for its depot; the source `*streaming-depot`; `:streaming` handed to each check; and the question to the one other gate: when the layer does not start `:streaming`, it reads `$$microbatch-facts` and `$$microbatch-task` (`:231-237`) |
| 12 | `module/streaming_gate.clj`, `other-gates-record-holds-layer?` (`:146-162`) | One other gate, whose rows are held to closing |
| 13 | `module/microbatch_gate.clj`, `add-to` (`:359-567`) | Five PStates named for its depot; the source `*microbatch-depot`; `:microbatch` at the test for a stray; and the question to the one other gate, read in `$$streaming-facts` (`:550-552`) |
| 14 | `record/rows.clj`, `routing-id` (`:780-789`) | `(if (= :microbatch …) uuid layer)`: an act-id that is not of the microbatch depot routes by its layer |
| 15 | `record/tables.clj`, `task` and `microbatch-task` (`:172-189`) | Two schemas of what a gate keeps for a task, one for each gate |
| 16 | `module/door_reads.clj`, `answer-by-act-id` (`:99-123`) | A choice of two, `:streaming` and `:microbatch`, each with its own table; any other word gives nil |
| 17 | `module/door_reads.clj`, `locks-of-request` (`:125-159`) | The same choice of two. Goes with A |
| 18 | `module/door_reads.clj`, `depot-of-layer` (`:161-182`) | The microbatch facts when the layer starts `:microbatch`, else the streaming facts |
| 19 | `door.clj`, the type `Door` (`:147-154`) | Two fields, one handle for each depot of acts |
| 20 | `door.clj`, `open` (`:709-716`) | Fetches the two depots by their two names |
| 21 | `door.clj`, `append!` (`:212-217`) | `(if (= :microbatch depot) …)`: any other word goes to the streaming depot; and the ack level is chosen by the same test |
| 22 | `door.clj`, `depot-of` (`:269-273`) | For a layer that is not made, the depot of form 4 |
| 23 | `door.clj`, `settle` (`:490-492`) | `(= :microbatch depot)` decides whether the first read of a request's answer waits. Goes with A |
| 24 | `door/stock.clj`, `ask-for` and `default-numbers` (`:126-130`, `:183-187`) | Two numbers, "most at once" for each depot, chosen by `(= :microbatch depot)`. Goes with A |
| 25 | `door/words.clj`, `acks` (`:190-202`) | A map from the two depot words to the two words of an ack; `send-act` reads it (`door.clj:874`) |
| 26 | `read_path.clj`, the type `ReadPath` (`:280-283`) | Four fields: the facts and the facts by value of each gate |
| 27 | `read_path.clj`, `open` (`:1122-1125`) | Fetches the four tables by their names |
| 28 | `read_path.clj`, `pstate-of` (`:376-380`) | A choice of four, by gate and table; anything else gives nil |
| 29 | `read_path.clj`, `task-routing-ids` (`:463`) | Reads the number of tasks from the handle of the microbatch facts |
| 30 | `read_path/sources.clj`, `gate-of` (`:91-96`) | The gate of a layer is the depot of form 4 |
| 31 | `read_path/sources.clj`, `own-rows-source` (`:105-108`) | `(= :microbatch gate)` says that every task holds the rows |
| 32 | `read_path/sources.clj`, `permission-source` (`:115-118`) | The same test |
| 33 | `read_path/sources.clj`, `place-source` (`:125-128`) | `(if (= :microbatch gate) entity layer)` for the routing id |
| 34 | `read_path/sources.clj`, `run-sources` (`:148-154`) | `(= :streaming gate)` gives one source; anything else is read as a microbatch layer |
| 35 | `read_path/sources.clj`, `by-value-source` (`:163-167`) | `(if (= :microbatch gate) the value's digest, layer)` |
| 36 | `read_path/answer.clj`, `fact-id` (`:121-125`) | Makes a fact's id with the depot of form 4 |

**Not in the list, because each takes the gate's depot as an argument and holds for any depot.** The
checks of `gate/decision.clj` that take `gate-depot` (`places`, `replaced-fact-fault`,
`store-key-fault`, `lock-in-this-record?`, `lock-ranges`, `opened`); `first-act/making-act?`;
`record/rows.clj`, `written-by?` and the three tests at the head of a read, which give the depot slot
back; `door/additions.clj`, `thing-to-append` and `request`, which copy the depot slot of the act-id;
`door.clj`, `locks-for`, whose stock is for a layer, a depot and a who; `read_path.clj`,
`counted-under`, and `read_path/sources.clj`, `calls-for-once`, which carry a source's gate as data.

**The proofs** name a depot word or a table of one gate in code on 410 lines: 222 in the pure proof,
73 in the door's, 55 in the module's, 27 and 19 in the read path's two, 8 in the act's and 6 in the
measures. They are not listed one by one: R6 is about new code of the store.

**What follows for this build, as facts and not as a plan.** Part B adds a thing that each gate does
at each decision, and a rebuild that reads logs of acts. A gate is of one depot by its nature, so
code inside a gate's own topology is of that gate. R6 bears on three kinds of new code: what is
shared between the gates and would name a depot word to choose a road; what stands outside the gates
and reads "the two" logs of acts or "the two" records, as the rebuild and its proof do; and any new
word that says "the other". The 33 that stay are the ground that such code stands on, and R6 does
not ask this build to change them.

## 3. Every use of a lock or of the seal

The ground for A5: "the plan lists every job that the locks and the seal did, meant or not, and says
where each goes". This section lists the uses as the code has them, and then the jobs. It says of no
job where it goes.

### 3.1 In the source, by file and form

"Code" is the count of code lines of the form that name a lock, the seal, the stock, the seat or
plain bytes, with strings and comments left out. Every form was read.

**`door/seal.clj`, whole (177 lines).** `ways`, `way-of-new-locks`, `lock-length`,
`random-part-length`, `tag-bits`, `random-source`, `random-bytes`, `known-way?`, `sealed-length`,
`new-lock`, `cipher`, `seal`, `open`, `opens?`. What they do: make 32 random bytes; seal plain bytes
under AES-256-GCM with a fresh random part of 12 bytes and a tag of 16, with the UTF-8 of the one text
of `[layer entity key]` as associated data; open them; say how long a seal of n plain bytes is
(n + 28). Callers: the door's additions, the door's stock, the decision, both gates, the proofs. No
gate calls `opens?`.

**`door/stock.clj`, whole but two things (384 lines).** `default-numbers` and `numbers`: the door's
five numbers; three are a lock's (`:fewest-at-once`, `:most-at-once-streaming`,
`:most-at-once-microbatch`), one is the wait for locks and the span for which a request in doubt is
kept (`:request-wait-ms`), and one is also the sleep of a maker's wait for an answer
(`:read-every-ms`). `longest-wait-ms`: the longest wait that the door counts, of any kind. `ask-for`:
how many locks a request asks for. `nothing`, `unused`, `holds-nothing?`, `take-locks`,
`with-request`, `request-sent`, `request-in-doubt-since`, `without-request`, `kept-long-enough?`,
`can-seal?`, `runs-of`, `with-locks-read`, `count-of`: the stock as a value.

**`door/additions.clj`.**

| Form | Code | What the use does |
|---|---|---|
| `most-values-in-one-act` | 1 | The most locks that one act takes: the smaller of two limits, 256 |
| `parts-read`, `read-parts` | 0 | Read `:who`, `:permission`, `:version` and `:session` of the maker's thing. The door reads these four for one reason: the request for locks carries them, and the stock is chosen by `:who`. A part whose read throws ends in `:read-the-act`, and nothing is appended |
| `entry-of` | 15 | Says which fact is a value fact. Gives `:add-its-parts` for a value fact that already holds `:sealed` or `:lock-id`; `:seal-that-many-values` at the 257th value; `:seal-a-value` for a value that is no value of the form, for a value in a fact that is a map and no plain map, and for a value whose sealed bytes would be over 65,536. With each of these words nothing is appended. For a value fact it makes the plain bytes, the UTF-8 of the one text of the value's normal form, and the place |
| `walk` | 1 | Counts the value facts, and stops at the first word |
| `parts-carried`, `carried-of`, `reading*`, `reading` | 1 | Keep, for the request, the four parts of the act as given; give `:values`, how many locks the act needs |
| `facts-to-append` | 15 | Seals each value to its place with the next lock; holds the limit again on the real sealed bytes |
| `thing-to-append` | 2 | Takes the locks of the act |
| `request` | 1 | Makes the door's request: an act of the door's own making, under the maker's who, permission, session and version, with one fact under `:softland.store/lock-request` |

**`door.clj`.**

| Form | Code | What the use does |
|---|---|---|
| The type `Door`, `printed` | 2 | Holds the handle of the query `locks-of-request`, the five numbers and the stocks. The door prints as its module's name so that no stock and no lock is in a print |
| `read-locks` | 3 | One round trip: the locks of a request that this door sent |
| `read-answer-until` | 1 | The wait for an answer. One branch is a lock's: a request in doubt is given up by its age. The rest serves `answer-of` |
| `enter!`, `leave!`, `take!` | 15 | The stocks as the memory of the process: a map from `[layer depot who]` to a count of calls, a cell and a seat |
| `waited-word`, `settle`, `ask`, `in-the-seat`, `locks-for` | 23 | The getting of locks: take; else wait for the stock's seat; read an earlier call's open request to its end; send a request and read its answer and its locks. `locks-for` also gives `:read-the-act` at `:who` for a who that cannot be hashed or compared, and nothing is appended |
| `open` | 1 | Fetches `locks-of-request`; takes the opener's numbers through `stock/numbers`; starts with no stock |
| `send-act` | 2 | Steps 6 and 7: the locks, then the seal |
| `answer-of` | 1 | Cuts a maker's bound to `stock/longest-wait-ms`. Not a lock's use: the number lives in the stock's file |
| `stock-count` | 2 | One of the door's six calls: three numbers about the locks that the door holds |

**`door/words.clj`.** `words`: seven of the door's fourteen words are of a lock or of the seal
(`:seal-a-value`, `:seal-that-many-values`, `:have-its-request-accepted`,
`:append-its-request-cleanly`, `:get-locks-within-its-wait`, `:read-its-requests-answer`,
`:read-its-requests-locks`), and an eighth says the seal's two parts beside the door's two
(`:add-its-parts`). `carried`: five of the twelve things that a word carries are of a lock or of the
seal (`:most`, `:sealed-bytes`, `:request`, `:request-answer`, `:waited-ms`).

**`door/declaration.clj`.**

| Form | Code | What the use does |
|---|---|---|
| `max-sealed-bytes` | 1 | 65,536: the most bytes of one sealed value. "It bounds the bytes of one row" |
| `max-locks-in-request` | 1 | 256: the most locks of one request, and so the range of a lock's index |
| `store-keys`, the entry `:softland.store/lock-request` | 2 | Makes the request one of the store's keys: its value arrives plain, its fact sits on the uuid of its own act, and a read of a tool's facts leaves it out |
| `shapes`: the part `:lock-id` of `:arrived-fact`, `:lock-id`, `:lock-count` | 3 | The shape of a lock's id and of a count. A lock id that does not fit gives `:lock-id-malformed` |
| `rules` and `rule-checks`: ten rules | 37 | `:store-key-fact-has-no-seal`, `:lock-request-holds-a-count`, `:lock-request-within-the-limit`, `:lock-request-sits-on-its-act`, `:plain-value-is-sealed`, `:sealed-fact-has-no-plain-value`, `:seal-is-whole`, `:sealed-is-bytes`, `:sealed-within-the-limit`, `:lock-named-once`, each with its word. They stand at fixed places in the order of the 27 rules, which is the order of the reasons |
| `ids-of` | 1 | Holds the layer of a lock id's act-id to the limit of an id's text |
| `without-sealed`, `with-sealed`, `check*` | 6 | Set each fact's `:sealed` aside before the engine reads the thing, and put it back: no leaf of the engine fits a run of bytes |

**`gate/decision.clj`.**

| Form | Code | What the use does |
|---|---|---|
| `lock-in-this-record?`, `lock-ranges` | 11 | Which rows of locks an act needs: one range for each request whose locks it names. A lock id of another layer or of the other depot gives no range |
| `opened-fact`, `opened` | 19 | The last check and the most costly. Four words, in order: `:lock-not-made-for-this-layer`, `:lock-already-seals-a-value`, `:lock-way-not-known`, `:lock-does-not-open-the-value`. Opens each sealed value once and gives the plain bytes |
| `accept-writes`, `writes` | 9 | Hand the plain bytes to the fact's row; write the lock's row again with the fact that it now seals; give one key for each lock that a request asks for. A refusal writes no lock row |
| `by-value-writes` | 1 | Takes the plain bytes of the row's value for the digest. Not a lock's use: the bytes are the row's |

**`gate/batch.clj`.**

| Form | Code | What the use does |
|---|---|---|
| `lock-in-this-record?`, `lock-indexes-by-request` | 8 | As in the decision, for `:microbatch` |
| `questions` | 8 | An act asks the home task of each request whose locks it names; a request asks at its own uuid how many locks it will make. A fact under `:softland.store/lock-request` also asks about its place |
| `marks-places-asked` | 0 | Leaves a request's place out of the reads of marked keys |
| `reads-of`, `lock-replies`, `replies`, `with-replies` | 10 | Read the ranges of locks and reply. A lock is shared when an act of another turn names it, or a request of another turn in the batch will make it |
| `who-meets` | 0 | Rule 3: an act that names a shared lock goes to the meeting task |
| `routed` | 8 | A lock that comes to seal a value gives two rows: the home's row with no bytes, on the task of the request's uuid, and the whole row on the task of the value's entity. A new lock's home is the act's uuid |
| `decided`, `decide-or-send` | 7 | Call `opened`; carry the lock rows of an act that meets |
| `rows-in-write-order`, `rows-to-send` | 2 | Put each home's row of a lock before any other row |
| `new-pass`, `lay-over`, `decide-next` | 14 | A pass holds the lock rows that it wrote. A lock that a request of the pass makes has no bytes yet, so the check is given a row drawn for that call alone |

**`module/streaming_gate.clj`.** `new-lock-row` (2): 32 fresh random bytes at the write. `add-to`
(17): declares `$$streaming-locks`; step 6 reads one range of locks for each request that the act
names; the writes of the kinds `:lock` and `:new-lock`.

**`module/microbatch_gate.clj`.** `new-lock-row` (2). `lock-rows-in` (8): the ranges, with a yield.
`write-row` (6): the kinds `:lock`, `:lock-home` and `:new-lock`. `add-to` (7): declares
`$$microbatch-locks`; step 2 reads the ranges that were asked; step 4 draws a lock row for each act
of a pass.

**`module/door_reads.clj`.** `add-to` (11): the query `locks-of-request`, held to the closing of the
request's batch in a microbatch layer, where it gives a lock that seals a value without its bytes.

**`record/rows.clj`.** `lock-key`, `lock-row`, `lock-row-sealing`, `lock-row-without-bytes`,
`seals-a-fact?`, `lock-of`, `locks-by-index`, `locks-by-index-used-without-bytes`, `lock-range-read`,
`lock-range-read-by-routing-id` (21 in all): the key and the rows of a lock, and the tests at the
head of the read of locks. `fact-row` (4): takes the plain bytes that a lock opened. `held-value`,
`plain-bytes`: the String of a row's value has a second form, U+0000 and one character for each byte,
for bytes that are no text; and `read-value` has a fourth answer, `[:not-a-value]`. Both are there
because a gate keeps what a lock opened, whatever the bytes are (`:362-393`).

**`record/tables.clj`.** `locks` (3): the schema of a lock's row.

**Docstrings only, no code.** `act.clj`, `door/text.clj`, `door/digest.clj`, `module.clj`,
`read_path.clj` and `read_path/answer.clj`. Two of them say a thing that is a job: `module.clj:77-79`, that the acts of a layer depend on the depot's
order for "a request and the first use of its lock"; and `door/digest.clj:19-25`, that the digest of
a thing is taken with its sealed bytes, fed as bytes and not as hex. The code of the read path names
no lock: the four hits of the search there are Clojure's `locking`.

### 3.2 In the proofs, by file and form

The count after each name is its code lines that name a lock, the seal or the stock. A name with no
count is a check; the others are the proof's own hands and tables of cases. Read as names and
counts; the checks that a line of this paper leans on were read one by one.

**`act_proof.clj`: 2 forms, 3 lines.** `parts-cases` 1, `refusal-rows` 2. Both say that the maker's
form refuses a fact that carries a seal or a lock id as `:unknown-part`. That stays true.

**`pure_proof.clj`: 76 of 175 forms, 570 lines.** Hands and tables: `lock-id`, `sealed-fact`, `good`,
`digest-cases` 3, `declared-shapes` 3, `declared-rules` 10, `declared-reasons` 7,
`declaration-parts-cases` 5, `whole`, `whole-form`, `tools-own-keys`, `sealed-under`,
`passing-cases` 10, `makers-rows`, `doors-rows` 23, `id-rows` 4, `order-rows` 8, `many-facts`,
`limit-rows` 8, `seal-cases` 22, `making`, `first-act-rows`, `record-cases` 33, `early-cases`,
`row-by`, `places-rows` 7, `places-cases` 4, `seal-fixture` 8, `sealing`, `seals-rows` 13,
`seals-cases` 22, `writes-cases` 23, `streaming-step`, `on-fact`, `by-value-cases` 3, `base-locks`,
`base-sealed`, `arrival-cases` 23, `reply-cases` 26, `item`, `meeting-cases` 7, `deciding-cases` 31,
`routing-cases` 19, `write-row`, `run-batch`, `one-at-a-time` 6, `differences` 6, `flat-of`,
`record-of`, `batch-fixture` 14, `making-fixture`, `marks-fixture` 10, `rows-by-place`, `door-words`
4, `read-refused`, `door-words-cases`, `said`, `locks-of-a-read`, `locks-for`, `built`,
`not-an-act-cases` 13, `additions-cases` 27, `with-locks-of` 4, `writing` 6, `stock-cases` 69. Checks
whose own code names one: `the-declaration`, `the-maker's-check-inside-the-door's`, `the-seal`,
`the-records-rows`, `the-decision`, `a-batch-is-decided-in-one-order`,
`a-batch-run-twice-with-one-clock`, `the-doors-additions`, `the-doors-stock`. The other nine checks
run the tables above.

**`module_proof.clj`: 45 of 146 forms, 348 lines.** The hand: `open-hand`, `locks-of` 5,
`ask-for-locks!` 6, `take-lock!` 3, `seal-with` 3, `sealed`, `lock-row`, `ask-for-base-locks!` 6,
`base-lock-key`, `empty-the-hand!`, `base-snapshot`, `as-the-tables-hold-it?`. Checks: `layers-are-made`
2, `an-act-lands` 16, `refusals-and-repeats` 20, `settings-and-permissions` 7, `the-path-of-a-lock`
38, `strays-and-limits` 8, `the-reads-take-anything` 4, `what-a-fact-keeps` 25,
`the-keys-that-take-a-second-write` 5, `what-a-permission-allows` 2, `what-points-at-a-thing` 5,
`an-act-lands-in-the-base` 22, `an-answer-is-readable-when-its-batch-is-closed` 5,
`refusals-and-repeats-in-the-base` 8, `one-batch-one-order` 8, `a-person-acts-in-the-base` 4,
`an-act-on-several-tasks` 22, `what-a-fact-keeps-in-the-base` 17,
`the-keys-that-take-a-second-write-in-the-base` 4, `what-a-permission-allows-in-the-base` 2,
`what-points-at-a-thing-in-the-base` 2, `one-batch-under-a-marked-key` 4,
`the-path-of-a-lock-in-the-base` 25, `a-place-of-the-base-never-gives-nothing` 5,
`after-the-module-is-started-again` 7, `the-stamps-and-the-depot` 4,
`the-base-after-the-module-is-started-again` 10, `the-stamps-and-the-depot-of-the-base` 4,
`a-batch-runs-a-second-time` 16, and the helper `seen-before-closing` 4. Of its 33 checks, 4 name
none: `what-rama-carries`, `the-base-is-made`, `a-long-batch` and
`each-gate-tells-the-other-gates-layers`.

**`door_proof.clj`: 37 of 89 forms, 198 lines.** Hands: `open-round`, `request?`, `locks-round`,
`lock-rows` 4, `key-of-lock`, `lock-ids-of`, `locks-asked-in`, `stocks-of`, `stock-told`, `opened` 4,
`without-the-random-parts`. Checks: `the-door-is-opened` 3, `locks-and-the-seal` 11,
`the-door-and-the-hand-send-the-same-thing` 12, `the-digest-is-the-gates` 2, `things-that-are-no-act`
13, `a-refused-request` 4, `a-holder-of-see` 1, `a-request-in-doubt` 6, `reads-that-throw` 16,
`the-seat-and-the-wait` 10, `a-requests-answer-with-another-digest` 10, `an-act-over-three-runs` 2,
`the-openers-numbers` 2, `a-who-that-cannot-be-a-key` 3, `a-lock-never-crosses-a-who` 4,
`many-makers-at-once` 6, `the-base-through-the-door` 3, `a-wait-for-locks-that-passes` 6,
`a-request-in-doubt-in-the-base` 2, `an-act-id-with-the-other-depot-word` 3,
`the-module-is-started-again` 9, `the-doors-memory-starts-again` 20, `no-locks-bytes-anywhere` 17. Of
its 27 checks, 4 name none in code: `an-act-with-no-value`, `round-trips-are-counted`,
`the-answer-back`, `an-append-that-threw`. Two of those four still rest on the seal: 5.1 says how.

**`read_path_proof.clj`: 13 of 76 forms, 43 lines.** `open-round!`, `keeping-pushes` 3,
`the-read-path-is-opened`, `the-four-things-that-a-place-can-hold` 7, `the-stores-own-facts`,
`a-very-large-run` 2, `one-push-for-one-thing` 5, `the-read-paths-own-bound-on-its-pushes` 5,
`reads-that-fail` 10, `the-first-thing-a-function-is-given` 5, `a-batch-that-runs-a-second-time`,
`the-checks-of-the-first-cluster`. Most of these lines count the door's requests among what was
sent. One check makes a thing by the seal that no other road makes:
`the-four-things-that-a-place-can-hold` seals, by the proof's own hand, bytes that are no value of
the form and bytes that are no text, so that a place holds them (`:538-560`).

**`read_path_pure_proof.clj`: none. `read_measures.clj`: 1 form, 3 lines** (`m2`).

### 3.3 The jobs, as the code shows them

A job is what a use does for the store, whether anyone meant it. "Named" says whether the contract's
A5 names it. The contract names three (J4 in its narrow form, J5, J6). Twenty-two are listed; the
plan says where each goes.

| # | The job | What kind | Where the code does it | Named |
|---|---|---|---|---|
| J1 | A value in the log can be made unreadable for good: a lock is destroyed and the log's copy cannot be opened | Meant (Sid, 6 and 9 October). No code destroys a lock, and no forget is built | `door/seal.clj:9-16`; `door.clj:107-110` | By entry 2 and by the contract's "Not in this build", not by A5 |
| J2 | One lock for one value: the grain of a forget | Meant (9 October, entry 2) | `gate/decision.clj:428`; `door/declaration.clj:579-581` | By entry 2 |
| J3 | Nothing secret is on a depot | Meant (rule 5) | `module.clj:56-58`; `door/seal.clj:54-57` | By entry 2: every value is plain in the log |
| J4 | A stop before the log. With an empty stock, a value act is stopped by the refusal of the door's request, and nothing of the act is appended. The request carries the act's who, permission, session, version, layer and depot word, so the stop holds for each refusal that a gate gives the request for those: an unknown version, no session, a permission that is not in the act's layer, a layer that is not made, a layer of the other gate or the wrong depot word, a who that is not the permission's holder, a permission that does not stand, a permission that says `:see` | A side effect: "As it falls out" (the store's page, line 281) | `door.clj:526-569`, `:814-819`; `door/additions.clj:416-442`. A proof of each kind: `a-refused-request`, `a-holder-of-see`, `an-act-id-with-the-other-depot-word` | In part: A5 and finding 1 name "a maker who may not act" |
| J5 | A tie of a sealed value to its place: sealed bytes that are copied to another layer, entity or key do not open | Meant (rule 16) | `door/seal.clj:36-38`, `:127-136`; `gate/decision.clj:430` | Yes |
| J6 | A bound on the bytes of one value, 65,536 on the sealed bytes, which is 65,508 plain bytes. Held in two places: by the door before the append, so that such a value reaches no log through the door; and by a gate | A bound | `door/additions.clj:228-232`, `:376-379`; `door/declaration.clj:76-79`, `:566-570` | Yes, as one bound that moves. That the door held it before the log is not named |
| J7 | A tie of a sealed value to a layer and a depot through the lock's id, and by the door alone to a who: a lock goes only to an act of the who that asked for it | The first a check of a gate, the second the door's own rule | `gate/decision.clj:386-392`, `:427`; `door.clj:610-613` | No |
| J8 | One accepted fact for one run of sealed bytes: a second value under a lock that seals one is refused, and so are two facts of one act under one lock. A sealed fact that is copied out of the log into another act of the same place is refused | A refusal word; a side effect of J2 | `gate/decision.clj:428`; `door/declaration.clj:326`, `:579-581` | No |
| J9 | A check that the bytes of a value arrived as they were sealed: the cipher's tag fails for bytes that were changed | A refusal word; a side effect of the cipher | `door/seal.clj:157-169`; `gate/decision.clj:430-432` | No |
| J10 | A stop before the log, of the door's own: a value that is no value of the form, and a value in a fact that is a map and no plain map, are not appended. The reason in the source: "after the seal no gate can tell" | A stop | `door/additions.clj:26-29`, `:200-203`, `:223-224` | No |
| J11 | A stop before the log, of the door's own: an act with more than 256 values is not appended; and the walk of facts that have no end stops at the 257th value | A stop and a bound | `door/additions.clj:220-221`, `:248-252` | No |
| J12 | Which bytes stand for a value. The door makes them: the UTF-8 of the one text of the value's normal form. A gate keeps what a lock opened and does not read it. So a thing that is appended without the door can put any bytes into a fact's row, up to 65,508, bytes that are no text among them; and a read then says `[:not-a-value]` | The first meant (the door's); the second a side effect: the one road by which bytes that are no value reach a row | `door/additions.clj:227`; `gate/decision.clj:456-458`; `record/rows.clj:224-259`, `:362-393` | No |
| J13 | Every value under a plain key crosses the depot, and Rama's writing and reading of a record, as a run of bytes. Only values under store keys cross it as Clojure data today: a keyword, a vector of ids, a count, a set of keys | A side effect | `door/additions.clj:385`; `door/declaration.clj:162-197` | No |
| J14 | Each send of an act with a value is another thing: other locks and a fresh random part, so another digest. A maker's own second send of one act under one act-id reads "not mine" for one of the two | A side effect | `door.clj:828-830`, `:905-910`; `door/seal.clj:143-144`. The check: `the-answer-back`, `test-store/softland/store/door_proof.clj:837-884` | No |
| J15 | A pace on one who. One call at a time asks for one stock; a request asks for 4 to 64 locks in a streaming layer and up to 256 in the base; in the base a request's answer is readable only when its batch is closed, and the stock gives nothing meanwhile; a call that waits ends after 10 seconds | A side effect of the stock. Reasoned from the size rule and the seat; no run measured it as a pace | `door/stock.clj:151-187`; `door.clj:627-632`, `:648-659` | No. Entry 7 names "a bound on how much one sender can put into the log" as not built |
| J16 | An order that the depot must hold: a request before the first use of its lock | An ordering | `module.clj:77-79` | No |
| J17 | Acts that no maker made. Each request is a record on a depot of acts, with an answer, a stamp and a row of the facts table at `<layer> <the request's uuid> :softland.store/lock-request` that holds the count. A read that asks for the store's own facts is shown that row | A row that a read shows; a count of things on a depot | `door/additions.clj:437-442`; `record/rows.clj:44-49`; `read_path/answer.clj:93-107`; the receipt, section 8 | No |
| J18 | A record of which request, and so which who, permission and session, asked for the lock that seals a fact; and from a lock to the fact. The door's page keeps it as room for forgetting a person by her requests | A row; meant as room, used by nothing | `record/rows.clj:675-681`; `door.clj:107-110` | No |
| J19 | One of the two reasons for closing, and one of the two things that a second attempt of a batch makes anew. The store's page: "A second attempt makes other lock bytes, and can give an act into a layer of the other gate the other of two words. So a reader could see a lock's bytes or a word that later change." | A reason for another part's rule | The store's page, lines 666-685 and 245-250; `module/door_reads.clj:43-59` | No |
| J20 | The door's memory, its sixth call and two of its numbers: the stocks, `stock-count`, and the numbers that an opener may give | The memory of a process; a count that a caller reads | `door.clj:942-954`, `:689-716` | A1 names the stock. The call and the numbers are not named |
| J21 | A stop before the log, of the stock's making: an act with a value whose `:who` cannot be hashed or compared is not appended | A stop | `door.clj:663-668` | No |
| J22 | A home for two numbers that are not a lock's: the longest wait that the door counts, and the sleep between two reads of a maker's wait for an answer | A place in a file | `door/stock.clj:84-88`, `:103-110`; `door.clj:335`, `:933` | No: the contract says that the file holds nothing else |

**Costs that go with them, as the source counts them.** These are no jobs; D measures what is left.
One read of a lock's row and one AES-GCM opening for each sealed value in a gate; one row of a lock
written again, and in the base two; one event of a gate and about 550 bytes "for good" for each
request (`door/stock.clj:167-168`); for a first value in a layer, three round trips in a streaming
layer, and in the base the time until the request's batch is closed (`door.clj:797-802`); the largest
case that the store's page names, 32,768 rows of locks read for one act; and the hazard under "Who
reads a lock": any client of the cluster can read a lock's bytes.

## 4. Every table that a gate writes, part by part

The ground for B3: "The plan lists every table and every part of a row, and says for each what it is
made again from." This section says what each part is made from today, and by which function. It
says of no part what a rebuild makes it from. Every write of a table in the module's code was found
by a search for the one call that writes (`local-transform>`): eight in the streaming gate's
dataflow, eleven in the microbatch gate's, one in the topology that keeps a batch's clock. The
schemas are read at `record/tables.clj:128-204`.

"The act" below is the thing on its depot. "The decision" is what the gate's checks gave for it. "A
clock" is a reading of `System/currentTimeMillis` inside a gate. "Earlier rows" are rows of the same
gate's tables as they were when the act was decided.

### 4.1 The answers: `$$streaming-answers`, `$$microbatch-answers`

One row for each act-id under which a gate decided a thing. Written once: check (a) ends every later
thing under the act-id before any write (`module/streaming_gate.clj:210-211`;
`gate/batch.clj:584-602`). Made by `rows/answer-row` (`record/rows.clj:201-208`), handed on by
`decision/writes` (`gate/decision.clj:620`).

| Part | Made from |
|---|---|
| The key, `"<layer> <act uuid>"` | The act: the first and third slots of its act-id (`rows/answer-key`, `:170-178`). The depot slot is not in it |
| `:refused` | The decision: the word of the first check that failed, absent for an accept. The checks in their order, and what each reads: the declaration's check, the thing alone (`door/declaration.clj:661-684`); the 10-second rule, the act and a clock (`gate/decision.clj:92-105`); the standing, the act and two earlier rows, and for a layer of which the gate's own record holds nothing the other gate's row of that layer and, in the streaming gate, the other gate's `:closed-through` (`:139-191`; `module/streaming_gate.clj:231-237`); the places, the act and earlier rows (`:299-330`); the seals, the act and rows of locks (`:435-467`) |
| `:stamp` | In the streaming gate: a clock read in the event, the task's last stamp, and the greatest stamp that the act carries (`module/streaming_gate.clj:216`, `:311-312`; `gate/stamp.clj:79-82`). In the microbatch gate: the batch's base plus the act's turn (`gate/batch.clj:495-502`). The base is made from the batch's kept clock, the gate's last stamp before the batch, and the greatest stamp that any thing of the batch carries, held to 10 seconds ahead (`:475-493`). The turn is the act's position in its partition's part of the batch, times the number of tasks, plus the partition's number (`:176-185`) |
| `:digest` | The act: the SHA-256 of the one text of the thing as it arrived, 32 bytes (`door/digest.clj:41-54`) |

### 4.2 The facts: `$$streaming-facts`, `$$microbatch-facts`

One row for each place, the current fact. Written only for an accepted act, one row for each of its
facts; a later accepted fact at the place writes over it. Made by `rows/fact-row`
(`record/rows.clj:281-305`), handed on by `decision/accept-writes` (`gate/decision.clj:544-569`).

| Part | Made from |
|---|---|
| The key, `"<layer> <entity> <key>"` | The act: its layer, and the fact's `:e` and `:k` (`rows/place-key`, `:180-186`) |
| `:value` | Absent for a retract. Under a store key: the act, the one text of the fact's `:v`. Under a plain key, today: the act's sealed bytes and the bytes of a lock, which the gate reads from its table of locks (`gate/decision.clj:430`). `held-value` makes the String |
| `:by-act`, `:by-index` | The act: the uuid of its act-id, and the fact's position in it |
| `:stamp` | The decision's stamp, as in 4.1 |
| `:who`, `:permission` | The act: the one text of its `:who`, and of the permission that it names |
| `:level-since` | Only in the row at a layer's marked-keys place. A map from each key that the new fact marks to a stamp. A key that the row before held keeps the stamp that the row before gave it: an earlier row. A key that is newly marked gets, in the streaming gate, the stamp of the act that marks it; in the microbatch gate, the last stamp that the act's batch can hold (`gate/decision.clj:485-496`, `:553-555`; `gate/batch.clj:574-582`, `:864`) |

In the microbatch gate a row under one of five store keys is written on every task
(`gate/batch.clj:210-231`, `:816-818`; `module/microbatch_gate.clj:559-563`). So one accepted fact
under such a key is as many rows as the module has tasks.

### 4.3 The facts by value: `$$streaming-facts-by-value`, `$$microbatch-facts-by-value`

For a fact under a key that the layer marks. Made by `decision/by-value-writes`
(`gate/decision.clj:498-532`), with `rows/by-value-key` and `rows/by-value-row`.

| Part | Made from |
|---|---|
| The key, `"<layer> <key> <since> <value digest> <entity>"` | The act: its layer, the fact's key and entity. `<since>`: an earlier row, the stamp that the layer's marks row gives the key. `<value digest>`: the SHA-256 of the plain bytes of the value that the fact's row holds |
| `:by-act`, `:by-index`, `:stamp`, `:who`, `:permission` | As in 4.2: the fact's row without its value |
| That an entry is put | The decision, and which marks the gate held for the act. In the streaming gate: the layer's marks as they were before the act (`gate/decision.clj:355-371`). In the microbatch gate: the marks as every task held them when the act's batch began (`gate/batch.clj:93-103`). An entry is put for each fact with a value that the function is called for; it does not compare the fact's stamp with the key's |
| That an entry is taken out | An earlier row: the row before at the fact's place. Its key is taken out only when that row has a value, its stamp is later than the key's stamp, and the digest of its value is not the new one's |

Nothing takes out an entry whose key holds an earlier stamp of the mark (F12). In a pass of the
microbatch gate, of two acts of one batch that write one place under a marked key, one row is sent
for each key, the last (`gate/batch.clj:1102-1116`).

### 4.4 The locks: `$$streaming-locks`, `$$microbatch-locks`

| Part | Made from |
|---|---|
| The key, `"<layer> <request uuid> <index>"` | The request: its layer, the uuid of its act-id, and an index below its count |
| `:bytes` | The JVM's random source, at the write (`door/seal.clj:120-125`). In no act, no answer and no depot |
| `:way` | A number of the code, 1 |
| `:seals-act`, `:seals-index` | The act that uses the lock, and the fact's position in it |

A goes: these two tables leave the module.

### 4.5 What the streaming gate keeps for a task: `$$streaming-task`

| Part | Made from |
|---|---|
| `:last-stamp` | The stamp of the decision that was made last on the task. Each decision writes it, an accept and a refusal (`gate/decision.clj:621`; `module/streaming_gate.clj:346-347`). It is read before each stamp is made |
| `:strays` | A map from one of five words to a set of digests as hex text. For a thing that is a stray: the word is `declaration/stray-reason` of the gate's depot and the thing; the member is the digest of the thing (`module/streaming_gate.clj:198-202`). The set is on the task of the depot partition where the thing sits. A thing that comes twice is one member. The write is not behind check (a) |

What a gate keeps of a stray is made from the thing on its depot and from nothing else: no row, no
clock, no order. The contract's B3 says of it: "what a gate keeps of a stray rests, by the store's
page, on an order that no depot holds (rule 19)". The store's rule 19 names an order between the two
depots for another thing: "which of the two words an act sent to the wrong depot got, the word for a
layer of the other gate or the word for a layer that is not made" (the store's page, lines 812-815).
That act is no stray: its act-id names the depot where it arrived, so it gets an answer, and the
word is the answer's `:refused`. P8.

### 4.6 What the microbatch gate keeps for a task: `$$microbatch-task`

| Part | Made from |
|---|---|
| `:strays` | As in 4.5, with the depot `:microbatch`, on the task where the depot put the thing (`module/microbatch_gate.clj:394-399`) |
| `:turns`, `{:batch :given}` | The text of Rama's microbatch id of the run, and how many things of that run the task has given a turn. Written for each thing that the task takes, a stray too (`:384-392`; `gate/batch.clj:159-174`). After a batch it holds the id and the count of the last batch in which the task took a thing |
| `:last-stamp` | On the gate's task only. The last stamp that a batch can hold: its base, plus the greatest position in the batch plus one, times the number of tasks, minus one (`gate/batch.clj:504-509`; `module/microbatch_gate.clj:447-451`). Written only for a batch that took a thing that is no stray |
| `:closed-through` | On every task. The gate's last stamp as it was before the run, written by the first run that finds it other than the gate's task's `:closed-through` (`gate/batch.clj:511-518`; `module/microbatch_gate.clj:452-455`). So it follows the last stamp one run behind |

### 4.7 The kept clock of a batch: `$$batch-clock`

No gate writes it: the topology `batch-clock` does. It is here because entry 1 says "Every table".
One row, under the word `:microbatch`: `{:batch :clock}`, the microbatch id and the clock of the
newest batch that kept one. Made by `batch/clock-row-to-keep` from the records of the depot of batch
clocks: a record is written when the table holds no row or a row of an older batch, and a record of
the row's own batch writes nothing (`gate/batch.clj:442-467`; `module/batch_clock.clj:90-96`). So of
two records of one batch the row holds the one that the topology processed first. That is the first
on the partition when the topology takes them in order; its source has Rama's default retry mode, in
which a failed record is tried again alone and a later record can be processed before it
(`module/batch_clock.clj:72-75`; `11-stream-topologies.md:158`, read, not run).

### 4.8 Where a row is

A table is one part on each task. The task of a row is made from the module's number of tasks and a
hash: of the act's `:layer` for every row of a streaming layer; in a microbatch layer, of the act's
uuid for an answer, of the entity for a fact, of the value digest for a row of the facts by value,
and of the word `:microbatch` for the gate's last stamp; a stray and a count of turns are on the
partition where the depot put the thing (`record/tables.clj:77-88`; `gate/batch.clj:45-49`). The
number of tasks is set at launch and is in no depot.

### 4.9 Which parts come from something that is in no depot today

Facts of the code. The contract names two: "what a gate keeps of a stray", and the last stamp of a
batch.

| # | The part | What it comes from that no depot holds | In B1's four things? |
|---|---|---|---|
| 1 | A lock's `:bytes`; and so the `:value` of every fact under a plain key | The random source; the value needs the lock | Goes with A: the value is then in the log of acts (entry 1, fourth point) |
| 2 | Each `:stamp`, of an answer, of a fact, of a row of the facts by value | The streaming gate: a clock read in the event, and the order in which the task decided. The microbatch gate: the clock is on the depot of batch clocks; the base and the turn need the batch's cut, which things of which partitions fell into which run, and that is on no depot (the store's page, rule 9) | Yes: "the stamp" |
| 3 | Each `:refused` whose check read a clock, an earlier row, or the other gate's record | The same clock; the order of the decisions; for a layer of the other gate, what that gate had made by then, and in the streaming gate how far that gate had closed | Yes: "refused or not with the word" |
| 4 | `:level-since`, and `<since>` in a key of the facts by value | In the streaming gate the stamp of the act that marked the key, which is that act's answer's stamp. In the microbatch gate the last stamp of the batch | The first: yes. The second: no; the contract's B3 says "a log comes to hold it" |
| 5 | The microbatch gate's `:last-stamp` | The last stamp of the batch, as in 4 | No; the same sentence of B3 |
| 6 | The streaming gate's `:last-stamp` of a task | The stamp of the last decision on the task: 2 | Yes, when every decision on the task has an entry and each entry says which task it was made on, or that can be found |
| 7 | `:closed-through` | The gate's last stamp before a run, and that a later run of the gate took place | No. The contract does not name it |
| 8 | `:turns` | Rama's microbatch id, and the batch's cut | No. The contract does not name it |
| 9 | The task of each row, and how many rows a fact under one of the five keys is | The module's number of tasks | No. The contract does not name it |
| 10 | The row of `$$batch-clock` | It is on a depot. Which of two records of one batch it holds is the order in which a topology processed them | It is a log of its own (B3) |
| 11 | What a gate keeps of a stray | Nothing: the thing on its depot gives the word, the digest and the task | Not needed. The contract says otherwise: P8 |

Two parts are made from earlier rows and from nothing else that is new: which stamp a key that stays
marked keeps (4.2), and which entry of the facts by value is taken out (4.3). So whatever makes
these tables again must know, for each act, the row before of each of its places under a marked key
and of the marked-keys place, as they were when the act was decided. That is a property of the rows,
and no way of doing it is said here.

## 5. The states and the edges that the three changes imply

Each is traced as a question. Where the contract's words settle it, the line says so. Where they do
not, it points to an open point of section 8, and nothing is chosen here.

### 5.1 Part A: an act at each point from maker to table, once nothing is sealed

| Point | Today, read in the source | What the contract's words imply | Not settled |
|---|---|---|---|
| 1. The maker's act | The one form, with plain values (`act.clj:156-176`) | The same | |
| 2. The door reads it | Four tests of the door's own additions, with five words: a plain map (`:read-the-act`); no `:act-id` and no `:depot` already (`:add-its-parts`); a layer that an act-id can hold, and an act-id given back that is of that layer (`:make-an-act-id`); the walk of the facts (`:add-its-parts`, `:seal-a-value`, `:seal-that-many-values`, `:read-the-act`). With a word, nothing is appended (`door/additions.clj:314-344`) | A1: the door seals nothing, so the walk has nothing to do. A4: "The door checks nothing of an act before it appends it … as its first rule says already" | Which of the first three tests stay, since each leaves a thing in no log: P1 |
| 3. The parts that the door reads | `:layer` and `:facts` for its additions; `:who` for the stock; `:who`, `:permission`, `:version`, `:session` for the request (`door/additions.clj:106-112`) | With no stock and no request the door has no use for `:who`, `:permission`, `:version`, `:session`, or for `:facts`. R4: "No part of this build puts a look at a permission before the log" | |
| 4. The act-id | Given back, or made with one read of the layer's depot (F6) | The same | |
| 5. The thing to append | The maker's map with `:act-id` and `:depot`; `:facts` as a vector; each value fact without `:v` and with `:sealed` and `:lock-id` (`door/additions.clj:387-412`) | A1: "appends each act as the maker gave it". The two additions stay: a gate answers under the act-id, and the declaration asks for `:depot` | "The form in which a fact is appended" is the builder's to choose. Whether `:facts` that came as a list goes as a list: the one text tells a list from a vector, and the declaration takes both |
| 6. The digest that the maker is given | Of the thing appended, with its sealed bytes (`door/digest.clj:19-25`). Two sends of one act give two digests (J14) | Of the thing appended, with its plain values. One act under one act-id has one digest, however often it is sent | What a maker's own second send then reads: P6 |
| 7. The append | For a value act: a request before it when the stock is short, then the act. `:ack` to the streaming depot, `:append-ack` to the microbatch depot (`door.clj:212-217`) | One append for one send, and no append of the door's own making. The two ack levels stay | |
| 8. On the depot | Each value under a plain key as sealed bytes; all else plain; for ever | Every value plain (entry 2); for ever (entry 3). Through the door the log now takes what the door stopped: a value act of a maker whose request a gate would have refused (J4); a value that is no value of the form (J10); an act with more than 256 values (J11); a value over the limit of bytes (J6); a value act whose who cannot be hashed (J21), when Rama's client can write it | |
| 9. The gate's digest | Of the thing as it arrived. It is the door's digest because the one text is blind to what Rama's writing of a record may change (`door/text.clj:52-60`) | The same rule now carries every value that `value?` takes: a string, a double, a big integer, a decimal, a nested map with nil in it. Today only bytes and the values of store keys cross the depot (J13) | That it holds for each kind of value as a fact's `:v` is shown by no run of this store. The module's proof shows one thing of each kind inside a thing that is no act |
| 10. The declaration's check of a fact | The engine's leaf `:value` reads `:v` first, under any key, and gives its normal form; then the rule refuses a plain `:v` under a plain key (`act.clj:326`; `door/declaration.clj:546-550`) | A2: "The rule that refuses a value that is not sealed goes, and the rules and the words of the seal with it." What is left by the engine's own rules: a `:v` that is no value of the form, or is inside more than 32 collections, is `:malformed`. A fact that carries a part that the shape does not name is `:unknown-part` (`act.clj:354`). A fact under `:softland.store/lock-request` is `:store-key-not-known` once that key leaves the table (`door/declaration.clj:484-487`) | What a gate says of a thing in today's form, with `:sealed` and `:lock-id`: P4 |
| 11. The bound on the bytes of a value | On the sealed bytes: an array's length, at no cost (`door/declaration.clj:566-570`) | A6: "counted on the plain value". A gate holds the bytes of a plain value only as the one text that it makes for the row of an accepted act (`record/rows.clj:304-305`). So a bound that is held before any row is read needs a count of that text, or of something else, for each value of each act, a refused one too | What is counted, the number, and its word: P3 |
| 12. The gate's decision | Standing, places, then the seals: four words, one read of a lock's row and one opening for each value | The last check is gone: an act that passes the places is accepted. A gate reads and writes no row of a lock | |
| 13. The fact's row | `:value` is the String of what the lock opened: any bytes (J12) | `:value` is the one text of the value in its normal form: the branch that store keys use today (F7). The value digest of the order by value is the same for one value (F7) | Whether the second form of `held-value` and the fourth answer of `read-value` stay, since no gate's write gives them: P5 |
| 14. The answer, and the door's read of it | `answer-by-act-id`; the four answers | The same. "Not mine" is then said only of a thing that differs (J14) | P6 |
| 15. The door's reads of the module | Three queries; five handles | A3: no read of a request's locks. Two queries and four handles | |
| 16. The door's memory | The stocks; five numbers fixed at the opening | A1: "The door then holds nothing in the memory of its process" | The two numbers of a maker's wait: P2 |

**What goes away with the request for locks, beside the locks.** The store's own keys are five where
they were six: `:softland.store/lock-request` leaves (A3; entry 2). The door makes no act of its own, so each
record on a depot of acts that came through the door is a maker's (J17). A value act is not held
behind an earlier act of the same who (J16), and nothing paces a who's values through the door
(J15). Closing keeps one of its two reasons (J19): a second attempt of a batch can still give an act
into a layer of the other gate the other of two words.

**What today's proofs assert that part A makes untrue.** By group, not by line. "Goes" means that
the thing which the check held is gone; "changes" means that the check holds a thing that stays and
states it in a way that becomes false.

- *The act's proof.* Nothing. Its two forms that name a seal say that the maker's form refuses one.
- *The pure proof.* Goes: `the-seal`, `the-doors-stock`. Changes: `the-declaration` and
  `refusals-of-the-declaration` (ten rules and their words; the shapes of an arrived fact);
  `the-order-of-the-reasons` (the order of the rules is the order of the reasons, and ten leave it);
  `the-one-text-and-the-digest` (its cases with sealed bytes); `the-records-rows` (a lock's key and
  rows; a row from what a lock opened); `the-decision` (the check of the seals; the rows of an accept
  with lock rows in hand); `the-batch`, `a-batch-is-decided-in-one-order` and
  `a-batch-under-marked-keys` (questions, replies, meeting and routing over locks; every value of
  their fixtures is sealed); `a-batch-run-twice-with-one-clock` (which things differ on a second
  attempt); `the-doors-words` (fourteen words); `the-doors-additions` (three additions; the walk; the
  request).
- *The module's proof.* 29 of its 33 checks send a value that its hand sealed. Goes:
  `the-path-of-a-lock`, `the-path-of-a-lock-in-the-base`; in `the-stamps-and-the-depot` and its base
  twin, "no lock's bytes are on the depot"; in `the-reads-take-anything`, the read of locks. Changes:
  every check that reads a fact back, since its act went in sealed and now goes in plain;
  `what-a-fact-keeps` and its base twin, which hold the plain row against the sealed log;
  `strays-and-limits`, for the limits of sealed bytes and of locks; `a-batch-runs-a-second-time`, for
  the lock bytes that a second attempt made anew; `refusals-and-repeats`, where a repeat is the same
  sealed bytes sent again.
- *The door's proof.* 23 of its 27 checks name a lock. Goes: `locks-and-the-seal`,
  `a-refused-request`, `a-request-in-doubt`, `the-seat-and-the-wait`,
  `a-requests-answer-with-another-digest`, `a-lock-never-crosses-a-who`,
  `a-wait-for-locks-that-passes`, `a-request-in-doubt-in-the-base`, `the-doors-memory-starts-again`,
  `no-locks-bytes-anywhere`, `a-who-that-cannot-be-a-key`. Changes: `the-door-is-opened` (five
  handles); `the-openers-numbers` (five numbers); `the-door-and-the-hand-send-the-same-thing` ("but
  for the random parts"); `things-that-are-no-act` (the seal's words); `a-holder-of-see` and
  `an-act-id-with-the-other-depot-word`, each of which is stopped at the request today and would
  then be a recorded refusal under the act's own act-id; `reads-that-throw`, `many-makers-at-once`,
  `the-base-through-the-door`, `an-act-over-three-runs`, `the-module-is-started-again`, which count
  or time requests. And two of the four that name no lock: `round-trips-are-counted`, whose counts
  are of a send that meets a request; and `the-answer-back`, which holds that "a maker's own second
  send of one act with a value is another thing: another digest; one of the two reads its own answer
  and the other reads not mine" (`door_proof.clj:871-872`).
- *The read path's proof.* Changes: `the-four-things-that-a-place-can-hold`, two of whose four are
  made by a seal of the proof's own hand; and the checks that count the door's requests among what
  was sent.

**Where a check could pass for a wrong reason after A** (finding 16 asks the review to look). Reasoned
from the forms above, not from a run. A check that holds two digests to be unequal, where the seal's
random part was the reason and another difference now is. A check that reads nil or an empty map and
takes it for "nothing was made", where the table or the query that it read is gone. A check that
holds a row's `:value` against a String made by the same function from the same value. A check that
expects a refusal and names no word, where the thing is now refused earlier, as `:unknown-part`, for
carrying a seal.

### 5.2 Part B: what the keeping of an answer and a rebuild meet

What must hold, in the contract's words, before the edges. R1: "A rebuild decides nothing again. It
applies the answer that the log holds." B3: it "reads the logs of acts, the log of answers and the
log of the clocks of batches, and no table". B4: its tables are compared "row by row" with what the
gates wrote. B5: "An entry of an attempt that did not count does no harm." R7: "A second attempt of
an event or of a batch leaves the store as one attempt would." R3: the tables stay the record that
is read. "When this build passes: … each answer that a gate gave is in a log".

What Rama gives, each read and not run by this session. A stream topology's writes to its tables
"will be discarded" when its event throws or the process dies, and the record is processed again
(`11-stream-topologies.md:170`). A record that completed can be handed again after a restart
(`:166`; the rig ran it: `RIG.md`, finding 2 under its table of claims, as a finding). The skill, of
a stream topology: without a commit boundary "the PState writes and the depot append are in the same
atomic group", and a reader of those tables could "miss the writes entirely if there's a failure
after the append but before the PStates commit" (`stream.md:60`); its rule is "ALWAYS add a commit boundary before
`depot-partition-append!` to an internal depot in a stream topology" (`stream.md:49`). An append
from a microbatch topology is not exactly-once: "A retry re-appends" (`microbatch.md:127`;
`12-microbatch-topologies.md:130`); the probe ran that on 1.6.0 and saw the record of a failed
attempt stay. An append from a topology goes to the partition of the task that the code is on
(`14-depots.md`, "Appending to depots from topologies"). An append with `:ack` must not go to a depot
that the appending stream topology itself consumes (`depot-design.md:66`).

| # | The edge | What the code or Rama shows | What must hold, as a property | Open |
|---|---|---|---|---|
| 1 | An act is in a log of acts and has no answer yet | Between its append and its decision; in the base until its batch runs. `answer-of` gives "nothing yet" | A rebuild at that moment gives no answer and no fact for it, as the tables do | The moment at which "equal" is asked: P13 |
| 2 | An entry is in the log of answers for an attempt that did not count | In a stream event: when the append stays and the event's writes are discarded. In a batch: when the attempt fails after its append | The tables made again hold the answer that the gates' tables hold, and no row of the entry that did not count (B5, R7) | P11, P12 |
| 3 | What such an entry can differ in, in the streaming gate | The second run reads the clock anew: another stamp (`module/streaming_gate.clj:113-115`). The verdict can differ too: the 10-second rule is held against the new clock; and after a failure a later act of the layer can be decided first, so the rows that the checks read are others (`:120-124`). It is under the same act-id with the same digest | A rebuild tells the two apart by something other than the act-id and the digest | P11 |
| 4 | What such an entry can differ in, in the microbatch gate | Every attempt takes the batch's kept clock, so the stamp and the verdict are the same, but for one word: an act into a layer of the other gate can get the other of two words (`module/microbatch_gate.clj:180-185`). After A no lock's bytes differ | Two entries of one act that are equal do no harm whichever is taken; the one case of the word is told apart | P11 |
| 5 | An answer is in the tables and no entry is in the log | When the tables' write commits and the append does not follow, a second run ends at check (a) and writes nothing (`module/streaming_gate.clj:108-113`) | "each answer that a gate gave is in a log" | P12 |
| 6 | An entry is in the log, and no record of a log of acts has its act-id and its digest | Not from the gates: a gate decides only what it took from its depot. From a log that was cut or restored apart, which this build does not make; or from two records under one act-id of which the entry is of neither's digest, which the code cannot make | The rebuild names such an entry; it does not make a row from an answer alone | |
| 7 | The same thing twice under one act-id | The second is a repeat: check (a) ends it, and nothing is written (`:41-45`). After A the door's own second send is this (J14) | One answer, one set of rows | |
| 8 | Two different things under one act-id | The first has the answer, with its digest. The second "gets no answer of its own" (`gate/batch.clj:905-907`). In one batch of the base the least turn has the act-id (`:584-590`). In a streaming layer the first in the depot's order has it, and after a failure the first that was decided | The rows are of the thing whose digest the answer holds, and of no other | |
| 9 | One thing on two depots | On the depot that its act-id names it is an act; on the other it is a stray of that gate, with the word `:act-id-of-the-other-gate` | Each log of acts is read as the depot that it is | |
| 10 | The order between the two logs of acts | No depot holds it. It decided one thing: for an act into a layer of which a gate's record held nothing, whether the other gate had made the layer by then, and so which of two words it got, or whether a making act was accepted | The word and the accept are in the answer (B1). No row of one gate is made from a row of the other | |
| 11 | The order between two partitions of the base | The batch and then the turn; which batch a thing fell into is a fact of timing (the store's page, rule 9) | The stamps hold it: one last stamp for the gate, and no two things of a batch share a turn (F9) | |
| 12 | The order after a failure in a streaming layer | A later act can be decided before the failed one is tried again | The stamps hold it: strictly later on the task at each decision (F9) | The setting that would close the window is Sid's, after D measures it |
| 13 | The first act of a layer | Accepted only when the gate's record holds nothing of the layer and the other gate's does not hold it. Two rows: the layer's depot and its first permission; in the base each on every task | The two rows, from the act and its answer | |
| 14 | A stray | No answer. Its digest under its word, on the task where it arrived. A stray of the base takes a turn, so it moves the stamps of the things after it in its batch; those stamps are in their answers | The set, from the thing on its depot (4.5) | P8 |
| 15 | A refused act, and the act of a maker who may not act | An answer with a word, a stamp and a digest; no fact (entry 7: "no fact of it is in the tables") | The same | |
| 16 | A retract | A row with no value at its place. Under a marked key the entry of the value before is taken out and none is put (`gate/decision.clj:507-514`) | The same, which needs the row before as it was at the decision (4.9) | P10 |
| 17 | A key is marked | In a streaming layer the key's stamp is the marking act's own. In the base it is the last stamp of the batch, and each act of that batch was decided with the marks as the batch began | The same `:level-since`, and so the same keys of the order by value | The last stamp of a batch is in no act and no answer (F8); the contract: "a log comes to hold it" |
| 18 | A mark of a key and a fact under that key in one batch of the base | The gate puts no entry for that fact, whatever the two turns are: the fact was decided with the marks as the batch began (`module/microbatch_gate.clj:64-70`). A walk that applies the acts one by one in the order of their stamps meets, at the fact, a marks row that the gate did not have when it decided the fact, when the mark's turn is the earlier one; and `by-value-writes` puts an entry for every fact that it is called for | No entry for that fact. The rule that gives it for both gates is the store's own sentence: the order of a marked key holds every fact under that key whose stamp is later than the key's stamp, and no other | |
| 19 | The fact that marks a key is itself under a marked key, or marks its own key | "A fact of the act that writes the mark of its own key is not in it" (`gate/decision.clj:360-362`) | The same | |
| 20 | A mark is taken off and put on again | Another stamp, so other keys. The rows of the earlier marking stay, and no read names them (F12) | "Row by row" meets them: the tables made again hold them too, or they are an exception that is named with its reason | P9 |
| 21 | Two acts of one batch write one place | Both are accepted and each has an answer. One row is written at the place, the later turn's; the earlier act's row is never in the table. For a key of the facts by value the last row of the pass is sent, which can be a key taken out that was never put | The same final rows | |
| 22 | A fact of the base under one of the five keys | One row on each task | The same on each task: it rests on the number of tasks (4.8) | P9 |
| 23 | The gate's last stamp, made again | It is read before the next stamp is made, and it is the stamp of a key that is marked in its batch | Not below any stamp that the gate gave, and not below any `:level-since` (F10) | |
| 24 | `:closed-through` and `:turns` | Made from the runs of the gate, and from Rama's id of a run (4.6) | Named: what each is made again from, or why it is not | P9 |
| 25 | The depot of batch clocks | One record for each batch with things, and one more for each later attempt that found no clock kept (`module.clj:66-68`). Of two records of one batch the kept one is the first that was processed (4.7) | The row of `$$batch-clock` that the topology wrote | P9 |
| 26 | The id of a batch | The row of `$$batch-clock` holds one, and "rests on one thing of Rama's: the id of a batch goes up and never comes again" (`record/tables.clj:198-202`). `:turns` holds one as text | Tables that are made again hold ids of the runs that decided the acts. Whether a gate can go on from them is the use on a running store, which the contract leaves out; the paper names it so that the receipt can | |
| 27 | An accepted act whose thing no longer has the declared shape | The rows of an accept are made from the act in its one form, which the declaration's check gives (F11). R1: "A gate whose rules were changed after an act was decided gives that act no new answer" | The answer that the log holds is applied | What the rows are made from when the check of today refuses the thing: P14 |
| 28 | The number of tasks | The turn, the last stamp of a batch, the task of each row and the count of rows under the five keys are made with it | The comparison of B4 is made on a store with the number of tasks that decided the acts, or says what differs | P9 |

**What must hold of the log of answers itself.** From the contract, each as a property. It holds, for
an act: "the act it is for, refused or not with the word, the stamp, and the digest of the act", and
"No value of a fact is in it" (B1). Nothing reads it but a rebuild (B2); it is no second road to an
answer for the door, for a gate or for a tool (R3). No trimming option is set on it (B6, entry 3).
Nothing is shown before it is in the tables (R5): a reader never learns of a decision from the log
first. A gate's event or batch that runs a second time leaves it such that the rebuild still comes
to the tables' answer (B5, R7).

**Which decisions get an entry.** The contract says "for an act". The code has five kinds of thing
that arrive. A thing that the declaration refuses: it is no act in the form's sense, and it has an
answer with a word, a stamp and a digest. An act that the checks refuse, and an act that is
accepted: an answer each. A repeat, and a second and different thing under a used act-id: no
decision and no write. A stray: no answer, and one member of a set. So "what it decided for an act"
reads as each thing that got an answer's row, and a stray and a repeat have nothing to keep. The
strays' sets are a table that a gate writes all the same (4.5).

### 5.3 Part C: the three bounds on a permission's depth, and what each counts

A permission is `[who layer granted-in]`, or that with a fourth slot that holds the permission
above, or that with a fifth slot, a word (`act.clj:186-194`). "Depth" below is how many permission
ids one permission holds, itself counted: a first permission has depth 1.

| The number | Where | What it counts | What it is held on |
|---|---|---|---|
| `act/max-permission-depth`, 4 | `act.clj:117-119`, through `nesting` (`:199-202`) and the engine (`:393-399`) | How many times the named shape `:permission-id` is around a place, the place's own counted. A depth of 5 fits no shape | Both places where a permission is read as a shape: the act's own `:permission`, a part of the act; and the value of a fact under `:softland.store/permission`, which the rule `:permission-fact-holds-a-permission` hands to the same engine with a count that starts anew |
| `act/max-depth`, 32 | `act.clj:106-109`, in `value?` (`:265-290`) | How many collections are around a part of a value: a collection that has 32 or more around it is no value. A permission of depth d is d vectors, the innermost with d - 1 around it. So a permission of depth 32 is a value, and one of depth 33 is not | A fact's `:v` only, by the leaf `:value`. So it bounds a permission that is given. It does not bound an act's own `:permission`: that part is no value and the leaf never reads it |
| `text/max-depth`, 64 | `door/text.clj:74-80`, `:198` | How many collections are around a part in the one text: a part with more than 64 around it is written `#too-deep` | Every text, counted from the thing whose text is taken. For the digest of a thing that is the act: its own `:permission` has 1 around it, so a permission of depth 64 there writes its innermost ids as `#too-deep`; a given permission is in a fact, in the facts, in the act, with 3 around it. For a row's `:permission`, and for the value of a permission's row, the text is of the permission alone, with nothing around it |

**What follows when the 4 goes, each reasoned from the lines above.**

1. The engine gives a named shape that has no number in `nesting` the number 1: "A name that is not
   here can be inside itself no times, so no shape goes round" (`act.clj:199-201`, `:397`). So "the
   number goes" cannot mean that the shape has no entry there: every given permission would be
   refused. Some bound stays on the shape, or the engine's rule changes: P16.
2. A given permission is bounded at 32 by `value?` whatever the shape allows. The leaf reads `:v`
   in the shape's pass, before any rule. So a given permission of depth 33 is refused `:malformed`
   by the leaf. Today a given permission of depth 5 is refused `:permission-fact-not-a-permission`
   by the rule. The word for "too deep to give" changes.
3. An act's own `:permission` has no bound but the shape's. Its bound is whatever point 1 leaves.
   The contract's "Kept: as deep as a value may be, 32 today" is true of a given permission by the
   code, and of an act's own only if point 1 is made so.
4. Whether a deeper own permission could ever stand: no. A permission's row is written by a making
   act, of depth 1, or by a give whose value passed `value?`, of depth 32 at most. Check (d) holds
   the text of the act's permission against the text in the row (`gate/decision.clj:175`;
   `record/rows.clj:331-341`). A text with `#too-deep` in it is equal to no row's text, since no row
   was written from one. So no act stands under a permission that is deeper than every bound; what
   the bound decides is the word that it is refused with, and what it costs before it is refused.
5. What a deep permission costs before any check refuses it. The engine walks it, one call inside
   another for each depth (`act.clj:379-449`); `ids-of` walks it the same way for the limit of an
   id's text (`door/declaration.clj:423-431`); a throw there, a stack that runs out among them, is
   caught and gives `:malformed` (`:683-684`). The digest walks the whole thing before the check
   (`module/streaming_gate.clj:214-215`), and stops at 64. Rama writes and reads the thing before
   any of this.
6. What a deep permission costs after it stands (finding 14). Its one text is a field of each fact's
   row and of each row of the facts by value. Each id's text is 256 characters at most, and a
   permission of depth d holds 3d ids; so its text is up to about 780 characters for each depth:
   about 3 KB at depth 4 and about 25 KB at depth 32. An act of 256 facts writes that text 256
   times, and once more for each of its facts under a marked key: up to about 6.4 MB of permission
   text for one act at depth 32, where it is about 0.8 MB at depth 4. Rama sends whole rows to each
   push (F14). These are reckoned from the limits, not measured.
7. C2's bound on the size of an act can only be a gate's: the door checks nothing (A4). So an act
   over it is in the log whole, and is refused there.
8. "A gate still reads none of the permissions above an act's own" (the contract; the store's page,
   lines 505-508). Lifting the 4 changes no read.

**What today's proofs assert that part C makes untrue.** The act's proof: "a permission five deep"
is `:malformed`, with and without the slot (`act_proof.clj:435-440`), and its line of limits
(`:191`, `:534`). The pure proof: the same for an arrived act, and "a permission fact whose value is
five deep" (`pure_proof.clj:631`, `:651`). The module's proof, in `settings-and-permissions`: "a
permission five deep is no permission: the holder of one four deep cannot give"
(`module_proof.clj:960-961`). The pure proof's check of the text at 64 and 65 collections stays
(`pure_proof.clj:302-304`).

## 6. Operations

An operation here is a call or a step that this build changes or adds. For each: how soon its effect
is needed, what drives its volume, what must always be true, what grows, what happens when two come
at once, and its edges. No number below is a target: the contract sets none. A count of reads and
writes is "counted from the dataflow, not measured", as the store's page says of its own.

### 6.1 A send through the door (`send-act`), part A

- **How soon.** Today a send of a value act whose stock holds locks is one append, and one read of
  the layer's depot before it when the maker gave no act-id. With an empty stock it is three round
  trips more in a streaming layer, and in the base the time until the request's batch is closed
  (`door.clj:797-802`). After A every send is the one append and at most the one read. The read
  build measured the streaming road at 100 acts a second, from the call of `send-act` to its return,
  with locks taken from a stock: a middle of 4.5 ms in-process (the receipt, section 4, M1). D
  measures the same shape after.
- **Volume.** One record on a depot of acts for each send. Today also one request for each 4 to 64
  values of a who in a streaming layer, and for each 4 to 256 in the base; after A none.
- **What must always be true.** The door changes an act by two additions and no more, the act-id
  and the depot; it fills no missing part, reorders no fact and names no permission
  (`door.clj:811-812`, less the third addition). The digest that the maker is given is the digest of
  the very thing that was appended. An ack is a signal and never the answer. No call throws for what
  a maker gave. The door keeps nothing of a send once the call returns. It makes no act of its own.
  It puts no look at a permission before the log (R4). It seals no value, asks for no lock and keeps
  no stock (A1).
- **What grows.** The logs of acts, with every value plain, a refused act's value too (entry 7),
  for ever (entry 3). Nothing in the door: today its memory grows with the whos that wrote a value
  through it (`door.clj:62-68`).
- **Two at once.** Today two calls of one who that both find the stock short take turns at the
  stock's seat. After A nothing in the door orders two sends. Two sends that one maker makes one
  after the other still reach the depot in that order, since a call returns after its append
  (`door.clj:821-826`).
- **Edges.** A thing that is no plain map; a thing that already holds `:act-id` or `:depot`; a layer
  that is no id; an act-id of another layer: P1. A value that is no value of the form, an act with
  more than 256 facts, a value over the bound: each goes into the log and is refused by a gate
  (5.1, point 8). A thing that Rama's client cannot write: the append throws, and the word is
  `:append-cleanly`, the one word of which "nothing was appended" cannot be said. A thing that has
  no end: today the walk ends it at the 257th value and does not end it otherwise; with no walk it
  is Rama's client that meets it. The same act sent twice under one act-id: one thing, one digest,
  and a repeat at the gate (P6).

### 6.2 A gate's decision of an act whose values are plain, part A

- **How soon.** In the streaming gate the decision is on the way of the maker's append, which waits
  for it. One accepted act with one value under a key that is not marked costs 5 reads and 4 row
  writes today (the answer, the permission, the lock, the marked keys, the last stamp; the fact,
  the lock, the answer, the last stamp). Without the lock it is 4 reads and 3 row writes. In the
  microbatch gate it is 4 reads, 4 row writes and 6 messages today (the store's page, "What one
  accepted act costs the microbatch gate"); without the lock and its two rows, 3 reads and 2 row writes; the
  messages were not counted again. One opening of a seal less for each value. One count of each
  value's bytes more, if the bound of A6 is held where the bound of today is (5.1, point 11), and
  for a refused act too.
- **Volume.** The acts of a layer, on its task; in the base, the things of a batch. Entry 7 adds the
  acts that the door's request used to stop: each now reaches a gate and is decided. The store's
  page says what such an act costs: 4 reads at most in the streaming gate, and in the microbatch
  gate up to 256 reads of places, since an act whose permission's place is empty asks for each of
  its rows (lines 276-282). Sid was told of the cost on one layer's thread (entry 7, last point).
- **What must always be true.** Every check of today but the seals, in today's order, with today's
  words. A refusal is data; no gate throws on what arrives. One act-id has one answer. Each write is
  a whole row at a fixed key, so a second run writes the same keys. An accepted value is in its
  fact's row as the one text of its normal form, and the order by value keeps a value where it was
  (F7). A gate makes no lock and opens none (A2). A gate keeps nothing in the memory of a process.
- **What grows.** One answer for each act-id, for ever. No table of locks; today one row for each
  lock, used or not, for ever.
- **Two at once.** As today: one event for one act on a layer's task; in the base, the turn. Acts of
  one batch no longer meet over a lock.
- **Edges.** A `:v` of `false`, of an empty string, of an empty map: each is a value and no retract;
  only nil is a retract (`act.clj:175`; `record/rows.clj:304`). A value whose text is exactly at the
  bound. A value inside 32 collections, and inside 33. A fact that carries `:sealed` or `:lock-id`
  (P4). A value under a store key: as today.

### 6.3 The door's other calls, part A

`open`: four fetches where there were five; what an opener may give is P2. `act-id-for` and
`act-ids-for`: unchanged. `answer-of`: unchanged in what it reads and gives; it reads the tables by
act-id and never the log of answers (R3); its bound and its sleep are two numbers that live in the
stock's file today (P2). `stock-count`: the call has nothing to count (J20).

### 6.4 The keeping of an answer in a log, part B

- **How soon.** It is added to every decision. In the streaming gate the maker's append waits for
  the event, so whatever the keeping costs there is paid by each streaming act; D asks for that
  number. The one thing run so far is of the other gate: an append with `:ack` from inside an
  attempt of a batch, to a depot that a stream topology of the module consumes, cost about 3 ms
  for a batch on one machine with one copy, "a floor and not a cost" (the probe, B3). The contract's
  three runs before the plan are for the stream topology.
- **Volume.** One for each thing that gets an answer, accepted or refused: the volume of the
  answers' rows. And more after a failure: one for each attempt that kept one and did not count.
- **What must always be true.** It holds the act that it is for, refused or not with the word, the
  stamp and the digest, and no value of a fact (B1). Each answer that a gate gave is in a log. The
  tables stay the record that is read, and nothing but a rebuild reads the log (B2, R3). Nothing is
  shown before it is in the tables (R5). A second attempt leaves the store as one attempt would
  (R7), and an entry of an attempt that did not count does no harm (B5). No trimming option is set
  (B6). A gate still gives the same answer when Rama runs its event again after a commit: check (a)
  comes first.
- **What grows.** The log, for ever, never cut (entries 2 and 3). "Nobody has priced the disk"
  (entry 3).
- **Two at once.** The streaming gate's event has no partitioner and does not give its task back,
  so that no other act of the layer runs between its reads and its writes
  (`module/streaming_gate.clj:94-100`; the store's page, lines 283-285). Whatever keeps the answer
  leaves that true. In the microbatch gate an act is decided in step 3 on the task where it arrived,
  or in step 4 on its layer's meeting task; an append from a topology goes to the partition of the
  task that the code is on (read, not run).
- **Edges.** 5.2, edges 2 to 5.

### 6.5 The rebuild, part B

- **How soon.** It is on no act's way and no read's. The contract sets no time. Its use on a
  running store is not in this build.
- **Volume.** It reads every record of every log: its work grows with all that was ever sent. Sid,
  12 October: "there is cost to be paid for replay".
- **What must always be true.** It decides nothing (R1). It reads the logs and no table (B3; P10).
  It makes each table that the gates write, and each part of each row, or the part is named with
  its reason (B3; P9). Its rows are the rows that the gates wrote (B4). It cuts nothing out of a log
  and writes no migration of a depot (R2). It keeps no store of its own beside what it makes (R5).
- **What grows.** What it makes. The form is the plan's.
- **Two at once.** Not asked by the contract: the proof runs it after the acts are decided (P13).
- **Edges.** 5.2, all.

### 6.6 The comparison, part B

A proof, not a part of the store. It must reach every table of section 4 on every task, and each
part of each row. What it sends is in B4; section 9 adds what the contract asks elsewhere and B4's
list does not hold.

### 6.7 An act under a deep permission, and a give, part C

- **How soon.** The same events as 6.2. What depth adds is in 5.3, points 5 and 6: walks that go as
  deep as the permission before any check, and one text of the whole chain in each row.
- **Volume.** Every act carries its own permission whole. Every accepted fact's row holds its text.
- **What must always be true.** A permission can be deeper than four. Every bound on its depth
  stays below the text's 64 (C3). A bound on the size of an act is held (C2), by a gate. The receipt
  says how deep a permission can be.
- **Edges.** A give at each depth from 4 to 33; an act whose own permission is deeper than any
  permission that was given; a permission with the fifth slot at each depth; a making act, whose
  permission has three slots and no more.

### 6.8 The measures, part D

Three, each beside `reads/runs/measures-final.txt`, each with no live read open or saying that one
is: an act through the door and a gate; what the keeping of an answer adds to an act of the
streaming gate and to a batch of the microbatch gate; and Rama's
`topology.stream.max.executing.per.task` at 1, at 100 acts a second and at the most that one maker
holds. The build leaves that setting as it is; the result goes to Sid. One thing to hold in view
when the first is read beside M1: M1's sends took their locks from a stock, so the round of a
request is not in its middle; it is in the first sends of a who.

### 6.9 What is kept for ever after this build

The two logs of acts, with each value plain and each refused act whole. The log of answers, with
each entry of an attempt that did not count. The depot of batch clocks. Whatever comes to hold the
last stamp of a batch. One answer's row for each act-id. One member of a set for each different
stray. The rows of the facts by value of each earlier marking of a key. The store can forget none
of it (entry 2: "The store can forget nothing until a later build").

## 7. Entity State × Write Matrix

The reads that are asked of every state: **A**, the answer by act-id (`answer-of`); **P**, the read
path at a place of the act; **L**, the record on a log of acts; **N**, the log of answers, which
only a rebuild reads; **S**, the count of strays, an operator's select. Each line says what the read
gives after the write, and why.

### 7.1 A thing under an act-id

```
Nothing under the act-id  x  a send through the door, or an append without it
  - A: "nothing yet" until a gate decides; then the answer, with the digest of this thing
  - P: no change until an accept
  - L: the thing, with each value plain, for ever; also when a gate will refuse it (entry 7)
  - N: nothing until the decision
  - S: no change, unless the thing is a stray

On its depot, not decided  x  the gate's event, or its batch: accepted
  - A: accepted, with the stamp; in the base only when the batch is closed
  - P: each fact of the act at its place; a fact of the base as soon as its task shows the batch
  - L: unchanged
  - N: what the gate decided: the act it is for, accepted, the stamp, the digest
  - S: no change

On its depot, not decided  x  the gate's event, or its batch: refused
  - A: refused, with the word and the stamp
  - P: no fact of the act anywhere
  - L: unchanged: the value of a refused act stays in the log
  - N: the act it is for, refused with the word, the stamp, the digest
  - S: no change

Decided  x  the same thing again under the act-id
  - A: the first answer; its digest is this thing's too, so the sender reads it as its own
  - P: no change
  - L: a second record of the same thing
  - N: no new decision was made; what a second record leaves there is the plan's to trace (edge 7)
  - S: no change

Decided  x  another thing under the act-id
  - A: the first answer; its digest is not this thing's, so its sender reads "not mine"
  - P: no change: no fact of the second thing
  - L: a second record, of the other thing
  - N: nothing for the second thing: it "gets no answer of its own"
  - S: no change

A stray  x  it arrives, once or many times
  - A: "nothing yet", for ever
  - P: no change
  - L: one record for each arrival
  - N: nothing: a stray has no answer
  - S: one more under its word the first time, and no more after

Decided by an attempt that did not count  x  the event, or the batch, runs again
  - A: the second run's answer, and never the first's: the first was never visible
  - P: the second run's rows
  - L: unchanged
  - N: what the first attempt kept can still be there, beside the second's; for an act of the base
       the two are equal but for one word, and for a streaming act they can differ in the stamp and
       in the verdict (edges 3 and 4)
  - S: no change

Decided, and its tables lost or wrong  x  a rebuild
  - A: the answer that the log holds, with the same word, stamp and digest (R1)
  - P: for an accept, each fact that is still the current fact of its place, with the same row
  - L, N: unchanged: a rebuild writes to no log of acts and cuts none (R2)
  - S: the same set, from the things on the depot
```

### 7.2 A place in a layer

```
No row  x  an accepted fact with a plain value
  - P: the value, as its maker gave it; who wrote it and under which permission when asked
  - read-value of the row: [:value v]
  - the order by value, when the layer marks the key: one entry, under the key's stamp

A value  x  an accepted fact with another value, with or without :replaces
  - P: the new value; one row, the place's current fact
  - the order by value: the new entry in; the entry of the value before out, when that row was
    written since the mark

A value  x  an accepted retract
  - P: "nothing here"; the row is there with no value
  - the order by value: the entry of the value before out, and none in

Any  x  a refused act that names the place
  - P: no change; nothing of the act is in any table but its answer

Any  x  two accepted acts of one batch of the base
  - P: the later turn's row; the earlier act's row was never in the table
  - A: each act has its own answer, accepted

A row of a value that is no value of the form (through a seal, today)  x  after A
  - no gate's write makes one; a read's fourth answer has no road to it (P5)

Any  x  a rebuild
  - P: the same current row at the place, in each of its parts
```

### 7.3 A marked key in a layer

```
Not marked  x  an accepted fact under the key
  - the order by value: no entry
Not marked  x  an accepted mark
  - the marks row: the key, with a stamp: the marking act's in a streaming layer, the last stamp
    of the batch in the base
  - the order by value: nothing is filled from before the mark
Marked  x  an accepted fact under the key, with a stamp later than the key's
  - the order by value: its entry, under the key's stamp and the digest of its value
Marked in this batch of the base  x  a fact under the key in the same batch
  - the order by value: no entry, whatever the two turns are (edge 18)
Marked  x  the mark is taken off
  - the marks row: the key is not in it
  - the order by value: the rows of that marking stay; no read names them
Taken off  x  marked again
  - the marks row: the key, with another stamp; so other keys of the order by value
Any  x  a rebuild
  - the marks row, and each row of the order by value, as the gates left them; the rows of an
    earlier marking are among what "row by row" meets (edge 20)
```

### 7.4 A permission: one holder in one layer

```
None  x  a making act of the layer
  - the permission's row: the first permission, depth 1
A permission of depth d that says "see and act"  x  its holder gives one beneath it
  - today: accepted when d is 1 to 3; refused :permission-fact-not-a-permission when d is 4
  - after C: accepted while the given permission, of depth d + 1, is within every bound; what it
    is refused with at the bound is in 5.3, point 2
A permission of depth d  x  an act under it
  - the act's answer: accepted or refused by the same checks at every depth
  - each fact's row: the one text of the whole permission, about 780 characters for each depth
An act that names a permission deeper than any that was given  x  it arrives
  - its answer: refused; with which word, and after how much walking, is 5.3, points 3 to 5
Any  x  a rebuild
  - the permission's row, and the text in each fact's row, the same
```

### 7.5 A batch of the base

```
Running, first attempt  x  the gate keeps what it decided
  - A: nothing of the batch: its answers are held to closing
  - P: nothing until a task shows its part
  - N: what the attempt kept, as soon as the log shows it; a rebuild that read it now would hold
       an answer that the tables do not (P13)
Failed  x  the second attempt
  - A: still nothing
  - P: rows that are the same as the first attempt's, where a task had shown them
  - N: the first attempt's entries stay; the second's are equal but for one word (edge 4)
Committed, not closed  x  the next run of the gate
  - A: the answers, now: the batch is closed
  - the gate's :closed-through, on every task: the gate's last stamp of before that run
Closed  x  a rebuild
  - every row of the batch; the gate's last stamp, which can be later than each act's (F8);
    :closed-through and :turns as 4.6 and P9 say
```

### 7.6 The tables as a whole

```
As the gates wrote them  x  a rebuild from the logs
  - every read of the door and of the read path: the same as before it, at every act-id and place
  - the comparison of B4: equal, row by row, but for each exception that is named with its reason
As the gates wrote them  x  a gate's rule is changed in code
  - A, for an act that was decided before: the same answer, also after a rebuild (R1)
Made again  x  the gates decide a new act
  - not in this build: the use of a rebuild on a running store (edge 26 names what it meets)
```

## 8. Open points for the session that holds the build to rule on

Each has the words in question, what the options are, and what each would mean. This paper chooses
none. "The contract, line n" is a line of `BUILD-before-real-data.md`. `[builder's]` is engineering,
for the session that holds the build. `[Sid's]` is his, and the build decides none of it. Where a
builder's point has a side that Sid should hear of, the head says so.

### P1 `[builder's]`, and it meets entry 7: what the door still drops

The words. The contract, line 242: "The door checks nothing of an act before it appends it (entry
7), as its first rule says already." Sid's page, lines 654-655: "An act from a maker who may not act
is written to the log, and then refused. The door checks nothing first." His line there: "we are not
choosing the destructive choice here." R11, lines 354-356, says that this line reaches a builder's
own defaults, and marks that as "the cutter's reading". The code, `door/additions.clj:15-25`: "The
door checks nothing of the act's form … What is tested here is only what the door's own additions
need", and of a thing that fails those tests: it "would get no answer, ever, and its maker could not
tell it from an act that is on its way".

What is at stake. With the seal gone, three tests of the door are left that end in a word and in no
append: the thing is no plain map; it already holds `:act-id` or `:depot`; its layer cannot be in an
act-id, or the act-id given back is not of its layer.

- **They stay.** This is what "as its first rule says already" reads as: D1 has stood beside these
  tests since the door was built. A thing of one of the three kinds is in no log; its maker is told
  with a word of the door's.
- **They go, and the door appends what Rama's client can write.** Such a thing is then in a log, as
  a stray or under an act-id that the door wrote over a maker's own part. It gets no answer, and its
  maker reads "nothing yet" for ever.

Either way the door looks at no permission, so R4 holds. The first drops three kinds of thing that
a maker handed over; whether that is "the destructive choice" in Sid's sense is not said by any line
of his: he spoke of a maker who may not act.

### P2 `[builder's]` "The door then holds nothing in the memory of its process"

The words. The contract, line 236. And line 162: "The two files that hold nothing else,
`door/seal.clj` and `door/stock.clj`". The code: the door "holds its five numbers, fixed at the
opening" (`door.clj:45`); `answer-of` sleeps for `:read-every-ms` between two reads and cuts a
maker's bound to `stock/longest-wait-ms` (`door.clj:335`, `:933`); both are declared in
`door/stock.clj` (`:84-88`, `:103-110`).

- **"Nothing" means no state that changes between two calls.** The door keeps its handles and the
  numbers of a wait, fixed at the opening, in another home than the stock's file. `open` still takes
  a map of numbers, with two names where it had five.
- **"Nothing" means no number either.** The sleep and the bound of a wait are constants of the code,
  or the maker gives them to `answer-of`. `open` takes no numbers.

It decides what `open` takes and what becomes of the check `the-openers-numbers`.

### P3 `[builder's]` The bound on the bytes of one value

The words. The contract, lines 249-250: "The bound on the bytes of one value stays, counted on the
plain value. It is 65,536 today, on the sealed bytes." The code: `max-sealed-bytes`, "How many bytes
one sealed value can hold … it bounds the bytes of one row" (`door/declaration.clj:76-79`); a seal
is 28 bytes longer than its plain bytes (`door/seal.clj:109-118`).

Five things the words leave open.

1. *The number.* 65,536 on the plain bytes takes a value 28 bytes longer than the longest of today.
   65,508 takes the same values as today. Either is "the builder's number, kept".
2. *What is counted.* The UTF-8 of the value's one text is what was sealed and what the row holds.
   Another count, such as what Rama writes for the value, is not what the row holds.
3. *Where it is held.* Today the bound is the first thing a gate can know of a sealed value: an
   array's length. A plain value has no length until its text is made. Held at the declaration, as
   today, it costs one walk of each value of each act before any row is read, a refused act's too.
   Held where the row is made, it costs nothing more, and it comes after every read of the decision.
   The order of the reasons follows from the choice.
4. *Values under store keys.* They are plain today, and no rule counts their bytes; other limits
   hold them. "The bytes of one value" either takes them in or stays a rule of plain keys.
5. *The word.* `:sealed-value-too-large` is a word of the seal and goes (A2). A word is made (R8).

And the door no longer holds this bound before the log (J6, A4): through the door a value of any
size that Rama's client can write is in the log.

### P4 `[builder's]` A thing in today's sealed form, after the change

The words. The contract, lines 237-239: "The rule that refuses a value that is not sealed goes, and
the rules and the words of the seal with it." Line 251: "No log is written again. Nothing real is in
one, and the proofs make their own." The contract does not say what a gate does with a fact that
carries `:sealed` and `:lock-id`, or with a request for locks.

- **The two parts leave the shape of an arrived fact.** The engine then refuses such a fact as
  `:unknown-part`, the word that the maker's form gives today for "a fact that carries a seal, a
  lock id, a layer or a mark" (`act.clj:537-539`). A request is refused `:store-key-not-known` by a
  rule that stands.
- **A word of its own** says that the thing is in the sealed form.

It follows either way that the code after this build can read no log that the code at `13a1d65b`
wrote: every value act of such a log is refused. A7 says that nothing real is in one.

### P5 `[builder's]`, and Sid should hear one part: bytes that are no value

The words. The contract, line 237: a gate "keeps it in the fact's row, as the read build left that
row". The store's page, lines 433-438: the row's String is "the text itself when the bytes are
well-formed UTF-8 … and else U+0000 and one character for each byte". `read-value` gives four
answers, the fourth `[:not-a-value]` (`record/rows.clj:362-393`). After A the row's bytes are always
the one text of a value: no gate's write gives the second form of the String or the fourth answer.

- **Both stay**, as code that no act through a gate can reach. A later road for bytes finds them.
  The read path's check of "the four things that a place can hold" loses the hand that made two of
  the four.
- **Both go** with the seal that made them needed.

The part for Sid. Today a thing that is appended without the door can put up to 65,508 bytes of any
kind at a place, under a seal, and a gate keeps them. After A no arrived value can be a run of
bytes. The sheet's last page lists "A value cannot be bytes, such as an image" among the builder's
numbers, "kept, and not decided" (`g-check.txt`, line 367). Nobody asked for that road; it closes.

### P6 `[builder's]`, for the receipt: a maker's own second send

The words. `door.clj:828-830`: "A second send of one act is another thing when the act has a value:
other locks and another random part, so another digest". `:905-907`: "A maker's own second send
reads 'not mine'." The store's rule 1 (`BUILD-store-module.md`, line 176): "A second, different act
under a used act-id must not read back the first act's answer as its own."

After A the same act under the same act-id is one thing with one digest, so both sends read the one
answer as their own, and "not mine" is said only of a thing that differs. That is what rule 1 asks.

- **Take it as it falls out.** The door's docstring and the check `the-answer-back` say the new
  thing. A maker cannot tell from an answer which of her two sends a gate decided, and both are the
  same thing.
- **Make each send differ.** That is a third addition of the door's, against "appends each act as
  the maker gave it" (A1).

### P7 `[builder's]` Which lines of the earlier handoffs change

The words. R9, the contract, lines 347-351: "This build changes these lines", and then: of
`BUILD-store-module.md`, item 7 and rules 5, 15, 16, 9, 19, and "of rule 20, the limit on the locks
of one request"; of `BUILD-door.md`, part 3, part 4's seal, and "rules D3 and D6 go with them".

Lines that this build also makes untrue and R9 does not name, each read. In `BUILD-store-module.md`:
rule 13, "a request for locks holds a count" (line 215); rule 20, "65,536 bytes in one sealed value"
(line 240), which A6 changes; rule 21, its third limit, "the locks take no more than 2 times the
bytes of the values" (line 244); item 5, a fact's row as "value or sealed" (line 119). In
`BUILD-door.md`: part 1, "Its one memory is a stock of locks" (line 110); part 7, the door's proof,
which names four cases of a lock (line 156). And D6 (line 179) holds one clause that is not a
lock's: the door "never reads a fact of the record for a maker".

- **R9's list is the heads**, and each line that the build makes untrue changes with it.
- **R9's list is the whole list**, and the other lines stand as written.

For D6: it goes whole, and then no rule of a handoff says that the door reads no fact for a maker,
though the door's docstring does (`door.clj:116-119`); or it loses its two clauses of a lock.

### P8 `[builder's]` B3's sentence on a stray

The words. The contract, lines 267-269: "One is known: what a gate keeps of a stray rests, by the
store's page, on an order that no depot holds (rule 19); the plan confirms that, or finds a log for
it." The store's page, rule 19, lines 812-815: "which of the two words an act sent to the wrong
depot got, the word for a layer of the other gate or the word for a layer that is not made: that
rests on an order between the two depots that no depot holds." The code: 4.5.

- **"A stray" there means rule 19's act that was sent to the wrong depot.** That act has an answer,
  and the word is in it. B1 puts it in the log, and nothing more is needed.
- **"A stray" means the sets of strays.** Then the code shows nothing that rests on an order. The
  plan would confirm the other thing: that the thing on its depot gives the word, the digest and
  the task.

Either way this paper found nothing that a gate keeps of a stray and that a log of acts does not
give. If the cutter knew of something else, it is not in the source that was read.

### P9 `[builder's]` names each; Sid hears each exception: parts that are made from the runs, and what "equal" means

The words. Entry 1, Sid's page, line 520: "Every table can be made again from the logs". The
contract, lines 265-267: "it makes each table that the gates write. The plan lists every table and
every part of a row … What cannot be made again is named with its reason, and goes to Sid in the
receipt." Line 275: "row by row". Lines 47-48: "with each exception named and its reason given".

The parts in question, from 4.9: `:turns` (Rama's id of a run, and the batch's cut);
`:closed-through` (that a later run took place); the row of `$$batch-clock`, which no gate writes
and entry 1's "every table" takes in; the task of each row and the count of rows under the five
keys (the number of tasks); the rows of the facts by value of an earlier marking (F12). The
contract names none of the first four.

For each, three roads.

- **Made again, equal.** For `:turns` and the rows of the kept clock that asks that something of
  each run be in a log, which is the plan's to weigh.
- **Made again to what a store at rest holds**, though not from a log: for one, a `:closed-through`
  that is the gate's last stamp. "Equal" then means equal in each part that a read or a gate's next
  decision can tell.
- **Named as an exception**, with its reason.

What "row by row" covers is part of the same ruling: each table on each task, or the rows of a table
whatever task they are on. The first holds only on a store with the number of tasks that decided
the acts.

### P10 `[builder's]` "and no table"

The words. The contract, lines 264-265: a rebuild "reads the logs of acts, the log of answers and
the log of the clocks of batches, and no table." Against 4.9: the stamp that a key keeps when it
stays marked, and the entry of the facts by value that is taken out, are made from the row before.

- **No table that a gate wrote.** The rebuild may know what it has itself made so far.
- **No table at all.** Then what the row before gives is carried some other way.

### P11 `[builder's]` How an entry that counted is told from one that did not

The words. The contract, line 263: "Nothing reads the log of answers but a rebuild." R3, lines
333-334: "The log of answers is no second road to an answer for the door, for a gate or for a
tool." Against the list of what the builder chooses, lines 371-373: "whether a topology reads it",
and "How the rebuild tells an entry that counted from one of an attempt that did not".

What the edges of 5.2 show. In the microbatch gate two entries of one act are equal but for one word
in one case. In the streaming gate they can differ in the stamp and in the verdict, under one act-id
and one digest.

- **Nothing but the rebuild reads the log.** Then what tells the two apart is in the logs
  themselves: in what an entry holds, or in the order of a log's part.
- **A topology of the module reads the log**, as one reads the depot of batch clocks today. Then
  line 263 is narrowed to R3's words: no reader, no door, no gate's decision and no tool.

The contract gives the second half of this to the builder, so line 263 is the sentence that gives
way if the second road is taken.

### P12 `[builder's]` The keeping of an answer, and the commit of the tables

The words. The contract, lines 45-46: "each answer that a gate gave is in a log". R7, lines 343-344:
"A second attempt of an event or of a batch leaves the store as one attempt would." B5, line 276:
"An entry of an attempt that did not count does no harm." The skill, `stream.md:49`: "ALWAYS add a
commit boundary before `depot-partition-append!` to an internal depot in a stream topology", and its
check of a plan fails one that has none (`artifact-plan-validation.md:63`). The streaming gate: "The
event has no partitioner and does not yield … all its writes are one group"
(`module/streaming_gate.clj:94-96`).

Two orders, each with a window, both read and not run.

- **The answer is kept in the same group as the tables' writes.** A failure after the keeping and
  before the commit leaves an entry of an attempt that did not count (edges 2 and 3). The skill's
  check fails this form.
- **The tables commit first.** A failure between the commit and the keeping leaves an answer with
  no entry, and the second run ends at check (a) (edge 5). "Each answer that a gate gave is in a
  log" then needs something that comes back for it.

The contract's three runs before the plan are what say which windows are real on 1.6.0. One reading
to rule on: whether "the store" of R7 takes in the log of answers. If it does, Rama's page says that
no order meets it to the letter in the microbatch gate, since an append is not set back; B5 is then
the contract's own answer for the log, and R7 is of the tables.

### P13 `[builder's]` The moment of a rebuild

The words. The contract, lines 274-275: "Then it runs the rebuild and compares". It is silent on a
rebuild while a gate has things that it has not decided, or a batch that is not closed.

- **Equal is asked of a store at rest**: every appended thing decided, every batch closed, the
  run after the last batch done. The proof waits for that.
- **Equal is asked at any moment.** Then edges 1 and 2 and the first row of 7.5 each need a rule.

"The use of it on a running store is not in this build" (line 375) points at the first.

### P14 `[builder's]` R1, and the one form of an accepted act

The words. R1, lines 329-330: "A rebuild decides nothing again. It applies the answer that the log
holds. A gate whose rules were changed after an act was decided gives that act no new answer."
Finding 11: a rebuild that calls `decision/writes` "makes its rows by the functions that the gates
call". `writes` takes the act in its one form, which the declaration's check gives; the normal form
of each value comes from that check (`act.clj:326`).

- **Making the one form is not deciding.** The rebuild hands each accepted thing to the
  declaration's engine for its form. An accepted thing that the engine of the day refuses is then
  an exception to name: the log says accepted, and no form is to be had.
- **The rebuild makes rows from the thing with no check.** Then the normal form of a value is made
  by something else, and a row's text must still be the gate's.

### P15 `[builder's]` "a log comes to hold it"

The words. The contract, line 270: "The last stamp of a batch is in no act and in no answer (finding
8): a log comes to hold it, since Sid's decision is every table." Sid's page, lines 542-544: "it
goes into a log too. He was told so and did not object." B3 names three kinds of log that a rebuild
reads.

Which log is the plan's. What this paper adds: a batch has two more things of its own that no log
holds (4.9, rows 7 and 8), and whatever is ruled for P9 says whether they travel with the last
stamp. If the last stamp is in none of B3's three logs, B3's list of what a rebuild reads grows by
one.

### P16 `[builder's]`, around a number that is Sid's and stays not decided: "`max-permission-depth` goes"

The words. C1, the contract, line 285: "`max-permission-depth` goes, in the act's form and in the
declaration". C3, lines 290-294: "A permission that is given is a value, and a value is inside 32
collections at most … So every bound on the depth of a permission stays below the text's". "Not
decided", lines 363-364: "Kept: as deep as a value may be, 32 today". The code: `nesting`, "A name
that is not here can be inside itself no times" (`act.clj:199-202`, `:397`); and an act's own
`:permission` is no value (5.3, points 1 to 3).

- **The shape keeps a number, and it is the value's.** One bound for both roads. A given permission
  of depth 33 is `:malformed` by the leaf; an act's own of depth 33 is `:malformed` by the shape.
  The word `:permission-fact-not-a-permission` is then never given for depth.
- **The engine learns a shape with no number**, and an act's own permission is bounded by C2's bound
  on the size of an act alone. That bound must then keep it below 64 (C3), and it must be held
  before the engine walks the permission, or the depth of the walk is the JVM's stack.
- **Another number for the shape.**

What is Sid's is how deep; the contract keeps 32 and says that he did not answer. What is the
builder's is where the number is held, for which of the two roads, and with which word.

### P17 `[builder's]` The bound on the size of an act

The words. C2, lines 287-289: "A bound on the size of an act takes its place. What it counts and its
number are the builder's to propose … A permission's whole chain is written into each row under it
(finding 14), which the bound can count." Entry 6: "a bound on the size of an act takes its place".

What a bound can count, each with what it leaves out: the ids of the permission; the characters of
the permission's text; the bytes of the act's one text; the text that an accept would write, facts
times the permission's text, which is what finding 14 says is paid. Where it is held: by a gate
(A4), so an act over it is in the log whole. What it stands beside: 256 facts, 1,024 entries of
`:based-on`, 256 subjects, 256 characters of an id, and the bound of P3; with those, an act of today
is already bounded, at about 16.8 MB of values. The number is kept and decided by nobody.

### P18 `[builder's]` R6, and the new code of part B

The words. R6, lines 341-342: "No new code takes the depots of acts to be two."

- **It forbids a new choice by a depot word, a new pair of names, and a new "the other".** Code
  that is handed a gate's depot, or a list of logs, passes.
- **It asks more**: that the rebuild and its proof take any number of logs of acts.

Section 2 is the list that either reading is held against.

### P19 `[builder's]` "As the maker gave it"

The words. A1, line 235: "The door appends each act as the maker gave it". The list of what the
builder chooses, line 370: "The form in which a fact is appended, now that nothing is sealed."

Two things the code does today that are neither sealing nor the door's two additions. It makes
`:facts` a vector when it is a sequence (`door/additions.clj:410-411`). And the bytes that it seals
are of the value's normal form (`:227`). "As the maker gave it" reads as: neither is done, a list
goes as a list, and a gate gives the normal form, as it does already (`act.clj:326`). The digest is
the same either way but for a list against a vector.

### P20 `[Sid's]` The order of acts after a failure

The contract, line 362: "Kept: Rama's default for the setting, as today (D)." D measures the setting
at 1 and the result goes to him. Nothing of this build rests on his answer: the stamps hold the
order in which a gate decided (edge 12).

### P21 `[Sid's]` How deep a permission can be

The contract, lines 363-364: "Kept: as deep as a value may be, 32 today (C3). Sid was told that
'lifted' gives about 32 and not a depth without end; he did not answer it." The queue's row
"act-kept" holds "32 collections" as kept and not decided. P16 is the builder's side of it.

### P22 `[Sid's]` What entry 7 left open, and what this build takes away beside it

Entry 7, Sid's page, lines 678-685: not built, "a bound on how much one sender can put into the log,
and a way to block a sender", due "before a stranger can reach the door"; and a session's question
"not yet asked of him: whether the owner of a place sees what a stranger sent, or only that she
tried." The build decides none. What the receipt should say beside it, as facts: the locks were the
one thing that paced a who's values through the door (J15), and four stops of the door go with them
(J4, J6, J10, J11). He decided that the door checks nothing first; the list is what that takes in.

### What this paper would put before Sid in the receipt, decided by nobody

1. No arrived value can be a run of bytes once the seal is gone (P5).
2. A maker's own second send of one act reads its answer as her own (P6).
3. Each part of a table that is not made again from a log, with its reason (P9).
4. What the door still drops, if its three tests stay (P1).
5. Which of the door's stops went with the locks, and that nothing paces a sender (P22).

## 9. What the proof of this build must be able to show

Each as a thing that can be observed. From "When this build passes" (the contract, lines 45-50), from
B4 and B5, and from the edges above.

**From "When this build passes".**

1. A maker's value is in the log as she gave it: the record on the depot holds the value plain, and
   its digest is the one that the door gave her.
2. No code of the store makes, asks for, keeps or uses a lock: a search of `src-store` finds no
   lock, no seal and no stock in code; the module has no table of locks and no read of a request's
   locks; `:softland.store/lock-request` is refused as a key that the store does not know.
3. Each answer that a gate gave is in a log: for each answer's row, of each gate, an entry with its
   act, its word or none, its stamp and its digest, and no value of a fact.
4. A rebuild makes the tables again from the logs alone, equal row by row, with each exception named.
5. A permission can be deeper than four: a give at depth 5, an act under it, and what the receipt
   says is the deepest.
6. The proofs of today pass, each changed where a line became untrue (5.1, 5.3).

**B4's list, in the contract's words.** Through the door and through no door, on both depots:
accepted and refused acts; an act from a maker who may not act; an act sent twice; a second and
different act under a used act-id; a stray; a retract; a fact under a marked key.

**What the contract asks elsewhere, or an edge of 5.2 needs, and B4's list does not hold.**

- A failed attempt of each gate, with what the log of answers holds after it (B5: "with a run").
- The first act of a layer, on each depot; and an act into a layer of the other gate (edges 10, 13).
- A mark and a fact under its key in one batch of the base (edge 18); a mark that is taken off and
  put on again, with a fact under each marking (edge 20; finding 12 says the comparison "will meet
  them", and it meets them only if the proof makes one).
- Two acts of one batch on one place (edge 21); a fact of the base under one of the five keys
  (edge 22); a batch whose last stamp is later than each of its acts' (F8).
- A thing that the declaration refuses, which has an answer and is no act in the form's sense.
- A value of each kind that `value?` takes, as a fact's `:v`, through each depot, read back equal
  and with the door's digest (5.1, point 9).

**The measures of D**, each with no live read open or saying that one is.

**What a pass does not show.** A cluster of several machines; a killed process; a move of a task's
leader; a second copy of a row; a rebuild on a store whose gates are deciding; a gate that goes on
from tables that were made again; a number of tasks other than the one that decided the acts. The
receipt's first line stays: "proven by a hand-played maker, used by no inhabitant yet".

