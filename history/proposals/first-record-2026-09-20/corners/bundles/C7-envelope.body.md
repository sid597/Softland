# C7 envelope (13)(11): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L83-84 · BODY · NEW-REASON C7**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

> "Datoms constitute a single, flat, universal relation, and there is no other structural component to Datomic. This is important, as the more structural components you have in your model the more rigidity you get in your applications." — Hickey [H-IM]


---
**datalog L85-86 · BODY · NEW-REASON C7**

> "Having an atomic unit at the bottom of the model ensures that representations of novelty (e.g. transactions) are only as big as the new facts themselves." — Hickey [H-IM]


---
**datalog L87-96 · BODY · ABOVE C7,C8,E3**

Asked in 2012 whether Datomic would support business time next to technical time:

> "So the time on transactions is technical time. But the thing is, transactions are first class. So you can make assertions of transactions. So if you want to assert an attribute of a transaction, which is its business time, you can do that. But the granularity you have for that is the transaction level, not the datom level. Otherwise datoms become enormous… Or any other fact about the transaction: business time, business user, business process, business approval. Put them on the transaction." — Hickey [H-WD]

And in 2016, on the Datomic mailing list, the clearest statement of the principle:

> "In Datomic, Lucy probably said everything in the transaction that asserted 'Fred likes Ethel', so we instead put the provenance on the transaction (which can have an open set of attributes). 'When' Lucy said that is similarly (automatically) tracked once, on the transaction. This is substantially more efficient than replicating this on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)." — Hickey [H-ML16]

The docs say the same as practice: "Most entities in a system model the 'what' of your domain. Transactions provide a place to model 'when', 'who', 'where', and 'why'." [D-BEST]


---
**datalog L97-100 · BODY · ABOVE C7**

Hickey treats Sid's exact question as open and worth asking. From the 2016 exchange with Alan Kay:

> "What constitutes minimal sufficiency of 'data' is a useful and interesting question. E.g. should data always incorporate time, what are the tradeoffs of labeling being in- or out-of-band, per datom or dataset, how to handle provenance etc." — Hickey [H-HN16]


---
**datalog L151-158 · BODY · ABOVE C7**

**Why fixed slots are a mistake.** From *Maybe Not*:

> "And a straight product type just completely complects the _meaning_ of things with their position in a list." [H-MN]

> "Because what is the challenge of having a place? There always has to be something in the place." [H-MN]

> "I am just going to leave the key out. I am going to leave it out of the set… The maps know what they know." [H-MN]


---
**datalog L202-202 · BODY · DISAGREES C7,C4**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED (practitioner observation of a running system, Francis Avila, Datomic 0.9.5173) [AVILA]. Every Datomic database begins with the same bootstrap transactions. The very first one contains only naming facts, and the first of those names itself: `[10 10 :db/ident …]`. Entity 10 is the attribute `:db/ident`, and the datom that says so uses attribute 10. Small fixed integers are given to the system's own things: `0` is `:db.part/db`, `3` is `:db.part/tx`, `4` is `:db.part/user`, `15` is `:db/excise`, `50` is `:db/txInstant`. The bootstrap transactions are stamped `1970-01-01T00:00:00`. "First non-bootstrap transaction is always T >= 1000." "More bootstrap transactions or datoms may be added in later datomic versions."

---
**datalog L281-281 · BODY · DISAGREES C7**

- REPORTED principle. "When something is missing from a set, leave it out!" [H-MN]. In a record with fixed places, the start of a chain must hold *something* in that place, and that something is a null with a special meaning. In an open set of attributes, a fact that starts a chain simply has no such attribute, and "the maps know what they know."

---
**datalog L325-326 · BODY · NEW-REASON C7,W2**

- REPORTED. Plain data. "Did we use print / read on Clojure data relentlessly, because it is a cheap way to get serialization? Absolutely… If you do not consider doing that already in your programs, just do it." [H-WD]. "All interaction with Datomic is represented by data." [H-IM]. And the long argument with Alan Kay is about exactly this: facts are data, and "putting facts behind a dynamic interpreter… breaks the idea of data." [H-HN16]
- INFERRED. A class is a place-oriented record whose meaning lives in code that will be rebuilt hundreds of times. Plain maps with named keys outlive the runtime. The runtime is the one thing in Sid's system that is not made of facts, so nothing in the log should need the runtime's classes in order to be read.

---
**datalog L356-356 · BODY · ABOVE C7**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

- **Alan Kay**, on whether data without an interpreter is a good idea at all. Hickey: data is the more fundamental idea.

---
**datalog L391-392 · BODY · NEW-CASE C7,C8**
*3. Nubank: Datomic lived with, at scale › 3.3 What they regret*

2. **Acts that left no trace of their origin.** "what I regret the most, is to have implicit operations that are not stored into the database, or that don't have origination information. That was one of the earlier mistakes, the major mistakes that we had." — Lucas Cavalcanti, 2021. He ties it to a regulated industry: "that ended up harming us a lot, the ability to explaining things that happened." [N-POD]


---
**datalog L404-404 · BODY · NEW-CASE C7,C8**
*3. Nubank: Datomic lived with, at scale › 3.4 Which questions they speak to*

- **(11) because of, and the premise.** REPORTED regret 2. "Origination information" is because-of and by-whom. Their worst early mistake was acts without it.

