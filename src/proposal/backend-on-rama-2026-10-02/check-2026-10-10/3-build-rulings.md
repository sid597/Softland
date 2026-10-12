# Part 3 — what the build sessions decided by themselves

Check run of 10 October 2026. One of six independent sessions. This part reads the two build
contracts, the two build folders (`store-module/`, `door/`), and the store's own README under
`src-store/softland/store/`, and asks of every ruling a build session made on its own open point:
does it decide something about what the store IS, that Sid or a later layer has to live with, or
is it the builder's own?

Nothing in the repository was changed by this run.

Paths are under `/mnt/data/projects/Softland` unless a full path is given. `B-store` is
`src/proposal/backend-on-rama-2026-10-02/BUILD-store-module.md`, `B-door` is `BUILD-door.md` beside
it, `SM/` is `store-module/` and `D/` is `door/` in that folder, `MAP` is
`src-store/softland/store/README.md`, `DEC` is `src-store/README.md` (the decisions page).
‹id› is a line Sid typed and «id» is session text, both as the memory tool shows them.

How each claim is marked: CHECKED means I opened the file, the code, Rama's local docs or the
record of Sid's lines and it says so. DERIVED means it follows from checked things by my own
reasoning and nothing ran. NOT CHECKED means I am passing on what a build paper says.
Nothing in this file was run: no proof, no cluster.

---

## The short of it

The build sessions made 66 rulings by my count from the papers (12 + 2 + 1 + 5 for the store's
module; 22 + 8 + 10 + 6 for the door), beside the nine rules that the cutter of the first contract
wrote in as "the build session's, not Sid's". Most of the door's are the builder's own: how a
stock of locks behaves, what a wait does, the fourteen words. They can be changed later without
anyone outside the door noticing.

The ones that are not the builder's own share one root. The build was told that reads were "not
in this build", that a rebuild was not asked for, and that the runtime is the only program that
connects. Inside those three fences every ruling is careful. The rulings below are the ones where
the fence itself decided something about the store, and nobody carried the fence to Sid as a
question.

Two things were done well and should be said, because they change how much to trust the rest. The
build sessions wrote down nearly every one of these in their own papers, with "Sid should know",
and the receipt of 8 October («380c05e8», 13,303 characters) listed most of them. And where a
ruling was against one of Sid's decisions the review said so in those words (`SM/FULL_SPEC_REVIEW.md:77`,
"A ruled choice against a decision"). So this is not a case of things hidden. It is a case of
things told in one long list, in the register of "for your information", at the end of a build,
to someone who answered the list with one line: ‹8573450f› "so what next??? which one should i be
printign???". None of them came back as a question with its consequences. CHECKED, both ids.

The findings are in the order of how badly and how irreversibly they bite, worst first. 1 and 2
are the two that cannot be undone once records are kept. 3 and 4 are what 1 and 2 grow from inside
the build. 5 and 6 freeze at the first kept record. 7 to 12 land later or can be turned back. 13
and 14 are about the record of who said what. 15 is a list of small ones.

## 1. The record cannot be built again from the depots. The reason Sid gave for F2 A does not hold as built, and a build session ruled a rebuild out of scope by itself.

**What it is.** Sid decided that the record lives in the gates' tables and that there is no
second depot of decisions. His reason, in his own typing, ‹27882354› (7 October): "f2 - A, note to
self- it is recomputable given the version a act was proposed at it will be rejected every time so
storing it a new depotdoesn't make sense". CHECKED.

That reason rests on a sentence a session gave him as a fact in the option he ticked, «802316c6»:
"the acts depot *is* the replay log (every act, as sent), and replaying the gate over it rebuilds
the record in order", and "the replay you want already exists in the acts depot". CHECKED. The
decisions page then turned his note into "an act proposed at a version is refused the same way
every time" (`DEC:143-146`); those words are a session's, in no line he typed (memory tool, `who`).
CHECKED.

The build then found, in its own papers, that replaying the gate over the depot does not rebuild
the record. Six separate ways, each written down by a build session:

1. A lock's bytes are in no depot (builder's rule 5, `B-store:185-187`). The depot holds every
   value sealed. So the depot alone opens nothing: without the locks table every value ever
   written is unreadable, with the depot whole. "Every act, as sent" is not what the depot holds.
2. "A stamp holds the gate's clock time at the decision (H2 A)" (rule 19, `B-store:238`). A
   rebuild gives other stamps (`MAP:492-494`).
3. Rule 17 refuses an act that carries a stamp more than 10 seconds ahead of the gate's clock.
   At a rebuild the clock is later, so an act that was refused can be accepted (`SM/PROGRESS.md:135-136`).
   An answer a maker was given can flip.
4. Two microbatch acts on two depot partitions that replace one fact: the winner depends on
   which batch each fell into, "a fact of timing", and a rebuild cuts other batches
   (`MAP:454-461`). The other act can win.
5. After a failure the streaming gate can decide a later act of a layer before an earlier one
   (`MAP:155-164`). A rebuild reads the depot in order. Different outcome.
6. Which of two refusal words an act on the wrong depot gets rests on the timing of the other
   gate (`MAP:495-498`).

CHECKED that the build papers say each of the six. NOT CHECKED by a run: the papers themselves say
none was run ("Not reached by any proof", `SM/FULL_SPEC_REVIEW.md:227-257`). Item 1 I checked in
the code: no namespace of the module appends a lock to a depot, and the fact's row holds sealed
bytes and a pointer to a lock (`record/tables.clj:93-102`).

**Where the build session ruled.** Open point 10 of the implied spec (`SM/IMPLICIT_SPEC.md:1551-1568`)
laid out three ways: (a) "recomputable" reaches the decisions and the current facts only; (b) the
stamps come out the same too, which "changes where H2 A's 'clock time' is read from, which is
Sid's row"; (c) "a rebuild is not a requirement of this build". The build session took (c) and
wrote it as rule 19 (`B-store:236-239`). The same paper names the collision in so many words:
"*F2 A against C rule 5 and H2 A*" (`SM/IMPLICIT_SPEC.md:1636-1638`). So the session saw that two
of Sid's decisions (F2 A, H2 A) and one builder's rule (5) cannot all hold, and settled it by
taking the rebuild out of the build.

**Whose it is.** F2 A and its reason: Sid's (‹27882354›). The fact under it: a session's, given to
him in the option, never checked before he ticked. Rules 5, 17, 19: a build session's. How it came
back to him: one bullet in the receipt, under the heading "What a rebuild from the depots gives
back (read)": "The decisions and the current facts of a layer on one task. Not the locks, not the
stamps. Nothing is built for a rebuild." («380c05e8», part 2), and one line under "Open from
before, none blocking":
"Whether a rebuild must pick the same winner between two acts on different partitions. It does not
today." Neither says "the reason you gave for F2 A is not true of what was built." The decisions
page still carries the reason as standing (`DEC:143-146`). CHECKED.

**What Sid loses if this is left.**
- The tables are the only truth, and nothing says so. If the tables are lost, or a wrong
  backup of them is restored, the depot does not bring them back: values cannot be opened,
  decisions do not come out the same. Sid's own picture of the depot as the thing to fall back
  on is gone.
- Changing the shape of the record later. The build's own page says reads "can still change the
  shape of the record" (`SM/PROGRESS.md:76-78`). One way to reshape in Rama is a new table filled
  by a topology that reads the depot from its start. That only gives the same record if the gate
  is the same on every run. It is not. The other way is to change a table's rows in place (the
  skill has `references/pstate-migration.md`; I read its name and not the file). That can turn a
  row into another row. It cannot bring back what no row holds: the act, the fact before.
- The thing he declined, a second log, is exactly what the build says would repair two of the six:
  "What would make it the same is a record of each batch's cut and clock, a second log beside the
  depot. Nothing is built for it; whether it is wanted is Sid's." (`MAP:459-461`). He declined a
  second log on the grounds that the first was enough.

**When it lands.** At the first kept record, and it cannot be repaired backwards. For every act
decided before a log of each batch's cut and clock exists, the cut and the clock are gone; no later
change can make those acts replay the same.

