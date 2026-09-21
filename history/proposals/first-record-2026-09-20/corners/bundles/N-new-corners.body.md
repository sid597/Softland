# N new corners: body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L197-197 · BODY · NEW-CORNER C1**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- INFERRED, from unique identity and upsert. Client-minted random ids solve clashing. They do not solve *sameness*. Two agents that ingest the same paper will mint two entities for it. Datomic's tool for this is a unique-identity attribute: "If a transaction specifies a unique identity for a temporary id, and that unique identity already exists in the database, then that temporary id will resolve to the existing entity." [D-IDENT]. For a field seeded from ten million papers, the convention to fix early is which outside keys (DOI, arXiv id, ORCID) are unique identities that the gate checks, so that the second ingest lands on the first entity.

---
**datalog L389-390 · BODY · NEW-CORNER C3**
*3. Nubank: Datomic lived with, at scale › 3.3 What they regret*

1. **An owner was not written on every saying.** "one thing that we do not add and I would have liked to add, a customer identifier. Because, if every transaction had an identifier that could point to the actual customer that owns that data, things like splitting databases for sharding would have been much, much easier. That's a small detail, but something I would like to get better in the beginning." [N-QCON]. The InfoQ transcript does not label the speaker. It is one of the two presenters, Edward Wible or Rafael Ferreira.


---
**datalog L405-405 · BODY · NEW-CORNER C5,C3**
*3. Nubank: Datomic lived with, at scale › 3.4 Which questions they speak to*

- **(16) layer.** INFERRED from regret 1. Sid's layer slot is close to the identifier Nubank wishes it had written: it says whose context a fact lives in. Where Sid's design differs is the base layer. A fact *about* a person that sits in the shared base carries no mark of whose it is. Nubank's regret was about splitting later. The same missing mark would make erasure by subject hard. If Sid wants either, the mark has to be there from the first record.

---
**datalog L487-487 · BODY · NEW-CORNER E3**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.4 Which questions they speak to*

- **(11) when, and valid time.** REPORTED. XTDB's disagreement with Datomic is that the time a thing was true is not the time the store learned it, and that both belong on every record. For a field seeded from papers this is live: publication dates, retractions of papers, corrections that arrive years late. Datomic's answer is that valid time is an ordinary attribute. XTDB's answer, in my paraphrase, is that it must be an axis on every record, because otherwise every application rebuilds it, and badly. Waeselynck, a Datomic user, ends up agreeing with XTDB on the diagnosis. [VAL17]

---
**datalog L568-569 · BODY · NEW-CORNER C3,C5**
*6. The group: who matters most, who I dropped, who is missin*

3. **Nubank.** Eleven years on the design, under audit law, at a scale near the one Sid holds in mind. Every regret they name is about something not written at the moment of the act: the owner, the origin, the refused request. That is Sid's question, answered from the far end.


---
**frontiers L292-292 · BODY · NEW-CORNER C5,C8**
*5. Noria and what followed (Jon Gjengset, Malte Schwarzkopf) › 5.4 Which questions they speak to*

- **(9) delete, backups included.** REPORTED: per-owner physical unit, per-owner key, delete the unit or the key, revoke derived data through the dataflow. INFERRED for Sid: a person's durable layer is the natural unit of ownership. If every fact in that layer is encrypted under that person's key from the first record, then erasure, backups included, is one key deletion, and the log is never rewritten. This cannot be added later: facts written in the clear stay in the clear in every backup. Their second point also transfers: a fact with no owner is a compliance bug. "By whom" and "layer" together must always resolve to an owner.

---
**frontiers L355-356 · BODY · NEW-CORNER C4**
*7. Question by question: what this camp says*

**(17) First facts.** Thin here. INFERRED: every timeline has a least time (REPORTED in the formalism: "Each `T` contains a minimal time `t0`"). The first facts are the facts at that time. The grammar of "grammar" and the policy that lets the gate write must exist at t0 or nothing can be admitted. If a second store starts with the same t0 facts under the same ids, joining two stores later is a reclocking problem and not also an identity problem.


---
**log L100-101 · BODY · NEW-CORNER -**
*Voice by voice › 1. Pat Helland*

**6. Disagreements.** With his own earlier self and the two-phase-commit tradition. With relational normalization for immutable data ("Normalization Is for Sissies" is his own heading). By name, with Werner Vogels and the Dynamo paper for discussing eventual consistency in terms of storage reads and writes: "Storage systems alone cannot provide the commutativity we need" (*Building on Quicksand*, 2009). His point: the record must carry the operation the person meant, not just a resulting value.


