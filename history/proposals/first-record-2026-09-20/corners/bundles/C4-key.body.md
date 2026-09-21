# C4 key (3)(17): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L64-64 · BODY · CARRIED C4**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.1 What they built, and what they chose*

- **Attributes are entities.** Schema is data in the same database. The attribute slot of a datom holds an entity id. A keyword name is attached through `:db/ident`.

---
**datalog L135-144 · BODY · NEW-REASON C4**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.2 Their reasons, in their words*

**Why names, and why never break one.** From *Spec-ulation*:

> "Adding stuff is growth. Period. It is just easy. It is just accretion. And removing stuff is always breakage. Always." [H-SPEC]

> "the namespace is part of the name… We are always dealing in Clojure with these nice hopefully globally unique names." [H-SPEC]

> "turn what would have been breaking into accretion. In other words, if you are going to have a variant, give birth to a variant. Do not muck with a thing." [H-SPEC]

The Datomic docs turn this into rules: "The meaning of a name is established when the name is first introduced… Never remove a name. Reusing that name to mean something substantially different breaks programs that depend on that meaning. This can be even worse than removing the name." [D-BEST]


---
**datalog L169-170 · BODY · NEW-CASE C4**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.3 What they later changed, regretted, or moved away from*

4. **Schema became alterable.** For the first 21 months an attribute could not be changed once defined. December 2013: "we have added the ability to alter existing schema attributes after they are first defined… Schema alteration has been our most requested enhancement." [D-BLOG13]. What stayed fixed forever: "You can never alter :db/valueType, :db/fulltext, :db/tupleAttrs, :db/tupleTypes, :db/tupleType, or :db.tuple/discontinued." [D-CHANGE]. So the list of things that can never change got shorter under pressure, and what was left is the value's type.


---
**datalog L171-172 · BODY · NEW-REASON C4,X2**

5. **Schema does not travel back in time, and they document it as a limit.** "Because Datomic maintains a single set of physical indexes, and supports query across time, a database value utilizes the single schema associated with its current basis. Thus traveling back in time does not take the working schema back in time, as the infrastructure to support the past schema may no longer exist." [D-CHANGE]. Schema is facts with history, yet old data is always read through today's schema.


---
**datalog L204-204 · BODY · DISAGREES C4,C7**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- INFERRED. For Sid: base, the gate, the first kinds and the first grammar need ids that are *the same in every store*, so they must be fixed constants, not minted. The first fact must be the one that lets a name be attached to an id, stated in terms of itself. A reserved range or reserved namespace must exist from the start. The honest stamp for a fact nobody wrote at a real moment is a stated sentinel, not a fake timestamp. And "by whom" needs a first actor that is declared, not discovered: the system itself.

---
**datalog L209-209 · BODY · CARRIED C4**

- REPORTED. Both. "When an entity has an ident, you can use that ident in place of the numeric identifier, e.g. `[42 :person/loves :pizza]` instead of: `[42 1007 1124]`." [D-IDENT]. Attributes are entities so that schema is data: "Because Datomic schema is stored as data, you can and should annotate your schema elements." [D-BEST]

---
**datalog L210-210 · BODY · NEW-REASON C4**

- REPORTED. How they keep it fast: "Idents are designed to be extremely fast and always available. All idents associated with a database are stored in memory in every Datomic transactor and peer." This is also why idents are for schema and enums only, never for ordinary entities. [D-IDENT]

---
**datalog L211-211 · BODY · NEW-REASON C4**

- REPORTED. Rename by accretion: "Both the new ident and the old ident will refer to the entity." "We don't recommend re-purposing an old :db/ident." "Datomic allows multiple :db/idents to refer to a single entity ID." [D-CHANGE], [D-BEST]

---
**datalog L212-212 · BODY · NEW-REASON C4**

- REPORTED. Hickey's position on names is that a namespaced name is already meant to be global, and its meaning is fixed at first use. A changed meaning is a new name. [H-SPEC]

---
**datalog L213-213 · BODY · NEW-REASON C4**

- INFERRED, from *Spec-ulation* plus the `valueType` rule. "The grammar is itself a fact with versions" is safe only when each version accepts everything the earlier one did. Hickey's two kinds of growth are providing more and requiring less. A version that requires more, or provides less, is breakage wearing a version number. The camp's rule would be: an incompatible shape is a new key. Datomic enforces this by making the value type unalterable.

---
**datalog L215-215 · BODY · DISAGREES C4**

- Disagreement inside the camp: DataScript uses plain words with nothing behind them (section 5.2). Instant uses UUIDs with names as separate rows (section 5.1).

---
**datalog L216-217 · BODY · DISAGREES C4**

- Second store: in Datomic an attribute's entity id differs from database to database, while the ident is the same. So it is the *name* that crosses stores, and the id that is local. If Sid wants ids to cross stores, key ids must be global from the start (as Instant's are).


---
**datalog L334-334 · BODY · CARRIED C4**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.5 What resembles Sid's situation, and what differs*

- Schema, and in Pro even code, are facts in the same store.

---
**datalog L341-341 · BODY · ABOVE C4**

