# C2 as-of (10)(5): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L63-63 · BODY · NEW-REASON C2**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.1 What they built, and what they chose*

- **Readers need no coordination.** Peers read immutable index segments from storage and merge in recent novelty from memory. A database value never changes under a reader.

---
**datalog L129-134 · BODY · NEW-REASON C2**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

**Why a basis is one number that you can hand to someone.**

> "You actually can have permalinks for databases. I want to go back to this. I want to remember this database. I want to tell somebody: I think this database was messed up. And I can send them a link, and three weeks later they can go look at that link." — Hickey [H-DD]

> "I would characterize the level of coupling involved in sharing 'the basis is 12345' as categorically different from having to nest database access within the same transaction." — Hickey [H-HN12]


---
**datalog L261-261 · BODY · NEW-REASON C2**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED. In this camp a unit of work reads one database value, and that value has one basis. "You should use a single database v value for a unit of work in order to maintain consistency." [D-BEST]. "Every query we issue to that value of the database has the same basis." [H-DD]

---
**datalog L275-275 · BODY · CARRIED C2**

- REPORTED. Datomic removes the problem for its readers. A database value is the stored index merged with recent novelty in memory: "it is a live merge join between the live index and storage." [H-DD]. So a reader is never behind its own basis: "with no gaps". When a reader needs to be at least as far as some point, it says so and waits: "Peers can synchronize on a time basis via Connection.sync." [D-ACID]

---
**datalog L276-276 · BODY · CARRIED C2**

- INFERRED. In Rama the indexes do trail the depot. The camp's rule transfers directly: a read is *as of what the reader actually saw*. So yes, write down how far the index had really got, because that position *is* the as-of. Writing the log's head instead would be the map lying. If the reader needs the head, it waits for the index to reach it, as `sync` does.

---
**datalog L277-278 · BODY · CARRIED C2**

- REPORTED, XTDB v2, on the same point: `COMMIT SYNC waits for indexing; COMMIT ASYNC returns as soon as the transaction is submitted to the log.` [X-TXS]. They made the gap a named choice.


---
**datalog L481-482 · BODY · CARRIED C2**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.3 What they changed, and what bit them*

10. **A generation number for the log.** "Epochs allow a cluster to safely reset its log state following partial log loss, corruption, or intentional recovery operations, without requiring full reindexing of storage data… Epochs only move forwards." [X-LOG]


---
**datalog L491-491 · BODY · CARRIED C2**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.4 Which questions they speak to*

- **(5) index behind log.** REPORTED. The gap is a named choice per commit: "COMMIT SYNC waits for indexing; COMMIT ASYNC returns as soon as the transaction is submitted to the log." [X-TXS]

---
**frontiers L33-33 · BODY · NEW-REASON C2**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.1 What was built, and what was chosen*

