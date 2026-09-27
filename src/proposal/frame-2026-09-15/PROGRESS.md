# Progress — where the frame work stands, and what is next

Rewrite **Now** whenever state changes; keep it short. Append to **Log**; never
edit it. A fresh session reads Now first, then LEDGER.md's "Where the walk
stands", then what they point at. Two Claude sessions work this folder in
parallel; if you change state, rewrite Now and append to Log. A line under
"Ruled" is Sid's; reopening one is allowed, with a reason, said out loud.

## Now (25 September 2026)

**Where we are.** The frame walk closed on 18 September (LEDGER.md). The
first-record research produced CORNERS.md on 21 September: the store's
conventions that no later fact can put right, from eight research voices. On
22 to 24 September Sid worked through it in two parallel Claude sessions,
built his own frame for what a corner is, ruled the store-level lines in chat,
and on the evening of the 24th ruled the nine that block the rig. The chat
verbatim is not in this folder; these are chat rulings, not ledger rows, until
it is pasted. Vocabulary fixed on the 24th: "key" means a fact's key, the
"in this respect" part; "lock" means an encryption key. Never one word for both.
The formal model ran two rounds on 24 and 25 September; the readings it
forced are ruled below, under "Sharpenings". Rama was checked on the 25th;
see the end of Now.

**Sid's frame for a corner** (his, 23 September). Time is the root: the log
appends and time runs one way, so a fact written later about a moment is a
claim, never a witness. A record's position on four dimensions is set at write
and permanent. Grain: how finely it was written; can be coarsened later, never
refined. Binding: what it points at and whether that holds still; the fixed
piece is that a stored pointer always resolves to the same thing; the social
piece is the dependency chain, what happens when a pointed-at thing changes,
filled in by people and groups with a default and a guaranteed walk. Source:
whose word each part rests on; per part; fixed at the time. Uniformity: a rule
every record follows; a change splits the store into before and after for
ever. A fifth dimension at the exit: exposure, reads that happened cannot be
unread. Cost is a check on every line, not a dimension. Standing stance: where
a choice has two ends the store offers the whole spectrum with defaults; the
toolmaker and user choose; uniformity rules are the one exception. The shape
that recurs: the menu is baked in code, the default lives in the first facts,
the pick is a fact on a layer, key, tool or value. A pick that decides where
or how something is written sticks; a pick applied at read can change.

**Ruled in chat, 23 and 24 September, store level.**
- "Never rewritten" is about meaning, not bytes. Re-encoding is allowed;
  removal is allowed when the removal is a fact; nothing comes back different.
- One log. Erasure by destroying a lock, not values beside the log. This
  reverses CORNERS.md's C5. Consequence: locks are for forgetting, not for
  hiding from the operator; the gate reads every value it matches on.
- The envelope: a small positional core on each fact (entity, key, value slot,
  act id and index, replaces) plus named parts the store owns (by whom, layer,
  based on, because of, permission, session, claimed when, expected versions,
  subjects), a version marker, and the gate refusing parts it does not know.
  Tools add facts, never envelope parts.
- Acts of any size, one fact included. Act id and index on every fact; who,
  when and reads on the act once. A stream is many small acts; the thread is
  an entity; a status fact on it says streaming, complete, or stopped.
- Every offer names the permission it acts under. The session is the root;
  narrower permissions are ordinary facts under it.
- Names: random, made by the offerer before the gate, scheme-tagged.
- Owner is derivable at read time from author, layer and permission.
- Two deletes. Retract: a new fact, undoable, time travel shows it. Forget:
  the lock is destroyed, gone for everyone including the past, time travel
  shows "erased on this date."
- Operator trusted at launch. No signing; no slot needed, a later edition can
  add the part. Facts from before that day rest on the operator's word.
- Keys are ids; word and grammar are facts about them.