- **Closed world versus the planet.** Hickey chose the closed-world assumption to avoid "universal naming, open-world, shared semantics". Sid's base layer is universal naming and shared semantics.

---
**datalog L355-355 · BODY · DISAGREES C4,P0**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.6 Who they disagree with, and on what*

- **Nikita Prokopov** (DataScript): attributes as plain words, no history.

---
**datalog L516-517 · BODY · CARRIED C4**
*5. The leads › 5.1 Instant (Stepan Parunashvili, Joe Averbukh)*

**How Instant identifies attributes. This speaks directly to (3).** REPORTED from their source. The attribute slot of a triple is a UUID: `attr_id uuid REFERENCES attrs(id)`. Names live in a separate table: `idents (id uuid, attr_id uuid, etype text, label text)`, unique per app on `(etype, label)`. [I-SQL]. In code an identity is "an id, etype, and label (in that order) but we consider the ident name to simply be the etype and label." [I-ATTR]. The client mints these ids itself: `const attrId = uuid(); const fwdIdent = [uuid(), etype, label];` [I-INSTAML]. A rename updates the names and touches no triples. [I-ATTR]


---
**datalog L518-519 · BODY · NEW-REASON C4,C1**

So Instant reached Datomic's answer on its own: id in the fact, name as a record about the id, rename without rewriting. It went one step further than Datomic: the ids are global UUIDs, minted at the edge. They never say why. INFERRED from the code: renames are safe, and a client can create an attribute while offline.


---
**datalog L530-531 · BODY · DISAGREES C4,P0**
*5. The leads › 5.2 Nikita Prokopov (DataScript)*

**What he dropped, on purpose.** "No `:db/ident` for attributes, keywords are _literally_ attribute values, no integer id behind them." "Simplified schema, not queryable." [T-README]. And history: "There's no history tracking at DB level. When datom is removed from a DB, there's no trace of it anywhere. Retracted means gone." [T-INT]. The reason is where it runs: "DataScript DBs operate in constant space… This is unlike Datomic which keeps history of all changes, thus grows monotonically." [T-README]


---
**datalog L532-533 · BODY · DISAGREES C4**

So on (3) he is the camp's voice for *words*. The argument is simplicity at small scale, in one process, with no renames to survive. It is not an argument that holds for a shared store meant to last years.


---
**datalog L544-545 · BODY · NEW-REASON C4,E6**

INFERRED for Sid. A tool's signature is a subscription. For every landed fact the runtime must find the tools it matches, at machine rate. Prokopov's warning is that the signature language must be *weaker* than the query language, so that it can be run backwards: from a fact to the signatures that want it. Sid's rule that patterns match on the key is exactly this kind of weakening. It is also one more reason the key must be a first-class, indexable thing fixed before the first record. Instant built the same piece and called it the invalidator.


---
**datalog L556-557 · BODY · CARRIED C4**
*5. The leads › 5.3 Huahai Yang (Datalevin)*

On (3) he sides with Datomic without fuss: "Attributes are stored in indices as integer ids… This is the same as Datomic." [Y-ALTER]


---
**frontiers L316-316 · BODY · DISAGREES C4**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.2 Reasons, in Goebel's words*

- Keys. REPORTED: "strong, global names (in the form of fully qualified keywords) are found in the a-slot. This should be considered a great blessing and display of wisdom and kindness."

---
**frontiers L331-331 · BODY · DISAGREES C4,E6**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.4 Which questions Goebel speaks to*

- **(3) key: word or id?** REPORTED: global qualified names are "a great blessing". That is a vote for the word. INFERRED: in 3DF each attribute is its own input collection and its own index, so the key is the unit of sharing between queries. If keys were opaque ids with names held as facts, every pattern would need a lookup that is itself time-varying. That lookup would have to be a recorded read too.

---
**frontiers L357-358 · BODY · DISAGREES C4**
*7. Question by question: what this camp says*

**(3) Key: word or id?** Goebel votes for global names (REPORTED). INFERRED: the key is the unit of indexing and of sharing between standing queries. Resolve names to ids when a pattern is registered, and record that lookup as a read.


---
**frontiers L447-448 · BODY · DISAGREES C4**
*11. What each source implies for a second store*

- **Goebel, REPORTED:** across id domains, UUIDs. INFERRED: if the first facts (base, gate, first kinds) carry the same ids in every store, a later merge is only a time problem.


---
**log L141-141 · BODY · ABOVE C4**
*Voice by voice › 2. Mahesh Balakrishnan: CORFU, Tango, FuzzyLog, Delos, and h*

- (17) REPORTED: a hard-coded id for the first object; a tiny compare-and-set register outside the log to say which log is current.

---
**log L151-152 · BODY · DISAGREES C4**
*Voice by voice › 3. Greg Young and event sourcing as practiced*

**1. What he built and chose.** Young named CQRS, built Event Store (now KurrentDB), and wrote the one book on living with events that can never be edited: *Versioning in an Event Sourced System*. Choices: events are never updated; a stream is the unit of order and of optimistic concurrency ("expected version"); the client makes each event's id; event types are names; schema is weak (maps); every message carries three ids.