**What would settle it.** One question to Sid, put as what it is: "Must the store be able to build
its tables again from the depots and get the same answers?" With the two answers costed. If yes:
the gate's clock reading and each batch's cut have to be written somewhere that replays (the small
second log), rule 17 has to stop reading a clock or its outcome has to be logged, and there has to
be a stated home and backup for locks. If no: strike the reason from the decisions page, say in
one place that the tables are primary data, and say how the record's shape changes without a replay.
Either is a fine answer. What is not fine is the page saying one and the code doing the other.

## 2. The record's shape and where each row sits were fixed with reads fenced out. The write was costed to the seek; the read was never costed. This is Sid's third example, and I checked it in the code: it is true.

**What it is.** The contract says "Reads and `resolve` (group 6, unanswered)" are not in this
build (`B-store:252`). Inside that fence the plan designed three tables and counted the write path
exactly: "one accepted act with one sealed value costs 4 reads and 4 row writes" (`MAP:287-289`).
What it costs to read a value back was not designed for, because no read of a fact was in scope.
The one count of it that I found is a clause in the check of the second plan: "The read side
finds the bytes beside the value, two reads on one task"
(`SM/PLAN_VALIDATION-microbatch-gate.md`, the ruling "one copy of a lock's bytes"). I did not read
the two plans whole, so there may be another.

The Rama skill, which the contract makes the method of the build, says tables are designed from
the reads: "Design a Rama application by working top-down: tasks → PStates → queries → …"
(`.claude/skills/rama/references/app-design.md:3-5`), and the first line of its checklist for a
table is "What queries does this PState serve? (drives structure choice)"
(`references/pstate-schema.md:286`). CHECKED. The session that ran the build told Sid why the
staged method was worth its time: "in Rama the two things that are expensive to change afterwards
are the shape of the stored data and which task it sits on" («63a195fd»). CHECKED. Those two things
were then fixed without the reads.

What the record is, as built (CHECKED in `record/tables.clj:86-110` and `record/rows.clj:1-60`):

- A fact's row, under the key "<layer> <entity> <key>", holds `:value` as **sealed bytes**, the id
  of the lock that seals it, the id of the fact that wrote it, and a stamp.
- The lock is a row in **another table**, under the key "<layer> <request uuid> <index>": the key
  of the request that asked for the lock, not of the value it seals.
- A place holds one row, its current fact. The fact before it is not in the record.
- An answer's row holds a refusal word, a stamp and a digest. The act itself (who made it, in
  which session, under which permission, based on what, because of what) is in no table. It is
  in the depot only, and a depot is not read by act-id.
- The gate opens every sealed value when it accepts it, to test that the lock opens it, and
  drops the plain bytes (`gate/decision.clj:308`, `door/seal.clj:169-174`).

What follows for a read (DERIVED from the above; nothing ran):

- **Two seeks for each value.** One for the fact's row, one for its lock's row in the other
  table. The fact rows of one entity are neighbours and come back in one range; their locks are
  wherever their requests put them. By the skill's own numbers (a seek about 0.3 to 0.5 ms, a
  step to the next row a few microseconds) a screen of 1,000 values is about 1 seek plus 1,000
  steps for the facts and up to 1,000 seeks for the locks.
- **What Rama pushes is sealed.** A reactive read of the facts table delivers sealed bytes. The
  lock is a second read. This is exactly what Sid's third example says, and the reads sheet's own
  option B says "Rama pushes each change to the runtime with its value" (`decisions/groups/g-reads.txt:175`)
  without saying the value is sealed and its lock is elsewhere.
- **Nothing in the store can find a fact by its value.** Entity and key are plain; the value is
  not. "Every entity whose `:type` is `:claim`", "everything that links to this note", a search:
  none can be a table in the store as long as the store keeps nothing of a plain value.
- **The past is not in the record.** "As it was yesterday" and "how did this come to be" have to
  be built from the depot (sealed, and not the same on a replay: finding 1).
- **The first reader that is hurt is the store's own gate.** Sid decided that each layer holds a
  copy of each grammar it uses and that a gate reads it on its own task (F5 A), agreeing with a
  cost that was put as "a copy is bytes and a rare write; a fetch is a cross-task hop on every
  act" (`DEC:156-158`). As the record stands, a copy that a layer holds is facts in that layer
  (the contract: "the record must leave room for them: a layer holds facts under its own
  entity"), unless a grammar gets a fourth store key or a table of its own, which nobody has
  decided. A gate refuses a plain value under any key that is not one of the store's three
  (`B-store:135-136`), so a grammar's values are sealed in the table like any other. A gate that checks an act against a grammar must then read the
  grammar's rows, read the lock of each, and open each, for every act; or hold opened grammars in
  memory, which is a cache, and the project's rule sends a cache to its own examination. The cost
  he agreed to counted the hop and not the opening. Grammar checks are "not in this build"
  (`B-store:58-59`), so no paper met this. DERIVED.

**Where it came from. This matters, because the reason was lost on the way.** The contract's item
7 takes its working answer "from ruling 7 of the rulings file": "lock rows co-located with their
values in the record" (`B-store:128-131`). That line began as a session's sentence on 24 September,
«26bad2ac»: "If it lives on the same task as its value, **keyed so it comes back in the same read**,
it costs nothing extra. So a rig constraint: lock rows are co-located with their values." It is in
no line Sid typed and no paste of his (memory tool, `who`). CHECKED. The reason for co-location
was "the same read". What was built is "the same task": the lock is keyed by its request, so it
does not come back in the same read. The constraint was kept and its reason was dropped. The same
September text also says "Indexes built from values hold plaintext. Forget must reach them, so
every such index is rebuildable or purgeable by value", and the rulings file has "Who opens
values: the gate by default … at the cost of no matching, no shape check and no index on them" for
a value a tool marks opaque (`src/proposal/frame-2026-09-15/PROGRESS.md:88-90`), which only makes
sense if an ordinary value CAN be matched and indexed by the store. Those are sessions' words too
(Sid pasted them, did not type them). So the older thinking pointed at "the gate reads values and
may index them, and forget has to reach the index". The build's rule 16 went the other way, "keeps
nothing of the plain value" (`B-store:225-226`), with no line that says it is choosing between the
two.

**Whose it is.** "The seal is below the form; the door seals; a gate refuses an unsealed value":
Sid's (decision 3 of 6 October, `DEC:69-72`). "One lock seals one value": Sid's (H1 A, 9 October).
That a value stays sealed **in the table**, that its lock sits under its request's key, that the
gate keeps nothing of what it opens, that a place holds one row, that an answer holds no act: all
a session's, from the contract's items 5 and 7 and builder's rule 16. None was a question to Sid.

**What Sid loses.** If reads are designed on top of this record as it stands: every screen pays
two seeks a value and a decrypt in the runtime; "stream from there" delivers bytes nobody can
show; nothing can be found by what it says. If reads are designed first and the record follows
(as the skill says), the tables and their placement change, and after the first kept record that
is a migration of every row, on a store whose replay is not exact (finding 1).

It bites inside one more of his decisions. Sid decided that a layer's depot is set by its members
(F1 A) and that a layer starts streaming (F4 C). The two gates keep two separate sets of tables,
placed by different rules, and neither was designed for a layer to pass from one to the other
(finding 10).

**When it lands.** When reads are built. It gets much dearer if the first kept record comes first.

**What would settle it.** Decide the reads before the first kept record, and put three real
screens through the record as built on paper, with the skill's arithmetic: seeks, steps, round
trips. And ask Sid the one question nobody asked: "Must a value stay sealed in the table, or only
in the log?" The gate already holds the plain bytes at the moment it accepts. If the table may
hold plain values, forget has to delete a table row as well as destroy a lock (tables can be
written; the depot cannot), and reads, indexes and pushes become ordinary. If it may not, then
finding by value is not something the store does, and Sid should hear that as a property of
Softland before he hears it as a bug.

## 3. How a lock comes to exist was never a question. It is a session's design, and it is the root of the stock in the door, the dead lock rows, the base's ceiling, and "closing".

**What it is.** A lock is made like this: the door sends an act of its own, a "request for locks",
through the same depot as a maker's acts; the gate, inside its topology, makes random bytes for
each lock and writes them to its locks table; the door reads the bytes back by a query, keeps
them in the memory of its process (the "stock"), and seals values with them one by one
(`B-store:128-139`, item 7; `B-door:119-130`, part 3). CHECKED.

The contract gives the reason in one clause: "in Rama a query cannot write a PState, and both
depots are for acts" (`B-store:131-132`). So three things were held fixed and the design was
squeezed out between them: (a) a value is sealed before a depot keeps it (Sid's, decision 3 of
6 October); (b) a lock's bytes are never appended to a depot (builder's rule 5, from the rig's
finding that the log keeps every appended value); (c) only a topology writes a table. Nobody put
the three side by side for Sid and asked what should give.

