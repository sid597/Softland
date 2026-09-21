# C5 erasure (9)(1): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L67-68 · BODY · CARRIED C5**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.1 What they built, and what they chose*

- **Forgetting was added later** (excision, May 2013), as an exception.


---
**datalog L163-164 · BODY · CARRIED C5,P0**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.3 What they later changed, regretted, or moved away from*

1. **Forgetting was added.** Launch position (2012): never forget. February 2013, Hickey writes "while the past may be forgotten, it is immutable." [H-IM]. May 2013, Halloway announces excision and frames it as forced from outside: "there is a fly in the ointment. In certain situations you may be forced to excise data… This may happen if you store data that must comply with privacy or IP laws, or you may have a regulatory requirement to keep records for seven years and then 'shred' them." [S-EXC13]. By 2018 Hickey's summary of the product is: "We keep everything that you ever said, unless you explicitly go out of your way to tell us to remove it." [H-IONS]


---
**datalog L165-166 · BODY · CARRIED C5**

2. **Forgetting turned out to be hard to build correctly, even for them.** Official release notice, 23 October 2025: "Datomic Release 1.0.7469 fixes a bug that prevented excisions from removing datoms from the as-of or history indexes. This release also includes a tool to detect incomplete excisions to the index and optionally re-apply them." [D-REL]. On the forum, one user reported in June 2021 that most of 6.3 million excised entities were still visible afterwards, and in February 2025 showed an excised entity still readable in the history database. The release notice does not name that thread, so linking the two is my reading, not Datomic's statement. How long the bug existed is not stated. The fix shipped with a *checker*.


---
**datalog L167-168 · BODY · CARRIED C5**

3. **Datomic Cloud shipped without excision** and still lacks it. Halloway, February 2018, to a user asking about GDPR: "Datomic Cloud does not currently support excision. I have created a feature request to track this." [S-FORUM]. No alternative was offered. The nearest official advice is Marshall Thompson's, 2019: "Excision is NOT intended, nor is it suitable for, 'clean up' of old/temporary data … If you need to store data that you plan to 'throw away' at a later point, I would recommend either using an alternative store for that data or using a separate logical database in Datomic that can be deleted at the database-level." [M-FORUM]


---
**datalog L188-188 · BODY · CARRIED C5,P0**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED. The forgetting leaves a permanent trace. "Note that the excise attributes themselves are protected from excision, so there is no way to 'erase your tracks'. Every excision creates a permanent record… Thus excision strikes a delicate balance between forgetting and remembering that you forgot." [D-EXC]

---
**datalog L189-189 · BODY · CARRIED C5**

- REPORTED. Forgetting sits outside the timeline. "Note that excision is a special operation that happens outside the timeline of Datomic history, removing data across all of history as if the data never happened. While the excise request itself is transactional, the excision operation is not transactional." [D-EXC]

---
**datalog L220-220 · BODY · CARRIED C5**

- REPORTED. Three tools, each with a stated limit. Retraction: logical only, history keeps the value. `:db/noHistory`: "The purpose of :db/noHistory is to conserve storage, not to make semantic guarantees about removing information." [D-SCHEMA]. Excision: real removal, at a price: "Excision puts a substantial burden on background indexing. Large excisions can trigger indexing jobs whose execution time is proportional to the size of the entire database, leading to back pressure and reduced write availability. Try to avoid excising more than a few thousand datoms at a time on a live system." [D-EXC]

---
**datalog L221-221 · BODY · CARRIED C5,P0**

- REPORTED. The docs argue against their own feature: "Legitimate motivations for removing data are *very rare*. A common desire is to be able to erase mistakes, but this is almost always a questionable idea… (Imagine a source control system that removed the history of code defects once they were fixed)." The one reason they grant: "Privacy laws might require you to excise data from ex-customers." [D-EXC]

---
**datalog L222-222 · BODY · CARRIED C5**

- REPORTED. The two features fight: "excision relies on full entity history to identify excision targets. :db/noHistory attributes do not guarantee full history nor precisely when history is removed. Therefore excision cannot guarantee full removal of all datoms with a :db/noHistory attribute from index and log." [D-SCHEMA]

---
**datalog L223-223 · BODY · CARRIED C5**

- Backups: not found. The excision page implies old backups still hold the data ("short of restoring a backup"). No page says it outright.

---
**datalog L224-224 · BODY · CARRIED C5**

- REPORTED (community, not official). What practitioners do instead: keep sensitive values outside. Waeselynck, 2018: "we avoid storing privacy-sensitive data in Datomic by storing it as values in a complementary, domain-agnostic Key/Value-store, while having the keys referenced from Datomic. To our surprise, we've found that this approach preserves almost all of the architectural advantages of Datomic." [VAL18]. Users also propose crypto-shredding: encrypt per subject, delete the key. No Datomic staff member endorses either in anything I found.

---
**datalog L225-226 · BODY · CARRIED C5**

- INFERRED. The lesson for a store that never rewrites: decide *before the first record* which kinds of value may never enter the log in the clear. After the first record it is too late for all earlier facts. The camp's own experience says erasure by rewriting is slow, breaks the guarantee that the past reads the same, and is easy to get subtly wrong across derived indexes. XTDB's answer is in section 4.


---
**datalog L352-352 · BODY · CARRIED C5**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

- **XTDB**, on erasure. Datomic: an exception bolted on. XTDB v1: designed in from the start.

---
**datalog L427-427 · BODY · CARRIED C5,C3**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.1 What they built, and what they chose*

- a **transaction log**: one Kafka topic with a single partition. It is immutable. It holds operations, but each document in an operation is replaced by its hash.

---
**datalog L428-428 · BODY · CARRIED C5,C1**

