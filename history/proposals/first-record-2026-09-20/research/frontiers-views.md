# Frontiers and incremental views: how this camp would reason about the first record

Research session 7, group A. Written 2026-09-20 by Claude Fable 5.1 (max effort), for the orchestrator's first-record loop.
Sid's current leanings were withheld on purpose. Nothing here is anchored to them.

**How the work was done.** Every source below was fetched in this session as raw text (curl, pdftotext) into a scratch folder and read there. Every quote was copied from that fetched text, not from memory. Where I join a quote across a line break of a two-column PDF I say so. Reading and judgment were done in this session; nothing in this report was delegated.

**Marks.** REPORTED = they wrote or said it (quote or close paraphrase with source). INFERRED = I reconstructed it from their system or their stated principles; it is my reasoning, not their words. INSTITUTIONAL = a view I attribute to a company from a multi-author document.

**Could not open.** Nikolas Goebel's ETH master's thesis (rate-limited twice by the ETH server). The ACM Queue article by Alvaro and Tymon (HTTP 403). Talks and videos were not searched. GitHub's API rate limit stopped me from checking the author of one Materialize design doc (the 2023 transaction doc); I mark it INSTITUTIONAL.

---

## 0. The camp's shared model, in one page

These people disagree on a lot. They share one picture, and it helps to have it before the detail.

1. **A collection is a function of time.** You never have "the data". You have the data *as of a time*. Materialize's formalism: "A *time-varying collections* (TVC) is a map of partially ordered *times* to *collection versions*."
2. **What is stored is changes, each stamped with a time.** `(data, time, diff)`. The contents at time T are the sum of all changes with time ≤ T. This is an append-only log of small facts. It is very close to Sid's store.
3. **Times need not be one number.** They need a partial order with a least upper bound. A vector of per-partition positions is a legal time. So is one integer.
4. **A frontier says which times are finished.** It is a set of times such that nothing earlier than it will ever arrive again. For one integer it is one number. For a vector it is a set of vectors (an antichain). Frontiers are data that travel with the stream. They are not a guess.
5. **A consistent answer is one that is exactly right for the inputs as of some stated time.** A reader knows it got one when the frontier of what it read has passed that time. Before that, the honest answer is "not ready yet", never a partial number.
6. **Everything derived is a deterministic function of timestamped inputs.** So it can be recomputed, checked, and explained after the fact, if the inputs, their times, and the logic are kept.

Translated into Sid's words: the gate's stamp is the time. "As of" is a frontier. A running answer is a view. "What was shown" is a read at a time. "The map must not lie" is what Jamie Brandon calls internal consistency. The camp's main message for the first record is about items 3 to 5: **put one authoritative time on every fact, make completeness a recorded thing, and bind every scalar "as of" to the per-partition positions it stands for.** Facts written without that cannot get it later.

---

## 1. Frank McSherry (Naiad, timely and differential dataflow, Materialize)

### 1.1 What was built, and what was chosen

