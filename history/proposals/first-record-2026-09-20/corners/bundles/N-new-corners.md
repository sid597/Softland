# N new corners: main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L45-46 · SHORT · NEW-CORNER C3,C5**
*1. The short version*

**5. The regrets are about what was not written down at the time.** Nubank, on hindsight: "one thing that we do not add and I would have liked to add, a customer identifier. Because, if every transaction had an identifier that could point to the actual customer that owns that data, things like splitting databases for sharding would have been much, much easier." [N-QCON]. And Lucas Cavalcanti: "what I regret the most, is to have implicit operations that are not stored into the database, or that don't have origination information." [N-POD]. Nobody in this camp regrets recording too much provenance. They regret recording too little, and they regret putting firehose data in the fact store.


---
**datalog L607-607 · ABOVE · NEW-CORNER C1**
*7. What this camp would question above the table*

2. **Sameness, not only uniqueness.** Random ids guarantee two things never clash. They also guarantee the same paper ingested twice becomes two entities. Datomic's unique-identity attributes, checked at the gate, are the camp's tool.

---
**datalog L611-614 · ABOVE · NEW-CORNER E3**

6. **A decision on valid time.** Datomic: an attribute. XTDB: an axis on every record. A field seeded from papers has late corrections and "with effect from" built into its subject. Either choice is defensible. Not choosing means Datomic's by default, and then Waeselynck's essay describes what follows.

---


---
**datalog L834-834 · R2 · NEW-CORNER -**
*Round two › R2.1 The tailored questions › T1. The unit of saying*

2. **Per-fact because-of.** A saying has one cause. The failure mode is a lane that flushes a buffer of unrelated items as one saying, for speed. R: Datomic's import guidance is about batching and pipelining for throughput [D-BEST], and its provenance example is one source file per transaction [D-TXDATA]. I: the convention must say that a saying has one cause, because the gate cannot detect a saying that has two.

---
**datalog L841-841 · R2 · NEW-CORNER -**

- R: Shortcut, after doing it: "Interpreting an individual transaction is out of scope for this post, but it is by no means trivial. You could use additional audit fields to provide more detail." [SC17]. Who and where are easy. *What act this was* is hard to read back out of a bag of facts, unless the act is named on the saying. Sid's because-of, plus the tool's identity on the saying, is the additional field they wished for.

---
**datalog L842-842 · R2 · NEW-CORNER C3,C5**

- R: Nubank's two regrets, quoted in 3.3. Sayings with no origin ("implicit operations… that don't have origination information") and no owner on every transaction. Both are about too little on the saying. Neither is about the saying being too coarse.

---
**datalog L871-872 · R2 · NEW-CORNER C5**
*Round two › R2.1 The tailored questions › T2. XTDB v1 against leans (9) and (1)*

3. *Which keys?* A person asks to be forgotten after three years. There are 40,000 values about them, in the base and in other people's layers, written by hundreds of agents. Keys per value do not tell you which values are about this person. Nothing can tell you later unless it was marked at admission. This is Nubank's regret carried over: "if every transaction had an identifier that could point to the actual customer that owns that data…" [N-QCON]. The sharpening: wrap each value's key under a key for the subject or owner, named when the fact is admitted. Then forgetting a person is one key.


---
**datalog L964-965 · R2 · NEW-CORNER C5**
*Round two › R2.2 Ask A: the leans, one by one*

**(9) Value under a per-value key; delete by destroying the key.** See T2. Stands as the best shape anyone here has found. Four sharpenings. Hash the ciphertext. Give destroyed keys tombstones. Wrap value keys under a key for the subject or owner, named at admission. Treat the derived indexes and the key store's backups as the hard part, and build a checker for both.


---
**datalog L1016-1016 · R2 · NEW-CORNER -**
*Round two › R2.4 Ask C: what they would change above the table*

2. **A representation of "no longer so".** I did not see one in the envelope as briefed. R: Hickey's stated reason for not using RDF is that "without a temporal notion or proper representation of retraction, RDF statements are insufficient for representing historical information" [H-IM]. Sid has the temporal notion. The camp offers two lived answers for the other half. Datomic has an explicit added-or-retracted part on every fact [D-MODEL]. XTDB: "Retractions in XTDB are implicit and deleted documents are simply replaced with empty documents" [X-FAQ]. **Case:** a person un-says a relation because it was wrong. What is written? If the answer is *a later version with an empty value*, then every grammar must admit empty for ever, which is Hickey's "Maybe sheep" at the level of values. If it is a part of the fact, it is uniform and needs no grammar to agree. Either can work. It has to be chosen before the first record.