---
**log L135-135 · BODY · NEW-CORNER E5**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- (11 based-on), (4) REPORTED: a write carries the versions it read, and is void if any moved. INFERRED: Tango records reads for *validation*. Sid records reads for *provenance*, and the gate validates only one of them (the cell's expected version). The record should say which reads were checked by the gate and which were only declared.

---
**log L160-160 · BODY · NEW-CORNER -**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- The three ids: "Let's say every message has 3 ids. 1 is its id. Another is correlation the last it causation. The rules are quite simple. If you are responding to a message, you copy its correlation id as your correlation id, its message id is your causation id. This allows you to see an entire conversation (correlation id) or to see what causes what (causation id)." (event-store Google Group, 29 March 2015.)

---
**log L178-178 · BODY · NEW-CORNER -**

- (11 because-of) REPORTED: carry both the conversation id and the immediate cause, and let infrastructure fill them. Note the mismatch: Sid's "because of" (the fact that started the chain) is Young's *correlation* id. Young's *causation* id is the immediate parent. Sid's envelope has the first and not the second.

---
**log L197-197 · BODY · NEW-CORNER -**
*Voice by voice › 4. Martin Kleppmann*

- Causation: "The original event ID is included in all of these generated events so that their origin can be traced." (same.)

---
**log L413-414 · BODY · NEW-CORNER C8**
*Question by question: what this camp would say › (8) By whom*

**For the first record (INFERRED).** "By whom" should be stamped by the gate from the authenticated channel, for the same reason "when" is: an offer's own claim about itself is not evidence. A signature by the actor is the stronger form and the only one that survives a move to a second store, but it is heavier. On a person's agent versus the person, and on where "acts for" lives, this camp has nothing. Look to the group that has Zanzibar.


---
**log L444-445 · BODY · NEW-CORNER -**
*Question by question: what this camp would say › (11) Based on: is every read listed?*

- *Tracing origin.* Kleppmann's online event processing: "The original event ID is included in all of these generated events so that their origin can be traced."


---
**log L448-449 · BODY · NEW-CORNER E5,E6**

**For the first record (INFERRED).** Sid's based-on is provenance, a fourth purpose that nobody in this camp stores at scale, and the gate validates only one read (the cell's expected version). So each listed read should say who vouches for it: checked by the gate, observed by the runtime, or declared by the actor. A read that only made a tool fire is a cause, not a dependency, and belongs with because-of. To keep size down, use the camp's tricks: a pattern plus an as-of instead of a list; direct reads only; one frontier for "everything I could have known"; and one shared read record for the many facts that come out of one context. The crossing fact already is such a record: a model's offer can point at the single "what the model was given" fact rather than repeat its reads. For an anchor outside the store, record enough to survive the outside changing: the identifier, and a hash or copy of what was read.


---
**log L464-465 · BODY · NEW-CORNER -**
*Question by question: what this camp would say › (11) Because of: always filled; empty only when it starts a *

REPORTED. Young's rule is the practice everywhere: "every message has 3 ids", and a reply copies the conversation id and sets its cause to the id of the message it answers. Infrastructure fills them, not application code; Event Store's projections "honor both the correlationId and causationId patterns for any events it produces internally". Kleppmann's pipeline does the same with one id.


---
**log L466-467 · BODY · NEW-CORNER -**

**For the first record (INFERRED).** Sid's because-of is "the fact whose landing started the chain". That is Young's correlation id. The immediate cause, Young's causation id, has no slot in the nine parts unless based-on marks which read was the trigger. The camp carries both, because they answer different questions: "show me the whole conversation" and "what caused what". Young uses the immediate cause to explain corrections. Filled always, and by the runtime.


---
**log L570-570 · BODY · NEW-CORNER E1,C4**
*The second store: what each source implies*

- **Certificate Transparency.** Many stores is the normal case. There is no order across logs, ever. Each log's identity is a key, and each receipt names its log. Safety is a client rule: a quorum of independent logs. Logs are born and retired on a schedule. INFERRED: the cheapest insurance is a store id on every verdict. In a one-store world it is a constant and costs almost nothing, but if it is left out it is missing from every early fact for good.

---
**meaning L107-111 · BODY · NEW-CORNER -**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

**Turn 3 — Kay, 11946532, 15:44**

> What is "data" without an interpreter (and when we send "data" somewhere, how
> can we send it so its meaning is preserved?)


---
**meaning L204-210 · BODY · NEW-CORNER -**

