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
| a run on Rama beside the stages: what a second attempt of a batch does, and what state survives a failed attempt (`batch-probe/`) | done; ruling 17 | `00721838` |
| the implied spec, with the check of each "as built" line of the contract against the code; rulings on its 31 points | done | `dc70e98b` |
| the split into parts: three, `base-for-a-reader`, `what-the-gates-keep`, `read-path`; rulings 32 to 39 on its notes | done; judged sound | in the commit after `dc70e98b` |
| the plan of `base-for-a-reader`, with two runs of its own; rulings 46 to 51 on its six points | done | in the commit after `b378e8f0` |
| the plan of `what-the-gates-keep`; rulings 40 to 45 on its five points | done | in the commit after `a8c1783b` |
| the plan of `read-path`, with two runs of its own; rulings 60 to 70 on its eleven points | done | in the commit after `a6857ba8` |
| the check of the plan of `what-the-gates-keep`; rulings 52 to 55 | done. Verdict minor-fail: eleven faults, each fixed in the plan by the check; it ran the removal of a top-level row (`plan-check-probes-what-the-gates-keep/`) | in the commit after `2ea0fff9` |
| the check of the plan of `base-for-a-reader`; rulings 56 to 59 | done. Verdict minor-fail: nine faults, each fixed in the plan by the check; it applied ruling 49 and ran three clusters in one JVM (`plan-check-probes-base-for-a-reader/`) | in the commit after `794bc2fd` |
| the check of the plan of `read-path`; rulings 74 to 80 | done. Verdict minor-fail: fifteen faults, each fixed in the plan by the check; it broke and repaired the plan's argument for "what points at", applied ruling 64, and ran nine things on Rama's client (`plan-check-probes-read-path/`) | in the commit after `ffc10f29` |
| the build of `what-the-gates-keep` (first, by ruling 53); rulings 71 to 73 | done. The build stage's verdict is pass. The build session ran the four proofs itself: act `{:test 8, :pass 160, :fail 0, :error 0}`, pure `{:test 17, :pass 671, :fail 0, :error 0}`, module `{:test 31, :pass 333, :fail 0, :error 0}`, door `{:test 27, :pass 199, :fail 0, :error 0}`, each with exit 0 | `371c6d2f` the code with its proofs; `ffc10f29` the papers |
| the build of `base-for-a-reader` | running, on the tree at `371c6d2f` | — |
| the build of `read-path` | running, beside the build above (ruling 38), on files of its own until that build has ended | — |
| one fresh review of the whole against the contract | — | — |
| the measures of the contract's section C | — | — |
| the receipt to Sid | — | — |

How a stage is run: a fresh agent reads the skill's doc for its stage, the contract and its read
list, and the papers before it; it writes one paper (or the code) and does not commit. This session
reads its report and the parts of its paper that need a ruling, checks the facts that a ruling rests
on in the source, rules, has the rulings applied, and commits with a message that says what and why.
Each commit names its own paths in the commit itself (`git commit -m ... -- <paths>`), since another
session's staged files are in the index. No `Co-Authored-By`, no session link. Never push. An agent that writes code runs on Opus 5.5 at
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

### On the split into parts (`DECOMPOSITION.json`), its eight notes

The split is judged sound: three parts, `base-for-a-reader` (A7 alone), `what-the-gates-keep` (A1 to
A6 for both gates, and R7), `read-path` (B1 to B5, R1 to R5, and C). A cut by gate was weighed and
left: both gates share one declaration, one set of checks and one row shape. The papers of a part
carry its name: `PLAN-<part>.md`, `PLAN_VALIDATION-<part>.md`, and so on.

32. **The order.** As the split has it. `base-for-a-reader` is built first: its run of ruling 17 (d)
    says what a row of the base may rest on, before anything is built on those rows.
33. **The other way of A7.** The plan of `base-for-a-reader` states the stop test and what "the row
    before" would ask of each part. It does not design that way in full. If the stop is met, that
    plan runs again and the two later plans are changed where they touch a row of the base. The check
    of `what-the-gates-keep` may pass before that run is in; its pass then holds only if the run
    holds, and the page says so.
34. **A depot that holds no act.** Sid's decision of 7 October is "Two depots, one for each gate". The
    clock's depot takes no act and no gate decides from it, so the two depots of acts stay two. Nobody
    asked him. A line for the receipt.
