# X also held, and can wait: body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L119-122 · BODY · NEW-REASON X1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

Stuart Halloway, 2024, on why everything in a transaction shares one time:

> "Everything in a Datomic transaction happens atomically at a single point in time. Datomic transactions are totally ordered, and this ordering is visible via the time t shared by every datom in the transaction. These properties vastly simplify reasoning about time. With this information model intermediate database states are inexpressible." — Halloway [S-HN24]


---
**datalog L214-214 · BODY · NEW-REASON X2,E1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- INFERRED, from limit 5 in section 2.3. If grammars are versioned facts, then some record must say *which version admitted this value*. Datomic never recorded that and reads all of history through today's schema. It documents the consequence. For Sid the natural home is the gate's verdict: it already "names what it checked".

---
**datalog L244-244 · BODY · DISAGREES X4**

- REPORTED. In Datomic, everyone with a connection sees everything. Visibility is a function applied at read time by trusted code: "A filter can be used to e.g. remove datoms that are incorrect, not applicable at a point in time, or not available for security reasons." The docs show a filter that reads an attribute of each datom's *transaction* (`:source/confidence`) and keeps only trusted ones: "Queries using this filter can focus on finding data of interest, without worrying about the cross-cutting concern of how trusted the data is." [D-FILTER]

---
**datalog L246-246 · BODY · ABOVE X3,C3**

- INFERRED, and this is a challenge. Datomic has no layer. Its three nearest things are separate mechanisms. A *speculative* layer is `with`: "I wonder what this database would look like _if_ I made these transactions. You can do that completely locally." [H-DD]. It is never stored. A *durable private* space is a separate database, joined at read time because "Database is an argument to query." A *trust* view is a filter. Sid's one layer slot does three jobs: it decides who may see, it decides which row wins, and it is part of the cell the gate compares-and-sets. Hickey's word for that is complecting. The camp would ask whether these three must be the same thing.

---
**datalog L247-247 · BODY · NEW-REASON X3**

- REPORTED limit. "as-of Is Not a Branch… with plus as-of lets you see a speculative db with recent datoms filtered out, but it does not let you branch the past." [D-FILTER]. Datomic cannot do durable branches. Sid's layers are durable and shared, so they go beyond what this camp built. Datahike and TerminusDB did build branching (section 6, missing voices).

---
**datalog L248-249 · BODY · NEW-REASON X4**

- For the literal question, the closest voices are Instant (default open) and Fluree (default closed, "Recommended for production"). Sections 5.1 and 6.


---
**datalog L346-346 · BODY · ABOVE X1,C7**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.5 What resembles Sid's situation, and what differs*

- **Unit of admission.** Datomic admits a *set* of facts at one point in time. As the brief describes it, Sid's offer is one fact. If that is right, there is no way to land several facts together, and nowhere for shared provenance to live but on each fact. If Sid already has a unit of saying, this difference disappears.

---
**datalog L347-348 · BODY · NEW-REASON X3**

- **Layers.** Datomic has none, and cannot branch.


---
**datalog L357-358 · BODY · NEW-CASE X1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

- **Jepsen**, on whether a transaction is a set or a sequence. Resolved by changing the docs, not the system.


---
**datalog L429-430 · BODY · NEW-REASON W1**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.1 What they built, and what they chose*

- an **index** on every node (RocksDB), built by reading the whole log.


---
**datalog L471-472 · BODY · NEW-REASON W1**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.3 What they changed, and what bit them*

5. **A full copy on every node.** Version 1's index "requires the underlying Key-Value store (e.g. RocksDB) to exist on every node with a full replica of almost all data." [X-DD7]. Version 2 moved to shared object storage.


---
**datalog L520-521 · BODY · DISAGREES X4**
*5. The leads › 5.1 Instant (Stepan Parunashvili, Joe Averbukh)*

**(16) Who sees a fact before any permissions exist?** REPORTED. Everyone. "If a rule is not set then by default it evaluates to true." "By default, all permissions are considered to be 'true'. To change that, use the '$default' key." Rules are expressions per namespace for view, create, update, delete. View rules run after the query: "On the backend every object that satisfies a query will run through the `view` rule before being passed back to the client." All rules for an app are one JSON blob in one row. [I-PERMS], [I-RULES]


