# C7 envelope (13)(11): main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L37-38 · SHORT · ABOVE C7,C8,E3,E6**
*1. The short version*

**1. The camp puts provenance on the saying, not on the fact.** Datomic's fact is a fixed five-part tuple: entity, attribute, value, transaction, added-or-retracted. Who, when, why, from what source, on what basis: all of these are attributes of the *transaction*, which is itself an entity with an open set of attributes. Hickey, 2016: provenance goes "on the transaction (which can have an open set of attributes)… This is substantially more efficient than replicating this on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)." [H-ML16]. Sid's envelope puts by-whom, when, based-on and because-of on every fact. This camp would move them to one reified act of saying that many facts share.


---
**datalog L47-50 · SHORT · ABOVE C7,C3**

**The strongest challenge to the premise.** Hickey built Datomic for a closed world and said so: "Being oriented toward business information systems, Datomic adopts the closed-world assumption, avoiding the challenges of universal naming, open-world, shared semantics etc of the semantic web." [H-IM]. One store for the planet, with keys everyone shares, is exactly the problem he chose to avoid. His camp would not build one store with a layer slot. They would build many databases, each with its own single order, and compose them at read time: "Database is an argument to query. It is not the ambient container for a query." [H-DD]. Their second challenge is the envelope itself: a fixed nine-part record is what Hickey calls place-oriented. "If you have places, you have to have something in the place." [H-MN]

---


---
**datalog L592-593 · ABOVE · ABOVE C7,X2,A2**
*7. What this camp would question above the table*

**"Tools, grammars, policies, and definitions being facts in the same store."** Datomic did this for schema and, in Pro, for code. Three lessons from living with it. (a) Schema facts have history, but reading the past through a past schema was never built, and is documented as a limit. If grammars have versions, some record must say which version admitted each value. (b) Code as data gave way to code named by a git commit. The commit is the identity and an unreproducible build says so in its name. Sid's third kind of read, the outside anchor, is the natural fit for a tool's body. (c) "Enforced starting on the transaction after they are asserted" only means something when rule and data share one order. If policy facts live in another order from the facts they govern, the gate must write down where it read them.


---
**datalog L596-597 · ABOVE · ABOVE C7,C8,E3**

**"A fixed nine-part envelope both ends share forever."** Their sharpest objection, with a constructive alternative. A fixed record is place-oriented: "If you have places, you have to have something in the place." Provenance on every fact is, in Hickey's words, less efficient and less *correct* than provenance on the saying. "Otherwise datoms become enormous." Yet they do accept a fixed tuple at the bottom. Theirs has five parts, and the test is simple: every fact always has every part, and no part is ever empty. Put Sid's nine through that test. Entity, key, value, version and layer pass. By-whom and when are always present but are the same for everything said in one act. Based-on and because-of are sometimes empty. By the camp's rule the last four are attributes of the saying, held in an open set that can grow by accretion, and only the first five are the envelope. "Forever" then has to hold for five things, not nine.


---
**datalog L621-621 · ABOVE · DISAGREES C7**
*8. Questions this camp would call the wrong question*

- **(11) "Because of: always filled, empty only when it starts a chain?"** Only a slot can be empty. An attribute that is absent is simply absent. The question exists because of the envelope.

---
**datalog L622-622 · ABOVE · ABOVE C7**

- **(13) "Plain maps or classes?"** Not a question for this camp. Data. The argument with Alan Kay is their whole answer.

---
**datalog L781-784 · R2 · ABOVE C7**
*Round two › R2.1 The tailored questions › T1. The unit of saying*

*The saying.* One per act of offering. It is an entity.

| part of the saying | who writes it | note |
|---|---|---|

---
**datalog L792-792 · R2 · DISAGREES C7**

| part of the saying | who writes it | note |
| because of | the runtime | absent when the saying starts a chain |

---
**datalog L803-804 · R2 · DISAGREES C7**

That is five parts on the fact. It passes the test from section 7: every fact always has every part, except "replaces", which is absent only when there is truly nothing to replace.


---
**datalog L805-806 · R2 · ABOVE C7,C8**

**Sources for the redrawing.** R: Hickey puts provenance "on the transaction (which can have an open set of attributes)… as the 'saying' of it *is* the transaction" [H-ML16]. R: "Transactions provide a place to model 'when', 'who', 'where', and 'why'" [D-BEST]. R: two independent teams do it. Nubank attaches the git version of the service and the user's credentials to every transaction [N-QCON]. Shortcut, 2017: "We add the following two attributes to every transaction to indicate both the actor and the context: :audit/user — the actor who carried out the transaction; :audit/org — the context in which the transaction occurred" [SC17]. Shortcut's second attribute is Sid's layer, sitting on the saying.


---
**datalog L821-821 · R2 · ABOVE C7,C8,E3**