35. **Rows of the store's page that word an earlier decision of Sid's.** Such a row's wording of his
    decision is not written again as new text. Its "Where it shows" side changes with the code, and
    rows are added for the entries of 11 and 12 October. The decisions sections of
    `src-store/README.md` are his and are not edited by this build.
36. **The measures of C.** The plan of `read-path` gives each shape. The measuring code lives in
    `test-store/`, beside the proofs, as a run that is no proof: it prints what it measured and holds
    nothing to a number. The build session runs it at the end and puts what it printed in this folder
    and in the receipt.
37. **No cut in `read-path`** now. If its plan grows past what one session can check, the plan says
    so at its top and names the cut between a read asked once and a read kept live.
38. **The plans of the first two parts are written side by side**, and each check reads both. The one
    walk of a batch is changed by two plans: `base-for-a-reader` owns the batch's clock in step 2 and
    the depot and topology that keep it; `what-the-gates-keep` owns every other change of the walk.
    Each plan names the lines of the walk that it changes. The builds of the first two parts run one
    after the other. The plan of `read-path` starts when the plan of `what-the-gates-keep` is written,
    and takes the record's forms from that plan and not from the code. Its build may start while
    `what-the-gates-keep` is being built, on files of its own only; its proof against the module runs
    when that build has ended. This is one step away from the skill's "each subsystem builds on the
    subsystems before it", taken for time at Sid's word that the build must end by the morning.
39. **The head comments.** Three source files tell whoever changes them to read the store-module's
    plan of that part again. The plan sessions read those plans' sections on the walks. Each such
    comment gains this build's plan of the part that changed the file.

### On the plan of `what-the-gates-keep` (`PLAN-what-the-gates-keep.md`), its five points

The build session read the plan's "PState Design" (the fact's row, the three keys, the permission, the
mark, the facts by value), its hand-off section, its open points and its difficulty log whole. The
forms are judged sound and go to the check as they stand.

40. **A mark of a key in the store's namespace is refused**, by the declaration, with a plain word. The
    store's own facts are not kept in the other order (the plan's point 1, its (a)).
41. **A key that is newly marked in a microbatch layer gets the last stamp of its batch**, not the
    stamp of the act that marked it (point 2, (a)). One sentence then holds for both gates, and a read
    may say it: the order of a marked key holds every fact under that key whose stamp is later than
    the key's stamp, and no other. It adds two lines and one argument to step 2 of the walk, beside
    the clock's lines of the other part and not in them; the two checks read those lines together.
42. **The "text that no other value has" of ruling 10 is the SHA-256 of the value's bytes**, 64 hex
    characters in the row's key (point 3, (a)). A key then has a bound whatever the value is, and a
    value's text may hold a space.
43. **Since which stamp a key's order is level is kept in the row of the marks**, as a field that the
    gate derives (point 4, (a)), and that stamp is part of each row's key in the other order. A mark
    that is taken off and put on again gets a new stamp, so it names other rows; the rows of the
    earlier marking stay and no read names them. This is how ruling 20 is met.
44. **A reply carries the row before with its whole value**, as today (point 5, (a)); the measures say
    whether that must change.
45. **Where the microbatch gate reads the marks.** Ruling 18 counted one read on the act's own task.
    The plan has the home task of each place read the marks in step 2, once for each task and layer in
    a batch, with the place's row before in hand. It is the same single read for an act on one entity
    that is alone in its batch, fewer under load, and one for each task for an act that is spread
    over several. Taken as the plan has it; it replaces that sentence of ruling 18.

### On the plan of `base-for-a-reader` (`PLAN-base-for-a-reader.md`), its six points

The build session read the plan's report, its changed step 2 line by line, its open points and its
lines for the receipt. The plan stage ran two things of its own (`plan-probes-base-for-a-reader/`):
on a stand-in walk, that the append works in place on the gate's task and that a run with nothing
pays nothing; and on the store's own module, with nothing under `src-store/` changed, a second
attempt of a real batch: 15 of 15 rows differ between the attempts today, in `:stamp` only. So what
the repair must make the same is the stamp, as the implied spec derived.

