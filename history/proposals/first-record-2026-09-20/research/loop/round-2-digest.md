# Round two — digest of what came back

Kept by the orchestrator so the returns survive a context squeeze. Each entry
is the worker's own summary, shortened, with my note (I) on what it touches.
The full text is the "Round two" section of each report.

Correction: the "back HH:MM" times in the headings below were my estimates and
run ahead of the machine clock. The order of arrival is right; for real times
see `state.md`, which is taken from the transcripts.

## research-1 (Rama, Marz, Kreps) — back 12:31

Pressed hardest:

1. Lean (11) "gate's clock only", together with (0). If the offers depot is the
   permanent record and the gate reads a clock, the never-rewritten log cannot
   rebuild the facts, and the facts live only in mutable PState rows. Deciding
   case: the first re-partition. Rama cannot add tasks, and RPL's recompute
   recipe "only works if your processing is deterministic". Two exits: publish
   admitted facts to a facts depot and dedupe by id (Kreps's primary-backup
   log); or move the clock read in front of the append (Marz carries time in
   the record). Tool matching needs a feed of landed facts anyway.
2. Lean (9) "reaches backups": true of values, false of keys. The key store is
   itself backed up; a restore in year three brings back every key destroyed
   since. The gate needs plaintext to check shape, so it reads a key on every
   admission.
3. Lean (10) "never one number": after a re-partition, cuts written on
   never-rewritten facts point at a layout that no longer exists. Positions
   need a layout epoch from day one. A full cut at 1,024 tasks is 8 KB per
   pattern read, so intern it. Hot entities: promise order per cell, not per
   entity.

