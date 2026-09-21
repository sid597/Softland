# C3 order (10): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L62-62 · BODY · CARRIED C3,C2**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.1 What they built, and what they chose*

- **One writer per database, one total order.** A *transactor* serializes all transactions. Each gets a number, `t`. A database value is identified by its `basis-t`.

---
**datalog L105-108 · BODY · CARRIED C3**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

> "There is research done at Yale that proved it is much faster to just serialize transactions and do it all in memory, than it is to have a complicated scheme for trying to figure out who is overlapping with who." — Hickey [H-WD]

> "All transactions are serialized, period." — Hickey [H-HN12]


---
**datalog L109-114 · BODY · ABOVE C3**

He was blunt about the cost. An audience member asked whether the transactor is the bottleneck:

> "It _is_ the bottleneck… Because nothing is infinite. That is why it is not a problem. If you need arbitrary write scaling, this is not the system for you. If you are like 99% of the businesses that could not saturate one box with the amount of novelty in your system, this was a good fit… But trying to make a universal system that can handle infinite, means dropping a whole bunch of value." — Hickey [H-WD]

> "That said, Datomic is not the right choice when you require unlimited write scalability." — Hickey [H-ARCH]


---
**datalog L115-118 · BODY · CARRIED C3**

And on what is lost the moment a store is sharded:

> "You start sharding, and then you cannot query against shards. You cannot do transactions across shards. You cannot ensure consistency across shards. And really, that is why I think you should consider these independent." — Hickey [H-WD]


---
**datalog L252-252 · BODY · CARRIED C3,C2**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED. One number, because one order. "Transactions are fully serialized, and transaction ids increase over time, so transaction ids are a total ordering of all transactions." [D-MODEL]. "Peers always see all transactions up to their time basis, in order, with no gaps." [D-ACID]

---
**datalog L255-255 · BODY · CARRIED C3,E1,X2**

- INFERRED. What must share a partition: whatever one gate decision must see consistently and whatever must land together. In Datomic that is the whole database, schema included. That is why Datomic can say exactly when a rule starts to apply: attribute predicates "will be enforced starting on the transaction after they are asserted." [D-SCHEMA]. If Sid's policy and grammar facts sit in another partition from the fact being admitted, the question of which policy was in force has no answer unless the gate writes down the position it read them at.

---
**datalog L256-256 · BODY · CARRIED C3,C2**

- INFERRED. "As of: one number or a position per partition?" The camp would say the answer falls out of a prior choice: *what is the unit that has one timeline?* Inside that unit, as-of is one number by construction. Across units it is one number per unit, and a query takes several database values as arguments. Hickey's complaint about sharding is that the units stop being queryable together. His remedy is to choose the unit by meaning: "I called that sort of a 'shard above'. And there are systems that shard below, where it is the system's responsibility to shard. That is always mechanical, and the system does not understand what you are doing, mostly. Sharding above becomes an application problem. On the other hand you have a lot of flexibility." [H-IONS]

---
**datalog L257-258 · BODY · CARRIED C3,C2**

- My synthesis, marked as mine and not theirs: the unit Sid already has is the layer. If a layer is the unit of order, then "as of" for an asker is a short list, one position per layer in their stack: base, their own, the session. Three numbers can be said aloud, stored on a fact, and handed to someone else, which is the property Hickey prizes. A position per physical partition cannot. Whether one Rama partition can carry the base layer of a whole field is a question for the Rama group, not this one. Datomic's own guidance is that "operational considerations" begin past 10 billion datoms of history in one database. [D-FAQ]


---
**datalog L343-343 · BODY · ABOVE C3**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.5 What resembles Sid's situation, and what differs*

- **Write rate.** Hickey's claim is that 99% of businesses cannot saturate one box. Tens of agents per person at machine rate is the other 1%, and he says plainly his system is not for it.

---
**datalog L344-344 · BODY · NEW-REASON C3,C2**

- **Size.** Guidance starts to bite at 10 billion datoms of history per database. A field of ten million papers with summary layers, plus crossings, passes that soon.

---
**datalog L353-353 · BODY · CARRIED C3**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

- **Partitioned logs** (Kafka, Rama). Hickey: shards are independent databases, and you lose query, transactions and consistency across them.

---
**datalog L367-370 · BODY · CARRIED C3**
*3. Nubank: Datomic lived with, at scale › 3.1 What they built, and what they chose*

A bank on Datomic, from 2013. Datomic is the default system of record. One database per microservice. Later, whole copies of the stack, each serving a slice of customers.

The numbers, with dates. About 2016: "over a dozen independent Datomic databases" [N-STORY]. May 2024: "an average of 2.5 billion Datomic transactions being processed each day" [N-JEPBLOG]. July 2025, a Nubank conference abstract: "3000+ Datomic databases, 4000 microservices, and process over 70 billion Kafka events/day" [N-DEVBCN]. Nubank bought Cognitect, Datomic's maker, in 2020.