46. **A stream topology of its own keeps the clock** (the plan's point 1, its (a)): `batch-clock`,
    beside `streaming-gate`. As a second source of the streaming gate, every batch of the base would
    wait in that gate's queue on one task, and the streaming gate would own a row of the microbatch
    gate's working. The skill's "at most one" guards against a topology with no reason; this one has
    one. A line for the receipt.
47. **The table keeps one row, the newest batch's clock** (point 2, (a)). The depot keeps every
    record. A record of the same batch or of an older one writes nothing.
48. **The window on the store's own module is left to the batch probe** (point 3, (a)), and the
    proofs' map and the receipt say so (ruling 17 (e)).
49. **The check of the second attempt has a cluster of its own** (point 4, its (b), against the plan's
    recommendation). Every other check of the module's proof runs on the functions as the store
    defines them; the three functions that the proof replaces are in place only for the cluster that
    makes a batch run twice, and are put back after it. Reason: what a pass of the module's proof
    shows stays plain, the module as defined. It costs about 30 s for each run of that proof. If two
    clusters in one JVM do not work, the check becomes a proof file with a command of its own, and
    the proofs' map says how to run it.
50. **The depot of batch clocks is not trimmed** (point 5), as the depots of acts are not. Nothing
    reads its old records. A line for the receipt, and a row proposed for the queue: is the log of
    batch clocks kept for ever, as the logs of acts are.
51. **Rama's phase timeout stays unset** (point 6, (a)). After a fault of the clock's topology on
    another worker, the base can stand still for that timeout, about 30 s in the plan's run, and no
    stamp is made meanwhile. A line for the receipt.

The plan reads ruling 17 (b) as "nothing of an act and no lock's bytes": the record on the clock's
depot holds the batch's id beside its clock. That is the ruling's sense. When the gate cannot read a
kept clock back after the ack, it throws and Rama runs the batch again: it is the one place where the
module's own code throws, and it guards against a stamp from a clock that is not kept.

For the receipt, from this plan and its check: two attempts of a batch can still differ across a
module update that changes the code that makes a row, when the update falls between the two attempts;
the update that first brings the repair is one such; and Sid's line of 7 October, "a second depot of
decisions adds nothing", stands beside ruling 34: the new depot holds no decision, and it does hold a
thing that the depots of acts cannot give again, a reading of the clock.

For the receipt, from the plan of `what-the-gates-keep`: after A1 no row of the facts table names a lock, so a lock is found
only from the act in the depot; one value that very many places hold is a run on one task, which the
contract's "a run of neighbouring rows" asks for; the removal of a row of the other order rests on a
navigator that the store has not run before, so the build's proof must show it.

### On the check of the plan of `what-the-gates-keep` (`PLAN_VALIDATION-what-the-gates-keep.md`)

52. **The verdict is minor-fail and is taken.** Eleven faults, none of the design, each fixed in the
    plan by the check; the forms of rulings 40 to 45 stand. The check counted the reads, writes and
    messages from the walk itself, traced the microbatch gate with real batches by hand, and ran the
    one thing that the store had never run: the removal of a row at a top-level key works in a stream
    and in a microbatch topology, and a failed attempt's removal is set back. The pass holds only if
    the run of ruling 17 (d) holds (ruling 33).
53. **The order of the builds changes; ruling 32 is replaced.** `what-the-gates-keep` is built first.
    Its plan has passed its check while the other check still runs, and the two builds run one after
    the other. The split's first note allows it: the two parts share no mechanism. One thing moves
    with it: that this part's new rows are the same on a second attempt is shown by the build of
    `base-for-a-reader`, which now comes after and sees every row. The risk that is taken: if the run
    of ruling 17 (d) does not hold, this part's rows of the base are planned again. The plan stage's
    own run on the store's module makes that unlikely: two attempts differ today in the stamp only.
54. **Three names that the rig's code also has stay**: `:by-value`, `value-digest` and `read-values`.
    The plan's read list holds no file of the rig, so none comes from it. "By value" is the contract's
    own phrase in A6, and "digest" is the store's word since its module. Each says what the thing is.
    The receipt names the three for Sid all the same; each is his to change.
55. **A cheaper form that the check found is not taken.** One row for each streaming layer, holding the
    layer's last stamp and its marks, would give an accepted act 4 reads again where this build makes
    it 5. It changes what a stamp is, from "increasing on its task" to "in its layer", and the
    contract changes two of the store's rules and no stamp. A row proposed for the queue, to weigh
    with the measures.

For the receipt, from this check: in a streaming layer every accepted act with a fact under a plain
key pays one seek more than today, 5 where it paid 4, also in a layer that marks no key (by the
store's arithmetic 384 to 616 acts a second on one task, against 480 to 770); a push on a run can be
sent the removal of a place that the run never held, and the read path takes it as no change; after a
worker of Rama was started again, a stream topology was handed its partition's records once more, so
check (a) and the clock keeper's "first write wins" are what make that harmless.

### On the check of the plan of `base-for-a-reader` (`PLAN_VALIDATION-base-for-a-reader.md`)

56. **The verdict is minor-fail and is taken.** The design holds: the check found no trace that reaches
    a stamp from a clock that is not kept, and none in which two attempts make a fact's row, or an
    answer's verdict or stamp, differently. Nine faults, each of the plan's text or of its proof, each
    fixed in the plan. Two of them were ways for the proof to pass falsely: it could not tell two
    attempts from two batches, and its forced throw left the first attempt's note whole only by where
    a task happened to sit. Both are closed in the plan.
57. **Ruling 49 is applied and was run.** Three clusters one after the other in one JVM, on the store's
    own module: a function replaced after the first launch runs in the second cluster, and put back,
    the third runs the store's own. The second cluster costs about 6 s for each run of the module's
    proof, not 30, and needs no proof file of its own.
58. **The head comments are four, not three**: `module/door_reads.clj` has one too. Ruling 39 holds for
    each file that a part changes.
59. **The gate's one throw stays**, with what it costs said plainly: a throw in the gate starts the
    worker again, with both gates and the reads on it, and while its cause stays the batch fails each
    time. It is reached only when a clock that was acked cannot be read back, which no traced case
    gives without a fault of the cluster or of this module. The other answer, a stamp from a clock
    that is not kept, is the one thing that must not be made. A line for the receipt.

For the receipt, from this check: after a commit that fails, the tasks of the base can show one batch
seconds apart; not run here: the late ack of an attempt that Rama gave up, what the append gives when
the keeper fails, a move of a leader, a module update in the middle of a commit.

### On the plan of `read-path` (`PLAN-read-path.md`), its eleven points and three readings

The build session read the plan's report, its section "Live" whole (what one live read opens, the
steps of a callback, why no put-together value is kept, what a live read follows, the ending), and its
open points and lines for the receipt. The plan stage ran two probes of its own
(`plan-probes-read-path/`): how a client learns the number of tasks and names one task for a select
and for a push, and which paths Rama serves for a run. The plan says that no ruling fails, that it
needs no store of its own beside Rama's push, and that it needs no cut (ruling 37).

Three rulings are read by the plan in a way that it states at its top; each reading is taken.
- Ruling 23: one push follows a layer's depot, setting, list and marks together. They are neighbours,
  all on the layer's own entity, so it is one run.
- Ruling 23: an asker's permission is followed only while the layer's setting is "members". Under
  "everyone" it shapes nothing.
- Ruling 30: the depot fact is always read, since it rides that same run at no cost.

60. **Once or live is the call, not a part of the query** (the plan's point 1, its (a)): `read-once`
    and `read-live` take the same query.
61. **A read asked once reads a layer's fact in the same call as its rights** (point 2, (a)). The
    runtime can so read up to 257 rows of a layer for an asker who may then not see it, and drops
    them; nothing of them is handed over. It halves the round trips of the commonest read. The other
    answer is one function that gives false. A line for the receipt.
62. **One task of a microbatch layer is named with `com.rpl.rama.Helpers/genHashingIndexKeys`**
    (point 3, (a)). Rama's pages show it for tests; it ran on Rama 1.6.0 for a select and for a push.
    Only a run across entities of the base needs it. A line for the receipt, and a question for
    Rama's team; Sid's line on that sheet is "we will do it right as rama team will suggest".
63. **The numbers of the build stand as the plan lists them** (point 4): 256 places read first; 20,000
    places for one run; 40,000 checks of ruling 12 for one read; 2,000 paths; a wait of 10 s. None
    bounds the bytes of a run: no table gives the size of a run's values before it is read. Each is
    kept and not decided.
64. **Ruling 12's edge is closed the other way, against the plan's recommendation** (point 5). At a
    place that a farther layer's order gives, the nearest layer that holds a counted row wins, and the
    place is given when the winner's value is the thing that was asked for. So a nearer layer whose
    own fact at the place points at the same thing no longer hides the place: the read gives it from
    the nearer layer's row, which the check of ruling 12 already has in hand. Reason: that is B3 as the
    contract writes it; and a hole where two layers say the same thing is a wrong answer that the
    read can see. It costs a comparison of bytes. The asker is still told, for each layer, since which
    stamp its order is level; a place of a nearer layer that no farther layer's order holds is found
    only through the nearer layer's own order. The count of pushes stays one for each such place and
    nearer layer; the measures give it, and the row for the queue (P12) carries the number.
65. **`:from-layer` when the asker may not see the start layer — provisional, for Sid** (point 6, (a)).
    The layer's list is not used. The answer says that this one layer is left out, and gives nothing
    else. With the list used, she would learn what a layer that she may not see builds on. It goes
    with ruling 8 in the row for the queue.
66. **Five functions more in `record/rows.clj`** are added by this part's build as its last step
    (point 7, (a)), when the build of `what-the-gates-keep` has ended; its pure code is written
    against them from the start.
67. **The asker's function is called on a thread of Rama's client, under the read's lock** (point 8,
    (a)). The docstring says what the function must not do. The check of the plan is asked to break
    this: a function that leaves, or that asks again, inside its own call.
68. **A place under a key of the store's own, asked without the store's own facts, is turned away with
    a word** (point 9, (a)). To answer "no fact" would say that a layer has no setting when it has one.
69. **When a thing that shapes a read changes, the asker is given the whole answer anew** (point 10,
    (a)): a setting, a permission, a list or a mark. This replaces the sentence of ruling 24 that gave
    each place again one by one. Such changes are rare, and one form fewer reaches a caller.
70. **A finding for the queue, decided by nobody** (point 11). One select with a path that Rama does not
    serve stopped a worker of the in-process cluster. Entry 4 trusts each program that reaches Rama's
    ports with the whole store; such a program can also stop a worker with one call. The read path
    sends five forms of path and no other. A line for the receipt and a row proposed for the queue.

### On the build of `what-the-gates-keep`

71. **The build's verdict is pass and is taken.** The build session ran the four proofs itself on the
    tree as the build left it; the count lines are in the table of the stages. It read whole the
    changed dataflow of the two gates, and in `gate/decision.clj` the standing with its new check, the
    opening of a sealed value, the stamps of the marks, and the writes of the facts by value. It
    traced those writes by hand against the plan's sentence for a marked key: a fact replaced, a
    retract, the same value written again, a fact of the act that marks its own key, and a mark taken
    off and put on again with the fact replaced meanwhile. In each the run under the key's current
    stamp holds a place exactly when the place's current fact has that value and was written since
    that stamp. It checked ruling 35 in the rows of the store's page and of the door's page that word
    an earlier decision of Sid's: his wording stands, and the "Where it shows" side says what the code
    now holds.
72. **The one place where the plan could not be met is taken as built.** The plan's line had the gate
    read a layer's marked keys for every question about a place; its own cost table said that a
    request for locks reads what it reads today. The build kept the cost table: a question about a
    place under a store key asks for no marks.
73. **43 checks of the proofs were written again and none was removed**; six of them were beyond the
    plan's list. `TEST_VALIDATION-what-the-gates-keep.md` gives each with its old and new sense and
    the decision that made the old one untrue. The check that this part's rows are the same on a
    second attempt was not written, by ruling 53; the build of `base-for-a-reader` writes it.

For the receipt, from this build: what no check of this part reaches is a push, a second attempt of a
batch, an event that Rama runs again, a real cluster, and the cost of the gate's one read more, which
is counted from the dataflow and not measured; the build session's page lists scratch that the build
left under `reads/runs/` and did not commit.

### On the check of the plan of `read-path` (`PLAN_VALIDATION-read-path.md`)

74. **The verdict is minor-fail and is taken.** Fifteen faults, each fixed in the plan. The check was
    asked to break the plan's least certain argument, that no "before" value need be kept, and it
    did, for one word of a query. For a place, an entity and a run the argument holds as worded (a
    model played 620 lists of callbacks in every order). For "what points at" it did not: when a
    farther order loses a place in the moment in which a nearer layer gets a row there, one order of
    the two callbacks left the asker with a place that the tables no longer give (118 of 672 small
    lists). A fix that keeps no value exists and is in the plan; played in every order, 13 scenarios
    and 672 lists, none fails. That is derived and modelled, not run on Rama. So the build does not
    stop for R2. The check applied ruling 64 first, and ran nine things on Rama's client about
    threads, openings and callbacks (`plan-check-probes-read-path/`).
75. **Ruling 67 stands, with its cost as it was run.** While one asker's function stayed for 2.5 s,
    other askers' pushes on the same thread of Rama's client waited, and so did selects and openings
    of pushes from other threads. So the function must return at once, and a caller that has work to
    do hands the change to a thread of its own; the docstring says what was run. Rama's own callback
    has the same rule, and the read path passes it on. The other answer is an executor of the read
    path's own with a queue. A line for the receipt.
76. **A read holds one mark more: "the whole answer is owed".** It is true or false, and no value of
    the tables. Without it a layer that joins over a farther order never gives its own places. Ruling
    26's list of what the read path keeps gains it.
77. **A read asked from inside an asker's function is turned away with a word of its own.** Without
    the guard such a read can wait out its bound with a thread of Rama's client held.
78. **The function's first call is the whole answer** (the check's point 4, its other answer, against
    its fix by docstring). `read-live` calls the asker's function once with `{:anew answer}` on the
    caller's own thread, under the read's turn, before it returns and before any change is told. Then
    a screen that is made only from what the function was given is right, in order. With the first
    answer only in what `read-live` returns, a change could reach the function before its caller had
    that answer in place, and a caller that then put the answer in place would show an older state
    over a newer one, against R5. What `read-live` returns still holds that same answer under `:now`,
    and the docstring says that a caller who keeps a screen uses the function's calls alone.
