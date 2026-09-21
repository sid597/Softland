# Immutable facts and Datalog: what this camp would fix before the first record

Research session `research-2`, 20 September 2026. Group: IMMUTABLE FACTS AND DATALOG.
Written for the orchestrator and for Sid. Round one: I was not given Sid's current leanings, on purpose.

Cutter: Claude Fable 5.1, max effort. Gathering of the XTDB, excision/Nubank and Instant/Prokopov/Datalevin sources was delegated to three read-only Opus gatherers. Every quote they returned carries a local file and line. I spot-checked the decision-changing ones against the raw downloads myself. All reading of Hickey, the Datomic docs, Stuart Halloway and Jepsen is mine. All judgment is mine.

---

## 0. How to read this

**Markers.** Every claim about what someone would say to Sid's questions carries one of:

- **REPORTED** — they wrote or said it. A quote or close paraphrase with a source follows.
- **INFERRED** — I reconstructed it from their system or their stated principles. I say from what.
- **INSTITUTIONAL** — company practice or product docs with no named author. I am inferring what the institution thought.

**Quotes are verbatim.** Each was copied from raw text I downloaded (talk transcripts, docs pages, mailing-list archives, the Hacker News API, GitHub issues). Where a quote was collected by a gatherer, I say so in the source list and I re-checked the ones that carry weight. I use no quote from the auto-captioned Råberg talk, because the captions are garbled.

**What I could not open or find.**

- No transcript of Hickey's *The Database as a Value* (2012). I used *Deconstructing the Database* (QCon, Nov 2012) and *The Functional Database* (QCon NY, Jun 2013). They cover the same ground. The closing slide of *Deconstructing* is titled "The Database as a Value".
- Nothing by Rich Hickey on excision, GDPR or erasure. His whole Hacker News history (72 comments) was searched. Zero hits. The reasoning on erasure below is Stuart Halloway's and the Datomic docs', not Hickey's.
- No official Datomic guidance recommending crypto-shredding, or recommending that personal data be kept out of Datomic. Only users and one community essay say that.
- No explicit XTDB statement that version 2 dropped content-addressed documents. The evidence is circumstantial.
- Nothing from Nubank on personal data, LGPD or erasure. Absence of a finding, not proof of absence.
- Instant never says *why* its attribute slot is a UUID. The code shows that it is.

**Short source keys** like [H-DD] are resolved in section 9.

---

## 1. The short version

Five findings, then the camp's strongest challenge. The working is in the sections that follow.

**1. The camp puts provenance on the saying, not on the fact.** Datomic's fact is a fixed five-part tuple: entity, attribute, value, transaction, added-or-retracted. Who, when, why, from what source, on what basis: all of these are attributes of the *transaction*, which is itself an entity with an open set of attributes. Hickey, 2016: provenance goes "on the transaction (which can have an open set of attributes)… This is substantially more efficient than replicating this on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)." [H-ML16]. Sid's envelope puts by-whom, when, based-on and because-of on every fact. This camp would move them to one reified act of saying that many facts share.

**2. Key: word or id? Both, and the split is exact.** In storage the attribute slot holds an id: `[42 1007 1124]`. The name is a fact about that id: `:db/ident`. Renaming adds a name and the old name keeps working. "Never remove a name." The one thing that can never change is the value's type. Instant made the same choice independently with UUIDs. DataScript chose plain words and says so. [D-IDENT], [D-CHANGE], [D-BEST], [I-SQL], [T-README]

**3. "As of" is one number, because there is one order. And that is a scope decision, not a storage decision.** Datomic: "transaction ids are a total ordering of all transactions." A reader sees "all transactions up to their time basis, in order, with no gaps." [D-MODEL], [D-ACID]. XTDB v2 enforces a single Kafka partition and refuses to start otherwise. Both admit the price in plain words: a hard ceiling on writes. Hickey: "It *is* the bottleneck… nothing is infinite." At real scale the camp does not partition the log. It makes many databases. Nubank runs more than 3000. The unit of total order is the unit of consistent reading.

**4. Never rewritten is not the same as never lost, and the camp says so.** Hickey, 2013: "while the past may be forgotten, it is immutable." [H-IM]. Datomic added excision a year after launch because law forced it. It costs index rebuilds "proportional to the size of the entire database", it is absent from Datomic Cloud, and in October 2025 Datomic fixed a bug that had left excised data readable through the as-of and history indexes. XTDB v1 designed for erasure from day one: the log holds only content hashes, the documents sit in a store that can forget. "The transaction log can stay immutable." [X-DOC]. That design then broke replay. XTDB v2 gave up the permanent log altogether.

**5. The regrets are about what was not written down at the time.** Nubank, on hindsight: "one thing that we do not add and I would have liked to add, a customer identifier. Because, if every transaction had an identifier that could point to the actual customer that owns that data, things like splitting databases for sharding would have been much, much easier." [N-QCON]. And Lucas Cavalcanti: "what I regret the most, is to have implicit operations that are not stored into the database, or that don't have origination information." [N-POD]. Nobody in this camp regrets recording too much provenance. They regret recording too little, and they regret putting firehose data in the fact store.

**The strongest challenge to the premise.** Hickey built Datomic for a closed world and said so: "Being oriented toward business information systems, Datomic adopts the closed-world assumption, avoiding the challenges of universal naming, open-world, shared semantics etc of the semantic web." [H-IM]. One store for the planet, with keys everyone shares, is exactly the problem he chose to avoid. His camp would not build one store with a layer slot. They would build many databases, each with its own single order, and compose them at read time: "Database is an argument to query. It is not the ambient container for a query." [H-DD]. Their second challenge is the envelope itself: a fixed nine-part record is what Hickey calls place-oriented. "If you have places, you have to have something in the place." [H-MN]

---

## 2. Rich Hickey and Datomic (with Stuart Halloway, the Datomic docs, and Jepsen)

### 2.1 What they built, and what they chose

Datomic (2012). A database where the unit is an immutable fact, the *datom*:

> "A datom is an immutable atomic fact that represents the addition or retraction of a relation between an entity, an attribute, a value, and a transaction. A datom is expressed as a five-tuple: an entity id (E), an attribute (A), a value for the attribute (V), a transaction id (Tx), a boolean (Op) indicating whether the datom is being added or retracted" [D-MODEL]

The choices that matter here:

- **Accretion only.** New facts are added. Old facts are never changed. A change of value is a retraction datom plus an assertion datom in the same transaction.
- **One writer per database, one total order.** A *transactor* serializes all transactions. Each gets a number, `t`. A database value is identified by its `basis-t`.
- **Readers need no coordination.** Peers read immutable index segments from storage and merge in recent novelty from memory. A database value never changes under a reader.
- **Attributes are entities.** Schema is data in the same database. The attribute slot of a datom holds an entity id. A keyword name is attached through `:db/ident`.
- **Transactions are entities.** Every datom points at its transaction. The transaction carries the wall-clock time and whatever else the application asserts about it.
- **Time is transaction time only.** Other notions of time are ordinary attributes.
- **Forgetting was added later** (excision, May 2013), as an exception.

### 2.2 Their reasons, in their words

**Why facts, and why never changed.**

> "So we are going to say this stuff is facts, which means 'something that happened'… And one of the key things that falls out of that is: facts _cannot_ be changed. They are recordings of what happened in the past. You do not change facts. You do not update facts, or anything like that. What do you do? You accumulate new facts." — Hickey [H-DD]

> "But don't facts change? Didn't my friend get a new email address? Didn't that change the fact of his email address? No, it did not! There is now a new fact, which is: today, your friend's email address is this. It did not change the fact that yesterday, your friend's email address was that." — Hickey [H-VoV]

> "One can't make a technical argument against keeping information. It's obviously desirable (git) and often legally necessary." — Hickey [H-HN14]

**Why the fact is small, and why the transaction is the path to everything else.**

> "And we call that a Datom. But it is just an entity, an attribute, a value, and some path to time. We use the transaction, because it is also a path to other information about what happened, including provenance, or causality, or operations, or anything else like that." — Hickey [H-DD]

> "Datoms constitute a single, flat, universal relation, and there is no other structural component to Datomic. This is important, as the more structural components you have in your model the more rigidity you get in your applications." — Hickey [H-IM]

> "Having an atomic unit at the bottom of the model ensures that representations of novelty (e.g. transactions) are only as big as the new facts themselves." — Hickey [H-IM]

Asked in 2012 whether Datomic would support business time next to technical time:

> "So the time on transactions is technical time. But the thing is, transactions are first class. So you can make assertions of transactions. So if you want to assert an attribute of a transaction, which is its business time, you can do that. But the granularity you have for that is the transaction level, not the datom level. Otherwise datoms become enormous… Or any other fact about the transaction: business time, business user, business process, business approval. Put them on the transaction." — Hickey [H-WD]

And in 2016, on the Datomic mailing list, the clearest statement of the principle:

> "In Datomic, Lucy probably said everything in the transaction that asserted 'Fred likes Ethel', so we instead put the provenance on the transaction (which can have an open set of attributes). 'When' Lucy said that is similarly (automatically) tracked once, on the transaction. This is substantially more efficient than replicating this on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)." — Hickey [H-ML16]

The docs say the same as practice: "Most entities in a system model the 'what' of your domain. Transactions provide a place to model 'when', 'who', 'where', and 'why'." [D-BEST]

Hickey treats Sid's exact question as open and worth asking. From the 2016 exchange with Alan Kay:

> "What constitutes minimal sufficiency of 'data' is a useful and interesting question. E.g. should data always incorporate time, what are the tradeoffs of labeling being in- or out-of-band, per datom or dataset, how to handle provenance etc." — Hickey [H-HN16]

**Why one writer.**

> "Process, which is a term I will use to say 'the acquisition of novelty', of novel information, is something that requires coordination. In the end, I do not care if it is transactional or eventual consistency. That thing at the end that is going to merge together your stuff, that is a form of coordination. There have to be rules to govern what is allowed, and what is not. Somebody has to be responsible for doing it. We cannot all do it." — Hickey [H-DD]

> "There is research done at Yale that proved it is much faster to just serialize transactions and do it all in memory, than it is to have a complicated scheme for trying to figure out who is overlapping with who." — Hickey [H-WD]

> "All transactions are serialized, period." — Hickey [H-HN12]

He was blunt about the cost. An audience member asked whether the transactor is the bottleneck:

> "It _is_ the bottleneck… Because nothing is infinite. That is why it is not a problem. If you need arbitrary write scaling, this is not the system for you. If you are like 99% of the businesses that could not saturate one box with the amount of novelty in your system, this was a good fit… But trying to make a universal system that can handle infinite, means dropping a whole bunch of value." — Hickey [H-WD]

> "That said, Datomic is not the right choice when you require unlimited write scalability." — Hickey [H-ARCH]

And on what is lost the moment a store is sharded:

> "You start sharding, and then you cannot query against shards. You cannot do transactions across shards. You cannot ensure consistency across shards. And really, that is why I think you should consider these independent." — Hickey [H-WD]

Stuart Halloway, 2024, on why everything in a transaction shares one time:

> "Everything in a Datomic transaction happens atomically at a single point in time. Datomic transactions are totally ordered, and this ordering is visible via the time t shared by every datom in the transaction. These properties vastly simplify reasoning about time. With this information model intermediate database states are inexpressible." — Halloway [S-HN24]

**Why readers must be free.**

> "Perception is not a coordinated activity. Everybody is free in this room to look at whatever they want, or look at whomever they want. You do not need to get permission, or coordinate, or anything else." — Hickey [H-DD]

> "The critical thing is that queries and reads not part of a transformation never create a transaction, and don't interact with the transactor at all." — Hickey [H-HN12]

**Why a basis is one number that you can hand to someone.**

> "You actually can have permalinks for databases. I want to go back to this. I want to remember this database. I want to tell somebody: I think this database was messed up. And I can send them a link, and three weeks later they can go look at that link." — Hickey [H-DD]

> "I would characterize the level of coupling involved in sharing 'the basis is 12345' as categorically different from having to nest database access within the same transaction." — Hickey [H-HN12]

**Why names, and why never break one.** From *Spec-ulation*:

> "Adding stuff is growth. Period. It is just easy. It is just accretion. And removing stuff is always breakage. Always." [H-SPEC]

> "the namespace is part of the name… We are always dealing in Clojure with these nice hopefully globally unique names." [H-SPEC]

> "turn what would have been breaking into accretion. In other words, if you are going to have a variant, give birth to a variant. Do not muck with a thing." [H-SPEC]

The Datomic docs turn this into rules: "The meaning of a name is established when the name is first introduced… Never remove a name. Reusing that name to mean something substantially different breaks programs that depend on that meaning. This can be even worse than removing the name." [D-BEST]

**Why derived answers are not data.**

> "putting facts behind a dynamic interpreter (one that could answer the same question differently at different times, mix facts with opinions/derivations or have effects) certainly exceeds (and breaks) the idea of data… Consider the difference between a calculation involving (several times) a fact (date-of-birth) vs a live-updated derivation (age). The latter can produce results that don't add up. 'date-of-birth' is data and 'age' (unless temporally-qualified, 'as-of') is not." — Hickey [H-HN16]

And on caching, which matters for this project: Datomic caches the *sources* of answers, never the answers. The old model "put the _answers_ to questions in cache, hoping maybe we will ask the same question again later." In Datomic "the _sources_ of answers get cached", meaning immutable index segments, which can be cached anywhere because they never change. [H-DD]

**Why fixed slots are a mistake.** From *Maybe Not*:

> "And a straight product type just completely complects the _meaning_ of things with their position in a list." [H-MN]

> "Because what is the challenge of having a place? There always has to be something in the place." [H-MN]

> "I am just going to leave the key out. I am going to leave it out of the set… The maps know what they know." [H-MN]

### 2.3 What they later changed, regretted, or moved away from

These are worth more than the design talks.

1. **Forgetting was added.** Launch position (2012): never forget. February 2013, Hickey writes "while the past may be forgotten, it is immutable." [H-IM]. May 2013, Halloway announces excision and frames it as forced from outside: "there is a fly in the ointment. In certain situations you may be forced to excise data… This may happen if you store data that must comply with privacy or IP laws, or you may have a regulatory requirement to keep records for seven years and then 'shred' them." [S-EXC13]. By 2018 Hickey's summary of the product is: "We keep everything that you ever said, unless you explicitly go out of your way to tell us to remove it." [H-IONS]

2. **Forgetting turned out to be hard to build correctly, even for them.** Official release notice, 23 October 2025: "Datomic Release 1.0.7469 fixes a bug that prevented excisions from removing datoms from the as-of or history indexes. This release also includes a tool to detect incomplete excisions to the index and optionally re-apply them." [D-REL]. On the forum, one user reported in June 2021 that most of 6.3 million excised entities were still visible afterwards, and in February 2025 showed an excised entity still readable in the history database. The release notice does not name that thread, so linking the two is my reading, not Datomic's statement. How long the bug existed is not stated. The fix shipped with a *checker*.

3. **Datomic Cloud shipped without excision** and still lacks it. Halloway, February 2018, to a user asking about GDPR: "Datomic Cloud does not currently support excision. I have created a feature request to track this." [S-FORUM]. No alternative was offered. The nearest official advice is Marshall Thompson's, 2019: "Excision is NOT intended, nor is it suitable for, 'clean up' of old/temporary data … If you need to store data that you plan to 'throw away' at a later point, I would recommend either using an alternative store for that data or using a separate logical database in Datomic that can be deleted at the database-level." [M-FORUM]

4. **Schema became alterable.** For the first 21 months an attribute could not be changed once defined. December 2013: "we have added the ability to alter existing schema attributes after they are first defined… Schema alteration has been our most requested enhancement." [D-BLOG13]. What stayed fixed forever: "You can never alter :db/valueType, :db/fulltext, :db/tupleAttrs, :db/tupleTypes, :db/tupleType, or :db.tuple/discontinued." [D-CHANGE]. So the list of things that can never change got shorter under pressure, and what was left is the value's type.

5. **Schema does not travel back in time, and they document it as a limit.** "Because Datomic maintains a single set of physical indexes, and supports query across time, a database value utilizes the single schema associated with its current basis. Thus traveling back in time does not take the working schema back in time, as the infrastructure to support the past schema may no longer exist." [D-CHANGE]. Schema is facts with history, yet old data is always read through today's schema.

6. **The "single writer" safety argument was withdrawn in 2024.** Jepsen tested Datomic Pro and found it sound, but called the single-writer claim "wrong in two senses": a standby transactor can believe it should take over while the old one still runs. "During this window Datomic is not a single-writer system, but a multi-writer one! … Thankfully this doesn't matter: Datomic's safety property follows directly from the Sequential consistency of the storage system's CaS operation. Any number of concurrent transactors ought to be safe." After the collaboration, "Datomic also removed the 'single-writer' argument from their safety documentation." [JEP]. The current docs state the real mechanism: "Every successful transaction performs a storage CAS ensuring that its basis is the previous transaction." [D-ACID]

7. **They renamed the thing a client submits.** After Jepsen: "Going forward, Datomic intends to refer to this structure as a 'transaction request', and to its elements as 'data'. The [:db/add ...] and [:db/retract ...] forms are 'assertion requests' and 'retraction requests,' respectively. This helps distinguish between assertion datoms… and the incomplete [entity, attribute, value] assertion request in a transaction request." [JEP]. Twelve years in, they found they needed separate words for the offer and the fact. Sid already has them.