- 11948601, to someone who asked whether the interpreter even needs to travel
  when bandwidth is everywhere: "How can you find it? The association between
  "patterns" and interpretation becomes an "object" when this is part of the
  larger scheme. When you've just got bits and you send them somewhere, you
  don't even have "data" anymore. Even with something like EDI or XML, think
  about what kinds of knowledge and process are actually needed to even do the
  simplest things."

---
**meaning L358-360 · BODY · NEW-CORNER -**
*Part one — the exchange › 1.3 What each actually claimed*

5. The hard part is finding the interpreter: "How can you find it?" The pairing
   of pattern and interpretation, made part of the scheme, is what Kay calls an
   object.

---
**meaning L512-513 · BODY · NEW-CORNER -**
*Part one — the exchange › 1.6 My reading: is this the axis under Sid's questions?*

2. The thing pointed at can be run by a runtime that does not exist yet. That
   is row (12), and the runtime that is not a fact.

---
**meaning L570-577 · BODY · NEW-CORNER -**
*Part one — the exchange › 1.7 What each would make of Sid's design (all INFERRED)*

- *The runtime that is not a fact.* This is Kay's regress. Tool bodies are facts,
  but in what language, run by what? The store will outlive a hundred runtimes.
  Kay's direction: make the bottom turtle tiny, universal, and written down, so
  any future runtime is one more implementation of it. The 1978 image came back
  because its machine was small and specified, and the image carried the rest.
  INFERRED: the first facts (17) should include, or anchor to, the definition of
  the body language. "Re-derivable from the same reads" is only true relative
  to that definition.

---
**meaning L1181-1185 · BODY · NEW-CORNER C6**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

- INFERRED: that is three different things that Sid's "superseded = same cell,
  later version" folds into one. A newer value. A value that was wrong. A value
  that was right then. If 37 replaces 25 without saying which, no reader can
  tell later. Wikidata had to add the reason. So for (6): keep "replacing 25" on
  37, and keep *why*.

---
**meaning L1186-1191 · BODY · NEW-CORNER -**

- Merges. INSTITUTIONAL (Help:Redirects): "Under no circumstances should
  redirects be deleted or repurposed for another object. Deleting them would
  mean invalidating possible references". Ids for life will still be minted
  twice for one thing, often, when ten million papers arrive. Pointers to the
  losing id cannot be rewritten in a store that never rewrites. So how a merge
  is said, and how every reader resolves it, is a day-one convention.

---
**meaning L2004-2009 · BODY · NEW-CORNER C4**
*Part three — substrates › 3.4 Jonathan Edwards (Subtext, schema change)*

INFERRED: Sid's grammars are facts with versions. A new version that only holds
the new shape is a before-and-after record. Years later no tool can tell whether
a field was renamed or replaced, and so cannot read old values through the new
grammar. If a grammar version also says *what was done* to the previous one,
that stays possible. This is one more thing that cannot be added afterwards.


---
**meaning L2246-2254 · BODY · NEW-CORNER C4**
*Part four — Sid's questions, hung on the parts of the fact › (3) Key: a word, or an id with its name and shape as facts?*

**My read.** Store the id. Keep the name as a fact about the key: a public
nickname in base, and each person's own word in their layer, which "nearest layer
wins" resolves for free. Two rules come with it, each from two unrelated systems.
A key's kind of value never changes; a breaking change of shape is a new key
(Wikidata, Datomic). A grammar grows only by adding (Hickey). Two extensions from
this camp: give the *fields inside a value* ids too, never reused (protobuf,
Edwards). And let a new grammar version say what was done to the old one, not only
what it now is (Edwards). Both are gone forever if left out.


---
**meaning L2298-2305 · BODY · NEW-CORNER C8**
*Part four — Sid's questions, hung on the parts of the fact › (8) By whom*

**My read.** "By whom" is a chain. The gate vouches only for the head: the party
whose authenticated channel the offer arrived on. An agent is itself, never the
person. "Acts for" is a grant fact from the person, which the agent's offers cite.
Person and instrument are then both on every fact, which is exactly what Wikidata
lost. The signing decision has to be made before the first record. If offers are
not signed when made, the authorship of every early fact rests on the gate's word
for ever, and no later change can fix that.


---
**meaning L2374-2382 · BODY · NEW-CORNER E5,E6**
*Part four — Sid's questions, hung on the parts of the fact › (11) Based on: is every read listed?*

