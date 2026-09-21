# A above the table: body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L101-104 · BODY · NEW-REASON A3**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

**Why one writer.**

> "Process, which is a term I will use to say 'the acquisition of novelty', of novel information, is something that requires coordination. In the end, I do not care if it is transactional or eventual consistency. That thing at the end that is going to merge together your stuff, that is a form of coordination. There have to be rules to govern what is allowed, and what is not. Somebody has to be responsible for doing it. We cannot all do it." — Hickey [H-DD]


---
**datalog L145-148 · BODY · NEW-REASON A1**

**Why derived answers are not data.**

> "putting facts behind a dynamic interpreter (one that could answer the same question differently at different times, mix facts with opinions/derivations or have effects) certainly exceeds (and breaks) the idea of data… Consider the difference between a calculation involving (several times) a fact (date-of-birth) vs a live-updated derivation (age). The latter can produce results that don't add up. 'date-of-birth' is data and 'age' (unless temporally-qualified, 'as-of') is not." — Hickey [H-HN16]


---
**datalog L149-150 · BODY · NEW-REASON A1,W1**

And on caching, which matters for this project: Datomic caches the *sources* of answers, never the answers. The old model "put the _answers_ to questions in cache, hoping maybe we will ask the same question again later." In Datomic "the _sources_ of answers get cached", meaning immutable index segments, which can be cached anywhere because they never change. [H-DD]


---
**datalog L177-178 · BODY · ABOVE A2**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.3 What they later changed, regretted, or moved away from*

8. **Code moved out of the database.** Datomic Pro can store a transaction function as data in the database, with "versions of the code live in the Database", or take it from the classpath, with versions "external to the database in e.g. traditional source control." [D-TXFN]. With Ions (2018) Hickey tied running code to git: "So we connect application revisions to git identity. If you ever were wondering 'what version of the code is this, actually that is running?' There is no question. It is the name of the version." Builds that cannot be tied to a commit get a name prefixed "unreproducible". [H-IONS]


---
**datalog L253-253 · BODY · CARRIED A3**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED. Two gates *can* write one ordered unit safely, provided each append is a compare-and-set against the unit's previous position. That is Jepsen's correction, accepted by the Datomic team (section 2.3, item 6).

---
**datalog L254-254 · BODY · NEW-REASON A3,X1**

- REPORTED. What compare-and-set on one cell does *not* give. Hickey, 2009: "You can use CAS, which is essentially saying there's one timeline per identity. Right? And it's uncoordinated. It's impossible to coordinate two things that are using CAS timelines, but CAS timelines are still useful." [H-AWTY]. Sid's gate checks one cell: entity plus key plus layer. That is one timeline per cell. Any rule that spans two cells cannot be enforced by it: a relation and its inverse, a move from one place to another, five facts that must land together or not at all.

---
**datalog L307-307 · BODY · NEW-REASON A2**

- REPORTED practice. Nubank puts the code version on every transaction: "we attach that with the Git version of the service." [N-QCON]. The Datomic docs list "the application that executed it" among the things to record on a transaction. [D-TXDATA]

---
**datalog L308-308 · BODY · NEW-REASON A2**

- REPORTED. Hickey on the right identifier for running code: a git commit, because "that is the identity. I hate human made up 'version 2'… These things mean nothing." A build that is not a clean commit is labelled "unreproducible" in its name, so that nobody mistakes it. [H-IONS]. That is the map not lying, applied to the runtime.

---
**frontiers L69-70 · BODY · NEW-REASON A1**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.2 The reasons, in McSherry's words*

