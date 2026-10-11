# The reads: where the build stands

For a session that takes this build over, and for the receipt to Sid. Kept by the build session
"read implementation" (Opus 5.5), started on 12 October 2026 on Sid's line "Build the reads. Start
from src/proposal/backend-on-rama-2026-10-02/BUILD-reads.md and read what it names, in its order."
"The build session" below is the one that holds the build at the time.

## Sid's words for this run

All of 12 October 2026, in the build session's chat, as he typed them.

- "Run it by the Rama skill's stages, as the store's module and the door ran, and keep the
  explanation rules in src-store/CLAUDE.md."
- "Decide none of my open questions: build what the page marks as kept for now, list what you choose
  as provisional, and give me one receipt at the end."
- "i will be goint to sleep so keep going and if there is somethign that needs my decision you make
  it today and store it to present to me tommorow .. do the commits don't wait for my permissions."
- "use opus-5.5-xhigh for code implementation if you use subagents for it ... remember
  opus-5.5-xhigh"
- "make sensible decisions dont get blocked on me i need this finished by tomorrow morning"
- "you have access to 2 other sessions namely session 1 and session 2 when you start to degrade your
  context window the best way is to switch over to the parallel sesions htey can continue your work"

How the build session reads the second and the third line together. His open questions stay open:
the queue, and the contract's list "Not decided — kept only so that the build can run". Each is built
to the kept answer, so that the other answer is a small change, and is decided by nobody. Where the
build meets a new thing that only he can decide, the build session picks a working answer so that the
build does not stop, marks it **provisional, for Sid**, and lists it under "For the receipt". A
working answer is never written as a decision of his, in this page or in the tree.

## What to read, in this order

1. `../BUILD-reads.md` — the contract. It is the full spec, and it names what is not decided.
2. `src-store/README.md` — Sid's decisions, the section of 11 and 12 October above all. It wins over
   the contract.
3. This folder's papers, by stage: `IMPLICIT_SPEC.md`, `DECOMPOSITION.json`, then for each part
   `PLAN*.md` and `PLAN_VALIDATION*.md`, and after the build `IMPLEMENTATION_VALIDATION*.md`,
   `TEST_VALIDATION*.md` and `FULL_SPEC_REVIEW.md`.
