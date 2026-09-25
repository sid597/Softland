# Where the store work goes next: the three sessions' conclusion (25 September 2026)

Verbatim, as Sid pasted it on the evening of 25 September. Written by the
session named rama-protocol-sealing-design after review by
model-seed-specification and partition-seam-architecture, which both voted
for it to lead. Nothing here is ruled. Sid's decisions of that evening are in
`src/proposal/frame-2026-09-15/PROGRESS.md`, "Now" and "Next"; the eight
defaults below are proposals the seed round meets in its trace. Section
titles carry heading marks added here; the wording is unchanged.

---

## Where the store work goes next: the conclusion from all three sessions

This is the only session you need to carry. The vote was 2 to 1 for it to lead: both other sessions voted for this one, and I had voted for model-seed-specification. Both reviewed what follows, and everything they caught is in it. They'll send you nothing more, except that model-seed-specification holds the file-and-line facts for any check you want. Nothing has been written to disk.

## The conclusion in three lines

- Not yet: don't build the real store as a separate project, don't migrate the server, don't run the rig in its briefed order, and don't write edition one yet.
- Next: you rule on eight things that decide what a kept record contains, and each goes into the formal model first. Then a third model round designs the seed against a real tool. Meanwhile the rig builds the read gateway.
- The thesis test: add a grammar and a tool by writing facts only, first on the rig, then in src-inland with records not kept. After that come locks, edition one, a two-node failover run, and the first kept record, in one-owner layers.

The rule behind all of it: the first kept record is the only decision that can't be undone, so everything that writes a record has to be complete from day one, and everything that only reads records can come later. Rule on things only when the next piece of work needs them. The model is the test every build must pass.

## Where things stand

- The rulings are consistent. The model ran 30,000 histories per check across three seeds, and every ruling you made holds. Flipping any one of them brings back its own failure. The only failing property is x2, which was never promised.
- What running Rama taught us:
  - Refusals must be data. An ordinary exception kills the worker and replays records that were already acknowledged.
  - An offer must carry the stamps of what it depended on. A read of the other store only returns what it holds now.
  - Shared layers must be read through a settled frontier. A microbatch becomes visible one partition at a time.
  - The gate's work per event must stay well under Rama's 5 s stream timeout (rama-check/RESULTS.md:45). The 30 s figure in RESULTS.md is when Rama killed the stalled batch.
