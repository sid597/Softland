# C1 name (2)(1): main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L617-617 · ABOVE · DISAGREES C1**
*8. Questions this camp would call the wrong question*

- **(1) "Is a fact pointed at by the gate's number, or by its own id?"** Facts have no ids. Entities have ids and sayings have numbers. Wanting to point at a fact means one of two things. Either you want the saying, so use the gate's number. Or you have found a thing you have not yet named, so make it an entity.

---
**datalog L786-786 · R2 · DISAGREES C1**
*Round two › R2.1 The tailored questions › T1. The unit of saying*

| part of the saying | who writes it | note |
| position | the gate | one number in the order of the saying's home. It is the version of every fact in the saying |

---
**datalog L788-788 · R2 · CARRIED C1,C8,E4**

| part of the saying | who writes it | note |
| by whom, and the grant it acts under | the door | attributes, not slots |

---
**datalog L789-789 · R2 · CARRIED C1,E3**

| part of the saying | who writes it | note |
| when | the gate's clock | once |

---
**datalog L800-800 · R2 · DISAGREES C1**

| part of the fact | note |
| saying, and its place within the saying | the path to everything in the table above |

---
**datalog L801-802 · R2 · DISAGREES C1,C7,C6**

| part of the fact | note |
| replaces | lean (6). The position of the fact it supersedes in that cell. Absent for a new cell |


---
**datalog L810-810 · R2 · NEW-REASON C1**

- R: Datomic has the primitive for it. A unique-value attribute refuses a second assertion: "Attempts to assert a new tempid with a unique value already in the database will cause an IllegalStateException." [D-IDENT]. XTDB v2 added transaction metadata for the same purpose: "upstream request IDs, correlation IDs" [X-TXS].

---
**datalog L812-813 · R2 · DISAGREES C1**

- R, and this supports lean (1) from an unexpected side. Nubank splits databases by "the chronological replay of database transactions into multiple new databases while preserving timestamps and fixing historical mistakes" [N-STORY]. A replay gives every fact a new position. A name that is not the position survives it. So the camp's lived practice backs the lean's split between the name and the gate's number. It only moves the name up one level, to the saying.


---
**datalog L818-818 · R2 · DISAGREES C1**

- **(1)** The name is the saying's id. A fact is addressed as saying plus place, or as cell plus position. One salted hash per saying, not one per fact.

---
**datalog L823-824 · R2 · DISAGREES C1**

- The per-fact "version" slot disappears. The version of a cell is the position of the saying that last wrote it. R: Halloway, "this ordering is visible via the time t shared by every datom in the transaction" [S-HN24].


---
**datalog L836-837 · R2 · DISAGREES C1**

4. **Saying something about one fact.** R: Hickey declines it. "Reified transactions can not be used to model a graph" [H-ML16]. If a single fact needs things said about it, that fact was an entity all along. I: Sid's based-on already points at a cell at a version, which needs no per-fact id. So nothing in the brief is lost here.


---
**datalog L838-840 · R2 · CARRIED C1**

**What Datomic users regret about provenance at the transaction level.**

- R: Hickey names the limit himself. "The granularity you have for that is the transaction level, not the datom level." [H-WD]

---
**datalog L929-929 · R2 · CARRIED C1,E3**
*Round two › R2.1 The tailored questions › T5. "A fixed envelope is place-oriented." What would they fi*

- **"Should data always incorporate time."** Yes, but by reference. R: a datom carries "some path to time", and that path is the transaction [H-DD]. Time is in the fixed part as a pointer. It is never repeated as a value.

---
**datalog L955-956 · R2 · CARRIED C1,W1,C3**
*Round two › R2.2 Ask A: the leans, one by one*

- *No time inside* costs locality only if an index leads with the id. R: "checking for the existence or uniqueness of random (v4) UUIDs has poor locality, as reads will scatter" [D-IDENT]. **Case:** the gate's check of a cell, at machine rate, against an index of ten billion random ids, is a cold read every time. If indexes lead with the layer, the locality comes from the layer and random ids cost nothing. So this lean is safe under T3's unit and expensive under lean (10)'s.


---
**datalog L984-985 · R2 · DISAGREES C1**

**(1) Point at a fact by its own content-and-salt id.** Half backed, half pressed. *Backed:* a name that is not the gate's number. R: Nubank's replays into new databases change every position [N-STORY]. *Pressed:* put the name on the saying. R: Hickey on hashes as names, "it does not convey anything about order unless you have the rest of the repo. It does not imply anything about causality" [H-SPEC]. **Case:** a walk over a based-on list of 40 content ids learns nothing about which came first without looking each one up. A pointer written as name plus position carries its order with it. And see T2: hash the ciphertext.