---
**datalog L473-474 · BODY · NEW-CASE C7,W2**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.3 What they changed, and what bit them*

6. **Anything-goes values.** Version 1 "allows almost any data to be stored including deeply nested documents with arbitrary java.io.Serializable types." Looking ahead to version 2: "Apache Arrow data types are more restrictive — in a way we consider hygienic, not detrimental." [X-DD7]


---
**datalog L475-476 · BODY · NEW-REASON C7**

7. **Whole documents as the unit of change.** "The main downside of XTDB's document model is that re-transacting entire documents to update a single field can be considered inefficient." [X-FAQ]. They saw Datomic's small facts as the more exact tool: "Datomic's datom model provides a very granular and comprehensive interface for expressing novelty."


---
**datalog L492-492 · BODY · NEW-CASE C7,W2**
*4. XTDB (JUXT: Håkan Råberg, Jeremy Taylor, James Henderson) › 4.4 Which questions they speak to*

- **(13) storage.** REPORTED item 6. They let arbitrary serialized classes in, and later called the stricter typed format hygienic.

---
**datalog L538-539 · BODY · NEW-REASON C7,X4**
*5. The leads › 5.2 Nikita Prokopov (DataScript)*

> "Datomic has RDF-like data model (datom = entity, attribute, value) which is at a great granularity level for security and subscription filters. Datom is the smallest piece of information that could be synchronized." [T-WEB]


---
**datalog L566-566 · BODY · ABOVE C7,C3**
*6. The group: who matters most, who I dropped, who is missin*

1. **Rich Hickey and the Datomic record.** Not mainly for the five-tuple. For three things. The *saying* as the home of provenance. The discipline about names: an id in the fact, a name as a fact about it, never break, a new meaning is a new name. And the honesty about scope: closed world, one box, "shard above". Hickey tells Sid exactly which parts of his reasoning stop holding at the scale of the planet.