**How it was put to Sid.** As the third of three facts that the store "has to read … itself" and
so keeps unencrypted: "3. the door's request for locks" («c738ecc3», 7 October). His answer, ‹751c2af7›: "8. idk what
else is missing or could be missing, so lets go ... btw how will encryption work in super nested
structure as i discribed above??? who has the key how and where is it stored??" CHECKED. He asked
the real question in that same line. The answer he got, «2d4d6464»: "**Who has the key:** the
store, and only the store. It makes a lock for each value and keeps it on its own disk, in the
record next to the value", and "Nobody in that structure ever holds a key". CHECKED.

Two days later the door's sheet asked him H1, "how much does one lock seal?", with three boxes.
Each of the three keeps the same mechanism: under A and B "the door keeps a stock of locks", under
C "the door asks for a lock once for a session and a layer" (`decisions/groups/g-door.txt:98-105`).
So the grain was his to choose and the mechanism was the frame of the question. The sheet also
says the door "asks for more before the stock runs out"; the door's build then ruled the opposite,
"the door asks for locks only at need, one request at a time" (`D/PLAN.md:1842`, ruling 1).
CHECKED.

Neither sentence of «2d4d6464» is true of what was built. The door, which runs in the runtime's
process, holds lock bytes in memory: up to 64 unused locks for each layer, depot and who in a streaming layer,
up to 256 in the base (`src-store/softland/store/door/README.md`, "The stock"). And "any client of
the cluster can read a lock's bytes" (`MAP:165-169`). And the lock is not "next to the value"
(finding 2). CHECKED in the maps; DERIVED that this contradicts what he was told.

**What the design costs, each from the build's own papers** (CHECKED that the papers say so; none
is my measurement):

- *A round trip before a first write.* A who's first value in a layer waits for a request to be
  appended, decided, answered and its locks read. In a streaming layer 1 send in 64 pays this
  (`D/PROGRESS.md:254-256`).
- *A ceiling in the base.* "One who gets at most 256 locks for each time that a request's batch
  takes to close", 0.75 to 1.03 seconds on the in-process cluster (`D/PROGRESS.md:230-233`). The
  system is one who. So everything the system writes into the base through one door is held to
  about 256 values a second, before anything is measured (the last sentence is my inference).
- *Rows that seal nothing, for good.* A lock taken for an act that is refused, or lost at a
  restart of the runtime, is a row that is never used and never removed. In the review's own run
  273 of 1,429 lock rows sealed nothing (`D/PROGRESS.md:277-278`). A lock's row is about 140 bytes
  in a streaming layer and about 245 in the base (`SM/PROGRESS.md:119-126`, `169-172`). Every
  restart of a runtime strands what is in every stock.
- *Requests in the log for ever.* Each request is an act in a depot that is never trimmed.
- *State in the runtime.* The door "owns no durable state", but it owns a memory that makers
  share, with a seat, a bound, requests "in doubt", and fourteen words for what can go wrong with
  it. By my count about twenty of the door's forty-six rulings are about this one memory (P5, P7,
  P8, P9, P16, P18, P22; rulings 1, 3, 4, 6, 8, 11, 12, 13, 16, 17 on the plan; 2, 3, 4 on the
  review). The review found four faults in it after the build session had read the code whole and
  found none (`D/FULL_SPEC_REVIEW.md:102-109`).
- *A gate that is not the same twice.* Because the bytes are random and made inside the topology,
  a second attempt of a batch "makes other lock bytes" (`MAP:385-388`). Rama's own doc names this
  exactly: "A microbatch topology is not necessarily deterministic … If you use … random numbers …
  the results after a retry could be different" (`reference/rama/docs/12-microbatch-topologies.md`,
  the paragraph after the commit phase). CHECKED. That is why "closing" exists (finding 4) and one
  of the reasons a rebuild is not the same (finding 1).

**Whose it is.** A session's: the cutter of the contract (item 7), then the plans. The word
`:lock-request` is a session's, written "because 'lease' is the rig's" (`DEC:226-227`). That looks
like the rig's thing under a new word, but I have not verified it: I did not open the rig's code
(it is not a source, and another part may look). The door's own papers say "the rig's code has a
function `stock!`" and "whether this one did [come from the rig] is not known" (`D/PROGRESS.md:246-248`).
Sid's own on this: only that one lock seals one value (H1 A), which is about what a forget takes,
not about how a lock is made or who holds its bytes.

**Was anything else looked at?** I searched the two plans and the implied spec of the store's
module for "derive", "kdf", "master" as words about locks and found nothing. CHECKED by word
search only; a design could be discussed in other words, and I did not read the plans whole. In
what I read, the papers weigh how many locks a request asks for, and never whether a lock should
be asked for.

**What Sid loses.** Not one thing; a tax on everything. Every sealed value drags a request, a
stock, a second table and a second read behind it, and the store's two gates stop being repeatable.
All of it follows from a design he was shown as the third item of a list.

**When it lands.** Now in the code. At the first kept record the id of a lock, `[the request's
act-id, index]`, is in every fact's row and every sealed fact in the depot, and the requests are
in the log for good. At scale: the dead rows and the base's ceiling.

**What would settle it.** Put the three fixed things, (a) (b) (c), to Sid as one question, with
"who holds a lock's bytes, when, and where" drawn for each way out. At least these ways were not
written down by any build paper (DERIVED, mine, and each changes what a forget means, which is
why it is his): the bytes worked out inside the store from a secret and the lock's id, so that
nothing is random and nothing is asked for ahead; the lock made by the door and handed over by a
road that is not the acts depot and can be erased; the gate sealing what it stores, so the door's
seal only has to protect the log. I am not saying one of these is right. I am saying the one that
was built was never compared with anything.

## 4. "Closing": the base's tables cannot be read straight, and cannot be pushed from, without a rule the build made up. The reads sheet does not carry it.

**What it is.** Because a second attempt of a batch reads the clock again and makes other lock
bytes (finding 3), a reader could see a stamp or a lock that later changes. So the build added a
mark on each task, `:closed-through`, and its three reads give a row of a microbatch layer only
when the row's stamp is not later than the mark (`MAP:385-400`, "Engineering choices: Closing").
An engineering choice, the builder's, and a sound one given what it was handed.

What it leaves behind is written in the build's own words:

- "A select that goes straight to a PState of the microbatch gate is not held to that: it sees a
  batch that is not closed, which Rama can still run again with another clock and other lock
  bytes. The PStates are not declared private." (`MAP:173-177`)
- "A place holds one row … after a batch replaces a fact, its place holds no row of a closed batch
  until that batch is closed, and a place that is replaced in every batch never holds one"
  (`MAP:395-398`). And from the session that judged the review: "So a read of facts cannot be held
  to closing as the three reads are, without more: the row before, kept beside the new one; or
  attempts of a batch made the same, by keeping each batch's clock" (`SM/PROGRESS.md:225-229`).
- An answer in the base is readable only after the next run of the gate: 873 to 893 ms in the
  proofs' runs, with nothing else going on (`SM/PROGRESS.md:175-180`).