---
**datalog L577-577 · BODY · NEW-REASON X3**
*6. The group: who matters most, who I dropped, who is missin*

- **Datahike** (Christian Weilbach) and **TerminusDB**. Datomic-like and RDF-like stores with durable branches and merging. Datomic says "as-of is not a branch". These two built the branch. They bear directly on layers. Not examined.

---
**datalog L578-578 · BODY · NEW-REASON X3,E5**

- **RDF named graphs and W3C PROV.** A quad's fourth part names the graph a statement belongs to. That is Sid's layer slot, twenty years early, along with a standard vocabulary for derivation and attribution (PROV's `wasDerivedFrom` and `wasAttributedTo`, named from memory, not checked this session). Hickey set RDF aside because "without a temporal notion or proper representation of retraction, RDF statements are insufficient for representing historical information." [H-IM]. The named-graph community's experience with one context slot doing several jobs would be worth a session.

---
**frontiers L65-66 · BODY · NEW-REASON W1,E6**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.2 The reasons, in McSherry's words*

**Many standing queries.** Two answers. First, share indexes across queries. REPORTED (paper abstract): "Current systems for data-parallel, incremental processing and view maintenance over high-rate streams isolate the execution of independent queries. This creates unwanted redundancy and overhead in the presence of concurrent incrementally maintained queries: each query must independently maintain the same indexed state" ([Shared Arrangements, VLDB 2020](https://www.vldb.org/pvldb/vol13/p1793-mcsherry.pdf)). McSherry's later summary: "Pre-building the arrangements (think 'indexes') results in a very low marginal cost of new queries." ([A decade in review, 2024](https://github.com/frankmcsherry/blog/blob/master/posts/2024-12-23.md)). Second, turn queries into data. REPORTED: "You just put your parameter bindings on a data bus, and the answer (and any changes) stream out the other side." "This is a high-throughput take on prepared statements, where many users can submit many concurrent parameter bindings, all on the data plane rather than control plane." ([Lateral Joins and Demand-Driven Queries, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-08-13.md))


---
**frontiers L84-84 · BODY · DISAGREES W3,C2**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(10) as-of: one number or a position per partition?** Both, bound by a recorded map. REPORTED: the `remap` collection. The asker sees one number. The store durably keeps, for each such number, the per-partition positions it stands for. The interleaving across partitions is invented once and then locked, "so that all downstream uses can be consistent". INFERRED for Sid: the first record should already live on one store-wide timeline, and the map from that timeline to depot partition positions should itself be facts. Facts admitted before such a map exists can never be given an exact "as of".

---
**frontiers L97-98 · BODY · NEW-REASON W1,E6**

- **Standing-pattern cost.** REPORTED: share arrangements; make patterns data and join landings against them in one dataflow. INFERRED: "millions of standing patterns" should be one indexed collection of patterns, not millions of subscriptions.


---
**frontiers L145-145 · BODY · NEW-CASE W3**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.3 What changed or was regretted*

- **dida is marked for replacement by something simpler.** On Brandon's home page, under dida: "Roughly works, but definitely not production-ready. Likely to be replaced by something simpler (eg)." The "eg" links to the DBSP paper (checked in the page's HTML). So Brandon, having rebuilt differential dataflow by hand, points at the totally ordered model as the one to keep.

---
**frontiers L146-146 · BODY · NEW-CASE X5,C2**

- **Testing was the gap, even at Materialize.** REPORTED: "the vast majority of the tests I've looked at only test the final output after everything has settled. Even materialize, which is otherwise pretty heavily tested, does not have many tests which examine intermediate outputs." The advice: "figuring out some application-specific invariants that you can monitor in production to at least put a lower bound on the error rate."

---
**frontiers L147-147 · BODY · NEW-REASON X5,C2**

- **Failures were easier to cause than expected.** REPORTED: "not only are these failures easier to trigger than I expected, the resulting behavior is much more complex than I expected and that I can't currently predict or explain the dynamics."

---
**frontiers L149-150 · BODY · NEW-REASON W1**

- **Why differential dataflow did not spread.** Brandon asked users. REPORTED complaints: "where is all the state?", "which operators are internally stateful? how much memory will this use?", "how to get data out, especially how to pull results instead of pushing them". ([Why isn't differential dataflow more popular?, 2021](https://www.scattered-thoughts.net/writing/why-isnt-differential-dataflow-more-popular/))


---
**frontiers L157-157 · BODY · NEW-REASON W3**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.4 Which questions Brandon speaks to*

- **(10) one number or many?** INFERRED: Brandon's pointer to DBSP says: take one number if you possibly can. Partial orders are the cost of loops and of inputs that advance separately. Pay it only where forced.

---
**frontiers L235-236 · BODY · NEW-REASON W3,E3**
*4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff;  › 4.2 Reasons, in their words*

REPORTED: "Time is not the wall-clock time, but essentially a counter of the sequence of transactions applied to the database. Since transactions are linearizable, they have a total order, which defines a linear time t dimension". On origins: "DBSP is inspired from Differential Dataflow [28] (DD), and started as an attempt to provide a simpler formalization of DD". On the difference (joined across line breaks of the PDF's related-work section): "DD's computational model is more powerful than DBSP, since it allows past values in a stream to be 'updated'. In contrast, our model assumes that the inputs of a computation arrive in the time order while allowing for nested time domains via the modular lifting transformer." "in essence DBSP is 'deconstructing' DD into simple component building blocks". All operators are "synchronous": "they consume and produce data at the same 'rate'." ([DBSP, Budiu, Chajed, McSherry, Ryzhyk, Tannen, VLDB 2023; arXiv](https://arxiv.org/pdf/2203.16684))


---
**frontiers L245-245 · BODY · NEW-REASON W3**
*4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff;  › 4.3 What changed or was learned*

- The whole project is a change of mind about differential dataflow: keep the algebra, drop partially ordered time and updates to the past.

---
**frontiers L246-246 · BODY · NEW-REASON W1**

- REPORTED, Ryzhyk, 12 Sept 2025: "before an IVM engine can handle real-time updates, it must first ingest and process the entire historical dataset. In other words, a good IVM engine also needs to be a capable batch engine — and today's systems aren't." ([The Dirty Secret of IVM](https://www.feldera.com/blog/backfill-explained))

---
**frontiers L247-248 · BODY · NEW-REASON W1**

- REPORTED, Ryzhyk: "There's no such thing as a streaming-only workload." ([Let's make streaming analytics boring](https://www.feldera.com/blog/lets-make-streaming-boring))


---
**frontiers L255-256 · BODY · NEW-REASON X1,W1**
*4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff;  › 4.4 Which questions they speak to*

- **Seeding ten million papers** is backfill too. INFERRED: the store needs a bulk path that keeps the same semantics as one-at-a-time landings.


---
**frontiers L259-260 · BODY · NEW-REASON W3,C3**
*4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff;  › 4.5 Resemblance and difference*

Resembles: one linear admission order is natural for a single gate. Differs: DBSP assumes inputs arrive in time order through one door. Sid's depots are partitioned with no order across partitions, so the single counter has to be made (section 1, reclocking), not assumed.


---
**frontiers L284-284 · BODY · NEW-REASON X4,W1**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.3 What changed afterwards*

- **Multiverse databases** (HotOS 2019). REPORTED: "The database applies policies for each user, filtering and transforming the base data to form a user-specific 'parallel universe' database that contains only data that the user is allowed to see". The point is a small trusted base: "multiverse databases limit the TCB to the privacy policies and the database code enforcing them". The cost named: "Multiverse databases' per-user transformations risk expensive queries if applied dynamically on reads, or impractical storage requirements if the database proactively materializes policy-compliant views." The design: "a joint dataflow across 'universes' that combines global, shared computation and cached state with individual, per-user processing and state." "Our early prototype supports thousands of parallel universes on a single server." Writes go to the base only: "applications cannot write to user universes directly." ([Towards Multiverse Databases, Marzoev, Araújo, Schwarzkopf, and others](https://people.csail.mit.edu/malte/pub/papers/2019-hotos-multiversedb.pdf))

---
**frontiers L290-290 · BODY · NEW-REASON X4**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.4 Which questions they speak to*

- **(16) who sees a fact before permissions exist?** INFERRED from multiverse: nobody but the trusted base. A universe holds only what a policy lets in. No policy, no entry.

---
**frontiers L291-291 · BODY · NEW-REASON X3,W1**

- **Layers.** INFERRED: Sid's layers and multiverse universes look alike and are opposite in one way. Universes are read views derived from one shared base; writes go to the base. Sid's layers are places that are written. Multiverse's cost warning still applies to anything computed per person.

---
**frontiers L365-366 · BODY · NEW-REASON X3,C2,X4**
*7. Question by question: what this camp says*

**(16) Layer: who sees a fact before permissions exist?** INFERRED from multiverse: nobody outside the trusted base. INFERRED from CALM: "nearest layer wins" is a statement about absence, so a layered read is consistent only under a frontier for every layer it looked in.


---
**log L247-247 · BODY · NEW-CASE W1,W2**
*Voice by voice › 5. Certificate Transparency, Trillian, and their descendants*

- *Storage.* Let's Encrypt: "Annual cloud costs for our logs are approaching seven figures. The biggest contributor to this is that the data is stored in a relational database." "we previously had a test log fail when we ran into a 16 TiB limit in MySQL." "The Static CT API has proven to be operationally better and more scalable than the RFC 6962 design in almost every way."

---
**log L425-426 · BODY · NEW-REASON X4,C4**
*Question by question: what this camp would say › (16) Layer: who sees a fact before any permissions exist?*

The camp is the wrong place to ask. CT is public by rule: "Log operators MUST NOT impose any conditions on retrieving or sharing data from the log." Tango, Delos, Hyder and Aurora have no readers they distrust. INFERRED, from how they bootstrap: at genesis there are no policy facts, so the rule that applies before any exist must live in the runtime, and the safe rule is that nobody sees anything until a policy fact says so. The first policy facts are genesis facts.


---
**meaning L338-341 · BODY · NEW-REASON X1**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

- 11946681 and 11941131: Kay points to Alex Warth's "Worlds" paper for
  "possible worlds" reasoning, and to a "roll back 'worlds' mechanism (like
  transactions)" as the safe way to do meta-level change.


---
**meaning L499-509 · BODY · NEW-REASON X2,A1**
*Part one — the exchange › 1.6 My reading: is this the axis under Sid's questions?*

**Fifth, Sid's design takes a position neither of them took.** Facts stay
plain. Every interpreter (tool, grammar, definition) is a fact in the same
store. Each fact points at the versions it used. That is *reference instead of
bundling*. It keeps Hickey's inertness: a stored interpreter cannot act until
someone chooses to run it. It answers Kay's "How can you find it?": follow the
pointer. The bet is that a pointer to an immutable version is as good as
carrying the thing. The same bet is made by Smalltalk images (every instance
points at its class), Datomic (every attribute is an entity), Unison (every
reference is a hash), and nanopublications (trusty URIs). The bet holds only
if three things are true, and each one is a row in Sid's table:


---
**meaning L707-714 · BODY · NEW-CASE X4**
*Part two — the people › 2.1 The capability camp: Hardy, Miller, Yee, Shapiro, Donnel*

- Lemmer-Webber on how early choices set. REPORTED, "Re: Re: Bluesky and
  Decentralization" (2024), https://dustycloud.org/blog/re-re-bluesky-decentralization/:
  "I consider the decision to have blocks be publicly queryable to be an example
  of emergent behavior from initial decisions... early architectural decisions
  can have long-standing architectural results, and while many things can be
  changed, some things are particularly difficult to change form an initial
  starting point." (The typo is in the original.)


---
**meaning L747-756 · BODY · NEW-REASON X4**

- (16) who sees a fact before permissions exist. INFERRED: only its writer. In
  their model a new thing is reachable by nobody else until a reference is
  passed. REPORTED (Miller, thesis §7.3, on unguessable "Swiss numbers"): "if
  you do not know an unguessable secret, you can only come to know it if someone
  who knows it and can talk to you chooses to tell it to you." For Sid the hole
  is the pattern read. "Everything matching a pattern as of a point" over one
  heap sees all that is not hidden. Lemmer-Webber on the one system that tried a
  public shared heap, REPORTED: "Bluesky and ATProto have no design for this at
  present, and most of the architectural assumptions assume public messages
  only."

---
**meaning L1213-1222 · BODY · NEW-CASE W1**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

**What hurt at scale.**

- The index, not the log. INSTITUTIONAL (WMF Search Platform, Oct 2023): growth
  of "roughly 1 billion triples per year"; "it took us ~3 months to reload data
  from scratch"; scholarly articles are about half of all triples and "affect
  only about 2% of queries". The graph was split on 9 May 2025. Cross-graph
  questions now need federation. The rule for the split is itself a statement
  (instance of: scholarly article). This is Sid's "whole field seeded from ten
  million papers" case, already lived: the seed dominated the index and served
  almost nobody.

---
**meaning L1250-1259 · BODY · ABOVE X2**

**Regrets.** REPORTED ("Wikidata: The Making Of", 2023): "data uniformity and
coherency has emerged as one of the big challenges [...] Wikidata does not
enforce a fxed schema [...] it also leads to reduced coherence and uniformity
across groups of similar concepts, which is an obstacle to re-use." (The PDF
text drops "fi"; the word is "fixed".) Stored community queries, their Phase 3,
were never built: it "would have served as a forcing function to increase the
uniformity". And: "Originally, Vrandečić had not planned for a SPARQL query
service [...] Fortunately he was wrong." New kinds of entity were slow: lexemes
took from 2012 to 2018. Functions went to a different wiki altogether.


---
**meaning L1260-1267 · BODY · NEW-REASON X2**

**Transfer.** Very close on substance: one store, opaque ids, claims with
sources, never-reused ids, machine-rate writers. Different on the gate:
Wikidata admits almost anything and reports problems later; its regret is
incoherence. That supports a gate that checks shape. Different on privacy:
almost everything is public. Disagreements: community members against the
constraints-as-statements move; ontologists against editor practice; Pintscher
against mass-edit tools; the founders against their own 2012 flexibility.


---
**meaning L1494-1497 · BODY · NEW-CASE X3**
*Part two — the people › 2.7 Kay beyond the thread, STEPS, Worlds, and Ingalls*

- Limits they state: worlds "only capture the in-memory side effects"; the top
  world's commit "is currently a no-op", so nothing persists; and depth costs.
  Reading through a chain 1,000 worlds deep took 27 seconds until they added two
  runtime primitives, then 0.04.

---
**meaning L1562-1563 · BODY · NEW-REASON X1**
*Part two — the people › 2.8 David Reed's pseudo-time, and Croquet*

- Tentative versions commit together: "We call the set of tokens created by an
  atomic action a possibility".

---
**meaning L1731-1736 · BODY · NEW-CASE W2,C4**
*Part two — the people › 2.10 Self (David Ungar, Randall Smith)*

- Classes, under the floor, for speed. REPORTED (the implementation paper): "From
  the implementation point of view, maps look much like classes, and achieve the
  same sorts of space savings for shared data. But maps are totally transparent
  at the SELF language level". And the bootstrap: "All map objects share the same
  map, called the "map map." The map map is its own map."


---
**meaning L1737-1740 · BODY · NEW-REASON W2,C4**

For (13), INFERRED: plain maps in the model; the runtime is free to discover
shapes and store them compactly, as long as that stays invisible. For (17): one
more self-describing root with a single hand-cut circle.


---
**meaning L1916-1924 · BODY · NEW-REASON X4**
*Part three — substrates › 3.2 Linda (David Gelernter)*

Kay, in the AMA, pointed at Linda and said not to start from it: "what is the
similar idea scaled for 40 years later?" INFERRED: the forty-year gap is
protection and attribution. Linda has one space, no owner of a tuple, no record
of who put it there, and destructive reads. Sid's design already differs on each
point: facts are never removed, every fact has a writer, and layers split the
space. What Linda still warns about is reads. A pattern over one space sees
everything in it. I could not open Gelernter's "Multiple tuple spaces in Linda"
(1989), which is Gelernter's own answer and would be the closest precedent for layers.


---
**meaning L1932-1939 · BODY · ABOVE X3**
*Part three — substrates › 3.3 Webstrates (Klokmose, Eagan, Baader, Mackay, Beaudouin-L*

**The one-substance problem, in one sentence.** REPORTED (UIST 2015): "a
webstrate shares the DOM, the whole DOM and nothing but the DOM. Problems
typically come from either wanting to share information that is not represented
in the DOM or not wanting to share information that is in the DOM." With no
layers, personal material was hidden with style rules: "we do include the content
in the DOM, and use local style rules injected in the document to hide unwanted
content."


---
**meaning L2068-2079 · BODY · NEW-REASON X4,C4,C8**
*Part three — substrates › 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tri*

**Permissions: clubs, and an end to the regress.** REPORTED (Miller, Tribble,
Pandya, Stiegler, "The Open Society and Its Media"): "one can distinguish between
who can read a document, who can read the list of people who can read a document,
and who can read that list, out to any desired degree of distinction [...]
However, infinite regress and needless complexity are avoided by using clubs that
are self-reading or self-editing". That is an answer to (16) and (17): the
first policy must govern itself. Also: "All actions in the system are taken by
someone. [...] There are no official truths. There is only who said what, and the
structure of the system reflects that." And: "no one may endorse with the
identity of another". A club's `signatureClub` is recorded "acts for": "Members
of this Club are allowed to endorse with the ID of this Club".


---
**meaning L2325-2335 · BODY · NEW-REASON X4**
*Part four — Sid's questions, hung on the parts of the fact › (16) Layer: who sees a fact before any permissions exist?*

### (16) Layer: who sees a fact before any permissions exist?

**Camp.** Only its maker. In the capability model a new thing is reachable by
nobody else until a reference is handed over (2.1). Worlds: a child world's
changes are private until commit (2.7). The counter-examples all live inside one
trust boundary: Linda's space is global by design (3.2); Folk lets any program
overwrite another's state (3.1); Webstrates had no layers and hid personal
material with style rules (3.3). Bluesky shows where public-by-default goes:
"most of the architectural assumptions assume public messages only", with public
block lists as "emergent behavior from initial decisions" (2.1).


---
**meaning L2336-2340 · BODY · NEW-REASON X4,C2**

**My read.** A layer is born private to its maker. Base is public. The first
policy facts land before the first ordinary fact (17). Pattern reads are scoped to
the layers the reader holds. One more rule, from the Webstrates restore hole: a
read "as of" the past is allowed or refused under today's policy (3.3).


---
**meaning L2566-2572 · BODY · NEW-CASE W1,E5**
*Part five — for the group › 7. The voices that matter most, who I dropped, who is missin*

3. **Wikidata** (Vrandečić, Krötzsch, Pintscher). The nearest existing thing to
   Sid's base layer, fourteen years in. Opaque keys worked. Claims-not-truth worked.
   Rank-not-delete worked, once a reason was added. What hurt is the list Sid
   should fear: a seeded corpus that was half the index and two per cent of the
   use; history tables at their limit; tools writing under people's names; a
   machine-filled provenance slot that came to mean nothing.


---
**rama L108-109 · BODY · NEW-REASON X4**
*Part one — What Rama's own reference says › Question by question › (8) By whom: who checks it? Is a person's agent itself, or t*

- **CHECKED** `docs/18-module-dependencies.md:7-9` — any module on the cluster can mirror any other module's depots and PStates.


---
**rama L123-126 · BODY · NEW-REASON X4**
*Part one — What Rama's own reference says › Question by question › (16) Layer: who sees a fact before any permissions exist?*

**NOT IN THE REFERENCE.** Rama has no read permissions. The one visibility rule it has is about commit, not people:

- **CHECKED** `skill/microbatch.md:130` — "readers outside the owning topology — query topologies, foreign reads, other topologies — see only committed state."


---
**rama L127-128 · BODY · NEW-REASON X4**

**IMPLIED:** inside the cluster, everyone sees everything that has committed. "Who sees" is enforced by whatever stands between people and the cluster.


---
**rama L212-212 · BODY · NEW-REASON W2**
*Part one — What Rama's own reference says › Question by question › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

- **CHECKED** `docs/30-clj-serialization.md:9-13` — built in: basic types, java.util collections, Clojure data structures, and `defrecord`. "Under the hood, Rama uses Nippy."

---
**rama L214-214 · BODY · NEW-REASON W2**

- **CHECKED** `docs/17-serialization.md:144` — "It's critical that all modules and clients that interact with those objects have this serialization registered."

---
**rama L215-215 · BODY · NEW-REASON W2**

- **CHECKED** `skill/operate.md:14` — "Client Rama version must match cluster version (same major and minor)."

---
**rama L285-286 · BODY · ABOVE W1**
*Part two — Nathan Marz › 2. His reasons, in his words*

On what is wrong with databases. "There's a fundamental tension between being a source of truth versus being an indexed store that answers queries quickly. The traditional RDBMS architecture conflates these two concepts into the same datastore. The solution is to treat these two concepts separately." (*Everything wrong with databases and why their complexity is now unnecessary*, 2024, https://blog.redplanetlabs.com/2024/01/09/everything-wrong-with-databases-and-why-their-complexity-is-now-unnecessary/)


---
**rama L352-352 · BODY · ABOVE W1**
*Part two — Nathan Marz › 6. Who he disagrees with, on what*

- Databases as a category: global mutable state, and one store trying to be both truth and index.

---
**rama L355-356 · BODY · NEW-CASE W1**

- The Datomic and XTDB line, by implication. RPL's case study of Multiply is the evidence (INSTITUTIONAL, and it is RPL's own marketing, so weigh it as such): a Clojure team building "an AI-powered platform for collaboration and co-creation" with agents, first on Datomic "for its immutable data model", then XTDB, then Rama. Their stated problems: "Squeezing everything into a fixed data model was unnatural", "It often felt like we were working against the database", "We ran into bottlenecks for running deep live queries… we were unable to get fault tolerance across multiple nodes." (https://blog.redplanetlabs.com/2025/03/04/how-multiply-went-from-datomic-to-xtdb-to-rama/). Read closely, their pain was not facts as the source of truth. It was one fixed index over facts as the only way to ask. Their Rama build has 17 depots "according to the entities they affect" and 19 purpose-built PStates.


---
**rama L405-406 · BODY · DISAGREES W3,C2**
*Part three — The dissent: Jay Kreps › 4. Which of Sid's questions he speaks to*

**(10)** REPORTED: order per partition only; one writer per partition; global order is not worth wanting. INFERRED: on "as of", he would say a position per partition, always. One number only when there is one partition.


---
**skeptics L44-45 · BODY · DISAGREES X4**
*1. The ordinary default, question by question*

**(16) Layer: who sees a fact before permissions exist?** Everyone inside the application. The default is open inside and closed at the API. Tenancy is a `tenant_id` column and a WHERE clause written by hand. Roles are checked in application code (COMMON PRACTICE). Brandur Leach shows how "remember the predicate" fails, about soft delete: "forgetting that extra predicate on deleted_at can have dangerous consequences as it accidentally returns data that's no longer meant to be seen." ([Soft Deletion Probably Isn't Worth It](https://brandur.org/soft-deletion))


---
**skeptics L149-150 · BODY · NEW-REASON X5,P0**
*3. Big-tech operational lessons › 3.1 Amazon*

Andy Warfield on how "never lose" is kept up by people. REPORTED: "When an engineer makes changes that can result in a change to our durability posture, we do a durability review. The process borrows an idea from security research: the threat model." "we really focus on identifying coarse-grained 'guardrails'. These are simple mechanisms that protect you from a large class of risks." One guardrail was an executable model of the storage node: "It wound up being about 1% of the size of the real system, but allowed us to perform testing at a level that would have been completely impractical to do against a hard drive". Warfield also gives a sense of what the hardware does: "That's a bit error rate of 1 in 10^15 requests. In the real world, we see that blade of grass get missed pretty frequently – and it's actually something we need to account for in S3." ([Building and operating a pretty big storage system called S3, 2023](https://www.allthingsdistributed.com/2023/07/building-and-operating-a-pretty-big-storage-system.html))


---
**skeptics L239-239 · BODY · DISAGREES X4**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (16) visibility | Open inside the app; predicate by hand | Zanzibar: permission checks at a recorded freshness (REPORTED). Pavlo: least privilege, above all for agents (REPORTED) |

---
**sync L727-728 · BODY · NEW-REASON X4,C2**
*2. Section one: sync and multiplayer › 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)*

- (16) Read permission is hard under one global version and natural under
  per-row versions plus a per-client view record. REPORTED.

---
**sync L729-730 · BODY · NEW-REASON X2**

- (3) The client states its schema version in both directions; two schema
  generations coexist for a while. REPORTED.

---
**sync L766-770 · BODY · NEW-REASON X2,C4**
*2. Section one: sync and multiplayer › 2.7 tldraw sync (Steve Ruiz)*

- Migrations run both ways, with named versions, so old and new clients can
  share a room: the server "runs migrations to make sure clients of different
  versions can collaborate without issue." The warning: "If you omit
  `migrations`, clients on different versions won't be able to collaborate
  without errors." (tldraw docs, "tldraw sync", https://tldraw.dev/docs/sync)

---
**sync L775-777 · BODY · NEW-REASON X2**

**3. Regrets.** None written that I found. The announcement names the hard
part: "migrations and version skew, which are best handled from within the SDK".


---
**sync L903-907 · BODY · NEW-REASON W2**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- *What held.* "Automerge 3.0 uses the same file format as Automerge 2". The
  gains came from a new in-memory form: "pasting Moby Dick into an Automerge 2
  document consumes 700Mb of memory, in Automerge 3 it only consumes 1.3Mb".
  (https://automerge.org/blog/automerge-3/)


---
**sync L992-996 · BODY · NEW-CASE X3**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- Layers should not depend on each other: "We experimented with 'Git-like'
  dependent drafts in Upwelling and found that it made the model more difficult
  for users to understand and did not add meaningful value. ... A key insight
  of Upwelling is therefore that there should be no dependence between one
  draft and another".

---
**sync L997-999 · BODY · NEW-REASON X3**

- Layers float: "when a draft is merged onto the stack, it is also merged into
  all existing drafts. Any conflicts are therefore surfaced and resolved in the
  drafts before they are merged onto the stack."

---
**sync L1015-1019 · BODY · NEW-REASON X3**

- Two levels only: "There's no branching from branches. You can only create
  branches from main, and merge back to main. (This seems sufficient for most
  writing use cases, although we've already encountered occasional cases where
  it's not enough.)" A branch can be made after the fact: "You can also create
  a branch retroactively with your current edit session".

---
**sync L1111-1114 · BODY · NEW-REASON X3**

- The layer wish, stated as an open problem: "users must have the freedom to
  reject edits made by another collaborator, or to make private changes to a
  version of the document that is not shared with others".


---
**sync L1121-1123 · BODY · NEW-REASON X3**

- Layers. Keep layers independent of each other. When the base moves, every
  open layer must be re-examined. Two levels were nearly enough. A layer can be
  cut out of a session after the fact. REPORTED.

---
**sync L1483-1487 · BODY · DISAGREES X4**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- *Permissions came last.* "The original design focus of the protocol was to
  support public conversations in a global context"; private records are 2026
  roadmap work. The word "GDPR" does not occur in the paper or the repository,
  account, or blob specs.


---
**sync L1521-1521 · BODY · DISAGREES X4**

- (16) Public first; permissions are being retrofitted four years in. REPORTED.

---
**sync L1529-1533 · BODY · DISAGREES X4**

**5. Resemblance and difference.** Same scale ambition. Same records with
schema names, authors, and versions. Different: many writers who do not trust
each other; public by default; deletion treated as a right; no provenance on
records; no central gate, so no compare-and-set across accounts.


---
**sync L2412-2413 · BODY · NEW-REASON X2**
*4. Question by question › (3) Key: a word, or an id with its name and shape as facts?*

- The client states its schema version on every exchange: Replicache. Up and
  down migrations so mixed versions can work together: tldraw. REPORTED.