- a **document store**: documents keyed by the hash of their content. This part can forget.

---
**datalog L433-434 · BODY · CARRIED C5,P0**

**Version 2** (early access April 2023, launched 12 June 2025). A rewrite. Columnar files (Apache Arrow) in object storage, arranged as a log-structured merge tree. SQL first. An `ERASE` statement. The log is no longer the permanent record. Version 2.1 added several databases per cluster. Version 2.2 (September 2026) made indexing single-writer, with a second log of resolved transactions.


---
**datalog L437-440 · BODY · CARRIED C5,C1**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.2 Their reasons*

**Why hashes on the log: so the log never has to change.**

> "we hash the {:xt/id :luke, :name "Luke"} document… we store the document, keyed by its hash… we put the transaction operation on the transaction log, but replace the document with its hash… We do this for eviction - this distinction between documents and transactions means that, if Luke were to request that we irretrievably remove all his data (e.g. due to a GDPR/CCPA request), we only need to forget the content of the document and the transaction log can stay immutable." — James Henderson, 2020 [X-DOC]


---
**datalog L441-442 · BODY · CARRIED C5,C1**

> "We use two topics because whilst the transaction topic is immutable, messages in the document topic can be permanently erased, forming the basis of Crux's ground-up strategy to provide ease of content eviction for data privacy reasons, to align with compliance regimes such as GDPR. Using a separate topic for the content documents also allows for compaction to remove duplicates, as the message ID is a content hash of the document. From a Kafka perspective, the transaction topic uses a single Kafka partition." — JUXT, 2019 [X-INTRO]


---
**datalog L443-444 · BODY · CARRIED C5**

The original design card set the goal: "It should be a relatively cheap operation, and not require rebuilding the entire topics or indexes." — Råberg, 2018 [X-32]


---
**datalog L463-464 · BODY · CARRIED C5,C2**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.3 What they changed, and what bit them*

1. **Erasure broke the one guarantee.** "It is important to note that Evict is the only operation which will have effects on the results returned when querying against an earlier Transaction Time." [X-V1TX]. They never found a way round this. Nobody has.


---
**datalog L465-466 · BODY · CARRIED C5**

2. **Forgotten content broke replay.** "The indexer will pause consumption of transactions while waiting for all their documents to appear. When evicting documents they will have been compacted in Kafka, so replay will just block with the current behaviour." — Råberg, 2019 [X-184]. The fix was a tombstone: the log's hash must always resolve to *something*, either the document or a marker that it was evicted.


---
**datalog L479-480 · BODY · CARRIED C5**

9. **Erasure in version 2 is eventual, and happens by not copying.** `ERASE` "Irrevocably erases documents from a table, for all valid-time, for all system-time." But: "The ERASE is effective as soon as the transaction is committed - no longer accessible to an application - and under the hood the relevant data is guaranteed to be fully erased only once all background index processing has completed and the changes have been written to the remote object storage." [X-SQLQ]. The mechanism, from the implementer: "I am stopping copying compaction history when an `erase` event is encountered for a given `iid`." [X-4036]. Files are immutable. A compactor periodically writes new files from old ones. Erased data is simply not carried forward.


---
**datalog L485-485 · BODY · CARRIED C5,P0**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.4 Which questions they speak to*

- **(0) and (9): never rewritten, never lost, erasure.** REPORTED. Two designs, both lived with. *Version 1*: keep the log immutable for ever by never letting erasable content into it. The log holds a hash. The content sits where it can be forgotten. *Version 2*: nothing is physically permanent. Files are immutable, a compactor rewrites them, and forgetting is what the compactor leaves behind. INFERRED for Sid: version 1's split is the closest known fit to "the store never rewrites". It maps onto Sid's third kind of read, the anchor to something outside the store. But take the three lessons with it. A pointer into the forgettable place must always resolve, to the content or to a tombstone. Rebuilding anything from the log must survive missing content. And erasure still changes what past reads return, which every walk of based-on must be able to say out loud: *this read stood on something since erased*.

---
**datalog L567-567 · BODY · CARRIED C5,E1**
*6. The group: who matters most, who I dropped, who is missin*

2. **JUXT and XTDB.** The only team that took "immutable" and "must be able to forget" as joint requirements from the first day, and then wrote down what broke. Their version 1 is the closest existing design to "a store that never rewrites and can still forget". Their version 2 is the record of what that cost. They are also the precedent for Sid's gate: offers appended, judged afterwards, refusals kept.

---
**frontiers L94-94 · BODY · DISAGREES C5,P0**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(9) delete.** INFERRED from the formalism: a delete is a retraction at a time. When `since` passes that time, compaction cancels the +1 and −1 and the data is physically gone. Erasure and "as of, forever" cannot both hold for the same fact. One must give.

---
**frontiers L182-183 · BODY · CARRIED C5**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.2 The reasons, in their words*

**Overwrite is the non-monotone act.** REPORTED: "Bare assignment [10] is a nonmonotonic programming construct: outputs based on a prefix of assignments may have to be retracted when new assignments come in." Immutability is the monotone pattern: "an immutable variable is a simple monotonic pattern: it transitions from being undefined to its final value, and never goes back." On deletion: "Instead of explicitly allowing deletion (a non-monotonic construct), tombstones masked immutable values with corresponding immutable tombstone values. Taken together, a data item with tombstone monotonically transitions from undefined, to a defined value, and ultimately to tombstoned."


---
**frontiers L212-212 · BODY · CARRIED C5**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.4 Which questions they speak to*

- **(9) delete.** REPORTED: tombstones keep deletion monotone. INFERRED: they hide a value; they do not remove it, so they do not answer erasure.