- **(11)** When: once. Based on: once. Because of: once.

---
**datalog L925-928 · R2 · ABOVE C7**
*Round two › R2.1 The tailored questions › T5. "A fixed envelope is place-oriented." What would they fi*

### T5. "A fixed envelope is place-oriented." What would they fix instead?

Hickey names Sid's question himself: "What constitutes minimal sufficiency of 'data' is a useful and interesting question. E.g. should data always incorporate time, what are the tradeoffs of labeling being in- or out-of-band, per datom or dataset, how to handle provenance etc." [H-HN16]. Taking his four clauses one at a time, with Datomic's own answers:


---
**datalog L930-930 · R2 · ABOVE C7,C4**

- **"Labeling… in- or out-of-band."** In-band, per fact. The label is the attribute, and it rides in every datom. That is what makes a fact readable alone. What stays out of band is the label's *definition*: its type, its cardinality, its word. Those are facts about the label. A fixed envelope labels out of band, by position. Part seven means because-of only because both ends agree that it does, for ever. R: "a straight product type just completely complects the _meaning_ of things with their position in a list" [H-MN].

---
**datalog L931-931 · R2 · ABOVE C7**

- **"Per datom or dataset."** Per dataset, where the dataset is what was said together. R: his seismometer example in the same thread is provenance per stream: "given the numbers and the provenance alone (these numbers are from a seismometer)" [H-HN16]. R: "replicating this on many facts" is the thing he argues against [H-ML16].

---
**datalog L932-933 · R2 · ABOVE C7**

- **"How to handle provenance."** As an open set of named attributes on the saying. The set grows. It never breaks.


---
**datalog L942-945 · R2 · DISAGREES C7**

**The deciding case: the tenth part.** Sid's scales name it already. *The planet, with an economy*: every act will one day need a price, a payer, a budget. *A second institution's store*: every saying that crosses will need to say where it came from and who vouches for it, because the door's check of by-whom means nothing outside the store that ran the door. With nine places, a tenth means a new envelope version, a runtime that reads both for ever, and every earlier fact silently lacking the part. With a vocabulary, it is one new key in the next edition of the first facts. "Both ends share it forever" is a promise that can be kept for five parts. For nine, it would already have been broken by year three.

---


---
**datalog L972-973 · R2 · NEW-CASE C7**
*Round two › R2.2 Ask A: the leans, one by one*

**(11) "No grace period for any of the three." Stands, and it is the most strongly sourced lean on the table.** R: Cavalcanti, "what I regret the most, is to have implicit operations that are not stored into the database, or that don't have origination information" [N-POD]. **Case:** a background job, a migration or a repair script writes facts with no because-of "just this once". Three years later those are the facts nobody can explain, and in a regulated setting that is the finding.


---
**datalog L974-975 · R2 · DISAGREES C7**

**(11) Because of: "empty only at a chain's start." Sharpen.** An empty slot cannot tell *this starts a chain* from *the cause is not known here*. R: "The maps know what they know" [H-MN]. **Case:** a fact arrives from the second store whose cause never came with it. Or its cause sits in a layer this reader may not see. Or its cause was erased. None of these starts a chain. Make a chain's start a positive statement, such as the kind of the act (*a person's click*), and let an absent because-of mean unknown.


---
**datalog L1015-1015 · R2 · ABOVE C7,X1,C3**
*Round two › R2.4 Ask C: what they would change above the table*

1. **Strongest change: add the saying.** Make the unit of admission a set of facts said together, with one home, one position, one verdict and an open set of provenance on it. It takes the envelope from nine parts to five. It gives tools a dial for the precision of based-on. It gives corrections to provenance somewhere to live. It gives the tenth part somewhere to go. It costs partition-by-entity. That is why (10) and the saying have to be decided together.

---
**frontiers L420-420 · ABOVE · ABOVE C7**
*9. What this camp would question above the table*

- **A fixed nine-part envelope, forever.** Goebel: "different communication contexts call for slightly different types of datoms" (REPORTED). The camp's own envelope is three parts, `(data, time, diff)`, and everything else is data. The camp would ask which of the nine parts the *system* must understand in order to be consistent (time; what is retracted) and which are payload that could equally be ordinary facts about the fact (INFERRED).

---
**frontiers L575-575 · R2 · ABOVE C7,C4**
*Round two (2026-09-20): the leans, pressed from the frontier › C. Above the table: what the leans change*

- The fixed envelope: the leans quietly add three kinds that only the floor writes (verdict, read set, cut or announced point). They belong in the seed of lean (17).

---
**log L548-549 · ABOVE · ABOVE C7**
*For the group › 8. What this camp would question above the table*