- **Naiad** (Microsoft Research, SOSP 2013, with Murray, Isaacs, Isard, Barham, Abadi). A dataflow system where every message carries a logical timestamp and the system tells operators when a timestamp is complete. REPORTED: "Edges carry records with logical timestamps that enable global progress to be measured." The timestamp had structure: an input epoch plus one loop counter per nested loop. Completion was computed from a "could-result-in" relation: "When an active pointstamp p's precursor count is zero, there is no other pointstamp in the active set that could-result-in p, and we say that p is in the frontier of active pointstamps. The scheduler may deliver any notification in the frontier." ([Naiad, 2013](https://sigops.org/s/conferences/sosp/2013/papers/p439-murray.pdf))
- **Timely dataflow and differential dataflow** (Rust, 2014 onward). Differential dataflow stores each collection as an append-only log of changes. REPORTED: "Differential dataflow transcribes evolving collections as an append-only log of changes to the collection." ([Explaining outputs in modern computations, 2016](https://github.com/frankmcsherry/blog/blob/master/posts/2016-03-27.md))
- **Materialize** (2019 onward). SQL views kept up to date over changing inputs, with one promise. REPORTED: "Every output Materialize produces corresponds exactly to the input data at some recent time. It is as if you paused the world to evaluate your query. We can tell you what that time is, or you can choose." ([Understanding Consistency in Materialize, 2024](https://github.com/frankmcsherry/blog/blob/master/posts/2024-11-25.md))

The load-bearing choice in all three: **time is explicit on every change, and the system reports which times are complete.** Everything else hangs off it.

### 1.2 The reasons, in McSherry's words

**What a timestamp is.** It is an instruction, not a measurement. REPORTED: "Virtual time is a technique for distributed systems that says events should be timestamped prescriptively rather than descriptively. The recorded time says when an event should happen, rather than when it did happen." And: "The virtual time an update is assigned becomes the truth about when that update happens. These times must reflect constraints on the input: updates in the same input transaction must be given the same virtual time, updates that are ordered in the input must be given virtual times that respect that order. Once recorded, the explicitly timestamped history is now unambiguous on matters of concurrency." ([Virtual Time for Scalable Performance, Materialize blog, 14 June 2022](https://materialize.com/blog/virtual-time-consistency-scalability/))

**Why it pays.** REPORTED, same post: "Once input data are recorded as explicit histories, the potential confusion of concurrency is largely removed. Problems of behavioral coordination are reduced to 'just computation': components must produce the correct timestamped output from their timestamped input". And: "Multiple versions are a first class citizen in Materialize's data model, rather than an internal mechanism for optimizing performance."

**Many ordered inputs, no order between them.** This is Sid's partition problem exactly. McSherry's answer is to invent one order and then write it down. REPORTED: "Materialize interleaves the independent serializations of your upstream databases into one, not unlike how you might shuffle together two decks of cards: the order within each deck stays the same, but the interleaving of the decks is up to us." And: "Materialize cannot make independent sources become mutually consistent (a very hard, perhaps ill-specified distributed systems problem), but it can place all of them on a *common timeline*." And the honest part: "It is admittedly guessing a bit, about how updates to unrelated sources interleave, but having done so there is now one view of all sources, shared by all users. Materialize resolves and locks down one source of ambiguity, so that all downstream uses can be consistent with each other, and with each source individually." (2024 consistency post)

**How the binding is recorded: reclocking.** McSherry's design doc (authorship checked in the repo's commit history: McSherry, 22 July 2021; addendum 21 April 2022). REPORTED: "'Reclocking' a stream of data is (for the purposes of this document) translating its events from one gauge of progress to another. One example is taking Kafka topics (whose records are stamped with progressing `(partition, offset)` pairs) into Materialize's system timeline (whose records are stamped with milliseconds since the unix epoch)." The stated goals include: "There is an explicit representation of the translation (rather than a behavioral description)", "The translation should be sidecar metadata, rather than a rewriting of the source data", and "The combination of durable source data and durable metadata should result in a durable reclocked stream." The mechanism: "We are going to use a `remap` collection, which is a map from a target gauge `IntoTime` to values from the source gauge `FromTime` that must themselves form an antichain." And: "Whenever `IntoTime` advances, we record the current values of `FromTime` as the new contents of `remap`." The payoff: "This is an example of how `remap`, durably recorded, introduces the basis for consistent recovery." ([Reclocking design doc](https://github.com/MaterializeInc/materialize/blob/main/doc/developer/design/20210714_reclocking.md))

The same doc names a trap that bites Sid directly, because Rama partitions can be added. REPORTED: "The use of antichains is potentially confusing for sources. For example, Kakfa's (partition, offset) pairs would be frustrating with dynamically arriving parts if the part identifiers are not both ordered and starting from small identifiers. Otherwise, recording 'these parts have not yet started' is non-trivial."

**What a consistent read is, and how a reader knows.** From the Materialize formalism doc (file introduced by McSherry 28 Feb 2022, expanded by Kyle Kingsbury in March 2022; checked in commit history). REPORTED: "At any wall-clock time a TVC is (in general) unknowable. Some of its past changes have been forgotten or were never recorded, some information has yet to arrive, and its future is unwritten." So every collection carries two frontiers: "We think of `since` as the 'read frontier': times not later than or equal to `since` cannot be correctly read. We think of `upper` as the 'write frontier': times later than or equal to `upper` may still be written to the TVC." A read at time t is correct only when t is between them. ([Formalism](https://github.com/MaterializeInc/materialize/blob/main/doc/developer/platform/formalism.md))

**Derived things lag, by rule.** REPORTED: "Each arrowhead necessarily lags the arrowheads of its immediate inputs." The asker picks where to stand, trading three things (2024 consistency post): "Responsiveness: Always choose a timestamp to the left of (before) the arrowhead of the query output." "Freshness: Always choose a timestamp to the right of (after) all input arrowheads." "Consistency: Always choose a timestamp to the right of (after) all previously chosen timestamps." While waiting, the honest display is per-input status relative to the chosen time: `ready`, `refreshing`, `pending`. McSherry: "This looks (to me) closest to what a person who wants the answer to their query wants to know".

**Freshness on demand.** REPORTED: "When you issue a command at C, Materialize can transact against the upstream primary to learn the current state of the replication log V, and then ensure that its response at R reflects at least everything through V." "It's a surprisingly simple strategy to remove replication lag: just .. wait out the lag." ([Zero-Staleness, 2024](https://github.com/frankmcsherry/blog/blob/master/posts/2024-08-13.md))

**Why weaker is not acceptable.** REPORTED: "natural eventually consistent computations can produce *unboundedly large and systematic errors*" and "you should be prepared for your results to be *never-consistent*" and "And that is one of the pain points for eventual consistency in streaming: who even knows?" ([Eventual Consistency isn't for Streaming, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-06-19.md)). Later: "Inconsistent or transiently incorrect results are unacceptable for operational work; at best you have to stall your operational plane to sort things out, and at worst you may take irrevocable incorrect actions." ([Consistency and Operational Confidence, 2023](https://github.com/frankmcsherry/blog/blob/master/posts/2023-09-19.md))

**Completeness as statements in the log.** McSherry's change-data-capture format has two kinds of record, both append-only and both safe to duplicate and reorder. REPORTED: "We will make two types of statements, each of which will be statements that are both true about the final history and can be made before that history is complete. We will make these statements only once we are certain they are and will remain true." Updates say what changed at a time. "*Progress statements* have the form `progress (time, count)` and report the number of distinct non-zero updates that occur at `time`." Result: "The statements above may be arbitrarily duplicated and reordered, and we can still recover as much of the history as is fully covered by the update and progress statements." ([Change Data Capture part 1, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-08-01.md))

**"New value for this key" is a poor change format.** REPORTED, on upserts: "at each moment in time you don't know what that prior value was" and "It seems like upsert based counting needs to maintain a copy of the collection just to interpret the changes flying at it." And why people do it anyway: "they are easier to produce, and put the burden of unpacking them on someone else." ([Upserts in Differential Dataflow, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-03-26.md))

**Explaining an answer.** Explanations are computed on demand from timestamped inputs. They are not stored on each output. REPORTED: the goal is "a subset of the input collection whose output under the computation agrees with the actual output". "when we explain records in a differential dataflow computation, we will ask for explanations *at a specific timestamp*". And the warning about recording everything: keeping every contributing input "ends up being all the edges in the connected component. Accurate, but not yet helpful." Of the two record-everything baselines: "Both of these approach end up asking for pretty much all the edges in the graph. This isn't helpful." (Explaining outputs, 2016)

**Many standing queries.** Two answers. First, share indexes across queries. REPORTED (paper abstract): "Current systems for data-parallel, incremental processing and view maintenance over high-rate streams isolate the execution of independent queries. This creates unwanted redundancy and overhead in the presence of concurrent incrementally maintained queries: each query must independently maintain the same indexed state" ([Shared Arrangements, VLDB 2020](https://www.vldb.org/pvldb/vol13/p1793-mcsherry.pdf)). McSherry's later summary: "Pre-building the arrangements (think 'indexes') results in a very low marginal cost of new queries." ([A decade in review, 2024](https://github.com/frankmcsherry/blog/blob/master/posts/2024-12-23.md)). Second, turn queries into data. REPORTED: "You just put your parameter bindings on a data bus, and the answer (and any changes) stream out the other side." "This is a high-throughput take on prepared statements, where many users can submit many concurrent parameter bindings, all on the data plane rather than control plane." ([Lateral Joins and Demand-Driven Queries, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-08-13.md))

**A gate written as a view.** In 2025 McSherry sketched transaction commit as a maintained view. REPORTED: "we'll write all transaction intents to a table, and maintain a view over the table that reports which transactions commit and which roll back." "Many transactions can be summed up by their *read sets* and *write sets*. Read sets are the values that the transaction read (or failed to read, if absent)." Cleanup is separate and later: "We can remove failed transactions, remove the read sets of committed transactions, and remove writes that are themselves overwritten without being observed". McSherry's own caveat: "This post is largely for educational purposes; please do not actually implement transactions this way without having a hard think about what you need." ([Transaction Processing in the Data Plane, 2025](https://github.com/frankmcsherry/blog/blob/master/posts/2025-04-27.md))

**Wall-clock "now" inside a standing answer.** REPORTED: "The 'art' to using `mz_now()` comes down to finding the fewer, discrete moments where you would like your maintained view to change, and re-framing your logic to reveal this." A rule like "older than one day" must be written as a validity bound on each record, so the system knows the future moment each record flips. ([Programming with time in Materialize, 2025](https://github.com/frankmcsherry/blog/blob/master/posts/2025-09-22.md))

### 1.3 What changed over the years (worth more than the papers)

McSherry does not write regrets as regrets. The changes are visible in the record.

- **Naiad's system-run notifications became capabilities.** The unpublished Timestamp Tokens paper (Lattuada and McSherry, [arXiv 2022](https://arxiv.org/pdf/2210.06113)) says earlier interfaces "require a deeper involvement of the system itself: continually invoking operators in Flink and sequencing notifications in Naiad", and proposes tokens instead: holding a token for a time is the right to still write at that time. REPORTED from McSherry: "The Timestamp Tokens paper never got published." and "Of all the papers, this is the one closest to explaining what timely dataflow contributes." (Decade in review)
- **Virtual time came from live reconfiguration.** REPORTED: "Moments where you would otherwise need to pause the system can be handled by 'virtually' cutting over from one configuration to another using the built in timestamps. This would be a foundational insight that led to the use of virtual time as the coordinating principle at Materialize." (Decade in review, on the Megaphone paper)
- **Materialize grew a durable layer it did not start with.** The 2021 reclocking doc exists because sources were not replayable: its goal B is "Describing durable state that can make sources like Kafka, Postgres, and files exactly replayable in the system timeline." The 2022 "unbundled" architecture split Storage, Compute, Adapter, joined only by times.
- **Independent per-shard frontiers were not enough for writes across shards.** The 2023 transaction design (INSTITUTIONAL; author not verified) added one coordinating log: "Efficient atomic multi-shard writes are accomplished through a new singleton-per-environment _txns shard_". "As time progresses, the upper of every data shard is logically (but not physically) advanced en masse with a single write to the txns shard." The costs are stated plainly: "All txn writes are linearized through the txns shard, so there is some limit to horizontal and geographical scale out." and "Latency on contended workloads will likely be quite bad. At a high level, if N txns are run concurrently, 1 will commit and N-1 will have to (usually cheaply) retry." ([Txn management design](https://github.com/MaterializeInc/materialize/blob/main/doc/developer/design/20230705_v2_txn_management.md))
- **Upserts had to be supported anyway**, with a stateful operator that holds the whole keyed collection, because "many folks show up with only upserts".
- **On living up to it.** REPORTED, about publishing Materialize's product principles: "The work slowed down as I realized that we didn't always live up to these principles, and I wanted to fix that before smugly announcing how great we were." And: "No progress happens without clarity around your limitations and shortcomings." (Decade in review)

### 1.4 Which questions McSherry speaks to

- **(10) as-of: one number or a position per partition?** Both, bound by a recorded map. REPORTED: the `remap` collection. The asker sees one number. The store durably keeps, for each such number, the per-partition positions it stands for. The interleaving across partitions is invented once and then locked, "so that all downstream uses can be consistent". INFERRED for Sid: the first record should already live on one store-wide timeline, and the map from that timeline to depot partition positions should itself be facts. Facts admitted before such a map exists can never be given an exact "as of".
- **(10) can two gates write one layer?** INFERRED from the virtual-time post and the 2023 txn design: yes, if one cheap authority assigns times and the gates only do the work. "Adapter scales largely by avoiding substantial work on the critical path of timestamp assignment." The part that must be single is time assignment, not checking.
- **(10) what must share a partition?** INFERRED: only what must share a *time source*. Order across partitions comes from the timeline, not from co-location. REPORTED constraint: updates of one transaction get one time; ordered inputs get ordered times.
- **(11) when: whose clock, used for order?** REPORTED: the ingest point's clock, and it *is* the order ("becomes the truth about when that update happens"). Materialize uses milliseconds since the epoch as the value, but an oracle hands them out, so they never go backward and ties are decided. INFERRED for Sid: if the gate's `when` is not the order, the camp would ask what it is for. A second, ordering time next to a decorative wall-clock time is two sources of truth.
- **(5) index lag written down?** REPORTED via `since` and `upper`: an index is a collection with its own `upper`. A read at a time at or past `upper` is not a correct read. So the rule is: choose the read time inside the index's frontier, or wait. Then the recorded "as of" *is* how far the index had got. INFERRED: a separate "lag" field is only needed if readers are allowed to read past the frontier, and the camp would forbid that outright.
- **(4) follow the latest or stay on the version read?** REPORTED basis: explanations are always "at a specific timestamp". INFERRED: a recorded read is always pinned. "Following" is a view computed over pinned reads plus newer versions. That view is what Sid calls staleness.
- **(4)/(11) which reads, and do they all count?** REPORTED: full provenance is "Accurate, but not yet helpful"; useful explanations are minimal and computed on demand. REPORTED (2025): a transaction's read set includes what it "failed to read, if absent". INFERRED: for anything deterministic, record times and logic, not per-output reads. For anything not re-derivable, record the read set, including reads that came back empty.
- **(6) keep "replacing 25" on 37?** INFERRED from the upsert critique: yes. A landing that says only "new value" forces every matcher to hold prior state. A landing that names what it retracts can be processed without state.
- **(7) keep the gate's yes/no and refusals?** REPORTED sketch: intents all land; the verdict is a view over them; failed intents can be tidied later. INFERRED: keep offers and verdicts in a collection that may have its own, faster-moving `since`.
- **(0) never rewritten, or never lost?** REPORTED: Materialize compacts. "Compaction advances the `since` frontier". What is promised is correct reads between `since` and `upper`, and a refusal below `since`. INFERRED: the camp's invariant is "never answer wrongly", not "never rewrite". If Sid ever trims, a `since` per collection keeps the map honest.
- **(9) delete.** INFERRED from the formalism: a delete is a retraction at a time. When `since` passes that time, compaction cancels the +1 and −1 and the data is physically gone. Erasure and "as of, forever" cannot both hold for the same fact. One must give.
- **(12) runtime version, rebuilds as facts?** REPORTED basis: Megaphone cut-overs happen at a timestamp. INFERRED: a rebuild is an event on the timeline. "As of T, runtime R" should be readable from the store.
- **(14) the hand.** INFERRED from lateral joins: what a person is looking at is an input collection that drives which answers are maintained. It is data. It is also high-rate and short-lived, so it wants aggressive compaction.
- **Standing-pattern cost.** REPORTED: share arrangements; make patterns data and join landings against them in one dataflow. INFERRED: "millions of standing patterns" should be one indexed collection of patterns, not millions of subscriptions.

### 1.5 What resembles Sid's situation, and what differs

Resembles: an append-only log of small timestamped changes is the substance. Derived things are never the truth. Many partitioned, independently ordered inputs. A hard line against showing states that never existed.

Differs: Materialize's inputs are other systems' transactions; Sid's are offers from people and agents, and the gate itself decides. Materialize compacts history on purpose; Sid forbids rewriting. Materialize's views are SQL over relations; Sid's tools are opaque bodies behind pattern signatures, so determinism is a promise, not a property. Materialize serves one tenant at a time per environment; Sid wants one store for the planet. Materialize holds no per-fact provenance; Sid wants it on every fact.

### 1.6 Disagreements

With Noria and with Flink and Kafka Streams, on eventual consistency for computed views (REPORTED above; McSherry grants Noria's case: "systems like Noria that target keyed look-ups for maintained views, for which you might reasonably expect updates to cease for the records that influence your query results"). With the DBSP authors, mildly, on whether times must be partially ordered (section 4). With the "big data" systems crowd on scale: "many systems have either a surprisingly large COST, often hundreds of cores, or simply underperform one thread for all of their reported configurations." ([Scalability! But at what COST?, HotOS 2015](https://www.usenix.org/system/files/conference/hotos15/hotos15-paper-mcsherry.pdf)). INFERRED: McSherry would ask for the single-thread number before accepting "thousands of landings a second" as a scale problem.

---

## 2. Jamie Brandon (internal consistency; the person at the screen)

### 2.1 What was built, and what was chosen

Brandon worked on Eve, then at Materialize, then wrote dida, a small re-implementation of differential dataflow meant to be understood. The piece that matters here is a test: the same bank-transfer workload run through Flink, Kafka Streams/ksqlDB, and differential dataflow/Materialize, checking whether the running total (which must always be zero) ever shows a non-zero value. Flink and ksqlDB showed impossible totals. Differential dataflow did not. ([Internal consistency in streaming systems, 17 April 2021](https://www.scattered-thoughts.net/writing/internal-consistency-in-streaming-systems/); Brandon's disclaimer: "I used to work for Materialize which is one of the systems being compared in this post.")

### 2.2 The reasons, in Brandon's words

**The definition.** REPORTED: "A system is internally consistent if every output is the correct output for some subset of the inputs provided so far." Against: "A system is eventually consistent if when we stop providing new inputs it eventually produces the correct output for the set of inputs provided so far."

**Why database intuition fails here.** REPORTED: "An eventually consistent key-value database might show you old values. An eventually consistent streaming system might show you outputs that are completely impossible for any set of inputs, and might never converge to a correct value as long as new inputs keep arriving."

**The four ways to lie** (REPORTED, section headings and text):
1. "Combining streams without synchronization": "if the join between credits and debits is not synchronized then we could be calculating the balance using the current value of credits and the past value of debits, effectively creating money."
2. "Early emission from non-monotonic operators": an aggregate that emits after each of four related changes is wrong three times out of four. "To be internally consistent, total needs to wait until it's seen all the updates that correspond to the original transaction before emitting an output."
3. "Confusing changes with corrections": "this stream does not distinguish between changes to the correct value and corrections of intermediate outputs."
4. "Inconsistent rejections in watermarks": "To be internally consistent, a streaming system can only reject inputs at the edge, so that all operators are working on data derived from the same set of upstream inputs."

**Why it matters to someone acting on a screen.** REPORTED: "at the edge of the system it's not possible to take action on outputs if you don't know which ones are correct. You might email a warning to a customer for going overdrawn on their account, only to later find that this was just a transient inconsistency." And the fix: "To be internally consistent, there needs to be some way to determine when an output value is correct. This could be in the form of eg progress statements which tell you that the output for a given timestamp will no longer change."

**What it costs the person who must reason about it.** REPORTED: "Internally inconsistent systems require the user to reason about all the possible interleavings of stream events. Internally consistent systems allow the user to pretend they are just operating a very fast batch system." ([An opinionated map of incremental and streaming systems, 2021](https://www.scattered-thoughts.net/writing/an-opinionated-map-of-incremental-and-streaming-systems/)). And: "The effect is similar to undefined behavior in programming languages."

**What it costs the machine.** REPORTED: "I don't expect that guaranteeing internal consistency will have much impact on throughput or horizontal scaling. The amount of metadata that must be tracked is very small and can be amortized over large batches of updates. There is, however, a strong tradeoff between correctness and latency."

**The model that makes it possible.** REPORTED: "Doing this correctly typically requires having an explicit model of time which is tracked alongside incoming data to allow the system to reason about which versions of various intermediate results go together and when it is safe to produce an output." In dida's design notes: "To guarantee that the results are consistent we add timestamps, multiversion indexes and frontiers." On what a timestamp is: "These timestamps could be actual real world timestamps (eg unix epochs) or they could just be arbitrary integers that we increment every time we make a new change. Their job is just to keep track of which output changes were caused by which input changes." On readers: "Frontiers are also useful at the output of the system - downstream consumers can watch the frontier to learn when the have seen all the changes to the output for a given timestamp and can now safely act on the result." ([dida, docs/why.md](https://github.com/jamii/dida/blob/main/docs/why.md))

**Dependency tracking at the wrong grain.** Brandon's map splits incremental systems into unstructured (each node is one value, any edge) and structured (each node is a collection, edges are a fixed set of operators). REPORTED: "unstructured systems tend to struggle with controlling the granularity of incremental computation. Make the values in each node too big and you do a lot of unnecessary recomputation. Make them too small and the overhead of the graph metadata dominates the actual computation."

**Eager or lazy.** REPORTED: "lazy systems can't provide notifications when a certain output appears or changes because they won't even know about the output until asked to calculate it." and "people tend to use eager systems and approximate laziness where needed by either running ad-hoc queries against the output or by adding a list of outputs-to-maintain into the inputs."

**State belongs in a database with a log.** An early piece, and the one place this camp speaks for Sid's premise. REPORTED: the questions that matter are "When did this state change?", "What caused it to change?", "Did this invariant ever break?", "How did this output get here?", and "I propose that if we were to manage state more like a database and less like a traditional imperative language then understanding and debugging programs would become easier." ([Local state is harmful, 2014](https://www.scattered-thoughts.net/writing/local-state-is-harmful/))

### 2.3 What changed or was regretted

- **dida is marked for replacement by something simpler.** On Brandon's home page, under dida: "Roughly works, but definitely not production-ready. Likely to be replaced by something simpler (eg)." The "eg" links to the DBSP paper (checked in the page's HTML). So Brandon, having rebuilt differential dataflow by hand, points at the totally ordered model as the one to keep.
- **Testing was the gap, even at Materialize.** REPORTED: "the vast majority of the tests I've looked at only test the final output after everything has settled. Even materialize, which is otherwise pretty heavily tested, does not have many tests which examine intermediate outputs." The advice: "figuring out some application-specific invariants that you can monitor in production to at least put a lower bound on the error rate."
- **Failures were easier to cause than expected.** REPORTED: "not only are these failures easier to trigger than I expected, the resulting behavior is much more complex than I expected and that I can't currently predict or explain the dynamics."
- **What Brandon would design from scratch.** REPORTED: "If I was designing a system from scratch I'd be tempted to allow multiple waves of watermarks from the beginning", so that early outputs and late inputs are possible "without risking internal inconsistency".
- **Why differential dataflow did not spread.** Brandon asked users. REPORTED complaints: "where is all the state?", "which operators are internally stateful? how much memory will this use?", "how to get data out, especially how to pull results instead of pushing them". ([Why isn't differential dataflow more popular?, 2021](https://www.scattered-thoughts.net/writing/why-isnt-differential-dataflow-more-popular/))

### 2.4 Which questions Brandon speaks to

- **"The map must not lie" is internal consistency.** INFERRED but close to a restatement: Sid's principle is Brandon's definition with a display attached. Brandon adds the sharper half: stale is the *mild* failure. The bad one is an output that was never true for any input.
- **Sid's system, as described, is open to all four failures.** INFERRED. Nobody routes; tools fire on landings; tools read through indexes that lag; one landing can start several chains of work. A running answer built from two reads at different index positions is failure 1. A tool that fires on the first of several related landings is failure 2. A crossing record that cannot say "this replaced a partial answer" versus "the world changed" is failure 3. Failure 4 is already avoided: the gate is the only place that rejects.
- **(5) index lag.** REPORTED principle: a reader needs "some way to determine when an output value is correct". INFERRED: yes, write down how far the index had got, and better, do not answer past it.
- **(11)/(4) reads on every fact.** INFERRED from the unstructured-versus-structured warning: per-fact read lists are the unstructured road, where "the overhead of the graph metadata dominates". Brandon would record reads at pattern grain with a time, not one entry per matched fact.
- **(10) one number or many?** INFERRED: Brandon's pointer to DBSP says: take one number if you possibly can. Partial orders are the cost of loops and of inputs that advance separately. Pay it only where forced.
- **(14) shown versus looked.** INFERRED from "confusing changes with corrections": a "what was shown" record should say whether the answer shown was complete for its as-of. A partial answer that was shown is a different fact from a complete one.
- **(12) rebuilds.** INFERRED from the testing section: if the semantics are "compare the outputs at each timestamp to your favourite batch system", then the runtime version is part of what makes a past output checkable.
- **Standing patterns.** REPORTED: make the list of wanted outputs an input.

### 2.5 Resemblance and difference

Resembles: Brandon cares about a person looking at output and acting on it, which is Sid's crossing. Brandon comes from Eve, where programs reacted to facts in one store. Differs: Brandon's tests are on SQL-style aggregates over money. Sid's running answers may be model-shaped and not checkable against a batch oracle. Brandon never had to keep provenance per fact.

### 2.6 Disagreements

With the Flink and Kafka Streams designers, on whether eventual consistency tells you anything: "eventual consistency alone does not provide any useful constraints." With the coordination-avoidance line (section 3), in spirit: "google seems to be moving everything to being backed by spanner, because reasoning about weaker consistency models at scale is just too difficult and too expensive." With McSherry, only on complexity: simpler time if possible.

---

## 3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes, lineage-driven fault injection, Hydro)

### 3.1 What was built, and what was chosen

A fifteen-year line at Berkeley (Alvaro now at UC Santa Cruz). Dedalus: Datalog where time is an ordinary column. Bloom: a language built on it. CALM: the result that says exactly when coordination is needed. Blazes: a tool that finds where a dataflow needs coordination and adds the cheapest kind. Molly: lineage-driven fault injection, later run at Netflix. Hydro: the current attempt. The choice that matters: **treat time and order as data in the logic, and coordinate only where the logic is non-monotone.**

### 3.2 The reasons, in their words

**The CALM theorem.** REPORTED: "A program P is monotonic if for any input sets S,T where S ⊆ T, P(S) ⊆ P(T)." "Theorem 1. Consistency As Logical Monotonicity (CALM). A program has a consistent, coordination-free distributed implementation if and only if it is monotonic." And the intuition: "monotonic programs are 'safe' in the face of missing information, and can proceed without coordination. Non-monotonic programs, by contrast, must be concerned that truth of a property could change in the face of new information. Therefore they cannot proceed until they know all information has arrived, requiring them to coordinate." ([Keeping CALM, Hellerstein and Alvaro, CACM 2020; arXiv](https://arxiv.org/pdf/1901.01930))

**Overwrite is the non-monotone act.** REPORTED: "Bare assignment [10] is a nonmonotonic programming construct: outputs based on a prefix of assignments may have to be retracted when new assignments come in." Immutability is the monotone pattern: "an immutable variable is a simple monotonic pattern: it transitions from being undefined to its final value, and never goes back." On deletion: "Instead of explicitly allowing deletion (a non-monotonic construct), tombstones masked immutable values with corresponding immutable tombstone values. Taken together, a data item with tombstone monotonically transitions from undefined, to a defined value, and ultimately to tombstoned."

**A manifest makes a non-monotone step safe.** This is the closest thing in the literature to Sid's "based on". REPORTED, about the shopping cart: "the checkout operation is enhanced with a manifest from the client of all its update message IDs that preceded the checkout message: replicas can delay processing of the checkout message until they have processed all updates in the manifest." The lesson drawn: "Rather than micro-optimize protocols to protect race conditions in procedural code, modern distributed systems creativity often involves minimizing the use of such protocols."

**Reads are where the danger is.** REPORTED: "Yet CRDT guarantees extend only to data updates; observations of CRDT state are unconstrained and unsafe." The example is named "The Potato and the Ferrari, a.k.a. Early Read": a cart is two growing sets, added and removed; the contents are A − R; checkout reads before the removal of the Ferrari arrives. "This truly expensive consistency bug arises when the query 'reads' the state of the 2P-Set 'too early'". Monotone threshold queries are safe to read locally. Set difference is not. "So what are developers to do when they need one of these nonmonotone queries? The simple and safe solution is to coordinate!" ([Keep CALM and CRDT On, Laddad, Power, Milano, Cheung, Crooks, Hellerstein, VLDB 2022](https://www.vldb.org/pvldb/vol16/p856-power.pdf))

**Two ways to coordinate: sequence, or seal.** REPORTED, Blazes: "Two extreme approaches include (a) establishing a single total order in which all instances of a given component receive messages (a sequencing strategy) and (b) disallowing components from producing outputs until all of their inputs have arrived (a sealing strategy)." Sealing "indicates when partitions of a stream have stopped changing." And: "Note that sealing is significantly less constrained than ordering: it enforces an output barrier per partition, but allows asynchrony both in the arrival of a batch's inputs and in interleaving across batches." ([Blazes, Alvaro, Conway, Hellerstein, Maier, ICDE 2014; arXiv](https://arxiv.org/pdf/1309.3324))

**Time as data.** REPORTED, Dedalus abstract: earlier languages pushed state and delay outside the logic, which "forces programmers to think operationally. We argue that the missing component from these previous languages is a notion of time." Every fact carries a time column. A fact persists only because a rule carries it to the next moment. A message's arrival time is chosen by the receiver's world, not the sender's: each asynchronous head "will take some unspecified time suffix value". The sender's time may ride along as plain data. They call this entanglement: "recording a binding of both the time value of the deduction and the time value of its consequence." "It allows a rule to reference the logical clock time of the deduction that produced one (or more) of its subgoals". ([Dedalus: Datalog in Time and Space, Alvaro, Marczak, Conway, Hellerstein, Maier, Sears; tech report 2009](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2009/EECS-2009-173.pdf))

**What time is for.** REPORTED, Hellerstein: "I call this the 'Fateful Time' conjecture because it argues that the inherent purpose of time is to seal fate." And the CRON conjecture: "Program semantics require causal message ordering if and only if the messages participate in non-monotonic derivations." "Time does matter, exactly in those cases where ignoring it would result in logically ambiguous fate". ([The Declarative Imperative, SIGMOD Record 2010; tech report](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2010/EECS-2010-90.pdf))

**Lineage as a working tool.** REPORTED: "A lineage-driven fault injector reasons backwards from correct system outcomes to determine whether failures in the execution could have prevented the outcome." The programmer's job is "to use data lineage to reason about the redundancy of support (or lack thereof)". An outcome can have several independent supports. It is in danger only if every one of them can be knocked out. Molly turns lineage into a formula and asks a solver. ([Lineage-driven Fault Injection, Alvaro, Rosen, Hellerstein, SIGMOD 2015](https://people.ucsc.edu/~palvaro/molly.pdf))

### 3.3 What changed or was regretted

- **The languages were not adopted.** REPORTED: "Declarative programming environments for distributed computing have emerged in academia and industry over the past decade [11, 41, 65], but adoption of these 'revolutionary' approaches has been limited. Moving forward, we advocate an evolutionary Lift and Support approach: given a program specification written in a familiar style, automatically lift as much as possible to a higher-level declarative Intermediate Representation (IR) used by the compiler, and encapsulate what remains in UDFs". ([New Directions in Cloud Programming, Cheung, Crooks, Hellerstein, Milano, CIDR 2021](https://www.cidrdb.org/cidr2021/papers/cidr2021_paper16.pdf)). Hydro keeps Bloom's tick: "Each iteration ('tick') of the loop uses the developer's program specification to compute new results from the snapshot, and atomically updates state at the end of the tick."
- **Fine lineage did not survive contact with production.** At Netflix, REPORTED: "Rule-based languages such as Dedalus also make it trivial to collect fine-grained data lineage during execution." But: "this requirement was not acceptable at Netflix. First, there were simply too many applications to port to Dedalus." So: "we had to look for another source of lineage data beyond the program text." They used request traces: "a call graph characterizes how a collection of services contributed to a system outcome, while LDFI's data lineage characterized how individual data elements and fine-grained computation steps contributed to an outcome. Because we were willing to sacrifice some precision in order to improve the performance of automated failure testing, it seemed that call graphs could stand in for lineage" (a figure interrupts this sentence in the PDF text). ([Automating Failure Testing Research at Internet Scale, Alvaro, Andrus, Sanden, Rosenthal, Basiri, Hochstein, SoCC 2016](https://people.ucsc.edu/~palvaro/socc16.pdf))
- **CRDTs were found wanting on the read side** by the same people who had praised the pattern (2020 paper praises it; 2022 paper: "unconstrained and unsafe").
- **Consistency became a per-handler choice.** In Hydro the consistency "facet" is declared per endpoint, with "eventual" as the default.

### 3.4 Which questions they speak to

- **Where Sid's store is monotone, and where it stops.** INFERRED straight from CALM: appending facts is monotone. Four things are not. "Current" (no later version exists). "Nearest layer wins" (no nearer row exists). The gate's version check. The gate's policy check, if policy can be withdrawn. Each is a statement about absence. By the theorem each needs either coordination or a seal. The gate is coordination placed exactly at the version check, which is right. The other three are *reads*, and the design as described gives them nothing.
- **(10) can two gates write one layer?** INFERRED from CALM and Blazes: for keys whose values only accumulate (set-like), yes, with no coordination. For keys with one current value (register-like), all offers for a cell must pass through one sequencer. So whether a key is a growing set or a register is a property the grammar fact could state. A gate that runs compare-and-set on every cell of every key coordinates more than the logic needs.
- **(10) what must share a partition?** INFERRED from Blazes: whatever one non-monotone decision reads. A per-partition seal is cheaper than a global order, so put a cell, and what the gate must check against it, under one seal.
- **(11) whose clock, used for order?** REPORTED principles: the receiver assigns the time; the sender's time is data; order matters only for non-monotone steps. INFERRED: stamped by the gate, used for order exactly at supersession and absence checks, and not needed anywhere else.
- **(11)/(4) based-on.** REPORTED: the manifest pattern. INFERRED: "based on" is a manifest. A reader of fact F can refuse to act until it holds everything F stood on. That turns based-on from an audit trail into a safety mechanism. REPORTED (entanglement): carrying the premise's time inside the consequence is a first-class idea, which is what "a read of one fact at its version" is.
- **(4) depends-on versus how-I-got-here.** INFERRED from LDFI: a flat list of reads cannot say "either of these would have done". Lineage that separates alternative supports lets doubt be computed properly: a fact is in doubt only when every support has a doubtful member. Without that split, doubt spreads too far, and a map that overstates doubt also lies.
- **(5) index lag.** REPORTED: the early read. INFERRED: a pattern read over a lagging index is the Potato and the Ferrari. Record the seal it read under, or wait for one.
- **(9) delete.** REPORTED: tombstones keep deletion monotone. INFERRED: they hide a value; they do not remove it, so they do not answer erasure.
- **(11) because-of.** INFERRED from Dedalus: base facts have no derivation and derived facts always have one. "Empty only when it starts a chain" is the same split.
- **(7) refusals.** INFERRED from LDFI: a refusal is lineage for something that did *not* happen. McSherry makes the same point from the other side: explanations must cover "the *absence* of output records".
- **Tools as facts, with opaque bodies.** REPORTED lesson from Netflix: lineage is free when the program is rules, and coarse when it is opaque code. INFERRED: Sid's tool signatures are declarative, so reads made by matching are free to record. Reads made inside a body can be captured only if the runtime is the body's only way to read.

### 3.5 Resemblance and difference

Resembles: facts with time as a column; rules that fire on facts; lineage as the way to explain; a sharp line between re-derivable and not. Bloom and Dedalus are the nearest academic relatives of "tools are facts, matched not routed". Differs: they aimed to *avoid* a single writer; Sid chose one. They could assume programs written as rules; Sid's tool bodies are opaque and some are models. Their lineage was for testing a protocol, not for keeping forever on every fact.

### 3.6 Disagreements

With the position that strong consistency should simply be used everywhere, on cost: the CACM paper opens with coordination as the dominant cost. With CRDT advocates, on reads. With their own earlier bet on new languages. With McSherry and Brandon not on substance but on emphasis: Berkeley asks where coordination can be skipped. Materialize asks how to make one timeline cheap.

---

## 4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff; McSherry is a co-author of the paper)

### 4.1 What was built, and what was chosen

A theory of incremental view maintenance modelled on signal processing, and a company running it. The choice: **time is one counter, and inputs arrive in time order.**

### 4.2 Reasons, in their words

REPORTED: "Time is not the wall-clock time, but essentially a counter of the sequence of transactions applied to the database. Since transactions are linearizable, they have a total order, which defines a linear time t dimension". On origins: "DBSP is inspired from Differential Dataflow [28] (DD), and started as an attempt to provide a simpler formalization of DD". On the difference (joined across line breaks of the PDF's related-work section): "DD's computational model is more powerful than DBSP, since it allows past values in a stream to be 'updated'. In contrast, our model assumes that the inputs of a computation arrive in the time order while allowing for nested time domains via the modular lifting transformer." "in essence DBSP is 'deconstructing' DD into simple component building blocks". All operators are "synchronous": "they consume and produce data at the same 'rate'." ([DBSP, Budiu, Chajed, McSherry, Ryzhyk, Tannen, VLDB 2023; arXiv](https://arxiv.org/pdf/2203.16684))

**What must be recorded to recompute.** Feldera's fault-tolerance write-up is the most concrete statement in this whole group. REPORTED: "For each batch of data that Feldera processes through the pipeline, it logs enough information to obtain another copy of the batch's input data later." "The Kafka input adapter logs per-partition event offsets within each input topic, the HTTP GET input adapter logs byte offsets within a URL, and so on. There is no way to re-read data that arrives through the HTTP input connector or through ad-hoc queries, so in those cases, Feldera logs, and replays, all of the input data." "Feldera uses a checksum included in the log to ensure that the data read for replay is the same as the original data." ([How Feldera Fault Tolerance Works, Ben Pfaff, 9 Dec 2024](https://www.feldera.com/blog/fault-tolerance-technical-details))

**What "re-derivable" quietly depends on.** REPORTED, same post: "the replay process requires computation in a pipeline to be reproducible, meaning that, given the same input and the same pipeline state, the pipeline always produces the same output." Exceptions: "queries that use SQL's NOW function, which returns the current time; this can be made reproducible by logging the time that was originally used and using that time in the replay. We also need to avoid other reasons that results can be non-reproducible, such as floating-point results that differ across processor architectures (by replaying on the same architecture) or across compiler optimizations (by using the same compiler and optimizations)."

**Why the batch boundaries themselves are recorded.** REPORTED: replaying the same input cut differently "would defeat Feldera's synchronous streaming guarantee, which says that Feldera produces exactly one output change for each input change".

### 4.3 What changed or was learned

- The whole project is a change of mind about differential dataflow: keep the algebra, drop partially ordered time and updates to the past.
- REPORTED, Ryzhyk, 12 Sept 2025: "before an IVM engine can handle real-time updates, it must first ingest and process the entire historical dataset. In other words, a good IVM engine also needs to be a capable batch engine — and today's systems aren't." ([The Dirty Secret of IVM](https://www.feldera.com/blog/backfill-explained))
- REPORTED, Ryzhyk: "There's no such thing as a streaming-only workload." ([Let's make streaming analytics boring](https://www.feldera.com/blog/lets-make-streaming-boring))

### 4.4 Which questions they speak to

- **(10) one number.** REPORTED: one counter. And the same pattern as reclocking: the scalar step is bound, in a durable log, to per-partition offsets. Two teams arrived at it separately. That is the strongest single signal in this report.
- **(12) runtime version and machine kind.** REPORTED: reproducibility depends on processor architecture and on compiler and optimization settings, and "now" must be logged. INFERRED for Sid: "deterministically re-derivable from the same reads" is true only relative to tool version, runtime version, and machine kind. If those are not written at session start, a crossing record from years ago cannot be re-derived to the bit, only approximately. With hundreds of rebuilds planned, this is a first-record item.
- **(11) based-on for ingests.** REPORTED: if a source can be re-read, log where to re-read it and a checksum; if it cannot, log the data itself. INFERRED: Sid's third kind of read (an anchor to a git commit or a paper) wants a content checksum beside the pointer, or the anchor cannot be checked later.
- **Tools registered late.** INFERRED from the backfill post: "match landings to tools" is the incremental half of a standing query. A tool registered today is a query whose answer is defined over every earlier fact as well. Does a new tool see old facts? That is backfill, and it is a decision the design has to make on purpose.
- **Seeding ten million papers** is backfill too. INFERRED: the store needs a bulk path that keeps the same semantics as one-at-a-time landings.

### 4.5 Resemblance and difference

Resembles: one linear admission order is natural for a single gate. Differs: DBSP assumes inputs arrive in time order through one door. Sid's depots are partitioned with no order across partitions, so the single counter has to be made (section 1, reclocking), not assumed.

### 4.6 Disagreements

With differential dataflow on partial orders. Brandon sides with DBSP.

---

## 5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf)

### 5.1 What was built, and what was chosen

Noria (MIT, OSDI 2018): materialized views as a dataflow that also serves reads, for web applications. Two choices. **Eventual consistency, on purpose.** And **partial state**: a view may have holes; a miss sends an "upquery" backward through the dataflow to fill the hole; cold entries are evicted.

### 5.2 Reasons, in their words

REPORTED, Gjengset's thesis: "To achieve high parallel processing performance, Noria's dataflow avoids global progress tracking or coordination. An update injected at a base table takes time to propagate through the dataflow, and the update may appear in different views at different times." Candidly: "Eventual consistency is an inherently vague consistency model — an eventually consistent system may return incorrect results as long as it eventually returns the right result." The defence: "Noria reads are generally just stale." The design reason: "By design, Noria's read and write paths are disconnected from one another: reads can usually proceed even if the write path is busy. This is both the reason why Noria's read performance is so high, and why it gives weaker consistency guarantees that competing systems." And the escape hatch: "application developers direct those queries where strong consistency is necessary to other, better suited systems." ([Partial State in Dataflow-Based Materialized Views, PhD thesis, MIT 2020](https://jon.thesquareplanet.com/papers/phd-thesis.pdf))

What is kept and what is recomputed: base tables are the truth. Every view entry can be dropped and rebuilt. Determinism is required: "Noria requires that operators are deterministic functions over their own state and the inputs from their ancestors."

### 5.3 What changed afterwards

- REPORTED, thesis section 8.3.3: "adding partial state to a system with stronger consistency guarantees should not require extensive changes. In fact, parts of the design could likely be simplified". So weak consistency was not essential to partial state. It was a simplification for read speed.
- Schwarzkopf's group at Brown later worked on read-your-writes for their dataflow (Sc.M. thesis, Ishan Sharma, May 2022; I read only its front matter and mechanism keywords: per-client tickets of recent writes, reads that block until the views catch up). INFERRED: they came back for the guarantee Noria left out.
- The commercial successor ReadySet still describes itself as eventually consistent (seen in a search result snippet from its docs; I did not open the page).
- **Multiverse databases** (HotOS 2019). REPORTED: "The database applies policies for each user, filtering and transforming the base data to form a user-specific 'parallel universe' database that contains only data that the user is allowed to see". The point is a small trusted base: "multiverse databases limit the TCB to the privacy policies and the database code enforcing them". The cost named: "Multiverse databases' per-user transformations risk expensive queries if applied dynamically on reads, or impractical storage requirements if the database proactively materializes policy-compliant views." The design: "a joint dataflow across 'universes' that combines global, shared computation and cached state with individual, per-user processing and state." "Our early prototype supports thousands of parallel universes on a single server." Writes go to the base only: "applications cannot write to user universes directly." ([Towards Multiverse Databases, Marzoev, Araújo, Schwarzkopf, and others](https://people.csail.mit.edu/malte/pub/papers/2019-hotos-multiversedb.pdf))
- **Erasure by construction** (Poly 2019 position paper, then K9db, OSDI 2023). REPORTED: "Our key idea is for each user to have her own, structured shard of the storage backend". "a user shard never contains information related to other users, or derived information that combines multiple users' data." Everything combined is a view over shards. "if she demands erasure of her data, the service deletes the shard and streams revocation messages that remove derived data." ([Position: GDPR Compliance by Construction](https://people.csail.mit.edu/malte/pub/papers/2019-poly-gdpr.pdf)). K9db made it real: "Each user's µDB contains the data they own, and is encrypted with a user-specific key." And on backups: "encrypting data at rest and deleting encryption keys (referred to as 'crypto-shredding'), e.g., to make backups inaccessible, is widely considered a compliant approach". K9db also guards against "data without an owner being left behind in the database." ([K9db, OSDI 2023](https://www.usenix.org/system/files/osdi23-albab.pdf))

### 5.4 Which questions they speak to

- **Running answers never stored.** REPORTED: Noria's partial state is this idea with engineering. Keep only what is being read, fill holes on demand, evict the rest. INFERRED: "never stored" should mean "never the truth", not "never held". Recomputing from the log each time costs the whole history.
- **(16) who sees a fact before permissions exist?** INFERRED from multiverse: nobody but the trusted base. A universe holds only what a policy lets in. No policy, no entry.
- **Layers.** INFERRED: Sid's layers and multiverse universes look alike and are opposite in one way. Universes are read views derived from one shared base; writes go to the base. Sid's layers are places that are written. Multiverse's cost warning still applies to anything computed per person.
- **(9) delete, backups included.** REPORTED: per-owner physical unit, per-owner key, delete the unit or the key, revoke derived data through the dataflow. INFERRED for Sid: a person's durable layer is the natural unit of ownership. If every fact in that layer is encrypted under that person's key from the first record, then erasure, backups included, is one key deletion, and the log is never rewritten. This cannot be added later: facts written in the clear stay in the clear in every backup. Their second point also transfers: a fact with no owner is a compliance bug. "By whom" and "layer" together must always resolve to an owner.
- **(9) who read the erased thing.** INFERRED: their "revocation messages that remove derived data" is Sid's walk over based-on. For running answers it is automatic. For stored offers that stood on the erased fact (a model's summary of it), based-on finds them, and a policy must say what happens to them.

### 5.5 Resemblance and difference

Resembles: many users, per-user views over a shared base, one system serving reads at interactive speed, erasure as a design input. Differs: Noria chose to let the map be briefly wrong, which Sid rules out. Noria's base tables are mutable SQL tables.

### 5.6 Disagreements

With McSherry and Brandon, head on, about eventual consistency for computed views. Gjengset's position is that for keyed web reads it is fine and buys read throughput. McSherry concedes that case and no further.

---

## 6. Nikolas Goebel (3DF, declarative differential dataflow)

### 6.1 What was built

3DF: a query engine on differential dataflow over a Datomic-style fact model, taking new standing queries at run time, started "as a research project under the supervision of Frank McSherry" ([home page](https://www.nikolasgoebel.com/)). REPORTED from the README: "Declarative accepts queries expressed in a Datalog-inspired binding language and turns them into differential dataflows dynamically and at runtime." And: "it enforces a fully-normalized, RDF-like data model heavily inspired by systems like Datomic or LogicBlox." ([declarative-dataflow README](https://github.com/comnik/declarative-dataflow)). Of everyone in this group, this is the nearest thing to Sid's store: small attribute-keyed facts, patterns registered while running, results kept current.

### 6.2 Reasons, in Goebel's words

One short post takes the five-part datom (entity, attribute, value, transaction, added?) apart slot by slot. ([What's In A Datom?, 24 Dec 2018](https://www.nikolasgoebel.com/2018/12/24/whats-in-a-datom.html))

- Ids. REPORTED: "Within a system, identifying entities by positive integers (eids) should'nt usually leave much to be desired. The question of what to put in the e-slot becomes more interesting, once we consider communcation across system boundaries. Separate systems might not use the same identification scheme. Even if they do, systems need to coordinate the assignment of identifiers, such as to avoid collisions." "Alternatively, clients might make use of a UUID scheme, in order to avoid coordination entirely. We can therefore add the new shape [uuid a v tx added?] to our collection, for use in communication between separate eid domains."
- Keys. REPORTED: "strong, global names (in the form of fully qualified keywords) are found in the a-slot. This should be considered a great blessing and display of wisdom and kindness."
- Time. REPORTED: "timestamps most certainly don't have to be scalars and allow us to talk about multiple axes of time, manage speculative multi-user computations, and work with heterogeneous data sources."
- Added or retracted. REPORTED: Goebel generalizes the boolean to "diff (difference, as in change in multiplicity)".
- Intent. REPORTED: "Datoms do not record intent, they record facts." "whenever we talk about a source-of-truth, we are referring to a system that has access to user intent and the authority to impose its interpretation." "When replicating / propagating information, we want to be careful to only ever send intent-less data. As soon as more than one system has authority to impose interpretation, we are playing the game of distributed consensus, which is not a fun game at all."
- The envelope itself. REPORTED, from the abstract: "different communication contexts call for slightly different types of datoms."

On transactions ([Perspectives 2019](https://www.nikolasgoebel.com/2019/12/30/perspectives-2019.html)), REPORTED: "Transaction processing really is a three step process: Work hard to minimize the number of conflicts. Actually change things. Work hard to correct the conflicts that did arise." "After spending some time working with Frank McSherry, I have grown much more interested in approaches that invest the bare minimum in step (1), and instead focus on doing step (3) as efficiently as possible."

### 6.3 What changed

3DF is described in the past tense on Goebel's site. Goebel's present work, REPORTED from the home page: "make incremental view maintenance practical in the context of an expressive relational language, for use cases involving many thousands of views at petabyte scale", by "building a dataflow and memoization engine for demand-driven maintenance". In 2019 Goebel had written: "What will remain is the distinction between data-driven and demand-driven systems". So the person who built eager maintenance over facts moved to demand-driven maintenance once the count of views reached the thousands. 3DF's README is also frank: "Declarative is less efficient and much more opinionated than hand-written Differential Dataflow."

### 6.4 Which questions Goebel speaks to

- **(2) ids.** REPORTED: integers inside one system; UUIDs the moment two id domains must talk, "to avoid coordination entirely". INFERRED: for one store with client-minted ids and a possible second store, this says 128-bit random from the start. Goebel is silent on ids that leak time or place.
- **(3) key: word or id?** REPORTED: global qualified names are "a great blessing". That is a vote for the word. INFERRED: in 3DF each attribute is its own input collection and its own index, so the key is the unit of sharing between queries. If keys were opaque ids with names held as facts, every pattern would need a lookup that is itself time-varying. That lookup would have to be a recorded read too.
- **(7) offers, the gate, federation.** REPORTED: intent is interpreted by one authority; only intent-less facts are replicated. INFERRED: an offer is intent; a gated fact is not. A second store should receive facts, never offers. Two gates interpreting the same offers is "the game of distributed consensus".
- **The gate under machine-rate writers.** INFERRED from the three-step remark: compare-and-set that refuses and makes the writer retry is an investment in step 1. With tens of agents per person writing to the same cells, Goebel would look at repairing conflicts after the fact.
- **The fixed envelope.** REPORTED: different contexts want different shapes.
- **Standing patterns at scale.** REPORTED: demand-driven, memoized, at "many thousands of views".

### 6.5 Resemblance and difference

Resembles: the data model, run-time registration, the Clojure and Datomic lineage. Differs: 3DF read facts from a source of truth such as Datomic and did not decide anything. Sid's store is the source of truth and contains its own gate. The written record is thin: blog posts and a README. The thesis could not be opened.

### 6.6 Disagreements

With conflict avoidance as the centre of transaction processing. With always-eager maintenance, by example.

---

## 7. Question by question: what this camp says

Short form. Reasons and sources are in sections 1 to 6.

**(0) The log: never rewritten, or only never lost?** The camp's invariant is neither. It is "never answer wrongly". Materialize compacts history and moves a recorded `since`; reads below it are refused, not faked (REPORTED). The reclocking doc wants new knowledge added as "sidecar metadata, rather than a rewriting of the source data" (REPORTED), which supports never-rewrite. INFERRED: if Sid holds never-rewrite, the camp has no quarrel. If Sid ever trims, give every collection a `since` fact from the start, so an as-of read can tell "nothing was there" from "that was trimmed".

**(2) Ids.** Thin here. Goebel: UUIDs across id domains (REPORTED). INFERRED from CALM: a client can mint a random id with no coordination; issuing sequential ids needs coordination.

**(17) First facts.** Thin here. INFERRED: every timeline has a least time (REPORTED in the formalism: "Each `T` contains a minimal time `t0`"). The first facts are the facts at that time. The grammar of "grammar" and the policy that lets the gate write must exist at t0 or nothing can be admitted. If a second store starts with the same t0 facts under the same ids, joining two stores later is a reclocking problem and not also an identity problem.

**(3) Key: word or id?** Goebel votes for global names (REPORTED). INFERRED: the key is the unit of indexing and of sharing between standing queries. Resolve names to ids when a pattern is registered, and record that lookup as a read.

**(9) Delete.** Three mechanisms, all REPORTED. Tombstones (CALM): monotone, hides, does not erase. Retraction plus compaction (Materialize): erases for real once `since` passes, and gives up as-of reads for that stretch. Per-owner unit plus per-owner key (Schwarzkopf): erases by destroying a key, leaves the log untouched, covers backups. INFERRED: only the third fits never-rewrite, and it has to be in place at the first record.

**(8) By whom.** Thin here. Multiverse: enforce in the store, keep the trusted base small (REPORTED). Goebel: the authority that interprets intent is the source of truth (REPORTED). INFERRED: the gate checks the actor. "Acts for" must be a fact the gate reads, and that read belongs on the verdict.

**(11) When: whose clock, used for order?** The strongest agreement in the camp. The ingest point assigns the time. It is prescriptive. It is the order (McSherry, REPORTED). It may be a counter (DBSP, REPORTED) or look like wall-clock time if an oracle keeps it monotone (Materialize). The sender's time is ordinary data (Dedalus, REPORTED). Order is needed only where the logic is non-monotone (Hellerstein, REPORTED). INFERRED: one field is the order. If the gate's per-cell version is the order within a cell, then something must order across cells, or no cross-cell "as of" exists. A wall-clock `when` that is not that thing should not look like it.

**(16) Layer: who sees a fact before permissions exist?** INFERRED from multiverse: nobody outside the trusted base. INFERRED from CALM: "nearest layer wins" is a statement about absence, so a layered read is consistent only under a frontier for every layer it looked in.

**(10) Order.** Two gates on one layer: fine for accumulating keys, never for a register-like cell (INFERRED from CALM and Blazes). Share a partition: a cell and whatever its gate decision must read (INFERRED from Blazes). As-of: one number for the asker, a position per partition in the record, tied by a durable map (McSherry and Feldera, REPORTED). New partitions must be expressible as "not yet started" (McSherry, REPORTED as an open problem).

**(11) Based-on: every read?** At pattern grain with a time, not per matched fact (INFERRED from Brandon and McSherry). Include reads that found nothing (McSherry 2025, REPORTED). Include the match that made a tool fire: in lineage terms the rule firing *is* the edge (INFERRED from LDFI). Entries the runtime fills in for a reply are the coarse, call-graph kind of lineage, and should be marked as that kind (INFERRED from the Netflix paper).

**(4) Depends-on or path; follow or pin?** Recorded reads are pinned, always. Following the latest is a view over them (INFERRED from "explanations at a specific timestamp"). Separate what the fact needed from what was merely seen, and allow alternative supports, or doubt spreads too far (INFERRED from LDFI's redundancy of support and McSherry's "Accurate, but not yet helpful").

**(5) Index lag.** Yes, and more than yes. A read is *defined* by the frontier it ran under (formalism, Brandon, CRDT paper, all REPORTED). A pattern read that does not know the index's frontier cannot say what it read.

**(11) Because-of.** INFERRED: base facts start chains and derived facts always have a cause. One thing the camp adds. Naiad's epoch is a because-of tag, and the reason to carry it is to know when all work caused by an input has *finished* (REPORTED: notification "after all messages with a specified timestamp have been delivered"). Without that, a screen can show an answer while half the chain from a landing is still in flight. That is Brandon's first failure.

**(1) Version: gate's number, or the fact's own id? Random or from content?** INFERRED from the CDC format: an update's identity is its content plus its time ("there should be only one entry for each `(data, time)`", REPORTED). That is why duplicates and reordering do no harm. An id computed from content makes re-delivery and cross-store exchange idempotent. The gate's number is still needed, because it is the order.

**(6) Keep "replacing 25" on 37?** Yes (INFERRED from the upsert critique). Otherwise every matcher must hold prior state to know what just stopped being true.

**(7) Gate's yes/no; refusals.** Keep offers as the log of intent; the verdict can be computed from it (McSherry 2025, REPORTED as a sketch). Refusals are the lineage of what did not happen (INFERRED from LDFI and from McSherry's point about explaining absence). The camp would let them be trimmed sooner than facts.

**(12) Runtime version and machine.** Yes. Feldera REPORTED that bit-exact recomputation depends on architecture and compiler. A rebuild is an event at a time (McSherry's Megaphone, REPORTED basis).

**(14) The hand.** INFERRED: what is on screen is the demand signal and belongs in the store as an input. It is also the first data to trim. Shown and looked-at are different facts. A shown answer should carry whether it was complete for its as-of.

**(15) A click.** Nothing from this camp.

**(13) Storage.** INFERRED: the log is the truth and indexes are rebuildable, which matches Rama. The camp stores sorted immutable batches of `(data, time, diff)` and merges them. Trimming is `since`. Backups are of the log plus the sidecar maps, which are part of the truth and not derived.

---

## 8. Voices that matter most, who was dropped, who is missing

**The three that matter most for Sid.**

1. **Frank McSherry.** For question 10 and everything tied to it. McSherry is the only one who faced "many ordered partitions, one store-wide as-of" and wrote down a mechanism with its failure modes: invent the interleaving, record it durably as a side map, read only between frontiers. The 2023 transaction design shows what it costs when independent frontiers must move together.
2. **Jamie Brandon.** For the principle. "The map must not lie" is internal consistency, and Brandon tested real systems against it, named the ways it breaks, and explained what it means for a person acting on a screen. Brandon also supplies the grain warning that bears on per-fact provenance.
3. **Alvaro and Hellerstein.** For knowing where the gate is needed and where it is waste, for the manifest as a safety tool, and for lineage that separates alternative supports. Their record of what did not survive production (new languages, fine-grained lineage) is the most useful regret in the group.

**Kept but second.** Feldera, for one thing nobody else wrote down: what re-derivation depends on (architecture, compiler, logged "now", recorded batch cuts). Schwarzkopf, for erasure by construction and per-person views. Goebel, for the datom taken apart slot by slot.

**Dropped.** Noria's core as a model for Sid: it chose to be briefly wrong, which Sid's principles forbid. It stays as the honest opposing voice and as the best account of partial state. Bloom the language: not adopted, by its authors' own account.

**Missing, in my judgment.**
- **Rule engines and content-based publish/subscribe** (Forgy's Rete; the Siena and Le Subscribe work on matching events against very many subscriptions). This is the literature that faces "millions of standing patterns against a stream" head-on. The incremental-view camp answers it only sideways (share indexes; make patterns data).
- **Self-adjusting computation and build systems** (Umut Acar's work; "Build Systems à la Carte" by Mokhov, Mitchell, Peyton Jones). They record, on each built thing, the inputs it read and their hashes, then decide what to redo. That is "based on" plus "recompute when the reads move", studied for years. Brandon cites the build-systems paper as the map of the unstructured side.
- **Tyler Akidau and the Google Dataflow/Beam line.** The main opposing view on completeness: watermarks as estimates, late data, triggers, corrections. Brandon engages it. McSherry rejects it.
- **Kyle Kingsbury (Jepsen)**, who co-wrote Materialize's formalism and tests such claims for a living.
- **Eve** (Chris Granger and others, with Brandon): everything as facts in one store with programs reacting to them. I did not find a written retrospective. If one exists it bears directly on the premise.
I did not research these. Other sessions may hold some of them.

---

## 9. What this camp would question above the table

- **An append-only store of small facts as the one substance.** Broad agreement on the substance. Goebel: "Databases are collections of time-varying relations, streams are derivatives of time-varying relations" (REPORTED). One firm change: say what stopped being true, not only what is newly true. Supersession by "same cell, later version" is an upsert, and McSherry's critique of upserts applies (INFERRED).
- **Tools, grammars, policies and definitions as facts in the same store.** Two cautions. Hellerstein and co-authors: a revolutionary programming model had "limited" adoption, and they now lift what they can and wrap the rest as opaque functions (REPORTED). And: once policies and grammars are facts, the gate's own decision is a non-monotone read of them. The verdict should carry the as-of of the grammar and policy it used, or verdicts cannot be explained later (INFERRED).
- **One store for the planet with personal layers.** McSherry would ask for the single-thread baseline before believing the scale problem (INFERRED from COST). Materialize's own design accepts that a single linearization point puts "some limit to horizontal and geographical scale out" (REPORTED). Schwarzkopf would point out that one shared log mixes owners, so erasure needs per-owner keys from day one (INFERRED). Multiverse says per-person computed views are affordable only with a joint dataflow and partial state (REPORTED).
- **A fixed nine-part envelope, forever.** Goebel: "different communication contexts call for slightly different types of datoms" (REPORTED). The camp's own envelope is three parts, `(data, time, diff)`, and everything else is data. The camp would ask which of the nine parts the *system* must understand in order to be consistent (time; what is retracted) and which are payload that could equally be ordinary facts about the fact (INFERRED).
- **Running answers never stored; only crossings recorded.** Agreement that derived state is never the truth. Disagreement if "never stored" means "never held". This camp's whole craft is holding indexed state so that recomputation is cheap, and evicting it safely (Noria). Crossing records need an as-of, the tool version, and the runtime version to be re-derivable (REPORTED for Feldera; INFERRED for Sid).
- **Every read recorded as provenance on every fact.** The sharpest disagreement. McSherry: complete provenance is "Accurate, but not yet helpful"; compute explanations on demand. Brandon: at fine grain "the overhead of the graph metadata dominates the actual computation". Alvaro: fine-grained lineage was the first thing given up in production. Their split would be this. Deterministic results need no stored reads at all, only times, logic version and runtime version. Non-deterministic results (a person's act, a model's reply) need a read set, at pattern grain, pinned, with empty reads included (INFERRED synthesis).
- **One writer.** Agreement that one authority must assign time. Disagreement that one authority must do all the checking on the write path. McSherry: keep heavy work off "the critical path of timestamp assignment" (REPORTED), and consider deciding commits in the data plane as a view over intents (REPORTED sketch). CALM: most keys may not need a version check at all (INFERRED).

**The strongest challenge this camp makes to the premise.** As described, the system is an asynchronous, match-driven dataflow over lagging indexes. In Brandon's terms that is an eventually consistent streaming system, and such systems show states that were never true. Sid's principle forbids exactly that. What prevents it (one ordering time on every fact, frontiers that say what is complete, a durable map from that time to partition positions) has to be in the record format and the admission path from the first record. It cannot be reconstructed for facts admitted without it.

---

## 10. Questions this camp would call the wrong question

- **"As of: one number or a position per partition?"** A false choice. Both, tied by a recorded map. The question that matters: who invents the interleaving, and where is that written down?
- **"If an index was behind when a pattern was read, is how far it had got written down?"** This treats lag as a footnote to a read. For this camp the frontier *is* the read. The question that matters: may anything read without naming a time inside the index's frontier? If not, there is nothing extra to write.
- **"Follow the latest, or stay on the version read?"** Not a property of a read entry. Records pin. Views follow. Staleness is the difference between the two.
- **"Whose clock?"** Secondary. First: which single field is the order, and who is its only author? After that the clock question mostly answers itself.
- **"Match, don't route", asked only about landings.** A standing pattern is a query over all facts as of now, not only over facts that land after it was registered. The question the design skips: what does a newly registered tool owe to history?
- **"Can two gates ever write one layer?"** Too coarse. It depends on the key: growing set or register.

---

## 11. What each source implies for a second store

- **McSherry, REPORTED** (reclocking goal A): "Permitting the use of streams from one timeline in another timeline." A second store is another timeline. Bring its facts in through a recorded remap from its times to ours. Each store stays internally consistent. The interleaving between the two is invented by the receiver and then fixed.
- **Brandon, REPORTED:** "the changes, timestamps and frontiers at the output are exactly the information that is required at the input, so we can take multiple such systems with different internal implementations and they can be composed into a single consistent computation so long as they stick to this format." So the interface between two stores is three things: changes, times, frontiers. A store that does not publish frontiers cannot be composed consistently with another.
- **McSherry, REPORTED:** the exchange format can be made safe under duplication and reordering if completeness statements travel with the data (the progress-count format).
- **Goebel, REPORTED:** send only intent-less facts. Never let two stores interpret the same offers.
- **CALM, INFERRED:** accumulating facts merge across stores with no coordination. Anything with a "current" needs exactly one owning store per cell.
- **Goebel, REPORTED:** across id domains, UUIDs. INFERRED: if the first facts (base, gate, first kinds) carry the same ids in every store, a later merge is only a time problem.

---

## Sources (all opened in this session unless noted)

McSherry and Materialize
- Blog index and posts: https://github.com/frankmcsherry/blog (posts 2016-03-27, 2020-03-26, 2020-06-19, 2020-08-01, 2020-08-13, 2023-09-19, 2024-08-13, 2024-11-25, 2024-12-23, 2025-04-27, 2025-09-22)
- Virtual Time for Scalable Performance (14 June 2022): https://materialize.com/blog/virtual-time-consistency-scalability/
- Reclocking design doc (2021, addendum 2022): https://github.com/MaterializeInc/materialize/blob/main/doc/developer/design/20210714_reclocking.md
- Materialize formalism (2022): https://github.com/MaterializeInc/materialize/blob/main/doc/developer/platform/formalism.md
- Platform V2 txn management design (2023; author not verified): https://github.com/MaterializeInc/materialize/blob/main/doc/developer/design/20230705_v2_txn_management.md
- Naiad (SOSP 2013): https://sigops.org/s/conferences/sosp/2013/papers/p439-murray.pdf
- Shared Arrangements (VLDB 2020): https://www.vldb.org/pvldb/vol13/p1793-mcsherry.pdf
- Timestamp tokens (arXiv 2022; abstract only read): https://arxiv.org/pdf/2210.06113
- Scalability! But at what COST? (HotOS 2015; abstract only read): https://www.usenix.org/system/files/conference/hotos15/hotos15-paper-mcsherry.pdf

Jamie Brandon
- Internal consistency in streaming systems (2021): https://www.scattered-thoughts.net/writing/internal-consistency-in-streaming-systems/
- An opinionated map of incremental and streaming systems (2021): https://www.scattered-thoughts.net/writing/an-opinionated-map-of-incremental-and-streaming-systems/
- Why isn't differential dataflow more popular? (2021): https://www.scattered-thoughts.net/writing/why-isnt-differential-dataflow-more-popular/
- dida, docs/why.md: https://github.com/jamii/dida/blob/main/docs/why.md ; home page note on dida: https://www.scattered-thoughts.net/
- Local state is harmful (2014): https://www.scattered-thoughts.net/writing/local-state-is-harmful/

Alvaro, Hellerstein and co-authors
- Keeping CALM (CACM 2020): https://arxiv.org/pdf/1901.01930
- Keep CALM and CRDT On (VLDB 2022): https://www.vldb.org/pvldb/vol16/p856-power.pdf
- Blazes (ICDE 2014): https://arxiv.org/pdf/1309.3324
- Dedalus (tech report 2009): https://www2.eecs.berkeley.edu/Pubs/TechRpts/2009/EECS-2009-173.pdf
- The Declarative Imperative (2010): https://www2.eecs.berkeley.edu/Pubs/TechRpts/2010/EECS-2010-90.pdf
- Lineage-driven Fault Injection (SIGMOD 2015): https://people.ucsc.edu/~palvaro/molly.pdf
- Automating Failure Testing Research at Internet Scale (SoCC 2016): https://people.ucsc.edu/~palvaro/socc16.pdf
- New Directions in Cloud Programming (CIDR 2021): https://www.cidrdb.org/cidr2021/papers/cidr2021_paper16.pdf
- Fetched but not used: Consistency Analysis in Bloom (CIDR 2011)

DBSP and Feldera
- DBSP (VLDB 2023): https://arxiv.org/pdf/2203.16684
- How Feldera Fault Tolerance Works (Pfaff, 9 Dec 2024): https://www.feldera.com/blog/fault-tolerance-technical-details
- The Dirty Secret of IVM (Ryzhyk, 12 Sept 2025): https://www.feldera.com/blog/backfill-explained
- Let's make streaming analytics boring (Ryzhyk): https://www.feldera.com/blog/lets-make-streaming-boring

Noria and after
- Gjengset PhD thesis (MIT 2020): https://jon.thesquareplanet.com/papers/phd-thesis.pdf
- Noria (OSDI 2018; fetched, thesis used instead): https://www.usenix.org/system/files/osdi18-gjengset.pdf
- Towards Multiverse Databases (HotOS 2019): https://people.csail.mit.edu/malte/pub/papers/2019-hotos-multiversedb.pdf
- Position: GDPR Compliance by Construction (Poly 2019): https://people.csail.mit.edu/malte/pub/papers/2019-poly-gdpr.pdf
- K9db (OSDI 2023): https://www.usenix.org/system/files/osdi23-albab.pdf
- Read-Your-Writes Consistency in Streaming Dataflow Systems (Sharma, Brown Sc.M. 2022; front matter only): https://cs.brown.edu/media/filer_public/4f/3d/4f3dbe01-770d-4b8e-bae9-e491a14a01fd/sharmaishan.pdf

Nikolas Goebel
- What's In A Datom? (2018): https://www.nikolasgoebel.com/2018/12/24/whats-in-a-datom.html
- Perspectives 2019: https://www.nikolasgoebel.com/2019/12/30/perspectives-2019.html
- Home page (present and past work): https://www.nikolasgoebel.com/
- declarative-dataflow README: https://github.com/comnik/declarative-dataflow
- Not opened (rate-limited): Optimising Distributed Dataflows in Interactive Environments, ETH 2019, https://doi.org/10.3929/ethz-b-000343045

---

# Round two (2026-09-20): the leans, pressed from the frontiers camp

Written from what was read in round one. No new sources were opened. Marks: R = reported (source named; URLs are in this file's source list unless given here), I = inferred by me, N = institutional. The leans are softland-ff's, not Sid's rulings. I did not check Rama; where Rama matters I say what needs checking.

## T1. The smallest convention for internal consistency

Three things, all small (I, built from R parts).
1. Every answer an index gives carries the point it was complete through. Materialize's rule (R): a read at t is correct only between `since` and `upper`.
2. Those points are comparable. If every read behind a running answer names the same point, the answer is exact as of it. If they differ, the floor has two honest moves: re-read everything at the older point (prevent), or show the answer marked as built from two moments (paint). Brandon (R): there must be "some way to determine when an output value is correct". McSherry's display (R): ready, refreshing, pending.
3. A crossing record stores that point and that status.

Nothing else is needed on the fact. What cannot be added later is item 2. If early reads name points that cannot be compared, no later rule can tell whether two of them were the same moment. Leans (10) and (11) together decide whether points are comparable. That is T2.

## T2. The reclocking map as a first-record item

There are two roads to a comparable as-of with no global tick. Both are R in origin. Their fit to Rama is I.

**Road A, McSherry's remap (R).** A separate, slow minter records cut facts. Cut k holds a position for every partition, each at least as far as in cut k−1, so the cuts form a chain. A read names k, eight bytes, and the vector is looked up. Gates never wait for the minter. Recorded: the cut facts, in one small low-rate depot, plus an explicit entry when a partition begins, because McSherry flags that "recording 'these parts have not yet started' is non-trivial" (R). If the Rama camp is right that a microbatch has a global tick, a microbatch is a natural minter: batch k consumed a known range in each partition. Whether Rama exposes and keeps those ranges needs checking. One rule makes named cuts safe with no checking (I): offers read only at named cuts. A fact then always lands after the cut it read under, so any later cut that contains the fact also contains everything it stood on. The price: an agent cannot stand on its own last write until the next cut.

**Road B, Aurora DSQL's way** (R, https://brooker.co.za/blog/2024/12/05/inside-dsql-writes.html). No map at all. Each gate stamps `when` so that it never goes backward within its own partition, and keeps announcing how far it has got even when idle. "As of T" then means, in each partition, everything stamped at or before T, and T is ready once every gate has announced past it. The map is implicit in the facts' own stamps. One more rule keeps causes before effects across different clocks: the gate stamps a fact later than every stamp named in its based-on. Dedalus shows the same repair (R): "implement Lamport clocks [15] atop Dedalus, which allows programs to ensure temporal monotonicity". With that rule a read of one fact at its version needs no waiting at all.

What is recorded on road B: nothing new. Two promises on `when` (never backward within a partition; later than what the fact stood on), and the status on each crossing. That is the smallest possible first-record item. Lean (11), "never used for order", is the one lean that closes it. Facts stamped without those promises can only ever be reached by road A.

**Research-6's opaque store-minted token, "as Zanzibar did".** The same thing under another name, on one condition. Zanzibar's token wraps a Spanner timestamp (R, https://www.usenix.org/system/files/atc19-pang.pdf), and that timestamp lives in the data. Opaque to tools is right; it is the defence against Hyrum's Law. But what the token means must be a fact, or be computable from facts. A token whose meaning lives only in the running runtime dies at the first rebuild, and every as-of recorded before that becomes unreadable.

## T3. Lean (4)'s two marks

**Follow-latest or stay-on-version.** I agree with the Datomic camp. It is the wrong question, and round one said so ("Records pin. Views follow."). The mark is fixed at writing. What decides is a fact that does not exist yet: the one that later supersedes. Their case, at Sid's scale: a model summary stands on 200 paper facts in the ten-million-paper field. Under stay, every metadata fix marks it stale, and at that volume everything is always stale. That map cries wolf. Under follow, a retraction flows through in silence. That map hides doubt. The staleness definition can get both right, if the superseding fact says what kind of change it is. That is one more reason for lean (6): "replacing 25" is where the kind of change belongs.

**Depends-on or how-she-got-here.** This one can be known at writing, but the lean's form is wrong twice (I). First, it is a counterfactual: would the fact differ without this read? For a deterministic tool that is a computation, McSherry's minimal explanation (R), not a guess. For a model reply nobody can know it, the tool included. What can be known at writing is the read's role: trigger, given to the model, matched, navigated. Record the role and let the doubt definition weigh roles. Second, a yes-or-no mark cannot say that either of two reads would have sufficed. LDFI (R) reasons about "the redundancy of support". Without support groups, doubt spreads too far.

So, against the lean: reads at pattern grain, pinned, empty reads included (McSherry, R: "or failed to read, if absent"), each with a role, optionally grouped into alternative supports. No follow-or-stay mark.

## T4. Every read, against its cost (an exchange)

**The skeptics' best case.** Brandon (R): at fine grain "the overhead of the graph metadata dominates the actual computation". McSherry (R): complete provenance is "Accurate, but not yet helpful". Alvaro at Netflix (R): fine lineage was the first thing given up, and call graphs "sacrifice some precision". Big tech's revealed choice (R, other report): a few typed reads that the system acts on, and no more. Numbers (I; my arithmetic on the brief's scale, not anyone's measurement). A fact is perhaps 300 bytes. A tool fired by a pattern that matched 2,000 facts, listing each as id plus version, carries about 48 KB of reads, 160 times the fact. Twenty agents for one person at ten facts a second each comes near 10 MB a second for that person, never trimmed. Under lean (10) each pattern read's as-of is a vector: ten reads over 256 partitions adds 20 KB per fact. Last, "no grace period" meets opaque bodies. A model call or a host call can read around the floor. A list that claims to be complete and is not is worse than no list. It is the map lying about itself.

**The lineage camp's rejoinder.** The asymmetry is Sid's own: an unrecorded read is gone for every earlier fact, and a recorded one costs bytes. For anything not re-derivable the read set can never be recomputed. McSherry's own commit sketch (R) sums a transaction up as its read set and write set. The CALM manifest (R) makes based-on a safety tool: a reader can wait until it holds what a fact stood on. That cannot be retrofitted. Signatures are rules, and with rules fine lineage is "trivial to collect" (Alvaro, R). S3 (R, other report): where the store stays silent on staleness, every serious user builds a side system. And the dominance argument is about one edge per value. It does not touch reads at pattern grain.

**The cheapest shape that still supports the five walks (I).**
- One read-set fact per firing, not per offer. Each offer from that firing points at it once.
- Each entry holds: a pattern (or one fact id at its version), the as-of point (eight bytes, by road A or B), a role, and a digest, which is the count and a hash over the ids and versions that matched. An empty read is an entry with count zero.
- One honesty slot on the read set: complete, or partial (the body could read around the floor). This is not a grace period. It is the difference between "everything" and "everything the floor saw".
- A crossing points at a read set the same way, and adds what was rendered and its status.

The digest is what makes pattern grain enough. Re-running the pattern at its as-of must reproduce the digest. If it does, the full list is recovered exactly, for about 64 bytes instead of 48 KB. If it does not (an index bug, an erased value, a changed policy), the floor knows the recovered list is inexact and can say so.

The walks. *What went stale:* re-run pinned reads at now, compare digests, and let the staleness definition read the difference. *What is in doubt:* walk from fact to read set to matched facts, treating support groups as "or". *What did I look at* and *what was the model given:* from crossing to read set to re-run, checked by the digest. *Who read the erased thing:* direct reads of it are exact. Pattern reads are found through an index from key to pattern reads, then re-run. Where a pattern's condition tested the erased value, the answer is "may have read". For an erasure audit, over-reporting is the safe side.

## A. Leans this camp would reject or sharpen

- **(10) and (11), rejected as a pair.** McSherry (R): the stamp "becomes the truth about when that update happens", and answers must move "to the right of (after) all previously chosen timestamps". Bare vectors can be incomparable, so that promise cannot be kept. With `when` barred from ordering, nothing else orders across partitions. Deciding case: the store grows from 64 to 256 partitions in year two. Every recorded as-of is a vector over a partitioning that no longer exists. A scalar survives untouched.
- **(4), sharpened.** See T3.
- **(5), sharpened.** If no read may pass the index's point, the as-of already is "how far it had got". For "what was withheld", record the policy version, not a list. A list of withheld things leaks them.
- **(7), sharpened twice.** Materialize's design doc (N): "if N txns are run concurrently, 1 will commit and N-1 will have to (usually cheaply) retry." Deciding case: thirty of one person's agents on one hot cell at machine rate is thousands of refusals a second for that person, kept for ever under (13). Goebel (R) would repair rather than refuse. CALM (I): let the grammar say when a key is a growing set, which needs no version check and produces no refusals. Second, the verdict must name the point at which the gate read grammar and policy. Deciding case: a policy is withdrawn at T1, and a gate that has seen that partition only through T0 says yes at T2. With the point on the verdict, that is explainable. Without it, it looks like a gate bug for ever.
- **(9), sharpened.** A matcher that tests values must hold keys, and indexes will hold plain values. Erasure reaches them only because they are derived and get rebuilt (Schwarzkopf's revocation, R). K9db (R) keys per owner. With one key per value, "forget me" is a walk over millions of keys. Wrap value keys under an owner key, so that deleting either one erases.
- **(14), rejected on "never trimmed".** McSherry (R): find "the fewer, discrete moments" at which the answer changes. Deciding case: pan and zoom at ten ticks a second, eight hours a day, is about 290,000 facts per person per day. Record the hand when it changes what is demanded, not on a clock.
- **(1), (2), (6), (12), (17): stand.** Content plus salt is the CDC identity (R) with a caller's intent token inside it. One thing follows: an offerer knows a fact's id before it lands, so based-on can name a fact that was later refused. The gate has to check that what an offer stands on did land, and that check is itself a read at a point. (12) is Feldera's list (R). Add the tool's version, which based-on gives for free.
- **(3), stands with one rule.** Signatures must hold key ids and record the word lookup as a read. Deciding case: the word "relation" is re-pointed from one key to another. If standing patterns hold the word, every one of them changes meaning with nothing landing on any tool.

## B. Wrong questions

- "A cut, never one number": still a false choice. Ask whether as-of points are comparable, and where their meaning is stored.
- Follow or stay: ask what the superseding fact says about why it supersedes.
- "How far the index had really got": ask whether any read may pass the index's point at all.

## C. Above the table: what the leans change

- Every read recorded: with read sets per firing, pattern grain and digests, this camp's objection shrinks to one demand, the honesty slot.
- The runtime as the one non-fact: minting cuts, or announcing points, is runtime work whose residue must be facts. Otherwise every as-of dies at a rebuild.
- The fixed envelope: the leans quietly add three kinds that only the floor writes (verdict, read set, cut or announced point). They belong in the seed of lean (17).
- One writer: a gate per partition is fine by CALM. The gate's own reads across partitions are the new risk, covered under (7).

**Resembles, differs.** Materialize, Feldera and DSQL each own the clock of every input. Sid's gates do too, which is why road B is open. None of them keeps provenance per fact or forbids trimming. The read-set shape is my construction from their parts. None of them runs it.