---
**frontiers L285-286 · BODY · CARRIED C5**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.3 What changed afterwards*

- **Erasure by construction** (Poly 2019 position paper, then K9db, OSDI 2023). REPORTED: "Our key idea is for each user to have her own, structured shard of the storage backend". "a user shard never contains information related to other users, or derived information that combines multiple users' data." Everything combined is a view over shards. "if she demands erasure of her data, the service deletes the shard and streams revocation messages that remove derived data." ([Position: GDPR Compliance by Construction](https://people.csail.mit.edu/malte/pub/papers/2019-poly-gdpr.pdf)). K9db made it real: "Each user's µDB contains the data they own, and is encrypted with a user-specific key." And on backups: "encrypting data at rest and deleting encryption keys (referred to as 'crypto-shredding'), e.g., to make backups inaccessible, is widely considered a compliant approach". K9db also guards against "data without an owner being left behind in the database." ([K9db, OSDI 2023](https://www.usenix.org/system/files/osdi23-albab.pdf))


---
**frontiers L293-294 · BODY · NEW-REASON C5,E6**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.4 Which questions they speak to*

- **(9) who read the erased thing.** INFERRED: their "revocation messages that remove derived data" is Sid's walk over based-on. For running answers it is automatic. For stored offers that stood on the erased fact (a model's summary of it), based-on finds them, and a policy must say what happens to them.


---
**frontiers L359-360 · BODY · DISAGREES C5,P0**
*7. Question by question: what this camp says*

**(9) Delete.** Three mechanisms, all REPORTED. Tombstones (CALM): monotone, hides, does not erase. Retraction plus compaction (Materialize): erases for real once `since` passes, and gives up as-of reads for that stretch. Per-owner unit plus per-owner key (Schwarzkopf): erases by destroying a key, leaves the log untouched, covers backups. INFERRED: only the third fits never-rewrite, and it has to be in place at the first record.


---
**log L72-72 · BODY · CARRIED C5,P0**
*Voice by voice › 1. Pat Helland*

- On deletion: "In many environments, the immutable data may be deleted and the identifier will subsequently be mapped to an indication of "no present data," but it will never return data other than the original contents." (*Data on the Outside versus Data on the Inside*, CIDR 2005 and ACM Queue 2020.)

---
**log L86-86 · BODY · CARRIED C5,P0**

- (9) REPORTED: deletion is allowed; a different answer under the same id is not. "Records are deleted by adding tombstones."

---
**log L112-112 · BODY · CARRIED C5,P0**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- History costs storage, and trimming sells history: an object "can forgo the ability to roll back (or index into the log) before a checkpoint with a forget call, which allows Tango to trim the log and reclaim storage capacity." (Tango.)

---
**log L161-161 · BODY · CARRIED C5**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- Deletion: "Deleting a whole stream is a safe operation, whereas deleting a single event or editing an event is not." And: "Instead of deleting, an alternative allowable under many rule sets is to encrypt the event data and forget the key which is kept in another system." (chapter "Whoops, I Did It Again".)

---
**log L169-169 · BODY · CARRIED C5,P0**

- The Overeem study (25 engineers, 19 systems, *Journal of Systems and Software* 2021) is the best record of lived regret. Its five challenges: "event system evolution, the steep learning curve, lack of available technology, rebuilding projections, and data privacy." On upcasters: "If you have been running upcasters for a long time, you will have quite a stack of them in place, which slows down the entire loading." On rebuilds: "Rebuilding is slow" (ten engineers). On privacy law: "Systems HealthSys and P-PaySys use some form of anonymization and removal of information to comply. Obviously, this requires them to rewrite events. System IdentitySys takes a completely different approach. The system separates the events and the personal information in two different stores." The vendors took a side: Event Store and AxonDB "deliberately do not offer these operations" (in-place edits). The study's advice ends: "In-place transformation should only be used by those systems that do not require immutability or an audit log."

---
**log L170-171 · BODY · DISAGREES C5**

- On crypto-shredding, the practice literature disagrees with Young. Mathias Verraes: deleting the key "effectively makes all copies and backups of the sensitive data unusable", but he prints a lawyer's correction: "the law does not consider deleting the encryption key equal to actually deleting the data itself." On keeping personal data in a side store, Verraes names the cost: it "breaks the concept of an Event Store as the Single Source of Truth", and the residue: a person "can be identified not only through personal information, but through the associations and relations with other information."


---
**log L179-179 · BODY · CARRIED C5**

- (9) REPORTED: whole-stream deletion, truncation from the front, private and public streams, or forget the key. Event Store's documentation adds that even a deleted stream "retains one event within the stream to indicate the stream's existence", and warns about this for sensitive data.

---
**log L186-187 · BODY · DISAGREES C5,P0**

**6. Disagreements.** With Martin Fowler's 2005 framing, which treats retroactive correction as a first-class feature; Young treats rewriting as the nuclear option. With the GDPR lawyers, via Verraes, on whether forgetting a key is deletion. No hostile dispute with Helland, whom he cites with approval.


---
**log L201-202 · BODY · CARRIED C5,P0**
*Voice by voice › 4. Martin Kleppmann*

- Deletion: "If permanent deletion of records is required (e.g. to delete personal data in compliance with the GDPR right to be forgotten [62]), an immutable event log requires extra care. Proposed solutions include periodically rewriting the log to remove any records that need to be deleted, or encrypting personal data with a per-user key that can be deleted". (DEBS 2021.)


---
**log L207-207 · BODY · CARRIED C5,P0**

- The local-first team's own reported failure: "CRDTs store all history, including character-by-character text edits. These pile up, but can't easily be truncated because it's impossible to know when someone might reconnect to your shared document after six months away". (*Local-first software*, 2019.)

---
**log L208-208 · BODY · CARRIED C5,P0**

- His shipped system deletes. A Bluesky repository holds everything a user did "minus any records they have explicitly deleted". He lists the opposite as a defect of Secure Scuttlebutt: "it is not possible to delete content once it has been posted".

---
**log L218-218 · BODY · CARRIED C5,P0**

- (9) REPORTED: rewriting the log is a legitimate option; so is forgetting a key; downstream copies are cleaned by deleting from the log and replaying.

---
**log L248-248 · BODY · CARRIED C5,P0**
*Voice by voice › 5. Certificate Transparency, Trillian, and their descendants*

- *Logs end.* Chrome policy: "new CT logs must be temporally sharded", and a log is removed once its date range has passed.

---
**log L251-251 · BODY · DISAGREES C5**

- *Removal was proposed and refused.* Ryan Sleevi on name redaction, 2016: allowing it freely "is non-viable, precisely because it would allow a CA to redact domains to a degree that prevents detection of misissuance." The ecosystem's answer to privacy law is to keep personal data out. The Linux Foundation's notice for Sigstore's log: "You MAY NOT include any personal data about an individual other than yourself", and what you submit is "maintained indefinitely".

---
**log L261-261 · BODY · DISAGREES C5**

- (9) INSTITUTIONAL: removal is impossible by design, so sensitive content is kept out, or only a hash is logged (the Go checksum database logs hashes of modules, not modules).

---
**log L298-298 · BODY · CARRIED C5,P0**
*Voice by voice › 7. Phil Bernstein's Hyder*

- The log is not kept. Live nodes "can be copied to the end of the log by a copier transaction, thereby freeing up the segment for reuse." And: "we found GC to be detrimental to performance."

---
**log L321-321 · BODY · CARRIED C5,P0**
*Voice by voice › 8. Amazon Aurora, and what AWS did next*

- The log is not kept: storage nodes advance pages "by coalescing the older log records and then safely garbage collecting them." Durability beyond that lives elsewhere: the storage layer "continuously and transparently backs up redo log streams to Amazon S3."

---
**log L344-345 · BODY · CARRIED C5,P0**
*Voice by voice › 9. Two short notes on voices the brief did not list*

**Nathan Marz (Storm, Lambda, Rama).** He built the substrate, and his fact model is close to Sid's. "data is inherently time based. A piece of data is a fact that you know to be true at some moment of time." "CRUD has become CR." His reason for immutability is human error: "writing bad data does not override or otherwise destroy good data." He rejects keep-the-last-N as a substitute: "once the database compacts the row, the old value is gone." He admits deletion exists: "There are a few cases where you do want to permanently delete data, such as regulations requiring you to purge data after a certain amount of time." (*How to beat the CAP theorem*, 2011.) Red Planet Labs' slogan for the same idea: "no fact once learned is ever lost to time".


---
**log L348-348 · BODY · CARRIED C5,P0**

- "Depot migrations never change the offsets of records. If a record is excised, a small tombstone value is written in its place. Topologies and foreign depot reads will exclude those tombstones when reading from the depot."

---
**log L402-402 · BODY · DISAGREES C5**
*Question by question: what this camp would say › (9) Value: never removed, so how is one deleted, backups inc*

- **Erasure**: the content must truly go. REPORTED options: excise in place and leave a marker (Helland's "no present data"; Rama's tombstone); keep the erasable part in a second store (Verraes, and one system in the Overeem study, which serves "default values" once the data is gone); encrypt and forget the key (Young, Kleppmann, Dudycz); rewrite the log (Kleppmann; two systems in the Overeem study: "Obviously, this requires them to rewrite events."); or refuse to take the data at all (CT; the Linux Foundation notice).

---
**log L403-404 · BODY · CARRIED C5,P0**

- **Trimming** for space. A different matter; see (13).


---
**log L405-406 · BODY · DISAGREES C5**

Each erasure road has a named cost. The second store "breaks the concept of an Event Store as the Single Source of Truth" (Verraes). Forgetting a key reaches "all copies and backups", but "the law does not consider deleting the encryption key equal to actually deleting the data itself." Without it, backups mean "restoring and cleaning up backups and creating new ones with the cleaned-up data" (Dudycz). And after any of them, the person may still be findable through what points at them.


---
**meaning L555-561 · BODY · CARRIED C5,P0**
*Part one — the exchange › 1.7 What each would make of Sid's design (all INFERRED)*

- On never rewriting, Hickey's own product gave ground. INSTITUTIONAL (Datomic docs,
  checked): "Excision is the complete removal of a set of datoms matching a
  predicate"; "Privacy laws might require you to excise data"; and "the excise
  attributes themselves are protected from excision, so there is no way to
  'erase your tracks'. Every excision creates a permanent record." That is a
  worked answer to (0) and (9): never lost, rarely and visibly cut.