79. **A cheaper way for the checks of ruling 12 is not built in this build.** The check designed it on
    paper: where a nearer layer is small, read it whole once, or keep one push on it, and take every
    check of that layer from it; in the worked case 2 pushes against 10,020. The skill says not to put
    a cheaper design aside without building it. It is put aside here for three reasons, and the
    receipt says so. What "what points at" means through layers is Sid's and not decided (P12), so
    the checks may go or change. The way that is built was played in every order by a model, and the
    cheaper one was not. And it is a second way to answer one question, with a switch by size under
    an open read. One function makes the checks, so the cheaper way is a change of that function. The
    measures give the cost of the way that is built, and the row for the queue carries both.
80. **The read path's pure checks are a proof of their own**, beside its proof against the module, each
    with its own command, and the proofs' map says so. The plan put them in `pure_proof.clj`, which
    the build of `base-for-a-reader` is changing at the same time; and that file is already long. The
    pure proof of the read path runs on the alias with Clojure alone, so it also holds that the read
    path's pure namespaces load with no Rama.

For the receipt, from this check: Rama's client had two threads in-process, and how many it has on a
real cluster is not known here; a call of Rama that blocks, made inside a callback, hung in one run of
three, and the read path makes none; after ruling 64 a place of a nearer layer that no farther layer's
order holds is still found only through the nearer layer's own order; a run across entities of the
base costs one seek on each task, so it grows with the cluster; a live read across entities of the
base would not notice a change in the number of tasks, which never changes.

## For the receipt: what Sid should know, as found so far

1. **A slip of the build session, undone.** Its first commit took another session's staged rename
   with it (`decisions/groups/g-reads.txt -> g-reads-first.txt`): a commit with no path takes the
   whole index, and not only what was added by name. A minute later, with nothing built on them, the
   build session undid its own two commits and made them again by path (`00721838`, `dc70e98b`). The
   rename is staged and not committed again, as it was; no file changed. From then on each commit of
   this build names its paths in the commit itself.
2. **The four proofs' checks change.** See ruling 1: the row that A1 changes, and the exact set of
   store keys, are checked by today's proofs.

The receipt's parts, as the contract lists them: the line "proven by a hand-played
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