---
**frontiers L213-213 · BODY · DISAGREES C7**
*3. Peter Alvaro and Joe Hellerstein (CALM, Dedalus, Blazes,  › 3.4 Which questions they speak to*

- **(11) because-of.** INFERRED from Dedalus: base facts have no derivation and derived facts always have one. "Empty only when it starts a chain" is the same split.

---
**frontiers L320-321 · BODY · ABOVE C7**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.2 Reasons, in Goebel's words*

- The envelope itself. REPORTED, from the abstract: "different communication contexts call for slightly different types of datoms."


---
**frontiers L334-334 · BODY · ABOVE C7**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.4 Which questions Goebel speaks to*

- **The fixed envelope.** REPORTED: different contexts want different shapes.

---
**log L126-126 · BODY · NEW-CASE C7**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- *The entry format.* "In our first implementation, each entry was a literal stack of buffers (similar to a network packet); each engine would push/pop its own header. However, we found that such a layout was brittle against stack upgrades". "Since the entry is instead a map of headers, each engine can simply check within the apply upcall if its own header is within the entry". (Delos, SOSP 2021.)

---
**log L130-131 · BODY · NEW-REASON C7**

- *Nobody reserved room.* The gatherer searched all seven papers for reserved fields, format versions, forward or backward compatibility. Zero hits. The only format evidence is one format bit in Tango's stream header and Delos's retrofit above.


---
**log L140-140 · BODY · NEW-CASE C7**

- (13) REPORTED: a map of named parts survived version skew; a positional layout did not.

---
**log L175-175 · BODY · NEW-REASON C7**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

- (13) REPORTED: weak schema (maps, with his three mapping rules) over typed classes.

---
**log L206-206 · BODY · NEW-REASON C7**
*Voice by voice › 4. Martin Kleppmann*

- On formats he moved from confidence to an open problem. 2016: "Raw events are so simple and obvious that a "schema migration" doesn't really make sense". 2021: "any data format changes must be both forward and backward compatible", with lenses (Project Cambria) as the hope. Cambria names the failure of the frozen-format road: "a record full of optional fields is hard to use".

---
**log L245-245 · BODY · NEW-CASE C7,C1**
*Voice by voice › 5. Certificate Transparency, Trillian, and their descendants*

- *The reserved slot.* "RFC 6962 specifies no extensions, and current logs produce empty extensions fields." The slot was opaque but already inside the signed message. Sunlight "takes advantage of the lack of merge delay to embed the leaf index in an SCT extension". "The extension costs just 8 bytes." Auditors can now fetch "by index, rather than by hash", which removed the hash-lookup database. The new interface is described as "an alternative encoding format for the same data": the serving shape changed; the record and its signature did not.

---
**log L246-246 · BODY · NEW-CASE C7**

- *The clean version-two format died.* Andrew Ayer, 2024: "All CT logs and consumers implement a version of CT that is defined in RFC 6962. There are no plans to adopt RFC 9162."

---
**log L512-513 · BODY · NEW-CASE C7**
*Question by question: what this camp would say › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

REPORTED on format. Delos moved from a positional layout to "a map of headers" after the positional one proved "brittle against stack upgrades". Young chooses weak schema and states its three mapping rules. Event Store warns against code type names. The Overeem study reports stacks of upcasters slowing every load. Cambria names the opposite failure: "a record full of optional fields is hard to use". CT fixed one canonical encoding forever, left one opaque extension slot inside the signature, and eleven years later that slot carried the redesign, while the clean second format was never adopted.


---
**meaning L186-197 · BODY · NEW-REASON C7**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

**Turn 8 — Hickey, 11949133, 20:26**

> It contravenes the common and historical use of the word 'data' to imply
> undifferentiated bits/scribbles. It means
> facts/observations/measurements/information and you must at least grant it
> sufficient formatting and metadata to satisfy that definition. The fact that
> most data requires some human involvement for interpretation (e.g. pointing
> the right program at the right data) in no way negates its utility (we've
> learned a lot about the universe by recording data and analyzing it over the
> centuries), even though it may be insufficient for some bootstrapping system
> you envision.


---
**meaning L217-219 · BODY · ABOVE C7**

- 11954816: "It is worth thinking of an analogy to TCP/IP -- what is the
  smallest thing that could be universal that will allow everything else to
  happen?"

---
**meaning L226-237 · BODY · ABOVE C7**

- **11960130, 23 June 11:13 — Kay's last substantive word:**

> TCP/IP is "written in such a way that it works on all target systems". This
> partially worked because it was early, partly because it is small and simple,
> partly because it doesn't try to define structures on the actual messages, but
> only minimal ones on the "envelopes". And partly because of the "/" which does
> not force a single theory.
>
> This -- and the Parc PUP "internet" which preceded it and influenced it -- are
> examples of trying to organize things so that modules can interact universally
> with minimal assumptions on both sides.
>

---
**meaning L268-277 · BODY · NEW-REASON C7,E3,C8**

> Nothing about the *idea* of 'data' implies a lack of formatting/labeling/use
> of common language to convey the facts/observations, in fact it requires it.
> Data is *not* merely a signal and that is why we have two different
> ideas/words. '42' is not, itself, a fact (datum). What constitutes minimal
> sufficiency of 'data' is a useful and interesting question. E.g. should data
> always incorporate time, what are the tradeoffs of labeling being in- or
> out-of-band, per datom or dataset, how to handle provenance etc. That has
> nothing to do with data as an idea and everything to do with representing data
> well.
>

---
**meaning L361-364 · BODY · ABOVE C7**
*Part one — the exchange › 1.3 What each actually claimed*

6. Kay accepts the regress is real ("A good question isn't it?"). The direction
   for an answer is a smallest universal thing, on the TCP/IP model: minimal
   structure on the envelope, none on the message, and a seam ("the "/"") so no
   single theory is forced.

---
**meaning L372-376 · BODY · NEW-REASON C7**

**Hickey** (REPORTED, my ordering):

1. Data is a record of something known or uttered at a point in time. It
   predates computing. The word already implies enough labelling to be a fact:
   "'42' isn't data."

---
**meaning L391-394 · BODY · NEW-REASON C7,E3,C8**

8. Hickey names the open design question unprompted: "What constitutes minimal
   sufficiency of 'data' is a useful and interesting question. E.g. should data
   always incorporate time, what are the tradeoffs of labeling being in- or
   out-of-band, per datom or dataset, how to handle provenance etc."

---
**meaning L440-444 · BODY · ABOVE C7**
*Part one — the exchange › 1.5 What stayed unresolved*

2. **Minimal sufficiency.** Hickey lists the sub-questions: time on every
   datum, labels in-band or out-of-band, per datom or per dataset, provenance.
   Kay asks the mirror question: the "smallest thing that could be universal",
   with structure only on the "envelopes". Both walked up to the envelope
   question from opposite sides. Neither answered it.

---
**meaning L470-478 · BODY · NEW-REASON C7**
*Part one — the exchange › 1.6 My reading: is this the axis under Sid's questions?*

**Second, that axis is the reason Sid's table exists.** Sid's sentence,
"whatever is not written into a fact when it is made is gone for every earlier
fact", is Kay's Turn 3 with the future as the receiver. A store that never
rewrites turns every record into a message to a reader who is not there yet:
a person in year eight, the hundredth rebuild of the runtime, a second store.
Hickey's list in Turn 9 (time, in-band or out-of-band labels, per datom or per
dataset, provenance) is Sid's table in one sentence, written in 2016 as an open
question by the person best placed to answer it.


---
**meaning L526-534 · BODY · ABOVE C7**
*Part one — the exchange › 1.7 What each would make of Sid's design (all INFERRED)*

- *The nine-part envelope.* Hickey's own unit is smaller. REPORTED
  (Deconstructing the Database, 2012): "we call that a Datom. But it is just an
  entity, an attribute, a value, and some path to time. We use the transaction,
  because it is also a path to other information about what happened, including
  provenance, or causality, or operations, or anything else like that." And:
  "you want it to be minimal." INFERRED: Hickey would keep entity, key, value, and a
  path to the admission. Hickey would hang by-whom, based-on, and because-of on the
  admission as ordinary facts. Then the provenance theory can grow without
  touching the envelope.

---
**meaning L535-539 · BODY · DISAGREES C7**

- *"Fixed forever".* REPORTED (Spec-ulation, 2016): growth is "provide more"
  and "require less"; "Breaking changes are broken"; "If you say you cannot do
  X, it means you can never do X." INFERRED: an envelope may gain optional
  parts. It may never demand a new part of old facts. Readers must carry parts
  they do not understand.