**The nine, ruled 24 September (evening). The rig can start.**
1. Gate kind: stream for one-owner layers, microbatch for shared ones; two
   fact stores, each with its own gate. The stream and microbatch claims come
   from the Rama research file; the rama skill verifies before code.
2. Placement: by layer for one-owner layers, by entity for shared ones. N, the
   task count, fixed at launch, chosen for two years. The class on the offer,
   verified by the gate against the layer's class fact; a hot layer can be
   re-classed.
3. Read entry. One format: rows for point reads; one line per pattern read
   carrying the pattern, the moment, the role and a fingerprint of what
   matched; a complete-or-partial mark; empty pattern reads included. Two
   splits. One rule, by kind of reader: a model's or a person's reads are the
   crossing's exact list, always, because there is no re-run. Spectrum, by
   tool: a deterministic tool gets the short entry by default and may ask for
   exact rows in its signature. Eager by default in personal and shared
   layers; agent session layers may default to none, with the line added on
   promotion (waits on 82). Roles at seed: stood on, shown, matched, passed
   through; trigger is already because-of.
4. Clock promises: yes. Never backward within a unit; never earlier than
   anything the fact stood on.
5. Keys as ids: confirmed.
6. Who opens values: the gate by default. A key's grammar may declare its
   values opaque when a tool needs it, at the cost of no matching, no shape
   check and no index on them, and shown as opaque.
7. Where a value's wrapped lock lives: every value gets its own small lock at
   write, wrapped under the locks of the people it is about. Own row in the
   lock store for a person's own layer and their hand sessions; in the record
   for agent sessions, group layers and the base. A mark on any value
   overrides. Excision is the operator's fallback for unmarked record-default
   values. Lock rows are co-located with their values. Lock grain: per value
   by default; a setting on the layer, itself a fact the person can change,
   switches that layer to per act, one small lock shared by all values in an
   act, forgettable only as a whole; it affects only values written after
   the switch. Per-value locks inherit the act's subject list.
7b. A value about two people, one forgotten: survives by default; dies by a
   mark at write or a group's rule.
8. Subject slot on the act: filled from the layer's owner, the key's grammar,
   and the tool.
9. Default visibility: base open to any authenticated actor; a person's own
   and session layers private to that person; group layers visible to the
   group's members. Seed policy facts, so a store can differ.

**Layer kinds, four.** Base, group, personal, session; session split into hand
and agent by the session-start fact. Group layers are shared: microbatch gate,
by entity, wrapped locks in the record, members-only visibility, root
permission made with the layer naming the group.

**Two consequences.** A promotion from a personal layer into a group layer or
the base crosses from the stream gate to the microbatch one; under no
optimism the person's view shows "promotion pending" until it lands. A group
member cannot forget one value in a group layer themselves unless the group's
rule requires the mark on write; otherwise it is an excision.

**Sharpenings from the formal model, ruled 24 and 25 September.** The model
(src/proposal/formal-model-2026-09-24/; its README has the readings, the
results table and every trace) read the rulings above literally, found six
places where they were silent or pulled against a property, and a second
round added five changes. Each line below is a reading Sid made his in chat;
each flipped back alone brings back its own failure over 30,000 histories.
Chat rulings, not ledger rows, until the verbatim is pasted.
- Envelope, acts: layer belongs on the act; the gate refuses an act whose
  facts name another layer. Promotion is the cross-layer move, two acts.
- Names: an act that causes further offers names every one of them before
  the first gate; a promotion's landing is named from the request's name
  under a scheme reserved to the store. A name carries the layer and class
  it was made for and reaches one gate only. A reused name with different
  content is refused by a digest of the offer. An answer is found by name
  plus layer: the layer's home partition when placed by layer; a name row
  the microbatch commit writes in the same batch when placed by entity.
- Ruling 2: re-classing a one-owner layer to by-entity moves it to the
  microbatch gate. The single-owner order promise ends at the re-class.