CHECKED that the papers say this, and that Rama's doc supports the cause: changes of one task
become visible together, "though changes on different tasks may become visible at slightly
different times", and "When a new version of a PState is computed for the same microbatch ID, the
new version replaces the old version in the external view"
(`reference/rama/docs/12-microbatch-topologies.md`, commit phase and the paragraph after). NOT
CHECKED by a run: the build says no proof reaches a second attempt of a real batch.

**Why Sid has to live with it.** By his own account in the brief for this check, the drawing made
with him has "proxies push changes" (I did not open the drawing; another part has it). His typed
lines: "I want to write directly to rama and then stream from there" (‹a30a751b›, 11 July), and
today "the electric missionary client should read from the pstates directly correct??"
(‹e4a06ef7›). For the base, a push straight from the facts table is a push of rows that are not
closed. Either the
reads of the base go through the store's own code and its mark (which answers the reads sheet's
W3 for the base before Sid ticks it), or the push is taken as it comes and a screen can be shown a
stamp that is replaced a moment later after a failure, or the gate is made the same on every
attempt and closing is not needed at all. The third is the same repair as in finding 1.

I searched the reads sheet (`decisions/groups/g-reads.txt`) for "clos": the only hits are "closed
layer", a visibility setting. CHECKED by word search. So the sheet Sid is about to answer on reads
does not tell him that the base's tables have this rule, though the receipt of 8 October did, under
"For the reads, your next deciding" («380c05e8»).

**Whose it is.** The builder's choice, forced by finding 3's randomness and by reading the clock
inside the gate. Not a decision of Sid's, and not put as one.

**What Sid loses.** For the base: about a second before any answer; no straight read and no safe
push; and a read of a busy place that can stay empty. The base is the layer "everyone subscribes
to" (his F4 C heuristic, `DEC:150-155`), so this is the read path with the most readers.

**When it lands.** When reads are built.

**What would settle it.** Take it into the reads sheet as a fact of the base, beside W3 and W4.
And decide finding 1: if the gate must be the same on every attempt, closing goes away with it.

## 5. A holder has one permission in a layer, written once, never changed, never ended; and the gate does not read the chain above it.

**What it is.** Three rulings that lock together.

- "A permission fact sits on its holder. … It follows that a holder has one permission in a
  layer. Sid should know this one" (`MAP:291-294`).
- "In this build a store key takes one write for a place. A permission that already stands is not
  given again; a fact under `:permission` on a place that holds one is refused; each retract under
  a store key is refused" (rule 13, `B-store:215-218`).
- "The gate does not read the permissions above the act's own … nothing in this build ends a
  permission. Reading them would cost up to 3 reads on every act to find what cannot be false"
  (`MAP:296-299`). The contract said the opposite: "each permission above it in its chain stands"
  (`B-store:109-110`, item 3 (d)); the review lists it among the places where the contract is not
  met (`SM/FULL_SPEC_REVIEW.md:210`).

CHECKED in the maps and the contract.

**Whose it is.** A session's, all three. Sid's own words on permission: a permission "ties back to
a single individual"; walking back comes down to who created the group, "invited who, who gave the
permission to do xyz in their group" (he typed it: ‹d05a12e6›, 19:06 UTC on 7 October, which is
8 October by his clock and on the decisions page; CHECKED). "To do xyz" reads as a permission for something in particular. As built, a permission
says nothing but that it exists, and a person can hold exactly one in a layer for ever. Told to
him in the receipt as one bullet; no answer came.

**What Sid loses, and when.**
- *At the first kept record:* the place of a permission, "<layer> <holder> :softland.store/permission",
  is where every kept permission sits. If a holder may later hold two (one from each of two people
  who invited her; one to see and one to act, which the reads sheet already offers him as option
  C, "permissions that say what they allow: see, or see and act", `decisions/groups/g-reads.txt:96`),
  the place changes, and every gate read of a permission with it. As built a permission has no
  slot for what it allows, and a holder who is moved from "see" to "see and act" would need a
  second write on a place that takes one. So the reads sheet offers a box that the record as
  built cannot hold, and the sheet does not say so. DERIVED.
- *The day a permission can end*, which is the day someone is removed from a group: the gate must
  read the chain, "up to 3 reads more for each act" (`SM/PROGRESS.md:190-192`). The streaming
  gate's whole cost today is 4 reads, and the plan's own arithmetic at 4 reads is 480 to 770 acts a
  second against the target of 1,000 (`MAP:504-508`). So the build's speed numbers are for a store
  in which nobody can ever be removed. DERIVED from the build's own figures.
- H10 in the queue is "Forget after a revoke". It presupposes a revoke.

**What would settle it.** Before the first kept record, decide two things together: what a
permission says (only "may write", or what it lets its holder do), and how one ends. Then place
the permission fact once, with the chain's read counted into the target.

## 6. Everything in an act except `:v` goes into a depot that is never trimmed, plain, and a forget cannot reach it. A malformed or refused thing goes in whole.

**What it is.** The door's build found it and ruled it "literal" (P19, `D/IMPLICIT_SPEC.md:1299-1312`,
ruling at `:1540-1541`): "The door seals the `:v` of a fact under a plain key … All else that a
maker gives is appended as given, and the depot keeps it for good": each id, each key, `:who`,
`:session`, `:subjects`, a value under a store key, and anything in a thing whose form is wrong.
"The gate refuses the three rows; the log keeps them all the same." Rule D1, from the contract:
the door "checks nothing of the act's form; a malformed act goes in and comes back as the gate's
refusal" (`B-door:137-138`). And builder's rule 4: "The acts depots are never trimmed. That is
Rama's default; F2 A rests on it" (`B-store:184`). CHECKED.

**Whose it is.** D1 is the cutter's reading of Sid's decision 2 of 6 October (the gate checks what
arrives). It is stricter than his own F8 C, which says "The door may warn early; the gate is the
authority" (`DEC:167-170`): the contract's door checks nothing, early or late. Rule 4 is a build
session's, and its stated reason is F2 A, whose own reason does not
hold as built (finding 1). The door's build proposed a row for the queue, "whether the door runs
the maker's check before it seals and appends", gave its own view (yes), and left it "decided by
nobody" (`D/PROGRESS.md:216-221`). Sid has decided the part he was asked: a person's id is a UUID
that says nothing (?4 A), and K10 in the queue asks whether anything written plain may be made
from a person.

**What Sid loses.** "Forget" in Softland means: the `:v` of accepted facts. Not an entity's id (an
id may be a readable keyword, his decision 9), not a key, not who wrote, not a refused act, not a
value a broken or foreign sender left unsealed. Any of those that names or describes a person is
in the log for good. That may be what he wants. Nobody told him that this is what forget is.

**When it lands.** At the first kept record, and it does not come back out: the depot is never
trimmed by a builder's rule.

