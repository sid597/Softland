# P0 premise (0): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L190-191 · BODY · DISAGREES P0**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- INFERRED, from the above. Rama's depot migration is a function applied to every record. This camp would allow it for exactly one purpose: turning a record into a tombstone that says *something was here and was forgotten, by this request*. They would forbid it for repair or reshaping. Repair is a new fact. The convention to fix before the first record is the *shape of a tombstone* and the rule that a migration may only produce one.


---
**datalog L327-328 · BODY · CARRIED P0,C5**

- REPORTED. Trimmed: only by declared exception (no-history attributes, excision), plus garbage collection of old *index* segments, which are derived. "It should not be surprising when you move to an immutable process where new information requires new storage that you end up with… garbage on disk, and garbage collection for disk." [H-DD]. The log itself is never trimmed in Datomic.


---
**datalog L354-354 · BODY · DISAGREES P0**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

- **Huahai Yang** (Datalevin): "Database is where immutability may not be a good fit, at least not all the time." [Y-HN]

---
**datalog L469-470 · BODY · CARRIED P0,W1**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.3 What they changed, and what bit them*

4. **Rebuilding indexes by replaying the log became unbearable.** A production user, 2023: about 600 GB and 100 million transactions, and a minor upgrade meant "a full reindex of the main 'golden' data store taking days to weeks." Jeremy Taylor's reply: "XTDB 2 will compute and maintain incremental indexes on-the-fly based on the raw data… This also means the transaction log is now ~ephemeral in the new architecture (no more event-sourcing-style replays required, ever)." [X-HN23]. Version 1 had said the opposite: "The event-log that Crux uses is the golden store of data, with Crux leveraging Kafka's infinite retention capability." [X-INTRO]


---
**datalog L502-502 · BODY · CARRIED P0**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.6 Who they disagree with*

- **Their own version 1**, on whether the log is the permanent record.

---
**datalog L504-505 · BODY · ABOVE P0**

- **Event sourcing as a habit**: "Event sourcing should be for, well, events — not a bandaid on the fact that almost every major database throws out data with each UPDATE operation." [X-DD7]


---
**datalog L522-523 · BODY · DISAGREES P0**
*5. The leads › 5.1 Instant (Stepan Parunashvili, Joe Averbukh)*

**Not append-only.** REPORTED from code. Triples are upserted and deleted in place. There is no transaction column on a triple and no history table. [I-TRIPLE]. Before building, Parunashvili wrote: "And god forbid an error happens and we accidentally delete data. In a world of facts there would be no such thing — you can just undo the deletions. But alas, this is not the world most of us live in." [I-SPEC]. What shipped is undo for deleted *attributes* only: "we implemented soft deletes at the column level. Even if a rogue agent deletes your columns, you can undo it." [I-ARCH]. They wanted the world of facts and did not build it. The one place they added undo was the place agents hurt them.


---
**datalog L550-551 · BODY · DISAGREES P0**
*5. The leads › 5.3 Huahai Yang (Datalevin)*

> "To keep things simple and familiar, Datalevin behaves the same way as most other databases: when data are deleted, they are gone." [Y-README]


---
**datalog L552-553 · BODY · DISAGREES P0**

> "Database is where immutability may not be a good fit, at least not all the time. In many use cases, database is where application state resides, hence a mutable database is a better fit." — 2025 [Y-HN]


---
**datalog L554-555 · BODY · DISAGREES P0,C1**

An earlier README put it flatly: "Datalevin is not an immutable database, and there is no 'database as a value' feature. Since history is not kept, transaction ids are not stored." [Y-OLD]. His README cites Waeselynck's essay and the Jepsen report as his reasons.


---
**datalog L558-561 · BODY · ABOVE P0**

His value to Sid is one distinction: *application state* is not *information*. It is Hickey's own line ("Datomic is not about keeping stuff") drawn by someone who thinks most data falls on the other side of it.

---


---
**frontiers L93-93 · BODY · DISAGREES P0**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(0) never rewritten, or never lost?** REPORTED: Materialize compacts. "Compaction advances the `since` frontier". What is promised is correct reads between `since` and `upper`, and a refusal below `since`. INFERRED: the camp's invariant is "never answer wrongly", not "never rewrite". If Sid ever trims, a `since` per collection keeps the map honest.

---
**frontiers L96-96 · BODY · DISAGREES P0,E7**

- **(14) the hand.** INFERRED from lateral joins: what a person is looking at is an input collection that drives which answers are maintained. It is data. It is also high-rate and short-lived, so it wants aggressive compaction.

---
**frontiers L103-104 · BODY · DISAGREES P0**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.5 What resembles Sid's situation, and what differs*

Differs: Materialize's inputs are other systems' transactions; Sid's are offers from people and agents, and the gate itself decides. Materialize compacts history on purpose; Sid forbids rewriting. Materialize's views are SQL over relations; Sid's tools are opaque bodies behind pattern signatures, so determinism is a promise, not a property. Materialize serves one tenant at a time per environment; Sid wants one store for the planet. Materialize holds no per-fact provenance; Sid wants it on every fact.


---
**frontiers L141-142 · BODY · NEW-REASON P0**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.2 The reasons, in Brandon's words*

**State belongs in a database with a log.** An early piece, and the one place this camp speaks for Sid's premise. REPORTED: the questions that matter are "When did this state change?", "What caused it to change?", "Did this invariant ever break?", "How did this output get here?", and "I propose that if we were to manage state more like a database and less like a traditional imperative language then understanding and debugging programs would become easier." ([Local state is harmful, 2014](https://www.scattered-thoughts.net/writing/local-state-is-harmful/))


---
**frontiers L351-352 · BODY · DISAGREES P0**
*7. Question by question: what this camp says*

**(0) The log: never rewritten, or only never lost?** The camp's invariant is neither. It is "never answer wrongly". Materialize compacts history and moves a recorded `since`; reads below it are refused, not faked (REPORTED). The reclocking doc wants new knowledge added as "sidecar metadata, rather than a rewriting of the source data" (REPORTED), which supports never-rewrite. INFERRED: if Sid holds never-rewrite, the camp has no quarrel. If Sid ever trims, give every collection a `since` fact from the start, so an as-of read can tell "nothing was there" from "that was trimmed".


---
**frontiers L385-386 · BODY · DISAGREES P0,E7**

**(14) The hand.** INFERRED: what is on screen is the demand signal and belongs in the store as an input. It is also the first data to trim. Shown and looked-at are different facts. A shown answer should carry whether it was complete for its as-of.


---
**frontiers L389-390 · BODY · DISAGREES P0**

**(13) Storage.** INFERRED: the log is the truth and indexes are rebuildable, which matches Rama. The camp stores sorted immutable batches of `(data, time, diff)` and merges them. Trimming is `since`. Backups are of the log plus the sidecar maps, which are part of the truth and not derived.


---
**log L80-81 · BODY · NEW-REASON P0**
*Voice by voice › 1. Pat Helland*

- On cost: "Increased fidelity of memories implies increased cost!" (*Memories, Guesses, and Apologies*, blog, 2007.)


---
**log L128-128 · BODY · DISAGREES P0**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- *"Forever" met production.* "Consensus is forever... until it's not: Deletion of arbitrary entries is typically quite difficult in conventional consensus protocols. However, with virtual consensus, we can delete an entry simply by changing the metadata of the VirtualLog. Similarly, altering written entries is possible via remapping. We found this kind of surgical editing capability useful when faced with site-wide outages: on one occasion, a "poison" entry caused hangs on all learners processing the log." (Delos, OSDI 2020.)

---
**log L129-129 · BODY · CARRIED P0,C5**

- *Old segments go cold, not away.* Delos moved "older segments to a Loglet layered on cold storage (BackupLoglet)", which gave "point-in-time restore".

---
**log L142-142 · BODY · DISAGREES P0**