---
**meaning L1083-1089 · BODY · CARRIED C5,C1**
*Part two — the people › 2.4 IPFS and IPLD (Juan Benet), and what AT Protocol did wit*

**Deletion under content addressing.**

- REPORTED (Jorropo, IPFS maintainer, forum 2022): "You cannot delete files from
  HTTP. Because if an other server you don't control host the data, you cannot
  delete it from that server. [...] for IPFS things are exactly the same". The
  difference Jorropo grants: with hashes, a surviving copy is found and verified
  automatically.

---
**meaning L1090-1093 · BODY · CARRIED C5**

- INSTITUTIONAL (IPFS docs): "information about which nodes are retrieving
  and/or reproviding which CIDs is publicly available." And on encrypting
  before hashing: "Future breakthroughs in computing might allow going back and
  decrypting older content".

---
**meaning L1192-1196 · BODY · CARRIED C5,P0**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

- Privacy removal exists even here. INSTITUTIONAL (Meta oversight policy):
  suppression hides revisions from all but a small named group; it is "clearly
  identified in the page history what edits had been suppressed"; and
  "Suppressions are logged privately."


---
**meaning L1320-1321 · BODY · CARRIED C5**
*Part two — the people › 2.6 RDF, W3C PROV, nanopublications, trusty URIs*