- Rulings 7, 7b, 8: a per-value lock is wrapped under the value's own
  subjects, the three sources applied to that fact; the act's subject slot
  is the union, for finding. In a one-owner layer the owner's lock is always
  required, and 7b applies among the other subjects: Alice's mention of Bob
  in her own layer dies with Alice and survives Bob. In shared layers there
  is no person owner and 7b applies as written: about one person, dies with
  them; about two, survives one by default; marked, dies with any. A per-act
  lock is wrapped under the act's union, the coarser cut the person chose.
- Forget, time travel: a read as of a moment shows nothing admitted after
  it, except an erasure, which shows only its date. A deterministic tool's
  short read entry cannot re-run to its fingerprint once a matched value is
  forgotten; expected.
- The first consequence, promotion: two steps, a uniformity rule chosen
  once. At the read-out the stream gate opens the value through its lock on
  the owner's partition and writes a crossing fact there. Pending has two
  states: not yet read out, still forgettable; read out and not landed,
  crossed. A forget ordered before the read-out refuses the promotion; after
  it the copy is not recalled, because the read-out is a read that happened.
  The copy is about whoever the target's grammar and tool name, not its
  former owner. Crossed does not promise done; a landing can still be
  refused. Say all of this at the point of promotion.
- Permissions: a permission lives in the layer it governs; a gate checks
  only permissions in layers it orders; an offer citing a permission kept in
  another layer is refused. A forget names a permission too.
- Under all of it, Sid's rule: order between the two stores exists only
  through stood-on. A check one gate makes against the other store is a
  read, stamped in the checking store, never atomic with the other's
  writes, and the store promises nothing that needs such an order.

**Rig constraints, not rulings.** Every index over values is rebuildable from
the log or purgeable by value id, so forget reaches it. Lock rows sit on the
same task as their values. Every forget and every restore is a fact; forget
facts are replayed after any restore. Fingerprints over values are keyed.

