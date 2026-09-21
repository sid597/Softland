# C2 as-of (10)(5): main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L900-901 · R2 · CARRIED C2**
*Round two › R2.1 The tailored questions › T3. One order as a scope choice, against lean (10)*

**Name the cut.** The lean says "never one number". The camp would say *always one number*, by making the cut a thing with a name. An index build is a fact: *build 8812 covers these positions in these units*. Every read against that build says *as of build 8812*. The vector is written once, with the build. It is never written on a fact. R for the pattern: Datomic's basis is exactly this. A database value is a named index plus the log's tail, and one number names it [H-DD], [D-ACID]. R: XTDB v2's single writer closes a block "at the end of each block (~100k rows/15 minutes)" and writes it to object storage [X-DBS]. That is a named unit of index progress. R: this is also what Hickey prizes about a basis. It can be said aloud and handed over [H-HN12]. Lean (5) then has an exact answer: how far the index had really got is the build number. The cost is lag. A read of the whole base is as of the last build, not as of now. The honest form is to say so on the read. *A question for the Rama camp, which I cannot answer:* whether Rama's own batch machinery already produces a store-wide number that names a consistent cut. If it does, that number is the basis.


---
**datalog L902-903 · R2 · NEW-CASE C2**

**What Nubank's practice says about "as of" across databases.** N, from R sources. They do not have one, and they never record a vector of bases. About 2016: "Datomic queries can take multiple database sources as inputs into a single query so the company can conduct cross service joins" [N-STORY]. By 2017 that had stopped scaling: "even for simple things, like how many customers we have… So what we did is the traditional approach of making an ETL" [N-QCON]. Between services, consistency is eventual and carried by messages that are safe to repeat [N-QCON]. Questions about the whole estate are answered from a separate extract that lags, and their "as of" is the extract's. That is the named build again, reached by necessity.


---
**datalog L982-983 · R2 · NEW-REASON C2**
*Round two › R2.2 Ask A: the leans, one by one*

**(5) How far the index had got, and what was withheld. Stands.** R: `sync` [D-ACID]; `COMMIT SYNC` [X-TXS]. *Sharpen "what was withheld":* you cannot list what you were not allowed to see. Record the *view*: the layer stack and the policy version in force. R: a Datomic database value is a basis plus its filters, and a filtered value is a different value [D-FILTER]. So the basis is positions plus view.


---
**datalog L1003-1003 · R2 · ABOVE C2**
*Round two › R2.3 Ask B: leans the camp would call the wrong question*

2. **(10) "As of is a cut, never one number."** Ask instead: *what names a cut?* A cut that must be said, stored and handed over needs a name. T3.

---
**frontiers L20-20 · SHORT · NEW-REASON C2,W3**
*0. The camp's shared model, in one page*

3. **Times need not be one number.** They need a partial order with a least upper bound. A vector of per-partition positions is a legal time. So is one integer.

---
**frontiers L21-21 · SHORT · NEW-REASON C2**

4. **A frontier says which times are finished.** It is a set of times such that nothing earlier than it will ever arrive again. For one integer it is one number. For a vector it is a set of vectors (an antichain). Frontiers are data that travel with the stream. They are not a guess.

---
**frontiers L22-22 · SHORT · NEW-REASON C2**

5. **A consistent answer is one that is exactly right for the inputs as of some stated time.** A reader knows it got one when the frontier of what it read has passed that time. Before that, the honest answer is "not ready yet", never a partial number.

---
**frontiers L431-431 · ABOVE · ABOVE C2,W3**
*10. Questions this camp would call the wrong question*

- **"As of: one number or a position per partition?"** A false choice. Both, tied by a recorded map. The question that matters: who invents the interleaving, and where is that written down?

---
**frontiers L432-432 · ABOVE · ABOVE C2**

- **"If an index was behind when a pattern was read, is how far it had got written down?"** This treats lag as a footnote to a read. For this camp the frontier *is* the read. The question that matters: may anything read without naming a time inside the index's frontier? If not, there is nothing extra to write.

---
**frontiers L516-517 · R2 · DISAGREES C2**
*Round two (2026-09-20): the leans, pressed from the frontier › T1. The smallest convention for internal consistency*

Nothing else is needed on the fact. What cannot be added later is item 2. If early reads name points that cannot be compared, no later rule can tell whether two of them were the same moment. Leans (10) and (11) together decide whether points are comparable. That is T2.


---
**frontiers L522-523 · R2 · CARRIED C2,C3**
*Round two (2026-09-20): the leans, pressed from the frontier › T2. The reclocking map as a first-record item*