4. `push-probe/RESULTS.md` (11 October, the cutter's) and `batch-probe/RESULTS.md` (12 October, this
   build's): what was run on Rama. Each is a probe: no form, name or table shape comes from one.
5. "Rulings", below: what the build session ruled on each paper, with the reason.

## The starting point, confirmed

The contract asks for three things before the first stage.

- `git log --oneline -5` at the start: `c3e65c06` (Sid's answers on the read sheet, the starter for
  the read build, and the run of what Rama's push can watch), `fcdbfced`, `81e7d946`, `30f9859f`,
  `bff3eac1`.
- `git status --short` at the start: ` M src/app/server/README.md`; the staged rename
  `decisions/groups/g-reads.txt -> g-reads-first.txt`; untracked `docs/.obsidian/`,
  `docs/Untitled.md`, `decisions/groups/g-check.txt`, `decisions/groups/g-reads.txt`. None is this
  build's. They are left as they are, and every commit of this build stages its own paths by name.
- The four proofs, run by the build session on that tree on 12 October, each with exit 0. This is
  the "before":
  - `clojure -M:store -m softland.store.act-proof` — `{:test 8, :pass 150, :fail 0, :error 0}`
  - `clojure -M:store -m softland.store.pure-proof` — `{:test 16, :pass 540, :fail 0, :error 0}`
  - `clojure -M:store-module -m softland.store.module-proof` — `{:test 22, :pass 227, :fail 0, :error 0}`
  - `clojure -M:store-module -m softland.store.door-proof` — `{:test 26, :pass 193, :fail 0, :error 0}`

  The full outputs are outside the repository, in
  `/mnt/data/projects/research/softland/reads-build-2026-10-12/before/`. They were moved there
  before Sid said that nothing may wait on a prompt; from then on a run's output stays inside the
  repository, in this folder's `runs/`, cut to the lines that a proof prints for a reader.

## The stages

The Rama skill's stages, as the store's module and the door ran: each design stage in a fresh
session (the Agent tool, model Opus 5.5, effort xhigh), judged here. The read path is a client of the
module and not a module; what the gates keep is a change of the module. Each stage's paper says how
it adapts the skill's template.

| stage | state | commit |
|---|---|---|
| the starting point confirmed | done | — |
| a run on Rama beside the stages: what a second attempt of a batch does, and what state survives a failed attempt (`batch-probe/`) | running | — |
| the implied spec, with the check of each "as built" line of the contract against the code | running | — |
| the split into parts | — | — |
| the plan of each part | — | — |
| the check of each plan | — | — |
| the build of each part | — | — |
| one fresh review of the whole against the contract | — | — |
| the measures of the contract's section C | — | — |
| the receipt to Sid | — | — |

How a stage is run: a fresh agent reads the skill's doc for its stage, the contract and its read
list, and the papers before it; it writes one paper (or the code) and does not commit. This session
reads its report and the parts of its paper that need a ruling, checks the facts that a ruling rests
on in the source, rules, has the rulings applied, and commits with a message that says what and why.
No `Co-Authored-By`, no session link. Never push. An agent that writes code runs on Opus 5.5 at
effort xhigh, at Sid's word.

The batch probe runs beside the first stage and is not a stage of the skill. The contract asks for
it: A7's repair "is tried first, with a run". It starts early so that the plan does not wait for it.

## Rulings

Each ruling is the build session's, with its reason. None is a decision of Sid's. Three marks:
**kept** is the contract's own kept answer, built so that the other answer is a small change;
**provisional, for Sid** is a working answer to a thing that only he can decide and that the contract
does not name, picked so that the build does not stop; **builder's** is engineering, and is listed in
the receipt as a provisional choice.

### On the implied spec (`IMPLICIT_SPEC.md`), its 31 points

The paper is judged sound. It found none of the contract's 34 "as built" lines false; eight hold in
part, and the plan takes each as the paper states it (its table, lines 97 to 150, and F1 to F11). The
build session read the open points, F1 to F11 and the A7 section whole, and checked in the source the
facts that a ruling rests on: the store keys' table and `setting-keys` (`door/declaration.clj`),
check (d) and `store-key-fault` (`gate/decision.clj`), the three reads' hold to closing
(`module/door_reads.clj`), and the two gates' walks.

1. **P1, the four proofs — builder's.** The four files stay. Each check that a decision of this build
   makes untrue is written again for the new truth, never only removed, and the counts change. The
   receipt gives the counts before and after and names each check that was written again, with the
   decision that made it untrue. No new store key takes the word `owner` or `class`: the proofs use
   both as keys that the store does not know, "owner" is not decided, and "class" is retired.
2. **P2, where a permission's part lives — builder's, flagged for Sid.** Option 1: a slot of the
   permission itself. A given permission is `[who layer granted-in parent]` or
   `[who layer granted-in parent allows]`; a first permission stays exactly `[who layer granted-in]`.
   Without the slot it reads as "see and act". Reasons: it is the reading that was before Sid ("A
   permission gets one slot more: what it allows", the sheet's W1, which he did not strike); check
   (d) stays one read and a comparison of bytes, so nobody raises her right by naming it; a making
   act cannot say "see", as it falls out of its exact form; and each act in the log then says under
   which right it was made, which is what entry 7 keeps for a read. The cost: `act.clj` changes a
   second time, and decision 8 of 6 October names two forms where the code will read three. The
   receipt says so and names option 2 as the other answer. The part is read by one function, so
   option 2 changes that function, the shape, and check (d).
3. **P3, "an act needs act" — builder's.** The check stands with the standing, straight after check
   (d): an act whose permission stands and says "see" gets one new reason word, before any place is
   read. The word is plain and listed for Sid. A making act keeps its exact form, so one whose
   permission carries the slot is no right making act and is refused `:layer-not-made`; the docstring
   says so. The door does not change: a holder of "see" who sends a value meets the refusal at the
   door's own request for locks, and reads the gate's word in that request's answer. The receipt says
   so.
4. **P4, one permission for each holder — kept.** As built. Row for the queue.
5. **P5, who may write the setting, the list and the mark — kept.** Whoever may act in the layer. The
   three are held by one function, so the other answer is one check. The receipt says plainly what
   the kept answer lets happen: any holder of "act" can set her layer to "everyone", and any holder
   of "act" in the base can set the base to "members". Row for the queue.
6. **P6, what a layer starts with — kept; its builder's part ruled.** A start value is read from no
   fact, by one function of the layer's id beside `depot-at-making`. The making act does not change.
   Reason: a layer that is already made holds no such fact, so the code must read "no fact" either
   way, and a third fact at the making would change the builder's form of a making act for nothing
   that the contract asks. The receipt says what it costs: a later change of the kept answer changes
   how every layer with no fact reads. Row for the queue (K14's first half).
7. **P7, "see" stays "see" — kept.** It falls out of one write for a permission's place. Row for the
   queue.
8. **P8, what "left one out" says — provisional, for Sid.** The contract's kept line is built: the
   read says that it left a layer out. The working answer adds which layer, and one of two words for
   why: she may not see it, or it is not made. Reason: the asker gave the list, or read it from a
   layer that she may see, so the id tells her nothing new; and one word for both would make the kept
   line false for a layer that is not made. What a read says of a layer that it left out is made by
   one function. Row for the queue.
9. **P9, how far a read walks — provisional, for Sid.** The one list: a read from a layer looks in
   that layer and then in the layers of its own list, in the list's order, and no further. Reasons:
   it is what both pages say ("A read gets that list"; "The list that the layer gives"); it costs one
   read; no two lists can meet or name each other; and "the one who writes the fact states the
   order" then gives the whole order. One function turns a start layer into a list of layers, so the
   walk is a change of that function. The builder's parts: a list that names its own layer or a layer
   twice is read with each layer once, at its first place, and the start layer always first. Row for
   the queue.
10. **P10, a marked key whose value is no thing's id — provisional, for Sid.** Every fact with a value
    under a marked key is kept in the other order, whatever the value is, by a text of the value that
    no other value has. That is the contract's own sentence, "the gates keep each fact in one more
    order, by value". Nothing is refused, and no gate judges what an id is, which is the queue's K5.
    The run of one value never holds a row of another value. A retract has no row in that order. Row
    for the queue: what a marked key may hold.
11. **P11, the store's own facts in a tool's read — provisional, for Sid.** A read leaves out each row
    under a key of the store's namespace unless the query asks for them. Reasons: a run across
    entities otherwise carries every request for locks, which is the door's working and no tool's
    fact; and the default that shows less is the one whose other answer is a small change. Any asker
    who may see the layer may ask for them. Row for the queue: may a member read a layer's
    permissions, setting, list and marks.
12. **P12, "what points at a thing" through layers — provisional, for Sid.** B3 holds for this word as
    the contract writes it: a place that a farther layer's run gives is shown only when no nearer
    layer holds a row at that place. Reason: it is the page's rule, and without it a person who moves
    a block in her own layer still sees it where the base has it. It costs one read, or one push, for
    each such place and each nearer layer. The measures of C give that cost for a canvas through one
    layer and through three, and the receipt puts the number beside the row for the queue. One
    function makes that check, so the cheap answer, the runs joined, is that function left out.
13. **P13, one moment for all the layers — kept as it falls out.** No lining up. The queue's ?6 stays
    open, with the paper's finding.
14. **P14, what a read gives for a value — builder's.** A read tells four things apart for a place: no
    fact in any layer that she may see; a fact that says "nothing here"; a fact with its value, read
    back from its one text into the value that a maker gave; and a fact whose bytes are no value of
    the form, given with a word that says so. The read path never throws on a row. No gate checks
    what a lock opens: the contract does not ask it.
15. **P15, R4 — builder's.** "Is sent" is read as entry 7 has it: the caller is handed nothing of a
    part that she did not ask for. Rama sends the runtime the whole row. The docstring says what
    crosses from Rama to the runtime for each row. Askers who asked for different parts of the same
    rows share one push.
16. **P16, A7 — builder's.** (1) The first sentence is read as the contract reads it when it says that
    the repair "gives both": a read shows no fact that a second try could change. (2) The door's
    three reads stay held to closing, as the contract says of the door's requests. So a reader can
    see a fact of the base up to one run before its maker reads "accepted" for it at the door; the
    receipt says so. (3) Under the repair, the facts that the read path itself reads in the base (a
    permission, the setting, the list, the marks) are read as a tool's facts are, straight. (5) The
    streaming gate's question about the base stays held to closing. (6) The receipt says that the
    repair keeps a first part of what rule 9 left to Sid. What "the run holds" means is ruled with
    the batch probe's result, below.
17. **P17, what a second attempt gives — settled by `batch-probe/RESULTS.md`, as far as one machine
    can settle it.** The build session read the probe's table and its section B7 whole. The window is
    real and was seen: with one task's thread held, three tasks showed a batch's rows 3.4 s before
    the fourth, and when the commit then failed Rama ran the batch again and replaced those rows
    (B7.2). A failure while a batch is processed shows no row at all (B2.1). One road kept a value
    across attempts: an append with `:ack` to a second depot that a stream topology of the module
    puts in a PState of its own, first write wins; a second attempt read the first attempt's clock
    each time (B3.ii, B4). With the clock kept and the rows the same in every part, a select was
    given no change and a push got one more callback with the same value (B7.3). So **the repair is
    built**, under these rules:
    - (a) The clock is kept before any stamp is made from it, and only in a run that has things to
      decide.
    - (b) Only the clock goes on that depot. A lock's bytes never do (rule 5). The door's three reads
      stay held to closing, because a new lock's bytes and one refusal word still differ between
      attempts.
    - (c) The read path gives its caller nothing when a push calls back with a value that is the
      same.
    - (d) What "the run holds" means. The proof makes a real batch of the store's microbatch gate run
      a second time, with no hook in the module's source (the probe's B5.a), and shows that each
      fact's row of the second attempt is the same in every field, the stamp too, as the first
      attempt made it. If the build cannot make that hold on the store's own gate, it stops the
      repair and keeps the row before beside the new one, as the contract says.
    - (e) The receipt says what was run where. The window, and what a reader is given across it,
      were run on the probe's module and not on the store's. A killed process, a change of leader
      and a real cluster were not run.
18. **P18, the three new keys in a microbatch layer — builder's.** Their rows are on every task, as a
    layer's depot fact and each permission are: a gate reads the marks where an act is decided, and a
    read finds the setting and the list on the task where it reads (Sid's reason for F5 A, read onto
    these). A mark holds from the batch after the one that wrote it: each act of a batch is decided
    with the marks as its task held them when the batch began, which is the same on every attempt
    and costs one read on the act's own task. The other way sends a question from every act of every
    batch to one task, for a write that is rare; rule 6 warns of that. The store's page says plainly
    that a mark and a fact of one batch are not decided in the order of their turns.
19. **P19, the form of the mark — builder's.** One fact on the layer's own entity, whose value holds
    the layer's marked keys. A gate then reads the marks of an act with one read, where one fact for
    each key would cost a read for each key of the act. How many keys a layer may mark is a number of
    the build, kept and not decided.
20. **P20, facts before the mark, and a mark taken off — builder's.** The other order holds the facts
    that are written while the key is marked. Nothing is filled from before, and no event or batch
    walks a layer. A read of "what points at" is told since which stamp that key's order is level.
    The rows of a key whose mark is taken off stay. A later mark of the same key must never show a
    row whose fact is no longer the current fact of its place: the plan says how, and what it costs.
21. **P21, a retract and `:replaces` under the new keys — builder's.** A retract under a store key
    stays refused. A mark is taken off, and a list is emptied, by a second write of a value that says
    so. `:replaces` stays optional, as for every fact (rule 2): two members who write a setting at
    once leave the later one, and a writer who must not lose a write names `:replaces`.
22. **P22, the who and the permission in a row — builder's.** Each is a field of one class, held as
    the text of what it is. No row holds a collection.
23. **P23, what changes a live read beside a fact — builder's.** A live read follows each fact that
    shapes it, by a push of its own that those who need it share: whether a layer is made, its
    setting, the asker's permission, the list of the start layer, the marks. So a layer that she may
    now see joins the read; a list that is written again changes the layers under the open read; a
    layer that is made joins; a key that is marked starts to give rows. Reason: R5, "a screen is shown
    the newest state", and one way of working for joining and for ending.
24. **P24, the ending — builder's.** When a layer is no longer rightful for an asker, her live read in
    that layer ends, and the read through the list goes on and says that the layer is left out. Each
    place that the layer had won is given again with what now wins it. She is told a word of the read
    path's own and nothing of the layer's facts. When Rama ends a push, the read ends with a word
    that says so, and the caller asks again; the read path does not open it again by itself. When
    Rama sends a value anew, the caller is given a whole new value.
25. **P25, leaving — builder's.** A caller says that an asker left by the handle that the opening
    gave. Nothing bounds a caller that never leaves but the end of the process, and the docstring
    says so, as the door says of its stocks. The two races of B5 are closed in the code that shares
    a push.
26. **P26, R1 and R2 — builder's.** R1 is held by the tree: one namespace opens the tables to read
    facts, and the proofs' own handles are the stated exception. R2: what a live read gives is worked
    out from the values that Rama's pushes hold, at each change, for the places that the change
    names. The read path keeps who looks at what, what each asked for, and the pushes. It keeps no
    put-together value between two changes. If the build finds that it needs one, it stops and says
    so on this page, as the contract asks.
27. **P27 — facts; the measures of C give the first two, in-process.** The receipt names what was not
    run.
28. **P28, the shapes of the measures — builder's.** The plan gives each shape. At the least: the
    streaming gate, and the base beside it; live reads open on the layer in three counts; a canvas of
    5,000 facts as one run, and as "what points at the canvas", once and live, through one layer and
    through three; how late a push is, from the append's call and from its ack, for each depot. No
    target.
29. **P29, "any run" — builder's.** The plan gives the words of a run. Both ends are in one layer and
    are made of ids within the limit. In the base a run across entities has one part on each task;
    the read path puts the parts together and gives once a row that every task holds.
30. **P30, which record a read looks in — builder's.** The record that `depot-at-making` names, as
    `depot-of-layer` does. Whether the layer is made is read from its depot fact there, only when the
    asker's own permission does not already say so.
31. **P31, who asks — builder's.** A caller names one asker for a read: an id, or the read path's word
    for a person who is not signed in. An asker that is no id within the limit is turned away with a
    word. `:system` is a who like any other.

## For the receipt: what Sid should know, as found so far

Nothing yet. The receipt's parts, as the contract lists them: the line "proven by a hand-played
asker, used by no inhabitant yet"; the commit hashes; the proofs' outputs with their count lines
before and after; the measures of C; every provisional choice; every not-decided thing as it was
built; every place where the contract could not be met as written, and why; what the build found that
is not decided, proposed as rows for the queue and decided by nobody; `git status --short`.

## For a session that takes this build over

Read the list above in its order, then the table of the stages: the first row that is not "done" is
where the build stands. A stage that is "running" was given to a fresh agent by the session before
you; if no paper of that stage is in this folder, run the stage again from its start. Rule on a paper
before the next stage starts. Keep this page true at each stage, and commit it with the stage's
paper.
