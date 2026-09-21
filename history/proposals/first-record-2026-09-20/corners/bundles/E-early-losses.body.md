# E early losses: body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L123-128 · BODY · ABOVE E6**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

**Why readers must be free.**

> "Perception is not a coordinated activity. Everybody is free in this room to look at whatever they want, or look at whomever they want. You do not need to get permission, or coordinate, or anything else." — Hickey [H-DD]

> "The critical thing is that queries and reads not part of a transformation never create a transaction, and don't interact with the transactor at all." — Hickey [H-HN12]


---
**datalog L175-176 · BODY · NEW-CASE E1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.3 What they later changed, regretted, or moved away from*

7. **They renamed the thing a client submits.** After Jepsen: "Going forward, Datomic intends to refer to this structure as a 'transaction request', and to its elements as 'data'. The [:db/add ...] and [:db/retract ...] forms are 'assertion requests' and 'retraction requests,' respectively. This helps distinguish between assertion datoms… and the incomplete [entity, attribute, value] assertion request in a transaction request." [JEP]. Twelve years in, they found they needed separate words for the offer and the fact. Sid already has them.


---
**datalog L179-180 · BODY · NEW-REASON E3**

9. **Practitioners learned that transaction time is not domain time.** Val Waeselynck, a long-time Datomic user, 2017: "Datomic does not let you change your mind about the information you encode in its time-travel features, and that's usually too big a constraint." He separates "event time: the time at which stuff happened" from "recording time: the time at which you're system learns that stuff happened", and concludes the time-travel features are "extremely valuable for debugging, auditing, and integrating to other data systems. But you should probably not implement your business logic with them." [VAL17]. Huahai Yang cites this essay as a reason Datalevin keeps no history.


---
**datalog L237-237 · BODY · NEW-REASON E3**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED. Never for order. "Datomic's own t time value exactly orders transactions in monotonically ascending order… By contrast, wall clock times specified by :db/txInstant are imprecise as more than one transaction can be recorded in the same millisecond. For filtering databases by time, establishing the relative order of events in a historical database, or any other time-based operation requiring exact precision, you should always use the t (or related tx) value." [D-BEST]

---
**datalog L238-238 · BODY · NEW-REASON E3,C2**

- REPORTED. The clock is kept in step with the order, so a wall-clock "as of" can be mapped onto a position: an explicit instant "must respect the monotonic ordering of wall-clock time, i.e. you must choose a :db/txInstant value that is not older than any existing transaction, and not newer than the transactor's clock time." [D-TXDATA]. XTDB v2 has the identical rule for backfill. [X-TXS]

---
**datalog L239-239 · BODY · NEW-CASE E3**

- INFERRED. For the seed of ten million papers this rule bites. "When the paper was published" cannot be the gate's stamp. It is a domain fact. The gate's stamp can only say when the store learned it. Waeselynck's event time and recording time are the two things, and only recording time belongs to the gate. [VAL17]

---
**datalog L245-245 · BODY · NEW-REASON E5**

- INFERRED. That example is the camp's version of Sid's "doubt": a view computed at read time from facts about the saying, never a stored flag. It supports Sid's design of staleness and doubt as walks.

---
**datalog L263-263 · BODY · NEW-REASON E6**

- INFERRED. That makes Sid's second kind of read ("everything matching a pattern as of some point") the general case. The first kind (one fact at its version) is the same thing with a narrow pattern. The third kind (an anchor outside the store) is the only one that is truly a different thing, because the store cannot re-derive it.

---
**datalog L264-264 · BODY · NEW-REASON E5**

- INFERRED. Reads that only made a tool fire: in Datomic's model these are the basis of the saying, like any other read. "Put them on the transaction." The camp would not mix them into the same attribute as the reads the content stands on. See (4).

---
**datalog L265-266 · BODY · ABOVE E6**

- A tension to name. Hickey's architecture gets its reach from *not* recording reads. "Queries and reads not part of a transformation never create a transaction." Sid records reads in two places only: on a fact that stood on them, and at a crossing. That respects the principle: a read is written down only at the moment it becomes part of an act. A design that logged every read would undo the separation of perception from process that this camp considers its central idea.


---
**datalog L269-269 · BODY · DISAGREES E5**

- INFERRED, from *Maybe Not* and the open attribute set. The camp would not put a flag inside one slot. Two meanings are two attributes. One names what the content stands on. The other names the path that led here. "A straight product type just completely complects the _meaning_ of things with their position in a list." [H-MN]

---
**datalog L270-270 · BODY · NEW-REASON E5,C2**

- REPORTED mechanism, INFERRED application. "Follow the latest, or stay on the version read?" In Datomic a reference points at an *identity*, never at a version. The version comes from the database value you read it through. So the same reference answers both questions: through the old basis it gives what the actor stood on, through the current value it gives what is there now, and the difference between the two *is* the doubt. Hickey's terms: "identity… is just a construct we use to collect the time series", and a state is "a snapshot. This entity has this value at this point-in-time." [H-AWTY]. Nothing has to be chosen when the fact is written, as long as the basis was written.

---
**datalog L300-300 · BODY · DISAGREES E1**

- REPORTED. In Datomic a yes *is* the transaction entity. There is no separate verdict. A no leaves no trace: "If one part of a transaction fails, the entire transaction fails, and the database is left unchanged." [D-ACID]. The caller gets an exception.

---
**datalog L301-301 · BODY · NEW-REASON E1**

- REPORTED. What a caller gets back on success is richer than yes: "every call to transact returns the database state just before the transaction, the database state the transaction produced, and the set of datoms the transaction expanded to." [JEP]. "What it checked" is the state just before.

---
**datalog L302-302 · BODY · DISAGREES E1**

- INFERRED. With one total order and a deterministic gate, the verdict is re-derivable: it is a pure function of the state at the previous position and the offer. Under Sid's own rule it would not need storing. That holds only while everything the gate reads sits in the same order as the fact it admits. Once policy and grammar live elsewhere, the gate's reads have their own basis and that basis is not implied by the fact's position. Then the verdict must say what it read, at what position. This is the strongest argument from inside the camp for Sid's "its yes or no is itself a fact naming what it checked."

---
**datalog L303-304 · BODY · DISAGREES E1**

- REPORTED. Refusals: Datomic does not keep them. XTDB v2 does (section 4). Hickey's principle points toward keeping them: "you have to reify process. You have to turn it into a thing that you can look at and touch." [H-DD]. Nubank's regret points the same way: "We don't see every request that happens before the database write and the events are effectively lost." [N-QCON]


---
**datalog L309-310 · BODY · DISAGREES E8,A2**

- INFERRED. One session entity holds the runtime's commit and the machine. Each saying points at the session. Written once, not per fact. A rebuild is a new session entity with a new commit. It is a fact because it happened and because later doubt will need it: "which facts were admitted by the gate as built from commit X" is a walk, provided the commit was written.


---
**datalog L313-313 · BODY · NEW-REASON E7**

- REPORTED. The camp's line is between information and "stuff". "There are other things you might use storages for, other than information. Sometimes you need a place to keep stuff. Datomic is not about keeping stuff." [H-DD]. High-churn values get no history: "For high churn attributes, such as a counter or version incrementer, the cost of storing history is frequently not worth the impact on database size or indexing performance." [D-BEST]

---
**datalog L314-314 · BODY · NEW-CASE E7**

- REPORTED regret. Nubank: Datomic "is really good for high-value business data. It doesn't work that well for fire hose writes… So we use Datomic as the default database for everything, and you are lulled into the false sense of security." [N-QCON]

---
**datalog L315-315 · BODY · DISAGREES E7**

- INFERRED. Pan and hover are a firehose. Selecting and pointing are different when they end in an act: then they are part of what that act stood on, and they ride on its saying. The camp's default would be: a motion becomes a fact when it becomes the basis of something, not before.

---
**datalog L316-317 · BODY · NEW-REASON E5,E2**

- INFERRED, from Hickey's definition of a fact as "an event or thing known to have happened". The system *knows* it showed something. It does not know the person looked. "Shown" is a fact the store can attest. "Looked" is a conclusion someone draws. The camp would record the first and derive the second, and would not let the record of the first be read as the second.


---
**datalog L320-320 · BODY · DISAGREES E4,C8**

- The camp is nearly silent. Datomic runs transaction functions inside the writer and trusts whoever can transact. The one relevant warning is the one quoted under (8).

---
**datalog L321-322 · BODY · NEW-REASON E4**

- INFERRED. Since tools are facts and policies are facts, "may act for" is a fact the gate reads. The camp's only contribution is from *Spec-ulation*: a grant, once relied on, should grow and not be silently narrowed. Narrowing is a new grant with a new name.


---
**datalog L337-338 · BODY · NEW-REASON E2,C2**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.5 What resembles Sid's situation, and what differs*

- Time travel is used for exactly what Sid wants it for: "what was I looking at", "what was given". That is recording time, which is the use Waeselynck says works.


---
**datalog L345-345 · BODY · DISAGREES E6**

- **Reads.** Datomic never records a read. Sid records reads as provenance and at crossings.

---
**datalog L351-351 · BODY · NEW-REASON E3**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

- **XTDB**, on time. Datomic: transaction time only, everything else is an attribute. XTDB: valid time is a first-class axis on every record.

---
**datalog L393-394 · BODY · NEW-CASE E1**
*3. Nubank: Datomic lived with, at scale › 3.3 What they regret*

3. **Offers that never became facts were lost.** "something I would do differently is think about event sourcing a little bit earlier. We have it down streamed from Datomic, but not up streamed. We don't see every request that happens before the database write and the events are effectively lost." [N-QCON]


---
**datalog L395-396 · BODY · ABOVE E7**

4. **The fact store was used for things that are not facts.** "it is really good for high-value business data. It doesn't work that well for fire hose writes, or long strings… So we use Datomic as the default database for everything, and you are lulled into the false sense of security, I will start a service with Datomic, when the answer should be S3." [N-QCON]


---
**datalog L403-403 · BODY · NEW-CASE E1**
*3. Nubank: Datomic lived with, at scale › 3.4 Which questions they speak to*

- **(7) refusals.** REPORTED regret 3. They wish they had the requests that came before the write. That is the offer stream, refusals included.

---
**datalog L406-406 · BODY · ABOVE E7**

- **(14) the hand.** REPORTED regret 4. Firehose data does not belong in the fact store.

---
**datalog L431-432 · BODY · NEW-REASON E3**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.1 What they built, and what they chose*

Every record has two times: system (transaction) time and valid time.


---
**datalog L445-448 · BODY · NEW-REASON E3**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.2 Their reasons*

**Why two times.**

> "In situations where your database is not the ultimate owner of the data—where corrections to data can flow in from various sources and at various times—use of transaction-time is inappropriate for historical queries." [X-BITEMP]


---
**datalog L449-450 · BODY · NEW-REASON E3**

They name three drivers: "lag, corrections, and efficient auditability." And a rule of thumb from the v2 docs: "In short, any time you hear the phrase 'as of' or 'with effect from' in a requirement, the answer is probably 'valid time'." [X-TIME]


---
**datalog L477-478 · BODY · CARRIED E1,E3,A1**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.3 What they changed, and what bit them*

8. **Determinism on every node, relaxed in 2.2.** "Since its inception, XT has had an architecture where every node reads the whole log and resolves all of the transactions… it means that every transaction has to be deterministic - that every node has to arrive at the same state reading the same messages… it did mean that any kind of error had to restart all of the nodes." And, candidly: "even in hindsight I wouldn't have changed that decision, it got us off the ground." [X-LEAD]. What it cost: "every feature had to be expressible as a pure, deterministic function of the log — which ruled out anything that wanted to draw per-transaction metadata (e.g. tx-id, system-time) from outside the log." [X-DBS]. From 2.2 one leader reads the *source log*, resolves each transaction, and writes the result to a *replica log* that the other nodes follow. "'Transaction resolution' here means turning SQL (which may read existing data in order to determine what changes to make) into simpler put, delete and erase events (which don't)." [X-LEAD]


---
**datalog L489-489 · BODY · CARRIED E1**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.4 Which questions they speak to*

- **(7) the gate's yes or no, and refusals.** REPORTED. This is the camp's precedent for Sid's gate. In XTDB, transactions are appended to the log *before* they are judged: "unconfirmed transactions are optimistically appended, and therefore a transaction in XTDB is not confirmed until a node reads from the transaction log and confirms it locally." [X-FAQ]. Every submission gets a number and a place in the order, whether or not it commits. The verdict is kept, refusals included. The docs show the table: `_id | committed | error | system_time`, with a row `2 | f | ... "Precondition failed" ...`. "Check the xt.txs table for the transaction result to see if the assertion failed." [X-TXS]. Since 2.1 a caller can attach its own metadata to a transaction, stored beside the verdict: "you might use this to attach upstream request IDs, correlation IDs, or other data lineage information." [X-TXS]

---
**datalog L490-490 · BODY · CARRIED E1,A1**

- **(12) and the runtime that will be rebuilt hundreds of times.** INFERRED from item 8. XTDB began by recording only offers and making every node re-derive the outcome. That forces every future build to reproduce every past verdict exactly, for ever. Seven years on (Crux previewed in April 2019; the single-writer post is dated 17 September 2026) they started recording the resolved outcome as its own log. Sid's gate is the only writer and its verdict is a fact, so Sid is already on the side XTDB moved to. The lesson is to stay there: the record is what the gate decided, never a promise that a later gate would decide the same.

---
**datalog L497-498 · BODY · CARRIED E1,C5**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.5 Resembles and differs*

Resembles: immutability and erasure designed together; offers appended then judged; refusals kept; one partition; a runtime rewritten from scratch while the data had to survive. Differs: the unit is a document or row, not a fact; no provenance per record beyond the transaction; no permissions at the level of a fact; one organisation per database.


---
**datalog L501-501 · BODY · CARRIED E1,E3**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.6 Who they disagree with*

- **Datomic**, on valid time, and on the writer: Datomic has an active transactor, XTDB "a passive transaction log". [X-FAQ]

---
**datalog L506-509 · BODY · CARRIED E1,A3**

**What XTDB implies for a second store.** Version 2.1 lets one cluster serve several databases. Version 2.2's single writer was introduced partly to allow "change-data-capture-fed secondaries". So their road to a second store is: one store is the writer of record, another follows its resolved log. Not two equal writers.

---


---
**datalog L534-537 · BODY · NEW-REASON E6**
*5. The leads › 5.2 Nikita Prokopov (DataScript)*

**The Web After Tomorrow (2015) is about "match, don't route".** REPORTED.

> "The same query will then be used to filter whole-DB changelog and decide what parts of it server should push to which client. Fetch is about trying to get the data given the query. Push is about finding the affected subscriptions given the changed data." [T-WEB]


---
**datalog L540-543 · BODY · ABOVE E6**

And the part he says is unsolved:

> "Subscription language is the biggest missing piece of this stack. Datomic and DataScript speak Datalog which is very powerful language, but it is hard to reverse efficiently. If you have very high volume of the transactions going through your DB, for each of them you'll have to determine which clients should get an update. Running a Datalog query per client is not an option." [T-WEB]