**My read.** List what was *used*, mechanically and completely, for every tool run
and every crossing. The runtime knows this, and it is cheap, because a pattern
read is one entry, not one per match. Mark each entry with who filled it: the
actor, or the runtime. The read that made a tool fire is "because of", not "based
on"; PROV keeps the trigger apart from usage in the same way ("started by an
entity, known as trigger", PROV-DM §5.1.6). A person's usage records belong in
that person's layer. That is the only way to square "what was I looking at
yesterday" with Xanadu's rule.


---
**rama L81-82 · BODY · NEW-CORNER C4**
*Part one — What Rama's own reference says › Question by question › (17) The first facts: what ids, who writes them, the same in*

**IMPLIED:** the first facts are ordinary appends made by some client after launch. If they sit at the head of the offers depot, and the depot is never trimmed, every topology ever added later can replay them. Whether they are "the same in every store" is a convention outside Rama. One Rama fact bears on it: the gate needs grammar and policy facts in order to admit anything, so the very first appends must be admitted by rules that are not yet facts. The reference gives no pattern for that. Module code is the only place those first rules can live.


---
**rama L110-111 · BODY · NEW-CORNER C8**
*Part one — What Rama's own reference says › Question by question › (8) By whom: who checks it? Is a person's agent itself, or t*

**IMPLIED:** Rama treats everything that can reach the cluster as one trust domain. "By whom" is a field the offerer fills in. Rama will not check it. Either a tier in front of the cluster vouches for the actor, or the offer carries a signature that the gate topology verifies. Which of the two is a first-record decision, because a signature has to be in the record from the start to be checkable later.


---
**rama L247-248 · BODY · NEW-CORNER -**
*Part one — What Rama's own reference says › Other items the brief named*

**Module updates.** **CHECKED** `docs/19-operating-rama.md:441`: PState clients have zero downtime; "Depot appends may have some downtime where appends are buffered clientside… anywhere from two seconds to thirty seconds." **CHECKED** `:424`: "There's currently no way to rename a depot or PState in a module update." Removing one deletes its data and needs an explicit `--objectsToDelete`. **IMPLIED:** depot and PState names are forever. The brief says the runtime will be rebuilt hundreds of times. That is cheap in Rama terms (each rebuild is a module update), as long as names and the task count never change.


---
**skeptics L222-222 · BODY · NEW-CORNER C7**
*3. Big-tech operational lessons › 3.4 Google: Hyrum's Law, and two instances of the fix*

- The nine-part envelope. INFERRED: every part that can be seen is a promise, whatever the documentation says. Are version numbers gapless within a cell? Tools will count edits by subtracting. Is `when` unique? Is it monotone across cells? Are based-on entries listed in read order? Does "empty because-of" always mean the start of a chain? Each will be depended on. Go's answer is to randomize whatever you do not promise. Zanzibar's is to wrap it. For Sid: decide which properties of each part are promises, then make the rest unobservable. That is easier before the first tool exists than after.

---
**sync L561-563 · BODY · NEW-CORNER -**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

- (0) "Never lost" is a number. Theirs went from sixty seconds to under one.
  REPORTED. For Sid: decide what the gate's yes means physically (acknowledged
  after how many durable copies) before the first record. INFERRED.

---
**sync L1934-1940 · BODY · NEW-CORNER -**
*3. Section two: versioning › 3.2 Jujutsu, and Mercurial's changeset evolution (Martin von*

- Mercurial's wish list names what the markers should have carried: "Storing
  more information about what the type of rewrite in obsolescence markers would
  be useful." The feature is still off by default after about fourteen years,
  out of commitment "to backward compatibility". Three kinds of divergence had
  to be named and then renamed after real use. Marker exchange "can be very
  slow". (ChangesetEvolution and its developer page)


---
**sync L2159-2167 · BODY · NEW-CORNER P0**
*3. Section two: versioning › 3.5 Irmin (Thomas Gazagnaire, Anil Madhavapeddy, the Tarides*

**4. Which questions.** (1) A hash that others verify can never change, and the
encoding under it becomes a frozen public spec. REPORTED. Hash addressing costs
an index on every read and write; they kept hashes for the few things outsiders
must verify and used plain pointers inside. REPORTED. (13), (0) "Storage is
cheap" lasted seven years. Plan the trim boundary before the first record.
REPORTED fact, INFERRED advice. (3) One value type per store forces either
opaque blobs or one giant union. REPORTED. Sid's "grammar looked up by key" is
the third way; the retrospective suggests the need is real. INFERRED.


---
**sync L2685-2687 · BODY · NEW-CORNER -**
*4. Question by question › (4) Based on: does each read say whether the fact depends on*

- Mercurial's developers wish their markers had recorded what kind of rewrite
  took place. REPORTED.