**What would settle it.** Say to Sid in one sentence what a forget reaches and what it does not,
and let him say whether that is the promise. If it is not: the door's check before the append
(the build's own proposed row) closes the malformed case; ids need K10; and "never trimmed" needs
its own decision, since Rama can remove records from a depot (the skill lists a depot migration
with tombstones, `.claude/skills/rama/references/depot-migration.md`; I read its name in the
skill's index and did not read the file).

## 7. "The runtime is the only program that connects" sits under five build rulings. It is a session's sentence, and the door's contract dates it as if it were settled.

**What it is.** Sid's first example, seen from the build's side. The door's contract says: "In
this build the door is the module's only client in the runtime (3 October: the runtime is the
only Rama client)" (`B-door:112-113`), and makes it rule D8. "The runtime is the only Rama client.
No other program connects to the module" is in no line Sid typed and no paste of his; it first
appears in a session's text of 2 October, «5f2bfa33», tagged there as a proposal and a derivation
(memory tool, `who`). CHECKED.

The rulings that stand on it, each a build session's:

1. *Rule 14* (`B-store:219-222`): the gate "cannot check `:who`. Until sign-in is built, any sender
   can make any layer that is not made, for any holder." And the door, part 6: "A sign-in built
   later stands in front of the door and refuses before this code runs" (`B-door:151-155`). So who
   someone is, is to be checked in the runtime and nowhere in the store, for good, not only
   "until sign-in" (my reading of the two together).
2. *Rule 16, second half* (`B-store:230-232`): "Any caller of the module can read a lock's bytes
   in this build; the door proper is the guard, later." The door's build then ruled that the door
   is no guard (P9, P12: "the door as built is no guard of a gate", `D/IMPLICIT_SPEC.md:1513-1514`,
   `1522-1524`). So the guard that rule 16 put off is now nowhere, and the map says "A guard of the
   record is not built" (`MAP:165-169`).
3. *Ruling A on the plan's check* (`SM/PLAN_VALIDATION-streaming-gate-and-record.md:228-235`): the
   streaming gate does not check that a thing arrived on its layer's task, for three reasons, the
   third being "this build has one module and one client". The check of the plan had put the test
   in, on the grounds of Sid's own F8 C, "anything can append to a depot without the door"
   (`:190-191`); the build session took it out. What follows is written in the map: a thing that
   another topology appends lands on the wrong task, and can "leave rows and an answer on that
   task that no read finds" (`MAP:143-154`).
4. *The tables are not private* (`MAP:173-177`), and part 1's build found that a private table can
   still be read by the module's own queries (`SM/PROGRESS.md:155-157`), so closing them is
   possible. They are "left readable in this build; the proof looks into them to show the facts".
5. *D8 itself*, with two exceptions already (both proofs read round the door).

**Whose it is.** The sentence: a session's. Sid's own, on the other side: "I want to write
directly to rama and then stream from there" (‹a30a751b›, 11 July), and today, ‹e4a06ef7›: "the
electric missionary client should read from the pstates directly correct?? … that is what we want
for reactive ones and if the user is trusted and verified for their layer they can stream".
CHECKED, both typed by him. And F8 C is his: the gate is the authority "because anything can
append to a depot without the door" (`DEC:167-170`). So his decision says the gate must not trust
the door, and five build rulings make the gate safe only if nothing but the door ever connects.

**What Sid loses.** The day a second program connects (an ingestor, a converter that runs as its
own process, a second module, a client that reads tables as he describes): it can name any who,
make any layer, read any lock, and read rows of the base that are not closed. And the gate's
authority, which he decided on purpose, is authority over the form of an act and not over who
sent it.

**When it lands.** When reads are built his way, or at the first maker that is not the runtime.

**What would settle it.** Another part has the architecture file's side of this. From the build's
side: list every program that will ever append to a depot or read a table, by name, and for each
ruling above say whether it still holds. Rulings 2 and 3 are small to reverse now.

## 8. The limits, and what a value is. One end of Sid's spectrum is cut off by numbers nobody decided.

**What it is.** Rule 20 (`B-store:240-243`): 256 facts in one act, 65,536 bytes in one sealed
value, 256 characters in an id or a key, 1,024 entries of `:based-on`, 256 locks in a request.
"Each number of the build in them is kept, not decided." And a value is what `act.clj`'s `value?`
takes (`src-store/softland/store/act.clj:245-267`): nil, a boolean, a string, a keyword, a UUID, a
number, and maps, vectors and sets of those. Not a run of bytes. The door seals "the UTF-8 of the
one text of the value" (`door/seal.clj`, "Not here"), that is, a value is stored as printed
Clojure text. CHECKED in the code.

**Whose it is.** The numbers: a build session's (ruled on open point 11, where the other choice
was "no limits; the gate gives the task back during a long act", `SM/IMPLICIT_SPEC.md:1570-1582`).
What a value can be is in no decision that I found on the decisions page; of a value, decision 12
names only its depth, 32 collections, as "kept from today's code, not decided" (`DEC:125-129`). That a value is stored as text: the door's
builder ("Which plain bytes stand for a value: the door's"). The door's build proposed "who holds
the form of a value once it is sealed" as a row for the queue, "decided by nobody"
(`D/IMPLICIT_SPEC.md:1563-1566`), because no gate looks at what a lock opens.

Sid's own, 9 October (`DEC:344-354`, his dictation): how often a tool writes is "a spectrum, and
that is a choice that layers and tools and groups can make"; "the tool is deciding how much of
their different actions are put together inside a single value or inside a single act". And his
heuristic for streaming, typed, ‹32f65085›: "editor, canvas, 3d roaming around, live
collaboration". The door's page reads his spectrum line and concludes "The door takes any grain"
(`D/PROGRESS.md:262-269`). That is true only inside the numbers. A tool at the coarse end (a page
as one value, a drawing, a model, an import of a graph as one act) meets 65,536 bytes and 256
facts, and a picture can be a value only as text, under the same 65,536 bytes. DERIVED.

**What Sid loses.** The coarse end of a spectrum he has said the store offers. And an import
larger than 256 facts is not one act: it is many, so it is not accepted or refused whole, and
its act-ids are a series. The fine end is hit from the other side by findings 3 and 12: a letter
for each value costs a lock's row of 140 to 245 bytes and a share of a request.

**When it lands.** At the first real maker: a converter or an importer.

**What would settle it.** The numbers can be loosened without refusing anything that was accepted
(the build says so of rule 12, and it holds for these). But they are also the only bound on how
long one act holds a task (finding 10), so they cannot simply be raised. Ask Sid what the biggest
thing is that one value and one act must carry, and whether a value can be bytes; then measure.
And name one owner for "what the plain bytes of a value are", before the first kept record: after
it, every reader for ever must read whatever the first door wrote.

## 9. The stamp: the gate reads its own clock, an act may be up to 10 seconds ahead, and nobody asked what a stamp has to put in order.

