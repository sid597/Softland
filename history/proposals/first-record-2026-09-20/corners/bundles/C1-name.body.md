# C1 name (2)(1): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L55-58 · BODY · CARRIED C1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.1 What they built, and what they chose*

Datomic (2012). A database where the unit is an immutable fact, the *datom*:

> "A datom is an immutable atomic fact that represents the addition or retraction of a relation between an entity, an attribute, a value, and a transaction. A datom is expressed as a five-tuple: an entity id (E), an attribute (A), a value for the attribute (V), a transaction id (Tx), a boolean (Op) indicating whether the datom is being added or retracted" [D-MODEL]


---
**datalog L65-65 · BODY · CARRIED C1**

- **Transactions are entities.** Every datom points at its transaction. The transaction carries the wall-clock time and whatever else the application asserts about it.

---
**datalog L79-82 · BODY · CARRIED C1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

**Why the fact is small, and why the transaction is the path to everything else.**

> "And we call that a Datom. But it is just an entity, an attribute, a value, and some path to time. We use the transaction, because it is also a path to other information about what happened, including provenance, or causality, or operations, or anything else like that." — Hickey [H-DD]


---
**datalog L194-194 · BODY · DISAGREES C1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED. In Datomic the single writer mints ids. "Entity ids are assigned by the transactor, and never change." Clients use temporary ids inside a request and the transactor resolves them. [D-IDENT]. No clash is possible because one process hands them out.

---
**datalog L195-195 · BODY · DISAGREES C1,C3**

- REPORTED. Ids are opaque to programs but not to the machine. "Semantically, entity ids are opaque values uniquely identifying an entity within a database." [D-TXDATA]. Yet: "The partition is encoded via high bits in the entity ID. Therefore, entities in the same partition are sorted together… Partitions are strictly a locality optimization." [D-PART]. The id deliberately gives away *where*, because the id is an index key and its bits decide what sits next to what on disk.

---
**datalog L196-196 · BODY · DISAGREES C1**

- REPORTED. For ids that must be global, Datomic says use a UUID as a *fact about* the entity, and prefers ones that lead with time. It states the leak plainly: "If the ability to discover the time that a squuid was created leaks sensitive information, then squuids may not be appropriate. However, you should still prefer Squuids (or v7 UUIDs) if your ids may ever be indexed in other, non-Datomic systems." [D-IDENT]

---
**datalog L198-199 · BODY · CARRIED C1,W1**

- INFERRED. On the leak question the camp's position is: decide what you want near what. A time-leading id clusters recent writes and leaks creation time. A random id leaks nothing and scatters every lookup. For a store where people's private acts are entities, the leak is real: an id that shows when a private note was made is visible to anyone who is ever handed the id.


---
**datalog L240-241 · BODY · CARRIED C1,E3**

- REPORTED, and the camp's reason for one stamp per saying: "entire transactions are never redundant, as the transaction time is always a new fact." [D-TXDATA]. The time is a fact about the act.


---
**datalog L282-282 · BODY · CARRIED C1,C6**

- REPORTED precedent. Datomic's own docs model a causal link as an attribute on the saying: `[1235 :correction/for 1234]`. "This might be used to, for example, record that a particular transaction corrects another transaction that was made in error." [D-MODEL]

---
**datalog L283-284 · BODY · CARRIED C1**

- REPORTED. Hickey chose the transaction as the path to time because it is "also a path to other information about what happened, including provenance, or causality." [H-DD]. Causality is named as belonging to the saying.


---
**datalog L287-287 · BODY · DISAGREES C1**

- REPORTED. In Datomic a fact has no id of its own. It *is* its five parts. You point at an entity, or at a transaction, or you name entity plus attribute at a point in time. Hickey declined to give facts their own identity: "Reified transactions can not be used to model a graph", and "many of the common business uses for reified edges are better handled by reified transactions." [H-ML16]

---
**datalog L288-288 · BODY · DISAGREES C1**

- INFERRED. So the camp's answer to "gate's number or own id" is: the gate's number, because it identifies the saying and carries order. If you find you need to point at one fact and say things about it, the camp would say you have found an entity you have not yet named.

---
**datalog L289-289 · BODY · CARRIED C1**

- REPORTED. Hickey on content hashes as names, 2016: people "like the characteristics of it, in terms of being a universal unforgeable key. But it does not convey anything about order unless you have the rest of the repo. It does not imply anything about causality. I mean '4 is greater than 3' at least says that. It came after." [H-SPEC]. He sees value in both and wants them joined: "I think that there is a way to integrate this stuff."

---
**datalog L290-291 · BODY · DISAGREES C1**

- INFERRED. For one store, the gate's number is enough and is better, because it orders. For a second store, the gate's number means nothing outside its own store, so something that survives the crossing is needed: a store id with the number, or a content hash. If it is a content hash, read section 4 first, because XTDB shows what a content hash does when the content must be forgotten.


---
**datalog L359-362 · BODY · DISAGREES C1,C4**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

**What Datomic implies for a second store.** Entity ids are local to a database ("database-unique"). Names are not: the ident `:person/email` means the same in every database that uses it, and the system's own vocabulary has the same ids everywhere. The order number `t` is local. So across two Datomic databases, what travels is names, outside keys held as unique identities, and wall-clock time as a rough guide. What does not travel is an entity id, a transaction number, or a basis. A query can take both databases as arguments, each with its own basis. Nubank's experience (next section) is that this works for a handful of databases and stops working for analytics across thousands.

---


---
**datalog L371-372 · BODY · CARRIED C1,C8,A2**
*3. Nubank: Datomic lived with, at scale › 3.1 What they built, and what they chose*

They use the reified transaction exactly as Hickey intended: "transactions in Datomic are a first-class concept, and we attach that with the Git version of the service, we attach the credentials of the user that is making that transaction, and several other things." [N-QCON]


---
**datalog L467-468 · BODY · CARRIED C1,C5**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.3 What they changed, and what bit them*

3. **The same content coming back.** "There is a race condition which would happen if someone tries to resurrect a document in quick succession of evicting it, or replaying it. We also need to mitigate someone submitting a new document for an evicted entity." — Råberg, 2019 [X-432]. That issue is still open. With content-addressed ids, identical content *is* the same id. So forgotten content that arrives again cannot be told from the original unless the store keeps a permanent marker that this content was evicted.


---
**datalog L486-486 · BODY · CARRIED C1,C5**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.4 Which questions they speak to*

- **(1) content-computed ids.** REPORTED. XTDB v1 used them for documents, and named three benefits in one paragraph: erasure, de-duplication, ordering. The costs are items 2 and 3 above. INFERRED: version 2 identifies a row by a user-supplied `_id` and no longer exposes a content hash, so at the level of a record they moved off it. I found no statement saying so. What stayed content-determined is the *file*: "if two nodes upload the same file, we know that they have the same content." [X-BI3]. *My own note, not from their writing:* a content hash left in a permanent log still says something after the content is gone. Anyone who can guess the content can confirm it was there. For short, guessable values (a name, a diagnosis, a yes or no) a bare hash is not erasure. A salt per record, itself forgettable, closes this.

---
**frontiers L313-314 · BODY · CARRIED C1**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.2 Reasons, in Goebel's words*

