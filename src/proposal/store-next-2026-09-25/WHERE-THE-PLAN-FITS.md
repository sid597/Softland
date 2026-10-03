# Where the plan fits

The markdown twin of the page https://claude.ai/artifact/X2mz1vGvYkNcp6KStQ9Zxa, made from the same data. The page was made for Sid on 25 September 2026 (afternoon, IST) by the session "Store work phase planning and execution roadmap"; the twin was transcribed by another session at 16:36 IST and corrected by the page's session at Sid's word ("write the map") that evening; "you" is Sid. Nothing here is a ruling.

**Status, 25 September 2026, evening.** This map lays the three sessions' conclusion (CONCLUSION.md beside this file) over the history. Sections 1 and 2 (levels, threads) and 4 (the eight rulings as proposed) still hold. Sections 3 (the proposed order) and 6 (who decides) describe the conclusion *as proposed* and are superseded by `src/proposal/frame-2026-09-15/PROGRESS.md`, "Next", settled the same evening: the rig is a candidate for the store core, not a throwaway and not the first edition; no ruling on R1 to R8 before the seed round, which pulls what it meets; the operator question on forgetting stays open, due before phase 2; a held-back tool gives the count its blind test. Map notes 1 and 7 in section 7 were wrong and are corrected in place. "The count on throwaway records" in the short answer below is the conclusion's wording; the records are still not kept, but the code may be.

**Marks.** [checked] read against the source on 25 September · [sessions] from the three sessions' conclusion or the history account, not re-read · [my reading] derived by the map's session, for Sid to weigh.

**The short answer** [my reading]. The plan is the hypothesis you started with on 1 September: build Softland in Softland. Since then the work has gone down one level at a time. Each question was asked because the one above it couldn't be answered without it. The three sessions' plan starts from the bottom level and climbs back. It tests your hypothesis early, with the count on throwaway records. Then it goes back down for the one step that can't be undone, the first kept record, and ends at one truth.

Sections: 0 The layer up · 1 Levels · 2 Threads · 3 Your phases and the proposed order · 4 The eight rulings · 5 The seed · 6 Who decides · 7 What the map shows · 8 Words · 9 Sources.

## 0. The layer up, in your words

The page was missing the layer this whole map hangs from, and you had already written it that morning. From your prompt of 25 September, 05:43 UTC, to the session "partition-seam-architecture" (recall id e7a25bb0-7322-449e-86a8-05159809dbe1) [checked]:

> I'm building Softland, a place that people and agents change from the inside, including the parts they use to change it. Everything lives in one store that only appends and never rewrites a record. I've been working out the rules every record is written under, and I call that the protocol.
>
> It's more than data and schema. In an ordinary app the database holds the data in a schema, and the app's code decides who may do what, what a user is and how versions work, and that code can change any day. In Softland the app's code is itself data, written live by anyone, so nothing above the store can be trusted to decide who may do what, and those decisions move down into the fixed part. Because the store never rewrites, the rules a record was written under stay with it for good. So the protocol holds data and schema, and also fine-grained auth and authorization, versioning, the concept of a user and time travel. It's the one part of the architecture that can't be rebuilt.
>
> ```
> the architecture — everything in Softland
>  ├─ rebuilt many times:  runtime · leaves · renderers · the store's Rama build
>  └─ never rebuilt, only split into before/after:  the rules every record is written under
>       what     entity · key · value · grammar    ← data/schema is only this row
>       who      by whom · session · owner
>       where    layer: base · group · personal · session
>       may      permission · visibility
>       when     clock promises · as-of (time travel)
>       why      based on · because of · read entries
>       change   replaces · retract · forget (locks)
> ```
>
> [...] It fixes meaning, not bytes, so the store underneath can be re-encoded and rebuilt; the store is the Rama build that carries the protocol. Some of my rulings are about how the store carries it, like which kind of gate, placement and where lock rows sit, and those can be rebuilt. The rest are the protocol.
>
> When the protocol work is done we would have the protocol, src/client, src/server and src-inland. [...] Then we use the src-inland example build to start building the first tool in Softland.

What this settles for the map [my reading]: everything below that is about how the store *carries* the protocol (gate kind, placement, lock rows, lease or holder) can be rebuilt, so the model and the build settle it. What is *protocol* freezes at the first kept record, so it is yours, and it is asked at the step of a worked trace where it bites. The first tool in Softland is the round-three tool, and src-inland is where it runs.

## 1. Six levels down, and the way back up

For each level: how the question narrowed, with dates, and the plan's steps that answer it. Step numbers are the plan's order.

### Level 1: Can Softland be built from inside?

How we got down here:
- 1 Sep: Build Softland in Softland, after the curves probe page. [checked]
- 7 Sep: The client’s first subject is Softland itself: use it to change its own codebase. [checked]
- 11 Sep: Inland’s test: point at the thing you are using to point, change it, keep using it. [checked]

The plan's way back up: Step 2 · tool in src-inland · After step 8 · Softland changes Softland.

The plan’s eight steps don’t include using Softland to change Softland. Step 2, the same tool in src-inland, is your first use of the store. The self-changing loop from 1 and 7 September comes back after step 8, once Inland runs on the store core. [my reading]

### Level 2: What must stay compiled code?

How we got down here:
- 6 Sep: Tools are records over a vocabulary; capabilities remain code. [checked]
- 11 Sep: Inland’s floor needed compiled repairs; the smallest floor wasn’t found. [checked]
- 13 Sep: The forced first step: repo material referenced as facts. [checked]

The plan's way back up: A3 · round three · Step 1 · tools on the rig · Step 2 · tool in src-inland.

The count asks Inland’s floor question again, this time of the store: how many compiled steps do one new tool and one new grammar need? Its two classes echo the 6 September ruling. Capabilities were always expected to stay code, so the telling number is the protocol steps. [my reading]

### Level 3: Does everything live in one store?

How we got down here:
- 12–13 Sep: Why isn’t Inland building on the server? One truth or two? [sessions]
- 14 Sep: Two admissions, two vocabularies. [sessions]
- 18 Sep: Ruled: one truth, one Rama for the world. [checked]

The plan's way back up: Step 8 · one truth.

The answer to 12 September turned out to be neither Inland nor the server. The store core is new, and at step 8 both move onto it. [my reading]

### Level 4: What is everything made of?

How we got down here:
- 14–15 Sep: To be pointed at, a thing must exist as data, in layers. One substrate. [sessions]
- 17 Sep: A layer is thin; builds-on is facts; the nearest active layer wins. [checked]
- 24 Sep: Four layer kinds; a stream gate for one-owner layers, a microbatch gate for shared ones. [checked]

The plan's way back up: B1 · phase 5: reads · Step 7 · micro store.

Phase 5 confirms how a read composes layers (the nearest active layer wins). Step 7 brings shared layers, promotion and the base’s re-class, before the first group or the second person. [sessions]

### Level 5: What freezes at the first record?

How we got down here:
- 18 Sep: The store never rewrites a record: find the conventions that would paint you in a corner. [checked]
- 21 Sep: CORNERS.md, from eight research voices. [checked]
- 23 Sep: Your frame: time is the root; grain, binding, source, uniformity; exposure. [checked]
- 23–24 Sep: The store-level rulings, then the nine. [checked]

The plan's way back up: A1 · you rule R1–R8 · Step 4 · edition one · Step 6 · first kept record.

R1 to R8 decide what a kept record contains. Edition one writes it down. Step 6 keeps the first record, in one-owner layers only. [sessions]

### Level 6: Do the rules hold, and does Rama carry them?

How we got down here:
- 23 Sep: Think every scenario through, from time as the root. [sessions]
- 24–25 Sep: The formal model, two rounds: the baseline holds except x2. [checked]
- 25 Sep: Rama probed; rig phase 1 green; three numbers measured. [checked]

The plan's way back up: Step 0 · hygiene · A2 · into the model · B1 · phase 5: reads · B2 · grammars · Step 3 · locks · Step 5 · failover run.

The model checks each ruling (lane A) while the rig builds reads (lane B). Locks and then a two-node failover run test what Rama must carry before anything is kept. [sessions]

[my reading] The plan doesn't climb in a straight line. It reaches level 2 early: steps 1 and 2 are the count, with nothing kept. Then it drops to levels 5 and 6 for locks, edition one, a failover run and the first kept record (steps 3 to 6). Then it climbs back through level 4 to level 3 (steps 7 and 8). Dependencies set the order; the levels show what each step is for.

## 2. Eight threads through the history and the plan

