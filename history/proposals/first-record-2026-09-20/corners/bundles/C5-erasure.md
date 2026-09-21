# C5 erasure (9)(1): main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L43-44 · SHORT · CARRIED C5,P0**
*1. The short version*

**4. Never rewritten is not the same as never lost, and the camp says so.** Hickey, 2013: "while the past may be forgotten, it is immutable." [H-IM]. Datomic added excision a year after launch because law forced it. It costs index rebuilds "proportional to the size of the entire database", it is absent from Datomic Cloud, and in October 2025 Datomic fixed a bug that had left excised data readable through the as-of and history indexes. XTDB v1 designed for erasure from day one: the log holds only content hashes, the documents sit in a store that can forget. "The transaction log can stay immutable." [X-DOC]. That design then broke replay. XTDB v2 gave up the permanent log altogether.


---
**datalog L609-609 · ABOVE · CARRIED C5**
*7. What this camp would question above the table*

4. **A checker for forgetting.** Datomic shipped an "excision repair tool" after finding that excised data had stayed in history indexes for years. Any store with derived indexes needs a way to prove that a forgotten thing is gone from every one of them.

---
**datalog L620-620 · ABOVE · ABOVE C5**
*8. Questions this camp would call the wrong question*

- **(9) "How is one deleted?"** Three questions in one. Retracting, forgetting, and never recording in the first place have different answers and different costs. Asked as one question, it gets the most expensive answer for all three.

---
**datalog L845-848 · R2 · CARRIED C5**
*Round two › R2.1 The tailored questions › T2. XTDB v1 against leans (9) and (1)*

### T2. XTDB v1 against leans (9) and (1)

Lean (9) is XTDB v1's shape with one change. v1 kept a hash in the immutable log and put the *document* where it could be forgotten. The lean keeps the ciphertext in the immutable log and puts the *key* where it can be forgotten. R, the v1 design in their words: "whilst the transaction topic is immutable, messages in the document topic can be permanently erased" [X-INTRO]. So there are again two stores with two sets of rules. The forgettable one is now tiny per item. That is an improvement: deletes are cheap, and nothing waits for compaction.


---
**datalog L853-853 · R2 · CARRIED C5,C2**

| v1 failure | source | under leans (9) and (1) |
| Erasure changes what past reads return | R [X-V1TX]: "Evict is the only operation which will have effects on the results returned when querying against an earlier Transaction Time" | **Carries over, softened.** The fact stays: entity, key, saying, all of it. Only the value is unreadable. v1 lost the whole document. Here a walk of based-on can say *this stood on something since erased*. That is better than anything the camp built |

---
**datalog L854-854 · R2 · CARRIED C5**

| v1 failure | source | under leans (9) and (1) |
| Replay blocks on missing content | R [X-184] | **Removed from the log, moved to the key store.** Ciphertext never goes missing. But a rebuild must tell a destroyed key from a key it cannot reach. v1's fix was a tombstone, and the same fix is needed: a key lookup must always answer with the key or with a marker that it was destroyed |

---
**datalog L855-855 · R2 · CARRIED C5,C1**

| v1 failure | source | under leans (9) and (1) |
| The same content comes back | R [X-432], still open | **Removed by the salt and the key.** Content that returns gets a new key, a new salt and a new id. There is no collision. The price: the store can no longer recognise that erased content has been submitted again, so a rule of "never accept this again" cannot be enforced. The protection against guessing and the ability to recognise are the same property seen from two sides |

---
**datalog L856-856 · R2 · CARRIED C5**

| v1 failure | source | under leans (9) and (1) |
| Copies outside the log | R [D-REL]: Datomic's fix of October 2025 for excised data left in as-of and history indexes. R [X-SQLQ]: v2 erasure is complete only "once all background index processing has completed" | **Carries over in full.** "Rewrites nothing" is true of the log only. Every derived index that holds a plaintext value must be purged, and that must be checkable. Datomic had to ship a repair tool |

---
**datalog L857-858 · R2 · CARRIED C5**

| v1 failure | source | under leans (9) and (1) |
| Backups | R [D-EXC]: "Excision is irrevocable. You are strongly encouraged to backup a database before excising data." | **Improved by the lean.** Datomic's own procedure leaves the forgotten data in a backup. Key deletion does reach backups of the log. It reaches them only if backups of the *key store* are themselves in the delete plan. Lean (13) says so. That sentence carries a lot of weight |


