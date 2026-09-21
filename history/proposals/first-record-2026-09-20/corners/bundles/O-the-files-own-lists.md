# O the files' own lists: main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L910-912 · R2 · OWN-LIST E1,X2**
*Round two › R2.1 The tailored questions › T4. Reading old facts through today's schema*

**What must be written at admission, and where.**

1. **On the gate's verdict: for each key used in the saying, the version of the grammar fact the gate checked against. And the version of the policy fact.** Not on the fact, which would be 200 copies. Not on the saying, because the offerer does not know what the gate will check against. The verdict already "names what it checked". This is the content of that phrase.

---
**datalog L913-913 · R2 · OWN-LIST E1,C3**

2. **Why it cannot be worked out later.** Grammar facts live in the base. The fact lives in some other layer, with its own order. Finding *the grammar in force when this fact landed* means mapping a position in one order to a position in another. That mapping does not exist unless the gate wrote it down. R for contrast: Datomic can say a rule is "enforced starting on the transaction after" it is asserted only because schema and data share one order [D-SCHEMA]. Sid's grammars and facts will not share one.

---
**datalog L914-915 · R2 · OWN-LIST E1**

3. **Optionally, on the offer:** the grammar version the offerer built against. It makes a refusal readable: built for version 3, checked under version 4.


---
**datalog L916-918 · R2 · OWN-LIST W2,C7**

**What must be fixed for ever, so that the list above is enough.**

4. **The value's encoding belongs to the floor, not to the grammar.** One self-describing plain-data encoding, for ever. A grammar is a predicate over plain data. It never decides the bytes. Then any old value can be read with no grammar at all, and the recorded version tells what was promised about its shape. R: the one thing Datomic made unalterable is the value type [D-CHANGE]. I: because the type decides the bytes and the sort order in every index, and everything else was found safe to change later. R: XTDB let "arbitrary java.io.Serializable types" in and later called stricter types "hygienic" [X-DD7]. R: Hickey's habit of plain printed data [H-WD]. If a grammar can decide the encoding, then every decoder must be kept alive across hundreds of rebuilds of the runtime, and one lost decoder loses every value written under it.

---
**datalog L919-920 · R2 · OWN-LIST C4**

5. **Growth only under one key.** R: provide more, require less; "turn what would have been breaking into accretion" [H-SPEC]. A grammar version that requires more, or provides less, is a new key. R: "Never remove a name… Reusing that name to mean something substantially different… can be even worse" [D-BEST]. A rename is a second word on the same key.


---
**datalog L934-936 · R2 · OWN-LIST C7**
*Round two › R2.1 The tailored questions › T5. "A fixed envelope is place-oriented." What would they fi*

**So what the camp would fix before the first record** (I, put together from the above):

1. A small positional core. Entity, key, value, saying, replaces. Something must be positional at the very bottom. They keep it to the parts that every fact always has.

---
**datalog L937-937 · R2 · OWN-LIST C1**

2. The saying as an entity with an id and a position.

---
**datalog L938-938 · R2 · OWN-LIST C4,C7**

3. A *vocabulary*, not a layout. The first keys that describe sayings: by whom, acts under, when, layer, session, based on, because of, verdict. Each is an ordinary key, with an id, a word and a grammar, written in the first facts. They fix names, not places.

---
**datalog L939-939 · R2 · OWN-LIST W2**

4. The floor's one value encoding (T4).

---
**datalog L940-941 · R2 · OWN-LIST C4**

5. The rules of growth: never reuse a word, a second word is an alias, an incompatible shape is a new key.


---
**frontiers L25-26 · SHORT · OWN-LIST C2,E3,W3**
*0. The camp's shared model, in one page*

Translated into Sid's words: the gate's stamp is the time. "As of" is a frontier. A running answer is a view. "What was shown" is a read at a time. "The map must not lie" is what Jamie Brandon calls internal consistency. The camp's main message for the first record is about items 3 to 5: **put one authoritative time on every fact, make completeness a recorded thing, and bind every scalar "as of" to the per-partition positions it stands for.** Facts written without that cannot get it later.


---
**frontiers L425-426 · ABOVE · OWN-LIST C2,W3**
*9. What this camp would question above the table*