---
**log L155-155 · BODY · NEW-REASON C4,C7**

- What a version is: "A new version of an event must be convertible from the old version of the event. If not, it is not a new version of the event but rather a new event." (chapter "Basic Type Based Versioning".)

---
**log L156-156 · BODY · DISAGREES C4**

- The price of maps: "you are no longer allowed to rename something. … You can get around this by supporting both Id and ItemId, but this can quickly become annoying, especially with an Event Sourced system, where you cannot just deprecate it but must carry it forward into the future." (chapter "Weak Schema".)

---
**log L157-157 · BODY · NEW-REASON C4**

- Meaning is frozen: "semantic meaning cannot change between versions of software. There is no good way for a downstream consumer to understand a semantic meaning change." (chapter "General Versioning Concerns".)

---
**log L162-163 · BODY · DISAGREES C4,C1**

- From the Event Store documentation: the event id is "A unique identifier representing this event. Event Store uses this for idempotency if you write the same event twice you should use the same identifier both times." The type "should be a “friendly” name rather than a CLR type name". "The idempotence check is based on the EventId and stream." And the caveat: "Idempotence is not guaranteed if you use ExpectedVersion.Any."


---
**log L174-174 · BODY · NEW-REASON C4**

- (3) REPORTED: names can never be renamed, meaning can never change, and a type name tied to code is a known mistake. INFERRED: he would want the key to be a stable id whose display name is free to change, with the rule that a changed meaning is a new key.

---
**log L390-391 · BODY · ABOVE C4**
*Question by question: what this camp would say › (17) The first facts*

**For the first record (INFERRED).** A short list of well-known ids fixed in the runtime, from which everything else is found. Genesis facts that are identical in every store, ids and content, so that two stores mean the same thing by the first kinds; a content hash makes "identical" checkable. A named genesis actor with a well-known id, or no actor at all, as in Automerge. And a plain list of what can never be a fact because it is needed to read the facts: the encoding and hash rules, the trust anchor, and the pointer that says where a store continues if it moves.


---
**log L394-395 · BODY · DISAGREES C4**
*Question by question: what this camp would say › (3) Key: a word, or an id with its name and shape as facts?*

REPORTED. Young on names in a store that never forgets: "you are no longer allowed to rename something", and you "must carry it forward into the future." On meaning: "semantic meaning cannot change between versions of software." On versions: "If not, it is not a new version of the event but rather a new event." Event Store's documentation warns against tying the type to code: it "should be a “friendly” name rather than a CLR type name". The Overeem study: "The data schema is not explicitly defined at all, but is implicitly encoded" in the application. Helland: "all message schemas be versioned and that each message use the version-dependent identifier of the precise definition of the message format. Alternatively, the schema can be embedded in the message." The AT Protocol separates the permanent id from the changeable name.


---
**log L396-397 · BODY · NEW-REASON C4,X2**

**For the first record (INFERRED).** The lived regret is names that cannot be renamed and meanings that drift. The fix both Helland and the AT Protocol point to is a stable id, with the name as a changeable fact about it. Two rules go with it. A key's meaning never changes; a changed meaning is a new key (Young). Each fact, or its verdict, pins the grammar version it was checked against (Helland), because a grammar "with versions" means a bare key no longer says which shape applied. The cost: a raw log keyed by ids cannot be read without the dictionary. Identical genesis facts in every store are what make that dictionary portable.


---
**log L575-576 · BODY · NEW-REASON C4**
*The second store: what each source implies*

- **Genesis.** If the first facts are identical in every store, two stores agree on what the first kinds mean, and facts can move between them without translation. If each store mints its own, every exchange needs a mapping.


---
**meaning L179-185 · BODY · NEW-REASON C4,C7**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

**Turn 7 — Kay, 11948729, 19:31**

> 2nd Paragraph: How do they know they are even bits? How do they know the bits
> are supposed to be numbers? What kind of numbers? Relating to what?
>
> Etc


---
**meaning L757-763 · BODY · NEW-REASON C4,X3**
*Part two — the people › 2.1 The capability camp: Hardy, Miller, Yee, Shapiro, Donnel*

- (3) names. Stiegler's petnames, REPORTED: "a key that is global and securely
  unique (but not necessarily memorable); a nickname that is global and
  memorable (but not at all unique), and a petname that is securely unique and
  memorable (but private, not global)." INFERRED: Sid's layers give this for
  free. The key's id is the key. Its public name is a nickname fact in base. A
  person's own word for it is a petname fact in their layer. Nearest layer wins
  is petname lookup.

---
**meaning L823-830 · BODY · NEW-CASE C4**
*Part two — the people › 2.2 Kenton Varda: Protocol Buffers, Cap'n Proto, Sandstorm, *

- INSTITUTIONAL (protobuf.dev, proto3 guide): "This number cannot be changed
  once your message type is in use because it identifies the field in the
  message wire format." "Field numbers should never be reused." Reuse "makes
  decoding wire-format messages ambiguous", with consequences listed as "A
  parse/merge error (best case scenario) / Leaked PII/SPII / Data corruption".
  Best-practices page: "Even if you think no one is using the field, don't
  re-use a tag number. If the change was live ever, there could be serialized
  versions of your proto in a log somewhere."