Wrong question: the rider on (0). (9) and (10) are now-or-never regardless (the
bytes of the first value; Rama's fixed task count). What (0) really freezes is
every choice of representation.

Strongest change above the table: one model, several storage promises. Leans
(0), (7), (11), (13), (14) put hand ticks (86M a day for 300 people on one
problem), refusals and fire-only reads into a never-trimmed log with full
provenance. Marz's garbage collection and Kafka's retention windows exist for
that class. Lean (9) also makes the key store a second thing that is not a fact.

My notes (I):

- Three camps now converge on the cut: name it with one number, record once
  what the number stands for, and stamp positions with a layout epoch
  (research-1 here, research 7's reclocking, research-2's XTDB epochs).
- "A facts depot is needed anyway as the feed that tools match on" is a strong
  structural argument for the results-log side of Kreps's fork.
- "Several storage promises" collides with Sid's sharpening at ledger line 755
  ("we cannot predict this without storing these signals somehow ... the fixed
  part never drops a crossing silently"). The camps do not say "do not capture".
  They say capture under a stated promise. A retention promise can itself be a
  policy fact on a key, which keeps one substance; then "never trimmed" becomes
  "never dropped silently: the drop is a fact". Sid must choose.
- The key store as a second non-fact thing presses layer 0 ("only the runtime is
  not"), not only lean (9).
- This round two was written at about 400K of context and reads sharp. Evidence
  for keeping the same sessions.

## research-2 (Hickey, Datomic, Nubank, XTDB) — back 12:19; text from line 763 of facts-datalog.md

Pressed hardest:

1. Lean (10), partition by entity. Deciding case: a pattern read over one
   person's layer. Entity hashing scatters that layer across every partition,
   so "as of" is a cut of up to N positions, and lean (11) lists it on every
   fact that stood on it. With the layer as the unit of order it is one number.
   Entity partitioning also makes landing facts together impossible, and puts
   private facts in the base's partitions and indexes. Against "never one
   number": name the cut (an index build is a fact; reads say "as of build N").
   Nubank (N): never a cross-database as-of; whole-estate questions come from a
   lagging extract. The worker marks all of this as inference: nobody in this
   camp lived in one partitioned store, and the Rama camp must say whether a
   layer can be a unit of order.
2. Leans (11), (1), (7), everything per fact. Deciding case: one model reply
   becomes 200 facts, so 200 envelopes, ids, salts and verdicts, and no atomic
   landing (fact 117 fails; 199 stand as a reply no model gave). Based-on filled
   in by the runtime is per invocation anyway, so it is 200 copies. The saying:
   an offerer-minted id (Nubank's correlation-id practice, R), the gate's
   position, one layer, one verdict; the fact keeps entity, key, value, saying,
   replaces. Backs lean (1)'s "the name is not the number" (Nubank replays
   databases, R) but names the saying, not the fact.
3. Leans (9) and (1). Hash the ciphertext: a salt kept in the clear does not
   stop guessing short values. Destroyed keys need tombstones (XTDB issue 184).
   Wrap value keys under a subject key named at admission, or "forget this
   person" cannot find its keys. Derived indexes need a checker (Datomic's 2025
   fix).

Also: (17) the seed grows in editions (Datomic, R); a first fact that names
itself cannot hash itself, so first ids come from words. (14) pan/zoom ticks
rejected.

Wrong question: lean (4)'s follow-latest or stay, fixed at writing. A typo fix
(v4) and a reversal (v5) need different answers and no mark written in advance
gets both right; the staleness definition, read later, can.

Strongest change above the table: add the saying, decided together with (10).
Second: a representation of "no longer so".

My notes (I):

- Lean (4) holds two different marks. Depends-on versus how-she-got-here is the
  actor's intent, known only at the moment, so by the loss test it must be
  written. Follow-latest versus stay is a policy about future changes, which the
  staleness definition (already a fact in the frame) can decide later. Split the
  lean: keep the first mark, drop the second.
- "Name the subject at admission" now has three independent sources: Nubank's
  regret (no owner id on every transaction), research 7 (only per-owner keys
  from the first record fit never-rewrite: Schwarzkopf, K9db), and this. A
  strong candidate for the table: whose data is this, written on the saying.
- Fault line 1 is live and has a precise form: is the unit of order the entity
  (research-1, research-4: "order belongs to keyed units, never to a layer") or
  the layer (research-2)? The 200-facts case and the private-facts-in-base-
  partitions case are the ones to put to the other side.
- "No longer so": the frame replaces by a later version in the same cell. It
  has no way to say a thing stopped being so without saying what is so instead.

## research-4 (log as truth) — back 12:27; section "Round two" of log-as-truth.md

Pressed hardest:

1. Lean (1), sharpened. A content-derived id is right, and Rama decides it:
   topology depot appends can repeat, so the gate's number cannot be the name.
   But the camp asks first: is the fact the offer, unchanged? Tango and
   Kleppmann never let the judge edit what it judges. Make the fact the offer
   byte for byte; put when, version, epoch and what-was-checked on the verdict
   beside it. One salt cannot do two jobs: a public salt for uniqueness, a
   secret salt inside a value commitment. Ingest lanes need derived salts.
   Deciding case: the ten-million-paper seed re-run after dying at paper 6.2M
   writes millions of refusals.
2. Leans (0) and (9). Define never-rewritten over the canonical form, not over
   Rama's bytes (case: a dead serializer in year twelve). Key destruction needs
   a recorded legal fallback (case: a regulator who rejects key deletion) and
   must itself be a fact (case: a key-store restore un-deletes). A bare value
   hash leaks short values after erasure.
3. Lean (7) with (13) and (14). Thirty agents on one cell make about 29
   refusals per fact; refusals and gesture ticks become the largest data in the
   store. Trim rule: keep each refusal's envelope and verdict for ever; destroy
   the refused value's key after the retry window. That rewrites nothing.

T3 (unit of order): the base layer decides it. A layer cannot be the unit of
order everywhere. Record "as of" as a cut always; give hand and crossing facts
the session as their entity.

Wrong question: (1) asks how the id is computed; ask whether the fact is the
offer unchanged. Above the table: lean (9) adds a second non-fact, the key
store. Losing it leaves a store of hashes.

## research-3 (meaning, objects, substrates) — back 12:28; section "Round two" of meaning-objects-substrates.md

Pressed hardest:

1. Leans (15) and (8). Door-verified identity, a recorded acts-for grant and
   "only enabled tools" still admit Hardy's confused deputy. Deciding case: a
   shared layer; A has enabled tool T ("file a summary on the entity a note
   names"); B, who may only write notes, lands a note naming an entity in A's
   personal layer; T fires by match and writes B's words there under A's
   authority; every check passes. Same case with a poisoned paper and a
   summarising model. Smallest fix: the gate never looks for a permission, it
   checks the one the offer cites. `under` is a grant fact at a version; grants
   name the grantee and pin the tool version; the verdict records grant,
   grammar and the cut. A click is one grant fact whose "covers" is the
   selection. A tool with no grant may give running answers only.
2. Lean (14). Tick-driven motion facts, kept for ever. Webstrates tried
   durable-by-default and walked it back (30 to 50 operations a second of
   cursor). Record "shown" in the viewer's layer when what is shown changes,
   not when time passes. Raw motion stays ephemeral unless the person turns
   capture on.
3. Leans (9) and (0). AT Protocol lost permanent history to deletion, through
   strong links. Lean (9) is sound if the kept hash is over ciphertext. But the
   key store becomes a second mutable substance whose backups are the hole.
   By-whom should be an opaque actor id whose tie to a human can be cut.

Also (17): split the seed into a universal half and a store-local genesis, or
"by whom: the gate" is ambiguous across stores.

Wrong questions: "which tools may act in a person's name" (ask what was handed
over, for what, until when); "looking" (only "shown" is knowable); "the fact's
id" (sayings have ids, facts do not). Above the table: decide what deserves to
be a fact; every read, tick and verdict, never trimmed, drowns the map. The
worker withdrew its round-one ask for a writer's clock: the based-on cut is the
writer's clock.

My notes (I), across research-2, -3, -4 and research-1:

- One structure keeps arriving from different camps: the offer is the saying.
  It is written by the offerer, has an offerer-minted id, is kept byte for
  byte, and may hold many facts. Everything the gate adds (when, version,
  epoch, what it checked, under which grant, at which cut, under which release)
  goes on the verdict beside it. Sources: Hickey's "the saying of it is the
  transaction" (checked by me), Tango and Kleppmann via research-4, Amazon's
  caller-minted id with stored parameters as the check via research 7,
  TigerBeetle via research-6, Rama's retry rule via research-1. This also
  answers Kreps's fork: the log holds the requests and the judge's results,
  both written, neither re-derived.
- Under that structure Sid's held line "an offer and a fact differ by one slot
  each way" changes form: they do not differ at all; the verdict carries the
  rest. "Replacing 25" is then kept by construction, because the offer's
  expected version is part of the kept offer. Lean (6) stops being a reopen.
- The confused-deputy case includes prompt injection: a poisoned paper read by
  a summarising model that acts under a person's authority. "Under what
  authority" is a loss-corner item: it cannot be added to early facts later.
- Three camps independently reject tick-driven hand facts kept for ever
  (research-1: 86M a day for 300 people; research-2: pan/zoom rejected;
  research-3: Webstrates walked it back). This collides with Sid's sharpening
  at ledger line 755. research-4's trim rule offers a middle: keep every
  envelope and verdict, let the value's key expire. Sid must choose.
- Three camps independently say lean (9) creates a second thing that is not a
  fact, the key store, and that its backups are the hole. Key destruction must
  itself be a fact, and restores must replay destructions.
- The hash kept on the fact must be over the ciphertext (or a commitment with a
  secret salt), never over the plaintext: short values can be guessed.
- Round three: line 1 is running. Lines 3 and 6 are folded into round two for
  research-6 and research 7. Lines 2, 4 and 5 have converged; research-5's
  report may reopen 2 or 5.

## Round three, line 1 — the unit of order (research-1 back 12:33, research-2 back 12:36)

research-1 conceded: a layer can be a unit of order in Rama (custom depot
partitioner; one task, one event, atomic), and Marz himself places by owner, not
by item. One PState has one owning topology, so there is no second microbatch
gate for big sayings; under entity placement a multi-entity saying cannot land
whole. Rejoinder: a team layer is one thread; a layer cannot be split or
re-homed; layers are few and unequal, so hashing balances badly; every stack
read pays a hop per layer. "The line turns on which read dominates." Verdict on
the hybrid: buildable; classify by writers, not visibility (single-owner layers
by layer; team layers and base by entity). Before the first record: placement
class readable from the offer; an epoch on every position; the saying as a unit
with one id and one verdict.

research-2 conceded: the base cannot be one order; a hot team layer hits
Hickey's ceiling; interning kills the 8 KB argument. Held: by research-1's own
account a 200-fact saying under entity keys is either not atomic (stream) or
unanswered and globally haltable (microbatch); layer-keyed private layers give
atomic, fast, answered. Lived practice splits by owner, never by hash (Nubank R;
Hickey "shard below" R; XTDB adds databases, not partitions R). Verdict on the
hybrid: the camp would accept it; it is the shape they live. Conditions: the
base gets a named cut; one saying, one unit (two layers means two sayings linked
by because-of; many-entity base sayings take the slow gate and are answered by
the verdict fact). Every saying carries: owner, offerer-minted id, the unit's
own count as position (not a task offset) plus layout epoch, gate stamp,
because-of root. Open risk: a team layer's keying is fixed at birth.

## research-6 (clocks, ids, determinism) — back 12:41; section 8 of clocks-ids-determinism.md

Pressed hardest:

1. Lean (1), id as hash of offer plus salt: sharpen. It fuses name and
   integrity; names are for ever, hashes and byte encodings are not. Deciding
   cases: in year twelve the hash allows collisions and no old pointer can be
   re-made; tens of agents in three languages canonicalize one plain map two
   ways, so one offer gets two ids and a retry lands twice. Keep a random
   offerer-made name; carry an algorithm-tagged digest beside it in pointers.
   State the expected version as the predecessor's id and digest, and lean (6)
   comes free.
2. Lean (7) with (10): a refusal must be written in the cell's partition, in
   the same atomic step as the compare-and-set: keyed to the target entity, only
   layered to the session. Deciding case: the gate refuses, crashes before the
   refusal lands elsewhere; the agent retries; the cell has moved; the offer
   succeeds. TigerBeetle's flaw before 0.16.4.
3. Lean (10), partition by entity: grants sit in other partitions. Alice
   revokes at 900; the agent's next offer meets a gate that has read to 850, and
   lands. The verdict must carry the cut it read the grant at, or revocation
   needs a serial point per layer.

Also (17): a seed-derived gate id collides at federation. (9): a plain hash of
a short erased value is guessable.

Wrong question: lean (5)'s "what was withheld". Recording it leaks that hidden
facts exist; record the policy position the answer was filtered at.

Above the table: crossings to a model carry content, not only a digest; the
envelope is no longer nine parts.

Line 3 (its T4): take the capability camp's slot (each offer cites the grant it
invokes; this cannot be reconstructed later) and Zanzibar's ordered evaluation.
Because-of already names the instigator.

My notes (I):

- One real disagreement remains on the name of a saying. research-6, Nubank's
  practice and Amazon's practice say a random offerer-made name with a digest
  beside it. research-3 says the offer's id is a hash of canonical bytes with a
  nonce and an algorithm tag. research-4 says both in different places. The
  deciding case is research-6's: two client languages canonicalize one map two
  ways, so a hash-as-name breaks retry safety, which is the very reason Rama
  wants offerer-made ids. A random name with an algorithm-tagged digest beside
  it keeps every property either side wants. I read this as settled in favour
  of the random name unless research-5 brings a counter-case from Git or SSB.
- Verdict placement is a wrinkle for the hybrid: a refusal is layered to the
  offerer's session but must be written where the target cell lives, in the
  same step as the compare-and-set.
- Revocation is the hard case for any placement where grants and cells sit in
  different units. Zanzibar's zookie was invented for exactly this ("new
  enemy"). The verdict recording the cut at which it read the grant makes the
  race visible and honest even where it cannot be prevented.
- Line 3 closed with both: the capability camp's slot and Zanzibar's ordered
  evaluation.

## research 7 (frontiers and views; defaults, skeptics, big tech) — round two in both of its files

Pressed hardest:

1. Leans (10) and (11) as a pair, by both of its camps. With "as of" a bare cut
   and `when` barred from order, nothing orders across partitions and cuts can
   be incomparable. Deciding case: the store grows from 64 to 256 partitions in
   year two; every recorded cut is a vector over a partitioning that no longer
   exists. A scalar survives. Aurora DSQL runs the reverse of both leans in
   production.
2. Lean (9), by the EDPB: ciphertext and a hash on the immutable record are two
   of the three forms it calls "not advisable". Case: a year-one value under a
   cipher that is weak by year six, with the ciphertext in every backup by
   design. "Value outside" survives XTDB's troubles if value ids are random
   (never content-derived), tombstones sit in the value store, "value absent" is
   a handled state from record one, and the value is durable before admission.
3. Lean (4): agrees with the Datomic camp that follow-or-stay is the wrong
   question. Depends-on is a counterfactual nobody can know for a model reply;
   record the read's role and alternative supports instead.
4. Leans (13), (17), (1): content-derived ids make the map's byte encoding a
   forever-promise. Case: a year-four rebuild changes key order; seed ids stop
   re-deriving, silently.
5. Lean (7): hot cells under compare-and-set mean thousands of refusals a
   second per person, kept for ever; a late retry must find its verdict by offer
   id from any session. Lean (8): a scheduled tool at 3 a.m. has no session to
   verify against.

Wrong questions: "cut or number" (ask whether as-of points are comparable and
where their meaning is stored); "maps or classes" (ask exactly which bytes are
hashed).

Strongest change above the table: keep `when` able to order. Two promises,
never backward within a partition and later than everything the fact stood on,
cost no bytes at record one and cannot be added later. They make the reclocking
map implicit under per-partition Rama gates with no global tick. research-6's
token is the same thing, provided its meaning is a fact.

Line 6 (its T4), recording every read against its cost: listing every matched
fact costs roughly 160 times the fact, plus 20 KB of vectors. One read-set fact
per firing, with pattern, as-of point, role, digest and an honesty slot
(complete or partial), supports all five walks at about 64 bytes per read. "Who
read the erased thing" over-reports only where a pattern tested the erased
value.

My notes (I):

- The two promises on `when` are the cleanest first-record item in the whole
  loop: no bytes, no new slot, impossible to add later. They turn the gate's
  stamp into a hybrid logical clock: still the gate's clock, as lean (11) wants,
  but monotone within a unit and later than everything in based-on. "As of T" is
  then a consistent cut that survives a re-partition. Knowing a cut is complete
  needs heartbeats per unit, which is runtime behaviour and can come later.
  This sharpens lean (11) instead of reversing it: the wall clock is still never
  trusted for order; the stamp is made orderable by rule.
- The cipher-ageing case is independent of any regulator and I find it decisive
  against keeping ciphertext in a log that is never rewritten: erasure by key
  destruction lasts only as long as the cipher, and a never-rewritten log cannot
  be re-encrypted. So erasable values belong outside the log, under a random
  value id. Whether a key's values sit inline or outside has to be known at
  admission, so it belongs on the grammar. To verify the EDPB wording before it
  is quoted to Sid.
- Line 6 closes: one read-set per firing at pattern grain, carried by the
  saying. The digest in it also answers research-6's challenge that
  re-derivation drifts across rebuilds: a later re-derivation can be compared
  with what was actually shown.
- Lean (8)'s "verified against the logged-in session" has no answer for a
  standing or scheduled tool. Citing a standing grant does.