---
**datalog L570-571 · BODY · ABOVE E6**
*6. The group: who matters most, who I dropped, who is missin*

A fourth for one question only: **Prokopov**, because *The Web After Tomorrow* is the clearest written statement of match-don't-route in this camp, and of why the matching language has to be weaker than the query language.


---
**frontiers L41-42 · BODY · DISAGREES E3**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.2 The reasons, in McSherry's words*

**What a timestamp is.** It is an instruction, not a measurement. REPORTED: "Virtual time is a technique for distributed systems that says events should be timestamped prescriptively rather than descriptively. The recorded time says when an event should happen, rather than when it did happen." And: "The virtual time an update is assigned becomes the truth about when that update happens. These times must reflect constraints on the input: updates in the same input transaction must be given the same virtual time, updates that are ordered in the input must be given virtual times that respect that order. Once recorded, the explicitly timestamped history is now unambiguous on matters of concurrency." ([Virtual Time for Scalable Performance, Materialize blog, 14 June 2022](https://materialize.com/blog/virtual-time-consistency-scalability/))


---
**frontiers L63-64 · BODY · ABOVE E6,A1**

**Explaining an answer.** Explanations are computed on demand from timestamped inputs. They are not stored on each output. REPORTED: the goal is "a subset of the input collection whose output under the computation agrees with the actual output". "when we explain records in a differential dataflow computation, we will ask for explanations *at a specific timestamp*". And the warning about recording everything: keeping every contributing input "ends up being all the edges in the connected component. Accurate, but not yet helpful." Of the two record-everything baselines: "Both of these approach end up asking for pretty much all the edges in the graph. This isn't helpful." (Explaining outputs, 2016)


---
**frontiers L67-68 · BODY · DISAGREES E1,P0**

**A gate written as a view.** In 2025 McSherry sketched transaction commit as a maintained view. REPORTED: "we'll write all transaction intents to a table, and maintain a view over the table that reports which transactions commit and which roll back." "Many transactions can be summed up by their *read sets* and *write sets*. Read sets are the values that the transaction read (or failed to read, if absent)." Cleanup is separate and later: "We can remove failed transactions, remove the read sets of committed transactions, and remove writes that are themselves overwritten without being observed". McSherry's own caveat: "This post is largely for educational purposes; please do not actually implement transactions this way without having a hard think about what you need." ([Transaction Processing in the Data Plane, 2025](https://github.com/frankmcsherry/blog/blob/master/posts/2025-04-27.md))


---
**frontiers L87-87 · BODY · DISAGREES E3**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(11) when: whose clock, used for order?** REPORTED: the ingest point's clock, and it *is* the order ("becomes the truth about when that update happens"). Materialize uses milliseconds since the epoch as the value, but an oracle hands them out, so they never go backward and ties are decided. INFERRED for Sid: if the gate's `when` is not the order, the camp would ask what it is for. A second, ordering time next to a decorative wall-clock time is two sources of truth.

---
**frontiers L89-89 · BODY · NEW-REASON E5**

- **(4) follow the latest or stay on the version read?** REPORTED basis: explanations are always "at a specific timestamp". INFERRED: a recorded read is always pinned. "Following" is a view computed over pinned reads plus newer versions. That view is what Sid calls staleness.

---
**frontiers L90-90 · BODY · ABOVE E6,A1**

- **(4)/(11) which reads, and do they all count?** REPORTED: full provenance is "Accurate, but not yet helpful"; useful explanations are minimal and computed on demand. REPORTED (2025): a transaction's read set includes what it "failed to read, if absent". INFERRED: for anything deterministic, record times and logic, not per-output reads. For anything not re-derivable, record the read set, including reads that came back empty.

---
**frontiers L92-92 · BODY · DISAGREES E1**

- **(7) keep the gate's yes/no and refusals?** REPORTED sketch: intents all land; the verdict is a view over them; failed intents can be tidied later. INFERRED: keep offers and verdicts in a collection that may have its own, faster-moving `since`.

---
**frontiers L126-126 · BODY · NEW-REASON E2,C2**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.2 The reasons, in Brandon's words*

3. "Confusing changes with corrections": "this stream does not distinguish between changes to the correct value and corrections of intermediate outputs."

---
**frontiers L137-138 · BODY · NEW-REASON E6**

**Dependency tracking at the wrong grain.** Brandon's map splits incremental systems into unstructured (each node is one value, any edge) and structured (each node is a collection, edges are a fixed set of operators). REPORTED: "unstructured systems tend to struggle with controlling the granularity of incremental computation. Make the values in each node too big and you do a lot of unnecessary recomputation. Make them too small and the overhead of the graph metadata dominates the actual computation."


---
**frontiers L139-140 · BODY · NEW-REASON E6**

**Eager or lazy.** REPORTED: "lazy systems can't provide notifications when a certain output appears or changes because they won't even know about the output until asked to calculate it." and "people tend to use eager systems and approximate laziness where needed by either running ad-hoc queries against the output or by adding a list of outputs-to-maintain into the inputs."


---
**frontiers L156-156 · BODY · NEW-REASON E6**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.4 Which questions Brandon speaks to*

- **(11)/(4) reads on every fact.** INFERRED from the unstructured-versus-structured warning: per-fact read lists are the unstructured road, where "the overhead of the graph metadata dominates". Brandon would record reads at pattern grain with a time, not one entry per matched fact.

---
**frontiers L158-158 · BODY · NEW-REASON E2,C2**

- **(14) shown versus looked.** INFERRED from "confusing changes with corrections": a "what was shown" record should say whether the answer shown was complete for its as-of. A partial answer that was shown is a different fact from a complete one.

---
**frontiers L160-161 · BODY · NEW-REASON E6**

- **Standing patterns.** REPORTED: make the list of wanted outputs an input.


---
**frontiers L190-191 · BODY · NEW-REASON E3**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.2 The reasons, in their words*

**Time as data.** REPORTED, Dedalus abstract: earlier languages pushed state and delay outside the logic, which "forces programmers to think operationally. We argue that the missing component from these previous languages is a notion of time." Every fact carries a time column. A fact persists only because a rule carries it to the next moment. A message's arrival time is chosen by the receiver's world, not the sender's: each asynchronous head "will take some unspecified time suffix value". The sender's time may ride along as plain data. They call this entanglement: "recording a binding of both the time value of the deduction and the time value of its consequence." "It allows a rule to reference the logical clock time of the deduction that produced one (or more) of its subgoals". ([Dedalus: Datalog in Time and Space, Alvaro, Marczak, Conway, Hellerstein, Maier, Sears; tech report 2009](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2009/EECS-2009-173.pdf))


---
**frontiers L194-195 · BODY · NEW-REASON E5**

**Lineage as a working tool.** REPORTED: "A lineage-driven fault injector reasons backwards from correct system outcomes to determine whether failures in the execution could have prevented the outcome." The programmer's job is "to use data lineage to reason about the redundancy of support (or lack thereof)". An outcome can have several independent supports. It is in danger only if every one of them can be knocked out. Molly turns lineage into a formula and asks a solver. ([Lineage-driven Fault Injection, Alvaro, Rosen, Hellerstein, SIGMOD 2015](https://people.ucsc.edu/~palvaro/molly.pdf))


---
**frontiers L199-199 · BODY · NEW-CASE E6**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.3 What changed or was regretted*

- **Fine lineage did not survive contact with production.** At Netflix, REPORTED: "Rule-based languages such as Dedalus also make it trivial to collect fine-grained data lineage during execution." But: "this requirement was not acceptable at Netflix. First, there were simply too many applications to port to Dedalus." So: "we had to look for another source of lineage data beyond the program text." They used request traces: "a call graph characterizes how a collection of services contributed to a system outcome, while LDFI's data lineage characterized how individual data elements and fine-grained computation steps contributed to an outcome. Because we were willing to sacrifice some precision in order to improve the performance of automated failure testing, it seemed that call graphs could stand in for lineage" (a figure interrupts this sentence in the PDF text). ([Automating Failure Testing Research at Internet Scale, Alvaro, Andrus, Sanden, Rosenthal, Basiri, Hochstein, SoCC 2016](https://people.ucsc.edu/~palvaro/socc16.pdf))

---
**frontiers L208-208 · BODY · DISAGREES E3**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.4 Which questions they speak to*

- **(11) whose clock, used for order?** REPORTED principles: the receiver assigns the time; the sender's time is data; order matters only for non-monotone steps. INFERRED: stamped by the gate, used for order exactly at supersession and absence checks, and not needed anywhere else.

---
**frontiers L209-209 · BODY · NEW-REASON E5,C2**

- **(11)/(4) based-on.** REPORTED: the manifest pattern. INFERRED: "based on" is a manifest. A reader of fact F can refuse to act until it holds everything F stood on. That turns based-on from an audit trail into a safety mechanism. REPORTED (entanglement): carrying the premise's time inside the consequence is a first-class idea, which is what "a read of one fact at its version" is.

---
**frontiers L210-210 · BODY · NEW-REASON E5**

- **(4) depends-on versus how-I-got-here.** INFERRED from LDFI: a flat list of reads cannot say "either of these would have done". Lineage that separates alternative supports lets doubt be computed properly: a fact is in doubt only when every support has a doubtful member. Without that split, doubt spreads too far, and a map that overstates doubt also lies.

---
**frontiers L214-214 · BODY · NEW-REASON E1**

- **(7) refusals.** INFERRED from LDFI: a refusal is lineage for something that did *not* happen. McSherry makes the same point from the other side: explanations must cover "the *absence* of output records".

---
**frontiers L215-216 · BODY · NEW-REASON E6**

- **Tools as facts, with opaque bodies.** REPORTED lesson from Netflix: lineage is free when the program is rules, and coarse when it is opaque code. INFERRED: Sid's tool signatures are declarative, so reads made by matching are free to record. Reads made inside a body can be captured only if the runtime is the body's only way to read.


---
**frontiers L219-220 · BODY · NEW-REASON E6,A1**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.5 Resemblance and difference*

Resembles: facts with time as a column; rules that fire on facts; lineage as the way to explain; a sharp line between re-derivable and not. Bloom and Dedalus are the nearest academic relatives of "tools are facts, matched not routed". Differs: they aimed to *avoid* a single writer; Sid chose one. They could assume programs written as rules; Sid's tool bodies are opaque and some are models. Their lineage was for testing a protocol, not for keeping forever on every fact.


---
**frontiers L252-252 · BODY · DISAGREES E8,A1**
*4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff;  › 4.4 Which questions they speak to*

- **(12) runtime version and machine kind.** REPORTED: reproducibility depends on processor architecture and on compiler and optimization settings, and "now" must be logged. INFERRED for Sid: "deterministically re-derivable from the same reads" is true only relative to tool version, runtime version, and machine kind. If those are not written at session start, a crossing record from years ago cannot be re-derived to the bit, only approximately. With hundreds of rebuilds planned, this is a first-record item.

---
**frontiers L254-254 · BODY · ABOVE E6**

- **Tools registered late.** INFERRED from the backfill post: "match landings to tools" is the incremental half of a standing query. A tool registered today is a query whose answer is defined over every earlier fact as well. Does a new tool see old facts? That is backfill, and it is a decision the design has to make on purpose.

---
**frontiers L326-327 · BODY · NEW-CASE E6,W1**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.3 What changed*

3DF is described in the past tense on Goebel's site. Goebel's present work, REPORTED from the home page: "make incremental view maintenance practical in the context of an expressive relational language, for use cases involving many thousands of views at petabyte scale", by "building a dataflow and memoization engine for demand-driven maintenance". In 2019 Goebel had written: "What will remain is the distinction between data-driven and demand-driven systems". So the person who built eager maintenance over facts moved to demand-driven maintenance once the count of views reached the thousands. 3DF's README is also frank: "Declarative is less efficient and much more opinionated than hand-written Differential Dataflow."


---
**frontiers L335-336 · BODY · NEW-REASON E6,W1**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.4 Which questions Goebel speaks to*

- **Standing patterns at scale.** REPORTED: demand-driven, memoized, at "many thousands of views".


---
**frontiers L363-364 · BODY · DISAGREES E3**
*7. Question by question: what this camp says*

**(11) When: whose clock, used for order?** The strongest agreement in the camp. The ingest point assigns the time. It is prescriptive. It is the order (McSherry, REPORTED). It may be a counter (DBSP, REPORTED) or look like wall-clock time if an oracle keeps it monotone (Materialize). The sender's time is ordinary data (Dedalus, REPORTED). Order is needed only where the logic is non-monotone (Hellerstein, REPORTED). INFERRED: one field is the order. If the gate's per-cell version is the order within a cell, then something must order across cells, or no cross-cell "as of" exists. A wall-clock `when` that is not that thing should not look like it.


---
**frontiers L369-370 · BODY · NEW-REASON E6,E5**

**(11) Based-on: every read?** At pattern grain with a time, not per matched fact (INFERRED from Brandon and McSherry). Include reads that found nothing (McSherry 2025, REPORTED). Include the match that made a tool fire: in lineage terms the rule firing *is* the edge (INFERRED from LDFI). Entries the runtime fills in for a reply are the coarse, call-graph kind of lineage, and should be marked as that kind (INFERRED from the Netflix paper).


---
**frontiers L371-372 · BODY · NEW-REASON E5**

**(4) Depends-on or path; follow or pin?** Recorded reads are pinned, always. Following the latest is a view over them (INFERRED from "explanations at a specific timestamp"). Separate what the fact needed from what was merely seen, and allow alternative supports, or doubt spreads too far (INFERRED from LDFI's redundancy of support and McSherry's "Accurate, but not yet helpful").


---
**frontiers L381-382 · BODY · DISAGREES E1**

**(7) Gate's yes/no; refusals.** Keep offers as the log of intent; the verdict can be computed from it (McSherry 2025, REPORTED as a sketch). Refusals are the lineage of what did not happen (INFERRED from LDFI and from McSherry's point about explaining absence). The camp would let them be trimmed sooner than facts.


---
**frontiers L407-407 · BODY · NEW-REASON E5,C1**
*8. Voices that matter most, who was dropped, who is missing*

- **Self-adjusting computation and build systems** (Umut Acar's work; "Build Systems à la Carte" by Mokhov, Mitchell, Peyton Jones). They record, on each built thing, the inputs it read and their hashes, then decide what to redo. That is "based on" plus "recompute when the reads move", studied for years. Brandon cites the build-systems paper as the map of the unstructured side.

---
**frontiers L445-445 · BODY · NEW-REASON E1,A3**
*11. What each source implies for a second store*

- **Goebel, REPORTED:** send only intent-less facts. Never let two stores interpret the same offers.

---
**log L77-77 · BODY · NEW-REASON E3**
*Voice by voice › 1. Pat Helland*

- On time: "The contents of a message are always from the past, never from now." and "There is no simultaneity at a distance." (*Outside versus Inside*.)

---
**log L78-78 · BODY · NEW-REASON E1**

- On retry: "The entity must durably remember the transition from a message being OK to process into the state where the message will not have substantive impact." and "if a reply is required, the same reply must be returned." (*Life beyond*.)

---
**log L92-92 · BODY · NEW-REASON E1**

- (7) REPORTED: the decision must be durable and the same reply must come back on retry. He does not say the decision is a record in the same log.

---
**log L93-93 · BODY · NEW-REASON E3**

- (11 when) REPORTED: every service has its own now; data seen is "unlocked and an artifact of the past."

---
**log L94-94 · BODY · NEW-REASON E6**

- (11 based-on) REPORTED, a small but useful point: processing stays idempotent "even if a log record describing the read is written. The log record is not substantive to the behavior of the entity." Recording reads does not change what a write means.

---
**log L104-105 · BODY · CARRIED E1,C3**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

**1. What he built and chose.** A fifteen-year line of systems where a shared log is the only interface. CORFU (2012): a log over flash, positions handed out by a sequencer. Tango (2013): in-memory objects that are views of the log, with transactions as records in the log. FuzzyLog (2018): the log relaxed to a partial order for sharding and geography. Delos (Meta, 2020 and 2021): the log itself made swappable, with protocol "engines" stacked over it; in production for years. Then a single-author retrospective in 2024.


---
**log L109-109 · BODY · DISAGREES E6,E5**

- A write carries its reads, and everyone judges it the same way: "each commit record contains a read set: a list of objects read by the transaction along with their versions, where the version is simply the last offset in the shared log that modified the object. A transaction only succeeds if none of its reads are stale when the commit record is encountered". Each client decides "independently but deterministically". (Tango, SOSP 2013.)

---
**log L110-110 · BODY · NEW-REASON E1**

- When a reader cannot judge, the verdict is written into the log: "a client executing a transaction must insert a decision record for a transaction if there's some other client in the system that hosts an object in its write set but not all the objects in its read set." (Tango.)

---
**log L119-119 · BODY · NEW-REASON E3**

- Physical time enters only as a proposal: "To support time-based trimming in a way that is robust to clock skew and drift, we implemented a TimeEngine". (Delos, SOSP 2021.)

---
**log L134-134 · BODY · NEW-REASON E1,A1**

- (7) REPORTED: verdicts are written into the log whenever some reader could not re-derive them. INFERRED: Sid's gate is always in that case, because the code that would re-derive the verdict will be rebuilt hundreds of times. The verdict must be a record.

---
**log L181-181 · BODY · DISAGREES E1**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- (7) Looked for and not found: nothing in the book, the threads, or the documentation says rejected commands are stored. A wrong expected version is an error returned to the caller and leaves no record.

---
**log L194-194 · BODY · CARRIED E1**
*Voice by voice › 4. Martin Kleppmann*

- The gate, in his terms: "Going through a leader would still be useful if you want to validate that writes meet certain constraints before writing them to the log." (same.)

---
**log L195-195 · BODY · CARRIED E1,C3**

- Offer, then verdict, both recorded. A username claim "doesn't yet guarantee uniqueness; it merely establishes an ordering of claims. (If you're using a partitioned stream like a Kafka topic, you need to ensure that all claims to the same username go to the same partition.)" Then a processor "writes the outcome ("successfully registered" or "username already taken") to a separate "registrations" event stream." (*Making Sense of Stream Processing*, 2016.) Again in 2021: "an initial event represents only the intention to perform a certain action; then a stream processor joins that event with the current state to determine whether the action is permitted, and if so, emits a new event to a stream of validated events". (*Thinking in Events*, DEBS 2021.)

---
**log L199-199 · BODY · NEW-REASON E3,P0**

- Clock-made total orders are not logs: "Timestamp ordering produces a totally ordered sequence of events, but it is not a log, because new events are not always appended to the end." (same.)

---
**log L215-215 · BODY · CARRIED E1**

- (7) REPORTED: both outcomes recorded, in a separate stream.

---
**log L221-222 · BODY · NEW-REASON E6**

- (11 based-on) Unverified: my memory is that his book has a section on treating reads as events, for tracing what a person saw before deciding, with a warning about storage cost. Third-party book notes confirm the idea is there. I could not verify any wording and do not quote it.


---
**log L234-234 · BODY · NEW-CASE E1**
*Voice by voice › 5. Certificate Transparency, Trillian, and their descendants*

- Why the receipt was only a promise: waiting for a real position was the first design, but "CAs found this delay in their issuance pipelines to be unacceptable." So: "This signed timestamp is a promise for future inclusion in the log." (Laurie.)

---
**log L235-235 · BODY · NEW-REASON E1**

- What may enter: "In order to avoid logs being spammed into uselessness, it is required that each chain is rooted in a known CA certificate." (RFC 6962.) Laurie: "logs are useful only if their size is manageable."

---
**log L236-236 · BODY · NEW-REASON E3**

- Clock rules: the tree-head "timestamp MUST be at least as recent as the most recent SCT timestamp in the tree. Each subsequent timestamp MUST be more recent than the timestamp of the previous update." "TLS clients MUST reject SCTs whose timestamp is in the future." (RFC 6962.)

---
**log L244-244 · BODY · NEW-CASE E1**

- *The promise-now, include-later design failed in operation.* Google's Aviator log in 2016 blew through its 24-hour merge window. Let's Encrypt, 2025: "there have been multiple incidents in which important logs have exceeded their maximum merge delay, breaking that promise." Sunlight removed the gap: entries are sequenced before the receipt is returned, so "the effective merge delay is zero!"

---
**log L252-252 · BODY · DISAGREES E1**

- *Refusals leave no trace.* Chrome policy: "Rejected logging submissions must not be issued an SCT by the CT log." The only record of a refusal is the error sent back.

---
**log L260-260 · BODY · DISAGREES E1**

- (7) REPORTED: refusals are not recorded, on purpose, to control spam and growth.

---
**log L262-262 · BODY · NEW-REASON E3**

- (11 when) REPORTED: the log's clock, forced monotone, never trusted from the future.

---
**log L279-280 · BODY · NEW-REASON E5,C2**
*Voice by voice › 6. Lamport, as foundation*

INFERRED: Sid's based-on is Lamport's first remedy. A write that names what it read tells the system what came before it, across partitions, without any clock. So based-on is doing two jobs: provenance, and the only true cross-partition order the store will ever have.


---
**log L283-284 · BODY · DISAGREES E1**
*Voice by voice › 7. Phil Bernstein's Hyder*

**1. What they built and chose.** A database with no partitioning, where one shared log is everything. Each server runs a transaction against a snapshot and appends an "intention": its writes, plus its reads when the isolation level needs them. Every server then rolls the log forward through the same deterministic procedure, meld, which decides commit or abort. Compare Sid's gate: Hyder has no gate. The append is the only arbitration, and the verdict is a pure function of the log.


---
**log L286-286 · BODY · CARRIED E1**

- "The log is the database." (Bernstein, Reid, Das, *Hyder*, CIDR 2011.)

---
**log L287-287 · BODY · CARRIED E1**

- "Unlike conventional database systems, appending an intention to the log does not commit the transaction. Meld makes that decision." (Bernstein, Reid, Wu, Yuan, *Optimistic Concurrency Control by Melding Trees*, VLDB 2011.)

---
**log L289-289 · BODY · DISAGREES E1**

- "Since all servers (including T's executer) read the same log, they all make the same commit/abort decision regarding T." And: "The only point of arbitration between servers is the atomic append of an intention to the log." (CIDR 2011.)

---
**log L291-291 · BODY · CARRIED E1**

- Refused work stays in the log: "log records include the updates of both committed and aborted transactions." (CIDR 2011.)

---
**log L292-293 · BODY · DISAGREES E6**

- Reads cost nothing, because they are not recorded: "Since queries execute against snapshots, they are not logged or melded. Hence, they scale out linearly". (SIGMOD 2015.)


---
**log L302-302 · BODY · DISAGREES E1**

- (7) REPORTED: the verdict is derived and never written, and refused intentions stay in the log forever. Bernstein and Das state the contrast with Tango themselves: Tango "rolls forward the log to determine T's commit/abort decision and then writes that decision to the log." INFERRED: deriving works only while every judge runs identical code forever. Sid's runtime will be rebuilt hundreds of times, so Sid is in Tango's case, not Hyder's.

---
**log L304-304 · BODY · NEW-REASON E6,C2**

- (11 based-on), (5) REPORTED: an intention carries one snapshot position for everything it read. INFERRED: that is the compact form of a read set, and Sid's "pattern read as of a point" is the same idea.

---
**log L310-311 · BODY · DISAGREES E1**

**6. Disagreements.** With primary-copy replication ("Hyder does not use a primary copy!"). With shared-lock designs such as Oracle RAC. With Tango, on writing decisions down and on one log versus several. CORFU's authors returned the criticism, on hole handling.


---
**log L326-327 · BODY · DISAGREES E1,E3**
*Voice by voice › 8. Amazon Aurora, and what AWS did next*

- *Aurora DSQL* (2024 on) drops the single writer. "Each key in the database belongs to at most one adjudicator at any given time". "Each transaction's writes go atomically to a single journal, but we can have as many journals as we need." A total order is built per consumer: crossbars "merge-sort them into a total order for each subscriber shard." The journal holds only accepted work: "What goes on the Journal isn't requests for transactions, but committed transactions." A lagging index is never read: if a storage node is not current up to the transaction's start time, "the reader is made to wait until replication catches up." History is short by design: old row versions are forgotten after a fixed time, currently "five minutes before the current wall-clock time." Marc Brooker's stated reason for the redesign: "application programmers find dealing with eventual consistency difficult". On their clocks: "clock skew beyond the expected bounds causes the system to lose linearizability, but not isolation, durability, or atomicity."


---
**log L332-332 · BODY · DISAGREES E1**

- (7) REPORTED: Aurora derives commit from a watermark. DSQL logs only accepted work. Refusals leave no record in either.

---
**log L334-335 · BODY · NEW-REASON E6,C2**

- (4) INFERRED from DSQL's five-minute horizon and Aurora Backtrack's limit ("The limit for a backtrack window is 72 hours."): reading an index as of a point far in the past is something these systems refuse to do. Sid's "what was I looking at yesterday" cannot lean on an index answering old as-of reads unless that index is built to keep history. Years later the cheap question is "has anything matching this pattern landed since that frontier?". The expensive one is "what exactly did the pattern return then?".


---
**log L349-349 · BODY · CARRIED E3**
*Voice by voice › 9. Two short notes on voices the brief did not list*

- "Appends also store and index in the depot partition the time of the append." So Rama already keeps an append time per partition. If the gate's "when" is a separate stamp, the store holds two clocks for one event.

---
**log L417-418 · BODY · DISAGREES E3**
*Question by question: what this camp would say › (11) When: whose clock? Ever used for order?*

REPORTED, and close to unanimous. Order is position; the clock is a label. Kreps: the entry number is a timestamp "decoupled from any particular physical clock". Balakrishnan: a position "is effectively a logical timestamp". Tango sets itself against Spanner, which "uses real time as an ordering mechanism via synchronized clocks". Hyder never mentions a clock. Delos admits physical time only as a proposal that the log then orders. Automerge's wall-clock field is "optional". CT uses the log's own clock and forces it to be sane: a tree head's time "MUST be at least as recent as the most recent SCT timestamp in the tree", and clients reject timestamps "in the future". The exception is Aurora DSQL, which reads "as of" a physical time, and says plainly what breaks when the clock is wrong: linearizability, and nothing else. That road belongs to the Spanner group.


---
**log L419-420 · BODY · NEW-REASON E3**

Marz adds a distinction the envelope should respect: "A piece of data is a fact that you know to be true at some moment of time." That moment belongs to the statement. The gate's "when" is the moment of admission. For ten million papers ingested in a week, the two differ by decades.


---
**log L421-422 · BODY · CARRIED E3**

**For the first record (INFERRED).** The gate's clock stamps "when", and nothing ever orders by it. Make it monotone within a partition, as CT does, so it can never contradict position. Keep world-time in the value. Note that Rama already stores an append time per partition; decide whether the gate's "when" is that stamp or a second one.


---
**log L442-442 · BODY · DISAGREES E6,E5**
*Question by question: what this camp would say › (11) Based on: is every read listed?*

- *Validation.* Tango's commit record lists every object read with its version, and the write is void if any moved. Hyder's intention carries its read dependencies. Balakrishnan states the general law: a stored output "can only be applied … if the intervening entries did not invalidate it".

---
**log L446-447 · BODY · NEW-REASON E6,E2**

On size: Kleppmann records direct parents only, "which ensures that this set remains small"; Tango writes "only one commit record per transaction" however many objects it touches; Hyder names a whole read set by one snapshot position. On whether recording a read changes anything: Helland says no: "The log record is not substantive". On reads of things outside the store: Young says copy the answer in, because the outside will change: "enrich the information returned from the call onto the event."


---
**log L454-455 · BODY · NEW-REASON E5,C1**
*Question by question: what this camp would say › (4) Based on: depends-on or merely passed through? Follow th*

INFERRED. The asymmetry decides it. From a pinned pointer, "the latest" can always be computed by walking forward. From a pointer that says only "latest", what was actually read is gone forever. Following the latest is what a running answer does; a stored fact pins. Whether the fact depends on a read or merely passed through it is a judgment available only at write time, so it has to be written then. Tango gives the precedent of two classes: reads inside the read set, which void the write if they move, and everything else.


---
**log L492-492 · BODY · DISAGREES E1**
*Question by question: what this camp would say › (7) Beside each fact: is the gate's yes or no kept? Where? A*

- *Log the offer, derive the verdict.* Hyder. Refused work stays in the log forever; the verdict is never written.

---
**log L493-493 · BODY · CARRIED E1**

- *Log the offer, write the verdict.* Tango's decision records. Kleppmann's pattern, twice, with both outcomes going "to a separate "registrations" event stream."

---
**log L494-495 · BODY · DISAGREES E1**

- *Judge first, log only what passed.* Aurora, DSQL ("committed transactions"), Event Store, CT. A refusal is an error message and leaves no record. CT does this on purpose: "Rejected logging submissions must not be issued an SCT", because "logs are useful only if their size is manageable."


---
**log L496-497 · BODY · NEW-REASON E1**

Helland supplies the requirement that cuts across all three: the receiver "must durably remember the transition", and on retry "the same reply must be returned." His bank example also bounds it: a check must clear "in less than one year", which "limits the list of cleared checks the bank must maintain".


---
**log L498-499 · BODY · DISAGREES E1**

**For the first record (INFERRED).** A yes is a fact, and should name everything it checked: the grammar version, the policy version, the expected version, plus the gate's epoch, the runtime version and the store. None of those can be recovered later. A no must live at least as long as a retry can arrive, keyed by the offer's id, so the same offer gets the same answer. Whether refusals live forever is a policy choice with a spam cost. Kleppmann's separate stream is the middle road, and Rama's depot trimming can apply to that stream without touching the fact log. A refusal that points at an offer nobody kept explains nothing, so decide whether refused offers are kept with their verdicts. Logging offers before judging them has one more benefit that matters to a runtime rebuilt hundreds of times: a new gate can be run against the old offers and its verdicts compared with the recorded ones.


---
**log L508-509 · BODY · NEW-REASON E7,E5**
*Question by question: what this camp would say › (14) The hand, and (15) a click*

The camp has little here, and I would not lean on it. What it offers is about grain. Kleppmann in 2015: a shopper adding and then removing an item has "information value". His team in 2019: "CRDTs store all history, including character-by-character text edits. These pile up", and they could not be trimmed. Young: events are named for what happened in the domain, and "do not use the word “And” in an event name". INFERRED: record acts at the grain of intent, not motion samples; and if motion is kept at all, keep it where trimming is allowed (a session layer, a separate stream). On being shown versus looking, the Claimant Model helps: the runtime can vouch for "shown"; only the person, or an instrument, can vouch for "looked". They are two claims by two actors. On which tools may act in a person's name, this camp has nothing beyond Kleppmann's 2026 remark that an interface "defines which buttons the AI is allowed to press."


---
**log L635-635 · BODY · DISAGREES E3**
*Appendix: material touching names moved to other sessions*

- **Spanner, seen from this camp.** Tango defines itself against it: "Spanner [18] uses real time as an ordering mechanism via synchronized clocks". Helland's 2016 revision of *Life beyond Distributed Transactions* concedes that Spanner-class systems work, and removes his 2007 "Maginot Line" comparison.

---
**log L636-636 · BODY · DISAGREES E3,C2**

- **Clock-based "as of" in production.** Aurora DSQL reads as of each transaction's start time, taken from EC2's precision clocks, makes lagging storage wait, and states what fails when a clock is wrong: "clock skew beyond the expected bounds causes the system to lose linearizability, but not isolation, durability, or atomicity." It keeps only five minutes of old versions. Source: Brooker et al., arXiv 2607.13276.

---
**meaning L244-248 · BODY · NEW-REASON E4**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

> What is the minimal "stuff" that could be part of the "TCP/IP" apparatus that
> could allow "meanings" to be sent, not just bits -- and what assumptions need
> to be made on the receiving end to guarantee the safety of a transmitted
> meaning?


---
**meaning L249-257 · BODY · NEW-REASON E4**

**Turn 9 — Hickey, 11962116, 23 June 16:13.** Five hours after Kay's TCP/IP comment.
A reply to `mmiller`'s gloss of Kay. This is Hickey's full position. I give it
almost whole, because it reads like a specification.

> If we can't agree on what words mean we can't communicate. This discussion is
> undermined by differing meanings for "data", to no purpose. You can of course
> instead send me a program that (better?) explains yourself, but I don't trust
> you enough to run it :)
>

---
**meaning L291-295 · BODY · NEW-REASON E4,A1**

> When interacting with an ambassador one may or may not get the facts, and may
> get different answers at different times. And one must always fear that some
> question you ask will start a war. Science couldn't have happened if consuming
> and reasoning about data had that irreproducibility and risk.
>

---
**meaning L365-367 · BODY · NEW-REASON E4**
*Part one — the exchange › 1.3 What each actually claimed*

7. Kay names the safety problem unprompted and leaves it open: "what assumptions
   need to be made on the receiving end to guarantee the safety of a transmitted
   meaning?"

---
**meaning L389-390 · BODY · NEW-REASON E4**

7. Trust: "I don't trust you enough to run it". And: "one must always fear that
   some question you ask will start a war."

---
**meaning L435-439 · BODY · NEW-REASON E4**
*Part one — the exchange › 1.5 What stayed unresolved*

### 1.5 What stayed unresolved

1. **Safety of a transmitted meaning.** Kay asked it. Hickey joked about it.
   Nobody worked on it. Note that this is the life's work of Mark S. Miller's
   camp, in Part two.

---
**meaning L514-516 · BODY · DISAGREES E5,E6,X2**
*Part one — the exchange › 1.6 My reading: is this the axis under Sid's questions?*

3. The pointer is on every fact from the first one. That is rows (3), (4),
   and (11 based on).


---
**meaning L517-525 · BODY · NEW-REASON E2,C2**
*Part one — the exchange › 1.7 What each would make of Sid's design (all INFERRED)*

### 1.7 What each would make of Sid's design (all INFERRED)

**Hickey.** Hickey would recognise it as close to home ground. Immutable,
accreting facts. Time on every fact. Provenance. Derivations kept apart from
facts, and recorded only with an as-of. "Running answers never stored, only
crossings recorded" is Hickey's date-of-birth and age distinction made into a rule.
A crossing is a derivation that has been "temporally-qualified". Hickey would
press on four things.


---
**meaning L630-635 · BODY · NEW-CASE E4**
*Part two — the people › 2.1 The capability camp: Hardy, Miller, Yee, Shapiro, Donnel*

- Hardy on what happens when you patch a who-may-do-what rule set. REPORTED:
  "Every time we added a clause enabling the opening of a file in a categorical
  situation we would introduce security problems in programs that had been
  secure. Every time we added restrictions to these categories we broke other
  legitimate programs. The last time that I wrote down the requirements for a
  program to open a file, it required fourteen boolean operators".

---
**meaning L652-656 · BODY · NEW-REASON E4**

- Miller, "Robust Composition" §3.1, against a static policy table. REPORTED:
  "you often do not know in advance what authorities the program actually
  needs: the least authority needed by the program changes as execution
  progresses". And: "we must provide the right amount of authority
  just-in-time".

---
**meaning L694-698 · BODY · NEW-CASE E4**

**What they changed or regret.**

- Hardy's team first patched the deputy with a "switch hats" call. REPORTED:
  "Note the increase in complexity! [...] It soon became clear, however, that
  more than two "authorities" were necessary".

---
**meaning L731-746 · BODY · DISAGREES E4**

- **The deputy in Sid's design** (INFERRED, and the main point). Put three of
  Sid's choices together. Tools are matched to landed facts; nobody chose to
  invoke them. Policy is "this actor may write this key in this layer". A tool
  produces offers. Now ask whose authority the tool's offer carries. If it is
  the authority of the person whose fact matched, that authority is "exercised,
  but not selected, by its user": ambient, by their definition. Anyone who can
  land a tool whose signature matches your facts gets code run as you. If
  instead the tool is its own actor with its own policy rows, it is Hardy's
  compiler. It holds write rights from its author, and takes its target from
  the fact that matched. A person who cannot write to a cell can land a fact
  that steers a tool that can. The gate sees a permitted actor writing a
  permitted key and says yes. The camp's repair is that the offer must *select*
  the authority it uses. It cites the grant it is exercising. The gate checks
  that chain, not a row for the actor. That also puts "under what authority" on
  the record for every fact, which is one more thing that cannot be added
  later.

---
**meaning L769-778 · BODY · NEW-REASON E4,C8**

**Transfer: what is like Sid's case and what is not.** Their systems are live
objects passing references. Sid's is inert facts and one gate. A central gate
that decides is, to this camp, the access-list architecture. But a gate can
check capability-shaped policy: a chain of grant facts cited by the offer.
Miller, Yee, and Shapiro criticise certificate schemes where the permission is
not bound to the request, so purpose cannot be known. An offer that cites its
grant is bound to the request. The other difference: this camp avoids global
logs and non-repudiation on purpose. Sid wants a permanent record. Horton shows
they accept logs; they do not accept logs as proof.


---
**meaning L779-788 · BODY · ABOVE E4**

**Who they disagree with.** Access-list and role-based designs in general
(Hardy: "Exercise for the reader: Show that access lists do not solve this
problem"). Boebert and Gong on whether capabilities can confine. Lemmer-Webber
against Bluesky's architecture (below), and against four "anti-solutions":
blocklists, content filtering, reputation scoring, re-centralisation. Inside
the camp, Varda against purists (HN 16093457): "capability people tend to get
too extreme, and this tends to lead to failure. With Sandstorm we've tried to be
more pragmatic." And (HN 16098698): "I don't necessarily recommend exposing pure capabilities
in a UI for end users."


---
**meaning L1006-1017 · BODY · NEW-CASE E5**
*Part two — the people › 2.3 Unison (Paul Chiusano, Rúnar Bjarnason, Arya Irani), and*

**Follow the latest, or stay on the version read? (4)** REPORTED (Chiusano,
2015): "we never modify a definition in place, causing other code to break. When
we modify some code, we are creating a new version, referenced by no one. It is
up to us to then propagate that change to the transitive dependents of the old
code." (2020): "correct definitions should never require upgrading." So: stay on
the version read. Following is a separate, explicit act that makes new versions
of the dependents. The regret is the tooling for that act. REPORTED (Rebecca
Mark, 2023): "Unison's process for updating code, merging code, and upgrading
library dependencies is the roughest part of the experience right now." "It's
easy to accidentally replace your human readable names with mysterious hashes."
Patches, the structure that recorded replacements, were deprecated in 2024.


---
**meaning L1234-1239 · BODY · NEW-CASE E5**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

- A provenance slot filled by machine became noise. INSTITUTIONAL
  (Help:Sources): "Statements that are only supported by "P143" are not
  considered sourced statements". P143 is "imported from Wikimedia project",
  which bots fill in. It had to be ruled not-a-source. This bears on (11) and
  (4): a based-on list that the runtime fills with everything in reach will be
  read the same way.

---
**meaning L1277-1283 · BODY · NEW-REASON E5**
*Part two — the people › 2.6 RDF, W3C PROV, nanopublications, trusty URIs*

**PROV: a read is not a dependence.** This is question (4), answered by a
standard. INSTITUTIONAL, PROV-DM, https://www.w3.org/TR/prov-dm/:

- Usage: "the beginning of utilizing an entity by an activity."
- Derivation: "a transformation of an entity into another, an update of an
  entity resulting in a new one, or the construction of a new entity based on a
  pre-existing entity."

---
**meaning L1284-1292 · BODY · NEW-CASE E5**

- The cut between them: "If an artifact was used by an activity that also
  generated a new artifact, it does not always follow that the second artifact
  was derived from the first. In the activity of creating a painting, an artist
  may have mixed some paint that was never actually applied to the canvas: the
  painting would typically not be considered a derivation from the unused
  paint." And: "PROV does not attempt to specify the conditions under which
  derivations exist; rather, derivation is considered to have been determined by
  unspecified means. Thus, while a chain of usage and generation is necessary
  for a derivation to hold between entities, it is not sufficient".

---
**meaning L1293-1294 · BODY · NEW-REASON E5**

- There is a catch-all, influence, and the standard says not to lean on it: "It
  is RECOMMENDED to adopt these more specific relations".

---
**meaning L1295-1303 · BODY · NEW-REASON E5**

- INFERRED: two kinds of "based on" with two different sources. *Used* can be
  recorded by machinery: what was read, what the model was given. It is
  complete and noisy. *Derived from* can only be declared by the actor, and the
  actor can be wrong. For a deterministic tool the two are the same thing: its
  answer is a function of its reads. For a person or a model they differ, and
  only the usage list is reliable. Keep them as two relations. Do not let one
  pose as the other. Wikidata's P143 (2.5) shows what happens to a slot filled
  by machine and read as if it meant something.


---
**meaning L1318-1319 · BODY · NEW-REASON E1**

- (7): "A bundle is a named set of provenance descriptions, and is itself an
  entity, so allowing provenance of provenance to be expressed."

---
**meaning L1324-1332 · BODY · DISAGREES E6**

- Every read? No. "Applications are free to decide which level of granularity
  they want describe". Nothing in PROV requires any read to be recorded. The
  nearest remark on the cost of not recording is Luc Moreau (W3C interview,
  2013), REPORTED: "Sometimes you have to reconstruct provenance information
  because it wasn't recorded at the right time. This can be very tedious." On
  verdicts, Paul Groth in the same interview: "we did not standardize a single
  weighting system for this." So Sid's "every read on every fact" goes further
  than PROV's authors chose to go.


---
**meaning L1482-1488 · BODY · NEW-REASON E5**
*Part two — the people › 2.7 Kay beyond the thread, STEPS, Worlds, and Ingalls*

**Worlds are Sid's layers, built and measured.** Warth, Ohshima, Kaehler, Kay,
"Worlds: Controlling the Scope of Side Effects" (ECOOP 2011). A child world sees
its parent's state unless it has its own. Then:

- A read pins. REPORTED: "Once a variable (or slot, memory location, etc.) has
  been read or modified in a world w, subsequent changes to that variable in w's
  parent world are not visible in w." Unread slots keep following the parent.

---
**meaning L1498-1506 · BODY · NEW-REASON E1,E5**

- What this says to Sid, INFERRED. "Nearest layer wins" is Worlds lookup, and
  the cost of deep chains is real and was fixed in the runtime, not the model.
  Promotion is Worlds commit, with one difference. Worlds refuses a commit whose
  reads have moved. Sid's gate checks only the cell being written. Sid's answer
  to moved reads is to show staleness, not to refuse. That is a defensible
  choice. It should be a chosen one. It also means the gate's verdict should
  carry enough position to let anyone later decide whether a fact's reads were
  still current when it landed.


---
**meaning L1611-1613 · BODY · DISAGREES E2,E6**
*Part two — the people › 2.8 David Reed's pseudo-time, and Croquet*

Also: in Croquet "A view can read directly from a model at any time", and nothing
records it. Being shown is not a fact there.


---
**meaning L1666-1678 · BODY · NEW-CASE E5**
*Part two — the people › 2.9 Joe Armstrong*

**Follow the latest, or stay: said at the point of use.** REPORTED (thesis,
2003, §3.8): "The Erlang system allows for two versions of code for every module
[...] processes which execute code in this module can choose either to continue
executing the old code for the module, or to use the new code. The choice is
determined by how the code is called." A call written with the module name
follows the latest. A plain call stays. The limits: "it is the programmer's
responsibility to ensure that the new code to be called is compatible with the
old code." And: "Note there is a limit to two versions of the code. If a third
attempt is made to re-load the module then all processes executing code in the
first module will be killed." For (4): Erlang marks each use as follow or stay.
Folk marks it in the verb (3.1). Worlds pins on read (2.7). Unison always pins,
and following is a separate act (2.3). Nobody in this camp leaves it unsaid.


---
**meaning L1679-1693 · BODY · NEW-CASE E1**

**The gate is Armstrong's contract checker.** REPORTED (UBF paper, 2002): "Between the
components we place an entity which we call a contract checker [...] the contact
checker checks the legality of the flow of messages between the components."
A contract covers shape *and* the state of the conversation: "If I am in state S
and you send me a message of type T1 then I will reply with a message type T2
and move to state S1". A refusal says what was expected: "I was in state S and I
expected you to send me a message of type T but you sent me the message M which
is wrong." And "both client and server are informed". Thesis §9.2: "We can
impose a type system on our programming language, or we can impose a contract
checking mechanism between any two components [...] I prefer the use of a
contract checker." Armstrong's field note (thesis; the PDF text mangles "often"): "the
contract checker often complained about contract violations that I did not
believe [...] Almost invariably the contract checker was right and I was wrong.
I think we have a tendency to believe what we had expected to see".


---
**meaning L1694-1697 · BODY · NEW-REASON E1**

For (7), INFERRED: keep refusals, and make a refusal say what was expected and
what was received. Armstrong's reason is the best one available. Agents and people
believe what they expected to see. The refusal is the record that does not.


---
**meaning L1828-1835 · BODY · NEW-REASON E5**
*Part three — substrates › 3.1 Dynamicland's Realtalk, and Folk Computer*

**Based-on in miniature, and two kinds of read.** INSTITUTIONAL (Folk README):
"Any wishes/claims you make in the body will get automatically revoked if the
claim that the `When` was matching is revoked." That is a running answer with
its reads tracked, never stored. And they had to separate a read that creates a
dependence from one that does not. REPORTED (Rizwan, newsletter, Feb 2024):
`Query!` "is used on stuff like the web server to snapshot query the database as
it is right now, without introducing any new reactive dep".


---
**meaning L1847-1855 · BODY · NEW-CASE E2,C2**

- A consistent cut at the boundary. REPORTED (Sept 2025): "each invocation of
  the block gets an associated version object, and all downstream statements of
  the invocation are tagged with that version. [...] When the inflight counter on
  a version hits 0, that version is considered converged". Unconverged
  statements are filtered out of boundary queries "so we exclude them from 'side
  effects' at the boundary between Folk and the outside world". That is
  "because of" (every downstream statement tagged with what started it) and
  Sid's crossings (only a settled answer may leave), both added after years
  without them.

---
**meaning L1860-1865 · BODY · ABOVE E7**

- Events are still open. REPORTED (Feb 2024): the system "doesn't actually
  evaluate for every intermediate state. This is the behavior that you want for
  statements! [...] (If you _do_ want to process intermediate states, you want
  something with different semantics from normal statements, like event
  statements or something. This is an open question.)"


---
**meaning L1866-1875 · BODY · DISAGREES E7,E2**

**The hand.** For this camp almost nothing the hand does is recorded.
INSTITUTIONAL (Dynamicland FAQ): "Most Realtalk objects respond to the current
physical situation, the here and now, and do not remember anything. There's
very little "data"." Pointing and gaze are left to the room: "physical materials
[...] which naturally engage social cues such as pointing, line of sight, and
shared attention." "Objects can't see people (Realtalk does not track people)".
For (14): their default is that a motion is a statement while it lasts and
nothing afterwards. Only what someone chose to hold persists. "Being shown" is
not recorded at all.


---
**meaning L1904-1915 · BODY · ABOVE E4**
*Part three — substrates › 3.2 Linda (David Gelernter)*

**The hole, stated by the author as a feature.** REPORTED (same paper): "Tuple
names are global to a given program's TS. A tuple added to TS may be removed by
an in( ) statement occurring anywhere else in the program." In "Linda in
Context" (1989) they admit what monitors give that Linda does not: they "allow
all operations on a particular shared structure to be encapsulated in a simple
and language-enforced way". Ken Kahn and Mark Miller wrote a critical letter to
CACM in reply. The gatherer could not retrieve it, so I do not quote it. Carriero
and Gelernter's 1992 answer concedes the ground: "These issues are not
confronted by current Linda implementations which target parallel applications
where runtime performance (not reliability, security and so on) is the driving
consideration."


---
**meaning L1940-1951 · BODY · DISAGREES E7**
*Part three — substrates › 3.3 Webstrates (Klokmose, Eagan, Baader, Mackay, Beaudouin-L*

**Durable by default had to be walked back.** INSTITUTIONAL (Webstrates docs):
"By default, all changes made to the DOM get persisted on the server and
synchronized to all connected clients. If some data shouldn't get persisted,
special effort has to be made." So they added a `<transient>` element (it is not
in the 2015 paper at all), then a protected mode because "browser extensions or
libraries continually pollute the DOM", then throttling, because "moving the
cursor around for a second can easily generate 30-50 operations", with a
warning that throttling "is almost guranteed to eventually cause
inconsistencies". For (14): this is what "hand motions are facts by default"
looks like after a few years. They ended by dropping records, at a cost to
consistency.


---
**meaning L1952-1963 · BODY · NEW-CASE E1,C2,X4**

**Policy inside the medium, and time travel.** Permissions are an attribute in
the document. INSTITUTIONAL (docs): deleting and restoring had to be made a
separate admin right, "because otherwise a malicious user may simply delete the
webstrate and recreate it as their own, or restore the webstrate to before the
permissions were added and take over the webstrate." Also: "Permissions will
expire after 2 minutes", a window in which what is enforced is not what is
written, and nothing records it. INFERRED, for a store with "as of" reads: a read
of the past must be allowed or refused under *today's* policy, never the policy
as of then. Otherwise every restriction can be walked around by asking for the
day before. And the verdict must name the policy version it used, because caches
make "current" fuzzy.


---
**meaning L2044-2051 · BODY · DISAGREES E6,C5**
*Part three — substrates › 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tri*

**Never rewritten, versus never lost.** Nelson's deletion is removal from a
version's list of contents. The content keeps its address, and the older version
still lists it. That is Sid's question (0) answered as "never rewritten". But the
rule had an exception from the start. INSTITUTIONAL (Xanadu FAQ, requirement
1e): "Documents must remain accessible indefinitely, safe from any kind of loss,
damage, modification, censorship or removal except by the owner. It must be
impossible to falsify ownership or track individual readers of any document."


---
**meaning L2052-2059 · BODY · DISAGREES E6,C5**

Two things in that sentence cut against Sid's draft. The owner may withdraw. And
tracking readers must be *impossible*. "Every read recorded", "what was shown",
and "who read the erased thing" are the opposite design. INFERRED: the Xanadu
answer would be that a record of what I was shown is mine. It belongs in my
layer, readable by me, and by nobody else without my grant. "Who read the erased
thing" then needs either my consent or an operator who can see into every
personal layer. That is the god's-eye view Lemmer-Webber warns about.


---
**meaning L2306-2315 · BODY · DISAGREES E3**
*Part four — Sid's questions, hung on the parts of the fact › (11) When: whose clock, and is it ever used for order?*

### (11) When: whose clock, and is it ever used for order?

**Camp.** Order comes from an arbiter, not a clock. Croquet: models "have no
concept of real-world time"; the reflector stamps everything (2.8). Durable
Objects: order is arrival at the one thread (2.2). Hewitt: arrival order cannot be
deduced, only awaited (2.11). did:plc: a "server-generated timestamp" (2.4). A
writer's clock is a claim: AT Protocol says not to trust it (2.4). Reed is the
exception. Reed's pseudo-time comes from the writer's clock plus a site id, and it
*is* the order (2.8).


---
**meaning L2316-2324 · BODY · DISAGREES E3**

**My read.** Keep two things apart. Order is the gate's number, never a clock.
"When" is the gate's clock at admission. It is information, not order. But there
is a third time the envelope lacks: when the thing being recorded happened. A
paper from 1998 ingested in 2026. A person's note made offline. Wikidata keeps
this as start and end qualifiers, separate from rank (2.5). Hickey lists it first
among Hickey's open questions: "should data always incorporate time" (1.2). The
writer's time is a claim, and it should have a place, or it is gone forever for
every imported fact.


---
**meaning L2363-2373 · BODY · DISAGREES E6**
*Part four — Sid's questions, hung on the parts of the fact › (11) Based on: is every read listed?*

### (11) Based on: is every read listed?

**Camp.** PROV does not require any read to be recorded; "Applications are free
to decide which level of granularity" (2.6). Wikidata's machine-filled source slot
became noise and was ruled not-a-source (2.5). Folk tracks reactive reads
automatically and lets a program opt out (3.1). Croquet and Realtalk do not record
views at all (2.8, 3.1). Xanadu: tracking readers must be "impossible" (3.5). On
the other side, Moreau: reconstructing provenance later "can be very tedious"
(2.6). Reed and Worlds both record read sets, but only to validate a commit (2.7,
2.8).


---
**meaning L2383-2391 · BODY · NEW-REASON E5**
*Part four — Sid's questions, hung on the parts of the fact › (4) Based on: dependence or path? follow the latest, or stay*

### (4) Based on: dependence or path? follow the latest, or stay?

**Camp.** A read is not a dependence. PROV's unused paint (2.6). Everyone marks
the mode at the point of use. Erlang, per call (2.9). Folk, by the verb: `When`
follows, `Query!` samples (3.1). Worlds: a read pins (2.7). Unison: always pinned,
and following is a separate act; "correct definitions should never require
upgrading" (2.3). Armstrong: late-bound while developing, pinned when deployed
(2.9).


---
**meaning L2392-2399 · BODY · NEW-REASON E5**

**My read.** The second half of the question dissolves. A fact is about the past:
it stood on version N, and that never changes. Only a running answer follows. So
facts pin, always, and "stale" is computed as pinned version not equal to current.
The first half is a real decision. Each entry says whether the actor *depended* on
it or merely *had it*. For a deterministic tool the two are the same. For a person
or a model only the actor can say, and may be wrong. Record both and never merge
them.


---
**meaning L2465-2472 · BODY · NEW-REASON E1**
*Part four — Sid's questions, hung on the parts of the fact › (7) The gate's yes or no*

### (7) The gate's yes or no

**Camp.** Armstrong: a refusal says what was expected and what arrived, both sides
are told, and the checker is usually right where the human is wrong (2.9). did:plc
keeps even nullified operations (2.4). PROV bundles make provenance of provenance
ordinary (2.6). Webstrates shows why a verdict must name the policy version it used
(3.3). Wikidata checks after the fact and only reports (2.5).


---
**meaning L2473-2480 · BODY · NEW-REASON E1**

**My read.** Keep both, as facts that point at the offer's own id. A verdict names
the grammar version, the grant or policy it relied on, and the positions it checked
against. One caution the camp would raise. Refusals at machine rate are a way to
write into the store without permission. Lemmer-Webber's inbox spam and the
nanopublication registry's quotas are the same problem (2.1, 2.6). Keep the verdict
and the offer's id. Keep the body of a refused offer, if at all, in the offerer's
own layer and quota.


---
**meaning L2491-2496 · BODY · DISAGREES E8,A2**
*Part four — Sid's questions, hung on the parts of the fact › (12) The runtime's version, and rebuilds*

**My read.** Yes. "The runtime" as an actor has to mean *which* runtime. One fact
per build, one per session start, and each tool-made offer or crossing can reach
them. Go one step further if possible: tool bodies state the version of the body
language they were written against. The runtime gets rebuilt a hundred times. What
it implements can still be small, written down, and versioned.


---
**meaning L2497-2507 · BODY · DISAGREES E7,E2**
*Part four — Sid's questions, hung on the parts of the fact › (14) The hand*

### (14) The hand

**Camp.** Durable by default was tried and walked back. Webstrates: "30-50
operations" per second of cursor movement, then a transient element, a protected
mode, and throttling that breaks consistency (3.3). Realtalk remembers almost
nothing, and "Objects can't see people" (3.1). Folk drops intermediate states on
purpose, and treats events as an open question (3.1). Croquet records only what
passes through the reflector; views read freely and unrecorded (2.8). Xanadu
forbids tracking readers (3.5). The Powerbox makes one kind of motion matter: a
choice that grants (2.2).


---
**meaning L2508-2515 · BODY · DISAGREES E7**

**My read.** A motion becomes a fact when it changes state others rely on, when it
grants authority, or when a later fact needs to stand on it. Pan, hover, and
pointing are statements while they last, at most session facts that may be trimmed.
"Shown" and "looked" are different claims. The runtime can attest that it showed
something. Nothing can attest that a person looked. So record "shown", and never
derive "read" from it. "Who read the erased thing" is really "to whom was it
shown".


---
**meaning L2516-2522 · BODY · ABOVE E4,C8**
*Part four — Sid's questions, hung on the parts of the fact › (15) A click*

### (15) A click

**Camp.** Wrong question, they would say (2.1, 2.2). Nothing acts in a person's
name. A click designates, and the designation is the grant: "Which calendar should
the app use?" Authority is handed over "just-in-time", for a purpose, and can be
taken back.


---
**meaning L2523-2528 · BODY · DISAGREES E4**

**My read.** The click fact names the person, what was chosen, and the tool. The
tool's offers cite it. Tools that fire with no click run under standing grants,
which are separate, narrow, revocable facts. This is the repair for the deputy
problem in 2.1. It needs "under what authority" to be on the record from the first
fact.


---
**meaning L2555-2561 · BODY · DISAGREES E4**
*Part five — for the group › 7. The voices that matter most, who I dropped, who is missin*

1. **Mark Miller's camp** (Hardy's deputy, Horton, Lemmer-Webber's field
   experience, Varda's Sandstorm). They hold the one objection that gets *worse*
   because of Sid's central move. Matching tools to facts with nobody choosing
   them, plus policy keyed on the actor, is ambient authority by their definition.
   It cannot be patched later, because "under what authority was this written"
   is not on the early facts. And they have the constructive half too: the
   chain for "by whom", the grant for "acts for", the click that designates.

---
**rama L114-114 · BODY · CARRIED E3,C2**
*Part one — What Rama's own reference says › Question by question › (11) When: whose clock? Ever used for order?*

- **CHECKED** `docs/14-depots.md:196` — "Appends also store and index in the depot partition the time of the append. This is used for 'start from' options." This is the depot leader's clock. **NOT IN THE REFERENCE:** any way for topology code to read that time, or a record's offset.

---
**rama L115-115 · BODY · CARRIED E3**

- **CHECKED** `docs/06-tying-it-together.md:404` — RPL's own tutorial stamps time inside the ETL: `.each(System::currentTimeMillis).out("*joinedAtMillis")`. That is the clock of whichever machine leads that task.

---
**rama L116-116 · BODY · CARRIED E3**

- **CHECKED** `skill/testing.md:323` — module code should call `TopologyUtils/currentTimeMillis` so tests can control time.

---
**rama L119-120 · BODY · CARRIED E3,E1**

**IMPLIED:** the brief's "stamped by the gate at admission" would mean, in Rama, the wall clock of the task leader that ran the gate for that partition. Different partitions are led by different machines. So the stamp is comparable within a partition only as far as one machine's clock is steady, and across partitions only as far as NTP holds. A leader change can step it backwards. Rama's camp would record it and never sort by it. Under a stream gate, a retry re-reads the clock, so the same offer can get a different "when" on replay unless the first verdict is looked up and reused.


---
**rama L149-150 · BODY · NEW-REASON E6**
*Part one — What Rama's own reference says › Question by question › (11) Based on: is every read listed?*

**NOT IN THE REFERENCE.** Rama keeps no record of reads. Foreign selects, query topology invokes, and reactive subscriptions leave no trace. The only read guarantee it gives is monotonic: "You'll never read an earlier version of data from a PState than you've already read" (`docs/23-acid-semantics.md:83`). Recording reads is wholly Sid's design. **IMPLIED:** at machine write rates, each fact carrying its reads multiplies record size. The reference's rule of thumb for depot reads is about 50 KB per fetch (`docs/14-depots.md:242`), which hints at the record sizes RPL has in mind: small.


---
**rama L186-186 · BODY · NEW-REASON E1**
*Part one — What Rama's own reference says › Question by question › (7) Beside each fact: is the gate's yes/no kept? Where? Are *

- **CHECKED** `docs/14-depots.md:212` — "An exception doesn't mean the append did not go through – it just means it didn't go through cleanly."

---
**rama L187-187 · BODY · NEW-CASE E1**

- **CHECKED** `docs/11-stream-topologies.md:297` — under throttling, "Depot appends with AckLevel.ACK while the limit is hit will throw an exception back to clients. In these cases the appends will have gone through."

---
**rama L189-190 · BODY · NEW-REASON E1**

- **CHECKED** `skill/microbatch.md:129` — "A record that throws deterministically retries forever… blocking the topology. Malformed input must be rejected in dataflow, not allowed to throw."


---
**rama L191-192 · BODY · DISAGREES E1**

**IMPLIED:** (a) Every offer, refused or not, is already in the offers depot for good, so refusals are "kept" the moment the offer lands, unless something filters before the depot. (b) A refusal must be a value the gate writes, never an exception. (c) Under a stream gate, a replayed offer would find its own version already current and refuse itself, unless the gate first looks the offer up by id and returns the verdict it gave before. So a verdict index keyed by offer id is needed for correctness. (d) The offerer who got an exception needs the same index to learn what happened. Keeping the verdict is therefore forced. Where it lives: in a PState on the same task as the cell, written in the same event as the fact, so the two are atomic (`docs/23-acid-semantics.md:41`).


---
**rama L193-194 · BODY · NEW-REASON E1**

Can a fact and its yes/no land atomically? **CHECKED** yes. In a stream gate, if both writes are in one event on one task. In a microbatch gate, always.


---
**rama L195-196 · BODY · NEW-REASON E1**

How a refusal gets back to the offerer. **CHECKED** stream: `ack-return>` sends a value back through the `append` call (`docs/11-stream-topologies.md:200-202`). By default only the first attempt counts (`docs/14-depots.md:214`). **CHECKED** microbatch: it cannot. "Microbatch topologies do not integrate with depot appends because they run asynchronously to the appends" (`docs/12-microbatch-topologies.md:102`); `:ack` "confirms depot durability only" (`skill/microbatch.md:134`). The offerer reads the verdict PState, by polling or by a reactive `proxy` (`docs/15-pstates.md:436`).


---
**rama L208-209 · BODY · NEW-REASON E7**
*Part one — What Rama's own reference says › Question by question › (14) The hand, and (15) a click*

**NOT IN THE REFERENCE.** One number helps size (14): a stream gate tracks every record one at a time and is throttled per task (`topology.stream.max.executing.per.task`, `docs/11-stream-topologies.md:295`). Pointer motion as facts through a stream gate would compete with real writes for that budget.


---
**rama L227-227 · BODY · NEW-REASON E1,E7**
*Part one — What Rama's own reference says › The gate: stream or microbatch*

| | Stream gate | Microbatch gate |
| Latency | "single-digit millisecond" (`skill/stream.md:5`) | "at least 300ms" (`skill/phase-1-plan.md:112`) |

---
**rama L228-228 · BODY · NEW-REASON E1,C1**

| | Stream gate | Microbatch gate |
| Delivery | at-least-once (`docs/11-stream-topologies.md:156`) | exactly-once for PStates (`docs/12-microbatch-topologies.md:126`) |

---
**rama L230-230 · BODY · NEW-REASON E1**

| | Stream gate | Microbatch gate |
| Verdict to offerer | yes, `ack-return>` | no; read a PState |

---
**rama L233-233 · BODY · NEW-REASON E1**

| | Stream gate | Microbatch gate |
| One bad record | retried, then others continue | "retries forever… blocking the topology" |

---
**rama L237-238 · BODY · NEW-REASON E1,C3**

RPL's general advice: "Unless you require millisecond-level update latency for your PStates, you should generally prefer microbatch topologies" (`docs/12-microbatch-topologies.md:171`).


---
**rama L239-240 · BODY · DISAGREES E1,C1**

**IMPLIED, my reading.** Sid's "no optimism" rule changes the weight of that advice. If a not-yet-fact is never shown as a fact, then a person's own act does not appear until the gate has spoken. With a microbatch gate that is a third of a second at best, on every keystroke-level act, for everyone. With a stream gate it is milliseconds. So the latency row is not a tuning detail for Sid; it is felt in the hand. The price of the stream gate is on the rows below it: offers need client-made ids, verdicts must be kept, the compare-and-set must be written to be replay-safe, and everything that must agree must share a partition. Those are exactly first-record decisions. The microbatch gate would let Sid skip them and would give a global clock for free, but it makes admission for the whole planet depend on every task group being healthy at once. Rama modules can run both kinds of topology side by side. A stream topology for the interactive path with microbatch topologies for heavier derived indexes is a common Rama shape: RPL's Mastodon build and Multiply's backend both mix the two.


---
**rama L295-296 · BODY · DISAGREES E3**
*Part two — Nathan Marz › 3. What he later changed, regretted, or migrated*

**(b) From a set of timestamped facts to ordered logs.** In 2010–2012 the master dataset was a set. Duplicates were harmless. Order came from the timestamp inside each fact: "you may have multiple gender DataUnits for someone… you would select the most recent gender DataUnit." (*Thrift + Graphs*). In Rama, order comes from the depot partition ("local ordering"), and the timestamp is just a field: "The appended records would have three keys: userId, location, and timestamp. The depot is partitioned by userId." (HN 38942073). He moved from time-as-order to position-as-order.


---
**rama L299-300 · BODY · CARRIED E3,A1**

**(d) RPL now says in writing where recompute fails.** From RPL's migrations post (Sam Adams, 2024, INSTITUTIONAL, https://blog.redplanetlabs.com/2024/09/30/migrating-terabytes-of-data-instantly-can-your-alter-table-do-this/): recomputing from depots may be "infeasible or impossible" when "you've enabled depot trimming", when "your existing PStates have data that was non-deterministically generated", or when "scanning millions of depot records might be egregiously inefficient". The second case is exactly a gate that stamps clocks and versions.


---
**rama L303-304 · BODY · NEW-CASE E1,A3**

**(f) Retry safety got stricter.** His 2025 collaborative-editor post builds a version-checked write in a stream topology and never mentions retries (I searched the post for retry, idempotent, duplicate: no hits). RPL's 2026 agent guidance says "every write in a stream topology must be either naturally idempotent… or explicitly deduplicated." INSTITUTIONAL. The rule was learned between the two.


---
**rama L319-320 · BODY · DISAGREES E3**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(11) When.** REPORTED: every fact carries a time, and it is the time the statement was true or was made, supplied with the record. 2010–2012: that time orders facts. 2023 on: depot position orders facts and time is data. INFERRED: he would keep both a source time in the offer and the gate's stamp, and order by neither.


---
**rama L323-324 · BODY · DISAGREES E6**

**(11, 4, 5) Based on; recording reads.** He has never written about recording reads as provenance (HN search for "provenance": zero hits). Two things bear on it. First, his definition: a fact "stands on its own". In his vocabulary, something whose truth depends on what was read is not data. It is a view. Sid's split between offers and running answers is the same line. But Sid also hangs reads on raw facts (a person's act, based on what they saw). That has no counterpart in Marz's model. Second, REPORTED: his 2025 agent platform records this kind of thing, and records it in the runtime, not in the data. "A trace of every agent invoke is viewable… Every aspect of execution is captured, including node emits, node timings, model calls, token counts, database read/write latencies, tool calls, subagent invokes." (Agent-o-rama README, https://github.com/redplanetlabs/agent-o-rama). And: "model calls are automatically traced – this node didn't have to record any tracing info explicitly." (*Introducing Agent-o-rama*, 2025, https://blog.redplanetlabs.com/2025/11/03/introducing-agent-o-rama-build-trace-evaluate-and-monitor-stateful-llm-agents-in-java-or-clojure/). INFERRED: on "are entries the runtime fills in listed?", he would say the runtime should fill in nearly all of them, because authors forget. On where they live, he would keep them in a trace keyed by the run, beside the facts, not inside each fact.


---
**rama L344-344 · BODY · NEW-REASON E6**
*Part two — Nathan Marz › 5. What resembles Sid's situation, and what differs*

- He never recorded reads.

---
**rama L407-408 · BODY · DISAGREES E3**
*Part three — The dissent: Jay Kreps › 4. Which of Sid's questions he speaks to*

**(11) When.** REPORTED: order by log position, never by wall clock. Wall-clock time is something "you might include in your messages". He notes the other school: "Spanner—Not everyone loves logical time for their logs. Google's new database tries to use physical time and models the uncertainty of clock drift directly by treating the timestamp as a range."


---
**skeptics L42-43 · BODY · DISAGREES E3**
*1. The ordinary default, question by question*

**(11) When: whose clock, used for order?** The database server's `now()` for `created_at` and `updated_at`. Careful teams order by a sequence, a log position, or an offset. Many order by `created_at` anyway. Marc Brooker states the default creed, REPORTED: "real wall-clock physical time is great for human-consumption (like log timestamps and UI presentation), but shouldn't be relied on by computer for things like actually affect the operation of the system. This remains a solid starting point, the right default position, but the picture has always been more subtle." And on ordering writes by clock: "Using physical clocks to order writes is, for good reasons, controversial. In fact, most experienced distributed system builders would consider it a sin." ([It's About Time!, 2023](https://brooker.co.za/blog/2023/11/27/about-time.html))


---
**skeptics L48-49 · BODY · DISAGREES E6,E5**

**(11) Based on: is every read listed?** No read is listed. Reads are not data. Where the law demands it, read access goes to a separate audit log with its own retention (the AWS post above names pgAudit for this). Lineage exists per dataset in data platforms, not per row (COMMON PRACTICE).


---
**skeptics L50-51 · BODY · DISAGREES E5**

**(4) Depends-on versus path; follow or pin?** No default exists, because reads are not recorded.


---
**skeptics L54-55 · BODY · DISAGREES E5,E6**

**(11) Because of.** A trace id and a parent span id, or a correlation id and a causation id in message headers. OpenTelemetry, REPORTED: "Links exist so that you can associate one span with one or more spans, implying a causal relationship." ([OpenTelemetry, Traces](https://opentelemetry.io/docs/concepts/signals/traces/)). The shape is the same as Sid's: one parent (because of), many links (based on). The difference is fate: traces are sampled and expire.


---
**skeptics L60-61 · BODY · DISAGREES E1**

**(7) Is the gate's yes/no kept? Refusals?** Success is the row existing. Refusals go to application logs or a dead-letter queue and expire. Even Amazon's newest database keeps only accepted work in its log. Brooker, REPORTED: "What goes on the Journal isn't requests for transactions, but committed transactions." ([DSQL Vignette: Transactions and Durability, 2024](https://brooker.co.za/blog/2024/12/05/inside-dsql-writes.html))


---
**skeptics L64-65 · BODY · DISAGREES E7**

**(14) The hand.** Clicks and views go to a separate analytics pipeline, sampled, outside the system of record (COMMON PRACTICE; no source opened).


---
**skeptics L66-67 · BODY · DISAGREES E4**

**(15) A click: which tools may act in a person's name?** OAuth scopes granted once on a consent screen, usually broad (COMMON PRACTICE). Pavlo, REPORTED: "it remains good practice only to grant minimal privileges to accounts. Restricting accounts is especially important with unmonitored agents that may start going wild all up in your database."


---
**skeptics L117-118 · BODY · NEW-REASON E4**
*2. The skeptics › 2.2 Andy Pavlo, generally*

Pavlo's yearly reviews track which systems survive. REPORTED (2025 review): a section titled "The Dominance of PostgreSQL Continues". On agents, REPORTED: "An interesting feature that has proven helpful for agents is database branching. Although not specific to MCP servers, branching allows agents to test database changes quickly without affecting production applications. Neon reported in July 2025 that agents create 80% of their databases." And the warning quoted above: "nobody should trust an application with unfettered database access". And: "Enterprise DBMSs already have automated guardrails and other safety mechanisms that open-source systems lack, and thus, they are better prepared for an agentic ecosystem."


---
**skeptics L119-120 · BODY · ABOVE E4**

INFERRED for Sid: Pavlo's picture of agents and data is a disposable private branch per agent plus least privilege per account. That is the default's version of Sid's session layer and policy facts. Pavlo would accept the need. Pavlo would doubt that a new store is needed to meet it.


---
**skeptics L157-158 · BODY · DISAGREES E3,C2,C3**
*3. Big-tech operational lessons › 3.1 Amazon*

There is more than one adjudicator. How does a reader know it has seen everything up to a time? REPORTED (the post's math symbols are written here in plain text): "when the adjudicator allows a transaction to commit at τ_commit it also promises to never commit another transaction at an earlier timestamp. Once storage has seen a transaction with a timestamp greater than τ_start from every adjudicator, it knows it has the full set of data. But that could take a long time, especially if there write rate is low. We solve this with a type of heartbeat protocol, where adjudicators promise to move their commit points forward in lock step with the physical clock, and share that commitment with storage." ([DSQL Vignette: Transactions and Durability, 2024](https://brooker.co.za/blog/2024/12/05/inside-dsql-writes.html))


---
**skeptics L159-160 · BODY · DISAGREES E3,C2**

On clocks, Brooker walks up a ladder from "for the amusement of humans" to ordering writes. The consistent-read recipe, REPORTED (math symbols in plain text): "the client picks its T_request start, then goes to a replica and says 'wait until you're sure you've seen all the writes before T_request start, then do this read for me'. This complicates writes somewhat (writes need to be totally ordered in an order consistent with physical time), but makes consistent reads easy." The trade: "Relying on physical time allows distributed systems to avoid coordination in some cases where it would have otherwise been necessary. However, if that time is wrong, the result will also likely be wrong."


---
**skeptics L161-162 · BODY · DISAGREES E3,E1,C3,C2**

What this says to Sid. INFERRED: (10) Two gates can serve one store if each owns a disjoint set of cells, each promises never to stamp backward, and each keeps announcing how far it has got even when idle. A reader then takes the minimum over the gates. That is the frontier idea of the group A report, in production at Amazon. (11) The gate's clock can be the order if the gate makes it monotone. (7) Amazon keeps refusals out of the log. (1) and (10) Compare-and-set under tens of agents per person means hot cells and retries. Brooker's advice is to design so that no single key gets hotter as load grows. INFERRED: a person's most active cells are exactly such keys.


---
**skeptics L169-170 · BODY · NEW-REASON E6,A1**

Constant work (Colm MacCárthaigh). REPORTED: "many of our most reliable systems use very simple, very dumb, very reliable constant work patterns." "One, they don't scale up or slow down with load or stress. Two, they don't have modes, which means they do the same operations in all conditions." On caches: "most caches have modes. So, when a cache is empty, response times get much worse, and that can make the system unstable." The Route 53 example pushes a full table every few seconds instead of sending changes. ([Reliability, constant work, and a good cup of coffee; archived](https://web.archive.org/web/2023/https://aws.amazon.com/builders-library/reliability-and-constant-work/))


---
**skeptics L171-172 · BODY · ABOVE E6,C1**

What this says to Sid. INFERRED: (1) An offer should carry an id minted by whoever makes it, and that id should stay on the fact. A content hash cannot tell a retry from a second, deliberate, identical statement. Two people asserting the same relation are two facts. This is in tension with group A's finding that content-plus-time identity makes re-delivery harmless. The two fit together if the hash is a helper (Amazon's "deep validation", the IETF "fingerprint") and the minted id is the identity. "No optimism" has an Amazon cousin in "no fallback": one path, made reliable. "Recompute when reads move" is work in proportion to change, which is the opposite of constant work. A burst of landings, such as seeding ten million papers, becomes a burst of recomputation. MacCárthaigh would ask which paths must not have that shape. The gate's view of policy is the first candidate.


---
**skeptics L224-225 · BODY · NEW-REASON E3,C2**
*3. Big-tech operational lessons › 3.4 Google: Hyrum's Law, and two instances of the fix*

- (11). Spanner's well-known answer to clocks. REPORTED: "If the uncertainty is large, Spanner slows down to wait out that uncertainty." ([Spanner, OSDI 2012](https://static.googleusercontent.com/media/research.google.com/en//archive/spanner-osdi2012.pdf)). Brooker's DSQL note describes the same family of idea with AWS's clocks.


---
**skeptics L238-238 · BODY · DISAGREES E3**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (11) when | DB `now()`; often misused for order | Brooker: wall clock is for humans by default; ordering by it is "a sin" unless error is bounded (REPORTED). DSQL, Spanner: bounded clocks plus waiting (REPORTED) |

---
**skeptics L240-240 · BODY · DISAGREES E3,C2,C3**

| Sid's question | Ordinary default | Skeptics and big tech |
| (10) order, as-of | One number on one primary; per-partition offsets on a log | DSQL: several adjudicators, each monotone, heartbeats, reader waits for all (REPORTED). DynamoDB: one leader per partition (REPORTED). TAO: cross-shard writes not atomic, repaired later (REPORTED) |

---
**skeptics L241-241 · BODY · DISAGREES E6,E5**

| Sid's question | Ordinary default | Skeptics and big tech |
| (11) based-on | Not recorded | Dynamo: a writer "must specify which version it is updating" (REPORTED). Zanzibar: the permission read's freshness is stored with the content (REPORTED) |

---
**skeptics L244-244 · BODY · DISAGREES E5,E6**

| Sid's question | Ordinary default | Skeptics and big tech |
| (11) because-of | Trace id and parent span; sampled; expires | OpenTelemetry links imply "a causal relationship" (REPORTED) |

---
**skeptics L247-247 · BODY · DISAGREES E1**

| Sid's question | Ordinary default | Skeptics and big tech |
| (7) verdicts, refusals | Success is the row; refusals in expiring logs | DSQL: the Journal holds "committed transactions", not requests (REPORTED). Stripe: the first response is cached and replayed (REPORTED) |

---
**skeptics L249-249 · BODY · DISAGREES E7**

| Sid's question | Ordinary default | Skeptics and big tech |
| (14) the hand | Separate analytics pipeline | No source found |

---
**skeptics L250-250 · BODY · DISAGREES E4**

| Sid's question | Ordinary default | Skeptics and big tech |
| (15) a click | OAuth scopes, broad | Pavlo on agents and privilege (REPORTED) |

---
**skeptics L258-258 · BODY · DISAGREES E3,C2**
*5. Voices that matter most, who was dropped, who is missing*

1. **Marc Brooker, on DSQL and clocks.** DSQL is a gate, an ordered log of accepted work, and derived storage, built at Amazon in 2024. It shows how several gates can share one store (monotone promises and heartbeats), what optimistic checks cost on hot keys, and where a wall clock can be trusted.

---
**sync L147-149 · BODY · NEW-REASON E1,A3**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

- The expected state is a query, not a number: "Each Write operation includes a
  dependency check consisting of an application-supplied query and its expected
  result." (same)

---
**sync L158-160 · BODY · NEW-REASON E3**

- No synchronized clocks: "There is no requirement that servers have
  synchronized clocks, which is crucial since trying to ensure clock
  synchronization across portable computers is problematic." (SOSP 1995)

---
**sync L171-178 · BODY · DISAGREES E6,E8**

- A session is two sets: "read-set = set of WIDs for the Writes that are
  relevant to session Reads write-set = set of WIDs for those Writes performed
  in the session". The dependency set of a read is defined: "RelevantWrites(S,t,R)
  is a smallest set that is 'enough' to completely determine the result of R."
  The server returns it: "This presumes that the server can compute the
  relevant Writes and return this information along with the Read result."
  (Terry et al., "Session Guarantees for Weakly Consistent Replicated Data",
  PDIS 1994, https://www.cs.utexas.edu/users/dahlin/Classes/GradOS/papers/SessionGuaranteesPDIS.pdf)

---
**sync L207-209 · BODY · DISAGREES E1**

- Refusals sit outside the store: "By convention, most Bayou data collections
  include an error log for unresolvable conflicts. Such conventions, however,
  are outside the domain of the Bayou storage system." (SOSP 1995)

---
**sync L220-223 · BODY · NEW-REASON E1,E6**

- (4) The "expected version" generalises to "a query and its expected result".
  The dependency set of a read is "the smallest set … enough to completely
  determine the result", computed by the server, not declared by the client.
  REPORTED.

---
**sync L230-232 · BODY · DISAGREES E1**

- (7) Conflicts that cannot be resolved go to an in-band error log by
  convention, not by the store. REPORTED. They would likely say the same of
  refusals: keep them as data, in the application's own terms. INFERRED.

---
**sync L237-237 · BODY · NEW-REASON E3**

- (11 when) Per-server monotonic stamps; clocks need not agree. REPORTED.

---
**sync L267-271 · BODY · NEW-REASON E6**
*2. Section one: sync and multiplayer › 2.2 Convex (Sujay Jayakar, James Cowling, Jamie Turner; 2021*

**1. What they built and chose.** A hosted reactive database. Transactions are
deterministic JavaScript functions. One committer writes an append-only
transaction log. Every query's read set is recorded and used twice: to detect
commit conflicts, and to know when a live query must rerun.


---
**sync L277-279 · BODY · NEW-REASON E6**

- "After querying the index, we record the index range we scanned in the
  transaction's read set. The read set precisely records all of the data that a
  transaction queried."

---
**sync L280-284 · BODY · NEW-REASON E6**

- One mechanism, two uses: "we detect whether the query's result would have
  changed using the exact same algorithm the committer uses for detecting
  serializability conflicts: Walk the log after the query's begin timestamp and
  see if any entry overlaps… the sync worker reruns the function and pushes its
  updated return value to the client."

---
**sync L288-290 · BODY · NEW-REASON E3**

- One writer: "The committer in our system is the sole writer to the
  transaction log… The committer starts by first assigning a commit timestamp
  to the transaction that's larger than all previously committed transactions."

---
**sync L303-305 · BODY · NEW-REASON E3**

- Two times on one record: commit timestamps are "Hybrid Logical Clocks of
  nanoseconds since the Unix epoch"; the visible `_creationTime` is separate
  "and has different guarantees compared to the commit timestamp."

---
**sync L306-312 · BODY · NEW-REASON E1,A3**

- The offer, in their words: "treating the transaction as a declarative
  proposal to write records on the basis of any read record versions (the 'read
  set'). At the end of the transaction, the writes all commit if every version
  in the read set is still the latest version of that record." They add: "This
  is akin to being unable to push your Git repository because you're not at
  HEAD." (Convex docs, "OCC and Atomicity", https://docs.convex.dev/database/advanced/occ)


---
**sync L341-346 · BODY · NEW-REASON E5,E6**

- (11 reads), (4) Reads are recorded by the runtime, as index *ranges*, so a
  later insert into the range counts as a change. No one declares reads by
  hand. REPORTED. Every recorded read is a real dependency, because the
  function is deterministic. REPORTED in effect; the generalisation to Sid's
  "role of a read" is INFERRED: for a deterministic tool, all reads are
  dependencies; for a model or a person, none can be proven to be.

---
**sync L351-352 · BODY · NEW-REASON E3**

- (11 when) Two clocks with different guarantees: an ordering clock (HLC, set
  by the committer) and a display time. REPORTED.

---
**sync L368-373 · BODY · DISAGREES E6**

**5. Resemblance and difference.** Closest living system to Sid's based-on for
running answers, and to the gate as sole writer. Different: Convex documents
are mutable rows with no provenance kept on them; read sets live in memory for
live queries and are not stored as history. Convex offers optimistic updates;
Sid refuses them. Convex is per-customer, not one store for everyone.


---
**sync L395-400 · BODY · NEW-REASON E3,A1**
*2. Section one: sync and multiplayer › 2.3 Croquet and TeaTime (David A. Smith, David P. Reed, Alan*

- "Croquet's collaboration architecture is based upon the concept of replicated
  versioned objects coordinated by a universal timebase embedded in the
  communications protocol." And: "I/O events exist in real time, and provide
  the coordination between real time and 'pseudo-time'". (Smith, Kay, Raab,
  Reed, "Croquet — A Collaboration System Architecture", C5 2003,
  https://worrydream.com/refs/Smith_DA_2003_-_Croquet,_A_Collaboration_System_Architecture.pdf)

---
**sync L412-417 · BODY · DISAGREES E7**

- The hand stays out of the record: "For events published by a view and
  received by a model, the data needs to be serializable, because it will be
  sent via the reflector to all users. For view-to-view events it can be any
  value or object." (same) And smoothness is a view trick: "we do automatic
  in-betweening in the view by decoupling the rendering position from the model
  position". (croquet/multiblaster-tutorial README, read 2026-09-20)

---
**sync L441-448 · BODY · DISAGREES E8**

- (12) The running code version is part of the world's identity. REPORTED.
  For Sid: write the runtime build and the tool-body versions down at session
  start, because "deterministically re-derivable" is only true per version.
  INFERRED. Croquet's regret is the warning on the other side: do not let the
  code version become part of the *data's* identity, or a rebuild orphans the
  data. They needed a hand-written, version-free projection to survive
  rebuilds. For Sid this is already the design (facts outlive runtimes); the
  lesson is for running answers and crossings only. INFERRED.

---
**sync L450-451 · BODY · DISAGREES E7**

- (14) Pointer smoothing and hover are view-to-view and never become records.
  Only what must change the shared model is serialised. REPORTED.

---
**sync L452-453 · BODY · NEW-REASON E3,A1**

- (11 when) The reflector's time is the only time inside the model; "Never use
  `Date.now()` in models". REPORTED.

---
**sync L500-502 · BODY · NEW-REASON E1**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

- The server checks more than shape: "Figma's multiplayer servers reject parent
  property updates that would cause a cycle … Clients can't reject changes from
  the server because the server is the ultimate authority."

---
**sync L567-570 · BODY · ABOVE E1**

- (7) The gate must sometimes check an invariant across many records (no
  cycles in the tree), not only shape, version and policy of one record.
  REPORTED for Figma. For Sid: the brief's three checks have no home for
  cross-cell invariants. INFERRED.

---
**sync L621-622 · BODY · DISAGREES E1**
*2. Section one: sync and multiplayer › 2.5 Linear (Tuomas Artman)*

- Refusals vanish: a rejected transaction will "undo any changes made on the
  client side and be removed from the `executingTransaction` queue."

---
**sync L628-636 · BODY · DISAGREES E1,C6**

**4. Which questions.** (10) One global number works at Linear's size, and it
leaks: any customer can watch the whole system's write rate in the counter.
REPORTED fact, INFERRED reading. (1) The server's number is the database
version; entity ids are client-made UUIDs. REPORTED. (7) Refusals are not kept.
REPORTED. (6) The base value of a pending change is rewritten on rebase, so the
original "what I thought I was replacing" is lost. REPORTED. No-optimism:
Linear is the nearest product to Sid's rule. The durable local copy never
holds an unconfirmed change; only the in-memory model does. REPORTED.


---
**sync L669-675 · BODY · NEW-REASON E4,E7**
*2. Section one: sync and multiplayer › 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)*

- What is recorded is the intent: "Zero then sends a mutation (a record of the
  mutator having run with certain arguments) to your server's push endpoint",
  which ends by "recording the fact that the mutation ran". And: "The result
  from the client mutator is considered speculative and is discarded as soon as
  the result from the server mutator is known." (Zero docs, "Custom Mutators",
  https://zero.rocicorp.dev/docs/custom-mutators, read through web.archive.org
  because the live page would not open)

---
**sync L686-689 · BODY · DISAGREES E2**

- A record of what was shown, deliberately not durable: "A Client View Record
  (CVR) is a minimal representation of a Client View snapshot", and "The storage
  doesn't need to be durable — if the CVR is lost, the server can just send a
  reset patch." (row-version page)

---
**sync L690-693 · BODY · NEW-REASON E4,A3**

- Why a gate at all: "you get fine-grained authorization for free …
  Implementing this would be quite difficult with a CRDT, because there is no
  place to put the logic that rejects an unauthorized change." (Rocicorp,
  "Ready Player Two", 2023-10-18, https://rocicorp.dev/blog/ready-player-two)

---
**sync L708-712 · BODY · DISAGREES E1**

- A failing mutation is not kept as data. The documented push handler logs the
  error, aborts, and retries in an "errorMode" that skips the business logic
  but still advances the client's last mutation id, so one bad mutation cannot
  block the queue. (global-version and row-version pages; paraphrase from
  source)

---
**sync L731-735 · BODY · NEW-REASON E4,E7**

- (7), (15) The server is the only place a rejection can live. REPORTED. What
  is recorded is the named mutator and its arguments, which is the record of a
  person's act; the server's effects follow from it. REPORTED. For Sid: a click
  becomes "this named action with these arguments, on what I was shown", and
  tools act *because of* that fact, as themselves. INFERRED.

---
**sync L739-743 · BODY · DISAGREES E2,E1**

**5. Resemblance and difference.** Same authority, same compare against a base,
same view that the offer is an intent. Different: speculative results are shown
unmarked by default; the server's mutation log is not an audit record; CVRs
are throwaway.


---
**sync L758-763 · BODY · DISAGREES E7**
*2. Section one: sync and multiplayer › 2.7 tldraw sync (Steve Ruiz)*

- Three scopes, given in the docs as a table (paraphrased here, not quoted):
  *document* records are persisted and synced (shapes, pages, bindings);
  *session* records are optionally persisted and not synced (current page,
  camera position); *presence* records are synced and not persisted (cursor
  positions, user selection). In their words: "Presence records sync to other
  users in real time but aren't saved." (same)

---
**sync L778-785 · BODY · DISAGREES E7,C1,C4**

**4. Which questions.** (14) The clearest answer in the camp: pointer and
selection are *presence* (shared live, never saved); camera and current page
are *session* (kept locally, never shared); only shapes are *document*.
REPORTED. (2), (3) They put the type in the id on purpose, for safety.
REPORTED. Convex did the same and reversed in 2025 (2.2). (3) Kind names are
words in one flat namespace that "must not collide". REPORTED. (16), (10)
Comments needed a separately permissioned partition. REPORTED.


---
**sync L923-925 · BODY · NEW-REASON E5,P0**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- (4) List the frontier, not the closure. Judge a record from its recorded past
  only. A missing dependency makes a record undeliverable, not invalid; do not
  drop it. REPORTED, the last with a regret attached.

---
**sync L935-939 · BODY · NEW-CASE E4,E2**

- (7) A 2026 pattern worth noting: "an intent record (with a hash of the
  authorized content) is written into the document itself… If the document
  changes between intent and publish, the hash mismatch blocks the publish".
  REPORTED. It is a gate that checks a person approved *this exact content*.


---
**sync L1029-1039 · BODY · NEW-CASE E2,E5,A1**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- A record of what was shown, built in 2026: "One patchwork tool listens to all
  of the 'open document' events in the system and logs them into an 'Account
  History' document. For each entry, it keeps track of the timestamp, the
  document url and the heads at the time it was opened, and the tool it was
  opened with. This minimal set lets you see exactly what was viewed, when, and
  how." A colleague's margin note: "You're saving the heads (to see it as it
  was). Might you also save the tool heads?" And on reopening: "it opens the
  most recent version of the document (even though we saved the heads),
  because that is the expected user experience". (grjte, "Account History",
  2026-03-17, https://www.inkandswitch.com/patchwork/notebook/account-history/)


---
**sync L1063-1066 · BODY · NEW-REASON E1**

- A refusal that carries its own repair: "when a peer rejects a message due to
  an old timestamp, the rejecting peer sends their current timestamp along with
  the rejection message."


---
**sync L1070-1071 · BODY · NEW-REASON E7**

- The delight: "We were frequently (and unexpectedly) delighted by the
  persistent-by-default UI state."

---
**sync L1072-1077 · BODY · NEW-CASE E7,C4**

- The cost: "Our prototype stores all state, including ephemeral UI state that
  would normally live exclusively in the main object graph, in the database, so
  any change to the layout of that ephemeral state forced a migration… In most
  cases, we chose to simply delete the relevant tables and recreate them while
  in development, which essentially recreates the traditional workflow with
  ephemeral state."

---
**sync L1084-1089 · BODY · DISAGREES E7**

- On the hand: "some fast-changing data (e.g. mouse position or animation
  timers) could swamp the CRDT with low-value information; there is no reason
  to persist such updates. We therefore divide the application state into two
  parts: persistent state that is replicated, and ephemeral state that exists
  only locally." With an admitted gap: "ephemeral data is not associated with a
  particular CRDT state".

---
**sync L1133-1135 · BODY · DISAGREES E7**

- (14) Do not persist the hand. If you do, tie each sample to the version of
  what was on screen, which they did not. Persisting all UI state is delightful
  and makes every UI change a schema migration. REPORTED.

---
**sync L1136-1137 · BODY · NEW-REASON E5,A1**

- (4), (12) Record the pinned version *and* the tool; reopen the latest by
  default; someone will ask for the tool's version too. REPORTED.

---
**sync L1138-1139 · BODY · NEW-REASON E3,C3**

- (10), (11 when) A centre exists to impose one order. There is no objective
  "first". REPORTED.

---
**sync L1140-1142 · BODY · NEW-REASON E1**

- (7) A refusal should carry what the refused party needs to fix itself.
  REPORTED (for clocks). Generalised to Sid's gate: INFERRED.


---
**sync L1231-1234 · BODY · NEW-REASON E5,E6**
*2. Section one: sync and multiplayer › 2.11 Seph Gentle (ShareDB, Google Wave, diamond-types, Eg-wa*

- Parents are captured, not declared: "the previous frontier in the replica's
  local copy of the graph becomes the new event's parents." And they are the
  context of meaning: "The set of parents of an event in the graph is the
  version of the document in which that operation must be interpreted."

---
**sync L1251-1254 · BODY · NEW-REASON E6**

- The common case is compressed: "By default we assume that every event has
  exactly one parent, namely its predecessor in the topological sort. Any
  events for which this is not true are listed explicitly." (paper, §3.8)


---
**sync L1270-1271 · BODY · NEW-REASON E5,E6**

- (4), (11 reads) The context an act was made in is part of the act. Capture it
  mechanically. REPORTED.

---
**sync L1277-1279 · BODY · NEW-REASON E6,C7**

- (11 because-of) Compress the common case: assume the obvious single parent,
  list only exceptions. REPORTED.


---
**sync L1449-1453 · BODY · NEW-CASE E3**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- *Timestamps stay "Unfortunate"* (his section heading). "createdAt and
  indexedAt can be combined in to a sortAt for use in display and ordering
  posts in feeds. This continues to cause confusion and consternation, and some
  folks would think that global reliable timestamps are important enough to
  warrant an extension to the protocol." (same)

---
**sync L1502-1503 · BODY · NEW-REASON E5,C1**

- (4) A reference either floats or pins, and the record's author chooses per
  reference. INSTITUTIONAL.

---
**sync L1519-1520 · BODY · NEW-CASE E3**

- (11 when) Client time and indexer time are both kept, and combined for
  display. REPORTED, with regret.

---
**sync L1553-1557 · BODY · DISAGREES E3,P0**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

- Storage behaviour is encoded in the kind number. For replaceable kinds, "only
  the latest event MUST be stored by relays, older versions MAY be discarded."
  Ephemeral kinds are not stored. The winner between versions is chosen by the
  author's clock, then by hash: "the event with the lowest id (first in lexical
  order) should be retained".

---
**sync L1558-1559 · BODY · DISAGREES E1**

- A refusal is a wire message to one client (`OK … false` with a reason
  prefix), never a stored thing.

---
**sync L1616-1617 · BODY · DISAGREES E3**

- (11 when) The author's clock decides which version wins, so it can be gamed.
  REPORTED.

---
**sync L1699-1704 · BODY · DISAGREES E3**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

- *Clocks, argued in full.* "any system that *relies* on accurate timestamps is
  a centralised system in disguise." Willow accepts arbitrary author timestamps
  and bounds the damage: "An Entry with a high timestamp only overwrites
  Entries in the same subspace." ("Timestamps, Really?",
  https://willowprotocol.org/more/timestamps_really/index.html)


---
**sync L1713-1714 · BODY · NEW-REASON E5,C4**

- (4) Whether a reference floats or pins is a *type of field*, chosen in the
  grammar. REPORTED.

---
**sync L1725-1727 · BODY · NEW-REASON E3**

- (11 when) Sid's store is centralised openly, not "in disguise", so the
  objection does not bite; the bound on blast radius still does. INFERRED.


---
**sync L1808-1813 · BODY · NEW-REASON E3**
*3. Section two: versioning › 3.1 Git (Linus Torvalds, Junio Hamano, Jeff King, brian m. c*

- Order is the graph, never the clock. Where a clock is used, it is repaired
  against the graph: a commit's "corrected committer date" is "the maximum of
  its committer date and one more than the largest corrected committer date
  among its parents." The raw date is a heuristic that "is not used when the
  topological order is required (such as merge base calculations)." (Git,
  "commit-graph" technical doc)

---
**sync L1852-1856 · BODY · NEW-CASE E3,C2**

- *A number that should have been there.* Generation numbers had to be added in
  a side file because commit dates skew. Two generations of that side file now
  coexist, and readers must detect a mix. (I could not open the mailing-list
  thread where maintainers are said to wish the number had been in the commit;
  lore.kernel.org blocks fetches. I do not claim the quote.)

---
**sync L1872-1875 · BODY · NEW-REASON E3**

- (11 when) Two clocks on a record; neither orders; when a usable clock is
  needed, make it monotone along the causal links. REPORTED. For Sid: the gate
  can stamp "when" as the later of its wall clock and one tick past the latest
  "when" among the facts read. INFERRED.

---
**sync L1906-1909 · BODY · NEW-REASON E3,C8**
*3. Section two: versioning › 3.2 Jujutsu, and Mercurial's changeset evolution (Martin von*

- Every operation is a record with its actor, place and time: the operation
  object holds "pointers to the operation(s) immediately before it, as well as
  metadata about the operation, such as timestamps, username, hostname,
  description." (architecture)

---
**sync L2016-2019 · BODY · DISAGREES E3**
*3. Section two: versioning › 3.3 Fossil (D. Richard Hipp; 2006–now)*

- The one place a writer's clock decides: "When two or more tags with the same
  name are applied to the same artifact, the tag with the latest (most recent)
  date is used." (fileformat)


---
**sync L2188-2191 · BODY · NEW-REASON E5**
*3. Section two: versioning › 3.6 Pijul (Pierre-Étienne Meunier, Florent Becker; 2015–now)*

- Dependencies are the minimum needed to make sense of the change, and tools
  may add more: "this is just the minimal set of dependencies needed to make
  sense of the text edits. Hooks and scripts may add extra language-dependent
  dependencies based on semantics." (same)

---
**sync L2205-2209 · BODY · NEW-CASE E5**

- *Two kinds of link.* "Changes now have two different sets of dependencies:
  one is the set of strict dependencies, which are change we require in order
  to apply the current change, while the other one is merely a set of 'known'
  changes, which the apply algorithm checks to decide whether to mark a
  conflict or not."

---
**sync L2549-2560 · BODY · DISAGREES E3**
*4. Question by question › (11) When: whose clock? Ever used for order?*

**What the camp says.** With one orderer, order needs no clock: Figma, Convex,
Weidner, Linear. REPORTED. Keep two times when two exist: Git (author,
committer), Convex (a commit clock, and a display time that "has different
guarantees compared to the commit timestamp"), AT Protocol (client and
indexer), SSB (claimed and received).
REPORTED. A usable wall clock is repaired against causality: Git's corrected
commit date; Convex's hybrid logical clock. REPORTED. Author clocks that decide
winners can be gamed: Nostr. REPORTED. "There is not necessarily an underlying
objective truth as to which operation occurs 'first'": Keyhive. REPORTED.
Fossil lets a later record correct a date. REPORTED. Newbold files timestamps
under "Unfortunate", permanently. REPORTED.


---
**sync L2635-2637 · BODY · NEW-REASON E6**
*4. Question by question › (11) Based on: is every read listed, including reads that on*

- Reads are captured by the runtime, never declared. Convex: "we record the
  index range we scanned in the transaction's read set." Eg-walker: "the
  previous frontier … becomes the new event's parents." REPORTED.

---
**sync L2638-2639 · BODY · NEW-REASON E6**

- Record the *range*, not the rows, so that a later arrival inside the range
  counts as a change: Convex. REPORTED.

---
**sync L2644-2645 · BODY · NEW-REASON E6**

- List the frontier, not everything reachable from it: Kleppmann. Assume the
  obvious single parent and list only exceptions: Gentle. REPORTED.

---
**sync L2646-2649 · BODY · DISAGREES E2**

- A record of what a client was sent can be throwaway if it can be rebuilt
  (Replicache's CVR), or durable if it is the person's own history
  (Patchwork's Account History). REPORTED.


---
**sync L2665-2668 · BODY · NEW-REASON E6,C2**

The tradeoff the camp names: a complete literal read list is the honest record
and does not scale (Bayou). A predicate plus a position scales and is honest
only if the index position is truthful.


---
**sync L2673-2674 · BODY · NEW-CASE E5**
*4. Question by question › (4) Based on: does each read say whether the fact depends on*

- Pijul split one set into "strict dependencies" and "merely a set of 'known'
  changes", after shipping the undivided set. REPORTED.

---
**sync L2675-2677 · BODY · NEW-REASON E5,C6**

- Automerge keeps what I had seen (`deps`) apart from what I overwrite
  (`preds`), and Kleppmann holds that the second "are not redundant".
  REPORTED.

---
**sync L2678-2679 · BODY · NEW-REASON E5**

- For a deterministic function every recorded read is a real dependency by
  construction: Convex. INFERRED from their design.

---
**sync L2683-2684 · BODY · NEW-REASON E5**

- Patchwork saves the pinned heads and still reopens the latest, "because that
  is the expected user experience". REPORTED.

---
**sync L2827-2830 · BODY · NEW-REASON E1,E4**
*4. Question by question › (7) Beside each fact: is the gate's yes or no kept? Where? A*

- What authorised a record can be written on the record: Matrix's
  `auth_events`. INSTITUTIONAL. A verdict should be decidable "only on the
  updates in before(u)", that is, from the record's own recorded past:
  Kleppmann. REPORTED.

---
**sync L2833-2835 · BODY · DISAGREES E1**

- Nobody else keeps refusals. Nostr's is a wire message. Linear drops the
  transaction. Replicache logs an error and moves the counter on. Bayou leaves
  it to an application's "error log" by convention. REPORTED.

---
**sync L2836-2837 · BODY · NEW-REASON E1**

- A refusal should carry what the refused party needs to repair itself:
  Keyhive, for clocks. REPORTED.

---
**sync L2838-2839 · BODY · NEW-CASE E4**

- "an intent record (with a hash of the authorized content)" that blocks a
  publish on mismatch: Automerge, 2026. REPORTED.

---
**sync L2891-2903 · BODY · DISAGREES E7**
*4. Question by question › (14) The hand: which motions become facts by default? Is bei*

**What the camp says.** tldraw: pointer and selection are presence (shared
live, never saved); camera and current page are session state (kept locally,
not shared); only shapes are the document. REPORTED. PushPin: fast-changing
data "could swamp the CRDT with low-value information; there is no reason to
persist such updates", and its gap: "ephemeral data is not associated with a
particular CRDT state". REPORTED. Riffle stored all UI state, was "delighted",
and found that "any change to the layout of that ephemeral state forced a
migration". REPORTED. Croquet: view-to-view traffic is never serialised; smooth
motion is made up in the view. REPORTED. Nostr has kinds that relays do not
store. REPORTED. Patchwork logs what was *opened*, at which version, with which
tool. It does not log motion. REPORTED. Bayou marks the unconfirmed on screen
and carries the mark through queries. REPORTED.


---
**sync L2987-2992 · BODY · NEW-REASON E6,A1,C1**
*5. The group › 5.1 The voices that matter most, who I dropped, who is missi*

2. **Sujay Jayakar (Convex).** The nearest living system to "based-on makes
   running answers recompute", under a sole writer. It shows that recorded
   reads can be ranges, captured by the runtime, and that one mechanism can
   serve both commit conflicts and live invalidation. It shows how far
   determinism has to reach. And it has a fresh reversal on what an id should
   know about its own kind.

---
**sync L3036-3039 · BODY · NEW-REASON E5,C8**

- **The provenance literature (W3C PROV).** It already distinguishes kinds of
  link between a thing and what it came from ("used", "was derived from", "was
  informed by") and has a relation for acting on behalf of another agent. That
  is (4) and (8) with a vocabulary. Nobody in my camp cites it.