Each thread is one question that runs through both the history and the plan. The first six columns are dated history, "25 Sep" is the three sessions' conclusion, and "Next" is where the thread goes.

| Thread | 1–11 Sep | 12–13 Sep | 14–15 Sep | 16–18 Sep | 20–24 Sep | 24–25 Sep | 25 Sep (conclusion) | Next |
|---|---|---|---|---|---|---|---|---|
| 1 Built from inside | Build Softland in Softland; Inland: the floor stayed | Repo material as facts |  |  | The line to test | The count not started | The thesis test | A3 · round three → Step 1 · tools on the rig → Step 2 · tool in src-inland |
| 2 One truth | Inland’s own Rama | Why not build on top? | Two admissions | One truth, ruled |  | The rig is not the server | Server verdict | Step 2 · tool in src-inland → Step 8 · one truth |
| 3 Made of layers |  |  | Data, in layers | Thin layers, nearest wins | Four kinds, two gates | Stream store green | R7, R8 | Step 6 · first kept record → Step 7 · micro store |
| 4 The first record |  |  |  | Paint me in a corner | CORNERS.md; Your frame: time is the root; The nine | Stamp unit freezes | The one irreversible step | A1 · you rule R1–R8 → Step 4 · edition one → Step 6 · first kept record |
| 5 Forgetting |  |  |  |  | Two deletes; one log; Rulings 7 and 7b | Owner required; Plaintext left in the depot; 169 bytes per value | R1: forget is immediate | A2 · into the model → Step 3 · locks |
| 6 What a read stood on |  |  |  |  | Exposure; read entries | Order only via stood-on; Rama: reads at a moment; Stamps 20.7 s ahead | R2 to R6 | B1 · phase 5: reads → Step 4 · edition one |
| 7 The model |  |  |  |  | Every scenario | Round one: six silences; Round two: A to E | The test every build passes | A2 · into the model → A3 · round three → Step 7 · micro store |
| 8 The rig |  |  |  |  | The rig can start | Rama probed; Phase 1 green; Three numbers | First-edition build | Step 0 · hygiene → B1 · phase 5: reads → Step 1 · tools on the rig → Step 3 · locks → Step 5 · failover run |

### Thread 1: Built from inside

*Can Softland grow by writing facts, not code?*

**1 Sep and 7 Sep · The striving** [checked]
After the curves probe page landed, you named what you were striving for: everything understood, analysed, pointed at and learned within Softland. On 7 September the client’s first subject became Softland itself.

> this artifact that you created this si something that should should should be possible to do in softland … build softland in softland
> — vision/LOG.md:1306-1310, 1 Sep
>
> from there, I could use Softland to modify the codebase itself
> — vision/LOG.md:1360, 7 Sep

Sources: vision/LOG.md:1304-1310 [checked]; vision/LOG.md:1356-1362 [checked].

Connects to: Level 1 · Step 2 · tool in src-inland · Map note 5.

**11 Sep · What the Inland build showed** [checked]
Inland made the hypothesis a test: point at the thing you are using to point, change it, keep using it. Composition by named references worked. The compiled floor did not shrink: pending reads, scheduler ownership, input clipping, GPU lifetime and outcome framing all needed compiled repairs.

> The floor remained consequential. … This run establishes a useful authored construction, not that the smallest sufficient floor has been found.
> — history/docs/build-softland-in-softland/HANDOFF.md:224-229

Sources: history/docs/build-softland-in-softland/HANDOFF.md:217-229 [checked]; docs/builds/inland/intended-design.md:22 [checked].

Connects to: Level 2 · Step 1 · tools on the rig.

**13 Sep · The forced first step, and the tool** [checked]
The settled integration starter made one step unavoidable: repo material referenced as facts. A tool made inside must keep its correspondence as the material changes, and show the result or show that it is stale. Round three designs the seed against this tool.

> Nothing can be by-reference until repo material can be referenced as facts: a passage at a revision, a function at a revision, a decision’s clause, a check and its result.
> — docs/builds/inland/integration.md:36-38

Sources: docs/builds/inland/integration.md:35-45 [checked]; commit 898bfc5b, 13 Sep [checked].

Connects to: Seed: Designed against a real tool · Step 2 · tool in src-inland.

**24 Sep · The rig was to test the line** [checked]
When the nine were ruled, PROGRESS.md named the next step: a Rama rig that tests the line between compiled code and facts.

> the Rama rig that tests the line: add a tool and a grammar from inside, count how many new built-in steps were needed
> — PROGRESS.md:191-192

Sources: src/proposal/frame-2026-09-15/PROGRESS.md:188-195 [checked].

Connects to: Step 1 · tools on the rig.

**25 Sep · The count waits for most of the store** [checked]
Your phase 6 brief asks for the count and every step with its reason. The rig reached phase 1 and the three numbers. The count needs most of the store, so the rig proposed it as the real store’s first milestone.

> Then the count: add one new tool and one new grammar by writing facts only, and count how many new compiled steps were needed. Zero is the target. Report the count and every step that was needed, with why.
> — rig SPEC.md:99-101, your words, 25 Sep

Sources: rig SPEC.md:95-101 [checked]; rig README.md:48 [checked].

Connects to: Step 1 · tools on the rig.

**25 Sep · The thesis test** [sessions] (the conclusion)
The conclusion makes the count the first join step. Add a grammar and a tool by writing facts only, first on the rig, then in src-inland with records not kept. Each compiled step is classed as protocol (gate, envelope, read gateway, index kind) or capability (a new built-in function a recipe calls).

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Seed: Designed against a real tool · Step 1 · tools on the rig · Step 2 · tool in src-inland · Map note 6.

Next: A3 · round three → Step 1 · tools on the rig → Step 2 · tool in src-inland.

### Thread 2: One truth

*Does everything live in one store?*

**11 Sep · Inland ran on its own Rama** [checked]
Inland’s build ran on an isolated Rama beside the server and used none of it. Two days later the integration starter named the divergence as “Inland’s isolated Rama against ‘Rama is truth’”.

Sources: docs/builds/inland/integration.md:27 [checked]; history account, 12 Sep [sessions].

Connects to: Level 3.

**12–13 Sep · Why not build on the server?** [sessions]
When you saw that Inland used none of the server, you asked why it wasn’t building on top of it. The settled starter then made the workpiece expose one truth or two.

> Then this is wrong why should this be? Why not using and building on top?
> — chat, 12 Sep, as quoted in the history account; not found in the repo

Sources: history account [sessions]; src/proposal/inland-integration-2026-09-14/FACTS.md:59 [checked].

Connects to: Map note 2 · Step 8 · one truth.

**14 Sep · Two admissions, two vocabularies** [sessions]
The integration fact base found that src-inland has the loop and src is the world without a loop. That makes two admissions and two vocabularies for the same kind of change.

Sources: src/proposal/inland-integration-2026-09-14/FACTS.md, via the history account [sessions].

Connects to: Step 8 · one truth.

**18 Sep · One truth, one Rama** [checked]
You ruled one truth: everyone on the same database, one Rama deployed throughout the world. People’s own opinions sit over a shared thing below them. One Rama first, then many.

> one thing is ruled since: one truth. everyone is registered on the same database, a singular rama deployed throughout the world. alice and bob write their own opinions on a shared thing that sits below them
> — src/proposal/frame-2026-09-15/LEDGER.md:201

Sources: LEDGER.md:197-203 [checked]; LEDGER.md:213 [checked]; commit b307f46a, 18 Sep [checked].

Connects to: Level 3 · Step 8 · one truth · R8 · The base starts one-owner, owned by the root actor.

**25 Sep · The rig stayed apart** [checked]
The rig was built beside everything else. Nothing under src/app was read or touched, and it ran on the in-process cluster only.

> It is not the server: nothing under src/app is read or touched.
> — rig SPEC.md:14-15

Sources: rig SPEC.md:9-15 [checked].

Connects to: Server verdict (One truth, 25 Sep).

**25 Sep · The server’s verdict** [sessions] (the conclusion)
The store core is new. The server contributes its admission pattern, adapters and registries as seed input, and its side logs go when things are re-pointed. src-inland’s accept module is the nearest ancestor of a one-owner layer: one stream topology, a depot hashed by workspace, genesis rows through its gate. So it is the first place to mount the store. Whether the store replaces that module or sits beside it is undecided.

Sources: the three sessions’ conclusion (25 Sep) [sessions]; server facts: see “What the map shows”, item 2 [checked]; src-inland/softland/inland/module.clj:129-136 [sessions].

