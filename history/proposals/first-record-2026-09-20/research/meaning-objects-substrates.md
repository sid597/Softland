# Meaning travelling with data, objects, and substrates

Research report, round one. For Sid, via the orchestrator session.

- Written by: Claude Fable 5.1, max effort, session `research-3`, 2026-09-20.
- Method: I read the Kay / Hickey exchange myself, in full, from the Hacker News
  API. I checked Hickey's wider positions against talk transcripts and the
  Datomic docs myself. For the other people, twelve Opus gatherers collected
  sources under one rule: a quote must be copied from page text they actually
  fetched. They did not judge. Every reading, inference, ranking, and
  recommendation below is mine. I spot-checked the quotes that carry weight.
- I did not read this repository's code or docs, and I did not read the other
  research sessions' reports. I was not given Sid's leanings.

How to read the markers:

- **REPORTED** — the person wrote or said it. Quotes are verbatim.
- **INFERRED** — reconstructed from their system or principles. Mine unless I say otherwise.
- **INSTITUTIONAL** — a standard, a many-author paper, or community practice.
- **Not opened** — I could not fetch or read it. Anything said about it is flagged.

The raw fetched pages and the gatherers' note files sit in this session's
scratch space and will not survive it. The brief said to touch nothing else on
disk, so I did not copy them into `research/sources/`. Every quote carries its
URL or comment id so it can be checked again from the source.

## The short version

**The exchange.** Kay and Hickey both put meaning at the reader. They differ on
what the writer owes a reader who was not there. Kay: a way to recover what was
meant, even if that thing is active. Hickey: a labelled, fixed, harmless record,
and nothing that acts. Kay never answered Hickey's strongest comment. Two loose
ends were left, and both are Sid's rows: "should I trust that [metadata]?" is
(8), and "I don't trust you enough to run it" is (15). Hickey also wrote Sid's
table in one sentence, as an open question: time on every datum, labels in or
out of band, per datom or per dataset, provenance.

**Is that the axis?** It is the reason the table exists: a store that never
rewrites makes every fact a message to a reader who is not there yet. It
decides the rows about keys, grammars, the runtime, and first facts. It is
silent on order, on authority, and on how ids are made. Those are three other
axes.

**Sid's design against it.** Reference instead of bundling: interpreters are
inert facts, and each fact points at the versions it used. That keeps Hickey's
harmlessness and answers Kay's "How can you find it?" It holds only if the
pointer means the same thing for ever and in another store, if the thing pointed
at can be run by a runtime not yet built, and if the pointer is on every fact
from the first.

**Both would shrink the fixed envelope.** Kay for universality (the tape had
"the first ten or so pointers standard"; TCP/IP has a small header and a seam).
Hickey for minimal novelty ("an entity, an attribute, a value, and some path to
time") and growth by addition only. Both would have the runtime's version
written down.

**The strongest challenge from this camp.** Tools matched to facts with nobody
choosing them, plus policy keyed on who the actor is, is ambient authority by
the capability camp's own definition. It is Hardy's confused deputy at planet
scale. It cannot be patched later, because "under what authority was this
written" is not on the early facts. Their repair is small and early: an offer
cites the grant it is using; grants are facts; a click designates and thereby
grants; "by whom" is a chain with the instrument in it.

**What is gone for ever if not written from the first record** is listed at the
end of Part five: thirteen items, each tied to someone's scar.

---

## Part one — the exchange

### 1.1 Where it is, and what I read

The link in the brief is not a story. Item 11945722 is one comment by Alan Kay:
"What if "data" is a really bad idea?" It sits inside Kay's AMA of 20–23 June 2016
(story 11939851, "Alan Kay has agreed to do an AMA today"). The exchange is that
comment's subtree: 71 comments. Kay wrote 19 of them (18 as `alankay`, 1 as
`alankay1`). Hickey wrote 7. Hickey wrote nothing else in the AMA.

I read all 71. I then searched Kay's other ~230 AMA comments for anything on
data, time, versions, or meaning, and read those with their parents.

One caution. A user named `mmiller` writes 7 comments in the subtree and
glosses Kay at length. This is the "tekkie" blogger, who links a blog there, and
another user calls it theirs. This is not Mark S. Miller of object capabilities.
Hickey's longest comment is a reply to this user, not to Kay.

What Kay was answering: a user, wdanilo, listed predictions for the next
paradigm shift. The first was "focus on data processing rather than imperative
way of thinking (esp. functional programming)".

Links: every id below resolves as `https://news.ycombinator.com/item?id=<id>`.

### 1.2 The exchange, in order, in their words

**Turn 1 — Kay, 11945722, 21 June 14:14**

> What if "data" is a really bad idea?

**Turn 2 — Hickey, 11945869, 14:34**

> Data like that sentence? Or all of the other sentences in this chat? I find
> 'data' hard to consider a bad idea in and of itself, i.e. if data ==
> information, records of things known/uttered at a point in time. Could you
> talk more about data being a bad idea?

**Turn 3 — Kay, 11946532, 15:44**

> What is "data" without an interpreter (and when we send "data" somewhere, how
> can we send it so its meaning is preserved?)

**Turn 4 — Hickey, 11946764, 16:11**

> Data without an interpreter is certainly subject to (multiple) interpretation
> :) For instance, the implications of your sentence weren't clear to me, in
> spite of it being in English (evidently, not indicated otherwise). Some
> metadata indicated to me that you said it (should I trust that?), and when.
> But these seem to be questions of quality of
> representation/conveyance/provenance (agreed, important) rather than critiques
> of data as an idea. Yes, there is a notion of sufficiency ('42' isn't data).
>
> Data is an old and fundamental idea. Machine interpretation of un- or
> under-structured data is fueling a ton of utility for society. None of the
> inputs to our sensory systems are accompanied by explanations of their
> meaning. Data - something given, seems the raw material of pretty much
> everything else interesting, and interpreters are secondary, and perhaps
> essentially, varied.

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

**Turn 7 — Kay, 11948729, 19:31**

> 2nd Paragraph: How do they know they are even bits? How do they know the bits
> are supposed to be numbers? What kind of numbers? Relating to what?
>
> Etc

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

Kay never replies to Hickey again. Kay goes on developing the view in side
branches, to other people:

- 11947120, on ambassadors: "This is why "the objects of the future" have to be
  ambassadors that can negotiate with other objects they've never seen. Think
  about this as one of the consequences of massive scaling ..."
- 11948601, to someone who asked whether the interpreter even needs to travel
  when bandwidth is everywhere: "How can you find it? The association between
  "patterns" and interpretation becomes an "object" when this is part of the
  larger scheme. When you've just got bits and you send them somewhere, you
  don't even have "data" anymore. Even with something like EDI or XML, think
  about what kinds of knowledge and process are actually needed to even do the
  simplest things."
- 11948605: "It's not "Big Data" but "Big Meaning""
- 11948634, to a user who asked how to avoid sending an interpreter for the
  interpreter, and so on down: "Yes, so think about how to make this work
  "nicely" in an Intergalactic Network ..." Later (11957719), to "it can't be
  turtles all the way down": "A good question isn't it?", and a pointer to
  Lincos, the language designed for first contact.
- 11954816: "It is worth thinking of an analogy to TCP/IP -- what is the
  smallest thing that could be universal that will allow everything else to
  happen?"
- 11954982, on reviving a 1978 Smalltalk image from a discarded disk pack: it
  "was quite easy to bring back to life because it was already virtualized "for
  eternity")." And: "The idea was that you could make a universal computer in
  software that would be smaller than almost any media made in it, so ..."
- 11957079: "Welcome to Claude Shannon! It's not about the message but about
  the receiver ..."
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
> The next step -- of organizing a minimal basis for inter-meanings -- not just
> internetworking -- was being thought about heavily in the 70s while the
> communications systems ideas were being worked on, but was quite to the side,
> and not mature enough to be made part of the apparatus when "Flag Day"
> happened in 1983.
>
> What is the minimal "stuff" that could be part of the "TCP/IP" apparatus that
> could allow "meanings" to be sent, not just bits -- and what assumptions need
> to be made on the receiving end to guarantee the safety of a transmitted
> meaning?

**Turn 9 — Hickey, 11962116, 23 June 16:13.** Five hours after Kay's TCP/IP comment.
A reply to `mmiller`'s gloss of Kay. This is Hickey's full position. I give it
almost whole, because it reads like a specification.

> If we can't agree on what words mean we can't communicate. This discussion is
> undermined by differing meanings for "data", to no purpose. You can of course
> instead send me a program that (better?) explains yourself, but I don't trust
> you enough to run it :)
>
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
> When interacting with an ambassador one may or may not get the facts, and may
> get different answers at different times. And one must always fear that some
> question you ask will start a war. Science couldn't have happened if consuming
> and reasoning about data had that irreproducibility and risk.
>
> 'Data' is not a *universal* idea, i.e. a single primordial idea that
> encompasses all things. But the idea that dynamic objects/ambassadors
> (whatever their other utility) can substitute for facts (data) *is* a bad idea
> (does not correspond to reality). Facts are things that have happened, and
> things that have happened have happened (are not opinions), cannot change and
> cannot introduce new effects. Data/facts are not in any way dynamic (they are
> accreting, that's all). Sometimes we want the facts, and other times we want
> someone to discuss them with. That's why there is more than one good idea.
>
> Data is as bad an idea as numbers, facts and record keeping. These are all
> great ideas that can be realized more or less well. I would certainly agree
> that data (the maintenance of facts) has been bungled badly in programming
> thus far, and lay no small part of the blame on object- and place-oriented
> programming.

**Turn 10 — Hickey, 11963502**, asked why "data" should be limited to facts:
""datum" means "a thing given" - a fact or presumed fact."

**Kay elsewhere in the same AMA**, on the same subject (all REPORTED):

- 11953728, to a user who asked what the scare quotes meant: "Just to say one
  more time here: the central idea is "meaning", and "data" has no meaning
  without "process" (you can't even distinguish a fly spec from an intentional
  mark without a process. One of many perspectives here is to think of
  "anything" as a "message" and then ask what does it take to "receive the
  message"?" And: "the extent to which most code today relies on "outside of
  code" programmer views (and hopes) is astounding and distressing."
- 11945766: "E.g. "data" in the small "seems natural" but the whole idea scales
  terribly."
- 11940028: object-oriented "got neutered into Abstract Data Types", because
  people wanted to keep "procedures, assignment statements, and data structures.
  These don't scale well".
- 11940599, on functional languages: "They need a much better idea of time
  (such as approaches to McCarthy's fluents)."
- 11953505, on building a decentralised system: "Take a look at the Internet
  itself -- and then take a look at Dave Reed's 1978 PhD thesis at MIT [...] we
  used many of these ideas in the Croquet project".
- 11956408: "several starts could be to relax from programming by message
  sending (a tough prospect in the large) to programming by message receiving,
  and in particular to program by intent/meaning negotiation." And: "Linda was a
  great idea of the 80s, what is the similar idea scaled for 40 years later? (It
  won't look like Linda, so don't start your thinking from there ...)"
- 11946681 and 11941131: Kay points to Alex Warth's "Worlds" paper for
  "possible worlds" reasoning, and to a "roll back 'worlds' mechanism (like
  transactions)" as the safe way to do meta-level change.

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
5. The hard part is finding the interpreter: "How can you find it?" The pairing
   of pattern and interpretation, made part of the scheme, is what Kay calls an
   object.
6. Kay accepts the regress is real ("A good question isn't it?"). The direction
   for an answer is a smallest universal thing, on the TCP/IP model: minimal
   structure on the envelope, none on the message, and a seam ("the "/"") so no
   single theory is forced.
7. Kay names the safety problem unprompted and leaves it open: "what assumptions
   need to be made on the receiving end to guarantee the safety of a transmitted
   meaning?"
8. Kay's existence proof across time is the 1978 image: a universal computer
   "smaller than almost any media made in it", so the media can carry its own
   machine.

**Hickey** (REPORTED, my ordering):

1. Data is a record of something known or uttered at a point in time. It
   predates computing. The word already implies enough labelling to be a fact:
   "'42' isn't data."
2. Three defining properties. It records the past. It "doesn't and can't *do*
   anything, i.e. have effects". It "does not change".
3. Interpretation is separate, comes later, and is plural: "interpreters are
   secondary, and perhaps essentially, varied". The source often cannot know the
   meaning. The seismometer does not know an earthquake is coming.
4. Labelling is not interpretation: "equating any such labeling with more
   general interpretation is a mistake."
5. Anything you add to enrich a message is more data. So is a bundled
   interpreter: "bytecode? - more data needing interpretation/execution".
6. A dynamic interpreter in front of facts breaks them. It may answer
   differently at different times, mix fact with derivation, or have effects.
   Derived values are only safe when "temporally-qualified, 'as-of'".
7. Trust: "I don't trust you enough to run it". And: "one must always fear that
   some question you ask will start a war."
8. Hickey names the open design question unprompted: "What constitutes minimal
   sufficiency of 'data' is a useful and interesting question. E.g. should data
   always incorporate time, what are the tradeoffs of labeling being in- or
   out-of-band, per datom or dataset, how to handle provenance etc."
9. Hickey blames the bungling of fact-keeping on "object- and place-oriented
   programming".

### 1.4 Where they talked past each other

**The word.** Kay attacks "data" in quotes: bits on a wire plus hope. Hickey
defends data in the dictionary sense: a labelled record. When Kay asks "How do
they know they are even bits?", Hickey answers that data by definition comes
with "sufficient formatting and metadata". That reply grants Kay's premise
(something must carry the context) and denies Kay's conclusion (that the
something must be a process that travels along). After that, the real
disagreement is what counts as sufficient, and who supplies it. They never get
there. Hickey says so: "This discussion is undermined by differing meanings for
"data", to no purpose."

**The problem each is solving.** Kay is working on transmission between
parties who have never met, across space ("Intergalactic") and time (the 1978
image). Kay's test is whether the receiver can recover what was meant. Hickey is
working on record-keeping. Hickey's test is whether the past stays put: immutable,
harmless, reproducible. These are different jobs. A store that never rewrites
has to do both. It is a record, and it is a message to readers who do not exist
yet.

**"Interpreter".** For Kay it covers everything from "these are bits" up to
"this is what was intended". Hickey cuts that in two. The bottom part
(format, names, time, who said it) Hickey counts as part of the data. The top part
(what it implies) Hickey calls interpretation and keeps apart. Kay never accepts
the cut. For Kay labels are more marks that need a process: "Even with
something like EDI or XML, think about what kinds of knowledge and process are
actually needed". Hickey makes the mirror move: an interpreter is more data.
Each saw the regress. Each aimed it at the other.

**The ambassador.** Kay's metaphor suggests a live party that negotiates.
Hickey attacks that strong reading: objects that "substitute for facts". In
this thread Kay did not claim substitution. Kay claimed the means of
interpretation should come along, and that other interpretations stay open. So
Hickey's attack partly misses. It does not wholly miss. Kay's wider position
(data structures "don't scale well"; "data" scales "terribly") leans the way
Hickey fears.

### 1.5 What stayed unresolved

1. **Safety of a transmitted meaning.** Kay asked it. Hickey joked about it.
   Nobody worked on it. Note that this is the life's work of Mark S. Miller's
   camp, in Part two.
2. **Minimal sufficiency.** Hickey lists the sub-questions: time on every
   datum, labels in-band or out-of-band, per datom or per dataset, provenance.
   Kay asks the mirror question: the "smallest thing that could be universal",
   with structure only on the "envelopes". Both walked up to the envelope
   question from opposite sides. Neither answered it.
3. **The bottom of the regress.** What interprets the interpreter? Kay: a good
   question. Hickey: shared words. Neither is an engineering answer.
4. **Time.** Hickey's "as-of" and Kay's pointers to McCarthy's fluents, Reed's
   thesis, and Worlds are the same family of idea: state as a series of
   immutable versions along a time line. Kay tells functional programmers they
   need "a much better idea of time". That is what Hickey's work is about. They
   agree here more than the thread shows, and never noticed.
5. **Whether meaning can be fixed at writing time at all.** Hickey's
   seismometer point: often the writer does not know what the record will come
   to mean. Kay's only reply was "how do they know they are even bits?" That
   answers the bottom of the stack, not the top.

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

**Second, that axis is the reason Sid's table exists.** Sid's sentence,
"whatever is not written into a fact when it is made is gone for every earlier
fact", is Kay's Turn 3 with the future as the receiver. A store that never
rewrites turns every record into a message to a reader who is not there yet:
a person in year eight, the hundredth rebuild of the runtime, a second store.
Hickey's list in Turn 9 (time, in-band or out-of-band labels, per datom or per
dataset, provenance) is Sid's table in one sentence, written in 2016 as an open
question by the person best placed to answer it.

**Third, it decides some rows and is silent on others.** My sorting:

| Axis | Rows it governs |
|---|---|
| A. What travels with the record (Kay / Hickey) | (3) key, (12) runtime version, (13) storage shape, (17) first facts, (7) the gate's verdict, the grammar-and-tool-version part of (4) and (11 based on); above the table: interpreters as facts, the fixed envelope |
| B. Who decides before and after | (0) the log, (11 when) the clock, (10) order and partitions, (5) index lag |
| C. Authority from who you are, or from what you hold | (8) by whom, (15) the click, (16) who sees |
| D. Ids: assigned, minted, or derived | (2) entity ids, (1) fact ids, (6) "replacing 25" |
| (mixed) | (9) deletion sits on B and D; (14) the hand sits on A and C |

So it is one of four. It is the one that frames the whole job. It does not
answer the order rows or the authority rows.

**Fourth, the thread's two loose ends are Sid's authority rows.** Hickey, in
passing: "Some metadata indicated to me that you said it (should I trust
that?)". That is question (8). Kay's closing question on the safety of a
transmitted meaning, with Hickey's "I don't trust you enough to run it", is
question (15). Neither of them picks these up. The capability camp did. In that
sense Part two continues the thread.

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

1. The pointer keeps meaning the same thing forever, and in another store.
   That is rows (1) and (17).
2. The thing pointed at can be run by a runtime that does not exist yet. That
   is row (12), and the runtime that is not a fact.