- (0), (9) REPORTED: they trim, and they edited written entries when production demanded it. INFERRED: a store that promises never to rewrite still needs a planned, recorded way to neutralize a poison record.

---
**log L145-146 · BODY · CARRIED P0,C5**

**5. Resembles and differs.** Resembles: one log as the only interface; everything else is a view; writes that carry their reads; many clients; years in production; the people who built it wrote down what went wrong. Differs: these are redo logs. Entries are commands for a state machine, so trimming is natural and nobody needs an entry from five years ago. Write rates are control-plane rates. There is one operator and no stranger.


---
**log L154-154 · BODY · NEW-REASON P0**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- Why no edits, ever: "And if you allow a single update… Well, your data is now definitely maybe immutable. Also known as mutable." And: "Immutability is immutable. The moment you allow a single edit, everything becomes suspect." And the legal bar: "The bar for being able to submit your audit log to a court of law is that you must be able to rebuild your current state from your audit log." (chapter "Why can't I update an event?")

---
**log L165-165 · BODY · DISAGREES P0**

- His own named regret, on rewriting a store by copying it: "Admiteddly, I am likely to blame for the current popularity of Copy-Replace." "In retrospect, I did not speak enough to the practical downsides of the approach." "Copy-Replace is the nuclear-option of versioning." Yet: "I have found a huge number of them have given up on most of the other versioning strategies and just use Copy-Replace." The field went to the rewrite anyway.

---
**log L167-167 · BODY · NEW-CASE P0**

- How accountants really cope with long histories: they close the year. "In the accounting office there were 10 accounting systems. Nine were marked by their year and were read-only". The cost: "Running a projection that covered two years worth of information from the accounting system was a real pain."

---
**log L173-173 · BODY · DISAGREES P0**

- (0) REPORTED: never edit; and yet most teams end up copying and rewriting whole stores. INFERRED: a design that gives no cheap, honest way to evolve will be rewritten wholesale by its own users.

---
**log L196-196 · BODY · CARRIED P0,C5**
*Voice by voice › 4. Martin Kleppmann*

- His own definition of a log allows discarding: "Besides appending, the log may allow old events to be discarded". (*Online Event Processing*, 2019.)

---
**log L225-226 · BODY · ABOVE P0**

**6. Disagreements.** With dual writes. With pure append-only designs that cannot delete. With his own earlier single-log position, on cost. Implicitly with Sid's premise: local-first holds that "we treat the copy of the data on your local device … as the primary copy."


---
**log L232-232 · BODY · DISAGREES P0,C5**
*Voice by voice › 5. Certificate Transparency, Trillian, and their descendants*

- The promise: "A log is a single, ever-growing, append-only Merkle Tree". (RFC 6962.) "Once an entry has been accepted by the log, it can never be removed or changed." (Eijdenberg, Laurie, Cutter, *Verifiable Data Structures*, 2015.)

---
**log L233-233 · BODY · CARRIED P0**

- Never lost is an operations problem: "the log absolutely cannot afford to lose the certificate once it has issued an SCT." (Ben Laurie, *Certificate Transparency*, ACM Queue 2014.)

---
**log L239-240 · BODY · CARRIED P0**

- The untrusted operator: "It can't remove an observed record without detection." (Russ Cox, *Transparent Logs for Skeptical Clients*, 2019.)


---
**log L242-242 · BODY · CARRIED P0**

- *A verifiable log cannot repair itself.* Chrome's CT team, July 2021, on DigiCert's Yeti2022: "The issue was tracked down to a bit flip in a single leaf hash". "Because the bit flip occurred in the leaf hash itself, this unfortunately is a non-recoverable error for this CT Log. Fixing this entry would require generating a SHA-2 preimage for the bit-flipped leaf hash". "Bit flips such as this can occur in extremely rare circumstances due to hardware faults or even cosmic rays." Andrew Ayer: "There is no way for the log operator to fix this problem". "Yeti 2022 is toast."

---
**log L243-243 · BODY · CARRIED P0**

- *A restore from backup is a rollback, and a rollback is a fork.* Ayer: "In 2017, a log failed because it violated the append-only property after its database was rolled back during a botched backup restore."

---
**log L249-249 · BODY · CARRIED P0,C1**

- *One small thing must never roll back; the rest may.* Sunlight keeps a tiny global checkpoint table that "must never be changed or modified". Its duplicate cache is the opposite: "This part of Sunlight can tolerate data loss".

---
**log L254-255 · BODY · CARRIED P0**

- *Who really verifies.* Eric Rescorla, 2023: "no browser ever implemented gossip". "CT provides fairly limited public verifiability." Chrome's auditing "really depends on trusting Google". His verdict: "what we actually have is a countersignature scheme and that the Merkle tree machinery is unnecessary overhead". "The problem is that it's expensive futureproofing, both in terms of protocol complexity and in terms of operational brittleness." The live repair is witness cosigning: independent parties check a checkpoint against what they saw before and countersign it.


---
**log L257-257 · BODY · CARRIED P0**

- (0) INSTITUTIONAL: they promise all four things (never lost, never different, never re-ordered, provable), and their incident record is the price list for the fourth.

---
**log L263-263 · BODY · CARRIED P0,C7**

- (13) INSTITUTIONAL: a reserved, signed-over extension slot; one canonical encoding fixed forever; sharding by time so whole logs can retire; a restore is a fork unless the head is protected outside the backup.

---
**log L269-270 · BODY · CARRIED P0,E1**

**6. Disagreements.** Rescorla against the CT designers, on whether the Merkle machinery earns its cost. The redaction fight (certificate authorities wanted it; browser vendors refused). Scalog's point about deciding metadata before persistence is the same lesson CT learned with merge delay, from a different direction.


---
**log L308-309 · BODY · CARRIED P0,C5**
*Voice by voice › 7. Phil Bernstein's Hyder*

**5. Resembles and differs.** Resembles: optimistic writes checked against what was read; one log; every reader computes the same state. Differs: a research system on special hardware inside one data centre; the log is a redo log and is garbage-collected; no strangers, no attribution, no deletion problem.


---
**log L314-315 · BODY · CARRIED P0,C5**
*Voice by voice › 8. Amazon Aurora, and what AWS did next*

**1. What they built and chose.** One writer instance and up to fifteen read replicas over a shared, six-way replicated redo log. The instance ships only log records. Storage nodes build pages from the log and garbage-collect the log behind the readers.


---
**log L331-331 · BODY · CARRIED P0,C5**

- (0), (13) REPORTED: a redo log is garbage-collected once pages exist and a backup is cut. Even the "rewrite" of the ragged tail is an appended, epoch-stamped record.

---
**log L336-337 · BODY · CARRIED P0,C5**

**5. Resembles and differs.** Resembles: one writer stamping positions; readers that lag; "the log is the database" in so many words. Differs: a redo log for pages, garbage-collected, with no meaning at the record level, one tenant per volume, and a history horizon measured in minutes or hours.


---
**log L347-347 · BODY · CARRIED P0,C5**
*Voice by voice › 9. Two short notes on voices the brief did not list*

- "By default, depots permanently store all data appended to them." Trimming is an option.

---
**log L363-364 · BODY · ABOVE P0**
*Question by question: what this camp would say › (0) The log itself: never rewritten, or only never lost?*

The camp would first split the question. There are four promises, and sources differ on which they make.


---
**log L365-365 · BODY · CARRIED P0**

1. **Never lost.** This is operations and money. Laurie: "the log absolutely cannot afford to lose the certificate once it has issued an SCT." Helland: "Increased fidelity of memories implies increased cost!"

---
**log L366-366 · BODY · CARRIED P0,C5**

2. **Never different.** The same id never returns different content. Absent is allowed. Helland: "it will never return data other than the original contents." Young: "Immutability is immutable. The moment you allow a single edit, everything becomes suspect."

---
**log L367-367 · BODY · NEW-CASE P0,C2**