**Wall-clock "now" inside a standing answer.** REPORTED: "The 'art' to using `mz_now()` comes down to finding the fewer, discrete moments where you would like your maintained view to change, and re-framing your logic to reveal this." A rule like "older than one day" must be written as a validity bound on each record, so the system knows the future moment each record flips. ([Programming with time in Materialize, 2025](https://github.com/frankmcsherry/blog/blob/master/posts/2025-09-22.md))


---
**frontiers L76-76 · BODY · NEW-CASE A2,C2**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.3 What changed over the years (worth more than the papers)*

- **Virtual time came from live reconfiguration.** REPORTED: "Moments where you would otherwise need to pause the system can be handled by 'virtually' cutting over from one configuration to another using the built in timestamps. This would be a foundational insight that led to the use of virtual time as the coordinating principle at Materialize." (Decade in review, on the Megaphone paper)

---
**frontiers L85-85 · BODY · NEW-REASON A3,W4**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(10) can two gates write one layer?** INFERRED from the virtual-time post and the 2023 txn design: yes, if one cheap authority assigns times and the gates only do the work. "Adapter scales largely by avoiding substantial work on the critical path of timestamp assignment." The part that must be single is time assignment, not checking.

---
**frontiers L164-165 · BODY · NEW-REASON A1,E2**
*2. Jamie Brandon (internal consistency; the person at the sc › 2.5 Resemblance and difference*

Resembles: Brandon cares about a person looking at output and acting on it, which is Sid's crossing. Brandon comes from Eve, where programs reacted to facts in one store. Differs: Brandon's tests are on SQL-style aggregates over money. Sid's running answers may be model-shaped and not checkable against a batch oracle. Brandon never had to keep provenance per fact.


---
**frontiers L237-238 · BODY · NEW-REASON A1,C1**
*4. DBSP and Feldera (Mihai Budiu, Leonid Ryzhyk, Ben Pfaff;  › 4.2 Reasons, in their words*

**What must be recorded to recompute.** Feldera's fault-tolerance write-up is the most concrete statement in this whole group. REPORTED: "For each batch of data that Feldera processes through the pipeline, it logs enough information to obtain another copy of the batch's input data later." "The Kafka input adapter logs per-partition event offsets within each input topic, the HTTP GET input adapter logs byte offsets within a URL, and so on. There is no way to re-read data that arrives through the HTTP input connector or through ad-hoc queries, so in those cases, Feldera logs, and replays, all of the input data." "Feldera uses a checksum included in the log to ensure that the data read for replay is the same as the original data." ([How Feldera Fault Tolerance Works, Ben Pfaff, 9 Dec 2024](https://www.feldera.com/blog/fault-tolerance-technical-details))


---
**frontiers L239-240 · BODY · NEW-REASON A1**

**What "re-derivable" quietly depends on.** REPORTED, same post: "the replay process requires computation in a pipeline to be reproducible, meaning that, given the same input and the same pipeline state, the pipeline always produces the same output." Exceptions: "queries that use SQL's NOW function, which returns the current time; this can be made reproducible by logging the time that was originally used and using that time in the replay. We also need to avoid other reasons that results can be non-reproducible, such as floating-point results that differ across processor architectures (by replaying on the same architecture) or across compiler optimizations (by using the same compiler and optimizations)."


---
**frontiers L241-242 · BODY · NEW-REASON A1,X1**

**Why the batch boundaries themselves are recorded.** REPORTED: replaying the same input cut differently "would defeat Feldera's synchronous streaming guarantee, which says that Feldera produces exactly one output change for each input change".


---
**frontiers L289-289 · BODY · ABOVE A1**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.4 Which questions they speak to*

- **Running answers never stored.** REPORTED: Noria's partial state is this idea with engineering. Keep only what is being read, fill holes on demand, evict the rest. INFERRED: "never stored" should mean "never the truth", not "never held". Recomputing from the log each time costs the whole history.

---
**frontiers L319-319 · BODY · NEW-REASON A3,E1**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.2 Reasons, in Goebel's words*

- Intent. REPORTED: "Datoms do not record intent, they record facts." "whenever we talk about a source-of-truth, we are referring to a system that has access to user intent and the authority to impose its interpretation." "When replicating / propagating information, we want to be careful to only ever send intent-less data. As soon as more than one system has authority to impose interpretation, we are playing the game of distributed consensus, which is not a fun game at all."

---
**frontiers L332-332 · BODY · ABOVE A3,E1**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.4 Which questions Goebel speaks to*

- **(7) offers, the gate, federation.** REPORTED: intent is interpreted by one authority; only intent-less facts are replicated. INFERRED: an offer is intent; a gated fact is not. A second store should receive facts, never offers. Two gates interpreting the same offers is "the game of distributed consensus".

---
**frontiers L367-368 · BODY · DISAGREES A3,C3,C2**
*7. Question by question: what this camp says*

**(10) Order.** Two gates on one layer: fine for accumulating keys, never for a register-like cell (INFERRED from CALM and Blazes). Share a partition: a cell and whatever its gate decision must read (INFERRED from Blazes). As-of: one number for the asker, a position per partition in the record, tied by a durable map (McSherry and Feldera, REPORTED). New partitions must be expressible as "not yet started" (McSherry, REPORTED as an open problem).


---
**frontiers L383-384 · BODY · NEW-REASON A1,A2**

**(12) Runtime version and machine.** Yes. Feldera REPORTED that bit-exact recomputation depends on architecture and compiler. A rebuild is an event at a time (McSherry's Megaphone, REPORTED basis).


---
**frontiers L401-402 · BODY · NEW-REASON A1**
*8. Voices that matter most, who was dropped, who is missing*

**Kept but second.** Feldera, for one thing nobody else wrote down: what re-derivation depends on (architecture, compiler, logged "now", recorded batch cuts). Schwarzkopf, for erasure by construction and per-person views. Goebel, for the datom taken apart slot by slot.


---
**log L70-70 · BODY · ABOVE A1**
*Voice by voice › 1. Pat Helland*

- On the log as truth: "The truth is the log. The database is a cache of a subset of the log." (*Immutability Changes Everything*, ACM Queue 2015, section "Append-only Computing".)

---
**log L98-99 · BODY · ABOVE A1**

**5. Resembles and differs.** Resembles: many writers, permanent records, attribution, data that outlives the code that wrote it. His "outside data" is almost exactly Sid's fact. Differs: Helland assumes many services, each with private mutable "inside data". Sid's design has no inside data at all in storage; the only inside is the running answer, which is not stored. Helland never had to make one store serve as everyone's inside and outside at once.


---
**log L115-115 · BODY · ABOVE -**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- Why the log was made swappable: consensus-based systems are "complex, monolithic, and difficult to upgrade once deployed." What must live outside the log: "The VirtualLog MetaStore is a necessary and sufficient source of fault-tolerant consensus in our architecture". Why it cannot live inside: inline reconfiguration "requires the Loglet itself to be highly available for writes". (Delos, OSDI 2020.)

---
**log L117-117 · BODY · NEW-REASON A2**

- Code upgrades go through the log: "Once all servers have the new engine, we enable it by sending a command via the log itself. This ensures that the effects of the engine are visible on the local store beyond a consistent log position, retaining the property that the LocalStore is a deterministic function of the shared log." (Delos, SOSP 2021.)

---
**log L120-121 · BODY · NEW-REASON A1**

- Inputs or outputs in the log: "Having flexibility in whether we store inputs vs. outputs in the shared log can help balance ingress bandwidth against CPU overhead". And a stored output "can only be applied to the database if the intervening entries did not invalidate it". (OSR 2024.)


---
**log L124-124 · BODY · ABOVE A1**

- *Playback.* vCorfu (2017, by Tango co-authors): "The achilles' heel of shared log systems, however, is playback. To service any request, a client must read every single update and apply it to in-memory state". "In practice, this has limited the applicability of shared log systems to settings characterized by few clients or small global state."

---
**log L127-127 · BODY · NEW-CASE A2,A1**

- *Upgrades.* "Ad-hoc stack updates resulted in inconsistency events across servers in production, which caused us to formalize the two-phase update protocol". "Surprisingly, engine roll-out has been the only source of inconsistency in production so far, despite initial worries that developers would find it difficult to write deterministic code. We built extensive protection against this failure mode via incremental checksums of the LocalStore". (Delos, SOSP 2021.)

---
**log L139-139 · BODY · NEW-REASON A2,A1**

- (12) REPORTED: changes to interpreting code enter through the log; derived state is checksummed. This is the most production-tested answer to question 12 anywhere in the camp.

---
**log L158-158 · BODY · NEW-REASON A1,E2**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- Record the answer you relied on, not the recipe: "Make the call at the time of the event creation and enrich the information returned from the call onto the event. This allows for deterministic replays." (same chapter.)

---
**log L168-168 · BODY · ABOVE -**

- The ten-year retrospective, in InfoQ's words reporting his DDD Europe 2016 talk: "The single biggest bad thing that Young has seen during the last ten years is the common anti-pattern of building a whole system based on Event sourcing." "CQRS and Event sourcing are not top-level architectures and normally they should be applied selectively just in few places." His own blog in 2012 already said: "CQRS is not a top level architecture".

---
**log L180-180 · BODY · NEW-REASON A1,E2**

- (12) REPORTED: no software version in the envelope; instead record the computed result at write time.

---
**log L193-193 · BODY · ABOVE A1**
*Voice by voice › 4. Martin Kleppmann*

- The original thesis: "A materialized view is just a cached subset of the log, and you could rebuild it from the log at any time." (*Turning the database inside-out*, 2015.)

---
**log L205-205 · BODY · ABOVE -**

- From one way to a taxonomy: "there is no one true way: all of the categories in the taxonomy have important use cases." (same.)

---
**log L223-224 · BODY · ABOVE -**

**5. Resembles and differs.** Resembles: he thinks about humans and agents sharing one data substrate, about provenance, about strangers. Differs: his later systems give each person their own primary copy, which is the opposite of one truth, and they accept merges where Sid has a gate.


---
**log L265-266 · BODY · ABOVE A3**
*Voice by voice › 5. Certificate Transparency, Trillian, and their descendants*

- (10), second store. INSTITUTIONAL: many independent logs with no order across them is the design, not an accident. Safety comes from the client's rule ("At least 2-5 SCTs … from logs that were approved"), a quorum over independent stores.


---
**log L267-268 · BODY · ABOVE -**

**5. Resembles and differs.** Resembles: planet scale, strangers, permanence, an economy resting on it, one gate per log, "the map must not lie". Differs: CT entries are large, public, self-authenticating certificates with no personal layers and no supersession. Nothing in CT reads the log to compute an answer under time pressure. And CT is many stores by design, where Sid wants one.


---
**log L295-295 · BODY · NEW-REASON A3,W4**
*Voice by voice › 7. Phil Bernstein's Hyder*

- "Meld is inherently a sequential algorithm. Although parts of it can be parallelized, it ultimately must process log records in log sequence." (VLDB 2011.)

---
**log L297-297 · BODY · NEW-CASE A3,W4**

- Four years later: "meld has been the bottleneck that limits transaction throughput." The algorithm "is already heavily optimized, and we have been unable to improve it." (SIGMOD 2015.)

---
**log L317-317 · BODY · ABOVE A1**
*Voice by voice › 8. Amazon Aurora, and what AWS did next*

- "THE LOG IS THE DATABASE" is a section heading in the 2017 paper. Werner Vogels in 2019: "In Amazon Aurora, the log is the database. Database instances write redo log records to the distributed storage layer, and the storage takes care of constructing page images from log records on demand."

---
**log L319-319 · BODY · DISAGREES A3,W4**

- Why one writer: "Aurora provides all these isolation levels by making a simplifying assumption that at any time there is only a single writer generating log updates with LSNs allocated from a single ordered domain." (SIGMOD 2017.) "A single writer has local state for all writes and can easily coordinate snapshot isolation, consistency points for storage, transaction ordering, and structural atomicity. It is more complex for replicas." (SIGMOD 2018.)

---
**log L322-323 · BODY · CARRIED A3,E8,P0**

- The one rewrite, and how it is made safe. On recovery the database records "a truncation range that annuls any log records beyond the newly computed VCL". "The truncation ranges are versioned with epoch numbers, and written durably to the storage service". A deposed writer is fenced: "Aurora, rather than waiting for a lease to expire, just changes the locks on the door."


---
**log L329-329 · BODY · DISAGREES A3,W4**

- (10) INSTITUTIONAL: one writer bought simplicity and no consensus protocol. When AWS needed more than one decider, they sharded the deciders by key, so that each key has exactly one. They did not let two deciders share a key again. That is this camp's consistent answer to "can two gates write one layer": yes, and never one cell.

---
**log L353-354 · BODY · ABOVE A1**
*Voice by voice › 9. Two short notes on voices the brief did not list*

What Marz changed: in 2011 he argued for recomputing everything from the log. Rama maintains its PStates incrementally and keeps full recompute as the recovery road, to "fix corruption caused by a bug in your ETL code". I found no essay where he says this in so many words; the change is visible in what he built.


---
**log L388-389 · BODY · ABOVE -**
*Question by question: what this camp would say › (17) The first facts*

What cannot be inside, REPORTED. Delos: the register that says which log is current must sit outside the log, because putting it inside "requires the Loglet itself to be highly available for writes". CT: the list of trusted logs lives in browser policy, outside every log.


---
**log L431-432 · BODY · DISAGREES A3,W4**
*Question by question: what this camp would say › (10) Order: two gates on one layer? What must share a partit*

So: two gates can write one layer, and with Rama they must, because a layer spans partitions and each partition has its own leader. What no source allows is two deciders for one key at one time. Aurora's multi-master tried a shared unit of conflict and was retired. Both Aurora and CORFU fence a deposed writer with an epoch.


---
**log L437-438 · BODY · DISAGREES A3,W4**

**For the first record (INFERRED).** Read "the gate is the only writer" as "exactly one gate per partition at any moment, fenced by an epoch". Treat a cross-partition "as of" as a vector, and if a single name for it is wanted, make the vector a small fact of its own and point at that. Remember that the number of partitions and the partitioning function are part of what any position means. A pointer that depends on positions has baked the physical layout into the record; Delos virtualized its log addresses for exactly this reason.


---
**log L502-503 · BODY · NEW-CASE A2,A1**
*Question by question: what this camp would say › (12) Start of a session: is the runtime version and the kind*

REPORTED. This is the best-evidenced answer in the report. Delos: "engine roll-out has been the only source of inconsistency in production so far, despite initial worries that developers would find it difficult to write deterministic code." The fix: new code is enabled "by sending a command via the log itself", so that its effects are visible "beyond a consistent log position". The safety net: "incremental checksums of the LocalStore". Balakrishnan in 2024: learners "are upgraded more frequently than acceptors; and hence more likely to fail." Young's answer to the same problem is different: do not record the software version; record the computed result, which "allows for deterministic replays". Kreps: "Code will always change."


---
**log L504-505 · BODY · NEW-REASON A1,A2,E2**

**For the first record (INFERRED).** A running answer is re-derivable from the same reads only under the same tool version and the same runtime. Tools are facts, so their versions are already in the store. The runtime is the one thing that is not a fact, so its version has to be written wherever a re-derivation is promised: on each crossing and on each verdict. A change of runtime should land as a fact at a position, so that every reader can say which facts were admitted under which runtime. A crossing should carry a checksum of what was shown, so that a later re-derivation can be checked rather than trusted. The kind of machine matters only if it can change an answer (floating point, fonts, a model's weights); where it can, it is part of the version.


---
**log L528-529 · BODY · ABOVE -**
*For the group › 7. The voices that matter most for Sid, who I dropped, who i*

**Greg Young, read together with the Overeem study, matters second.** They lived longest with never-rewrite at the level where Sid's facts live: names, shapes, meanings, deletions, rebuilds. Young is also the person who warns against the whole premise, and names his own regret.


---
**meaning L96-99 · BODY · ABOVE -**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

**Turn 1 — Kay, 11945722, 21 June 14:14**

> What if "data" is a really bad idea?


---
**meaning L129-146 · BODY · ABOVE -**

**Turn 5 — Kay, 11946935, 16:30**

> There are lots of "old and fundamental" ideas that are not good anymore, if
> they ever were.
>
> The point here is that you were able to find the interpreter of the sentence
> and ask a question, but the two were still separated. For important
> negotiations we don't send telegrams, we send ambassadors.
>
> This is what objects are all about, and it continues to be amazing to me that
> the real necessities and practical necessities are still not at all
> understood. Bundling an interpreter for messages doesn't prevent the message
> from being submitted for other possible interpretations, but there simply has
> to be a *process* that can extract signal from noise.
>
> This is particularly germane to your last paragraph. Please think especially
> hard about what you are taking for granted in your last sentence.


---
**meaning L147-178 · BODY · ABOVE -**

**Turn 6 — Hickey, 11947809, 17:54**

> Without the 'idea' of data we couldn't even have a conversation about what
> interpreters interpret. How could it be a "really bad" idea? Data needn't be
> accompanied by an interpreter. I'm not saying that interpreters are
> unimportant/uninteresting, but they are separate. Nor have I said or implied
> that data is inherently meaningful.
>
> Take a stream of data from a seismometer. The seismometer might just record a
> stream of numbers. It might put them on a disk. Completely separate from that,
> some person or process, given the numbers and the provenance alone (these
> numbers are from a seismometer), might declare "there is an earthquake
> coming". But no object sent an "earthquake coming" "message". The seismometer
> doesn't "know" an earthquake is coming (nor does the earth, the source of the
> 'messages' it records), so it can't send a "message" incorporating that
> "meaning". There is no negotiation or direct connection between the source and
> the interpretation.
>
> We will soon be drowning in a world of IoT sensors sending
> context-or-provenance-tagged but otherwise semantic-free data (necessarily,
> due to constraints, without accompanying interpreters) whose implications will
> only be determined by downstream statistical processing, aggregation etc, not
> semantic-rich messaging.
>
> If you meant to convey "data alone makes for weak messages/ambassadors", well
> ok. But richer messages will just bottom out at more data (context metadata,
> semantic tagging, all more data) Ditto, as someone else said, any accompanying
> interpreter (e.g. bytecode? - more data needing interpretation/execution).
> Data remains a perfectly useful and more fundamental idea than "message". In
> any case, I thought we were talking about data, not objects. I don't think
> there is a conflict between these ideas.


---
**meaning L201-203 · BODY · ABOVE -**

- 11947120, on ambassadors: "This is why "the objects of the future" have to be
  ambassadors that can negotiate with other objects they've never seen. Think
  about this as one of the consequences of massive scaling ..."

---
**meaning L212-216 · BODY · ABOVE -**

- 11948634, to a user who asked how to avoid sending an interpreter for the
  interpreter, and so on down: "Yes, so think about how to make this work
  "nicely" in an Intergalactic Network ..." Later (11957719), to "it can't be
  turtles all the way down": "A good question isn't it?", and a pointer to
  Lincos, the language designed for first contact.

---
**meaning L220-223 · BODY · NEW-CASE A1,A2**

- 11954982, on reviving a 1978 Smalltalk image from a discarded disk pack: it
  "was quite easy to bring back to life because it was already virtualized "for
  eternity")." And: "The idea was that you could make a universal computer in
  software that would be smaller than almost any media made in it, so ..."

---
**meaning L296-304 · BODY · ABOVE -**

> 'Data' is not a *universal* idea, i.e. a single primordial idea that
> encompasses all things. But the idea that dynamic objects/ambassadors
> (whatever their other utility) can substitute for facts (data) *is* a bad idea
> (does not correspond to reality). Facts are things that have happened, and
> things that have happened have happened (are not opinions), cannot change and
> cannot introduce new effects. Data/facts are not in any way dynamic (they are
> accreting, that's all). Sometimes we want the facts, and other times we want
> someone to discuss them with. That's why there is more than one good idea.
>

---
**meaning L305-310 · BODY · ABOVE -**

> Data is as bad an idea as numbers, facts and record keeping. These are all
> great ideas that can be realized more or less well. I would certainly agree
> that data (the maintenance of facts) has been bungled badly in programming
> thus far, and lay no small part of the blame on object- and place-oriented
> programming.


---
**meaning L316-322 · BODY · ABOVE -**

- 11953728, to a user who asked what the scare quotes meant: "Just to say one
  more time here: the central idea is "meaning", and "data" has no meaning
  without "process" (you can't even distinguish a fly spec from an intentional
  mark without a process. One of many perspectives here is to think of
  "anything" as a "message" and then ask what does it take to "receive the
  message"?" And: "the extent to which most code today relies on "outside of
  code" programmer views (and hopes) is astounding and distressing."

---
**meaning L323-324 · BODY · ABOVE -**

- 11945766: "E.g. "data" in the small "seems natural" but the whole idea scales
  terribly."

---
**meaning L325-327 · BODY · ABOVE -**

- 11940028: object-oriented "got neutered into Abstract Data Types", because
  people wanted to keep "procedures, assignment statements, and data structures.
  These don't scale well".

---
**meaning L333-337 · BODY · ABOVE -**

- 11956408: "several starts could be to relax from programming by message
  sending (a tough prospect in the large) to programming by message receiving,
  and in particular to program by intent/meaning negotiation." And: "Linda was a
  great idea of the 80s, what is the similar idea scaled for 40 years later? (It
  won't look like Linda, so don't start your thinking from there ...)"

---
**meaning L342-357 · BODY · ABOVE -**
*Part one — the exchange › 1.3 What each actually claimed*

### 1.3 What each actually claimed

**Kay** (REPORTED, my ordering):

1. "Data" in scare quotes is the practice of shipping marks whose meaning lives
   somewhere else, usually in programmers' heads. The complaint is the
   separation: "the two were still separated".
2. Meaning needs a process. Without one "you can't even distinguish a fly spec
   from an intentional mark".
3. Hickey could resolve the ambiguity of Kay's sentence only because Kay was
   there to be asked. At scale, and over time, the writer is not there. So
   something able to answer must travel: "we don't send telegrams, we send
   ambassadors".
4. Kay concedes other readings stay possible: "Bundling an interpreter for
   messages doesn't prevent the message from being submitted for other possible
   interpretations".

---
**meaning L368-371 · BODY · NEW-CASE A1,A2**

8. Kay's existence proof across time is the 1978 image: a universal computer
   "smaller than almost any media made in it", so the media can carry its own
   machine.


---
**meaning L379-385 · BODY · ABOVE -**

3. Interpretation is separate, comes later, and is plural: "interpreters are
   secondary, and perhaps essentially, varied". The source often cannot know the
   meaning. The seismometer does not know an earthquake is coming.
4. Labelling is not interpretation: "equating any such labeling with more
   general interpretation is a mistake."
5. Anything you add to enrich a message is more data. So is a bundled
   interpreter: "bytecode? - more data needing interpretation/execution".

---
**meaning L395-397 · BODY · ABOVE -**

9. Hickey blames the bungling of fact-keeping on "object- and place-oriented
   programming".


---
**meaning L418-426 · BODY · ABOVE -**
*Part one — the exchange › 1.4 Where they talked past each other*

**"Interpreter".** For Kay it covers everything from "these are bits" up to
"this is what was intended". Hickey cuts that in two. The bottom part
(format, names, time, who said it) Hickey counts as part of the data. The top part
(what it implies) Hickey calls interpretation and keeps apart. Kay never accepts
the cut. For Kay labels are more marks that need a process: "Even with
something like EDI or XML, think about what kinds of knowledge and process are
actually needed". Hickey makes the mirror move: an interpreter is more data.
Each saw the regress. Each aimed it at the other.


---
**meaning L427-434 · BODY · ABOVE -**

**The ambassador.** Kay's metaphor suggests a live party that negotiates.
Hickey attacks that strong reading: objects that "substitute for facts". In
this thread Kay did not claim substitution. Kay claimed the means of
interpretation should come along, and that other interpretations stay open. So
Hickey's attack partly misses. It does not wholly miss. Kay's wider position
(data structures "don't scale well"; "data" scales "terribly") leans the way
Hickey fears.


---
**meaning L445-446 · BODY · ABOVE -**
*Part one — the exchange › 1.5 What stayed unresolved*

3. **The bottom of the regress.** What interprets the interpreter? Kay: a good
   question. Hickey: shared words. Neither is an engineering answer.

---
**meaning L452-456 · BODY · ABOVE -**

5. **Whether meaning can be fixed at writing time at all.** Hickey's
   seismometer point: often the writer does not know what the record will come
   to mean. Kay's only reply was "how do they know they are even bits?" That
   answers the bottom of the stack, not the top.


---
**meaning L457-469 · BODY · ABOVE -**
*Part one — the exchange › 1.6 My reading: is this the axis under Sid's questions?*

### 1.6 My reading: is this the axis under Sid's questions?

**First, the axis is slightly misnamed.** "Meaning lives with the record, or
with the reader" is not where they differ. Both put meaning at the reader. Kay:
"It's not about the message but about the receiver". Hickey: "interpreters are
secondary, and perhaps essentially, varied". They differ on what the writer
owes the reader. Kay: a way to recover the intended interpretation, because the
reader cannot ask. Hickey: a well-labelled, fixed, harmless record, and nothing
that acts. So the axis is:

> What must travel with a record so that a reader who was not there can recover
> what was meant, and may that thing be active?


---
**meaning L562-569 · BODY · ABOVE -**
*Part one — the exchange › 1.7 What each would make of Sid's design (all INFERRED)*

**Kay.** Kay would first see a store of facts and say "data", the idea that
"scales terribly". Then Kay would notice three moves the AMA asks for. Interpreters
are findable, by pointer, in the same place. Work is matched, not sent: that is
Kay's "programming by message receiving", and Sid's match-don't-route is a
candidate answer to Kay's "what is the similar idea [to Linda] scaled for 40
years later?" And the medium aims to be understood and rebuilt from inside,
which is the Smalltalk and STEPS aim. Kay would press on three things.


---
**meaning L701-706 · BODY · ABOVE -**
*Part two — the people › 2.1 The capability camp: Hardy, Miller, Yee, Shapiro, Donnel*

- Lemmer-Webber on ActivityPub, as its co-editor. REPORTED (OcapPub): "Authorization is also
  not specified". The essay calls the last-minute `sharedInbox` addition "the primary
  mistake", because the receiving server decides who gets a message, which
  "breaks the actor model". The retrofit did not land, so Spritely now makes
  capability security "what falls out of Spritely's tech when you write a
  program in it."

---
**meaning L789-805 · BODY · ABOVE -**

**Lemmer-Webber on one store for everyone.** "How decentralized is Bluesky
really?" (2024), https://dustycloud.org/blog/how-decentralized-is-bluesky/.
Bluesky is the nearest running thing to "one heap, everyone reads it, apps
match what they want".

- REPORTED: "Bluesky does not utilize message passing, and instead operates in
  what I call a shared heap architecture. [...] there is no directed delivery;
  if you want to see replies which are relevant to your messages, you (or
  someone operating on behalf of you) had better sort through and know about
  every possible message".
- REPORTED: "this relay has a god's-eye knowledge base. [...] they must operate
  at the level of gods rather than mortals."
- REPORTED (follow-up): "What would happen if we had a million self-hosted users
  and five new users were added to the network? [...] Under the public shared
  heap model, it is 10,000,025 new messages sent!" And: "ATProto does not scale
  wide: it's a liability to add more fully participating nodes onto the
  network."

---
**meaning L909-914 · BODY · NEW-CASE A3**
*Part two — the people › 2.2 Kenton Varda: Protocol Buffers, Cap'n Proto, Sandstorm, *

- REPORTED (2021), on a race found even with one thread: "rather than fix
  the apps, we decided to fix the model." The fix: "When a storage write
  operation is in progress, any new outgoing network messages will be held back
  until the write has completed [...] it is impossible for anything else in the
  world to observe a premature confirmation." This is "no optimism" built into a
  runtime.

---
**meaning L1161-1165 · BODY · ABOVE -**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

- Claims, not truth. REPORTED (CACM 2014): "there is no "true population of
  Rome" but rather a "population of Rome as published by the city of Rome in
  2011."" And: "Wikidata intends to represent all views rather than choose one
  "true" claim." Also: "Wikidata does not automatically record provenance but
  does provide for the structural representation of references."

---
**meaning L1440-1450 · BODY · ABOVE -**
*Part two — the people › 2.7 Kay beyond the thread, STEPS, Worlds, and Ingalls*

**How far Kay got.** Kay hedged in 1993: "At some point it will be easier to have
it carry even more information about itself—enough so its specifications can be
'understood' and its configuration into your mix done by the more subtle
matching of inference." Nineteen years later, the STEPS final report (VPRI,
2012, p.18; the gatherer read this from a page image because the PDF's text
layer is scrambled, so check the image if a word matters): "A Significant
Problem Still To Be Solved—Massively scalable intermodule coordination and
communication has not been achieved via any means in personal or any other kind
of computing." The ambassador has not been built at scale, by Kay or anyone.
Hickey's side of the argument has running systems. Kay's side has a direction.


---
**meaning L1474-1481 · BODY · NEW-REASON A1,A2**

STEPS' end state, REPORTED (2012 report): "much of today's personal computing
systems [...] can be built with just 1000s to tens of 1000s of lines of code".
But: "Less Than Planned [...] (a) the comprehensive 'bottom engine room' for the
entire system". And they blame optimisation for pulling them off course: "This
'siren's song' [...] has created a project that is a bit different than the
original proposal". The part they did not finish is the bottom: the part Sid
calls the runtime.


---
**meaning L1489-1493 · BODY · NEW-REASON A3,E5**

- Commit checks the whole read set. REPORTED: "A commit from wchild to wparent
  is only allowed to happen if, at commit-time, all of the variables [...] that
  were read in wchild have the same values in wparent as they did when they were
  first read". Otherwise "some of the assignments that were made in wchild may
  have been based on values that are now out of date."

---
**meaning L1518-1520 · BODY · ABOVE -**

- "Operating System: An operating system is a collection of things that don't
  fit into a language. There shouldn't be one."


---
**meaning L1528-1534 · BODY · ABOVE -**

**Transfer.** A Smalltalk image is one substance holding tools and content, for
one person, on one machine, with no record of who did what. It shows that a
medium can be read and rebuilt from inside. It says little about many writers,
attribution, or deletion. Internal tension worth keeping: Kay wants fences
around meta-level change; Piumarta and Warth expose the implementation with no
fence.


---
**meaning L1558-1561 · BODY · NEW-REASON A3,E5**
*Part two — the people › 2.8 David Reed's pseudo-time, and Croquet*

- Reads leave a mark. "If a WRITE with a pseudotime less than an already
  executed READ arrives at a site, it cannot be executed, for that would cause
  the value previously returned by the READ to be incorrect. [...] each version
  records the maximum pseudotime of the READs that have accessed that version."

---
**meaning L1564-1566 · BODY · ABOVE A3**

- Against a centre: mutual exclusion needs "advance knowledge of all potentially
  conflicting users", in effect "some central registry".


---
**meaning L1576-1578 · BODY · NEW-REASON A3**

- The centre comes back. "The node containing the commit record becomes a
  critical resource".


---
**meaning L1592-1596 · BODY · NEW-REASON A1,C3**

INFERRED: the Kay camp, after twenty years, runs on one small stamper per
session that fixes order and time, and on deterministic computation so that
derived state is never stored or sent. That is Sid's gate and Sid's running
answers, arrived at from the objects side.


---
**meaning L1605-1610 · BODY · NEW-REASON A1**

- Determinism has to be policed. Their changelog adds a warning for `Date` in
  model code (2021) and a flag that "detects accidental writes into model state"
  (2024). A tool that claims to give a running answer will read a clock or a
  random number by accident. Something has to catch it, or "never stored, always
  re-derivable" is false for that tool.


---
**meaning L1654-1665 · BODY · ABOVE -**
*Part two — the people › 2.9 Joe Armstrong*

**A global store of functions.** REPORTED (erlang-questions, "Why do we need
modules at all?", 24 May 2011): "all functions have unique distinct names / all
functions have (lots of) meta data / all functions go into a global
(searchable) Key-value database". Metadata as data about the function:
"code|source|documentation|type signatures|revision history|authors". This is
Sid's "tools are facts". The sharpest objection in the thread came from Richard
O'Keefe: "How would a revised system *work* in a distributed world? … How much
of a shared sea-of-functions can you update before having to revert to
backups?" Armstrong, on when to bind: "I guess when developing a)
[resolve at call time] is useful but when you deploy code it should be b)
[resolve when built]".


---
**meaning L1713-1719 · BODY · ABOVE -**
*Part two — the people › 2.10 Self (David Ungar, Randall Smith)*

**Reasons.** REPORTED ("Self: The Power of Simplicity"): "No object in a
class-based system can be self-sufficient; another object (its class) is needed
to express its structure and behavior. This leads to a conceptually infinite
meta-regress… Prototypes eliminate meta-regress." They made a bet in 1987: "A
working system will provide the chance to discover whether class-like objects
would be so useful that programmers will create them without encouragement from
the language."

---
**meaning L1776-1784 · BODY · ABOVE -**
*Part two — the people › 2.11 Carl Hewitt*

**On "one truth".** REPORTED (same paper, Hewitt's principles for information
integration): "In practice integrated information is invariably inconsistent".
"Persistence. Information is collected and indexed and no original information
is lost." "Pluralism: Information is heterogeneous, overlapping and often
inconsistent. There is no central arbiter of truth." "Provenance: The provenance
of information is carefully tracked and recorded." And: "Sponsorship: Sponsors
provide resources for computation, i.e., processing, storage, and
communications."


---
**meaning L1785-1791 · BODY · ABOVE -**

This list is close to Sid's own. It differs on one line. For Hewitt there is "no
central arbiter of truth". INFERRED: Hewitt would accept one log and one order. Hewitt
would not accept "one truth" if it means one consistent set of beliefs. Layers,
and Wikidata's claims-not-truth, already give Sid plurality of belief. So the
challenge is mostly to the slogan: "one truth" should mean one record of who
said what and in what order, and nothing more.


---
**meaning L1792-1797 · BODY · ABOVE -**

**Against facts as the only substance.** Hewitt argued against Kowalski that
"computation in general cannot be subsumed by deduction", because arrival orders
"cannot be deduced from prior information by mathematical logic alone". Sid's
design already grants this: the runtime and the gate are not facts, and what
they decide is written down as facts.


---
**meaning L1876-1880 · BODY · ABOVE -**
*Part three — substrates › 3.1 Dynamicland's Realtalk, and Folk Computer*

**On one store.** INSTITUTIONAL (FAQ): "we imagine a decentralized network of
local communities, not a database serving millions of customers." "Every site
has its own independent physical copy of Realtalk. [...] There is no centralized
official version of Realtalk."


---
**meaning L1881-1889 · BODY · NEW-CASE A1,A2**

**On rebuilding from inside.** Realtalk is written in Realtalk, on posters in
the room. Folk went the other way under pressure. REPORTED (Rizwan, 2024): the
runtime was rewritten "3 or 4 times… every 5 months or so… because Folk starts
feeling too slow", and the new kernel became a static binary because "no one was
messing with the kernel in practice anyway". The second rewrite was "such a
breaking change" that the old line stopped. Folk could afford that because it
keeps almost nothing. Sid's store keeps everything, so its facts and tool bodies
must not depend on any one runtime.


---
**meaning L1890-1894 · BODY · ABOVE -**

**Transfer.** Same idea of matching. Opposite stance on memory, identity, and
scale: one room, no users, nothing kept. No system in this family has run
match-don't-name among parties who do not trust each other. Sid's would be the
first.


---
**meaning L1895-1903 · BODY · ABOVE -**
*Part three — substrates › 3.2 Linda (David Gelernter)*

### 3.2 Linda (David Gelernter)

**What Gelernter built.** Tuple space: processes put tuples in, and take them out by
pattern. REPORTED ("Generative Communication in Linda", 1985): "just as the
receiver has no prior knowledge about the sender, the sender has none about the
receiver." A tuple stays until taken: "If it is never removed by in( ) it will,
in the abstract, remain in TS forever." Matching is not ordered: "Some suspended
in( ) statement will receive it, but which is logically nondetermined."


---
**meaning L2091-2099 · BODY · ABOVE -**
*Part three — substrates › 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tri*

- The engineers: Roger Gregory, in Wolf: "Stiegler and Miller screwed up the
  entire thing. I had something that was within six months of shipping." The
  1999 release notes call the two-language build "almost too painful", and say
  the released code "is not considered to be usable as yet". Miller and
  co-authors admit the single docuverse never existed: "for the moment, each
  server is still an island with respect to the other servers". Their stated
  principle was many operators, not one: "Open entry of server providers [...] in
  order to make centralized control impossible."


---
**meaning L2100-2108 · BODY · ABOVE -**

**The Web's side.** REPORTED (Berners-Lee, "Web Architecture from 50,000 feet"):
"A fundamental compromise which allows the Web to scale (but created the
dangling link problem) was the architectural decision that links should be
fundamentally mono-directional." The 1989 proposal: "A new system must allow
existing systems to be linked together without requiring any central control or
coordination." Clay Shirky (1996): "in any heterogeneous system links have to be
one-directional, because bi-directional links would require massive coordination
in a way that would limit its scope."


---
**meaning L2124-2130 · BODY · NEW-CASE A1,C4**
*Part three — substrates › 3.6 An addition: Urbit, on the runtime that is not a fact*

- INSTITUTIONAL (Urbit docs, Arvo overview): "The current state is a pure
  function of its event log: a chronological record of every action the operating
  system has ever performed." It is "stacked on top of a frozen instruction set
  known as Nock." And the first events deliver the interpreter: "The history
  starts with a bootstrap sequence that delivers Arvo itself, first as an
  inscrutable kernel written in Nock, then as the self-compiling Hoon source code
  for that kernel."

---
**meaning L2131-2137 · BODY · NEW-REASON A1,X2**

- REPORTED (Yarvin and Wolfe-Pauly, "Toward a Frozen Operating System", 2017):
  "In Kelvin versioning, a version is an integer in degrees Kelvin. Absolute zero
  is frozen — no further updates are possible." Nock is "defined in a page of
  axioms that gzips to 340 bytes." The rule for what sits on top: "B must state
  the version of A it was developed against. A, when loading B, must state its
  own current version, and the warmest version of itself with which it's
  backward-compatible."

---
**meaning L2143-2152 · BODY · NEW-REASON A1,A2**

INFERRED: this is Kay's "smallest universal thing" made concrete, with a number
that counts down so it can only get harder to change. It answers the regress by
freezing a tiny bottom and letting everything above it arrive through the log.
It also shows the price. Even with a frozen bottom they broke continuity more
than once. For Sid: what the runtime implements can be small, written down, and
versioned by a rule like "each tool states the version of the body language it
was written against", even if the runtime itself is rebuilt a hundred times.
(Urbit's founder is a divisive figure. The engineering idea stands apart from
that.)


---
**meaning L2352-2362 · BODY · DISAGREES A3,C2,C3**
*Part four — Sid's questions, hung on the parts of the fact › (10) Order: two gates on one layer? what shares a partition?*

**My read.** Two gates never write one cell. What else must share an order with a
cell: facts offered together that must land together (Reed's "possibility"); the
policy facts that govern the write, or a revocation races the write it should stop
(Webstrates had a two-minute window of exactly this); and the grammar version the
write is checked against. Policy and grammar will often sit in a different
partition from the facts they govern. So each verdict must name the positions of
the policy and grammar partitions it checked against. It follows that "as of" is a
position per partition. One number is only possible with one sequencer for the
planet, which is Varda's vote counter. A list of length one costs nothing today.
Define "as of" as a list from the first record.


---
**meaning L2481-2490 · BODY · NEW-REASON A1,A2**
*Part four — Sid's questions, hung on the parts of the fact › (12) The runtime's version, and rebuilds*

### (12) The runtime's version, and rebuilds

**Camp.** Croquet puts a hash of the code into the identity of the session,
because "only users running the exact same source code" compute the same thing, and
it had to add runtime checks for code that reads a clock (2.8). Urbit: each layer
"must state the version" of the layer below that it was built against (3.6). Unison
lost the meaning of old hashes when its inference algorithm changed (2.3).
Cloudflare: alarms made before a certain day lack a field, for ever (2.2). Both
Kay and Hickey land here (1.7).


---
**rama L131-131 · BODY · NEW-REASON A3**
*Part one — What Rama's own reference says › Question by question › (10) Order: can two gates ever write one layer? What must sh*

- **CHECKED** `docs/15-pstates.md:41` — "a PState can only be updated by the ETL topology that owns it."

---
**rama L141-142 · BODY · NEW-REASON A3,C3**

Two gates, one layer: in Rama this cannot happen by accident. One topology owns a PState. One thread runs a partition. **IMPLIED:** a compare-and-set on a cell needs no lock; it is a read and a write in one event on the cell's task.


---
**rama L199-200 · BODY · NEW-REASON A2,E8**
*Part one — What Rama's own reference says › Question by question › (12) Start of a session: is the running runtime version and *

- **CHECKED** `docs/07-terminology.md:39` — every deploy makes a new "module instance… a specific deployment of a module associated with a specific set of code."
- **CHECKED** `docs/25-integrating.md:191` and `skill/task-globals.md:81` — code can read its module instance id: `context.getModuleInstanceInfo().getModuleInstanceId()`, from inside a task global.

---
**rama L201-201 · BODY · NEW-REASON A2**

- **CHECKED** `docs/22-backups.md:19-26` — backups include the module jar.

---
**rama L202-203 · BODY · NEW-REASON A2**

- **NOT IN THE REFERENCE:** a runtime call for the Rama version, the jar's hash, or the machine kind.


---
**rama L204-205 · BODY · NEW-REASON A2,E8**

**IMPLIED:** a gate can stamp each verdict with the module instance id that made it. Linking that id to a build is Sid's job: a constant baked into the jar, and a fact written at deploy time. Rama will not write "a rebuild happened" anywhere a topology can see.


---
**rama L255-256 · BODY · ABOVE -**
*Part one — What Rama's own reference says › Other items the brief named*

**A single cluster deployed worldwide.** **NOT IN THE REFERENCE.** The reference describes clusters "in a data center, on the cloud, or on your local machine" (`docs/03-first-module.md:151`). **IMPLIED, and worth asking RPL about directly:** every append waits for its in-sync followers, and a microbatch gate waits for every task group. Spread across continents, that puts wide-area round trips inside every admission. Kept in one region, it puts every user on Earth a wide-area trip from the gate. The reference gives no guidance either way.


---
**rama L321-322 · BODY · DISAGREES A3,E1**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(10) Order and one writer.** REPORTED: one topology writes a PState; partition by the entity; "This entire topology definition executes atomically – all the PState queries, operational transformational logic, and PState writes all happen together and nothing else can run on the task in between… Rama doesn't have explicit transactions because transactional behavior is automatic when computation is colocated with storage." (*Massively scalable collaborative text editor backend with Rama in 120 LOC*, 2025, https://blog.redplanetlabs.com/2025/04/01/massively-scalable-collaborative-text-editor-backend-with-rama-in-120-loc/). That post is the nearest thing he has written to Sid's gate. Each edit carries the version its author saw. The depot is hashed by document id. The version is simply the length of the document's edit list. One detail matters: when the version is stale, his gate does not refuse. It transforms the edit against what was missed and admits it.


---
**rama L329-330 · BODY · DISAGREES A3,P0,E1**

**(7) The gate's yes/no; refusals.** REPORTED, and it cuts against the premise: his master dataset had no gate beyond a type check. "There's nothing preventing a Person from having multiple ages… That's absolutely true… Here's the thing though: it doesn't matter… you can resolve this during a 'full recompute' when choosing the data to ship to the application layer." (*Thrift + Graphs*). INFERRED: he would say the raw fact is that this actor offered this value, expecting version 25. That is true forever. Which offer won is a view over offers. If the rule for winning has a bug, the offers are still there and the view is recomputed. So on Sid's question he lands firmly: keep every offer and every verdict, refusals included, because the gate is code, and code is where human faults come from.


---
**rama L331-332 · BODY · NEW-REASON A2,E8**

**(12) Runtime version.** Not addressed directly. INFERRED from human-fault tolerance: after a bad deploy you must find what the bad code wrote. That needs the code's identity on what it wrote.


---
**rama L348-349 · BODY · ABOVE -**
*Part two — Nathan Marz › 5. What resembles Sid's situation, and what differs*

- His clusters are per customer, in a datacenter: "each user will just have their own cluster" (HN 37153669; "user" there means a Rama customer). Nothing he has written covers one store for everyone.


---
**rama L354-354 · BODY · ABOVE -**
*Part two — Nathan Marz › 6. Who he disagrees with, on what*

- Kreps, still, on integration. Rama is the claim that one system can be log, index and compute. Kreps calls that hubris (Part three).

---
**rama L369-370 · BODY · NEW-REASON A1**
*Part three — The dissent: Jay Kreps › 2. His reasons, in his words*

On determinism. "If two identical, deterministic processes begin in the same state and get the same inputs in the same order, they will produce the same output and end in the same state." And: "Deterministic means that the processing isn't timing dependent and doesn't let any other 'out of band' input influence its results. For example a program whose output is influenced by the particular order of execution of threads or by a call to gettimeofday or some other non-repeatable thing is generally best considered as non-deterministic." (*The Log*)


---
**rama L379-380 · BODY · NEW-REASON A3**

On one writer. "The processor is always the single writer for its local partition… This means that a lot of the complexity of reasoning about concurrent modifications from different processors disappears." (*Why local state is a fundamental primitive in stream processing*, O'Reilly, 2014, https://www.oreilly.com/content/why-local-state-is-a-fundamental-primitive-in-stream-processing/)


---
**rama L391-391 · BODY · NEW-REASON A1,W1**
*Part three — The dissent: Jay Kreps › 3. Where he and Marz disagree*

1. **Two systems.** "The problem with the Lambda Architecture is that maintaining code that needs to produce the same result in two complex distributed systems is exactly as painful as it seems like it would be. I don't think this problem is fixable." His alternative: keep the log, run one stream job, and when the code changes "start a second instance of your stream processing job that starts processing from the beginning of the retained data, but direct this output data to a new output table. When the second job has caught up, switch the application to read from the new table."

---
**rama L395-396 · BODY · ABOVE -**

5. **One system or many.** Kreps, 2017: "databases are mostly about queries, and I don't think Kafka really benefits from trying to add any kind of random access lookups directly against the log… Each of these systems have their own pros and cons, and I think it's hubris to think you can do better than all of them in a single system." Rama is the opposite bet.


---
**rama L397-398 · BODY · NEW-CASE A1**

What happened next. On points 1 and 2 Kreps was right, and Marz's own system shows it. Twitter, where Lambda was first built, followed. Summingbird, Twitter's one-program-two-systems framework, is marked "status: retired" on GitHub (https://github.com/twitter/summingbird). In 2021 Twitter wrote: "We have a lambda architecture with both batch and real-time processing pipelines, built within the Summingbird Platform… we propose to build pipelines in kappa architecture to process the events in streaming-only mode… we remove batch components." They then found the stream results more accurate than batch, because "the original TSAR batch pipelines discard late events." (*Processing billions of events in real time at Twitter*, 2021, https://blog.x.com/engineering/en_us/topics/infrastructure/2021/processing-billions-of-events-in-real-time-at-twitter-, INSTITUTIONAL). Summingbird adds nothing else for Sid; I dropped it.


---
**rama L415-416 · BODY · ABOVE -**
*Part three — The dissent: Jay Kreps › 4. Which of Sid's questions he speaks to*

**ALSO OPEN.** He would push on "one substance for everything" and "one store". The log should be the simple shared thing. Asking it to also be the index for every question is what he calls hubris.


---
**skeptics L62-63 · BODY · DISAGREES A2**
*1. The ordinary default, question by question*

**(12) Runtime version and machine kind; rebuilds.** In deploy logs and as attributes on traces, not in the data (COMMON PRACTICE). Hamilton, REPORTED: "ensure that all changes produce an audit log record so it's clear what was changed".


---
**skeptics L94-95 · BODY · ABOVE -**
*2. The skeptics › 2.1 Michael Stonebraker with Joe Hellerstein (2005) and with*

On document stores and schema-later (2024), REPORTED: "The main differences between them seems to be JSON support and the fact that NoSQL vendors allow 'schema later' databases. But the SQL standard added a JSON data type and operations in 2016." "we believe that the two kinds of systems will soon be effectively identical."


---
**skeptics L96-97 · BODY · ABOVE -**

On graph and triple stores (2024), REPORTED: "RDF databases (aka triplestores) only model a directed graph with labeled edges. Since property graphs are more common and are a superset of RDF, we will only discuss" property graphs. Then the argument: "the key challenge these systems have to overcome is that it is possible to simulate a graph as a collection of tables: Node (node_id, node_data) Edge (node_id_1, node_id_2, edge_data). This means that RDBMSs are always an option to support graphs." And on distribution: "previous research shows that distributed algorithms rarely outperform single-node implementations because of communication costs".


---
**skeptics L100-101 · BODY · ABOVE -**

Their conclusion (2024), REPORTED: "Another wave of developers will claim that SQL and the RM are insufficient for emerging application domains. People will then propose new query languages and data models to overcome these problems. There is tremendous value in exploring new ideas and concepts for DBMSs (it is where we get new features for SQL)." "However, we do not expect these new data models to supplant the RM." And: "Another concern is the wasted effort of new projects reimplementing the same components that are not novel but necessary to have a production-ready DBMS". ([What Goes Around Comes Around... And Around..., SIGMOD Record, June 2024](https://db.cs.cmu.edu/papers/2024/whatgoesaround-sigmodrec2024.pdf))


---
**skeptics L105-105 · BODY · ABOVE -**

- A fact or triple store as the one substance. INFERRED: they would say a nine-part fact is one row in one table, and a relational engine is "always an option". Their test is Lesson 12: what is the large functional or performance win that tables plus an audit log cannot give? They dismiss triple stores in one sentence, which tells you how much weight they give the form.

---
**skeptics L111-112 · BODY · ABOVE -**

**5. Resemblance and difference.** Their evidence is from business data processing: forms, orders, warehouses. Sid's content (claims, evidence, model replies, reads) is closer to their "free text" exception than to their centre. They judge by market survival. Sid is asking what must be fixed so as not to be trapped. Those are different questions. Their "elephants" lesson is about adoption, which bears on Sid only if others must build on the store.


---
**skeptics L123-124 · BODY · ABOVE -**
*2. The skeptics › 2.3 Dan McKinley, "Choose Boring Technology" (2015)*

**What and why.** McKinley ran engineering at Etsy through the years when it cut its stack back down. REPORTED: "Let's say every company gets about three innovation tokens. You can spend these however you want, but the supply is fixed for a long while." "If you choose to write your own database, oh god, you're in trouble." On why boring wins: "the capabilities of these things are well understood. But more importantly, their failure modes are well understood." "for shiny new technology the magnitude of unknown unknowns is significantly larger, and this is important." On cost: "It is basically always the case that the long-term costs of keeping a system working reliably vastly exceed any inconveniences you encounter while building it." On the test to apply: "consider how you would solve your immediate problem without adding anything new." ([mcfunley.com](https://mcfunley.com/choose-boring-technology))


---
**skeptics L125-126 · BODY · ABOVE -**

**What McKinley would say to Sid.** INFERRED: the store, the gate, the matcher, and the runtime are each a token, and Rama is another. The essay's exemption is "Any of those choices might be sensible if you're a javascript consultancy, or a database company." Sid's product is the medium itself, so Sid is near the exemption. That lowers the force of the charge. It does not lower the operations cost McKinley names.


---
**skeptics L127-128 · BODY · ABOVE -**

**What differs.** McKinley writes for a company whose product is not its infrastructure. McKinley does not argue against new technology: "Choose New Technology, Sometimes."


---
**skeptics L137-138 · BODY · DISAGREES A3,C3,C2**
*3. Big-tech operational lessons › 3.1 Amazon*

What they built. Dynamo (2007) was a key-value store that never refuses a write: "an 'always writeable' data store". Conflicting versions were kept side by side and merged later by the application. It tracked versions with vector clocks and made the writer say what it had read. REPORTED: "In Dynamo, when a client wishes to update an object, it must specify which version it is updating. This is done by passing the context it obtained from an earlier read operation, which contains the vector clock information." Vector clocks grew, so they were cut. REPORTED: "When the number of (node, counter) pairs in the vector clock reaches a threshold (say 10), the oldest pair is removed from the clock. Clearly, this truncation scheme can lead to inefficiencies in reconciliation as the descendant relationships cannot be derived accurately. However, this problem has not surfaced in production and therefore this issue has not been thoroughly investigated." ([Dynamo, SOSP 2007](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf))


---
**skeptics L139-140 · BODY · ABOVE -**

What changed. DynamoDB (2012) kept the name and dropped the core of that design. Each partition has one leader chosen by consensus. REPORTED: "The replication group uses Multi-Paxos [14] for leader election and consensus." "Only the leader replica can serve write and strongly consistent read requests." No vector clocks. No merging by the application. The paper gives the reason itself, so this is REPORTED, not institutional: "Dynamo still carried the operational complexity of self-managed large database systems." "Teams had to become experts on various parts of the database service and the resulting operational complexity became a barrier to adoption." And the telling line: "Amazon engineers preferred to use these services instead of managing their own systems like Dynamo, even though the functionality of Dynamo was often better aligned with their applications' needs." ([Amazon DynamoDB, USENIX ATC 2022](https://www.usenix.org/system/files/atc22-elhemali.pdf)). Werner Vogels, ten years on, lists the requirements for the hosted service and says of "Manageable": "This was perhaps the most important requirement if we wanted a broad set of users to adopt the service." ([A Decade of Dynamo, 2017](https://www.allthingsdistributed.com/2017/10/a-decade-of-dynamo.html))


---
**skeptics L200-201 · BODY · CARRIED A3**
*3. Big-tech operational lessons › 3.3 Microsoft Orleans (Sergey Bykov, Phil Bernstein, and oth*

**The single-writer promise and its limit.** REPORTED: "In failure-free times, Orleans guarantees that an actor only has a single activation. However, when failures occur, this is only guaranteed eventually." During membership change "two activations of a single-activation actor" can exist. The trade is stated: "We made this tradeoff in favor of availability over consistency". The remedy: "If it is insufficient, the application can rely on external persistent storage to provide stronger data consistency." And the judgment: "We have found that relying on recovery and reconciliation in this way is simpler, more robust, and performs better than trying to maintain absolute accuracy in the directory and strict coherence in the local directory caches." The storage layer carries the version check. REPORTED from the docs: "Any attempt to perform a write operation when the storage provider detects an Etag constraint violation should cause the write Task to be faulted with transient error InconsistentStateException". ([Grain persistence docs](https://learn.microsoft.com/en-us/dotnet/orleans/grains/grain-persistence/))


---
**skeptics L206-206 · BODY · CARRIED A3**

- One writer. REPORTED principle, INFERRED application: "one writer" is a property of a healthy cluster. It is not a property of a cluster during failover. Orleans learned to let two writers exist for a moment and to make the durable layer refuse the second. If the gate's compare-and-set lives only in the gate's memory and not at the point where the record becomes durable, two gates during a membership change can both say yes. I did not check how Rama handles this. It is the question to put to Rama.

---
**skeptics L248-248 · BODY · DISAGREES A2**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (12) runtime version | In deploy logs and trace attributes | Hamilton: every change leaves an audit record; keep old formats readable (REPORTED) |

---
**skeptics L307-307 · BODY · ABOVE -**
*8. What each source implies for a second store*

- **Stonebraker and Pavlo, REPORTED.** "If organizations trust each other, they can run a shared distributed DBMS more efficiently". INFERRED: their advice for the rare institution is not to start a second store. Share the first.

---
**sync L136-140 · BODY · ABOVE -**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

**1. What they built and chose.** A replicated database for laptops that
connect rarely. Any server accepts a write at once as *tentative*. One server,
the *primary*, fixes the final order and makes writes *committed*. Every write
carries its own conflict test and its own repair. Sessions carry guarantees.


---
**sync L161-165 · BODY · NEW-REASON A1**

- Determinism must cover limits, not only code: "a merge procedure cannot
  access time-varying or server-specific 'environment' information such as the
  current system clock or server's name. Moreover, merge procedures that fail
  due to exceeding their limits on resource usage must fail deterministically."
  (same)

---
**sync L166-170 · BODY · ABOVE -**

- Tentative is shown, marked: "Tentative reservations are indicated as such on
  the display (by showing them grayed)." And the mark travels through queries:
  "Each tuple is tagged with a 2-bit characteristic vector identifying the set
  of views that contain it… Our query processor respects and propagates these
  bits, so that in the result of a query each tuple is tagged". (same)

---
**sync L192-197 · BODY · ABOVE -**

- The unused option: "Interestingly, the Bayou applications that have been
  built to date never select the commit-only option when reading data. This is
  because users always want to see updates that they have made, even if the
  update has not yet been committed." (Terry et al., "The Case for
  Non-transparent Replication: Examples from Bayou", IEEE Data Engineering
  Bulletin, 1998, http://csis.pace.edu/~marchese/CS865/Papers/terry_the-case-for-non.pdf)

---
**sync L210-213 · BODY · ABOVE -**

- Two applications, opposite choices on showing tentativeness: the calendar
  showed "tentatively scheduled meetings in a different color"; the mail reader
  "does not distinguish between tentative and committed data". (1998)


---
**sync L236-236 · BODY · ABOVE -**

- (10) One primary orders; everything else is tentative. REPORTED.

---
**sync L238-240 · BODY · NEW-REASON A1**

- (12) Determinism needs uniform resource bounds. REPORTED. For Sid: "re-derivable
  from the same reads" is only true relative to a tool version, a runtime
  version, and the limits that runtime enforces. INFERRED.

---
**sync L241-242 · BODY · ABOVE -**

- (14) and the no-optimism principle: show tentative state, marked; carry the
  mark through queries into results. REPORTED. Nobody chose commit-only. REPORTED.

---
**sync L253-261 · BODY · ABOVE -**

**6. Who they disagree with.** With replication transparency: "'replication
transparency', while a laudable goal for supporting legacy applications, is not
appropriate for a replicated storage system", and "Many of these systems
started with the goal of replication transparency but gradually ended up
adding hooks for applications to give input to the replication process."
(1998). Read for Sid: they disagree with any design that hides the difference
between confirmed and unconfirmed from the person. They would also disagree
with hiding unconfirmed state entirely.


---
**sync L293-296 · BODY · NEW-REASON A1**
*2. Section one: sync and multiplayer › 2.2 Convex (Sujay Jayakar, James Cowling, Jamie Turner; 2021*

- Determinism reaches the scheduler: "determinism also requires that our
  runtime is deterministic. For example, if a query function issues two
  db.get() calls concurrently, we need to ensure that Promise.race returns the
  same result every time".

---
**sync L337-338 · BODY · NEW-REASON A1**

- An admitted limit: "Queries currently fully reexecute". (object-sync-engine)


---
**sync L362-364 · BODY · NEW-REASON A1,A2**

- (12) Determinism is a property of the runtime, down to promise scheduling.
  REPORTED. So a crossing that claims "re-derivable" should name the runtime
  build. INFERRED.

---
**sync L387-392 · BODY · NEW-REASON A1,E3**
*2. Section one: sync and multiplayer › 2.3 Croquet and TeaTime (David A. Smith, David P. Reed, Alan*

**1. What they built and chose.** Replicated deterministic computation. Every
participant runs the same model code. Only events from outside (a person's
input) travel. A reflector stamps and orders them. Everything else is
recomputed identically everywhere. This is Sid's split between running answers
and offers, built as a product.


---
**sync L401-406 · BODY · NEW-REASON A1,A2**

- The session is bound to the code: "A session id is created from the given
  session `name` and `options`, and a hash of all the registered Model classes
  and Constants. This ensures that only users running the exact same source
  code end up in the same session, which is a prerequisite for perfectly
  synchronized computation." (Croquet client library source, `session.js`,
  github.com/croquet/croquet, read 2026-09-20; JSDoc link markup removed)

---
**sync L418-423 · BODY · NEW-REASON A1**

- Multisynq docs (the current successor,
  https://docs.multisynq.io/tutorials/model-view-synchronizer and the snapshots
  tutorial): "The view can read from the model, but can't write to it
  directly." Snapshots let new users join "without replaying the entire event
  history".


---
**sync L430-438 · BODY · NEW-CASE A1,C1**

- Code as identity has a price: "whenever we change the model code, a new
  session is created… The old session state becomes inaccessible, because for
  one we cannot know if the new code will work with the old state, but more
  importantly, every client in the session needs to execute exactly the same
  code to ensure determinism." The remedy they had to add: "we need to use
  Croquet's explicit persistence. An app can call `persistSession()` with some
  JSON data". (multiblaster README) The session object now carries three ids:
  `id`, `persistentId`, `versionId`.


---
**sync L454-456 · BODY · ABOVE A1**

- (13) Pure recomputation from the event log is too slow to join; they
  snapshot. REPORTED. I report this without endorsing it for Sid: the project
  treats stored derived state as a red flag to be examined, not adopted.

---
**sync L459-463 · BODY · ABOVE -**

**5. Resemblance and difference.** Same split of "re-derivable" from "not
re-derivable". Same single stamping authority. Different: a Croquet session is
small and short-lived; its event log is not an audit record; identity and
permissions are outside it.


---
**sync L473-477 · BODY · DISAGREES A3,C6**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

**1. What they built and chose.** A multiplayer design tool. One server process
per open file is the authority. Conflicts resolve by last writer wins, per
property, per object. Not operational transform, and by their own account not a
true CRDT.


---
**sync L482-485 · BODY · NEW-REASON A3**

- "Figma isn't using true CRDTs though. CRDTs are designed for decentralized
  systems where there is no single central authority to decide what the final
  state should be. There is some unavoidable performance and memory overhead
  with doing this."

---
**sync L486-490 · BODY · DISAGREES A3,C6**

- "A conflict happens when two clients change the same property on the same
  object, in which case the document will just end up with the last value that
  was sent to the server. This approach is similar to a last-writer-wins
  register in CRDT literature except we don't need a timestamp because the
  server can define the order of events."

---
**sync L497-499 · BODY · ABOVE -**

- Unconfirmed local changes win on the local screen: "So we want to discard
  incoming changes from the server that conflict with unacknowledged property
  changes."

---
**sync L542-547 · BODY · ABOVE -**

- *A second algorithm for a second kind of value (2025).* For code layers they
  did not reuse last-writer-wins: "we used the Eg-walker algorithm to build the
  multiplayer collaboration service for code layers … The server reconciles
  simultaneous edits from all active clients using Eg-walker." (Burke and Kern,
  "Canvas, meet code", 2025-06-25, https://www.figma.com/blog/building-figmas-code-layers/)


---
**sync L573-575 · BODY · ABOVE -**

- Above the table: one conflict rule did not fit all kinds of value; text
  needed a causal event graph. REPORTED.


---
**sync L582-585 · BODY · ABOVE -**

**6. Who they disagree with.** With decentralised CRDTs as overhead without
benefit once a server exists. In 2025 the stated objection to CRDTs for text is
memory, not correctness. With their own earlier assumption of one global order.


---
**sync L610-617 · BODY · ABOVE -**
*2. Section one: sync and multiplayer › 2.5 Linear (Tuomas Artman)*

- The local database holds only confirmed state: "transactions will never
  directly modify the tables in the local database! Instead, they only alter
  in-memory models … only after receiving the corresponding delta packages from
  the server does the local models get updated." The reason: "the local
  database is a subset of the server database (the SSOT), and it cannot contain
  changes that have not been approved by the server. If the server rejects the
  transaction, modifying the model tables prematurely could make it difficult
  and error-prone to revert."

---
**sync L637-641 · BODY · ABOVE -**

**5–6. Resemblance, difference, disagreement.** Same single orderer and same
refusal to let unconfirmed state into the durable copy. Different: the screen
still shows the unconfirmed change. Disagrees with CRDTs on partial versus
total order.


---
**sync L653-655 · BODY · ABOVE -**
*2. Section one: sync and multiplayer › 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)*

- Two states, named: "local changes to the space in a client are immediately
  (optimistically) visible … We call these changes speculative, as opposed to
  canonical." ("How Replicache Works", https://doc.replicache.dev/concepts/how-it-works)

---
**sync L656-658 · BODY · ABOVE A1**

- The server may decide differently: "the push endpoint is not necessarily
  expected to compute the same result that the mutator on the client did. This
  is a feature." (same)

---
**sync L659-663 · BODY · ABOVE -**

- The reconcile: "hidden from the application's view, it rewinds the state of
  the Client View to the last version it got from the server, applies the patch
  … and then replays any pending mutations on top. It then atomically reveals
  this new state to the app". And: "Replicache is modeled under the hood like
  git." (same)

---
**sync L694-698 · BODY · ABOVE -**

- The premise Sid denies, in their words: "This is just a matter of physics –
  information can only travel so fast. If you want instantaneously responsive
  UI, this means you can't wait for the server – changes have to happen
  locally, on the client." (same)


---
**sync L736-738 · BODY · ABOVE -**

- (4) The server need not reach the client's result. REPORTED. So a speculative
  result is never evidence of anything. INFERRED.


---
**sync L744-747 · BODY · ABOVE -**

**6. Who they disagree with.** With CRDTs (no place for rejection; conflict
policy belongs in application code). Inside Rocicorp: global version versus row
version. With Sid's premise, directly, on physics.


---
**sync L786-790 · BODY · ABOVE -**
*2. Section one: sync and multiplayer › 2.7 tldraw sync (Steve Ruiz)*

**5–6.** Resembles Sid's gate (server authority, validation on every write).
Differs on purpose: "tldraw sync is not a general purpose real-time data
solution … we expect tldraw sync to work alongside your existing data layers."
That is a direct refusal of "one substance for everything".


---
**sync L1026-1028 · BODY · NEW-REASON A1**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- Derived versus asserted shows up in diffs: "One challenge we encountered was
  diff that distinguish manually-edited cells versus recalculated formula
  result cells."

---
**sync L1104-1106 · BODY · ABOVE -**

- "The key difference between traditional systems and local-first systems is
  not an absence of servers, but a change in their responsibilities: they are
  in a supporting role, not the source of truth."

---
**sync L1143-1153 · BODY · ABOVE -**

**5. Resemblance and difference.** They want for people what Sid wants: history
as material, private layers over a shared base, agents as visible
collaborators. They refuse Sid's centre on principle, and they have spent years
paying for that refusal in exactly the places Sid's gate is cheap: ordering,
permission, rejection, revocation, deletion.

**6. Who they disagree with.** With the cloud as source of truth. With Git's
model for non-programmers. With consensus as a fix for backdating: "fairly
counter to the local-first ethos … we currently consider it a last resort."
With content addressing for live documents.


---
**sync L1214-1218 · BODY · ABOVE -**
*2. Section one: sync and multiplayer › 2.10 Yjs (Kevin Jahns; 2015–now)*

**5–6.** Different from Sid in every premise (no centre, small documents, size
above all). The transfer is the regret: compactness was bought by leaving out
the envelope. He disagrees with central-server designs on viability without a
centre, and with Automerge on keeping everything.


---
**sync L1255-1263 · BODY · NEW-CASE A1**
*2. Section one: sync and multiplayer › 2.11 Seph Gentle (ShareDB, Google Wave, diamond-types, Eg-wa*

**3. What he changed or regretted.** The header of his own internals document:
"This was written for an earlier version of diamond types when I persisted the
merge structure like yjs and automerge do. This has much worse performance when
there are no concurrent changes, and a bigger file size." On Google Wave's
federation over OT: "We got it working, kinda, but it was complex and buggy…
But it never really worked." On unbounded growth: "Can you ever delete that
data? Probably not." ("I was wrong. CRDTs are the future", 2020,
https://josephg.com/blog/crdts-are-the-future/)


---
**sync L1280-1283 · BODY · NEW-REASON A1**

**5–6.** His events are keystrokes, not facts with authors; he has no gate. He
disagrees with Yjs and Automerge on persisting merge state, with his own OT
past on needing a centre, and would accept Sid's centre as the easy case.


---
**sync L1291-1294 · BODY · NEW-REASON A3**
*2. Section one: sync and multiplayer › 2.12 Matthew Weidner (CRDT survey, Fugue, Collabs, list-posi*

- "it is ironic to see that, in the centralized model used by this blog post,
  CRDTs and OT are merely optimizations over server reconciliation, which is
  straightforward and completely flexible." ("Architectures for Central Server
  Collaboration", 2024, https://mattweidner.com/2024/06/04/server-architectures.html)

---
**sync L1328-1336 · BODY · ABOVE -**

- The cost of strict no-optimism, stated exactly. One strategy: "if a client
  possesses pending local operations, it refuses to process remote operations.
  Instead, the client waits until the server has acknowledged all of its
  pending local operations". Its failure: "this strategy can block updates
  indefinitely: if a client creates new operations faster than the server
  acknowledges them—e.g., by typing rapidly on a high-latency connection—then
  that client will never display updates from the server." REPORTED. Sid's rule
  is the mirror image (wait for the gate before showing one's own act), and the
  same arithmetic applies to an agent writing at machine rate. INFERRED.

---
**sync L1594-1598 · BODY · ABOVE -**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

- *Key handling still blocks.* "Clients don't currently deal well with latency.
  In order for NIP 46 to work smoothly, clients will have to implement better
  loading, debouncing, optimistic updates, publish status, and 'undo'."
  (hodlbod, "Key Management is a Blocker", 2024)


---
**sync L1686-1688 · BODY · ABOVE -**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

- *Earthstar's list of SSB's faults:* "Does not allow editing or deletion… /
  Cannot reuse the same identity between many devices… / Uses an append only
  log, whereas Earthstar uses a key-value database."

---
**sync L1695-1698 · BODY · NEW-CASE A3,C6**

- *Forks.* When one key signs two different messages with the same number, SSB
  treats the feed as dead. Lavoie's analysis: such logs "do not provide
  eventual consistency when malicious participants fork their logs". The
  remedies record the fork with proof instead of rejecting the feed.

---
**sync L1719-1720 · BODY · NEW-REASON A3,C6**

- (10) Two writers on one log is a fork, and a fork kills the log unless forks
  are a recorded state. REPORTED.

---
**sync L1728-1736 · BODY · ABOVE -**

**5–6.** SSB is the nearest thing to Sid's never-rewrite rule that lived for
years among real users, and its builders are the most direct witnesses to what
that rule costs when ids, order and integrity are all tied to the bytes. Sid's
gate removes their hardest problems (forks, multi-device, onboarding by full
replay). It does not remove deletion or format ageing. Tarr, quoted in Gabby
Grove, insisted the author stay on every entry: "It should always be known
which key-pair created a signature". Staltz: "I fundamentally don't trust any
system that has an admin with sudo powers."


---
**sync L1910-1913 · BODY · DISAGREES A3**
*3. Section two: versioning › 3.2 Jujutsu, and Mercurial's changeset evolution (Martin von*

- Writes never fail; divergence is a value: "The operation cannot fail to
  commit (except for disk failures and such). It is left for the next command
  to notice if there were divergent operations." A contested pointer is
  recorded as "moved from A to B or C". (concurrency)

---
**sync L1941-1954 · BODY · DISAGREES A3,C1,C7**

**4. Which questions.** (1) Keep a stable random id for the thing that evolves
and a separate id for each immutable state of it. REPORTED. If a content hash
is used, *everything* that distinguishes two records must be inside it.
REPORTED as a bug they live with. (6) Write "replaces" down, with who, when,
and what kind of replacement. REPORTED, the last as a wish. (12) Log each
operation with user, host, time and command. REPORTED. (17) Give the origin a
fixed, recognisable id that is the same in every store and is never stored.
REPORTED. (10) jj takes the opposite road to compare-and-set: accept both,
record the divergence, let the next reader resolve. REPORTED. It suits a
single person's repository; under machine-rate agents it would turn every
contended cell into a standing conflict. INFERRED. (13) An envelope field that
some writers do not understand gets lost unless preservation is a rule for all
writers. REPORTED.


---
**sync L1982-1984 · BODY · ABOVE -**
*3. Section two: versioning › 3.3 Fossil (D. Richard Hipp; 2006–now)*

- Users and permissions are outside the permanent record: local state,
  including "authorized users", "is not versioned and is not synchronized with
  the global state" and is "not intended to be enduring." (fileformat)

---
**sync L2606-2607 · BODY · DISAGREES A3**
*4. Question by question › (10) Order: can two gates write one layer? What must share a*

- Two writers on one log is a fork: SSB. jj accepts divergence and records it.
  Upwelling wanted a server to approve merges one at a time. REPORTED.

---
**sync L2863-2876 · BODY · NEW-REASON A1,A2**
*4. Question by question › (12) Start of a session: is the runtime version and the kind*

**What the camp says.** Croquet puts a hash of the code into the session id,
"a prerequisite for perfectly synchronized computation", and had to add a
code-free `persistentId` and a hand-written projection to survive code changes.
REPORTED. Bayou: determinism needs uniform resource limits. Convex: determinism
reaches promise scheduling. REPORTED. Figma proved its log by byte-identical
replay across hundreds of thousands of files. REPORTED. Patchwork logs the tool
with each open, and a colleague asks for the tool's version too. REPORTED. jj
logs user, host, time and command per operation. REPORTED. Replicache sends the
schema version both ways, and two generations coexist during migration.
REPORTED. Weidner: a replica identity belongs to one session and must not be
reused after a crash. REPORTED. Git's side index has two live generations.
REPORTED. Automerge: a first record tied to initialisation code is fragile.
REPORTED.


---
**sync L2981-2986 · BODY · ABOVE -**
*5. The group › 5.1 The voices that matter most, who I dropped, who is missi*

1. **Doug Terry and the Bayou papers.** The nearest ancestor of Sid's whole
   shape: offers that carry an expected state, one orderer, a session that
   knows what it has read, and tentativeness that travels through queries into
   every result row. They also give a rare kind of evidence. They built the
   principled option, a commit-only view, and then reported that no application
   ever chose it. If Sid reads one set of papers, these.

---
**sync L3011-3012 · BODY · ABOVE -**

- Wallace, Boodman, Weidner (the server decides; CRDTs are overhead) against
  Ink & Switch (the server is "in a supporting role, not the source of truth").

---
**sync L3016-3017 · BODY · DISAGREES A3**

- jj (a write never fails; divergence is a stored value) against everyone with
  compare-and-set.

---
**sync L3020-3021 · BODY · ABOVE -**

- Terry (show the unconfirmed, marked) against Sid's rule read literally.


---
**sync L3214-3217 · BODY · ABOVE -**
*6. The second store: what each source implies*

- **Exit.** A person's layer, exported, is a small second store. If that export
  is defined early, "one store for the planet" has the answer AT Protocol calls
  credible exit. If it is not, the camp will not believe the rest.