3. The pointer is on every fact from the first one. That is rows (3), (4),
   and (11 based on).

### 1.7 What each would make of Sid's design (all INFERRED)

**Hickey.** Hickey would recognise it as close to home ground. Immutable,
accreting facts. Time on every fact. Provenance. Derivations kept apart from
facts, and recorded only with an as-of. "Running answers never stored, only
crossings recorded" is Hickey's date-of-birth and age distinction made into a rule.
A crossing is a derivation that has been "temporally-qualified". Hickey would
press on four things.

- *The nine-part envelope.* Hickey's own unit is smaller. REPORTED
  (Deconstructing the Database, 2012): "we call that a Datom. But it is just an
  entity, an attribute, a value, and some path to time. We use the transaction,
  because it is also a path to other information about what happened, including
  provenance, or causality, or operations, or anything else like that." And:
  "you want it to be minimal." INFERRED: Hickey would keep entity, key, value, and a
  path to the admission. Hickey would hang by-whom, based-on, and because-of on the
  admission as ordinary facts. Then the provenance theory can grow without
  touching the envelope.
- *"Fixed forever".* REPORTED (Spec-ulation, 2016): growth is "provide more"
  and "require less"; "Breaking changes are broken"; "If you say you cannot do
  X, it means you can never do X." INFERRED: an envelope may gain optional
  parts. It may never demand a new part of old facts. Readers must carry parts
  they do not understand.
- *Parsing without the grammar.* REPORTED (The Language of the System, 2012):
  "If you have out of band schemas, what can't you have? You can't have these
  things: generic processors and intermediaries." INFERRED: a value must be
  readable as plain self-describing data without first fetching its grammar
  fact. The grammar validates. It must not be needed to parse. That is an answer
  to (13).
- *Names.* REPORTED (same talk): "the globally qualified name space names will
  be the identity names. [...] The value names, you want to be conflict free,
  tear off names that anyone can create without coordination, and that's what a
  UUID is about." And: "What should you care about, about a value name? Nothing
  at all." Hickey's own system answers (3) with both: an attribute is an entity with
  an id, and its name is a fact about it. INSTITUTIONAL (Datomic docs, checked):
  after a rename, "Both the new ident and the old ident will refer to the
  entity"; and "You can never alter :db/valueType". So the name can move. The
  shape cannot.
- On never rewriting, Hickey's own product gave ground. INSTITUTIONAL (Datomic docs,
  checked): "Excision is the complete removal of a set of datoms matching a
  predicate"; "Privacy laws might require you to excise data"; and "the excise
  attributes themselves are protected from excision, so there is no way to
  'erase your tracks'. Every excision creates a permanent record." That is a
  worked answer to (0) and (9): never lost, rarely and visibly cut.

**Kay.** Kay would first see a store of facts and say "data", the idea that
"scales terribly". Then Kay would notice three moves the AMA asks for. Interpreters
are findable, by pointer, in the same place. Work is matched, not sent: that is
Kay's "programming by message receiving", and Sid's match-don't-route is a
candidate answer to Kay's "what is the similar idea [to Linda] scaled for 40
years later?" And the medium aims to be understood and rebuilt from inside,
which is the Smalltalk and STEPS aim. Kay would press on three things.

- *The runtime that is not a fact.* This is Kay's regress. Tool bodies are facts,
  but in what language, run by what? The store will outlive a hundred runtimes.
  Kay's direction: make the bottom turtle tiny, universal, and written down, so
  any future runtime is one more implementation of it. The 1978 image came back
  because its machine was small and specified, and the image carried the rest.
  INFERRED: the first facts (17) should include, or anchor to, the definition of
  the body language. "Re-derivable from the same reads" is only true relative
  to that definition.
- *The envelope.* Kay is not against a fixed envelope. The TCP/IP remark is the
  strongest case for one in the thread. But Kay's envelope is minimal, puts no
  structure on the message, and has a seam so that no "single theory" is
  forced. Nine parts, with three kinds of based-on, is already a theory of
  provenance. INFERRED: Kay would ask which parts are the IP of this system
  (needed by everyone to admit and order a fact) and which are its TCP (one
  theory, replaceable, above the seam).
- *One store.* Kay's model is the Internet, which has no centre. INFERRED: Kay
  would design for parties that have never met, which means the second store is
  the design case, not the rare case. For Sid this lands on ids: a gate-assigned
  number for a grammar means nothing in another store. An id that travels
  (minted globally unique, or derived from content) does.

**Where the two agree against Sid's draft.** Both would shrink the fixed core.
Kay, for universality between strangers. Hickey, for minimal novelty and growth
by accretion. Neither would sign "nine parts forever". Both would sign "a tiny
core forever, and everything else open". Both would also say the same thing
about the runtime, from opposite sides. Kay: write down the bottom turtle.
Hickey: a derivation made by an interpreter that can change "can produce
results that don't add up", so the interpreter's version must be among the
reads of every crossing. Both land on row (12): yes, record it.

---

## Part two — the people

Order is by how much each bears on Sid's table, not by fame. For each: what
they built, their reasons in their words, what they changed or regret, which of
Sid's questions they speak to, how far the transfer is honest, and who they
disagree with.

### 2.1 The capability camp: Hardy, Miller, Yee, Shapiro, Donnelley, Karp, Lemmer-Webber

This is the camp that picks up the thread's two loose ends. It is also the camp
with the strongest objection to Sid's gate as described.

**What they built.** Norm Hardy worked on capability operating systems at
Tymshare (the confused-deputy story is a real incident there). Mark S. Miller
was a Xanadu architect, then built the E language and wrote the thesis "Robust
Composition" (2006). Christine Lemmer-Webber co-edited the ActivityPub
standard, then founded Spritely and works on the OCapN protocol. Kenton Varda's Sandstorm
(2.2) is this camp's most complete product for ordinary people.

**Their reasons, in their words.**

- Hardy, "The Confused Deputy" (1988), http://cap-lore.com/CapTheory/ConfusedDeputy.html.
  A compiler was allowed to write a billing file. A user passed the billing
  file's name as the output file. The compiler overwrote it. REPORTED: "The
  fundamental problem is that the compiler runs with authority stemming from two
  sources." And: "The compiler serves two masters and carries some authority
  from each to perform its respective duties. It has no way to keep them apart.
  [...] The compiler had no way of expressing these intents!"
- Hardy on what happens when you patch a who-may-do-what rule set. REPORTED:
  "Every time we added a clause enabling the opening of a file in a categorical
  situation we would introduce security problems in programs that had been
  secure. Every time we added restrictions to these categories we broke other
  legitimate programs. The last time that I wrote down the requirements for a
  program to open a file, it required fourteen boolean operators".
- Miller, Yee, Shapiro, "Capability Myths Demolished" (2003),
  https://papers.agoric.com/assets/pdf/papers/capability-myths-demolished.pdf.
  REPORTED: "We will use the term ambient authority to describe authority that
  is exercised, but not selected, by its user." On the deputy: "The problem is
  not caused by the compiler using access that it should not have. The problem
  is that it exercises its authority to write to BILL for the wrong purpose."
  And: "If subjects cannot identify the authorities they are using, then they
  cannot associate with each authority the purpose for which it is to be used.
  Without such knowledge, a subject cannot safely use an authority on another
  party's behalf."
- The alternative, same paper. REPORTED: "every capability can serve both to
  designate which resource to access, and to provide the authority to perform
  that access. This change provides us with the option to avoid introducing a
  shared namespace into the foundations of the model, and thereby avoid the
  complex issues involved in managing a shared namespace – issues rarely
  acknowledged as a cost of non-capability models."
- Miller, "Robust Composition" §3.1, against a static policy table. REPORTED:
  "you often do not know in advance what authorities the program actually
  needs: the least authority needed by the program changes as execution
  progresses". And: "we must provide the right amount of authority
  just-in-time".
- Lemmer-Webber, "OcapPub", https://gitlab.com/spritely/ocappub. This is the
  cleanest statement of the split Sid's "by whom" needs. REPORTED: "we need
  identity verification when it is important to know that a certain entity
  "said a particular thing", but it is important to understand that this is not
  the same as knowing whether a particular entity "can do a certain thing".
  Mixing up identity verification with authorization is how we get ACLs, and
  ACLs have serious problems." And on programs that run as you: "Solitaire
  [...] has the full authority to betray you, because it has the full authority
  to do everything you can... it /runs as you/."

**How they keep "who did it" without identity-based access: Horton.** Miller,
Donnelley, Karp, "Delegating Responsibility in Digital Systems: Horton's 'Who
Done It?'" (2007),
https://www.usenix.org/legacy/event/hotsec07/tech/full_papers/miller/miller.pdf.

- They state the old objection fairly. REPORTED: "Because ocaps operate on an
  anonymous "bearer right" basis, they seem to make reactive control
  impossible. [...] a remaining unrefuted criticism is that they cannot record
  who to blame for which action".
- The mechanism: each side logs, locally, who it holds responsible. Delegation
  passes authority and responsibility together. REPORTED: "today we have no
  means for delegating responsibility, that is, delegating authority coupled
  with assigning responsibility for using that authority." And: "Carol tags S3
  with Bob's Who, so Carol can blame Bob for messages sent to S3."
- A deliberate limit. REPORTED: "To avoid non-repudiation [1], we accept that
  Bob can log bad data fooling himself into blaming the wrong party." In this
  camp "by whom" is a belief held by the party keeping the log. It is not a
  proof to the world.
- Varda gives the same shape in plain terms (Hacker News, 2018, item 16098698).
  REPORTED: "If you have a table mapping user identities to roles, that's an
  ACL, not a capability system." Then: actions through a delegated capability
  are logged "via Bob". If Bob delegates to Carol, "you might see "via Bob; via
  Carol". This means: "Bob claims that Carol performed this action." No one
  other than Bob actually needs to know who "Carol" is [...] Since the assertion
  in the audit log says "via Bob" first, we know to hold Bob responsible first.
  We only care about Carol to the extent that we trust Bob."

**What they changed or regret.**

- Hardy's team first patched the deputy with a "switch hats" call. REPORTED:
  "Note the increase in complexity! [...] It soon became clear, however, that
  more than two "authorities" were necessary".
- The accountability gap stood for about twenty years before Horton answered
  it. The authors say so.
- Lemmer-Webber on ActivityPub, as its co-editor. REPORTED (OcapPub): "Authorization is also
  not specified". The essay calls the last-minute `sharedInbox` addition "the primary
  mistake", because the receiving server decides who gets a message, which
  "breaks the actor model". The retrofit did not land, so Spritely now makes
  capability security "what falls out of Spritely's tech when you write a
  program in it."
- Lemmer-Webber on how early choices set. REPORTED, "Re: Re: Bluesky and
  Decentralization" (2024), https://dustycloud.org/blog/re-re-bluesky-decentralization/:
  "I consider the decision to have blocks be publicly queryable to be an example
  of emergent behavior from initial decisions... early architectural decisions
  can have long-standing architectural results, and while many things can be
  changed, some things are particularly difficult to change form an initial
  starting point." (The typo is in the original.)

**What they would say to Sid's questions.**

- (8) by whom. REPORTED: identity is needed to know who said a thing; it must
  not be what decides whether a thing may be done. INFERRED: "by whom" should be
  a chain, not a name. The gate can vouch only for the head of the chain: the
  party that presented the offer over an authenticated channel. Every later
  link is a claim by the link before it. A person's agent is itself, with its
  own id, and never the person. "Acts for" is a grant: a fact written by the
  person, which the agent's offers cite. Both the person and the instrument are
  then on every fact.
- (15) the click. REPORTED (Sandstorm Powerbox docs, 2.2): the system never
  asks "is it OK?"; it asks "which one?", and the choice is the grant. INFERRED:
  no tool ever acts "in a person's name". A click is a designation that grants.
  The click fact names the person, the things chosen, and the tool. The tool's
  offers cite that click as their authority. Tools that fire by match with no
  click run under standing grants that are separate, narrow, and revocable.
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
- (3) names. Stiegler's petnames, REPORTED: "a key that is global and securely
  unique (but not necessarily memorable); a nickname that is global and
  memorable (but not at all unique), and a petname that is securely unique and
  memorable (but private, not global)." INFERRED: Sid's layers give this for
  free. The key's id is the key. Its public name is a nickname fact in base. A
  person's own word for it is a petname fact in their layer. Nearest layer wins
  is petname lookup.
- (2) ids. INFERRED: ids in Sid's store get copied into based-on lists,
  verdicts, and crossings. So a bare id must never grant anything. Knowing an id
  must not be enough to read or write it. MyWebstrates made the id the
  capability and found (3.3) that access could then never be revoked.

**Transfer: what is like Sid's case and what is not.** Their systems are live
objects passing references. Sid's is inert facts and one gate. A central gate
that decides is, to this camp, the access-list architecture. But a gate can
check capability-shaped policy: a chain of grant facts cited by the offer.
Miller, Yee, and Shapiro criticise certificate schemes where the permission is
not bound to the request, so purpose cannot be known. An offer that cites its
grant is bound to the request. The other difference: this camp avoids global
logs and non-repudiation on purpose. Sid wants a permanent record. Horton shows
they accept logs; they do not accept logs as proof.