3. **Never re-ordered or re-numbered.** Rama keeps offsets through a migration. Young shows why it matters: after a live copy-and-replace, a read model rebuilt to the same position may differ from the one built the first time, because the two "have seen different versions of history".

---
**log L368-369 · BODY · DISAGREES P0,C5**

4. **Provable to strangers.** Only CT promises this. The price is that the log can never repair itself (Yeti2022), can never be restored from a backup (the 2017 failure), and can never remove anything.


---
**log L370-371 · BODY · CARRIED P0,C5**

Who keeps the log forever depends on what the log holds. Redo logs are trimmed: Tango's forget call, Aurora's garbage collection, Delos ("continuously being trimmed or moved to backup storage"), Kafka by default, Hyder's copier. Record logs are kept: CT, event stores, ledgers. Sid's store is a record log.


---
**log L372-373 · BODY · DISAGREES P0**

Even the keepers bend at the physical level. Helland says it outright: "semantically immutable but can be physically changed." Delos edited written entries to survive a poison record. Kleppmann lists "periodically rewriting the log" as a legitimate way to meet privacy law. Young regrets that most teams end up rewriting whole stores. The Overeem study's vendors refuse to offer in-place edits at all, and one team zipped and encoded its events just to make editing hard.


---
**log L374-375 · BODY · DISAGREES P0,W2**

**For the first record (INFERRED).** Promise 1, 2 and 3. Reserve the right to change bytes without changing meaning, which is what a Rama depot migration is when used well. Make "meaning unchanged" checkable, which needs one canonical byte form and a hash from the first record; a hash added later proves nothing about the years before it. Keep promise 4 possible: a Merkle tree is a derived index over ordered, byte-stable entries, so it can be built later if and only if those two properties held from the start. Write down the one exception class (erasure, and neutralizing a poison record) and make each use of it a fact.


---
**log L514-515 · BODY · CARRIED P0,C5**
*Question by question: what this camp would say › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

REPORTED on trimming. Nobody who keeps a record log trims it. Everybody trims or tiers something: Tango's forget call, Aurora's garbage collection, CT's time-sharded logs that retire whole, Delos's cold-storage loglet, Young's year-end close. Kreps on what compaction destroys: "we can no longer recreate all previous states". Marz on why keeping the last few versions is not enough. Rama: depots keep everything by default, and trimming is a setting.


---
**log L516-517 · BODY · CARRIED P0**

REPORTED on backups. For an append-only store a restore is a rollback, and a rollback broke a CT log. Sunlight's rule is the clean statement: one tiny record of the latest head "must never be changed or modified", while the duplicate cache "can tolerate data loss". Delos gets point-in-time restore by tiering old segments. Helland's line applies to all of it: fidelity of memory is bought.


---
**log L518-519 · BODY · DISAGREES P0,E1**