**What it is.** Sid decided that a stamp holds the clock time and a counter (H2 A,
"clock-time+counter"). The sheet put it to him with one consequence: "If A: a read 'as of 15:00
yesterday' is one number in every layer; a stamp stays close to the clock"
(`decisions/groups/g-record.txt:227-228`). CHECKED. It did not say that the clock is read inside
the gate, so a second attempt or a replay gives another stamp. The build found that (open point
10 (b): making stamps come out the same "changes where H2 A's 'clock time' is read from, which is
Sid's row", `SM/IMPLICIT_SPEC.md:1562-1564`) and did not take it to him.

On top of H2 A the build session ruled:
- Rule 17: an act that carries a stamp more than 10 seconds ahead of the gate's clock is refused
  (`B-store:233-234`). The open point's own text: "the clocks of two nodes differ, so the bound is
  wider than that difference" (`SM/IMPLICIT_SPEC.md:1528-1531`).
- Rule 18 and the packing: milliseconds times 65,536 plus a counter; the microbatch gate keeps one
  sequence for the whole gate on one task (`MAP:318-323`, `378-383`).
- What 10 seconds lets through: "any sender whose thing passes the declaration can hold a gate's
  stamps up to 10 seconds ahead of the clock" (`SM/PROGRESS.md:230-232`).

**Whose it is.** H2 A: Sid's. Where the clock is read, the 10 seconds, the packing, one sequence
for the microbatch gate: a build session's.

**What Sid loses.** (i) Stamps of two tasks, or of the two gates, are in order only as far as
their machines' clocks agree, and any sender can push one gate 10 seconds ahead. A read "as of
15:00 in every layer" is then only as good as that. (ii) On a cluster of more than one machine
an honest act that stands on a fact stamped by a machine whose clock is more than 10 seconds
ahead is refused (DERIVED from rule 17). (iii) The clock in the decision is one of the reasons the store cannot be
replayed (finding 1) and the base needs closing (finding 4).

**When it lands.** When reads are built ("as of"), and at the first cluster of two machines.

**What would settle it.** Ask what nobody asked: what must a stamp put in order? The writes of
one place; of one layer; of every layer a screen shows together; or only "roughly when"? The
answer says whether a clock in the gate is good enough, and where the clock should be read.

## 10. A streaming layer lives whole on one task, with other people's layers; one act can hold that task; the review asked for a remedy and the build session ruled "no change".

**What it is.** "Every row of a streaming layer is on that task" (`MAP:53-54`): its facts, every
answer it ever got, every lock it ever asked for. A task is one thread, and many layers hash to
it. The streaming gate's event "does not give the task back", so "one act holds its task for all
its reads, and for the opening of each of its sealed values": up to 256 places, 256 sealed values
of 65,536 bytes each, and 32,768 lock rows for an act built to do it (`MAP:204-218`). The review
of the whole raised it as R5, against the skill's rule on long work on a task, and proposed a
change; the build session ruled "no change of the code … It is a choice of reads without a
measure, and F9 puts the measure first" (`SM/FULL_SPEC_REVIEW.md:124-132`, `168-171`). CHECKED.

The map says "such an act needs standing in the layer". Every signed-in person has standing in
her own layer, by Sid's own decision (K2, `DEC:197-200`). So the one who can hold a task is any
user, in her own layer, and what waits behind her is every other layer on that task. DERIVED.

**Whose it is.** "The streaming depot partitions by the act's `:layer`" is the cutter's line in
the contract (`B-store:92`), and comes from the drawing of 2 October (another part has that). That
the event does not yield, that answers and locks are never removed, and the ruling on R5: a build
session's.

**What Sid loses.** His second target, 20 ms for a person, is a promise about one person's
layer; as built it depends on what every neighbour on the task is doing. And a layer can never
be larger or busier than one thread and one disk partition allow, with rows that only grow. The
way out for a layer that outgrows a task is to change its depot, which is refused as "not decided"
(`:changing-a-depot-not-decided`), and is a move of every row between two differently placed sets
of tables with the depot written into every act-id (the queue's ?9: "listed, not asked: waits for
a test").

**When it lands.** At scale; and at the first layer shared by many people that is left streaming.

**What would settle it.** Measure (the build says so itself). But also put ?9 to Sid now and not
after a test: he decided that "the members of a layer set its depot, as a fact on the layer" (F1 A,
in the words of `B-door:72`), so changing it is a normal thing a group does, and the record as
built makes it a migration.

## 11. The order of a layer's acts after a failure was handed to Sid as a setting to choose. The fact under it was not checked, and the question above it was not asked.

**What it is.** "After a failure Rama can decide an act of a layer after a later act of that
layer; the setting `topology.stream.max.executing.per.task` at 1 closes that window at a cost in
throughput and stays at its default: his to choose. The review could not check that setting"
(`SM/PROGRESS.md:137-140`; `MAP:155-164`). In the receipt: "The order after a failure. One Rama
setting closes the window at a cost in throughput; it is at its default", under "Open from
before, none blocking" («380c05e8»). CHECKED.

What I could check in Rama's local docs: the setting exists and is "the max number of depot
records that can be pending for a stream topology for each task"
(`reference/rama/docs/11-stream-topologies.md:317`). And the retry mode the gate uses, "all after",
retries the failed record and every record after it on the partition (`:162`). The gate's own rule
(F7 A: a repeat is answered, not decided twice) then keeps the later act's first decision, which
is how the order turns. That part holds by reading. Whether the setting at 1 truly closes the
window: NOT CHECKED by anyone; it is the plan's inference.

**Whose it is.** A build session's: it chose the default, said so, and passed the choice up. This
is Sid's sixth shape. A choice was passed to him that rests on a fact nobody verified, in place
of the question he could answer: "must the acts of one layer be decided in the order they were
sent, even after a crash?"

**What Sid loses.** After a crash, in a streaming layer, a later act of a series can be decided
before an earlier one. If the later one replaces what the earlier one wrote, the later is refused
for no fault of its maker. No proof reaches this; "the in-process cluster gives no way to make
them happen" (receipt).

**When it lands.** In production, at the first failure under load.

**What would settle it.** Sid answers the question about order. Then one run on a real cluster
with a forced failure, with the setting at its default and at 1.

## 12. Sid's three targets were replaced by the build's own aims, and the build's own arithmetic misses all three.

**What it is.** Sid, ‹27882354›: "The limits are what we should be optimising for what we measured
is starting point maybe a better optimised sytem will perform better". The three, as the sheet
put them: 1,000 acts a second on one task; 100 acts a second for one person with the slowest 1 in
100 at 20 ms; locks no more than 2 times the bytes of the values. CHECKED.

What the build did with each (`B-store:244-248`, rule 21; `MAP:504-515`):

- *1,000 a second:* "By the cost of a cold read, 4 reads give 480 to 770 acts a second on one
  task; the first aim is met only when the rows are read from memory." With the chain of
  permissions read (finding 5), fewer.
- *20 ms for a person:* restated as "20 ms from the door's append to the answer in the door's
  hand". The door's build then found that counted from the maker's call, the slowest 1 in 100 "is
  then a send that asks" for locks, and proposed "from where the aim for one person is counted"
  as a row for the queue (`D/PROGRESS.md:254-256`, `282-284`). For the base the aim was set aside
  in a sentence: "not 20 ms, because a microbatch layer is not for what a person does directly".
- *Locks at most 2 times the values:* replaced by "110 bytes of lock or fewer for each value", and
  "the third does not hold for values that average under 55 bytes" in a streaming layer, "under
  about 90 to 123 bytes" in the base.

"None is measured" is said three times.

**Whose it is.** The targets: Sid's. The restated aims: a build session's.

**What Sid loses.** A later session can report "the aims are met" and be right about the aims
and wrong about his targets. And the third miss is driven by his own H1 A (one lock for each
value), which he decided on 9 October for what a forget takes. The door's sheet did tell him the
size: "a lock's rows are about 140 bytes for each value in a streaming layer and about 245 in the
base, as written: more than a short note itself" (`decisions/groups/g-door.txt:93-95`). It did not
set that beside his own third target, which it misses. CHECKED by reading that passage of the
sheet; I did not read the whole sheet.

**When it lands.** Now, as a matter of what "met" means; at scale as cost.

**What would settle it.** Measure against the three as he wrote them, from the maker's call. Put
the third beside H1 A for him once, with the size of a usual value, which he was asked on 7
October and left.

## 13. What authorised the rulings: "you decide" answered a question about how many stages to run.

**What it is.** The store's build page opens: "Sid, 8 October 2026: 'you decide ... pay more now
is better than future'; no test loop after every step; carry on without check-ins"
(`SM/PROGRESS.md:4-5`). Under that line the build session ruled on the twelve open points
(rules 10 to 21), which include what the seal is for, whether a rebuild is required, how many
permissions a holder has, and the limits.

His line, ‹ac795a4c›: "ok you decide yeah pay more now is better than future and i am going to
sleep so the time cost is basically zero for me". It answered «63a195fd», whose last heading is
"**Do we need it now?**", about running the skill's stages; his line before it, ‹51f028d2›: "ok
and we are goign to do 7 rama phases?? why?? do we need that now? its fine i am asking the
process". CHECKED. So "you decide" was about the process. It was not a hand-over of the store's
open points.

In fairness: each build paper says its rulings are "the build session's … never a decision of
Sid's, and each is his to strike" (`D/IMPLICIT_SPEC.md:1450-1452`), and the contracts say the
same. The papers do not pretend the rulings are his. The gap is that "his to strike" needs him to
read sixty-six rulings across 1.2 MB of papers, and the build went on to the next stage on each
ruling the same day.

**Whose it is.** A session's reading of his line.

**What would settle it.** Nothing to decide; a correction of the record. For a next build: an
open point whose ways differ in what the store is (the implied specs mark these themselves:
"Where a way would decide something that is Sid's, the text says so") stops the build and goes to
him, and only the rest is ruled.

## 14. The words. Well over a hundred were made by sessions; the reason words are stored in every answer.

**What it is.** The store's map lists them (`MAP:517-702`): the names in Rama, the fields of each
row, 5 words of a stray, 20 reason words of the declaration, 18 reason words of the gate, the
words of a batch; the door's map lists its own fourteen and its numbers. A refusal's word is a
field of the answer's row (`record/tables.clj:86-91`), so it is kept for every refused act, and it
is what a maker reads to learn why. The parts `:sealed` and `:lock-id` are in every sealed fact in
the depot. CHECKED.

The build did what its rule 8 asked: every word is plain and listed "for him to strike"
(`SM/PROGRESS.md:151`). One
ruling shows the cost of waiting: `:store-key-takes-one-write` "reads as a law of the store" (the
review's words) and was kept, "the word is his to change, with the others"
(`SM/FULL_SPEC_REVIEW.md:92-100`, `151-155`).

**Whose it is.** Sessions'. By the contracts' and the decisions page's own account, Sid's words in
this tree are few: act, depot, maker, `softland.store`, streaming, microbatch, system, the base.

**What Sid loses.** After the first kept record, changing a reason word means rewriting rows, and
after the first maker it means changing every maker that reads one. He has said what he thinks of
sessions' naming (‹32f65085›: "Naming::: bro you total shit at it").

**When it lands.** At the first kept record and the first maker.

**What would settle it.** One sitting with the list before the first kept record. It is long, but
it is one page of tables.

## 15. Smaller rulings that shape the store and are cheap to change today

Each is a build session's or the cutter's. Each is one line in a map. I list them so that none is
met later as a surprise.

- **A fact that names nothing to replace always wins its place** (builder's rule 2, `B-store:179-181`),
  and **a place holds one row**. Two people who write one place without `:replaces`: the later one
  stands, the earlier is in the log only. Sid asked on 2 October whether what happens when "2 users
  [are] on same unit" can be the group's policy, in data (‹48bd1d46›); the answer then was that a
  tool's rules could say so; those rules (grammar checks) are not built. So today the policy is
  whatever each maker does, fact by fact. Lands at the first tool with two writers.
- **An act names a place once** (rule 12). "Set, then change" is two acts. The build says that to
  loosen it refuses nothing that was accepted. I agree.
- **A layer is made by its first act, of exactly two facts** (`MAP:329-330`). The reason given to
  Sid: "so the record can still be rebuilt from the log" («c738ecc3»), which leans on finding 1.
  A making act can carry nothing else: no name, no members.
- **The system makes a person's layer and names her permission, not its own** (`B-store:145-151`):
  "the one place where the holder of the permission that an act names is not the act's `:who`".
  The builder's reading, told in the receipt. It is an exception to E4 A ("each actor acts under
  its own permission"), itself a session's reading that he let stand.
- **A lock is used only for values of the same who and layer as its request** (the cutter's rule,
  `B-door:123-125`): "keeps room for a forget of a person that finds her locks by her requests".
  The gate holds a lock to its layer only, so the rule is the door's alone. It leans toward
  "a person's values are those she wrote". The September rulings file has a value's lock wrapped
  "under the locks of the people it is about" (`src/proposal/frame-2026-09-15/PROGRESS.md:91-92`),
  a different idea of whose a value is. Forget is not designed; this rule is already a stock for
  each who.
- **A thing with a wrong act-id is a "stray" and never gets an answer** (rule 10). Its sender reads
  "nothing yet" for ever. The door does not send one; anything else can.
- **A permission and the depot fact of the base are copied onto every task** (`MAP:355-358`): "That
  is Sid's reason for F5 A, read onto the permissions." His F5 A was about a copy of a grammar.
  Each give in the base writes one row for each task.
- **An act-id's uuid is random** (Sid's, decision 5). The plan's check noted "the Rama skill asks
  for a version 7 UUID" as one of four things "for Sid" (`SM/PLAN_VALIDATION-streaming-gate-and-record.md:219`).
  The skill: "Always use UUID7 over UUID4 … UUID7 is time-ordered — IDs sort chronologically, which
  is useful for range queries" (`.claude/skills/rama/references/unique-ids.md:15`). It bears on any
  read that walks acts in the order of time. The words "uuid7", "uuidv7" and "v7" are in no line
  he typed (memory tool, `search`); I did not read the session texts that hold them.
- **What arrives on a depot has no edition mark of its own.** The act has `:version`; the door's
  additions and the request for locks have none (I searched `door/declaration.clj` for "version":
  nothing). The way of sealing has a number, on the lock's row, which is the right idea.
- **The store's namespace is taken exactly** (`softland.store.x` is not the store's): queued as K15.
- **A lock of a refused act never goes back to the stock** (P7); **the door asks the layer's depot
  for every act-id** and keeps no copy, by the project's rule on caching (ruling 2 on the plan).
  The second is the caching rule kept as written. The cost is one round trip for each act-id, and
  its cause is that an act-id holds the depot, so nothing can be sent before the depot is known.

## The builder's own: what Sid does not need to read

These are rulings and choices that stay inside the code that made them. Changing one changes no
kept record and nothing a later layer sees. I read each ruling's short form; I did not trace each
in the code.

- In the door: the seat and the count of calls; the bound of 10 seconds and the wait's 200 ms; a
  request "in doubt" and how long it is kept; that a read that came back ends a request; the walk
  before the seat; the oldest run first; `sealed-length`; the private test of a plain map; the
  interrupt mark; `act-ids-for` and `stock-count`; the two ack levels; which files sit in which
  folder; how the commits were grouped (rulings 3 to 8, 10 to 14, 17, 18 on the plan; P1, P4, P5,
  P6, P10, P11, P13 to P18, P20 to P22; the six on the review but for what finding 3 names).
- In the module: that a query takes the routing id first; the four steps of a batch, the turn, the
  meeting task (they decide the order inside one batch, which is nobody's business outside the
  gate, apart from finding 1); the set of strays; SHA-256 over the one text; AES-256-GCM, with the
  number of the way on the lock's row so that it can change; the retry mode and the start of each
  source; the function names taken out of the map (R1); the folders.
- Rule 11 (the digest comes back with the answer, and the sender compares) and the door's fourth
  answer "not mine" are a clean way to keep one act-id from being read as two acts' answer.

## What nobody asked at all

In the order of the findings. Each is a question that a build ruling answered, or stepped round,
without it ever being a question to Sid.

1. **Must the store be able to build its tables again from the log and get the same answers?**
   And what, then, is backed up? (finding 1)
2. **Which reads does the record serve?** The skill's first step for a table. The record was
   shaped first. (finding 2)
3. **Must a value stay sealed in the table, or only in the log?** Sid's own third example. The
   build ruled "keeps nothing of the plain value". (finding 2)
4. **Who holds a lock's bytes, when and where; may the runtime's process hold them?** Sid asked it
   himself on 7 October, in the line in which he said "lets go". The answer he got does not
   describe what was built. (finding 3)
5. **Is a lock asked for, or is there another way for one to exist?** Every option on H1's sheet
   kept the asking. (finding 3)
6. **May a table of the base be read straight, or pushed from?** His line of today says the client
   should read the tables directly. The base's tables have a rule that a straight read skips.
   (finding 4)
7. **How does a permission end, and what does a permission say?** The gate's cost and the place of
   a permission were fixed as if neither will ever be needed. (finding 5)
8. **Which programs connect to the cluster?** (finding 7; Sid's first example)
9. **What does a forget reach, said as a promise?** `:v` of accepted facts, and nothing else in an
   act; not a refused thing; not an id. (finding 6)
10. **What is the biggest thing one value and one act must carry; can a value be bytes; who owns
    what the plain bytes of a value are?** (finding 8)
11. **What must a stamp put in order?** (finding 9)
12. **Must the acts of one layer be decided in the order they were sent, after a crash too?**
    (finding 11)
13. **How does a value get from one layer into another?** Promotion, a group that publishes to the
    world, a layer's copy of a grammar (Sid's F5 A). One lock seals one value, and the seal is
    bound to its place ("sealed bytes copied to another entity, key or layer do not open", rule 16).
    So every value that moves is opened and sealed again under a new lock, by something that holds
    plain values, at the rate at which the target layer gives locks; for the base that is finding
    3's ceiling. DERIVED; no paper of the build works it through. The queue has promotion as ?10,
    "listed, not asked: waits for a build".
14. **How does a layer change its depot?** He decided the members set it (F1 A). (finding 10)
15. **What happens by default when two write one place, and can one place hold many values?**
    (finding 15, first item)
16. **Can the proofs be made to reach a retry?** Every claim about what the store does when Rama
    runs something a second time is argued from the docs; "the in-process cluster gives no way to
    make them happen" (the receipt). Rama's doc says "Any exception thrown anywhere in the
    processing of a depot record will cause that topology event to retry"
    (`reference/rama/docs/11-stream-topologies.md:146`). A test-only switch that makes a gate throw
    once would seem to give a retry on the in-process cluster. I did not try it, and the build
    papers do not say they considered it. Sid was told that the test of a failed machine waits for
    production (F9 a); he was not told that every retry waits with it.

One more, which is not a question about the store. Each build was held to "nothing is left to ask
before the build" (`B-store:9-10`, `B-door:10`). Then the first stage of each found open points:
12 for the module, 22 for the door. The method had no step at which an open point that decides
what the store is goes back to Sid before the next stage runs. The papers mark such points
themselves ("Where a way would decide something that is Sid's, the text says so",
`SM/IMPLICIT_SPEC.md:1372-1373`), and then the build session rules on them the same day, including
open point 10, whose way (b) the paper marks as "Sid's row".

## For another part

- *The decisions page.* Entry 2 of 7 October gives "His reason" in words a session wrote («6631e4be»);
  his typed note is ‹27882354›. And the reason rests on finding 1's fact.
- *The decisions page.* The depot is written into every act-id (decision 5, "freeze into every
  act-id at the first kept record") and the members may change a layer's depot (F1 A). Each is
  fine alone.
- *The decisions page, H2 A.* The sheet gave one consequence of a clock in the stamp
  (`g-record.txt:227-228`) and not that the gate then is not the same twice.
- *The architecture files.* "The runtime is the only Rama client" was tagged a proposal and a
  derivation where it was first written («5f2bfa33»); the door's contract cites it by date.
- *The flows.* One value read end to end through this record: a fact's row, then its lock's row
  in another table under another key, then a decrypt outside the store. One value moved to
  another layer: opened and sealed again.
- *What Rama is assumed to do, and the numbers.* Not checked by anyone: that
  `topology.stream.max.executing.per.task` at 1 closes the order window; that one client handle
  may be used by many threads (one probe run of 16 threads; "the local documentation is silent");
  that the run after a batch with things starts at once ("one sentence of the skill can be read
  the other way"); what a worker does with bytes of a Java type it does not have. Every number is
  arithmetic or one machine's in-process run: 140 and 245 bytes for a lock, 480 to 770 acts a
  second, 873 to 893 ms and 0.75 to 1.03 s for the base, 14.9 ms for a request of 64, a query's
  70 seconds.
- *The reads sheet.* Closing is not on it. Its option C for who may see needs a permission that
  says what it allows. Its option B for W3 says Rama pushes each change "with its value"; the value
  is sealed.
- *The widest view.* "You decide" (finding 13).

## What I read, and what I did not

**Read whole.** `BUILD-store-module.md`; `BUILD-door.md`; `store-module/PROGRESS.md`;
`door/PROGRESS.md`; `src-store/softland/store/README.md`; `src-store/README.md`;
`src-store/CLAUDE.md`; the Rama skill's `SKILL.md`; `src-store/softland/store/record/tables.clj`.

**Read in part, the rulings and what they answer.**
- `store-module/IMPLICIT_SPEC.md`: lines 118-198 (the rules for every operation) and 1364-1638 (the
  twelve open points, and where the contract and the decisions page differ), of 1,638.
- `store-module/FULL_SPEC_REVIEW.md`: lines 69-260 (what waited for a ruling, the five rulings,
  where the contract is not met, what no proof reaches), of 414.
- `store-module/PLAN_VALIDATION-streaming-gate-and-record.md`: lines 181-345 (what was left for
  the build session and for Sid; rulings A and B), of 1,685.
- `store-module/PLAN_VALIDATION-microbatch-gate.md`: the ruling "one copy of a lock's bytes",
  "Closing", rules 9 and 19, the table of claims about Rama, and its last section, of 769.
- `door/IMPLICIT_SPEC.md`: P7 to P9, P19 to P22, and the rulings on P1 to P22 with the four rows
  proposed for the queue (lines 1130-1186, 1299-1346, 1439-1577), of 1,577.
- `door/PLAN.md`: the eighteen rulings (lines 1820-1981), of 2,107.
- `door/FULL_SPEC_REVIEW.md`: the points and the six rulings (lines 52-137), of 540.
- `src-store/softland/store/door/README.md`: "Engineering choices".
- Code: the namespace docstrings of `record/rows.clj`, `door/seal.clj`, `door/text.clj`; `value?`
  in `act.clj`; single lines of `gate/decision.clj`; a search of the tree for every append to a
  depot (two, both in `door.clj`).
- Rama's local docs: the commit phase and what follows in `12-microbatch-topologies.md`; the retry
  modes and the configs in `11-stream-topologies.md`; a word search of the two testing pages.
- The skill's references: the first lines of `app-design.md`, the checklist of `pstate-schema.md`,
  `unique-ids.md` by word search.
- Sheets: the passages of `g-reads.txt` on W3 and on who may see, `g-record.txt` on H2,
  `g-store.txt` on F9, `g-door.txt` on H1. `OPEN-QUESTIONS.md`: every row's title and state.
- `src/proposal/frame-2026-09-15/PROGRESS.md`: lines 40-62, 84-120, 240-252.
- Sid's lines, by the memory tool: ‹ac795a4c›, ‹51f028d2›, ‹27882354›, ‹751c2af7›, ‹8573450f›,
  ‹32f65085›, ‹d4fd70ba›, ‹48bd1d46›, ‹e4a06ef7›, ‹a30a751b›, ‹d05a12e6›, ‹0cc0e69c›; and session
  texts «63a195fd», «802316c6» (the F2 passage), «c738ecc3», «2d4d6464», «380c05e8» (part 2 of 3),
  «26bad2ac» (the passage on co-location), «a174f29f» (one passage).

**Headings only.** The rest of each paper above.

**Not read.** Both plans of the store's module (160,088 and 122,398 bytes) and the body of the
door's plan; `door/PLAN_VALIDATION.md` beyond its headings; all four `IMPLEMENTATION_VALIDATION`
and `TEST_VALIDATION` papers; both `DECOMPOSITION.json`. The two gates' code
(`streaming_gate.clj`, `microbatch_gate.clj`, `batch.clj`, `decision.clj` but for one line),
`door.clj`, `stock.clj`, `additions.clj`, `declaration.clj` but for word searches, and every proof
under `test-store/`. Parts 1 and 3 of the receipt «380c05e8», and the door's receipt of
10 October. The two architecture files, the folder's own `PROGRESS.md`, the rig, `vision/LOG.md`
and `docs/carry-on.md`: for what Sid wants I leaned on his own lines through the memory tool, and
on the decisions page's quotations where I say so. No git command was run, so I did not check the
counts of rulings in the commit messages against the papers; my count from the papers is
12 + 2 + 1 + 5 for the module and 22 + 8 + 10 + 6 for the door.

**Not run.** Anything. No proof, no cluster, no probe. Where a finding says DERIVED, it is
arithmetic or reasoning from what I read.

**Where I am least sure.**
- Finding 2's cost of a read is my arithmetic from the skill's figures and the keys of the two
  tables. A read through a query topology on one task could fetch a fact and its lock in one
  event with no network hop; it is still two seeks. I did not design or cost a real screen.
- Finding 3's "ways not written down" are mine and unexamined. They are there to show that a
  comparison was missing, not to propose one.
- Finding 4: how often Rama runs a batch a second time is unknown to me. If it is only after a
  worker fails in the commit phase, the window is rare. Rare is still not never, and for a lock's
  bytes one time is a value that can never be opened; that is the build's own reasoning, and I
  think it is right.
- I did not verify, line by line, the six ways of finding 1 in the gates' code. I verified that
  the build's papers assert each, that a lock's bytes are appended by no code, and Rama's own
  sentence on a topology that uses random numbers.