**The strongest challenge this camp makes to the premise.** As described, the system is an asynchronous, match-driven dataflow over lagging indexes. In Brandon's terms that is an eventually consistent streaming system, and such systems show states that were never true. Sid's principle forbids exactly that. What prevents it (one ordering time on every fact, frontiers that say what is complete, a durable map from that time to partition positions) has to be in the record format and the admission path from the first record. It cannot be reconstructed for facts admitted without it.


---
**frontiers L511-512 · R2 · OWN-LIST C2**
*Round two (2026-09-20): the leans, pressed from the frontier › T1. The smallest convention for internal consistency*

Three things, all small (I, built from R parts).
1. Every answer an index gives carries the point it was complete through. Materialize's rule (R): a read at t is correct only between `since` and `upper`.

---
**frontiers L513-513 · R2 · OWN-LIST C2**

2. Those points are comparable. If every read behind a running answer names the same point, the answer is exact as of it. If they differ, the floor has two honest moves: re-read everything at the older point (prevent), or show the answer marked as built from two moments (paint). Brandon (R): there must be "some way to determine when an output value is correct". McSherry's display (R): ready, refreshing, pending.

---
**frontiers L514-515 · R2 · OWN-LIST E2,C2**

3. A crossing record stores that point and that status.


---
**log L30-31 · SHORT · OWN-LIST C1,P0**
*The short version*

1. **A fact needs three names, because there are three jobs (questions 1, 2, 6).** Every mature system in this camp ended up with a name made by the client before admission (for retry and for pointing at a thing before it lands), a position given by the writer (for order and compare-and-set), and a hash of the content (for integrity). None of them could add the missing one later. Rama's own documentation says depot appends made from a microbatch topology "currently do not have exactly-once semantics in the face of failures and retries", so a fact's identity cannot be the gate's number. Pointers should pin id plus hash, as the AT Protocol does. The position can ride along as a hint: Sunlight put the index inside the receipt and deleted a database.


---
**log L32-33 · SHORT · OWN-LIST P0,C5,W2**

2. **"Never rewritten" is four promises, and they come apart (questions 0, 9, 13).** Never lost. Never a different answer under the same id. Never re-ordered. Provable to strangers. Helland allows absence and forbids difference: deleted data may map to "no present data" but "it will never return data other than the original contents". Rama's excision (a tombstone in place, offsets kept) fits that rule exactly. Certificate Transparency shows the price of the fourth promise: one flipped bit retired the Yeti2022 log, and a botched backup restore killed another. To keep the fourth promise possible later, a canonical byte form and a hash must exist from the first record. To keep erasure possible, the value must be separable from the envelope from the first record, and ids and envelope must carry nothing personal, because they are what survives.


---
**log L34-35 · SHORT · OWN-LIST A2,A1,E2,E1**

3. **Code-version skew, not application non-determinism, is what breaks replay in production (question 12).** Delos: "engine roll-out has been the only source of inconsistency in production so far, despite initial worries that developers would find it difficult to write deterministic code." Their fix: new code is enabled "by sending a command via the log itself", so every replica switches at one position, and derived state is checksummed. For Sid: the runtime version belongs on every verdict and every crossing, a runtime change should itself land as a fact, and a crossing should carry a checksum of what was shown.


---
**log L36-37 · SHORT · OWN-LIST C3,C2,E3**

