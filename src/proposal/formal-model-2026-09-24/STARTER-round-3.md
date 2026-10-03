# Starter for the third round, 25 September 2026

The prompt for the session that runs the model's third round: the seed,
designed against the reference tool and walked through the protocol act by
act. Sid's words, assembled by the session whose answer led the 25 September
review of what comes next, with five changes made at settle by the session
that carried the write-set: the rig is named a candidate; R1 to R8 point at
CONCLUSION.md; a scope line keeps the held-back tool out of reach; two lines
on how questions reach Sid; and "a query can't write" narrowed to PStates,
since the Rama check found queries may write task globals and depots.
Spent once the round is reported.

---

I'm building Softland, a place people and agents change from the inside, including the parts they use to change it. Everything lives in one store that only appends and never rewrites. The rules every record is written under are the protocol: the one part of the architecture that can't be rebuilt. Changing it is a new edition that splits the store into before and after. The store is the Rama build that carries the protocol, and it will be rebuilt many times.

    never rebuilt, only split into before/after: the rules every record is written under
      what     entity · key · value · grammar
      who      by whom · session · owner
      where    layer: base · group · personal · session
      may      permission · visibility
      when     clock promises · as-of (time travel)
      why      based on · because of · read entries
      change   replaces · retract · forget (locks)

This phase started on 13 September with a forced first step: nothing can be by-reference until repo material can be referenced as facts, a passage at a revision, a function at a revision, a decision's clause, a check and its result (docs/builds/inland/integration.md). Making that possible took me down a level at a time: what everything is made of, what freezes at the first kept record, my rulings (src/proposal/frame-2026-09-15/PROGRESS.md, the Now section), a formal model of them that holds after two rounds except x2, which was never promised (src/proposal/formal-model-2026-09-24/), and a Rama rig whose stream store is built and its suite passes, now a candidate for the store core (branch rig-2026-09-25, worktree /mnt/data/projects/Softland-rig-2026-09-25). This session climbs back up to that first step.

Design the store's seed against one real tool, and walk the tool through the protocol end to end. The tool: a reference to a passage or a function at a revision; when the material changes, it shows the result or shows that it is stale. It has to meet integration.md's requirements for a tool made inside. Its first material is this project's own citations: the file:line references in handoffs, plans and PROGRESS.md, which go wrong silently whenever those files change.

What I want from this session:

1. The seed this tool needs, as facts: the grammar fact (value shape, subjects, opaque flag, index hints), the tool fact (what it matches, its signature, its read-entry preference, the permission it acts under), the built-in steps it calls, read roles, root permissions, default visibility and constant ids. And how repo material enters the store: what entity a passage at a revision is, and how the store knows the passage at the next revision is the same one (open items 17 and 60). In my walk a new version is a new fact on the same thing (LEDGER.md:168).
2. Where staleness comes from. My frame says the fixed side guarantees that when something you built on changes, you find out, always; and that when a pointed-at thing changes, people and groups fill in what happens, with a default and a guaranteed walk (PROGRESS.md:28-31; open item 44 is the default). Show me which part of the tool's staleness is that walk and which part is the tool.
3. The tool's whole trace, act by act: the material ingested at one revision, the reference made, the file changed at the next, what the tool reads, what is recorded about that read, what the stale result stands on, what my open view is delivered. Three sessions left me eight defaults, R1 to R8 (full text in src/proposal/store-next-2026-09-25/CONCLUSION.md, under "Eight rulings"). Where a step depends on one, show me the default at that step and I'll rule it there. Every default you put to me says what it is for one level up and what we are checking it against. When a comparison is dense, put it on a page I can navigate and keep the chat for the decision. Tell me which defaults the trace never touches; those wait for edition one.
4. The model extended to check this: reads and read entries, grammars and tools, the trace as a fixed history, each default I keep as a configuration entry. Run it and show me what fails.
5. A one-page contract of the seed, which a fresh session that saw none of this could use to write a different tool and grammar as facts alone. I'll give that session a tool you don't know about, and count what it needs that the contract can't say. Don't look for the held-back tool in recall or in today's chats.
6. What the trace needs from the rig, in build order. A build session is working on the rig's reads in its own worktree; this list is what it builds next.

What running Rama taught us binds the trace: refusals are data, an offer carries the stamps of what it stood on, a shared layer is read through a settled frontier, and a query can't write a PState, so a recorded read goes through a gateway that queries, appends, then answers (the rig folder's README.md, "Findings").

Nothing lands on disk until I say settle, except the model's code, which you need in order to run it; show me the full write-set before anything is committed. Never read src/app/server/env.clj. No Co-Authored-By and no session links in commit messages. Ask before deleting anything. Don't write in any rig worktree.

Flavorings. The working session takes none, since it has to extend the model. These are for any side sessions you run on the same question.
- Claude, widest. It can't be pulled into the rig's half-megabyte of plans. It might see whether tracking a passage across revisions is your binding dimension in its smallest form, and what that means for identity beyond text: a function, a shape, a part in 3D.
- Claude, one mechanism deep. Most likely the link across revisions, worked through. It might find exactly where "the same passage at the next revision" breaks (a split, a merge, a move to another file), which this chat has only named.
- Claude, whole system. With receipts on both sides, it might find where the rig's phase-1 envelope (the {fid stamp} stood-on map) and src-inland's accept module (a depot hashed by workspace) already disagree with the trace.
- Codex. It does what the words say. On the narrow question of the seed's shapes against your rulings, it might catch a grammar or tool fact that contradicts a ruling where a Claude session would smooth it over.

read nothing on disk, no code, never src/app/server/env.clj.
read nothing on disk; code only if a claim needs a receipt, scoped to one file, say why you went; never src/app/server/env.clj.
read nothing on disk; code only if a claim needs a receipt; never src/app/server/env.clj.
attached: src/proposal/frame-2026-09-15/PROGRESS.md and docs/builds/inland/integration.md. i am asking what facts the store's seed must hold so that a reference to a passage or function at a revision can be added as facts alone and still show that it is stale when the file changes, and what i want from it is the grammar fact, the tool fact and the identity rule for repo material, each resting on a ruling i can find by line.