**A fixed nine-part envelope both ends share forever.** The camp would say that fixing the parts is the wrong thing to fix. CT froze a format and survived only because of one opaque slot; its clean second version was never adopted. Delos's positional entries broke under version skew. Young's maps survive but can never be renamed. Cambria shows where tolerance ends. What they would fix forever instead: that the envelope is a map of named parts; that it has a version; that there is one extension slot and it is inside the hash; that a reader ignores what it does not know; and that a changed meaning gets a new name.


---
**log L723-723 · R2 · NEW-REASON C7**
*Round two: the leans, pressed › A. The other leans*

- **(13) Stands**, with T4's volume warning. Plain maps: Delos moved to "a map of headers" (R). Add the envelope version and the extension slot.

---
**meaning L2611-2620 · ABOVE · DISAGREES C7**
*Part five — for the group › 8. What this camp would question above the table, ranked by *

**2. A fixed nine-part envelope, shared by both ends, for ever.** Strong. Kay and
Hickey both shrink it (1.7). Varda: "required is forever"; no header is "one of the
best properties"; storage and wire shapes diverge (2.2). AT Protocol carries a dead
required slot for good (2.4). Kay's own regret is a design frozen too soon (2.7).
Self's rule about features born from compelling examples (2.10). The camp is not
against an envelope. Kay's tape has "the first ten or so pointers standard".
TCP/IP has a small header and a seam. Their position is: a tiny core that the gate
needs in order to admit and order a fact; everything else open, growing only by
addition; what is *required* set by gate policy, which is a fact and can change.


---
**meaning L2685-2693 · ABOVE · ABOVE C7**
*Part five — for the group › 9. Questions this camp would call the wrong question*

- **The table itself**, Kay might say. "The key in making great and growable
  systems is much more to design how its modules communicate rather than what
  their internal properties and behaviors should be." On that view the thing to
  fix before the first record is the conversation between an offerer and the
  gate: what an offer must say, what a verdict says back. The stored shape should
  then be as open as that conversation allows. Self's warning belongs here too.
  Nineteen questions, each with a compelling example, is how a design gets five
  pages of rules.


---
**meaning L2921-2931 · R2 · ABOVE C7**
*Round two › T3. Shrinking the envelope: to what?*

### T3. Shrinking the envelope: to what?

**Kay's test (I, from R §1.2).** TCP/IP put structure, in Kay's words, "only
minimal ones on the "envelopes"", and left a seam. So: a part is core only if the
things that handle a record *without understanding it* need it to do their job.
In Sid's store those are the gate (admit, order), the store (partition, index),
the layer resolver, the cutter, and every copier. They need: entity (the
partition key under lean (10)), key, layer, version, the value as opaque,
separable bytes, and the saying's id. That is the Datomic camp's five plus the
saying's id. I side with their cut.


---
**meaning L2932-2935 · R2 · ABOVE C7,C6**

**Where the rest goes.** Who, when, under, based-on, because-of, and the expected
version are attributes of the saying. They are this system's "TCP": one theory of
provenance and authority, above the seam, free to change.


---
**meaning L2936-2943 · R2 · NEW-REASON C7**