8. **Code moved out of the database.** Datomic Pro can store a transaction function as data in the database, with "versions of the code live in the Database", or take it from the classpath, with versions "external to the database in e.g. traditional source control." [D-TXFN]. With Ions (2018) Hickey tied running code to git: "So we connect application revisions to git identity. If you ever were wondering 'what version of the code is this, actually that is running?' There is no question. It is the name of the version." Builds that cannot be tied to a commit get a name prefixed "unreproducible". [H-IONS]

9. **Practitioners learned that transaction time is not domain time.** Val Waeselynck, a long-time Datomic user, 2017: "Datomic does not let you change your mind about the information you encode in its time-travel features, and that's usually too big a constraint." He separates "event time: the time at which stuff happened" from "recording time: the time at which you're system learns that stuff happened", and concludes the time-travel features are "extremely valuable for debugging, auditing, and integrating to other data systems. But you should probably not implement your business logic with them." [VAL17]. Huahai Yang cites this essay as a reason Datalevin keeps no history.

### 2.4 Question by question

Order and numbers follow the brief.

**(0) The log: never rewritten, or only never lost?**

- REPORTED. Never rewritten: yes. Never lost: no. "while the past may be forgotten, it is immutable." [H-IM]. The camp separates three acts that Sid's question folds together. *Changing* a fact: never. *Retracting* a fact: a new fact that says it no longer holds; the old one stays in history. *Forgetting* a fact: physical removal, rare, deliberate, and itself recorded.
- REPORTED. The forgetting leaves a permanent trace. "Note that the excise attributes themselves are protected from excision, so there is no way to 'erase your tracks'. Every excision creates a permanent record… Thus excision strikes a delicate balance between forgetting and remembering that you forgot." [D-EXC]
- REPORTED. Forgetting sits outside the timeline. "Note that excision is a special operation that happens outside the timeline of Datomic history, removing data across all of history as if the data never happened. While the excise request itself is transactional, the excision operation is not transactional." [D-EXC]
- INFERRED, from the above. Rama's depot migration is a function applied to every record. This camp would allow it for exactly one purpose: turning a record into a tombstone that says *something was here and was forgotten, by this request*. They would forbid it for repair or reshaping. Repair is a new fact. The convention to fix before the first record is the *shape of a tombstone* and the rule that a migration may only produce one.

**(2) Entity: how is an id made so two never clash? May it give away when or where it was made?**

- REPORTED. In Datomic the single writer mints ids. "Entity ids are assigned by the transactor, and never change." Clients use temporary ids inside a request and the transactor resolves them. [D-IDENT]. No clash is possible because one process hands them out.
- REPORTED. Ids are opaque to programs but not to the machine. "Semantically, entity ids are opaque values uniquely identifying an entity within a database." [D-TXDATA]. Yet: "The partition is encoded via high bits in the entity ID. Therefore, entities in the same partition are sorted together… Partitions are strictly a locality optimization." [D-PART]. The id deliberately gives away *where*, because the id is an index key and its bits decide what sits next to what on disk.
- REPORTED. For ids that must be global, Datomic says use a UUID as a *fact about* the entity, and prefers ones that lead with time. It states the leak plainly: "If the ability to discover the time that a squuid was created leaks sensitive information, then squuids may not be appropriate. However, you should still prefer Squuids (or v7 UUIDs) if your ids may ever be indexed in other, non-Datomic systems." [D-IDENT]
- INFERRED, from unique identity and upsert. Client-minted random ids solve clashing. They do not solve *sameness*. Two agents that ingest the same paper will mint two entities for it. Datomic's tool for this is a unique-identity attribute: "If a transaction specifies a unique identity for a temporary id, and that unique identity already exists in the database, then that temporary id will resolve to the existing entity." [D-IDENT]. For a field seeded from ten million papers, the convention to fix early is which outside keys (DOI, arXiv id, ORCID) are unique identities that the gate checks, so that the second ingest lands on the first entity.
- INFERRED. On the leak question the camp's position is: decide what you want near what. A time-leading id clusters recent writes and leaks creation time. A random id leaks nothing and scatters every lookup. For a store where people's private acts are entities, the leak is real: an id that shows when a private note was made is visible to anyone who is ever handed the id.

**(17) The first facts.**

- REPORTED (practitioner observation of a running system, Francis Avila, Datomic 0.9.5173) [AVILA]. Every Datomic database begins with the same bootstrap transactions. The very first one contains only naming facts, and the first of those names itself: `[10 10 :db/ident …]`. Entity 10 is the attribute `:db/ident`, and the datom that says so uses attribute 10. Small fixed integers are given to the system's own things: `0` is `:db.part/db`, `3` is `:db.part/tx`, `4` is `:db.part/user`, `15` is `:db/excise`, `50` is `:db/txInstant`. The bootstrap transactions are stamped `1970-01-01T00:00:00`. "First non-bootstrap transaction is always T >= 1000." "More bootstrap transactions or datoms may be added in later datomic versions."
- INSTITUTIONAL. So: the first facts are written by the system, not by an actor. They are identical in every database. They carry a time that is openly not a real time. Room below 1000 is reserved so the system's own vocabulary can grow. The `:db` namespace is reserved for the system for ever: "The :db namespace, and all :db.* namespaces, are reserved for use by Datomic." [D-SCHEMA]. The vocabulary for forgetting (`:db/excise`) is among the first facts.
- INFERRED. For Sid: base, the gate, the first kinds and the first grammar need ids that are *the same in every store*, so they must be fixed constants, not minted. The first fact must be the one that lets a name be attached to an id, stated in terms of itself. A reserved range or reserved namespace must exist from the start. The honest stamp for a fact nobody wrote at a real moment is a stated sentinel, not a fake timestamp. And "by whom" needs a first actor that is declared, not discovered: the system itself.
- Second store: identical bootstrap ids are what let two stores understand each other's keys at all.

**(3) Key: a word, or an id with its name and shape as facts about it?**

- REPORTED. Both. "When an entity has an ident, you can use that ident in place of the numeric identifier, e.g. `[42 :person/loves :pizza]` instead of: `[42 1007 1124]`." [D-IDENT]. Attributes are entities so that schema is data: "Because Datomic schema is stored as data, you can and should annotate your schema elements." [D-BEST]
- REPORTED. How they keep it fast: "Idents are designed to be extremely fast and always available. All idents associated with a database are stored in memory in every Datomic transactor and peer." This is also why idents are for schema and enums only, never for ordinary entities. [D-IDENT]
- REPORTED. Rename by accretion: "Both the new ident and the old ident will refer to the entity." "We don't recommend re-purposing an old :db/ident." "Datomic allows multiple :db/idents to refer to a single entity ID." [D-CHANGE], [D-BEST]
- REPORTED. Hickey's position on names is that a namespaced name is already meant to be global, and its meaning is fixed at first use. A changed meaning is a new name. [H-SPEC]
- INFERRED, from *Spec-ulation* plus the `valueType` rule. "The grammar is itself a fact with versions" is safe only when each version accepts everything the earlier one did. Hickey's two kinds of growth are providing more and requiring less. A version that requires more, or provides less, is breakage wearing a version number. The camp's rule would be: an incompatible shape is a new key. Datomic enforces this by making the value type unalterable.
- INFERRED, from limit 5 in section 2.3. If grammars are versioned facts, then some record must say *which version admitted this value*. Datomic never recorded that and reads all of history through today's schema. It documents the consequence. For Sid the natural home is the gate's verdict: it already "names what it checked".
- Disagreement inside the camp: DataScript uses plain words with nothing behind them (section 5.2). Instant uses UUIDs with names as separate rows (section 5.1).
- Second store: in Datomic an attribute's entity id differs from database to database, while the ident is the same. So it is the *name* that crosses stores, and the id that is local. If Sid wants ids to cross stores, key ids must be global from the start (as Instant's are).

**(9) Value: never removed, so how is one deleted, backups included?**

- REPORTED. Three tools, each with a stated limit. Retraction: logical only, history keeps the value. `:db/noHistory`: "The purpose of :db/noHistory is to conserve storage, not to make semantic guarantees about removing information." [D-SCHEMA]. Excision: real removal, at a price: "Excision puts a substantial burden on background indexing. Large excisions can trigger indexing jobs whose execution time is proportional to the size of the entire database, leading to back pressure and reduced write availability. Try to avoid excising more than a few thousand datoms at a time on a live system." [D-EXC]
- REPORTED. The docs argue against their own feature: "Legitimate motivations for removing data are *very rare*. A common desire is to be able to erase mistakes, but this is almost always a questionable idea… (Imagine a source control system that removed the history of code defects once they were fixed)." The one reason they grant: "Privacy laws might require you to excise data from ex-customers." [D-EXC]
- REPORTED. The two features fight: "excision relies on full entity history to identify excision targets. :db/noHistory attributes do not guarantee full history nor precisely when history is removed. Therefore excision cannot guarantee full removal of all datoms with a :db/noHistory attribute from index and log." [D-SCHEMA]
- Backups: not found. The excision page implies old backups still hold the data ("short of restoring a backup"). No page says it outright.
- REPORTED (community, not official). What practitioners do instead: keep sensitive values outside. Waeselynck, 2018: "we avoid storing privacy-sensitive data in Datomic by storing it as values in a complementary, domain-agnostic Key/Value-store, while having the keys referenced from Datomic. To our surprise, we've found that this approach preserves almost all of the architectural advantages of Datomic." [VAL18]. Users also propose crypto-shredding: encrypt per subject, delete the key. No Datomic staff member endorses either in anything I found.
- INFERRED. The lesson for a store that never rewrites: decide *before the first record* which kinds of value may never enter the log in the clear. After the first record it is too late for all earlier facts. The camp's own experience says erasure by rewriting is slow, breaks the guarantee that the past reads the same, and is easy to get subtly wrong across derived indexes. XTDB's answer is in section 4.

**(8) By whom: who checks it? Is a person's agent itself, or the person? Where is "acts for" recorded?**

- REPORTED. Datomic has no built-in actor. It is an attribute the application puts on the transaction: "the purpose of the transaction, the application that executed it, the provenance of the data it added, or the user who caused it to execute." [D-TXDATA]. Nubank does exactly this: "we attach that with the Git version of the service, we attach the credentials of the user that is making that transaction, and several other things." [N-QCON]
- REPORTED. Nobody checks it. Datomic trusts its peers. The docs warn only that "database functions are deployed via transactions, so you should prevent arbitrary transactions from untrusted users." [D-TXFN]
- INFERRED. This is a real gap between the camp and Sid. Datomic's writers are a company's own servers. Sid's writers are people, agents and models that do not trust each other. The camp has no answer to "who checks by-whom" because it never had to. What it does offer is the *place*: actor, the principal acted for, and the grant that allows it are three attributes on the saying, each pointing at an entity. "Acts for" is then a fact like any other, with its own history. Since "transactions can have an open set of attributes", agent and person need not be squeezed into one slot. A slot forces the choice Sid is asking about. An open set does not.
- Missing voice: Fluree signs transactions, so by-whom is verified by cryptography. Noted in section 6.

**(11) When: whose clock? Ever used for order?**

- REPORTED. The writer's clock. "Datomic assigns a :db/txInstant for every transaction." [D-MODEL]
- REPORTED. Never for order. "Datomic's own t time value exactly orders transactions in monotonically ascending order… By contrast, wall clock times specified by :db/txInstant are imprecise as more than one transaction can be recorded in the same millisecond. For filtering databases by time, establishing the relative order of events in a historical database, or any other time-based operation requiring exact precision, you should always use the t (or related tx) value." [D-BEST]
- REPORTED. The clock is kept in step with the order, so a wall-clock "as of" can be mapped onto a position: an explicit instant "must respect the monotonic ordering of wall-clock time, i.e. you must choose a :db/txInstant value that is not older than any existing transaction, and not newer than the transactor's clock time." [D-TXDATA]. XTDB v2 has the identical rule for backfill. [X-TXS]
- INFERRED. For the seed of ten million papers this rule bites. "When the paper was published" cannot be the gate's stamp. It is a domain fact. The gate's stamp can only say when the store learned it. Waeselynck's event time and recording time are the two things, and only recording time belongs to the gate. [VAL17]
- REPORTED, and the camp's reason for one stamp per saying: "entire transactions are never redundant, as the transaction time is always a new fact." [D-TXDATA]. The time is a fact about the act.

**(16) Layer: who sees a fact before any permissions exist?**

- REPORTED. In Datomic, everyone with a connection sees everything. Visibility is a function applied at read time by trusted code: "A filter can be used to e.g. remove datoms that are incorrect, not applicable at a point in time, or not available for security reasons." The docs show a filter that reads an attribute of each datom's *transaction* (`:source/confidence`) and keeps only trusted ones: "Queries using this filter can focus on finding data of interest, without worrying about the cross-cutting concern of how trusted the data is." [D-FILTER]
- INFERRED. That example is the camp's version of Sid's "doubt": a view computed at read time from facts about the saying, never a stored flag. It supports Sid's design of staleness and doubt as walks.
- INFERRED, and this is a challenge. Datomic has no layer. Its three nearest things are separate mechanisms. A *speculative* layer is `with`: "I wonder what this database would look like _if_ I made these transactions. You can do that completely locally." [H-DD]. It is never stored. A *durable private* space is a separate database, joined at read time because "Database is an argument to query." A *trust* view is a filter. Sid's one layer slot does three jobs: it decides who may see, it decides which row wins, and it is part of the cell the gate compares-and-sets. Hickey's word for that is complecting. The camp would ask whether these three must be the same thing.
- REPORTED limit. "as-of Is Not a Branch… with plus as-of lets you see a speculative db with recent datoms filtered out, but it does not let you branch the past." [D-FILTER]. Datomic cannot do durable branches. Sid's layers are durable and shared, so they go beyond what this camp built. Datahike and TerminusDB did build branching (section 6, missing voices).
- For the literal question, the closest voices are Instant (default open) and Fluree (default closed, "Recommended for production"). Sections 5.1 and 6.

**(10) Order: can two gates ever write one layer? What must share a partition? Is "as of" one number or a position per partition?**

- REPORTED. One number, because one order. "Transactions are fully serialized, and transaction ids increase over time, so transaction ids are a total ordering of all transactions." [D-MODEL]. "Peers always see all transactions up to their time basis, in order, with no gaps." [D-ACID]
- REPORTED. Two gates *can* write one ordered unit safely, provided each append is a compare-and-set against the unit's previous position. That is Jepsen's correction, accepted by the Datomic team (section 2.3, item 6).
- REPORTED. What compare-and-set on one cell does *not* give. Hickey, 2009: "You can use CAS, which is essentially saying there's one timeline per identity. Right? And it's uncoordinated. It's impossible to coordinate two things that are using CAS timelines, but CAS timelines are still useful." [H-AWTY]. Sid's gate checks one cell: entity plus key plus layer. That is one timeline per cell. Any rule that spans two cells cannot be enforced by it: a relation and its inverse, a move from one place to another, five facts that must land together or not at all.
- INFERRED. What must share a partition: whatever one gate decision must see consistently and whatever must land together. In Datomic that is the whole database, schema included. That is why Datomic can say exactly when a rule starts to apply: attribute predicates "will be enforced starting on the transaction after they are asserted." [D-SCHEMA]. If Sid's policy and grammar facts sit in another partition from the fact being admitted, the question of which policy was in force has no answer unless the gate writes down the position it read them at.
- INFERRED. "As of: one number or a position per partition?" The camp would say the answer falls out of a prior choice: *what is the unit that has one timeline?* Inside that unit, as-of is one number by construction. Across units it is one number per unit, and a query takes several database values as arguments. Hickey's complaint about sharding is that the units stop being queryable together. His remedy is to choose the unit by meaning: "I called that sort of a 'shard above'. And there are systems that shard below, where it is the system's responsibility to shard. That is always mechanical, and the system does not understand what you are doing, mostly. Sharding above becomes an application problem. On the other hand you have a lot of flexibility." [H-IONS]
- My synthesis, marked as mine and not theirs: the unit Sid already has is the layer. If a layer is the unit of order, then "as of" for an asker is a short list, one position per layer in their stack: base, their own, the session. Three numbers can be said aloud, stored on a fact, and handed to someone else, which is the property Hickey prizes. A position per physical partition cannot. Whether one Rama partition can carry the base layer of a whole field is a question for the Rama group, not this one. Datomic's own guidance is that "operational considerations" begin past 10 billion datoms of history in one database. [D-FAQ]

**(11) Based on: is every read listed, including reads that only made a tool fire, and entries the runtime fills in?**

- REPORTED. In this camp a unit of work reads one database value, and that value has one basis. "You should use a single database v value for a unit of work in order to maintain consistency." [D-BEST]. "Every query we issue to that value of the database has the same basis." [H-DD]
- INFERRED, and it is the camp's main contribution to this question. They would not list reads. They would record *the basis and the question*. With a total order and immutable values, the set of facts read is re-derivable: run the same pattern against the same basis and get the same rows. "Same query, same results." [H-DD]. Under Sid's own rule that re-derivable things are not stored, the rows a pattern returned need not be listed. The pattern and the point must be.
- INFERRED. That makes Sid's second kind of read ("everything matching a pattern as of some point") the general case. The first kind (one fact at its version) is the same thing with a narrow pattern. The third kind (an anchor outside the store) is the only one that is truly a different thing, because the store cannot re-derive it.
- INFERRED. Reads that only made a tool fire: in Datomic's model these are the basis of the saying, like any other read. "Put them on the transaction." The camp would not mix them into the same attribute as the reads the content stands on. See (4).
- A tension to name. Hickey's architecture gets its reach from *not* recording reads. "Queries and reads not part of a transformation never create a transaction." Sid records reads in two places only: on a fact that stood on them, and at a crossing. That respects the principle: a read is written down only at the moment it becomes part of an act. A design that logged every read would undo the separation of perception from process that this camp considers its central idea.