**Open while the rig runs.** These change what a kept record carries, not
how the rig is built: a read's moment across the two stores: a read is final
only for what the store held when taken, since the other store can still
admit something dated at or before it, so a tool's short read entry re-run
later can match more; either a read closes its moment in every store it
read, or the entry carries one moment per store (the model's x2); the lock
store's order of a person's forget against a read-out, reasoned not shown;
whether a person can always forget their own values once their write
permission is revoked; writes about someone already forgotten; the exact
bytes the fingerprint covers and the written logical form of a record (56); first-facts ids as constants and growth by
editions (59); opaque actor ids with one erasable link to the person (41);
ingest as random entity id plus a registry cell plus same-as (17, 60); the
runtime's meaning as first facts (3); the stale table's first default (44);
what stays a person's (83); forget's full reach (77); when a session layer
closes (82); repair lockout (84); what a new tool owes history (85).

**25 September, evening.** Rama was checked: the two claims the model
assumes ran on an in-process cluster (rama-check-2026-09-25/RESULTS.md).
A stream event is atomic on one task and replays at least once; a
microbatch is decided before it is visible but not visible on every task
at one instant; an exception in topology code is fatal to the worker, so
refusals are data. The rig built and tested the stream store (13 tests,
728 checks) and measured the three numbers on slices, on branch
rig-2026-09-25. Three sessions then proposed eight defaults, R1 to R8,
that decide what a kept record carries (store-next-2026-09-25/
CONCLUSION.md); none is ruled. Sid's decisions of the evening: the rig's
code is a candidate for the store core, built in the form it should be,
records not kept, adoption after a read of its code; no ruling before the
model's third round, which walks the reference tool through the protocol
and puts each default to Sid at the step where it applies; the count is
taken on a held-back tool written by a fresh session from the round's
one-page contract, so it can fail. Still Sid's, due before phase 2 and the
first kept record: whether forget must reach the operator's copy of a
value in the depot, which Rama keeps until a trim by entry count that a
quiet partition never reaches; the stamp's form, proposed as a hybrid of
wall milliseconds and a counter; what a read entry's fingerprint covers;
whether two nodes can run here; the three thresholds and the size of
hand-session values.

**Next.** Two lanes. The model's third round designs the seed against the
reference tool of 13 September, a reference to a passage or a function at a
revision that shows the result or shows that it is stale, and walks it
through the protocol act by act, with Sid ruling each default at the step
where it applies (formal-model-2026-09-24/STARTER-round-3.md). The rig
builds the read exit the count needs on the stream store, then the code
that reads a file at a git revision into passages and functions with no
store identity (rig STARTER-next.md). Then the blind count: a fresh
session writes a held-back tool and grammar as facts from round three's
one-page contract and reports what the contract could not say. Then phase
6 on the rig: grammars at the gate, a minimal runner, both tools as facts,
the running count, read in three classes: fixed-side steps the frame
already promised, fixed-side steps nobody anticipated, and capabilities.
Then src-inland on this project's own file:line citations, Sid's first
use, with a scribe for the misses; this needs the merge decision on the
rig branch. Then the read side whole and the recount, locks under the
forget road Sid rules, edition one with the decision record as
DECISIONS.md, a failover run if two nodes exist, the first kept record in
one-owner layers, the micro store and promotion before the first shared
layer, and the server onto the store core.

## Log (append only)

- 2026-09-21. A session wrote CORNERS.md from the first-record research.
- 2026-09-22. Sid moved CORNERS.md here from first-record-2026-09-20/.
- 2026-09-23. Session A: the layer −1 and 0 diff after CORNERS.md; Sid's
  frame for a corner; an 86-line decision list in chat.
- 2026-09-23 and 24. Session B: a 14-line list; in dialogue with it Sid ruled
  meaning not bytes, one log with keys, small core, acts of any size, named
  permissions, random names, two deletes.
- 2026-09-24. Sessions A and B converged on the nine. This file written by a
  subagent on Session A's instruction, at Sid's go.
- 2026-09-24, evening. Sid ruled the nine in chat, plus group layers as a
  fourth layer kind. Session A rewrote Now; point 3 now states both splits,
  after Session B noted the compressed ruling let a model fall into the
  short default.
- 2026-09-24, late. Lock grain refinement on 7 from Session B, settled by
  Sid: per value by default in personal and hand layers, per act by a
  layer setting. Session A added the line to Now.
- 2026-09-24. A fresh session built the formal model and reported six
  readings. A review session took positions; Sid ruled the six, the
  owner-required line and the stood-on rule in chat that evening.
- 2026-09-24, night. A second session ran round two from
  STARTER-round-2.md: changes A to E, and a name tag proposed to make E hold.
- 2026-09-25. Sid ruled the name tag his, kept 7b for a mention of Bob in
  Alice's own layer, and left the read's moment open. The round was
  committed. The review session added the sharpenings to Now.
- 2026-09-25, evening. Three sessions (rama-protocol-sealing-design,
  model-seed-specification, partition-seam-architecture) concluded on what
  comes next (store-next-2026-09-25/CONCLUSION.md). Five more answered
  the new-session question; the last of them leads the seed round. Sid
  went with the plan settled in the session carrying this write-set, with
  fixes from the others: candidate, no ruling before round three, a
  held-back tool for the count. That session wrote the state paragraph
  in Now, Next, STARTER-round-3.md and the rig's STARTER-next.md, and
  changed one sentence of Now in place so the line ranges the rig's SPEC
  cites did not move.
- 2026-09-27. Two sessions (softland-de, softland-62) checked Inland's pointer
  as the first tool on the store, mapped the three codebases onto one system
  and walked the pointer through the protocol. At Sid's word they converged:
  the first tool is a citation that stays true (the 13 September reference
  tool), the pointer second as its hand, its walk folded into the next session
  so the one page covers both kinds of tool; edition one waits for the
  pointer's screen run too. Handover: citation-2026-09-27/HANDOFF.md.