- (9): "Invalidation is the start of the destruction, cessation, or expiry of an
  existing entity by an activity."

---
**meaning L1352-1362 · BODY · DISAGREES C5,P0**

**Updating and retracting when nothing can change.** REPORTED (Kuhn et al.,
2016): a nanopublication "cannot be deleted or "unpublished," but only marked
retracted or superseded by the publication of a new nanopublication". The
convention (2021): a new version says `supersedes` in its own publication info;
a retraction is a separate nanopublication whose assertion is `retracts`. "We
only consider them valid if the retraction or update is signed with the same key
pair, but more flexible solutions are possible in the future." The Python
library lets you retract something that is not yours with `force=True`. So
anyone can *write* a retraction. Whether it *counts* is decided by each reader.
"Is this retracted?" is a query over backlinks, not a flag.


---
**meaning L1974-1978 · BODY · CARRIED C5,P0**
*Part three — substrates › 3.3 Webstrates (Klokmose, Eagan, Baader, Mackay, Beaudouin-L*

**History.** "The server keeps the entire log of operations on each webstrate,
which could be culled." It never was. Restore appends. But `?delete` destroys a
whole document and its history "completely and unrecoverably". Their deletion
story is one door, at document size.


---
**meaning L2021-2027 · BODY · DISAGREES C5**
*Part three — substrates › 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tri*

### 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tribble and others)

**What they promised.** One docuverse for the planet. Permanent addresses for
everything. Nothing deleted. Every version kept. Quotation that stays connected
to its source, with credit and payment. It is the closest thing to a precedent
for Sid's layer zero. It did not ship.


---
**meaning L2173-2175 · BODY · CARRIED C5,P0**
*Part four — Sid's questions, hung on the parts of the fact › (0) The log: never rewritten, or only never lost?*

**Split.** Those who planned the cut (Datomic, Wikidata) kept their history.
Those who did not (AT Protocol) lost it.


---
**meaning L2267-2277 · BODY · CARRIED C5,C6**
*Part four — Sid's questions, hung on the parts of the fact › (9) Value: never removed, so how is one deleted, backups inc*

**My read.** Nobody in this camp has a clean answer for backups. Datomic's own
advice, to back up before cutting, leaves the cut content in the backup. What the
camp does teach is what must be true of the record so that a cut is *possible*:
the value can be removed while the envelope stays, so based-on walks still work;
no other fact's validity depends on a strong hash of the cut content (AT Protocol
removed its back-pointer for this reason); and if a fact's id is a hash that covers
its value, the hashed content includes a random salt that is cut along with it, or
the surviving id lets anyone confirm a guess. Sid's store has one advantage none
of them had. Crossings are recorded, so it knows which screens, models, and hosts
were handed the content.


---
**rama L52-53 · BODY · CARRIED C5,P0**
*Part one — What Rama's own reference says › Question by question › (0) The log itself: never rewritten, or only never lost?*

- **CHECKED** `docs/14-depots.md:288` — "A depot's records can be migrated to new values or excised as part of a module update. A migration is specified as a function of one argument that takes in a depot record and returns the new value to replace it with. If the function returns Depot.TOMBSTONE, the record is removed from the depot."
- **CHECKED** `docs/14-depots.md:310` — "Depot migrations never change the offsets of records. If a record is excised, a small tombstone value is written in its place. Topologies and foreign depot reads will exclude those tombstones when reading from the depot."

---
**rama L54-54 · BODY · CARRIED C5,P0**

- **CHECKED** `skill/depot-migration.md:85-93` — the mechanism is two logs. Rama builds a new log beside the old one, then "atomically switches to the new log and deletes the original."

---
**rama L55-55 · BODY · CARRIED C5,P0**

- **CHECKED** `docs/14-depots.md:306` — the migration function "must be idempotent (as of version 1.5.0)" and it also runs on new appends until the module is updated again to remove it.

---
**rama L94-94 · BODY · CARRIED C5,P0**
*Part one — What Rama's own reference says › Question by question › (9) Value: never removed, so how is one deleted, backups inc*

- **CHECKED** depot excision exists (`docs/14-depots.md:288-310`, quoted under (0)). The slot stays. The content goes. Readers skip it.

---
**rama L95-95 · BODY · CARRIED C5**

- **CHECKED** `docs/15-pstates.md:181-200` — PStates are mutable, so removal there is an ordinary write (`termVoid`). One trap: "If you instead remove a parent of the subindexed structure, the reference to the subindexed structure will be deleted but the individual elements on disk will not… you should delete them explicitly before deleting the top-level key."

---
**rama L96-97 · BODY · CARRIED C5**

- **CHECKED** `docs/22-backups.md:19-34` — backups hold depots, PStates, topology progress, the module jar, and configs. They are incremental and file-level ("log segments for depots, SSTs and write-ahead logs for PStates").
- **CHECKED** `docs/22-backups.md:112-114` — old backups go only when backup GC says so: `backup.max.age.hours`, `backup.min.backups.to.keep`.

---
**rama L98-99 · BODY · CARRIED C5**

- **NOT IN THE REFERENCE:** encryption at rest, per-record keys, crypto-shredding, or any statement on how an excised record leaves old backups.