**(4) Does each read say whether the fact depends on it, or whether it was only how the actor got here? Follow the latest, or stay on the version read?**

- INFERRED, from *Maybe Not* and the open attribute set. The camp would not put a flag inside one slot. Two meanings are two attributes. One names what the content stands on. The other names the path that led here. "A straight product type just completely complects the _meaning_ of things with their position in a list." [H-MN]
- REPORTED mechanism, INFERRED application. "Follow the latest, or stay on the version read?" In Datomic a reference points at an *identity*, never at a version. The version comes from the database value you read it through. So the same reference answers both questions: through the old basis it gives what the actor stood on, through the current value it gives what is there now, and the difference between the two *is* the doubt. Hickey's terms: "identity… is just a construct we use to collect the time series", and a state is "a snapshot. This entity has this value at this point-in-time." [H-AWTY]. Nothing has to be chosen when the fact is written, as long as the basis was written.
- The camp has no precedent for the depends-on versus got-here distinction itself. That part is Sid's own.

**(5) If an index was behind the log when a pattern was read, is how far it had really got written down?**

- REPORTED. Datomic removes the problem for its readers. A database value is the stored index merged with recent novelty in memory: "it is a live merge join between the live index and storage." [H-DD]. So a reader is never behind its own basis: "with no gaps". When a reader needs to be at least as far as some point, it says so and waits: "Peers can synchronize on a time basis via Connection.sync." [D-ACID]
- INFERRED. In Rama the indexes do trail the depot. The camp's rule transfers directly: a read is *as of what the reader actually saw*. So yes, write down how far the index had really got, because that position *is* the as-of. Writing the log's head instead would be the map lying. If the reader needs the head, it waits for the index to reach it, as `sync` does.
- REPORTED, XTDB v2, on the same point: `COMMIT SYNC waits for indexing; COMMIT ASYNC returns as soon as the transaction is submitted to the log.` [X-TXS]. They made the gap a named choice.

**(11) Because of: always filled, empty only when it starts a chain?**

- REPORTED principle. "When something is missing from a set, leave it out!" [H-MN]. In a record with fixed places, the start of a chain must hold *something* in that place, and that something is a null with a special meaning. In an open set of attributes, a fact that starts a chain simply has no such attribute, and "the maps know what they know."
- REPORTED precedent. Datomic's own docs model a causal link as an attribute on the saying: `[1235 :correction/for 1234]`. "This might be used to, for example, record that a particular transaction corrects another transaction that was made in error." [D-MODEL]
- REPORTED. Hickey chose the transaction as the path to time because it is "also a path to other information about what happened, including provenance, or causality." [H-DD]. Causality is named as belonging to the saying.

**(1) Version: is a fact pointed at by the gate's number, or by its own id? If its own id: random, or computed from content?**

- REPORTED. In Datomic a fact has no id of its own. It *is* its five parts. You point at an entity, or at a transaction, or you name entity plus attribute at a point in time. Hickey declined to give facts their own identity: "Reified transactions can not be used to model a graph", and "many of the common business uses for reified edges are better handled by reified transactions." [H-ML16]
- INFERRED. So the camp's answer to "gate's number or own id" is: the gate's number, because it identifies the saying and carries order. If you find you need to point at one fact and say things about it, the camp would say you have found an entity you have not yet named.
- REPORTED. Hickey on content hashes as names, 2016: people "like the characteristics of it, in terms of being a universal unforgeable key. But it does not convey anything about order unless you have the rest of the repo. It does not imply anything about causality. I mean '4 is greater than 3' at least says that. It came after." [H-SPEC]. He sees value in both and wants them joined: "I think that there is a way to integrate this stuff."
- INFERRED. For one store, the gate's number is enough and is better, because it orders. For a second store, the gate's number means nothing outside its own store, so something that survives the crossing is needed: a store id with the number, or a content hash. If it is a content hash, read section 4 first, because XTDB shows what a content hash does when the content must be forgotten.

**(6) When 37 replaces 25, is "replacing 25" kept on 37?**

- REPORTED. Datomic writes the replacement into the log. It does not leave it to be worked out. When a single-valued attribute changes, the same transaction carries a retraction of the old value and an assertion of the new one: `[42 :user/favorite-color :green 4567 true]` and `[42 :user/favorite-color :blue 4567 false]`. [D-MODEL]
- REPORTED. Datomic's compare-and-set compares the expected *value*, not a version number: "If the entity has the expected value for the given attribute in db-before, then db/cas will expand to a list form asserting the new value. Otherwise, the transaction will abort." [D-TXFN]
- INFERRED. Sid's offer already states the version it expects to replace. Keeping that number on the landed fact costs a few bytes, and it makes the chain of versions walkable from the log alone, with no index. The camp's habit is to put in the log whatever the log would otherwise need an index to reconstruct. Indexes are derived and get rebuilt. XTDB v1's reindexing "taking days to weeks" shows what it costs when the log is not self-sufficient. [X-HN23]

**(7) Beside each fact: is the gate's yes or no kept? Where? Are refusals kept?**

- REPORTED. In Datomic a yes *is* the transaction entity. There is no separate verdict. A no leaves no trace: "If one part of a transaction fails, the entire transaction fails, and the database is left unchanged." [D-ACID]. The caller gets an exception.
- REPORTED. What a caller gets back on success is richer than yes: "every call to transact returns the database state just before the transaction, the database state the transaction produced, and the set of datoms the transaction expanded to." [JEP]. "What it checked" is the state just before.
- INFERRED. With one total order and a deterministic gate, the verdict is re-derivable: it is a pure function of the state at the previous position and the offer. Under Sid's own rule it would not need storing. That holds only while everything the gate reads sits in the same order as the fact it admits. Once policy and grammar live elsewhere, the gate's reads have their own basis and that basis is not implied by the fact's position. Then the verdict must say what it read, at what position. This is the strongest argument from inside the camp for Sid's "its yes or no is itself a fact naming what it checked."
- REPORTED. Refusals: Datomic does not keep them. XTDB v2 does (section 4). Hickey's principle points toward keeping them: "you have to reify process. You have to turn it into a thing that you can look at and touch." [H-DD]. Nubank's regret points the same way: "We don't see every request that happens before the database write and the events are effectively lost." [N-QCON]

**(12) Start of a session: is the runtime version and the kind of machine written down? Are rebuilds facts?**

- REPORTED practice. Nubank puts the code version on every transaction: "we attach that with the Git version of the service." [N-QCON]. The Datomic docs list "the application that executed it" among the things to record on a transaction. [D-TXDATA]
- REPORTED. Hickey on the right identifier for running code: a git commit, because "that is the identity. I hate human made up 'version 2'… These things mean nothing." A build that is not a clean commit is labelled "unreproducible" in its name, so that nobody mistakes it. [H-IONS]. That is the map not lying, applied to the runtime.
- INFERRED. One session entity holds the runtime's commit and the machine. Each saying points at the session. Written once, not per fact. A rebuild is a new session entity with a new commit. It is a fact because it happened and because later doubt will need it: "which facts were admitted by the gate as built from commit X" is a walk, provided the commit was written.

**(14) The hand: which motions become facts by default? Is being shown the same as looking?**

- REPORTED. The camp's line is between information and "stuff". "There are other things you might use storages for, other than information. Sometimes you need a place to keep stuff. Datomic is not about keeping stuff." [H-DD]. High-churn values get no history: "For high churn attributes, such as a counter or version incrementer, the cost of storing history is frequently not worth the impact on database size or indexing performance." [D-BEST]
- REPORTED regret. Nubank: Datomic "is really good for high-value business data. It doesn't work that well for fire hose writes… So we use Datomic as the default database for everything, and you are lulled into the false sense of security." [N-QCON]
- INFERRED. Pan and hover are a firehose. Selecting and pointing are different when they end in an act: then they are part of what that act stood on, and they ride on its saying. The camp's default would be: a motion becomes a fact when it becomes the basis of something, not before.
- INFERRED, from Hickey's definition of a fact as "an event or thing known to have happened". The system *knows* it showed something. It does not know the person looked. "Shown" is a fact the store can attest. "Looked" is a conclusion someone draws. The camp would record the first and derive the second, and would not let the record of the first be read as the second.

**(15) A click: which tools may act in a person's name?**

- The camp is nearly silent. Datomic runs transaction functions inside the writer and trusts whoever can transact. The one relevant warning is the one quoted under (8).
- INFERRED. Since tools are facts and policies are facts, "may act for" is a fact the gate reads. The camp's only contribution is from *Spec-ulation*: a grant, once relied on, should grow and not be silently narrowed. Narrowing is a new grant with a new name.

**(13) Storage: plain maps or classes? Ever trimmed? Backups?**

- REPORTED. Plain data. "Did we use print / read on Clojure data relentlessly, because it is a cheap way to get serialization? Absolutely… If you do not consider doing that already in your programs, just do it." [H-WD]. "All interaction with Datomic is represented by data." [H-IM]. And the long argument with Alan Kay is about exactly this: facts are data, and "putting facts behind a dynamic interpreter… breaks the idea of data." [H-HN16]
- INFERRED. A class is a place-oriented record whose meaning lives in code that will be rebuilt hundreds of times. Plain maps with named keys outlive the runtime. The runtime is the one thing in Sid's system that is not made of facts, so nothing in the log should need the runtime's classes in order to be read.
- REPORTED. Trimmed: only by declared exception (no-history attributes, excision), plus garbage collection of old *index* segments, which are derived. "It should not be surprising when you move to an immutable process where new information requires new storage that you end up with… garbage on disk, and garbage collection for disk." [H-DD]. The log itself is never trimmed in Datomic.

### 2.5 What resembles Sid's situation, and what differs

**Resembles.**

- Facts are small, immutable, and carry a path to time.
- Schema, and in Pro even code, are facts in the same store.
- One gate. Offers and facts are different things (Datomic now says "request" and "datom").
- The store is meant to explain itself: "it is a database that greatly facilitates your knowing why it is in the state it is in, and how it got there." [D-EXC]
- Time travel is used for exactly what Sid wants it for: "what was I looking at", "what was given". That is recording time, which is the use Waeselynck says works.

**Differs. These limit the transfer.**

- **Closed world versus the planet.** Hickey chose the closed-world assumption to avoid "universal naming, open-world, shared semantics". Sid's base layer is universal naming and shared semantics.
- **Trusted writers versus strangers.** Datomic never verifies an actor.
- **Write rate.** Hickey's claim is that 99% of businesses cannot saturate one box. Tens of agents per person at machine rate is the other 1%, and he says plainly his system is not for it.
- **Size.** Guidance starts to bite at 10 billion datoms of history per database. A field of ten million papers with summary layers, plus crossings, passes that soon.
- **Reads.** Datomic never records a read. Sid records reads as provenance and at crossings.
- **Unit of admission.** Datomic admits a *set* of facts at one point in time. As the brief describes it, Sid's offer is one fact. If that is right, there is no way to land several facts together, and nowhere for shared provenance to live but on each fact. If Sid already has a unit of saying, this difference disappears.
- **Layers.** Datomic has none, and cannot branch.

### 2.6 Who they disagree with, and on what

- **XTDB**, on time. Datomic: transaction time only, everything else is an attribute. XTDB: valid time is a first-class axis on every record.
- **XTDB**, on erasure. Datomic: an exception bolted on. XTDB v1: designed in from the start.
- **Partitioned logs** (Kafka, Rama). Hickey: shards are independent databases, and you lose query, transactions and consistency across them.
- **Huahai Yang** (Datalevin): "Database is where immutability may not be a good fit, at least not all the time." [Y-HN]
- **Nikita Prokopov** (DataScript): attributes as plain words, no history.
- **Alan Kay**, on whether data without an interpreter is a good idea at all. Hickey: data is the more fundamental idea.
- **Jepsen**, on whether a transaction is a set or a sequence. Resolved by changing the docs, not the system.

**What Datomic implies for a second store.** Entity ids are local to a database ("database-unique"). Names are not: the ident `:person/email` means the same in every database that uses it, and the system's own vocabulary has the same ids everywhere. The order number `t` is local. So across two Datomic databases, what travels is names, outside keys held as unique identities, and wall-clock time as a rough guide. What does not travel is an entity id, a transaction number, or a basis. A query can take both databases as arguments, each with its own basis. Nubank's experience (next section) is that this works for a handful of databases and stops working for analytics across thousands.

---

## 3. Nubank: Datomic lived with, at scale

### 3.1 What they built, and what they chose

A bank on Datomic, from 2013. Datomic is the default system of record. One database per microservice. Later, whole copies of the stack, each serving a slice of customers.

The numbers, with dates. About 2016: "over a dozen independent Datomic databases" [N-STORY]. May 2024: "an average of 2.5 billion Datomic transactions being processed each day" [N-JEPBLOG]. July 2025, a Nubank conference abstract: "3000+ Datomic databases, 4000 microservices, and process over 70 billion Kafka events/day" [N-DEVBCN]. Nubank bought Cognitect, Datomic's maker, in 2020.

They use the reified transaction exactly as Hickey intended: "transactions in Datomic are a first-class concept, and we attach that with the Git version of the service, we attach the credentials of the user that is making that transaction, and several other things." [N-QCON]

### 3.2 Their reasons

> "And we use Datomic for most transactional workloads, it is like Git for your data. You never lose anything, you made a fact true, and then later not true, but you never do an update in place and lose that history." — Edward Wible [N-QCON]

On how they scale, which is not by partitioning a database:

> "So we considered a different model, the scalability unit model. When you are doing scalability units, your shards are not database shards, they are actually copies of your infrastructure… we build clones of the infrastructure and we assign different partitions of our customer base to different clones, those are our shards." — Rafael Ferreira [N-QCON]

> "Our primary database, Datomic, runs multiple transactors in Kubernetes, each handling a subset of data… Nubank took this idea a step further by sharding not just databases, but also key services and entire microservice clusters." [N-LIMITS]

This is Hickey's "shard above", done by the people who later bought the product. It is forced by the product: "In on-prem you should run a single primary logical DB per transactor." — Marshall Thompson, 2020 [M-SLACK]

### 3.3 What they regret

All four regrets are about the moment a fact is made.

1. **An owner was not written on every saying.** "one thing that we do not add and I would have liked to add, a customer identifier. Because, if every transaction had an identifier that could point to the actual customer that owns that data, things like splitting databases for sharding would have been much, much easier. That's a small detail, but something I would like to get better in the beginning." [N-QCON]. The InfoQ transcript does not label the speaker. It is one of the two presenters, Edward Wible or Rafael Ferreira.

2. **Acts that left no trace of their origin.** "what I regret the most, is to have implicit operations that are not stored into the database, or that don't have origination information. That was one of the earlier mistakes, the major mistakes that we had." — Lucas Cavalcanti, 2021. He ties it to a regulated industry: "that ended up harming us a lot, the ability to explaining things that happened." [N-POD]

3. **Offers that never became facts were lost.** "something I would do differently is think about event sourcing a little bit earlier. We have it down streamed from Datomic, but not up streamed. We don't see every request that happens before the database write and the events are effectively lost." [N-QCON]

4. **The fact store was used for things that are not facts.** "it is really good for high-value business data. It doesn't work that well for fire hose writes, or long strings… So we use Datomic as the default database for everything, and you are lulled into the false sense of security, I will start a service with Datomic, when the answer should be S3." [N-QCON]

A fifth, about reading across many databases: "What did not scale well was using Datomic and our operational transactional infrastructure for aggregations, even for simple things, like how many customers we have. Our CEO asked this many times and it was increasingly difficult to answer… even, with sharding and fragmenting the operational systems so that they scale better, you are making analysis harder." They built an ETL to a separate analytic store. [N-QCON]

### 3.4 Which questions they speak to

- **(10) order.** INSTITUTIONAL. One total order per database, thousands of databases, the split chosen by meaning (service, then customer). They never tried to make one database carry the bank.
- **(8) by whom and (12) runtime version.** REPORTED. Both are on every transaction: the user's credentials and the git version of the service. This is practice since the early years, and they name no regret about it.
- **(7) refusals.** REPORTED regret 3. They wish they had the requests that came before the write. That is the offer stream, refusals included.
- **(11) because of, and the premise.** REPORTED regret 2. "Origination information" is because-of and by-whom. Their worst early mistake was acts without it.
- **(16) layer.** INFERRED from regret 1. Sid's layer slot is close to the identifier Nubank wishes it had written: it says whose context a fact lives in. Where Sid's design differs is the base layer. A fact *about* a person that sits in the shared base carries no mark of whose it is. Nubank's regret was about splitting later. The same missing mark would make erasure by subject hard. If Sid wants either, the mark has to be there from the first record.
- **(14) the hand.** REPORTED regret 4. Firehose data does not belong in the fact store.
- **(9) erasure.** Nothing found. Their only public statements on personal data are about encryption at rest and keeping services separate.

### 3.5 Resembles and differs

Resembles: years of operation, audit permanence by law, very many writers, real scale, provenance on every saying. Differs: a closed world with one owner, trusted writers, each database belonging to one service, no shared layer anyone may write.

### 3.6 Who they disagree with

Quietly, with two of Hickey's claims. "99% of the businesses… could not saturate one box": they did, and cloned the stack. "Queries can take more than one database": true, and it did not hold for analytics across a fragmented estate.

---

## 4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson)