---
**meaning L540-545 · BODY · NEW-REASON C7,W2**

- *Parsing without the grammar.* REPORTED (The Language of the System, 2012):
  "If you have out of band schemas, what can't you have? You can't have these
  things: generic processors and intermediaries." INFERRED: a value must be
  readable as plain self-describing data without first fetching its grammar
  fact. The grammar validates. It must not be needed to parse. That is an answer
  to (13).

---
**meaning L578-584 · BODY · ABOVE C7**

- *The envelope.* Kay is not against a fixed envelope. The TCP/IP remark is the
  strongest case for one in the thread. But Kay's envelope is minimal, puts no
  structure on the message, and has a seam so that no "single theory" is
  forced. Nine parts, with three kinds of based-on, is already a theory of
  provenance. INFERRED: Kay would ask which parts are the IP of this system
  (needed by everyone to admit and order a fact) and which are its TCP (one
  theory, replaceable, above the seam).

---
**meaning L591-599 · BODY · ABOVE C7,A2**

**Where the two agree against Sid's draft.** Both would shrink the fixed core.
Kay, for universality between strangers. Hickey, for minimal novelty and growth
by accretion. Neither would sign "nine parts forever". Both would sign "a tiny
core forever, and everything else open". Both would also say the same thing
about the runtime, from opposite sides. Kay: write down the bottom turtle.
Hickey: a derivation made by an interpreter that can change "can produce
results that don't add up", so the interpreter's version must be among the
reads of every crossing. Both land on row (12): yes, record it.


---
**meaning L845-851 · BODY · NEW-CASE C7**
*Part two — the people › 2.2 Kenton Varda: Protocol Buffers, Cap'n Proto, Sandstorm, *

**Unknown fields: dropped, then restored.** This is a natural experiment on
(13).

- REPORTED (Varda, 2014): "Apparently, version 3 of Protocol Buffers, aka
  "proto3", removes this feature. I honestly don't know what they're thinking.
  This feature has been absolutely essential in many of Google's internal
  systems."

---
**meaning L852-855 · BODY · NEW-REASON C7**

- INSTITUTIONAL (Jisi Liu of the protobuf team, 2016, issue 272): the drop was
  to let implementations choose, which "simplifies implementations and enables
  struct-like API". That is a storage-shape argument: closed, class-like
  records.

---
**meaning L856-859 · BODY · NEW-CASE C7**

- INSTITUTIONAL (release notes v3.5.0, Nov 2017): "Unknown fields are now
  preserved in proto3 for most of the language implementations for proto3 by
  default." The present docs still warn how they get lost: "Serialize a proto to
  JSON. Iterate over all of the fields in a message to populate a new message."

---
**meaning L860-864 · BODY · NEW-REASON C7**

- Lesson, INFERRED: a reader built from closed shapes loses what it does not
  understand. Anything that copies or forwards facts (a backup, an index
  builder, a second store, a runtime rebuild) must carry unknown parts through
  untouched. Plain open maps do this by default. Classes do not.


---
**meaning L865-872 · BODY · NEW-CASE C7**

**"Required" and the fixed envelope.**