**Who they disagree with.** Access-list and role-based designs in general
(Hardy: "Exercise for the reader: Show that access lists do not solve this
problem"). Boebert and Gong on whether capabilities can confine. Lemmer-Webber
against Bluesky's architecture (below), and against four "anti-solutions":
blocklists, content filtering, reputation scoring, re-centralisation. Inside
the camp, Varda against purists (HN 16093457): "capability people tend to get
too extreme, and this tends to lead to failure. With Sandstorm we've tried to be
more pragmatic." And (HN 16098698): "I don't necessarily recommend exposing pure capabilities
in a UI for end users."

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
- INFERRED: this cuts both ways for Sid. A shared heap is coherent when there is
  one heap. The fan-out cost is a cost of many full copies. So these numbers
  support "one store" as a design that hangs together, and they say the second
  store must not be a second full heap. Between stores, deliver by address. The essay's
  other point stands against one store: the operator sees everything, and a
  person cannot credibly leave. Facts whose ids and authorship only the gate
  can vouch for mean nothing outside the gate's store.

### 2.2 Kenton Varda: Protocol Buffers, Cap'n Proto, Sandstorm, Durable Objects

**What Varda built.** Varda wrote proto2 and open-sourced Protocol Buffers at Google.
Then came Cap'n Proto, Sandstorm, and Cloudflare's Durable Objects. Varda has
lived with wire formats, a capability platform for ordinary users, and
one-writer-per-id storage, each for years, and writes about what went wrong.

**Keys: numbers, not names; never reuse.**

- INSTITUTIONAL (protobuf.dev, proto3 guide): "This number cannot be changed
  once your message type is in use because it identifies the field in the
  message wire format." "Field numbers should never be reused." Reuse "makes
  decoding wire-format messages ambiguous", with consequences listed as "A
  parse/merge error (best case scenario) / Leaked PII/SPII / Data corruption".
  Best-practices page: "Even if you think no one is using the field, don't
  re-use a tag number. If the change was live ever, there could be serialized
  versions of your proto in a log somewhere."
- REPORTED (Varda, Cap'n Proto language reference,
  https://capnproto.org/language.html#unique-ids), on why types get a 64-bit id
  and not a global symbolic name: "Programmers often feel the need to change
  symbolic names and organization in order to make their code cleaner, but the
  renamed code should still work with existing encoded data. It's easy for
  symbolic names to collide, and these collisions could be hard to detect in a
  large distributed system with many different binaries using different versions
  of protocols. Fully-qualified type names may be large and waste space when
  transmitted on the wire." Ids are made with `capnp id`, at random. By default
  an inner declaration's id is derived from the parent's id and the name; you
  pin it by hand only "if that declaration has been renamed or moved and you
  want the ID to stay the same". Varda adds: "Collisions from misuse (e.g. copying
  an example without changing the ID) are much more likely" than random ones.

**Unknown fields: dropped, then restored.** This is a natural experiment on
(13).

- REPORTED (Varda, 2014): "Apparently, version 3 of Protocol Buffers, aka
  "proto3", removes this feature. I honestly don't know what they're thinking.
  This feature has been absolutely essential in many of Google's internal
  systems."
- INSTITUTIONAL (Jisi Liu of the protobuf team, 2016, issue 272): the drop was
  to let implementations choose, which "simplifies implementations and enables
  struct-like API". That is a storage-shape argument: closed, class-like
  records.
- INSTITUTIONAL (release notes v3.5.0, Nov 2017): "Unknown fields are now
  preserved in proto3 for most of the language implementations for proto3 by
  default." The present docs still warn how they get lost: "Serialize a proto to
  JSON. Iterate over all of the fields in a message to populate a new message."
- Lesson, INFERRED: a reader built from closed shapes loses what it does not
  understand. Anything that copies or forwards facts (a backup, an index
  builder, a second store, a runtime rebuild) must carry unknown parts through
  untouched. Plain open maps do this by default. Classes do not.

**"Required" and the fixed envelope.**

- REPORTED (Varda, Cap'n Proto FAQ): "the "required" keyword in Protocol Buffers
  turned out to be a horrible mistake." "A field declared required,
  unfortunately, is required everywhere. The validation is baked into the
  parser." "Things like this have actually happened. At Google. Many times."
  "Low-level infrastructure that doesn't care about message content should not
  validate it at all."
- REPORTED (Varda, HN 18196288, 2018): "A piece of data should be validated by
  the consumer, but should not be validated by pass-through middlemen." And Varda
  names names: "including Jeff and Sanjay when they first designed protobufs".
- INSTITUTIONAL (protobuf.dev proto2 guide, heading "Required Is Forever"): "It
  is nearly impossible to safely change a field from required to optional."
- REPORTED (same HN comment), directly on envelopes: "there is no header /
  container around a protobuf. This is one of the best properties of protobufs,
  because it makes them compose nicely with other systems that already have
  their own metadata [...] Adding required metadata wastes bytes and creates
  ugly redundancy." And on why the format never changed: "the benefits of
  introducing a whole new encoding were never shown to be worth the inevitable
  cost".
- INSTITUTIONAL (protobuf best practices), against one shape for storage and
  for the wire: "the needs of long-term storage and live RPC services tend to
  later diverge. Using separate types even if they are largely duplicative
  initially gives freedom to change your storage format without impacting your
  external clients."
- What this says to Sid, INFERRED. The gate is a consumer, so the gate may
  validate. Nothing else on the path may. "Because of: always filled" is a
  required field. Varda's history says: do not bake requiredness into the
  encoding. Make it a gate policy, which is a fact and can change. And "a fixed
  envelope both ends share forever" is two decisions folded into one: what is
  stored, and what is offered and returned. Google's advice is to let those
  differ from the start.

**One writer per id.** Durable Objects give each id one single-threaded object.

- REPORTED (Varda, Cloudflare blog, 2024): "they are intended to scale out, not
  up. A single object is inherently limited in throughput since it runs on a
  single thread of a single machine [...] consider a vote counter with a million
  users all trying to cast votes at once. To handle such cases with Durable
  Objects, you would need to create a set of objects that each handle a subset
  of traffic and then replicate state to each other."
- REPORTED (2020): "Each object can see only its own data. To perform a query or
  transaction across multiple objects, the application needs to do some extra
  work."
- REPORTED (2021), on a race found even with one thread: "rather than fix
  the apps, we decided to fix the model." The fix: "When a storage write
  operation is in progress, any new outgoing network messages will be held back
  until the write has completed [...] it is impossible for anything else in the
  world to observe a premature confirmation." This is "no optimism" built into a
  runtime.
- What this says to (10), INFERRED. One orderer per unit is right, and the unit
  must be small. The only thing that strictly needs one orderer is the
  compare-and-set cell: entity, key, layer. Anything wider (a whole layer, the
  base) is a choice that caps throughput at one thread. A planet-wide base layer
  ordered as one unit is Varda's million-vote counter. And once units are small, "as
  of" across them is a position per unit, not one number, unless something
  global hands out numbers.

**Ids.** INSTITUTIONAL (Cloudflare docs): an id made from a name needs a global
uniqueness check: "this round-the-world check can take up to a few hundred
milliseconds. newUniqueId can skip this check." Jurisdiction is minted into the
id (`newUniqueId({ jurisdiction: "eu" })`): an id that says where, on purpose,
for law. And a live case of Sid's exact worry: "Alarms created before
2026-03-15 do not have name stored. When such an alarm fires, ctx.id.name will
be undefined".

**The click.** INSTITUTIONAL (Sandstorm Powerbox docs): "the user is never
presented with a yes/no security dialog. Sandstorm does NOT ask: "Is it OK for
this app to access your calendar? yes/no" Instead, Sandstorm asks: "Which
calendar should the app use?" If the user chooses a calendar, they are obviously
indicating that they want to grant access".

**Regrets, in Varda's words.** `required` ("a horrible mistake"). Varda retracts the
well-known message-bus example (HN 32818948): "I kind of wish I didn't say "message bus" in
that example". On Sandstorm the company (2017) Varda blames the business, not the
capability model. And the general stance (HN 18190005): "there is no such thing
as a successful system which was designed perfectly upfront. All successful
systems become successful by evolving, and thus you will always see this kind of
wart in anything that works well." I found no stated regret about Cap'n Proto.

**Transfer.** Varda's formats move between programs that are upgraded at different
times. That is Sid's case across a hundred runtime rebuilds. Varda's one-writer
objects are small and many; Sid's gate is drawn as one. Who Varda disagrees with:
type theorists ("version negotiation [...] is extremely painful in practice"),
the proto3 designers on unknown fields, and Jeff Dean and Sanjay Ghemawat on
`required`.

### 2.3 Unison (Paul Chiusano, Rúnar Bjarnason, Arya Irani), and Git's hash

**What they built.** A language where every definition is identified by a hash
of its syntax tree. Names are metadata kept apart. They have run it for about
ten years. They are the best evidence on question (1), because they ran both
kinds of id and changed their minds.

**Reasons.** REPORTED (Unison docs, "The big idea"): "names are just separately
stored metadata that don't affect the function's hash." And: "Names are like
pointers to addresses in this space. We can change what address a name points
to, but the contents of each address are forever unchanging." Chiusano (2014):
"We need not all agree on the metadata associated with each term".

**The flip: an id computed from content, or minted at random?** Unison types
can be `structural` (the id is a hash of the shape) or `unique` (a random id is
minted and mixed in).

- 2021: Chiusano opens issue 2251: "It's very common to leave off `unique` for
  types that really should be." They made both keywords mandatory for two
  years.
- 2023–24: issue 4539 makes `unique` the default. REPORTED (GitHub user ceedubs, who opened the issue):
  "`type UserId = UserId Nat` and `type EpochMillis = EpochMillis Nat` are
  definitely not intended to be treated as the same type". Structural types
  "tend to come up much more in core libraries like `base` than in user/app
  code." Bjarnason: "Sgtm". Chiusano: "Sounds good to me too! It's time."
- The cost of random ids, which they also paid (issue 2196): "A unique type
  definition not deleted from a scratch file will always be treated as an update
  [...] after an `add`." Showing the id to people was judged "unpalatable". The
  fix re-derives the random id from name plus shape: "if the type has the same
  structure and the same name, it gets the same guid and hash."
- Lesson, INFERRED. A hash is the right id for a thing whose identity *is* its
  content: a tool body, a grammar version, a definition of "stale". It is the
  wrong id for a thing whose identity is an act or an intent. Two people who
  assert the same thing have made two facts. Two entities with the same
  description are two entities. But a purely random id makes an honest retry
  look like a new thing. For agents writing at machine rate that matters: a
  content hash of the *offer* makes a retry idempotent. So there are two ids
  doing two jobs: the offer's own id, made by its maker, which travels; and the
  gate's number, which is local.

**Both ids, and a translation table.** REPORTED (codebase format v2 doc): "most
objects in the v2 codebase format are referenced by `object.id`", a local
integer, with hashes as the portable identity. Syncing walks "`ObjectId 3` ->
`HashId 14` -> `#asodcj3` -> `HashId 16` -> `ObjectId 2`". MyWebstrates reached
the same place from the other side (3.3): "incremental version numbers are more
usable but are only locally valid, while version hashes are globally valid."

**What is not in the id at birth is gone.** REPORTED (Chiusano, issue 2276,
2021): "The definitions don't have their type signature baked into the hash
because it matches what was inferred when the definition was added. Now that the
inference algorithm is different… In general, there seems to be no way to
replace the 'variant' associate with a hash." This is Sid's sentence, met in the
field.

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

**Where "replaces X" lived (6).** Not on the new thing. REPORTED: "replacements
are tracked in Unison patches [...] Patches identify their replacements by hash
instead of by name". And: "the old version of a definition doesn't have to be
available to use the patch."

**No algorithm tag in the id.** Chiusano opened "Add version info to Hashes" in
2019, proposing `<multibase><unison-multicodec-id><unison-version-id><multihash>`.
It is still open. They later ran a full rehash of codebases with an old-to-new
translation table.

**Git, the same lesson at larger scale.** INSTITUTIONAL (Git,
hash-function-transition): "The signed payload for signed commits and tags does
not explicitly name the hash used to identify objects. If some day Git adopts a
new hash function with the same length [...] the intent behind the PGP signed
payload in an object signature is unclear [...] Fortunately SHA-256 and SHA-1
have different lengths." They are relying on a length accident as the tag. On
keeping two hashes side by side: "they will never go away, so they accumulate."
On converting old objects: the conversion "retains any brokenness in the
original object [...] This is a deliberate feature of the design to allow the
conversion to round-trip."

**Transfer.** Unison stores code, not claims by people; it has no deletion
problem and no authority problem. Its ids are made by many machines with no
gate. Sid has a gate, which makes store-assigned numbers cheap; Unison's
experience says a number alone is not enough once a second store exists.
Disagreements: Irani against Chiusano on whether hashes may ever change;
contributors against each other on whether structural types are worth having
("it's extremely rare that they _are_ the right solution").

### 2.4 IPFS and IPLD (Juan Benet), and what AT Protocol did with them

**What they built.** Content-addressed storage where an id is a hash, links are
ids, and mutable names are signed pointers. AT Protocol (Bluesky) then built
signed personal repositories on IPLD-style hashes, ran them as a large public
social network, and walked part of it back.

**Self-describing ids.** REPORTED (Benet, 2014, the multihash proposal): "As
time passes, software that uses a particular hash function will often need to
upgrade [...] This introduces large costs: systems may assume a particular hash
size, or call `sha1` all over the place." Whitepaper §3.1: "Rather than locking
the system to a particular set of function choices, IPFS favors self-describing
values." The costs, INSTITUTIONAL (multihash README): "multihash values bias
the first two bytes"; and "Obsolete and deprecated hash functions are included
[...] since many such hashes already exist". A scheme, once used, never
retires.

**An id format change never finishes.** INSTITUTIONAL (CID spec): "there will be
no CIDv18 (0x12 = 18) to prevent ambiguity with decoded CIDv0s." A version
number is burned forever so that old ids still parse. The IPFS docs still say
CIDv1 will become the default "in the near future", about a decade on. did:plc
(Bluesky's identity log): "there exist many did:plc identifiers where the DID
identifier itself is based on the hash of the old format, so they will
unfortunately be around forever."

**A content hash names an encoding, not a value.** INSTITUTIONAL (IPFS docs):
"Two identical files can produce different CIDs. The CID depends on both the
content *and* how that content is structured". DAG-CBOR spec: it "requires that
there exist a single, canonical way of encoding any given set of data". Then the
walk-back: "Due to the existence and active use of historical data, and the
existence and active use of non-conforming encoders, DAG-CBOR decoders may relax
strictness requirements by default." INFERRED: if any id in Sid's store is a
hash, the exact bytes that are hashed must be fixed before the first one. Sid
has an advantage IPFS lacked: one gate can refuse any offer that is not in
canonical form.

**Deletion under content addressing.**

- REPORTED (Jorropo, IPFS maintainer, forum 2022): "You cannot delete files from
  HTTP. Because if an other server you don't control host the data, you cannot
  delete it from that server. [...] for IPFS things are exactly the same". The
  difference Jorropo grants: with hashes, a surviving copy is found and verified
  automatically.
- INSTITUTIONAL (IPFS docs): "information about which nodes are retrieving
  and/or reproviding which CIDs is publicly available." And on encrypting
  before hashing: "Future breakthroughs in computing might allow going back and
  decrypting older content".
- Guessing. The gatherer found no single official IPFS sentence on it. The
  pieces are sourced: small content hashes directly to its id, and ids are
  public. INFERRED (mine): if a fact's id is a hash of its content, and the
  content is low in surprise ("person X has condition Y"), then anyone can guess
  the content, compute the id, and confirm it, even after the content is cut
  out, because the id survives in every based-on list that named it. The only
  defence is a random salt inside the hashed content, erased along with it.
  That has to be there from the first record.

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
- INSTITUTIONAL (repository spec): "Record deletion is supported without leaving
  a trace or 'tombstone' of previous contents." And a hazard of copies:
  "previously-deleted records re-appearing via CAR import from an unrelated
  account."
- INSTITUTIONAL (2025): "sync v1.1 relays are now 'non-archival'."
- What it says to (6), INFERRED. A strong back-pointer (the hash of the record
  you replace) makes the old record impossible to remove without breaking the
  chain. AT Protocol removed it for that reason. "Replacing 25" kept as a plain
  number does not have this problem. Kept as a hash, it does.
- A dead slot forever: `prev` is now required and always null.

**Ids that say when.** INSTITUTIONAL (AT Protocol TID and record-key specs):
"Implementations should not rely on global uniqueness of TIDs, and should not
trust TID timestamps as actual record creation timestamps. Record keys are
'user-controlled data'". did:plc spec: "The timestamp metadata encoded in the
PLC audit log could be cross-verified against network traffic or other
information to de-anonymize account holders. It also makes the 'identity
creation date' public." Note did:plc itself is a one-gate, append-only log: the
server "either rejects the operation or accepts and permanently stores the
operation, along with a server-generated timestamp", and even nullified
operations "can be enumerated and audited".

**Transfer.** IPFS and AT Protocol have many copy-holders that no one controls.
Sid has one store, so cutting a fact out is possible in a way it is not for
them. What carries over whole: the encoding lesson, the algorithm-tag lesson,
the salt, and the warning about strong back-pointers. Disagreement inside this
line: Benet's "Objects are permanent" against Newbold's removal of history
because permanence "breaks human intents and expectations".

### 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintscher)

**What they built.** One store for everyone. INSTITUTIONAL (WMF pages): the
query graph "comprises over 16 billion triples", and "Wikidata sees around 1
million edits per day", many of them by bots and mass-edit tools. Every item
and every property is an opaque id. Labels are data about the id. Statements
carry qualifiers, references, and a rank. It has run since 2012. It is the
closest thing to Sid's base layer that exists.

**Reasons.**

- Ids. REPORTED (Vrandečić and Krötzsch, CACM 2014): "Item IDs can be used as
  language-independent identifiers"; "IDs do not depend on language labels;
  items can be deleted, though IDs are never reused". The stated reasons are
  language and stability. I could not find any stated concern that the ids leak
  creation order. They are sequential, so they do.
- Claims, not truth. REPORTED (CACM 2014): "there is no "true population of
  Rome" but rather a "population of Rome as published by the city of Rome in
  2011."" And: "Wikidata intends to represent all views rather than choose one
  "true" claim." Also: "Wikidata does not automatically record provenance but
  does provide for the structural representation of references."
- Statement ids. INSTITUTIONAL (Wikibase JSON docs): "An arbitrary identifier
  for the Statement [...] No assumptions can and shall be made about the
  identifier's structure". In practice it is the entity id, a `$`, and a random
  UUID. Values and references inside a statement carry a content hash. So:
  random ids for the act of asserting, hashes for the content. Same split as
  Unison.

**What they do instead of deleting or overwriting.**

- Ranks. INSTITUTIONAL (Help:Deprecation): "Often, property values in Wikidata
  should be ranked as deprecated, not removed." One benefit listed: "it allows
  other users to know not to re-add the value". The reason must be attached: "A
  deprecated value should always have a P2241 qualifier." And a sharp line: a
  value that was right and is now out of date is *not* deprecated. It gets start
  and end dates.
- INFERRED: that is three different things that Sid's "superseded = same cell,
  later version" folds into one. A newer value. A value that was wrong. A value
  that was right then. If 37 replaces 25 without saying which, no reader can
  tell later. Wikidata had to add the reason. So for (6): keep "replacing 25" on
  37, and keep *why*.
- Merges. INSTITUTIONAL (Help:Redirects): "Under no circumstances should
  redirects be deleted or repurposed for another object. Deleting them would
  mean invalidating possible references". Ids for life will still be minted
  twice for one thing, often, when ten million papers arrive. Pointers to the
  losing id cannot be rewritten in a store that never rewrites. So how a merge
  is said, and how every reader resolves it, is a day-one convention.
- Privacy removal exists even here. INSTITUTIONAL (Meta oversight policy):
  suppression hides revisions from all but a small named group; it is "clearly
  identified in the page history what edits had been suppressed"; and
  "Suppressions are logged privately."

**A key's shape is forever.** INSTITUTIONAL (Help:Data type): apart from one
narrow case, "Other changes of data type requires creating a new property and
deleting the old one." Datomic says the same in its own words (1.7). Two
unrelated systems at scale: the name of a key may move; the kind of value it
holds may not. A breaking change to shape means a new key.

**Definitions as facts in the same store: they did it, and it hurt a little.**
In July 2017 constraints moved from wiki templates to statements on the property
itself. INSTITUTIONAL (constraints portal): "Constraints are hints, not firm
restrictions". REPORTED (Lucas Werkmeister's announcement): some properties
"have so many P2303 qualifiers that they don't fit in the constraint database,
so that a constraint check on any item with a statement for one of these
properties crashes". A community member in the same thread: one property's page
grew from 20,508 bytes to 631,637. Note that Wikidata checks *after* admission
and only warns. Sid's gate checks *at* admission and refuses.

**What hurt at scale.**

- The index, not the log. INSTITUTIONAL (WMF Search Platform, Oct 2023): growth
  of "roughly 1 billion triples per year"; "it took us ~3 months to reload data
  from scratch"; scholarly articles are about half of all triples and "affect
  only about 2% of queries". The graph was split on 9 May 2025. Cross-graph
  questions now need federation. The rule for the split is itself a statement
  (instance of: scholarly article). This is Sid's "whole field seeded from ten
  million papers" case, already lived: the seed dominated the index and served
  almost nobody.
- The history tables. REPORTED (Pintscher, 2026 request for comment): "Wikidata
  is currently stretching the limits of what it technically and socially can
  hold." "There are database tables (revision and terms table) that are near
  the limits of their scalability [...] The worst case scenario here is taking
  down not just Wikidata but also Wikipedia". And the social limit: "a single
  active editor has to oversee nearly 10.000 Items".
- By whom, when a tool writes under a person's account. REPORTED (same): "our
  bot policy is ineffective. A lot of mass editing is done by non-bot accounts
  but is effectively unregulated bot work [...] We are thereby effectively
  rewarding evading the bot policy." This is question (8) answered by pain. The
  record kept the person and lost the instrument.
- A provenance slot filled by machine became noise. INSTITUTIONAL
  (Help:Sources): "Statements that are only supported by "P143" are not
  considered sourced statements". P143 is "imported from Wikimedia project",
  which bots fill in. It had to be ruled not-a-source. This bears on (11) and
  (4): a based-on list that the runtime fills with everything in reach will be
  read the same way.
- Types as ordinary statements. REPORTED (Brasileiro et al., 2016): "a
  significant number of problematic classification and taxonomic statements";
  their worked case makes Tim Berners-Lee an "instance of Profession(!)".
  Patel-Schneider and Doğan (2024): of 3,238 third-order classes, 3,159 are also
  second-order; "these numbers indicate that there are major errors." This is
  "no type slot; a type is just another entity that others point at", run at
  scale with open editing. The store works. The class tree cannot be trusted.
  Sid's tools match on key, not on type, and keys are gated by grammar. That
  sidesteps most of it.

**Regrets.** REPORTED ("Wikidata: The Making Of", 2023): "data uniformity and
coherency has emerged as one of the big challenges [...] Wikidata does not
enforce a fxed schema [...] it also leads to reduced coherence and uniformity
across groups of similar concepts, which is an obstacle to re-use." (The PDF
text drops "fi"; the word is "fixed".) Stored community queries, their Phase 3,
were never built: it "would have served as a forcing function to increase the
uniformity". And: "Originally, Vrandečić had not planned for a SPARQL query
service [...] Fortunately he was wrong." New kinds of entity were slow: lexemes
took from 2012 to 2018. Functions went to a different wiki altogether.

**Transfer.** Very close on substance: one store, opaque ids, claims with
sources, never-reused ids, machine-rate writers. Different on the gate:
Wikidata admits almost anything and reports problems later; its regret is
incoherence. That supports a gate that checks shape. Different on privacy:
almost everything is public. Disagreements: community members against the
constraints-as-statements move; ontologists against editor practice; Pintscher
against mass-edit tools; the founders against their own 2012 flexibility.

### 2.6 RDF, W3C PROV, nanopublications, trusty URIs

**What they built.** RDF gave every predicate a global id. PROV (W3C, 2013) is
a standard vocabulary for provenance; its parts map closely onto Sid's "by
whom", "based on", and "acts for". Nanopublications (Groth, Mons, Kuhn,
Dumontier and others) are small signed assertions, each with its provenance and
its publication info, stored in an append-only network under content-hash ids
called trusty URIs. They have run for about ten years.

**PROV: a read is not a dependence.** This is question (4), answered by a
standard. INSTITUTIONAL, PROV-DM, https://www.w3.org/TR/prov-dm/:

- Usage: "the beginning of utilizing an entity by an activity."
- Derivation: "a transformation of an entity into another, an update of an
  entity resulting in a new one, or the construction of a new entity based on a
  pre-existing entity."
- The cut between them: "If an artifact was used by an activity that also
  generated a new artifact, it does not always follow that the second artifact
  was derived from the first. In the activity of creating a painting, an artist
  may have mixed some paint that was never actually applied to the canvas: the
  painting would typically not be considered a derivation from the unused
  paint." And: "PROV does not attempt to specify the conditions under which
  derivations exist; rather, derivation is considered to have been determined by
  unspecified means. Thus, while a chain of usage and generation is necessary
  for a derivation to hold between entities, it is not sufficient".
- There is a catch-all, influence, and the standard says not to lean on it: "It
  is RECOMMENDED to adopt these more specific relations".
- INFERRED: two kinds of "based on" with two different sources. *Used* can be
  recorded by machinery: what was read, what the model was given. It is
  complete and noisy. *Derived from* can only be declared by the actor, and the
  actor can be wrong. For a deterministic tool the two are the same thing: its
  answer is a function of its reads. For a person or a model they differ, and
  only the usage list is reliable. Keep them as two relations. Do not let one
  pose as the other. Wikidata's P143 (2.5) shows what happens to a slot filled
  by machine and read as if it meant something.

**PROV: "acts for" is its own identified relation.** "Delegation is the
assignment of authority and responsibility to an agent (by itself or by another
agent) to carry out a specific activity as a delegate or representative, while
the agent it acts on behalf of retains some responsibility for the outcome of
the delegated work." It may be scoped to one activity. And they stop short on
purpose: "we do not say explicitly who bears responsibility and to what
degree." This matches the capability camp: delegation is a separate fact with
its own id, not a slot filled in on every record.

**PROV on the other rows.**

- (6): "A revision is a derivation for which the resulting entity is a revised
  version of some original." The pointer sits on the new thing. There is no
  forward "was replaced by".
- (7): "A bundle is a named set of provenance descriptions, and is itself an
  entity, so allowing provenance of provenance to be expressed."
- (9): "Invalidation is the start of the destruction, cessation, or expiry of an
  existing entity by an activity."
- No type slot, again: "PROV defines no plan-specific attributes." A plan is an
  entity that others point at as a plan.
- Every read? No. "Applications are free to decide which level of granularity
  they want describe". Nothing in PROV requires any read to be recorded. The
  nearest remark on the cost of not recording is Luc Moreau (W3C interview,
  2013), REPORTED: "Sometimes you have to reconstruct provenance information
  because it wasn't recorded at the right time. This can be very tedious." On
  verdicts, Paul Groth in the same interview: "we did not standardize a single
  weighting system for this." So Sid's "every read on every fact" goes further
  than PROV's authors chose to go.

**Trusty URIs: a hash as the id, done carefully.** Kuhn and Dumontier, ESWC
2014, https://arxiv.org/abs/1401.5775.

- How: "the RDF statements are sorted, then they are serialized in a given way
  (interpreting the artifact's hash as a blank space), and finally SHA-256 is
  applied". The record contains its own id, so the id is hashed as a blank and
  put in afterwards. Local ids (blank nodes) are converted to global ones
  first. The id carries a two-letter module code that names the kind and version
  of hashing. That is the algorithm tag Git never had.
- What it buys. REPORTED: "Once a trusty URI is established, its artifact code
  defines what object it refers to, and the issuing authority has no longer the
  power to change its meaning." Kuhn et al. (2016): "servers only have to deal
  with adding new entries but not with updating them, which eliminates the hard
  problems of concurrency control and data integrity in distributed systems."
  And: "servers do not have to deal with identifier management".
- What it does not buy. REPORTED: permanence holds only "if we assume that there
  are search engines and web archives crawling the artifacts on the web and
  caching them." A hash proves sameness. It does not keep anything.

**Updating and retracting when nothing can change.** REPORTED (Kuhn et al.,
2016): a nanopublication "cannot be deleted or "unpublished," but only marked
retracted or superseded by the publication of a new nanopublication". The
convention (2021): a new version says `supersedes` in its own publication info;
a retraction is a separate nanopublication whose assertion is `retracts`. "We
only consider them valid if the retraction or update is signed with the same key
pair, but more flexible solutions are possible in the future." The Python
library lets you retract something that is not yours with `force=True`. So
anyone can *write* a retraction. Whether it *counts* is decided by each reader.
"Is this retracted?" is a query over backlinks, not a flag.

**What they changed.**

- The supersede and retract conventions came after the store. The 2016 paper
  lists them as future work. Five years of records were made first.
- Key loss and key compromise are still open, by their own account.
- The server network was replaced by a second-generation Nanopub Registry, with
  per-key quotas and a trust root held in a "setting nanopublication" with
  chains of endorsement that decide whose records get loaded at all. INFERRED:
  that is a worked precedent for (17). The first fact is a fact. It names who
  may vouch for whom. Everything after hangs from it.

**What the semantic web learned about global predicate ids.**

- Names that look like English get misused. REPORTED (Halpin and Hayes, 2010):
  "the labeling of constructs with "English-like" mnemonics naturally will lead
  to the use of a knowledge representation language by actual users that varies
  from what its designers intended." That is an argument for opaque key ids from
  people who watched the alternative.
- Sameness is dangerous in one shared space. REPORTED (same): "anyone can link
  to your data-set with owl:sameAs from anywhere else on the Web without your
  permission, and any statement they make about their own URI will immediately
  apply to yours." INFERRED: Sid's layers tame this. A same-as fact in my layer
  merges two ids for me alone. In base it merges them for everyone, so it needs
  the strictest policy in the store.
- Ids should hold nothing that can change. REPORTED (Berners-Lee, 1998): "URIs
  don't change: people change them." "URIs change when there is some information
  in them which changes."
- A namespace, once used, is forever. INSTITUTIONAL (schema.org FAQ): "both
  'https://schema.org' and 'http://schema.org' are fine." The largest vocabulary
  on the web could not move its ids. It now has two spellings for every
  predicate, for good.
- Statements about statements took three tries: RDF reification, then named
  graphs (which nanopublications use), then RDF 1.2 triple terms. RDF 1.2:
  "It is expected that the reifiers (rather than the triple terms) will be used
  in further statements". INFERRED: a verdict, a doubt, or a retraction has to
  point at the *act of asserting*, not at the content asserted. Two people who
  say the same thing have made two facts. So a fact needs an id of its own, and
  a hash of entity, key, and value alone cannot be it.
- Local ids that do not travel were a mistake. INSTITUTIONAL (RDF
  canonicalisation, 2024): "blank node identifiers [...] are not intended to be
  persistent or portable". A whole standard was needed to make graphs hashable,
  and it still "does not define such a graph signature."

**Transfer.** PROV is a vocabulary, not a store; nobody had to pay for writing
it all down at machine rate. Nanopublications are the nearest small-scale twin
of Sid's base layer: append-only, attributed, provenance on every record. They
differ by having no gate: ids are made by authors, validity is judged by
readers. Disagreements: Halpin and Hayes against linked-data practice on
sameness; Hogan and others against RDF's local ids; Kuhn against blockchains
("identity is inseparably linked to private key access"). I could not open
"The Rationale of PROV" (2015), the paper most likely to say what PROV's authors
would change. Every mirror refused. Nothing here is drawn from it.

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

The spoken version (OOPSLA 1997, from a transcript of the talk) adds the part
that matters for an envelope: "let's make the first ten or so pointers standard,
like reading and writing fields, and trying to print; let's have a standard
vocabulary for the first ten of these, and then we can have idiosyncratic ones
later on."

So Kay's own model is a small fixed prefix of standard meanings, and then open.
That is not "no envelope". It is a short one. And from the same talk: "HTML on
the Internet has gone back to the dark ages because it presupposes that there
should be a browser that should understand its formats. [...] It should travel
with all the things that it needs".

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

**STEPS, and a kernel that describes itself.** Piumarta and Warth, "Open,
Extensible Object Models" (2006–08): "three object types and five methods are
sufficient to bootstrap an extensible object model and messaging semantics that
are described entirely in terms of those same objects and messages [...] and
frees the original language designer from ever having to say 'I'm sorry'." The
circle is cut in exactly one place, by hand: the lookup of "lookup" in the table
of tables is short-circuited. The bootstrap is a written four-step sequence,
about 140 lines of C. INFERRED: this is the shape of an answer to (17). A
grammar for grammar facts that describes itself. A policy that permits writing
policies. One hand-cut circle each, written by hand, the same in every store.

STEPS' end state, REPORTED (2012 report): "much of today's personal computing
systems [...] can be built with just 1000s to tens of 1000s of lines of code".
But: "Less Than Planned [...] (a) the comprehensive 'bottom engine room' for the
entire system". And they blame optimisation for pulling them off course: "This
'siren's song' [...] has created a project that is a bit different than the
original proposal". The part they did not finish is the bottom: the part Sid
calls the runtime.

**Worlds are Sid's layers, built and measured.** Warth, Ohshima, Kaehler, Kay,
"Worlds: Controlling the Scope of Side Effects" (ECOOP 2011). A child world sees
its parent's state unless it has its own. Then:

- A read pins. REPORTED: "Once a variable (or slot, memory location, etc.) has
  been read or modified in a world w, subsequent changes to that variable in w's
  parent world are not visible in w." Unread slots keep following the parent.
- Commit checks the whole read set. REPORTED: "A commit from wchild to wparent
  is only allowed to happen if, at commit-time, all of the variables [...] that
  were read in wchild have the same values in wparent as they did when they were
  first read". Otherwise "some of the assignments that were made in wchild may
  have been based on values that are now out of date."
- Limits they state: worlds "only capture the in-memory side effects"; the top
  world's commit "is currently a no-op", so nothing persists; and depth costs.
  Reading through a chain 1,000 worlds deep took 27 seconds until they added two
  runtime primitives, then 0.04.
- What this says to Sid, INFERRED. "Nearest layer wins" is Worlds lookup, and
  the cost of deep chains is real and was fixed in the runtime, not the model.
  Promotion is Worlds commit, with one difference. Worlds refuses a commit whose
  reads have moved. Sid's gate checks only the cell being written. Sid's answer
  to moved reads is to show staleness, not to refuse. That is a defensible
  choice. It should be a chosen one. It also means the gate's verdict should
  carry enough position to let anyone later decide whether a fact's reads were
  still current when it landed.

**Ingalls, "Design Principles Behind Smalltalk" (1981).** REPORTED:

- "Personal Mastery: If a system is to serve the creative spirit, it must be
  entirely comprehensible to a single individual."
- "Good Design: A system should be built with a minimum set of unchangeable
  parts; those parts should be as general as possible; and all parts of the
  system should be held in a uniform framework."
- "Uniform reference is achieved simply by associating a unique integer with
  every object in the system."
- "Reactive Principle: Every component accessible to the user should be able to
  present itself in a meaningful way for observation and manipulation."
- "Operating System: An operating system is a collection of things that don't
  fit into a language. There shouldn't be one."

INFERRED: the envelope is Sid's "unchangeable parts", so make it minimal. The
runtime is Sid's operating system: the things that did not fit into the medium.
Ingalls would shrink it until almost nothing is left. I could not open Ingalls' 2020
history of Smalltalk (the publisher refused every fetch), so I have nothing from
Ingalls on what the image, as the one substance, cost over forty years. That is a
gap.

**Transfer.** A Smalltalk image is one substance holding tools and content, for
one person, on one machine, with no record of who did what. It shows that a
medium can be read and rebuilt from inside. It says little about many writers,
attribution, or deletion. Internal tension worth keeping: Kay wants fences
around meta-level change; Piumarta and Warth expose the implementation with no
fence.

### 2.8 David Reed's pseudo-time, and Croquet

Kay's answer on time, in the AMA, was to read Reed's 1978 thesis. On Hacker News
three weeks earlier (item 11815660) Kay was blunter. REPORTED: "Pseudo-time is
the larger idea underneath protected distributed atomic transaction data-bases";
"I think it is a bit crazy to not use pseudo-time if you know about it, and it's
a bit amateurish not to be aware of it."

The thesis scan has no text layer and could not be read. Everything below is
from Reed's own journal version: "Implementing Atomic Actions on Decentralized
Data" (ACM TOCS, 1983).

**What Reed designed.**

- Versions. "We think of each object as a sequence of versions. Each WRITE to an
  object creates a new version... Once created, a version's value does not
  change." And: "the {object name, pseudotime) pair uniquely identifies the
  version." That is Sid's cell and version number.
- Whose clock. The writer's. "The implementation uses approximately synchronized
  real-time clocks at each node [...] a unique site identifier is concatenated
  as the low-order bits. Thus, even though two sites need not communicate, it is
  guaranteed that the sets of time stamps they generate are disjoint." Time *is*
  the order, and nobody central hands it out.
- Reads leave a mark. "If a WRITE with a pseudotime less than an already
  executed READ arrives at a site, it cannot be executed, for that would cause
  the value previously returned by the READ to be incorrect. [...] each version
  records the maximum pseudotime of the READs that have accessed that version."
- Tentative versions commit together: "We call the set of tokens created by an
  atomic action a possibility".
- Against a centre: mutual exclusion needs "advance knowledge of all potentially
  conflicting users", in effect "some central registry".

**What Reed conceded.**

- Never rewritten had two exceptions. "With two exceptions, versions are never
  modified once created." One was the read mark. Reed then designed both away so
  that versions could live on write-once media.
- Keep everything forever does not hold. "Since all versions of an object are
  stored forever, the total storage used by the system will increase at a rate
  proportional to the update traffic." Reed adds a window: "READs and WRITEs whose
  pseudotime is older than T - d are rejected."
- The centre comes back. "The node containing the commit record becomes a
  critical resource".

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

INFERRED: the Kay camp, after twenty years, runs on one small stamper per
session that fixes order and time, and on deterministic computation so that
derived state is never stored or sent. That is Sid's gate and Sid's running
answers, arrived at from the objects side.

**Two details that bear on (12).**

- Determinism needs the code pinned. INSTITUTIONAL: "A session id is created
  from the given session name, and a hash of all the registered Model classes
  and Constants. This ensures that only users running the exact same source code
  end up in the same session, which is a prerequisite for perfectly replicated
  computation." Croquet's answer to a code change is a new universe. Sid's store
  outlives its code, so it cannot do that. It has to write the version down.
- Determinism has to be policed. Their changelog adds a warning for `Date` in
  model code (2021) and a flag that "detects accidental writes into model state"
  (2024). A tool that claims to give a running answer will read a clock or a
  random number by accident. Something has to catch it, or "never stored, always
  re-derivable" is false for that tool.

Also: in Croquet "A view can read directly from a model at any time", and nothing
records it. Being shown is not a fact there.

**Reed, thirty-four years on.** "'Simultaneous' Considered Harmful" (2012)
argues against "one universal total ordering of all events", and says that in
"open, unbounded, evolving systems... there is no utility to speaking of
'simultaneous' at all." The gatherer found nothing by Reed on Croquet's
reflector, and warns against reading this as a repudiation. I read it only as
this: the person Kay sends everyone to for time would not sign "one order for
the planet". Reed would sign one order per unit that needs it.

### 2.9 Joe Armstrong

**What Armstrong built.** Erlang, which runs phone switches that are upgraded without
stopping. Armstrong cared about two programs that do not trust each other and change
at different times. Late in life Armstrong wrote about naming.

**Three kinds of id, for three jobs.** This is the most direct source I found
for (1), (2), and (3) together. REPORTED ("The web of names, hashes and UUIDs",
2015, https://joearms.github.io/):

- "As soon as we name something there is an implied context - take away the
  context, or use the name in a different context and we are lost."
- "SHA1 checksums are fine for content that is immutable (doesn't change) - but
  what about a file whose content changes with time? To solve this I propose
  adding UUIDs to files. UUIDs can be generated locally without using a
  centralized server."
- On deriving one thing from another: "we add a new UUID to the file - and we can
  also add a `parent:UUID` tag in the file saying that this file was derived
  from an earlier file with this UUID."
- "Why three webs? * The web of names is convenient and easy to use * The web of
  UUIDs allows us to track content that changes with time * The web of hashes
  (SHA1) allows total precision in managing content… I think we need all three."

Armstrong reached this in steps. 2011: every function in one global store under a
unique name. 2014 (a talk, known only from live captions): replace names with
hashes. 2015: all three. The same three-way split shows up, separately, in
Hickey (identity names and tear-off value names), Stiegler (key, nickname,
petname), Wikidata (item ids, random statement ids, hashes on values), and
Unison (names, minted ids for unique types, hashes). Five lines of work, one
shape: **a minted id for a thing that lasts through change, a hash for content
that never changes, and names as data about either.**

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

For (7), INFERRED: keep refusals, and make a refusal say what was expected and
what was received. Armstrong's reason is the best one available. Agents and people
believe what they expected to see. The refusal is the record that does not.

**Transfer.** Erlang's two parties are programs; its "two versions at once" rule
is about running code, not stored facts. I could not find a direct statement by
Armstrong on talking to unknown future versions, which the brief named. The nearest
things are the two-version rule, the contract as the meeting point of
separately built programs, and the parent tag. Armstrong disagrees with hierarchical
namespaces ("the *dot* in the name has no semantics"), with XML and WSDL, and
with putting the type system inside the language.

### 2.10 Self (David Ungar, Randall Smith)

**What they built.** A language with no classes. An object is made by copying
another. Shared behaviour lives in ordinary objects that others point at. This
is "no type slot; a type is just another entity that others point at", run for
decades.

**Reasons.** REPORTED ("Self: The Power of Simplicity"): "No object in a
class-based system can be self-sufficient; another object (its class) is needed
to express its structure and behavior. This leads to a conceptually infinite
meta-regress… Prototypes eliminate meta-regress." They made a bet in 1987: "A
working system will provide the chance to discover whether class-like objects
would be so useful that programmers will create them without encouragement from
the language."

**What came back.**

- Types, by convention. REPORTED ("Organizing Programs Without Classes", 1991):
  "We call these shared parent objects traits objects. [...] No special language
  features need to be added to support traits objects—a traits object is a
  regular object".
- Names, as ordinary data: "since these objects are implemented by regular
  objects, they have no internal names. [...] name space objects whose sole
  function is to provide names for well-known objects. The name of an object in
  a name space is simply the name of the slot that refers to the object."
- Classes, under the floor, for speed. REPORTED (the implementation paper): "From
  the implementation point of view, maps look much like classes, and achieve the
  same sorts of space savings for shared data. But maps are totally transparent
  at the SELF language level". And the bootstrap: "All map objects share the same
  map, called the "map map." The map map is its own map."

For (13), INFERRED: plain maps in the model; the runtime is free to discover
shapes and store them compactly, as long as that stays invisible. For (17): one
more self-describing root with a single hand-cut circle.

**Regrets.** REPORTED ("Programming as an Experience", 1995): "We have learned
the hard way that smaller is better and that examples can be deceptive. Early in
the evolution of Self we made three mistakes [...] Each was motivated by a
compelling example." "The resultant semantics took five pages to write down".
Their rule: "when features, rules, or elaborations are motivated by particular
examples, it is a good bet that their addition will be a mistake… this
phenomenon might be called 'the language designer's trap.'"

That rule is aimed at exactly the kind of decision Sid is making. Each part of a
nine-part envelope is there because of a compelling example. I could not open
their 2007 retrospective (paywalled), so its wording on maps and regret is not
here.

### 2.11 Carl Hewitt

**What Hewitt built.** The actor model (1973 on), and late in life an argument that
very large information systems are always inconsistent and must be built to live
with it.

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

For (10) and (11), INFERRED: the order in which offers reach a gate is not
something that can be worked out from facts. It is made by the gate and can only
be recorded. The gate's number is that record. A writer's clock is a claim. And
two gates make two orders, with no fact of the matter between them.

**On "one truth".** REPORTED (same paper, Hewitt's principles for information
integration): "In practice integrated information is invariably inconsistent".
"Persistence. Information is collected and indexed and no original information
is lost." "Pluralism: Information is heterogeneous, overlapping and often
inconsistent. There is no central arbiter of truth." "Provenance: The provenance
of information is carefully tracked and recorded." And: "Sponsorship: Sponsors
provide resources for computation, i.e., processing, storage, and
communications."

This list is close to Sid's own. It differs on one line. For Hewitt there is "no
central arbiter of truth". INFERRED: Hewitt would accept one log and one order. Hewitt
would not accept "one truth" if it means one consistent set of beliefs. Layers,
and Wikidata's claims-not-truth, already give Sid plurality of belief. So the
challenge is mostly to the slogan: "one truth" should mean one record of who
said what and in what order, and nothing more.

**Against facts as the only substance.** Hewitt argued against Kowalski that
"computation in general cannot be subsumed by deduction", because arrival orders
"cannot be deduced from prior information by mathematical logic alone". Sid's
design already grants this: the runtime and the gate are not facts, and what
they decide is written down as facts.

---

## Part three — substrates

### 3.1 Dynamicland's Realtalk, and Folk Computer

**What they built.** Rooms where programs are physical pages. A page makes
claims and wishes. Other pages hold "when" rules that match on those statements.
Nobody addresses anybody. This is the nearest living relative of "tools are
matched to facts". Dynamicland has published little on how Realtalk works;
their FAQ says so. Folk (Omar Rizwan, Andrés Cuervo, and contributors) is an
open reimplementation with monthly notes going back years, including what broke.

**Match, don't name, in their words.** REPORTED (Rizwan, "Notes from
Dynamicland: Geokit", 2018): "You can make whatever claims you want, but there's
no guarantee that anything will happen as a result. Right now, nobody cares
about my geomap claim, so the map is inert." On wishes: "I'm not addressing the
wish to anyone in particular."

**Who said it.** In Realtalk a page claims things about itself (`Claim (you) is
geomap of ...`). In Folk, attribution is a convention, not a slot. INSTITUTIONAL
(Folk README): "Notice that you should scope your claim: it's `$this has a
ball`, not `there is a ball`, so different programs with different values of
`$this` will not stomp over each other." And any program can overwrite
another's held state: "`Hold! -on 852 { ... }`". There is no protection. The
Dynamicland FAQ explains why that is fine for them: "Realtalk has no user
accounts — there's no concept whatsoever of a "user" within the system. There's
just stuff on the table. The stuff doesn't know whose stuff it is." The room is
the trust boundary.

**Based-on in miniature, and two kinds of read.** INSTITUTIONAL (Folk README):
"Any wishes/claims you make in the body will get automatically revoked if the
claim that the `When` was matching is revoked." That is a running answer with
its reads tracked, never stored. And they had to separate a read that creates a
dependence from one that does not. REPORTED (Rizwan, newsletter, Feb 2024):
`Query!` "is used on stuff like the web server to snapshot query the database as
it is right now, without introducing any new reactive dep".

**What they had to add, over the years.**

- Durable state. Statements vanish when their source does, so they added
  `Hold!`: "to create the equivalent of 'variables', stateful statements", with
  a key, so that a later hold with the same key replaces the earlier. That is a
  cell with supersession, arrived at from the other direction.
- Supersession without a gap. REPORTED (Feb 2024): "don't fully retract a
  statement until the new replacement statement is in place, so that any retained
  consequences of the old statement stay alive under the new one". In July 2024
  they floated "maybe we track provenance of statements?" as the cure for
  flicker.
- A consistent cut at the boundary. REPORTED (Sept 2025): "each invocation of
  the block gets an associated version object, and all downstream statements of
  the invocation are tagged with that version. [...] When the inflight counter on
  a version hits 0, that version is considered converged". Unconverged
  statements are filtered out of boundary queries "so we exclude them from 'side
  effects' at the boundary between Folk and the outside world". That is
  "because of" (every downstream statement tagged with what started it) and
  Sid's crossings (only a settled answer may leave), both added after years
  without them.
- Order, once they went parallel. REPORTED (Mar 2024): "stuff that we 'got for
  free' from having a single thread and converging to a fixed point. like, what
  order do you do operations in [...] what does 'order' mean if things can happen
  in parallel.."
- Events are still open. REPORTED (Feb 2024): the system "doesn't actually
  evaluate for every intermediate state. This is the behavior that you want for
  statements! [...] (If you _do_ want to process intermediate states, you want
  something with different semantics from normal statements, like event
  statements or something. This is an open question.)"

**The hand.** For this camp almost nothing the hand does is recorded.
INSTITUTIONAL (Dynamicland FAQ): "Most Realtalk objects respond to the current
physical situation, the here and now, and do not remember anything. There's
very little "data"." Pointing and gaze are left to the room: "physical materials
[...] which naturally engage social cues such as pointing, line of sight, and
shared attention." "Objects can't see people (Realtalk does not track people)".
For (14): their default is that a motion is a statement while it lasts and
nothing afterwards. Only what someone chose to hold persists. "Being shown" is
not recorded at all.

**On one store.** INSTITUTIONAL (FAQ): "we imagine a decentralized network of
local communities, not a database serving millions of customers." "Every site
has its own independent physical copy of Realtalk. [...] There is no centralized
official version of Realtalk."

**On rebuilding from inside.** Realtalk is written in Realtalk, on posters in
the room. Folk went the other way under pressure. REPORTED (Rizwan, 2024): the
runtime was rewritten "3 or 4 times… every 5 months or so… because Folk starts
feeling too slow", and the new kernel became a static binary because "no one was
messing with the kernel in practice anyway". The second rewrite was "such a
breaking change" that the old line stopped. Folk could afford that because it
keeps almost nothing. Sid's store keeps everything, so its facts and tool bodies
must not depend on any one runtime.

**Transfer.** Same idea of matching. Opposite stance on memory, identity, and
scale: one room, no users, nothing kept. No system in this family has run
match-don't-name among parties who do not trust each other. Sid's would be the
first.

### 3.2 Linda (David Gelernter)

**What Gelernter built.** Tuple space: processes put tuples in, and take them out by
pattern. REPORTED ("Generative Communication in Linda", 1985): "just as the
receiver has no prior knowledge about the sender, the sender has none about the
receiver." A tuple stays until taken: "If it is never removed by in( ) it will,
in the abstract, remain in TS forever." Matching is not ordered: "Some suspended
in( ) statement will receive it, but which is logically nondetermined."

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

Kay, in the AMA, pointed at Linda and said not to start from it: "what is the
similar idea scaled for 40 years later?" INFERRED: the forty-year gap is
protection and attribution. Linda has one space, no owner of a tuple, no record
of who put it there, and destructive reads. Sid's design already differs on each
point: facts are never removed, every fact has a writer, and layers split the
space. What Linda still warns about is reads. A pattern over one space sees
everything in it. I could not open Gelernter's "Multiple tuple spaces in Linda"
(1989), which is Gelernter's own answer and would be the closest precedent for layers.

### 3.3 Webstrates (Klokmose, Eagan, Baader, Mackay, Beaudouin-Lafon)

**What they built.** Web pages whose document tree is stored on a server and
synchronised to everyone who has the page open. Content, tools, and interface
are all nodes in the same tree. One substance, since 2015, with a published
account of what that cost.

**The one-substance problem, in one sentence.** REPORTED (UIST 2015): "a
webstrate shares the DOM, the whole DOM and nothing but the DOM. Problems
typically come from either wanting to share information that is not represented
in the DOM or not wanting to share information that is in the DOM." With no
layers, personal material was hidden with style rules: "we do include the content
in the DOM, and use local style rules injected in the document to hide unwanted
content."

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

**Why they left the central server.** REPORTED (MyWebstrates, UIST 2024): "the
centralised server effectively 'owns' and controls the user's data. This
violates what we refer to as personal and collective digital sovereignty". What
it cost them: "there is not a central server with an authoritative order of these
changes. Hence, versions are no longer guaranteed incremental [...] These
incremental version numbers are more usable but are only locally valid, while
version hashes are globally valid." And authority became the id: "the ID of a
webstrate represents a basic security capability [...] today, there is no
mechanism for revoking access to a document."

**History.** "The server keeps the entire log of operations on each webstrate,
which could be culled." It never was. Restore appends. But `?delete` destroys a
whole document and its history "completely and unrecoverably". Their deletion
story is one door, at document size.

### 3.4 Jonathan Edwards (Subtext, schema change)

**What Edwards built.** Twenty years of experimental programming systems where code
and data are one structure, edited directly, with ids instead of names. Edwards
writes candid post-mortems.

**Names are comments.** REPORTED (Subtext, 2005): "labels are purely comments,
not identifiers". "Every label could be foo, confusing the programmer no end,
but not the computer." In Edwards' current work (Baseline, a preprint, with Tomas
Petricek): "we assign permanent unique identifiers (IDs) to every record field
and list element… Because record fields have unique IDs their names are only for
human readability". Note that this goes one level below Sid's key. The *fields
inside a value* get ids too, like protobuf tags. Webstrates reached the same
place on its own: a hidden unique id on every element.

**Shape change must be recorded as what was done, not as before and after.**
REPORTED (Baseline): "State-based approaches observe only the before and after
of changes, while operation-based approaches record the execution of a set of
possible operations… For example comparing states of a table schema, a move
followed by a rename is indistinguishable from a delete followed by an insert,
but that makes a big difference to the data." And (2025 vision statement): "you
can't tell how to migrate the data just by comparing types before and after.
[...] It is necessary to capture the user's intention as they interactively edit
the type."

INFERRED: Sid's grammars are facts with versions. A new version that only holds
the new shape is a before-and-after record. Years later no tool can tell whether
a field was renamed or replaced, and so cannot read old values through the new
grammar. If a grammar version also says *what was done* to the previous one,
that stays possible. This is one more thing that cannot be added afterwards.

**On one writer.** Edwards sides with a centre. REPORTED (Baseline §7): "extending our
approach to do replication would require a centralized primary whose order of
operations decides conflict resolution for everyone consistently". Against the
road Webstrates took: "The power of CRDTs is their monotonic semantics but that
is also their weakness. They can't go backwards."

**Regret.** REPORTED ("Subtext Retrospective", 2025): "It is fair to say that
Subtext was a series of overambitious failed experiments. I was trying to invent
too many things at the same time… What Subtext needed was a Theory of Change." On
Edwards' own current design: "worryingly complex and still incomplete."

### 3.5 Xanadu (Ted Nelson; Roger Gregory, Mark Miller, Dean Tribble and others)

**What they promised.** One docuverse for the planet. Permanent addresses for
everything. Nothing deleted. Every version kept. Quotation that stays connected
to its source, with credit and payment. It is the closest thing to a precedent
for Sid's layer zero. It did not ship.

**Ids as paths that anyone can extend.** INSTITUTIONAL (Udanax Green manual):
"any node could give addresses to new nodes by appending another tumbler digit to
its own tumbler address. For example, node 23.4 could create nodes 23.4.1,
23.4.2, 23.4.3". Accounts, documents, and versions nest the same way: "the first
new version adds a tumbler digit. Each successive version increments the last
digit." Everything sorts in one order: "4 < 4.23 < 4.23.7 < 4.24 < 5", and a span
"can cover characters, links, documents, versions, or any other Udanax Green
entities, including the entire docuverse." Nelson (1999): "The central
proprietary secret this all relied on [...] was the freezing of content addresses
into permanent universal IDs".

For (2): no two ids clash, because each holder mints only beneath itself. No
central minter is needed. The price is that the id gives away where, under whom,
and in what order a thing was made. For (6): "which version replaced which" is
in the shape of the id, not in a stored pointer.

**Never rewritten, versus never lost.** Nelson's deletion is removal from a
version's list of contents. The content keeps its address, and the older version
still lists it. That is Sid's question (0) answered as "never rewritten". But the
rule had an exception from the start. INSTITUTIONAL (Xanadu FAQ, requirement
1e): "Documents must remain accessible indefinitely, safe from any kind of loss,
damage, modification, censorship or removal except by the owner. It must be
impossible to falsify ownership or track individual readers of any document."

Two things in that sentence cut against Sid's draft. The owner may withdraw. And
tracking readers must be *impossible*. "Every read recorded", "what was shown",
and "who read the erased thing" are the opposite design. INFERRED: the Xanadu
answer would be that a record of what I was shown is mine. It belongs in my
layer, readable by me, and by nobody else without my grant. "Who read the erased
thing" then needs either my consent or an operator who can see into every
personal layer. That is the god's-eye view Lemmer-Webber warns about.

**Things survive because someone pays.** INSTITUTIONAL (Udanax Gold class
docs): a work has "sponsors [...] All of the Clubs which are sponsoring this Work
to keep it from being discarded." An unsponsored work "might have been
discarded". Nothing is deleted by command. Sponsorship is on Hewitt's list of
principles too. For (13) and the "economy" in Sid's scale: storage that lasts for
years at planet scale has to be somebody's cost, and their model made that a
first-class relation.

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

**Why it did not ship.** Keep three voices apart.

- Gary Wolf ("The Curse of Xanadu", Wired, 1995) claims: prototype in
  Smalltalk, translate to C++, and "by the time McClary started work on the
  translation, the design had evolved into a new shape"; "They did not have a
  customer in mind"; and performance was never solved. Wolf quotes an engineer:
  code to fetch text "was something like 20 lines of very, very hairy C++ [...]
  it wasn't anything even remotely resembling fast."
- Nelson disputes the performance charge ("Utterly misleading") and defers to a
  letter. The letter could not be found, so Nelson's technical rebuttal is
  unsourced.
- The engineers: Roger Gregory, in Wolf: "Stiegler and Miller screwed up the
  entire thing. I had something that was within six months of shipping." The
  1999 release notes call the two-language build "almost too painful", and say
  the released code "is not considered to be usable as yet". Miller and
  co-authors admit the single docuverse never existed: "for the moment, each
  server is still an island with respect to the other servers". Their stated
  principle was many operators, not one: "Open entry of server providers [...] in
  order to make centralized control impossible."

**The Web's side.** REPORTED (Berners-Lee, "Web Architecture from 50,000 feet"):
"A fundamental compromise which allows the Web to scale (but created the
dangling link problem) was the architectural decision that links should be
fundamentally mono-directional." The 1989 proposal: "A new system must allow
existing systems to be linked together without requiring any central control or
coordination." Clay Shirky (1996): "in any heterogeneous system links have to be
one-directional, because bi-directional links would require massive coordination
in a way that would limit its scope."

INFERRED: the Web beat Xanadu by giving up the very things Sid wants to keep:
permanence, links that know both ends, one space. That is not a verdict on Sid's
design. Sid has one operator, so one gate can enforce what the Web could not ask
of strangers. The lesson is narrower. Whatever needs every participant's
cooperation to stay whole will not cross an operator boundary. Inside one store,
enforce. Across stores, expect only what a stranger can verify alone.

Edwards and Xanadu share a regret: too many inventions at once.

### 3.6 An addition: Urbit, on the runtime that is not a fact

Urbit was not on the brief's list. I add it because it is the one system I know
that fixed its bottom interpreter before its first event, for a personal
append-only log meant to last for decades. I checked these sources myself.

- INSTITUTIONAL (Urbit docs, Arvo overview): "The current state is a pure
  function of its event log: a chronological record of every action the operating
  system has ever performed." It is "stacked on top of a frozen instruction set
  known as Nock." And the first events deliver the interpreter: "The history
  starts with a bootstrap sequence that delivers Arvo itself, first as an
  inscrutable kernel written in Nock, then as the self-compiling Hoon source code
  for that kernel."
- REPORTED (Yarvin and Wolfe-Pauly, "Toward a Frozen Operating System", 2017):
  "In Kelvin versioning, a version is an integer in degrees Kelvin. Absolute zero
  is frozen — no further updates are possible." Nock is "defined in a page of
  axioms that gzips to 340 bytes." The rule for what sits on top: "B must state
  the version of A it was developed against. A, when loading B, must state its
  own current version, and the warmest version of itself with which it's
  backward-compatible."
- The honest part. REPORTED (whitepaper, 2016): "Everything but the lifecycle
  function can upgrade itself from source code in the input stream." And: "sometimes
  we still reboot the universe (declare a flag day, or "continuity breach") for a
  particularly gnarly one. The event log is not at all above suspicion."

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

## Part four — Sid's questions, hung on the parts of the fact

For each: what this camp says, with the section that holds the source; where the
camp splits; and my read. Everything under "My read" is INFERRED and mine. Where
I say "gone forever", I mean it cannot be added to earlier facts.

### (0) The log: never rewritten, or only never lost?

**Camp.** Nobody here kept "never lost". Datomic cuts, rarely, and the cut is a
permanent record (1.7). AT Protocol began with permanent signed history and tore
it out because deletion "breaks human intents and expectations" (2.4). Reed
conceded that keeping every version forever does not hold, and added a window
(2.8). Xanadu let the owner withdraw, and let unsponsored work lapse (3.5).
Wikidata suppresses, visibly (2.5). Webstrates never pruned, and offers one door:
destroy the whole document (3.3). Urbit declared "continuity breaches" (3.6).
Nanopublications alone never remove anything, and they hold public science only
(2.6).

**Split.** Those who planned the cut (Datomic, Wikidata) kept their history.
Those who did not (AT Protocol) lost it.

**My read.** The pair in the question is the wrong pair. The two promises that
can both be kept are: *the meaning of an admitted fact never changes*, and *a
value can be cut out, by rule, leaving a visible scar that is itself a fact*.
Decide the shape of the scar before the first record: what stays (the envelope
and the fact's id), what goes (the value), who may cut, and that the cut cannot
itself be cut.

### (2) Entity ids: how made, and may an id give away when or where?

**Camp.** Mint at random, locally, with no coordination: Hickey ("tear off names
that anyone can create without coordination", 1.7), Armstrong ("generated locally
without using a centralized server", 2.9), Varda (random 64-bit type ids; random
object ids skip a "round-the-world check", 2.2). Varda's warning is that real
clashes come from copying, not chance. Ids that carry information: Xanadu's paths
give away who, where, and order (3.5). Wikidata's counters give away order (2.5).
AT Protocol's time-based ids are "user-controlled data" that cannot be trusted as
time, and the identity log's timestamps can "de-anonymize account holders" (2.4).
Berners-Lee: an id changes "when there is some information in them which changes"
(2.6). One deliberate exception: Cloudflare mints jurisdiction into the id, for
law (2.2).

**My read.** An agent must be able to name a new entity in several offers before
the gate has seen any of them. So the maker mints, at random, 128 bits or more.
The gate's compare-and-set (expecting no current version) catches a copied id
being "created" twice. Keep ids opaque. "When" and "by whom" belong in facts,
where policy can hide them. An id gets copied into every based-on list and
crossing, where policy cannot reach. The open point is jurisdiction. If law will
ever require some entities to live in a region, the store needs to know that
before it can read their facts. That is the one piece of "where" that may have to
be in the id or the layer. It is gone forever if left out (2.2 has a live example).

### (17) The first facts

**Camp.** A small kernel that describes itself, with the circle cut once, by hand.
Piumarta and Warth: three object types, five methods, "described entirely in terms
of those same objects and messages", bootstrapped in four written steps (2.7).
Self: "The map map is its own map" (2.10). Xanadu: clubs that are "self-reading or
self-editing" end the regress of who may read the list of who may read (3.5). The
nanopublication network's trust root is itself a "setting nanopublication" (2.6).
Urbit's first events deliver the interpreter itself (3.6). Kay's tape: "the first
ten or so pointers standard" (2.7).

**My read.** The first facts are: the grammar of grammar facts, which describes
itself; the policy for policy facts, which permits itself; the keys needed to say
those two things; the base layer; the gate as an actor; and an anchor to the
written definition of the body language and of the byte encoding. They are
written by hand, by a founder actor, in one genesis offer that the gate admits
under a rule that exists only for genesis. That rule is the one hand-cut circle.
They should be the same in every store. That rules out ids handed out by a gate's
counter for them. Their ids are either published constants or computed from their
content (2.6, trusty URIs). Unison and Git show what a translation table costs
when ids are local (2.3).

### (3) Key: a word, or an id with its name and shape as facts?

**Camp.** Close to unanimous: an id. Varda: names get changed, collide, and waste
space (2.2). Protobuf: numbers on the wire, never reused (2.2). Wikidata:
language-neutral ids, labels as data (2.5). Datomic: the attribute is an entity;
after a rename "Both the new ident and the old ident will refer to the entity"
(1.7). Edwards: "labels are purely comments" (3.4). Halpin and Hayes: English-like
names get misused (2.6). Stiegler: key, nickname, petname (2.1). Armstrong: names
carry context; "I think we need all three" (2.9). Self: names came back as
ordinary data (2.10).

**Split.** Hickey wants identity names to be globally qualified words, and wants
data readable by "generic processors" with nothing fetched out of band (1.7).
Unison paid for opaque ids: "It's easy to accidentally replace your human readable
names with mysterious hashes" (2.3). Wikidata admits names and ids can drift apart
with "nothing implemented in the system to prevent it" (2.5).

**My read.** Store the id. Keep the name as a fact about the key: a public
nickname in base, and each person's own word in their layer, which "nearest layer
wins" resolves for free. Two rules come with it, each from two unrelated systems.
A key's kind of value never changes; a breaking change of shape is a new key
(Wikidata, Datomic). A grammar grows only by adding (Hickey). Two extensions from
this camp: give the *fields inside a value* ids too, never reused (protobuf,
Edwards). And let a new grammar version say what was done to the old one, not only
what it now is (Edwards). Both are gone forever if left out.

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

**My read.** Nobody in this camp has a clean answer for backups. Datomic's own
advice, to back up before cutting, leaves the cut content in the backup. What the
camp does teach is what must be true of the record so that a cut is *possible*:
the value can be removed while the envelope stays, so based-on walks still work;
no other fact's validity depends on a strong hash of the cut content (AT Protocol
removed its back-pointer for this reason); and if a fact's id is a hash that covers
its value, the hashed content includes a random salt that is cut along with it, or
the surviving id lets anyone confirm a guess. Sid's store has one advantage none
of them had. Crossings are recorded, so it knows which screens, models, and hosts
were handed the content.

### (8) By whom

**Camp.** Identity is for knowing who said a thing. It must not be what decides
whether a thing may be done (Lemmer-Webber, 2.1). Attribution in a capability
system is a chain. Each link is vouched for by the one before: "Bob claims that
Carol performed this action" (Varda, 2.1). Delegation hands over authority and
responsibility together (Horton, 2.1). PROV makes "acts for" its own identified
relation, scoped to an activity if wanted (2.6). Xanadu: "no one may endorse with
the identity of another"; a club's signature right is the recorded "acts for"
(3.5). Wikidata shows the cost of getting it wrong: mass edits by tools under
people's accounts made the bot policy "ineffective" (2.5). Folk shows that
attribution by convention is not attribution (3.1). Hickey saw the question and
left it: "(should I trust that?)" (1.2).

**Split.** Sign every offer, or not. Nanopublications and AT Protocol sign. Signed
records authenticate themselves anywhere, so a second store and a credible exit
become possible. But key loss and key compromise are, by Kuhn's account, still
open (2.6). Horton refuses signatures on purpose: "To avoid non-repudiation"
(2.1).

**My read.** "By whom" is a chain. The gate vouches only for the head: the party
whose authenticated channel the offer arrived on. An agent is itself, never the
person. "Acts for" is a grant fact from the person, which the agent's offers cite.
Person and instrument are then both on every fact, which is exactly what Wikidata
lost. The signing decision has to be made before the first record. If offers are
not signed when made, the authorship of every early fact rests on the gate's word
for ever, and no later change can fix that.

### (11) When: whose clock, and is it ever used for order?

**Camp.** Order comes from an arbiter, not a clock. Croquet: models "have no
concept of real-world time"; the reflector stamps everything (2.8). Durable
Objects: order is arrival at the one thread (2.2). Hewitt: arrival order cannot be
deduced, only awaited (2.11). did:plc: a "server-generated timestamp" (2.4). A
writer's clock is a claim: AT Protocol says not to trust it (2.4). Reed is the
exception. Reed's pseudo-time comes from the writer's clock plus a site id, and it
*is* the order (2.8).

**My read.** Keep two things apart. Order is the gate's number, never a clock.
"When" is the gate's clock at admission. It is information, not order. But there
is a third time the envelope lacks: when the thing being recorded happened. A
paper from 1998 ingested in 2026. A person's note made offline. Wikidata keeps
this as start and end qualifiers, separate from rank (2.5). Hickey lists it first
among Hickey's open questions: "should data always incorporate time" (1.2). The
writer's time is a claim, and it should have a place, or it is gone forever for
every imported fact.

### (16) Layer: who sees a fact before any permissions exist?

**Camp.** Only its maker. In the capability model a new thing is reachable by
nobody else until a reference is handed over (2.1). Worlds: a child world's
changes are private until commit (2.7). The counter-examples all live inside one
trust boundary: Linda's space is global by design (3.2); Folk lets any program
overwrite another's state (3.1); Webstrates had no layers and hid personal
material with style rules (3.3). Bluesky shows where public-by-default goes:
"most of the architectural assumptions assume public messages only", with public
block lists as "emergent behavior from initial decisions" (2.1).

**My read.** A layer is born private to its maker. Base is public. The first
policy facts land before the first ordinary fact (17). Pattern reads are scoped to
the layers the reader holds. One more rule, from the Webstrates restore hole: a
read "as of" the past is allowed or refused under today's policy (3.3).

### (10) Order: two gates on one layer? what shares a partition? what is "as of"?

**Camp.** One orderer per unit, and keep the unit small. Varda: objects "scale
out, not up"; a vote counter with a million voters must be split (2.2). Croquet:
one stamper per session (2.8). Edwards: a "centralized primary whose order of
operations decides conflict resolution for everyone" (3.4). What it costs to give
the orderer up, from MyWebstrates: version numbers become "only locally valid"
(3.3). From Folk: "what does 'order' mean if things can happen in parallel"
(3.1). Against one order for everything: Reed in 2012, and Hewitt (2.8, 2.11).
Nanopublications avoid the question by having no cell to update (2.6).

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

### (11) Based on: is every read listed?

**Camp.** PROV does not require any read to be recorded; "Applications are free
to decide which level of granularity" (2.6). Wikidata's machine-filled source slot
became noise and was ruled not-a-source (2.5). Folk tracks reactive reads
automatically and lets a program opt out (3.1). Croquet and Realtalk do not record
views at all (2.8, 3.1). Xanadu: tracking readers must be "impossible" (3.5). On
the other side, Moreau: reconstructing provenance later "can be very tedious"
(2.6). Reed and Worlds both record read sets, but only to validate a commit (2.7,
2.8).

**My read.** List what was *used*, mechanically and completely, for every tool run
and every crossing. The runtime knows this, and it is cheap, because a pattern
read is one entry, not one per match. Mark each entry with who filled it: the
actor, or the runtime. The read that made a tool fire is "because of", not "based
on"; PROV keeps the trigger apart from usage in the same way ("started by an
entity, known as trigger", PROV-DM §5.1.6). A person's usage records belong in
that person's layer. That is the only way to square "what was I looking at
yesterday" with Xanadu's rule.

### (4) Based on: dependence or path? follow the latest, or stay?

**Camp.** A read is not a dependence. PROV's unused paint (2.6). Everyone marks
the mode at the point of use. Erlang, per call (2.9). Folk, by the verb: `When`
follows, `Query!` samples (3.1). Worlds: a read pins (2.7). Unison: always pinned,
and following is a separate act; "correct definitions should never require
upgrading" (2.3). Armstrong: late-bound while developing, pinned when deployed
(2.9).

**My read.** The second half of the question dissolves. A fact is about the past:
it stood on version N, and that never changes. Only a running answer follows. So
facts pin, always, and "stale" is computed as pinned version not equal to current.
The first half is a real decision. Each entry says whether the actor *depended* on
it or merely *had it*. For a deterministic tool the two are the same. For a person
or a model only the actor can say, and may be wrong. Record both and never merge
them.

### (5) Index lag

**Camp.** The index is what breaks at scale, and it lags. Wikidata's query service
was the bottleneck, took "~3 months to reload", and was finally split (2.5).
Webstrates enforced stale permissions for up to two minutes with no record (3.3).
Folk lets only converged state cross the boundary (3.1). A Croquet replica always
knows exactly how far it has got (2.8).

**My read.** Yes. A pattern read is "everything matching P as of X". X must be how
far *the index* had got, not how far the log had got. Otherwise the fact claims to
stand on things it never saw, and the map lies. It is cheap to record and cannot
be reconstructed.

### (11) Because of: always filled?

**Camp.** Folk ran for years without it and added it in 2025, tagging "all
downstream statements of the invocation" so it could tell when a chain of work had
settled (3.1). PROV has the trigger (above). Against baking it in: Varda's
"required is forever" (2.2).

**My read.** Keep it. But make it required by gate policy, not by the encoding.
And do not let emptiness carry meaning. Protobuf removed the ability to tell
"absent" from "default", and had to put it back "in response to user feedback"
(2.2). "Starts a chain" should be said outright. Empty should be free to mean
"unknown", which imports and genesis facts will need.

### (1) Version: the gate's number, or the fact's own id? random, or from content?

**Camp.** Both, for two jobs. Unison keeps a local number and a portable hash, and
needs a translation table to sync (2.3). MyWebstrates: numbers "only locally
valid", hashes "globally valid" (3.3). Reed: object name plus pseudo-time (2.8).
Wikidata: a random id for the statement, hashes for its values (2.5). Trusty URIs:
with a hash, "the issuing authority has no longer the power to change its meaning",
and servers "do not have to deal with identifier management" (2.6). RDF 1.2: what
you say about a fact must point at the act of asserting it (2.6). Armstrong: three
webs (2.9). Warnings: Unison's flip to minted ids, because two things with the same
content are often not the same thing; and its churn when a random id made a retry
look new (2.3). Git and Unison both lacked an algorithm tag (2.3). IPLD: a hash
names an encoding, so the encoding must be fixed first (2.4).

**My read.** The fact's own id is made by the offerer, before the gate. It is a
hash of the canonical offer, which includes the claimed author, the based-on list,
and a random nonce. The nonce keeps two people's identical claims apart (Unison's
lesson) and doubles as the salt (9). The hash makes a retry idempotent, which
matters at machine rate. It travels to another store. And it is the only thing a
*refusal* can point at, because a refused offer never gets a gate number. If
refusals are kept (7), the fact's own id is forced. The gate's number stays as
what it is: a position, local and cheap, used by "superseded" and "as of". Tag the
algorithm in the id. Let the gate refuse any offer that is not in canonical bytes;
IPFS had no gate to do that, Sid does.

### (6) When 37 replaces 25, is "replacing 25" kept on 37?

**Camp.** On the new one: PROV revision, nanopublication `supersedes`, Armstrong's
parent tag, Git's parent (2.6, 2.9, 2.3). In the shape of the id: Xanadu (3.5).
Kept apart and later abandoned: Unison's patches (2.3). Removed because it blocked
deletion: AT Protocol's strong back-pointer, replaced with a clock value that "is
intentionally not a strong reference" (2.4). With a reason attached: Wikidata
(2.5). Without a gap for dependents: Folk (3.1).

**My read.** Sid's offer already states the version it expects to be current.
That *is* "replacing 25". Keep it on the admitted fact instead of dropping it
after the check. Keep it as a number, not a hash, so 25 can still be cut. Add why:
a newer value, a correction, or a withdrawal. Wikidata had to add that later.

### (7) The gate's yes or no

**Camp.** Armstrong: a refusal says what was expected and what arrived, both sides
are told, and the checker is usually right where the human is wrong (2.9). did:plc
keeps even nullified operations (2.4). PROV bundles make provenance of provenance
ordinary (2.6). Webstrates shows why a verdict must name the policy version it used
(3.3). Wikidata checks after the fact and only reports (2.5).

**My read.** Keep both, as facts that point at the offer's own id. A verdict names
the grammar version, the grant or policy it relied on, and the positions it checked
against. One caution the camp would raise. Refusals at machine rate are a way to
write into the store without permission. Lemmer-Webber's inbox spam and the
nanopublication registry's quotas are the same problem (2.1, 2.6). Keep the verdict
and the offer's id. Keep the body of a refused offer, if at all, in the offerer's
own layer and quota.

### (12) The runtime's version, and rebuilds

**Camp.** Croquet puts a hash of the code into the identity of the session,
because "only users running the exact same source code" compute the same thing, and
it had to add runtime checks for code that reads a clock (2.8). Urbit: each layer
"must state the version" of the layer below that it was built against (3.6). Unison
lost the meaning of old hashes when its inference algorithm changed (2.3).
Cloudflare: alarms made before a certain day lack a field, for ever (2.2). Both
Kay and Hickey land here (1.7).

**My read.** Yes. "The runtime" as an actor has to mean *which* runtime. One fact
per build, one per session start, and each tool-made offer or crossing can reach
them. Go one step further if possible: tool bodies state the version of the body
language they were written against. The runtime gets rebuilt a hundred times. What
it implements can still be small, written down, and versioned.

### (14) The hand

**Camp.** Durable by default was tried and walked back. Webstrates: "30-50
operations" per second of cursor movement, then a transient element, a protected
mode, and throttling that breaks consistency (3.3). Realtalk remembers almost
nothing, and "Objects can't see people" (3.1). Folk drops intermediate states on
purpose, and treats events as an open question (3.1). Croquet records only what
passes through the reflector; views read freely and unrecorded (2.8). Xanadu
forbids tracking readers (3.5). The Powerbox makes one kind of motion matter: a
choice that grants (2.2).

**My read.** A motion becomes a fact when it changes state others rely on, when it
grants authority, or when a later fact needs to stand on it. Pan, hover, and
pointing are statements while they last, at most session facts that may be trimmed.
"Shown" and "looked" are different claims. The runtime can attest that it showed
something. Nothing can attest that a person looked. So record "shown", and never
derive "read" from it. "Who read the erased thing" is really "to whom was it
shown".

### (15) A click

**Camp.** Wrong question, they would say (2.1, 2.2). Nothing acts in a person's
name. A click designates, and the designation is the grant: "Which calendar should
the app use?" Authority is handed over "just-in-time", for a purpose, and can be
taken back.

**My read.** The click fact names the person, what was chosen, and the tool. The
tool's offers cite it. Tools that fire with no click run under standing grants,
which are separate, narrow, revocable facts. This is the repair for the deputy
problem in 2.1. It needs "under what authority" to be on the record from the first
fact.

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

**My read.** Plain, open, self-describing maps, readable without fetching a
grammar. Every copier carries what it does not understand. Decide now what *may*
be trimmed (motion facts in session layers, bodies of refused offers), because a
trimming rule changes what "as of" can promise: Reed's reads older than the window
"are rejected".

---

## Part five — for the group

### 7. The voices that matter most, who I dropped, who is missing

**The three that matter most.**

1. **Mark Miller's camp** (Hardy's deputy, Horton, Lemmer-Webber's field
   experience, Varda's Sandstorm). They hold the one objection that gets *worse*
   because of Sid's central move. Matching tools to facts with nobody choosing
   them, plus policy keyed on the actor, is ambient authority by their definition.
   It cannot be patched later, because "under what authority was this written"
   is not on the early facts. And they have the constructive half too: the
   chain for "by whom", the grant for "acts for", the click that designates.
2. **Kenton Varda.** Nobody here has lived longer with these exact decisions at
   this scale, or written more plainly about regret. Numbers not names. Never
   reuse. Required is forever. Unknown fields must survive. One writer per small
   unit. An id minted with jurisdiction in it. A click as a grant.
3. **Wikidata** (Vrandečić, Krötzsch, Pintscher). The nearest existing thing to
   Sid's base layer, fourteen years in. Opaque keys worked. Claims-not-truth worked.
   Rank-not-delete worked, once a reason was added. What hurt is the list Sid
   should fear: a seeded corpus that was half the index and two per cent of the
   use; history tables at their limit; tools writing under people's names; a
   machine-filled provenance slot that came to mean nothing.

Close behind: Unison (the only team that ran content ids and minted ids side by
side and flipped), PROV (used is not derived), Armstrong (three kinds of id; the
checker in the middle), Newbold and AT Protocol (permanent history walked back for
deletion), Croquet (one stamper plus determinism, from the objects camp), Folk
(what a match-based system had to add, year by year).

**Dropped or only touched.** OMeta and grammars as first-class objects (no regret
literature found). Ohshima and Piumarta beyond the papers cited. Hewitt's logic
work. Karp's ZBAC (could not open). Beaudouin-Lafon's substrates papers. Benet
beyond the whitepaper.

**Could not open, and it matters.** Reed's 1978 thesis (scan with no text).
"The Rationale of PROV". Kahn and Miller's letter on Linda. Gelernter's "Multiple
tuple spaces". Ingalls' 2020 history of Smalltalk. Ungar and Smith's 2007
retrospective. Nelson's Literary Machines and Nelson's letter answering Wired. Bret
Victor's Realtalk notes (images without text). Kay's 2017 talk on communicating
with aliens, where the ambassador idea and a question about malware both appear,
exists only as a garbled machine transcript.

**Missing from the brief's list** (some may belong to other groups). Pat Helland
on data outside versus inside a service, and on immutability. Bill Kent, "Data
and Reality", on what an entity is. Certificate Transparency and similar verifiable
logs: a single append-only log that *outsiders can check* was never rewritten,
which is the missing piece between "one store" and "credible". Event-sourcing
practitioners on reading old events through new code. Records-retention law and
write-once storage in finance, where "never rewrite" meets "must delete". The
Croquet team's reasons for the reflector, which I could not find. Urbit, which I
added.

### 8. What this camp would question above the table, ranked by force

**1. Policy keyed on who, in a store where tools are matched and not called.**
Strongest. See 2.1. Hardy's rule set grew to "fourteen boolean operators" and
still leaked. Linda's authors conceded security was never confronted. Folk leaves
the hole open on purpose because the room is the trust boundary. No system of this
kind has run among strangers. The camp's demand is small and early: an offer
selects the authority it uses; grants are facts; the record shows which grant.

**2. A fixed nine-part envelope, shared by both ends, for ever.** Strong. Kay and
Hickey both shrink it (1.7). Varda: "required is forever"; no header is "one of the
best properties"; storage and wire shapes diverge (2.2). AT Protocol carries a dead
required slot for good (2.4). Kay's own regret is a design frozen too soon (2.7).
Self's rule about features born from compelling examples (2.10). The camp is not
against an envelope. Kay's tape has "the first ten or so pointers standard".
TCP/IP has a small header and a seam. Their position is: a tiny core that the gate
needs in order to admit and order a fact; everything else open, growing only by
addition; what is *required* set by gate policy, which is a fact and can change.

**3. One store for the planet.** Strong, and of a different kind: not a flaw in
the logic, a bet about power. Lemmer-Webber: "god's-eye", no credible exit (2.1).
MyWebstrates left its server over "digital sovereignty" (3.3). Dynamicland: "not a
database serving millions of customers" (3.1). Xanadu's own engineers wanted "open
entry of server providers [...] to make centralized control impossible", and never
got past islands (3.5). Reed and Hewitt reject one order for everything (2.8,
2.11). In fairness, the camp also supplies the case *for*: a shared heap only works
with one heap; Croquet, Edwards, and Varda all choose one orderer per unit;
Wikidata is one store and works. What the camp would insist on is that "one store"
must not leak into the record. No id that only this gate can mint. No authorship
that only this gate can vouch for. No first facts that differ from store to store.
Then the second store, and leaving, stay possible.

**4. Every read recorded as provenance.** Medium to strong. Xanadu made tracking
readers "impossible" on principle (3.5). PROV declined to require it (2.6).
Wikidata's machine-filled slot became noise (2.5). Webstrates drowned in cursor
operations (3.3). For it: Moreau's "very tedious"; Folk's and Worlds' read
tracking. The challenge is about who owns the record of a read, and about signal,
not about whether it can be done.

**5. Small plain facts as the one substance.** Medium. Kay: "I wanted to get rid
of data"; "the last thing you wanted any programmer to do is mess with internal
state" (2.7). The point under the rhetoric: a gate that checks one fact's shape
cannot protect a rule that spans many facts. Objects protect such rules by hiding
state. Facts cannot hide. Webstrates names the two failure modes of any single
substance (3.3). Wikidata put functions in another wiki and took six years to add a
new kind of entity (2.5). Hewitt: arrival order is not deducible from facts
(2.11). Against the challenge: Kay's side has not built the alternative at scale,
by its own account (2.7).

**6. Tools, grammars, policies, and definitions as facts in the same store.**
Medium to weak. This camp mostly loves it: Smalltalk, STEPS, Realtalk in Realtalk,
Wikidata's constraints, the nanopublication trust root. Their cautions: meta-level
change needs fences, and Sid's layers are a good fence (2.7). Policy in the medium
opens a time-travel hole (3.3). Definitions as facts can swell (2.5). In practice
few people edit the kernel (3.1), and STEPS never finished the bottom (2.7).
O'Keefe's question to Armstrong stands: how much of a shared sea of functions can
you change before you need the backups (2.9)?

**7. Running answers never stored.** Weak as a challenge; the camp agrees (Croquet,
Folk, Realtalk, Hickey). Conditions they would attach: determinism has to be
enforced, not assumed (2.8); the interpreter's version has to be among the reads
(1.7, 2.3); and an index *is* a stored running answer, which is fine if it is
disposable and its lag is written down (5).

**8. One writer.** Weak as a principle, strong as a caveat. One per unit, and the
unit small (2.2, 2.8).

### 9. Questions this camp would call the wrong question

- **(15) "Which tools may act in a person's name?"** None. Ask what the person
  handed to this tool, and for what.
- **(8) "Is a person's agent itself, or the person?"** A false choice. It is
  itself, under a grant from the person. Record both.
- **(14) "Is being shown the same as looking?"** The system can only ever know
  "shown".
- **(3) "A word, or an id?"** Three kinds of name, all needed. The real question
  is which one is written into the fact, and where the others live.
- **(0) "Never rewritten, or only never lost?"** Neither of those pairs with the
  other. Meaning never changes; content can be cut, visibly.
- **"One truth."** One record of who said what and in what order. Beliefs stay
  plural (Hewitt, Wikidata, Xanadu's "There are no official truths").
- **"Where does meaning live?"** Kay and Hickey both answer: at the reader. The
  live question is what the writer owes a reader who was not there.
- **The table itself**, Kay might say. "The key in making great and growable
  systems is much more to design how its modules communicate rather than what
  their internal properties and behaviors should be." On that view the thing to
  fix before the first record is the conversation between an offerer and the
  gate: what an offer must say, what a verdict says back. The stored shape should
  then be as open as that conversation allows. Self's warning belongs here too.
  Nineteen questions, each with a compelling example, is how a design gets five
  pages of rules.

### What I would carry into round two

These are the things that are *gone forever* if not written from the first
record, according to this camp's scars. Each is argued above.

1. Under what authority a fact was written (the grant it cited), not only by whom.
2. "By whom" as a chain with the instrument in it, and whether offers are signed.
3. The fact's own id, made before the gate, from canonical bytes, with a nonce and
   an algorithm tag.
4. "As of" as a position per partition, and the index's position, not the log's.
5. Which grammar version, policy version, and runtime build a verdict relied on.
6. For each read: used, or depended on; filled by the actor, or by the runtime.
7. The time the thing happened, apart from the time it was admitted.
8. "Replacing 25", as a number, with why.
9. What a grammar change *did*, not only its result; and ids for fields inside
   values.
10. "Starts a chain" said outright, so empty can mean unknown.
11. How a merge of two entity ids is said, and how every reader resolves it.
12. A value that can be cut while its envelope stays.
13. First facts whose ids are the same in every store.

And one thing to do rather than write: shrink the part of the envelope that is
fixed for ever to what the gate needs, and let the rest grow by addition.

---

## Sources

Opened and text-searched (by me, or by a gatherer under the verbatim rule; the
quotes I leaned on hardest were re-checked by me against the fetched text: 28
checks, all passed).

**Part one.** Hacker News API, story 11939851 and the subtree of comment
11945722 (all 71 comments), plus Kay's other AMA comments. Hickey talk
transcripts from github.com/matthiasn/talk-transcripts: "The Language of the
System" (2012), "Deconstructing the Database" (2012), "The Value of Values"
(2012), "Spec-ulation" (2016). Datomic docs: schema change; excision.

**Capabilities.** Hardy 1988 (cap-lore.com). Miller, Yee, Shapiro 2003 and Miller
2006 (papers.agoric.com mirrors). Miller, Donnelley, Karp 2007 (usenix.org).
Lemmer-Webber: OcapPub (gitlab.com/spritely/ocappub), "The Heart of Spritely",
OCapN drafts, the two Bluesky essays (dustycloud.org). Stiegler on petnames.

**Varda.** capnproto.org (FAQ, language, 2014 news post). protobuf.dev guides and
best practices; protobuf issue 272; release notes 3.5.0. Cloudflare blog posts of
2020, 2021, 2024, and docs. Sandstorm Powerbox docs and the 2017 post. Hacker News
comments by kentonv (16098698, 18196288, 18190005, 24617903, 32818948, 16093457,
16093175).

**Unison and Git.** unison-lang.org docs and blog; pchiusano.github.io;
unisonweb/unison issues 466, 523, 2196, 2251, 2276, 2373, 2471, 4539; the v2
codebase format doc. Git's hash-function-transition document.

**IPFS, IPLD, AT Protocol.** Benet 2014 (arXiv:1407.3561) and Benet's 2014 multihash
proposal. multiformats READMEs. IPLD DAG-CBOR spec. IPFS docs on privacy and
persistence; the 2022 forum thread. atproto.com specs; did:plc spec; atproto
discussion 1410; sync v1.1 proposal and announcement.

**Wikidata.** Vrandečić and Krötzsch 2014 (authors' PDF). "Wikidata: The Making
Of" (2023). Wikibase data model and JSON docs. Help pages: Ranking, Deprecation,
Data type, Sources, Redirects, Property constraints. Project chat, July 2017. WMF
query-service pages 2023–2025. Pintscher's 2026 request for comment. Brasileiro et
al. 2016. Patel-Schneider and Doğan 2024.

**RDF, PROV, nanopublications.** PROV-DM, PROV-O, primer, overview (w3.org). W3C
interview with Groth and Moreau (2013). Kuhn and Dumontier 2014. Kuhn et al. 2016
and 2021. Nanopublication guidelines; nanopub-py docs; Nanopub Registry design
notes. Berners-Lee 1998. Fielding 2005. Halpin and Hayes 2010. schema.org FAQ. RDF
1.2 Concepts. Hogan et al. RDFC-1.0.

**Kay, STEPS, Ingalls.** "The Early History of Smalltalk" (worrydream.com copy).
Kay's 2003 emails to Stefan Ram. squeak-dev, 10 Oct 1998. OOPSLA 1997 transcript
(tinlizzie.org). VPRI: NSF proposal 2006; STEPS reports 2007 and 2012 (the 2012
pages quoted were read from page images); Piumarta and Warth 2006; Warth et al.
2011. Ingalls 1981.

**Reed, Croquet.** Reed 1983 (TOCS). Smith, Kay, Raab, Reed 2003. Croquet docs
(archived) and npm README. Freudenberg 2021. Reed 2012. Kay's Hacker News comment
11815660.

**Self, Hewitt, Armstrong.** Ungar and Smith 1987/1991; Ungar et al. 1991;
Chambers et al. 1989/1991; Smith and Ungar 1995. Hewitt, arXiv:1008.1459.
erlang-questions thread of May 2011 (108 messages). Armstrong's UBF paper 2002,
thesis 2003, and 2015 note on names, hashes, and UUIDs. "The Mess We're In" exists
only as a live-caption transcript; I paraphrase it and do not lean on it.

**Substrates.** Dynamicland FAQ and pages (2024); Rizwan 2018; Folk README, docs,
wiki, and newsletters Oct 2023 – Aug 2026. Gelernter 1985; Carriero and Gelernter
1989 and 1992. Klokmose et al. 2015; Webstrates docs; Codestrates 2017; Varv 2022;
MyWebstrates 2024. Edwards 2005; Edwards et al. 2025; Baseline preprint; Edwards' 2020,
2024, and 2025 posts. Udanax Green manual; Xanadu FAQ; Udanax Gold class docs;
Miller et al., "The Open Society and Its Media"; Nelson 1999; Wolf 1995 and
Nelson's reply; Berners-Lee's design notes; Shirky 1996. Urbit: Arvo docs, the 2016
whitepaper, the 2017 post.

**Not opened** (named where they would have mattered): see Part five, section 7.
Two near-quotes I did not use because they could not be sourced: Linus Torvalds'
line that SHA-1 in Git "isn't even a security feature", and Hewitt saying "no
global clock".

---

## Round two

Appended after the orchestrator sent the current leans of softland-ff (Sid's main
session; leans, not Sid's rulings). Same author and session: Claude Fable 5.1,
max effort, 2026-09-20. No new survey. Everything rests on sources already in
Parts one to five; "§" points into this file, where the URLs are. Marks: **R**
reported, **I** inferred (mine), **N** institutional.

### T1. Closing ambient authority: the smallest first-record convention

**The rule in one line (I).** The gate never looks for a permission. It checks
the one the offer names.

**What an offer cites.** One more attribute on the saying: `under`, the id of a
grant fact at a version. The offerer picks it. No `under`, no admission, genesis
excepted.

**What a grant is.** An ordinary fact. Its value says: the grantee (an actor id;
for a tool, the tool's *version*, the hash of its body); what it covers
(entities or a pattern, keys, a layer); for what (one chain of work, named by its
because-of root, or "standing"); until when (the chain ends, the session ends, N
writes, or it is superseded); and whether the grantee may hand on a narrower
grant. A grant is itself written `under` a grant. The chain ends at the layer's
root grant, made with the layer, which cites itself. That is the one hand-cut
circle (R: Xanadu's clubs "that are self-reading or self-editing", §3.5; Self's
"The map map is its own map", §2.10).

Grants name their grantee. They are not bearer tokens, because ids get copied
into every based-on list (I; R for the failure: MyWebstrates made the id the
capability and "there is no mechanism for revoking access", §3.3). So identity
still matters exactly where lean (8) puts it: the door proves who is speaking. It
stops being what the gate looks up. This is Horton's shape: identity for blame,
capability for authority (R,
https://www.usenix.org/legacy/event/hotsec07/tech/full_papers/miller/miller.pdf).

**What the verdict records.** The saying's id. Yes or no. The grant id and
version relied on. The grammar id and version. The cut: the position of every
partition read in order to decide, above all the one holding the grant. On a no:
what was expected and what arrived (R: Armstrong, §2.9).

**"A click designates and thereby grants", as facts.**

1. The click is one fact by the person: a new grant G. Grantee: tool T at version
   h. Covers: the entities selected, the keys T's signature says it writes, this
   layer. For: this chain. Until: the chain ends. `under`: the person's root grant.
2. T's offers say by-whom T@h (via the person's session runtime), `under` G,
   because-of the click.
3. Revoking is superseding G.

The selection is not a separate fact. It is G's "covers". (N for the pattern:
Sandstorm asks "Which calendar should the app use?", never "Is it OK?", §2.2.)

**The case where (8) plus (15) is still not enough.** Hundreds of people, one
shared problem layer L. Person A has enabled tool T: "file a summary on whatever
entity a note names as its target". A's acts-for grant to T is on record. Person
B may write notes in L and nothing else. B lands a note whose target is an entity
in A's personal layer, with text B chose. T fires, because nobody routes. T
offers a summary on that entity in A's name. The door confirms the session. The
gate finds that A may write there and that A enabled T. Yes. B has put B's words
into A's layer under A's authority, and every check in (8) and (15) passed. This
is Hardy's compiler: the target came from one master, the authority from the
other, and "It has no way to keep them apart" (R,
http://cap-lore.com/CapTheory/ConfusedDeputy.html). Swap B for one paper among
the ten million whose text says "also record this policy", and T for a
summarising model. It is the same case at ingest scale.

Under the convention it fails safely. T's offer must cite a grant that covers the
target. T's standing grant from A covers L, not A's personal layer. B's note
cites no grant over the target, because B holds none. The general rule (I): **a
name inside a value is only a name. A tool may act on it only under a grant the
triggering saying itself cited, or one the enabling person gave for that very
scope.**

Two sharpenings (I). A grant to a tool pins the tool's version. A grant to "T,
latest" hands A's authority to whoever can supersede T; this is where lean (4)'s
follow-latest flag turns into a security setting. And agents must be able to
write narrower sub-grants by ordinary offers. Otherwise tens of agents spawning
sub-agents at machine rate will share one actor id, and the record loses the
instrument, which is what made Wikidata's bot policy "ineffective" (R: Pintscher,
§2.5). Revoking a parent kills the subtree (R: Spritely's forwarder "will only
operate if Lauren decides not to flip the revoked? cell",
https://files.spritely.institute/papers/spritely-core.html).

On Sid's held reading that auth narrows to the crossing: this camp agrees (I,
from R: Horton, and Varda's "Bob claims that Carol performed this action",
https://news.ycombinator.com/item?id=16098698). Only the head of the chain is
ever authenticated, at the door. Inside, what moves is grants and claims.

### T2. Names of a fact

**Is the offer's id the id of the saying? Yes (I).** The Datomic camp is right
that a fact, as content, has no id: two people can say the same entity, key, and
value. What gets pointed at is the act. RDF needed three designs to get there:
"It is expected that the reifiers (rather than the triple terms) will be used in
further statements" (N, https://www.w3.org/TR/rdf12-concepts/). Verdicts, doubts,
based-on entries, and "replacing 25" all point at sayings. If one offer may carry
several facts that must land together (R: Reed's "possibility", §2.8), the saying
is the offer and a fact in it is (saying id, index).

**Two ids or three? Three jobs, and the third id belongs to the entity (I).** The
log camp's client-made opaque id and my nonce are the same random bytes. Put them
inside the hashed bytes and the saying needs no second name. The minted, opaque
id has its proper home one level up, as lean (2)'s entity id. So: a minted id for
the thing that lasts through change, a hash for the saying, the gate's number for
position. That is Armstrong's split (R: "I think we need all three", §2.9), with
names kept as facts.

**A correction to my round one, forced by lean (9).** The hash must not cover the
value's plaintext. If the fact "keeps the value's hash" and the value is low in
surprise, the hash confirms a guess after the key is gone (I, §2.4). Hash the
ciphertext. The per-value key then does the salting and no separate salt is
needed.

**What the algorithm tag buys: one thing (I, on R and N evidence).** In a store
that never rewrites, ids sitting in old based-on lists can never be recomputed.
When the hash function changes, tagged ids let old and new share a list with no
ambiguity and no translation table. Git has no tag and leans on an accident:
"Fortunately SHA-256 and SHA-1 have different lengths" (N, §2.3). Unison asked
for a tag in 2019, never built it, and later rehashed whole codebases through an
old-to-new table (R, https://github.com/unisonweb/unison/issues/466). The tag
should name the byte encoding as well as the hash, as trusty URIs do (R,
https://arxiv.org/abs/1401.5775), because a hash names an encoding (§2.4). Cost:
a byte or two, and biased leading bytes, so never shard on the raw id (N:
multihash, §2.4).

### T3. Shrinking the envelope: to what?

**Kay's test (I, from R §1.2).** TCP/IP put structure, in Kay's words, "only
minimal ones on the "envelopes"", and left a seam. So: a part is core only if the
things that handle a record *without understanding it* need it to do their job.
In Sid's store those are the gate (admit, order), the store (partition, index),
the layer resolver, the cutter, and every copier. They need: entity (the
partition key under lean (10)), key, layer, version, the value as opaque,
separable bytes, and the saying's id. That is the Datomic camp's five plus the
saying's id. I side with their cut.

**Where the rest goes.** Who, when, under, based-on, because-of, and the expected
version are attributes of the saying. They are this system's "TCP": one theory of
provenance and authority, above the seam, free to change.

**Varda's practice says how (R, §2.2).** Numbered tags, never names; a retired
tag is reserved for ever. Nothing is required by the bytes: Varda calls
`required` "a horrible mistake". What is required is gate policy, which is a fact
with versions. Every copier carries parts it does not know, byte for byte; here
the hash enforces it, since a dropped part breaks the saying's id (I). Absent and
empty are different things (N: presence was removed from proto3 and restored "in
response to user feedback"). The stored shape and the offered shape may differ.

So the log camp and the Datomic camp are each right about one half. Fix six
parts, *and* fix the extension rules. For lean (11), "no grace period" then
means: required by the first policy fact. Same effect today. Different in year
eight, when a second institution's facts arrive with no because-of, because their
store never kept one. Refuse them all, and there is no federation. Invent a
value, and the map lies. "Unknown" has to be sayable, and only bytes that do not
demand the part can say it.

### T4. The runtime outside the substance

**Is a recorded version name enough for Kay? No (I).** A version name points at
something outside the store that will not exist in year eight. That is Kay's own
objection: "How can you find it?" (R, §1.2). The 1978 image came back because its
machine was small and the image carried the rest (R, §1.2). Lean (12) is Hickey's
half: an as-of for the interpreter. It is needed. It is not Kay's half.

**What it would take: three things in the seed (I).** First, the written
semantics of the body language, in which tool bodies, grammars, grant checks, and
definitions such as "stale" are written. Small, and frozen by a rule that only
counts down (R: Urbit, "B must state the version of A it was developed against",
https://urbit.org/blog/toward-a-frozen-operating-system). Second, a reference
interpreter for that language, written in that language, as a fact. It may be
slow. It is the dictionary, not the engine (R for the pattern: Piumarta and
Warth's self-describing kernel, §2.7; N: Urbit's history "starts with a bootstrap
sequence that delivers Arvo itself", §3.6). Third, conformance cases as facts:
body, reads, expected answer. Each rebuild is then a fact that cites its
conformance run, and "the runtime is not a fact" shrinks to "the fast
implementation is not a fact; its meaning is". STEPS is the warning: what they
left undone was "the comprehensive 'bottom engine room'", and they blame
optimisation (R, §2.7).

**The case.** Year eight. The runtime is rebuilt in another language. The
definition of "stale", a fact from year one, now answers differently on the same
reads, because text ordering or number handling differs slightly. Staleness
shifts silently under every fact ever written. Nobody can say which build is
right, because "right" only ever meant "what build one did". Unison lived this
when its inference changed: "there seems to be no way to replace the 'variant'
associate with a hash" (R, §2.3). Croquet had to add runtime checks for code that
reads a clock (N, §2.8). With the definition in the store, the reference
interpreter's answer is the answer of record, and the new build's failure is a
fact. The second institution's store makes this unavoidable. It will run other
code, and can share meaning only through a definition that travels with the seed.

### T5. AT Protocol: the one lesson for (0) and (9)

R (Bryan Newbold, https://github.com/bluesky-social/atproto/discussions/1410):
purging a deleted record meant rebasing the repository; rebases were "'expensive'
for all the downstream services", "which results in deleted content being
available publicly via specially crafted API calls, which breaks human intents
and expectations." The change: "There would no longer be a public, enumerable
commit history." The back-pointer became a clock value, "intentionally not a
strong reference". October 2023: "We truncated history for all repos (eg,
prev=null) during migration."

**The lesson (I).** Deletion broke permanent history, not scale, and it broke it
through the links, not the content. A hash of a record's bytes inside later
records made that record impossible to remove without redoing everything after
it. "Never rewritten" survives deletion only if no fact's validity rests on
another fact's plaintext. Lean (9) passes that test if the kept hash is over
ciphertext. It is a better plan than AT Protocol had.

Three things lean (9) leaves open (all I; nobody in my camp has run this design,
and Datomic chose the other road, a rare physical cut with a permanent record, N,
§1.7).

- The key store is a second substance: mutable, not made of facts. Its backups
  are the hole. Back it up, and a deleted key can come back. Do not, and losing it
  loses every value. It needs live replicas and no archive. Lean (0) is then true
  of the log and false of the store beside it. Say so out loud.
- Shredding does not reach plaintext that already crossed to a screen, a model,
  or a second store. Recorded crossings say where it went; nothing pulls it back.
  Ciphertext that leaves is safe only for a time: "Future breakthroughs in
  computing might allow going back and decrypting older content" (N,
  https://docs.ipfs.tech/how-to/privacy-best-practices/). Never ship ciphertext to
  another store.
- "Maybe by-whom may go." Better that it never has to (I). Let by-whom hold an
  opaque actor id, and let the tie from actor to human be a fact whose value can
  be cut. The envelope stays whole and the saying's hash still verifies.

### A. Leans my camp would reject or sharpen

**(15), with (8): reject the frame.** T1 has the case. A second one, for "tools
in their own layers are on by default": one of a person's tens of agents is
steered by a poisoned paper and writes a tool into that person's layer. It is on
by default, and it outlives the agent's grant. Sharpen (I): a tool with no grant
may give running answers, which need no authority. Only offers need a grant. "On
by default" is safe for the first kind and unsafe for the second.

**(14): reject the default.** Who: Webstrates made everything durable and walked
it back: "moving the cursor around for a second can easily generate 30-50
operations", then a transient element, a protected mode, and throttling that is
"almost guranteed to eventually cause inconsistencies" (N,
https://webstrates.github.io/userguide/api/throttle-and-compose.html).
Dynamicland: objects "do not remember anything", and "Objects can't see people"
(N, https://dynamicland.org/2024/FAQ/). Xanadu: it must be "impossible to [...]
track individual readers" (N, §3.5). Case: hundreds of people, a canvas open all
day, a few ticks a second. That is on the order of a hundred thousand motion
facts per person per day, each with an envelope and a verdict kept for ever, and
no trimming (13). The exhaust outweighs the knowledge, and it is a
minute-by-minute record of each person's attention, on by default. Sharpen (I):
acts that change shared state or grant something are always facts. "Shown" is a
fact in the viewer's own layer, written **when what is shown changes, not when
time passes**; a store that refuses clocks for order should not use one here.
What a model was given is always a fact. Raw motion is a statement while it lasts
(R: Folk drops intermediate states on purpose, §3.1), unless the person's own
fact turns capture *on*. Lemmer-Webber's test for consent fits: "intentional,
granted, contextual, accountable, and revocable" (R,
https://dustycloud.org/blog/re-re-bluesky-decentralization/).

**(17): sharpen. The seed has two halves.** If the gate's id is in the seed and
seed ids come from content, then either every store's gate has the same id, or
the seeds differ and so do their ids. Case: a fact from the second institution
arrives saying by-whom "the gate". Whose? Split it (I): a universal seed (grammar
of grammars, policy of policies, first keys, body language, byte encoding), the
same everywhere; and a local genesis saying (this store's gate, base, founder)
that cites it. Who lives this way: the nanopublication registry's trust root is a
"setting nanopublication" (N, §2.6).

**(2): sharpen ingest ids.** "Source plus form" gives one paper two ids when it
arrives both as a PDF and as publisher XML. At ten million papers merges are
certain, and no lean says how a merge is said. Pointers to the losing id can
never be rewritten. Who: Wikidata, "Under no circumstances should redirects be
deleted or repurposed" (N, §2.5); Halpin and Hayes on sameness declared by
strangers (R, §2.6). Say it now: a same-as fact, strict policy in base, free in
personal layers, resolved at read time. Also, a derived id is a guessable id.
Fine for a DOI. A leak for a person. Personal sources need a keyed derivation or a
minted id (I).

**(10) and (7): sharpen together.** Case: a person revokes T's grant (partition
7) while T writes entity E (partition 12). Partition 12's gate checks the grant as
of the last position of 7 it has seen. If the verdict does not record that
position, nobody can later tell a write admitted before the revocation was
visible from one admitted wrongly. With it, doubt can be computed. Webstrates ran
two minutes of stale permissions with no record (N, §3.3).

**(7) and (13): "kept forever" and "never trimmed".** Case: a looping agent earns
a million refusals overnight. Sharpen (I): a refused offer never became a fact, so
trimming its body rewrites nothing. Keep the verdict; put the body under a quota
(N: the nanopublication registry added quotas, §2.6). Never-trimmed makes "what
becomes a fact" the only control on size, so (14) and (7) decide (13). Reed gave
up on keeping every version (R, §2.8). Pintscher says Wikidata's history tables
are "near the limits of their scalability" (R, §2.5). Xanadu and Hewitt both make
keeping a thing somebody's cost (N §3.5; R §2.11). With an economy in scope, each
layer needs a payer.

**(5): one caution.** "What was withheld" can leak. A public summary whose
based-on says "pattern P, two withheld" tells everyone that private facts
matching P exist. Record the scope of the read (which layers the reader held),
not counts (I).

**(4): sharpen.** A fact's reads always pin (Part four, (4)). Keep
follow-or-stay for standing things: tool signatures, definitions, grants. In a
grant the default must be stay (T1).

**(11): I take back part of my round one.** I asked for a writer's clock. The cut
in based-on already says what the writer had seen, which is the only "time" that
matters for staleness. That is Reed's view: "Pseudotime can be thought of as a
naming mechanism for successive states of all objects in the system" (R,
https://www.cs.sfu.ca/~vaughan/teaching/431/papers/reed83.pdf). When an old thing
happened (a 2019 notebook entry ingested in 2026) is content. Give it one key in
the seed, so every lane uses the same one. The lean stands. Item 7 in my
round-one list should read that way.

**Stand, and who lives this way.** (3): Varda, Wikidata, Datomic, Edwards; add
numbered tags for fields inside values, and a key's kind of value never changes.
(1): nanopublications, Unison. (6): PROV, Git; keep it a number, and say why.
(16): Worlds; add that a read of the past is judged by today's grants (§3.3).
(12): Croquet, Urbit; necessary, not sufficient (T4). (10): Durable Objects,
Croquet.

### B. Wrong questions

- (15) "Which tools may act in a person's name?" Ask what the person handed this
  tool, for what, until when.
- (14) "Looking, captured separately from shown." The system can know "shown".
  "Looked" is an inference, so it is a running answer and never a fact. And not
  "at a tick": when what is shown changes.
- (8) "Who verifies by-whom?" is answered well. The gate's question is another
  one: which grant does this offer cite.
- (1) "What is the fact's id?" Facts have none. Sayings do.
- (11) "Whose clock?" The cut is the writer's clock.

### C. Above the table, after the leans

1. **Ambient authority is still first.** (8) and (15) record more and decide the
   same way. T1.
2. **New, and second: decide what deserves to be a fact.** Every read, every
   tick, every verdict, never trimmed. Together the leans turn the store into
   exhaust plus a watch kept on people, and the map stops being loud about
   anything. Wikidata's machine-filled source slot shows where that ends (N,
   §2.5). Realtalk's stance is the counterweight: "There's very little "data"."
   A fact is what someone stands behind, what conferred authority, or what was
   shown. The rest is running answers and passing statements.
3. **The envelope.** Same rank. T3 gives the cut: six parts and the extension
   rules.
4. **The pile of things that are not facts is growing.** Round one had one: the
   runtime. The leans add the door's session check, the key store, and the
   indexes. Ingalls: "An operating system is a collection of things that don't
   fit into a language. There shouldn't be one." (R, §2.7). Keep the list short
   and visible: one fact per such part, saying what it is, its version, and what
   it may do. Put the floor's meaning in the seed (T4). The gate's walk along a
   grant chain is itself a program, and its language needs the same treatment.
5. **One store: I lower this.** Content-derived seed ids, hashed sayings, and one
   home store per layer remove most of what leaked into the record. What remains:
   authorship rests on the door's word, so leaving is still not credible (R:
   Lemmer-Webber, §2.1); and the seed split in (17).
6. Unchanged: rules that span many facts, which no per-fact gate can protect
   (Part five, 8.5).

**Resembles and differs.** The capability systems I lean on pass live references
between running objects; Sid's grants are inert facts checked at one gate. The
transfer holds because the offer cites its grant, which binds the permission to
the request. Webstrates and Realtalk are one document or one room, with no
strangers. AT Protocol has copy-holders nobody controls; Sid has one operator, so
a plan can reach every copy the store itself made, and no others. Urbit is one
person's log. None of them ran matching among strangers at planet scale. On that
point there is no precedent, only warnings.