---
**datalog L377-380 · BODY · CARRIED C3**
*3. Nubank: Datomic lived with, at scale › 3.2 Their reasons*

On how they scale, which is not by partitioning a database:

> "So we considered a different model, the scalability unit model. When you are doing scalability units, your shards are not database shards, they are actually copies of your infrastructure… we build clones of the infrastructure and we assign different partitions of our customer base to different clones, those are our shards." — Rafael Ferreira [N-QCON]


---
**datalog L381-382 · BODY · CARRIED C3**

> "Our primary database, Datomic, runs multiple transactors in Kubernetes, each handling a subset of data… Nubank took this idea a step further by sharding not just databases, but also key services and entire microservice clusters." [N-LIMITS]


---
**datalog L383-384 · BODY · CARRIED C3**

This is Hickey's "shard above", done by the people who later bought the product. It is forced by the product: "In on-prem you should run a single primary logical DB per transactor." — Marshall Thompson, 2020 [M-SLACK]


---
**datalog L397-398 · BODY · CARRIED C3**
*3. Nubank: Datomic lived with, at scale › 3.3 What they regret*

A fifth, about reading across many databases: "What did not scale well was using Datomic and our operational transactional infrastructure for aggregations, even for simple things, like how many customers we have. Our CEO asked this many times and it was increasingly difficult to answer… even, with sharding and fragmenting the operational systems so that they scale better, you are making analysis harder." They built an ETL to a separate analytic store. [N-QCON]


---
**datalog L401-401 · BODY · CARRIED C3**
*3. Nubank: Datomic lived with, at scale › 3.4 Which questions they speak to*

- **(10) order.** INSTITUTIONAL. One total order per database, thousands of databases, the split chosen by meaning (service, then customer). They never tried to make one database carry the bank.

---
**datalog L413-418 · BODY · CARRIED C3**
*3. Nubank: Datomic lived with, at scale › 3.6 Who they disagree with*

### 3.6 Who they disagree with

Quietly, with two of Hickey's claims. "99% of the businesses… could not saturate one box": they did, and cloned the stack. "Queries can take more than one database": true, and it did not hold for analytics across a fragmented estate.

---


---
**datalog L451-456 · BODY · CARRIED C3**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.2 Their reasons*

**Why one order.**

> "All transactions that perform writes are serialized via a totally-ordered durable log." [X-TXDOC]

For Kafka the partition count must be "Exactly 1. A single partition is what makes the log strictly ordered." The node checks this on an existing topic. [X-KAFKA]


---
**datalog L457-458 · BODY · CARRIED C3**

> "writes for a given database happen once, on a single thread, and reads scale out across the cluster. This design implies a hard upper limit on transaction throughput, but the key advantage is the concrete information guarantees about exactly when, how & why data across the database has changed." [X-WHAT]


---
**datalog L459-460 · BODY · NEW-REASON C3**

On the limit itself: "You can get a hell of a lot done in a single thread - locally, we've seen 300k docs/second written on a single thread when you take network latency out." [X-LEAD]


---
**datalog L488-488 · BODY · CARRIED C3**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.4 Which questions they speak to*

- **(10) order.** REPORTED. Single partition, enforced. "Hard upper limit." Several databases per cluster since 2.1.

---
**datalog L524-525 · BODY · CARRIED C3**
*5. The leads › 5.1 Instant (Stepan Parunashvili, Joe Averbukh)*

**(10) Order.** REPORTED. From one Postgres: a transactions table with an increasing id, and the write-ahead log feeding an "invalidator" that works out which live queries a change affects. [I-ARCH], [I-TXSQL]


---
**frontiers L78-78 · BODY · CARRIED C3,A3**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.3 What changed over the years (worth more than the papers)*