- **Naiad** (Microsoft Research, SOSP 2013, with Murray, Isaacs, Isard, Barham, Abadi). A dataflow system where every message carries a logical timestamp and the system tells operators when a timestamp is complete. REPORTED: "Edges carry records with logical timestamps that enable global progress to be measured." The timestamp had structure: an input epoch plus one loop counter per nested loop. Completion was computed from a "could-result-in" relation: "When an active pointstamp p's precursor count is zero, there is no other pointstamp in the active set that could-result-in p, and we say that p is in the frontier of active pointstamps. The scheduler may deliver any notification in the frontier." ([Naiad, 2013](https://sigops.org/s/conferences/sosp/2013/papers/p439-murray.pdf))

---
**frontiers L43-44 · BODY · NEW-REASON C2**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.2 The reasons, in McSherry's words*

**Why it pays.** REPORTED, same post: "Once input data are recorded as explicit histories, the potential confusion of concurrency is largely removed. Problems of behavioral coordination are reduced to 'just computation': components must produce the correct timestamped output from their timestamped input". And: "Multiple versions are a first class citizen in Materialize's data model, rather than an internal mechanism for optimizing performance."


---
**frontiers L45-46 · BODY · NEW-REASON C2,W3**

**Many ordered inputs, no order between them.** This is Sid's partition problem exactly. McSherry's answer is to invent one order and then write it down. REPORTED: "Materialize interleaves the independent serializations of your upstream databases into one, not unlike how you might shuffle together two decks of cards: the order within each deck stays the same, but the interleaving of the decks is up to us." And: "Materialize cannot make independent sources become mutually consistent (a very hard, perhaps ill-specified distributed systems problem), but it can place all of them on a *common timeline*." And the honest part: "It is admittedly guessing a bit, about how updates to unrelated sources interleave, but having done so there is now one view of all sources, shared by all users. Materialize resolves and locks down one source of ambiguity, so that all downstream uses can be consistent with each other, and with each source individually." (2024 consistency post)


---
**frontiers L47-48 · BODY · NEW-REASON C2,W3**

**How the binding is recorded: reclocking.** McSherry's design doc (authorship checked in the repo's commit history: McSherry, 22 July 2021; addendum 21 April 2022). REPORTED: "'Reclocking' a stream of data is (for the purposes of this document) translating its events from one gauge of progress to another. One example is taking Kafka topics (whose records are stamped with progressing `(partition, offset)` pairs) into Materialize's system timeline (whose records are stamped with milliseconds since the unix epoch)." The stated goals include: "There is an explicit representation of the translation (rather than a behavioral description)", "The translation should be sidecar metadata, rather than a rewriting of the source data", and "The combination of durable source data and durable metadata should result in a durable reclocked stream." The mechanism: "We are going to use a `remap` collection, which is a map from a target gauge `IntoTime` to values from the source gauge `FromTime` that must themselves form an antichain." And: "Whenever `IntoTime` advances, we record the current values of `FromTime` as the new contents of `remap`." The payoff: "This is an example of how `remap`, durably recorded, introduces the basis for consistent recovery." ([Reclocking design doc](https://github.com/MaterializeInc/materialize/blob/main/doc/developer/design/20210714_reclocking.md))


---
**frontiers L49-50 · BODY · CARRIED C2**

The same doc names a trap that bites Sid directly, because Rama partitions can be added. REPORTED: "The use of antichains is potentially confusing for sources. For example, Kakfa's (partition, offset) pairs would be frustrating with dynamically arriving parts if the part identifiers are not both ordered and starting from small identifiers. Otherwise, recording 'these parts have not yet started' is non-trivial."


---
**frontiers L51-52 · BODY · CARRIED C2**

**What a consistent read is, and how a reader knows.** From the Materialize formalism doc (file introduced by McSherry 28 Feb 2022, expanded by Kyle Kingsbury in March 2022; checked in commit history). REPORTED: "At any wall-clock time a TVC is (in general) unknowable. Some of its past changes have been forgotten or were never recorded, some information has yet to arrive, and its future is unwritten." So every collection carries two frontiers: "We think of `since` as the 'read frontier': times not later than or equal to `since` cannot be correctly read. We think of `upper` as the 'write frontier': times later than or equal to `upper` may still be written to the TVC." A read at time t is correct only when t is between them. ([Formalism](https://github.com/MaterializeInc/materialize/blob/main/doc/developer/platform/formalism.md))


---
**frontiers L53-54 · BODY · NEW-REASON C2**

**Derived things lag, by rule.** REPORTED: "Each arrowhead necessarily lags the arrowheads of its immediate inputs." The asker picks where to stand, trading three things (2024 consistency post): "Responsiveness: Always choose a timestamp to the left of (before) the arrowhead of the query output." "Freshness: Always choose a timestamp to the right of (after) all input arrowheads." "Consistency: Always choose a timestamp to the right of (after) all previously chosen timestamps." While waiting, the honest display is per-input status relative to the chosen time: `ready`, `refreshing`, `pending`. McSherry: "This looks (to me) closest to what a person who wants the answer to their query wants to know".


---
**frontiers L55-56 · BODY · NEW-REASON C2**

**Freshness on demand.** REPORTED: "When you issue a command at C, Materialize can transact against the upstream primary to learn the current state of the replication log V, and then ensure that its response at R reflects at least everything through V." "It's a surprisingly simple strategy to remove replication lag: just .. wait out the lag." ([Zero-Staleness, 2024](https://github.com/frankmcsherry/blog/blob/master/posts/2024-08-13.md))


---
**frontiers L57-58 · BODY · NEW-REASON C2**

**Why weaker is not acceptable.** REPORTED: "natural eventually consistent computations can produce *unboundedly large and systematic errors*" and "you should be prepared for your results to be *never-consistent*" and "And that is one of the pain points for eventual consistency in streaming: who even knows?" ([Eventual Consistency isn't for Streaming, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-06-19.md)). Later: "Inconsistent or transiently incorrect results are unacceptable for operational work; at best you have to stall your operational plane to sort things out, and at worst you may take irrevocable incorrect actions." ([Consistency and Operational Confidence, 2023](https://github.com/frankmcsherry/blog/blob/master/posts/2023-09-19.md))


---
**frontiers L59-60 · BODY · CARRIED C2**

**Completeness as statements in the log.** McSherry's change-data-capture format has two kinds of record, both append-only and both safe to duplicate and reorder. REPORTED: "We will make two types of statements, each of which will be statements that are both true about the final history and can be made before that history is complete. We will make these statements only once we are certain they are and will remain true." Updates say what changed at a time. "*Progress statements* have the form `progress (time, count)` and report the number of distinct non-zero updates that occur at `time`." Result: "The statements above may be arbitrarily duplicated and reordered, and we can still recover as much of the history as is fully covered by the update and progress statements." ([Change Data Capture part 1, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-08-01.md))


---
**frontiers L75-75 · BODY · NEW-REASON C2**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.3 What changed over the years (worth more than the papers)*

- **Naiad's system-run notifications became capabilities.** The unpublished Timestamp Tokens paper (Lattuada and McSherry, [arXiv 2022](https://arxiv.org/pdf/2210.06113)) says earlier interfaces "require a deeper involvement of the system itself: continually invoking operators in Flink and sequencing notifications in Naiad", and proposes tokens instead: holding a token for a time is the right to still write at that time. REPORTED from McSherry: "The Timestamp Tokens paper never got published." and "Of all the papers, this is the one closest to explaining what timely dataflow contributes." (Decade in review)

---
**frontiers L77-77 · BODY · NEW-CASE C2,A1**

- **Materialize grew a durable layer it did not start with.** The 2021 reclocking doc exists because sources were not replayable: its goal B is "Describing durable state that can make sources like Kafka, Postgres, and files exactly replayable in the system timeline." The 2022 "unbundled" architecture split Storage, Compute, Adapter, joined only by times.

---
**frontiers L88-88 · BODY · CARRIED C2**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(5) index lag written down?** REPORTED via `since` and `upper`: an index is a collection with its own `upper`. A read at a time at or past `upper` is not a correct read. So the rule is: choose the read time inside the index's frontier, or wait. Then the recorded "as of" *is* how far the index had got. INFERRED: a separate "lag" field is only needed if readers are allowed to read past the frontier, and the camp would forbid that outright.

---
**frontiers L107-108 · BODY · NEW-REASON C2,W3**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.6 Disagreements*

With Noria and with Flink and Kafka Streams, on eventual consistency for computed views (REPORTED above; McSherry grants Noria's case: "systems like Noria that target keyed look-ups for maintained views, for which you might reasonably expect updates to cease for the records that influence your query results"). With the DBSP authors, mildly, on whether times must be partially ordered (section 4). With the "big data" systems crowd on scale: "many systems have either a surprisingly large COST, often hundreds of cores, or simply underperform one thread for all of their reported configurations." ([Scalability! But at what COST?, HotOS 2015](https://www.usenix.org/system/files/conference/hotos15/hotos15-paper-mcsherry.pdf)). INFERRED: McSherry would ask for the single-thread number before accepting "thousands of landings a second" as a scale problem.


---
**frontiers L115-116 · BODY · NEW-CASE C2**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.1 What was built, and what was chosen*

Brandon worked on Eve, then at Materialize, then wrote dida, a small re-implementation of differential dataflow meant to be understood. The piece that matters here is a test: the same bank-transfer workload run through Flink, Kafka Streams/ksqlDB, and differential dataflow/Materialize, checking whether the running total (which must always be zero) ever shows a non-zero value. Flink and ksqlDB showed impossible totals. Differential dataflow did not. ([Internal consistency in streaming systems, 17 April 2021](https://www.scattered-thoughts.net/writing/internal-consistency-in-streaming-systems/); Brandon's disclaimer: "I used to work for Materialize which is one of the systems being compared in this post.")


---
**frontiers L119-120 · BODY · NEW-REASON C2**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.2 The reasons, in Brandon's words*

**The definition.** REPORTED: "A system is internally consistent if every output is the correct output for some subset of the inputs provided so far." Against: "A system is eventually consistent if when we stop providing new inputs it eventually produces the correct output for the set of inputs provided so far."


---
**frontiers L121-122 · BODY · NEW-REASON C2**

**Why database intuition fails here.** REPORTED: "An eventually consistent key-value database might show you old values. An eventually consistent streaming system might show you outputs that are completely impossible for any set of inputs, and might never converge to a correct value as long as new inputs keep arriving."


---
**frontiers L123-124 · BODY · NEW-CASE C2**

**The four ways to lie** (REPORTED, section headings and text):
1. "Combining streams without synchronization": "if the join between credits and debits is not synchronized then we could be calculating the balance using the current value of credits and the past value of debits, effectively creating money."

---
**frontiers L125-125 · BODY · NEW-CASE C2,X1**

2. "Early emission from non-monotonic operators": an aggregate that emits after each of four related changes is wrong three times out of four. "To be internally consistent, total needs to wait until it's seen all the updates that correspond to the original transaction before emitting an output."

---
**frontiers L127-128 · BODY · NEW-REASON C2,E1**

4. "Inconsistent rejections in watermarks": "To be internally consistent, a streaming system can only reject inputs at the edge, so that all operators are working on data derived from the same set of upstream inputs."


---
**frontiers L129-130 · BODY · NEW-CASE C2,E2**

**Why it matters to someone acting on a screen.** REPORTED: "at the edge of the system it's not possible to take action on outputs if you don't know which ones are correct. You might email a warning to a customer for going overdrawn on their account, only to later find that this was just a transient inconsistency." And the fix: "To be internally consistent, there needs to be some way to determine when an output value is correct. This could be in the form of eg progress statements which tell you that the output for a given timestamp will no longer change."


---
**frontiers L131-132 · BODY · NEW-REASON C2**

**What it costs the person who must reason about it.** REPORTED: "Internally inconsistent systems require the user to reason about all the possible interleavings of stream events. Internally consistent systems allow the user to pretend they are just operating a very fast batch system." ([An opinionated map of incremental and streaming systems, 2021](https://www.scattered-thoughts.net/writing/an-opinionated-map-of-incremental-and-streaming-systems/)). And: "The effect is similar to undefined behavior in programming languages."


---
**frontiers L133-134 · BODY · NEW-REASON C2**

**What it costs the machine.** REPORTED: "I don't expect that guaranteeing internal consistency will have much impact on throughput or horizontal scaling. The amount of metadata that must be tracked is very small and can be amortized over large batches of updates. There is, however, a strong tradeoff between correctness and latency."


---
**frontiers L135-136 · BODY · CARRIED C2**

**The model that makes it possible.** REPORTED: "Doing this correctly typically requires having an explicit model of time which is tracked alongside incoming data to allow the system to reason about which versions of various intermediate results go together and when it is safe to produce an output." In dida's design notes: "To guarantee that the results are consistent we add timestamps, multiversion indexes and frontiers." On what a timestamp is: "These timestamps could be actual real world timestamps (eg unix epochs) or they could just be arbitrary integers that we increment every time we make a new change. Their job is just to keep track of which output changes were caused by which input changes." On readers: "Frontiers are also useful at the output of the system - downstream consumers can watch the frontier to learn when the have seen all the changes to the output for a given timestamp and can now safely act on the result." ([dida, docs/why.md](https://github.com/jamii/dida/blob/main/docs/why.md))


---
**frontiers L148-148 · BODY · NEW-REASON C2**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.3 What changed or was regretted*

- **What Brandon would design from scratch.** REPORTED: "If I was designing a system from scratch I'd be tempted to allow multiple waves of watermarks from the beginning", so that early outputs and late inputs are possible "without risking internal inconsistency".

---
**frontiers L153-153 · BODY · NEW-REASON C2**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.4 Which questions Brandon speaks to*

- **"The map must not lie" is internal consistency.** INFERRED but close to a restatement: Sid's principle is Brandon's definition with a display attached. Brandon adds the sharper half: stale is the *mild* failure. The bad one is an output that was never true for any input.

---
**frontiers L154-154 · BODY · NEW-CASE C2,E2**

- **Sid's system, as described, is open to all four failures.** INFERRED. Nobody routes; tools fire on landings; tools read through indexes that lag; one landing can start several chains of work. A running answer built from two reads at different index positions is failure 1. A tool that fires on the first of several related landings is failure 2. A crossing record that cannot say "this replaced a partial answer" versus "the world changed" is failure 3. Failure 4 is already avoided: the gate is the only place that rejects.

---
**frontiers L155-155 · BODY · CARRIED C2**

- **(5) index lag.** REPORTED principle: a reader needs "some way to determine when an output value is correct". INFERRED: yes, write down how far the index had got, and better, do not answer past it.

---
**frontiers L168-169 · BODY · NEW-REASON C2,W3**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.6 Disagreements*

With the Flink and Kafka Streams designers, on whether eventual consistency tells you anything: "eventual consistency alone does not provide any useful constraints." With the coordination-avoidance line (section 3), in spirit: "google seems to be moving everything to being backed by spanner, because reasoning about weaker consistency models at scale is just too difficult and too expensive." With McSherry, only on complexity: simpler time if possible.


---
**frontiers L184-185 · BODY · NEW-CASE C2,E5**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.2 The reasons, in their words*

**A manifest makes a non-monotone step safe.** This is the closest thing in the literature to Sid's "based on". REPORTED, about the shopping cart: "the checkout operation is enhanced with a manifest from the client of all its update message IDs that preceded the checkout message: replicas can delay processing of the checkout message until they have processed all updates in the manifest." The lesson drawn: "Rather than micro-optimize protocols to protect race conditions in procedural code, modern distributed systems creativity often involves minimizing the use of such protocols."


---
**frontiers L186-187 · BODY · NEW-CASE C2**

**Reads are where the danger is.** REPORTED: "Yet CRDT guarantees extend only to data updates; observations of CRDT state are unconstrained and unsafe." The example is named "The Potato and the Ferrari, a.k.a. Early Read": a cart is two growing sets, added and removed; the contents are A − R; checkout reads before the removal of the Ferrari arrives. "This truly expensive consistency bug arises when the query 'reads' the state of the 2P-Set 'too early'". Monotone threshold queries are safe to read locally. Set difference is not. "So what are developers to do when they need one of these nonmonotone queries? The simple and safe solution is to coordinate!" ([Keep CALM and CRDT On, Laddad, Power, Milano, Cheung, Crooks, Hellerstein, VLDB 2022](https://www.vldb.org/pvldb/vol16/p856-power.pdf))


---
**frontiers L200-200 · BODY · NEW-REASON C2**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.3 What changed or was regretted*

- **CRDTs were found wanting on the read side** by the same people who had praised the pattern (2020 paper praises it; 2022 paper: "unconstrained and unsafe").

---
**frontiers L201-202 · BODY · DISAGREES C2**

- **Consistency became a per-handler choice.** In Hydro the consistency "facet" is declared per endpoint, with "eventual" as the default.


---
**frontiers L211-211 · BODY · NEW-CASE C2**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.4 Which questions they speak to*

- **(5) index lag.** REPORTED: the early read. INFERRED: a pattern read over a lagging index is the Potato and the Ferrari. Record the seal it read under, or wait for one.

---
**frontiers L251-251 · BODY · NEW-REASON C2,W3**
*4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff;  › 4.4 Which questions they speak to*

- **(10) one number.** REPORTED: one counter. And the same pattern as reclocking: the scalar step is bound, in a durable log, to per-partition offsets. Two teams arrived at it separately. That is the strongest single signal in this report.

---
**frontiers L271-272 · BODY · DISAGREES C2**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.1 What was built, and what was chosen*

Noria (MIT, OSDI 2018): materialized views as a dataflow that also serves reads, for web applications. Two choices. **Eventual consistency, on purpose.** And **partial state**: a view may have holes; a miss sends an "upquery" backward through the dataflow to fill the hole; cold entries are evicted.


---
**frontiers L275-276 · BODY · DISAGREES C2**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.2 Reasons, in their words*

REPORTED, Gjengset's thesis: "To achieve high parallel processing performance, Noria's dataflow avoids global progress tracking or coordination. An update injected at a base table takes time to propagate through the dataflow, and the update may appear in different views at different times." Candidly: "Eventual consistency is an inherently vague consistency model — an eventually consistent system may return incorrect results as long as it eventually returns the right result." The defence: "Noria reads are generally just stale." The design reason: "By design, Noria's read and write paths are disconnected from one another: reads can usually proceed even if the write path is busy. This is both the reason why Noria's read performance is so high, and why it gives weaker consistency guarantees that competing systems." And the escape hatch: "application developers direct those queries where strong consistency is necessary to other, better suited systems." ([Partial State in Dataflow-Based Materialized Views, PhD thesis, MIT 2020](https://jon.thesquareplanet.com/papers/phd-thesis.pdf))


---
**frontiers L281-281 · BODY · NEW-REASON C2,W1**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.3 What changed afterwards*

- REPORTED, thesis section 8.3.3: "adding partial state to a system with stronger consistency guarantees should not require extensive changes. In fact, parts of the design could likely be simplified". So weak consistency was not essential to partial state. It was a simplification for read speed.

---
**frontiers L282-282 · BODY · NEW-CASE C2**

- Schwarzkopf's group at Brown later worked on read-your-writes for their dataflow (Sc.M. thesis, Ishan Sharma, May 2022; I read only its front matter and mechanism keywords: per-client tickets of recent writes, reads that block until the views catch up). INFERRED: they came back for the guarantee Noria left out.

---
**frontiers L283-283 · BODY · DISAGREES C2**

- The commercial successor ReadySet still describes itself as eventually consistent (seen in a search result snippet from its docs; I did not open the page).

---
**frontiers L297-298 · BODY · DISAGREES C2**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.5 Resemblance and difference*

Resembles: many users, per-user views over a shared base, one system serving reads at interactive speed, erasure as a design input. Differs: Noria chose to let the map be briefly wrong, which Sid rules out. Noria's base tables are mutable SQL tables.


---
**frontiers L301-302 · BODY · DISAGREES C2**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.6 Disagreements*

With McSherry and Brandon, head on, about eventual consistency for computed views. Gjengset's position is that for keyed web reads it is fine and buys read throughput. McSherry concedes that case and no further.


---
**frontiers L373-374 · BODY · CARRIED C2**
*7. Question by question: what this camp says*

**(5) Index lag.** Yes, and more than yes. A read is *defined* by the frontier it ran under (formalism, Brandon, CRDT paper, all REPORTED). A pattern read that does not know the index's frontier cannot say what it read.


---
**frontiers L375-376 · BODY · NEW-REASON C2,E6**

**(11) Because-of.** INFERRED: base facts start chains and derived facts always have a cause. One thing the camp adds. Naiad's epoch is a because-of tag, and the reason to carry it is to know when all work caused by an input has *finished* (REPORTED: notification "after all messages with a specified timestamp have been delivered"). Without that, a screen can show an answer while half the chain from a landing is still in flight. That is Brandon's first failure.


---
**frontiers L397-397 · BODY · NEW-REASON C2,W3**
*8. Voices that matter most, who was dropped, who is missing*

1. **Frank McSherry.** For question 10 and everything tied to it. McSherry is the only one who faced "many ordered partitions, one store-wide as-of" and wrote down a mechanism with its failure modes: invent the interleaving, record it durably as a side map, read only between frontiers. The 2023 transaction design shows what it costs when independent frontiers must move together.

---
**frontiers L398-398 · BODY · NEW-REASON C2,E6**

2. **Jamie Brandon.** For the principle. "The map must not lie" is internal consistency, and Brandon tested real systems against it, named the ways it breaks, and explained what it means for a person acting on a screen. Brandon also supplies the grain warning that bears on per-fact provenance.

---
**frontiers L403-404 · BODY · DISAGREES C2**

**Dropped.** Noria's core as a model for Sid: it chose to be briefly wrong, which Sid's principles forbid. It stays as the honest opposing voice and as the best account of partial state. Bloom the language: not adopted, by its authors' own account.


---
**frontiers L408-408 · BODY · DISAGREES C2**

- **Tyler Akidau and the Google Dataflow/Beam line.** The main opposing view on completeness: watermarks as estimates, late data, triggers, corrections. Brandon engages it. McSherry rejects it.

---
**frontiers L442-442 · BODY · NEW-REASON C2,W3**
*11. What each source implies for a second store*

- **McSherry, REPORTED** (reclocking goal A): "Permitting the use of streams from one timeline in another timeline." A second store is another timeline. Bring its facts in through a recorded remap from its times to ours. Each store stays internally consistent. The interleaving between the two is invented by the receiver and then fixed.

---
**frontiers L443-443 · BODY · NEW-REASON C2**

- **Brandon, REPORTED:** "the changes, timestamps and frontiers at the output are exactly the information that is required at the input, so we can take multiple such systems with different internal implementations and they can be composed into a single consistent computation so long as they stick to this format." So the interface between two stores is three things: changes, times, frontiers. A store that does not publish frontiers cannot be composed consistently with another.

---
**frontiers L444-444 · BODY · NEW-REASON C2**

- **McSherry, REPORTED:** the exchange format can be made safe under duplication and reordering if completeness statements travel with the data (the progress-count format).

---
**log L91-91 · BODY · CARRIED C2**
*Voice by voice › 1. Pat Helland*

- (5) REPORTED, and a gap: "When you read data from alternate indices, you must understand that it is potentially out of sync with the entity itself." He tells the reader to tolerate lag. He never proposes recording how far the index had got.

---
**log L114-114 · BODY · NEW-REASON C2,E5**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- What "as of" becomes in a partial order: "The return value of log-snapshot acts as a vector timestamp for the color". And every record carries what its writer had seen: "It includes the vector timestamp of nodes seen thus far in the new entry; as a result, each appended entry includes pointers to the set of nodes it causally depends on". (FuzzyLog.)

---
**log L118-118 · BODY · CARRIED C2**

- How far a reader had got travels in the record: the ViewTrackingEngine "adds a header on each outgoing propose with its local playback position", and "the view is a deterministic function of the log; and as a result, so is the decision to trim a prefix of the log." (Delos, SOSP 2021.)

---
**log L136-136 · BODY · NEW-REASON C2,E5**

- (5) REPORTED: Delos and FuzzyLog both put how far the writer had seen into every record, as one compact value. INFERRED: an offer can carry one frontier that bounds everything its actor could have known, alongside any itemized reads.

---
**log L137-137 · BODY · CARRIED C2,C3**

- (10) REPORTED: across shards or regions, "as of" is a vector, and vectors "can be compared to check if one subsumes the other". A single global order couples failure domains (blast radius).

---
**log L147-148 · BODY · CARRIED C2,C3**

**6. Disagreements.** With Raft-style designs that fuse the log and the application ("aggressively combine both planes into a single protocol"). With Hyder, whose hole-filling "resorts to a heavy recovery protocol that involves sealing the system". Against Balakrishnan: Scalog (Cornell, NSDI 2020) says CORFU's order-before-persistence is the root of its holes: "records are first replicated, and only then assigned a position in the total order", and the dilemma "arises whenever a storage system makes decisions about an item's metadata before making persistent the item itself." Scalog's shards report "an integer vector summarizing the records stored", and "a deterministic function that specifies how to order the records in between two consecutive cuts" turns vectors into one order. Note its footnote: "These guarantees hold only in the absence of trimming."


---
**log L217-217 · BODY · CARRIED C2**
*Voice by voice › 4. Martin Kleppmann*

- (5) REPORTED: a subscriber "periodically checkpoints the latest LSN it has processed", and the approach "does not provide isolation for read requests that are sent directly to data stores". He names the lagging-index read as an open problem.

---
**log L275-275 · BODY · NEW-REASON C2**
*Voice by voice › 6. Lamport, as foundation*

- "The relation "happened before" is therefore only a partial ordering of the events in the system."

---
**log L276-276 · BODY · NEW-REASON C2,W3**

- Any total order made from it is a choice: "It is only the partial ordering which is uniquely determined by the system of events."

---
**log L277-278 · BODY · NEW-CASE C2,E5**

- The anomaly and its first cure. Someone issues request A, then phones a friend who issues B elsewhere; B may be ordered first, because "that precedence information is based on messages external to the system." The first remedy: "the person issuing request A could receive the timestamp TA of that request from the system. When issuing request B, his friend could specify that B be given a timestamp later than TA. This gives the user the responsibility for avoiding anomalous behavior." The second remedy is synchronized physical clocks.


---
**log L288-288 · BODY · NEW-REASON C2,E6**
*Voice by voice › 7. Phil Bernstein's Hyder*

- What a read stood on is one position: the intention "contains a reference R to T's snapshot, which is the last committed transaction in the log that contributed to the database state that T read". (CIDR 2011.)

---
**log L320-320 · BODY · CARRIED C2**
*Voice by voice › 8. Amazon Aurora, and what AWS did next*

- Lagging readers: a replica "typically lags behind the writer by a short interval (20 ms or less)", and "replica read views must lag durability consistency points at the writer instance."

---
**log L330-330 · BODY · CARRIED C2**

- (5) REPORTED: two honest designs. Aurora replicas read as of wherever they have got to. DSQL picks the as-of first and makes a lagging reader wait. Neither ever reports an as-of that the index had not reached.

---
**log L342-343 · BODY · CARRIED C2,C5,P0**
*Voice by voice › 9. Two short notes on voices the brief did not list*

**Jay Kreps (Kafka).** The strongest plain statement that position is the clock. "The log entry number can be thought of as the "timestamp" of the entry", which has "the convenient property that it is decoupled from any particular physical clock." A replica's whole state is one number per log: "you can describe each replica by a single number, the timestamp for the maximum log entry it has processed." He refused a global order: "Each partition is a totally ordered log, but there is no global ordering between partitions", and defended the refusal: "it is not meaningful to talk about a total order over their behavior." He is honest about what compaction costs: "we can no longer recreate all previous states of the source system, only the more recent ones." In 2017 he accepted keeping data forever, with a warning: "the bar for both software correctness and operational practices increases quite dramatically". Against Marz's Lambda architecture: "I don't think this problem is fixable." (*The Log*, 2013; *Questioning the Lambda Architecture*, 2014; *It's Okay To Store Data In Apache Kafka*, 2017.)


---
**log L435-436 · BODY · CARRIED C2,C3**
*Question by question: what this camp would say › (10) Order: two gates on one layer? What must share a partit*

"As of": inside one partition, one number (Kreps). Across partitions, a vector: FuzzyLog's snapshot "acts as a vector timestamp"; Scalog's cut is "an integer vector"; Hyder's partitioned LSNs "are incomparable". One number for the whole store exists only when something orders the vectors. Scalog's ordering layer does that, and DSQL's crossbars build an order per consumer. A single global order has a cost nobody predicted: blast radius, where one lost slot stalls every reader (Balakrishnan, 2024).


---
**log L443-443 · BODY · NEW-REASON C2,E5**
*Question by question: what this camp would say › (11) Based on: is every read listed?*

- *How far the writer had seen.* FuzzyLog puts "the vector timestamp of nodes seen thus far in the new entry". Delos adds "its local playback position" to each proposal. One compact value, not a list.

---
**log L458-459 · BODY · CARRIED C2**
*Question by question: what this camp would say › (5) Based on: if the index was behind the log, is how far it*

REPORTED. The older generation simply tolerates lag. Helland: an index is "potentially out of sync with the entity itself"; "There may be things with identities that have not yet been indexed." Kleppmann calls the direct read of a lagging store an open problem: the approach "does not provide isolation for read requests that are sent directly to data stores". The newer systems put the position into the record or the read. Kreps: a replica is "a single number". Aurora: a read view is anchored to a point the replica has reached. DSQL: a lagging reader "is made to wait". Delos: each proposal carries "its local playback position". A CT proof is always against a stated tree size.


---
**log L460-461 · BODY · CARRIED C2,E6,E2**

**For the first record (INFERRED).** This is less an extra field than the definition of the field. The as-of of a pattern read is the position the serving index had applied. It is never the log's tail and never the time of asking. Two honest designs exist: report where the index was, or name the as-of first and wait for the index to reach it. The dishonest one is to record the as-of you wanted while serving from an index that had not got there. One more consequence, from Aurora's and DSQL's short horizons: a pattern read recorded as "pattern plus as-of" is cheap to check for staleness later ("has anything matching landed since?") and expensive or impossible to re-run later ("what did it return?"). If a crossing must be reproducible, record at least a checksum of what was shown.


---
**log L572-572 · BODY · ABOVE C2**
*The second store: what each source implies*

- **Balakrishnan.** A store can continue elsewhere if the pointer to its successor lives outside it. Across regions, order becomes a graph and "as of" becomes a vector that can be compared.

---
**log L574-574 · BODY · NEW-REASON C2**

- **Bernstein and Das.** Positions from different logs "are incomparable"; a partition id can break ties if one order is needed, and that order is arbitrary.

---
**meaning L278-290 · BODY · NEW-CASE C2,A1**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

> But equating any such labeling with more general interpretation is a mistake.
> For instance, putting facts behind a *dynamic* interpreter (one that could
> answer the same question differently at different times, mix facts with
> opinions/derivations or have effects) certainly exceeds (and breaks) the idea
> of data. Which is precisely why we need the idea of data, so we can
> differentiate and talk about when that is and is not happening - am I dealing
> with facts, an immutable observation of the past ("the king is dead") or just
> a temporary (derived) opinions ("there may be a revolt"). Consider the
> difference between a calculation involving (several times) a fact
> (date-of-birth) vs a live-updated derivation (age). The latter can produce
> results that don't add up. 'date-of-birth' is data and 'age' (unless
> temporally-qualified, 'as-of') is not.
>

---
**meaning L328-329 · BODY · NEW-REASON C2**

- 11940599, on functional languages: "They need a much better idea of time
  (such as approaches to McCarthy's fluents)."

---
**meaning L386-388 · BODY · NEW-REASON C2,A1**
*Part one — the exchange › 1.3 What each actually claimed*

6. A dynamic interpreter in front of facts breaks them. It may answer
   differently at different times, mix fact with derivation, or have effects.
   Derived values are only safe when "temporally-qualified, 'as-of'".

---
**meaning L447-451 · BODY · NEW-REASON C2**
*Part one — the exchange › 1.5 What stayed unresolved*

4. **Time.** Hickey's "as-of" and Kay's pointers to McCarthy's fluents, Reed's
   thesis, and Worlds are the same family of idea: state as a series of
   immutable versions along a time line. Kay tells functional programmers they
   need "a much better idea of time". That is what Hickey's work is about. They
   agree here more than the thread shows, and never noticed.

---
**meaning L1760-1770 · BODY · ABOVE C2,C3**
*Part two — the people › 2.11 Carl Hewitt*

**On order.** REPORTED ("Actor Model of Computation: Scalable Robust Information
Systems", arXiv:1008.1459): "The Actor Model supports indeterminacy because the
reception order of messages can affect future behavior." "In the Actor Model,
there is no hypothesis of simultaneous change in multiple locations." "The
entire computation is not in any well-defined state." On the arbiter that
decides arrival order: "typically we cannot observe the details by which the
order in which an Actor processes messages has been determined. Attempting to do
so affects the results. [...] we await outcomes." (The gatherer could not find
Hewitt saying "there is no global clock" in so many words. Do not quote Hewitt as
saying it.)


---
**meaning L1771-1775 · BODY · NEW-REASON C2,E3**

For (10) and (11), INFERRED: the order in which offers reach a gate is not
something that can be worked out from facts. It is made by the gate and can only
be recorded. The gate's number is that record. A writer's clock is a claim. And
two gates make two orders, with no fact of the matter between them.


---
**meaning L2400-2407 · BODY · NEW-CASE C2,W1**
*Part four — Sid's questions, hung on the parts of the fact › (5) Index lag*

### (5) Index lag

**Camp.** The index is what breaks at scale, and it lags. Wikidata's query service
was the bottleneck, took "~3 months to reload", and was finally split (2.5).
Webstrates enforced stale permissions for up to two minutes with no record (3.3).
Folk lets only converged state cross the boundary (3.1). A Croquet replica always
knows exactly how far it has got (2.8).


---
**meaning L2408-2412 · BODY · NEW-REASON C2**

**My read.** Yes. A pattern read is "everything matching P as of X". X must be how
far *the index* had got, not how far the log had got. Otherwise the fact claims to
stand on things it never saw, and the map lies. It is cheap to record and cannot
be reconstructed.


---
**rama L136-136 · BODY · NEW-REASON C2**
*Part one — What Rama's own reference says › Question by question › (10) Order: can two gates ever write one layer? What must sh*

- **CHECKED** `docs/14-depots.md:234` — "A record in a depot partition is identified by a 'partition index' and an 'offset'." There is no global offset.

---
**rama L137-138 · BODY · CARRIED C2**

- **CHECKED** `docs/25-integrating.md:336` — "The microbatch ID is a 64 bit value that increments by one with each successful microbatch… each microbatch ID is associated with a specific range of data on each depot partition."
- **CHECKED** `skill/microbatch.md:140` — `(ops/current-microbatch-id :> *mb-id)` makes that number readable in topology code.

---
**rama L139-140 · BODY · NEW-REASON C2**

- **CHECKED** `skill/microbatch.md:128` — "external readers can observe two tasks on different microbatches at the same moment… Cross-partition atomicity… is a property of the settled result, not of what a reader sees mid-commit."


---
**rama L145-146 · BODY · CARRIED C2**

"As of": under a microbatch gate, one number works, because every partition advances through the same microbatch IDs. A reader must still confirm that the partitions it touches have all committed that ID. Under a stream gate, "as of" is a position per partition. Since the reference shows no way for topology code to see offsets, the gate would have to keep its own per-partition counter.


---
**rama L157-157 · BODY · CARRIED C2**
*Part one — What Rama's own reference says › Question by question › (5) Based on: if an index was behind the log when a pattern *

- **CHECKED** the depot head is readable by clients: `foreign-depot-partition-info` returns `{:start-offset … :end-offset …}` per partition (`skill/depot-reference.md:243-244`).

---
**rama L158-158 · BODY · CARRIED C2**

- **CHECKED** topology progress is kept in internal PStates, `$$__streaming-state-<topologyId>` and `$$__microbatcher-state-<topologyId>` (`docs/11-stream-topologies.md:142`, `docs/12-microbatch-topologies.md:116`), and is shown in the Cluster UI. **NOT IN THE REFERENCE:** a client API to read it.

---
**rama L159-160 · BODY · CARRIED C2**

- **CHECKED** `skill/testing.md:157-165` — RPL's advice is to build your own: "materialize progress state as part of the design (e.g. counters of work enqueued and work completed)".


---
**rama L161-162 · BODY · CARRIED C2**

**IMPLIED:** how far the index had really got can be written on a pattern read only if the indexing topology writes its own progress mark into a PState. Under microbatch that mark is the microbatch ID. Under stream it is a counter the topology keeps per partition. This is cheap to add on day one and impossible to add for reads already recorded.


---
**rama L231-231 · BODY · CARRIED C2**
*Part one — What Rama's own reference says › The gate: stream or microbatch*

| | Stream gate | Microbatch gate |
| Global "as of" number | none | microbatch ID |

---
**rama L249-250 · BODY · CARRIED C2**
*Part one — What Rama's own reference says › Other items the brief named*

**PState lag and a readable cut.** Covered under (5) and (10). The depot head is readable. Topology progress is not, unless you materialize it. The microbatch ID is the only built-in global cut.


---
**rama L373-374 · BODY · CARRIED C2**
*Part three — The dissent: Jay Kreps › 2. His reasons, in his words*

On position as state. "You can describe each replica by a single number, the timestamp for the maximum log entry it has processed. This timestamp combined with the log uniquely captures the entire state of the replica." (*The Log*)


---
**rama L375-376 · BODY · CARRIED C2**

On reading a lagging index. "The client can get read-your-write semantics from any node by providing the timestamp of a write as part of its query—a serving node receiving such a query will compare the desired timestamp to its own index point and if necessary delay the request until it has indexed up to at least that time to avoid serving stale data." (*The Log*)


---
**rama L409-410 · BODY · CARRIED C2**
*Part three — The dissent: Jay Kreps › 4. Which of Sid's questions he speaks to*

**(5)** REPORTED: a derived index should know its "index point" and compare it with the position a reader asks for. INFERRED: on "is how far it had really got written down?", yes. It is one number per partition, and the index already has it.


---
**skeptics L52-53 · BODY · DISAGREES C2,E6**
*1. The ordinary default, question by question*

**(5) Index lag written down?** No. Replica lag is a metric on a dashboard, not something stored with a read. Read-your-writes is handled by routing a user to the primary, or, in big tech, by a token the reader carries (section 3).


---
**skeptics L147-148 · BODY · NEW-CASE C2**
*3. Big-tech operational lessons › 3.1 Amazon*

REPORTED (Vogels, 2021): S3's metadata cache meant "in extremely rare circumstances we would exhibit eventual consistency on writes." What customers did about it is the lesson: "customers put in place their own application code to track consistency outside of S3 for their S3 usage." "Netflix open sourced s3mper, which used Amazon DynamoDB as a consistent store to identify those rare cases that S3 would serve an inconsistent response. Cloudera and the Apache Hadoop community worked on S3Guard". The fix, after fourteen years: "This new replication logic allows us to reason about the 'order of operations' per-object in S3." "We introduced a new component into the S3 metadata subsystem to understand if the cache's view of an object's metadata was stale. This component acts as a witness to writes, notified every time an object changes. This new component acts like a read barrier during read operations allowing the cache to learn if its view of an object is stale." And the bar: "Strong consistency must always be strong with no exceptions." ([Diving Deep on S3 Consistency, 2021](https://www.allthingsdistributed.com/2021/04/s3-strong-consistency.html))


---
**skeptics L151-152 · BODY · NEW-CASE C2,P0**

What this says to Sid. INFERRED: (5) When the store does not tell a reader whether a derived view is current, every serious user builds a side system to find out. S3's customers did. That is an argument *for* Sid's wish to make staleness a first-class thing, from the company that waited longest to do it. The mechanism they chose is small: order per object, and a witness that can say "stale" at read time. (0) "Never lost" is a practice with reviews and a threat model. It is not a property of being append-only.


---
**skeptics L183-184 · BODY · NEW-CASE C2,C7,X2**
*3. Big-tech operational lessons › 3.2 Meta: TAO, FlightTracker, RAMP-TAO*

FlightTracker (2020) added read-your-writes across caches and indexes that TAO never had. It did so with a token the reader carries. REPORTED: "the FlightTracker service accumulates the metadata of a user's recent writes and exposes the metadata as a data type we call a Ticket." "our strategy for caches is to ignore cache entries that may be stale compared to writes in the Ticket; we refer to the resulting cache miss as a consistency miss." The lessons are the best in this report. REPORTED: "The addition of global sessions to a code base is often done fairly late in the product development cycle, to fix issues neglected in the initial design." And: "Closing consistency loopholes with FlightTracker revealed the underlying systems were not actually eventually consistent. We have found low-probability bugs that cause permanent inconsistencies in TAO, graph indexes, and even database replication. These bugs were previously difficult to notice, as they were outnumbered by transient inconsistencies. Ticket-inclusive reads should never return old data, so now that we have FlightTracker even a single occurrence of a stale result is actionable. Bugs leading to permanent inconsistencies included protocol flaws, incorrect handling of error conditions, and relying on data invariants that were not honored by all historical data." ([FlightTracker, OSDI 2020](https://www.usenix.org/system/files/osdi20-shi.pdf))


---
**skeptics L189-189 · BODY · DISAGREES C2,E6**

- (5) REPORTED mechanism, INFERRED application: big tech does not record index lag on each read. It gives the *reader* a token naming the writes it must see, and makes every cache and index honor it. For Sid, that token is a small based-on list handed to the read path.

---
**skeptics L190-190 · BODY · NEW-REASON C2**

- The lesson I would put in front of Sid first: a system that tolerates "briefly stale" cannot see its own permanent bugs, because they hide among the transient ones. Once every stale read is an error, each one is "actionable". That is an operational argument for "the map must not lie" from a team that came to it late.

---
**skeptics L218-219 · BODY · NEW-REASON C2,E1**
*3. Big-tech operational lessons › 3.4 Google: Hyrum's Law, and two instances of the fix*

3. Hand out a token nobody can read. Zanzibar, REPORTED: "A Zanzibar client requests an opaque consistency token called a zookie for each content version". "Zanzibar encodes a current global timestamp in the zookie and ensures that all prior ACL writes have lower timestamps. The client stores the zookie with the content change in an atomic write to the client storage." The check is then made "at least as fresh as the timestamp for the content version." What the opacity buys: "It ensures that Zanzibar respects causal ordering between ACL and content updates, but otherwise grants Zanzibar freedom to choose evaluation timestamps so as to meet its latency and availability goals." ([Zanzibar, USENIX ATC 2019](https://www.usenix.org/system/files/atc19-pang.pdf))


---
**skeptics L243-243 · BODY · DISAGREES C2,E6**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (5) index lag | A metric, not data | S3: a witness as a "read barrier" (REPORTED). FlightTracker: reader-carried Ticket; "consistency miss" (REPORTED). S3 customers built their own trackers when the store was silent (REPORTED) |

---
**skeptics L306-306 · BODY · NEW-REASON C2,C3**
*8. What each source implies for a second store*

- **DSQL, REPORTED basis.** Regions share one Journal and adjudicators that never stamp backward. INFERRED: a second store run by another institution cannot join that promise. The boundary between stores is then a place where order is invented, not inherited, as in the group A report.

---
**sync L227-227 · BODY · CARRIED C2**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

- (5) Yes: the checkpoint records the id of the last write it reflects. REPORTED.

---
**sync L291-292 · BODY · NEW-REASON C2**
*2. Section one: sync and multiplayer › 2.2 Convex (Sujay Jayakar, James Cowling, Jamie Turner; 2021*

- One "as of" for the whole screen: "The sync worker additionally guarantees
  that all queries in the client's query set are at the same timestamp."

---
**sync L347-348 · BODY · CARRIED C2**

- (5) Reads happen at a timestamp; the index is derived and served at that
  timestamp. The position of a read is always known. REPORTED.

---
**sync L349-350 · BODY · NEW-REASON C2**

- (10) One committer; "as of" is one timestamp; the whole client view sits at
  one timestamp. INSTITUTIONAL.

---
**sync L407-411 · BODY · CARRIED C2**
*2. Section one: sync and multiplayer › 2.3 Croquet and TeaTime (David A. Smith, David P. Reed, Alan*

- Lag is a value, and it is shown: "Usually `now == externalNow`, but if the
  model has not caught up yet, then `now < externalNow`. We call the difference
  'backlog'. If the backlog is too large, Croquet will put an overlay on the
  scene, and remove it once the model simulation has caught up." (`view.js`,
  same repository)

---
**sync L449-449 · BODY · CARRIED C2**

- (5) How far behind am I is a first-class value and is shown loudly. REPORTED.

---
**sync L524-534 · BODY · NEW-CASE C2,C3**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

- *The global order (2024).* "LiveGraph's implementation relied on a globally
  ordered stream of updates, a reasonable assumption to make when there's one
  primary database without replicas… As our lone database splintered into a
  collection of vertical shards, global ordering was no longer guaranteed."
  The assumption was "deeply baked into LiveGraph", so the stopgap was
  "artificially combining all replication streams into one." The cost: "The
  global stream could move forward only if each shard was producing updates.
  This meant that transient blips had outsized effects on users—if one shard
  was unavailable, all optimistic updates stalled (no comments could be
  made!)". (Figma Engineering, "Keeping It 100(x) With Real-time Data At
  Scale", 2024-05-17, https://www.figma.com/blog/livegraph-real-time-data-at-scale/)

---
**sync L535-541 · BODY · NEW-CASE C2**

- *Recompute instead of maintain (2024).* "most query results never change
  after initial load", so "our cache could be invalidation-based". The older
  machinery had one reason: "The historical motive for a mutation-based cache
  was load". And the price of not knowing a read's position: "If an
  invalidation arrives during an in-progress read, there is no way to tell
  whether the result is from before or after the invalidation. To ensure
  eventual consistency, LiveGraph must re-fetch in either case." (same)

---
**sync L559-560 · BODY · NEW-REASON C2**

- (5) If reads carry no position, every invalidation during a read forces a
  re-fetch. REPORTED.

---
**sync L601-605 · BODY · NEW-REASON C2**
*2. Section one: sync and multiplayer › 2.5 Linear (Tuomas Artman)*

- A total order, one number: "all transactions sent by clients follow a total
  order, whereas CRDTs typically require only a partial order … When a
  transaction is successfully executed by the server, the global `lastSyncId`
  increments by 1. This ID effectively serves as the version number of the
  database."

---
**sync L606-609 · BODY · NEW-REASON C2**

- One number for every customer: "unlike a file revision number that typically
  applies to a single file, `lastSyncId` spans the entire database, regardless
  of which workspace the changes occur in… even if a single transaction happens
  in your workspace, the `lastSyncId` often increments significantly".

---
**sync L721-724 · BODY · NEW-REASON C2,C3**
*2. Section one: sync and multiplayer › 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)*

- (10) One global version is simple, serialises every write, and makes
  per-reader permissions and deletes harder. Row versions remove the global
  lock and cost more to read. REPORTED. The position handed to clients should
  be opaque. REPORTED.

---
**sync L928-929 · BODY · NEW-REASON C2**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- (10) A version is a set of heads. A single number over a graph means
  relabelling when the graph changes (Good, issue #763). REPORTED.

---
**sync L1078-1080 · BODY · NEW-REASON C2**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- A budget for showing only consistent state: "The goal … should be to converge
  all queries to their new result within a single frame after a write".


---
**sync L1302-1310 · BODY · NEW-REASON C2**
*2. Section one: sync and multiplayer › 2.12 Matthew Weidner (CRDT survey, Fugue, Collabs, list-posi*

- With one writer, a version is one number: "A typical version corresponds to a
  point in time on the homeserver: the state resulting from the first 𝑛 updates
  in the homeserver's log, for some 𝑛. In principle, one could also expose
  versions that include tentative local updates, but these would require more
  complicated versionIDs (e.g., encoding a vector clock)." And: "A version is
  immutable, and it is unambiguously and globally identified by a version
  string of the form `<docID>@<versionID>`." ("Proposal: Versioned
  Collaborative Documents", PLF 2023,
  https://mattweidner.com/assets/pdf/versioned_collaborative_documents.pdf)

---
**sync L1322-1325 · BODY · NEW-REASON C2,C3**

**3. What he changed or regretted.** He reversed on needing CRDTs or OT under a
server. He named the limit of his own position libraries: they "fix each
character's position in a global total order as soon as the user types it".


---
**sync L1339-1341 · BODY · NEW-REASON C2**

- (10) One number per ordering domain, as long as nothing tentative is ever
  part of a version. REPORTED. Sid's no-optimism rule is what keeps "as of"
  simple. INFERRED.

---
**sync L1407-1410 · BODY · CARRIED C2,E3**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- Clocks: a label's timestamp carries the note "timestamps in a distributed
  system are not trustworthy or verified by default." Index lag is reported
  outside records: "Services can indicate synchronization status using the
  Atproto-Repo-Rev HTTP response header". (specs, Labels; Sync)

---
**sync L1504-1506 · BODY · CARRIED C2**

- (5) Lag is a header, not a record, and they still argue about stickiness.
  REPORTED. The fix they propose for identity reads is to tell the client the
  version to expect. REPORTED.

---
**sync L1675-1677 · BODY · NEW-REASON C2,P0**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

- *A derived number had to be stored once pruning existed.* "we NEED this field
  because it is the most reliable way of calculating lipmaa distances between
  msgs, in the face of sliced replication."

---
**sync L1723-1724 · BODY · NEW-REASON C2,P0**

- (5), (13) Once anything can be pruned, numbers that used to be derivable must
  be stored. REPORTED.

---
**sync L2192-2195 · BODY · NEW-REASON C2,C4**
*3. Section two: versioning › 3.6 Pijul (Pierre-Étienne Meunier, Florent Becker; 2015–now)*

- A version has no order: "versions are essentially unordered sets of patches."
  Its id is order-independent, and the empty repository has a fixed one: "The
  version identifier of an empty repository is always the identity element 1".
  (FAQ; Theory)

---
**sync L2598-2599 · BODY · NEW-CASE C2,C3**
*4. Question by question › (10) Order: can two gates write one layer? What must share a*

- Those who assumed one global order regret it or document its ceiling:
  Figma's LiveGraph; Replicache's global version. REPORTED.

---
**sync L2601-2602 · BODY · NEW-REASON C2**

- A moment can be named three ways: a vector (large, universal), a frontier
  (small, needs shared history), a local number (local only): Gentle. REPORTED.

---
**sync L2603-2604 · BODY · NEW-REASON C2**

- With one writer and no tentative state, a version is one integer per domain;
  tentative state forces vectors: Weidner. REPORTED.

---
**sync L2605-2605 · BODY · NEW-REASON C2**

- Build a whole screen at one position: Convex. REPORTED.

---
**sync L2705-2717 · BODY · CARRIED C2**
*4. Question by question › (5) Based on: if an index was behind the log, is how far it *

**What the camp says.** Bayou "stably records the unique identifier of the last
Write reflected in the Tuple Store checkpoint". REPORTED. Croquet names the gap
("backlog") and covers the scene with an overlay when it is large. REPORTED.
Figma's LiveGraph does not stamp reads, so "there is no way to tell whether the
result is from before or after the invalidation", and it re-fetches every time.
REPORTED. AT Protocol reports lag in an HTTP header and still debates whether a
server is "read-sticky"; its fix for identity reads is that "clients know what
version of the DID document to expect". REPORTED. Bayou's session guarantees
are the general form: a session remembers what it has read and written and
refuses a server that is behind it. REPORTED. Git's side index now exists in
two generations that readers must tell apart. REPORTED. PPPPP had to store a
number that used to be derivable once pruning existed. REPORTED.


---
**sync L3043-3044 · BODY · CARRIED C2**
*5. The group › 5.1 The voices that matter most, who I dropped, who is missi*

- **Differential dataflow and Materialize (Frank McSherry).** Positions as
  frontiers; recomputation when inputs move. Bears on (5) and (10).

---
**sync L3045-3046 · BODY · NEW-REASON C2,C3**

- **cr-sqlite and ElectricSQL.** Per-cell versions in SQL; positions handed to
  clients.

---
**sync L3203-3205 · BODY · NEW-REASON C2,E3**
*6. The second store: what each source implies*

- **"As of" becomes a map** from store to position token (Gentle's frontier).
  Never compare "when" across stores; there is "not necessarily an underlying
  objective truth" about which came first (Keyhive, REPORTED).