---
**meaning L1197-1202 · BODY · NEW-REASON C4**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

**A key's shape is forever.** INSTITUTIONAL (Help:Data type): apart from one
narrow case, "Other changes of data type requires creating a new property and
deleting the old one." Datomic says the same in its own words (1.7). Two
unrelated systems at scale: the name of a key may move; the kind of value it
holds may not. A breaking change to shape means a new key.


---
**meaning L1203-1212 · BODY · ABOVE C4,X2**

**Definitions as facts in the same store: they did it, and it hurt a little.**
In July 2017 constraints moved from wiki templates to statements on the property
itself. INSTITUTIONAL (constraints portal): "Constraints are hints, not firm
restrictions". REPORTED (Lucas Werkmeister's announcement): some properties
"have so many P2303 qualifiers that they don't fit in the constraint database,
so that a constraint check on any item with a statement for one of these
properties crashes". A community member in the same thread: one property's page
grew from 20,508 bytes to 631,637. Note that Wikidata checks *after* admission
and only warns. Sid's gate checks *at* admission and refuses.


---
**meaning L1240-1249 · BODY · ABOVE C4**

- Types as ordinary statements. REPORTED (Brasileiro et al., 2016): "a
  significant number of problematic classification and taxonomic statements";
  their worked case makes Tim Berners-Lee an "instance of Profession(!)".
  Patel-Schneider and Doğan (2024): of 3,238 third-order classes, 3,159 are also
  second-order; "these numbers indicate that there are major errors." This is
  "no type slot; a type is just another entity that others point at", run at
  scale with open editing. The store works. The class tree cannot be trusted.
  Sid's tools match on key, not on type, and keys are gated by grammar. That
  sidesteps most of it.


---
**meaning L1368-1373 · BODY · NEW-REASON C4,X4**
*Part two — the people › 2.6 RDF, W3C PROV, nanopublications, trusty URIs*

- The server network was replaced by a second-generation Nanopub Registry, with
  per-key quotas and a trust root held in a "setting nanopublication" with
  chains of endorsement that decide whose records get loaded at all. INFERRED:
  that is a worked precedent for (17). The first fact is a fact. It names who
  may vouch for whom. Everything after hangs from it.


---
**meaning L1374-1380 · BODY · NEW-REASON C4**

**What the semantic web learned about global predicate ids.**

- Names that look like English get misused. REPORTED (Halpin and Hayes, 2010):
  "the labeling of constructs with "English-like" mnemonics naturally will lead
  to the use of a knowledge representation language by actual users that varies
  from what its designers intended." That is an argument for opaque key ids from
  people who watched the alternative.

---
**meaning L1381-1386 · BODY · NEW-CASE C4,X3**

- Sameness is dangerous in one shared space. REPORTED (same): "anyone can link
  to your data-set with owl:sameAs from anywhere else on the Web without your
  permission, and any statement they make about their own URI will immediately
  apply to yours." INFERRED: Sid's layers tame this. A same-as fact in my layer
  merges two ids for me alone. In base it merges them for everyone, so it needs
  the strictest policy in the store.

---
**meaning L1390-1393 · BODY · NEW-CASE C4**

- A namespace, once used, is forever. INSTITUTIONAL (schema.org FAQ): "both
  'https://schema.org' and 'http://schema.org' are fine." The largest vocabulary
  on the web could not move its ids. It now has two spellings for every
  predicate, for good.

---
**meaning L1463-1473 · BODY · NEW-REASON C4,X4**
*Part two — the people › 2.7 Kay beyond the thread, STEPS, Worlds, and Ingalls*

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


---
**meaning L1622-1633 · BODY · NEW-REASON C4**
*Part two — the people › 2.9 Joe Armstrong*

### 2.9 Joe Armstrong

**What Armstrong built.** Erlang, which runs phone switches that are upgraded without
stopping. Armstrong cared about two programs that do not trust each other and change
at different times. Late in life Armstrong wrote about naming.

**Three kinds of id, for three jobs.** This is the most direct source I found
for (1), (2), and (3) together. REPORTED ("The web of names, hashes and UUIDs",
2015, https://joearms.github.io/):

- "As soon as we name something there is an implied context - take away the
  context, or use the name in a different context and we are lost."

---
**meaning L1985-1993 · BODY · NEW-REASON C4**
*Part three — substrates › 3.4 Jonathan Edwards (Subtext, schema change)*

**Names are comments.** REPORTED (Subtext, 2005): "labels are purely comments,
not identifiers". "Every label could be foo, confusing the programmer no end,
but not the computer." In Edwards' current work (Baseline, a preprint, with Tomas
Petricek): "we assign permanent unique identifiers (IDs) to every record field
and list element… Because record fields have unique IDs their names are only for
human readability". Note that this goes one level below Sid's key. The *fields
inside a value* get ids too, like protobuf tags. Webstrates reached the same
place on its own: a hidden unique id on every element.


---
**meaning L1994-2003 · BODY · NEW-CASE C4**