- Phase 1 is green, including injected crashes. RIG.md says so; I haven't re-run it.
- First benchmark numbers (in-process cluster, so only the order of magnitude means anything; the notes aren't committed yet):
  - One agent layer on one task handled 2,234 acts/s with 16 writers, p99 latency 11.75 ms, using 37% of a core.
  - At that rate, stamps ran 20.7 s ahead of the wall clock (BENCH_NOTES-stream.md:56).
- The server:
  - It has no authentication. When the caller leaves out an actor, the actor defaults to "sid" (page/matter_room.cljc:66-70).
  - The capability check compares what the caller itself sent (rama/envelope.clj:367).
  - The only ownership rule it enforces is that only the original asserter can retract a relation (rama/relation_kernel.clj:490-497).
  - Branch and visibility fields are constants that nothing reads.
  - Verdict: the store core is new. The server contributes its admission pattern, its adapters, and its registries as seed input. Its side logs are forbidden by "one log" and go away when things are re-pointed.
- src-inland: its accept module (src-inland/softland/inland/module.clj:129-136) is the nearest existing ancestor of a one-owner layer. It has one stream topology, a depot hashed by workspace, and genesis rows admitted through its gate. That makes it the first place to mount the store. Nobody has yet done the pass to decide whether the store replaces that module or sits beside it.

## Eight rulings: my defaults, each yours to veto, each into the model first

R1. Forget is immediate.
- "One log" means the gate's per-task log, in your phase-1 words (SPEC.md:59). The depot is only where offers enter, because an append lands before any gate code runs. Nothing that could open a value may ever sit in the depot.
- The door seals each value before appending it. The lock reaches the gate by a path the depot never sees.
- Default path: lease.
  - In a lease act, the gate mints a batch of locks. The act is answered by name like any offer. The locks go into the lock store, wrapped under the session owner's lock.
  - The door seals each value under one of the leased locks, and the offer cites that lock's id.
  - At decision time the gate reads the lock from its own task, then re-wraps it under the value's actual subjects. Without that re-wrap, a value marked to die with any of its subjects could still be opened by the owner alone.
  - Nothing unreplicated sits in the decision path, and it costs one gate act per batch.
- Alternative path: the in-memory holder (PLAN-locks-and-forgetting.md:739-752). A query hands the lock to the task's in-memory holder before the append. That costs one round trip per offer, and the holder is empty after a worker restart.
- Both paths go into the model. Under either one, a missing lock is refused on the offer's face with nothing recorded, so a resend under the same name is still allowed.
- Riders:
  - The digest that catches a reused name is an HMAC over the plaintext, keyed by the value's lock. It is never taken over the sealed bytes, because a resend gets sealed again and its bytes differ.
  - To check a resend, the gate opens it with its own lock, recomputes the digest under the lock it recorded the first time, and compares.
  - After a value is forgotten, a same-name offer is answered from the record without a content check. So a reused name with different content can't be told apart from a retry. That is the price of forgetting.
  - This closes a gap the phase-2 plan left open: it kept the answer record's digest over plaintext "Unchanged".
  - Read-entry fingerprints cover the ids of the matched facts, not their values.
  - Every index can either be purged by value id (the cheap path for a single forget) or rebuilt from the record (the path after a restore). We have to build both tools, because Rama's PStates don't replay the depot.
- The alternative you're declining: a horizon, where forgotten values persist for a while before disappearing. It weakens "gone for everyone including the past" (PROGRESS:61), and it would need period keys in edition one.
- None of the three sessions thinks the depot needs a separate adversarial review, because the lock never enters the depot.

R2. Stamps are a hybrid logical clock: wall-clock milliseconds plus a counter.
- It never goes backward.
- Stamps compare across units and stores, which ruling 4 and property P5 already require.
- It stays within clock skew of real time.
- The stamp also serves as the read position.

R3. Read entries carry a frontier.
- A one-owner layer's stamp goes inline. A shared layer is recorded as the id of its settled frontier.
- "As of T" stays one number.
- Closing a single moment across both stores stays an option for edition two.

R4. Based-on is the act's stood-on entries: fact ids together with their stamps.

R5. Reads go through a read gateway that runs on the server.
- It queries, appends the read entry, then answers. Nothing is shown before its entry has been acknowledged.
- Agent-session reads are recorded there too, and kept or dropped when the session closes.
- This partly reopens ruling 3's "may default to none". What an agent stood on can be rebuilt from based-on. What it was shown, matched or passed through is lost unless it's recorded.

R6. Standing reads. src-inland's renderers hold reads open, and no ruling covered that.
- Each standing read gets one entry, opened with its pattern, role and frontier.
- Each delivery that shows something new adds a stamped line with the delivered fact ids. The ids are exact for a person or a model, and a fingerprint for a deterministic tool.
- Deliveries are recorded at the rate the person is shown things (LEDGER:158). For an agent, that rate is its model-call rate.
- A delivery of nothing new adds no line.
- The entry closes at unsubscribe or session close. It closes with a fingerprint over everything delivered and a mark saying whether the record is complete or partial.
- An entry left open by a crash is closed when the session closes.

R7. How permissions reach a layer. This re-reads "the session is the root": the session becomes the root of what it may do in each layer, not one root over everything.
- Each layer has a root permission, created with the layer.
- Opening a session writes a narrower permission into each layer the session may write, through that layer's own gate.
- Agents and tools get narrower permissions still, beneath those.
- Revoking a permission cuts everything below it.
- Change D forces this reading. In the model's trace 14, a gate that checks a permission kept in the other store misses a revocation.

R8. The base starts as a one-owner layer owned by the root actor, not a person.
- It runs on the stream gate, and is re-classed to shared when the first group arrives.
- Its lock rules follow its kind from day one: shared-layer rules, 7b as written, locks kept in the record. The root actor has no person lock, so there's no owner-required wrap. Only the gate and placement follow its class, so the later re-class changes nothing about locks.
- First facts stay facts (ruling 9). Their ids are constants, which is item 59's default.
- The split, for you to see. I had proposed the seed as edition constants. That contradicts ruling 9, so I've withdrawn it.

Also going into the model:
- P6 widens to "nothing retained opens or confirms a forgotten value". The depot, the lease rows or holder, and the lock store become separate places the model checks for retained data.
- New cases: the two lock-path cases above, and a layer gated by the stream gate with its locks in the record.

## The seed: model round three

- What it contains:
  - the vocabulary of built-in steps;
  - the tool fact: what it matches, its signature, its read-entry preference, and the permission it acts under;
  - the grammar fact: value shape, subjects, the opaque flag, index hints;
  - read roles, root permissions, default visibility, and constant ids.

  It closes open items 3, 59, 85 and 44.
- Designed against a real tool: a reference to a passage or function at a revision that shows the result, or shows that it is stale, when the material changes (docs/builds/inland/integration.md:41-45). That one tool exercises grammar facts, tool facts, read entries, based-on, the stale default, and what a new tool owes the history before it. The grammar-fact shape comes first.
- Candidate inputs from the server, treated as candidates rather than authority:
  - the capability set and actor types (envelope.clj:55-56, 81-86);
  - relation kinds (relation_kernel.clj:49-56);
  - activation and ground kinds (worn/activation_event.cljc:19-47);
  - the receipts that already record what was visible (episode/material_circulation.clj:137-142).
- The count. Zero compiled steps is the target, not a pass/fail gate. Your phase-6 brief says to report the count and every step, with the reason for each. Classify each step once you've seen it:
  - protocol: gate, envelope, read gateway, index kind;
  - capability: a new built-in function a recipe calls.

  A staleness index is the likely first honest protocol step.

## The order

Hygiene.
- One git worktree per concurrent session. The benchmark work is writing into the rig worktree right now.
- Your decision on the uncommitted gate_test.clj.
- A trace of what rewrote gate.clj on the 25th.
- The rig becomes the first-edition build: its code is kept, its records are not, and every phase follows the full build discipline.
Lane A, with you: R1 to R8 into the model, then round three.

Lane B, without you:
- Phase 5: the read gateway, frontier entries, standing reads, and value indexes that can be purged or rebuilt.
  - Values stay unsealed until phase 2; the "open" step is a pass-through for now.
  - Read roles are placeholders.
  - Index kinds take the grammar's index hints as parameters, so grammars plug in later without rework.
- Then grammars at the gate, as soon as round three fixes the grammar shape. They must come before locks, because ruling 8 derives a value's subjects from its grammar.
- Numbers: one person's layer on one thread is under way. The cost of reads being writes comes after phase 5.

Where the lanes join:
1. Phase 6: tools and a minimal runner. It comes after phase 5, because matching is a pattern read.
2. The same tool in src-inland, through the operation set (the door's API), with records not kept. This gives the real count, and it's your first use. It needs your merge decision on the rig branch.
3. Phase 2: the lock store and door sealing, using lease or holder according to the model, with lock-store growth measured alongside.
4. Edition one, written from the model.
   - The operations come first: offer and answer, read, retract, forget, promote, revoke, re-class, and open/close for layers and sessions. Then the envelope, refusal codes, read-entry format, and seed constants.
   - It can't be sealed until these close:
     - R1 to R8;
     - the record's written form and fingerprint (56);
     - seed ids (59);
     - opaque actor ids with one erasable link to the person (41), plus the minimum door;
     - ingest identity (17, 60);
     - what a new tool owes history (85);
     - the open gate rules: writes about someone already forgotten, and forgetting your own values after your write permission is revoked.
   - These can follow later without touching any record: the stale default (44), repair lockout (84), and the rest of session close (82).
5. A two-node failover run, because "visible implies replicated" is only a docs promise so far. Check first that the cluster and licence allow two nodes.
6. The first kept record, in one-owner layers only.
7. The micro store with its frontier, promotion, and the base's re-class, before the first group or the second person. Replaying the model's histories through the rig stays a standing practice.
8. One truth. src-inland's Rama and the server's object container move onto the store core. The adapters and page are re-pointed, the side logs go, and existing material comes back in as ingest.

## Open questions: who decides, and when

- You, now:
  - Keep or veto each of R1 to R8.
  - The rig's new label.
  - Thresholds for the three numbers. The rig currently assumes at least 1,000 acts/s per task at agent rate; at least 100 acts/s at 20 ms p99 for one person; and a lock store no bigger than 2× the value bytes.
  - The partial gate_test.clj.
- You, before the src-inland run: the merge decision on the rig branch.
- You, before the failover run: whether the cluster and licence allow two nodes.
- The model decides:
  - lease versus holder;
  - the two lock-path cases;
  - the stream-gated layer with its locks in the record.
- Round three decides:
  - the seed's contents and shapes;
  - what a new tool owes history;
  - whether a staleness index counts as a protocol step.
- Phase 5 will surface:
  - How a read composes layers. The rule held since 17 September is that the nearest active layer wins, found by walking builds-on (PARTS:150-156); it needs confirming.
  - What a standing read means for the "no re-run" rule that applies to models.
- Edition one closes: 56, 59, 41 with the door, 17 and 60, 85, and the open gate rules.
- Edition two: closing a single moment across both stores.

## Where the facts live

- Depot trimming: .claude/skills/rama/references/depot-reference.md:263-275.
- Queries are read-only for stored state but may write task-global state: query-topologies.md:3, 5, 151.
- The 5 s stream timeout: src/proposal/rama-check-2026-09-25/RESULTS.md:45.
- Clock drift and throughput: rig BENCH_NOTES-stream.md:56 (not committed).
- The holder path, and the "Unchanged" digest: rig PLAN-locks-and-forgetting.md:723-760.
- The {fid stamp} pairs: rig PLAN-stream-store.md:71, 120-121.
- The tool: docs/builds/inland/integration.md:41-45.
- The inland mount point: src-inland/softland/inland/module.clj:129-136.
- Server facts:
  - envelope.clj:18-56, 81-86, 113-119, 367
  - server_jetty.clj:1062-1065
  - matter_room.cljc:66-70
  - relation_kernel.clj:490-497
  - object_container.clj:113, 628-657
  - bin/land:35 (8 tasks)
- Your rulings: PROGRESS.md (store level 42-65, the nine 67-107, sharpenings 120-165, open items 172-186).
- Two server maps with verdicts exist: 33 points in model-seed-specification and 27 in partition-seam-architecture. Either can become a file.

## What gets written when you say settle

I'll show you the full set before anything lands:
- PROGRESS.md: Now gets the rulings you keep, Next gets the order, plus one Log line dated 2026-09-25 naming the three sessions.
- RIG.md: one status line (first-edition build; code kept, records not).
- Optionally: one server map under src/proposal/<work>/.

★ Insight ─────────────────────────────────────
- Lease versus holder comes down to one rule: a decision should depend only on replicated state. The in-memory holder puts process-local memory into the gate's decision path, and a worker restart empties it. A lease makes the key durable before the offer exists, so replaying the offer reaches the same answer.
- Keying the reuse digest by the value's own lock makes one mechanism do two jobs. While the lock lives, retries are checked exactly. Once the lock is destroyed, the digest can't confirm anything about the forgotten value.
  ─────────────────────────────────────────────────