One short post takes the five-part datom (entity, attribute, value, transaction, added?) apart slot by slot. ([What's In A Datom?, 24 Dec 2018](https://www.nikolasgoebel.com/2018/12/24/whats-in-a-datom.html))


---
**frontiers L315-315 · BODY · DISAGREES C1**

- Ids. REPORTED: "Within a system, identifying entities by positive integers (eids) should'nt usually leave much to be desired. The question of what to put in the e-slot becomes more interesting, once we consider communcation across system boundaries. Separate systems might not use the same identification scheme. Even if they do, systems need to coordinate the assignment of identifiers, such as to avoid collisions." "Alternatively, clients might make use of a UUID scheme, in order to avoid coordination entirely. We can therefore add the new shape [uuid a v tx added?] to our collection, for use in communication between separate eid domains."

---
**frontiers L330-330 · BODY · DISAGREES C1**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.4 Which questions Goebel speaks to*

- **(2) ids.** REPORTED: integers inside one system; UUIDs the moment two id domains must talk, "to avoid coordination entirely". INFERRED: for one store with client-minted ids and a possible second store, this says 128-bit random from the start. Goebel is silent on ids that leak time or place.

---
**frontiers L353-354 · BODY · NEW-REASON C1**
*7. Question by question: what this camp says*

**(2) Ids.** Thin here. Goebel: UUIDs across id domains (REPORTED). INFERRED from CALM: a client can mint a random id with no coordination; issuing sequential ids needs coordination.


---
**frontiers L377-378 · BODY · DISAGREES C1**

**(1) Version: gate's number, or the fact's own id? Random or from content?** INFERRED from the CDC format: an update's identity is its content plus its time ("there should be only one entry for each `(data, time)`", REPORTED). That is why duplicates and reordering do no harm. An id computed from content makes re-delivery and cross-store exchange idempotent. The gate's number is still needed, because it is the order.


---
**log L73-73 · BODY · NEW-CASE C1,X2**
*Voice by voice › 1. Pat Helland*

- On pointers: "When referencing data from outside, the identifier used for the reference must specify data that is immutable. If you find an immutable document that tells you to read today's New York Times to find out more details, that doesn't do you any good without more details (specifically the date and region of the paper)." He continues: "Note that this model allows for each data item to refer to its schema using simply another arc in the DAG." (same.)

---
**log L74-74 · BODY · DISAGREES C1**

- On what names an immutable thing: "By first binding the changing value of the key to a unique version of the key (e.g., [Key, Version-1]), you can view the version as immutable data." (*Mind Your State for Your State of Mind*, ACM Queue 2018.)

---
**log L79-79 · BODY · NEW-REASON C1**

- On ids: "All that really matters is that the identity is unique within the spatial and temporal bounds of its use." and, on a central authority that hands out ids: "Does this authority scale?" (*Identity by Any Other Name*, ACM Queue 2019.) And: "Another important technique is to ensure that important identifiers such as customer IDs are never reused." (*Outside versus Inside*.)

---
**log L87-87 · BODY · NEW-REASON C1,E5**

- (4) REPORTED: a pointer must name an immutable version. INFERRED: "follow the latest" is something a reader computes; it is never what a stored pointer says.

---
**log L89-89 · BODY · DISAGREES C1,C6**

- (1), (6) REPORTED: the name of an immutable thing is [key, version]; a version records what it replaces ("one parent and one child"), and a store that is not linearizable produces many parents.

---
**log L95-95 · BODY · NEW-REASON C1**

- (2) REPORTED: ids never reused, scoped to cover every party that will use them. He does not choose between random and content-derived, and says nothing on whether an id may leak its origin.

---
**log L116-116 · BODY · DISAGREES C1**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- Retry: "Retries are idempotent (i.e., the same command is written to the same position)". (Delos, OSDI 2020.) This works only because the position is fixed before the retry.

---
**log L133-133 · BODY · DISAGREES C1**

- (1) INSTITUTIONAL: inside one log the position is the only name, and it is enough. INFERRED: it stopped being enough the moment the log became a chain of logs. Delos had to keep positions stable across different log implementations, which is an argument for not letting a pointer depend on physical layout.

---
**log L166-166 · BODY · NEW-CASE C1**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- The trap inside that rewrite: "Can it really be considered idempotency when two completely different events have the same identifier but potentially drastically different information in them?"

---
**log L176-176 · BODY · NEW-REASON C1**

- (1), (2) REPORTED: client-made id for idempotent retry, scoped to the stream; plus the stream revision for order. Two names, two jobs.

---
**log L200-200 · BODY · DISAGREES C1**
*Voice by voice › 4. Martin Kleppmann*

- Naming by content: "Let u be any update, encoded as a byte string. We then identify u by its hash H (u)". Why: counter-based ids work "only … with trusted nodes, since a Byzantine node can easily generate duplicate IDs." The costs: "The ID is therefore only known after the update has been encoded as a byte string", and "The downside of using hashes as IDs is that they require more space than other schemes." Record only direct parents: "Dependencies that can be reached transitively via other dependencies are not included in the set of predecessor hashes, which ensures that this set remains small". (*Making CRDTs Byzantine Fault Tolerant*, 2022.)

---
**log L209-209 · BODY · CARRIED C1**

- His shipped system pins pointers with both name and hash: a reference to a record in another repository "also includes its CID". Accounts have "an immutable, unique identifier", so a user can "change their handle without affecting their social graph."

---
**log L210-210 · BODY · DISAGREES C1**

- Automerge, which he co-designed: actor ids are random ("the reference library generates 128-bit random identifiers"); "Operation IDs are lamport timestamps"; a change "is identified by its change hash"; the wall clock is "optional"; the root object is "identified by the object ID with a null actor id and null counter".

---
**log L214-214 · BODY · DISAGREES C1,C6**

- (1), (2), (6) REPORTED: random opaque ids for long-lived things (actors, accounts); content hashes for versions and changes; parents recorded on each change; pointers that carry name plus hash. This is the most complete shipped answer in the camp, and it is the one I lean on for question 1.

---
**log L229-230 · BODY · DISAGREES C1**
*Voice by voice › 5. Certificate Transparency, Trillian, and their descendants*

**1. What they built and chose.** Since 2013, every publicly trusted web certificate is written to append-only Merkle-tree logs that anyone may read and check. One sequencer per log. Many independent logs, with no order across them. An entry has two names: the hash of its content and its index. The log's own clock stamps everything.


---
**log L237-237 · BODY · NEW-REASON C1,E1**

- Retry: "If the log has previously seen the certificate, it MAY return the same SCT as it returned before." (RFC 6962.)

---
**log L238-238 · BODY · DISAGREES C1,C4**

- Genesis: "The hash of an empty list is the hash of an empty string". A log is identified by "the SHA-256 hash of the log's public key". The list of logs a browser accepts lives outside every log, in browser policy. (RFC 6962; Chrome CT policy.)

---
**log L250-250 · BODY · DISAGREES C1**

- *Duplicate suppression is an optimization.* "this cache can be best-effort and lossy: if some entries were lost, the log can just accept a few duplicates".

---
**log L258-258 · BODY · DISAGREES C1**

- (1) REPORTED: two names. The hash is what a client already holds; the index is what the tree needs. Putting the index in the receipt removed a database.

---
**log L259-259 · BODY · DISAGREES C1**

- (2) REPORTED: resubmission may return the same receipt; exact duplicate suppression is not a correctness property.

---
**log L264-264 · BODY · DISAGREES C1,C4**

- (17) REPORTED: every log starts from the same empty state; identity is a key; the trust list lives outside.

---
**log L290-290 · BODY · NEW-REASON C1**
*Voice by voice › 7. Phil Bernstein's Hyder*

- Retry: "AppendStripe is atomic. It is also idempotent, so a caller that fails to receive a reply from an AppendStripe can simply reissue the operation." (CIDR 2011.)

---
**log L351-352 · BODY · NEW-REASON C1**
*Voice by voice › 9. Two short notes on voices the brief did not list*

- The caveat that matters for ids: "If you do depot appends as part of your microbatch topology to either the same module or other modules, those currently do not have exactly-once semantics in the face of failures and retries. However, this is on our roadmap."


---
**log L378-379 · BODY · NEW-REASON C1**
*Question by question: what this camp would say › (2) Entity: how is an id made so two never clash? May an id *

REPORTED. Helland: unique "within the spatial and temporal bounds of its use", never reused, and a central authority raises the question "Does this authority scale?". Kleppmann: ids built from a node id and a counter work "only … with trusted nodes". Automerge mints "128-bit random identifiers". The AT Protocol gives every account "an immutable, unique identifier" so that the human-readable handle can change. Event Store takes a client-made id and scopes its duplicate check: "The idempotence check is based on the EventId and stream."


---
**log L382-383 · BODY · NEW-REASON C1**

**For the first record (INFERRED).** Random, opaque, client-minted, never reused. Decide the scope of the duplicate check on purpose (Event Store's is per stream, and its documentation admits "Idempotence is not guaranteed if you use ExpectedVersion.Any").


---
**log L452-453 · BODY · CARRIED C1,E5**
*Question by question: what this camp would say › (4) Based on: depends-on or merely passed through? Follow th*

REPORTED. Pin, always. Helland: the pointer "must specify data that is immutable", and the newspaper example shows why a pointer to "today's" anything is useless later. Tango's read set carries versions. The AT Protocol's pointer "also includes its CID". Helland even names the fork, as two delivery modes: history with no gaps, or currency, the latest with gaps allowed.


---
**log L471-471 · BODY · NEW-REASON C1**
*Question by question: what this camp would say › (1) Version: pointed at by the gate's number, or by its own *

- Event Store: a client-made event id for retry, a stream revision for order, a global position for reading everything.

---
**log L472-472 · BODY · DISAGREES C1**

- CT: the content hash is what a client already holds; the index is what the tree needs. Putting the index in the receipt removed a database.

---
**log L473-473 · BODY · CARRIED C1**

- The AT Protocol: a pointer carries the record's name and its content hash.

---
**log L474-474 · BODY · DISAGREES C1**

- Automerge: random actor ids; operation ids that "are lamport timestamps"; each change "identified by its change hash".

---
**log L475-475 · BODY · DISAGREES C1**

- Position-only systems (Tango, Hyder, Aurora) work inside one log. Delos had to virtualize positions once one log became a chain of logs.

---
**log L476-476 · BODY · CARRIED C1**

- Kleppmann on hashes: they cannot be forged by an untrusted writer, they are "only known after the update has been encoded", and "they require more space".

---
**log L477-477 · BODY · NEW-CASE C1**

- Young on the failure: "two completely different events have the same identifier".

---
**log L478-478 · BODY · NEW-REASON C1**

- Scalog: deciding the position before the record is safe is what creates holes.

---
**log L479-480 · BODY · NEW-REASON C1**

- Rama: a topology's depot appends may repeat after a failure.


---
**log L481-482 · BODY · DISAGREES C1**

**For the first record (INFERRED).** Three names, three jobs. An own id minted before the gate, random and opaque, so that a retry is recognizably the same offer, so that a thing can be pointed at before it lands, and so that a pointer survives re-partitioning or a second store. The gate's number, for order and compare-and-set inside a cell, meaningful only in this store and this partition. A content hash over the canonical form, which turns "same id, never different content" from a promise into a check. Pointers (based-on, because-of, replaces) carry the own id plus the hash, and may carry the gate's number as a hint. The shipped systems suggest a split for random versus computed: long-lived identities are random and opaque (actors, accounts, entities), while versions and changes are named by content (change hashes, CIDs). Two cautions on naming a fact by its content. The hash has to cover the offer's parts and not the gate's, or it cannot exist before admission. And once a value is erased, a bare content hash of it leaks; see (9).


---
**log L534-535 · BODY · DISAGREES C1**
*For the group › 7. The voices that matter most for Sid, who I dropped, who i*

**Kleppmann is the internal dissenter.** His path from one log to many, and his shipped naming scheme (random ids for identities, hashes for versions, pointers that carry both), are what I would take from him.


---
**log L569-569 · BODY · NEW-REASON C1**
*The second store: what each source implies*

- **Helland.** Ids must be scoped to cover every party that will ever use them, and "It is not uncommon for one system to provide an alias for its identifiers." Pointers must name immutable versions. Reference data is published with version ids "known to be increasing". INFERRED: own ids and content hashes cross a store boundary; gate numbers do not.

---
**log L571-571 · BODY · DISAGREES C1**

- **Kleppmann.** Updates named by hash merge with no coordinator, and a pointer into someone else's repository carries the hash, so neither side has to trust the other. Counter-based ids fail the moment a second, less trusted writer exists.

---
**meaning L546-554 · BODY · DISAGREES C1,C4**
*Part one — the exchange › 1.7 What each would make of Sid's design (all INFERRED)*

- *Names.* REPORTED (same talk): "the globally qualified name space names will
  be the identity names. [...] The value names, you want to be conflict free,
  tear off names that anyone can create without coordination, and that's what a
  UUID is about." And: "What should you care about, about a value name? Nothing
  at all." Hickey's own system answers (3) with both: an attribute is an entity with
  an id, and its name is a fact about it. INSTITUTIONAL (Datomic docs, checked):
  after a rename, "Both the new ident and the old ident will refer to the
  entity"; and "You can never alter :db/valueType". So the name can move. The
  shape cannot.

---
**meaning L585-590 · BODY · ABOVE C1**

- *One store.* Kay's model is the Internet, which has no centre. INFERRED: Kay
  would design for parties that have never met, which means the second store is
  the design case, not the rare case. For Sid this lands on ids: a gate-assigned
  number for a grammar means nothing in another store. An id that travels
  (minted globally unique, or derived from content) does.


---
**meaning L646-651 · BODY · DISAGREES C1,E4**
*Part two — the people › 2.1 The capability camp: Hardy, Miller, Yee, Shapiro, Donnel*

- The alternative, same paper. REPORTED: "every capability can serve both to
  designate which resource to access, and to provide the authority to perform
  that access. This change provides us with the option to avoid introducing a
  shared namespace into the foundations of the model, and thereby avoid the
  complex issues involved in managing a shared namespace – issues rarely
  acknowledged as a cost of non-capability models."

---
**meaning L764-768 · BODY · NEW-CASE C1,E4**

- (2) ids. INFERRED: ids in Sid's store get copied into based-on lists,
  verdicts, and crossings. So a bare id must never grant anything. Knowing an id
  must not be enough to read or write it. MyWebstrates made the id the
  capability and found (3.3) that access could then never be revoked.


---
**meaning L806-813 · BODY · ABOVE C1,C8**

- INFERRED: this cuts both ways for Sid. A shared heap is coherent when there is
  one heap. The fan-out cost is a cost of many full copies. So these numbers
  support "one store" as a design that hangs together, and they say the second
  store must not be a second full heap. Between stores, deliver by address. The essay's
  other point stands against one store: the operator sees everything, and a
  person cannot credibly leave. Facts whose ids and authorship only the gate
  can vouch for mean nothing outside the gate's store.


---
**meaning L831-844 · BODY · DISAGREES C1,C4**
*Part two — the people › 2.2 Kenton Varda: Protocol Buffers, Cap'n Proto, Sandstorm, *

- REPORTED (Varda, Cap'n Proto language reference,
  https://capnproto.org/language.html#unique-ids), on why types get a 64-bit id
  and not a global symbolic name: "Programmers often feel the need to change
  symbolic names and organization in order to make their code cleaner, but the
  renamed code should still work with existing encoded data. It's easy for
  symbolic names to collide, and these collisions could be hard to detect in a
  large distributed system with many different binaries using different versions
  of protocols. Fully-qualified type names may be large and waste space when
  transmitted on the wire." Ids are made with `capnp id`, at random. By default
  an inner declaration's id is derived from the parent's id and the name; you
  pin it by hand only "if that declaration has been renamed or moved and you
  want the ID to stay the same". Varda adds: "Collisions from misuse (e.g. copying
  an example without changing the ID) are much more likely" than random ones.


---
**meaning L923-930 · BODY · DISAGREES C1**

**Ids.** INSTITUTIONAL (Cloudflare docs): an id made from a name needs a global
uniqueness check: "this round-the-world check can take up to a few hundred
milliseconds. newUniqueId can skip this check." Jurisdiction is minted into the
id (`newUniqueId({ jurisdiction: "eu" })`): an id that says where, on purpose,
for law. And a live case of Sid's exact worry: "Alarms created before
2026-03-15 do not have name stored. When such an alarm fires, ctx.id.name will
be undefined".


---
**meaning L952-958 · BODY · DISAGREES C1**
*Part two — the people › 2.3 Unison (Paul Chiusano, Rúnar Bjarnason, Arya Irani), and*

### 2.3 Unison (Paul Chiusano, Rúnar Bjarnason, Arya Irani), and Git's hash

**What they built.** A language where every definition is identified by a hash
of its syntax tree. Names are metadata kept apart. They have run it for about
ten years. They are the best evidence on question (1), because they ran both
kinds of id and changed their minds.


---
**meaning L959-964 · BODY · DISAGREES C1,C4**

**Reasons.** REPORTED (Unison docs, "The big idea"): "names are just separately
stored metadata that don't affect the function's hash." And: "Names are like
pointers to addresses in this space. We can change what address a name points
to, but the contents of each address are forever unchanging." Chiusano (2014):
"We need not all agree on the metadata associated with each term".


---
**meaning L965-976 · BODY · DISAGREES C1**

**The flip: an id computed from content, or minted at random?** Unison types
can be `structural` (the id is a hash of the shape) or `unique` (a random id is
minted and mixed in).

- 2021: Chiusano opens issue 2251: "It's very common to leave off `unique` for
  types that really should be." They made both keywords mandatory for two
  years.
- 2023–24: issue 4539 makes `unique` the default. REPORTED (GitHub user ceedubs, who opened the issue):
  "`type UserId = UserId Nat` and `type EpochMillis = EpochMillis Nat` are
  definitely not intended to be treated as the same type". Structural types
  "tend to come up much more in core libraries like `base` than in user/app
  code." Bjarnason: "Sgtm". Chiusano: "Sounds good to me too! It's time."

---
**meaning L977-981 · BODY · NEW-CASE C1**

- The cost of random ids, which they also paid (issue 2196): "A unique type
  definition not deleted from a scratch file will always be treated as an update
  [...] after an `add`." Showing the id to people was judged "unpalatable". The
  fix re-derives the random id from name plus shape: "if the type has the same
  structure and the same name, it gets the same guid and hash."

---
**meaning L982-991 · BODY · DISAGREES C1**

- Lesson, INFERRED. A hash is the right id for a thing whose identity *is* its
  content: a tool body, a grammar version, a definition of "stale". It is the
  wrong id for a thing whose identity is an act or an intent. Two people who
  assert the same thing have made two facts. Two entities with the same
  description are two entities. But a purely random id makes an honest retry
  look like a new thing. For agents writing at machine rate that matters: a
  content hash of the *offer* makes a retry idempotent. So there are two ids
  doing two jobs: the offer's own id, made by its maker, which travels; and the
  gate's number, which is local.


---
**meaning L992-998 · BODY · DISAGREES C1**

**Both ids, and a translation table.** REPORTED (codebase format v2 doc): "most
objects in the v2 codebase format are referenced by `object.id`", a local
integer, with hashes as the portable identity. Syncing walks "`ObjectId 3` ->
`HashId 14` -> `#asodcj3` -> `HashId 16` -> `ObjectId 2`". MyWebstrates reached
the same place from the other side (3.3): "incremental version numbers are more
usable but are only locally valid, while version hashes are globally valid."


---
**meaning L999-1005 · BODY · CARRIED C1,A1**

**What is not in the id at birth is gone.** REPORTED (Chiusano, issue 2276,
2021): "The definitions don't have their type signature baked into the hash
because it matches what was inferred when the definition was added. Now that the
inference algorithm is different… In general, there seems to be no way to
replace the 'variant' associate with a hash." This is Sid's sentence, met in the
field.


---
**meaning L1023-1027 · BODY · CARRIED C1**

**No algorithm tag in the id.** Chiusano opened "Add version info to Hashes" in
2019, proposing `<multibase><unison-multicodec-id><unison-version-id><multihash>`.
It is still open. They later ran a full rehash of codebases with an old-to-new
translation table.


---
**meaning L1028-1038 · BODY · CARRIED C1**

**Git, the same lesson at larger scale.** INSTITUTIONAL (Git,
hash-function-transition): "The signed payload for signed commits and tags does
not explicitly name the hash used to identify objects. If some day Git adopts a
new hash function with the same length [...] the intent behind the PGP signed
payload in an object signature is unclear [...] Fortunately SHA-256 and SHA-1
have different lengths." They are relying on a length accident as the tag. On
keeping two hashes side by side: "they will never go away, so they accumulate."
On converting old objects: the conversion "retains any brokenness in the
original object [...] This is a deliberate feature of the design to allow the
conversion to round-trip."


---
**meaning L1039-1046 · BODY · CARRIED C1**

**Transfer.** Unison stores code, not claims by people; it has no deletion
problem and no authority problem. Its ids are made by many machines with no
gate. Sid has a gate, which makes store-assigned numbers cheap; Unison's
experience says a number alone is not enough once a second store exists.
Disagreements: Irani against Chiusano on whether hashes may ever change;
contributors against each other on whether structural types are worth having
("it's extremely rare that they _are_ the right solution").


---
**meaning L1047-1053 · BODY · DISAGREES C1**
*Part two — the people › 2.4 IPFS and IPLD (Juan Benet), and what AT Protocol did wit*

### 2.4 IPFS and IPLD (Juan Benet), and what AT Protocol did with them

**What they built.** Content-addressed storage where an id is a hash, links are
ids, and mutable names are signed pointers. AT Protocol (Bluesky) then built
signed personal repositories on IPLD-style hashes, ran them as a large public
social network, and walked part of it back.


---
**meaning L1054-1063 · BODY · CARRIED C1**

**Self-describing ids.** REPORTED (Benet, 2014, the multihash proposal): "As
time passes, software that uses a particular hash function will often need to
upgrade [...] This introduces large costs: systems may assume a particular hash
size, or call `sha1` all over the place." Whitepaper §3.1: "Rather than locking
the system to a particular set of function choices, IPFS favors self-describing
values." The costs, INSTITUTIONAL (multihash README): "multihash values bias
the first two bytes"; and "Obsolete and deprecated hash functions are included
[...] since many such hashes already exist". A scheme, once used, never
retires.


---
**meaning L1064-1071 · BODY · CARRIED C1**

**An id format change never finishes.** INSTITUTIONAL (CID spec): "there will be
no CIDv18 (0x12 = 18) to prevent ambiguity with decoded CIDv0s." A version
number is burned forever so that old ids still parse. The IPFS docs still say
CIDv1 will become the default "in the near future", about a decade on. did:plc
(Bluesky's identity log): "there exist many did:plc identifiers where the DID
identifier itself is based on the hash of the old format, so they will
unfortunately be around forever."


---
**meaning L1072-1082 · BODY · CARRIED C1,W2**

**A content hash names an encoding, not a value.** INSTITUTIONAL (IPFS docs):
"Two identical files can produce different CIDs. The CID depends on both the
content *and* how that content is structured". DAG-CBOR spec: it "requires that
there exist a single, canonical way of encoding any given set of data". Then the
walk-back: "Due to the existence and active use of historical data, and the
existence and active use of non-conforming encoders, DAG-CBOR decoders may relax
strictness requirements by default." INFERRED: if any id in Sid's store is a
hash, the exact bytes that are hashed must be fixed before the first one. Sid
has an advantage IPFS lacked: one gate can refuse any offer that is not in
canonical form.


---
**meaning L1121-1124 · BODY · DISAGREES C1,C6**

- What it says to (6), INFERRED. A strong back-pointer (the hash of the record
  you replace) makes the old record impossible to remove without breaking the
  chain. AT Protocol removed it for that reason. "Replacing 25" kept as a plain
  number does not have this problem. Kept as a hash, it does.

---
**meaning L1127-1137 · BODY · DISAGREES C1**

**Ids that say when.** INSTITUTIONAL (AT Protocol TID and record-key specs):
"Implementations should not rely on global uniqueness of TIDs, and should not
trust TID timestamps as actual record creation timestamps. Record keys are
'user-controlled data'". did:plc spec: "The timestamp metadata encoded in the
PLC audit log could be cross-verified against network traffic or other
information to de-anonymize account holders. It also makes the 'identity
creation date' public." Note did:plc itself is a one-gate, append-only log: the
server "either rejects the operation or accepts and permanently stores the
operation, along with a server-generated timestamp", and even nullified
operations "can be enumerated and audited".


---
**meaning L1154-1160 · BODY · DISAGREES C1**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

**Reasons.**

- Ids. REPORTED (Vrandečić and Krötzsch, CACM 2014): "Item IDs can be used as
  language-independent identifiers"; "IDs do not depend on language labels;
  items can be deleted, though IDs are never reused". The stated reasons are
  language and stability. I could not find any stated concern that the ids leak
  creation order. They are sequential, so they do.

---
**meaning L1166-1172 · BODY · DISAGREES C1**

- Statement ids. INSTITUTIONAL (Wikibase JSON docs): "An arbitrary identifier
  for the Statement [...] No assumptions can and shall be made about the
  identifier's structure". In practice it is the entity id, a `$`, and a random
  UUID. Values and references inside a statement carry a content hash. So:
  random ids for the act of asserting, hashes for the content. Same split as
  Unison.


---
**meaning L1268-1276 · BODY · DISAGREES C1**
*Part two — the people › 2.6 RDF, W3C PROV, nanopublications, trusty URIs*

### 2.6 RDF, W3C PROV, nanopublications, trusty URIs

**What they built.** RDF gave every predicate a global id. PROV (W3C, 2013) is
a standard vocabulary for provenance; its parts map closely onto Sid's "by
whom", "based on", and "acts for". Nanopublications (Groth, Mons, Kuhn,
Dumontier and others) are small signed assertions, each with its provenance and
its publication info, stored in an append-only network under content-hash ids
called trusty URIs. They have run for about ten years.


---
**meaning L1333-1341 · BODY · DISAGREES C1**

**Trusty URIs: a hash as the id, done carefully.** Kuhn and Dumontier, ESWC
2014, https://arxiv.org/abs/1401.5775.

- How: "the RDF statements are sorted, then they are serialized in a given way
  (interpreting the artifact's hash as a blank space), and finally SHA-256 is
  applied". The record contains its own id, so the id is hashed as a blank and
  put in afterwards. Local ids (blank nodes) are converted to global ones
  first. The id carries a two-letter module code that names the kind and version
  of hashing. That is the algorithm tag Git never had.

---
**meaning L1342-1347 · BODY · DISAGREES C1**

- What it buys. REPORTED: "Once a trusty URI is established, its artifact code
  defines what object it refers to, and the issuing authority has no longer the
  power to change its meaning." Kuhn et al. (2016): "servers only have to deal
  with adding new entries but not with updating them, which eliminates the hard
  problems of concurrency control and data integrity in distributed systems."
  And: "servers do not have to deal with identifier management".

---
**meaning L1348-1351 · BODY · CARRIED C1,P0**

- What it does not buy. REPORTED: permanence holds only "if we assume that there
  are search engines and web archives crawling the artifacts on the web and
  caching them." A hash proves sameness. It does not keep anything.


---
**meaning L1387-1389 · BODY · NEW-REASON C1**

- Ids should hold nothing that can change. REPORTED (Berners-Lee, 1998): "URIs
  don't change: people change them." "URIs change when there is some information
  in them which changes."

---
**meaning L1394-1400 · BODY · NEW-REASON C1**

- Statements about statements took three tries: RDF reification, then named
  graphs (which nanopublications use), then RDF 1.2 triple terms. RDF 1.2:
  "It is expected that the reifiers (rather than the triple terms) will be used
  in further statements". INFERRED: a verdict, a doubt, or a retraction has to
  point at the *act of asserting*, not at the content asserted. Two people who
  say the same thing have made two facts. So a fact needs an id of its own, and
  a hash of entity, key, and value alone cannot be it.

---
**meaning L1401-1405 · BODY · CARRIED C1**

- Local ids that do not travel were a mistake. INSTITUTIONAL (RDF
  canonicalisation, 2024): "blank node identifiers [...] are not intended to be
  persistent or portable". A whole standard was needed to make graphs hashable,
  and it still "does not define such a graph signature."


---
**meaning L1514-1515 · BODY · DISAGREES C1**
*Part two — the people › 2.7 Kay beyond the thread, STEPS, Worlds, and Ingalls*

- "Uniform reference is achieved simply by associating a unique integer with
  every object in the system."

---
**meaning L1549-1552 · BODY · DISAGREES C1,C2**
*Part two — the people › 2.8 David Reed's pseudo-time, and Croquet*

- Versions. "We think of each object as a sequence of versions. Each WRITE to an
  object creates a new version... Once created, a version's value does not
  change." And: "the {object name, pseudotime) pair uniquely identifies the
  version." That is Sid's cell and version number.

---
**meaning L1553-1557 · BODY · DISAGREES C1,E3**

- Whose clock. The writer's. "The implementation uses approximately synchronized
  real-time clocks at each node [...] a unique site identifier is concatenated
  as the low-order bits. Thus, even though two sites need not communicate, it is
  guaranteed that the sets of time stamps they generate are disjoint." Time *is*
  the order, and nobody central hands it out.

---
**meaning L1634-1637 · BODY · DISAGREES C1**
*Part two — the people › 2.9 Joe Armstrong*

- "SHA1 checksums are fine for content that is immutable (doesn't change) - but
  what about a file whose content changes with time? To solve this I propose
  adding UUIDs to files. UUIDs can be generated locally without using a
  centralized server."

---
**meaning L1641-1644 · BODY · DISAGREES C1**

- "Why three webs? * The web of names is convenient and easy to use * The web of
  UUIDs allows us to track content that changes with time * The web of hashes
  (SHA1) allows total precision in managing content… I think we need all three."


---
**meaning L1645-1653 · BODY · DISAGREES C1**

Armstrong reached this in steps. 2011: every function in one global store under a
unique name. 2014 (a talk, known only from live captions): replace names with
hashes. 2015: all three. The same three-way split shows up, separately, in
Hickey (identity names and tear-off value names), Stiegler (key, nickname,
petname), Wikidata (item ids, random statement ids, hashes on values), and
Unison (names, minted ids for unique types, hashes). Five lines of work, one
shape: **a minted id for a thing that lasts through change, a hash for content
that never changes, and names as data about either.**


---
**meaning L1964-1973 · BODY · DISAGREES C1**
*Part three — substrates › 3.3 Webstrates (Klokmose, Eagan, Baader, Mackay, Beaudouin-L*

**Why they left the central server.** REPORTED (MyWebstrates, UIST 2024): "the
centralised server effectively 'owns' and controls the user's data. This
violates what we refer to as personal and collective digital sovereignty". What
it cost them: "there is not a central server with an authoritative order of these
changes. Hence, versions are no longer guaranteed incremental [...] These
incremental version numbers are more usable but are only locally valid, while
version hashes are globally valid." And authority became the id: "the ID of a
webstrate represents a basic security capability [...] today, there is no
mechanism for revoking access to a document."


---
**meaning L2028-2038 · BODY · DISAGREES C1**
*Part three — substrates › 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tri*

**Ids as paths that anyone can extend.** INSTITUTIONAL (Udanax Green manual):
"any node could give addresses to new nodes by appending another tumbler digit to
its own tumbler address. For example, node 23.4 could create nodes 23.4.1,
23.4.2, 23.4.3". Accounts, documents, and versions nest the same way: "the first
new version adds a tumbler digit. Each successive version increments the last
digit." Everything sorts in one order: "4 < 4.23 < 4.23.7 < 4.24 < 5", and a span
"can cover characters, links, documents, versions, or any other Udanax Green
entities, including the entire docuverse." Nelson (1999): "The central
proprietary secret this all relied on [...] was the freezing of content addresses
into permanent universal IDs".


---
**meaning L2039-2043 · BODY · DISAGREES C1,C6**

For (2): no two ids clash, because each holder mints only beneath itself. No
central minter is needed. The price is that the id gives away where, under whom,
and in what order a thing was made. For (6): "which version replaced which" is
in the shape of the id, not in a stored pointer.


---
**meaning L2109-2115 · BODY · ABOVE C1,C8**

INFERRED: the Web beat Xanadu by giving up the very things Sid wants to keep:
permanence, links that know both ends, one space. That is not a verdict on Sid's
design. Sid has one operator, so one gate can enforce what the Web could not ask
of strangers. The lesson is narrower. Whatever needs every participant's
cooperation to stay whole will not cross an operator boundary. Inside one store,
enforce. Across stores, expect only what a stranger can verify alone.


---
**meaning L2183-2196 · BODY · DISAGREES C1**
*Part four — Sid's questions, hung on the parts of the fact › (2) Entity ids: how made, and may an id give away when or wh*

### (2) Entity ids: how made, and may an id give away when or where?

**Camp.** Mint at random, locally, with no coordination: Hickey ("tear off names
that anyone can create without coordination", 1.7), Armstrong ("generated locally
without using a centralized server", 2.9), Varda (random 64-bit type ids; random
object ids skip a "round-the-world check", 2.2). Varda's warning is that real
clashes come from copying, not chance. Ids that carry information: Xanadu's paths
give away who, where, and order (3.5). Wikidata's counters give away order (2.5).
AT Protocol's time-based ids are "user-controlled data" that cannot be trusted as
time, and the identity log's timestamps can "de-anonymize account holders" (2.4).
Berners-Lee: an id changes "when there is some information in them which changes"
(2.6). One deliberate exception: Cloudflare mints jurisdiction into the id, for
law (2.2).


---
**meaning L2197-2206 · BODY · DISAGREES C1**

**My read.** An agent must be able to name a new entity in several offers before
the gate has seen any of them. So the maker mints, at random, 128 bits or more.
The gate's compare-and-set (expecting no current version) catches a copied id
being "created" twice. Keep ids opaque. "When" and "by whom" belong in facts,
where policy can hide them. An id gets copied into every based-on list and
crossing, where policy cannot reach. The open point is jurisdiction. If law will
ever require some entities to live in a region, the store needs to know that
before it can read their facts. That is the one piece of "where" that may have to
be in the id or the layer. It is gone forever if left out (2.2 has a live example).


---
**meaning L2426-2439 · BODY · DISAGREES C1**
*Part four — Sid's questions, hung on the parts of the fact › (1) Version: the gate's number, or the fact's own id? random*

### (1) Version: the gate's number, or the fact's own id? random, or from content?

**Camp.** Both, for two jobs. Unison keeps a local number and a portable hash, and
needs a translation table to sync (2.3). MyWebstrates: numbers "only locally
valid", hashes "globally valid" (3.3). Reed: object name plus pseudo-time (2.8).
Wikidata: a random id for the statement, hashes for its values (2.5). Trusty URIs:
with a hash, "the issuing authority has no longer the power to change its meaning",
and servers "do not have to deal with identifier management" (2.6). RDF 1.2: what
you say about a fact must point at the act of asserting it (2.6). Armstrong: three
webs (2.9). Warnings: Unison's flip to minted ids, because two things with the same
content are often not the same thing; and its churn when a random id made a retry
look new (2.3). Git and Unison both lacked an algorithm tag (2.3). IPLD: a hash
names an encoding, so the encoding must be fixed first (2.4).


---
**meaning L2440-2450 · BODY · DISAGREES C1**

**My read.** The fact's own id is made by the offerer, before the gate. It is a
hash of the canonical offer, which includes the claimed author, the based-on list,
and a random nonce. The nonce keeps two people's identical claims apart (Unison's
lesson) and doubles as the salt (9). The hash makes a retry idempotent, which
matters at machine rate. It travels to another store. And it is the only thing a
*refusal* can point at, because a refused offer never gets a gate number. If
refusals are kept (7), the fact's own id is forced. The gate's number stays as
what it is: a position, local and cheap, used by "superseded" and "as of". Tag the
algorithm in the id. Let the gate refuse any offer that is not in canonical bytes;
IPFS had no gate to do that, Sid does.


---
**meaning L2460-2464 · BODY · DISAGREES C1,C6**
*Part four — Sid's questions, hung on the parts of the fact › (6) When 37 replaces 25, is "replacing 25" kept on 37?*

**My read.** Sid's offer already states the version it expects to be current.
That *is* "replacing 25". Keep it on the admitted fact instead of dropping it
after the check. Keep it as a number, not a hash, so 25 can still be cut. Add why:
a newer value, a correction, or a withdrawal. Wikidata had to add that later.


---
**meaning L2562-2565 · BODY · DISAGREES C1,C7**
*Part five — for the group › 7. The voices that matter most, who I dropped, who is missin*

2. **Kenton Varda.** Nobody here has lived longer with these exact decisions at
   this scale, or written more plainly about regret. Numbers not names. Never
   reuse. Required is forever. Unknown fields must survive. One writer per small
   unit. An id minted with jurisdiction in it. A click as a grant.

---
**rama L63-63 · BODY · DISAGREES C1**
*Part one — What Rama's own reference says › Question by question › (2) Entity: how is an id made so two never clash? May an id *

- **CHECKED** `skill/unique-ids.md:9` — "If any consuming topology is stream: Generate the ID client-side with `(ops/random-uuid7)` and include it in the depot record. Do NOT generate IDs inside stream topologies — on retry, `ops/random-uuid7` or `ModuleUniqueIdPState` produce a different ID, making the entire entity creation non-idempotent."

---
**rama L64-64 · BODY · NEW-REASON C1**

- **CHECKED** `skill/unique-ids.md:13` — "128 bits is the minimum size needed to avoid birthday paradox collisions at scale… random 64-bit Longs have ~10% collision probability at just 2 billion IDs — unacceptable for production."

---
**rama L65-65 · BODY · DISAGREES C1**

- **CHECKED** `skill/unique-ids.md:15` — "Always use UUID7 over UUID4 when generating UUIDs. UUID7 is time-ordered — IDs sort chronologically, which is useful for range queries on subindexed maps and preserves insertion order."

---
**rama L66-66 · BODY · NEW-REASON C1**

- **CHECKED** `skill/unique-ids.md:17` — ordering is only to the millisecond. "Don't write code or tests that assume sub-millisecond ordering of UUID7s."

---
**rama L67-67 · BODY · DISAGREES C1**

- **CHECKED** `skill/unique-ids.md:44-46` — the in-cluster alternative, `ModuleUniqueIdPState`, packs "22 bits for the generating task ID and 42 bits for a monotonically increasing counter". It is "Safe to use in microbatch topologies" and "Tricky in stream topologies".

---
**rama L68-69 · BODY · DISAGREES C1**

- **CHECKED** `skill/unique-ids.md:98` — composite ids are endorsed: "a 'post' can be identified by `[user-id task-unique-post-id]`".


---
**rama L70-71 · BODY · DISAGREES C1**

On whether an id may give away when or where it was made: Rama's guidance does not treat this as a problem. It treats it as a feature. UUIDv7 gives away the millisecond. The module id generator gives away the task. **NOT IN THE REFERENCE:** any privacy or unlinkability concern about ids. **IMPLIED:** the reason RPL likes time-ordered ids is physical. PState maps are sorted by the serialized key (`docs/17-serialization.md:165-169`), so time-ordered ids put new entries next to each other on disk and make "latest N" a cheap range scan. Random ids scatter writes. If Sid wants ids that reveal nothing, the cost is paid in index locality, and the time order has to be carried by some other sorted key.


---
**rama L88-89 · BODY · CARRIED C1,W1**
*Part one — What Rama's own reference says › Question by question › (3) Key: a word, or an id too?*

- **CHECKED** `docs/17-serialization.md:167-169` — anything used as a PState map key must serialize to the same bytes every time, and sorts on disk by those bytes.


---
**rama L169-169 · BODY · DISAGREES C1**
*Part one — What Rama's own reference says › Question by question › (1) Version: is a fact pointed at by the gate's number, or b*

- **CHECKED** Rama's own name for a record is (partition index, offset) (`docs/14-depots.md:234`).

---
**rama L170-170 · BODY · CARRIED C1,P0**

- **CHECKED** that name survives depot migrations (`docs/14-depots.md:310`).

---
**rama L171-171 · BODY · NEW-CASE C1**

- **CHECKED** it does not survive a restore: "If Module A is restored to a prior version where the depot contained entries up to offset 107, then any new data appended after the restore will start at offset 107" (`docs/22-backups.md:106`). Old offsets get reused for different records.

---
**rama L172-172 · BODY · CARRIED C1,C2**

- **CHECKED** it does not survive a re-partition: the documented way to get more tasks is a new module whose "processing would also make new depots that are a copy of the old depots" (`docs/19-operating-rama.md:487`).

---
**rama L173-173 · BODY · NEW-REASON C1**

- **CHECKED** it does not survive trimming (`docs/14-depots.md:325`).

---
**rama L176-177 · BODY · CARRIED C1,P0**

**IMPLIED:** point at facts by an id inside the record. Treat the gate's number as what it is: an ordering within one cell, not a name. On "random or computed from content": **NOT IN THE REFERENCE.** One consequence is worth stating. A content-derived id and Rama's migration door cannot both be used. A depot migration changes a record's content in place, so a hash of the content would stop matching. If Sid chooses content-derived ids, they are also choosing never to use depot migrations on those records.


---
**rama L188-188 · BODY · NEW-REASON C1,E1**
*Part one — What Rama's own reference says › Question by question › (7) Beside each fact: is the gate's yes/no kept? Where? Are *

- **CHECKED** `skill/stream.md:13` — "A stream topology can retry a record even after all PState writes have completed and committed… every write in a stream topology must be either naturally idempotent… or explicitly deduplicated."

---
**rama L232-232 · BODY · DISAGREES C1**
*Part one — What Rama's own reference says › The gate: stream or microbatch*

| | Stream gate | Microbatch gate |
| In-gate id and counter generation | unsafe on retry | safe |

---
**rama L253-254 · BODY · NEW-REASON C1,C8**
*Part one — What Rama's own reference says › Other items the brief named*

**A later second store.** **CHECKED** mirrors are between modules of one cluster: "Mirrors in Rama don't store data locally – operations done on them always go through to the original" (`docs/18-module-dependencies.md:9`). Appends to a mirror depot do not wait for the other module's stream topologies (`:129`). Reads of another module's PStates are "similar… to read committed" (`docs/23-acid-semantics.md:104`). **NOT IN THE REFERENCE:** anything between clusters. No cross-cluster mirror, no federation, no geo-replication, and no guidance on regions or wide-area links. The only mentions of geography are a sample custom partitioner named `partition-by-region` and the phrase quoted in the next item. **CHECKED** the one bridge for outside logs is `ExternalDepot`: it "adapts an external partitioned log (e.g. Kafka, Kinesis, custom queue) into the Rama depot abstraction" by partition count, start offset, end offset, and fetch-by-offset (`skill/external-depots.md:3-27`). **IMPLIED:** a second store would be read as an external depot, by (partition, offset), and its facts re-admitted through the local gate as ingests. What crosses is the record with its own id. That is one more reason the id and the actor must be inside the record and meaningful without the store that made them.


---
**rama L281-282 · BODY · NEW-REASON C1**
*Part two — Nathan Marz › 2. His reasons, in his words*

On his chapter 2, summarized by him. "The core idea is that each record should be a 'fact' that stands on its own as something true at a moment in time. When you write your batch computations, you should make them work on any set of valid facts. There's nothing wrong with saying the same record twice, as logically 'A and A' is the same as 'A'." (HN, 2012-01-10, https://news.ycombinator.com/item?id=3449142)


---
**rama L301-302 · BODY · DISAGREES C1**
*Part two — Nathan Marz › 3. What he later changed, regretted, or migrated*

**(e) Ids grew up.** Rama's current guidance is 128-bit UUIDv7, made by the client, because "random 64-bit Longs have ~10% collision probability at just 2 billion IDs" (`skill/unique-ids.md`). From memory of *Big Data* chapter 2, which I could not open in this session, the book's pageview example used a random 64-bit nonce to make each fact identifiable. If my memory is right, that is a quiet reversal. Treat it as unverified.


---
**rama L309-310 · BODY · DISAGREES C1**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(2) Entity ids.** REPORTED 2010: his nodes were named by typed, natural identifiers in a union (`union PersonID { 1: string email; }`, a project by apache name or github id or sourceforge id). The id carries the type. That is the opposite of Sid's opaque id with no type slot. INSTITUTIONAL 2026: client-made UUIDv7. INFERRED: he does not value opacity. He values an id that survives a retry and sorts well on disk. He would not worry that an id reveals its millisecond. He would ask Sid a different question: when the same paper is ingested twice, what makes the two entities one? In his model that is a computation over "these two are the same" facts, not something an id scheme solves. (The book's treatment of this is from memory, not opened.)


---
**rama L363-364 · BODY · DISAGREES C1,P0**
*Part three — The dissent: Jay Kreps › 1. What he built, and what he chose*

Kafka at LinkedIn (2010 on), then Confluent. A partitioned, replicated, append-only log as the one thing every other system reads from. His choices: order only within a partition; a record's position is its permanent name; retention is a setting; derived state belongs to consumers and must be rebuildable by deterministic code reading the log.


---
**rama L401-402 · BODY · DISAGREES C1,P0**
*Part three — The dissent: Jay Kreps › 4. Which of Sid's questions he speaks to*

**(0)** REPORTED: retention is a setting. Forever is fine if you run the log like a database. INSTITUTIONAL (Kafka design docs, https://kafka.apache.org/43/design/design/#log-compaction): compaction rewrites old segments in the background, and yet "Ordering of messages is always maintained. Compaction will never re-order messages, just remove some. The offset for a message never changes. It is the permanent identifier for a position in the log." Note what this is. Kafka and Rama were built apart, and landed on the same rule: **positions are permanent; contents may be removed.** Neither camp holds "never rewritten".


---
**rama L411-412 · BODY · DISAGREES C1**

**(1)** INSTITUTIONAL: Kafka names a record by its position. One caution from general knowledge that I did not open a source for: a position is local to the log that gave it. Copying a topic to another cluster gives new offsets. For Sid's later second store this matters. What crosses must carry its own id.


---
**skeptics L32-33 · BODY · DISAGREES C1**
*1. The ordinary default, question by question*

**(2) Entity ids.** Auto-increment integers inside one database. UUIDv4 when many clients or services mint. UUIDv7 when the id is a primary key in a B-tree. The RFC gives the reason. REPORTED: "UUID versions that are not time ordered, such as UUIDv4 (described in Section 5.4), have poor database-index locality. This means that new values created in succession are not close to each other in the index; thus, they require inserts to be performed at random locations. The resulting negative performance effects on the common structures used for this (B-tree and its variants) can be dramatic." The same RFC asks everyone not to look inside: "As general guidance, avoiding parsing UUID values unnecessarily is recommended; instead, treat UUIDs as opaquely as possible." And it admits the leak: "Timestamps embedded in the UUID do pose a very small attack surface. The timestamp in conjunction with an embedded counter does signal the order of creation for a given UUID and its corresponding data". And: "they MUST NOT be used as security capabilities (identifiers whose mere possession grants access)." ([RFC 9562, 2024](https://www.rfc-editor.org/rfc/rfc9562.txt)). Big-tech ids leak on purpose. Instagram's stated requirement, REPORTED: "Generated IDs should be sortable by time (so a list of photo IDs, for example, could be sorted without fetching more information about the photos)" ([Sharding & IDs at Instagram; archived](https://web.archive.org/web/2015/https://instagram-engineering.com/sharding-ids-at-instagram-1cf5a71e5a5c)). Twitter, REPORTED: "To generate the roughly-sorted 64 bit ids in an uncoordinated manner, we settled on a composition of: timestamp, worker number and sequence number." ([Announcing Snowflake, 2010](https://blog.x.com/engineering/en_us/a/2010/announcing-snowflake)). So the default answer to "may an id give away when or where it was made?" is yes, and nobody minds until later (section 3.4).


---
**skeptics L56-57 · BODY · DISAGREES C1,E1**

**(1) Version: gate's number, or own id; random or from content?** Rows are pointed at by primary key, not by version. Optimistic locking uses an integer `version` column or an ETag. Events are pointed at by stream and sequence number, or by a random event id. Content hashes are for blobs and build artifacts. For deduplicating requests the default is a client-minted random key. Stripe, REPORTED: "When performing a request, a client generates a unique ID to identify just that operation and sends it up to the server along with the normal payload." ([Stripe, Designing robust and predictable APIs with idempotency](https://stripe.com/blog/idempotency)). The IETF draft, REPORTED: "The idempotency key MUST be unique and MUST NOT be reused with another request with a different request payload." It allows a content fingerprint only as a helper: "An idempotency fingerprint MAY be used in conjunction with an idempotency key". And keys expire: "The resource MAY require time based idempotency keys to be able to purge or delete a key upon its expiry." ([draft-ietf-httpapi-idempotency-key-header-07](https://datatracker.ietf.org/doc/html/draft-ietf-httpapi-idempotency-key-header))


---
**skeptics L165-166 · BODY · CARRIED C1,E1**
*3. Big-tech operational lessons › 3.1 Amazon*

Idempotent interfaces (Malcolm Featonby). This speaks straight to question (1), random id or content hash. REPORTED: "You could derive a hash of the parameters present and assume that any request from the same caller with identical parameters is a duplicate. On the surface, this seems to simplify both the customer experience and the service implementation." "However, we have found that this approach doesn't work in all cases." "It's possible that the caller actually wants two identical EC2 instances." What they do instead: "At Amazon, our preferred approach is to incorporate a unique caller-provided client request identifier into our API contract." The token is an audit tool too: "It also has the benefit of making that intent readily auditable because the unique identifier is present in logs like AWS CloudTrail. Furthermore, by labeling the created resource with the unique client request identifier, customers are able to identify resources created by any given request." Same token, different content: "we return a validation error indicating a parameter mismatch between idempotent requests. To support this deep validation, we also store the parameters used to make the initial request along with the client request identifier." Late retries after the thing was deleted: "we hold with the principle of least astonishment", so the original answer is returned. The closing caveat: "there is cost and complexity inherent in building services to meet the contract this article describes, and that complexity is not right for all solutions." ([Making retries safe with idempotent APIs](https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/))


---
**skeptics L177-178 · BODY · DISAGREES C1,C3**
*3. Big-tech operational lessons › 3.2 Meta: TAO, FlightTracker, RAMP-TAO*

**What they built and chose.** TAO serves Facebook's social graph: objects and typed associations, read a billion times a second. The id choice is the opposite of opaque. REPORTED: "Each object id contains an embedded shard id that identifies its hosting shard. Objects are bound to a shard for their entire lifetime. An association is stored on the shard of its id1, so that every association query can be served from a single server. Two ids are unlikely to map to the same server unless they were explicitly colocated at creation time." Associations carry a time field because of how they are read: "Each association has a 32-bit time field, which plays a central role in queries". Writes that touch two shards are not atomic. REPORTED: "TAO does not provide atomicity between the two updates. If a failure occurs the forward may exist without an inverse; these hanging associations are scheduled for repair by an asynchronous job." ([TAO, USENIX ATC 2013](https://www.usenix.org/system/files/conference/atc13/atc13-bronson.pdf))


---
**skeptics L179-180 · BODY · DISAGREES C1,C3**

**Why.** REPORTED in the sentence itself: "so that every association query can be served from a single server." INSTITUTIONAL: they accepted that an object can never move, and that placement must be decided at the moment of creation, in return for one-hop reads. I infer the trade. The paper states the mechanism and its purpose, not the regret.


---
**skeptics L188-188 · BODY · DISAGREES C1,C3**

- (2) INFERRED: an id that carries a shard fixes a fact's home for life, and forces the question of what lives together to be answered at creation. Sid's opaque id avoids both. It pays with a lookup on every route. Meta judged that lookup too costly at their read rate.

---
**skeptics L198-199 · BODY · DISAGREES C1**
*3. Big-tech operational lessons › 3.3 Microsoft Orleans (Sergey Bykov, Phil Bernstein, and oth*

**What they built and chose.** Virtual actors. REPORTED: "Perpetual existence: actors are purely logical entities that always exist, virtually. An actor cannot be explicitly created or destroyed and its virtual existence is unaffected by the failure of a server that executes it. Since actors always exist, they are always addressable." Identity is a type plus a key the user picks. REPORTED from the docs: "Grains in Orleans each have a single, unique, user-defined identifier consisting of two parts: The grain type name, uniquely identifying the grain class. The grain key, uniquely identifying a logical instance of that grain class." ([Orleans tech report, 2014](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/Orleans-MSR-TR-2014-41.pdf); [Grain identity docs](https://learn.microsoft.com/en-us/dotnet/orleans/grains/grain-identity))


---
**skeptics L205-205 · BODY · DISAGREES C1**

- (2) INFERRED: "an opaque id, for life, no type slot" is close to perpetual existence. There is no create event and no destroy event. An entity exists because something points at it. Orleans differs in one way: the type is part of the identity. Orleans never had to say what happens when a thing's type changes, because it cannot. Sid's typeless id leaves that open on purpose.

---
**skeptics L213-214 · BODY · DISAGREES C1**
*3. Big-tech operational lessons › 3.4 Google: Hyrum's Law, and two instances of the fix*

**The law, seen in an id.** Twitter, 2010, REPORTED: ids must be "roughly sortable" because "this is how we and most Twitter clients sort tweets." Here the observable property (order by id) became the interface, and the vendor said so. The same post on width: "We've been through the painful process of growing the number of bits used to store tweet ids before. It's unsurprisingly hard to do when you have over 100,000 different codebases involved."


---
**skeptics L217-217 · BODY · NEW-REASON C1**

2. Declare opacity in the contract. RFC 9562 section 6.12, quoted above. INFERRED: by Hyrum's Law this is the weakest fix. A UUIDv7's timestamp can be seen, so someone will sort by it.

---
**skeptics L221-221 · BODY · NEW-REASON C1**

- (2) INFERRED: if an id shows when or where it was made, tools will sort by it, bucket by it, and guess age from it. After that the id format cannot change. With facts that are never rewritten and tools that live in the store, the dependence is permanent on both sides. An id that shows nothing is the only kind that stays free.

---
**skeptics L233-233 · BODY · DISAGREES C1**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (2) ids | UUIDv4, UUIDv7, Snowflake; time leaks; shard embedded at the biggest scale | Hyrum: whatever shows gets depended on (REPORTED). Twitter: clients sort by id; widening ids is "painful" (REPORTED). TAO: shard in the id means bound "for their entire lifetime" (REPORTED) |

---
**skeptics L245-245 · BODY · CARRIED C1**

| Sid's question | Ordinary default | Skeptics and big tech |
| (1) version and id | Integer version or ETag; random event id; hash only for blobs | Amazon: a parameter hash "doesn't work in all cases"; prefer a caller-minted id; store parameters with it for "deep validation" (REPORTED) |

---
**skeptics L259-259 · BODY · DISAGREES C1,C3**
*5. Voices that matter most, who was dropped, who is missing*

2. **The FlightTracker and TAO authors at Meta.** They lived ten years with ids that embed a shard, with indexes that lag, and with weak guarantees added late. They wrote down what it cost: permanent bugs hiding among transient ones, historical data that breaks later rules, and how hard "who is this request for?" became.

---
**skeptics L303-303 · BODY · NEW-REASON C1**
*8. What each source implies for a second store*

- **Twitter and RFC 9562, REPORTED basis.** Ids minted without coordination are what let a second minting authority exist at all. 128 random bits need no worker registry. Snowflake-style ids need one: "worker numbers are chosen at startup via zookeeper".

---
**skeptics L304-304 · BODY · NEW-REASON C1,C3**

- **TAO, INFERRED.** An id that names its home cannot move to another store without being renamed. An opaque id can.

---
**sync L143-146 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

- A write names itself at first acceptance, not at commit: "Each Bayou Write
  also contains a globally unique WriteID assigned by the server that first
  accepted the Write." (Terry et al., "Managing Update Conflicts in Bayou",
  SOSP 1995, https://people.eecs.berkeley.edu/~brewer/cs262b/update-conflicts.pdf)

---
**sync L183-189 · BODY · DISAGREES C1**

- A server's birth is a record, and its id is made from its maker's: "A Bayou
  server … creates itself by sending a [creation write] to another server…
  The creation write is handled … just as a write from a client." The new id
  is the pair of the creator's stamp and the creator's id, and they note the
  cost: "if replicas are created linearly, one from the next, server
  identifiers will be increasingly longer". (SOSP 1997)


---
**sync L216-219 · BODY · DISAGREES C1,C7**

- (1) A write gets its own id where it is first accepted, and gets the
  orderer's number later. The number slot is there from the start, filled with
  infinity. REPORTED. For Sid: an offer has an id before the gate; the version
  is a second, later name. INFERRED.

---
**sync L297-302 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.2 Convex (Sujay Jayakar, James Cowling, Jamie Turner; 2021*

- The id: "Each ID contains a varint encoded table number, 14 bytes of
  randomness, a 2 byte timestamp, and a two byte version number and checksum.
  The randomness goes before the timestamp so writes are scattered in ID space,
  utilizing all shards' write throughput under range partitioning. The creation
  timestamp has day granularity and lets us efficiently ban ID reuse without
  having to keep deleted IDs around forever."

---
**sync L315-320 · BODY · NEW-CASE C1**

- December 2025, reversing "the id knows its own kind": "Before we can support
  custom IDs, we need to stop relying on our special current ID encoding… It is
  a special format that currently encodes the table it belongs to". The driver:
  custom ids are "useful when migrating data from other databases or
  optimistically generating IDs on clients, for apps that work offline".
  (Convex, "Why ctx.db is changing", 2025-12-10, https://news.convex.dev/db-table-name/)

---
**sync L329-336 · BODY · DISAGREES C1**

- Optimism costs double work and shows as flicker: "Developers implement each
  logical mutation… twice: once for the authoritative server change and once
  for an optimistic update to the local store." (Convex, "An Object Sync Engine
  for Local-first Apps", https://stack.convex.dev/object-sync-engine) The
  optimistic-updates docs create "a temporary Id" that "will also be rolled
  back and replaced with the true ID once the server assigns it", and invite
  the reader to insert a mistake: "You should see a flicker".
  (https://docs.convex.dev/client/react/optimistic-updates)

---
**sync L353-356 · BODY · DISAGREES C1**

- (2) The id deliberately leaks a little: the day, the table number (hidden
  name), and *the version of the id format*. They now regret the table part.
  REPORTED. The format-version field inside the id is the part worth copying.
  INFERRED.

---
**sync L357-358 · BODY · NEW-CASE C1**

- (1) Client-made ids are wanted for offline work and for import, and the
  server-only id blocked both for years. REPORTED.

---
**sync L365-367 · BODY · DISAGREES C1**

- (9) "ban ID reuse without having to keep deleted IDs around forever":
  deletion is real, and the id's day-stamp exists to make that safe. REPORTED.


---
**sync L491-496 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

- Ids are made by clients: "This can be easily accomplished by assigning every
  client a unique client ID and including that client ID as part of
  newly-created object IDs. That way no two clients will ever generate the same
  object ID. Note that we can't solve this by having the server assign IDs to
  newly-created objects because object creation needs to be able to work
  offline."

---
**sync L551-554 · BODY · DISAGREES C1**

- (2) Ids are made by the client from a server-issued client id plus a local
  part, because creation cannot wait for the server. REPORTED. For Sid the
  same reason holds for a different cause: not offline work, but anything that
  must point at an offer before the gate answers. INFERRED.

---
**sync L664-668 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)*

- The offer's identity is a per-client counter: the push carries "the pending
  last mutation id for the client that is pushing. This is the high water mark".
  The position is opaque: "The cookie is a value opaque to the client". A poke
  carries no data: "All the poke does is tell the client that it should pull
  again soon." (same)

---
**sync L701-703 · BODY · NEW-CASE C1,X2**

- Mutation ids moved from per client to per client *group*. The stated cause is
  schema change: "For brief periods during schema migrations, two client groups
  can coexist in the same browser profile." ("How Replicache Works")

---
**sync L717-720 · BODY · DISAGREES C1**

- (1) An offer has its own identity: client group id plus a counter. It exists
  to make retries safe and to tell the client which of its pending changes the
  server has seen. REPORTED. For Sid: this is the id a refusal can point at.
  INFERRED.

---
**sync L755-757 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.7 tldraw sync (Steve Ruiz)*

- Ids carry their type: "The `id` is a branded string that includes the type
  prefix (`shape:`, `page:`, `binding:`). This prevents accidentally mixing up
  IDs from different record types." (tldraw docs, "Store", https://tldraw.dev/sdk-features/store)

---
**sync L791-798 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

### 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion Henry, Alex Good, Andrew Jeffery; 2017–now)

**1. What they built and chose.** A CRDT library for JSON-like documents that
keeps all history. Three naming schemes live side by side. Operations are named
(counter, actor). Changes are named by the SHA-256 of their bytes and list the
hashes of the changes they depend on. Documents are named by a random UUID. A
version of a document is its set of head hashes.


---
**sync L801-807 · BODY · DISAGREES C1,C7**

- "A change is identified by its change hash which is the SHA256 hash of the
  binary representation of the change." "An operation ID is a pair of (actor
  ID, counter), where the counter is a unique always-incrementing value per
  actor." "The object ID is the operation ID of the operation that created the
  object." And the root: "Each document has a root `map` which is identified by
  the object ID with a `null` actor id and `null` counter." (Good and Jeffery,
  "Automerge Binary Document Format", https://automerge.org/automerge-binary-format-spec/)

---
**sync L819-828 · BODY · DISAGREES C1**

- Why hashes, when peers are strangers: a bad peer "may generate several
  distinct updates with the same sequence number, and send them to different
  nodes (this failure mode is known as equivocation)". Counter-and-actor ids
  "only works with trusted nodes". The cost is stated too: "The downside of
  using hashes as IDs is that they require more space than other schemes."
  Dependencies are a frontier, not a closure, and a record is judged only from
  its own recorded past: "we can safely decide whether update u is valid by
  basing the decision only on the updates in before(u)". (Kleppmann, "Making
  CRDTs Byzantine Fault Tolerant", PaPoC 2022,
  https://martin.kleppmann.com/papers/bft-crdt-papoc22.pdf)

---
**sync L829-836 · BODY · DISAGREES C1**

- Why they keep both: "I think it was right to ship the first version of the
  binary encoding with both hash chaining and actor IDs, even though they are
  essentially two different ways of expressing the same thing." The measured
  cost of hashes: "For the paper editing trace with 260k changes, the hashes
  alone add up to more than 8MB". And deletion is where hash ids hurt: "with
  hashes, the size of this deletion change would be >32 bytes times the number
  of deleted chars." (Kleppmann in GitHub Discussion #546, "Automerge without
  actor IDs", 2023, https://github.com/automerge/automerge/discussions/546)

---
**sync L848-853 · BODY · DISAGREES C1**

- *From vector clocks to hashes (0.x to 1.0).* "Dependencies between changes
  are now expressed by referencing the hashes of dependencies, rather than
  their actorId and sequence number". `getChangesForActor()` was removed "since
  it does not fit with a hash chaining approach." Declared breaking; an upgrade
  tool was promised and I found no evidence it shipped. (automerge-classic
  CHANGELOG)

---
**sync L854-858 · BODY · NEW-CASE C1**

- *A writer cannot name its own record.* "the frontend does not know the hash
  of a change until it has done a round-trip through the backend. Omitting the
  hash of the local actor's most recent change allows the frontend to generate
  several changes in quick succession without waiting". (BINARY_FORMAT.md,
  automerge 1.0.1-preview.6, https://unpkg.com/automerge@1.0.1-preview.6/BINARY_FORMAT.md)

---
**sync L865-871 · BODY · CARRIED C1,C7**

- *Hash ids are fragile in practice.* Conrad Irwin: "Currently we use the hash
  of the serialized chunk to identify the change; this seems like a sensible
  approach, but it's fragile in practice." Kleppmann's answer: "Postel's law
  does not hold here. The parser should accept only what the encoder produces,
  and nothing more." (#588) Also: "DEFLATE compression is not guaranteed to be
  deterministic", and saved files are not byte-identical across replicas.
  (issue #595)

---
**sync L910-915 · BODY · DISAGREES C1**

- (1) Name operations cheaply (counter + actor) and name batches by content
  hash *when peers cannot be trusted*. REPORTED. With one trusted gate,
  equivocation cannot happen inside the store, so the main reason for hash
  names is gone; it returns between stores and for outside audit. INFERRED.
  Hash names cost space, cost on deletion, and cannot be known by the writer
  until the bytes are final. REPORTED.

---
**sync L1043-1045 · BODY · DISAGREES C1,X4**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- What holds before permissions exist: "by default you can write into any
  Automerge document that you know the document ID for. This style is sometimes
  called 'Swiss number' or 'Rumpelstiltskin' security."

---
**sync L1090-1092 · BODY · NEW-REASON C1**

- On content-derived names for things that change: "this approach is not
  suitable for collaboration software, where the shared data changes
  frequently, and the hash would change on every modification."

---
**sync L1096-1100 · BODY · DISAGREES C1,E4**

- No gate, and they miss it: "The PushPin implementation currently has no
  mechanism for determining which users' writes to a document should be
  accepted." The URL "acts as a bearer token", and "there is no real way of
  revoking a user's access".


---
**sync L1109-1110 · BODY · DISAGREES C1,X4**

- "Access permissions for documents beyond secret URLs remain an open research
  question."

---
**sync L1131-1132 · BODY · DISAGREES C1,X4**

- (16) Before permissions exist, knowing the id is the permission. REPORTED.
  So ids must be unguessable and must never be the only guard. INFERRED.

---
**sync L1154-1159 · BODY · DISAGREES C1,C8**
*2. Section one: sync and multiplayer › 2.10 Yjs (Kevin Jahns; 2015–now)*

### 2.10 Yjs (Kevin Jahns; 2015–now)

**1. What he built and chose.** The most deployed CRDT library. An item's id is
(client id, clock). Client ids are random integers made per session. Deletes
carry no metadata. Deleted content is garbage-collected by default.


---
**sync L1162-1164 · BODY · DISAGREES C1**

- The id width is an artefact: "This is a random 53-bit integer (53 bits
  because that fits in the javascript safe integer range". (Yjs INTERNALS.md,
  https://github.com/yjs/yjs/blob/main/INTERNALS.md)

---
**sync L1165-1169 · BODY · DISAGREES C1**

- Less entropy is enough when only clients, not items, are random: "Database
  systems often like to use UUIDv4 which allow 122 bits of entropy. But keep in
  mind that they assign a random GUID to every single item. Yjs only generates
  random ids for clients, so we don't need as much entropy." (Jahns,
  discuss.yjs.dev thread 312, 2020, read via web.archive.org)

---
**sync L1178-1180 · BODY · NEW-CASE C1**

- The id width was held back for compatibility: "we currently only generate 32
  random bits. This has historical reasons because I want to keep compatibility
  to the 32 bit approach." (thread 312) The current source generates 53 bits.

---
**sync L1181-1185 · BODY · NEW-CASE C1**

- A clash can only be noticed afterwards. The FAQ: "When two Y.Doc instances
  with the same ClientID exist, the document might get permanently corrupted
  without a way to recover." The source now rotates the id at runtime and
  prints "Changed the client-id because another client seems to be using it."
  (docs.yjs.dev/api/faq; `src/utils/Transaction.js`)

---
**sync L1242-1250 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.11 Seph Gentle (ShareDB, Google Wave, diamond-types, Eg-wa*

- Shared ids and local numbers are different things: "Semantically we use
  attributed IDs: `(agent ID, seq)`. But internally all operations are locally
  linearized in time… They're also local only - other peers will end up with
  different id-to-order mappings." And three ways to name a moment: a full
  vector clock "can be interpreted by any peer at any time"; a frontier set is
  "much smaller" but "if a peer is missing the latest changes, the frontier set
  will be incomprehensible"; the local next-order number is "a local only
  number". (diamond-types INTERNALS.md,
  https://github.com/josephg/diamond-types/blob/master/INTERNALS.md)

---
**sync L1311-1316 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.12 Matthew Weidner (CRDT survey, Fugue, Collabs, list-posi*

- On ids: replica ids "are usually random instead of 'the highest replica ID so
  far plus 1'", because replicas are created concurrently. Sizes he lists: "a
  UUID v4 is 122 random bits, a Collabs replicaID is 60 random bits (10 base64
  chars), and a Yjs clientID is 32 random bits". (CRDT Survey, Part 3, 2023,
  https://mattweidner.com/2023/09/26/crdt-survey-3.html; the Yjs figure was
  true then)

---
**sync L1342-1343 · BODY · NEW-REASON C1**

- (2) Random, because creation is concurrent. Session-scoped, not device-scoped.
  REPORTED.

---
**sync L1375-1379 · BODY · CARRIED C1**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- A name for the slot, a second name for the version: "An AT URI pointing to a
  specific record in a repository is not a strong reference, in that it is not
  content-addressed." "When a strong reference to another record is required,
  best practice is to use a CID hash in addition to the AT URI."
  (https://atproto.com/specs/at-uri-scheme)

---
**sync L1380-1385 · BODY · DISAGREES C1**

- Time-ordered keys are for the tree, not for meaning: the TID scheme "was
  intentionally selected to provide chronological sorting of MST keys… Appends
  are more efficient than random insertions". And the warning: "these keys can
  be specified by the end user and could have any value, so they should not be
  trusted." "because atproto is an open network, uniqueness of TIDs can not be
  guaranteed." (specs, Repository; Record Key; TID)

---
**sync L1445-1448 · BODY · CARRIED C1**

- *Content-addressed references make migration recursive.* On removing a legacy
  blob format: "If we only update the records, then 'strong' references (by
  CID) will throw warnings (eventually), but that is way easier than recursive
  migrations". (Newbold, "Protocol Tech Debt (2024)", Discussion #2128)

---
**sync L1492-1495 · BODY · DISAGREES C1**

- (2) Time-ordered ids buy storage locality and nothing else; never trust the
  time in them; expect adversaries to choose them. INSTITUTIONAL. An id that
  hashes the first record binds you to that record's format forever. REPORTED
  as a regret.

---
**sync L1540-1545 · BODY · DISAGREES C1,E3**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

### 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020–now)

**1. What they built and chose.** Signed events sent to dumb relays. The event
id is the hash of a canonical serialisation. Kinds are integers. Time is the
author's. Deletion is a request.


---
**sync L1548-1552 · BODY · DISAGREES C1**

- The id: "To obtain the `event.id`, we `sha256` the serialized event", a fixed
  array of pubkey, created_at, kind, tags, content. The canonical form is
  written into the spec: "To prevent implementation differences from creating a
  different event ID for the same event, the following rules MUST be followed
  while serializing".

---
**sync L1601-1602 · BODY · DISAGREES C1**

- (1) Content-hash ids work if the canonical form is in the spec from day one.
  REPORTED. They have no server number at all. REPORTED.

---
**sync L1628-1633 · BODY · DISAGREES C1**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

### 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, André Staltz, cryptix, Aljoscha Meyer, Sam Gwilym, the p2panda team; 2014–now)

**1. What SSB chose.** One append-only log per key pair. Every message names the
previous message's hash and its sequence number. The message id is the hash of
the whole message, content and signature included.


---
**sync L1636-1636 · BODY · DISAGREES C1**

- "A message ID is a hash of the message including signature."

---
**sync L1656-1660 · BODY · DISAGREES C1,C5**

- *The id stopped covering the content.* Bamboo's entry holds "the hash of the
  actual _payload_" and its size, so the payload can go. Gabby Grove: "the feed
  entry only references content by hash to enable content deletion without
  breaking verification of the feed". PPPPP: "**Msg ID** = `hash(msg.metadata)`".
  All four successors made this same change.

---
**sync L1672-1674 · BODY · DISAGREES C1**

- *A fixed first record.* PPPPP: "**Moot** = the root of a feed, a msg that is
  deterministically predictable and empty, so to allow others to pre-know its
  msg ID".

---
**sync L1708-1710 · BODY · DISAGREES C1,C5**

- (1) Do not let the id cover the value. Hash the envelope, and inside it the
  hash of the value. REPORTED, by four independent teams. Put the canonical
  form in the spec, never in a runtime. REPORTED.

---
**sync L1721-1722 · BODY · DISAGREES C1,C4**

- (17) The first record should be predictable and empty so that everyone can
  know its id in advance. REPORTED.

---
**sync L1755-1762 · BODY · DISAGREES C1,C5**
*2. Section one: sync and multiplayer › 2.16 Matrix (added; checked by me against spec.matrix.org on*

- *The id is a hash that survives redaction.* "The reference hash of an event
  covers the essential fields of an event, including content hashes… The event
  is put through the redaction algorithm. The signatures and unsigned
  properties are removed from the event, if present. The event is converted
  into Canonical JSON. A sha256 hash is calculated". A separate "content hash
  … covers the complete event including the unredacted contents." (Server-Server
  API) This is the two-hash design of 2.15, in production since room version 3
  (2019).

---
**sync L1763-1769 · BODY · DISAGREES C1**

- *They moved from assigned ids to hash ids, and said why.* With a separate id
  field, "servers receive multiple events with the same ID in either the same
  or different rooms where the server cannot easily keep track of which event
  it should be using." (Room version 3) The proposal adds that clients "should
  already be treating event IDs as opaque strings", and that hash agility now
  rides on the container's version: "now that room versions exist, changing
  hash functions can be achieved by bumping the room version." (MSC1659)

---
**sync L1775-1782 · BODY · DISAGREES C1**

For Sid: (9) a worked, deployed shape for deleting a value and keeping the
fact; (1) a content hash can be an id *if* it is defined over the redacted form
and the canonical encoding is in the spec, and note their reason for hash ids
(many servers assigning clashing ids) does not exist under one gate; (7) a
precedent for writing "what the gate checked" onto the record; (17) the first
record is the one whose authorisation list is empty. All INFERRED applications
of INSTITUTIONAL facts.


---
**sync L1789-1795 · BODY · DISAGREES C1**
*3. Section two: versioning › 3.1 Git (Linus Torvalds, Junio Hamano, Jeff King, brian m. c*

**1. What they built and chose.** History named by content. "It uses the SHA-1
hash function to name content. For example, files, directories, and revisions
are referred to by hash values unlike in other traditional version control
systems where files or versions are referred to via sequential numbers." (Git,
"hash-function-transition", git.git, first dated 2017-03-03,
https://git-scm.com/docs/hash-function-transition)


---
**sync L1798-1805 · BODY · DISAGREES C1**

- The hash is a name and a checksum first: "There's a big difference between
  using a cryptographic hash for things like security signing, and using one
  for generating a 'content identifier' for a content-addressable system like
  git." "Our trust is in people". "Think of it like 'parity on steroids'."
  With a concession: "in git we also end up using the SHA1 when we use 'real'
  cryptography for signing the resulting trees, so the hash does end up being
  part of a certain chain of trust." (Torvalds, public post, 2017-02-25,
  archived at web.archive.org/web/2017/https://plus.google.com/+LinusTorvalds/posts/7tp2gYWQugL)

---
**sync L1839-1847 · BODY · CARRIED C1**

- *What the plan could not carry over.* Sidecar data keyed by the old id: "The
  `git notes` tool annotates objects using their SHA-1 name as key. This design
  does not describe a way to migrate notes trees to use SHA-256 names." Ids in
  free text: after a rewrite, "if some of your commit messages refer to prior
  commits by (abbreviated) sha1, after the rewrite those messages will now
  refer to commits that are no longer part of the history." (git-filter-repo
  README) Signatures that do not say which scheme they mean: "The signed
  payload for signed commits and tags does not explicitly name the hash used to
  identify objects… Fortunately SHA-256 and SHA-1 have different lengths."

---
**sync L1848-1851 · BODY · CARRIED C1,C7**

- *Two non-goals they chose.* "Intermixing objects using multiple hash
  functions in a single repository", and "Taking the opportunity to fix other
  bugs in Git's formats and protocols." Old brokenness is kept on purpose so
  conversion can round-trip.

---
**sync L1876-1877 · BODY · NEW-REASON C1,E1**

- (7), (6) Later knowledge goes beside the record, keyed by the record's id.
  That makes id stability load-bearing. REPORTED.

---
**sync L1882-1884 · BODY · CARRIED C1**

- (13) Never mix two id schemes in one store (Git). Fossil says the opposite
  (3.3). REPORTED.


---
**sync L1898-1902 · BODY · DISAGREES C1**
*3. Section two: versioning › 3.2 Jujutsu, and Mercurial's changeset evolution (Martin von*

- The stable name is random: "A change ID is a unique identifier for a change.
  They are typically 16 bytes long and are often randomly generated." It is
  shown in its own alphabet (letters k–z) so it cannot be mistaken for a
  content hash. "Rewriting a commit results in a new commit, and thus a new
  commit ID, but the change ID generally remains the same." (glossary)

---
**sync L1927-1930 · BODY · NEW-CASE C1**

- When part of a record sits outside the hashed content, two different records
  clash: "Because we use the Git Object ID as commit ID, two commits that
  differ only in their change ID, for example, will get the same commit ID, so
  we error out when trying to write the second one of them." (jj architecture)

---
**sync L1971-1974 · BODY · DISAGREES C1**
*3. Section two: versioning › 3.3 Fossil (D. Richard Hipp; 2006–now)*

- No sequence anywhere: "The global state of a fossil repository is an
  unordered set of artifacts." Each is "named by a hash of its content. No
  prefixes, suffixes, or other information is added to an artifact before the
  hash is computed." (same)

---
**sync L2009-2015 · BODY · DISAGREES C1**

- *The hash change that took days.* "Historical check-ins will keep their same
  historical SHA1 names. New check-ins will get more secure SHA3-256 hash
  names." "Fossil (version 2.0 and later) allows the SHA1 and SHA3 hashes to be
  mixed within the same repository… There is no need to 'convert' a
  repository". The stated motive: "it is a public relations problem. So the
  decision was made to migrate Fossil away from SHA1." Fossil 2.0 shipped eight
  days after SHAttered. (hashpolicy)

---
**sync L2020-2034 · BODY · DISAGREES C1**

**4. Which questions.** (0) Never rewritten is a property of the format and the
tools; the bytes can always be edited by whoever holds the disk. The promise is
to make integrity hard to break and every correction visible. REPORTED. (9)
Delete rarely, locally, by a kept list that also blocks re-entry; never let a
delete order spread by itself. REPORTED. (6), (7) Corrections are new records
that name their target; the wrong original stays readable. REPORTED. (1) Old
names keep their old scheme forever; new names may use a new scheme; one store
holds both. REPORTED. For Sid: if ids are random and scheme-free this whole
problem disappears; if a digest is carried, tag it with its algorithm and allow
more than one. INFERRED. (8), (16) Users and permissions are deliberately not
part of the enduring record. REPORTED. Sid makes the opposite choice (policies
are facts); Hipp's reason is that the list of users changes and should be
erasable. INFERRED. (13) Simple, self-describing text meant to be read without
the program. REPORTED.


---
**sync L2046-2051 · BODY · DISAGREES C1**
*3. Section two: versioning › 3.4 Dolt, and Noms before it (Tim Sehn, Aaron Son, Andy Arth*

### 3.4 Dolt, and Noms before it (Tim Sehn, Aaron Son, Andy Arthur, Dhruv Sringari, Jason Fulghum; Noms by Aaron Boodman and Rafael Weinstein)

**1. What they built and chose.** A SQL database with Git's model. Rows live in
content-addressed trees whose shape does not depend on edit order. Commits are
named by hash. Branch, merge, diff and `AS OF` work on tables.


---
**sync L2064-2079 · BODY · CARRIED C1,P0**

- *The format migration (2022).* They inherited Noms and its choices: "Building
  on top of Noms allowed us to get a head start on early versions of Dolt, but
  it also meant that we inherited its design decisions." (Arthur, "Migrating
  Dolt's Binary Format", 2022) Moving off it changed every name: "the migration
  changes all of the commit hashes because the storage format is different.
  Commit hashes are the output of a cryptographic hash function, so if a single
  bit changes, the commit hash will change." "During the job, we build a
  mapping of old commit hashes to new commit hashes." Forks were cut off; open
  pull requests were deleted; clones had to migrate too; a 5.7 GB database took
  about a hundred minutes. (Sringari, "Migrate your Dolt database to the new
  format on DoltHub", 2022-11-01) The method was a rebase of all history, with
  a check: "At a logical level, the data in each table is unchanged", and each
  table is "validated to ensure that they contain the same rows pre- and
  post-migration." Their own lesson: "It was pretty painful to build these
  mappings… A possible improvement here is to automatically encode this mapping
  during the migration." (Sringari, 2022-09-19)

---
**sync L2091-2095 · BODY · NEW-CASE C1**

- *Counters do not survive copies.* Shared auto-increment across branches was
  added in 2022, "But, Dolt is a decentralized version control system so each
  individual instance of Dolt knows nothing of its clones." The advice became:
  "UUID keys produce conflict-free merges on branches and clones." (Sehn,
  "AUTO_INCREMENT vs UUID Primary Keys", 2023)

---
**sync L2114-2118 · BODY · DISAGREES C1**

**5–6.** Dolt's rows are mutable, its history is for audit and merge, and its
users run their own copies. The resemblance is a fact-like store with names
derived from content and a single writer. They disagree with Git's partial
clone design, and with their own inheritance from Noms.


---
**sync L2121-2131 · BODY · DISAGREES C1**
*3. Section two: versioning › 3.5 Irmin (Thomas Gazagnaire, Anil Madhavapeddy, the Tarides*

**1–2. What they built and why.** A Git-like store as an OCaml library:
content-addressed values, commits with parents, branches as the only mutable
part, and merge functions supplied by the user. "keys are deterministically
computed from the values", and "The second backend store is the tag store: this
is the only mutable part of the system." The founding assumption on growth: "As
the store is append-only, there is no remove function. The store is expected to
grow forever, but garbage-collection and compression techniques can be used to
manage its growth. This is not an issue as commodity storage steadily becomes
more and more inexpensive." (Farinier, Gazagnaire, Madhavapeddy, "Mergeable
persistent data structures", JFLA 2015, https://gazagnaire.org/pub/FGM15.pdf)


---
**sync L2139-2143 · BODY · CARRIED C1**

- *Hash addresses were too slow.* "content-addressing bottlenecks transaction
  throughput… each read requires consulting the index, and each write requires
  adding a new entry to it." So they added "object addresses that are not
  hashes", after which "the index can be shrunk by a factor of 360 (from 21G to
  59MB)". (Tarides, "Lightning Fast with Irmin", 2022-04-26)

---
**sync L2174-2177 · BODY · DISAGREES C1**
*3. Section two: versioning › 3.6 Pijul (Pierre-Étienne Meunier, Florent Becker; 2015–now)*

**1. What they built and chose.** Version control on a theory of patches. A
change is named by the hash of its content, which includes its dependencies.
Independent changes commute. A repository state is an unordered set of changes.


---
**sync L2180-2185 · BODY · DISAGREES C1**

- A thing is named by the event that made it: "vertices are uniquely
  identified, by the hash of the change that introduced them, along with a
  position in that change. This means that two lines of text with the same
  content, introduced by different changes, will be different. It also means
  that a line keeps its identity, even if the change is applied in a totally
  different context." (manual, Theory, https://pijul.org/manual/theory.html)

---
**sync L2226-2238 · BODY · DISAGREES C1,C5**

**4. Which questions.** (4) The direct answer in this camp: split what a record
*requires* from what it merely *knew of*, and they learned to do so after
shipping one undivided set. REPORTED. (2) Name a new thing by the record that
introduced it, plus a position. REPORTED. For Sid: an entity id could be "the
id of the offer that first mentioned it, plus an index", which makes clashes
impossible without randomness and leaks nothing by inspection. INFERRED.
(1), (9) Hash the header; put the hash of the contents in the header; let the
contents be absent. REPORTED. (8) The actor on the record is a key; the name
is a changeable mapping outside the hashed record. REPORTED. (10) A version is
a set, and its id must not depend on order. REPORTED. Under one gate this is
unnecessary inside the store and becomes relevant only between stores.
INFERRED. (17) The empty state has a fixed id. REPORTED.


---
**sync L2239-2244 · BODY · DISAGREES C1**

**5–6.** Pijul's unit is a text edit by a trusted author. Its value for Sid is
the clean split of envelope from contents and of "requires" from "knew". It
disagrees with Git on merge ("Git doesn't guarantee the associative change
property") and on identity (cherry-picking "losing their identity"), and with
Darcs on commuting conflicts.


---
**sync L2314-2316 · BODY · DISAGREES C1**
*4. Question by question › (2) Entity: how is an id made so two never clash? May it giv*

- How many bits: Jahns argued fewer suffice when only clients are random, kept
  32 bits for compatibility, moved to 53. A clash without an authority means a
  document "permanently corrupted without a way to recover". REPORTED.

---
**sync L2317-2319 · BODY · DISAGREES C1**

- A thing can be named by the event that made it, plus a position: Automerge
  object ids, Pijul vertices. REPORTED. Bayou's variant (creator's id plus
  stamp) grows with each generation. REPORTED.

---
**sync L2320-2323 · BODY · DISAGREES C1**

- Leaking on purpose: AT Protocol's time-ordered keys exist for tree locality
  and must "not be trusted"; adversaries can choose them. Convex leaks the day,
  a format version, and (now regretted) the table. tldraw puts the type in the
  id for safety. REPORTED.

---
**sync L2324-2327 · BODY · DISAGREES C1**

- Against leaking: PushPin keeps the type in the reference, "because the same
  document content may be rendered differently in different contexts". Keyhive:
  by default, knowing an id is permission to write. Linear's one counter shows
  every tenant the global write rate. REPORTED facts.

---
**sync L2328-2330 · BODY · NEW-REASON C1**

- jj prints its random ids in a different alphabet from its hashes so that one
  kind of id is never mistaken for another. REPORTED.


---
**sync L2331-2335 · BODY · DISAGREES C1**

**Where they split.** Locality in the id (AT Protocol puts time first so
inserts append; Convex puts randomness first so writes spread across shards)
versus opaque ids (Keyhive, PushPin, Weidner). Note that the two "locality"
teams want opposite layouts. That is the sign that it is a storage concern.


---
**sync L2352-2354 · BODY · CARRIED C1**

The tradeoff: opaque random ids cost storage locality and debuggability.
Rama can partition by hash of id; people can be given names (facts) to read.


---
**sync L2454-2458 · BODY · DISAGREES C1,C5**
*4. Question by question › (9) Value: never removed, so how is one deleted, backups inc*

- Separate the value from the envelope. Matrix redaction (in production since
  2019) strips everything except a fixed list of envelope keys, and the event
  id is a hash of that stripped form, so it survives. Bamboo, Gabby Grove,
  PPPPP and Pijul each hash the envelope and put only the *hash of the
  contents* inside it. REPORTED and INSTITUTIONAL.

---
**sync L2468-2470 · BODY · DISAGREES C1**

- The id can outlive the value: Weidner. Convex stamps the day into ids so that
  reuse can be banned "without having to keep deleted IDs around forever".
  REPORTED.

---
**sync L2571-2581 · BODY · DISAGREES C1,X4**
*4. Question by question › (16) Layer: who sees a fact before any permissions exist?*

**What the camp says.** Without a server, knowing the id is the permission
(Keyhive, PushPin), and better than that is "an open research question" (the
local-first essay). REPORTED. AT Protocol began public and is adding private
data four years in. REPORTED. Things an enforcer needs must be visible to it:
blocks are public "because every protocol-conforming App View needs to know who
is blocking who". INSTITUTIONAL. A single global version makes per-reader
permission hard; per-row versions make it natural: Replicache. REPORTED.
Upwelling's drafts are private to one writer or open to all. REPORTED. tldraw
gave comments their own permissioned partition. REPORTED. Fossil keeps users
and permissions out of the enduring record. REPORTED.


---
**sync L2747-2753 · BODY · DISAGREES C1,C7**
*4. Question by question › (1) Version: is a fact pointed at by the gate's number, or b*

- Both exist in every system that lasted, and they name different things. jj:
  a random change id and a content commit id. AT Protocol: a slot name and a
  pinned version. did:plc: the server's number is "an annotation … and not an
  intrinsic property". diamond-types: a shared (agent, seq) id and a local
  order number that "other peers will end up with different" values for.
  Bayou: an id at first acceptance, a commit number later, with the slot
  holding infinity until then. REPORTED.

---
**sync L2757-2759 · BODY · DISAGREES C1**

- Content-derived names, the case for: they need no authority and they expose
  a lying peer (Kleppmann on equivocation); Matrix moved to them because many
  servers were assigning clashing ids. REPORTED and INSTITUTIONAL.

---
**sync L2760-2769 · BODY · CARRIED C1**

- Content-derived names, the costs, all REPORTED: the encoding under them is
  frozen (SSB's JavaScript printing; Tezos "cannot be changed"; did:plc
  "around forever"; Dolt "changes all of the commit hashes"). They cost space
  (8 MB per 260k changes) and cost most on deletion. The writer cannot know its
  own record's name until the bytes are final (Automerge's frontend). Anything
  left outside the hash makes two records clash (jj). Every outside reference
  breaks on migration, and sidecar data keyed by the old name is orphaned
  (Git notes). Changing the algorithm takes a decade unless mixing was designed
  in (Git against Fossil). Parsers must be strict forever ("Postel's law does
  not hold here").

---
**sync L2770-2772 · BODY · CARRIED C1**

- Irmin kept hashes only for what outsiders must verify and used plain pointers
  inside. REPORTED.


---
**sync L2840-2841 · BODY · NEW-REASON C1,E1**
*4. Question by question › (7) Beside each fact: is the gate's yes or no kept? Where? A*

- Later knowledge kept beside a record is keyed by the record's id, and is
  orphaned if the id changes: Git notes. REPORTED.

---
**sync L3013-3013 · BODY · CARRIED C1**
*5. The group › 5.1 The voices that matter most, who I dropped, who is missi*

- Git (never mix id schemes in one store) against Fossil (mix them forever).

---
**sync L3018-3019 · BODY · DISAGREES C1**

- tldraw (put the type in the id) against Convex in 2025 and PushPin (keep it
  out).