This is the only team in the camp that designed immutability and erasure *together*, shipped it, lived with it, and then rewrote the system. That makes it the best evidence on ids computed from content meeting deletion.

### 4.1 What they built, and what they chose

**Version 1** (Crux, 2019; renamed XTDB). Three separate parts:

- a **transaction log**: one Kafka topic with a single partition. It is immutable. It holds operations, but each document in an operation is replaced by its hash.
- a **document store**: documents keyed by the hash of their content. This part can forget.
- an **index** on every node (RocksDB), built by reading the whole log.

Every record has two times: system (transaction) time and valid time.

**Version 2** (early access April 2023, launched 12 June 2025). A rewrite. Columnar files (Apache Arrow) in object storage, arranged as a log-structured merge tree. SQL first. An `ERASE` statement. The log is no longer the permanent record. Version 2.1 added several databases per cluster. Version 2.2 (September 2026) made indexing single-writer, with a second log of resolved transactions.

### 4.2 Their reasons

**Why hashes on the log: so the log never has to change.**

> "we hash the {:xt/id :luke, :name "Luke"} document… we store the document, keyed by its hash… we put the transaction operation on the transaction log, but replace the document with its hash… We do this for eviction - this distinction between documents and transactions means that, if Luke were to request that we irretrievably remove all his data (e.g. due to a GDPR/CCPA request), we only need to forget the content of the document and the transaction log can stay immutable." — James Henderson, 2020 [X-DOC]

> "We use two topics because whilst the transaction topic is immutable, messages in the document topic can be permanently erased, forming the basis of Crux's ground-up strategy to provide ease of content eviction for data privacy reasons, to align with compliance regimes such as GDPR. Using a separate topic for the content documents also allows for compaction to remove duplicates, as the message ID is a content hash of the document. From a Kafka perspective, the transaction topic uses a single Kafka partition." — JUXT, 2019 [X-INTRO]

The original design card set the goal: "It should be a relatively cheap operation, and not require rebuilding the entire topics or indexes." — Råberg, 2018 [X-32]

**Why two times.**

> "In situations where your database is not the ultimate owner of the data—where corrections to data can flow in from various sources and at various times—use of transaction-time is inappropriate for historical queries." [X-BITEMP]

They name three drivers: "lag, corrections, and efficient auditability." And a rule of thumb from the v2 docs: "In short, any time you hear the phrase 'as of' or 'with effect from' in a requirement, the answer is probably 'valid time'." [X-TIME]

**Why one order.**

> "All transactions that perform writes are serialized via a totally-ordered durable log." [X-TXDOC]

For Kafka the partition count must be "Exactly 1. A single partition is what makes the log strictly ordered." The node checks this on an existing topic. [X-KAFKA]

> "writes for a given database happen once, on a single thread, and reads scale out across the cluster. This design implies a hard upper limit on transaction throughput, but the key advantage is the concrete information guarantees about exactly when, how & why data across the database has changed." [X-WHAT]

On the limit itself: "You can get a hell of a lot done in a single thread - locally, we've seen 300k docs/second written on a single thread when you take network latency out." [X-LEAD]

### 4.3 What they changed, and what bit them

1. **Erasure broke the one guarantee.** "It is important to note that Evict is the only operation which will have effects on the results returned when querying against an earlier Transaction Time." [X-V1TX]. They never found a way round this. Nobody has.

2. **Forgotten content broke replay.** "The indexer will pause consumption of transactions while waiting for all their documents to appear. When evicting documents they will have been compacted in Kafka, so replay will just block with the current behaviour." — Råberg, 2019 [X-184]. The fix was a tombstone: the log's hash must always resolve to *something*, either the document or a marker that it was evicted.

3. **The same content coming back.** "There is a race condition which would happen if someone tries to resurrect a document in quick succession of evicting it, or replaying it. We also need to mitigate someone submitting a new document for an evicted entity." — Råberg, 2019 [X-432]. That issue is still open. With content-addressed ids, identical content *is* the same id. So forgotten content that arrives again cannot be told from the original unless the store keeps a permanent marker that this content was evicted.

4. **Rebuilding indexes by replaying the log became unbearable.** A production user, 2023: about 600 GB and 100 million transactions, and a minor upgrade meant "a full reindex of the main 'golden' data store taking days to weeks." Jeremy Taylor's reply: "XTDB 2 will compute and maintain incremental indexes on-the-fly based on the raw data… This also means the transaction log is now ~ephemeral in the new architecture (no more event-sourcing-style replays required, ever)." [X-HN23]. Version 1 had said the opposite: "The event-log that Crux uses is the golden store of data, with Crux leveraging Kafka's infinite retention capability." [X-INTRO]

5. **A full copy on every node.** Version 1's index "requires the underlying Key-Value store (e.g. RocksDB) to exist on every node with a full replica of almost all data." [X-DD7]. Version 2 moved to shared object storage.

6. **Anything-goes values.** Version 1 "allows almost any data to be stored including deeply nested documents with arbitrary java.io.Serializable types." Looking ahead to version 2: "Apache Arrow data types are more restrictive — in a way we consider hygienic, not detrimental." [X-DD7]

7. **Whole documents as the unit of change.** "The main downside of XTDB's document model is that re-transacting entire documents to update a single field can be considered inefficient." [X-FAQ]. They saw Datomic's small facts as the more exact tool: "Datomic's datom model provides a very granular and comprehensive interface for expressing novelty."

8. **Determinism on every node, relaxed in 2.2.** "Since its inception, XT has had an architecture where every node reads the whole log and resolves all of the transactions… it means that every transaction has to be deterministic - that every node has to arrive at the same state reading the same messages… it did mean that any kind of error had to restart all of the nodes." And, candidly: "even in hindsight I wouldn't have changed that decision, it got us off the ground." [X-LEAD]. What it cost: "every feature had to be expressible as a pure, deterministic function of the log — which ruled out anything that wanted to draw per-transaction metadata (e.g. tx-id, system-time) from outside the log." [X-DBS]. From 2.2 one leader reads the *source log*, resolves each transaction, and writes the result to a *replica log* that the other nodes follow. "'Transaction resolution' here means turning SQL (which may read existing data in order to determine what changes to make) into simpler put, delete and erase events (which don't)." [X-LEAD]

9. **Erasure in version 2 is eventual, and happens by not copying.** `ERASE` "Irrevocably erases documents from a table, for all valid-time, for all system-time." But: "The ERASE is effective as soon as the transaction is committed - no longer accessible to an application - and under the hood the relevant data is guaranteed to be fully erased only once all background index processing has completed and the changes have been written to the remote object storage." [X-SQLQ]. The mechanism, from the implementer: "I am stopping copying compaction history when an `erase` event is encountered for a given `iid`." [X-4036]. Files are immutable. A compactor periodically writes new files from old ones. Erased data is simply not carried forward.

10. **A generation number for the log.** "Epochs allow a cluster to safely reset its log state following partial log loss, corruption, or intentional recovery operations, without requiring full reindexing of storage data… Epochs only move forwards." [X-LOG]

### 4.4 Which questions they speak to

- **(0) and (9): never rewritten, never lost, erasure.** REPORTED. Two designs, both lived with. *Version 1*: keep the log immutable for ever by never letting erasable content into it. The log holds a hash. The content sits where it can be forgotten. *Version 2*: nothing is physically permanent. Files are immutable, a compactor rewrites them, and forgetting is what the compactor leaves behind. INFERRED for Sid: version 1's split is the closest known fit to "the store never rewrites". It maps onto Sid's third kind of read, the anchor to something outside the store. But take the three lessons with it. A pointer into the forgettable place must always resolve, to the content or to a tombstone. Rebuilding anything from the log must survive missing content. And erasure still changes what past reads return, which every walk of based-on must be able to say out loud: *this read stood on something since erased*.
- **(1) content-computed ids.** REPORTED. XTDB v1 used them for documents, and named three benefits in one paragraph: erasure, de-duplication, ordering. The costs are items 2 and 3 above. INFERRED: version 2 identifies a row by a user-supplied `_id` and no longer exposes a content hash, so at the level of a record they moved off it. I found no statement saying so. What stayed content-determined is the *file*: "if two nodes upload the same file, we know that they have the same content." [X-BI3]. *My own note, not from their writing:* a content hash left in a permanent log still says something after the content is gone. Anyone who can guess the content can confirm it was there. For short, guessable values (a name, a diagnosis, a yes or no) a bare hash is not erasure. A salt per record, itself forgettable, closes this.
- **(11) when, and valid time.** REPORTED. XTDB's disagreement with Datomic is that the time a thing was true is not the time the store learned it, and that both belong on every record. For a field seeded from papers this is live: publication dates, retractions of papers, corrections that arrive years late. Datomic's answer is that valid time is an ordinary attribute. XTDB's answer, in my paraphrase, is that it must be an axis on every record, because otherwise every application rebuilds it, and badly. Waeselynck, a Datomic user, ends up agreeing with XTDB on the diagnosis. [VAL17]
- **(10) order.** REPORTED. Single partition, enforced. "Hard upper limit." Several databases per cluster since 2.1.
- **(7) the gate's yes or no, and refusals.** REPORTED. This is the camp's precedent for Sid's gate. In XTDB, transactions are appended to the log *before* they are judged: "unconfirmed transactions are optimistically appended, and therefore a transaction in XTDB is not confirmed until a node reads from the transaction log and confirms it locally." [X-FAQ]. Every submission gets a number and a place in the order, whether or not it commits. The verdict is kept, refusals included. The docs show the table: `_id | committed | error | system_time`, with a row `2 | f | ... "Precondition failed" ...`. "Check the xt.txs table for the transaction result to see if the assertion failed." [X-TXS]. Since 2.1 a caller can attach its own metadata to a transaction, stored beside the verdict: "you might use this to attach upstream request IDs, correlation IDs, or other data lineage information." [X-TXS]
- **(12) and the runtime that will be rebuilt hundreds of times.** INFERRED from item 8. XTDB began by recording only offers and making every node re-derive the outcome. That forces every future build to reproduce every past verdict exactly, for ever. Seven years on (Crux previewed in April 2019; the single-writer post is dated 17 September 2026) they started recording the resolved outcome as its own log. Sid's gate is the only writer and its verdict is a fact, so Sid is already on the side XTDB moved to. The lesson is to stay there: the record is what the gate decided, never a promise that a later gate would decide the same.
- **(5) index behind log.** REPORTED. The gap is a named choice per commit: "COMMIT SYNC waits for indexing; COMMIT ASYNC returns as soon as the transaction is submitted to the log." [X-TXS]
- **(13) storage.** REPORTED item 6. They let arbitrary serialized classes in, and later called the stricter typed format hygienic.
- **(0) again: log generations.** REPORTED item 10. A position in the log is really a pair: generation and offset. If the first record's position has no room for a generation, a recovered log cannot be told from the original.

### 4.5 Resembles and differs

Resembles: immutability and erasure designed together; offers appended then judged; refusals kept; one partition; a runtime rewritten from scratch while the data had to survive. Differs: the unit is a document or row, not a fact; no provenance per record beyond the transaction; no permissions at the level of a fact; one organisation per database.

### 4.6 Who they disagree with

- **Datomic**, on valid time, and on the writer: Datomic has an active transactor, XTDB "a passive transaction log". [X-FAQ]
- **Their own version 1**, on whether the log is the permanent record.
- **Datalog purists**, themselves included: "At JUXT, we absolutely love Datalog — but we're Clojure developers… We have routinely heard that 'there's just no escaping SQL' and we believe it." [X-DD7]
- **Event sourcing as a habit**: "Event sourcing should be for, well, events — not a bandaid on the fact that almost every major database throws out data with each UPDATE operation." [X-DD7]

**What XTDB implies for a second store.** Version 2.1 lets one cluster serve several databases. Version 2.2's single writer was introduced partly to allow "change-data-capture-fed secondaries". So their road to a second store is: one store is the writer of record, another follows its resolved log. Not two equal writers.

---

## 5. The leads

### 5.1 Instant (Stepan Parunashvili, Joe Averbukh)

Triples in Postgres, a Datalog-like query language, permissions, and sync to browsers. Context: in 2026 the team announced it is joining OpenAI, and the hosted service closes on 31 August 2027. The code is open source. Nikita Prokopov had joined them. [I-OAI], [I-SYNC]

**How Instant identifies attributes. This speaks directly to (3).** REPORTED from their source. The attribute slot of a triple is a UUID: `attr_id uuid REFERENCES attrs(id)`. Names live in a separate table: `idents (id uuid, attr_id uuid, etype text, label text)`, unique per app on `(etype, label)`. [I-SQL]. In code an identity is "an id, etype, and label (in that order) but we consider the ident name to simply be the etype and label." [I-ATTR]. The client mints these ids itself: `const attrId = uuid(); const fwdIdent = [uuid(), etype, label];` [I-INSTAML]. A rename updates the names and touches no triples. [I-ATTR]

So Instant reached Datomic's answer on its own: id in the fact, name as a record about the id, rename without rewriting. It went one step further than Datomic: the ids are global UUIDs, minted at the edge. They never say why. INFERRED from the code: renames are safe, and a client can create an attribute while offline.

**(16) Who sees a fact before any permissions exist?** REPORTED. Everyone. "If a rule is not set then by default it evaluates to true." "By default, all permissions are considered to be 'true'. To change that, use the '$default' key." Rules are expressions per namespace for view, create, update, delete. View rules run after the query: "On the backend every object that satisfies a query will run through the `view` rule before being passed back to the client." All rules for an app are one JSON blob in one row. [I-PERMS], [I-RULES]

**Not append-only.** REPORTED from code. Triples are upserted and deleted in place. There is no transaction column on a triple and no history table. [I-TRIPLE]. Before building, Parunashvili wrote: "And god forbid an error happens and we accidentally delete data. In a world of facts there would be no such thing — you can just undo the deletions. But alas, this is not the world most of us live in." [I-SPEC]. What shipped is undo for deleted *attributes* only: "we implemented soft deletes at the column level. Even if a rogue agent deletes your columns, you can undo it." [I-ARCH]. They wanted the world of facts and did not build it. The one place they added undo was the place agents hurt them.

**(10) Order.** REPORTED. From one Postgres: a transactions table with an increasing id, and the write-ahead log feeding an "invalidator" that works out which live queries a change affects. [I-ARCH], [I-TXSQL]

### 5.2 Nikita Prokopov (DataScript)

DataScript is Datomic's data model in the browser, written because Datomic's core was closed: "While Cognitect still hasn't open-sourced these, I started DataScript to cover that breach." [T-PROTO]

**What he dropped, on purpose.** "No `:db/ident` for attributes, keywords are _literally_ attribute values, no integer id behind them." "Simplified schema, not queryable." [T-README]. And history: "There's no history tracking at DB level. When datom is removed from a DB, there's no trace of it anywhere. Retracted means gone." [T-INT]. The reason is where it runs: "DataScript DBs operate in constant space… This is unlike Datomic which keeps history of all changes, thus grows monotonically." [T-README]

So on (3) he is the camp's voice for *words*. The argument is simplicity at small scale, in one process, with no renames to survive. It is not an argument that holds for a shared store meant to last years.

**The Web After Tomorrow (2015) is about "match, don't route".** REPORTED.

> "The same query will then be used to filter whole-DB changelog and decide what parts of it server should push to which client. Fetch is about trying to get the data given the query. Push is about finding the affected subscriptions given the changed data." [T-WEB]

> "Datomic has RDF-like data model (datom = entity, attribute, value) which is at a great granularity level for security and subscription filters. Datom is the smallest piece of information that could be synchronized." [T-WEB]

And the part he says is unsolved:

> "Subscription language is the biggest missing piece of this stack. Datomic and DataScript speak Datalog which is very powerful language, but it is hard to reverse efficiently. If you have very high volume of the transactions going through your DB, for each of them you'll have to determine which clients should get an update. Running a Datalog query per client is not an option." [T-WEB]

INFERRED for Sid. A tool's signature is a subscription. For every landed fact the runtime must find the tools it matches, at machine rate. Prokopov's warning is that the signature language must be *weaker* than the query language, so that it can be run backwards: from a fact to the signatures that want it. Sid's rule that patterns match on the key is exactly this kind of weakening. It is also one more reason the key must be a first-class, indexable thing fixed before the first record. Instant built the same piece and called it the invalidator.

### 5.3 Huahai Yang (Datalevin)

The dissent from inside Datalog. Datalevin keeps Datomic's query language and drops its premise.

> "To keep things simple and familiar, Datalevin behaves the same way as most other databases: when data are deleted, they are gone." [Y-README]

> "Database is where immutability may not be a good fit, at least not all the time. In many use cases, database is where application state resides, hence a mutable database is a better fit." — 2025 [Y-HN]

An earlier README put it flatly: "Datalevin is not an immutable database, and there is no 'database as a value' feature. Since history is not kept, transaction ids are not stored." [Y-OLD]. His README cites Waeselynck's essay and the Jepsen report as his reasons.

On (3) he sides with Datomic without fuss: "Attributes are stored in indices as integer ids… This is the same as Datomic." [Y-ALTER]

His value to Sid is one distinction: *application state* is not *information*. It is Hickey's own line ("Datomic is not about keeping stuff") drawn by someone who thinks most data falls on the other side of it.

---

## 6. The group: who matters most, who I dropped, who is missing

**The three voices that matter most for Sid.**