Connects to: Map note 2 · Step 2 · tool in src-inland · Step 8 · one truth.

Next: Step 2 · tool in src-inland → Step 8 · one truth.

### Thread 3: Made of layers

*What is everything made of?*

**14–15 Sep · To be pointed at, it must be data** [sessions]
Walking the integration, you hit the floor under it. Anything that can be pointed at must exist as data, and not in one store: in layers, because different data maps to different concepts. The frame picture was recorded on the 15th.

> for something that can be pointed at it has to exist in a data form and its not always a single stored data its is layers
> — chat with Codex, 14–15 Sep, as quoted in the history account
>
> ONE SUBSTRATE TO RULE THEM ALL
> — docs/carry-on.md:248

Sources: history account [sessions]; docs/carry-on.md:248 [checked]; commit dab67696, 15 Sep [checked].

Connects to: Level 4.

**17 Sep · A layer is thin** [checked]
In the walk, a layer became thin. Builds-on is facts, and a reader’s context comes from walking it. The nearest active layer wins. Phase 5 is where this gets confirmed on the store.

> are we treating the layers as like everything everything underneath it or ... can it be more granular ... infinitely compositional
> — LEDGER.md:158, your words

Sources: LEDGER.md:158 [checked]; PARTS-4-8.md:146-153 [checked].

Connects to: Who decides: Phase 5 will surface · Level 4.

**24 Sep · Layer kinds and gates** [checked]
There are four layer kinds: base, group, personal and session, with sessions split into hand and agent. One-owner layers go through a stream gate, placed by layer. Shared layers go through a microbatch gate, placed by entity. That makes two fact stores, each with its own gate.

Sources: PROGRESS.md:67-74 [checked]; PROGRESS.md:109-112 [checked].

Connects to: R7 · Permissions reach a layer through its own gate · R8 · The base starts one-owner, owned by the root actor.

**25 Sep · One-owner layers run** [checked]
Phase 1 built the stream store for one-owner layers. Acts are admitted whole on their layer’s home task, resends are answered from the record, and permissions are granted and revoked. The suite has 13 tests and 728 assertions, and three injected crashes replayed correctly.

Sources: rig RIG.md:19-42 [checked].

Connects to: Step 6 · first kept record.

**25 Sep · Permissions per layer; the base starts one-owner** [sessions] (the conclusion)
R7 gives each layer its own root permission. Sessions write narrower permissions into each layer they may write. R8 starts the base as a one-owner layer owned by the root actor, re-classed to shared when the first group arrives.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: R7 · Permissions reach a layer through its own gate · R8 · The base starts one-owner, owned by the root actor.

Next: Step 6 · first kept record → Step 7 · micro store.

### Thread 4: The first record

*What can never be undone?*

**18 Sep · The question behind the store work** [checked]
The store only adds records and never rewrites one, so some conventions freeze at the first record. Under one Rama, you asked for those first.

> what i want first. the conventions that would paint me in a corner
> — LEDGER.md:215

Sources: LEDGER.md:211-216 [checked]; CORNERS.md:12 [checked].

Connects to: Level 5.

**21 Sep · The corners** [checked]
Eight research voices produced CORNERS.md: the store’s conventions that no later fact can put right. You fenced the research files off: “I will not tell any session to read them.”

Sources: commit d0e7c1d9, 21 Sep [checked]; history/proposals/first-record-2026-09-20/README.md:4 [checked].

Connects to: Level 5.

**23 Sep · Your frame for a corner** [checked]
Time is the root. The log appends and time runs one way, so a fact written later about a moment is a claim, never a witness. A record’s position on four dimensions is set at write: grain, binding, source and uniformity. A fifth applies at the exit: exposure, because reads that happened cannot be unread.

Sources: PROGRESS.md:24-40 [checked].

Connects to: R5 · Reads go through a read gateway · Level 5.

**23–24 Sep · Store-level rulings, then the nine** [checked]
You ruled the store-level lines: meaning not bytes, one log, the envelope, acts of any size, named permissions, random names, two deletes, keys as ids. Then you ruled the nine that the rig’s architecture hangs from.

Sources: PROGRESS.md:42-107 [checked].

Connects to: Level 5 · The rig can start (The rig, 24 Sep).

**25 Sep · A convention the rig found** [checked]
The benchmarks found a convention nobody had asked about. With millisecond stamps, a task that decides more than 1,000 acts a second stamps ahead of real time. The stamp’s unit freezes at the first kept record.

> The stamp’s unit has to be settled before the first kept record.
> — rig RIG.md:224-225

Sources: rig RIG.md:221-225 [checked].

Connects to: R2 · Stamps are a hybrid logical clock.

**25 Sep · The rule behind the plan** [sessions] (the conclusion)
The first kept record is the only decision that can’t be undone. So everything that writes a record has to be complete from day one, and everything that only reads records can come later. Rule on things only when the next piece of work needs them.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Step 4 · edition one · Step 6 · first kept record · Map note 7.

Next: A1 · you rule R1–R8 → Step 4 · edition one → Step 6 · first kept record.

### Thread 5: Forgetting

*How is a value gone for everyone, the past included?*

**23–24 Sep · Forget destroys a lock** [checked]
There is one log, and erasure destroys a lock instead of keeping values beside the log (this reversed CORNERS.md’s C5). There are two deletes. Retract adds a new fact and can be undone. Forget destroys the lock: the value is gone for everyone, the past included, and time travel shows “erased on this date.” The older root is carry-on’s section on personal data and fine-grained access.

Sources: PROGRESS.md:45-47 [checked]; PROGRESS.md:60-62 [checked]; docs/carry-on.md:360-364 [checked].

Connects to: R1 · Forget is immediate.

**24 Sep · Where a value’s lock lives** [checked]
Every value gets its own small lock at write, wrapped under the locks of the people it is about. Lock rows go in a lock store for a person’s own layer and hand sessions. For agent sessions, groups and the base, they go in the record. A value about two people survives when one of them is forgotten, unless it was marked to die.

Sources: PROGRESS.md:91-102 [checked].

Connects to: R1 · Forget is immediate · Step 3 · locks.

**24–25 Sep · The model sharpened the lock rules** [checked]
A per-value lock is wrapped under the value’s own subjects. In a one-owner layer the owner’s lock is always required, and 7b applies among the other subjects. Alice’s mention of Bob in her own layer dies with Alice and survives Bob.

Sources: PROGRESS.md:138-145 [checked].

Connects to: R1 · Forget is immediate.

**25 Sep · The gap the phase 2 plan left open** [checked]
The phase 2 plan kept each value’s plaintext in the offer depot after a forget. It named the gap without closing it. Rama appends an offer before any gate code runs, so whatever the offer carries stays in the depot.

> The offer depot keeps each value’s plaintext after a forget, a gap the plan names and leaves open.
> — rig RIG.md:79-81

Sources: rig RIG.md:76-83 [checked].

Connects to: R1 · Forget is immediate.

**25 Sep · Lock store growth** [checked]
A lock row costs a fixed 169 bytes per value as raw bytes, whatever the value’s size, and grows linearly to 100,000 values. The assumed threshold is at most twice the value bytes. That is met for 200-byte values and exceeded for 40-byte values. The verdict waits on how big hand-session values usually are.

Sources: rig RIG.md:214-219 [checked]; rig STARTER-next.md, asks you for value sizes [checked].

Connects to: Map note 3 · R1 · Forget is immediate.

**25 Sep · R1: forget is immediate** [sessions] (the conclusion)
The door seals each value before appending it, and the lock reaches the gate by a path the depot never sees. The default path is a lease of locks minted by the gate. The alternative is an in-memory holder. The model decides between them.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: R1 · Forget is immediate.

Next: A2 · into the model → Step 3 · locks.

### Thread 6: What a read stood on

*What was seen, and as of when?*

**23–24 Sep · Reads that happened cannot be unread** [checked]
Exposure is the fifth dimension in your frame. Ruling 3 gave the read entry one format: pattern, moment, role, fingerprint, and a complete-or-partial mark. A model’s or a person’s reads are the exact list, because there is no re-run. Ruling 4 made the clock promises. Behind both, from July, is what you want from context: to verify it, not to hunt for it.

> I don't have to be hunter and gatherer of context only verifier
> — docs/carry-on.md:137

Sources: PROGRESS.md:34-35 [checked]; PROGRESS.md:75-86 [checked]; docs/carry-on.md:133-140 [checked].

Connects to: R3 · Read entries carry a frontier · R5 · Reads go through a read gateway.