4. **Order belongs to a keyed unit, never to a namespace; the position is the clock; "as of" across partitions is a vector (questions 5, 10, 11).** Helland: "You know only when a single unique key unifies both." A layer is a namespace over many ordered units, so many gates write one layer at once, and that is normal. A single "as of" number for the whole store exists only when something orders the vectors (Scalog's cuts). The as-of of a pattern read is the position the serving index had applied, never the log's tail. Two of Balakrishnan's systems put "how far the writer had seen" into every record (FuzzyLog, Delos ViewTracking).


---
**log L38-39 · SHORT · OWN-LIST E1**

5. **Log the offer before judging it, and write the verdict down (question 7).** Kleppmann describes this shape twice; Tango is this shape. It makes refusals free and lets a new gate be replayed against history. The verdict must be recorded, not re-derived, because the judging code changes (Tango's decision records; Delos's lesson above). Helland: on retry "the same reply must be returned". Certificate Transparency warns from the other side: refusals kept forever are a spam vector, so CT keeps none. A middle road exists: verdicts in a separate stream that may be trimmed.


---
**log L40-41 · SHORT · OWN-LIST C7**

6. **Fix the envelope's extension rules, not its nine parts.** CT's `extensions` field sat empty, opaque, and inside the signed message for about eleven years, then carried the one change that made the 2024 redesign possible: "The extension costs just 8 bytes." Delos's first entry format was positional, "a literal stack of buffers", and was "brittle against stack upgrades"; they moved to "a map of headers". Greg Young's weak-schema rules and the Cambria project name the cost on the other side: no renames ever, and "a record full of optional fields is hard to use".


---
**log L53-54 · SHORT · OWN-LIST -**
*The camp in one picture*

**Where they split.** Three fault lines, and the first record has to take a side on each.


---
**log L55-56 · SHORT · OWN-LIST C3,W3**

1. *One total order, or many orders.* One order: CORFU and Tango, Hyder, Aurora's single writer, each CT log, Event Store's single leader. Many orders: Helland's entities, Young's streams, Kafka's partitions, FuzzyLog, Kleppmann's later work. The telling fact is the direction of travel. The same people moved from one order to many as scale and geography bit: Balakrishnan from CORFU (2012) to FuzzyLog (2018), Kleppmann from Kafka and Samza (2015) to local-first and hash-named partial orders (2019 on). Delos kept a total order, but for a control plane with modest write rates. Rama has already chosen for Sid: order exists only within a partition.


---
**log L57-58 · SHORT · OWN-LIST P0**

2. *Kept forever, or trimmed.* This splits on what the log holds. A **redo log** holds commands or deltas for some other state: CORFU, Tango, Delos, Aurora, Kafka by default. These all trim. Balakrishnan in 2024: the shared log is "continuously being trimmed or moved to backup storage". A **record log** holds the facts themselves: CT, event stores, ledgers. These keep everything. Sid's store is a record log. So the redo-log systems teach Sid about position and order. About trimming they only give warnings.


---
**log L59-60 · SHORT · OWN-LIST P0**

3. *Trusted, or verifiable by strangers.* Only CT and its descendants make the promise checkable from outside. Everyone else trusts the operator. CT's decade of operations says exactly what that costs.


---
**log L579-582 · OWN · OWN-LIST -**
*Conventions this camp would fix before the first record*

## Conventions this camp would fix before the first record

All INFERRED: my distillation of the above, not any one source's list. Each is something that cannot be added to earlier facts later.


---
**log L583-583 · OWN · OWN-LIST C1**

1. Three names on every fact: a client-minted opaque id, the gate's number, and a content hash over a canonical form. Pointers carry id plus hash.

---
**log L584-584 · OWN · OWN-LIST P0,C1**

2. One canonical byte form and a named hash function, fixed and written down outside the store.

---
**log L585-585 · OWN · OWN-LIST C7**

3. The envelope is a map of named parts, with an envelope version and one extension slot that is inside the hash from day one.

---
**log L586-586 · OWN · OWN-LIST C5**

4. The value is separable from the envelope. Nothing personal in ids, keys or the actor slot. A salt on the hash of any value that may need erasing.

---
**log L587-587 · OWN · OWN-LIST C6**

5. "Replaces" is recorded, as a list of parents.

---
**log L588-588 · OWN · OWN-LIST E1**

6. The verdict is a fact naming what it checked: grammar version, policy version, expected version, gate epoch, runtime version, store id. Refusals go to a separate stream that may be trimmed, keyed by offer id, and the refused offer is kept with them.

---
**log L589-589 · OWN · OWN-LIST C2**

7. "As of" means the applied position of the index that served the read. Across partitions it is a vector, which can be named by a small fact. An offer can carry "how far I had seen".

---
**log L590-590 · OWN · OWN-LIST E5**

8. Every listed read is pinned to a version, marked depends-on or passed-through, and marked with who vouches for it: gate, runtime or actor.

---
**log L591-591 · OWN · OWN-LIST C4,X2**

9. The key is an id. Its meaning never changes. Each fact or verdict pins the grammar version.

---
**log L592-592 · OWN · OWN-LIST A2,E2**

10. A change of runtime lands as a fact. Every crossing carries the runtime version and a checksum of what was shown.

---
**log L593-593 · OWN · OWN-LIST C4**

11. Genesis: a few well-known ids in the runtime; genesis facts identical in every store; a written list of what lives outside the facts (encoding, trust anchor, successor pointer).

---
**log L594-594 · OWN · OWN-LIST -**

12. Both the chain's root and the immediate cause are recorded, and the runtime fills them.

---
**log L595-595 · OWN · OWN-LIST E3**

13. "When" is the gate's clock, monotone within a partition, never used for order. World-time lives in the value.

---
**log L596-596 · OWN · OWN-LIST P0,C5**

14. A planned, recorded road for excision and for neutralizing a poison record.

---
**log L597-598 · OWN · OWN-LIST C1,C3**

15. No stored pointer depends on a partition position, because the partition count and function are part of what a position means.


---
**log L659-659 · R2 · OWN-LIST P0**
*Round two: the leans, pressed › T1. The four promises against leans (0) and (9)*

**The exact minimum for promise 4 later** (I, from CT's record, N):

---
**log L660-660 · R2 · OWN-LIST P0,W2**

1. One canonical byte form of a fact, specified outside the store.

---
**log L661-661 · R2 · OWN-LIST C7,C1**

2. An envelope version and a named hash function inside the envelope.

---
**log L662-662 · R2 · OWN-LIST P0,E1**

3. The fact's hash, computed over that form at admission and kept in two places (the fact and its verdict), so that a flipped bit is a repair and not a Yeti2022.

---
**log L663-663 · R2 · OWN-LIST C5,P0**

4. Values enter the hash only through the commitment above, so erasure never breaks a chain.

---
**log L664-664 · R2 · OWN-LIST C7**

5. One extension slot inside the hashed form.

---
**log L665-666 · R2 · OWN-LIST C3,P0**

6. A per-partition order that no operation ever changes.


---
**log L703-706 · R2 · OWN-LIST -**
*Round two: the leans, pressed › T5. What must exist from record one so that nothing needs a *

### T5. What must exist from record one so that nothing needs a full playback

First, honestly: a truly new index always costs one full scan. Kreps's method is to run it beside the old one and cut over (R). The aim is that nothing else ever needs one. Five causes of playback in this camp, and what each needs:


---
**log L707-707 · R2 · OWN-LIST W2,P0,C5**

1. *New indexes.* I: make every future scan an envelope scan. Store envelopes apart from value bytes from day one. Lean (9) already makes values opaque. Ten million papers' values are large; their envelopes are small. Under lean (0) this layout cannot be changed later.

---
**log L708-708 · R2 · OWN-LIST C2**

2. *Readers without checkpoints.* vCorfu: "the entire log must be read to determine the most recent writes to each stream" (R). I: every derived thing names the cut it reflects and resumes from it.

---
**log L709-709 · R2 · OWN-LIST A1,A2**

3. *Runtime rebuilds.* Delos: "engine roll-out has been the only source of inconsistency in production so far" (R). I: each rebuild fact (lean 12) should say which keys' interpretation it changes, and derived state should record the runtime and tool versions that made it. Then a rebuild invalidates only what it names.

---
**log L710-710 · R2 · OWN-LIST E6,E2**

4. *Old as-of reads.* Indexes in this camp forget fast (DSQL keeps five minutes, R). I: a crossing should list what crossed, as ids and hashes, not only a pattern and a cut. A screen or a model's context is finite, so that list is always affordable. Young: "enrich the information returned from the call onto the event" (R). Then "what was I looking at yesterday" is a fetch by id.

---
**log L711-712 · R2 · OWN-LIST W1,C5**

5. *Erasure and doubt walks.* "Who read the erased thing" walks based-on backwards. I: that reverse index must exist before the first erasure, or the first erasure is a full scan.


---
**meaning L2694-2698 · R2 · OWN-LIST -**
*Part five — for the group › What I would carry into round two*

### What I would carry into round two

These are the things that are *gone forever* if not written from the first
record, according to this camp's scars. Each is argued above.


---
**meaning L2699-2699 · R2 · OWN-LIST E4**

1. Under what authority a fact was written (the grant it cited), not only by whom.

---
**meaning L2700-2700 · R2 · OWN-LIST C8**

2. "By whom" as a chain with the instrument in it, and whether offers are signed.

---
**meaning L2701-2702 · R2 · OWN-LIST C1**

3. The fact's own id, made before the gate, from canonical bytes, with a nonce and
   an algorithm tag.

---
**meaning L2703-2703 · R2 · OWN-LIST C2**

4. "As of" as a position per partition, and the index's position, not the log's.

---
**meaning L2704-2704 · R2 · OWN-LIST E1,A2,X2**

5. Which grammar version, policy version, and runtime build a verdict relied on.

---
**meaning L2705-2705 · R2 · OWN-LIST E5**

6. For each read: used, or depended on; filled by the actor, or by the runtime.

---
**meaning L2706-2706 · R2 · OWN-LIST E3**

7. The time the thing happened, apart from the time it was admitted.

---
**meaning L2707-2707 · R2 · OWN-LIST C6,C1**

8. "Replacing 25", as a number, with why.

---
**meaning L2708-2709 · R2 · OWN-LIST C4**

9. What a grammar change *did*, not only its result; and ids for fields inside
   values.

---
**meaning L2710-2710 · R2 · OWN-LIST C7**

10. "Starts a chain" said outright, so empty can mean unknown.

---
**meaning L2711-2711 · R2 · OWN-LIST -**

11. How a merge of two entity ids is said, and how every reader resolves it.

---
**meaning L2712-2712 · R2 · OWN-LIST C5**

12. A value that can be cut while its envelope stays.

---
**meaning L2713-2714 · R2 · OWN-LIST C4**

13. First facts whose ids are the same in every store.


---
**meaning L2715-2719 · R2 · OWN-LIST C7**

And one thing to do rather than write: shrink the part of the envelope that is
fixed for ever to what the gate needs, and let the rest grow by addition.

---


---
**meaning L2960-2974 · R2 · OWN-LIST A1,A2,C4**
*Round two › T4. The runtime outside the substance*

**What it would take: three things in the seed (I).** First, the written
semantics of the body language, in which tool bodies, grammars, grant checks, and
definitions such as "stale" are written. Small, and frozen by a rule that only
counts down (R: Urbit, "B must state the version of A it was developed against",
https://urbit.org/blog/toward-a-frozen-operating-system). Second, a reference
interpreter for that language, written in that language, as a fact. It may be
slow. It is the dictionary, not the engine (R for the pattern: Piumarta and
Warth's self-describing kernel, §2.7; N: Urbit's history "starts with a bootstrap
sequence that delivers Arvo itself", §3.6). Third, conformance cases as facts:
body, reads, expected answer. Each rebuild is then a fact that cites its
conformance run, and "the runtime is not a fact" shrinks to "the fast
implementation is not a fact; its meaning is". STEPS is the warning: what they
left undone was "the comprehensive 'bottom engine room'", and they blame
optimisation (R, §2.7).


---
**rama L31-34 · SHORT · OWN-LIST -**
*Part one — What Rama's own reference says › The short version*

## The short version

Eleven findings from the reference matter most for the first record. Each is backed in the sections below.


---
**rama L35-35 · SHORT · OWN-LIST P0**

1. **Rama does not promise "never rewritten". It promises that nothing is lost and that positions never move.** Depots keep everything by default. But Rama ships two doors for rewriting the log: depot migrations (transform or excise any record, offsets unchanged) and depot trimming (drop old records). "Never rewrite" is Sid's rule to hold, not Rama's.

---
**rama L36-36 · SHORT · OWN-LIST C3,C2**

2. **The partition count is fixed at launch.** "Tasks cannot be changed after launch." Growing past it means copying every depot into a new module. Log positions change when that happens.

---
**rama L37-37 · SHORT · OWN-LIST C1**

3. **So a log position is not a safe name for a fact.** Positions survive migrations. They do not survive a re-partition, a backup restore, or a trim. Only an id carried inside the record survives all four.

---
**rama L38-38 · SHORT · OWN-LIST C1**

4. **Ids must be made by the offerer, before the append.** Rama's own guidance: client-side UUIDv7, 128 bits minimum, never generated inside a stream topology. The reason is retries. This is the strongest single instruction the reference gives on Sid's table.

---
**rama L39-39 · SHORT · OWN-LIST E1**

5. **An append that throws may still have landed.** The offerer cannot tell. Only an offer id plus a kept verdict lets them find out. That makes keeping the gate's yes/no, refusals included, a structural need and not a nicety.

---
**rama L40-40 · SHORT · OWN-LIST C3,E1**

6. **Rama offers two different gates.** A stream gate is fast (milliseconds), can hand the verdict back inside the append call, is atomic only within one partition, and is at-least-once. A microbatch gate is exactly-once, atomic across all partitions, has a free global tick number, is slow (300 ms and up), cannot answer the offerer, and stops for everyone if any one task group stalls or any one record throws.

---
**rama L41-41 · SHORT · OWN-LIST C2,W3**

7. **With a microbatch gate, "as of" can be one number.** The microbatch ID is a 64-bit counter, the same on every partition, readable in topology code. With a stream gate there is no global number; "as of" is a position per partition, and the reference shows no way for topology code to see depot offsets.

---
**rama L42-42 · SHORT · OWN-LIST C1,P0**

8. **A topology writing to a depot is not exactly-once.** Only PState writes are. So if admitted facts are re-published to a second depot, that depot can hold duplicates, and every consumer must dedupe by fact id.

---
**rama L43-43 · SHORT · OWN-LIST E3,A1**

9. **Replay rebuilds only what is deterministic.** Rama's own re-partition recipe "only works if your processing is deterministic". A clock read or a generated id inside the gate is not. Whatever the gate adds (when, version) must be stored as primary data and copied, never recomputed.

---
**rama L44-44 · SHORT · OWN-LIST E5,E6**

10. **Rama records no reads.** There is no read log, no read provenance, no query audit. "Based on" is entirely Sid's convention. Rama neither helps nor hinders.

---
**rama L45-46 · SHORT · OWN-LIST C8,X4,C5**

11. **Rama has no authentication, no authorization, and no encryption in this reference.** Any client with a cluster connection can append to any depot and read any PState. "By whom", "who may see", and "delete by destroying a key" all have to be built above or in front of Rama.


---
**rama L466-466 · ABOVE · OWN-LIST C4,C1,C7,C3,C8,E3,E5**
*The group › 9. Questions this camp would call the wrong question*

- **"Which conventions must be fixed before the first record?"** They would split it in two. Representation can be migrated later; Rama ships the tool. Information that was not captured cannot be recovered by any migration. So the question becomes: what information will Sid wish had been captured? And which physical choices can Rama not undo? By that test, (3) word-or-id, (13) maps-or-classes, the id format in (2), and the envelope's exact shape are not first-record decisions if the migration door stays open. The first-record decisions are: who, acting for whom, based on what, because of what, under which code, at what time by whose clock, under which offer id; and in Rama, the task count, the partition key, the names, and ids inside the record.

---
**skeptics L394-395 · R2 · OWN-LIST C5**
*Round two (2026-09-20): the leans, pressed from the skeptics › T6. The EDPB against lean (9)*

**Does "value outside, reference inside" survive what XTDB v1 met?** I reason from research-2's relay only. Yes, if four things are fixed at record one (I).
1. Replay must not need the value. XTDB's indexer needed document contents. In Sid's envelope, everything a pattern matches by key is on the log. "Value absent" must be a state that every index and tool handles from the first record. That is the EDPB's "by design".

---
**skeptics L396-396 · R2 · OWN-LIST C5,P0**

2. Tombstones live in the value store and say "erased", so that missing never means unknown. An appended erasure fact on the log records who and when.

---
**skeptics L397-397 · R2 · OWN-LIST C1,C5**

3. Value ids are random, never computed from content. The resurrection race comes from content addressing: the same bytes written again reappear at the same address. A random id that is written once, in a store that refuses writes to a tombstoned id, has no such race. This is the one place where lean (17)'s content-derived ids must not be copied.

---
**skeptics L398-399 · R2 · OWN-LIST C5**

4. The value is durable before the offer is admitted. The other order leaves facts whose values never arrived, and those look erased. This is the outbox seam (R, microservices.io) coming back. Two stores is the price of this road, and "one substance" is then no longer strictly true.


---
**skeptics L400-403 · R2 · OWN-LIST E3,C2**
*Round two (2026-09-20): the leans, pressed from the skeptics › T7. Aurora DSQL as Sid's shape: what it fixed at its record *

## T7. Aurora DSQL as Sid's shape: what it fixed at its record one

All R from Brooker (https://brooker.co.za/blog/2024/12/05/inside-dsql-writes.html) unless marked.
1. Every journal entry has a commit time, and each adjudicator "promises to never commit another transaction at an earlier timestamp".

---
**skeptics L404-404 · R2 · OWN-LIST C3,C2,E3**

2. Idle adjudicators keep moving: "adjudicators promise to move their commit points forward in lock step with the physical clock, and share that commitment with storage". Without that a reader stalls on a quiet partition: "that could take a long time, especially if there write rate is low."

---
**skeptics L405-405 · R2 · OWN-LIST E3,C2**

3. A reader picks one time and waits until it has heard past that time "from every adjudicator".

---
**skeptics L406-406 · R2 · OWN-LIST E1,P0**

4. The Journal holds "committed transactions", not requests. Once there, an entry "can no longer be rejected", so consumers never filter and never coordinate.

---
**skeptics L407-407 · R2 · OWN-LIST C3,A3**

5. Checks are optimistic and happen at commit only. The price is hot keys: "you want the heat on your hottest write key to remain constant as the overall load on your database rises."

---
**skeptics L408-409 · R2 · OWN-LIST C3,X1**

6. A write that spans adjudicators needs "a cross-adjudicator coordination protocol", which Brooker does not describe.


---
**sync L3165-3176 · ABOVE · OWN-LIST E1,E5,E8**
*5. The group › 5.3 Questions this camp would call the wrong question*

8. **The premise of the table itself.** "Whatever is not written into a fact
   when it is made is gone for every earlier fact." The camp half agrees.
   Fossil's control artifacts, Git's notes, Mercurial's markers and AT
   Protocol's labels all add knowledge *beside* old records later, by new
   records that name them. What is truly gone is only what no one knew to
   capture at the moment. So the table sorts into two piles. **Must be captured
   at birth:** the offer's own id; the actor itself; based-on with roles,
   positions and viewpoint; because-of; what it replaces; the grounds of the
   verdict; the session. **Can be added beside, later:** names, types, digests,
   corrections to time and author, labels, translations between grammars. The
   first pile is where a mistake is permanent. It is also short.


---
**sync L3318-3324 · R2 · OWN-LIST C7**
*Round two: the leans, pressed › T1. "No optimism" against one worldwide gate*

**What to fix at record one so that both doors stay open.**

1. *The admission slot exists from birth.* Bayou gave every write its commit
   number slot at once, holding infinity until the primary filled it (R §2.1).
   Lean (6) already sees that an offer and a fact differ by one slot each way.
   So: one envelope for offer and fact, with the gate's slots explicitly "not
   yet". A tentative thing then needs no second format. (I)

---
**sync L3325-3331 · R2 · OWN-LIST E2**

2. *An "includes unadmitted input" mark, in what a running answer returns and in
   what a crossing records.* Bayou's query processor carried two bits from
   tuples into every result row (R §2.1). If the mark is not part of the
   contract from record one, the second door cannot be opened later without
   touching every tool, and old crossings can never say whether what was shown
   was all admitted. In committed-only mode the mark is always "no". It costs
   one bit. (I)

---
**sync L3332-3335 · R2 · OWN-LIST C2**

3. *The cut is an opaque token.* Weidner: versions that include tentative state
   "would require more complicated versionIDs" (R §2.12). A token that only the
   store compares can later hold "plus these unadmitted offers", and a gate
   epoch (below). Lean (10) says a cut; sharpen it to opaque. (I)

---
**sync L3336-3338 · R2 · OWN-LIST C1**

4. *Offerer-made ids.* Leans (1) and (2) have this. It is Weidner's reason for
   them (R §2.12).


---
**sync L3660-3669 · R2 · OWN-LIST C1,C2,C3,E8**
*Round two: the leans, pressed › C. Above the table: only what the leans change*

1. **Every personal layer is now a small store.** The hybrid placement turns
   "one writer" into "one writer per ordering domain", which is what this camp
   said in round one. It also changes "one store for the planet": from inside,
   it now has AT Protocol's shape, personal repositories plus shared indexes.
   So the second store stops being the rare later case. Its conventions are
   needed at record one: facts named by their own ids; gate numbers as
   per-layer annotations; references across layers that carry a digest or a
   cut; "as of" as a map; a gate epoch. The good news is that exit comes nearly
   free. A personal layer with its own gate is already exportable. This is my
   strongest change above the table. (I)