---
**datalog L865-866 · R2 · NEW-REASON C5,E1**

So the sharpening is one short rule: **hash what is stored, not what it means.** (I. My own analysis. Neither product team discusses it.) It has one consequence to accept knowingly. The gate must see plaintext to check the shape. After erasure, the verdict's line (for example: *shape accepted under grammar version 4*) is the only thing that still speaks about what the value was. See T4.


---
**datalog L869-869 · R2 · CARRIED C5**

1. *A key that is unreachable is not a key that is destroyed.* Year 4. An index is being rebuilt after a runtime rebuild. The key store has an outage. Are a billion values erased? If the rebuild treats a missing key as erasure, the new index silently drops them, and the map lies. If it blocks, that is issue 184 again. The answer is the tombstone: destroying a key leaves a marker, and that marker is a fact. R: Datomic, "excision strikes a delicate balance between forgetting and remembering that you forgot" [D-EXC].

---
**datalog L870-870 · R2 · CARRIED C5**

2. *One key per value, at machine rate.* The key store then holds as many entries as the log holds values. It is a second system of record. It is mutable. It holds the most sensitive material in the building. And losing one entry is an erasure nobody asked for, with no undo. It needs the write order v1 learned the hard way: the key must be durable before the fact lands. v1's version of that lesson: "The indexer will pause consumption of transactions while waiting for all their documents to appear" [X-184].

---
**datalog L873-874 · R2 · CARRIED C5,E6**

**Copies.** A private note is shown to a model. The reply quotes it. 200 facts follow. Each downstream value has its *own* key. Deleting the note's key touches none of them. What finds them is Sid's own walk: follow based-on forward from the erased thing. No one in this camp has that walk. It is an argument *for* recording reads, and an argument that "delete means throw the key away" is where erasure starts, not where it ends.


---
**datalog L875-876 · R2 · CARRIED C5,P0**

**What the XTDB authors wrote about leaving that design.** R: they never wrote one statement that says why they left content-addressed documents. What they wrote is what hurt. Forgotten content blocked replay [X-184]. Returning content raced with eviction [X-432]. Every node carried a full replica [X-DD7]. A reindex took "days to weeks", after which "the transaction log is now ~ephemeral… (no more event-sourcing-style replays required, ever)" [X-HN23]. v2 erases by not copying data forward during compaction [X-4036]. They speak of erasure as the exception: "unless you specifically need us to irrevocably 'ERASE' it, for legal reasons."


---
**datalog L877-878 · R2 · CARRIED C5,P0**

**What they would say of lean (9).** I. They would recognise it as their own version 1 with a better forgettable part. They would bring four warnings from having lived it. Every pointer into the forgettable place must always resolve. No rebuild may ever wait on something forgotten. Erasure will be the one operation that changes the past, so design the walks to say so. And the one I would weigh most: their deepest regret was not erasure. It was that everything could only be rebuilt by replaying a log that only grew. That bears on "never trimmed", in R2.2 under (13).


---
**frontiers L419-419 · ABOVE · ABOVE C5,W1,C3**
*9. What this camp would question above the table*

- **One store for the planet with personal layers.** McSherry would ask for the single-thread baseline before believing the scale problem (INFERRED from COST). Materialize's own design accepts that a single linearization point puts "some limit to horizontal and geographical scale out" (REPORTED). Schwarzkopf would point out that one shared log mixes owners, so erasure needs per-owner keys from day one (INFERRED). Multiverse says per-person computed views are affordable only with a joint dataflow and partial state (REPORTED).

---
**frontiers L552-553 · R2 · NEW-REASON C5,E6**
*Round two (2026-09-20): the leans, pressed from the frontier › T4. Every read, against its cost (an exchange)*

The walks. *What went stale:* re-run pinned reads at now, compare digests, and let the staleness definition read the difference. *What is in doubt:* walk from fact to read set to matched facts, treating support groups as "or". *What did I look at* and *what was the model given:* from crossing to read set to re-run, checked by the digest. *Who read the erased thing:* direct reads of it are exact. Pattern reads are found through an index from key to pattern reads, then re-run. Where a pattern's condition tested the erased value, the answer is "may have read". For an erasure audit, over-reporting is the safe side.