**24–25 Sep · Order between the two stores** [checked]
Under the sharpenings sits your rule: order between the two stores exists only through stood-on. A check one gate makes against the other store is a read. It is stamped in the checking store and is never atomic with the other store’s writes.

Sources: PROGRESS.md:162-165 [checked].

Connects to: R4 · Based-on is what the act stood on.

**25 Sep · What Rama showed about reads** [checked]
A microbatch becomes visible one task at a time, so a reader can see a batch on one task and not yet on another. A gate’s read of the other store returns what that store holds at attempt time, never what the offer stood on.

Sources: rig RIG.md:100 [checked]; rama-check RESULTS.md:75-90 [checked]; rama-check RESULTS.md:102-109 [checked].

Connects to: R3 · Read entries carry a frontier · R4 · Based-on is what the act stood on.

**25 Sep · Stamps ran ahead of the clock** [checked]
One agent layer on one task handled 2,234 acts a second with 16 writers, at a p99 of 11.75 ms and 37% of a core. Each decision stamps at least one past the last, so the task’s stamps ran 20.7 s ahead of the wall clock.

Sources: rig BENCH_NOTES-stream.md:44-56 [checked].

Connects to: R2 · Stamps are a hybrid logical clock.

**25 Sep · Five rulings about reads** [sessions] (the conclusion)
R2 makes stamps a hybrid logical clock. R3 puts a frontier on read entries. R4 records based-on as stood-on ids with their stamps. R5 adds a read gateway that records before it answers. R6 covers standing reads.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: R2 · Stamps are a hybrid logical clock · R3 · Read entries carry a frontier · R4 · Based-on is what the act stood on · R5 · Reads go through a read gateway · R6 · Standing reads.

Next: B1 · phase 5: reads → Step 4 · edition one.

### Thread 7: The model

*Do the rules hold together?*

**23 Sep · Think through every scenario** [sessions]
Your hypothesis for the corners: starting from time as the root, the scenarios might be thought through exhaustively.

> My hypothesis is thinking about this from this pov we might be able to exhaustively think about all the different scenarios
> — chat, 23 Sep, as quoted in the history account

Sources: history account [sessions].

Connects to: Level 6.

**24 Sep · The model, round one** [checked]
A fresh session built an executable model of the rulings. It found six places where they were silent or pulled against a property. That evening you ruled the six, the owner-required line and the stood-on rule.

Sources: PROGRESS.md:215-217 [checked]; commit cbb61196, 24 Sep [checked].

Connects to: Level 6.

**25 Sep · The model, round two** [checked]
Round two made five changes, A to E, and proposed a name tag to make E hold. You ruled the tag yours and left a read’s moment open. The baseline holds under three seeds at 30,000 histories per check, except x2, which was never promised. Every reading flipped back on its own brings back its own failure.

Sources: PROGRESS.md:218-222 [checked]; formal-model README.md:162-193 [checked]; commit c1d46d42 [checked].

Connects to: R7 · Permissions reach a layer through its own gate.

**25 Sep · The model becomes the gate** [sessions] (the conclusion)
Each new ruling goes into the model first. The model decides between lease and holder, the two lock-path cases, and a layer on the stream gate with its locks in the record. P6 widens to “nothing retained opens or confirms a forgotten value.”

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: A2 · into the model · Map note 1.

Next: A2 · into the model → A3 · round three → Step 7 · micro store.

### Thread 8: The rig

*Does Rama carry the rules?*

**24 Sep · Throwaway records need only the nine** [checked]
This cut made a rig possible. Records nobody keeps need only the rulings the rig’s architecture hangs from, and those were the nine.

> The nine, ruled 24 September (evening). The rig can start.
> — PROGRESS.md:67

Sources: PROGRESS.md:67 [checked].

Connects to: Level 6.

**25 Sep, 00:35–01:00 · Running Rama itself** [checked]
Two probes ran on an in-process cluster: rama-check and the rig’s phase 0. An ordinary exception kills the worker and replays records already acknowledged, so refusals must be data. A client can get an error for an offer that landed. A slow event is retried while it is still running, so gate work must stay well under the 5 s stream timeout. “Visible implies replicated” is a docs promise that no run has tested.

Sources: rama-check RESULTS.md:26-68 [checked]; rama-check RESULTS.md:111-133 [checked]; rig RIG.md:103-120 [checked].

Connects to: Step 5 · failover run.

**25 Sep, 09:30 · The stream store passes** [checked]
The first run stopped inside phase 1 at 03:05, when a safety classifier stopped the writing of the test suite. A partial gate_test.clj is still on disk, and it is yours to decide on. The second run, with Fable writing plans and Opus building, took phase 1 to green. At 08:52 something rewrote gate.clj, and the cause is not verified.

Sources: rig RIG.md:13-66 [checked].

Connects to: Step 0 · hygiene.

**25 Sep, noon · The numbers, on thin slices** [checked]
After you asked “so what next,” two sessions measured the three numbers on the stream store as built. Each small act makes 4 index writes. One layer’s task passed 5,000 acts a second with 128 writers and was still rising. One act at a time takes 3.3 ms typically and 4.7 ms at p99. A lock row costs 169 bytes per value.

Sources: rig RIG.md:205-231 [checked]; rig README.md:64-74 [checked].

Connects to: Who decides: You, now · Map note 3.

**25 Sep · The rig changes role** [sessions] (the conclusion)
The rig becomes the first-edition build. Its code is kept and its records are not, and every phase follows the full build discipline. Its briefed order is not run as written.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Your phases and the proposed order · Map note 1.

Next: Step 0 · hygiene → B1 · phase 5: reads → Step 1 · tools on the rig → Step 3 · locks → Step 5 · failover run.

## 3. Your phases, and the order the sessions propose

**Superseded (evening of 25 September).** This section is the conclusion's order as proposed. The order in force is PROGRESS.md "Next". The differences that matter: the rig is a candidate, not the first-edition build; grammars at the gate and a minimal runner come before any count; the count gets a blind test with a held-back tool written from round three's one-page contract; round three is entered through the tool's trace, not through the eight rulings.

On 25 September you briefed the rig in nine phases, 0 to 8 (rig SPEC.md:56-111, your words). The conclusion keeps most of them, changes their order, and adds steps your brief didn't have.

**Not yet**, says the conclusion: don't build the real store as a separate project; don't migrate the server; don't run the rig in its briefed order; don't write edition one yet.

### Your phases (briefed 25 September), and where each went

**Phase 0: The two Rama claims** (done)

Stream events are atomic on one task and processed at least once. A microbatch is decided at prepare, made visible at commit, and atomic across tasks. One stronger reading failed: a batch is not visible on every task at one instant.

Sources: rig RIG.md:85-120 [checked].

**Phase 1: The stream store** (done, green)

> A depot for offers, a stream gate topology, the envelope as ruled … Tests: the model’s stream cases.
> — rig SPEC.md, your words, 25 Sep

13 tests and 728 assertions pass, three injected crashes included.

Sources: rig SPEC.md:56-63 [checked]; rig RIG.md:19-42 [checked].

**Phase 2: Locks and forgetting** (plan written, not validated) → Step 3 · locks

> Real encryption, kept simple: a small symmetric lock per value at write, wrapped under person locks as ruled …
> — rig SPEC.md, your words, 25 Sep

Moved after phases 5 and 6. Grammars come first, because ruling 8 derives a value’s subjects from its grammar. R1 changes it too: the door seals values before they reach the depot.

Sources: rig SPEC.md:65-72 [checked]; PROGRESS.md:103-104 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

**Phase 3: The micro store** (plan written, not validated) → Step 7 · micro store

> A microbatch gate for group and base layers placed by entity, atomic across tasks …
> — rig SPEC.md, your words, 25 Sep

Moved after the first kept record. That record is in one-owner layers only, and under R8 the base stays one-owner until the first group.

Sources: rig SPEC.md:74-80 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

**Phase 4: Promotion** (not started) → Step 7 · micro store

> A request act in the owner’s layer; the read-out opens the value through its lock on the owner’s task …
> — rig SPEC.md, your words, 25 Sep

Moved with the micro store. A promotion crosses into a shared layer, so it waits for step 7.

Sources: rig SPEC.md:82-87 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

**Phase 5: Reads and read entries** (not started) → B1 · phase 5: reads

> Point reads and pattern reads; one read entry line per pattern read …
> — rig SPEC.md, your words, 25 Sep

Moved to first, as lane B, and widened: the read gateway (R5), frontier entries (R3), standing reads (R6), and indexes that can be purged or rebuilt (R1). Tools come after it, because matching is a pattern read.