**Road A, McSherry's remap (R).** A separate, slow minter records cut facts. Cut k holds a position for every partition, each at least as far as in cut k−1, so the cuts form a chain. A read names k, eight bytes, and the vector is looked up. Gates never wait for the minter. Recorded: the cut facts, in one small low-rate depot, plus an explicit entry when a partition begins, because McSherry flags that "recording 'these parts have not yet started' is non-trivial" (R). If the Rama camp is right that a microbatch has a global tick, a microbatch is a natural minter: batch k consumed a known range in each partition. Whether Rama exposes and keeps those ranges needs checking. One rule makes named cuts safe with no checking (I): offers read only at named cuts. A fact then always lands after the cut it read under, so any later cut that contains the fact also contains everything it stood on. The price: an agent cannot stand on its own last write until the next cut.


---
**frontiers L528-529 · R2 · NEW-REASON C2**

**Research-6's opaque store-minted token, "as Zanzibar did".** The same thing under another name, on one condition. Zanzibar's token wraps a Spanner timestamp (R, https://www.usenix.org/system/files/atc19-pang.pdf), and that timestamp lives in the data. Opaque to tools is right; it is the defence against Hyrum's Law. But what the token means must be a fact, or be computable from facts. A token whose meaning lives only in the running runtime dies at the first rebuild, and every as-of recorded before that becomes unreadable.


---
**frontiers L556-556 · R2 · DISAGREES C2,E3**
*Round two (2026-09-20): the leans, pressed from the frontier › A. Leans this camp would reject or sharpen*

- **(10) and (11), rejected as a pair.** McSherry (R): the stamp "becomes the truth about when that update happens", and answers must move "to the right of (after) all previously chosen timestamps". Bare vectors can be incomparable, so that promise cannot be kept. With `when` barred from ordering, nothing else orders across partitions. Deciding case: the store grows from 64 to 256 partitions in year two. Every recorded as-of is a vector over a partitioning that no longer exists. A scalar survives untouched.

---
**frontiers L567-567 · R2 · DISAGREES C2**
*Round two (2026-09-20): the leans, pressed from the frontier › B. Wrong questions*

- "A cut, never one number": still a false choice. Ask whether as-of points are comparable, and where their meaning is stored.

---
**frontiers L569-570 · R2 · ABOVE C2**

- "How far the index had really got": ask whether any read may pass the index's point at all.


---
**frontiers L574-574 · R2 · NEW-REASON C2,A2**
*Round two (2026-09-20): the leans, pressed from the frontier › C. Above the table: what the leans change*

- The runtime as the one non-fact: minting cuts, or announcing points, is runtime work whose residue must be facts. Otherwise every as-of dies at a rebuild.

---
**log L559-559 · ABOVE · ABOVE C2**
*For the group › 9. Questions this camp would call the wrong question*

2. **"Is how far the index had got written down?" (5).** That is what "as of" means. There is no other honest as-of to write.

---
**log L721-721 · R2 · DISAGREES C2**
*Round two: the leans, pressed › A. The other leans*

- **(5) Stands**, and "what was withheld" is better than anything in this camp. Nearest neighbour: Delos carries its playback position in every entry (R).

---
**log L732-732 · R2 · ABOVE C2,C3**
*Round two: the leans, pressed › B. Wrong questions, against the leans*

4. Lean (10) asks one number or a cut. Ask: who names partitions, and do the names survive re-homing?

---
**rama L538-539 · R2 · CARRIED C2,C3**
*Round two › T1. Which log › Shape (a): the offers depot is the permanent record; facts a*

Re-partition. Rama cannot add tasks (**CHECKED**, round one). The documented way out has two branches. Recompute in a new module: "This approach only works if your processing is deterministic, which may not be the case if your processing makes use of any random numbers (such as UUIDs)." Or: "An alternative approach is to repartition the PStates directly by making a special topology in the new module to iterate through the original PStates and repartition them to the new module's PStates." (**CHECKED** `docs/19-operating-rama.md:489`). Under (a) with a clock-reading gate only the second branch is open. It works, because the rows carry their stamps as data. It is a full physical rewrite of the facts, with "significant downtime" (`:491`).


---
**rama L556-557 · R2 · CARRIED C2,C3**
*Round two › T1. Which log › Shape (b): the gate decides, commits, then publishes the adm*

Re-partition. RPL's first recipe works as documented, because a consumer of a results log is deterministic.


---
**rama L639-639 · R2 · CARRIED C2**
*Round two › T5. One cluster worldwide*

3. Can followers serve reads, stale allowed, with a stated position? If not, is it planned?

---
**rama L645-645 · R2 · CARRIED C2,E3**

9. Can topology code read a record's depot offset and append time? That would give a native position for cuts and a native "when".

---
**rama L679-680 · R2 · DISAGREES C2**
*Round two › A. The leans, one by one*

**(10) Order. SHARPEN.** Kreps (R) agrees that a position per partition is the honest form. Two cases press on "never one number".
- The first re-partition. A position belongs to one partition of one layout. Rama grows by copying into a new layout. Cuts already written onto never-rewritten facts then point at a layout that no longer exists. Sharpen: every position carries a layout epoch from the first day.

---
**rama L682-682 · R2 · CARRIED C2,W3**

- Under a microbatch gate one global number exists (**CHECKED**, round one) and survives a re-partition. "Never one number" gives up Rama's only native global cut. Keep the slot able to hold either form.

---
**rama L687-688 · R2 · CARRIED C2**

**(5) How far the index had got. STANDS.** Kreps (R): the "index point". Kafka lives this way. In Rama the marker has to be built (**CHECKED** `skill/testing.md:157-165`).


---
**skeptics L295-295 · ABOVE · ABOVE C2,W4**
*7. Questions this camp would call the wrong question*

- **"Can two gates ever write one layer?"** DSQL's version: can every reader learn the point below which *no* gate will ever stamp? If so, the number of gates is a tuning matter.

---
**skeptics L419-419 · R2 · CARRIED C2,C3**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(10) "never one number": rejected.** The industry stores per-partition offsets only as consumer progress, never as something a person is shown. Deciding case (I): repartitioning in year two leaves every recorded cut unreadable unless old partition maps are kept for ever. TAO (R) avoided moving anything by binding objects to a shard "for their entire lifetime".

---
**sync L102-108 · SHORT · NEW-CASE C2,C3**
*1. The short version*

7. **"As of" should be an opaque position token, not an integer.** Figma's
   LiveGraph assumed one global order, called the assumption "deeply baked",
   and watched one unavailable shard stall every optimistic update. Replicache
   documents the ceiling of a single global version. Linear's one counter for
   all workspaces leaks everyone's write rate. Replicache's cookie is "opaque
   to the client". A token can be one number today and a vector later.


---
**sync L109-115 · SHORT · CARRIED C2**

8. **Record how far the index had got.** Bayou "stably records the unique
   identifier of the last Write reflected in the Tuple Store checkpoint".
   Croquet exposes "backlog" and shows an overlay when the model is behind.
   Figma's LiveGraph does not stamp reads and pays by re-fetching
   unconditionally. AT Protocol reports index lag in an HTTP header, outside
   any record, and still debates whether a server is "read-sticky".


---
**sync L3360-3363 · R2 · CARRIED C2**
*Round two: the leans, pressed › T1. "No optimism" against one worldwide gate*

- *Reads of the base from far away.* A running answer near the person stands on
  base facts that arrive late. That raises the stakes of (5), not of optimism.
  Croquet's overlay for a large backlog is the honest display (R §2.3).


---
**sync L3438-3440 · R2 · NEW-REASON C2,P0**
*Round two: the leans, pressed › T3. A separable value, against lean (9)*

- *Once things can be absent, numbers that were derivable must be stored.*
  PPPPP: "we NEED this field" (R §2.15).


---
**sync L3441-3445 · R2 · DISAGREES C2**

**For Sid (I).** "The map must not lie" picks Matrix's side over Willow's. A
reader must be able to tell three states apart: never existed; erased, with a
pointer to the erasure fact; exists but withheld from you. Lean (5)'s "what was
withheld" is the third.


---
**sync L3591-3596 · R2 · DISAGREES C2**
*Round two: the leans, pressed › A. The other leans*

**(16) Visibility.** "Base open": AT Protocol lived public-first and is adding
private data four years in (R §2.13). Stands if "open" is a policy fact in the
seed and not a property of the store. Sharpen: a verdict that sits in the
fact's own layer shows every reader which grant authorised it. If that grant
lives in a private layer, the display says "withheld". (I)


---
**sync L3602-3604 · R2 · DISAGREES C2**

**(5) Index position.** Stands: Bayou's checkpoint id, Croquet's backlog (R).
"What was withheld" is a piece nobody in this camp has. Keep it.


---
## Says the same as the ledger (counted, not copied)

- C2 · datalog · 4: L493-494, L610-610, L895-897, L1054-1054
- C2 · frontiers · 6: L18-18, L35-36, L37-38, L101-102, L317-317, L558-558
- C2 · meaning · 1: L3089-3093
- C2 · rama · 2: L681-681, L706-706
- C2 · skeptics · 2: L223-223, L308-308
- C2 · sync · 4: L198-201, L1272-1273, L2600-2600, L2640-2643