---
**datalog L1004-1004 · R2 · DISAGREES C1**
*Round two › R2.3 Ask B: leans the camp would call the wrong question*

3. **(1) "What is a fact's name?"** Ask instead: *what is the saying's name, and where in it is the fact?* T1.

---
**frontiers L562-562 · R2 · DISAGREES C1,E1**
*Round two (2026-09-20): the leans, pressed from the frontier › A. Leans this camp would reject or sharpen*

- **(1), (2), (6), (12), (17): stand.** Content plus salt is the CDC identity (R) with a caller's intent token inside it. One thing follows: an offerer knows a fact's id before it lands, so based-on can name a fact that was later refused. The gate has to check that what an offer stands on did land, and that check is itself a read at a point. (12) is Feldera's list (R). Add the tool's version, which based-on gives for free.

---
**log L558-558 · ABOVE · ABOVE C1**
*For the group › 9. Questions this camp would call the wrong question*

1. **"Pointed at by the gate's number, or by its own id?" (1).** Both, and also a hash. They are three jobs. Asking which one is like asking whether a book needs a title or a shelf mark.

---
**log L671-672 · R2 · DISAGREES C1**
*Round two: the leans, pressed › T2. The three names against leans (1), (2), (6)*

**Lean (1) stands on its main point, and Rama decides it.** Topology depot appends "currently do not have exactly-once semantics in the face of failures and retries" (R, Rama docs). With the gate's number as the name, a repeated append is two facts. With a content-derived id it is one fact seen twice. Who lives this way: Automerge's change hashes, the AT Protocol's CIDs, Git.


---
**log L674-674 · R2 · DISAGREES C1,C5**

- *The salt.* A public salt gives uniqueness and lets strangers verify, but leaks short values after erasure (T1). A secret salt protects them and stops strangers verifying. I: two salts for two jobs. A public one in the offer for uniqueness; a secret one inside the value commitment.

---
**log L675-675 · R2 · DISAGREES C1**

- *Dedupe at ingest.* The deciding case: the ten-million-paper seed dies at paper 6,200,000 and is re-run from the start. With random salts every fact is offered again under a new id, the cells are already filled, and the gate writes millions of refusals that lean (7) keeps forever. With salts derived the way lean (2) derives ingest entity ids, the ids repeat and the gate gives the old answer: "the same reply must be returned" (R, Helland). I: random salts for people and agents, derived salts for ingest lanes.

---
**log L676-676 · R2 · DISAGREES C1**

- *The duplicate check must be exact.* Sunlight's "cache can be best-effort and lossy" (R) only because CT tolerates duplicates. Sid's cells do not. Event Store scopes its check: "The idempotence check is based on the EventId and stream." (R). I: scope Sid's to the cell's partition, where lean (7) already puts the verdict.

---
**log L677-678 · R2 · DISAGREES C1**

- *Hash migration.* Not decisive. Name the algorithm inside the id. When a function weakens, the gate can issue attestation facts under a new one; they prove things only from that day on, under either design. The gate must refuse same id with different content, which is Young's trap: "two completely different events have the same identifier" (R).


---
**log L679-680 · R2 · DISAGREES C1**

**The question under lean (1).** Is the fact the offer unchanged, or the offer plus the gate's stamps? In this camp the judge never edits what it judges. Tango's commit record and its decision record are separate records (R). Kleppmann's claim and its outcome go to separate streams (R). I: let the fact be the offer, byte for byte, and put when, version, epoch and what-was-checked on the verdict beside it. Then the id hashes exactly what the offerer made, repeated appends are identical, and a stranger can check the offerer and the gate separately. The cost is a local join to read a fact's version. This is where I would press hardest.


---
**log L681-682 · R2 · DISAGREES C1,X1**