**Varda's practice says how (R, §2.2).** Numbered tags, never names; a retired
tag is reserved for ever. Nothing is required by the bytes: Varda calls
`required` "a horrible mistake". What is required is gate policy, which is a fact
with versions. Every copier carries parts it does not know, byte for byte; here
the hash enforces it, since a dropped part breaks the saying's id (I). Absent and
empty are different things (N: presence was removed from proto3 and restored "in
response to user feedback"). The stored shape and the offered shape may differ.


---
**meaning L2944-2951 · R2 · ABOVE C7**

So the log camp and the Datomic camp are each right about one half. Fix six
parts, *and* fix the extension rules. For lean (11), "no grace period" then
means: required by the first policy fact. Same effect today. Different in year
eight, when a second institution's facts arrive with no because-of, because their
store never kept one. Refuse them all, and there is no federation. Invent a
value, and the map lies. "Unknown" has to be sayable, and only bytes that do not
demand the part can say it.


---
**meaning L3137-3138 · R2 · ABOVE C7**
*Round two › C. Above the table, after the leans*

3. **The envelope.** Same rank. T3 gives the cut: six parts and the extension
   rules.

---
**rama L455-456 · ABOVE · ABOVE C7**
*The group › 8. What this camp would question above the table*

**A fixed nine-part envelope, shared forever.** Marz fixes the evolution rule, not the field list: add optional fields, never reuse an id, keep the power to migrate. He would call "forever" safe only if adding is allowed.


---
**rama L701-702 · R2 · NEW-CASE C7,P0**
*Round two › A. The leans, one by one*

**(13) Storage. SHARPEN.** Plain maps stand, with Marz's caveat (R, 2010): "JSON doesn't give you a real schema… A good schema… gives you errors at the time of creating a bad object." In Rama a depot accepts any object. The partitioner runs on the client, and "A throw propagates to the caller of `foreign-append!` rather than becoming a rejection the topology can observe" (**CHECKED** `skill/depot-design.md:132-134`). The case: a buggy agent host appends a hundred million malformed offers overnight. Under never-trimmed and never-rewritten they are permanent, and every replay from the beginning reads them. So the door checks the envelope before the append, and "never trimmed" is scoped to facts, not intake (T1, shape b). Backups: online backup is a paid feature; and see (9) for keys.


---
**skeptics L296-297 · ABOVE · ABOVE C7,W2**
*7. Questions this camp would call the wrong question*

- **"Storage: plain maps or classes?"** Hamilton would ask: what is your rollback story when a format change goes wrong, given that nothing can be rewritten?


---
**skeptics L429-430 · R2 · NEW-REASON C7,P0,X2**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(0), (6), (12): stand.** The bill for (0) is known. Hamilton (R): old formats must stay readable while rollback is possible, which is now for ever. FlightTracker (R): "data invariants that were not honored by all historical data".


---
**skeptics L436-437 · R2 · ABOVE C7,C1,W2**
*Round two (2026-09-20): the leans, pressed from the skeptics › B. Wrong questions*

- (13): not "maps or classes?" but "exactly which bytes are hashed?"


---
**sync L3070-3081 · ABOVE · DISAGREES C7**
*5. The group › 5.2 What this camp would question above the table*

**"Tools, grammars, policies, and definitions being facts in the same store."**
Patchwork does it for agents ("Bots can be shared, edited, and versioned just
like regular documents") and likes it. REPORTED. AT Protocol keeps the language
of schemas outside on purpose, lets an unknown schema through, and will not
resolve schemas in the write path. REPORTED. Cambria names the regress. Frazee:
"Schemas are only interpretable in the context of working software", so a
tool's body as a fact says what the tool does only relative to a runtime. The
sharpest worry is one Hipp states for deletes and that applies to policy: a
mechanism that travels through the store can "permanently destroy vital
information", including the means of repair. A bad policy fact could stop the
gate from admitting the fact that fixes it. INFERRED.


---
**sync L3096-3110 · ABOVE · DISAGREES C7**

**"A fixed nine-part envelope both ends share forever."** Nobody's envelope
held. Git survived by keeping headers it does not understand; jj's new header
is already being dropped by older tools. Automerge requires unknown bytes to be
retained. AT Protocol has said it will "bend the rules of protocol stability",
and that one core stream will change "even if this breaks lexicon evolution
rules". fiatjaf's law says optional parts become mandatory, and Willow
faults Nostr's open tag list for "the exponential state space of arbitrary
combinations of tags". So the camp's advice is a version marker, a rule for
unknown parts, and very few versions, because every reader must read all of
them forever (Git, on hash schemes: "they will never go away, so they
accumulate"). Note also what this report's own section 4 asks the envelope to
hold beyond the nine: the offer's id, what it replaces, the grounds of the
verdict, the session, the envelope version, perhaps a digest. The camp would
predict that nine is already not the number. INFERRED.


---
**sync L3160-3161 · ABOVE · ABOVE C7**
*5. The group › 5.3 Questions this camp would call the wrong question*

6. **(13) "Plain maps or classes?"** The question underneath is whether the
   logical fact is defined apart from every runtime.

---
**sync L3582-3590 · R2 · DISAGREES C7,E8**
*Round two: the leans, pressed › A. The other leans*

**(11) When, based on, because of.** The gate's clock, never for order: stands
(Figma, Convex, Weidner, R). "Every read listed": Bayou gave up literal lists
because they "could get large" (R §2.1). Deciding case: a tool reads every
abstract in the base as of a cut. Listed literally, that is ten million entries
on one fact. "Listed" has to mean pattern plus cut plus viewpoint (Convex's
ranges, R §2.2). "No grace period": stands, and Yjs shows why (R §2.10).
"Empty only at a chain's start": jj never has an empty parent (R §3.2);
pointing a chain's start at the session fact costs nothing.


---
**sync L3670-3673 · R2 · ABOVE C7**
*Round two: the leans, pressed › C. Above the table: only what the leans change*

2. **The envelope is no longer nine parts.** The leans add an offer id, what it
   replaces, a verdict beside, roles on reads, a value id or keyed hash, and a
   session. That is fine. It means the forever-promise should be about a
   version marker and a rule for unknown parts, not about the number nine. (I)

---
## Says the same as the ledger (counted, not copied)

- C7 · datalog · 1: L1007-1008
- C7 · rama · 1: L265-266
- C7 · skeptics · 1: L441-441
- C7 · sync · 3: L764-765, L1661-1662, L2950-2952