**For the first record (INFERRED).** Plain maps of named parts, not positional tuples and not language classes. An envelope version. One extension slot that is inside whatever is hashed from the first day; an extension added outside the hash later cannot be trusted the same way. One canonical byte form for hashing, independent of how Rama stores the record. Never trim the fact log; do trim or tier refusals, session-grain motion, and indexes. For backups, decide which one small thing must never roll back (each partition's head: position and hash), keep it outside the backup domain, and accept that after a restore some acknowledged facts are gone. Client-minted ids and an idempotent gate let clients offer them again. Helland predicted the situation: "Your partner may experience amnesia".


---
**log L526-527 · BODY · DISAGREES P0**
*For the group › 7. The voices that matter most for Sid, who I dropped, who i*

**Mahesh Balakrishnan matters most.** He is the only person here who ran "one log is the interface to everything" in production for years and then wrote down what went wrong. His corrections land directly on Sid's table: reads carried in the record, verdicts written into the log, how far the writer had seen as an envelope field, code upgrades through the log, the map-shaped entry, the surgical edit of a poison record, the blast radius of one global order. His systems differ from Sid's in one large way, which is that they are redo logs and so they trim, and that difference is easy to keep in view.


---
**log L530-531 · BODY · CARRIED P0,C7**

**The Certificate Transparency operators matter third:** Laurie for the design reasons, then Ayer, Valsorda, Let's Encrypt and Rescorla for the decade after. It is the only append-only log at planet scale that strangers check, and its incident record prices each promise. The reserved extension slot is the single most transferable trick in this report.


---
**log L573-573 · BODY · DISAGREES P0,C1**
*The second store: what each source implies*

- **Young.** Copy-and-transform makes a second store as a versioning tool, and its trap is ids that collide with different content.

---
**meaning L100-106 · BODY · NEW-REASON P0**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

**Turn 2 — Hickey, 11945869, 14:34**

> Data like that sentence? Or all of the other sentences in this chat? I find
> 'data' hard to consider a bad idea in and of itself, i.e. if data ==
> information, records of things known/uttered at a point in time. Could you
> talk more about data being a bad idea?


---
**meaning L258-267 · BODY · NEW-REASON P0**

> The defining aspect of data is that it reflects a recording of some
> facts/observations of the universe at some point in time (this is what 'data'
> means, and meant long before programmers existed and started applying it to
> any random updatable bits they put on disk). A second critical aspect of data
> is that it doesn't and can't *do* anything, i.e. have effects. A third aspect
> is that it does not change. That static nature is essential, and what makes
> data a "good idea", where a "good idea" is an abstraction that correlates with
> reality - people record observations and those recordings (of the past) are
> data. [...] Interpretation of those observations is completely orthogonal.
>

---
**meaning L377-378 · BODY · NEW-REASON P0**
*Part one — the exchange › 1.3 What each actually claimed*

2. Three defining properties. It records the past. It "doesn't and can't *do*
   anything, i.e. have effects". It "does not change".

---
**meaning L410-417 · BODY · NEW-REASON P0,C7**
*Part one — the exchange › 1.4 Where they talked past each other*

**The problem each is solving.** Kay is working on transmission between
parties who have never met, across space ("Intergalactic") and time (the 1978
image). Kay's test is whether the receiver can recover what was meant. Hickey is
working on record-keeping. Hickey's test is whether the past stays put: immutable,
harmless, reproducible. These are different jobs. A store that never rewrites
has to do both. It is a record, and it is a message to readers who do not exist
yet.


---
**meaning L1103-1115 · BODY · DISAGREES P0,C6**
*Part two — the people › 2.4 IPFS and IPLD (Juan Benet), and what AT Protocol did wit*

**AT Protocol walked back permanent history because of deletion.** This is the
strongest field evidence on (0) and (9).

- REPORTED (Bryan Newbold, atproto discussion 1410, 2023): "A problematic area
  [...] has been handling deletions of records [...] The current solution to
  full purges of deleted records has been to 'rebase' the repository [...] The
  problem with this is that rebases are 'expensive' for all the downstream
  services [...] which results in deleted content being available publicly via
  specially crafted API calls, which breaks human intents and expectations."
  The change: "There would no longer be a public, enumerable commit history."
  The back-pointer became a clock value: "It is intentionally not a strong
  reference". October 2023: "We truncated history for all repos (eg, prev=null)
  during migration."

---
**meaning L1116-1119 · BODY · DISAGREES P0,C5**

- INSTITUTIONAL (repository spec): "Record deletion is supported without leaving
  a trace or 'tombstone' of previous contents." And a hazard of copies:
  "previously-deleted records re-appearing via CAR import from an unrelated
  account."

---
**meaning L1120-1120 · BODY · CARRIED P0,C5**

- INSTITUTIONAL (2025): "sync v1.1 relays are now 'non-archival'."

---
**meaning L1138-1144 · BODY · DISAGREES P0,C5**

**Transfer.** IPFS and AT Protocol have many copy-holders that no one controls.
Sid has one store, so cutting a fact out is possible in a way it is not for
them. What carries over whole: the encoding lesson, the algorithm-tag lesson,
the salt, and the warning about strong back-pointers. Disagreement inside this
line: Benet's "Objects are permanent" against Newbold's removal of history
because permanence "breaks human intents and expectations".


---
**meaning L1223-1228 · BODY · ABOVE P0**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

- The history tables. REPORTED (Pintscher, 2026 request for comment): "Wikidata
  is currently stretching the limits of what it technically and socially can
  hold." "There are database tables (revision and terms table) that are near
  the limits of their scalability [...] The worst case scenario here is taking
  down not just Wikidata but also Wikipedia". And the social limit: "a single
  active editor has to oversee nearly 10.000 Items".

---
**meaning L1567-1571 · BODY · CARRIED P0**
*Part two — the people › 2.8 David Reed's pseudo-time, and Croquet*

**What Reed conceded.**

- Never rewritten had two exceptions. "With two exceptions, versions are never
  modified once created." One was the read mark. Reed then designed both away so
  that versions could live on write-once media.

---
**meaning L1572-1575 · BODY · DISAGREES P0,C2**

- Keep everything forever does not hold. "Since all versions of an object are
  stored forever, the total storage used by the system will increase at a rate
  proportional to the update traffic." Reed adds a window: "READs and WRITEs whose
  pseudotime is older than T - d are rejected."

---
**meaning L2060-2067 · BODY · DISAGREES P0**
*Part three — substrates › 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tri*

**Things survive because someone pays.** INSTITUTIONAL (Udanax Gold class
docs): a work has "sponsors [...] All of the Clubs which are sponsoring this Work
to keep it from being discarded." An unsponsored work "might have been
discarded". Nothing is deleted by command. Sponsorship is on Hewitt's list of
principles too. For (13) and the "economy" in Sid's scale: storage that lasts for
years at planet scale has to be somebody's cost, and their model made that a
first-class relation.


---
**meaning L2138-2142 · BODY · DISAGREES P0**
*Part three — substrates › 3.6 An addition: Urbit, on the runtime that is not a fact*

- The honest part. REPORTED (whitepaper, 2016): "Everything but the lifecycle
  function can upgrade itself from source code in the input stream." And: "sometimes
  we still reboot the universe (declare a flag day, or "continuity breach") for a
  particularly gnarly one. The event log is not at all above suspicion."


---
**meaning L2163-2172 · BODY · DISAGREES P0**
*Part four — Sid's questions, hung on the parts of the fact › (0) The log: never rewritten, or only never lost?*

**Camp.** Nobody here kept "never lost". Datomic cuts, rarely, and the cut is a
permanent record (1.7). AT Protocol began with permanent signed history and tore
it out because deletion "breaks human intents and expectations" (2.4). Reed
conceded that keeping every version forever does not hold, and added a window
(2.8). Xanadu let the owner withdraw, and let unsponsored work lapse (3.5).
Wikidata suppresses, visibly (2.5). Webstrates never pruned, and offers one door:
destroy the whole document (3.3). Urbit declared "continuity breaches" (3.6).
Nanopublications alone never remove anything, and they hold public science only
(2.6).


---
**meaning L2176-2182 · BODY · ABOVE P0,C5**

**My read.** The pair in the question is the wrong pair. The two promises that
can both be kept are: *the meaning of an admitted fact never changes*, and *a
value can be cut out, by rule, leaving a visible scar that is itself a fact*.
Decide the shape of the scar before the first record: what stays (the envelope
and the fact's id), what goes (the value), who may cut, and that the cut cannot
itself be cut.


---
**meaning L2255-2266 · BODY · DISAGREES P0,C5**
*Part four — Sid's questions, hung on the parts of the fact › (9) Value: never removed, so how is one deleted, backups inc*

### (9) Value: never removed, so how is one deleted, backups included?

**Camp.** Three different acts, often confused. *Saying* a fact is wrong or
withdrawn is a new fact: nanopublication retraction, Wikidata's deprecated rank
with a reason (2.6, 2.5). *Hiding* is policy: Wikidata suppression keeps the
content and narrows who sees it (2.5). *Cutting* removes the content: Datomic
excision (1.7), AT Protocol deletion "without leaving a trace" (2.4). On copies:
IPFS cannot delete what others hold, and a hash lets a surviving copy be found and
verified (2.4). AT Protocol's only rule for copies is operational: mirrors must
follow deletions "within seconds or minutes", and deleted records have re-appeared
through imports (2.4).


---
**meaning L2529-2540 · BODY · DISAGREES P0,C7**
*Part four — Sid's questions, hung on the parts of the fact › (13) Storage*

### (13) Storage

**Camp.** Plain open maps. Protobuf's unknown-fields reversal (2.2). Hickey's
generic processors (1.7). Self's hidden maps show the runtime can still store
shapes compactly where nobody sees (2.10). Google: let the stored shape and the
offered shape differ from the start (2.2). On trimming, everyone who said "forever"
gave ground: Reed's window, Urbit's checkpoints, AT Protocol's non-archival relays,
Wikidata's history tables "near the limits of their scalability" (2.8, 3.6, 2.4,
2.5). Xanadu and Hewitt both make keeping something somebody's cost (3.5, 2.11).
Varda, on the cheap end: "once data has been written at all, keeping it around for
an extra month is pretty cheap" (2.2).


---
**meaning L2592-2601 · BODY · CARRIED P0,C5**
*Part five — for the group › 7. The voices that matter most, who I dropped, who is missin*

**Missing from the brief's list** (some may belong to other groups). Pat Helland
on data outside versus inside a service, and on immutability. Bill Kent, "Data
and Reality", on what an entity is. Certificate Transparency and similar verifiable
logs: a single append-only log that *outsiders can check* was never rewritten,
which is the missing piece between "one store" and "credible". Event-sourcing
practitioners on reading old events through new code. Records-retention law and
write-once storage in finance, where "never rewrite" meets "must delete". The
Croquet team's reasons for the reflector, which I could not find. Urbit, which I
added.


---
**rama L56-56 · BODY · CARRIED P0,C5**
*Part one — What Rama's own reference says › Question by question › (0) The log itself: never rewritten, or only never lost?*

- **CHECKED** `docs/14-depots.md:320-327` — trimming is opt-in through the dynamic option `depot.max.entries.per.partition`. By default a record is not trimmed while any topology in any module still needs it.

---
**rama L57-58 · BODY · NEW-REASON P0**

- **CHECKED** `docs/03-first-module.md:107` — the stated purpose of keeping the log: "If you deploy a bug that corrupts your views, you can always recompute those views from scratch from the raw data in the logs… Event sourcing also provides a natural audit trail."


---
**rama L59-60 · BODY · CARRIED P0,C5**

What this adds up to. Rama's answer: never lost by default, rewritten when the operator decides, positions stable either way. A depot migration is a deploy, not a per-record call. It rewrites the whole depot in the background. **IMPLIED:** if Sid wants "never rewritten" to be true of the store, that is a policy Sid holds over the operator (never ship a depot migration, never set the trim option). Rama will not enforce it. The other reading is also open: keep "never rewrite" as the rule for meaning, and keep Rama's migration door for representation and for lawful erasure. Part two shows Marz has always taken the second reading.


---
**rama L216-216 · BODY · CARRIED P0**
*Part one — What Rama's own reference says › Question by question › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

- **CHECKED** trimming is off by default (above).

---
**rama L217-218 · BODY · NEW-REASON P0**

- **CHECKED** `docs/22-backups.md:126-143` — incremental online backup "requires a paid license". On the free version a backup means shutting the cluster down and copying the data directories and a Zookeeper snapshot.


---
**rama L235-236 · BODY · CARRIED P0,C1**
*Part one — What Rama's own reference says › The gate: stream or microbatch*

| | Stream gate | Microbatch gate |
| Re-publishing to a depot | at-least-once | "do not have exactly-once semantics… this is on our roadmap" (`docs/12-microbatch-topologies.md:130`; still so in the live docs today) |


---
**rama L241-242 · BODY · CARRIED P0,E3,A1**

One structural point sits under all of this. In Rama the only ordered, replicated, never-trimmed log is a depot, and the only exactly-once writes are PState writes. Clients append to depots. Topologies write PStates. So the natural Rama reading of Sid's design is: **the offers depot is the log, and the facts are PState rows the gate writes.** "The gate is the only writer" is then true of the fact PStates by construction (`docs/15-pstates.md:41`). But it means the permanent record is the offers, and the facts are, in RPL's words, "essentially materialized views of depots, with depots being the source of truth" (`docs/04-depots-etls-pstates.md:120`). That is fine only if the gate's decision can be re-derived from the offers in order. A clock read breaks that. So either the stamp is carried into the offer before the append (by the front tier), or the fact PStates are declared primary data in their own right and protected as such. The Rama skill says PStates are already that in practice: "PStates are durable storage in their own right — replicated and persisted to RocksDB at write time, NOT a materialized view recomputed from depots" (`.claude/skills/rama/SKILL.md`). Sid should pick which of the two is "the log that is never rewritten", because Rama gives them different guarantees.


---
**rama L251-252 · BODY · NEW-REASON P0**
*Part one — What Rama's own reference says › Other items the brief named*

**Replication.** **CHECKED** `docs/21-replication.md:41`: nothing is visible until it is on disk on the leader and on all in-sync followers. `:81`: "we recommend a replication factor of three and a min-ISR of two." `:152`: cluster metadata and leader election live in Zookeeper.


---
**rama L269-270 · BODY · CARRIED P0**
*Part two — Nathan Marz › 1. What he built where this decision was load-bearing, and w*

**Rama (started 2013, public 2023).** Depots are partitioned append-only logs. PStates are durable indexes, each owned and written by exactly one topology, kept up to date incrementally. In his words (HN, 2023-08-15, https://news.ycombinator.com/item?id=37139258): "Rama codifies and integrates the concepts I described in my book, with the high level model being: indexes = function(data) and query = function(indexes)."


---
**rama L273-274 · BODY · NEW-REASON P0**
*Part two — Nathan Marz › 2. His reasons, in his words*

On what a fact is. "A piece of data is an indivisible unit that you hold to be true for no other reason than it exists. It is like an axiom in mathematics." And: "data is inherently time based. A piece of data is a fact that you know to be true at some moment of time." And: "data is inherently immutable. Because of its connection to a point in time, the truthfulness of a piece of data never changes… CRUD has become CR." (*How to beat the CAP theorem*, 2011, http://nathanmarz.com/blog/how-to-beat-the-cap-theorem.html)


---
**rama L275-276 · BODY · NEW-REASON P0**

On raw versus derived. "When you keep tracing back where information is derived from, you eventually end up at information that's not derived from anything. This is the rawest information you have: information you hold to be true simply because it exists. Let's call this information data." (Marz and Warren, *Big Data*, Manning 2015, chapter 1; free sample chapter, mirrored at http://www.odbms.org/wp-content/uploads/2014/02/BD_meap_ch01.pdf)


---
**rama L277-278 · BODY · NEW-REASON P0**

On why immutable. His first reason is people, not machines. "In a production system, it's inevitable that someone will make a mistake sometime, such as by deploying incorrect code that corrupts values in a database. If you build immutability and recomputation into the core of a Big Data system, the system will be innately resilient to human error." And: "That raw pageview information is never modified. So when you make a mistake, you might write bad data, but at least you won't destroy good data." (*Big Data*, chapter 1)


---
**rama L287-288 · BODY · CARRIED P0,A3**

On one writer. "Rama PStates are globally readable but not globally writable. They are only writable from the topology that declares them. All code writing to PStates is thereby always in the exact same program. Additionally, since PStates are not the source of truth – the depots (event logs) are – mistakes can be corrected via recompute from the source of truth." (HN, 2024-01-10, https://news.ycombinator.com/item?id=38942073)


---
**rama L297-298 · BODY · DISAGREES P0,C5**
*Part two — Nathan Marz › 3. What he later changed, regretted, or migrated*

**(c) "Forever" got doors.** Already in 2011 he allowed three kinds of deletion: bad data ("delete the bad data and precompute the queries again"), low-value data ("Garbage collection is simply a function that takes in the master dataset and returns a filtered version of the master dataset… Mutability is really just an inflexible form of garbage collection"), and law ("regulations requiring you to purge data after a certain amount of time. These cases are easily supported"). Rama turned these into features: depot migrations with tombstones, and trimming. "Depots (the 'event sourcing' part of Rama) can be optionally trimmed… Some applications need this, while others don't." (HN 38934011)


---
**rama L307-308 · BODY · DISAGREES P0**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(0) Never rewritten, or never lost?** REPORTED: for Marz "immutable" means a new write never destroys an old one. It does not mean the bytes are never touched. He deletes bad data, collects garbage, purges for regulators, and migrates schemas: "When you change your schema, you'll have the capability to update all data to the new schema." (*Big Data*, chapter 1). INFERRED: he would tell Sid that "never rewritten" is stronger than the property that does the work. The property that does the work is: never overwritten, always re-derivable.


---
**rama L346-346 · BODY · DISAGREES P0**
*Part two — Nathan Marz › 5. What resembles Sid's situation, and what differs*

- He always kept a door open to rewrite representation. Sid closes it.

---
**rama L347-347 · BODY · CARRIED P0**

- He trusted the operator. One company owned the dataset. For a planet-wide store with an economy on it, the operator's power to rewrite is itself a trust question. His comfort with migration does not carry over for free.

---
**rama L371-372 · BODY · CARRIED P0,E3**
*Part three — The dissent: Jay Kreps › 2. His reasons, in his words*

On what to put in the log. "The 'state machine model' usually refers to an active-active model where we keep a log of the incoming requests and each replica processes each request. A slight modification of this, called the 'primary-backup model', is to elect one replica as the leader and allow this leader to process requests in the order they arrive and log out the changes to its state from processing the requests." His toy example: one log holds "+1", "*2"; the other holds "1", "3", "6". (*The Log*)


---
**rama L381-382 · BODY · DISAGREES P0**

On keeping the log. 2013: "Of course, we can't hope to keep a complete log for all state changes for all time. Unless one wants to use infinite space, somehow the log must be cleaned up." (*The Log*). 2017: "if you just set the retention to 'forever' or enable log compaction on a topic, then data will be kept for all time… it's not insane, people do this all the time, and Kafka was actually designed for this type of usage." With a warning: "when a system is treated as the canonical source for data the bar for both software correctness and operational practices increases quite dramatically." (*It's Okay To Store Data In Apache Kafka*, Confluent, 2017, https://www.confluent.io/blog/okay-store-data-apache-kafka/)


---
**rama L387-388 · BODY · NEW-REASON P0,A1**
*Part three — The dissent: Jay Kreps › 3. Where he and Marz disagree*

He starts with what he accepts. "I like that the Lambda Architecture emphasizes retaining the input data unchanged. I think the discipline of modeling data transformation as a series of materialized stages from an original input has a lot of merit." And: "Reprocessing is one of the key challenges of stream processing but is very often ignored… Code will always change."


---
**rama L394-394 · BODY · DISAGREES P0**

4. **Why the log exists.** For Marz it is there because people make mistakes. For Kreps it is there because code changes and many systems must read the same data. The first reason argues for keeping everything forever. The second argues for keeping as much as you may want to reprocess: "if you want to reprocess up to 30 days of data, set your retention in Kafka to 30 days."

---
**rama L413-414 · BODY · CARRIED P0,E3**
*Part three — The dissent: Jay Kreps › 4. Which of Sid's questions he speaks to*

**(7) and the shape of the gate.** REPORTED: the state-machine and primary-backup passage is Sid's fork, stated plainly. Log the offers and let every reader re-run the gate; then the gate must be deterministic, with no clock reads. Or let one gate decide and log its results; then the results are the truth and the gate may read a clock. Sid's design is the second. INFERRED: Kreps would say both work, and the mistake is to mix them by accident: to treat the offers as the log while the gate does things that cannot be replayed.


---
**skeptics L30-31 · BODY · DISAGREES P0**
*1. The ordinary default, question by question*

**(0) The log: never rewritten, or only never lost?** Neither. The row is the truth and is overwritten in place. AWS describes the usual way to keep history, in its own guidance for customers leaving its ledger database. REPORTED: "The conventional approach for audit tables is a table whose structure mirrors that of the table it audits. A trigger activates on every INSERT, UPDATE, and DELETE on the main table and sends a copy of the modified row into the audit table." ([AWS Database Blog, Replace Amazon QLDB with Amazon Aurora PostgreSQL for audit use cases; archived copy of Feb 2025](https://web.archive.org/web/2025/https://aws.amazon.com/blogs/database/replace-amazon-qldb-with-amazon-aurora-postgresql-for-audit-use-cases/)). The same post states the weakness: "with correct permissions, users can modify data in the audit tables." The built-in version is the temporal table. REPORTED: "A system-versioned temporal table is a type of user table designed to keep a full history of data changes, allowing point-in-time analysis." ([Microsoft Learn, Temporal tables](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal-tables)). Where the law demands write-once storage, it is bought per object and for a period. REPORTED: "S3 Object Lock can help prevent Amazon S3 objects from being deleted or overwritten for a fixed or variable amount of time, or indefinitely." ([S3 Object Lock](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html)). Why: current state is what nearly every query wants; storage and privacy law both push toward forgetting; history is kept to recover from mistakes. James Hamilton's well-known rule has that purpose and a window. REPORTED: "Soft delete only. Never delete anything. Just mark it deleted. When new data comes in, record the requests on the way. Keep a rolling two week (or more) history of all changes to help recover from software or administrative errors." ([On Designing and Deploying Internet-Scale Services, LISA 2007](https://www.usenix.org/legacy/event/lisa07/tech/full_papers/hamilton/hamilton.pdf); written while Hamilton was at Microsoft's Windows Live, before Amazon.)


---
**skeptics L68-69 · BODY · DISAGREES P0**

**(13) Storage: maps or classes; trimmed; backups.** Typed records (Avro or Protobuf with a registry) on the wire, typed columns at rest, a JSON column for the loose parts. Everything is trimmed: topic retention, TTLs, archive tiers. Backups are kept for weeks, with point-in-time restore (COMMON PRACTICE).


---
**skeptics L98-99 · BODY · ABOVE P0**
*2. The skeptics › 2.1 Michael Stonebraker with Joe Hellerstein (2005) and with*

On ledgers (2024), REPORTED: blockchain databases are "decentralized log-structured databases (i.e., ledger) that maintain incremental checksums using some variation of Merkle trees." "These vendors (incorrectly) promote the blockchain as providing better security and auditability that are not possible in previous DBMSs." "If organizations trust each other, they can run a shared distributed DBMS more efficiently without wasting time with blockchains." They allow a small exception: "There is possibly a (small) market for private blockchain DBMSs. Amazon's Quantum Ledger Database (QLDB) released in 2018 [65] provides the same immutable and verifiable update guarantees as a blockchain, but it is not decentralized".


---
**skeptics L102-103 · BODY · ABOVE P0**

**3. What later changed.** The "small market" they allowed for QLDB did not hold. AWS ended support for QLDB on 31 July 2025 (date from a search summary of an [InfoQ report](https://www.infoq.com/news/2024/07/aws-kill-qldb/); page not opened). AWS's own guidance was to move to Aurora PostgreSQL with audit tables (post opened; quoted in section 1). So the one managed, centralized, append-only, verifiable ledger from a major vendor was retired in favor of the ordinary default. INSTITUTIONAL: I have found no statement from Amazon of why. The 2024 paper's footnote says Amazon built QLDB "after finding no compelling use case for a fully decentralized blockchain DBMS". Too little demand is my inference, not a quote.


---
**skeptics L107-107 · BODY · DISAGREES P0,E1**

- (0) and (7), audit. REPORTED: better auditability from immutable ledgers is a claim vendors make "(incorrectly)". INFERRED: an ordinary database with restricted audit tables is enough for audit. They would ask what never-rewrite adds beyond audit.

---
**skeptics L141-142 · BODY · NEW-REASON P0**
*3. Big-tech operational lessons › 3.1 Amazon*

Lessons the DynamoDB paper lists, REPORTED: "Performing continuous verification of data-at-rest is a reliable way to protect against both hardware failures and software bugs in order to meet high durability goals." "Designing systems for predictability over absolute efficiency improves system stability. While components such as caches can improve performance, do not allow them to hide the work that would be performed in their absence". On silent corruption: "By maintaining checksums within every log entry, message, and log file, DynamoDB validates data integrity for every data transfer between two nodes."


---
**skeptics L143-144 · BODY · NEW-REASON P0,A3**

What this says to Sid. INFERRED: (a) Amazon moved from many writers with merging to one writer per partition with a version check. That is the direction Sid already chose. (b) The reason was operations, not theory. The better-fitting system lost inside its own company to the easier one. (c) "Never lost", at Amazon, is an activity: checksums in every record and a process that keeps re-reading data at rest. A store that means to keep facts for years has to decide at the first record whether each fact carries its own checksum. Facts stored without one cannot be verified later.


---
**skeptics L155-156 · BODY · CARRIED P0,E1,C3**

DSQL is the nearest big-tech design to Sid's gate that I found. Its parts: a query processor that holds a transaction's planned writes; an *adjudicator* that checks them for conflicts; a *Journal*, an ordered log of accepted transactions; storage replicas that apply the Journal with no coordination. REPORTED: "In DSQL, this task of looking for conflicts is also disaggregated. It's implemented in a service we call the adjudicator." "Once the adjudicator has decided we can commit our transaction, we write it to the Journal for replication." "This means that storage replicas don't need to coordinate at all when they consume the journal." It is optimistic: "OCC is exactly what's happening here." The cost of optimism under contention: "OCC aborts will only occur when two or more concurrent transactions attempt to write the same keys" (a footnote follows in the original). "The best way to avoid aborts is to design your schema in a way that avoids write hot spots". "As a rule of thumb, you want the heat on your hottest write key to remain constant as the overall load on your database rises."


---
**skeptics L194-195 · BODY · DISAGREES P0**
*3. Big-tech operational lessons › 3.2 Meta: TAO, FlightTracker, RAMP-TAO*

**Resemblance and difference.** Resembles: a graph of small typed things, planet scale, per-person views, indexes that lag. Differs: TAO is a cache over MySQL with mutable rows. It kept no history. It could, and did, run data migrations.


---
**skeptics L232-232 · BODY · DISAGREES P0**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (0) log | Mutable rows; audit table or temporal table; logs expire | Stonebraker and Pavlo: audit needs no ledger (REPORTED). AWS retired its ledger database and points to audit tables (REPORTED post; reason INSTITUTIONAL). DynamoDB and S3: "never lost" means checksums in every record, continuous re-reading, durability reviews (REPORTED) |

---
**skeptics L251-252 · BODY · DISAGREES P0**

| Sid's question | Ordinary default | Skeptics and big tech |
| (13) storage | Typed records plus a JSON column; trimmed; backups for weeks | DynamoDB: checksums everywhere (REPORTED). Hamilton: never drop an old format while rollback is possible (REPORTED) |


---
**skeptics L260-261 · BODY · ABOVE P0**
*5. Voices that matter most, who was dropped, who is missing*

3. **Stonebraker and Pavlo, read together with QLDB's retirement.** This is the strongest outside challenge. It is not that the design is wrong. It is that a relational database with audit tables absorbs most of it, and that the one major managed ledger product was withdrawn.


---
**skeptics L267-267 · BODY · CARRIED P0**

- **Certificate Transparency and Trillian** (Ben Laurie, Al Cutter, and others). Public append-only logs run for over a decade by several operators. Never rewritten, audited by outsiders, every entry attributed. That is the nearest real-world match to "never rewrite, audit permanence, many writers with attribution". I did not research it.

---
**skeptics L268-268 · BODY · CARRIED P0**

- **Regulated write-once practice** (SEC 17a-4, FDA 21 CFR Part 11). These are what "immutable" means in law, and both come with retention periods.

---
**skeptics L270-270 · BODY · CARRIED P0,C6**

- **Stripe's or Square's ledger engineering.** Money systems are the industry's everyday append-only stores with correction by reversal.

---
**sync L202-206 · BODY · DISAGREES P0**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

- "Never rewritten" was conceptual only: "The Write Log conceptually contains
  all Writes ever received by the server… In practice, a server can discard a
  Write from the Write Log once it becomes stable". An "O vector" records "the
  'omitted' prefix of committed Writes". The price of truncation is that a far-
  behind peer needs "a full database transfer". (SOSP 1995; SOSP 1997)

---
**sync L228-229 · BODY · DISAGREES P0**

- (0) Conceptually all writes; practically a truncated prefix with a recorded
  omission vector. REPORTED.

---
**sync L246-252 · BODY · DISAGREES P0**

**5. Resemblance and difference.** Close: a single orderer, offers with an
expected state, a session that knows what it has read, a wish that the display
not lie. Different: Bayou's whole point was disconnected work, so it *had* to
show tentative state. Sid's store is always reachable in principle. Bayou's
log was truncated on purpose; Sid's is the record. Bayou's scale was dozens of
replicas, not a planet.


---
**sync L503-507 · BODY · DISAGREES P0,C5**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

- Deletion really deletes: "Figma doesn't store any properties of deleted
  objects on the server. That data is instead stored in the undo buffer of the
  client that performed the delete… This helps keep long-lived documents from
  continuing to grow in size."


---
**sync L510-523 · BODY · DISAGREES P0**

- *The journal (2024).* The server held a file in memory and wrote a checkpoint
  "every 30 to 60 seconds". The admission: "Since checkpoints were created only
  every ~60 seconds, we could lose up to 60 seconds of work on the server-side
  if multiplayer crashes." They added a write-ahead journal: "Each change is
  assigned a sequence number, which is an incrementing integer associated with
  the file." The new target is still not zero: "(Our goal was <1s of data
  loss.)" To retrofit "every write is logged" they had to funnel all writes
  through one type: "By refactoring the code to isolate ownership of the file
  into an encapsulated type, it was easy to audit all cases where the file gets
  updated". They proved the log rebuilds state by replay: a "byte-by-byte
  identical file blob", and "After ~400K consecutive successful validations, we
  started rolling out." The journal "isn't cross-region replicated"; the
  checkpoints are. (Figma Engineering, "Making multiplayer more reliable",
  2024-04-29, https://www.figma.com/blog/making-multiplayer-more-reliable/)

---
**sync L564-566 · BODY · NEW-REASON P0,A1**

- (12), (13) The proof that a log is the truth is a replay that gives a
  byte-identical state, run hundreds of thousands of times. The disaster-
  recovery artifact was the snapshot, not the log. REPORTED.

---
**sync L571-572 · BODY · DISAGREES P0,C5**

- (9) Deleting really removes; undo data lives with the deleter. REPORTED. They
  accept lost history to bound growth. INFERRED.

---
**sync L872-874 · BODY · NEW-CASE P0**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- *Records with unresolved dependencies were dropped on save.* "when you save a
  doc it does not persist changes unless all their dependent changes are
  present." (Irwin, #595)

---
**sync L1201-1203 · BODY · NEW-REASON P0**
*2. Section one: sync and multiplayer › 2.10 Yjs (Kevin Jahns; 2015–now)*

- "It is not recommended to restore an old document state using snapshots,
  although that would certainly be possible." (INTERNALS.md)


---
**sync L1239-1241 · BODY · DISAGREES P0**
*2. Section one: sync and multiplayer › 2.11 Seph Gentle (ShareDB, Google Wave, diamond-types, Eg-wa*

- When history may go: "the event graph can be discarded if we know that no
  event we may receive in the future will be concurrent with any existing
  event."

---
**sync L1274-1276 · BODY · DISAGREES P0**

- (0), (13) With one gate nothing old is ever concurrent with anything new, so
  by his own rule the log is *technically* discardable below the head. Whether
  to keep it is then a question of provenance, not of merging. INFERRED.

---
**sync L1348-1351 · BODY · NEW-REASON P0,W2**
*2. Section one: sync and multiplayer › 2.12 Matthew Weidner (CRDT survey, Fugue, Collabs, list-posi*

- (13) "clients need to store the entire operation log indefinitely—not just
  the current state. You can mitigate this cost by using fancy compression
  and/or by putting the log in cold storage." REPORTED.


---
**sync L1370-1374 · BODY · DISAGREES P0,C5**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- Deletion is a first-class right of the record: "Record deletion is supported
  without leaving a trace or 'tombstone' of previous contents." And downstream:
  "services are expected not to differentiate between content which has never
  existed and content which has been entirely deleted." (specs, Repository;
  Accounts, https://atproto.com/specs/repository, https://atproto.com/specs/account)

---
**sync L1411-1414 · BODY · NEW-REASON P0**

- Metadata beside records: labels are separately signed statements about a
  record, optionally pinned to one version. A retraction is a new label with
  `neg` set, and "does not mean that the inverse of the label is 'true', only
  that the previous label has been retracted." (https://atproto.com/specs/label)

---
**sync L1469-1475 · BODY · DISAGREES P0**

- *The append-only identity log was edited.* "in late October, a number of
  invalid test operations were removed from the PLC directory, to bring it in
  compliance with the written specification." (Roadmap, 2026-03-24) Rejected
  operations are otherwise kept: "the set of all operations for all identifiers
  (even 'nullified' operations) can be enumerated and audited." Replicas now
  act as witnesses: if the primary dropped an operation, "the replicas would
  still have a copy of the deleted data." (Buchanan, "PLC Read Replicas", 2026)

---
**sync L1510-1511 · BODY · DISAGREES P0,E1**

- (7) Keep rejected operations and let replicas witness the log. REPORTED. Even
  so they purged bad rows once. REPORTED.

---
**sync L1514-1515 · BODY · DISAGREES P0,C5**

- (9) Delete without trace, by design, and they cannot make others comply.
  INSTITUTIONAL.

---
**sync L1649-1655 · BODY · ABOVE P0**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

- *The name of the design was withdrawn.* Aljoscha Meyer, in the disclaimer now
  at the top of the Bamboo README
  (https://github.com/AljoschaMeyer/bamboo):

  > Some years after authoring this specification, I do not consider the
  > "append-only log" terminology to be appropriate anymore… This makes the log
  > an "append-or-delete log", not an "append-only log".

---
**sync L1678-1685 · BODY · DISAGREES P0**

- *Completeness proofs were abandoned on purpose.* Willow: "The need to verify
  the hash chain also makes it difficult to edit or delete (meta-)data…
  designing systems for unbounded growth tends to backfire sooner or later.
  Willow deliberately avoids any cryptographic proofs of completeness, allowing
  for traceless data removal when the need arises." And on content addressing:
  "For every new piece of data, I must eventually leave the system and transmit
  a new hash out-of-band." (Meyer and Gwilym, "Willow Compared",
  https://willowprotocol.org/more/willow_compared/index.html)

---
**sync L1707-1707 · BODY · ABOVE P0**

- (0) The authors of append-only logs now say "append-or-delete". REPORTED.

---
**sync L1817-1824 · BODY · NEW-REASON P0,C1,C8**
*3. Section two: versioning › 3.1 Git (Linus Torvalds, Junio Hamano, Jeff King, brian m. c*

- Things known later go beside the record: notes "add annotations with
  information that was not available at the time a commit was written",
  "without touching the objects themselves." Identity is corrected outside
  history: `.mailmap` maps "author and committer names and email addresses to
  canonical real names". Replacement refs redirect one object to another, and
  are honoured by everything "except those doing reachability traversal (prune,
  pack transfer and fsck)". (git-notes, gitmailmap, git-replace docs)


---
**sync L1975-1981 · BODY · NEW-REASON P0,C8,E3**
*3. Section two: versioning › 3.3 Fossil (D. Richard Hipp; 2006–now)*

- Corrections are additions: "Fossil allows all of this not by removing or
  modifying existing repository entries, but rather by adding new supplemental
  records. Fossil keeps the original incorrect or unclear inputs and makes them
  readily accessible". Even who and when can be corrected this way: "The 'user'
  tag overrides the name of the check-in user. The 'date' tag overrides the
  check-in date." The list of uses includes "Fix faulty check-in date/times
  resulting from misconfigured system clocks." (rebaseharm; fileformat)

---
**sync L2001-2005 · BODY · NEW-REASON P0**

- *The honest limit.* "if shunning and purging were removed from Fossil, you
  could still remove artifacts from the repository with SQL DELETE statements;
  the repository database file is, after all, directly modifiable… Where the
  Fossil philosophy really takes hold is in making it difficult to violate the
  integrity of the hash tree." (fossil-v-git)

---
**sync L2035-2045 · BODY · NEW-REASON P0**

**5–6.** The closest philosophy to Sid's never-rewrite rule, held for twenty
years by one careful person, with its exception written down. Small scale, and
everyone has a full copy. He disagrees with Git: "Git strives to record what
the development of a project should have looked like had there been no
mistakes. Fossil, in contrast, puts more emphasis on recording exactly what
happened, including all of the messy errors, dead-ends, experimental branches,
and so forth." He cites a commentator who "characterized Git as recording
history according to the victors, whereas Fossil records history as it actually
happened." (fossil-v-git) He disagrees with Mercurial on spreading
supersession records.


---
**sync L2096-2101 · BODY · DISAGREES P0,C5**
*3. Section two: versioning › 3.4 Dolt, and Noms before it (Tim Sehn, Aaron Son, Andy Arth*

- *Deletion means rewriting.* "Deleting rows or dropping tables isn't
  sufficient, because the sensitive data will still be reachable from previous
  commits. What we need is a way to rewrite history and excise the data from
  all the commits." Then garbage-collect. (Arthur, "Filter-Branch in Dolt",
  2020) I found no Dolt post on erasure law as such.


---
**sync L2144-2149 · BODY · DISAGREES P0**
*3. Section two: versioning › 3.5 Irmin (Thomas Gazagnaire, Anil Madhavapeddy, the Tarides*

- *Growth was an issue after all.* "As rolling nodes execute, the pack file
  grows larger and larger, and no old data is discarded." "it is impossible to
  'delete' regions corresponding to dead objects and reclaim the space."
  Operators worked around it by exporting a snapshot, deleting everything, and
  importing it back. Garbage collection arrived in 2022. (Tarides, "Towards
  Minimal Disk-Usage for Tezos Bakers", 2022-11-10)

---
**sync L2168-2171 · BODY · DISAGREES P0**

**5–6.** Irmin under Tezos is a never-rewrite store with many verifiers, run
for years. It differs in that its history is pruned by design in most nodes. I
found no retrospective by Gazagnaire or Madhavapeddy.


---
**sync L2259-2262 · BODY · NEW-REASON P0**
*4. Question by question › (0) The log: never rewritten, or only never lost?*

- Hipp (Fossil) holds never-rewrite as policy for twenty years, and writes down
  its limit: the bytes are always editable by whoever holds the disk, so the
  real promise is "making it difficult to violate the integrity of the hash
  tree". He keeps one narrow exception, shunning. REPORTED.

---
**sync L2263-2265 · BODY · ABOVE P0**

- Meyer (Bamboo) withdrew the term: "append-or-delete log". Willow:
  "designing systems for unbounded growth tends to backfire sooner or later."
  REPORTED.

---
**sync L2266-2271 · BODY · DISAGREES P0,C5**

- Kleppmann, in *Designing Data-Intensive Applications* (2017, ch. 11): for
  some data "it's not sufficient to just append another event to the log to
  indicate that the prior data should be considered deleted — you actually want
  to rewrite history and pretend that the data was never written in the first
  place. For example, Datomic calls this feature excision and the Fossil
  version control system has a similar concept called shunning." REPORTED.

---
**sync L2272-2273 · BODY · ABOVE P0**

- Upwelling's authors call full history "a problem caused by keeping too much
  history". REPORTED.

---
**sync L2274-2275 · BODY · DISAGREES P0**

- Bayou's log held all writes "conceptually" and was truncated in practice,
  with a vector that records what was omitted. REPORTED.

---
**sync L2276-2277 · BODY · DISAGREES P0**

- Irmin assumed growth "is not an issue" (2015) and retrofitted garbage
  collection (2022). REPORTED.

---
**sync L2280-2281 · BODY · DISAGREES P0,C6**

- AT Protocol dropped its commit chain, made relays non-archival, and once
  removed rows from its append-only identity log. REPORTED.

---
**sync L2282-2283 · BODY · DISAGREES P0**

- Figma: "never lost" is a measured number (sixty seconds, then under one).
  REPORTED.

---
**sync L2287-2290 · BODY · DISAGREES P0,C5**

**Where they split.** Kleppmann and Automerge: keep everything, make it cheap.
Willow and AT Protocol: deletion is a right, so do not build proofs of
completeness. Hipp: keep everything, with a written exception.


---
**sync L2831-2832 · BODY · DISAGREES P0,E1**
*4. Question by question › (7) Beside each fact: is the gate's yes or no kept? Where? A*

- did:plc keeps rejected operations and lets replicas act as witnesses, and
  still had to remove invalid rows once. REPORTED.

---
**sync L2842-2844 · BODY · NEW-CASE P0**

- Making sure every write is logged needed a single type that owns the state:
  Figma. REPORTED.


---
**sync L2955-2956 · BODY · DISAGREES P0**
*4. Question by question › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

- Trimming was always retrofitted by those who said never: Irmin. Bayou planned
  it from the start, with a record of what was omitted. REPORTED.

---
**sync L2957-2959 · BODY · NEW-REASON P0**

- The disaster-recovery artifact was the snapshot, not the log: Figma. A
  restored or re-encoded store is checked for logical equality: Figma, Dolt.
  REPORTED.

---
**sync L2993-2999 · BODY · DISAGREES P0**
*5. The group › 5.1 The voices that matter most, who I dropped, who is missi*

3. **The signed-log veterans, read together: Bryan Newbold and Paul Frazee (AT
   Protocol) with Aljoscha Meyer (Bamboo, then Willow).** They lived with
   never-rewrite among strangers, at scale, and they publish their debts. They
   converge on four moves: the value apart from the envelope; no chain that
   proves completeness; the server's number as an annotation; and the hard
   truth that the first record's format is forever.


---
**sync L3009-3010 · BODY · DISAGREES P0,C5**

- Kleppmann (keep all history, make it cheap) against Meyer and Willow (no
  unbounded growth, traceless removal) against Hipp (keep all, shun rarely).

---
**sync L3040-3042 · BODY · CARRIED P0,E1**

- **Certificate Transparency, Trillian, Sigstore.** Append-only logs with
  witnesses and signed checkpoints: the model did:plc says it is moving
  toward. Bears on (0), (7) and the second store.

---
**sync L3194-3196 · BODY · CARRIED P0,C8**
*6. The second store: what each source implies*

- **Attestation, not just signatures.** Signatures age with key rotation (AT
  Protocol, REPORTED). The pattern that is emerging is a gate's attestation
  plus independent witnesses who keep copies (did:plc replicas, REPORTED).