- **Independent per-shard frontiers were not enough for writes across shards.** The 2023 transaction design (INSTITUTIONAL; author not verified) added one coordinating log: "Efficient atomic multi-shard writes are accomplished through a new singleton-per-environment _txns shard_". "As time progresses, the upper of every data shard is logically (but not physically) advanced en masse with a single write to the txns shard." The costs are stated plainly: "All txn writes are linearized through the txns shard, so there is some limit to horizontal and geographical scale out." and "Latency on contended workloads will likely be quite bad. At a high level, if N txns are run concurrently, 1 will commit and N-1 will have to (usually cheaply) retry." ([Txn management design](https://github.com/MaterializeInc/materialize/blob/main/doc/developer/design/20230705_v2_txn_management.md))

---
**frontiers L86-86 · BODY · CARRIED C3**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(10) what must share a partition?** INFERRED: only what must share a *time source*. Order across partitions comes from the timeline, not from co-location. REPORTED constraint: updates of one transaction get one time; ordered inputs get ordered times.

---
**frontiers L176-177 · BODY · NEW-REASON C3,A3**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.1 What was built, and what was chosen*

A fifteen-year line at Berkeley (Alvaro now at UC Santa Cruz). Dedalus: Datalog where time is an ordinary column. Bloom: a language built on it. CALM: the result that says exactly when coordination is needed. Blazes: a tool that finds where a dataflow needs coordination and adds the cheapest kind. Molly: lineage-driven fault injection, later run at Netflix. Hydro: the current attempt. The choice that matters: **treat time and order as data in the logic, and coordinate only where the logic is non-monotone.**


---
**frontiers L180-181 · BODY · NEW-REASON C3,A3,C2**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.2 The reasons, in their words*

**The CALM theorem.** REPORTED: "A program P is monotonic if for any input sets S,T where S ⊆ T, P(S) ⊆ P(T)." "Theorem 1. Consistency As Logical Monotonicity (CALM). A program has a consistent, coordination-free distributed implementation if and only if it is monotonic." And the intuition: "monotonic programs are 'safe' in the face of missing information, and can proceed without coordination. Non-monotonic programs, by contrast, must be concerned that truth of a property could change in the face of new information. Therefore they cannot proceed until they know all information has arrived, requiring them to coordinate." ([Keeping CALM, Hellerstein and Alvaro, CACM 2020; arXiv](https://arxiv.org/pdf/1901.01930))


---
**frontiers L188-189 · BODY · CARRIED C3**

**Two ways to coordinate: sequence, or seal.** REPORTED, Blazes: "Two extreme approaches include (a) establishing a single total order in which all instances of a given component receive messages (a sequencing strategy) and (b) disallowing components from producing outputs until all of their inputs have arrived (a sealing strategy)." Sealing "indicates when partitions of a stream have stopped changing." And: "Note that sealing is significantly less constrained than ordering: it enforces an output barrier per partition, but allows asynchrony both in the arrival of a batch's inputs and in interleaving across batches." ([Blazes, Alvaro, Conway, Hellerstein, Maier, ICDE 2014; arXiv](https://arxiv.org/pdf/1309.3324))


---
**frontiers L192-193 · BODY · NEW-REASON C3**

**What time is for.** REPORTED, Hellerstein: "I call this the 'Fateful Time' conjecture because it argues that the inherent purpose of time is to seal fate." And the CRON conjecture: "Program semantics require causal message ordering if and only if the messages participate in non-monotonic derivations." "Time does matter, exactly in those cases where ignoring it would result in logically ambiguous fate". ([The Declarative Imperative, SIGMOD Record 2010; tech report](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2010/EECS-2010-90.pdf))


---
**frontiers L205-205 · BODY · NEW-REASON C3,C2,X3**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.4 Which questions they speak to*

- **Where Sid's store is monotone, and where it stops.** INFERRED straight from CALM: appending facts is monotone. Four things are not. "Current" (no later version exists). "Nearest layer wins" (no nearer row exists). The gate's version check. The gate's policy check, if policy can be withdrawn. Each is a statement about absence. By the theorem each needs either coordination or a seal. The gate is coordination placed exactly at the version check, which is right. The other three are *reads*, and the design as described gives them nothing.

---
**frontiers L206-206 · BODY · DISAGREES C3,A3**

- **(10) can two gates write one layer?** INFERRED from CALM and Blazes: for keys whose values only accumulate (set-like), yes, with no coordination. For keys with one current value (register-like), all offers for a cell must pass through one sequencer. So whether a key is a growing set or a register is a property the grammar fact could state. A gate that runs compare-and-set on every cell of every key coordinates more than the logic needs.

---
**frontiers L207-207 · BODY · CARRIED C3**

- **(10) what must share a partition?** INFERRED from Blazes: whatever one non-monotone decision reads. A per-partition seal is cheaper than a global order, so put a cell, and what the gate must check against it, under one seal.

---
**frontiers L223-224 · BODY · NEW-REASON C3,A3**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.6 Disagreements*

With the position that strong consistency should simply be used everywhere, on cost: the CACM paper opens with coordination as the dominant cost. With CRDT advocates, on reads. With their own earlier bet on new languages. With McSherry and Brandon not on substance but on emphasis: Berkeley asks where coordination can be skipped. Materialize asks how to make one timeline cheap.


---
**frontiers L322-323 · BODY · DISAGREES C3,A3**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.2 Reasons, in Goebel's words*

On transactions ([Perspectives 2019](https://www.nikolasgoebel.com/2019/12/30/perspectives-2019.html)), REPORTED: "Transaction processing really is a three step process: Work hard to minimize the number of conflicts. Actually change things. Work hard to correct the conflicts that did arise." "After spending some time working with Frank McSherry, I have grown much more interested in approaches that invest the bare minimum in step (1), and instead focus on doing step (3) as efficiently as possible."


---
**frontiers L333-333 · BODY · DISAGREES C3,A3**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.4 Which questions Goebel speaks to*

- **The gate under machine-rate writers.** INFERRED from the three-step remark: compare-and-set that refuses and makes the writer retry is an investment in step 1. With tens of agents per person writing to the same cells, Goebel would look at repairing conflicts after the fact.

---
**frontiers L343-344 · BODY · DISAGREES C3,A3**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.6 Disagreements*

With conflict avoidance as the centre of transaction processing. With always-eager maintenance, by example.


---
**frontiers L399-400 · BODY · DISAGREES C3,A3**
*8. Voices that matter most, who was dropped, who is missing*

3. **Alvaro and Hellerstein.** For knowing where the gate is needed and where it is waste, for the manifest as a safety tool, and for lineage that separates alternative supports. Their record of what did not survive production (new languages, fine-grained lineage) is the most useful regret in the group.


---
**frontiers L446-446 · BODY · DISAGREES C3**
*11. What each source implies for a second store*

- **CALM, INFERRED:** accumulating facts merge across stores with no coordination. Anything with a "current" needs exactly one owning store per cell.

---
**log L67-68 · BODY · CARRIED C3,C1**
*Voice by voice › 1. Pat Helland*

**1. What he built and chose.** Helland built transaction systems for decades (Tandem, Microsoft, Amazon, Salesforce) and then wrote down what he stopped believing. His choice, stated across five papers: data that crosses a boundary is immutable, identified, versioned, and from the past; the unit of consistency is one uniquely keyed entity; retries are certain, so every request carries an id.


---
**log L76-76 · BODY · CARRIED C3**

- On what shares an order: "How can you know that two separate entities are guaranteed to be within the same transactional scope and, hence, atomically updatable? You know only when a single unique key unifies both. Now it is really one entity!" and "You can never count on different entity-key values residing in the same place." (*Life beyond Distributed Transactions*, ACM Queue 2016.)

---
**log L82-83 · BODY · CARRIED C3,C2**

**3. What he changed.** The clearest revision in the whole camp. The 2007 abstract of *Life beyond Distributed Transactions* says: "My experience over the last decade has led me to liken these platforms to the Maginot Line." The word "Maginot" appears twice in the 2007 paper and not once in the 2016 ACM Queue version. The 2016 version opens instead with a concession that Spanner-class systems "offer strongly consistent transactional environments at extremely large scale with excellent availability", and adds "Unfortunately, this is not broadly available to application developers." He softened the absolute and kept the practical conclusion. By contrast, the 2020 republication of *Outside versus Inside* ends: "Nomenclature aside, not much has changed." (XML appears 39 times in 2005 and once in 2020; the model is word-for-word the same.) His newest statement on partitions, CIDR 2024: "When each record's changes go to ONE log, it has a “home”." On re-partitioning: "Earlier record versions were placed in their old log. After repartitioning new versions must go to a new log. This is difficult in the midst of ongoing updates."


---
**log L90-90 · BODY · CARRIED C3**

- (10) REPORTED: only what sits under one key shares an order; nothing else may be assumed to be in the same place.

---
**log L113-113 · BODY · NEW-REASON C3,W3**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- Why not one total order: "the simplicity of a shared log requires imposing a system-wide total order that is expensive, often impossible, and typically unnecessary." And across regions "a total order may be impossible: a network partition can cut off clients from the sequencer". (FuzzyLog, OSDI 2018.)

---
**log L123-123 · BODY · NEW-REASON C3,W3**

- *Total order, relaxed.* FuzzyLog is the CORFU author saying the system-wide order was "typically unnecessary".

---
**log L125-125 · BODY · NEW-CASE C3**

- *Sharded readers over one log failed for a reason nobody predicted.* OSR 2024: "the key blocker for such a design proved to be blast radius: e.g., if we lose access to a single slot in the shared log after it is written (but before it is played), every learner across all shards will have to block processing at that entry".

---
**log L159-159 · BODY · CARRIED C3,A3**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- Order and concurrency belong to the stream: "it is imperative that both use ExpectedVersion set to the last event they read." (chapter "Cheating".)

---
**log L177-177 · BODY · CARRIED C3**

- (10) REPORTED: the stream is the unit of order; if two things need mutual order, put them in one stream or merge in the application.

---
**log L184-185 · BODY · CARRIED C3**

**5. Resembles and differs.** Resembles: never-rewrite with audit as the point; many writers; optimistic concurrency by expected version is Sid's offer almost exactly; years of lived experience. Differs: events are coarse business facts inside one team's bounded context, written by that team's code. Sid's facts are small, written by anyone, in one shared store, with grammars that are themselves facts. Young's "stream" is a much larger unit of order than Sid's cell.


---
**log L198-198 · BODY · NEW-REASON C3,W3**
*Voice by voice › 4. Martin Kleppmann*

- The cost of one order: "appending an event to the log requires at waiting least one network round-trip to the leader and/or a quorum of replicas." When writers may be disconnected, "the assumption of a totally ordered log becomes impossible to satisfy". (DEBS 2021; the typo is his.)

---
**log L216-216 · BODY · CARRIED C3**

- (10) REPORTED: all claims on one thing go to one partition; an event touching two entities breaks per-partition processing; a worldwide total order costs a round trip per write. His one real example of a single worldwide ordered store is low-rate: "The New York Times maintains all textual content published since the newspaper's founding in 1851 in a single log partition".

---
**log L296-296 · BODY · NEW-REASON C3,A3**
*Voice by voice › 7. Phil Bernstein's Hyder*

- "the longer it takes to meld a transaction's intention, the greater the number of intentions in each transaction's conflict zone and hence the greater the chance that each transaction aborts." (VLDB 2011.)

---
**log L299-300 · BODY · CARRIED C3,C2**

- The way out they proposed was more than one certifier and more than one log (Bernstein and Das, *IEEE Data Engineering Bulletin*, 2015). The rule for what must share a log: "There is a total order between transactions accessing the same partitions, which is preserved in all logs where both transactions appear." Across logs: "Notice that LSNs in different logs are incomparable". "two log entries have a defined order only if they accessed the same partition." Then: "It remains as future work to implement the algorithm proposed here and compare it to Tango's." I found no sign it was built, and no retrospective by Bernstein.


---
**log L303-303 · BODY · CARRIED C3,C2**

- (10) REPORTED: their own answer to "can there be two certifiers" is yes, if everything that must be mutually ordered passes through a shared log. Across logs, positions cannot be compared, and an "as of" becomes a pair or a vector.

---
**log L305-305 · BODY · NEW-CASE C3**

- (10, machine rate) INFERRED from their abort-rate finding. The longer an offer travels before the gate sees it, the more often its expected version is stale. With agents writing at machine rate into a worldwide store, a hot cell will refuse most offers. The cure in this camp is never a faster gate. It is grammar design: many small cells that do not contend, rather than one cell holding a collection. Helland says the same from the other side: "Storage systems alone cannot provide the commutativity we need".

---
**log L325-325 · BODY · NEW-CASE C3**
*Voice by voice › 8. Amazon Aurora, and what AWS did next*

- *Multi-master* (2019) let several instances write one volume. AWS's own announcement page now carries the line: "9/8/23: Multi-Master is no longer available as of Feb 28, 2023". Its unit of conflict was the 16 KiB page: "Aurora detects write conflicts at the level of the physical data pages", so unrelated rows could conflict, and "when conflicts occur, they incur substantial overhead." AWS published no reason for the retirement, and I draw none.

---
**log L350-350 · BODY · CARRIED C3**
*Voice by voice › 9. Two short notes on voices the brief did not list*

- Order: "there are no ordering guarantees across different depots", and "Rama guarantees local ordering."

---
**log L429-430 · BODY · CARRIED C3**
*Question by question: what this camp would say › (10) Order: two gates on one layer? What must share a partit*

REPORTED. The unit of order is a keyed thing, never a namespace. Helland: "You know only when a single unique key unifies both." Young: the stream. Kleppmann: "all claims to the same username go to the same partition." Bernstein and Das: "two log entries have a defined order only if they accessed the same partition." DSQL: each key belongs "to at most one adjudicator at any given time".


---
**log L433-434 · BODY · CARRIED C3**

What must share a partition: whatever one compare-and-set must see. For Sid that is the cell (entity, key, layer) at least. If anything else needs mutual order, such as a person's gestures in sequence, it must be keyed into one ordered unit on purpose; Young's advice is to put it in one stream or merge in the application. Helland's warning is that the choice is permanent in practice: "Earlier record versions were placed in their old log."


---
**log L532-533 · BODY · CARRIED C3,C5**
*For the group › 7. The voices that matter most for Sid, who I dropped, who i*

**Helland is the frame rather than a fourth voice.** Pin your pointers; absent is allowed and different is not; only one key shares an order; every request carries an id. He has few operational regrets to offer, because he writes principles, not systems.


---
**meaning L898-905 · BODY · CARRIED C3**
*Part two — the people › 2.2 Kenton Varda: Protocol Buffers, Cap'n Proto, Sandstorm, *

**One writer per id.** Durable Objects give each id one single-threaded object.

- REPORTED (Varda, Cloudflare blog, 2024): "they are intended to scale out, not
  up. A single object is inherently limited in throughput since it runs on a
  single thread of a single machine [...] consider a vote counter with a million
  users all trying to cast votes at once. To handle such cases with Durable
  Objects, you would need to create a set of objects that each handle a subset
  of traffic and then replicate state to each other."

---
**meaning L906-908 · BODY · CARRIED C3**

- REPORTED (2020): "Each object can see only its own data. To perform a query or
  transaction across multiple objects, the application needs to do some extra
  work."

---
**meaning L915-922 · BODY · DISAGREES C3,C2**

- What this says to (10), INFERRED. One orderer per unit is right, and the unit
  must be small. The only thing that strictly needs one orderer is the
  compare-and-set cell: entity, key, layer. Anything wider (a whole layer, the
  base) is a choice that caps throughput at one thread. A planet-wide base layer
  ordered as one unit is Varda's million-vote counter. And once units are small, "as
  of" across them is a position per unit, not one number, unless something
  global hands out numbers.


---
**meaning L1579-1591 · BODY · NEW-REASON C3,E3**
*Part two — the people › 2.8 David Reed's pseudo-time, and Croquet*

**What Croquet did with it.** The 2003 Croquet paper (Smith, Kay, Raab, Reed)
describes Reed's scheme: peers, tentative versions, two-phase commit. There is
no central orderer in it. Today's Croquet is built on one. INSTITUTIONAL
(Croquet docs): "Reflectors are stateless, public message-passing services".
"Models have no concept of real-world time. All they know about is simulation
time, which is governed by the reflector." "Every event that passes through the
reflector is timestamped." "All replicas of the model receive exactly the same
stream of events in exactly the same order." Vanessa Freudenberg (2021): "A
reflector just bounces events from one user to all users. There's no computation
involved". Kay says there were "three or four different versions of this
mechanism". The gatherer found no text saying why they moved from Reed's peers
to one orderer. That is a gap, not an inference.


---
**meaning L1614-1621 · BODY · ABOVE C3**

**Reed, thirty-four years on.** "'Simultaneous' Considered Harmful" (2012)
argues against "one universal total ordering of all events", and says that in
"open, unbounded, evolving systems... there is no utility to speaking of
'simultaneous' at all." The gatherer found nothing by Reed on Croquet's
reflector, and warns against reading this as a repudiation. I read it only as
this: the person Kay sends everyone to for time would not sign "one order for
the planet". Reed would sign one order per unit that needs it.


---
**meaning L1856-1859 · BODY · NEW-CASE C3**
*Part three — substrates › 3.1 Dynamicland's Realtalk, and Folk Computer*

- Order, once they went parallel. REPORTED (Mar 2024): "stuff that we 'got for
  free' from having a single thread and converging to a fixed point. like, what
  order do you do operations in [...] what does 'order' mean if things can happen
  in parallel.."

---
**meaning L2010-2015 · BODY · NEW-REASON C3**
*Part three — substrates › 3.4 Jonathan Edwards (Subtext, schema change)*

**On one writer.** Edwards sides with a centre. REPORTED (Baseline §7): "extending our
approach to do replication would require a centralized primary whose order of
operations decides conflict resolution for everyone consistently". Against the
road Webstrates took: "The power of CRDTs is their monotonic semantics but that
is also their weakness. They can't go backwards."


---
**meaning L2341-2351 · BODY · CARRIED C3**
*Part four — Sid's questions, hung on the parts of the fact › (10) Order: two gates on one layer? what shares a partition?*

### (10) Order: two gates on one layer? what shares a partition? what is "as of"?

**Camp.** One orderer per unit, and keep the unit small. Varda: objects "scale
out, not up"; a vote counter with a million voters must be split (2.2). Croquet:
one stamper per session (2.8). Edwards: a "centralized primary whose order of
operations decides conflict resolution for everyone" (3.4). What it costs to give
the orderer up, from MyWebstrates: version numbers become "only locally valid"
(3.3). From Folk: "what does 'order' mean if things can happen in parallel"
(3.1). Against one order for everything: Reed in 2012, and Hewitt (2.8, 2.11).
Nanopublications avoid the question by having no cell to update (2.6).


---
**rama L72-73 · BODY · CARRIED C3**
*Part one — What Rama's own reference says › Question by question › (2) Entity: how is an id made so two never clash? May an id *

One more constraint from partitioning. The depot partitioner hashes something in the record, "modded by number of tasks" (`docs/16-partitioners.md:153`). **IMPLIED:** whatever the id is, the part of it that the partitioner reads decides which facts are ordered with which, for the life of the module.


---
**rama L132-132 · BODY · CARRIED C3**
*Part one — What Rama's own reference says › Question by question › (10) Order: can two gates ever write one layer? What must sh*

- **CHECKED** `docs/23-acid-semantics.md:27` — "each task is single-threaded. This means all actions on a task happen in serial."

---
**rama L133-133 · BODY · CARRIED C3,E1**

- **CHECKED** `docs/23-acid-semantics.md:15` — "stream topologies are transactions for changes on a single partition, while microbatch topologies are cross-partition transactions for every change across all partitions."

---
**rama L134-134 · BODY · CARRIED C3**

- **CHECKED** `docs/14-depots.md:62` — without a shared partition, "data on different partitions are processed in parallel and independently."

---
**rama L135-135 · BODY · CARRIED C3**

- **CHECKED** `docs/14-depots.md:121` — "Data should be appended to the same depot when related… Data is related if local ordering is important or if they affect the same conceptual entities."

---
**rama L143-144 · BODY · CARRIED C3,E1,E4**

What must share a partition: under a stream gate, everything that has to change in one atomic step. That is the cell, the fact, the verdict, and any index that must never disagree with them. **IMPLIED:** also whatever the gate must read to decide. A grammar fact or policy fact on another partition costs a hop, and a hop ends the transaction (`skill/stream.md:41`). The check and the write are then two steps, and the policy can move between them. Under a microbatch gate nothing has to share a partition for atomicity, because the whole microbatch is one transaction.


---
**rama L229-229 · BODY · CARRIED C3,E1**
*Part one — What Rama's own reference says › The gate: stream or microbatch*

| | Stream gate | Microbatch gate |
| Atomic scope | one event on one task (`docs/23-acid-semantics.md:41`) | the whole microbatch, all partitions (`docs/23-acid-semantics.md:37`) |

---
**rama L234-234 · BODY · NEW-REASON C3**

| | Stream gate | Microbatch gate |
| One stalled task group | that group's partitions fail | "all microbatch topologies in the module… continuously fail" (`docs/21-replication.md:79`) |

---
**rama L245-246 · BODY · CARRIED C3,W1**
*Part one — What Rama's own reference says › Other items the brief named*

**Partitioner choice and co-locating everything about one entity.** **CHECKED** `.claude/skills/rama/SKILL.md` "Colocate related data": design partitioning "around the application's core queries, not just the top-level key… `$$post->likes` might all partition by account ID (not post ID)". **CHECKED** `skill/depot-design.md:138-145`: "Match the partition key to the primary PState access pattern… Mismatched key forces `|hash` repartition (extra hop, breaks local ordering)." **IMPLIED:** for Sid the cell is entity + key + layer, but the partition key should be coarser than the cell, so that one entity's facts across keys and layers sit together and one gate event can see them all. The entity id alone is the obvious candidate. The cost: a very hot entity (a shared base-layer type that everything points at) is one partition's load. Facts that point at it live with their own entity, not with it, so reads of "everything pointing at X" are a fan-out or a second index.


---
**rama L293-294 · BODY · CARRIED C3,W1**
*Part two — Nathan Marz › 3. What he later changed, regretted, or migrated*

**(a) From recompute-everything to incremental views.** In 2011 the enemy was incremental state: "It's a reliance on incremental algorithms and mutable state that leads to complexity in our systems." The book has a section titled "The problems with fully incremental architectures". In 2023: "Rama is not batch-based. That is, PStates are not materialized by recomputing from scratch. They're incrementally updated either with stream or microbatch processing. But PStates can be recomputed from the source data on depots if needed." (HN 37139258). He kept the log as the source of truth and the right to recompute. He dropped the second system and the constant recompute. That is the shape Kreps argued for in 2014. Rama's own recipe for re-partitioning a module (start a new module from the beginning of each depot, switch clients when caught up) is Kreps's Kappa recipe almost step for step. Marz now describes a depot as "exactly like the Apache Kafka but built into the system" (Software Engineering Daily, episode 1600, 2023-12-28, transcript at https://softwareengineeringdaily.com/wp-content/uploads/2023/12/SED1600-Rama.txt).


---
**rama L377-378 · BODY · NEW-REASON C3,W3**
*Part three — The dissent: Jay Kreps › 2. His reasons, in his words*

On global order. "Each partition is a totally ordered log, but there is no global ordering between partitions (other than perhaps some wall-clock time you might include in your messages)… Lack of a global order across partitions is a limitation, but we have not found it to be a major one. Indeed, interaction with the log typically comes from hundreds or thousands of distinct processes so it is not meaningful to talk about a total order over their behavior." (*The Log*)


---
**rama L392-392 · BODY · NEW-REASON C3**
*Part three — The dissent: Jay Kreps › 3. Where he and Marz disagree*

2. **Is streaming second-class?** Marz in 2011 let the speed layer be approximate and had batch correct it. Kreps: "there is no reason that a stream processing system can't give as strong a semantic guarantee as a batch system."

---
**skeptics L46-47 · BODY · CARRIED C3,C2,P0**
*1. The ordinary default, question by question*

**(10) Order.** One primary database gives one total order (its log sequence number). Kafka gives order inside a partition only, so teams pick the entity id as the partition key and give up cross-entity order. A consumer's place is stored as one offset per partition. So **the ordinary industry answer to "as of" on a partitioned log is a position per partition**, and the ordinary answer on a single database is one number. The transactional outbox exists because teams cannot write to a database and a log atomically. REPORTED: "The command must atomically update the database and send messages in order to avoid data inconsistencies and bugs. However, it is not viable to use a traditional distributed transaction (2PC) that spans the database and the message broker". And: "messages must be sent to the message broker in the order they were sent by the service." ([microservices.io, Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html)). Debezium states the aim: "An outbox pattern implementation avoids inconsistencies between a service's internal state (as typically persisted in its database) and state in events consumed by services that need the same data." ([Debezium outbox event router](https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html)). INFERRED: the outbox is the default's admission that two truths (rows and events) drift. Sid's one store has no such seam.


---
**skeptics L109-110 · BODY · ABOVE C3**
*2. The skeptics › 2.1 Michael Stonebraker with Joe Hellerstein (2005) and with*

- One store for the planet. REPORTED: distributed rarely beats a single node. INFERRED: scale up first, shard by person later.


---
**skeptics L185-186 · BODY · NEW-CASE C3,X1**
*3. Big-tech operational lessons › 3.2 Meta: TAO, FlightTracker, RAMP-TAO*

RAMP-TAO (2021) layered atomic visibility on top, after measuring the damage. REPORTED: "1 in 1,500 batched reads reflects partial transactional updates". They "bolt on": "Our layering strategy takes the 'bolt-on' [18] approach to stronger" guarantees, so that only "applications that need stronger guarantees incur the resulting performance costs." ([RAMP-TAO, VLDB 2021](https://www.vldb.org/pvldb/vol14/p3014-cheng.pdf))


---
**skeptics L192-193 · BODY · CARRIED C3,X1**

- (10) and one writer. REPORTED: cross-shard writes were not atomic and were repaired in the background. INFERRED: a chain of facts that must land together across partitions will sometimes land in part. Sid's because-of chain makes a half-landed chain detectable. TAO needed a repair job to find the same thing.


---
**skeptics L305-305 · BODY · NEW-REASON C3,A3**
*8. What each source implies for a second store*

- **Dynamo, REPORTED.** Two stores that both accept writes to the same cell brings back all that Amazon dropped: version vectors, truncated clocks, merging by the application.

---
**sync L555-558 · BODY · CARRIED C3,C2**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

- (10) The ordering unit is the file; one process owns it; routing prevents
  "split brain". REPORTED. A global order, once assumed, gets "deeply baked"
  and couples everyone's availability. REPORTED. So: never let code do
  arithmetic on a global position. INFERRED.

---
**sync L676-685 · BODY · NEW-REASON C3,C2,C5**
*2. Section one: sync and multiplayer › 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)*

- The documented menu of version schemes. Global version: "A single global
  version is stored in the database and incremented on each push … While
  simple, the Global Version Strategy does have concurrency limits because all
  pushes server-wide are serialized, and it doesn't support advanced features
  like incremental sync and read authorization as easily as row versioning."
  (https://doc.replicache.dev/strategies/global-version) Row version: "It does
  not require global locks or the concept of spaces. It does not require a soft
  deletes. Entities can be fully deleted. The disadvantage is that it pays for
  this flexibility in increased implementation complexity and read cost."
  (https://doc.replicache.dev/strategies/row-version)

---
**sync L771-774 · BODY · CARRIED C3,X4**
*2. Section one: sync and multiplayer › 2.7 tldraw sync (Steve Ruiz)*

- A second lane: "A room can serve some record types through a second
  partition, the object-store lane, which is persisted and permissioned
  separately from the document. Comments are the main use for it." (same)


---
**sync L1058-1062 · BODY · NEW-REASON C3,E3**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- What a centre gives: "One of the primary functions of a central server is
  that it serializes the operations performed by group members, meaning that it
  imposes a canonical ordering of events." With the caveat that "There is not
  necessarily an underlying objective truth as to which operation occurs
  'first'".

---
**sync L1415-1422 · BODY · CARRIED C3**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- Why no peer-to-peer, from a builder of two such systems: "We never solved
  multi-device syncronization in a way that preserved the convenience of the
  technology. Same for key backup/sync." "People won't sacrifice features for
  hypothetical improvements." And the partition rule: "You don't have to
  coordinate permissions between users because they never transact on the same
  primary records; instead you treat each user as the sole owner of their
  dataset". (Frazee, "Why isn't Bluesky a peer-to-peer network?", 2024-01-21,
  https://www.pfrazee.com/blog/why-not-p2p; the spelling is his)

---
**sync L1516-1518 · BODY · CARRIED C3**

- (10) One writer per repository; everything cross-cutting is an index. "To
  find all followers of user B requires indexing the content of all
  repositories." INSTITUTIONAL.

---
**sync L2052-2061 · BODY · CARRIED C3,C2**
*3. Section two: versioning › 3.4 Dolt, and Noms before it (Tim Sehn, Aaron Son, Andy Arth*

**2. Their reasons.** Row identity is the primary key: "Primary key values are
used to identify rows across versions for the purpose of diff and merge."
(docs, Conflicts) A point in time is named three ways: "The AS OF expression
must name a valid Dolt reference, such as a commit hash, branch name, or other
reference. Timestamp / date values are also supported." (docs, Querying
history) One writer at a time: "It is meant to be run on a single primary
server with multiple replicas for read scaling. If you need a primary bigger
than a single server, you must shard the data at the application layer."
(Sehn, "Why People Don't Use Dolt", 2024)


---
**sync L2595-2597 · BODY · CARRIED C3**
*4. Question by question › (10) Order: can two gates write one layer? What must share a*

- One sequencer per ordering domain. The domain is the natural unit of joint
  work: a file (Figma), a document (Weidner), a repository (AT Protocol), a
  session (Croquet), a whole database (Linear, Convex, Dolt). REPORTED.

---
**sync L2608-2610 · BODY · CARRIED C3**

- "they never transact on the same primary records": Frazee's rule for
  partitioning by owner. REPORTED.