1. **Rich Hickey and the Datomic record.** Not mainly for the five-tuple. For three things. The *saying* as the home of provenance. The discipline about names: an id in the fact, a name as a fact about it, never break, a new meaning is a new name. And the honesty about scope: closed world, one box, "shard above". Hickey tells Sid exactly which parts of his reasoning stop holding at the scale of the planet.
2. **JUXT and XTDB.** The only team that took "immutable" and "must be able to forget" as joint requirements from the first day, and then wrote down what broke. Their version 1 is the closest existing design to "a store that never rewrites and can still forget". Their version 2 is the record of what that cost. They are also the precedent for Sid's gate: offers appended, judged afterwards, refusals kept.
3. **Nubank.** Eleven years on the design, under audit law, at a scale near the one Sid holds in mind. Every regret they name is about something not written at the moment of the act: the owner, the origin, the refused request. That is Sid's question, answered from the far end.

A fourth for one question only: **Prokopov**, because *The Web After Tomorrow* is the clearest written statement of match-don't-route in this camp, and of why the matching language has to be weaker than the query language.

**Dropped.** Datalevin as a system: it speaks only by refusing the premise, and I kept that one sentence. DataScript as a store: in memory, no history, one process. Instant as a store: not append-only. I kept Instant for two facts, UUID keys and default-open permissions.

**Missing. They belong in this camp and I did not go deep.**