**Shape change must be recorded as what was done, not as before and after.**
REPORTED (Baseline): "State-based approaches observe only the before and after
of changes, while operation-based approaches record the execution of a set of
possible operations… For example comparing states of a table schema, a move
followed by a rename is indistinguishable from a delete followed by an insert,
but that makes a big difference to the data." And (2025 vision statement): "you
can't tell how to migrate the data just by comparing types before and after.
[...] It is necessary to capture the user's intention as they interactively edit
the type."


---
**meaning L2207-2217 · BODY · NEW-REASON C4,X4**
*Part four — Sid's questions, hung on the parts of the fact › (17) The first facts*

### (17) The first facts

**Camp.** A small kernel that describes itself, with the circle cut once, by hand.
Piumarta and Warth: three object types, five methods, "described entirely in terms
of those same objects and messages", bootstrapped in four written steps (2.7).
Self: "The map map is its own map" (2.10). Xanadu: clubs that are "self-reading or
self-editing" end the regress of who may read the list of who may read (3.5). The
nanopublication network's trust root is itself a "setting nanopublication" (2.6).
Urbit's first events deliver the interpreter itself (3.6). Kay's tape: "the first
ten or so pointers standard" (2.7).


---
**meaning L2229-2239 · BODY · CARRIED C4**
*Part four — Sid's questions, hung on the parts of the fact › (3) Key: a word, or an id with its name and shape as facts?*

### (3) Key: a word, or an id with its name and shape as facts?

**Camp.** Close to unanimous: an id. Varda: names get changed, collide, and waste
space (2.2). Protobuf: numbers on the wire, never reused (2.2). Wikidata:
language-neutral ids, labels as data (2.5). Datomic: the attribute is an entity;
after a rename "Both the new ident and the old ident will refer to the entity"
(1.7). Edwards: "labels are purely comments" (3.4). Halpin and Hayes: English-like
names get misused (2.6). Stiegler: key, nickname, petname (2.1). Armstrong: names
carry context; "I think we need all three" (2.9). Self: names came back as
ordinary data (2.10).


---
**meaning L2240-2245 · BODY · DISAGREES C4**

**Split.** Hickey wants identity names to be globally qualified words, and wants
data readable by "generic processors" with nothing fetched out of band (1.7).
Unison paid for opaque ids: "It's easy to accidentally replace your human readable
names with mysterious hashes" (2.3). Wikidata admits names and ids can drift apart
with "nothing implemented in the system to prevent it" (2.5).


---
**rama L76-78 · BODY · NEW-REASON C4**
*Part one — What Rama's own reference says › Question by question › (17) The first facts: what ids, who writes them, the same in*

**NOT IN THE REFERENCE.** Rama has no seed-data or genesis hook. The nearest things:

- **CHECKED** `docs/12-microbatch-topologies.md:37` — a global PState can be given an `initialValue`. That is a constant in module code, not a fact.

---
**rama L79-80 · BODY · NEW-REASON C4**

- **CHECKED** `skill/depot-reference.md:138-146` — a new topology can start from `:beginning` of a depot, once, on first deploy.


---
**rama L311-312 · BODY · NEW-REASON C4**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(17) First facts.** He has not written about this. INFERRED from his practice: the first rules live in code, because his schemas and functions always did.


---
**rama L345-345 · BODY · ABOVE C4,X2**
*Part two — Nathan Marz › 5. What resembles Sid's situation, and what differs*

- His schemas and functions lived in code, deployed together, one version live at a time. Sid's grammars, policies and tools are facts in the store, with every version live forever.

---
**skeptics L34-35 · BODY · DISAGREES C4**
*1. The ordinary default, question by question*

**(17) The first facts.** COMMON PRACTICE: schema migrations plus seed scripts. Well-known rows get hard-coded ids or are looked up by name. Ids differ between environments unless the seed pins them. The database's own catalog is the true first data and has fixed ids chosen by the vendor. I opened no source for this.


---
**skeptics L36-37 · BODY · DISAGREES C4,X2**

**(3) Key: a word, or an id?** A word for people, a number on the wire. Column names and event-type strings are words. The schema registry gives each schema a number. REPORTED: "The default compatibility mode is BACKWARD. Each schema version gets a unique ID and incremented version number. When schemas are updated, Schema Registry checks compatibility before accepting the new version." The reason for that default: "BACKWARD compatibility mode is the default, and preferred for Kafka, is so that you can rewind consumers to" the beginning of a topic. And the default checks only the previous version: "The Confluent Schema Registry default compatibility type BACKWARD is non-transitive". ([Confluent, Schema evolution and compatibility](https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html)). Renames are avoided because they are painful, so in practice the word is the identity.


---
**skeptics L92-93 · BODY · ABOVE C4**
*2. The skeptics › 2.1 Michael Stonebraker with Joe Hellerstein (2005) and with*