---
**rama L100-101 · BODY · DISAGREES C5**

**IMPLIED:** in Rama an erasure has three parts. (a) A depot migration that tombstones the records. It is a module update and a whole-depot rewrite, so erasures would be batched, not done one by one. (b) Explicit removal from every PState that copied the value, minding the subindex trap. (c) Waiting out the backup retention window, because old backup files still hold the bytes. The erasure is complete only when the oldest backup that contains the record has been collected. If Sid wants erasure to be one small act, the usual answer (encrypt each subject's values with their own key, destroy the key) is outside anything this reference offers, and must be fixed before the first record because it changes what bytes go in the value.


---
**rama L315-316 · BODY · DISAGREES C5**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(9) Deleting values.** REPORTED: purge for regulation is "easily supported"; bad data is deleted and views recomputed; Rama has tombstones. I found nothing by him on GDPR by name (HN search: zero hits), nothing on erasing from backups, and nothing on encryption beyond one line: "Things like E2E encryption are pretty easy to implement on top of Rama's existing primitives" (HN, 2023-08-16, https://news.ycombinator.com/item?id=37153669). INFERRED: he treats erasure as an operator's batch job over the dataset, and sees no conflict with immutability, because deleting says nothing about whether the fact was true.


---
**skeptics L38-39 · BODY · CARRIED C5**
*1. The ordinary default, question by question*

**(9) Value: how is one deleted, backups included?** Hard delete, and backups expire. For undo, soft delete. For event-sourced systems, two patterns from Mathias Verraes. Crypto-shredding, REPORTED: "Encrypt the sensitive attributes, with a different encryption key for each resource (such as a customer). Only give the key to consumers that require it. When the sensitive information needs to be erased, delete the encryption key instead, to ensure the information can never be accessed again. This effectively makes all copies and backups of the sensitive data unusable." ([Verraes, Crypto-Shredding, 2019](https://verraes.net/2019/05/eventsourcing-patterns-throw-away-the-key/)). Forgettable payloads, REPORTED: "Store the sensitive payload of an event in a separate store to control access and removal." ([Verraes, Forgettable Payloads, 2019](https://verraes.net/2019/05/eventsourcing-patterns-forgettable-payloads/)). The critics are in section 1.1.


---
**skeptics L74-74 · BODY · DISAGREES C5**
*1. The ordinary default, question by question › 1.1 Crypto-shredding and its critics*

- **The pattern's own author is cautious.** Verraes, REPORTED: "The Crypto-Shredding pattern is of course only as good as your encryption and your key management practices. Today's unbreakable encryption could be tomorrow's infosec disaster." And: "Both patterns do not solve the problem of consumer storing encrypted data, or using the encrypted data to compute some new value (that may still be sensitive)." And: "There is also a concern of legality: is Crypto-Shredding a sufficient implementation of delete requests under GDPR, or does the law consider the data not truly deleted?"

---
**skeptics L75-75 · BODY · DISAGREES C5**

- **A practitioner's legal reading, published on the same page.** Harrison J. Brown, REPORTED: "the law does not consider deleting the encryption key equal to actually deleting the data itself. Encrypted personal data is still personal data, regardless of whether anyone has the key." Brown's advice: crypto-shredding for business-sensitive data, and for personal data "your Forgettable Payloads pattern (where one stores the sensitive payload of an event in a separate store to control access and removal) is more appropriate".

---
**skeptics L76-76 · BODY · DISAGREES C5**

- **The French regulator, 2018, is softer.** CNIL, REPORTED: "when the data recorded on the blockchain is a commitment, a hash generated by a keyed- hash function or a ciphertext obtained through 'state of the art' algorithms and keys, the data controller can make the data practically inaccessible, and therefore move closer to the effects of data erasure." But: "these solutions do not, strictly speaking, result in an erasure of the data, insofar as the data would still exist in the blockchain." ([CNIL, Blockchain and the GDPR, 2018](https://www.cnil.fr/sites/cnil/files/atoms/files/blockchain_en.pdf))

---
**skeptics L77-77 · BODY · DISAGREES C5**

- **The European board, 2025, is harder.** EDPB, REPORTED: the rights to erasure and to object "must be complied with by design." And (one sentence, split by a page break in the PDF): "It is technically demanding and often difficult to grant the request for rectification or for erasure made by a data subject when clear text, encrypted or hashed data is recorded on a blockchain. It is therefore not advisable to register personal data in those forms on a blockchain. Instead, personal data in those forms should be stored off-chain." And: "the EDPB recommends looking at other tools if the strong integrity property of blockchains is not needed." On correction, the board accepts an appended fix in some cases: "the right to rectification can be met by a subsequent transaction, which announces the cancellation of an earlier transaction, even though the first transaction will still appear in the chain." ([EDPB Guidelines 02/2025, version for public consultation, April 2025](https://www.edpb.europa.eu/system/files/2025-04/edpb_guidelines_202502_blockchain_en.pdf))

---
**skeptics L78-79 · BODY · CARRIED C5**

- **The academic systems view disagrees with the critics.** The K9db paper (covered in the group A report) calls crypto-shredding "widely considered a compliant approach".


---
**skeptics L80-81 · BODY · DISAGREES C5**

INFERRED: the EDPB text is about blockchains, where no single party controls the log. Sid's store has one operator, which helps. The reasoning still carries to any log that is never rewritten. Its direction is: personal values outside the immutable log, referenced from it; keys per person as a second line, not the only one. "Value: never removed" is the clause a regulator would read first.


---
**skeptics L236-236 · BODY · DISAGREES C5**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (9) delete | Hard delete; backups age out; soft delete for undo; crypto-shredding in event stores | EDPB 2025: "not advisable" to put even encrypted personal data on an immutable ledger; "stored off-chain" (REPORTED). Verraes and Brown: forgettable payloads for personal data (REPORTED). Brandur Leach: soft delete "bleeds out into all parts of your code" (REPORTED) |

---
**skeptics L309-310 · BODY · CARRIED C5**
*8. What each source implies for a second store*

- **EDPB, INFERRED.** A second store is a second controller. An erasure then has to reach both. If personal values sit outside the log and are referenced from it, there is one place to erase. If they were copied into a second immutable log, there are two.


---
**sync L725-726 · BODY · NEW-REASON C5,C2**
*2. Section one: sync and multiplayer › 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)*

- (9) A global version forces soft deletes (a client that is behind must be
  told that a row went away); per-row versions allow real deletes. REPORTED.

---
**sync L841-845 · BODY · DISAGREES C5**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- History is wanted: "I think it's actually a desirable feature to keep as much
  history as possible… So I would rather put effort into making it efficient to
  store the whole history than into clearing the history." (Kleppmann,
  automerge-classic issue #51, 2018)


---
**sync L875-886 · BODY · DISAGREES C5**

- *Deletion never shipped.* Asked since 2018. "Discarding the history is not
  currently possible without replacing the document: if you don't have the same
  history, you can't merge the same changes. That's because we use a git-like
  commit hash for each change to ensure integrity." (van Hardenberg, issue #799,
  2023) Why it is hard without a centre: "that requires knowing what all your
  devices are, and what state they are in… sometimes devices suddenly disappear
  and never come back (e.g. because their owner dropped it in the toilet)."
  (Kleppmann, automerge-classic #259, 2020) Orion Henry's truncate proposal
  began from the cost of the only workaround, a fresh document, which "also
  removes any record of what actors are responsible for the current state of
  the document" (gist, about 2021–22); the proposal never shipped. The 2026
  closure of the request says compression made history cheap enough.

---
**sync L926-927 · BODY · DISAGREES C5**

- (9) They cannot delete, and they say why: integrity is chained through
  content hashes. REPORTED.

---
**sync L940-945 · BODY · DISAGREES C5**

**5. Resemblance and difference.** Automerge is the nearest thing to "every
fact carries its provenance graph", and it never deletes. Different: no
authority, so it needs hashes, tolerates dangling links, and cannot truncate.
Sid has an authority, so can use cheaper names and can know what every reader
has seen.


---
**sync L946-950 · BODY · DISAGREES C5**

**6. Who they disagree with.** Kleppmann with his own users on deleting
history. With Postel's law. With Irwin on whether predecessors are redundant.
Conceded to a 2026 Ink & Switch critique: "convergence is not the same as
correctness. This is a good criticism!"


---
**sync L1003-1010 · BODY · DISAGREES C5**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- The admission on deletion: "Because Upwelling currently records and shares
  the full history of a document, there is no way to excise names or
  information that may have appeared in earlier drafts. This is a problem
  caused by keeping too much history, a consequence of changing from a design
  that keeps much less." And the open problem: "CRDTs are currently designed to
  guarantee that users will see the same results from the same inputs. In this
  case, users will want to see the same results from different inputs."


---
**sync L1055-1057 · BODY · NEW-REASON C5**

- What cannot be revoked: "While we can revoke future write access, if someone
  has the data and the symmetric key, then they have the ability to read that
  data."

---
**sync L1107-1108 · BODY · NEW-REASON C5,P0**

- History "can't easily be truncated because it's impossible to know when
  someone might reconnect to your shared document after six months away".

---
**sync L1124-1126 · BODY · DISAGREES C5**

- (9) Full shared history makes names impossible to remove. They call it a
  problem, not a virtue. REPORTED. What someone has read cannot be unread.
  REPORTED.

---
**sync L1173-1175 · BODY · NEW-REASON C5**
*2. Section one: sync and multiplayer › 2.10 Yjs (Kevin Jahns; 2015–now)*

- "We can't garbage collect deleted structs (tombstones) while ensuring a
  unique order of the structs." (README)


---
**sync L1198-1200 · BODY · NEW-REASON C5**

- On erasure for compliance, his answer to a user: "What you are trying to do
  makes sense, but it is very complicated and requires deep understanding of
  the internal structure of a Yjs document". (discuss.yjs.dev thread 537, 2021)

---
**sync L1476-1478 · BODY · NEW-REASON C5,C8**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- *The identity log leaks.* "The full history of DID operations and updates,
  including timestamps, is permanently publicly accessible. This is true even
  after DID deactivation". (did:plc spec)

---
**sync L1560-1568 · BODY · NEW-REASON C5**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

- On deletion (NIP-09, "Event Deletion Request"): "Clients MAY choose to inform
  the user that their request for deletion does not guarantee deletion because
  it is impossible to delete events from all relays and clients." And the
  request must outlive its target: "Relays SHOULD continue to publish/share the
  deletion request events indefinitely". NIP-62 adds the legal case: "This
  procedure is legally binding in some jurisdictions, and thus, supporters of
  this NIP should truly delete events from their database", and "Relays MUST
  ensure the deleted events cannot be re-broadcasted into the relay."


---
**sync L1614-1615 · BODY · NEW-REASON C5**

- (9) A deletion request lives forever and cannot be enforced; a legal delete
  needs a permanent do-not-readmit list. REPORTED.

---
**sync L1717-1718 · BODY · CARRIED C5**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

- (9) A deleted value leaves a fixed-shape hole and the log stays valid.
  REPORTED.

---
**sync L1745-1754 · BODY · CARRIED C5**
*2. Section one: sync and multiplayer › 2.16 Matrix (added; checked by me against spec.matrix.org on*

- *Redaction keeps the envelope and drops the content.* "Since some events
  cannot be simply deleted, e.g. membership events, we instead 'redact' events.
  This involves removing all keys from an event that are not required by the
  protocol. This stripped down event is thereafter returned anytime a client or
  remote server requests it. Redacting an event cannot be undone, allowing
  server owners to delete the offending content from the databases."
  (Client-Server API, Redactions) The list of surviving keys is fixed per room
  version and is the envelope: `event_id`, `type`, `room_id`, `sender`,
  `state_key`, `content`, `hashes`, `signatures`, `depth`, `prev_events`,
  `auth_events`, `origin_server_ts`. (Room version 11)

---
**sync L1857-1865 · BODY · CARRIED C5**
*3. Section two: versioning › 3.1 Git (Linus Torvalds, Junio Hamano, Jeff King, brian m. c*

- *Deletion.* GitHub's guidance begins: "as a first step you need to revoke
  and/or rotate that secret… that may be sufficient to solve your problem."
  Rewriting advertises what was removed: "clueful users with an existing clone
  will notice the history divergence and can use it to quickly and easily find
  the sensitive data still in their clone". It also strips signatures "for
  commits that pre-date the sensitive data removal as well", and "You cannot
  remove sensitive data from other users' clones". (GitHub Docs, "Removing
  sensitive data from a repository")


---
**sync L1880-1881 · BODY · CARRIED C5**

- (9) Rotate first. Rewriting is loud, breaks every downstream name, and cannot
  reach copies. REPORTED.

---
**sync L1990-2000 · BODY · CARRIED C5**
*3. Section two: versioning › 3.3 Fossil (D. Richard Hipp; 2006–now)*

- *Shunning.* "Fossil is designed to keep all historical content forever.
  Fossil purposely makes it difficult for users to delete content …
  Nevertheless, there may occasionally arise legitimate reasons for deleting
  content." The mechanism: "Every Fossil repository maintains a list of the
  hash names of 'shunned' artifacts. Fossil will refuse to push or pull any
  shunned artifact." The order to delete does not travel, on purpose: "The fact
  that the shunning list does not propagate is a security feature. If the
  shunning list propagated then a malicious user (or a bug in the fossil code)
  might introduce a shun record that would propagate through all repositories
  in a network and permanently destroy vital information." The list outlives
  the content "to prevent the artifact from being reintroduced". (shunning)

---
**sync L2006-2008 · BODY · DISAGREES C5**

- *Deleting a person, not their record.* Scrubbing removes a user and password,
  "However, in the DAG, commits by 'bertina' will continue to be visible
  unchanged even though there is no longer any such user in Fossil." (shunning)

---
**sync L2186-2187 · BODY · NEW-REASON C5**
*3. Section two: versioning › 3.6 Pijul (Pierre-Étienne Meunier, Florent Becker; 2015–now)*

- Deleting is labelling: "this system is append-only, in the sense that
  deletions are handled by a more sophisticated labelling of the edges". (same)

---
**sync L2210-2214 · BODY · CARRIED C5**

- *Header apart from contents.* "one contains a header with metadata, the
  dependencies, known patches, edits, and a cryptographic hash of the contents.
  The other section contains the contents… This division allow a server and a
  client to skip contents that isn't alive anymore, while allowing the client
  to retrieve and verify the contents at a later time".

---
**sync L2223-2225 · BODY · CARRIED C5**

- *Removal is local.* "A change can only be unrecorded if all changes that
  depend on it are also unrecorded in the same operation." (unrecord reference)


---
**sync L2459-2462 · BODY · NEW-REASON C5**
*4. Question by question › (9) Value: never removed, so how is one deleted, backups inc*

- Delete rarely, locally, by a kept list that also blocks re-entry, and never
  let the order spread by itself: Fossil. Nostr agrees on the list ("MUST
  ensure the deleted events cannot be re-broadcasted") and spreads requests
  forever. REPORTED.

---
**sync L2463-2463 · BODY · NEW-REASON C5**

- What was read cannot be unread: Keyhive, the local-first essay. REPORTED.

---
**sync L2466-2467 · BODY · NEW-REASON C5,C2**

- A global version number forces soft deletes; per-row versions allow real
  ones: Replicache. REPORTED.

---
**sync L2471-2472 · BODY · CARRIED C5**

- Rotate first; rewriting is loud and cannot reach copies: GitHub's guidance.
  REPORTED.

---
**sync L2473-2474 · BODY · CARRIED C5**

- Backups "are often deliberately immutable"; deletion "is more a matter of
  'making it harder to retrieve the data'": Kleppmann. REPORTED.

---
**sync L2475-2476 · BODY · DISAGREES C5**

- Automerge and Upwelling cannot delete and say so. REPORTED.


---
**sync L3014-3015 · BODY · NEW-REASON C5,C6**
*5. The group › 5.1 The voices that matter most, who I dropped, who is missi*

- Mercurial (supersession records should travel) against Fossil (a delete
  order must not travel).

---
**sync L3190-3193 · BODY · DISAGREES C5**
*6. The second store: what each source implies*

- **Digests earn their place here.** Tagged with their algorithm, over a
  specified canonical form of the logical envelope, with the hash of the value
  inside. Allow several algorithms at once (Fossil) or bump a container version
  (Matrix). Do not tie them to storage bytes (Dolt, Tezos, SSB).

---
**sync L3206-3209 · BODY · NEW-REASON C5**

- **Deletion is a request.** Fossil refuses to let delete orders spread;
  Mercurial and Nostr let them spread; nobody can enforce them. What can be
  done: send the erasure fact, ask for an erasure fact back, and record
  silence.

