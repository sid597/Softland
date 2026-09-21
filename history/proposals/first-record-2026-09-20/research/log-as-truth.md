# Log as the source of truth: what this camp would fix before the first record

Research session: research-4. Date: 2026-09-20. For Sid's first-record decision. Reports to "orchestrator".

Scope after the scope change from softland-ff: Pat Helland, Martin Kleppmann, Greg Young and event sourcing as practiced, Mahesh Balakrishnan (CORFU, Tango, FuzzyLog, Delos), Phil Bernstein's Hyder, Amazon Aurora, Certificate Transparency and Trillian, and Lamport as foundation only. I added two short notes (Jay Kreps, Nathan Marz) because both are squarely in this camp and one of them built the substrate.

## How to read this

**Claim marks.**
- REPORTED: they wrote or said it. I give the quote and the source.
- INFERRED: I reconstructed it from their system or their principles. It is my reasoning, not their words.
- INSTITUTIONAL: a multi-author paper, a standards body, or company practice, and I am inferring what the institution thought.

**Quote rule.** Every quotation attributed to a source in this report was found, word for word, in source text downloaded during this session. Gatherer agents saved the raw text and confirmed each quote with `grep`. I re-grepped about 270 of the phrases myself, including every quote that carries a decision. Then a script extracted all 397 quoted segments from this finished report and checked each against the saved text. Where my rendering differed from a source (I had dropped a paper's bracketed citation numbers in four quotes, turned inner double quotes into single quotes in three, and transliterated a Greek symbol in two places), I corrected the report to match the source. Bracketed numbers such as [33] inside a quote are the source's own citation markers. The remaining mismatches were text-extraction artifacts: stray spaces left by stripped links, PDF hyphenation, a margin word interleaved by a two-column layout, and one scanning error in the 1978 Lamport PDF ("informatiori" for "information"). Phrases in quotation marks that are Sid's questions, words from the brief, or my own shorthand are not source quotes. Where I could not verify wording, I say so and do not quote.

**What I could not open.**
- *Designing Data-Intensive Applications* (Kleppmann) is paywalled. I have no verified wording from it. Where it matters I say "unverified" and give his own free papers instead.
- Greg Young's DDD Europe 2016 talk has no transcript I could reach. I use InfoQ's written report of it and label it as the reporter's words.
- ACM Queue's live site blocks automated fetches. Queue articles were read through Internet Archive captures of the same URLs.
- The body of Marz and Warren's *Big Data* chapter 2 (the fact-based model) was not reachable legitimately. The "nonce on every fact" claim is therefore not used.

**Model routing.** Source collection and mechanical quote checks: seven Opus subagents, one per source cluster. Reading of the decisive passages, all synthesis, all verdicts: this session (Fable 5.1, max effort). Nothing in the repository was read; the brief was treated as self-contained.

---

## The short version

Six findings for Sid's questions, then the camp's strongest challenge. The working is below.

1. **A fact needs three names, because there are three jobs (questions 1, 2, 6).** Every mature system in this camp ended up with a name made by the client before admission (for retry and for pointing at a thing before it lands), a position given by the writer (for order and compare-and-set), and a hash of the content (for integrity). None of them could add the missing one later. Rama's own documentation says depot appends made from a microbatch topology "currently do not have exactly-once semantics in the face of failures and retries", so a fact's identity cannot be the gate's number. Pointers should pin id plus hash, as the AT Protocol does. The position can ride along as a hint: Sunlight put the index inside the receipt and deleted a database.

2. **"Never rewritten" is four promises, and they come apart (questions 0, 9, 13).** Never lost. Never a different answer under the same id. Never re-ordered. Provable to strangers. Helland allows absence and forbids difference: deleted data may map to "no present data" but "it will never return data other than the original contents". Rama's excision (a tombstone in place, offsets kept) fits that rule exactly. Certificate Transparency shows the price of the fourth promise: one flipped bit retired the Yeti2022 log, and a botched backup restore killed another. To keep the fourth promise possible later, a canonical byte form and a hash must exist from the first record. To keep erasure possible, the value must be separable from the envelope from the first record, and ids and envelope must carry nothing personal, because they are what survives.

3. **Code-version skew, not application non-determinism, is what breaks replay in production (question 12).** Delos: "engine roll-out has been the only source of inconsistency in production so far, despite initial worries that developers would find it difficult to write deterministic code." Their fix: new code is enabled "by sending a command via the log itself", so every replica switches at one position, and derived state is checksummed. For Sid: the runtime version belongs on every verdict and every crossing, a runtime change should itself land as a fact, and a crossing should carry a checksum of what was shown.

4. **Order belongs to a keyed unit, never to a namespace; the position is the clock; "as of" across partitions is a vector (questions 5, 10, 11).** Helland: "You know only when a single unique key unifies both." A layer is a namespace over many ordered units, so many gates write one layer at once, and that is normal. A single "as of" number for the whole store exists only when something orders the vectors (Scalog's cuts). The as-of of a pattern read is the position the serving index had applied, never the log's tail. Two of Balakrishnan's systems put "how far the writer had seen" into every record (FuzzyLog, Delos ViewTracking).

5. **Log the offer before judging it, and write the verdict down (question 7).** Kleppmann describes this shape twice; Tango is this shape. It makes refusals free and lets a new gate be replayed against history. The verdict must be recorded, not re-derived, because the judging code changes (Tango's decision records; Delos's lesson above). Helland: on retry "the same reply must be returned". Certificate Transparency warns from the other side: refusals kept forever are a spam vector, so CT keeps none. A middle road exists: verdicts in a separate stream that may be trimmed.

6. **Fix the envelope's extension rules, not its nine parts.** CT's `extensions` field sat empty, opaque, and inside the signed message for about eleven years, then carried the one change that made the 2024 redesign possible: "The extension costs just 8 bytes." Delos's first entry format was positional, "a literal stack of buffers", and was "brittle against stack upgrades"; they moved to "a map of headers". Greg Young's weak-schema rules and the Cambria project name the cost on the other side: no renames ever, and "a record full of optional fields is hard to use".

**The camp's strongest challenge to the premise.** Two, and they come from the people who lived with these designs longest.

- *Nobody here recomputes derived answers from the log at scale.* vCorfu: "The achilles' heel of shared log systems, however, is playback." Ten of twenty-five event-sourcing engineers in the Overeem study complained that rebuilding is slow. Kleppmann: "In systems with a high event rate such replay may not be feasible." Marz himself moved from Lambda's recompute-from-scratch to Rama's incrementally maintained PStates. The camp stores derived state everywhere, never trusts it as truth, and keeps it honest by stamping it with the log position it reflects. Sid's rule that running answers are never stored is stricter than any system in this camp. This touches Sid's standing rule on caching, so I flag it for their own adversarial look rather than recommend anything.
- *The practitioners warn against one substance for everything.* InfoQ's report of Greg Young's ten-year retrospective: "CQRS and Event sourcing are not top-level architectures and normally they should be applied selectively just in few places." Kleppmann in 2021: "there is no one true way". The transfer is not clean (for Sid the audit trail is the product, not an add-on), but the costs they name are real and permanent: versioning forever, rebuild time, names that can never be renamed.

---

## The camp in one picture

**What they share.** The log is the truth and everything readable is derived from it. Helland: "The truth is the log. The database is a cache of a subset of the log." Kleppmann: "A materialized view is just a cached subset of the log, and you could rebuild it from the log at any time." Marz: "PStates are essentially materialized views of depots, with depots being the source of truth." Records are not edited; a correction is a new record. Helland: "Accountants don't use erasers". Whoever replays the same log must reach the same state, so anything non-deterministic is captured as data before it enters the log.

**Where they split.** Three fault lines, and the first record has to take a side on each.

1. *One total order, or many orders.* One order: CORFU and Tango, Hyder, Aurora's single writer, each CT log, Event Store's single leader. Many orders: Helland's entities, Young's streams, Kafka's partitions, FuzzyLog, Kleppmann's later work. The telling fact is the direction of travel. The same people moved from one order to many as scale and geography bit: Balakrishnan from CORFU (2012) to FuzzyLog (2018), Kleppmann from Kafka and Samza (2015) to local-first and hash-named partial orders (2019 on). Delos kept a total order, but for a control plane with modest write rates. Rama has already chosen for Sid: order exists only within a partition.

2. *Kept forever, or trimmed.* This splits on what the log holds. A **redo log** holds commands or deltas for some other state: CORFU, Tango, Delos, Aurora, Kafka by default. These all trim. Balakrishnan in 2024: the shared log is "continuously being trimmed or moved to backup storage". A **record log** holds the facts themselves: CT, event stores, ledgers. These keep everything. Sid's store is a record log. So the redo-log systems teach Sid about position and order. About trimming they only give warnings.

3. *Trusted, or verifiable by strangers.* Only CT and its descendants make the promise checkable from outside. Everyone else trusts the operator. CT's decade of operations says exactly what that costs.

---

## Voice by voice

### 1. Pat Helland

**1. What he built and chose.** Helland built transaction systems for decades (Tandem, Microsoft, Amazon, Salesforce) and then wrote down what he stopped believing. His choice, stated across five papers: data that crosses a boundary is immutable, identified, versioned, and from the past; the unit of consistency is one uniquely keyed entity; retries are certain, so every request carries an id.

**2. His reasons, in his words.**
- On the log as truth: "The truth is the log. The database is a cache of a subset of the log." (*Immutability Changes Everything*, ACM Queue 2015, section "Append-only Computing".)
- On what immutability means: "Data sets are semantically immutable but can be physically changed. You can add an index or two." (same, "Optimizing Data Sets".)
- On deletion: "In many environments, the immutable data may be deleted and the identifier will subsequently be mapped to an indication of "no present data," but it will never return data other than the original contents." (*Data on the Outside versus Data on the Inside*, CIDR 2005 and ACM Queue 2020.)
- On pointers: "When referencing data from outside, the identifier used for the reference must specify data that is immutable. If you find an immutable document that tells you to read today's New York Times to find out more details, that doesn't do you any good without more details (specifically the date and region of the paper)." He continues: "Note that this model allows for each data item to refer to its schema using simply another arc in the DAG." (same.)
- On what names an immutable thing: "By first binding the changing value of the key to a unique version of the key (e.g., [Key, Version-1]), you can view the version as immutable data." (*Mind Your State for Your State of Mind*, ACM Queue 2018.)
- On version history: "A linear version history is sometimes referred to as being strongly consistent: one version replaces another; there's one parent and one child; each version is immutable; each version has an identity. The alternative to linear version history is a DAG (directed acyclic graph) of version history, in which there are many parents and/or many children." (*Immutability Changes Everything*.)
- On what shares an order: "How can you know that two separate entities are guaranteed to be within the same transactional scope and, hence, atomically updatable? You know only when a single unique key unifies both. Now it is really one entity!" and "You can never count on different entity-key values residing in the same place." (*Life beyond Distributed Transactions*, ACM Queue 2016.)
- On time: "The contents of a message are always from the past, never from now." and "There is no simultaneity at a distance." (*Outside versus Inside*.)
- On retry: "The entity must durably remember the transition from a message being OK to process into the state where the message will not have substantive impact." and "if a reply is required, the same reply must be returned." (*Life beyond*.)
- On ids: "All that really matters is that the identity is unique within the spatial and temporal bounds of its use." and, on a central authority that hands out ids: "Does this authority scale?" (*Identity by Any Other Name*, ACM Queue 2019.) And: "Another important technique is to ensure that important identifiers such as customer IDs are never reused." (*Outside versus Inside*.)
- On cost: "Increased fidelity of memories implies increased cost!" (*Memories, Guesses, and Apologies*, blog, 2007.)

**3. What he changed.** The clearest revision in the whole camp. The 2007 abstract of *Life beyond Distributed Transactions* says: "My experience over the last decade has led me to liken these platforms to the Maginot Line." The word "Maginot" appears twice in the 2007 paper and not once in the 2016 ACM Queue version. The 2016 version opens instead with a concession that Spanner-class systems "offer strongly consistent transactional environments at extremely large scale with excellent availability", and adds "Unfortunately, this is not broadly available to application developers." He softened the absolute and kept the practical conclusion. By contrast, the 2020 republication of *Outside versus Inside* ends: "Nomenclature aside, not much has changed." (XML appears 39 times in 2005 and once in 2020; the model is word-for-word the same.) His newest statement on partitions, CIDR 2024: "When each record's changes go to ONE log, it has a “home”." On re-partitioning: "Earlier record versions were placed in their old log. After repartitioning new versions must go to a new log. This is difficult in the midst of ongoing updates."

**4. Which questions he speaks to.**
- (0) REPORTED: never changed in meaning; physical form may change. INFERRED: he would accept Rama's depot migration for re-encoding and for excision, and reject it for changing what a record says.
- (9) REPORTED: deletion is allowed; a different answer under the same id is not. "Records are deleted by adding tombstones."
- (4) REPORTED: a pointer must name an immutable version. INFERRED: "follow the latest" is something a reader computes; it is never what a stored pointer says.
- (3) REPORTED: a record may point at its schema as "another arc in the DAG", and "all message schemas be versioned and that each message use the version-dependent identifier of the precise definition of the message format." INFERRED: he would make the key an id, and have each fact (or its verdict) pin the grammar version it was checked against.
- (1), (6) REPORTED: the name of an immutable thing is [key, version]; a version records what it replaces ("one parent and one child"), and a store that is not linearizable produces many parents.
- (10) REPORTED: only what sits under one key shares an order; nothing else may be assumed to be in the same place.
- (5) REPORTED, and a gap: "When you read data from alternate indices, you must understand that it is potentially out of sync with the entity itself." He tells the reader to tolerate lag. He never proposes recording how far the index had got.
- (7) REPORTED: the decision must be durable and the same reply must come back on retry. He does not say the decision is a record in the same log.
- (11 when) REPORTED: every service has its own now; data seen is "unlocked and an artifact of the past."
- (11 based-on) REPORTED, a small but useful point: processing stays idempotent "even if a log record describing the read is written. The log record is not substantive to the behavior of the entity." Recording reads does not change what a write means.
- (2) REPORTED: ids never reused, scoped to cover every party that will use them. He does not choose between random and content-derived, and says nothing on whether an id may leak its origin.
- Nothing found on (8), (12), (17).

**5. Resembles and differs.** Resembles: many writers, permanent records, attribution, data that outlives the code that wrote it. His "outside data" is almost exactly Sid's fact. Differs: Helland assumes many services, each with private mutable "inside data". Sid's design has no inside data at all in storage; the only inside is the running answer, which is not stored. Helland never had to make one store serve as everyone's inside and outside at once.

**6. Disagreements.** With his own earlier self and the two-phase-commit tradition. With relational normalization for immutable data ("Normalization Is for Sissies" is his own heading). By name, with Werner Vogels and the Dynamo paper for discussing eventual consistency in terms of storage reads and writes: "Storage systems alone cannot provide the commutativity we need" (*Building on Quicksand*, 2009). His point: the record must carry the operation the person meant, not just a resulting value.

### 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and his critics

**1. What he built and chose.** A fifteen-year line of systems where a shared log is the only interface. CORFU (2012): a log over flash, positions handed out by a sequencer. Tango (2013): in-memory objects that are views of the log, with transactions as records in the log. FuzzyLog (2018): the log relaxed to a partial order for sharding and geography. Delos (Meta, 2020 and 2021): the log itself made swappable, with protocol "engines" stacked over it; in production for years. Then a single-author retrospective in 2024.

**2. The reasons, in the papers' words.**
- What a position is: a client appends, "obtaining a log position (which is effectively a logical timestamp)". (Balakrishnan, *Taming Consensus in the Wild*, SIGOPS OSR 2024.)
- The sequencer: "it is merely an optimization to reduce contention in the system and is not required for either safety or progress." Measured then at "around 180K appends/sec". (CORFU, NSDI 2012.)
- A write carries its reads, and everyone judges it the same way: "each commit record contains a read set: a list of objects read by the transaction along with their versions, where the version is simply the last offset in the shared log that modified the object. A transaction only succeeds if none of its reads are stale when the commit record is encountered". Each client decides "independently but deterministically". (Tango, SOSP 2013.)
- When a reader cannot judge, the verdict is written into the log: "a client executing a transaction must insert a decision record for a transaction if there's some other client in the system that hosts an object in its write set but not all the objects in its read set." (Tango.)
- The first thing is found by a fixed id: "This directory is itself a Tango object with a hard-coded OID." (Tango.)
- History costs storage, and trimming sells history: an object "can forgo the ability to roll back (or index into the log) before a checkpoint with a forget call, which allows Tango to trim the log and reclaim storage capacity." (Tango.)
- Why not one total order: "the simplicity of a shared log requires imposing a system-wide total order that is expensive, often impossible, and typically unnecessary." And across regions "a total order may be impossible: a network partition can cut off clients from the sequencer". (FuzzyLog, OSDI 2018.)
- What "as of" becomes in a partial order: "The return value of log-snapshot acts as a vector timestamp for the color". And every record carries what its writer had seen: "It includes the vector timestamp of nodes seen thus far in the new entry; as a result, each appended entry includes pointers to the set of nodes it causally depends on". (FuzzyLog.)
- Why the log was made swappable: consensus-based systems are "complex, monolithic, and difficult to upgrade once deployed." What must live outside the log: "The VirtualLog MetaStore is a necessary and sufficient source of fault-tolerant consensus in our architecture". Why it cannot live inside: inline reconfiguration "requires the Loglet itself to be highly available for writes". (Delos, OSDI 2020.)
- Retry: "Retries are idempotent (i.e., the same command is written to the same position)". (Delos, OSDI 2020.) This works only because the position is fixed before the retry.
- Code upgrades go through the log: "Once all servers have the new engine, we enable it by sending a command via the log itself. This ensures that the effects of the engine are visible on the local store beyond a consistent log position, retaining the property that the LocalStore is a deterministic function of the shared log." (Delos, SOSP 2021.)
- How far a reader had got travels in the record: the ViewTrackingEngine "adds a header on each outgoing propose with its local playback position", and "the view is a deterministic function of the log; and as a result, so is the decision to trim a prefix of the log." (Delos, SOSP 2021.)
- Physical time enters only as a proposal: "To support time-based trimming in a way that is robust to clock skew and drift, we implemented a TimeEngine". (Delos, SOSP 2021.)
- Inputs or outputs in the log: "Having flexibility in whether we store inputs vs. outputs in the shared log can help balance ingress bandwidth against CPU overhead". And a stored output "can only be applied to the database if the intervening entries did not invalidate it". (OSR 2024.)

**3. What they changed or regretted.** This line of work is a chain of corrections, which is why it matters.
- *Total order, relaxed.* FuzzyLog is the CORFU author saying the system-wide order was "typically unnecessary".
- *Playback.* vCorfu (2017, by Tango co-authors): "The achilles' heel of shared log systems, however, is playback. To service any request, a client must read every single update and apply it to in-memory state". "In practice, this has limited the applicability of shared log systems to settings characterized by few clients or small global state."
- *Sharded readers over one log failed for a reason nobody predicted.* OSR 2024: "the key blocker for such a design proved to be blast radius: e.g., if we lose access to a single slot in the shared log after it is written (but before it is played), every learner across all shards will have to block processing at that entry".
- *The entry format.* "In our first implementation, each entry was a literal stack of buffers (similar to a network packet); each engine would push/pop its own header. However, we found that such a layout was brittle against stack upgrades". "Since the entry is instead a map of headers, each engine can simply check within the apply upcall if its own header is within the entry". (Delos, SOSP 2021.)
- *Upgrades.* "Ad-hoc stack updates resulted in inconsistency events across servers in production, which caused us to formalize the two-phase update protocol". "Surprisingly, engine roll-out has been the only source of inconsistency in production so far, despite initial worries that developers would find it difficult to write deterministic code. We built extensive protection against this failure mode via incremental checksums of the LocalStore". (Delos, SOSP 2021.)
- *"Forever" met production.* "Consensus is forever... until it's not: Deletion of arbitrary entries is typically quite difficult in conventional consensus protocols. However, with virtual consensus, we can delete an entry simply by changing the metadata of the VirtualLog. Similarly, altering written entries is possible via remapping. We found this kind of surgical editing capability useful when faced with site-wide outages: on one occasion, a "poison" entry caused hangs on all learners processing the log." (Delos, OSDI 2020.)
- *Old segments go cold, not away.* Delos moved "older segments to a Loglet layered on cold storage (BackupLoglet)", which gave "point-in-time restore".
- *Nobody reserved room.* The gatherer searched all seven papers for reserved fields, format versions, forward or backward compatibility. Zero hits. The only format evidence is one format bit in Tango's stream header and Delos's retrofit above.

**4. Which questions this line speaks to.**
- (1) INSTITUTIONAL: inside one log the position is the only name, and it is enough. INFERRED: it stopped being enough the moment the log became a chain of logs. Delos had to keep positions stable across different log implementations, which is an argument for not letting a pointer depend on physical layout.
- (7) REPORTED: verdicts are written into the log whenever some reader could not re-derive them. INFERRED: Sid's gate is always in that case, because the code that would re-derive the verdict will be rebuilt hundreds of times. The verdict must be a record.
- (11 based-on), (4) REPORTED: a write carries the versions it read, and is void if any moved. INFERRED: Tango records reads for *validation*. Sid records reads for *provenance*, and the gate validates only one of them (the cell's expected version). The record should say which reads were checked by the gate and which were only declared.
- (5) REPORTED: Delos and FuzzyLog both put how far the writer had seen into every record, as one compact value. INFERRED: an offer can carry one frontier that bounds everything its actor could have known, alongside any itemized reads.
- (10) REPORTED: across shards or regions, "as of" is a vector, and vectors "can be compared to check if one subsumes the other". A single global order couples failure domains (blast radius).
- (11 when) REPORTED: no physical clock orders anything in any of these systems. Tango sets itself against Spanner on exactly this.
- (12) REPORTED: changes to interpreting code enter through the log; derived state is checksummed. This is the most production-tested answer to question 12 anywhere in the camp.
- (13) REPORTED: a map of named parts survived version skew; a positional layout did not.
- (17) REPORTED: a hard-coded id for the first object; a tiny compare-and-set register outside the log to say which log is current.
- (0), (9) REPORTED: they trim, and they edited written entries when production demanded it. INFERRED: a store that promises never to rewrite still needs a planned, recorded way to neutralize a poison record.
- Nothing found on client-made ids, on duplicate suppression when an acknowledged append is retried, or on (8).

**5. Resembles and differs.** Resembles: one log as the only interface; everything else is a view; writes that carry their reads; many clients; years in production; the people who built it wrote down what went wrong. Differs: these are redo logs. Entries are commands for a state machine, so trimming is natural and nobody needs an entry from five years ago. Write rates are control-plane rates. There is one operator and no stranger.

**6. Disagreements.** With Raft-style designs that fuse the log and the application ("aggressively combine both planes into a single protocol"). With Hyder, whose hole-filling "resorts to a heavy recovery protocol that involves sealing the system". Against Balakrishnan: Scalog (Cornell, NSDI 2020) says CORFU's order-before-persistence is the root of its holes: "records are first replicated, and only then assigned a position in the total order", and the dilemma "arises whenever a storage system makes decisions about an item's metadata before making persistent the item itself." Scalog's shards report "an integer vector summarizing the records stored", and "a deterministic function that specifies how to order the records in between two consecutive cuts" turns vectors into one order. Note its footnote: "These guarantees hold only in the absence of trimming."

### 3. Greg Young and event sourcing as practiced

**1. What he built and chose.** Young named CQRS, built Event Store (now KurrentDB), and wrote the one book on living with events that can never be edited: *Versioning in an Event Sourced System*. Choices: events are never updated; a stream is the unit of order and of optimistic concurrency ("expected version"); the client makes each event's id; event types are names; schema is weak (maps); every message carries three ids.

**2. His reasons, in his words.**
- Why no edits, ever: "And if you allow a single update… Well, your data is now definitely maybe immutable. Also known as mutable." And: "Immutability is immutable. The moment you allow a single edit, everything becomes suspect." And the legal bar: "The bar for being able to submit your audit log to a court of law is that you must be able to rebuild your current state from your audit log." (chapter "Why can't I update an event?")
- What a version is: "A new version of an event must be convertible from the old version of the event. If not, it is not a new version of the event but rather a new event." (chapter "Basic Type Based Versioning".)
- The price of maps: "you are no longer allowed to rename something. … You can get around this by supporting both Id and ItemId, but this can quickly become annoying, especially with an Event Sourced system, where you cannot just deprecate it but must carry it forward into the future." (chapter "Weak Schema".)
- Meaning is frozen: "semantic meaning cannot change between versions of software. There is no good way for a downstream consumer to understand a semantic meaning change." (chapter "General Versioning Concerns".)
- Record the answer you relied on, not the recipe: "Make the call at the time of the event creation and enrich the information returned from the call onto the event. This allows for deterministic replays." (same chapter.)
- Order and concurrency belong to the stream: "it is imperative that both use ExpectedVersion set to the last event they read." (chapter "Cheating".)
- The three ids: "Let's say every message has 3 ids. 1 is its id. Another is correlation the last it causation. The rules are quite simple. If you are responding to a message, you copy its correlation id as your correlation id, its message id is your causation id. This allows you to see an entire conversation (correlation id) or to see what causes what (causation id)." (event-store Google Group, 29 March 2015.)
- Deletion: "Deleting a whole stream is a safe operation, whereas deleting a single event or editing an event is not." And: "Instead of deleting, an alternative allowable under many rule sets is to encrypt the event data and forget the key which is kept in another system." (chapter "Whoops, I Did It Again".)
- From the Event Store documentation: the event id is "A unique identifier representing this event. Event Store uses this for idempotency if you write the same event twice you should use the same identifier both times." The type "should be a “friendly” name rather than a CLR type name". "The idempotence check is based on the EventId and stream." And the caveat: "Idempotence is not guaranteed if you use ExpectedVersion.Any."

**3. What he and the field changed or regretted.**
- His own named regret, on rewriting a store by copying it: "Admiteddly, I am likely to blame for the current popularity of Copy-Replace." "In retrospect, I did not speak enough to the practical downsides of the approach." "Copy-Replace is the nuclear-option of versioning." Yet: "I have found a huge number of them have given up on most of the other versioning strategies and just use Copy-Replace." The field went to the rewrite anyway.
- The trap inside that rewrite: "Can it really be considered idempotency when two completely different events have the same identifier but potentially drastically different information in them?"
- How accountants really cope with long histories: they close the year. "In the accounting office there were 10 accounting systems. Nine were marked by their year and were read-only". The cost: "Running a projection that covered two years worth of information from the accounting system was a real pain."
- The ten-year retrospective, in InfoQ's words reporting his DDD Europe 2016 talk: "The single biggest bad thing that Young has seen during the last ten years is the common anti-pattern of building a whole system based on Event sourcing." "CQRS and Event sourcing are not top-level architectures and normally they should be applied selectively just in few places." His own blog in 2012 already said: "CQRS is not a top level architecture".
- The Overeem study (25 engineers, 19 systems, *Journal of Systems and Software* 2021) is the best record of lived regret. Its five challenges: "event system evolution, the steep learning curve, lack of available technology, rebuilding projections, and data privacy." On upcasters: "If you have been running upcasters for a long time, you will have quite a stack of them in place, which slows down the entire loading." On rebuilds: "Rebuilding is slow" (ten engineers). On privacy law: "Systems HealthSys and P-PaySys use some form of anonymization and removal of information to comply. Obviously, this requires them to rewrite events. System IdentitySys takes a completely different approach. The system separates the events and the personal information in two different stores." The vendors took a side: Event Store and AxonDB "deliberately do not offer these operations" (in-place edits). The study's advice ends: "In-place transformation should only be used by those systems that do not require immutability or an audit log."
- On crypto-shredding, the practice literature disagrees with Young. Mathias Verraes: deleting the key "effectively makes all copies and backups of the sensitive data unusable", but he prints a lawyer's correction: "the law does not consider deleting the encryption key equal to actually deleting the data itself." On keeping personal data in a side store, Verraes names the cost: it "breaks the concept of an Event Store as the Single Source of Truth", and the residue: a person "can be identified not only through personal information, but through the associations and relations with other information."

**4. Which questions he speaks to.**
- (0) REPORTED: never edit; and yet most teams end up copying and rewriting whole stores. INFERRED: a design that gives no cheap, honest way to evolve will be rewritten wholesale by its own users.
- (3) REPORTED: names can never be renamed, meaning can never change, and a type name tied to code is a known mistake. INFERRED: he would want the key to be a stable id whose display name is free to change, with the rule that a changed meaning is a new key.
- (13) REPORTED: weak schema (maps, with his three mapping rules) over typed classes.
- (1), (2) REPORTED: client-made id for idempotent retry, scoped to the stream; plus the stream revision for order. Two names, two jobs.
- (10) REPORTED: the stream is the unit of order; if two things need mutual order, put them in one stream or merge in the application.
- (11 because-of) REPORTED: carry both the conversation id and the immediate cause, and let infrastructure fill them. Note the mismatch: Sid's "because of" (the fact that started the chain) is Young's *correlation* id. Young's *causation* id is the immediate parent. Sid's envelope has the first and not the second.
- (9) REPORTED: whole-stream deletion, truncation from the front, private and public streams, or forget the key. Event Store's documentation adds that even a deleted stream "retains one event within the stream to indicate the stream's existence", and warns about this for sensitive data.
- (12) REPORTED: no software version in the envelope; instead record the computed result at write time.
- (7) Looked for and not found: nothing in the book, the threads, or the documentation says rejected commands are stored. A wrong expected version is an error returned to the caller and leaves no record.
- (11 when) Looked for and not found: no verifiable Young quote against ordering by timestamp.

**5. Resembles and differs.** Resembles: never-rewrite with audit as the point; many writers; optimistic concurrency by expected version is Sid's offer almost exactly; years of lived experience. Differs: events are coarse business facts inside one team's bounded context, written by that team's code. Sid's facts are small, written by anyone, in one shared store, with grammars that are themselves facts. Young's "stream" is a much larger unit of order than Sid's cell.

**6. Disagreements.** With Martin Fowler's 2005 framing, which treats retroactive correction as a first-class feature; Young treats rewriting as the nuclear option. With the GDPR lawyers, via Verraes, on whether forgetting a key is deletion. No hostile dispute with Helland, whom he cites with approval.

### 4. Martin Kleppmann

**1. What he built and chose.** At LinkedIn he worked on Kafka and Samza and argued for "turning the database inside out": one append-only log, everything else derived. He then spent a decade on the opposite end: local-first software, Automerge, and the AT Protocol behind Bluesky. He is this camp's internal dissenter, and his own path is the evidence.

**2. His reasons, in his words.**
- The original thesis: "A materialized view is just a cached subset of the log, and you could rebuild it from the log at any time." (*Turning the database inside-out*, 2015.)
- The gate, in his terms: "Going through a leader would still be useful if you want to validate that writes meet certain constraints before writing them to the log." (same.)
- Offer, then verdict, both recorded. A username claim "doesn't yet guarantee uniqueness; it merely establishes an ordering of claims. (If you're using a partitioned stream like a Kafka topic, you need to ensure that all claims to the same username go to the same partition.)" Then a processor "writes the outcome ("successfully registered" or "username already taken") to a separate "registrations" event stream." (*Making Sense of Stream Processing*, 2016.) Again in 2021: "an initial event represents only the intention to perform a certain action; then a stream processor joins that event with the current state to determine whether the action is permitted, and if so, emits a new event to a stream of validated events". (*Thinking in Events*, DEBS 2021.)
- His own definition of a log allows discarding: "Besides appending, the log may allow old events to be discarded". (*Online Event Processing*, 2019.)
- Causation: "The original event ID is included in all of these generated events so that their origin can be traced." (same.)
- The cost of one order: "appending an event to the log requires at waiting least one network round-trip to the leader and/or a quorum of replicas." When writers may be disconnected, "the assumption of a totally ordered log becomes impossible to satisfy". (DEBS 2021; the typo is his.)
- Clock-made total orders are not logs: "Timestamp ordering produces a totally ordered sequence of events, but it is not a log, because new events are not always appended to the end." (same.)
- Naming by content: "Let u be any update, encoded as a byte string. We then identify u by its hash H (u)". Why: counter-based ids work "only … with trusted nodes, since a Byzantine node can easily generate duplicate IDs." The costs: "The ID is therefore only known after the update has been encoded as a byte string", and "The downside of using hashes as IDs is that they require more space than other schemes." Record only direct parents: "Dependencies that can be reached transitively via other dependencies are not included in the set of predecessor hashes, which ensures that this set remains small". (*Making CRDTs Byzantine Fault Tolerant*, 2022.)
- Deletion: "If permanent deletion of records is required (e.g. to delete personal data in compliance with the GDPR right to be forgotten [62]), an immutable event log requires extra care. Proposed solutions include periodically rewriting the log to remove any records that need to be deleted, or encrypting personal data with a per-user key that can be deleted". (DEBS 2021.)

**3. What he changed.**
- The verdict on his own 2014 thesis: "the core issues I raised in Turning the database inside-out in 2014 [33] are still unsolved. Most application logic is still executed in a request-response model: when the application receives a request, it queries a database and returns the result as of that point in time, but the client cannot subscribe to be notified whenever the query result changes". (DEBS 2021.) That unsolved part is Sid's running answer.
- From one way to a taxonomy: "there is no one true way: all of the categories in the taxonomy have important use cases." (same.)
- On formats he moved from confidence to an open problem. 2016: "Raw events are so simple and obvious that a "schema migration" doesn't really make sense". 2021: "any data format changes must be both forward and backward compatible", with lenses (Project Cambria) as the hope. Cambria names the failure of the frozen-format road: "a record full of optional fields is hard to use".
- The local-first team's own reported failure: "CRDTs store all history, including character-by-character text edits. These pile up, but can't easily be truncated because it's impossible to know when someone might reconnect to your shared document after six months away". (*Local-first software*, 2019.)
- His shipped system deletes. A Bluesky repository holds everything a user did "minus any records they have explicitly deleted". He lists the opposite as a defect of Secure Scuttlebutt: "it is not possible to delete content once it has been posted".
- His shipped system pins pointers with both name and hash: a reference to a record in another repository "also includes its CID". Accounts have "an immutable, unique identifier", so a user can "change their handle without affecting their social graph."
- Automerge, which he co-designed: actor ids are random ("the reference library generates 128-bit random identifiers"); "Operation IDs are lamport timestamps"; a change "is identified by its change hash"; the wall clock is "optional"; the root object is "identified by the object ID with a null actor id and null counter".
- In a March 2026 interview about the second edition of his book: "it seems important to have interfaces that allow AI agents and humans to work safely together, with the database as common ground".

**4. Which questions he speaks to.**
- (1), (2), (6) REPORTED: random opaque ids for long-lived things (actors, accounts); content hashes for versions and changes; parents recorded on each change; pointers that carry name plus hash. This is the most complete shipped answer in the camp, and it is the one I lean on for question 1.
- (7) REPORTED: both outcomes recorded, in a separate stream.
- (10) REPORTED: all claims on one thing go to one partition; an event touching two entities breaks per-partition processing; a worldwide total order costs a round trip per write. His one real example of a single worldwide ordered store is low-rate: "The New York Times maintains all textual content published since the newspaper's founding in 1851 in a single log partition".
- (5) REPORTED: a subscriber "periodically checkpoints the latest LSN it has processed", and the approach "does not provide isolation for read requests that are sent directly to data stores". He names the lagging-index read as an open problem.
- (9) REPORTED: rewriting the log is a legitimate option; so is forgetting a key; downstream copies are cleaned by deleting from the log and replaying.
- (11 when) REPORTED: wall-clock time is optional metadata and orders nothing.
- (17) REPORTED: a well-known null id for the root, the same in every replica, written by nobody.
- (11 based-on) Unverified: my memory is that his book has a section on treating reads as events, for tracing what a person saw before deciding, with a warning about storage cost. Third-party book notes confirm the idea is there. I could not verify any wording and do not quote it.

**5. Resembles and differs.** Resembles: he thinks about humans and agents sharing one data substrate, about provenance, about strangers. Differs: his later systems give each person their own primary copy, which is the opposite of one truth, and they accept merges where Sid has a gate.

**6. Disagreements.** With dual writes. With pure append-only designs that cannot delete. With his own earlier single-log position, on cost. Implicitly with Sid's premise: local-first holds that "we treat the copy of the data on your local device … as the primary copy."

### 5. Certificate Transparency, Trillian, and their descendants

**1. What they built and chose.** Since 2013, every publicly trusted web certificate is written to append-only Merkle-tree logs that anyone may read and check. One sequencer per log. Many independent logs, with no order across them. An entry has two names: the hash of its content and its index. The log's own clock stamps everything.

**2. The reasons, in their words.**
- The promise: "A log is a single, ever-growing, append-only Merkle Tree". (RFC 6962.) "Once an entry has been accepted by the log, it can never be removed or changed." (Eijdenberg, Laurie, Cutter, *Verifiable Data Structures*, 2015.)
- Never lost is an operations problem: "the log absolutely cannot afford to lose the certificate once it has issued an SCT." (Ben Laurie, *Certificate Transparency*, ACM Queue 2014.)
- Why the receipt was only a promise: waiting for a real position was the first design, but "CAs found this delay in their issuance pipelines to be unacceptable." So: "This signed timestamp is a promise for future inclusion in the log." (Laurie.)
- What may enter: "In order to avoid logs being spammed into uselessness, it is required that each chain is rooted in a known CA certificate." (RFC 6962.) Laurie: "logs are useful only if their size is manageable."
- Clock rules: the tree-head "timestamp MUST be at least as recent as the most recent SCT timestamp in the tree. Each subsequent timestamp MUST be more recent than the timestamp of the previous update." "TLS clients MUST reject SCTs whose timestamp is in the future." (RFC 6962.)
- Retry: "If the log has previously seen the certificate, it MAY return the same SCT as it returned before." (RFC 6962.)
- Genesis: "The hash of an empty list is the hash of an empty string". A log is identified by "the SHA-256 hash of the log's public key". The list of logs a browser accepts lives outside every log, in browser policy. (RFC 6962; Chrome CT policy.)
- The untrusted operator: "It can't remove an observed record without detection." (Russ Cox, *Transparent Logs for Skeptical Clients*, 2019.)

**3. What they changed, regretted, or abandoned.** A decade of operations, written down plainly.
- *A verifiable log cannot repair itself.* Chrome's CT team, July 2021, on DigiCert's Yeti2022: "The issue was tracked down to a bit flip in a single leaf hash". "Because the bit flip occurred in the leaf hash itself, this unfortunately is a non-recoverable error for this CT Log. Fixing this entry would require generating a SHA-2 preimage for the bit-flipped leaf hash". "Bit flips such as this can occur in extremely rare circumstances due to hardware faults or even cosmic rays." Andrew Ayer: "There is no way for the log operator to fix this problem". "Yeti 2022 is toast."
- *A restore from backup is a rollback, and a rollback is a fork.* Ayer: "In 2017, a log failed because it violated the append-only property after its database was rolled back during a botched backup restore."
- *The promise-now, include-later design failed in operation.* Google's Aviator log in 2016 blew through its 24-hour merge window. Let's Encrypt, 2025: "there have been multiple incidents in which important logs have exceeded their maximum merge delay, breaking that promise." Sunlight removed the gap: entries are sequenced before the receipt is returned, so "the effective merge delay is zero!"
- *The reserved slot.* "RFC 6962 specifies no extensions, and current logs produce empty extensions fields." The slot was opaque but already inside the signed message. Sunlight "takes advantage of the lack of merge delay to embed the leaf index in an SCT extension". "The extension costs just 8 bytes." Auditors can now fetch "by index, rather than by hash", which removed the hash-lookup database. The new interface is described as "an alternative encoding format for the same data": the serving shape changed; the record and its signature did not.
- *The clean version-two format died.* Andrew Ayer, 2024: "All CT logs and consumers implement a version of CT that is defined in RFC 6962. There are no plans to adopt RFC 9162."
- *Storage.* Let's Encrypt: "Annual cloud costs for our logs are approaching seven figures. The biggest contributor to this is that the data is stored in a relational database." "we previously had a test log fail when we ran into a 16 TiB limit in MySQL." "The Static CT API has proven to be operationally better and more scalable than the RFC 6962 design in almost every way."
- *Logs end.* Chrome policy: "new CT logs must be temporally sharded", and a log is removed once its date range has passed.
- *One small thing must never roll back; the rest may.* Sunlight keeps a tiny global checkpoint table that "must never be changed or modified". Its duplicate cache is the opposite: "This part of Sunlight can tolerate data loss".
- *Duplicate suppression is an optimization.* "this cache can be best-effort and lossy: if some entries were lost, the log can just accept a few duplicates".
- *Removal was proposed and refused.* Ryan Sleevi on name redaction, 2016: allowing it freely "is non-viable, precisely because it would allow a CA to redact domains to a degree that prevents detection of misissuance." The ecosystem's answer to privacy law is to keep personal data out. The Linux Foundation's notice for Sigstore's log: "You MAY NOT include any personal data about an individual other than yourself", and what you submit is "maintained indefinitely".
- *Refusals leave no trace.* Chrome policy: "Rejected logging submissions must not be issued an SCT by the CT log." The only record of a refusal is the error sent back.
- *The map was deleted.* Trillian's changelog, 2021: "Removed the experimental map API." Trillian itself is now in maintenance mode, succeeded by tile-based Tessera. I found no written reasons for the map's removal.
- *Who really verifies.* Eric Rescorla, 2023: "no browser ever implemented gossip". "CT provides fairly limited public verifiability." Chrome's auditing "really depends on trusting Google". His verdict: "what we actually have is a countersignature scheme and that the Merkle tree machinery is unnecessary overhead". "The problem is that it's expensive futureproofing, both in terms of protocol complexity and in terms of operational brittleness." The live repair is witness cosigning: independent parties check a checkpoint against what they saw before and countersign it.

**4. Which questions this speaks to.**
- (0) INSTITUTIONAL: they promise all four things (never lost, never different, never re-ordered, provable), and their incident record is the price list for the fourth.
- (1) REPORTED: two names. The hash is what a client already holds; the index is what the tree needs. Putting the index in the receipt removed a database.
- (2) REPORTED: resubmission may return the same receipt; exact duplicate suppression is not a correctness property.
- (7) REPORTED: refusals are not recorded, on purpose, to control spam and growth.
- (9) INSTITUTIONAL: removal is impossible by design, so sensitive content is kept out, or only a hash is logged (the Go checksum database logs hashes of modules, not modules).
- (11 when) REPORTED: the log's clock, forced monotone, never trusted from the future.
- (13) INSTITUTIONAL: a reserved, signed-over extension slot; one canonical encoding fixed forever; sharding by time so whole logs can retire; a restore is a fork unless the head is protected outside the backup.
- (17) REPORTED: every log starts from the same empty state; identity is a key; the trust list lives outside.
- (10), second store. INSTITUTIONAL: many independent logs with no order across them is the design, not an accident. Safety comes from the client's rule ("At least 2-5 SCTs … from logs that were approved"), a quorum over independent stores.

**5. Resembles and differs.** Resembles: planet scale, strangers, permanence, an economy resting on it, one gate per log, "the map must not lie". Differs: CT entries are large, public, self-authenticating certificates with no personal layers and no supersession. Nothing in CT reads the log to compute an answer under time pressure. And CT is many stores by design, where Sid wants one.

**6. Disagreements.** Rescorla against the CT designers, on whether the Merkle machinery earns its cost. The redaction fight (certificate authorities wanted it; browser vendors refused). Scalog's point about deciding metadata before persistence is the same lesson CT learned with merge delay, from a different direction.

### 6. Lamport, as foundation

One paper, three sentences that everything above rests on (*Time, Clocks, and the Ordering of Events in a Distributed System*, 1978).

- "The relation "happened before" is therefore only a partial ordering of the events in the system."
- Any total order made from it is a choice: "It is only the partial ordering which is uniquely determined by the system of events."
- The anomaly and its first cure. Someone issues request A, then phones a friend who issues B elsewhere; B may be ordered first, because "that precedence information is based on messages external to the system." The first remedy: "the person issuing request A could receive the timestamp TA of that request from the system. When issuing request B, his friend could specify that B be given a timestamp later than TA. This gives the user the responsibility for avoiding anomalous behavior." The second remedy is synchronized physical clocks.

INFERRED: Sid's based-on is Lamport's first remedy. A write that names what it read tells the system what came before it, across partitions, without any clock. So based-on is doing two jobs: provenance, and the only true cross-partition order the store will ever have.

### 7. Phil Bernstein's Hyder

**1. What they built and chose.** A database with no partitioning, where one shared log is everything. Each server runs a transaction against a snapshot and appends an "intention": its writes, plus its reads when the isolation level needs them. Every server then rolls the log forward through the same deterministic procedure, meld, which decides commit or abort. Compare Sid's gate: Hyder has no gate. The append is the only arbitration, and the verdict is a pure function of the log.

**2. The reasons, in their words.**
- "The log is the database." (Bernstein, Reid, Das, *Hyder*, CIDR 2011.)
- "Unlike conventional database systems, appending an intention to the log does not commit the transaction. Meld makes that decision." (Bernstein, Reid, Wu, Yuan, *Optimistic Concurrency Control by Melding Trees*, VLDB 2011.)
- What a read stood on is one position: the intention "contains a reference R to T's snapshot, which is the last committed transaction in the log that contributed to the database state that T read". (CIDR 2011.)
- "Since all servers (including T's executer) read the same log, they all make the same commit/abort decision regarding T." And: "The only point of arbitration between servers is the atomic append of an intention to the log." (CIDR 2011.)
- Retry: "AppendStripe is atomic. It is also idempotent, so a caller that fails to receive a reply from an AppendStripe can simply reissue the operation." (CIDR 2011.)
- Refused work stays in the log: "log records include the updates of both committed and aborted transactions." (CIDR 2011.)
- Reads cost nothing, because they are not recorded: "Since queries execute against snapshots, they are not logged or melded. Hence, they scale out linearly". (SIGMOD 2015.)

**3. What they changed or conceded.**
- "Meld is inherently a sequential algorithm. Although parts of it can be parallelized, it ultimately must process log records in log sequence." (VLDB 2011.)
- "the longer it takes to meld a transaction's intention, the greater the number of intentions in each transaction's conflict zone and hence the greater the chance that each transaction aborts." (VLDB 2011.)
- Four years later: "meld has been the bottleneck that limits transaction throughput." The algorithm "is already heavily optimized, and we have been unable to improve it." (SIGMOD 2015.)
- The log is not kept. Live nodes "can be copied to the end of the log by a copier transaction, thereby freeing up the segment for reuse." And: "we found GC to be detrimental to performance."
- The way out they proposed was more than one certifier and more than one log (Bernstein and Das, *IEEE Data Engineering Bulletin*, 2015). The rule for what must share a log: "There is a total order between transactions accessing the same partitions, which is preserved in all logs where both transactions appear." Across logs: "Notice that LSNs in different logs are incomparable". "two log entries have a defined order only if they accessed the same partition." Then: "It remains as future work to implement the algorithm proposed here and compare it to Tango's." I found no sign it was built, and no retrospective by Bernstein.

**4. Which questions this speaks to.**
- (7) REPORTED: the verdict is derived and never written, and refused intentions stay in the log forever. Bernstein and Das state the contrast with Tango themselves: Tango "rolls forward the log to determine T's commit/abort decision and then writes that decision to the log." INFERRED: deriving works only while every judge runs identical code forever. Sid's runtime will be rebuilt hundreds of times, so Sid is in Tango's case, not Hyder's.
- (10) REPORTED: their own answer to "can there be two certifiers" is yes, if everything that must be mutually ordered passes through a shared log. Across logs, positions cannot be compared, and an "as of" becomes a pair or a vector.
- (11 based-on), (5) REPORTED: an intention carries one snapshot position for everything it read. INFERRED: that is the compact form of a read set, and Sid's "pattern read as of a point" is the same idea.
- (10, machine rate) INFERRED from their abort-rate finding. The longer an offer travels before the gate sees it, the more often its expected version is stale. With agents writing at machine rate into a worldwide store, a hot cell will refuse most offers. The cure in this camp is never a faster gate. It is grammar design: many small cells that do not contend, rather than one cell holding a collection. Helland says the same from the other side: "Storage systems alone cannot provide the commutativity we need".
- (11 when) REPORTED by absence: no clock appears anywhere in the three Hyder papers except in the bibliography. Position is the only order.

**5. Resembles and differs.** Resembles: optimistic writes checked against what was read; one log; every reader computes the same state. Differs: a research system on special hardware inside one data centre; the log is a redo log and is garbage-collected; no strangers, no attribution, no deletion problem.

**6. Disagreements.** With primary-copy replication ("Hyder does not use a primary copy!"). With shared-lock designs such as Oracle RAC. With Tango, on writing decisions down and on one log versus several. CORFU's authors returned the criticism, on hole handling.

### 8. Amazon Aurora, and what AWS did next

**1. What they built and chose.** One writer instance and up to fifteen read replicas over a shared, six-way replicated redo log. The instance ships only log records. Storage nodes build pages from the log and garbage-collect the log behind the readers.

**2. The reasons, in their words.**
- "THE LOG IS THE DATABASE" is a section heading in the 2017 paper. Werner Vogels in 2019: "In Amazon Aurora, the log is the database. Database instances write redo log records to the distributed storage layer, and the storage takes care of constructing page images from log records on demand."
- Order from one counter: each record has a "Log Sequence Number (LSN) that is a monotonically increasing value generated by the database." (SIGMOD 2017.)
- Why one writer: "Aurora provides all these isolation levels by making a simplifying assumption that at any time there is only a single writer generating log updates with LSNs allocated from a single ordered domain." (SIGMOD 2017.) "A single writer has local state for all writes and can easily coordinate snapshot isolation, consistency points for storage, transaction ordering, and structural atomicity. It is more complex for replicas." (SIGMOD 2018.)
- Lagging readers: a replica "typically lags behind the writer by a short interval (20 ms or less)", and "replica read views must lag durability consistency points at the writer instance."
- The log is not kept: storage nodes advance pages "by coalescing the older log records and then safely garbage collecting them." Durability beyond that lives elsewhere: the storage layer "continuously and transparently backs up redo log streams to Amazon S3."
- The one rewrite, and how it is made safe. On recovery the database records "a truncation range that annuls any log records beyond the newly computed VCL". "The truncation ranges are versioned with epoch numbers, and written durably to the storage service". A deposed writer is fenced: "Aurora, rather than waiting for a lease to expire, just changes the locks on the door."

**3. What they changed or retired.**
- *Multi-master* (2019) let several instances write one volume. AWS's own announcement page now carries the line: "9/8/23: Multi-Master is no longer available as of Feb 28, 2023". Its unit of conflict was the 16 KiB page: "Aurora detects write conflicts at the level of the physical data pages", so unrelated rows could conflict, and "when conflicts occur, they incur substantial overhead." AWS published no reason for the retirement, and I draw none.
- *Aurora DSQL* (2024 on) drops the single writer. "Each key in the database belongs to at most one adjudicator at any given time". "Each transaction's writes go atomically to a single journal, but we can have as many journals as we need." A total order is built per consumer: crossbars "merge-sort them into a total order for each subscriber shard." The journal holds only accepted work: "What goes on the Journal isn't requests for transactions, but committed transactions." A lagging index is never read: if a storage node is not current up to the transaction's start time, "the reader is made to wait until replication catches up." History is short by design: old row versions are forgotten after a fixed time, currently "five minutes before the current wall-clock time." Marc Brooker's stated reason for the redesign: "application programmers find dealing with eventual consistency difficult". On their clocks: "clock skew beyond the expected bounds causes the system to lose linearizability, but not isolation, durability, or atomicity."

**4. Which questions this speaks to.**
- (10) INSTITUTIONAL: one writer bought simplicity and no consensus protocol. When AWS needed more than one decider, they sharded the deciders by key, so that each key has exactly one. They did not let two deciders share a key again. That is this camp's consistent answer to "can two gates write one layer": yes, and never one cell.
- (5) REPORTED: two honest designs. Aurora replicas read as of wherever they have got to. DSQL picks the as-of first and makes a lagging reader wait. Neither ever reports an as-of that the index had not reached.
- (0), (13) REPORTED: a redo log is garbage-collected once pages exist and a backup is cut. Even the "rewrite" of the ragged tail is an appended, epoch-stamped record.
- (7) REPORTED: Aurora derives commit from a watermark. DSQL logs only accepted work. Refusals leave no record in either.
- (12) INFERRED: both fence a deposed writer with an epoch number. A verdict that names the gate's epoch would let Sid tell, years later, which incarnation of the gate admitted a fact.
- (4) INFERRED from DSQL's five-minute horizon and Aurora Backtrack's limit ("The limit for a backtrack window is 72 hours."): reading an index as of a point far in the past is something these systems refuse to do. Sid's "what was I looking at yesterday" cannot lean on an index answering old as-of reads unless that index is built to keep history. Years later the cheap question is "has anything matching this pattern landed since that frontier?". The expensive one is "what exactly did the pattern return then?".

**5. Resembles and differs.** Resembles: one writer stamping positions; readers that lag; "the log is the database" in so many words. Differs: a redo log for pages, garbage-collected, with no meaning at the record level, one tenant per volume, and a history horizon measured in minutes or hours.

**6. Disagreements.** With Hyder and its cousins, on where to cut: "Aurora decouples storage at a level lower than that of Deuteronomy, Hyder, Sinfonia, and Yesquel." DSQL sets itself against Paxos-per-shard designs: its replication "is significantly different to DynamoDB [13], Spanner [8], or CockroachDB [30]".

### 9. Two short notes on voices the brief did not list

**Jay Kreps (Kafka).** The strongest plain statement that position is the clock. "The log entry number can be thought of as the "timestamp" of the entry", which has "the convenient property that it is decoupled from any particular physical clock." A replica's whole state is one number per log: "you can describe each replica by a single number, the timestamp for the maximum log entry it has processed." He refused a global order: "Each partition is a totally ordered log, but there is no global ordering between partitions", and defended the refusal: "it is not meaningful to talk about a total order over their behavior." He is honest about what compaction costs: "we can no longer recreate all previous states of the source system, only the more recent ones." In 2017 he accepted keeping data forever, with a warning: "the bar for both software correctness and operational practices increases quite dramatically". Against Marz's Lambda architecture: "I don't think this problem is fixable." (*The Log*, 2013; *Questioning the Lambda Architecture*, 2014; *It's Okay To Store Data In Apache Kafka*, 2017.)

**Nathan Marz (Storm, Lambda, Rama).** He built the substrate, and his fact model is close to Sid's. "data is inherently time based. A piece of data is a fact that you know to be true at some moment of time." "CRUD has become CR." His reason for immutability is human error: "writing bad data does not override or otherwise destroy good data." He rejects keep-the-last-N as a substitute: "once the database compacts the row, the old value is gone." He admits deletion exists: "There are a few cases where you do want to permanently delete data, such as regulations requiring you to purge data after a certain amount of time." (*How to beat the CAP theorem*, 2011.) Red Planet Labs' slogan for the same idea: "no fact once learned is ever lost to time".

What Rama's own documentation says, checked in the saved pages:
- "By default, depots permanently store all data appended to them." Trimming is an option.
- "Depot migrations never change the offsets of records. If a record is excised, a small tombstone value is written in its place. Topologies and foreign depot reads will exclude those tombstones when reading from the depot."
- "Appends also store and index in the depot partition the time of the append." So Rama already keeps an append time per partition. If the gate's "when" is a separate stamp, the store holds two clocks for one event.
- Order: "there are no ordering guarantees across different depots", and "Rama guarantees local ordering."
- The caveat that matters for ids: "If you do depot appends as part of your microbatch topology to either the same module or other modules, those currently do not have exactly-once semantics in the face of failures and retries. However, this is on our roadmap."

What Marz changed: in 2011 he argued for recomputing everything from the log. Rama maintains its PStates incrementally and keeps full recompute as the recovery road, to "fix corruption caused by a bug in your ETL code". I found no essay where he says this in so many words; the change is visible in what he built.

---

## Question by question: what this camp would say

The numbers are Sid's. For each: what the sources say, then what it means for the first record. The second part is always my inference unless marked.

### (0) The log itself: never rewritten, or only never lost?

The camp would first split the question. There are four promises, and sources differ on which they make.

1. **Never lost.** This is operations and money. Laurie: "the log absolutely cannot afford to lose the certificate once it has issued an SCT." Helland: "Increased fidelity of memories implies increased cost!"
2. **Never different.** The same id never returns different content. Absent is allowed. Helland: "it will never return data other than the original contents." Young: "Immutability is immutable. The moment you allow a single edit, everything becomes suspect."
3. **Never re-ordered or re-numbered.** Rama keeps offsets through a migration. Young shows why it matters: after a live copy-and-replace, a read model rebuilt to the same position may differ from the one built the first time, because the two "have seen different versions of history".
4. **Provable to strangers.** Only CT promises this. The price is that the log can never repair itself (Yeti2022), can never be restored from a backup (the 2017 failure), and can never remove anything.

Who keeps the log forever depends on what the log holds. Redo logs are trimmed: Tango's forget call, Aurora's garbage collection, Delos ("continuously being trimmed or moved to backup storage"), Kafka by default, Hyder's copier. Record logs are kept: CT, event stores, ledgers. Sid's store is a record log.

Even the keepers bend at the physical level. Helland says it outright: "semantically immutable but can be physically changed." Delos edited written entries to survive a poison record. Kleppmann lists "periodically rewriting the log" as a legitimate way to meet privacy law. Young regrets that most teams end up rewriting whole stores. The Overeem study's vendors refuse to offer in-place edits at all, and one team zipped and encoded its events just to make editing hard.

**For the first record (INFERRED).** Promise 1, 2 and 3. Reserve the right to change bytes without changing meaning, which is what a Rama depot migration is when used well. Make "meaning unchanged" checkable, which needs one canonical byte form and a hash from the first record; a hash added later proves nothing about the years before it. Keep promise 4 possible: a Merkle tree is a derived index over ordered, byte-stable entries, so it can be built later if and only if those two properties held from the start. Write down the one exception class (erasure, and neutralizing a poison record) and make each use of it a fact.

### (2) Entity: how is an id made so two never clash? May an id give away when or where it was made?

REPORTED. Helland: unique "within the spatial and temporal bounds of its use", never reused, and a central authority raises the question "Does this authority scale?". Kleppmann: ids built from a node id and a counter work "only … with trusted nodes". Automerge mints "128-bit random identifiers". The AT Protocol gives every account "an immutable, unique identifier" so that the human-readable handle can change. Event Store takes a client-made id and scopes its duplicate check: "The idempotence check is based on the EventId and stream."

Nobody in this camp writes about ids leaking their origin. INFERRED from the deletion material: ids are what survive erasure. Event Store warns that a deleted stream still "retains one event … to indicate the stream's existence". Verraes warns that a person "can be identified … through the associations and relations with other information." An id that embeds a time, a machine or an actor keeps telling that story after the value is gone. Since the gate stamps "when" and "by whom" separately, the id has no need to carry either.

**For the first record (INFERRED).** Random, opaque, client-minted, never reused. Decide the scope of the duplicate check on purpose (Event Store's is per stream, and its documentation admits "Idempotence is not guaranteed if you use ExpectedVersion.Any").

### (17) The first facts

REPORTED. Tango finds everything from one fixed name: "This directory is itself a Tango object with a hard-coded OID." Automerge's root is "identified by the object ID with a null actor id and null counter": the same in every replica, written by nobody. Every CT log begins from the same empty state ("The hash of an empty list is the hash of an empty string"); what differs is the log's key. Helland lets a record point at its schema as "another arc in the DAG".

What cannot be inside, REPORTED. Delos: the register that says which log is current must sit outside the log, because putting it inside "requires the Loglet itself to be highly available for writes". CT: the list of trusted logs lives in browser policy, outside every log.

**For the first record (INFERRED).** A short list of well-known ids fixed in the runtime, from which everything else is found. Genesis facts that are identical in every store, ids and content, so that two stores mean the same thing by the first kinds; a content hash makes "identical" checkable. A named genesis actor with a well-known id, or no actor at all, as in Automerge. And a plain list of what can never be a fact because it is needed to read the facts: the encoding and hash rules, the trust anchor, and the pointer that says where a store continues if it moves.

### (3) Key: a word, or an id with its name and shape as facts?

REPORTED. Young on names in a store that never forgets: "you are no longer allowed to rename something", and you "must carry it forward into the future." On meaning: "semantic meaning cannot change between versions of software." On versions: "If not, it is not a new version of the event but rather a new event." Event Store's documentation warns against tying the type to code: it "should be a “friendly” name rather than a CLR type name". The Overeem study: "The data schema is not explicitly defined at all, but is implicitly encoded" in the application. Helland: "all message schemas be versioned and that each message use the version-dependent identifier of the precise definition of the message format. Alternatively, the schema can be embedded in the message." The AT Protocol separates the permanent id from the changeable name.

**For the first record (INFERRED).** The lived regret is names that cannot be renamed and meanings that drift. The fix both Helland and the AT Protocol point to is a stable id, with the name as a changeable fact about it. Two rules go with it. A key's meaning never changes; a changed meaning is a new key (Young). Each fact, or its verdict, pins the grammar version it was checked against (Helland), because a grammar "with versions" means a bare key no longer says which shape applied. The cost: a raw log keyed by ids cannot be read without the dictionary. Identical genesis facts in every store are what make that dictionary portable.

### (9) Value: never removed, so how is one deleted, backups included?

The camp uses "delete" for three different things.
- **Retraction**: a new fact saying the old one is withdrawn. Nothing is removed. Helland: "Corrections can be made but only by making new entries in the ledger." Young prefers full reversals: "Always consider the auditor's perspective."
- **Erasure**: the content must truly go. REPORTED options: excise in place and leave a marker (Helland's "no present data"; Rama's tombstone); keep the erasable part in a second store (Verraes, and one system in the Overeem study, which serves "default values" once the data is gone); encrypt and forget the key (Young, Kleppmann, Dudycz); rewrite the log (Kleppmann; two systems in the Overeem study: "Obviously, this requires them to rewrite events."); or refuse to take the data at all (CT; the Linux Foundation notice).
- **Trimming** for space. A different matter; see (13).

Each erasure road has a named cost. The second store "breaks the concept of an Event Store as the Single Source of Truth" (Verraes). Forgetting a key reaches "all copies and backups", but "the law does not consider deleting the encryption key equal to actually deleting the data itself." Without it, backups mean "restoring and cleaning up backups and creating new ones with the cleaned-up data" (Dudycz). And after any of them, the person may still be findable through what points at them.

**For the first record (INFERRED).** Three things cannot be retrofitted. First, the value must be separable from the envelope, so that the value can go while the id, the edges and the verdict stay. Sid's "who read the erased thing" depends on exactly that. Second, nothing personal may sit in what stays: ids, keys, and above all "by whom". The actor must be an opaque id, with the link from that id to a person held as an erasable value. Third, if a content hash stays behind, it must not leak the value; a bare hash of a short value such as a name or a yes/no can be guessed, so the hash needs a random salt that is erased with the value. For backups, choose between short-lived backups that age out inside the legal deadline and forget-the-key, and know that lawyers dispute the second. One thing here favours Sid: the camp has no way to find derived copies of erased content, and Verraes can only broadcast a deletion event and hope. Walking based-on finds them.

### (8) By whom

This camp says little. CT does not record the submitter at all. In the AT Protocol the account signs the root of its own repository, so the signature is the "who". Helland: "Managing the requester's identity, the target's identity, and the identity of the work in question are some of the hardest problems in scalable systems that need idempotence", and across trust boundaries systems "provide an alias". Trillian's Claimant Model asks of any logged statement who claims it, who relies on it, who can check it, and who acts when it is false.

**For the first record (INFERRED).** "By whom" should be stamped by the gate from the authenticated channel, for the same reason "when" is: an offer's own claim about itself is not evidence. A signature by the actor is the stronger form and the only one that survives a move to a second store, but it is heavier. On a person's agent versus the person, and on where "acts for" lives, this camp has nothing. Look to the group that has Zanzibar.

### (11) When: whose clock? Ever used for order?

REPORTED, and close to unanimous. Order is position; the clock is a label. Kreps: the entry number is a timestamp "decoupled from any particular physical clock". Balakrishnan: a position "is effectively a logical timestamp". Tango sets itself against Spanner, which "uses real time as an ordering mechanism via synchronized clocks". Hyder never mentions a clock. Delos admits physical time only as a proposal that the log then orders. Automerge's wall-clock field is "optional". CT uses the log's own clock and forces it to be sane: a tree head's time "MUST be at least as recent as the most recent SCT timestamp in the tree", and clients reject timestamps "in the future". The exception is Aurora DSQL, which reads "as of" a physical time, and says plainly what breaks when the clock is wrong: linearizability, and nothing else. That road belongs to the Spanner group.

Marz adds a distinction the envelope should respect: "A piece of data is a fact that you know to be true at some moment of time." That moment belongs to the statement. The gate's "when" is the moment of admission. For ten million papers ingested in a week, the two differ by decades.

**For the first record (INFERRED).** The gate's clock stamps "when", and nothing ever orders by it. Make it monotone within a partition, as CT does, so it can never contradict position. Keep world-time in the value. Note that Rama already stores an append time per partition; decide whether the gate's "when" is that stamp or a second one.

### (16) Layer: who sees a fact before any permissions exist?

The camp is the wrong place to ask. CT is public by rule: "Log operators MUST NOT impose any conditions on retrieving or sharing data from the log." Tango, Delos, Hyder and Aurora have no readers they distrust. INFERRED, from how they bootstrap: at genesis there are no policy facts, so the rule that applies before any exist must live in the runtime, and the safe rule is that nobody sees anything until a policy fact says so. The first policy facts are genesis facts.

### (10) Order: two gates on one layer? What must share a partition? Is "as of" one number or many?

REPORTED. The unit of order is a keyed thing, never a namespace. Helland: "You know only when a single unique key unifies both." Young: the stream. Kleppmann: "all claims to the same username go to the same partition." Bernstein and Das: "two log entries have a defined order only if they accessed the same partition." DSQL: each key belongs "to at most one adjudicator at any given time".

So: two gates can write one layer, and with Rama they must, because a layer spans partitions and each partition has its own leader. What no source allows is two deciders for one key at one time. Aurora's multi-master tried a shared unit of conflict and was retired. Both Aurora and CORFU fence a deposed writer with an epoch.

What must share a partition: whatever one compare-and-set must see. For Sid that is the cell (entity, key, layer) at least. If anything else needs mutual order, such as a person's gestures in sequence, it must be keyed into one ordered unit on purpose; Young's advice is to put it in one stream or merge in the application. Helland's warning is that the choice is permanent in practice: "Earlier record versions were placed in their old log."

"As of": inside one partition, one number (Kreps). Across partitions, a vector: FuzzyLog's snapshot "acts as a vector timestamp"; Scalog's cut is "an integer vector"; Hyder's partitioned LSNs "are incomparable". One number for the whole store exists only when something orders the vectors. Scalog's ordering layer does that, and DSQL's crossbars build an order per consumer. A single global order has a cost nobody predicted: blast radius, where one lost slot stalls every reader (Balakrishnan, 2024).

**For the first record (INFERRED).** Read "the gate is the only writer" as "exactly one gate per partition at any moment, fenced by an epoch". Treat a cross-partition "as of" as a vector, and if a single name for it is wanted, make the vector a small fact of its own and point at that. Remember that the number of partitions and the partitioning function are part of what any position means. A pointer that depends on positions has baked the physical layout into the record; Delos virtualized its log addresses for exactly this reason.

### (11) Based on: is every read listed?

REPORTED. This camp records reads for three different purposes, and only one of them is Sid's.
- *Validation.* Tango's commit record lists every object read with its version, and the write is void if any moved. Hyder's intention carries its read dependencies. Balakrishnan states the general law: a stored output "can only be applied … if the intervening entries did not invalidate it".
- *How far the writer had seen.* FuzzyLog puts "the vector timestamp of nodes seen thus far in the new entry". Delos adds "its local playback position" to each proposal. One compact value, not a list.
- *Tracing origin.* Kleppmann's online event processing: "The original event ID is included in all of these generated events so that their origin can be traced."

On size: Kleppmann records direct parents only, "which ensures that this set remains small"; Tango writes "only one commit record per transaction" however many objects it touches; Hyder names a whole read set by one snapshot position. On whether recording a read changes anything: Helland says no: "The log record is not substantive". On reads of things outside the store: Young says copy the answer in, because the outside will change: "enrich the information returned from the call onto the event."

**For the first record (INFERRED).** Sid's based-on is provenance, a fourth purpose that nobody in this camp stores at scale, and the gate validates only one read (the cell's expected version). So each listed read should say who vouches for it: checked by the gate, observed by the runtime, or declared by the actor. A read that only made a tool fire is a cause, not a dependency, and belongs with because-of. To keep size down, use the camp's tricks: a pattern plus an as-of instead of a list; direct reads only; one frontier for "everything I could have known"; and one shared read record for the many facts that come out of one context. The crossing fact already is such a record: a model's offer can point at the single "what the model was given" fact rather than repeat its reads. For an anchor outside the store, record enough to survive the outside changing: the identifier, and a hash or copy of what was read.

### (4) Based on: depends-on or merely passed through? Follow the latest, or stay on the version read?

REPORTED. Pin, always. Helland: the pointer "must specify data that is immutable", and the newspaper example shows why a pointer to "today's" anything is useless later. Tango's read set carries versions. The AT Protocol's pointer "also includes its CID". Helland even names the fork, as two delivery modes: history with no gaps, or currency, the latest with gaps allowed.

INFERRED. The asymmetry decides it. From a pinned pointer, "the latest" can always be computed by walking forward. From a pointer that says only "latest", what was actually read is gone forever. Following the latest is what a running answer does; a stored fact pins. Whether the fact depends on a read or merely passed through it is a judgment available only at write time, so it has to be written then. Tango gives the precedent of two classes: reads inside the read set, which void the write if they move, and everything else.

### (5) Based on: if the index was behind the log, is how far it had got written down?

REPORTED. The older generation simply tolerates lag. Helland: an index is "potentially out of sync with the entity itself"; "There may be things with identities that have not yet been indexed." Kleppmann calls the direct read of a lagging store an open problem: the approach "does not provide isolation for read requests that are sent directly to data stores". The newer systems put the position into the record or the read. Kreps: a replica is "a single number". Aurora: a read view is anchored to a point the replica has reached. DSQL: a lagging reader "is made to wait". Delos: each proposal carries "its local playback position". A CT proof is always against a stated tree size.

**For the first record (INFERRED).** This is less an extra field than the definition of the field. The as-of of a pattern read is the position the serving index had applied. It is never the log's tail and never the time of asking. Two honest designs exist: report where the index was, or name the as-of first and wait for the index to reach it. The dishonest one is to record the as-of you wanted while serving from an index that had not got there. One more consequence, from Aurora's and DSQL's short horizons: a pattern read recorded as "pattern plus as-of" is cheap to check for staleness later ("has anything matching landed since?") and expensive or impossible to re-run later ("what did it return?"). If a crossing must be reproducible, record at least a checksum of what was shown.

### (11) Because of: always filled; empty only when it starts a chain?

REPORTED. Young's rule is the practice everywhere: "every message has 3 ids", and a reply copies the conversation id and sets its cause to the id of the message it answers. Infrastructure fills them, not application code; Event Store's projections "honor both the correlationId and causationId patterns for any events it produces internally". Kleppmann's pipeline does the same with one id.

**For the first record (INFERRED).** Sid's because-of is "the fact whose landing started the chain". That is Young's correlation id. The immediate cause, Young's causation id, has no slot in the nine parts unless based-on marks which read was the trigger. The camp carries both, because they answer different questions: "show me the whole conversation" and "what caused what". Young uses the immediate cause to explain corrections. Filled always, and by the runtime.

### (1) Version: pointed at by the gate's number, or by its own id? Random, or computed from content?

REPORTED. Mature systems carry more than one name, and each name has a job.
- Event Store: a client-made event id for retry, a stream revision for order, a global position for reading everything.
- CT: the content hash is what a client already holds; the index is what the tree needs. Putting the index in the receipt removed a database.
- The AT Protocol: a pointer carries the record's name and its content hash.
- Automerge: random actor ids; operation ids that "are lamport timestamps"; each change "identified by its change hash".
- Position-only systems (Tango, Hyder, Aurora) work inside one log. Delos had to virtualize positions once one log became a chain of logs.
- Kleppmann on hashes: they cannot be forged by an untrusted writer, they are "only known after the update has been encoded", and "they require more space".
- Young on the failure: "two completely different events have the same identifier".
- Scalog: deciding the position before the record is safe is what creates holes.
- Rama: a topology's depot appends may repeat after a failure.

**For the first record (INFERRED).** Three names, three jobs. An own id minted before the gate, random and opaque, so that a retry is recognizably the same offer, so that a thing can be pointed at before it lands, and so that a pointer survives re-partitioning or a second store. The gate's number, for order and compare-and-set inside a cell, meaningful only in this store and this partition. A content hash over the canonical form, which turns "same id, never different content" from a promise into a check. Pointers (based-on, because-of, replaces) carry the own id plus the hash, and may carry the gate's number as a hint. The shipped systems suggest a split for random versus computed: long-lived identities are random and opaque (actors, accounts, entities), while versions and changes are named by content (change hashes, CIDs). Two cautions on naming a fact by its content. The hash has to cover the offer's parts and not the gate's, or it cannot exist before admission. And once a value is erased, a bare content hash of it leaks; see (9).

### (6) Version: when 37 replaces 25, is "replacing 25" kept on 37?

REPORTED. Helland: a new version "captures a replacement for or an augmentation of an earlier version", with "one parent and one child" when the store is linearizable and "many parents and/or many children" when it is not. Kleppmann's updates each carry "a set of predecessor hashes". Event Store leaves the predecessor implicit, because revisions inside a stream are consecutive.

**For the first record (INFERRED).** Keep it. In Sid's store the gate's numbers are not consecutive within a cell (37 follows 25), so the predecessor is not implicit; it would have to be looked up in an index, and indexes are derived. The offer already states the version it expects, so keeping it costs one pointer. It is the only evidence of what the writer believed it was replacing. With hash names it makes each cell's history a chain that can be checked. And a list of parents extends to the day two stores, or two layers, meet; a bare number cannot.

### (7) Beside each fact: is the gate's yes or no kept? Where? Are refusals kept?

REPORTED. Three shapes exist in this camp.
- *Log the offer, derive the verdict.* Hyder. Refused work stays in the log forever; the verdict is never written.
- *Log the offer, write the verdict.* Tango's decision records. Kleppmann's pattern, twice, with both outcomes going "to a separate "registrations" event stream."
- *Judge first, log only what passed.* Aurora, DSQL ("committed transactions"), Event Store, CT. A refusal is an error message and leaves no record. CT does this on purpose: "Rejected logging submissions must not be issued an SCT", because "logs are useful only if their size is manageable."

Helland supplies the requirement that cuts across all three: the receiver "must durably remember the transition", and on retry "the same reply must be returned." His bank example also bounds it: a check must clear "in less than one year", which "limits the list of cleared checks the bank must maintain".

**For the first record (INFERRED).** A yes is a fact, and should name everything it checked: the grammar version, the policy version, the expected version, plus the gate's epoch, the runtime version and the store. None of those can be recovered later. A no must live at least as long as a retry can arrive, keyed by the offer's id, so the same offer gets the same answer. Whether refusals live forever is a policy choice with a spam cost. Kleppmann's separate stream is the middle road, and Rama's depot trimming can apply to that stream without touching the fact log. A refusal that points at an offer nobody kept explains nothing, so decide whether refused offers are kept with their verdicts. Logging offers before judging them has one more benefit that matters to a runtime rebuilt hundreds of times: a new gate can be run against the old offers and its verdicts compared with the recorded ones.

### (12) Start of a session: is the runtime version and the kind of machine written down? Are rebuilds facts?

REPORTED. This is the best-evidenced answer in the report. Delos: "engine roll-out has been the only source of inconsistency in production so far, despite initial worries that developers would find it difficult to write deterministic code." The fix: new code is enabled "by sending a command via the log itself", so that its effects are visible "beyond a consistent log position". The safety net: "incremental checksums of the LocalStore". Balakrishnan in 2024: learners "are upgraded more frequently than acceptors; and hence more likely to fail." Young's answer to the same problem is different: do not record the software version; record the computed result, which "allows for deterministic replays". Kreps: "Code will always change."

**For the first record (INFERRED).** A running answer is re-derivable from the same reads only under the same tool version and the same runtime. Tools are facts, so their versions are already in the store. The runtime is the one thing that is not a fact, so its version has to be written wherever a re-derivation is promised: on each crossing and on each verdict. A change of runtime should land as a fact at a position, so that every reader can say which facts were admitted under which runtime. A crossing should carry a checksum of what was shown, so that a later re-derivation can be checked rather than trusted. The kind of machine matters only if it can change an answer (floating point, fonts, a model's weights); where it can, it is part of the version.

### (14) The hand, and (15) a click

The camp has little here, and I would not lean on it. What it offers is about grain. Kleppmann in 2015: a shopper adding and then removing an item has "information value". His team in 2019: "CRDTs store all history, including character-by-character text edits. These pile up", and they could not be trimmed. Young: events are named for what happened in the domain, and "do not use the word “And” in an event name". INFERRED: record acts at the grain of intent, not motion samples; and if motion is kept at all, keep it where trimming is allowed (a session layer, a separate stream). On being shown versus looking, the Claimant Model helps: the runtime can vouch for "shown"; only the person, or an instrument, can vouch for "looked". They are two claims by two actors. On which tools may act in a person's name, this camp has nothing beyond Kleppmann's 2026 remark that an interface "defines which buttons the AI is allowed to press."

### (13) Storage: plain maps or classes? Ever trimmed? Backups?

REPORTED on format. Delos moved from a positional layout to "a map of headers" after the positional one proved "brittle against stack upgrades". Young chooses weak schema and states its three mapping rules. Event Store warns against code type names. The Overeem study reports stacks of upcasters slowing every load. Cambria names the opposite failure: "a record full of optional fields is hard to use". CT fixed one canonical encoding forever, left one opaque extension slot inside the signature, and eleven years later that slot carried the redesign, while the clean second format was never adopted.

REPORTED on trimming. Nobody who keeps a record log trims it. Everybody trims or tiers something: Tango's forget call, Aurora's garbage collection, CT's time-sharded logs that retire whole, Delos's cold-storage loglet, Young's year-end close. Kreps on what compaction destroys: "we can no longer recreate all previous states". Marz on why keeping the last few versions is not enough. Rama: depots keep everything by default, and trimming is a setting.

REPORTED on backups. For an append-only store a restore is a rollback, and a rollback broke a CT log. Sunlight's rule is the clean statement: one tiny record of the latest head "must never be changed or modified", while the duplicate cache "can tolerate data loss". Delos gets point-in-time restore by tiering old segments. Helland's line applies to all of it: fidelity of memory is bought.

**For the first record (INFERRED).** Plain maps of named parts, not positional tuples and not language classes. An envelope version. One extension slot that is inside whatever is hashed from the first day; an extension added outside the hash later cannot be trusted the same way. One canonical byte form for hashing, independent of how Rama stores the record. Never trim the fact log; do trim or tier refusals, session-grain motion, and indexes. For backups, decide which one small thing must never roll back (each partition's head: position and hash), keep it outside the backup domain, and accept that after a restore some acknowledged facts are gone. Client-minted ids and an idempotent gate let clients offer them again. Helland predicted the situation: "Your partner may experience amnesia".

---

## For the group

### 7. The voices that matter most for Sid, who I dropped, who is missing

**Mahesh Balakrishnan matters most.** He is the only person here who ran "one log is the interface to everything" in production for years and then wrote down what went wrong. His corrections land directly on Sid's table: reads carried in the record, verdicts written into the log, how far the writer had seen as an envelope field, code upgrades through the log, the map-shaped entry, the surgical edit of a poison record, the blast radius of one global order. His systems differ from Sid's in one large way, which is that they are redo logs and so they trim, and that difference is easy to keep in view.

**Greg Young, read together with the Overeem study, matters second.** They lived longest with never-rewrite at the level where Sid's facts live: names, shapes, meanings, deletions, rebuilds. Young is also the person who warns against the whole premise, and names his own regret.

**The Certificate Transparency operators matter third:** Laurie for the design reasons, then Ayer, Valsorda, Let's Encrypt and Rescorla for the decade after. It is the only append-only log at planet scale that strangers check, and its incident record prices each promise. The reserved extension slot is the single most transferable trick in this report.

**Helland is the frame rather than a fourth voice.** Pin your pointers; absent is allowed and different is not; only one key shares an order; every request carries an id. He has few operational regrets to offer, because he writes principles, not systems.

**Kleppmann is the internal dissenter.** His path from one log to many, and his shipped naming scheme (random ids for identities, hashes for versions, pointers that carry both), are what I would take from him.

**Dropped to light treatment.** Aurora and Hyder: both are redo logs, and they transfer on one question each (one writer; derived verdicts and abort rates). Lamport: by the brief. Trillian as software: its lessons are CT's lessons.

**Missing.** Rich Hickey and Datomic: the closest existing system to Sid's (small facts, one transactor, time travel, excision). Probably with another group; if not, it is the largest gap. Nathan Marz: he built the substrate and his 2011 fact model is nearly Sid's; treated briefly above, and I could not reach his book's chapter on facts. Jay Kreps: brief above. Git and Fossil: content-named history that people have lived with for twenty years, including Fossil's "shunning" as a deletion mechanism; not gathered. The provenance research community (W3C PROV; why-provenance and where-provenance in databases): based-on is lineage, and this camp has almost no literature on storing lineage at scale; if another group has Alvaro, they may cover it. Accountants: Young's year-end close is the only piece here of the oldest append-only practice of all. Blockchains: planet-scale, stranger-verified, never-delete systems with their own decade of regrets; not in anyone's brief as far as I know.

### 8. What this camp would question above the table

**An append-only store of small facts as the one substance for everything.** Split verdict. For: Helland's outside data is exactly this, Marz builds whole backends this way, and Kleppmann in 2026 wants "the database as common ground" for humans and agents. Against: the InfoQ report of Young's retrospective calls a whole system on event sourcing an "anti-pattern"; Kleppmann says "there is no one true way"; Kreps calls it "hubris to think you can do better than all of them in a single system". Helland's own split is between inside data (private, mutable, schema'd by one owner) and outside data (immutable, identified, shared). Sid's design makes all stored data outside data and leaves the inside as the unstored running answer. That is coherent, and nobody in this camp has run it. The transfer from Young is imperfect: his teams paid event sourcing's costs in places that did not need an audit trail, and for Sid the trail is the product. The costs are still real.

**Tools, grammars, policies and definitions as facts in the same store.** The camp would mostly agree. Tango keeps its directory as a Tango object. Helland points at schemas with "another arc in the DAG". They would insist that something always sits outside: Delos's MetaStore, CT's log list, the encoding rules. The brief says the one thing not made of facts is the runtime. This camp would add the trust anchor, the successor pointer, and the byte-level encoding, because all three are needed before any fact can be read.

**One store for the planet, with personal layers.** The sharpest internal dissent. Kleppmann: a total order costs "one network round-trip to the leader and/or a quorum of replicas" per append, and his local-first work makes the device the primary copy. FuzzyLog: a system-wide order is "expensive, often impossible, and typically unnecessary". Helland: "There is no simultaneity at a distance", and "you can know where you are writing or you can know when the write will complete but not both." CT chose many logs on purpose, so that no single operator is trusted, and Rescorla shows that even so, real verification came to rest on one company. Two separate points hide here. One store is not one order: with Rama's partitions Sid already has many orders, which answers the throughput half. The other half stays open. With no optimism allowed, every act waits for a round trip to wherever its partition's leader lives, and a single operator asks the planet for the trust that CT was built to avoid asking for.

**A fixed nine-part envelope both ends share forever.** The camp would say that fixing the parts is the wrong thing to fix. CT froze a format and survived only because of one opaque slot; its clean second version was never adopted. Delos's positional entries broke under version skew. Young's maps survive but can never be renamed. Cambria shows where tolerance ends. What they would fix forever instead: that the envelope is a map of named parts; that it has a version; that there is one extension slot and it is inside the hash; that a reader ignores what it does not know; and that a changed meaning gets a new name.

**Running answers never stored; only crossings recorded.** The camp's strongest pushback, given in the short version. They would protect two things inside Sid's rule: derived data must never become a source, and it must always be disposable. They would drop the word "never". Their replacement principle is that derived state is honest if it carries the position it reflects: Aurora's pages, Tango's views, a CT tree head, a Kafka consumer's offset. A cache hides how old it is; a view that names its position cannot. Whether that distinction survives Sid's own examination of caching is for that examination. I only report that every system here, including Rama itself, keeps derived state, and that the two that tried to live on playback (Tango's clients, Lambda's batch layer) were both corrected by their own authors' next systems.

**Every read recorded as provenance on every fact.** Nobody here does it, and two of them came close enough to show it is feasible. Tango and Hyder put read sets into the log for every transaction, but for validation, and they let them go when the log is trimmed. Hyder counts unrecorded reads as its source of read scale ("they are not logged or melded"). The honest statement is that Sid would be first to keep read sets forever at machine rate, and the known tricks for size are: patterns, not lists; direct reads, not the closure; one frontier; shared read records.

**One writer.** The camp agrees on one writer per ordered unit and rejects one writer per system. CORFU's sequencer is "merely an optimization". Aurora's single writer was a "simplifying assumption" that its successor dropped. DSQL gives each key one adjudicator and has as many as it needs. For Sid the phrase should mean: for any cell, at any moment, exactly one gate, fenced by an epoch.

### 9. Questions this camp would call the wrong question

1. **"Pointed at by the gate's number, or by its own id?" (1).** Both, and also a hash. They are three jobs. Asking which one is like asking whether a book needs a title or a shelf mark.
2. **"Is how far the index had got written down?" (5).** That is what "as of" means. There is no other honest as-of to write.
3. **"Never rewritten, or only never lost?" (0).** Four promises. Bytes may change. Meaning, order and identity may not. Provability is a fourth decision with its own bill.
4. **"Can two gates ever write one layer?" (10).** It assumes the layer is the unit of order. No system here orders a namespace. They order keyed units, and a namespace spans many of them.
5. **"Is the gate's yes or no kept beside each fact?" (7).** The prior question is whether offers are logged before they are judged. Once that is settled, where verdicts live and whether refusals survive follow from it.
6. **"Whose clock?" (11).** A fair question with a quick answer (the gate's, and never for order). The camp would add the question behind it: which time? The moment a statement is about, or the moment it was admitted.

---

## The second store: what each source implies

- **Helland.** Ids must be scoped to cover every party that will ever use them, and "It is not uncommon for one system to provide an alias for its identifiers." Pointers must name immutable versions. Reference data is published with version ids "known to be increasing". INFERRED: own ids and content hashes cross a store boundary; gate numbers do not.
- **Certificate Transparency.** Many stores is the normal case. There is no order across logs, ever. Each log's identity is a key, and each receipt names its log. Safety is a client rule: a quorum of independent logs. Logs are born and retired on a schedule. INFERRED: the cheapest insurance is a store id on every verdict. In a one-store world it is a constant and costs almost nothing, but if it is left out it is missing from every early fact for good.
- **Kleppmann.** Updates named by hash merge with no coordinator, and a pointer into someone else's repository carries the hash, so neither side has to trust the other. Counter-based ids fail the moment a second, less trusted writer exists.
- **Balakrishnan.** A store can continue elsewhere if the pointer to its successor lives outside it. Across regions, order becomes a graph and "as of" becomes a vector that can be compared.
- **Young.** Copy-and-transform makes a second store as a versioning tool, and its trap is ids that collide with different content.
- **Bernstein and Das.** Positions from different logs "are incomparable"; a partition id can break ties if one order is needed, and that order is arbitrary.
- **Genesis.** If the first facts are identical in every store, two stores agree on what the first kinds mean, and facts can move between them without translation. If each store mints its own, every exchange needs a mapping.

---

## Conventions this camp would fix before the first record

All INFERRED: my distillation of the above, not any one source's list. Each is something that cannot be added to earlier facts later.

1. Three names on every fact: a client-minted opaque id, the gate's number, and a content hash over a canonical form. Pointers carry id plus hash.
2. One canonical byte form and a named hash function, fixed and written down outside the store.
3. The envelope is a map of named parts, with an envelope version and one extension slot that is inside the hash from day one.
4. The value is separable from the envelope. Nothing personal in ids, keys or the actor slot. A salt on the hash of any value that may need erasing.
5. "Replaces" is recorded, as a list of parents.
6. The verdict is a fact naming what it checked: grammar version, policy version, expected version, gate epoch, runtime version, store id. Refusals go to a separate stream that may be trimmed, keyed by offer id, and the refused offer is kept with them.
7. "As of" means the applied position of the index that served the read. Across partitions it is a vector, which can be named by a small fact. An offer can carry "how far I had seen".
8. Every listed read is pinned to a version, marked depends-on or passed-through, and marked with who vouches for it: gate, runtime or actor.
9. The key is an id. Its meaning never changes. Each fact or verdict pins the grammar version.
10. A change of runtime lands as a fact. Every crossing carries the runtime version and a checksum of what was shown.
11. Genesis: a few well-known ids in the runtime; genesis facts identical in every store; a written list of what lives outside the facts (encoding, trust anchor, successor pointer).
12. Both the chain's root and the immediate cause are recorded, and the runtime fills them.
13. "When" is the gate's clock, monotone within a partition, never used for order. World-time lives in the value.
14. A planned, recorded road for excision and for neutralizing a poison record.
15. No stored pointer depends on a partition position, because the partition count and function are part of what a position means.

## What this camp cannot tell Sid

By whom and acting-for (8, 15). Visibility of private layers (16). Which gestures to record (14). Policies as facts and how a gate reads a policy that lives in another partition. Clock-based as-of. How to check any of this before building. These belong to the other groups.

---

## Sources

Every item below was downloaded in this session and its quotes checked against the saved text. ACM Queue and allthingsdistributed.com pages were read through Internet Archive captures of the same URLs, because the live sites block automated fetches.

**Helland.** *Immutability Changes Everything*, ACM Queue 13(9), 2015, https://queue.acm.org/detail.cfm?id=2884038 (and CIDR 2015). *Data on the Outside versus Data on the Inside*, CIDR 2005, http://cidrdb.org/cidr2005/papers/P12.pdf, and ACM Queue 2020, https://queue.acm.org/detail.cfm?id=3415014. *Life beyond Distributed Transactions: an Apostate's Opinion*, CIDR 2007 and ACM Queue 2016, https://queue.acm.org/detail.cfm?id=3025012. *Identity by Any Other Name*, ACM Queue 2019, https://queue.acm.org/detail.cfm?id=3314115. *Idempotence Is Not a Medical Condition*, ACM Queue 2012, https://queue.acm.org/detail.cfm?id=2187821. Helland and Campbell, *Building on Quicksand*, CIDR 2009, https://arxiv.org/abs/0909.1788. *Memories, Guesses, and Apologies*, MSDN blog, 15 May 2007 (Internet Archive capture of 16 March 2011). *Mind Your State for Your State of Mind*, ACM Queue 2018, https://queue.acm.org/detail.cfm?id=3236388. *Scalable OLTP in the Cloud: What's the BIG DEAL?*, CIDR 2024, https://www.cidrdb.org/cidr2024/papers/p63-helland.pdf.

**Balakrishnan and the shared log.** Balakrishnan et al., *CORFU: A Shared Log Design for Flash Clusters*, NSDI 2012, https://www.usenix.org/system/files/conference/nsdi12/nsdi12-final30.pdf. Balakrishnan et al., *Tango: Distributed Data Structures over a Shared Log*, SOSP 2013, https://maheshba.bitbucket.io/papers/tangososp.pdf. Wei et al., *vCorfu: A Cloud-Scale Object Store on a Shared Log*, NSDI 2017, https://www.usenix.org/system/files/conference/nsdi17/nsdi17-wei-michael.pdf. Lockerman et al., *The FuzzyLog: A Partially Ordered Shared Log*, OSDI 2018, https://www.usenix.org/system/files/osdi18-lockerman.pdf. Balakrishnan et al., *Virtual Consensus in Delos*, OSDI 2020, https://www.usenix.org/system/files/osdi20-balakrishnan.pdf. Balakrishnan et al., *Log-structured Protocols in Delos*, SOSP 2021, https://maheshba.bitbucket.io/papers/delos-sosp2021.pdf. Balakrishnan, *Taming Consensus in the Wild (with the Shared Log Abstraction)*, ACM SIGOPS Operating Systems Review 58(1), 2024, https://maheshba.bitbucket.io/papers/osr2024.pdf. Ding et al., *Scalog: Seamless Reconfiguration and Total Order in a Scalable Shared Log*, NSDI 2020, https://www.usenix.org/system/files/nsdi20-paper-ding.pdf.

**Young and event sourcing in practice.** Greg Young, *Versioning in an Event Sourced System*, Leanpub, https://leanpub.com/esversioning/read. Greg Young, "causation or correlation id?", event-store Google Group, 29 March 2015, https://groups.google.com/g/event-store/c/pvk2iEBgBtA. Greg Young, "CQRS", 2 March 2012, https://gregfyoung.wordpress.com/2012/03/02/cqrs/. Jan Stenberg, "A Whole System Based on Event Sourcing is an Anti-Pattern", InfoQ, April 2016, https://www.infoq.com/news/2016/04/event-sourcing-anti-pattern (a reporter's account of Young's talk). KurrentDB and EventStoreDB documentation, https://docs.kurrent.io (appending, streams, projections, cluster pages). Overeem, Spoor, Jansen, Brinkkemper, *An Empirical Characterization of Event Sourced Systems and Their Schema Evolution: Lessons from Industry*, Journal of Systems and Software 2021, https://arxiv.org/abs/2104.01146. Overeem, Spoor, Jansen, *The Dark Side of Event Sourcing: Managing Data Conversion*, SANER 2017. Mathias Verraes, "Forgettable Payloads" and "Crypto-Shredding", 13 May 2019, https://verraes.net/2019/05/eventsourcing-patterns-forgettable-payloads/ and https://verraes.net/2019/05/eventsourcing-patterns-throw-away-the-key/. Oskar Dudycz, "How to deal with privacy and GDPR in Event-Driven systems", https://event-driven.io/en/gdpr_in_event_driven_architecture/. Martin Fowler, "Event Sourcing", 2005, https://martinfowler.com/eaaDev/EventSourcing.html.

**Kleppmann.** "Turning the database inside-out with Apache Samza", 2015, https://martin.kleppmann.com/2015/03/04/turning-the-database-inside-out.html. *Making Sense of Stream Processing*, O'Reilly and Confluent, 2016 (free PDF from Confluent). Kleppmann, Beresford, Svingen, *Online Event Processing*, ACM Queue and CACM 2019, https://martin.kleppmann.com/papers/olep-cacm.pdf. *Thinking in Events: From Databases to Distributed Collaboration Software*, DEBS 2021, https://martin.kleppmann.com/papers/debs21-keynote.pdf. *Making CRDTs Byzantine Fault Tolerant*, PaPoC 2022, https://martin.kleppmann.com/papers/bft-crdt-papoc22.pdf. Kleppmann, Wiggins, van Hardenberg, McGranaghan, *Local-first software*, Ink and Switch, 2019, https://www.inkandswitch.com/essay/local-first/. Automerge binary format specification, https://automerge.org/automerge-binary-format-spec/. Kleppmann et al., *Bluesky and the AT Protocol*, arXiv:2402.03239. Litt, van Hardenberg, Henry, *Project Cambria*, Ink and Switch, 2021, https://www.inkandswitch.com/cambria/. Interview on the second edition of his book, The New Stack, 4 March 2026, https://thenewstack.io/data-intensive-applications-rewrite-2026/.

**Certificate Transparency and descendants.** RFC 6962 (2013) and RFC 9162 (2021). Ben Laurie, *Certificate Transparency*, ACM Queue 12(8), 2014, https://queue.acm.org/detail.cfm?id=2668154. Eijdenberg, Laurie, Cutter, *Verifiable Data Structures*, Google, 2015 (Trillian repository). Trillian README and changelog, https://github.com/google/trillian; the Claimant Model, same repository. Chrome CT team, "Retiring DigiCert Yeti2022 Log in Chrome", ct-policy list, 9 July 2021. Andrew Ayer, "How Certificate Transparency Logs Fail and Why It's OK", 9 July 2021, https://www.agwa.name. Chrome Certificate Transparency log policy, https://googlechrome.github.io/CertificateTransparency/log_policy.html. Filippo Valsorda, "The Sunlight CT log" design document, https://filippo.io/a-different-CT-log, and the Sunlight README. The Static CT API specification, https://c2sp.org/static-ct-api, and the witness protocol, https://c2sp.org/tlog-witness. Let's Encrypt, "End of Life Plan for RFC 6962 Certificate Transparency Logs", 14 August 2025, and "Reflections on a Year of Sunlight", 11 June 2025. Ryan Sleevi, "Policy Discussion: Name Redaction", ct-policy list, 2016. LF Projects, "Hosted Project Tools – Immutable Record notice". Russ Cox, *Transparent Logs for Skeptical Clients*, 2019, https://research.swtch.com/tlog. Eric Rescorla, "A hard look at Certificate Transparency: CT in Reality", 25 December 2023, https://educatedguesswork.org. Andrew Ayer's statement on RFC 9162: mdn/content issue 35570, 23 August 2024.

**Hyder.** Bernstein, Reid, Das, *Hyder: A Transactional Record Manager for Shared Flash*, CIDR 2011, http://cidrdb.org/cidr2011/Papers/CIDR11_Paper2.pdf. Bernstein, Reid, Wu, Yuan, *Optimistic Concurrency Control by Melding Trees*, PVLDB 4(11), 2011, https://www.vldb.org/pvldb/vol4/p944-bernstein.pdf. Bernstein, Das, Ding, Pilman, *Optimizing Optimistic Concurrency Control for Tree-Structured, Log-Structured Databases*, SIGMOD 2015. Bernstein and Das, *Scaling Optimistic Concurrency Control by Approximately Partitioning the Certifier and Log*, IEEE Data Engineering Bulletin 38(1), 2015, http://sites.computer.org/debull/A15mar/p32.pdf.

**Aurora and after.** Verbitski et al., *Amazon Aurora: Design Considerations for High Throughput Cloud-Native Relational Databases*, SIGMOD 2017. Verbitski et al., *Amazon Aurora: On Avoiding Distributed Consensus for I/Os, Commits, and Membership Changes*, SIGMOD 2018. Werner Vogels, "Amazon Aurora ascendant", 2019, allthingsdistributed.com. AWS, "Amazon Aurora Multi-Master is now generally available", 2019, with its retirement line dated 8 September 2023. AWS Aurora user guide pages on multi-master clusters and Backtrack (awsdocs repository). Marc Brooker, DSQL vignettes, December 2024, https://brooker.co.za/blog/. Brooker et al., *Aurora DSQL: Scalable, Multi-Region OLTP*, arXiv 2607.13276, July 2026.

**Lamport.** *Time, Clocks, and the Ordering of Events in a Distributed System*, CACM 21(7), 1978, https://lamport.azurewebsites.net/pubs/time-clocks.pdf.

**Kreps and Marz.** Jay Kreps, "The Log: What every software engineer should know about real-time data's unifying abstraction", LinkedIn Engineering, 2013 (Internet Archive capture). "Questioning the Lambda Architecture", O'Reilly Radar, 2014, https://www.oreilly.com/radar/questioning-the-lambda-architecture/. "It's Okay To Store Data In Apache Kafka", Confluent, 2017, https://www.confluent.io/blog/okay-store-data-apache-kafka/. Nathan Marz, "How to beat the CAP theorem", 2011, http://nathanmarz.com/blog/how-to-beat-the-cap-theorem.html. Rama documentation (depots, PStates, microbatch, tutorial), https://redplanetlabs.com/docs/~/index.html, fetched 2026-09-20. Red Planet Labs blog: "How we reduced the cost of building Twitter at Twitter-scale by 100x" (2023) and "Migrating terabytes of data instantly" (2024).

**Sought and not obtained.** Verified wording from *Designing Data-Intensive Applications*. A transcript of Young's DDD Europe 2016 talk. Young's original "Why can't I update an event" blog post (the book quotes it). A written reason for the removal of Trillian's map mode. A written reason for the retirement of Aurora multi-master. Any retrospective by Bernstein on Hyder. The Go checksum database's policy on taking a module down. Chapter 2 of Marz and Warren's *Big Data*.

---

## Appendix: material touching names moved to other sessions

Nothing was gathered on purpose for the moved names. The scope change arrived before any gathering began. Three incidental items came up inside my own sources and may help research-6:

- **Spanner, seen from this camp.** Tango defines itself against it: "Spanner [18] uses real time as an ordering mechanism via synchronized clocks". Helland's 2016 revision of *Life beyond Distributed Transactions* concedes that Spanner-class systems work, and removes his 2007 "Maginot Line" comparison.
- **Clock-based "as of" in production.** Aurora DSQL reads as of each transaction's start time, taken from EC2's precision clocks, makes lagging storage wait, and states what fails when a clock is wrong: "clock skew beyond the expected bounds causes the system to lose linearizability, but not isolation, durability, or atomicity." It keeps only five minutes of old versions. Source: Brooker et al., arXiv 2607.13276.
- **Calvin.** The Aurora 2018 paper names Calvin as the other way to avoid distributed commit, and the DSQL paper says its post-commit path "is somewhat similar to deterministic databases like Calvin [33] or SLOG [26]."

---

## Round two: the leans, pressed

Written 2026-09-20, after the orchestrator sent softland-ff's current leans. They are leans, not Sid's rulings. No new sources were fetched. Every quote below was verified in round one and its URL is in Sources above. Marks: R reported, I inferred, N institutional. Resembles and differs, once: this camp mostly ran redo logs for trusted clients under one operator; Sid's is a record log that strangers may one day check. I repeat the difference only where it changes an answer.

### T1. The four promises against leans (0) and (9)

**What each lean needs.** Lean (0) needs promises 2 and 3 (never different, never re-ordered) and takes 1 for granted. Lean (9) needs 1 and 2 for the ciphertext and the hash, and moves all real deletion into a key store. Neither needs promise 4 now.

**Sharpen (0): say what is never rewritten.** Helland would reject a byte-level reading: "semantically immutable but can be physically changed" (R). The deciding case: year twelve, the serializer that wrote the ten million papers' facts is dead or unsafe, and the runtime has been rebuilt two hundred times. If the promise covers Rama's stored bytes, re-encoding is forbidden forever. If it covers the canonical form that the hash covers, re-encoding is free and still checkable. I: define the promise over the canonical form.

**Press (9): under a strict (0), key destruction has no fallback.** Verraes prints the lawyer's view: "the law does not consider deleting the encryption key equal to actually deleting the data itself" (R). The deciding case: one regulator, somewhere on the planet, in some year, orders the ciphertext itself removed. Under strict never-rewritten Sid breaks the promise for everyone. Under Helland's rule the promise survives, because absence was always allowed: "it will never return data other than the original contents" (R). Rama's tombstone keeps offsets (R). I: keep key destruction as the normal road, name excision as the rare legal road, and make each use a fact.

**Press (9): a key-store restore un-deletes.** A CT log died because "its database was rolled back during a botched backup restore" (R, Ayer). For a key store, a rollback resurrects destroyed keys. I: every key destruction must itself be a fact in the store, so that after any key-store restore the destroyed list is replayed. "Reaches backups" is true only with that.

**Press (9): "the fact keeps the value's hash".** A bare hash of a short value (a yes, a diagnosis code, a name) can be guessed after erasure. I: keep a hiding commitment instead: a hash over the value plus a secret salt that lives and dies with the value's key.

**Sharpen "maybe by-whom may go".** If by-whom can be removed, envelopes are rewritable after all. I: by-whom is always an opaque actor id and never goes. What goes is the value of the fact that binds that id to a person. Then no envelope ever needs touching, and (0) holds for envelopes without exception. The residue is real and lean (9) does not remove it: a person "can be identified not only through personal information, but through the associations and relations with other information" (R, Verraes).

**The exact minimum for promise 4 later** (I, from CT's record, N):
1. One canonical byte form of a fact, specified outside the store.
2. An envelope version and a named hash function inside the envelope.
3. The fact's hash, computed over that form at admission and kept in two places (the fact and its verdict), so that a flipped bit is a repair and not a Yeti2022.
4. Values enter the hash only through the commitment above, so erasure never breaks a chain.
5. One extension slot inside the hashed form.
6. A per-partition order that no operation ever changes.

Nothing else is needed now. The tree, the signatures and the witnesses can all come later, because a Merkle tree is an index over positions and hashes. One caution: provability begins at the first checkpoint an outsider holds. If early history should ever be provable, anchor a hash of the partition heads somewhere outside, early and cheaply.

### T2. The three names against leans (1), (2), (6)

**Lean (1) stands on its main point, and Rama decides it.** Topology depot appends "currently do not have exactly-once semantics in the face of failures and retries" (R, Rama docs). With the gate's number as the name, a repeated append is two facts. With a content-derived id it is one fact seen twice. Who lives this way: Automerge's change hashes, the AT Protocol's CIDs, Git.

**What breaks when one name does two jobs.**
- *The salt.* A public salt gives uniqueness and lets strangers verify, but leaks short values after erasure (T1). A secret salt protects them and stops strangers verifying. I: two salts for two jobs. A public one in the offer for uniqueness; a secret one inside the value commitment.
- *Dedupe at ingest.* The deciding case: the ten-million-paper seed dies at paper 6,200,000 and is re-run from the start. With random salts every fact is offered again under a new id, the cells are already filled, and the gate writes millions of refusals that lean (7) keeps forever. With salts derived the way lean (2) derives ingest entity ids, the ids repeat and the gate gives the old answer: "the same reply must be returned" (R, Helland). I: random salts for people and agents, derived salts for ingest lanes.
- *The duplicate check must be exact.* Sunlight's "cache can be best-effort and lossy" (R) only because CT tolerates duplicates. Sid's cells do not. Event Store scopes its check: "The idempotence check is based on the EventId and stream." (R). I: scope Sid's to the cell's partition, where lean (7) already puts the verdict.
- *Hash migration.* Not decisive. Name the algorithm inside the id. When a function weakens, the gate can issue attestation facts under a new one; they prove things only from that day on, under either design. The gate must refuse same id with different content, which is Young's trap: "two completely different events have the same identifier" (R).

**The question under lean (1).** Is the fact the offer unchanged, or the offer plus the gate's stamps? In this camp the judge never edits what it judges. Tango's commit record and its decision record are separate records (R). Kleppmann's claim and its outcome go to separate streams (R). I: let the fact be the offer, byte for byte, and put when, version, epoch and what-was-checked on the verdict beside it. Then the id hashes exactly what the offerer made, repeated appends are identical, and a stranger can check the offerer and the gate separately. The cost is a local join to read a fact's version. This is where I would press hardest.

**"Facts have no ids, sayings do."** An offerer-minted id of the saying satisfies my first name fully: known before landing, stable on retry, portable between stores. Where one offer is one fact they are the same thing. I: reserve the form "saying id plus index" now (Kleppmann's hash scheme numbers the ids inside one update, R), so a saying that carries several facts for one entity stays possible. Across entities no atomic saying can exist under lean (10).

**Lean (2) stands** (Automerge's random 128-bit ids; AT Protocol DIDs). One press on ingest: a derived id inherits its source's hygiene. Helland: "There's nothing to stop them from changing SKU 12345 from a pair of ruby slippers to a can of chocolate sauce." (R). Preprint and journal version, merged DOIs: the derivation rule fixes entity grain forever. I: publish the rule as a versioned seed fact.

**Lean (6) stands** (Git; Automerge; Helland's "one parent and one child", R). Sharpen: a list of parents, named by fact id, not by the number 25.

### T3. Keyed units, or the layer as the unit of order?

The camps agree on the principle. Helland: "You know only when a single unique key unifies both." (R). If a layer is the unit of order, the layer is the key and all of it has one home. The disagreement is only about which key.

**The deciding case is the base layer.** If layers are the unit, base is one partition: one leader, one machine's throughput, one region's round trip for the whole planet, one blast radius (R, Balakrishnan 2024). FuzzyLog calls such an order "expensive, often impossible, and typically unnecessary" (R). Any layer written by hundreds of people plus their agents fails the same way. So the layer cannot be the unit everywhere.

**The Datomic camp's point survives inside lean (10).** A personal or session layer, at tens of agents, fits one partition, and there a single number is just a cut of length one. So record "as of" as a cut always. The second deciding case is the hand. Under partition-by-entity, one person's gestures on different entities have no mutual order except the gate's clock, which lean (11) forbids for order. I: give hand and crossing facts the session as their entity. Then the keyed unit is the meaningful unit, which is what both camps want. One warning stays. A personal layer that becomes a team layer must be re-homed, and "Earlier record versions were placed in their old log." (R, Helland). Cuts must name partitions by ids that are never reused.

### T4. Lean (7) against "refusals in a separate, trimmable stream"

Yes-verdicts forever, beside the fact, in the same partition: stands. Tango writes its decisions down (R), and the exact duplicate check in T2 needs the verdict to be local.

**Refusals forever is the part I would press.** The deciding case: thirty agents of one person keep one "current summary" cell fresh. Each landed fact makes about twenty-nine refusals. Hyder measured the mechanism: the longer the wait, "the greater the chance that each transaction aborts" (R). Under leans (7), (9) and (13), each refusal is a full offer, an encrypted value, a key in the key store, and a verdict, forever. Session layers become the largest data in the store. CT keeps no refusals at all: "Rejected logging submissions must not be issued an SCT by the CT log." (R), because "logs are useful only if their size is manageable" (R, Laurie).

**The trim rule** (I). Keep forever the refusal's envelope and verdict: offer id, actor, cell, expected version, reason, what was checked. Destroy the refused value's key after the retry window unless someone pins it. Helland bounds that window with his bank, where a check clears in "less than one year" (R). Under lean (9) this trim rewrites nothing, so it does not touch lean (0). What is lost: replaying a new gate against old refused offers beyond the window, and the content of what an agent tried to write. What stays: counts, reasons, and a commitment by which a holder can prove what they offered. Also decide when a session layer closes. Young's accountants close the year: "Nine were marked by their year and were read-only" (R). A closed session can move to cold storage, as Delos does with old segments (R), without being trimmed.

### T5. What must exist from record one so that nothing needs a full playback

First, honestly: a truly new index always costs one full scan. Kreps's method is to run it beside the old one and cut over (R). The aim is that nothing else ever needs one. Five causes of playback in this camp, and what each needs:

1. *New indexes.* I: make every future scan an envelope scan. Store envelopes apart from value bytes from day one. Lean (9) already makes values opaque. Ten million papers' values are large; their envelopes are small. Under lean (0) this layout cannot be changed later.
2. *Readers without checkpoints.* vCorfu: "the entire log must be read to determine the most recent writes to each stream" (R). I: every derived thing names the cut it reflects and resumes from it.
3. *Runtime rebuilds.* Delos: "engine roll-out has been the only source of inconsistency in production so far" (R). I: each rebuild fact (lean 12) should say which keys' interpretation it changes, and derived state should record the runtime and tool versions that made it. Then a rebuild invalidates only what it names.
4. *Old as-of reads.* Indexes in this camp forget fast (DSQL keeps five minutes, R). I: a crossing should list what crossed, as ids and hashes, not only a pattern and a cut. A screen or a model's context is finite, so that list is always affordable. Young: "enrich the information returned from the call onto the event" (R). Then "what was I looking at yesterday" is a fetch by id.
5. *Erasure and doubt walks.* "Who read the erased thing" walks based-on backwards. I: that reverse index must exist before the first erasure, or the first erasure is a full scan.

The premise is more survivable than my round one made it sound. Summaries are offers, so the largest derived things are already facts, and Rama's indexes are stored. The heel moves to index rebuilds.

### A. The other leans

- **(17) Sharpen.** Content-computed seed ids: stands (every CT log starts from the same empty hash, R). But split the seed. Kinds and grammars are universal and content-computed. The store's own id and its gate's actor id must be random per store. Deciding case: the second institution's store. If the gate's id is content-computed, two stores' verdicts carry the same actor and cannot be told apart.
- **(3) Sharpen the ground.** It is loss, not only shape and cost. Young: "you are no longer allowed to rename something" (R). A word used as a key can never be renamed. Also pin the grammar version on the verdict (Helland, R).
- **(11) Sharpen "every read listed" to "every read accounted for".** Deciding case: a summary tool scans 200,000 facts to write one summary. Listed, that is megabytes of pointers on one fact. As a pattern plus a cut it is one line. Hyder's reads scale because "they are not logged or melded" (R). Kleppmann lists direct parents only, "which ensures that this set remains small" (R). When, because-of, and no grace period: stand. Make when monotone within a partition (CT's rule, R), and note that Rama already stamps an append time (R).
- **(4) Sharpen.** Three marks, not two: depends-on, how-she-got-here, trigger. The trigger is Young's immediate cause (R), which the nine parts otherwise lack. Reject "the tool may correct" if it means after landing. Under (0) a correction is a second fact that every later walk must find. Let the tool mark in the offer; let the floor fill gaps and say that it guessed.
- **(5) Stands**, and "what was withheld" is better than anything in this camp. Nearest neighbour: Delos carries its playback position in every entry (R).
- **(12) Stands** (Delos, R). Add the gate's epoch to the verdict; Aurora fences writers by epoch (R).
- **(13) Stands**, with T4's volume warning. Plain maps: Delos moved to "a map of headers" (R). Add the envelope version and the extension slot.
- **(14) Press.** The local-first team: "CRDTs store all history, including character-by-character text edits. These pile up" (R), and they could not trim. Deciding case: pan and zoom ticks, hundreds of people, years, each tick a fact plus a verdict, never trimmed. I: put the settled viewport on the crossing fact, which needs it anyway, and record motion only at the grain of intent.
- **(8), (15), (16).** Nothing sourced here.

### B. Wrong questions, against the leans

1. Lean (0) asks rewritten or lost. Ask: never rewritten *what*, the canonical form or Rama's bytes?
2. Lean (1) asks how the id is computed. Ask first: is the fact the offer unchanged?
3. Lean (7) asks where refusals live. Ask: when does a session layer close?
4. Lean (10) asks one number or a cut. Ask: who names partitions, and do the names survive re-homing?
5. Lean (11) asks whether every read is listed. Ask whether every read is accounted for.

### C. Above the table: only what the leans change

- **The runtime is no longer the one non-fact.** Lean (9) creates a second: the key store. It is mutable, it can make any value vanish, and losing it leaves a store of hashes. Verraes: the pattern is "only as good as your encryption and your key management practices" (R). Delos's lesson is that the small thing outside the log is the one that is "necessary and sufficient" (R), and deserves the most care. The non-fact list is now: runtime, key store, encoding and hash rules, trust anchor.
- **One writer, doubled.** With (7), (11) and (14), every landed fact is two appends, every refusal two, every tick two. Hyder's sequential judge was its ceiling: "meld has been the bottleneck that limits transaction throughput" (R). I: fact and verdict should be one atomic append to one partition.
- **One store.** "A layer has one home store" admits many stores. A store id on every verdict from record one then stops being insurance and becomes necessary.

### The three I would press hardest

1. Lean (1): make the fact the offer, unchanged, with every gate stamp on the verdict. Case: Rama's repeated append; the seed re-run.
2. Leans (0) and (9): define never-rewritten over the canonical form; keep excision as a recorded legal road; record key destruction as a fact; commit to values with a secret salt. Cases: the regulator who rejects key deletion; the key-store restore.
3. Lean (7) with (13) and (14): refusals and ticks kept forever, at machine rate, are the largest data in the store. Case: thirty agents on one cell.