Sources: rig SPEC.md:89-93 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

**Phase 6: Tools and grammars from inside, then the count** (not started) → B2 · grammars, Step 1 · tools on the rig, Step 2 · tool in src-inland

> Add one new tool and one new grammar by writing facts only … Zero is the target.
> — rig SPEC.md, your words, 25 Sep

Split in three. Grammars at the gate come as soon as round three fixes their shape. Tools and the runner are step 1. The same tool in src-inland is step 2, which gives the real count.

Sources: rig SPEC.md:95-101 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

**Phase 7: The three numbers** (measured on thin slices) → Who decides: You, now

> Index writes per second at the agent rate; lock store growth under hand layers; one person’s layer on one thread.
> — rig SPEC.md, your words, 25 Sep

Done ahead of the other phases. The thresholds are still the main session’s assumptions, for you to set.

Sources: rig SPEC.md:103-107 [checked]; rig RIG.md:205-231 [checked].

**Phase 8: Replay the model’s histories** (not started) → Step 7 · micro store

> Replay the model’s fixed histories from scenarios.clj through the rig and compare …
> — rig SPEC.md, your words, 25 Sep

Becomes a standing practice at every step. The rig’s tests already ask the model for its answer to the same history.

Sources: rig SPEC.md:109-111 [checked]; rig RIG.md:250-252 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

### The proposed order

#### First

**Step 0 · hygiene: Hygiene** [sessions]
One git worktree per concurrent session, since the benchmark work was writing into the rig worktree. Your decision on the partial gate_test.clj. A trace of what rewrote gate.clj on the 25th. The rig gets its new label: the first-edition build, with its code kept and its records not.

Why here: RIG.md notes that two plan sessions were running in the rig worktree when gate.clj was rewritten. The cause is not verified.

Threads: The rig.

Sources: rig RIG.md:52-57 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

#### Lane A: with you (the rulings and the model)

**A1 · you rule R1–R8: You rule on R1 to R8** [sessions]
Keep or veto each of the eight defaults.

Why here: They decide what a kept record contains. Lane B builds on R2 to R6.

Threads: The first record, What a read stood on, Forgetting, Made of layers.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: The eight rulings · Map note 1.

**A2 · into the model: Each kept ruling into the model** [sessions]
The model decides between lease and holder, the two lock-path cases, and a layer on the stream gate with its locks in the record. P6 widens.

Threads: The model, Forgetting.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: R1 · Forget is immediate · R8 · The base starts one-owner, owned by the root actor.

**A3 · round three: Round three: the seed** [sessions]
The seed, designed against the 13 September tool. The grammar-fact shape comes first.

Threads: Built from inside, The model, Made of layers.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Seed: Designed against a real tool · Seed: What the seed contains.

#### Lane B: without you (the rig builds)

**B1 · phase 5: reads: Phase 5: reads** [sessions]
The read gateway, frontier entries, standing reads, and value indexes that can be purged or rebuilt. Values stay unsealed until phase 2, so the “open” step passes values through unchanged. Read roles are placeholders. Index kinds take the grammar’s index hints as parameters, so grammars plug in later without rework. Afterwards, measure the cost of reads being writes.

Why here: Tools need pattern reads, and R2 to R6 define what a read records.

Threads: What a read stood on, The rig, Made of layers.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: R3 · Read entries carry a frontier · R5 · Reads go through a read gateway · R6 · Standing reads · Who decides: Phase 5 will surface.

**B2 · grammars: Grammars at the gate** [sessions]
As soon as round three fixes the grammar shape.

Why here: They come before locks, because ruling 8 derives a value’s subjects from its grammar.

Threads: Built from inside, Forgetting.

Sources: PROGRESS.md:103-104 [checked].

Connects to: Step 3 · locks.

#### Where the lanes join

**Step 1 · tools on the rig: Phase 6: tools and a minimal runner** [sessions]
A tool is facts: what it matches, its signature, its read-entry preference and the permission it acts under. A minimal runner finds tools by matching. The count runs first on the rig.

Comes after: B1, because matching is a pattern read; and A3, for the tool and grammar shapes.

Threads: Built from inside, The rig.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: The thesis test (Built from inside, 25 Sep) · Map note 6.

**Step 2 · tool in src-inland: The same tool in src-inland** [sessions]
The tool runs in src-inland through the operation set (the door’s API), with records not kept. This gives the real count, and it is your first use.

Needs: Your merge decision on the rig branch.

Threads: Built from inside, One truth.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Repo material as facts (Built from inside, 13 Sep) · Map note 5 · Server verdict (One truth, 25 Sep).

**Step 3 · locks: Phase 2: the lock store and door sealing** [sessions]
Lease or holder, as the model decides. Lock-store growth is measured alongside.

Comes after: B2, because grammars give a value its subjects.

Threads: Forgetting, The rig.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: R1 · Forget is immediate · 169 bytes per value (Forgetting, 25 Sep).

**Step 4 · edition one: Edition one, written from the model** [sessions]
The operations come first: offer and answer, read, retract, forget, promote, revoke, re-class, and open and close for layers and sessions. Then the envelope, the refusal codes, the read-entry format and the seed constants.

Needs: It can’t be sealed until these close: R1 to R8, 56, 59, 41 with the door, 17 and 60, 85, and the open gate rules.

Threads: The first record, The model.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Who decides: Edition one must close.

**Step 5 · failover run: A two-node failover run** [sessions]
“Visible implies replicated” is so far only a docs promise.

Needs: Check first that the cluster and licence allow two nodes.

Threads: The rig.

Sources: rama-check RESULTS.md:111-133 [checked].

Connects to: Rama probed (The rig, 25 Sep, 00:35–01:00).

**Step 6 · first kept record: The first kept record** [sessions]
In one-owner layers only. This is the step that can’t be undone.

Threads: The first record, Made of layers.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: The one irreversible step (The first record, 25 Sep) · R8 · The base starts one-owner, owned by the root actor · Map note 4.

**Step 7 · micro store: The micro store, promotion, the base’s re-class** [sessions]
The micro store with its frontier, promotion, and the base’s re-class, before the first group or the second person. Replaying the model’s histories through the rig stays a standing practice.

Threads: Made of layers, The model.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: R8 · The base starts one-owner, owned by the root actor · Map note 4.

**Step 8 · one truth: One truth** [sessions]
src-inland’s Rama and the server’s object container move onto the store core. The adapters and page are re-pointed, the side logs go, and existing material comes back in as ingest.

Threads: One truth.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: One truth, ruled (One truth, 18 Sep) · Map note 2.

**After step 8 · Softland changes Softland: Softland changing Softland** [my reading]
Not a step in the plan. Once Inland runs on the store core, the loop from 1 and 7 September can close: use Softland to change its own codebase. The codebase-as-a-city idea from 13 September also waits here.

Threads: Built from inside.

Sources: the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Build Softland in Softland (Built from inside, 1 Sep and 7 Sep) · Map note 5.

## 4. The eight rulings

These eight defaults decide what a kept record contains. Each is yours to keep or veto, and each goes into the formal model first. [sessions] unless a line says otherwise.

### R1: Forget is immediate

A forgotten value is gone at once. To make that possible, the door seals each value before it is appended, so the depot never holds anything that could open a value.

**Why now.** The phase 2 plan left each value’s plaintext in the offer depot after a forget (RIG.md:79-81). Rama appends an offer before any gate code runs.

**Builds on.** One log, erasure by destroying a lock (PROGRESS.md:45-47). Forget: gone for everyone including the past (PROGRESS.md:60-62).

**Needed by.** B1 · phase 5: reads · A2 · into the model · Step 3 · locks.

**In detail.**
- “One log” means the gate’s per-task log, in your phase 1 words (rig SPEC.md:59-60). The depot is only where offers enter.
- Default path, the lease: in a lease act the gate mints a batch of locks, wrapped under the session owner’s lock. The door seals each value under a leased lock, and the offer cites its id. At decision the gate re-wraps it under the value’s actual subjects. Without that re-wrap, a value marked to die with any of its subjects could still be opened by the owner alone. It costs one gate act per batch, with nothing unreplicated in the decision path.
- Alternative path, the holder: a query hands the lock to the task’s in-memory holder before the append. It costs one round trip per offer, and the holder is empty after a worker restart (rig PLAN-locks-and-forgetting.md:739-752).
- Under either path, a missing lock is refused on the offer’s face with nothing recorded, so a resend under the same name is still allowed.
- Riders: the reuse digest is an HMAC over the plaintext, keyed by the value’s lock, so a re-sealed resend still compares. After a forget, a same-name offer is answered from the record without a content check. A reused name with different content then can’t be told from a retry, which is the price of forgetting. Read-entry fingerprints cover fact ids, not values. Every index can be purged by value id or rebuilt from the record, and both tools must be built, because Rama’s PStates don’t replay the depot.
- Declined: a horizon, where forgotten values persist for a while. It weakens “gone for everyone including the past” and would need period keys in edition one.