**"Facts have no ids, sayings do."** An offerer-minted id of the saying satisfies my first name fully: known before landing, stable on retry, portable between stores. Where one offer is one fact they are the same thing. I: reserve the form "saying id plus index" now (Kleppmann's hash scheme numbers the ids inside one update, R), so a saying that carries several facts for one entity stays possible. Across entities no atomic saying can exist under lean (10).


---
**log L683-684 · R2 · DISAGREES C1**

**Lean (2) stands** (Automerge's random 128-bit ids; AT Protocol DIDs). One press on ingest: a derived id inherits its source's hygiene. Helland: "There's nothing to stop them from changing SKU 12345 from a pair of ruby slippers to a can of chocolate sauce." (R). Preprint and journal version, merged DOIs: the derivation rule fixes entity grain forever. I: publish the rule as a versioned seed fact.


---
**log L717-717 · R2 · DISAGREES C1,C4**
*Round two: the leans, pressed › A. The other leans*

- **(17) Sharpen.** Content-computed seed ids: stands (every CT log starts from the same empty hash, R). But split the seed. Kinds and grammars are universal and content-computed. The store's own id and its gate's actor id must be random per store. Deciding case: the second institution's store. If the gate's id is content-computed, two stores' verdicts carry the same actor and cannot be told apart.

---
**log L730-730 · R2 · ABOVE C1**
*Round two: the leans, pressed › B. Wrong questions, against the leans*

2. Lean (1) asks how the id is computed. Ask first: is the fact the offer unchanged?

---
**log L743-743 · R2 · ABOVE C1**
*Round two: the leans, pressed › The three I would press hardest*

1. Lean (1): make the fact the offer, unchanged, with every gate stamp on the verdict. Case: Rama's repeated append; the seed re-run.

---
**meaning L2813-2822 · R2 · DISAGREES C1,E4**
*Round two › T1. Closing ambient authority: the smallest first-record con*

**What a grant is.** An ordinary fact. Its value says: the grantee (an actor id;
for a tool, the tool's *version*, the hash of its body); what it covers
(entities or a pattern, keys, a layer); for what (one chain of work, named by its
because-of root, or "standing"); until when (the chain ends, the session ends, N
writes, or it is superseded); and whether the grantee may hand on a narrower
grant. A grant is itself written `under` a grant. The chain ends at the layer's
root grant, made with the layer, which cites itself. That is the one hand-cut
circle (R: Xanadu's clubs "that are self-reading or self-editing", §3.5; Self's
"The map map is its own map", §2.10).


---
**meaning L2836-2840 · R2 · DISAGREES C1,E4**

**"A click designates and thereby grants", as facts.**

1. The click is one fact by the person: a new grant G. Grantee: tool T at version
   h. Covers: the entities selected, the keys T's signature says it writes, this
   layer. For: this chain. Until: the chain ends. `under`: the person's root grant.

---
**meaning L2884-2894 · R2 · DISAGREES C1**
*Round two › T2. Names of a fact*

### T2. Names of a fact

**Is the offer's id the id of the saying? Yes (I).** The Datomic camp is right
that a fact, as content, has no id: two people can say the same entity, key, and
value. What gets pointed at is the act. RDF needed three designs to get there:
"It is expected that the reifiers (rather than the triple terms) will be used in
further statements" (N, https://www.w3.org/TR/rdf12-concepts/). Verdicts, doubts,
based-on entries, and "replacing 25" all point at sayings. If one offer may carry
several facts that must land together (R: Reed's "possibility", §2.8), the saying
is the offer and a fact in it is (saying id, index).


---
**meaning L2895-2902 · R2 · DISAGREES C1**

**Two ids or three? Three jobs, and the third id belongs to the entity (I).** The
log camp's client-made opaque id and my nonce are the same random bytes. Put them
inside the hashed bytes and the saying needs no second name. The minted, opaque
id has its proper home one level up, as lean (2)'s entity id. So: a minted id for
the thing that lasts through change, a hash for the saying, the gate's number for
position. That is Armstrong's split (R: "I think we need all three", §2.9), with
names kept as facts.


---
**meaning L2909-2920 · R2 · CARRIED C1,W1**

**What the algorithm tag buys: one thing (I, on R and N evidence).** In a store
that never rewrites, ids sitting in old based-on lists can never be recomputed.
When the hash function changes, tagged ids let old and new share a list with no
ambiguity and no translation table. Git has no tag and leans on an accident:
"Fortunately SHA-256 and SHA-1 have different lengths" (N, §2.3). Unison asked
for a tag in 2019, never built it, and later rehashed whole codebases through an
old-to-new table (R, https://github.com/unisonweb/unison/issues/466). The tag
should name the byte encoding as well as the hash, as trusty URIs do (R,
https://arxiv.org/abs/1401.5775), because a hash names an encoding (§2.4). Cost:
a byte or two, and biased leading bytes, so never shard on the raw id (N:
multihash, §2.4).


---
**meaning L3123-3123 · R2 · DISAGREES C1**
*Round two › B. Wrong questions*

- (1) "What is the fact's id?" Facts have none. Sayings do.

---
**meaning L3146-3149 · R2 · DISAGREES C1**
*Round two › C. Above the table, after the leans*

5. **One store: I lower this.** Content-derived seed ids, hashed sayings, and one
   home store per layer remove most of what leaked into the record. What remains:
   authorship rests on the door's word, so leaving is still not credible (R:
   Lemmer-Webber, §2.1); and the seed split in (17).

---
**rama L520-521 · R2 · CARRIED C1,W1**
*Round two*

One correction to my own round one, left in place above and corrected here. Under (2) I said RPL prefers UUIDv7 for disk locality. That was my inference. The reference gives a different reason, sort order. See T3.


---
**rama L536-537 · R2 · NEW-CASE C1,C2**
*Round two › T1. Which log › Shape (a): the offers depot is the permanent record; facts a*

Restore. Consistent: "backups guarantee that depots contain all entries that may have affected any PStates" (**CHECKED** `docs/22-backups.md:34`). Everything after the backup point is gone. Depot offsets are reused (**CHECKED** `:106`). **IMPLIED:** so are the gate's numbers, because its counters are PState data and go back to old values. A fact that a screen showed yesterday as version 37 may be a different fact after a restore. This is the case that decides lean (1): name a fact by its own id.


---
**rama L554-555 · R2 · NEW-CASE C1**
*Round two › T1. Which log › Shape (b): the gate decides, commits, then publishes the adm*

Restore. The same loss after the backup point. Ids protect names, as under (a).


---
**rama L572-573 · R2 · DISAGREES C1**
*Round two › T2. Content ids against the migration door*

Does the conflict remain if the id is computed once and then carried as a stored name that nobody re-verifies? **IMPLIED:** no. A migration may change every other byte, and the id field is copied through. It is then a name.


---
**rama L574-576 · R2 · CARRIED C1**

What is lost when it is only a name.

1. Nobody can check that the content matches the id. A second store cannot use the id to trust content or to notice tampering.

---
**rama L577-577 · R2 · CARRIED C1,C4**

2. Lean (17) wants seed ids that "every store derives". Deriving is re-computing. So the seed facts are exactly where name-only does not work. The second store must produce the same bytes and the same hash in year five. The canonical encoding and the hash function then become the frozen thing, a choice of representation that can never migrate. Rama's own serializer cannot be that form. It is deterministic for built-in types (**CHECKED** `docs/17-serialization.md:167`), but it is internal to Rama, and clients are locked to the cluster's version (**CHECKED** `skill/operate.md:14`). Whether its bytes are stable across Rama versions: **NOT IN THE REFERENCE**.

---
**rama L578-579 · R2 · CARRIED C1**

3. With a random salt (lean 1), nobody without the salt can recompute. For ordinary facts the hash then buys one thing: someone who holds salt and content can later prove the id was minted for exactly that content. If nobody will ever ask for that proof, it is a random id with extra steps. RPL's retry reason is served equally by a random id (N).


---
**rama L580-581 · R2 · DISAGREES C1**

Who in this camp lives this way: nobody. Marz used natural ids (R, 2010), then random ones (book, from memory), and RPL uses UUIDv7 (N). Kafka uses positions. Content addressing belongs to the git, IPFS and transparency-log people. They are missing from this camp.


---
**rama L588-589 · R2 · DISAGREES C1**
*Round two › T3. UUIDv7 against "no time inside the id"*

Why RPL picks v7. **CHECKED** `skill/unique-ids.md:15`: "UUID7 is time-ordered — IDs sort chronologically, which is useful for range queries on subindexed maps and preserves insertion order." And `:19`: "Range queries and cursor pagination over UUID7-keyed subindexed structures are therefore time-ordered." The stated reason is sort order. Disk locality is not stated anywhere. I withdraw it as RPL's reason.


---
**rama L590-591 · R2 · CARRIED C1,W1**

Does that reason apply to an entity id that lasts for life? Only where something walks a map keyed by entity id and wants creation order. An entity id is mostly used for point lookup and as the partition key. "Newest entities first" can be served by a separate index keyed by the gate's stamp. The reason is strong for ids of things that get listed by time. Under lean (1) those ids come from content and salt, and time order within a cell comes from the gate's version. So nothing in the leans needs v7. Lean (2) stands.


---
**rama L592-593 · R2 · CARRIED C1,W1**

What a fully random 128-bit id costs in Rama. Partition balance: nothing, since the partitioner hashes either kind (**CHECKED** `skill/pstate-schema.md:52`). Reading everything about one entity: nothing, if its facts nest under the entity key, because a subindexed structure is contiguous (**CHECKED**, subindexing). Lost: time-ordered scans over ids, so one more index and one more write per entity. Unknown: extra write work in RocksDB from random top-level keys. **NOT IN THE REFERENCE**, and I did not source it elsewhere.


---
**rama L642-642 · R2 · CARRIED C1,C2**
*Round two › T5. One cluster worldwide*

6. Is there, or will there be, supported replication or mirroring of a depot between clusters? Are offsets preserved?

---
**rama L648-648 · R2 · CARRIED C1,W2**

12. Is the built-in serialization byte-stable across Rama versions, so that it could be hashed?

---
**rama L689-690 · R2 · NEW-CASE C1**
*Round two › A. The leans, one by one*

**(1) Name a fact by its own id. STANDS.** RPL (N). The deciding case is the restore in T1. Deriving the id from content: T2.


---
**rama L707-707 · R2 · CARRIED C1**
*Round two › B. Which leans this camp would call the wrong question*

3. **Leans (1) and (17), ids computed from content.** Ask instead: who will ever re-verify, and from which bytes? If nobody, use a random id. If somebody, the canonical bytes are the thing to freeze.

---
**skeptics L292-292 · ABOVE · ABOVE C1**
*7. Questions this camp would call the wrong question*

- **"May an id give away when or where it was made?"** Hyrum's Law turns it around. If it can be seen, it will be used, and then it is part of the contract. The question is which properties of an id Sid is willing to promise forever.

---
**skeptics L294-294 · ABOVE · ABOVE C1**

- **"Random, or computed from its content?"** A false choice in practice. A minted id carries intent. A content hash is a check on it. Amazon and the IETF draft both use the pair.

---
**skeptics L421-421 · R2 · DISAGREES C1,C4,W2,C7**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(13) with (17) and (1): sharpened.** Ids computed from content make the byte encoding of a plain map a promise for ever. That is Hyrum's Law (R) with no way out. Deciding case: a rebuild in year four changes map key order or number formatting, seed ids no longer re-derive, and "every store derives the same ones" fails in silence. Fix one canonical encoding at record one, and put its version inside what is hashed.

---
**skeptics L422-422 · R2 · DISAGREES C1,P0**

- **(1): stands, and pays twice.** A hash over content plus salt is Amazon's caller token (R, Featonby) bound to its parameters, so "same token, different intent" cannot happen. It is also the per-record checksum that DynamoDB keeps "within every log entry" (R). Deciding case: silent corruption on one replica in year five is found by re-hashing. The slots the gate adds (version, when) need cover too.

---
**skeptics L423-423 · R2 · DISAGREES C1**

- **(2): stands.** RFC 9562 (R): where security matters, "UUIDv4 (Section 5.4) SHOULD be utilized". By Hyrum's Law, an id that shows nothing stays free. On derived ingest ids: they can be guessed, so derive them only from public sources. Deciding case for keeping them: a second institution ingests the same ten million papers, and both stores agree on every entity id with no coordination.

---
**sync L57-65 · SHORT · NEW-REASON C1,E1**
*1. The short version*

1. **Three things need names, and most regrets come from giving them one.** The
   *cell* (entity + key + layer) is a mutable slot. The *version* is an
   immutable thing in that slot. The *offer* exists before the gate and may be
   refused. jj keeps two ids on purpose. AT Protocol separates the slot
   (AT URI) from the pinned version (URI + CID). The did:plc spec calls the
   server's sequence number "an annotation … and not an intrinsic property".
   A refusal never gets a gate number, so if refusals are kept, offers need
   their own id.


---
**sync L3150-3151 · ABOVE · ABOVE C1**
*5. The group › 5.3 Questions this camp would call the wrong question*

2. **(1) "The gate's number or its own id?"** Both, and a third name for the
   cell. They are not alternatives.

---
**sync L3345-3355 · R2 · ABOVE C1**
*Round two: the leans, pressed › T1. "No optimism" against one worldwide gate*

- *The person's agents.* One gate per layer can sit near one party. Deciding
  case: a person in Bangalore, thirty agents in a Virginia data centre, all
  writing the person's layer. Gate near the person: every agent step is a long
  round trip, and a chain whose steps each wait for admission runs at four or
  five steps a second. Gate near the agents: the person's hand waits. The known
  way out is to let an offer cite an earlier *unadmitted* offer by its id and
  admit them in order. Kleppmann: a missing dependency "simply results in the
  update … never being delivered" (R §2.8); Automerge's regret adds that such a
  record must wait, never be dropped (R §2.8). That is tentativeness between
  agents. So honest tentativeness is needed for agents before it is needed for
  people. (I)

---
**sync L3377-3382 · R2 · DISAGREES C1**
*Round two: the leans, pressed › T2. Content hash as the name*

**A correction first.** Matrix does not use a random name with a digest beside
it. Room versions 1 and 2 did (an assigned id, plus hashes). Matrix abandoned
that in version 3 because "servers receive multiple events with the same ID"
(N §2.16): many minters, no gate. Inside one store the gate refuses a duplicate
name, so that cost is gone. Between stores it comes back.


---
**sync L3383-3393 · R2 · CARRIED C1**

**The one remaining case for hash-as-name.** A *bare reference* that certifies
itself. A stranger holding only the name can check that what they were handed
is the thing named, trusting no gate. With a random name and a digest beside
it, the fact is checkable but a bare reference is not: a lying store can answer
a name with other content. The repair is known: a reference that crosses
stores carries name plus digest, which is AT Protocol's strong reference
(N §2.13). A second thing is lost: convergence. Hash names give two stores the
same name for the same content with no coordination (Irmin R §3.5; Dolt R
§3.4). Random names do not. The leans keep hash names in the two places where
that pays, ingest (2) and seeds (17). I press both under A.


---
**sync L3400-3401 · R2 · CARRIED C1**

- With a salt there is no convergence, so the hash buys only a commitment of
  name to content. A digest beside a random name gives the same. (I)

---
**sync L3418-3423 · R2 · DISAGREES C1**
*Round two: the leans, pressed › T3. A separable value, against lean (9)*

- *Matrix: absence is a first-class, served state.* The stripped event "is
  thereafter returned anytime a client or remote server requests it", and the
  spec tells servers to attach a copy of the redaction when serving it (N
  §2.16). The id still verifies, because it was computed over the stripped
  form. The list of keys that survive is fixed per room version. In effect the
  envelope is *defined* as what survives redaction.

---
**sync L3557-3570 · R2 · NEW-REASON C1,C4**
*Round two: the leans, pressed › A. The other leans*

**(2) Entity id.** Random, offerer-minted, no time: stands. Weidner, Figma,
Linear and Dolt live this way (R). Sharpen: the gate refuses a clash (I); the
written form carries a scheme marker (Convex, R §2.2). Press the ingest clause.
An id derived from "source plus form" freezes the identity rule forever, as
did:plc's first-record hash did (R §2.13). Deciding case: arXiv 2409.14252v1,
its v3, and the EuroSys DOI. One entity or three? Whatever the rule says on day
one can never be revised, and every normalisation slip (DOI case, URL forms)
splits an entity for good. Alternative (I): the entity id stays random. The
lane writes a *registry cell* whose entity id is derived from the source
identifier and whose value is the entity. Compare-and-set on that cell gives
exactly-once. A wrong merge or split is repaired by a new fact. The same device
enforces any uniqueness rule, such as a key's word in the base, which lean (3)
needs.


---
**sync L3645-3646 · R2 · ABOVE C1,C5**
*Round two: the leans, pressed › B. Wrong questions*

2. **(1).** Name against number is settled. The live question is what any hash
   covers, and whether it survives an erasure (T2).

---
**sync L3654-3657 · R2 · ABOVE C1**

6. **(2), ingest.** "How is the id derived from the source?" Ask where the rule
   "one source, one entity" is enforced. In a cell, under compare-and-set,
   where it can be corrected.


---
## Says the same as the ledger (counted, not copied)

- C1 · datalog · 6: L785-785, L807-808, L809-809, L811-811, L952-953, L954-954
- C1 · frontiers · 1: L253-253
- C1 · log · 1: L380-381
- C1 · meaning · 2: L510-511, L1094-1102
- C1 · rama · 3: L174-175, L325-326, L582-583
- C1 · skeptics · 1: L441-441
- C1 · sync · 23: L618-618, L1229-1230, L1272-1273, L1295-1299, L1337-1338, L1386-1391, L1442-1444, L1490-1491, L1637-1640, L1643-1646, L1806-1807, L1827-1838, L1868-1871, L1892-1894, L2134-2138, L2310-2313, L2754-2756, L2950-2952, L3183-3184, L3187-3189, L3397-3399, L3402-3410, L3411-3413

