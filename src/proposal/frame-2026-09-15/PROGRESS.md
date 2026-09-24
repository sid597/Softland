# Progress — where the frame work stands, and what is next

Rewrite **Now** whenever state changes; keep it short. Append to **Log**; never
edit it. A fresh session reads Now first, then LEDGER.md's "Where the walk
stands", then what they point at. Two Claude sessions work this folder in
parallel; if you change state, rewrite Now and append to Log. A line under
"Ruled" is Sid's; reopening one is allowed, with a reason, said out loud.

## Now (24 September 2026, evening)

**Where we are.** The frame walk closed on 18 September (LEDGER.md). The
first-record research produced CORNERS.md on 21 September: the store's
conventions that no later fact can put right, from eight research voices. On
22 to 24 September Sid worked through it in two parallel Claude sessions,
built his own frame for what a corner is, ruled the store-level lines in chat,
and on the evening of the 24th ruled the nine that block the rig. The chat
verbatim is not in this folder; these are chat rulings, not ledger rows, until
it is pasted. Vocabulary fixed on the 24th: "key" means a fact's key, the
"in this respect" part; "lock" means an encryption key. Never one word for both.

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

**Rig constraints, not rulings.** Every index over values is rebuildable from
the log or purgeable by value id, so forget reaches it. Lock rows sit on the
same task as their values. Every forget and every restore is a fact; forget
facts are replayed after any restore. Fingerprints over values are keyed.

**Open while the rig runs.** These change what a kept record carries, not
how the rig is built: the exact bytes the fingerprint covers and the written
logical form of a record (56); first-facts ids as constants and growth by
editions (59); opaque actor ids with one erasable link to the person (41);
ingest as random entity id plus a registry cell plus same-as (17, 60); the
runtime's meaning as first facts (3); the stale table's first default (44);
what stays a person's (83); forget's full reach (77); when a session layer
closes (82); repair lockout (84); what a new tool owes history (85).

**Next.** A formal model of the envelope's rules and the gate. Then the Rama
rig that tests the line: add a tool and a grammar from inside, count how many
new built-in steps were needed; measure index writes per second at the agent
rate, lock-store growth under hand layers, and one person's layer on one
thread. Then the decision record, both sessions' lists merged with rulings
marked, lands here as DECISIONS.md.

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