On schema-last (2005), REPORTED: "In a 'schema last' system, data instances must be self-describing, because there is not necessarily a schema to give meaning to incoming records. Without a self-describing format, a record is merely 'a bucket of bits'." And where it fits: "schema last is appropriate mainly for applications where free text is the mechanism for data entry." And the cost: "Any schema-last application will have to confront semantic heterogeneity on a record-by-record basis, where it will be even more costly to solve. This is a good reason to avoid 'schema last' if at all possible." The lessons they number: "Lesson 16: Schema-last is a probably a niche market". "Lesson 12: Unless there is a big performance or functionality advantage, new constructs will go nowhere." "Lesson 8: Logical data independence is easier with a simple data model than with a complex one." "Lesson 9: Technical debates are usually settled by the elephants of the marketplace, and often for reasons that have little to do with the technology." ([What Goes Around Comes Around, 2005](https://people.cs.umass.edu/~yanlei/courses/CS691LL-f06/papers/SH05.pdf))


---
**skeptics L106-106 · BODY · NEW-REASON C4,X2**

- Schema. INFERRED: Sid's design is not schema-last. The gate checks each value against a grammar fact. That is their "schema first": "the DBMS rejects any records that are not consistent with the schema." They would approve. Their warning lands on keys that are "locally extensible by practice": two groups coining two keys for one meaning is semantic heterogeneity "on a record-by-record basis".

---
**skeptics L234-234 · BODY · DISAGREES C4**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (17) first facts | Migrations and seeds; ids differ per environment | No source found |

---
**skeptics L235-235 · BODY · DISAGREES C4**

| Sid's question | Ordinary default | Skeptics and big tech |
| (3) key | Word for people, registry number on the wire; BACKWARD, non-transitive | 2005 paper: where keys grow locally, meaning drifts "record-by-record" (REPORTED) |

---
**sync L243-245 · BODY · NEW-REASON C4,E8**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

- (17) A server's existence is a write in the log; the first server is a fixed
  base case. REPORTED.


---
**sync L324-328 · BODY · DISAGREES C4,X2**
*2. Section one: sync and multiplayer › 2.2 Convex (Sujay Jayakar, James Cowling, Jamie Turner; 2021*

- The schema is not versioned. "The first push after a schema is added or
  modified will validate that all existing documents match the schema. If there
  are documents that fail validation, the push will fail."
  (https://docs.convex.dev/database/schemas) There is one current schema,
  enforced over the whole store.

---
**sync L359-361 · BODY · DISAGREES C4,X2**

- (3) One schema, enforced retroactively. REPORTED. This cannot transfer: a
  store that never rewrites cannot make old facts pass a new grammar.
  INFERRED.

---
**sync L887-891 · BODY · NEW-CASE C4,C1**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- *A content-named first record is fragile.* On making every client produce the
  same first change: "if the developer ever changes the initialisation code, it
  will produce a different change with a different hash… it would be a very
  fragile API that is easy to use incorrectly." (Kleppmann, automerge-classic
  #374, 2021)

---
**sync L964-967 · BODY · NEW-CASE C4,X2**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- The reversal: "Some of our early prototypes for storing documents that were
  compatible with multiple schemas performed data translations at write time.
  ... We eventually realized this was a flawed strategy. It struggled to handle
  new schemas getting added later on, after the write had already happened."

---
**sync L972-974 · BODY · NEW-REASON C4,X2**

- The pay-off for a store that never rewrites: "Since no evolution is done on
  write, old changes can be evolved using lenses that didn't even exist at the
  time of the original change."

---
**sync L976-980 · BODY · NEW-REASON C4,X2**

- The limit: they name three properties (consistency, conservation,
  predictability; "neither side operates on data they can't observe") and show
  a case where one must break. "Lens evolutions cannot magically make two
  incompatible pieces of software work perfectly together". And: "Data schemas
  aren't linear, even in centralized software."

---
**sync L983-988 · BODY · NEW-REASON C4**

- Five years on: "we have not yet built a production-ready version of this
  system that integrates with Automerge; one challenge is that it may require
  deep integration with the underlying data engine." (Litt, Horowitz, van
  Hardenberg, Matthews, "Malleable software", 2025,
  https://www.inkandswitch.com/essay/malleable-software/)


---
**sync L1093-1095 · BODY · NEW-REASON C4,C1**

- The type lives in the reference, not the thing: the content type "is part of
  the URL, not the document content, because the same document content may be
  rendered differently in different contexts."

---
**sync L1117-1120 · BODY · NEW-REASON C4,X2**

- (3) Tag every write with the grammar it was written under. Translate when
  reading, never when writing. Keep translators as data beside the facts.
  REPORTED. Expect that some pairs of grammars cannot be reconciled. REPORTED.
  Expect the engine to need deep support, or it will not ship. REPORTED.

---
**sync L1357-1364 · BODY · DISAGREES C4**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

### 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Daniel Holmgren, Devin Ivy; Martin Kleppmann as co-author; 2022–now)

**1. What they built and chose.** A signed record system at planet scale, built
by people who had built Secure Scuttlebutt and Beaker. Each account has one
signed repository. Records live at (account, schema name, key). Accounts are
named by permanent ids (DIDs), apart from changeable handles. Schemas are named
by words (NSIDs). Indexing is done by big separate services.


---
**sync L1392-1400 · BODY · DISAGREES C4,X2**

- Schema evolution rules: "Any new fields must be optional / Non-optional
  fields can not be removed… / Types can not change / Fields can not be renamed
  / If larger breaking changes are necessary, a new Lexicon name must be used."
  When is a schema fixed? "public adoption and implementation by a third party,
  even without explicit permission, indicates that the Lexicon has been
  released and should not break compatibility." Schemas are records: "Lexicon
  schemas are published publicly as records in atproto repositories". But the
  schema language is not written in itself: "It is an intentional decision to
  not express the Lexicon schema language itself recursively". (https://atproto.com/specs/lexicon)

---
**sync L1459-1464 · BODY · DISAGREES C4**

- *Field names collide.* "Bluesky added 'pinned posts' to profiles and
  unintentionally clobbered unspecced uses of that field." Frazee's conclusion:
  "Schemas are only interpretable in the context of working software", and
  "who controls a lexicon definition, and who has the most users for a given
  lexicon. Those are levers of authority." (Frazee, "Guidance on Authoring
  Lexicons", 2025, https://www.pfrazee.com/blog/lexicon-guidance)

---
**sync L1496-1501 · BODY · DISAGREES C4,X2**

- (3) Words, with owner authority through DNS, and strict additive rules.
  Breaking change means a new name. A schema is a record, but the language of
  schemas is outside the system. INSTITUTIONAL. The name is weaker than the
  software that writes it. REPORTED (Frazee). There is no schema version for a
  record to pin. REPORTED (Newbold). That last gap is the one Sid's "grammar is
  a versioned fact" closes. INFERRED.

---
**sync L1522-1525 · BODY · NEW-CASE C4**

- (17) "PLC stands for 'Placeholder' because we're not in love with a single
  service model." (Frazee, 2024.) The spec now reads "Public Ledger of
  Credentials". Governance moved to a Swiss association in 2025. REPORTED. A
  placeholder chosen for the first record became permanent.

---
**sync L1587-1593 · BODY · NEW-CASE C4**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

- *The registry of kinds.* "Most current NIPs are actually just schema
  descriptions of what tag is what and what is the shape of the events. It's
  kind of a waste of numbers and human memory". He proposes a bare registry
  "only for reserving kind numbers". (fiatjaf, "The end of NIPs", 2025,
  https://fiatjaf.com/311b999e.html) The index of NIPs keeps its dead: a dozen
  entries struck through with reasons, and deprecated kind numbers listed
  forever. NIP-41 (key invalidation) was removed outright.

---
**sync L1603-1607 · BODY · NEW-REASON C4**

- (3) Numbers need a registry and carry no meaning; the ranges that encode
  storage behaviour are irregular because kinds were handed out before the
  ranges existed. REPORTED facts; the reading is INFERRED. The idea worth
  keeping: *how a kind is stored* (kept, replaced, not stored) is a property of
  the kind. INFERRED.

---
**sync L1641-1642 · BODY · DISAGREES C4**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

- The kind is a bare word: `content.type` "must be a Unicode string between 3
  and 52 code units long".

---
**sync L1689-1694 · BODY · DISAGREES C4**

- *p2panda's schema ids and two kinds of reference.* "Application schema ids
  are constructed from the schema's name and document view id". And as field
  types: "Relations represent the whole referenced document through their
  document id… Pinned relations point at immutable versions of documents
  through their document view id". In 2024 they removed Bamboo: "we realised
  that we weren't making full use of the features it provides."

---
**sync L1711-1712 · BODY · DISAGREES C4**

- (3) A key can be a readable name plus the id of the exact grammar version
  (p2panda). REPORTED.

---
**sync L2080-2090 · BODY · DISAGREES C4**
*3. Section two: versioning › 3.4 Dolt, and Noms before it (Tim Sehn, Aaron Son, Andy Arth*

- *Column tags: an id for a name.* The idea: "generate and use a unique
  identifier to represent that column. If the column is renamed, no big deal,
  the identifier stays the same." The verdict: "column tags seem like a classic
  premature optimization." Users did not understand them, so they were hidden
  and made random. Random broke merging, because "if two branches add the same
  column, they must receive the same column tag". So they became deterministic,
  which is still wrong: "it's not truly history independent… it can cause a
  problem if two columns, even in different tables, try to use the same column
  tag." The post ends: "The next time we talk about column tags will be when we
  deprecate them." (Sehn and Fulghum, "Column Tags", 2025-05-15,
  https://www.dolthub.com/blog/2025-05-15-column-tags/)

---
**sync L2102-2113 · BODY · DISAGREES C4,P0**

**4. Which questions.** (1) If names come from bytes, a new byte format renames
everything, and every outside reference breaks unless a mapping is kept.
REPORTED. (13) Separate the logical fact from its encoding, and be able to
prove they match after a re-encode. REPORTED as practice. (3) An id for a
"kind of column" was their most regretted feature. REPORTED. The cause was
independent minting on branches that must later agree. One gate removes that
cause inside one store; layers and a second store bring it back. INFERRED. (2)
Counters fail wherever there is more than one place that can create; random ids
do not. REPORTED. (10) One primary, and sharding is the application's problem.
REPORTED. (9) Excision is a history rewrite followed by garbage collection.
REPORTED.


---
**sync L2153-2158 · BODY · NEW-REASON C4,C7**
*3. Section two: versioning › 3.5 Irmin (Thomas Gazagnaire, Anil Madhavapeddy, the Tarides*

- *An outside retrospective:* "it tries to do too many things and comes up
  short on some of them in ways that really matter." And on types: "you can
  only store values of a single type", with two workarounds: serialise
  everything, or grow one giant variant. (Patrick Ferris, "Irmin
  Retrospective", 2025, https://patrick.sirref.org/irmin-retro/)


---
**sync L2365-2369 · BODY · NEW-CASE C4,C1**
*4. Question by question › (17) The first facts*

- Do not derive the first record's id from code or content. Kleppmann: "it
  would be a very fragile API that is easy to use incorrectly." did:plc: ids
  that hash a legacy first operation "will unfortunately be around forever".
  Croquet: tying identity to a code hash made old state "inaccessible" after
  any code change. REPORTED.

---
**sync L2370-2373 · BODY · ABOVE C4**

- The language of grammars sits outside the system. AT Protocol: "an
  intentional decision to not express the Lexicon schema language itself
  recursively". Cambria names the same hole ("how Cambria's own data might be
  versioned"). REPORTED.

---
**sync L2374-2375 · BODY · NEW-REASON C4,E8**

- Bayou: a server's creation is an ordinary write, and the first server is a
  fixed base case. REPORTED.

---
**sync L2376-2377 · BODY · NEW-CASE C4**

- Frazee: "PLC stands for 'Placeholder'". It is now permanent. REPORTED.


---
**sync L2398-2401 · BODY · DISAGREES C4,X2**
*4. Question by question › (3) Key: a word, or an id with its name and shape as facts?*

- Words with an owner and additive-only evolution: AT Protocol. Names collide
  ("unintentionally clobbered unspecced uses of that field"). Fields "can not
  be renamed". Frazee: the name means what the software that writes it means.
  Newbold: there is no schema version for a record to pin. REPORTED.

---
**sync L2402-2404 · BODY · NEW-REASON C4**

- Numbers with a registry: Nostr. Storage behaviour is encoded in number
  ranges; dead numbers are listed forever; fiatjaf wants the registry to be
  only a registry. REPORTED.

---
**sync L2405-2405 · BODY · DISAGREES C4**

- Bare words: SSB; tldraw's flat namespace that "must not collide". REPORTED.

---
**sync L2406-2408 · BODY · DISAGREES C4**

- An id standing for a name: Dolt's column tags, "a classic premature
  optimization", because two branches must independently arrive at the same
  id. REPORTED.

---
**sync L2409-2409 · BODY · DISAGREES C4**

- A readable name plus the id of the exact grammar version: p2panda. REPORTED.

---
**sync L2410-2411 · BODY · NEW-REASON C4,X2**

- Tag each write with the grammar it was written under; translate on read;
  keep translators as data: Cambria. Unshipped after five years. REPORTED.

---
**sync L2414-2415 · BODY · DISAGREES C4,X2**

- One schema enforced over all existing data: Convex. REPORTED. Not available
  to a store that never rewrites.

---
**sync L2419-2421 · BODY · NEW-REASON C4**

- One value type per store forces blobs or one giant union: the Irmin
  retrospective. REPORTED.


---
**sync L2422-2424 · BODY · DISAGREES C4**

**Where they split.** Words (the majority practice) against ids (Dolt tried and
regrets it; p2panda combines both).


---
**sync L2680-2682 · BODY · NEW-REASON C4,E5**
*4. Question by question › (4) Based on: does each read say whether the fact depends on*

- Floating or pinned is a property of the *kind of reference*: p2panda's
  "Relations" and "Pinned relations" are field types; AT Protocol's plain URI
  floats and its "strong reference" pins. REPORTED and INSTITUTIONAL.

---
**sync L2960-2961 · BODY · NEW-REASON C4**
*4. Question by question › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

- How a kind is kept is a property of the kind: Nostr, tldraw. REPORTED.


---
**sync L3032-3035 · BODY · CARRIED C4,C1,C5**
*5. The group › 5.1 The voices that matter most, who I dropped, who is missi*

- **Datomic (Rich Hickey).** Attributes are entities whose names are facts
  about them; transactions are entities; "as of"; excision. It is the
  production precedent for the id side of (3), and the counterweight to Dolt's
  regret. Probably another group's.

---
**sync L3197-3199 · BODY · NEW-REASON C4**
*6. The second store: what each source implies*

- **Keys between stores.** Dolt's problem returns in full: two stores mint
  different ids for "the same" key. p2panda's readable name plus definition id
  is the known shape; a "same as" fact is the repair.

---
**sync L3200-3202 · BODY · NEW-REASON C4,X2**

- **Grammars will diverge.** "Data schemas aren't linear, even in centralized
  software." Cambria's answer is translators as data, applied on read, and an
  accepted list of pairs that cannot be reconciled.

---
**sync L3212-3213 · BODY · NEW-REASON C4,E8**

- **Birth.** Bayou made a server's creation an ordinary write in an existing
  server's log (REPORTED). A second store's birth can be a fact in the first.