Threads: Forgetting, The first record, The rig.

Sources: rig RIG.md:76-83 [checked]; PROGRESS.md:45-47, 60-62 [checked]; rig PLAN-locks-and-forgetting.md:739-752 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Plaintext left in the depot (Forgetting, 25 Sep) · 169 bytes per value (Forgetting, 25 Sep) · Map note 3.

### R2: Stamps are a hybrid logical clock

A stamp is wall-clock milliseconds plus a counter. It never goes backward, compares across units and stores, stays within clock skew of real time, and doubles as the read position.

**Why now.** At 2,234 acts a second on one task, millisecond stamps ran 20.7 s ahead of the wall clock. The stamp’s unit freezes at the first kept record.

**Builds on.** Ruling 4: never backward within a unit, never earlier than anything the fact stood on (PROGRESS.md:85-86). The model’s P5.

**Needed by.** B1 · phase 5: reads · Step 4 · edition one.

Threads: What a read stood on, The first record, The rig.

Sources: rig BENCH_NOTES-stream.md:44-56 [checked]; rig RIG.md:221-225 [checked]; PROGRESS.md:85-86 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Stamps 20.7 s ahead (What a read stood on, 25 Sep) · Stamp unit freezes (The first record, 25 Sep).

### R3: Read entries carry a frontier

A read entry records “as of when”. For a one-owner layer that is its stamp, inline. For a shared layer it is the id of its settled frontier. “As of T” stays one number.

**Why now.** A microbatch becomes visible one task at a time, so shared layers must be read through a settled frontier (rig choice R5).

**Builds on.** Ruling 3’s read entry: pattern, moment, role, fingerprint (PROGRESS.md:75-84). The open x2: a read’s moment across both stores (PROGRESS.md:172-177).

**Needed by.** B1 · phase 5: reads.

**In detail.**
- Closing a single moment across both stores stays an option for edition two.

Threads: What a read stood on, Made of layers.

Sources: rig RIG.md:100, 136-137 [checked]; rama-check RESULTS.md:75-90 [checked]; PROGRESS.md:75-84, 172-177 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Rama: reads at a moment (What a read stood on, 25 Sep) · Who decides: Edition two.

### R4: Based-on is what the act stood on

Every act lists the entries it stood on: fact ids together with their stamps.

**Why now.** A gate’s read of the other store returns only what it holds now, never what the offer stood on, so the offer has to carry those stamps.

**Builds on.** Your rule that order between the two stores exists only through stood-on (PROGRESS.md:162-165). The rig’s pick P9 already carries stamps.

**Needed by.** B1 · phase 5: reads · Step 1 · tools on the rig.

Threads: What a read stood on, The model.

Sources: rama-check RESULTS.md:102-109 [checked]; PROGRESS.md:162-165 [checked]; rig RIG.md:187-188 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Order only via stood-on (What a read stood on, 24–25 Sep) · Rama: reads at a moment (What a read stood on, 25 Sep).

### R5: Reads go through a read gateway

The gateway runs on the server. It queries, appends the read entry, then answers, so nothing is shown before its entry is acknowledged. Agent-session reads are recorded there too, and kept or dropped when the session closes.

**Why now.** What an agent stood on can be rebuilt from based-on. What it was shown, matched or passed through is lost unless it is recorded.

**Builds on.** Partly reopens ruling 3’s “agent session layers may default to none” (PROGRESS.md:81-83). Your frame’s exposure: reads that happened cannot be unread (PROGRESS.md:34-35).

**Needed by.** B1 · phase 5: reads.

**In detail.**
- My reading: this is the machinery behind “I don't have to be hunter and gatherer of context only verifier” (carry-on.md:137). To verify an agent’s work, you need what it was shown.
- The cost: reads become writes. The conclusion measures that after phase 5.

Threads: What a read stood on.

Sources: PROGRESS.md:81-83 [checked]; PROGRESS.md:34-35 [checked]; docs/carry-on.md:137 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Exposure; read entries (What a read stood on, 23–24 Sep) · R6 · Standing reads.

### R6: Standing reads

A read held open gets one entry, opened with its pattern, role and frontier. Each delivery that shows something new adds a stamped line with the delivered fact ids. A delivery with nothing new adds nothing. The entry closes at unsubscribe or session close.

**Why now.** src-inland’s renderers hold reads open, and no ruling covered that.

**Builds on.** Ruling 3. Deliveries are recorded at the rate the person is shown things (LEDGER.md:158); for an agent, at its model-call rate.

**Needed by.** B1 · phase 5: reads · Step 2 · tool in src-inland.

**In detail.**
- Delivered ids are exact for a person or a model, and a fingerprint for a deterministic tool.
- The entry closes with a fingerprint over everything delivered and a mark saying whether it is complete or partial. An entry left open by a crash is closed when the session closes.
- Phase 5 will surface what a standing read means for the “no re-run” rule that applies to models.

Threads: What a read stood on, Built from inside.

Sources: LEDGER.md:158 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Who decides: Phase 5 will surface · Step 2 · tool in src-inland.

### R7: Permissions reach a layer through its own gate

Each layer has a root permission, created with the layer. Opening a session writes a narrower permission into each layer the session may write, through that layer’s gate. Agents and tools get narrower ones beneath. Revoking a permission cuts everything below it.

**Why now.** Change D: a permission lives in the layer it governs, and a gate checks only permissions in layers it orders. In the model’s trace 14, a gate that checks a permission kept in the other store misses a revocation.

**Builds on.** Re-reads “the session is the root” (PROGRESS.md:56-57): the session becomes the root of what it may do in each layer, not one root over everything.

**Needed by.** A3 · round three · Step 1 · tools on the rig.

Threads: Made of layers, The model.

Sources: PROGRESS.md:56-57, 159-161 [checked]; formal-model README.md, smallest failing history 14 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: Round two: A to E (The model, 25 Sep) · Seed: What the seed contains.

### R8: The base starts one-owner, owned by the root actor

Until the first group arrives, the base runs on the stream gate like a one-owner layer, owned by the root actor rather than a person. At the first group it is re-classed to shared.

**Why now.** The seed needs the base’s class and owner, and the first kept record can then happen in one-owner layers only.

**Builds on.** Ruling 9: the base is open to any authenticated actor, and seed policy is facts (PROGRESS.md:105-107). The 18 September ruling: “a shared thing that sits below them.”

**Needed by.** A3 · round three · Step 6 · first kept record.

**In detail.**
- Its lock rules follow its kind from day one: shared-layer rules, 7b as written, locks kept in the record. The root actor has no person lock, so no owner-required wrap applies. Only the gate and placement follow its class, so the later re-class changes nothing about locks.
- First facts stay facts (ruling 9), and their ids are constants (item 59’s default). The earlier proposal to make the seed edition constants contradicted ruling 9 and was withdrawn.
- My reading: R8 is what lets the micro store and promotion wait until step 7.

Threads: Made of layers, One truth, The first record.

Sources: PROGRESS.md:105-107 [checked]; LEDGER.md:201 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

Connects to: One truth, ruled (One truth, 18 Sep) · Map note 4 · Step 7 · micro store.

**Also into the model.** P6 widens to "nothing retained opens or confirms a forgotten value". The depot, the lease rows or holder, and the lock store become separate places the model checks. The new cases are both lock paths, and a layer on the stream gate with its locks in the record (the base under R8).

## 5. Round three: the seed

The seed is the set of facts a store starts with. Round three of the model designs it against one real tool: the one the integration starter named on 13 September.

### What the seed contains [sessions]

- The vocabulary of built-in steps.
- The tool fact: what it matches, its signature, its read-entry preference, and the permission it acts under.
- The grammar fact: value shape, subjects, the opaque flag, index hints.
- Read roles, root permissions, default visibility, and constant ids.
- It closes open items 3, 59, 85 and 44.

Sources: the three sessions’ conclusion (25 Sep) [sessions]; PROGRESS.md:181-186, the open items [checked].

### Designed against a real tool [checked]