- **Fluree.** Datomic-shaped facts in an append-only ledger, with signed transactions and policy stored as data. I checked one page. It answers (16) the opposite way to Instant: "default-allow: false — Fail-closed. A flake with no targeting policies is denied. Recommended for production." and "default-allow: true — Fail-open… Useful in development or in deployments where an application layer handles authorization and Fluree is recording signed transactions for provenance." [FLU]. Fluree is the one system here that verifies by-whom with cryptography. It deserves a proper read for (8), (15) and (16).
- **Datahike** (Christian Weilbach) and **TerminusDB**. Datomic-like and RDF-like stores with durable branches and merging. Datomic says "as-of is not a branch". These two built the branch. They bear directly on layers. Not examined.
- **RDF named graphs and W3C PROV.** A quad's fourth part names the graph a statement belongs to. That is Sid's layer slot, twenty years early, along with a standard vocabulary for derivation and attribution (PROV's `wasDerivedFrom` and `wasAttributedTo`, named from memory, not checked this session). Hickey set RDF aside because "without a temporal notion or proper representation of retraction, RDF statements are insufficient for representing historical information." [H-IM]. The named-graph community's experience with one context slot doing several jobs would be worth a session.
- **Mozilla Mentat.** A Datomic-like store for one person's data, meant to sync across devices. Abandoned. A cautionary tale for personal layers. Not examined.
- **Eve** (Chris Granger). Everything, the interface included, as facts in one store, with programs as facts. The nearest thing I know of to a medium that is rebuilt from inside itself. Not examined.
- **Tim Ewald's "Reified Transactions" talk** (Datomic Conf 2015). The Datomic blog points to it as the full treatment. [D-BLOG15]. Not opened.
- **Val Waeselynck.** I used two of his essays. He is the practitioner who wrote most about living with Datomic, and would repay more.

---

## 7. What this camp would question above the table

Taking the ALSO OPEN list in order, then what they would add.

**"An append-only store of small facts as the one substance for everything."** They agree on facts and on small. "A Datom is a minimal and sufficient representation of a fact." They reject "everything". Hickey: "Datomic is not about keeping stuff." Nubank: not firehose writes, not long strings. Yang: application state is not information. Their test for whether something belongs is whether someone will later need to reason from it. A cursor position fails. A selection that led to an act passes, as part of that act.

**"Tools, grammars, policies, and definitions being facts in the same store."** Datomic did this for schema and, in Pro, for code. Three lessons from living with it. (a) Schema facts have history, but reading the past through a past schema was never built, and is documented as a limit. If grammars have versions, some record must say which version admitted each value. (b) Code as data gave way to code named by a git commit. The commit is the identity and an unreproducible build says so in its name. Sid's third kind of read, the outside anchor, is the natural fit for a tool's body. (c) "Enforced starting on the transaction after they are asserted" only means something when rule and data share one order. If policy facts live in another order from the facts they govern, the gate must write down where it read them.

**"One store for the planet with personal layers instead of personal databases."** The camp's deepest doubt. Hickey chose the closed world to avoid "universal naming, open-world, shared semantics". He says his design is wrong for "arbitrary write scaling". Both product teams state a hard ceiling per database. The camp's lived answer at scale is many databases, split by meaning, joined at read time. But the camp has no *good* answer for the planet either. Hickey's complaint about sharding (no query, no transactions, no consistency across shards) applies to the many-databases answer just as much, and Nubank confirms it: counting customers across the estate became "increasingly difficult" and needed an ETL. So what transfers is not the advice to use many databases. It is the narrower point: *one order per unit, and choose the unit by meaning.* Sid's layer is a unit chosen by meaning. Whether a layer can also be the unit of order is a question this camp raises and cannot settle.

**"A fixed nine-part envelope both ends share forever."** Their sharpest objection, with a constructive alternative. A fixed record is place-oriented: "If you have places, you have to have something in the place." Provenance on every fact is, in Hickey's words, less efficient and less *correct* than provenance on the saying. "Otherwise datoms become enormous." Yet they do accept a fixed tuple at the bottom. Theirs has five parts, and the test is simple: every fact always has every part, and no part is ever empty. Put Sid's nine through that test. Entity, key, value, version and layer pass. By-whom and when are always present but are the same for everything said in one act. Based-on and because-of are sometimes empty. By the camp's rule the last four are attributes of the saying, held in an open set that can grow by accretion, and only the first five are the envelope. "Forever" then has to hold for five things, not nine.

**"Running answers never stored, only crossings recorded."** Strong agreement, in Hickey's own words. A derivation that updates live is not data: "'date-of-birth' is data and 'age' (unless temporally-qualified, 'as-of') is not." A crossing is a derivation pinned to a basis: *this was shown, built from these reads.* That is an event that happened, so it is a fact. It is also Datomic's rule about caching: cache the sources of answers, never the answers. This project's standing rule against reaching for caches has the same root.

**"Every read recorded as provenance on every fact."** They would cut this twice. Not every read: the basis and the question, because the rows are re-derivable from those. Not on every fact: on the saying. And they would hold on to the principle underneath, that perception is free. "Queries and reads not part of a transformation never create a transaction." A read is written down only when it becomes part of an act. Sid's design already has this shape, since reads appear only on facts and at crossings. The risk is volume: if every render of every screen is a crossing, the crossings become Nubank's firehose.

**"One writer."** Restated by Jepsen and accepted by the Datomic team: safety does not come from there being one process. It comes from each append being a compare-and-set against the last position of its ordered unit. "Any number of concurrent transactors ought to be safe." The thing to fix before the first record is what the ordered unit is, and that every append names the position it follows.

**What they would add that is not on the table.**

1. **The unit of saying.** The nine parts have no transaction. As the brief describes it, an offer is one fact. Then there is no way to land several facts together, and no single place for their shared provenance. Both product teams treat a set of facts at one point in time as basic. Halloway: "intermediate database states are inexpressible." If Sid's design already has this unit, the camp's main objection to the envelope goes away with it.
2. **Sameness, not only uniqueness.** Random ids guarantee two things never clash. They also guarantee the same paper ingested twice becomes two entities. Datomic's unique-identity attributes, checked at the gate, are the camp's tool.
3. **What may never enter in the clear.** After the first record this cannot be fixed for earlier facts. XTDB v1 decided it on day one. Datomic decided it a year in, and its Cloud product never did.
4. **A checker for forgetting.** Datomic shipped an "excision repair tool" after finding that excised data had stayed in history indexes for years. Any store with derived indexes needs a way to prove that a forgotten thing is gone from every one of them.
5. **A generation number on log positions.** XTDB's epochs.
6. **A decision on valid time.** Datomic: an attribute. XTDB: an axis on every record. A field seeded from papers has late corrections and "with effect from" built into its subject. Either choice is defensible. Not choosing means Datomic's by default, and then Waeselynck's essay describes what follows.

---

## 8. Questions this camp would call the wrong question

- **(1) "Is a fact pointed at by the gate's number, or by its own id?"** Facts have no ids. Entities have ids and sayings have numbers. Wanting to point at a fact means one of two things. Either you want the saying, so use the gate's number. Or you have found a thing you have not yet named, so make it an entity.
- **(11) "Is every read listed?"** The question assumes a list. Theirs is: what was the basis, and what was asked? With one order per unit the list can be rebuilt from those two.
- **(10) "Is 'as of' one number or a position per partition?"** That is downstream. The real decision is what the unit with one timeline is. Inside it the answer is one number. Across units it is one number each. A position per physical partition means the unit was chosen by the machinery and not by meaning. Hickey's "shard below".
- **(9) "How is one deleted?"** Three questions in one. Retracting, forgetting, and never recording in the first place have different answers and different costs. Asked as one question, it gets the most expensive answer for all three.
- **(11) "Because of: always filled, empty only when it starts a chain?"** Only a slot can be empty. An attribute that is absent is simply absent. The question exists because of the envelope.
- **(13) "Plain maps or classes?"** Not a question for this camp. Data. The argument with Alan Kay is their whole answer.
- **(3) "Key: a word, or an id?"** The *right* question, and the one where the camp is most settled: both, split exactly as Datomic splits them.

---

## 9. Sources

Fetched 20 September 2026. "G" marks a source collected by a gatherer. "G✓" means I re-checked the quoted words against the raw download myself.

**Rich Hickey**

- [H-VoV] *The Value of Values*, GOTO Copenhagen, May 2012. Transcript: https://github.com/matthiasn/talk-transcripts/blob/master/Hickey_Rich/ValueOfValuesLong.md
- [H-DD] *Deconstructing the Database*, QCon SF, Nov 2012. Transcript: https://github.com/matthiasn/talk-transcripts/blob/master/Hickey_Rich/DeconstructingTheDatabase.md
- [H-WD] *Writing Datomic in Clojure*, GOTO Copenhagen, May 2012, including the audience questions. Transcript: https://github.com/matthiasn/talk-transcripts/blob/master/Hickey_Rich/WritingDatomicInClojure.md
- [H-FD] *The Functional Database*, QCon New York, Jun 2013. Same repository, `FunctionalDatabase.md`.
- [H-AWTY] *Are We There Yet?*, JVM Language Summit, Sept 2009. Same repository, `AreWeThereYet.md`.
- [H-SPEC] *Spec-ulation*, Clojure/conj, Dec 2016. Same repository, `Spec_ulation.md`.
- [H-MN] *Maybe Not*, Clojure/conj, Nov 2018. Same repository, `MaybeNot.md`.
- [H-IONS] *Datomic Ions*, Clojure/nyc, Sept 2018. Same repository, `DatomicIons.md`.
- [H-IM] "The Datomic Information Model", InfoQ, 1 Feb 2013. https://www.infoq.com/articles/Datomic-Information-Model/
- [H-ARCH] "The Architecture of Datomic", InfoQ, 2 Nov 2012. https://www.infoq.com/articles/Architecture-Datomic/
- [H-ML16] Datomic Google Group, "Modelling a graph using reified transactions?", 20 May 2016. Archive: https://datomic.narkive.com/Rn7jWmvv/modelling-a-graph-using-reified-transactions
- [H-HN12] Hacker News, 29 Aug 2012: https://news.ycombinator.com/item?id=4448230 , 4448283 , 4448351 , 4448381
- [H-HN14] Hacker News, 4 Jan 2014: https://news.ycombinator.com/item?id=7011102
- [H-HN16] Hacker News, 21–23 Jun 2016, the Alan Kay thread: https://news.ycombinator.com/item?id=11962116 and 11947809
- *The Database as a Value*: no transcript found. Not used.

**Datomic documentation and blog** (docs.datomic.com, blog.datomic.com)

- [D-MODEL] Data Model. https://docs.datomic.com/whatis/data-model.html
- [D-IDENT] Identity and Uniqueness. https://docs.datomic.com/schema/identity.html
- [D-SCHEMA] Schema Data Reference. https://docs.datomic.com/schema/schema-reference.html
- [D-CHANGE] Changing Schema. https://docs.datomic.com/schema/schema-change.html
- [D-TXDATA] Transaction Data. https://docs.datomic.com/transactions/transaction-data-reference.html
- [D-TXFN] Transaction Functions. https://docs.datomic.com/transactions/transaction-functions.html
- [D-ACID] ACID. https://docs.datomic.com/transactions/acid.html
- [D-FILTER] Database Filters. https://docs.datomic.com/reference/filters.html
- [D-BEST] Best Practices. https://docs.datomic.com/reference/best.html
- [D-PART] Partitions. https://docs.datomic.com/transactions/partitions.html
- [D-EXC] Excision. https://docs.datomic.com/operation/excision.html (G✓)
- [D-REL] Release notices, 23 Oct 2025, "Excision Repair Tool". https://docs.datomic.com/release-notices.html (G✓). The user reports of excised data remaining: forum.datomic.com topic 1874, "Excision is very slow for huge number of entities", posts of 18 Jun 2021 and 8–9 Feb 2025 (G✓).
- [D-FAQ] Datomic Knowledgebase, FAQ-Bot, 19 Oct 2020: https://ask.datomic.com/index.php/239/ and https://ask.datomic.com/index.php/403/
- [D-BLOG13] "Schema Alteration", 23 Dec 2013. https://blog.datomic.com/2014/01/schema-alteration.html
- [D-BLOG15] "Reified Transactions", 3 Nov 2015. https://blog.datomic.com/2015/12/reified-transactions.html
- [AVILA] Francis Avila, gist: the bootstrap transactions of a Datomic database, output under Datomic 0.9.5173. https://gist.github.com/favila/0a93bd6ac552d0d0ed11 . A practitioner's observation, not official documentation.

**Stuart Halloway and Cognitect staff**

- [S-EXC13] "Excision", Datomic blog, 10 May 2013, via the Wayback Machine (G).
- [S-FORUM] forum.datomic.com/t/support-for-excision-or-similar/323, 21 Feb 2018 (G✓)
- [S-HN24] Hacker News, 15–16 May 2024: https://news.ycombinator.com/item?id=40371918 , 40372598 , 40377742
- [M-FORUM] Marshall Thompson, forum.datomic.com topic 1132, 19 Aug 2019 (G)
- [M-SLACK] Marshall Thompson, Clojurians Slack #datomic, Apr 2020. https://clojurians-log.clojureverse.org/datomic/2020-04-25 (G✓)

**Third parties on Datomic**

- [JEP] Kyle Kingsbury, "Datomic Pro 1.0.7075", Jepsen, 15 May 2024. https://jepsen.io/analyses/datomic-pro-1.0.7075
- [VAL17] Val Waeselynck, "Datomic: this is not the history you're looking for", 8 Jul 2017. https://vvvvalvalval.github.io/posts/2017-07-08-Datomic-this-is-not-the-history-youre-looking-for.html
- [VAL18] Val Waeselynck, "Making a Datomic system GDPR-compliant", 1 May 2018. https://vvvvalvalval.github.io/posts/2018-05-01-making-a-datomic-system-gdpr-compliant.html (G)

**Nubank**

- [N-QCON] Edward Wible and Rafael Ferreira, "Architecting a Modern Financial Institution", QCon, on InfoQ 13 Dec 2017. https://www.infoq.com/presentations/nubank-architecture/ (G✓). The question-and-answer part of the transcript does not label speakers.
- [N-POD] Lucas Cavalcanti with Charles Humble, InfoQ podcast, 16 Aug 2021. https://www.infoq.com/podcasts/lucas-cavalcanti-nubank-fintech-clojure/ (G✓)
- [N-LIMITS] "Managing Cloud Limits", Building Nubank. https://building.nubank.com/managing-cloud-limits/ (G)
- [N-STORY] Datomic customer story, about 2016. https://www.datomic.com/nubanks-story.html (G✓)
- [N-JEPBLOG] Datomic blog post on the Jepsen report, May 2024 (G✓)
- [N-DEVBCN] Jordan Miller (Nubank), talk abstract, DevBcn, 10 Jul 2025. https://www.devbcn.com/2025/talks/949215 (G✓)

**XTDB / JUXT** (all G; the ones marked ✓ I re-checked)

- [X-DOC] James Henderson, "The XTDB 'Document Store'", 12 May 2020. https://xtdb.com/blog/xtdb-doc-store ✓
- [X-INTRO] "Introducing Crux", JUXT, 15 May 2019. https://www.juxt.pro/blog/introducing-crux/ ✓
- [X-32], [X-184], [X-432] Håkan Råberg, GitHub issues 32 (22 May 2018), 184 (20 Apr 2019), 432 (22 Nov 2019, still open). https://github.com/xtdb/xtdb/issues/ ✓
- [X-V1TX] v1 docs, Datalog Transactions. https://v1-docs.xtdb.com/
- [X-BITEMP] v1 docs, Bitemporality ✓
- [X-FAQ] v1 docs, FAQs. https://v1-docs.xtdb.com/resources/faq/
- [X-HN23] Hacker News, "XTDB 2.x Early Access", 27 Apr 2023. https://news.ycombinator.com/item?id=35733515 ✓
- [X-DD7] "Development Diary #7", May 2022. https://xtdb.com/blog/dev-diary-may-22 ✓
- [X-LEAD] "Following the leader: leadership election in XTDB 2.2", 17 Sep 2026. https://xtdb.com/blog/leadership-election ✓. Launch date of v2 from "Launching XTDB v2", 12 Jun 2025. https://xtdb.com/blog/launching-xtdb-v2 ✓
- [X-DBS] v2 docs, "Databases in XTDB", changelog for 2.2. https://docs.xtdb.com/about/dbs-in-xtdb ✓
- [X-WHAT] v2 docs, "What is XTDB?". https://docs.xtdb.com/intro/what-is-xtdb ✓
- [X-TXDOC] v2 docs, "Transactions in XTDB" ✓
- [X-TXS] v2 docs, "SQL Transactions". https://docs.xtdb.com/reference/main/sql/txs ✓ I found the `METADATA` passage and the refused-transaction row myself. The gatherer had reported transaction metadata as not found.
- [X-SQLQ] v2 docs, "SQL Quickstart". https://docs.xtdb.com/quickstart/sql-overview ✓
- [X-KAFKA], [X-LOG] v2 ops docs, Kafka log and Log. https://docs.xtdb.com/ops/config/log ✓
- [X-TIME] v2 docs, "Time in XTDB". https://docs.xtdb.com/about/time-in-xtdb ✓
- [X-4036] GitHub issue 4036, 16 Jan 2025.
- [X-BI3] "Building a Bitemporal Index (part 3): Storage". https://xtdb.com/blog/building-a-bitemp-index-3-storage
- Håkan Råberg's ClojuTRE 2019 talk: only auto-generated captions were available. Not quoted.

**Instant, Prokopov, Yang** (all G; ✓ as above)

- [I-SQL] `server/resources/migrations/01_bootstrap.up.sql`, github.com/instantdb/instant ✓
- [I-ATTR], [I-TRIPLE], [I-INSTAML], [I-TXSQL], [I-RULES] source files in the same repository: `attr.clj`, `triple.clj`, `instaml.ts`, and the transactions and rules migrations.
- [I-PERMS] Instant docs, Permissions. https://www.instantdb.com/docs/permissions ✓
- [I-ARCH] Instant essay on architecture. https://www.instantdb.com/essays/architecture
- [I-SPEC] Stepan Parunashvili, "Database in the Browser, a Spec", 2021. https://stopa.io/post/279
- [I-OAI] "The Instant team joins OpenAI", 2026, instantdb.com/essays ✓
- [I-SYNC] Instant essay by Nikita Prokopov on the future of sync, instantdb.com/essays
- [T-WEB] Nikita Prokopov, "The Web After Tomorrow", 23 Jun 2015. https://tonsky.me/blog/the-web-after-tomorrow/ ✓
- [T-README] DataScript README. https://github.com/tonsky/datascript ✓
- [T-INT] "A shallow dive into DataScript internals". https://tonsky.me/blog/datascript-internals/ ✓
- [T-PROTO] "Datomic as a Protocol". https://tonsky.me/blog/datomic-as-protocol/
- [Y-README], [Y-OLD], [Y-ALTER] Datalevin README (current, and v0.8.25) and `doc/alter.md`. https://github.com/juji-io/datalevin ✓
- [Y-HN] Huahai Yang, Hacker News, 24 Feb 2025. https://news.ycombinator.com/item?id=43155548 ✓

**Missing voice, lightly checked**

- [FLU] Fluree DB v4.0 docs, "Policy Model and Inputs". https://labs.flur.ee/docs/db/v4.0/security/policy-model

---

## Index: where each question is answered

| Sid's question | Main treatment | Strongest voice |
|---|---|---|
| (0) never rewritten or never lost | 2.4, 4.4 | Hickey 2013; XTDB v1 then v2 |
| (2) entity id | 2.4 | Datomic docs; Instant |
| (17) first facts | 2.4 | Datomic bootstrap (Avila) |
| (3) key: word or id | 2.4, 5.1, 5.2 | Datomic docs; Instant source; Prokopov against |
| (9) deleting a value | 2.4, 4.3, 4.4 | XTDB; Datomic excision record |
| (8) by whom | 2.4, 3.4 | Nubank practice; Fluree missing |
| (11) when | 2.4, 4.4 | Datomic docs; XTDB and Waeselynck against |
| (16) layer, before permissions | 2.4, 5.1, 6 | Instant open; Fluree closed; Datomic filters |
| (10) order | 2.4, 3, 4.2 | Hickey; Jepsen; XTDB; Nubank |
| (11) based on: every read | 2.4, 7 | Hickey on basis |
| (4) depends-on versus got-here | 2.4 | inferred only |
| (5) index behind log | 2.4, 4.4 | Datomic sync; XTDB commit sync |
| (11) because of | 2.4, 3.3 | *Maybe Not*; Nubank regret |
| (1) how a fact is pointed at | 2.4, 4.4 | Hickey on hashes; XTDB v1 |
| (6) "replacing 25" | 2.4 | Datomic retraction datoms |
| (7) the gate's verdict, refusals | 2.4, 4.4 | XTDB `xt.txs`; Nubank regret |
| (12) runtime version | 2.4, 3.4, 4.4 | Nubank; Hickey on git identity; XTDB 2.2 |
| (14) the hand | 2.4, 3.3 | Hickey on "stuff"; Nubank firehose |
| (15) a click | 2.4 | camp nearly silent |
| (13) storage | 2.4, 4.3 | Hickey; XTDB on serialized classes |

---
---

# Round two

Appended 20 September 2026. Round one above is unchanged. The orchestrator sent the current leans of softland-ff (Sid's main session). These are leans, not Sid's rulings. My job here is to press them from this camp's side.

Cutter: Claude Fable 5.1, max effort. No delegation this round. I worked from the sources already downloaded, plus four targeted checks: Datomic's "Upgrade Base Schema" page, two practitioner posts on transaction-level provenance, and greps in files I already held.

**Markers this round.** **R** = reported, with a source key. **I** = inferred by me, and I say from what. **N** = institutional. New source keys are listed at the end of this section. Older keys are in section 9.

**One thing to hold in mind throughout.** Nobody in this camp ever lived inside one partitioned store, and nobody in it ever verified an actor. So on (10) and (8) everything below is inference from their principles, not their experience. On (9), (1), (7), (3), (11) and (17) there is direct lived evidence.

---

## R2.1 The tailored questions

### T1. The unit of saying

**The redrawing.** Two things where Sid has one.

*The saying.* One per act of offering. It is an entity.

| part of the saying | who writes it | note |
|---|---|---|
| id | the offerer mints it, random | the saying's name. Used for retry safety, and it survives a replay or a move to another store |
| position | the gate | one number in the order of the saying's home. It is the version of every fact in the saying |
| layer | the offerer states it, the gate checks it | one layer per saying |
| by whom, and the grant it acts under | the door | attributes, not slots |
| when | the gate's clock | once |
| session | the runtime | a pointer to the session entity: runtime commit, kind of machine |
| based on | the runtime and the tool | the basis, the questions asked, outside anchors |
| because of | the runtime | absent when the saying starts a chain |
| verdict | the gate | what it checked, at which versions. For a no: which check failed |

*The fact.* Many per saying.

| part of the fact | note |
|---|---|
| entity, key, value | as now |
| saying, and its place within the saying | the path to everything in the table above |
| replaces | lean (6). The position of the fact it supersedes in that cell. Absent for a new cell |

That is five parts on the fact. It passes the test from section 7: every fact always has every part, except "replaces", which is absent only when there is truly nothing to replace.

**Sources for the redrawing.** R: Hickey puts provenance "on the transaction (which can have an open set of attributes)… as the 'saying' of it *is* the transaction" [H-ML16]. R: "Transactions provide a place to model 'when', 'who', 'where', and 'why'" [D-BEST]. R: two independent teams do it. Nubank attaches the git version of the service and the user's credentials to every transaction [N-QCON]. Shortcut, 2017: "We add the following two attributes to every transaction to indicate both the actor and the context: :audit/user — the actor who carried out the transaction; :audit/org — the context in which the transaction occurred" [SC17]. Shortcut's second attribute is Sid's layer, sitting on the saying.

**Who mints the saying's id.** The offerer. The gate gives the position. Both exist, and they do different jobs.

- R: this is how Nubank gets retry safety today. "When you consume a message, we have a correlate ID to know if you have seen that, and when that goes into the database, we use transaction functions so we don't have something that is double-counted, even in the presence of multiple messages." [N-QCON]
- R: Datomic has the primitive for it. A unique-value attribute refuses a second assertion: "Attempts to assert a new tempid with a unique value already in the database will cause an IllegalStateException." [D-IDENT]. XTDB v2 added transaction metadata for the same purpose: "upstream request IDs, correlation IDs" [X-TXS].
- I: so the Rama camp's need and this camp's practice agree. The offerer names the saying. The gate refuses a second saying with a name it has seen. The gate alone gives order.
- R, and this supports lean (1) from an unexpected side. Nubank splits databases by "the chronological replay of database transactions into multiple new databases while preserving timestamps and fixing historical mistakes" [N-STORY]. A replay gives every fact a new position. A name that is not the position survives it. So the camp's lived practice backs the lean's split between the name and the gate's number. It only moves the name up one level, to the saying.

**Layer: on the fact or on the saying?** On the saying. N: a Datomic transaction is submitted over one connection, to one database. R: Shortcut's context attribute is per transaction [SC17]. I: there are two reasons in Sid's world. The gate's policy question has the layer in it, and it is asked once per act. And facts that must land together need one order, so they need one home. An act that writes two layers is two sayings, the second because of the first. Promotion is already "an ordinary offer", so nothing in the design needs a saying that straddles layers.

**What this does to the leans.**

- **(1)** The name is the saying's id. A fact is addressed as saying plus place, or as cell plus position. One salted hash per saying, not one per fact.
- **(6)** Stays on the fact. It is per cell. Two facts in one saying can replace different versions.
- **(7)** One verdict per saying. Landing is all or nothing: R "If one part of a transaction fails, the entire transaction fails, and the database is left unchanged" [D-ACID]. A refusal names the cell or the check that failed.
- **(11)** When: once. Based on: once. Because of: once.
- **(12)** One pointer to the session.
- The per-fact "version" slot disappears. The version of a cell is the position of the saying that last wrote it. R: Halloway, "this ordering is visible via the time t shared by every datom in the transaction" [S-HN24].

**The deciding case: one agent turns one model reply into 200 facts.**

Under the leans as written: 200 envelopes. 200 copies of by-whom, when, because-of and the based-on list. 200 content ids, 200 salts, 200 verdicts. And no way to land them together. If fact 117 fails its grammar, 199 facts of a reply stand in the store with a hole in them. That is a state nobody said. R: Halloway, "With this information model intermediate database states are inexpressible" [S-HN24]. In Sid's terms the map would show a reply that no model gave.

Under the saying: 200 small facts, one saying, one verdict, one landing.

**What is lost, honestly.**

1. **Per-fact based-on.** The reply summarised 30 papers. Paper 17 is later retracted. Per-fact lists would cast doubt on about 7 facts. One list per saying casts doubt on all 200. Over-doubt is also a way for the map to lie. But look at who fills the list. Lean (11) says the runtime fills in entries. The runtime knows what the *invocation* read. It cannot know which read fed which output. So in practice the 200 per-fact lists are 200 copies of one list, with the same over-doubt and 200 times the bytes. Only a tool that tracks its own dependencies can do better. With sayings it does better by splitting: 40 claims become 40 sayings, each with its own reads, all because of the same crossing. The saying gives the tool a dial between one saying of 200 and 200 sayings of one. The per-fact envelope has no dial. It is fixed at 200 of one. (I, from the leans' own wording.)
2. **Per-fact because-of.** A saying has one cause. The failure mode is a lane that flushes a buffer of unrelated items as one saying, for speed. R: Datomic's import guidance is about batching and pipelining for throughput [D-BEST], and its provenance example is one source file per transaction [D-TXDATA]. I: the convention must say that a saying has one cause, because the gate cannot detect a saying that has two.
3. **Partitioning by entity.** A saying that touches 40 entities needs one order. That collides with lean (10). See T3. This is the real cost, and it is structural.
4. **Saying something about one fact.** R: Hickey declines it. "Reified transactions can not be used to model a graph" [H-ML16]. If a single fact needs things said about it, that fact was an entity all along. I: Sid's based-on already points at a cell at a version, which needs no per-fact id. So nothing in the brief is lost here.

**What Datomic users regret about provenance at the transaction level.**

- R: Hickey names the limit himself. "The granularity you have for that is the transaction level, not the datom level." [H-WD]
- R: Shortcut, after doing it: "Interpreting an individual transaction is out of scope for this post, but it is by no means trivial. You could use additional audit fields to provide more detail." [SC17]. Who and where are easy. *What act this was* is hard to read back out of a bag of facts, unless the act is named on the saying. Sid's because-of, plus the tool's identity on the saying, is the additional field they wished for.
- R: Nubank's two regrets, quoted in 3.3. Sayings with no origin ("implicit operations… that don't have origination information") and no owner on every transaction. Both are about too little on the saying. Neither is about the saying being too coarse.
- **Not found:** any written regret that a transaction bundled unrelated causes. I looked in Waeselynck's event-sourcing essay [VAL-ES], the Shortcut post, and the Nubank talks. The mechanism that would produce it is documented (batching for throughput). The regret is not. Treat item 2 above as a risk I infer, not a lesson they report.

### T2. XTDB v1 against leans (9) and (1)

Lean (9) is XTDB v1's shape with one change. v1 kept a hash in the immutable log and put the *document* where it could be forgotten. The lean keeps the ciphertext in the immutable log and puts the *key* where it can be forgotten. R, the v1 design in their words: "whilst the transaction topic is immutable, messages in the document topic can be permanently erased" [X-INTRO]. So there are again two stores with two sets of rules. The forgettable one is now tiny per item. That is an improvement: deletes are cheap, and nothing waits for compaction.

**Which of v1's failures carry over, and which the key or the salt removes.**

| v1 failure | source | under leans (9) and (1) |
|---|---|---|
| Erasure changes what past reads return | R [X-V1TX]: "Evict is the only operation which will have effects on the results returned when querying against an earlier Transaction Time" | **Carries over, softened.** The fact stays: entity, key, saying, all of it. Only the value is unreadable. v1 lost the whole document. Here a walk of based-on can say *this stood on something since erased*. That is better than anything the camp built |
| Replay blocks on missing content | R [X-184] | **Removed from the log, moved to the key store.** Ciphertext never goes missing. But a rebuild must tell a destroyed key from a key it cannot reach. v1's fix was a tombstone, and the same fix is needed: a key lookup must always answer with the key or with a marker that it was destroyed |
| The same content comes back | R [X-432], still open | **Removed by the salt and the key.** Content that returns gets a new key, a new salt and a new id. There is no collision. The price: the store can no longer recognise that erased content has been submitted again, so a rule of "never accept this again" cannot be enforced. The protection against guessing and the ability to recognise are the same property seen from two sides |
| Copies outside the log | R [D-REL]: Datomic's fix of October 2025 for excised data left in as-of and history indexes. R [X-SQLQ]: v2 erasure is complete only "once all background index processing has completed" | **Carries over in full.** "Rewrites nothing" is true of the log only. Every derived index that holds a plaintext value must be purged, and that must be checkable. Datomic had to ship a repair tool |
| Backups | R [D-EXC]: "Excision is irrevocable. You are strongly encouraged to backup a database before excising data." | **Improved by the lean.** Datomic's own procedure leaves the forgotten data in a backup. Key deletion does reach backups of the log. It reaches them only if backups of the *key store* are themselves in the delete plan. Lean (13) says so. That sentence carries a lot of weight |

**The low-entropy case.** A hash of a short value can be guessed: a yes or no, a diagnosis code, a name. Whether leans (9) and (1) are safe depends on one detail they do not state: *what is hashed*.

- If the hash is over the plaintext, and the salt is stored in the clear on the fact, then after the key is gone anyone who holds the fact can still test guesses against it. A salt in the clear stops tables computed in advance. It does not stop guessing one record. That would not be erasure.
- If the hash is over the ciphertext, the input is already unguessable, because the key and nonce are random. The salt can sit in the clear. After the key is gone the hash says only that these bytes were admitted.
- Or make the hash keyed by the per-value key, so that it dies with the key.

So the sharpening is one short rule: **hash what is stored, not what it means.** (I. My own analysis. Neither product team discusses it.) It has one consequence to accept knowingly. The gate must see plaintext to check the shape. After erasure, the verdict's line (for example: *shape accepted under grammar version 4*) is the only thing that still speaks about what the value was. See T4.

**Three further cases the leans should be tested on.** All I.

1. *A key that is unreachable is not a key that is destroyed.* Year 4. An index is being rebuilt after a runtime rebuild. The key store has an outage. Are a billion values erased? If the rebuild treats a missing key as erasure, the new index silently drops them, and the map lies. If it blocks, that is issue 184 again. The answer is the tombstone: destroying a key leaves a marker, and that marker is a fact. R: Datomic, "excision strikes a delicate balance between forgetting and remembering that you forgot" [D-EXC].
2. *One key per value, at machine rate.* The key store then holds as many entries as the log holds values. It is a second system of record. It is mutable. It holds the most sensitive material in the building. And losing one entry is an erasure nobody asked for, with no undo. It needs the write order v1 learned the hard way: the key must be durable before the fact lands. v1's version of that lesson: "The indexer will pause consumption of transactions while waiting for all their documents to appear" [X-184].
3. *Which keys?* A person asks to be forgotten after three years. There are 40,000 values about them, in the base and in other people's layers, written by hundreds of agents. Keys per value do not tell you which values are about this person. Nothing can tell you later unless it was marked at admission. This is Nubank's regret carried over: "if every transaction had an identifier that could point to the actual customer that owns that data…" [N-QCON]. The sharpening: wrap each value's key under a key for the subject or owner, named when the fact is admitted. Then forgetting a person is one key.

**Copies.** A private note is shown to a model. The reply quotes it. 200 facts follow. Each downstream value has its *own* key. Deleting the note's key touches none of them. What finds them is Sid's own walk: follow based-on forward from the erased thing. No one in this camp has that walk. It is an argument *for* recording reads, and an argument that "delete means throw the key away" is where erasure starts, not where it ends.

**What the XTDB authors wrote about leaving that design.** R: they never wrote one statement that says why they left content-addressed documents. What they wrote is what hurt. Forgotten content blocked replay [X-184]. Returning content raced with eviction [X-432]. Every node carried a full replica [X-DD7]. A reindex took "days to weeks", after which "the transaction log is now ~ephemeral… (no more event-sourcing-style replays required, ever)" [X-HN23]. v2 erases by not copying data forward during compaction [X-4036]. They speak of erasure as the exception: "unless you specifically need us to irrevocably 'ERASE' it, for legal reasons."

**What they would say of lean (9).** I. They would recognise it as their own version 1 with a better forgettable part. They would bring four warnings from having lived it. Every pointer into the forgettable place must always resolve. No rebuild may ever wait on something forgotten. Erasure will be the one operation that changes the past, so design the walks to say so. And the one I would weigh most: their deepest regret was not erasure. It was that everything could only be rebuilt by replaying a log that only grew. That bears on "never trimmed", in R2.2 under (13).

### T3. One order as a scope choice, against lean (10)

The lean: a layer has one home store. Within a store, partition by entity. "As of" is a position per partition, a cut, never one number.

**The camp's rule.** The unit of total order is the unit of consistent reading and the unit of landing together. R: Hickey, once data is sharded "you cannot query against shards. You cannot do transactions across shards. You cannot ensure consistency across shards" [H-WD]. So choose the unit by meaning, "shard above" [H-IONS].

**The candidates, inside one partitioned store.** All I.

- **The entity** (the lean). Every cell is ordered. Nothing larger is. A saying over 40 entities cannot land together. Any pattern read is a cut. The gate reads policy and grammar from other partitions on every admission.
- **The layer.** A saying has one home, so it lands together. A pattern over a layer is as of one number. Policy and grammar still live elsewhere, in the base, but that is one foreign position per admission, written on the verdict.
- **The person** (all of one person's layers in one order). R: this is Nubank's lived unit. "That was the decision we had on sharding, we separated by customer." [N-POD]. And their regret says the owner is the key to split on later. Cost: the session firehose shares an order with the person's durable facts.
- **The problem.** A shared layer for hundreds of people and their agents. It works as a layer until it is too hot. R on the ceiling: Hickey, "It *is* the bottleneck… nothing is infinite" [H-WD]. XTDB: "300k docs/second written on a single thread" locally [X-LEAD]. Hundreds of people times tens of agents at machine rate is within sight of that ceiling. When it is reached the problem splits into sub-problems, by meaning.
- **The base cannot be any single unit.** A field of ten million papers with summary layers does not fit one order, and Datomic's own guidance starts to bite at 10 billion datoms of history [D-FAQ]. The base has to be split by meaning: a source document with everything drawn directly from it, a topic's summary layer, and so on.

**The camp's pick.** The layer is the unit, keyed so that the owner is recoverable. The base is many units.

**The two test reads.**

1. *A pattern over one person's layer.* Entity-partitioned: that person's entities are scattered over every partition by the hash of their ids. "As of" is a cut with up to as many positions as there are partitions. Lean (11) then lists that cut on every fact that stood on the read. With 1,024 partitions that is about 8 KB of positions per pattern read, per fact, at machine rate, for ever. Layer as the unit: one number. **This read decides between the entity and the layer.**
2. *A pattern over the shared base.* No unit of order can hold it. Either a cut, or something else. The camp's "something else" is the next point.

**Name the cut.** The lean says "never one number". The camp would say *always one number*, by making the cut a thing with a name. An index build is a fact: *build 8812 covers these positions in these units*. Every read against that build says *as of build 8812*. The vector is written once, with the build. It is never written on a fact. R for the pattern: Datomic's basis is exactly this. A database value is a named index plus the log's tail, and one number names it [H-DD], [D-ACID]. R: XTDB v2's single writer closes a block "at the end of each block (~100k rows/15 minutes)" and writes it to object storage [X-DBS]. That is a named unit of index progress. R: this is also what Hickey prizes about a basis. It can be said aloud and handed over [H-HN12]. Lean (5) then has an exact answer: how far the index had really got is the build number. The cost is lag. A read of the whole base is as of the last build, not as of now. The honest form is to say so on the read. *A question for the Rama camp, which I cannot answer:* whether Rama's own batch machinery already produces a store-wide number that names a consistent cut. If it does, that number is the basis.

**What Nubank's practice says about "as of" across databases.** N, from R sources. They do not have one, and they never record a vector of bases. About 2016: "Datomic queries can take multiple database sources as inputs into a single query so the company can conduct cross service joins" [N-STORY]. By 2017 that had stopped scaling: "even for simple things, like how many customers we have… So what we did is the traditional approach of making an ETL" [N-QCON]. Between services, consistency is eventual and carried by messages that are safe to repeat [N-QCON]. Questions about the whole estate are answered from a separate extract that lags, and their "as of" is the extract's. That is the named build again, reached by necessity.

**Resembles and differs.** Resembles: many writers, a split by owner, a lagging consistent extract for the questions that cross everything. Differs: Nubank's units are whole databases with nothing shared between them. Sid's units would share one store, one gate design and one base. Nobody in this camp has done that.

### T4. Reading old facts through today's schema

**Datomic's limit, exactly.** R: "a database value utilizes the single schema associated with its current basis. Thus traveling back in time does not take the working schema back in time" [D-CHANGE]. And the concrete way it misleads: "After changing the cardinality of an attribute… An entity from a d/as-of, d/since, or d/history database that has an attribute with multiple values will return a single one of those values if the schema attribute has been changed to be single-valued." [D-CHANGE]. The past is shown through today's rule, and it shows one affiliation where there were three.

**What must be written at admission, and where.**

1. **On the gate's verdict: for each key used in the saying, the version of the grammar fact the gate checked against. And the version of the policy fact.** Not on the fact, which would be 200 copies. Not on the saying, because the offerer does not know what the gate will check against. The verdict already "names what it checked". This is the content of that phrase.
2. **Why it cannot be worked out later.** Grammar facts live in the base. The fact lives in some other layer, with its own order. Finding *the grammar in force when this fact landed* means mapping a position in one order to a position in another. That mapping does not exist unless the gate wrote it down. R for contrast: Datomic can say a rule is "enforced starting on the transaction after" it is asserted only because schema and data share one order [D-SCHEMA]. Sid's grammars and facts will not share one.
3. **Optionally, on the offer:** the grammar version the offerer built against. It makes a refusal readable: built for version 3, checked under version 4.

**What must be fixed for ever, so that the list above is enough.**

4. **The value's encoding belongs to the floor, not to the grammar.** One self-describing plain-data encoding, for ever. A grammar is a predicate over plain data. It never decides the bytes. Then any old value can be read with no grammar at all, and the recorded version tells what was promised about its shape. R: the one thing Datomic made unalterable is the value type [D-CHANGE]. I: because the type decides the bytes and the sort order in every index, and everything else was found safe to change later. R: XTDB let "arbitrary java.io.Serializable types" in and later called stricter types "hygienic" [X-DD7]. R: Hickey's habit of plain printed data [H-WD]. If a grammar can decide the encoding, then every decoder must be kept alive across hundreds of rebuilds of the runtime, and one lost decoder loses every value written under it.
5. **Growth only under one key.** R: provide more, require less; "turn what would have been breaking into accretion" [H-SPEC]. A grammar version that requires more, or provides less, is a new key. R: "Never remove a name… Reusing that name to mean something substantially different… can be even worse" [D-BEST]. A rename is a second word on the same key.

**The deciding case.** Year 1: the key for affiliation takes many values. Year 3: grammar version 5 makes it single-valued. Someone asks what they were looking at in year 1. The honest answer shows three affiliations. It does so only if the reader applies the version recorded at admission, and if rule 5 stopped version 5 from being a break under the same key in the first place. Datomic today shows one. The camp's own rules would have made version 5 a new key.

**With lean (9).** After a key is destroyed, the verdict's line about the grammar version is the only surviving statement about that value's shape. That is one more reason the line belongs on the verdict, which is kept for ever.

### T5. "A fixed envelope is place-oriented." What would they fix instead?

Hickey names Sid's question himself: "What constitutes minimal sufficiency of 'data' is a useful and interesting question. E.g. should data always incorporate time, what are the tradeoffs of labeling being in- or out-of-band, per datom or dataset, how to handle provenance etc." [H-HN16]. Taking his four clauses one at a time, with Datomic's own answers:

- **"Should data always incorporate time."** Yes, but by reference. R: a datom carries "some path to time", and that path is the transaction [H-DD]. Time is in the fixed part as a pointer. It is never repeated as a value.
- **"Labeling… in- or out-of-band."** In-band, per fact. The label is the attribute, and it rides in every datom. That is what makes a fact readable alone. What stays out of band is the label's *definition*: its type, its cardinality, its word. Those are facts about the label. A fixed envelope labels out of band, by position. Part seven means because-of only because both ends agree that it does, for ever. R: "a straight product type just completely complects the _meaning_ of things with their position in a list" [H-MN].
- **"Per datom or dataset."** Per dataset, where the dataset is what was said together. R: his seismometer example in the same thread is provenance per stream: "given the numbers and the provenance alone (these numbers are from a seismometer)" [H-HN16]. R: "replicating this on many facts" is the thing he argues against [H-ML16].
- **"How to handle provenance."** As an open set of named attributes on the saying. The set grows. It never breaks.

**So what the camp would fix before the first record** (I, put together from the above):

1. A small positional core. Entity, key, value, saying, replaces. Something must be positional at the very bottom. They keep it to the parts that every fact always has.
2. The saying as an entity with an id and a position.
3. A *vocabulary*, not a layout. The first keys that describe sayings: by whom, acts under, when, layer, session, based on, because of, verdict. Each is an ordinary key, with an id, a word and a grammar, written in the first facts. They fix names, not places.
4. The floor's one value encoding (T4).
5. The rules of growth: never reuse a word, a second word is an alias, an incompatible shape is a new key.

**The deciding case: the tenth part.** Sid's scales name it already. *The planet, with an economy*: every act will one day need a price, a payer, a budget. *A second institution's store*: every saying that crosses will need to say where it came from and who vouches for it, because the door's check of by-whom means nothing outside the store that ran the door. With nine places, a tenth means a new envelope version, a runtime that reads both for ever, and every earlier fact silently lacking the part. With a vocabulary, it is one new key in the next edition of the first facts. "Both ends share it forever" is a promise that can be kept for five parts. For nine, it would already have been broken by year three.

---

## R2.2 Ask A: the leans, one by one

Leans where the camp has nothing sourced are skipped or given one line.

**(0) Never rewritten. Stands, and XTDB v1 lived this way.** R: "the transaction log can stay immutable" [X-DOC]. It agrees with Hickey's "while the past may be forgotten, it is immutable" [H-IM], given lean (9). One thing to notice: Datomic's excision does rewrite its log, "those same datoms will be removed from the transaction log" [D-EXC], so the lean is stricter than Datomic. *Sharpen:* never rewritten, plus never trimmed, plus indexes rebuilt from the log is exactly the combination XTDB v1 had and left. Case under (13).

**(2) Entity id: random, minted by the offerer, no time inside. Stands, and Instant lives this way.** R: the client calls `uuid()` for entities and for attributes [I-INSTAML]. Two sharpenings.

- *Ingest ids derived from source plus form* settle sameness by construction, and freeze it. R: Datomic keeps identity as a fact that can grow: "It is legal for a single entity to have multiple different unique attributes", with the conflict case documented [D-IDENT]. **Case:** the same paper arrives through the arXiv lane and through the Crossref lane. Source and form differ, so the lane derives two ids. There are two entities for good, and every pattern must look through a same-as fact. With outside keys held as unique-identity facts that the gate checks, the second lane lands on the first entity as soon as either record carries both keys. What is known about sameness grows. An id computed at ingest cannot grow.
- *No time inside* costs locality only if an index leads with the id. R: "checking for the existence or uniqueness of random (v4) UUIDs has poor locality, as reads will scatter" [D-IDENT]. **Case:** the gate's check of a cell, at machine rate, against an index of ten billion random ids, is a cold read every time. If indexes lead with the layer, the locality comes from the layer and random ids cost nothing. So this lean is safe under T3's unit and expensive under lean (10)'s.

**(17) First facts. Sharpen twice.**

- *"A finite seed, written once."* R: Datomic's did not stay finite. "When the Datomic team enhances the base schema, all databases created with new versions of Datomic get the enhancements automatically. Existing databases do not automatically incorporate enhancements to the base schema." Tuples entered the base schema on 27 June 2019, seven years in. Older databases need an explicit step, which they made "an idempotent operation" [D-HOWTO]. **Case:** in year 3 the floor needs a new kind of system fact, such as an erasure request or a grant. The seed needs a second edition. Say *editions*: each one a saying by the floor, each safe to apply twice, none changing an earlier one. That is accretion, and the lean's content-computed ids suit it better than Datomic's small integers do.
- *"Seed ids computed from content."* **Case: the very first fact.** R: in Datomic it is `[10 10 :db/ident …]`. The attribute that names things is named by itself [AVILA]. A fact that mentions its own id cannot have an id computed from its own content. The hash would have to contain itself. So the first ids must come from something that does not loop: the *word*. The id of a seed key is a hash of its namespaced word. Every store derives the same id. It is also what Hickey says a name is for: "nice hopefully globally unique names" [H-SPEC]. (I.)

**(3) Key is an id; word and shape are facts about it. Stands. Datomic and Instant live this way** [D-IDENT], [I-SQL]. Datalevin also keeps attribute ids, not words, in its indexes [Y-ALTER]. *Sharpen the ground.* The lean says the ground is "shape and cost, not loss". In a store that never rewrites, it is also loss. R: Datomic's "most requested enhancement" was altering schema, and renaming comes first in its list [D-BLOG13]. **Case:** in year 2 a word turns out to be wrong, or a second institution uses the same word for something else. With words in every fact of a never-rewritten log, the mistake is permanent and the clash cannot be fixed. With ids, a rename is one new fact, and two institutions' keys are two ids that happen to share a word. One cost to plan for: R, "All idents associated with a database are stored in memory in every Datomic transactor and peer" [D-IDENT]. Every reader needs the table of words at hand.

**(9) Value under a per-value key; delete by destroying the key.** See T2. Stands as the best shape anyone here has found. Four sharpenings. Hash the ciphertext. Give destroyed keys tombstones. Wrap value keys under a key for the subject or owner, named at admission. Treat the derived indexes and the key store's backups as the hard part, and build a checker for both.

**(8) By whom. Stands for one store, and Nubank lives the recording half.** R: "we attach the credentials of the user that is making that transaction" [N-QCON]. The camp has never verified an actor, so it has nothing sourced on the door. One sharpening from the second-store case: the door's check holds only inside the store that ran the door. Across stores, by-whom arrives as a bare claim. The one voice on that is Fluree, which I only lightly checked. Missing.

**(11) When. Stands, and Datomic lives this way.** R: "you should always use the t (or related tx) value" for order [D-BEST]. *Sharpen:* keep the stamp monotonic with position inside each unit of order. R: an explicit instant must not be "older than any existing transaction" [D-TXDATA]. **Case:** *what was I looking at yesterday at 17:00* has to map a wall-clock time onto a position. If a gate's clock steps back after a time sync, two positions carry stamps in the wrong order and the mapping has no answer. The gate must never stamp earlier than its own last stamp in the same unit.

**(11) Based on: every read listed. Sharpen to the basis and the question, on the saying.** **Case:** the agent reads a pattern over 3,000 base facts, then one model reply, and writes 200 facts. If "listed" means the rows, that is 600,000 entries for one act. If it means the pattern read as one entry, it is still the same list 200 times over. One saying holds one entry. R: "Every query we issue to that value of the database has the same basis" [H-DD]. One caution the other way: a bare basis is enough only while T3's single-number "as of" holds. Under lean (10)'s cuts, the basis is itself the expensive part.

**(11) "No grace period for any of the three." Stands, and it is the most strongly sourced lean on the table.** R: Cavalcanti, "what I regret the most, is to have implicit operations that are not stored into the database, or that don't have origination information" [N-POD]. **Case:** a background job, a migration or a repair script writes facts with no because-of "just this once". Three years later those are the facts nobody can explain, and in a regulated setting that is the finding.

**(11) Because of: "empty only at a chain's start." Sharpen.** An empty slot cannot tell *this starts a chain* from *the cause is not known here*. R: "The maps know what they know" [H-MN]. **Case:** a fact arrives from the second store whose cause never came with it. Or its cause sits in a layer this reader may not see. Or its cause was erased. None of these starts a chain. Make a chain's start a positive statement, such as the kind of the act (*a person's click*), and let an absent because-of mean unknown.

**(16) Visibility. One sharpening, on where private facts physically sit.** R: Datomic's filter is a view that trusted code applies at read time [D-FILTER]. Instant applies view rules as a filter after the query: "every object that satisfies a query will run through the `view` rule before being passed back" [I-PERMS]. R: Prokopov draws two filters on the push path, security and then interest [T-WEB]. **Case:** with partition by entity, a person's private fact about entity E sits in the same partition, and the same index, as the base's facts about E. Privacy then depends on every read path and every match path applying the filter, for ever, across hundreds of rebuilds of the runtime. A filter applied after the fact also leaks through counts and timing. If the layer is the unit of partition, private layers are physically separate, and a forgotten filter cannot leak across. N: Nubank's isolation is physical, a database per service. *The gate's answer in the fact's layer:* stands. R: XTDB keeps `xt.txs` in the same database [X-TXS].

**(10) Order.** See T3. **This is the lean the camp presses hardest.** Partition by entity makes the saying impossible, makes every pattern read a cut, and spreads each private layer across every partition.

**(4) Marks on reads.** Two halves. *Depends-on or how-she-got-here:* keep it, as two named attributes (2.4). *Follow-latest or stay-on-version, fixed when written:* wrong question, see R2.3. One structural point. "The floor guesses; the tool may correct." In a store that never rewrites, a correction to provenance cannot edit the fact. It needs somewhere to go. If the saying is an entity, the correction is a new fact *about the saying*, with its own by-whom and when. R: "Transactions are ordinary entities: you can create attributes that are about transactions" [D-BEST], and the docs' `:correction/for` [D-MODEL]. With provenance inside each fact's envelope, the only way to correct a guess is to write a new version of a fact whose value did not change.

**(5) How far the index had got, and what was withheld. Stands.** R: `sync` [D-ACID]; `COMMIT SYNC` [X-TXS]. *Sharpen "what was withheld":* you cannot list what you were not allowed to see. Record the *view*: the layer stack and the policy version in force. R: a Datomic database value is a basis plus its filters, and a filtered value is a different value [D-FILTER]. So the basis is positions plus view.

**(1) Point at a fact by its own content-and-salt id.** Half backed, half pressed. *Backed:* a name that is not the gate's number. R: Nubank's replays into new databases change every position [N-STORY]. *Pressed:* put the name on the saying. R: Hickey on hashes as names, "it does not convey anything about order unless you have the rest of the repo. It does not imply anything about causality" [H-SPEC]. **Case:** a walk over a based-on list of 40 content ids learns nothing about which came first without looking each one up. A pointer written as name plus position carries its order with it. And see T2: hash the ciphertext.

**(6) Keep "replacing 25". Stands, and Datomic lives this way.** The retraction of the old value rides in the same transaction [D-MODEL]. **Case:** the index is lost, and the chain of versions must be rebuilt from the log alone. With "replaces" on each fact it is one pass. Without it the gate's logic must be run again, by a runtime that may be 300 rebuilds newer. R: XTDB's "days to weeks" [X-HN23].

**(7) The verdict kept for ever; refusals in the offerer's session layer.** Stands, and XTDB v2 lives the first half [X-TXS]. Two sharpenings. One verdict per saying. And a refusal filed away from the cell it lost on must carry *the position it was judged at and the version it found there*. **Case:** hundreds of agents contend for one hot cell in a shared problem layer. Most offers lose. An agent asks what it lost to. In XTDB the refusal sits in the same order as the winner, so the answer is its position. In the offerer's session layer it has no place in the target's order unless one was written on it. The lean's choice of layer is right for volume. Refusals at machine rate would bury a shared layer. The cross-reference is what it costs.

**(12) Session start. Stands, and Nubank lives this way.** R: the git version of the service on every transaction [N-QCON]. Sharpen with Hickey: the commit is the name, and a build that is not a clean commit says "unreproducible" in its name [H-IONS].

**(14) The hand. The camp rejects pan and zoom at a tick as facts kept for ever.** R: "Datomic is not about keeping stuff" [H-DD]. No history for "high churn attributes" [D-BEST]. Nubank: "It doesn't work that well for fire hose writes" [N-QCON]. DataScript, built for long-lived browser applications, keeps no history so that its databases "operate in constant space" [T-README]. Yang: application state is not information [Y-HN]. **Case:** one person, eight hours, ten ticks a second, is 288,000 facts a day. Under lean (9) that is also 288,000 keys a day in the key store, never trimmed. They are the most revealing facts in the store about how that person works. And they are already recorded, because every "what was shown" crossing carries the viewport it was shown through. *Sharpen:* point and select become part of the saying they led to. Pan and zoom live inside the shown-crossings. "A person may turn capture down by their own fact": stands, with the note that a store which never rewrites cannot honour it backwards in time.

**(15) A click.** The camp is nearly silent. One sourced caution about defaults. R: Instant chose open by default, and then, once agents arrived, built undo exactly where they were hurt: "Even if a rogue agent deletes your columns, you can undo it" [I-ARCH]. **Case:** a colleague's toolkit is copied into a person's own layer. Under the lean it is on by default, and it acts in their name at the next click. *Sharpen:* on by default only for tools that are in their layer *and* were said by them or under their grant. The by-whom on the saying that landed the tool decides it.

**(13) Storage.** *Plain maps:* stands [H-WD], [X-DD7]. *Backups inside the delete plan:* stands, and it is better than Datomic's own advice, see T2. *Never trimmed:* pressed. R: XTDB v1 held the same position, "Kafka's infinite retention capability" [X-INTRO]. They left it over one operation: a reindex of "about 600GB of data at ~100 million transactions… taking days to weeks" [X-HN23]. **Case:** in year 4 a runtime rebuild changes an index's layout. A log that is never trimmed and is the only source means a replay of the planet. The camp would accept a log that is never trimmed *only if no operation ever needs a full replay*. That means snapshots of the indexes. R: v1 added checkpoints, and "The default lifecycle of checkpoints is unbounded" [X-CKPT]. And a snapshot of an index is a copy of plaintext values, so it goes inside the delete plan as well.

---

## R2.3 Ask B: leans the camp would call the wrong question

1. **(4) "Follow the latest, or stay on the version read", marked when the fact is written.** Whether a later change to a read matters is not a property of the read. It is a property of the question someone asks later, and in Sid's design the definition of stale is itself a fact that will improve. **Case:** summary S stood on claim C at version 3. Version 4 fixes a typo. Version 5 reverses the claim. If S was marked "stay", it is never stale, which is wrong at version 5. If it was marked "follow", it is stale at version 4, which is noise. No mark chosen at writing gets both right. A year-5 definition of stale can, provided the year-1 read was recorded plainly as *what, as of where*. R: identity and state are separate, and the state comes from the value you read through [H-AWTY]. Ask instead: *what did it read, as of where?* Leave the verdict to the definition.
2. **(10) "As of is a cut, never one number."** Ask instead: *what names a cut?* A cut that must be said, stored and handed over needs a name. T3.
3. **(1) "What is a fact's name?"** Ask instead: *what is the saying's name, and where in it is the fact?* T1.
4. **(17) "A finite seed, written once."** Ask instead: *how does the seed grow without breaking any store that already has it?* Editions. R: [D-HOWTO].
5. **(14) "Which motions become facts at a tick?"** Ask instead: *which motions became the basis of an act, and which are already inside a shown-crossing?*
6. **(11) "Always filled, empty only at a chain's start."** As in round one: only a slot can be empty. Ask instead: *how does a chain's start say that it is one?*

---

## R2.4 Ask C: what they would change above the table

Round one's section 7 stands. What the leans add or sharpen:

1. **Strongest change: add the saying.** Make the unit of admission a set of facts said together, with one home, one position, one verdict and an open set of provenance on it. It takes the envelope from nine parts to five. It gives tools a dial for the precision of based-on. It gives corrections to provenance somewhere to live. It gives the tenth part somewhere to go. It costs partition-by-entity. That is why (10) and the saying have to be decided together.
2. **A representation of "no longer so".** I did not see one in the envelope as briefed. R: Hickey's stated reason for not using RDF is that "without a temporal notion or proper representation of retraction, RDF statements are insufficient for representing historical information" [H-IM]. Sid has the temporal notion. The camp offers two lived answers for the other half. Datomic has an explicit added-or-retracted part on every fact [D-MODEL]. XTDB: "Retractions in XTDB are implicit and deleted documents are simply replaced with empty documents" [X-FAQ]. **Case:** a person un-says a relation because it was wrong. What is written? If the answer is *a later version with an empty value*, then every grammar must admit empty for ever, which is Hickey's "Maybe sheep" at the level of values. If it is a part of the fact, it is uniform and needs no grammar to agree. Either can work. It has to be chosen before the first record.
3. **"The runtime is the one thing that is not a fact": keep it, and draw the line where XTDB ended up.** R: for seven years XTDB made every node re-derive outcomes from the log. "Every transaction has to be deterministic… any kind of error had to restart all of the nodes", and every feature "had to be expressible as a pure, deterministic function of the log" [X-LEAD], [X-DBS]. Sid's runtime will be rebuilt hundreds of times. So *re-derivable* has to mean re-derivable **now**, by today's runtime. It must never mean that build 400 would reproduce what build 12 computed. Everything that must stay as it was has to be an outcome that was written down: the gate's verdict, the landed facts, the crossings. The leans already do this. The change is to say so as a rule, and to apply it to running answers too. A running answer shown in year 1 is only known through its crossing. Recomputing it under today's runtime is a new answer, not a replay of the old one.
4. **"One store for the planet": one order per unit, units by meaning, cuts with names, an owner on every saying.** T3, and Nubank's regret.
5. **"Tools, grammars, policies as facts in the same store": yes, and the verdict must carry the versions.** T4. The seed grows in editions, and its first ids come from words.
6. **"Every read recorded on every fact": the basis and the question, once, on the saying.** Reading stays free and unrecorded except at acts and at crossings. R: [H-HN12].
7. **"An append-only store of small facts as the one substance": yes for information, no for the firehose.** See (14). The line is one Hickey and Nubank both draw.
8. **"Running answers never stored, only crossings recorded": unchanged, and strongly held.** R: "'age' (unless temporally-qualified, 'as-of') is not" data [H-HN16]. A crossing is the temporally qualified form.

---

## R2.5 What I could not source this round, and who is missing

- **No written regret about a transaction that bundled unrelated causes.** I looked, and I have said where. The risk is inferred.
- **Nothing in this camp on verifying an actor, on grants, or on a click acting in someone's name.** (8) and (15) get almost nothing sourced from here. Fluree is the missing voice. I checked one page of it in round one.
- **Nobody here lived inside one partitioned store.** All of T3 is inference from their principles, plus Nubank's practice with many separate stores. The Rama camp has to say whether a layer can be a unit of order, and whether a store-wide number can name a cut.
- **The low-entropy analysis in T2 is mine.** Neither product team discusses it.
- **Still unread:** Datahike and TerminusDB on durable branches, which bear on layers. RDF named graphs, on one context slot doing several jobs. Tim Ewald's talk on reified transactions, which might hold the regrets I could not find in writing.

## R2.6 Sources added this round

- [SC17] Shortcut (then Clubhouse), "Auditing with Reified Transactions in Datomic", 8 Apr 2017. https://blog.clubhouse.io/auditing-with-reified-transactions-in-datomic-f1ea30610285
- [VAL-ES] Val Waeselynck, "Datomic: Event Sourcing without the hassle", 12 Nov 2018. https://vvvvalvalval.github.io/posts/2018-11-12-datomic-event-sourcing-without-the-hassle.html
- [D-HOWTO] Datomic docs, "Upgrade Base Schema". https://docs.datomic.com/operation/howto.html
- Passages quoted this round for the first time, from sources already in section 9: [D-EXC] on backing up before excising and on removal from the transaction log; [D-CHANGE] on cardinality in historical databases; [D-IDENT] on unique values; [N-QCON] on correlation ids and messages that are safe to repeat; [N-STORY] on cross-service queries and on replaying history into new databases; [N-POD] "we separated by customer"; [X-FAQ] on implicit retractions; [X-DBS] on blocks. 
- [X-CKPT] XTDB v1 docs, "Checkpointing". https://v1-docs.xtdb.com/administration/checkpointing/ (collected by the round-one gatherer).

---
---

# Round three, line 1: what is the unit of total order, the entity or the layer?

Appended 20 September 2026. Rounds one and two are unchanged. Written from what was already in context. No new fetches. R, I and N as before. Source keys are in sections 9 and R2.6.

## 1. Their best reply, as strong as I can make it

- **The base breaks the claim.** The base is one layer, and the planet writes to it. A Rama task is single-threaded, and the task count is fixed at launch. So "the layer is the unit" either puts the base on one task, which cannot work, or quietly swaps in a smaller unit cut "by meaning". Then the layer is not the unit. Someone must draw those lines before the first record, and moving one later means copying every depot. Fields merge. Topics split. A hash needs no foresight.
- **The team layer breaks it again.** Hundreds of people and their agents at machine rate, on one thread. Hickey's own scope rule: "It _is_ the bottleneck… If you need arbitrary write scaling, this is not the system for you" [H-WD]. The layers that matter most are the ones his rule excludes.
- **Skew.** Keyed by layer, one key holds most of the data. Keyed by entity, load is even.
- **My cost argument is gone.** The 8 KB cut is answered by interning it, which is my own "name the cut".
- **Landing together already exists:** the microbatch gate.
- **Re-partitioning hurts under either key**, so the layer buys nothing there.

## 2. Rejoinder

I concede three things. The base cannot be one order; I said so in T3. A hot team layer hits the ceiling. Interning removes the byte cost. What is left is not about cost.

- **The saying.** By research-1's own account the fast gate is atomic inside one partition only. The atomic gate takes 300 ms or more, cannot answer the offerer, and halts for everyone when one task group stalls. So under entity keys a 200-fact saying is either not atomic or not answered. Keyed by layer, in a private layer, it is atomic, fast and answered. R: "intermediate database states are inexpressible" [S-HN24]. R: coordination belongs only where process needs it [H-DD]. A planet-wide halt is coordination nobody needs.
- **Lived practice splits by owner, never by hash.** R: Nubank, "we separated by customer" [N-POD], and their shards are copies of the whole stack [N-QCON]. R: their regret is that no owner id sat on every transaction, so re-cutting was hard [N-QCON]. R: Hickey, shard below "is always mechanical, and the system does not understand what you are doing, mostly" [H-IONS]; "You cannot do transactions across shards" [H-WD]. R: XTDB insists on "Exactly 1" partition [X-KAFKA] and accepts "a hard upper limit" in return for "concrete information guarantees" [X-WHAT]. When it needed more, it added databases (2.1). It did not add partitions.
- **Hickey's line cuts the other way.** He took a ceiling per unit in order to keep value: "trying to make a universal system that can handle infinite, means dropping a whole bunch of value" [H-WD]. The lesson is to keep units small enough for one order. One person with tens of agents is far under one thread. R: XTDB saw "300k docs/second written on a single thread" locally [X-LEAD].
- **Positions.** I: a position that belongs to the unit of meaning survives a re-partition. A position that belongs to a task does not. If the gate writes the layer's own count on the saying, copying depots into a new module changes nothing that any fact points at. A layout epoch is then needed only where task offsets are named, which is the base's cuts. R for the habit: Nubank replays databases "while preserving timestamps" [N-STORY]. What they could not preserve was the transaction number.

On the base, and on a team layer once it is hot, my camp has no lived answer. Nobody here ever ran a unit that everyone writes to. There the keyed-unit camps are right.

## 3. Verdict on the hybrid

**The camp would accept it. It is the shape each of them already lives.**

- **Hickey** (I, from R). Private layers are his databases. Each is a value with a basis, handed to the question: "Database is an argument to query" [H-DD]. On splitting by user he said: "If you had a way to shard like your user space… that you knew you would get locality out of, that would be a big win" [H-IONS]. The entity-sharded base is "shard below", and he would say it is no longer a database: it has no value and no basis. He would take it as a stated trade: "There is nothing wrong with making a different tradeoff. The trick is: understanding tradeoffs need to be made" [H-DD]. His one condition: the base gets a basis by another road, which is the named cut.
- **The XTDB authors** (I, from R). A private layer is their database: one log, one partition, one resolver whose outcomes are written down. The base is something they never built. They would name the base's cuts at block boundaries, as they do [X-DBS], and carry an epoch [X-LOG].
- **Nubank's engineers** (I, from R). This is their estate: units keyed by owner, and a lagging extract for whatever crosses everything [N-QCON]. They would warn that summary layers over the base are aggregates across units, the thing that became "increasingly difficult" for them. Build those as named, derived builds.

**A saying that spans two layers.** It does not. One saying, one unit of order. An act that needs two layers is two sayings, the second because of the first. In the base a saying has two roads. If it is about one entity, it takes the fast gate. If it is one statement about many entities, such as one paper's ingest, it takes the slow atomic gate, and its answer is the verdict fact, which the offerer's tools match on. That keeps "no optimism": nothing is assumed, and the answer is a fact.

**What every saying must carry from the first record, so the unit can be re-cut later.**

1. The **owner**: whose layer it is. For sayings in the base, the source or subject they are about. R: the Nubank regret.
2. The **offerer-minted id**. It survives any re-cut.
3. The **unit's own count** as the position, never a task offset. And a **layout epoch** wherever offsets are named (research-1's point; R: "Epochs only move forwards" [X-LOG]).
4. The **gate's stamp**, monotonic per unit. It is the one rough guide that survives everything.
5. The **root of its because-of chain**. I: it is the cheapest mark of which problem a saying belongs to, and the problem is the likeliest line along which a hot team layer would later be split.

**The open risk.** A team layer has to be declared layer-keyed or entity-keyed when it is born. The five marks above are what make changing that decision survivable.

**Resembles and differs.** Resembles: units keyed by owner, plus a lagging view of the whole estate. Differs: nobody in this camp shared a base between units, and nobody re-cut a live store. All of part 3 is inference from their principles.