---
**log L653-654 · R2 · NEW-CORNER C5**
*Round two: the leans, pressed › T1. The four promises against leans (0) and (9)*

**Press (9): a key-store restore un-deletes.** A CT log died because "its database was rolled back during a botched backup restore" (R, Ayer). For a key store, a rollback resurrects destroyed keys. I: every key destruction must itself be a fact in the store, so that after any key-store restore the destroyed list is replayed. "Reaches backups" is true only with that.


---
**log L667-668 · R2 · NEW-CORNER P0**

Nothing else is needed now. The tree, the signatures and the witnesses can all come later, because a Merkle tree is an index over positions and hashes. One caution: provability begins at the first checkpoint an outsider holds. If early history should ever be provable, anchor a hash of the partition heads somewhere outside, early and cheaply.


---
**log L720-720 · R2 · NEW-CORNER E5**
*Round two: the leans, pressed › A. The other leans*

- **(4) Sharpen.** Three marks, not two: depends-on, how-she-got-here, trigger. The trigger is Young's immediate cause (R), which the nine parts otherwise lack. Reject "the tool may correct" if it means after landing. Under (0) a correction is a second fact that every later walk must find. Let the tool mark in the offer; let the floor fill gaps and say that it guessed.

---
**log L739-740 · R2 · NEW-CORNER E1,C4**
*Round two: the leans, pressed › C. Above the table: only what the leans change*

- **One store.** "A layer has one home store" admits many stores. A store id on every verdict from record one then stops being insurance and becomes necessary.


---
**meaning L3062-3071 · R2 · NEW-CORNER C1,C4**
*Round two › A. Leans my camp would reject or sharpen*

**(2): sharpen ingest ids.** "Source plus form" gives one paper two ids when it
arrives both as a PDF and as publisher XML. At ten million papers merges are
certain, and no lean says how a merge is said. Pointers to the losing id can
never be rewritten. Who: Wikidata, "Under no circumstances should redirects be
deleted or repurposed" (N, §2.5); Halpin and Hayes on sameness declared by
strangers (R, §2.6). Say it now: a same-as fact, strict policy in base, free in
personal layers, resolved at read time. Also, a derived id is a guessable id.
Fine for a DOI. A leak for a person. Personal sources need a keyed derivation or a
minted id (I).


---
**rama L657-658 · R2 · NEW-CORNER C1,C4**
*Round two › A. The leans, one by one*

**(2) Entity id. STANDS.** RPL (N): made by the offerer, 128 bits. Who already lives this way: RPL's own guidance; Marz in 2010, whose ids came from the source (R). Sharpen the ingest-derived id: it names the source record, not the thing. The case: ten million papers, and the same paper arrives as a DOI record, an arXiv record and a PubMed record. Three entities, for life. Marz's id unions listed several kinds of id for one node (R) and settled sameness by computation (from memory of the book, unverified). An id for life cannot be merged later. So "same as" has to be a key from the first day.


---
**skeptics L425-425 · R2 · NEW-CORNER C8,E4**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(8): stands, with a hole.** It is what Hamilton and Pavlo ask for (R). FlightTracker (R): "Request endpoints may be invoked before login or after logout". Deciding case: a scheduled tool fires at 3 a.m. for a person who is logged out. There is no session to verify against. Actors that are not interactive need their own credential path from the first record.

---
**sync L3674-3681 · R2 · NEW-CORNER E1,A2**
*Round two: the leans, pressed › C. Above the table: only what the leans change*

3. **"The runtime as the one non-fact" now meets many gates.** With gates near
   people, several builds of the runtime admit facts at the same moment. Bayou
   required every server to enforce the same resource bounds so that checks
   fail the same way everywhere (R §2.1). Deciding case: a device gate on an
   old build admits a fact into a personal layer that the base's newer gate
   would refuse on shape. The fact is in the store forever, and its promotion
   fails later for a reason nobody recorded. So the gate's build belongs in
   the grounds of every verdict. (I)