- The tool is a reference to a passage or a function at a revision. When the material changes, it shows the result or shows that it is stale.
- It exercises grammar facts, tool facts, read entries, based-on, the stale default, and what a new tool owes the history before it.
- It comes from the integration starter you settled on 13 September. The grammar-fact shape comes first.

Sources: docs/builds/inland/integration.md:41-45 [checked]; commit 898bfc5b, 13 Sep [checked].

### Candidate inputs from the server [sessions]

- The capability set and actor types (envelope.clj:55-56, 81-86).
- Relation kinds (relation_kernel.clj:49-56).
- Activation and ground kinds (worn/activation_event.cljc:19-47).
- The receipts that already record what was visible (episode/material_circulation.clj:137-142).
- These are candidates, not authority.

Sources: the three sessions’ conclusion (25 Sep); lines not re-read [sessions].

### The count [sessions]

- Zero compiled steps is the target, not a pass-or-fail gate. Your phase 6 brief asks for the count and every step with its reason.
- Protocol steps change the gate, the envelope, the read gateway or an index kind.
- Capability steps add a new built-in function that a recipe calls.
- A staleness index is the likely first honest protocol step.

Sources: rig SPEC.md:95-101 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

## 6. Who decides what, and when

**Superseded (evening of 25 September).** "You, now" below is the conclusion's list. After the evening's settle: nothing is ruled before the seed round; R1 to R8 come to you inside the tool's trace, at the step that needs each; the operator question on forgetting (may the operator read a forgotten value's plaintext from the depot?) is due before phase 2; the thresholds, hand-session value sizes and gate_test.clj wait until something asks for them. See PROGRESS.md "Next" and "Open while the rig runs".

### You, now

- Keep or veto each of R1 to R8. [sessions]
- The rig’s new label. [sessions]
- Thresholds for the three numbers. Assumed now: at least 1,000 acts a second per task at the agent rate; for one person, at least 100 acts a second at 20 ms or less at p99; a lock store at most 2× the value bytes. [checked]
- How big hand-session values usually are. This decides whether a lock row per value is an affordable default. The rig’s starter asks for it; the conclusion doesn’t. [checked]
- The partial gate_test.clj. [checked]

Sources: rig RIG.md:281-285 [checked]; rig STARTER-next.md [checked]; the three sessions’ conclusion (25 Sep) [sessions].

### You, before a step

- Before step 2: the merge decision on the rig branch. [sessions]
- Before step 5: whether the cluster and licence allow two nodes. [sessions]

Sources: the three sessions’ conclusion (25 Sep) [sessions].

### The model decides

- Lease or holder. [sessions]
- The two lock-path cases. [sessions]
- The layer on the stream gate with its locks in the record. [sessions]

Sources: the three sessions’ conclusion (25 Sep) [sessions].

### Round three decides

- The seed’s contents and shapes. [sessions]
- What a new tool owes history (85). [sessions]
- Whether a staleness index counts as a protocol step. [sessions]

Sources: the three sessions’ conclusion (25 Sep) [sessions].

### Phase 5 will surface

- How a read composes layers. The rule since 17 September is that the nearest active layer wins, found by walking builds-on. It needs confirming. [checked]
- What a standing read means for the “no re-run” rule that applies to models. [sessions]

Sources: PARTS-4-8.md:146-153 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

### Edition one must close

- 56: the record’s written form and fingerprint. [checked]
- 59: seed ids. [checked]
- 41: opaque actor ids with one erasable link to the person, plus the minimum door. [checked]
- 17 and 60: ingest identity. [checked]
- 85: what a new tool owes history. [checked]
- The open gate rules: writes about someone already forgotten, and forgetting your own values after your write permission is revoked. [checked]
- Can follow later without touching any record: 44 the stale default, 84 repair lockout, 82 the rest of session close. [sessions]

Sources: PROGRESS.md:172-186 [checked]; the three sessions’ conclusion (25 Sep) [sessions].

### Edition two

- Closing a single moment across both stores (the model’s x2). [checked]

Sources: PROGRESS.md:172-177 [checked]; formal-model README.md:285 [checked].

### When you say settle

- Nothing has been written yet. [sessions]
- PROGRESS.md: Now gets the rulings you keep, Next gets the order, and one Log line dated 2026-09-25 names the three sessions. [sessions]
- RIG.md: one status line (first-edition build; code kept, records not). [sessions]
- Optionally, one server map under `src/proposal/<work>/`: 33 points in model-seed-specification, 27 in partition-seam-architecture. [sessions]

Sources: the three sessions’ conclusion (25 Sep) [sessions].

## 7. What the map shows

Places where laying the plan over the history shows a join, a gap or a pull. These are the map session's reading, for you to weigh. None of them is a ruling.

### 1. Lane B builds on R2 to R6 before the model has them [my reading; resolved that evening]

As first written: the conclusion says each ruling goes into the formal model first, and also that the rig builds the read gateway in the meantime. Phase 5 is R3, R5 and R6 made concrete, and the rig’s code is now kept. So your keep-or-veto on R2 to R6 is what actually unblocks lane B, and the model check runs beside the build rather than before it. The rig’s tests can ask the model for its answer to the same history (RIG.md:250-252), which narrows the risk.

Resolved (evening): lane B builds only the read exit the count needs, with R2 to R6 unruled. Anything a kept record would carry (bytes, a stamp, an id, what an entry holds) stops and asks; only what can change later without touching a record is picked as a rig choice. Standing reads and the frontier are not built; round three rules them in its trace.

Connects to: A1 · you rule R1–R8 · B1 · phase 5: reads · The test every build passes (The model, 25 Sep).

### 2. The 12 September question got a different answer than it asked for [checked]

You asked why Inland wasn’t building on top of the server. The conclusion’s answer is that neither one is the store. The core is new, the server contributes its admission pattern, adapters and registries as seed input, and at step 8 both the server and src-inland move onto the core. That fits the 18 September one-truth ruling. A read-only check confirmed the facts behind the verdict. An omitted actor defaults to “sid” (page/matter_room.cljc:66-70, used at :226). The capability check compares the capabilities the caller itself supplied, and its docstring says so (rama/envelope.clj:367-377). The only ownership rule is that only the original asserter can retract a relation (rama/relation_kernel.clj:490-497). A bounded look found no authentication step on the request path (door/server_jetty.clj:1060-1066). One claim holds only partly: visibility has no reader anywhere, but envelope.clj does check branch ids for shape.

Connects to: Why not build on top? (One truth, 12–13 Sep) · Server verdict (One truth, 25 Sep) · Step 8 · one truth.

### 3. The conclusion is a step behind the rig on the numbers [checked]

All three numbers are measured and committed on the rig branch (RIG.md:205-219, STARTER-next.md). The conclusion lists one person’s layer as still under way and doesn’t carry the lock-growth result. That result matters. A fixed 169 bytes per value is within the 2× threshold for 200-byte values and over it for 40-byte ones. So the verdict needs your answer on how big hand-session values usually are, and it bears on ruling 7’s lock grain and on R1.

Connects to: 169 bytes per value (Forgetting, 25 Sep) · Who decides: You, now · R1 · Forget is immediate.

### 4. R8 carries the reorder [my reading]

Phases 3 and 4 (the micro store and promotion) can move after the first kept record only because the base starts as a one-owner layer. If you veto R8, the first kept record needs the micro store first, and step 7 moves ahead of step 6.

Connects to: R8 · The base starts one-owner, owned by the root actor · Step 6 · first kept record · Step 7 · micro store.

### 5. Where your hypothesis comes back [my reading]

The plan’s eight steps end at one truth. Using Softland to change Softland, the line from 1 and 7 September, is not a step. Its first brick is the round-three tool, a reference to a passage or function at a revision, run in src-inland at step 2. The codebase-as-a-city idea from 13 September stays parked: set aside, not dropped.

Connects to: Build Softland in Softland (Built from inside, 1 Sep and 7 Sep) · Step 2 · tool in src-inland · After step 8 · Softland changes Softland.

### 6. The count’s two classes echo 6 September [my reading]

On 6 September you ruled that tools live as records over a vocabulary and capabilities remain code (carry-on §6, “Records and capabilities”). The count sorts each compiled step into protocol or capability. Under that ruling, capability steps were always expected. The number that tests the hypothesis is the protocol steps, and a staleness index is the likely first one.

Connects to: Seed: The count · Step 1 · tools on the rig · Inland: the floor stayed (Built from inside, 11 Sep).

### 7. Asking for all eight now fits “rule only when the next work needs it” [my reading; wrong, corrected that evening]