---
**frontiers L560-560 · R2 · NEW-REASON C5**
*Round two (2026-09-20): the leans, pressed from the frontier › A. Leans this camp would reject or sharpen*

- **(9), sharpened.** A matcher that tests values must hold keys, and indexes will hold plain values. Erasure reaches them only because they are derived and get rebuilt (Schwarzkopf's revocation, R). K9db (R) keys per owner. With one key per value, "forget me" is a walk over millions of keys. Wrap value keys under an owner key, so that deleting either one erases.

---
**log L651-652 · R2 · DISAGREES C5**
*Round two: the leans, pressed › T1. The four promises against leans (0) and (9)*

**Press (9): under a strict (0), key destruction has no fallback.** Verraes prints the lawyer's view: "the law does not consider deleting the encryption key equal to actually deleting the data itself" (R). The deciding case: one regulator, somewhere on the planet, in some year, orders the ciphertext itself removed. Under strict never-rewritten Sid breaks the promise for everyone. Under Helland's rule the promise survives, because absence was always allowed: "it will never return data other than the original contents" (R). Rama's tombstone keeps offsets (R). I: keep key destruction as the normal road, name excision as the rare legal road, and make each use a fact.


---
**log L657-658 · R2 · NEW-REASON C5,C8**

**Sharpen "maybe by-whom may go".** If by-whom can be removed, envelopes are rewritable after all. I: by-whom is always an opaque actor id and never goes. What goes is the value of the fact that binds that id to a person. Then no envelope ever needs touching, and (0) holds for envelopes without exception. The residue is real and lean (9) does not remove it: a person "can be identified not only through personal information, but through the associations and relations with other information" (R, Verraes).


---
**log L737-737 · R2 · ABOVE C5**
*Round two: the leans, pressed › C. Above the table: only what the leans change*

- **The runtime is no longer the one non-fact.** Lean (9) creates a second: the key store. It is mutable, it can make any value vanish, and losing it leaves a store of hashes. Verraes: the pattern is "only as good as your encryption and your key management practices" (R). Delos's lesson is that the small thing outside the log is the one that is "necessary and sufficient" (R), and deserves the most care. The non-fact list is now: runtime, key store, encoding and hash rules, trust anchor.

---
**log L744-744 · R2 · DISAGREES C5**
*Round two: the leans, pressed › The three I would press hardest*

2. Leans (0) and (9): define never-rewritten over the canonical form; keep excision as a recorded legal road; record key destruction as a fact; commit to values with a secret salt. Cases: the regulator who rejects key deletion; the key-store restore.

---
**meaning L2998-3004 · R2 · CARRIED C5,C6**
*Round two › T5. AT Protocol: the one lesson for (0) and (9)*

**The lesson (I).** Deletion broke permanent history, not scale, and it broke it
through the links, not the content. A hash of a record's bytes inside later
records made that record impossible to remove without redoing everything after
it. "Never rewritten" survives deletion only if no fact's validity rests on
another fact's plaintext. Lean (9) passes that test if the kept hash is over
ciphertext. It is a better plan than AT Protocol had.


---
**meaning L3005-3008 · R2 · CARRIED C5**

Three things lean (9) leaves open (all I; nobody in my camp has run this design,
and Datomic chose the other road, a rare physical cut with a permanent record, N,
§1.7).


---
**meaning L3009-3012 · R2 · ABOVE C5,P0**

- The key store is a second substance: mutable, not made of facts. Its backups
  are the hole. Back it up, and a deleted key can come back. Do not, and losing it
  loses every value. It needs live replicas and no archive. Lean (0) is then true
  of the log and false of the store beside it. Say so out loud.

---
**meaning L3013-3018 · R2 · CARRIED C5**

- Shredding does not reach plaintext that already crossed to a screen, a model,
  or a second store. Recorded crossings say where it went; nothing pulls it back.
  Ciphertext that leaves is safe only for a time: "Future breakthroughs in
  computing might allow going back and decrypting older content" (N,
  https://docs.ipfs.tech/how-to/privacy-best-practices/). Never ship ciphertext to
  another store.

---
**rama L655-656 · R2 · DISAGREES C5,P0**
*Round two › A. The leans, one by one*

**(0) Never rewritten. REJECT as stated.** Marz (R), Rama (N) and Kafka (N) all keep a door (round one). The deciding case: the thing that must go is not in the value slot. A token inside an external anchor in based-on. A private name inside the word of a key. A person's id in the by-whom of a refusal. Lean (9) lets only the value, and maybe by-whom, ever go. Under never-rewritten everything else stays in every replica and every backup. The camp keeps the operator's excision: the slot stays, the content goes, positions do not move. **I:** it would make each excision an audited act, itself a fact. Also sharpen: say what (0) covers. Under T1 it is a depot, and which depot matters.


---
**rama L663-664 · R2 · NEW-CASE C5**

**(9) Delete by destroying a key. SHARPEN.** Three points from the reference.
- The key store is itself stored. If it is a PState, it is in every backup until backup GC removes that backup (**CHECKED** `docs/22-backups.md:112-114`), and a restore brings PStates back as they were. "This reaches backups" is true of values and false of keys. The one case: in year three the module is restored to last night's backup, and every key destroyed since then is back. So the key store needs its own, shorter backup life, and deletions have to be re-applied after any restore. **I:** the deletion facts are what make that possible.

---
**rama L665-665 · R2 · NEW-REASON C5**

- The gate has to see plaintext to check shape against the grammar. Under T1(a) the offers depot is permanent, so the offer must already be encrypted when it is appended. So encryption happens at the door, and the gate needs the key on every admission. Per-value keys cannot be broadcast (T4). They have to sit with the cell.

---
**rama L666-667 · R2 · NEW-REASON C5**

- If the key sits in the same row as the fact, deleting it is an ordinary delete in mutable storage, which PStates allow anyway (**CHECKED** `docs/15-pstates.md:181`). The cryptography is needed for the depot copy and the backups, not for the row.


---
**rama L668-669 · R2 · CARRIED C5**

Marz (R) has said only that such encryption is "pretty easy to implement on top of Rama's existing primitives". Nobody in this camp has written about running it.


---
**rama L717-717 · R2 · ABOVE C5**
*Round two › C. What the leans change above the table*

2. **A second thing that is not made of facts.** Lean (9) creates a key store. It is mutable. It must be deletable. It is the most sensitive state in the system, and the gate reads it on every admission. "The one thing not made of facts is the runtime" stops being true.

---
**skeptics L388-389 · R2 · DISAGREES C5**
*Round two (2026-09-20): the leans, pressed from the skeptics › T6. The EDPB against lean (9)*

**What they recommend instead** (R, https://www.edpb.europa.eu/system/files/2025-04/edpb_guidelines_202502_blockchain_en.pdf). It is "not advisable to register personal data" on the immutable record as "clear text, encrypted or hashed data". Instead "personal data in those forms should be stored off-chain". What stays must be able to be "rendered anonymous". And they recommend "looking at other tools if the strong integrity property of blockchains is not needed". Lean (9) keeps two of those three forms on the record: ciphertext and a hash.


---
**skeptics L390-391 · R2 · DISAGREES C5**

**Two concrete failures** (I, from R parts). Ciphertext: Verraes (R), "Today's unbreakable encryption could be tomorrow's infosec disaster." Deciding case: a value written in year one under a cipher that is weak by year six, with its ciphertext in every log backup by design. Throwing the key away erased nothing that a later break cannot bring back. Hash: a plain hash over a small space of values (a diagnosis, a name, yes or no) is reversed by guessing, and equal hashes link facts to each other. CNIL's acceptable forms (R) are "a hash generated by a keyed hash function" whose key is deleted, or a commitment: "When a commitment scheme is perfectly hiding, deleting the witness [...] and the value committed is sufficient to render the commitment anonymous in such a way that it can no longer be considered personal data."


---
**skeptics L392-393 · R2 · DISAGREES C5**

**The lean, sharpened (I).** Personal values live outside the log in a store that can delete, encrypted under an owner key as a second line. On the fact sits a commitment or keyed hash whose key dies with the value. Base-layer values (papers, shared claims) stay inline, since promotion to base is a deliberate act of publication.


---
**skeptics L420-420 · R2 · DISAGREES C5**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(9): rejected as stated.** See T6.

---
**skeptics L434-434 · R2 · ABOVE C5**
*Round two (2026-09-20): the leans, pressed from the skeptics › B. Wrong questions*

- (9): not "which key scheme?" but "is any personal data, in any form, on the record that can never change?"

---
**skeptics L440-440 · R2 · ABOVE C5**
*Round two (2026-09-20): the leans, pressed from the skeptics › C. Above the table: what the leans change*

- One substance: moving values out of the log makes two stores, and the default's oldest seam, the dual write, returns.

---
**sync L66-74 · SHORT · DISAGREES C5,C1**
*1. The short version*

2. **Do not make a content hash the name of a fact.** Every team that did so
   froze a byte encoding forever, or paid to change it. SSB froze one
   JavaScript engine's JSON printing. Tezos says its context hash function
   "cannot be changed". did:plc has ids built on a legacy format that "will
   unfortunately be around forever". Dolt's format migration "changes all of
   the commit hashes". Git's move off SHA-1 is in its ninth year. If integrity
   is wanted, carry a tagged digest as an attribute, computed over the envelope
   plus the *hash of the value*. Matrix has done exactly this since 2019.


---
**sync L75-80 · SHORT · CARRIED C5,P0**

3. **Make the value separable from the envelope on day one.** This is the one
   thing every SSB successor changed (Bamboo, Gabby Grove, PPPPP, Willow), and
   Pijul and Matrix do the same. It is what makes deletion possible without
   breaking the log. Teams that promised "append-only forever" took it back:
   Aljoscha Meyer now calls his own design an "append-or-delete log".


---
**sync L3424-3426 · R2 · CARRIED C5**
*Round two: the leans, pressed › T3. A separable value, against lean (9)*

- *Gabby Grove: the hole has a fixed shape*, "so that the array the field is
  contained in has the same size in both cases". Bamboo keeps the payload's
  hash and size (R §2.15).

---
**sync L3427-3430 · R2 · CARRIED C5**

- *Pijul: header and contents are separate sections.* Contents may be absent,
  and fetched and verified later (R §3.6). Its rule for un-applying shows how
  it treats dependents: "A change can only be unrecorded if all changes that
  depend on it are also unrecorded in the same operation."

---
**sync L3446-3456 · R2 · DISAGREES C5**

**Against lean (9) as worded.** Nobody in my camp deletes by discarding a key.
All of them remove bytes. Keyhive states the limit of keys: "if someone has the
data and the symmetric key, then they have the ability to read that data" (R
§2.9). Deciding case: year-one ciphertext sits in a log that is never rewritten
(lean 0) and in every backup. In year twelve the cipher, or a key-handling bug,
fails. The log cannot be re-encrypted, because it cannot be rewritten. Leans
(0) and (9) collide. The camp's shape removes the collision: the log holds the
envelope and a random value id, with a keyed digest; values live *beside* the
log in a store that may be rewritten, re-encrypted and erased. Encryption per
value is then defence in depth, not the deletion mechanism. (I)


---
**sync L3457-3466 · R2 · NEW-REASON C5,C8**

Two smaller presses. "The fact keeps the value's hash": for a short value a
plain hash *is* the value; keyed, or gone. (I) "Maybe by-whom may go": if
by-whom is an opaque actor id and the name is a separate fact, by-whom never
needs to go; erase the name fact's value. Pijul is built this way (R §3.6), and
Fossil's 'bertina' shows what happens when the name is inside the record (R
§3.3). What the lean leaves out: a based-on list can identify a person, and so
can an entity and key alone ("this person has a fact under :diagnosis" says
enough with the value gone). Erasure may have to reach a cell's visibility, not
only a value. (I)


---
**sync L3548-3556 · R2 · DISAGREES C5**
*Round two: the leans, pressed › A. The other leans*

**(0) Never rewritten.** Reject as a physical claim; keep as a logical one.
Hipp lives by never-rewrite and says the bytes are always editable (R §3.3).
Meyer withdrew "append-only" (R §2.15). Deciding case: year six, the storage
encoding or the cipher must change under ten million papers and years of
facts. Dolt did it by re-encoding everything and proving the logical rows
unchanged (R §3.4). Tezos cannot (N §3.5). The lean says (0) makes (9) and (10)
now-or-never. Keeping values beside the log (T3) is exactly what makes (9) *not*
now-or-never.


---
## Says the same as the ledger (counted, not copied)

- C5 · datalog · 2: L608-608, L859-864
- C5 · log · 2: L380-381, L655-656
- C5 · meaning · 3: L1094-1102, L2903-2908, L3019-3022
- C5 · rama · 1: L584-585
- C5 · sync · 3: L1345-1347, L3402-3410, L3411-3413