- REPORTED (Varda, Cap'n Proto FAQ): "the "required" keyword in Protocol Buffers
  turned out to be a horrible mistake." "A field declared required,
  unfortunately, is required everywhere. The validation is baked into the
  parser." "Things like this have actually happened. At Google. Many times."
  "Low-level infrastructure that doesn't care about message content should not
  validate it at all."

---
**meaning L873-875 · BODY · NEW-REASON C7**

- REPORTED (Varda, HN 18196288, 2018): "A piece of data should be validated by
  the consumer, but should not be validated by pass-through middlemen." And Varda
  names names: "including Jeff and Sanjay when they first designed protobufs".

---
**meaning L876-877 · BODY · NEW-REASON C7**

- INSTITUTIONAL (protobuf.dev proto2 guide, heading "Required Is Forever"): "It
  is nearly impossible to safely change a field from required to optional."

---
**meaning L878-884 · BODY · ABOVE C7**

- REPORTED (same HN comment), directly on envelopes: "there is no header /
  container around a protobuf. This is one of the best properties of protobufs,
  because it makes them compose nicely with other systems that already have
  their own metadata [...] Adding required metadata wastes bytes and creates
  ugly redundancy." And on why the format never changed: "the benefits of
  introducing a whole new encoding were never shown to be worth the inevitable
  cost".

---
**meaning L885-889 · BODY · NEW-REASON C7,W2**

- INSTITUTIONAL (protobuf best practices), against one shape for storage and
  for the wire: "the needs of long-term storage and live RPC services tend to
  later diverge. Using separate types even if they are largely duplicative
  initially gives freedom to change your storage format without impacting your
  external clients."

---
**meaning L890-897 · BODY · NEW-REASON C7**

- What this says to Sid, INFERRED. The gate is a consumer, so the gate may
  validate. Nothing else on the path may. "Because of: always filled" is a
  required field. Varda's history says: do not bake requiredness into the
  encoding. Make it a gate policy, which is a fact and can change. And "a fixed
  envelope both ends share forever" is two decisions folded into one: what is
  stored, and what is offered and returned. Google's advice is to let those
  differ from the start.


---
**meaning L937-944 · BODY · ABOVE C7**

**Regrets, in Varda's words.** `required` ("a horrible mistake"). Varda retracts the
well-known message-bus example (HN 32818948): "I kind of wish I didn't say "message bus" in
that example". On Sandstorm the company (2017) Varda blames the business, not the
capability model. And the general stance (HN 18190005): "there is no such thing
as a successful system which was designed perfectly upfront. All successful
systems become successful by evolving, and thus you will always see this kind of
wart in anything that works well." I found no stated regret about Cap'n Proto.


---
**meaning L945-951 · BODY · ABOVE C7,A3**

**Transfer.** Varda's formats move between programs that are upgraded at different
times. That is Sid's case across a hundred runtime rebuilds. Varda's one-writer
objects are small and many; Sid's gate is drawn as one. Who Varda disagrees with:
type theorists ("version negotiation [...] is extremely painful in practice"),
the proto3 designers on unknown fields, and Jeff Dean and Sanjay Ghemawat on
`required`.


---
**meaning L1125-1126 · BODY · DISAGREES C7**
*Part two — the people › 2.4 IPFS and IPLD (Juan Benet), and what AT Protocol did wit*

- A dead slot forever: `prev` is now required and always null.


---
**meaning L1416-1427 · BODY · NEW-CASE C7,A1**
*Part two — the people › 2.7 Kay beyond the thread, STEPS, Worlds, and Ingalls*

### 2.7 Kay beyond the thread, STEPS, Worlds, and Ingalls

**The tape.** Kay's founding story for "meaning travels with the data" is an Air
Force file format from about 1961. REPORTED ("The Early History of Smalltalk",
1993): "some (to this day unknown) designer decided to finesse the problem by
taking each file and dividing it into three parts. The third part was all of
the actual data records of arbitrary size and format. The second part contained
the B220 procedures that knew how to get at records and fields to copy and
update the third part. And the first part was an array of relative pointers into
entry points of the procedures in the second part (the initial pointers were in
a standard order representing standard meanings)."


---
**meaning L1428-1433 · BODY · ABOVE C7**

The spoken version (OOPSLA 1997, from a transcript of the talk) adds the part
that matters for an envelope: "let's make the first ten or so pointers standard,
like reading and writing fields, and trying to print; let's have a standard
vocabulary for the first ten of these, and then we can have idiosyncratic ones
later on."


---
**meaning L1434-1439 · BODY · ABOVE C7**

So Kay's own model is a small fixed prefix of standard meanings, and then open.
That is not "no envelope". It is a short one. And from the same talk: "HTML on
the Internet has gone back to the dark ages because it presupposes that there
should be a browser that should understand its formats. [...] It should travel
with all the things that it needs".


---
**meaning L1451-1462 · BODY · ABOVE C7**

**Kay's regret is about freezing.** REPORTED (squeak-dev, 1998): "at PARC we
changed Smalltalk constantly, treating it always as a work in progress -- when
ST hit the larger world, it was pretty much taken as 'something just to be
learned' [...] Smalltalk-80 never really was mutated into the next better
versions of OOP. [...] I think this is a real mistake." From the same message:
"The key in making great and growable systems is much more to design how its
modules communicate rather than what their internal properties and behaviors
should be." And on changing the meta-level: "systems should allow these things,
but the design should be such that there are clear fences that have to be
crossed when serious extensions are made." In the AMA Kay named the fence:
a "roll back 'worlds' mechanism (like transactions)".


---
**meaning L1511-1513 · BODY · ABOVE C7**

- "Good Design: A system should be built with a minimum set of unchangeable
  parts; those parts should be as general as possible; and all parts of the
  system should be held in a uniform framework."

---
**meaning L1521-1527 · BODY · ABOVE C7,A2**

INFERRED: the envelope is Sid's "unchangeable parts", so make it minimal. The
runtime is Sid's operating system: the things that did not fit into the medium.
Ingalls would shrink it until almost nothing is left. I could not open Ingalls' 2020
history of Smalltalk (the publisher refused every fetch), so I have nothing from
Ingalls on what the image, as the one substance, cost over forty years. That is a
gap.


---
**meaning L1698-1705 · BODY · NEW-REASON C7**
*Part two — the people › 2.9 Joe Armstrong*

**Transfer.** Erlang's two parties are programs; its "two versions at once" rule
is about running code, not stored facts. I could not find a direct statement by
Armstrong on talking to unknown future versions, which the brief named. The nearest
things are the two-version rule, the contract as the meeting point of
separately built programs, and the parent tag. Armstrong disagrees with hierarchical
namespaces ("the *dot* in the name has no semantics"), with XML and WSDL, and
with putting the type system inside the language.


---
**meaning L1741-1748 · BODY · ABOVE C7**
*Part two — the people › 2.10 Self (David Ungar, Randall Smith)*

**Regrets.** REPORTED ("Programming as an Experience", 1995): "We have learned
the hard way that smaller is better and that examples can be deceptive. Early in
the evolution of Self we made three mistakes [...] Each was motivated by a
compelling example." "The resultant semantics took five pages to write down".
Their rule: "when features, rules, or elaborations are motivated by particular
examples, it is a good bet that their addition will be a mistake… this
phenomenon might be called 'the language designer's trap.'"


---
**meaning L1749-1753 · BODY · ABOVE C7**

That rule is aimed at exactly the kind of decision Sid is making. Each part of a
nine-part envelope is there because of a compelling example. I could not open
their 2007 retrospective (paywalled), so its wording on maps and regret is not
here.


---
**meaning L2413-2419 · BODY · NEW-CASE C7,E2**
*Part four — Sid's questions, hung on the parts of the fact › (11) Because of: always filled?*

### (11) Because of: always filled?

**Camp.** Folk ran for years without it and added it in 2025, tagging "all
downstream statements of the invocation" so it could tell when a chain of work had
settled (3.1). PROV has the trigger (above). Against baking it in: Varda's
"required is forever" (2.2).


---
**meaning L2420-2425 · BODY · DISAGREES C7**

**My read.** Keep it. But make it required by gate policy, not by the encoding.
And do not let emptiness carry meaning. Protobuf removed the ability to tell
"absent" from "default", and had to put it back "in response to user feedback"
(2.2). "Starts a chain" should be said outright. Empty should be free to mean
"unknown", which imports and genesis facts will need.


---
**meaning L2541-2548 · BODY · DISAGREES C7,P0**
*Part four — Sid's questions, hung on the parts of the fact › (13) Storage*

**My read.** Plain, open, self-describing maps, readable without fetching a
grammar. Every copier carries what it does not understand. Decide now what *may*
be trimmed (motion facts in session layers, bodies of refused offers), because a
trimming rule changes what "as of" can promise: Reed's reads older than the window
"are rejected".

---


---
**rama L213-213 · BODY · NEW-REASON C7,W2**
*Part one — What Rama's own reference says › Question by question › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

- **CHECKED** `docs/17-serialization.md:40-45` — Java serialization is for "experiments and tests", because "you won't be able to deserialize old versions of that type". For production: "a solution for custom types with first-class support for evolving types over time", naming Thrift and Protocol Buffers. RPL's Mastodon build uses Thrift.

---
**rama L219-220 · BODY · NEW-REASON C7,W2**

**IMPLIED:** plain Clojure maps need no registration anywhere, which suits an envelope "both ends share forever". A custom type ties every client, for good, to a serializer jar. The camp's own preference, though, is a schema with evolution rules (Part two). The two can be reconciled: plain data on the wire, and the schema as a checked convention. That is what Sid's "grammar as a fact" already is.


---
**rama L283-284 · BODY · NEW-REASON C7**
*Part two — Nathan Marz › 2. His reasons, in his words*

On schemas. "JSON doesn't give you a real schema and doesn't protect against data inconsistency… A good schema protects you against these kinds of errors, keeps your data consistent, and gives you errors at the time of creating a bad object." (*Thrift + Graphs*, 2010). Fourteen years on: "Schemas themselves are extremely important, and they should be as tight as possible." (HN, 2024-01-09, https://news.ycombinator.com/item?id=38934011)


---
**rama L333-334 · BODY · NEW-REASON C7,W2**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(13) Storage.** REPORTED: tight schemas; Thrift or Protobuf for anything that must evolve; trimming optional; backups built in. "The free version of Rama can run clusters up to two nodes" (*Rama… is now free for production use*, 2025, https://blog.redplanetlabs.com/2025/03/18/rama-the-100x-developer-platform-is-now-free-for-production-use/).


---
**rama L403-404 · BODY · DISAGREES C7,C5**
*Part three — The dissent: Jay Kreps › 4. Which of Sid's questions he speaks to*

**(9)** INSTITUTIONAL (same Kafka page): "A message with a key and a null payload will be treated as a delete from the log. Such a record is sometimes referred to as a tombstone… delete markers are special in that they will themselves be cleaned out of the log after a period of time." A reader that lags more than the retention of delete markers can miss the delete.


---
**skeptics L108-108 · BODY · ABOVE C7,C4**
*2. The skeptics › 2.1 Michael Stonebraker with Joe Hellerstein (2005) and with*

- (13). INFERRED from Lesson 8: keep the storage model simple so that logical changes are cheap. A fixed envelope is simple. A value whose shape is looked up through another versioned fact is not.

---
**skeptics L173-174 · BODY · NEW-REASON C7,P0**
*3. Big-tech operational lessons › 3.1 Amazon*

**James Hamilton.** Quoted in section 1. The paper predates Amazon, so I do not attribute it to Amazon. One more line bears on the fixed envelope. REPORTED: "Maintain forward and backward compatibility." "Don't rip out support for old file formats until there is no chance of a roll back to that old format in the future." INFERRED: under never-rewrite, that chance never reaches zero. Every shape a fact ever had must stay readable for good.


---
**skeptics L191-191 · BODY · NEW-REASON C7,X2**
*3. Big-tech operational lessons › 3.2 Meta: TAO, FlightTracker, RAMP-TAO*

- "data invariants that were not honored by all historical data". INFERRED: under never-rewrite, every early fact that breaks a later rule stays forever. Every tool must cope with every historical shape, or the store must keep, as facts, which rules held when.

---
**skeptics L211-212 · BODY · NEW-REASON C7,C1,C2**
*3. Big-tech operational lessons › 3.4 Google: Hyrum's Law, and two instances of the fix*

**The law.** Hyrum Wright, REPORTED: "With a sufficient number of users of an API, it does not matter what you promise in the contract: all observable behaviors of your system will be depended on by somebody." And further: "Given enough use, there is no such thing as a private implementation." "the implicit interface will eventually exactly match the implementation. At this point, the interface has evaporated". Wright credits the name: "credit goes to Titus Winters for actually naming it as 'Hyrum's Law'". ([hyrumslaw.com](https://www.hyrumslaw.com/)). Winters, in the book, REPORTED: "If users cannot depend on such things, your API will be easy to change. Given enough time and enough users, even the most innocuous change will break something". The book's example is hash iteration order. ([Software Engineering at Google, chapter 1](https://abseil.io/resources/swe-book/html/ch01.html))


---
**skeptics L216-216 · BODY · NEW-REASON C7,C1**

1. Make the property unobservable by making it random. Go, REPORTED: "the iteration order is not specified and is not guaranteed to be the same from one iteration to the next." ([Go maps in action](https://go.dev/blog/maps))

---
**skeptics L302-302 · BODY · NEW-REASON C7,C1,E3**
*8. What each source implies for a second store*

- **Hyrum's Law, INFERRED.** Once a second store exists, the first store's observable habits are the de facto protocol. Anything the second store does differently (id form, version numbering, stamp precision) will break tools that were never told those were not promises.

---
**sync L153-157 · BODY · DISAGREES C7**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

- The version slot exists from birth: "The CSN is the most significant factor
  used to determine a write's position in the log; uncommitted or tentative
  writes have a commit sequence number of infinity." (Petersen et al.,
  "Flexible Update Propagation for Weakly Consistent Replication", SOSP 1997,
  https://www.cs.cornell.edu/courses/cs614/2003sp/papers/PST97.pdf)

---
**sync L321-323 · BODY · NEW-REASON C7**
*2. Section one: sync and multiplayer › 2.2 Convex (Sujay Jayakar, James Cowling, Jamie Turner; 2021*

- June 2023: the `Id` class became plain strings, which "are much easier to
  pass to other services or across JSON-serialized boundaries."
  (https://news.convex.dev/announcing-convex-0-17-0/)

---
**sync L374-384 · BODY · NEW-REASON C7**

**6. Who they disagree with.** With CRDT libraries on where conflict policy
lives: "conflict resolution may need to be handled at the application layer as
a product decision, not at the framework layer", and CRDTs "often try to
resolve as many conflicts as possible automatically, embedding many product
decisions within the library". (Jayakar, "A Map of Sync",
https://stack.convex.dev/a-map-of-sync) This is Bayou's position, thirty years
on. With loose value grammars: "A fifth of the datatypes in MongoDB's BSON are
deprecated", and a large integer written from another language "will silently
lose precision when read in JavaScript, potentially corrupting the row if
JavaScript writes it back out." (Jayakar, "How Convex Works"; bears on (13)).


---
**sync L808-811 · BODY · DISAGREES C7**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- Forward compatibility is a duty of every implementation: "implementations
  must preserve columns that they do not understand", and for a change's extra
  bytes: "If future versions of automerge add new metadata to changes, this
  will allow old clients to collaborate with new clients". (same)

---
**sync L899-902 · BODY · NEW-CASE C7**

- *The storage engine leaked into the API.* "Previously, Hexane's types leaked
  through Automerge's surface, which meant any change to the storage engine was
  potentially a breaking change to Automerge. Now the boundary is sealed."
  (August '26, https://automerge.org/blog/2026-august/)

---
**sync L920-921 · BODY · DISAGREES C7,C4**

- (17) Give the first thing a fixed, defined id (null actor, null counter). Do
  not derive it from content or code. REPORTED.

---
**sync L931-933 · BODY · DISAGREES C7**

- (13) Preserve what you do not understand. Be strict about one canonical
  encoding if anything is hashed. Seal storage types away from the public
  surface. REPORTED.

---
**sync L981-982 · BODY · NEW-REASON C7,C4**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- The grammar of the grammar: "we'll need to consider how Cambria's own data
  might be versioned and lensed."

---
**sync L1401-1406 · BODY · DISAGREES C7,X2**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- The gate does not always know the grammar: "if the PDS knows the record
  Lexicon, it validates. If the PDS does not know it… the record is allowed to
  be created. This is also referred to as 'Fail-Open'." (same) Newbold: "we
  probably don't want PDS instances to have to do 'live' Lexicion resolution in
  the middle of processing record creation", and "We don't currently have a
  strong conception of 'recent' / 'latest' Lexicon schema". (Discussion #2940, 2024)

---
**sync L1454-1458 · BODY · NEW-CASE C7,C4**

- *The spec lost to the data.* Record keys could not contain colons; popular
  records already did; "we decided we'll allow colons in record keys". (same)
  And the general menu when old rows are wrong: "a bulk graph update (eg,
  re-writing records); provide some other migration path; slightly loosen the
  specification; or just accept some small breakage". (Discussion #1910)

---
**sync L1479-1482 · BODY · NEW-REASON C7,C8**

- *Encodings moved.* The data model now names "DRISL (which is successor to
  DAG-CBOR)", and floats are banned because re-encoding "is not always
  consistent". Old signatures age: "With key rotation, verification of older
  commit signatures can become ambiguous."

---
**sync L1526-1528 · BODY · NEW-CASE C7**

- (13) Floats, map order, null versus absent: every one became a rule after an
  interop failure. REPORTED.


---
**sync L1580-1586 · BODY · NEW-REASON C7,C6**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

- *Edits.* "Direct edits are a centralizing force on Nostr", and the general
  law: "No, they are not optional. If edits become widespread they necessarily
  become mandatory. Any client that doesn't implement edits will be displaying
  false information to its users". His alternative is a delay before
  publishing: "giving the user the opportunity to cancel and edit it again
  before it is actually posted." (fiatjaf, "The case against edits", 2024,
  https://fiatjaf.com/ad84e3b3.html)

---
**sync L1618-1621 · BODY · NEW-REASON C7**

- Above the table, fiatjaf's law in my paraphrase: any optional feature that
  changes what readers see becomes mandatory. REPORTED in substance (the edits
  quote above). It argues for a small envelope.


---
**sync L1814-1816 · BODY · DISAGREES C7**
*3. Section two: versioning › 3.1 Git (Linus Torvalds, Junio Hamano, Jeff King, brian m. c*

- A commit carries two actors and two clocks (author and committer, each with
  a time), and unknown headers are kept. This line is my background knowledge
  of the commit format, not a gathered quote.

---
**sync L1914-1916 · BODY · DISAGREES C7**
*3. Section two: versioning › 3.2 Jujutsu, and Mercurial's changeset evolution (Martin von*

- A fixed origin: "The root commit is a virtual commit at the root of every
  repository. It has a commit ID consisting of all '0's and a change ID
  consisting of all 'z's." (glossary)

---
**sync L1931-1933 · BODY · NEW-CASE C7**

- A field the old tools do not know is dropped silently: the change id header
  "is not preserved by all `git` tooling… preserved by a `git commit --amend`,
  but is not preserved through a rebase operation". (git-compatibility)

---
**sync L1967-1970 · BODY · NEW-REASON C7**
*3. Section two: versioning › 3.3 Fossil (D. Richard Hipp; 2006–now)*

- The format is meant to outlive its makers: "The global state of a fossil
  repository is kept simple so that it can endure in useful form for decades or
  centuries. A fossil repository is intended to be readable, searchable, and
  extensible by people not yet born." (fileformat)

---
**sync L2150-2152 · BODY · NEW-CASE C7,W2**
*3. Section two: versioning › 3.5 Irmin (Thomas Gazagnaire, Anil Madhavapeddy, the Tarides*

- *The on-disk format is at version five.* One step "is not
  backwards-compatible with existing stores… It is not forwards compatible."
  (mirage/irmin CHANGES.md)

---
**sync L2215-2217 · BODY · NEW-CASE C7**
*3. Section two: versioning › 3.6 Pijul (Pierre-Étienne Meunier, Florent Becker; 2015–now)*

- *The break.* "Certainly the most breaking change of them all is the new patch
  format… any repository started before now would have become obsolete in a
  matter of days."

---
**sync L2359-2364 · BODY · DISAGREES C7,C4**
*4. Question by question › (17) The first facts*

- Give the origin a fixed, defined id. jj: all zeros and all z's, a "virtual
  commit". Automerge: the root map has "a `null` actor id and `null` counter".
  Pijul: "the identity element 1". PPPPP: a first message "deterministically
  predictable and empty, so to allow others to pre-know its msg ID". Matrix:
  the create event is the one whose authorisation list is empty. REPORTED and
  INSTITUTIONAL.

---
**sync L2416-2418 · BODY · DISAGREES C7,X2**
*4. Question by question › (3) Key: a word, or an id with its name and shape as facts?*

- A gate that does not know a grammar lets the record through ("Fail-Open"),
  and should not fetch grammars "in the middle of processing record creation":
  AT Protocol. REPORTED.

---
**sync L2727-2733 · BODY · DISAGREES C7**
*4. Question by question › (11) Because of: always filled; empty only when it starts a *

**What the camp says.** jj makes the origin a real, fixed thing, so every
commit has a parent and no code handles "none". REPORTED. Matrix's create event
is the one record with an empty authorisation list. INSTITUTIONAL. Croquet
records only what came from outside; everything else follows from it. REPORTED.
jj's operation log keeps the command and its arguments for every operation;
Mercurial wishes it had kept the kind. REPORTED.


---
**sync L2941-2942 · BODY · NEW-REASON C7**
*4. Question by question › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

- Build for readers not yet born: Hipp. Keep even old brokenness so that data
  round-trips: Git. REPORTED.

---
**sync L2943-2945 · BODY · DISAGREES C7**

- Every writer must keep what it does not understand: Automerge's unknown
  columns; jj's header lost through `git rebase` is the counter-example.
  REPORTED.

---
**sync L2946-2949 · BODY · NEW-REASON C7**

- One runtime's serialisation habits must not define the format. AT Protocol's
  nullable-and-optional rule was settled by "the idiomatic way to serialize
  data structures in golang". SSB's ids depend on one engine's JSON printing.
  Automerge's storage types "leaked through" its public surface. REPORTED.

---
**sync L2953-2954 · BODY · NEW-REASON C7**

- Value types differ across runtimes: a large integer "will silently lose
  precision when read in JavaScript". Jayakar. REPORTED.