As first written: R2 to R6 and R1’s riders are needed by phase 5, which lane B starts now. R7 and R8 are needed by round three, lane A’s next step. R1’s core is needed by the model now and by step 3. So each of the eight is needed by work that starts right away.

Correction: that read "needed by" as "touched by". Checked against the rulings and the rig: R4 is already the rig's pick P9 under your stood-on rule (RIG.md:187-188), a confirmation, not a ruling. R3's frontier is written only for a shared layer, and under R8 none exists before step 7; the envelope's version marker lets the part arrive later. R6 has no design yet; the round-three tool draws it, and a ruling before the drawing is a ruling on nothing. R5b reverses your ruling 3 ("agent session layers may default to none", PROGRESS.md:81-83) and is a layer setting to build both ways and measure. R2 is one line in the rig's `stamp-for` (gate.clj:186-193). What the next work needs now is the seed round, and it pulls each question at the step of the tool's trace where it bites. The count is only honest against a runtime that already reads grammars and runs tools, so those come before it.

Connects to: A1 · you rule R1–R8 · B1 · phase 5: reads · A3 · round three.

## 8. Words

The plan's vocabulary, in plain words. "Key" is a fact's key; "lock" is an encryption key (fixed 24 September).

- **act**: One offer of one or more facts, admitted whole or not at all. Who, when and reads are recorded once on the act. (PROGRESS.md:53-55)
- **offer and answer**: An offer is an act sent to a gate. The answer is the gate’s recorded decision, found again by the act’s name. (PROGRESS.md:129-135)
- **gate**: The code that decides each offer: admit, refuse or stamp. A stream gate serves one-owner layers and a microbatch gate serves shared ones. (ruling 1)
- **depot**: Rama’s append-only intake. An offer lands here before any gate code runs. (R1)
- **door**: The operation set a client uses to make offers. Under R1 it seals values before they are appended. (conclusion)
- **envelope**: A small positional core on each fact, plus named parts the store owns. Tools add facts, never envelope parts. (PROGRESS.md:48-52)
- **layer**: A thin list of facts that a reader sees through. The four kinds are base, group, personal and session (hand or agent). (PROGRESS.md:109-112)
- **one-owner and shared layers**: A one-owner layer belongs to one person or to the root actor, and one task orders it. A shared layer belongs to a group or to everyone, and the microbatch gate orders it. (rulings 1 and 2)
- **placement**: Which task a fact lives on: by layer for one-owner layers, by entity for shared ones. (ruling 2)
- **re-class**: Moving a layer from one-owner to shared, which moves it from the stream gate to the microbatch gate. (PROGRESS.md:136-137)
- **promotion**: Moving a value from a personal layer into a group layer or the base. It takes two acts, one at each gate. (PROGRESS.md:150-158)
- **stamp**: A fact’s position in time. R2 makes it a hybrid logical clock: wall-clock milliseconds plus a counter. (R2)
- **frontier**: The point up to which a shared layer is settled on every task. Shared layers are read through it, so no reader sees half a batch. (R3; rig choice R5)
- **read entry**: The record of a read: pattern, moment, role, a fingerprint or the exact list, and a complete-or-partial mark. (ruling 3)
- **standing read**: A read held open, delivering new matches as they arrive, like a renderer’s live view. (R6)
- **based-on, stood-on**: The facts an act rested on, recorded as ids with their stamps. (R4)
- **key and lock**: A key is a fact’s key, the “in this respect” part. A lock is an encryption key. Never one word for both. (PROGRESS.md:18-19)
- **lock store**: Rows holding each value’s wrapped lock, for a person’s own layer and hand sessions. Other layers keep their locks in the record. (ruling 7)
- **forget and retract**: Retract adds a new fact and can be undone. Forget destroys the lock, so the value is gone for everyone, the past included. (PROGRESS.md:60-62)
- **lease and holder**: Two ways for the gate to get a value’s lock without it passing through the depot: a batch of locks minted ahead of time, or an in-memory handoff. (R1)
- **grammar**: Facts about a key: its value shape, its subjects, whether its values are opaque, and its index hints. (seed)
- **tool fact**: Facts about a tool: what it matches, its signature, its read-entry preference, and the permission it acts under. (seed)
- **seed**: The first facts a store starts with. Round three of the model designs it. (seed)
- **edition**: A fixed version of the record’s written form and the rules. Edition one is written from the model before the first kept record. (conclusion)
- **kept record**: A record nobody throws away. The rig’s records are not kept. The first kept record is the one step that can’t be undone. (conclusion)
- **the count**: How many new compiled steps adding one tool and one grammar needed. Zero is the target. (rig SPEC.md:95-101)
- **protocol and capability steps**: The count’s two classes. A protocol step changes the gate, the envelope, the read gateway or an index kind. A capability step adds a new built-in function that a recipe calls. (conclusion)
- **one truth**: One store for everyone: “a singular rama deployed throughout the world” (18 Sep). In the plan, step 8 moves src-inland and the server onto the store core. (LEDGER.md:201)
- **the model**: The executable formal model of the rulings. It runs random histories against the properties P1 to P8, rv, and x1 to x3. (formal-model README.md)
- **P5, P6, x2**: Properties the model checks. P5 is clocks. P6 is forget. x2 judges a promotion’s crossed state in hindsight; it fails in every configuration and is never promised. (formal-model README.md:162-186)
- **the rig**: A throwaway Rama build of the rulings, on branch rig-2026-09-25, on the in-process cluster only. The conclusion makes it the first-edition build. (rig RIG.md:1-6)
- **stream store and micro store**: The store for one-owner layers, behind the stream gate, and the store for shared layers, behind the microbatch gate. (ruling 1)
- **lane A and lane B**: Lane A is the work with you: the rulings, the model, round three. Lane B is the work without you: the rig building phase 5, then grammars. (conclusion)
- **src-inland**: The Inland build’s source tree, with its own Rama module, the loop and the renderers. It is the first place to mount the store. (conclusion)

## 9. Sources, and what was checked

Server facts were checked by a read-only helper that returned the lines verbatim. The chat quotes that aren't in the repo come from the history account Sid pasted.

- [checked] `src/proposal/frame-2026-09-15/PROGRESS.md`, read whole: the rulings, the nine, the sharpenings, the open items, the log.
- [checked] The rig worktree `/mnt/data/projects/Softland-rig-2026-09-25`, folder `src/proposal/rig-2026-09-25/`: RIG.md and SPEC.md whole, STARTER-next.md, README.md:45-74, BENCH_NOTES-stream.md:40-70, and the branch’s git log. Times on this page are IST, from commit times.
- [checked] `src/proposal/rama-check-2026-09-25/RESULTS.md`, read whole (untracked on main).
- [checked] `src/proposal/formal-model-2026-09-24/README.md`: the results table (153-195), smallest failing history 14, the x2 notes.
- [checked] `vision/LOG.md:1296-1383`; `docs/carry-on.md`:133-140, 187-280, 360-364.
- [checked] `LEDGER.md`:154-160, 195-222; `PARTS-4-8.md`:146-153; `CORNERS.md`:12, 367; `docs/builds/inland/integration.md`:27-60; `history/docs/build-softland-in-softland/HANDOFF.md`:205-235.
- [checked] Server facts, checked by a read-only helper that returned the lines verbatim: `page/matter_room.cljc`:66-70 and :226; `rama/envelope.clj`:18-19, 113-119, 367-377; `rama/relation_kernel.clj`:355-381, 490-497; `door/server_jetty.clj`:1060-1066. env.clj was never opened.
- [checked] Main’s git log since 1 September. Recall for today’s three sessions: rama-protocol-sealing-design (which wrote the conclusion), model-seed-specification, and partition-seam-architecture.
- [sessions] Not re-read: the chat-only quotes of 12, 13, 14–15, 16–18, 22 and 23 September (from the history account); the seed’s candidate server lines; the Rama docs lines (depot-reference.md:263-275, query-topologies.md:3, 5, 151); rig PLAN-stream-store.md:71, 120-121; src-inland module.clj:129-136; object_container.clj:113, 628-657; bin/land:35.
- [sessions] Earlier pages, not re-read here: [Softland in Softland](https://claude.ai/artifact/UuBfmhgm6YJ2HtJK91FdjP) (10 Sep), [Before the Rig](https://claude.ai/artifact/BTUz3Qk3SR9GCWLMsTuA2j) (24 Sep), [Softland store rig](https://claude.ai/artifact/Nd6s5VymTNiFRLdH1QnAd4) (25 Sep).
